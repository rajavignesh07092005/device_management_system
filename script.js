const BASE_URL = "https://amply-lagoon-regally.ngrok-free.dev/device_server_war_exploded";

let adminToken = null;

async function handleLogin() {
    const username = document.getElementById("login-username").value;
    const password = document.getElementById("login-password").value;
    const statusEl = document.getElementById("login-status");

    try {
        const response = await fetch(`${BASE_URL}/admin/login`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "ngrok-skip-browser-warning": "true"
            },
            body: JSON.stringify({ username, password })
        });

        const data = await response.json();

        if (response.ok) {
            adminToken = data.adminToken;
            statusEl.textContent = "Login successful.";
            showLoggedInSections();
        } else {
            statusEl.textContent = "Login failed: " + data.error;
        }
    } catch (err) {
        statusEl.textContent = "Error connecting to server: " + err.message;
    }
}

function showLoggedInSections() {
    document.getElementById("login-section").classList.add("hidden");
    document.getElementById("command-section").classList.remove("hidden");
    document.getElementById("history-section").classList.remove("hidden");
    document.getElementById("create-admin-section").classList.remove("hidden");
    document.getElementById("revoke-section").classList.remove("hidden");
    document.getElementById("logout-section").classList.remove("hidden");
}

function handleLogout() {
    adminToken = null;
    document.getElementById("login-section").classList.remove("hidden");
    document.getElementById("command-section").classList.add("hidden");
    document.getElementById("history-section").classList.add("hidden");
    document.getElementById("create-admin-section").classList.add("hidden");
    document.getElementById("revoke-section").classList.add("hidden");
    document.getElementById("logout-section").classList.add("hidden");
    document.getElementById("login-status").textContent = "";
}

async function handleSendCommand() {
    const deviceId = document.getElementById("command-device-id").value;
    const commandText = document.getElementById("command-text").value;
    const statusEl = document.getElementById("command-status");

    try {
        const response = await fetch(`${BASE_URL}/commands`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + adminToken,
                "ngrok-skip-browser-warning": "true"
            },
            body: JSON.stringify({
                deviceId: parseInt(deviceId),
                command: commandText
            })
        });

        const data = await response.json();

        if (response.ok) {
            statusEl.textContent = "Command sent successfully.";
        } else {
            statusEl.textContent = "Failed: " + data.error;
        }
    } catch (err) {
        statusEl.textContent = "Error: " + err.message;
    }
}

async function handleViewHistory() {
    const deviceId = document.getElementById("history-device-id").value;
    const tbody = document.getElementById("history-body");
    tbody.innerHTML = "";

    try {
        const response = await fetch(`${BASE_URL}/admin/history?deviceId=${deviceId}`, {
            method: "GET",
            headers: {
                "Authorization": "Bearer " + adminToken,
                "ngrok-skip-browser-warning": "true"
            }
        });

        const data = await response.json();

        if (response.ok) {
            data.forEach(entry => {
                const row = document.createElement("tr");
                row.innerHTML = `<td>${entry.commandId}</td><td>${entry.status}</td><td>${entry.changedAt}</td>`;
                tbody.appendChild(row);
            });
        } else {
            alert("Failed to load history: " + data.error);
        }
    } catch (err) {
        alert("Error: " + err.message);
    }
}

async function handleCreateAdmin() {
    const username = document.getElementById("new-admin-username").value;
    const password = document.getElementById("new-admin-password").value;
    const statusEl = document.getElementById("create-admin-status");

    try {
        const response = await fetch(`${BASE_URL}/admin/create`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + adminToken,
                "ngrok-skip-browser-warning": "true"
            },
            body: JSON.stringify({ username, password })
        });

        const data = await response.json();

        if (response.ok) {
            statusEl.textContent = "Admin created successfully.";
            document.getElementById("new-admin-username").value = "";
            document.getElementById("new-admin-password").value = "";
        } else {
            statusEl.textContent = "Failed: " + data.error;
        }
    } catch (err) {
        statusEl.textContent = "Error: " + err.message;
    }
}

async function handleRevoke() {
    const deviceId = document.getElementById("revoke-device-id").value;
    const statusEl = document.getElementById("revoke-status");

    try {
        const response = await fetch(`${BASE_URL}/admin/revoke`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + adminToken,
                "ngrok-skip-browser-warning": "true"
            },
            body: JSON.stringify({ deviceId: parseInt(deviceId) })
        });

        const data = await response.json();

        if (response.ok) {
            statusEl.textContent = "Device token revoked successfully.";
            document.getElementById("revoke-device-id").value = "";
        } else {
            statusEl.textContent = "Failed: " + data.error;
        }
    } catch (err) {
        statusEl.textContent = "Error: " + err.message;
    }
}