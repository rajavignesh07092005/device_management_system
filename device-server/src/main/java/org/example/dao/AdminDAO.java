package org.example.dao;

import org.example.util.DatabaseConnection;
import java.sql.*;

public class AdminDAO {

    public AdminRecord findByUsername(String username) throws Exception {
        String sql = "SELECT id, username, password_hash, admin_token, token_revoked FROM admins WHERE username=?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new AdminRecord(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("admin_token"),
                            rs.getBoolean("token_revoked")
                    );
                }
            }
        }
        return null;
    }

    public void insertAdmin(String username, String passwordHash) throws Exception {
        String sql = "INSERT INTO admins (username, password_hash) VALUES (?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.executeUpdate();
        }
    }

    public void updateToken(int adminId, String newToken) throws Exception {
        String sql = "UPDATE admins SET admin_token=?, token_revoked=FALSE WHERE id=?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newToken);
            statement.setInt(2, adminId);
            statement.executeUpdate();
        }
    }

    public boolean isValidToken(String token) throws Exception {
        String sql = "SELECT id FROM admins WHERE admin_token=? AND token_revoked=FALSE";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, token);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static class AdminRecord {
        public final int id;
        public final String username;
        public final String passwordHash;
        public final String adminToken;
        public final boolean tokenRevoked;

        public AdminRecord(int id, String username, String passwordHash,
                           String adminToken, boolean tokenRevoked) {
            this.id = id;
            this.username = username;
            this.passwordHash = passwordHash;
            this.adminToken = adminToken;
            this.tokenRevoked = tokenRevoked;
        }
    }
}