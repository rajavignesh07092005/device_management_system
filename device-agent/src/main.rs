use reqwest::{Client, StatusCode};
use serde_json::Value;
use std::fs;
use std::sync::Arc;
use std::time::Duration;
use tokio::sync::Mutex;
use tokio::time::sleep;
use winreg::enums::*;
use winreg::RegKey;

const BASE_URL: &str = "https://amply-lagoon-regally.ngrok-free.dev/device_server_war_exploded";
const PSK: &str = "MY_DEVICE_PSK";
const REGISTRY_PATH: &str = "SOFTWARE\\DeviceManagementAgent";
const HEARTBEAT_INTERVAL: u64 = 120;

#[tokio::main]
async fn main() {
    println!("=================================");
    println!(" DEVICE MANAGEMENT AGENT");
    println!("=================================");

    let psk = load_psk();
    let client = Client::new();

    let token = match load_token() {
        Some(saved_token) => {
            println!("Saved token found.");
            match authenticate(&client, &saved_token).await {
                Ok(true) => {
                    println!("Authentication successful.");
                    saved_token
                }
                Ok(false) => {
                    println!("Saved token is invalid. Registering again...");
                    match register(&client, &psk).await {
                        Ok(new_token) => {
                            save_token(&new_token);
                            new_token
                        }
                        Err(e) => {
                            println!("Re-registration failed: {}", e);
                            return;
                        }
                    }
                }
                Err(e) => {
                    println!("Authentication request failed: {}", e);
                    return;
                }
            }
        }
        None => {
            println!("No saved token. Registering device...");
            match register(&client, &psk).await {
                Ok(new_token) => {
                    save_token(&new_token);
                    new_token
                }
                Err(e) => {
                    println!("Registration failed: {}", e);
                    return;
                }
            }
        }
    };

    let shared_token = Arc::new(Mutex::new(token));

    let heartbeat_client = client.clone();
    let heartbeat_token = Arc::clone(&shared_token);
    tokio::spawn(async move {
        heartbeat_loop(heartbeat_client, heartbeat_token).await;
    });

    command_loop(client, shared_token).await;
}

// ---------- PSK loading ----------

fn load_psk() -> String {
    PSK.to_string()
}

// ---------- Device info collection ----------

fn get_hostname() -> String {
    hostname::get()
        .ok()
        .and_then(|h| h.into_string().ok())
        .unwrap_or_else(|| "unknown-host".to_string())
}

fn get_ip_address() -> String {
    local_ip_address::local_ip()
        .map(|ip| ip.to_string())
        .unwrap_or_else(|_| "unknown-ip".to_string())
}

fn get_os_version() -> String {
    let info = os_info::get();
    format!("{} {}", info.os_type(), info.version())
}

fn get_serial_number() -> Option<String> {
    use std::process::Command;

    let output = Command::new("wmic")
        .args(["bios", "get", "serialnumber"])
        .output();

    match output {
        Ok(result) => {
            let text = String::from_utf8_lossy(&result.stdout);
            let lines: Vec<&str> = text
                .lines()
                .map(|l| l.trim())
                .filter(|l| !l.is_empty())
                .collect();
            if lines.len() >= 2 {
                Some(lines[1].to_string())
            } else {
                None
            }
        }
        Err(_) => None,
    }
}

// ---------- Token storage (Windows Registry) ----------

fn save_token(token: &str) {
    let hkcu = RegKey::predef(HKEY_CURRENT_USER);
    match hkcu.create_subkey(REGISTRY_PATH) {
        Ok((key, _)) => {
            if let Err(e) = key.set_value("DeviceToken", &token) {
                println!("Could not save token to registry: {}", e);
            } else {
                println!("Token saved to registry.");
            }
        }
        Err(e) => println!("Could not create registry key: {}", e),
    }
}

fn load_token() -> Option<String> {
    let hkcu = RegKey::predef(HKEY_CURRENT_USER);
    let key = hkcu.open_subkey(REGISTRY_PATH).ok()?;
    let token: String = key.get_value("DeviceToken").ok()?;
    let token = token.trim().to_string();
    if token.is_empty() {
        None
    } else {
        Some(token)
    }
}

// ---------- Registration & authentication ----------

async fn register(client: &Client, psk: &str) -> Result<String, String> {
    let body = serde_json::json!({
        "psk": psk,
        "hostname": get_hostname(),
        "ipAddress": get_ip_address(),
        "osVersion": get_os_version(),
        "serialNumber": get_serial_number().unwrap_or_else(|| "unavailable".to_string())
    });

    let response = client
        .post(format!("{}/register", BASE_URL))
        .json(&body)
        .send()
        .await
        .map_err(|e| e.to_string())?;

    println!("Registration status: {}", response.status());

    if response.status() != StatusCode::CREATED {
        let text = response.text().await.unwrap_or_default();
        return Err(format!("Registration failed: {}", text));
    }

    let json: Value = response.json().await.map_err(|e| e.to_string())?;
    let token = json["deviceToken"]
        .as_str()
        .ok_or("deviceToken missing in response")?;

    println!("Registration successful.");
    println!("Device token: {}", token);
    Ok(token.to_string())
}

async fn authenticate(client: &Client, token: &str) -> Result<bool, String> {
    let response = client
        .get(format!("{}/device", BASE_URL))
        .bearer_auth(token)
        .send()
        .await
        .map_err(|e| e.to_string())?;

    if response.status() == StatusCode::OK {
        Ok(true)
    } else if response.status() == StatusCode::UNAUTHORIZED {
        Ok(false)
    } else {
        Err(format!("Authentication returned {}", response.status()))
    }
}

// ---------- Heartbeat loop ----------

enum HeartbeatError {
    Unauthorized,
    Other(String),
}

async fn heartbeat_loop(client: Client, shared_token: Arc<Mutex<String>>) {
    loop {
        let token = {
            let token_guard = shared_token.lock().await;
            token_guard.clone()
        };

        match send_heartbeat(&client, &token).await {
            Ok(_) => {
                println!("Heartbeat successful.");
            }
            Err(HeartbeatError::Unauthorized) => {
                println!("Heartbeat token rejected. Re-registering...");
                let psk = load_psk();
                match register(&client, &psk).await {
                    Ok(new_token) => {
                        save_token(&new_token);
                        let mut token_guard = shared_token.lock().await;
                        *token_guard = new_token;
                        println!("Heartbeat token updated.");
                    }
                    Err(e) => {
                        println!("Heartbeat re-registration failed: {}", e);
                        return;
                    }
                }
            }
            Err(HeartbeatError::Other(e)) => {
                println!("Heartbeat failed: {}", e);
            }
        }

        sleep(Duration::from_secs(HEARTBEAT_INTERVAL)).await;
    }
}

async fn send_heartbeat(client: &Client, token: &str) -> Result<(), HeartbeatError> {
    let body = serde_json::json!({
        "hostname": get_hostname(),
        "ipAddress": get_ip_address(),
        "osVersion": get_os_version(),
        "serialNumber": get_serial_number().unwrap_or_else(|| "unavailable".to_string())
    });

    let response = client
        .post(format!("{}/heartbeat", BASE_URL))
        .bearer_auth(token)
        .json(&body)
        .send()
        .await
        .map_err(|e| HeartbeatError::Other(e.to_string()))?;

    if response.status() == StatusCode::OK {
        Ok(())
    } else if response.status() == StatusCode::UNAUTHORIZED {
        Err(HeartbeatError::Unauthorized)
    } else {
        Err(HeartbeatError::Other(format!("HTTP {}", response.status())))
    }
}

// ---------- Command loop ----------

enum WaitError {
    Unauthorized,
    Other(String),
}

enum AckError {
    Unauthorized,
    Other(String),
}

async fn command_loop(client: Client, shared_token: Arc<Mutex<String>>) {
    loop {
        println!("Waiting for command...");

        let token = {
            let token_guard = shared_token.lock().await;
            token_guard.clone()
        };

        match wait_for_command(&client, &token).await {
            Ok(true) => {
                println!("Command available!");
                match fetch_command(&client, &token).await {
                    Ok(Some((command_id, command))) => {
                        println!("Command received: {}", command);
                        println!("Processing command: {}", command);

                        match acknowledge_command(&client, &token, command_id).await {
                            Ok(_) => {
                                println!("Command {} acknowledged.", command_id);
                            }
                            Err(AckError::Unauthorized) => {
                                println!("Token rejected during acknowledgement. Re-registering...");
                                let psk = load_psk();
                                match register(&client, &psk).await {
                                    Ok(new_token) => {
                                        save_token(&new_token);
                                        let mut token_guard = shared_token.lock().await;
                                        *token_guard = new_token;
                                    }
                                    Err(e) => {
                                        println!("Re-registration failed: {}", e);
                                        return;
                                    }
                                }
                            }
                            Err(AckError::Other(e)) => {
                                println!("Acknowledgement failed: {}", e);
                            }
                        }
                    }
                    Ok(None) => {
                        println!("No command available after signal.");
                    }
                    Err(e) => {
                        println!("Fetch failed: {}", e);
                    }
                }
            }
            Ok(false) => {
                println!("No command available.");
            }
            Err(WaitError::Unauthorized) => {
                println!("Token rejected while waiting. Re-registering...");
                let psk = load_psk();
                match register(&client, &psk).await {
                    Ok(new_token) => {
                        save_token(&new_token);
                        let mut token_guard = shared_token.lock().await;
                        *token_guard = new_token;
                        println!("Command-loop token updated.");
                    }
                    Err(e) => {
                        println!("Re-registration failed: {}", e);
                        return;
                    }
                }
            }
            Err(WaitError::Other(e)) => {
                println!("Wait failed: {}", e);
            }
        }
    }
}

async fn wait_for_command(client: &Client, token: &str) -> Result<bool, WaitError> {
    let response = client
        .get(format!("{}/commands/wait", BASE_URL))
        .bearer_auth(token)
        .send()
        .await
        .map_err(|e| WaitError::Other(e.to_string()))?;

    match response.status() {
        StatusCode::OK => Ok(true),
        StatusCode::NO_CONTENT => Ok(false),
        StatusCode::UNAUTHORIZED => Err(WaitError::Unauthorized),
        status => Err(WaitError::Other(format!("HTTP {}", status))),
    }
}

async fn fetch_command(client: &Client, token: &str) -> Result<Option<(i32, String)>, String> {
    let response = client
        .get(format!("{}/commands/fetch", BASE_URL))
        .bearer_auth(token)
        .send()
        .await
        .map_err(|e| e.to_string())?;

    if response.status() == StatusCode::NO_CONTENT {
        return Ok(None);
    }
    if response.status() != StatusCode::OK {
        return Err(format!("Fetch returned {}", response.status()));
    }

    let json: Value = response.json().await.map_err(|e| e.to_string())?;
    let command_id = json["commandId"].as_i64().ok_or("commandId missing")? as i32;
    let command = json["command"]
        .as_str()
        .ok_or("command missing")?
        .to_string();

    Ok(Some((command_id, command)))
}

async fn acknowledge_command(
    client: &Client,
    token: &str,
    command_id: i32,
) -> Result<(), AckError> {
    let body = serde_json::json!({ "commandId": command_id });

    let response = client
        .post(format!("{}/commands/ack", BASE_URL))
        .bearer_auth(token)
        .json(&body)
        .send()
        .await
        .map_err(|e| AckError::Other(e.to_string()))?;

    if response.status() == StatusCode::OK {
        Ok(())
    } else if response.status() == StatusCode::UNAUTHORIZED {
        Err(AckError::Unauthorized)
    } else {
        Err(AckError::Other(format!("HTTP {}", response.status())))
    }
}