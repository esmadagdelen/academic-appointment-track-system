package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Properties;

import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;

import util.DBConnection;

public class StudentPanel extends JFrame {

    private JComboBox<String> professorCombo;
    private JDatePickerImpl datePicker;
    private JComboBox<String> timeSlotCombo;
    private JButton createButton, cancelButton, suggestedButton, calendarButton;
    private JTextArea appointmentList;

    private int studentId;

    public StudentPanel(int studentId) {
        this.studentId = studentId;

        setTitle("Öğrenci Paneli");
        setSize(700, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(245, 250, 255));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Öğretim Üyesi Seçimi
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Öğretim Üyesi Seç:"), gbc);

        gbc.gridx = 1;
        professorCombo = new JComboBox<>();
        loadProfessors();
        panel.add(professorCombo, gbc);

        // Tarih Seçimi
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Tarih Seç:"), gbc);

        gbc.gridx = 1;
        UtilDateModel model = new UtilDateModel();
        model.setSelected(true);
        Properties p = new Properties();
        p.put("text.today", "Bugün");
        p.put("text.month", "Ay");
        p.put("text.year", "Yıl");

        JDatePanelImpl datePanel = new JDatePanelImpl(model, p);
        datePicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());
        panel.add(datePicker, gbc);

        // Saat Seçimi
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Saat Seç:"), gbc);

        gbc.gridx = 1;
        timeSlotCombo = new JComboBox<>(new String[]{
                "09:00", "09:20", "11:00", "11:20", "13:00", "13:20", "13:40"
        });
        panel.add(timeSlotCombo, gbc);

        // Randevu Oluştur Butonu
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        createButton = new JButton("Randevu Oluştur");
        createButton.setBackground(new Color(128, 0, 128));
        createButton.setForeground(Color.WHITE);
        createButton.setOpaque(true);
        createButton.setBorderPainted(false);
        createButton.addActionListener(this::createAppointment);
        panel.add(createButton, gbc);

        // Önerilen Randevular Butonu
        gbc.gridy = 4;
        suggestedButton = new JButton("Önerilen Randevular");
        suggestedButton.setBackground(new Color(0, 102, 204));
        suggestedButton.setForeground(Color.WHITE);
        suggestedButton.setOpaque(true);
        suggestedButton.setBorderPainted(false);
        suggestedButton.addActionListener(e -> new SuggestedAppointmentsFrame(studentId));
        panel.add(suggestedButton, gbc);

        // Haftalık Takvim Butonu
        gbc.gridy = 5;
        calendarButton = new JButton("Haftalık Takvim");
        calendarButton.setBackground(new Color(0, 153, 0));
        calendarButton.setForeground(Color.WHITE);
        calendarButton.setOpaque(true);
        calendarButton.setBorderPainted(false);
        calendarButton.addActionListener(e -> new StudentCalendarFrame(studentId));
        panel.add(calendarButton, gbc);

        // Randevu Listesi
        gbc.gridy = 6;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        appointmentList = new JTextArea();
        appointmentList.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(appointmentList);
        panel.add(scrollPane, gbc);

        // Randevu İptal Butonu
        gbc.gridy = 7;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        cancelButton = new JButton("Seçili Randevuyu İptal Et");
        cancelButton.setBackground(Color.RED);
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setOpaque(true);
        cancelButton.setBorderPainted(false);
        cancelButton.addActionListener(this::cancelAppointment);
        panel.add(cancelButton, gbc);

        loadAppointments();
        add(panel);
        setVisible(true);
    }

    private void loadProfessors() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name FROM professors")) {
            while (rs.next()) {
                professorCombo.addItem(rs.getInt("id") + " - " + rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createAppointment(ActionEvent e) {
        if (professorCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Lütfen bir öğretim üyesi seçin.");
            return;
        }

        String profData = professorCombo.getSelectedItem().toString();
        int professorId = Integer.parseInt(profData.split(" - ")[0]);

        java.util.Date selectedDate = (java.util.Date) datePicker.getModel().getValue();
        if (selectedDate == null) {
            JOptionPane.showMessageDialog(this, "Lütfen bir tarih seçin.");
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String appointmentDate = sdf.format(selectedDate);
        String timeSlot = (String) timeSlotCombo.getSelectedItem();

        try (Connection conn = DBConnection.getConnection()) {
            if (isAppointmentExists(conn, professorId, appointmentDate, timeSlot)) {
                JOptionPane.showMessageDialog(this, "Bu saat için zaten bir randevu var.");
                return;
            }

            PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO appointments (student_id, professor_id, appointment_date, time_slot, status) VALUES (?, ?, ?, ?, ?)");
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, professorId);
            pstmt.setString(3, appointmentDate);
            pstmt.setString(4, timeSlot);
            pstmt.setString(5, "pending");
            pstmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "Randevu talebi gönderildi.");
            loadAppointments();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private boolean isAppointmentExists(Connection conn, int professorId, String date, String timeSlot) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement(
                "SELECT * FROM appointments WHERE professor_id = ? AND appointment_date = ? AND time_slot = ? AND status != 'rejected' AND status != 'cancelled'");
        pstmt.setInt(1, professorId);
        pstmt.setString(2, date);
        pstmt.setString(3, timeSlot);
        ResultSet rs = pstmt.executeQuery();
        return rs.next();
    }

    private void loadAppointments() {
        appointmentList.setText("");
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT a.id, p.name AS professor_name, a.appointment_date, a.time_slot, a.status " +
                             "FROM appointments a JOIN professors p ON a.professor_id = p.id " +
                             "WHERE a.student_id = ? AND a.status != 'cancelled'")) {
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String status = formatStatus(rs.getString("status"));
                appointmentList.append("ID: " + rs.getInt("id") + ", Öğretim Üyesi: " +
                        rs.getString("professor_name") + ", Tarih: " +
                        rs.getString("appointment_date") + ", Saat: " +
                        rs.getString("time_slot") + ", Durum: " + status + "\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String formatStatus(String status) {
        return switch (status) {
            case "pending" -> "Bekliyor";
            case "approved" -> "Onaylandı";
            case "rejected" -> "Reddedildi";
            case "cancelled" -> "İptal Edildi";
            default -> status;
        };
    }

    private void cancelAppointment(ActionEvent e) {
        String input = JOptionPane.showInputDialog(this, "İptal etmek istediğiniz randevunun ID'sini girin:");
        if (input == null || input.trim().isEmpty()) return;

        try {
            int appointmentId = Integer.parseInt(input.trim());

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "UPDATE appointments SET status = 'cancelled' WHERE id = ? AND student_id = ?")) {
                pstmt.setInt(1, appointmentId);
                pstmt.setInt(2, studentId);
                int affected = pstmt.executeUpdate();

                if (affected > 0) {
                    JOptionPane.showMessageDialog(this, "Randevu iptal edildi.");
                    loadAppointments();
                } else {
                    JOptionPane.showMessageDialog(this, "Belirtilen ID ile size ait bir randevu bulunamadı.");
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Geçerli bir sayı girin.");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // Tarih formatlayıcı iç sınıf
    private static class DateLabelFormatter extends JFormattedTextField.AbstractFormatter {
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        @Override
        public Object stringToValue(String text) throws ParseException {
            return dateFormat.parseObject(text);
        }

        @Override
        public String valueToString(Object value) throws ParseException {
            if (value != null) {
                return dateFormat.format(((java.util.Calendar) value).getTime());
            }
            return "";
            
        }
        
    }
    
}
