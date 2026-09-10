package org.example.service;

import org.example.dao.AdminDAO;
import org.example.model.AdminLoginRequest;
import org.example.model.AdminCreateRequest;
import org.example.util.PasswordHasher;

import java.util.UUID;

public class AdminService {

    private final AdminDAO adminDAO = new AdminDAO();

    public String login(AdminLoginRequest request) throws Exception {
        AdminDAO.AdminRecord admin = adminDAO.findByUsername(request.username);
        if (admin == null) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        String[] parts = admin.passwordHash.split(":");
        String salt = parts[0];
        String storedHash = parts[1];

        String incomingHash = PasswordHasher.hash(request.password, salt);

        if (!incomingHash.equals(storedHash)) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        String newToken = UUID.randomUUID().toString();
        adminDAO.updateToken(admin.id, newToken);
        return newToken;
    }

    public void createAdmin(AdminCreateRequest request) throws Exception {
        String salt = PasswordHasher.generateSalt();
        String hashedPassword = PasswordHasher.hash(request.password, salt);
        String combined = salt + ":" + hashedPassword;
        adminDAO.insertAdmin(request.username, combined);
    }
}
