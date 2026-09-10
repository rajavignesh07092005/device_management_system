package org.example.dao;

import org.example.util.DatabaseConnection;
import java.util.*;

import org.example.util.DatabaseConnection;
import java.sql.*;

public class CommandDAO {

    public boolean addCommand(int deviceId, String command) throws Exception {
        String checkSql = "SELECT id FROM devices WHERE id=?";
        String insertSql = "INSERT INTO commands (device_id, command_text, status) VALUES (?, ?, 'PENDING')";
        String historySql = "INSERT INTO command_history (command_id, device_id, status) VALUES (?, ?, 'PENDING')";

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement check = connection.prepareStatement(checkSql)) {
                    check.setInt(1, deviceId);
                    try (ResultSet rs = check.executeQuery()) {
                        if (!rs.next()) {
                            connection.rollback();
                            return false;
                        }
                    }
                }

                int newCommandId;
                try (PreparedStatement insert = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    insert.setInt(1, deviceId);
                    insert.setString(2, command);
                    insert.executeUpdate();
                    try (ResultSet keys = insert.getGeneratedKeys()) {
                        keys.next();
                        newCommandId = keys.getInt(1);
                    }
                }

                try (PreparedStatement history = connection.prepareStatement(historySql)) {
                    history.setInt(1, newCommandId);
                    history.setInt(2, deviceId);
                    history.executeUpdate();
                }

                connection.commit();
                return true;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public boolean hasPendingCommand(int deviceId) throws Exception {
        String sql = "SELECT id FROM commands WHERE device_id=? AND status='PENDING' LIMIT 1";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, deviceId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    public CommandResult fetchCommand(int deviceId) throws Exception {
        String selectSql = "SELECT id, command_text FROM commands " +
                "WHERE device_id=? AND status='PENDING' ORDER BY id LIMIT 1 FOR UPDATE";
        String updateSql = "UPDATE commands SET status='FETCHED', fetched_at=CURRENT_TIMESTAMP WHERE id=?";
        String historySql = "INSERT INTO command_history (command_id, device_id, status) VALUES (?, ?, 'FETCHED')";

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Integer commandId = null;
                String commandText = null;

                try (PreparedStatement select = connection.prepareStatement(selectSql)) {
                    select.setInt(1, deviceId);
                    try (ResultSet rs = select.executeQuery()) {
                        if (rs.next()) {
                            commandId = rs.getInt("id");
                            commandText = rs.getString("command_text");
                        }
                    }
                }

                if (commandId == null) {
                    connection.commit();
                    return null;
                }

                try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                    update.setInt(1, commandId);
                    update.executeUpdate();
                }

                try (PreparedStatement history = connection.prepareStatement(historySql)) {
                    history.setInt(1, commandId);
                    history.setInt(2, deviceId);
                    history.executeUpdate();
                }

                connection.commit();
                return new CommandResult(commandId, commandText);
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public boolean acknowledgeCommand(int commandId, int deviceId) throws Exception {
        String updateSql = "UPDATE commands SET status='ACKNOWLEDGED', acknowledged_at=CURRENT_TIMESTAMP " +
                "WHERE id=? AND device_id=? AND status='FETCHED'";
        String historySql = "INSERT INTO command_history (command_id, device_id, status) VALUES (?, ?, 'ACKNOWLEDGED')";

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int updatedRows;
                try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                    update.setInt(1, commandId);
                    update.setInt(2, deviceId);
                    updatedRows = update.executeUpdate();
                }

                if (updatedRows == 0) {
                    connection.rollback();
                    return false;
                }

                try (PreparedStatement history = connection.prepareStatement(historySql)) {
                    history.setInt(1, commandId);
                    history.setInt(2, deviceId);
                    history.executeUpdate();
                }

                connection.commit();
                return true;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<HistoryEntry> getHistoryForDevice(int deviceId) throws Exception {
        String sql = "SELECT command_id, status, changed_at FROM command_history " +
                "WHERE device_id=? ORDER BY changed_at ASC";
        List<HistoryEntry> results = new java.util.ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, deviceId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    results.add(new HistoryEntry(
                            rs.getInt("command_id"),
                            rs.getString("status"),
                            rs.getTimestamp("changed_at").toString()
                    ));
                }
            }
        }
        return results;
    }

    public static class CommandResult {
        public final int id;
        public final String commandText;
        public CommandResult(int id, String commandText) {
            this.id = id;
            this.commandText = commandText;
        }
    }

    public static class HistoryEntry {
        public final int commandId;
        public final String status;
        public final String changedAt;
        public HistoryEntry(int commandId, String status, String changedAt) {
            this.commandId = commandId;
            this.status = status;
            this.changedAt = changedAt;
        }
    }
}