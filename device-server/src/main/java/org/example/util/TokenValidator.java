package org.example.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TokenValidator {

    public static boolean isValid(String token) throws Exception {
        if (token == null || token.isBlank()) {
            return false;
        }

        String sql = "SELECT id FROM devices WHERE device_token = ? AND token_revoked = FALSE";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, token);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public static int getDeviceId(String token) throws Exception {
        String sql = "SELECT id FROM devices WHERE device_token = ? AND token_revoked = FALSE";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, token);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id");
                }
                return -1;
            }
        }
    }
}
