package org.example.util;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordHasher {

    public static String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hash(String password, String salt) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(Base64.getDecoder().decode(salt));
        byte[] hashedBytes = digest.digest(password.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(hashedBytes);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: PasswordHasher <username> <password>");
            return;
        }
        String username = args[0];
        String password = args[1];

        String salt = generateSalt();
        String hashedPassword = hash(password, salt);

        // We store salt+hash together, separated by a colon,
        // so we only need ONE database column instead of two.
        String combined = salt + ":" + hashedPassword;

        System.out.println("Run this SQL manually:");
        System.out.println("INSERT INTO admins (username, password_hash) VALUES ('"
                + username + "', '" + combined + "');");
    }
}
