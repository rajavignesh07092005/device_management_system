package org.example.dao;

import org.example.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class DeviceDAO {

    public void updateHeartbeat(
            String token,
            String hostname,
            String ipAddress,
            String osVersion,
            String serialNumber
    ) throws Exception {

        String sql = """
            UPDATE devices
            SET hostname = ?,
                ip_address = ?,
                os_version = ?,
                serial_number = ?,
                last_seen = CURRENT_TIMESTAMP
            WHERE device_token = ?
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, hostname);   
            statement.setString(2, ipAddress);
            statement.setString(3, osVersion);
            statement.setString(4, serialNumber);
            statement.setString(5, token);

            statement.executeUpdate();
        }
    }

    public boolean revokeToken(int deviceId) throws Exception {
        String sql = "UPDATE devices SET token_revoked=TRUE WHERE id=?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, deviceId);
            return statement.executeUpdate() == 1;
        }
    }

    public Integer findDeviceIdByIdentity(String hostname, String serialNumber) throws Exception {
        String sql = "SELECT id FROM devices WHERE hostname=? AND serial_number=?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, hostname);
            statement.setString(2, serialNumber);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return null;
    }

    public void insertNewDevice(String hostname, String ipAddress, String osVersion,
                                String serialNumber, String deviceToken) throws Exception {
        String sql = "INSERT INTO devices (hostname, ip_address, os_version, serial_number, device_token) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, hostname);
            statement.setString(2, ipAddress);
            statement.setString(3, osVersion);
            statement.setString(4, serialNumber);
            statement.setString(5, deviceToken);
            statement.executeUpdate();
        }
    }

    public void reissueToken(int existingDeviceId, String ipAddress, String osVersion, String newToken) throws Exception {
        String sql = "UPDATE devices SET ip_address=?, os_version=?, device_token=?, token_revoked=FALSE " +
                "WHERE id=?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ipAddress);
            statement.setString(2, osVersion);
            statement.setString(3, newToken);
            statement.setInt(4, existingDeviceId);
            statement.executeUpdate();
        }
    }

    public List<DeviceSummary> getAllDevices() throws Exception {
        String sql = "SELECT id, hostname, ip_address, last_seen FROM devices ORDER BY id";
        List<DeviceSummary> results = new java.util.ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                results.add(new DeviceSummary(
                        rs.getInt("id"),
                        rs.getString("hostname"),
                        rs.getString("ip_address"),
                        rs.getTimestamp("last_seen") != null ? rs.getTimestamp("last_seen").toString() : "Never"
                ));
            }
        }
        return results;
    }

    public static class DeviceSummary {
        public final int id;
        public final String hostname;
        public final String ipAddress;
        public final String lastSeen;
        public DeviceSummary(int id, String hostname, String ipAddress, String lastSeen) {
            this.id = id;
            this.hostname = hostname;
            this.ipAddress = ipAddress;
            this.lastSeen = lastSeen;
        }
    }
}