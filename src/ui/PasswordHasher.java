package ui;

import util.DBConnection;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PasswordHasher {

    public static void main(String[] args) {
        try {
            updatePasswords("students");
            updatePasswords("professors");
            System.out.println("Şifreler hashlenerek güncellendi.");
            // Bağlantıyı kapat (opsiyonel, DBConnection'daki implementasyona göre)
            DBConnection.closeConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updatePasswords(String tableName) throws Exception {
        String selectSQL = "SELECT id, password FROM " + tableName + " WHERE password IS NOT NULL AND (password_hash IS NULL OR password_hash = '')";
        String updateSQL = "UPDATE " + tableName + " SET password_hash = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(selectSQL);
             ResultSet rs = selectStmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String plainPassword = rs.getString("password");
                String hashed = hashPassword(plainPassword);

                try (PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {
                    updateStmt.setString(1, hashed);
                    updateStmt.setInt(2, id);
                    updateStmt.executeUpdate();
                }
            }
        }
    }

    private static String hashPassword(String password) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(password.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
