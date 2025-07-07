package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.security.MessageDigest;
import java.sql.*;

import util.DBConnection;

public class LoginFrame extends JFrame {

    private JTextField emailField;
    private JPasswordField passwordField;
    private JComboBox<String> roleBox;
    private JLabel errorLabel;

    public LoginFrame() {
        setTitle("Akademik Randevu ve Takip Sistemi - Giriş");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(235, 245, 255));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Başlık
        JLabel titleLabel = new JLabel("Akademik Randevu Sistemine HOŞGELDİNİZ", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setForeground(new Color(33, 74, 134));
        titleLabel.setForeground(new Color(128, 0, 128)); // Klasik mor rengi (RGB)
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(titleLabel, gbc);

        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;

        // E-posta
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("E-posta:"), gbc);

        gbc.gridx = 1;
        emailField = new JTextField(20);
        emailField.setToolTipText("E-posta adresinizi giriniz");
        panel.add(emailField, gbc);

        // Şifre
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Şifre:"), gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        passwordField.setToolTipText("Şifrenizi giriniz");
        panel.add(passwordField, gbc);

        // Rol
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("Rol:"), gbc);

        gbc.gridx = 1;
        roleBox = new JComboBox<>(new String[]{"Öğrenci", "Öğretim Üyesi"});
        panel.add(roleBox, gbc);

        // Hata mesajı
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        errorLabel = new JLabel("");
        errorLabel.setForeground(Color.RED);
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(errorLabel, gbc);

        // Giriş butonu
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        JButton loginButton = new JButton("Giriş Yap");
        loginButton.setBackground(new Color(51, 122, 183));
        loginButton.setForeground(Color.WHITE);
        loginButton.setBackground(new Color(128, 0, 128)); // Klasik mor
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        loginButton.setFocusPainted(false);
        panel.add(loginButton, gbc);

        // Enter ile login tetikleme
        KeyAdapter enterKeyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                    loginButton.doClick();
                }
            }
        };
        emailField.addKeyListener(enterKeyListener);
        passwordField.addKeyListener(enterKeyListener);
        roleBox.addKeyListener(enterKeyListener);

        // Buton aksiyonu
        loginButton.addActionListener(e -> {
            errorLabel.setText("");
            String email = emailField.getText().trim();
            String password = new String(passwordField.getPassword());
            String role = (String) roleBox.getSelectedItem();

            if (email.isEmpty() || password.isEmpty()) {
                errorLabel.setText("Lütfen tüm alanları doldurun.");
                return;
            }

            try {
                if (validateUser(email, password, role)) {
                    int userId = getUserId(email, role);
                    if (userId == -1) {
                        errorLabel.setText("Kullanıcı ID alınamadı.");
                        return;
                    }

                    if ("Öğrenci".equals(role)) {
                        new StudentPanel(userId).setVisible(true);
                    } else {
                        new ProfessorPanel(userId).setVisible(true);
                    }
                    dispose();
                } else {
                    errorLabel.setText("E-posta veya şifre hatalı!");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                errorLabel.setText("Bir hata oluştu.");
            }
        });

        add(panel);
        setVisible(true);
    }

    private boolean validateUser(String email, String password, String role) throws Exception {
        String sql;
        if ("Öğrenci".equals(role)) {
            sql = "SELECT * FROM students WHERE email = ? AND password_hash = ?";
        } else if ("Öğretim Üyesi".equals(role)) {
            sql = "SELECT * FROM professors WHERE email = ? AND password_hash = ?";
        } else {
            return false;
        }

        String hashedPassword = hashPassword(password);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, hashedPassword);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private int getUserId(String email, String role) {
        String table = "Öğrenci".equals(role) ? "students" : "professors";
        String sql = "SELECT id FROM " + table + " WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private String hashPassword(String password) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(password.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
