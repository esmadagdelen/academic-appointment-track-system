package ui;

import util.DBConnection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.util.Properties;
import org.jdatepicker.impl.*;

//Tek bir randevu oluşturma ekranı (randevu ekleme için)
public class StudentAppointmentFrame extends JFrame {

    private JComboBox<String> professorComboBox;
    private JComboBox<String> timeSlotComboBox;
    private JButton createAppointmentButton;

    private int studentId;

    private JDatePickerImpl datePicker;

    public StudentAppointmentFrame(int studentId) {
        this.studentId = studentId;

        setTitle("Randevu Oluştur");
        setSize(500, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 12, 12, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Hoca seçimi
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Hoca Seçin:"), gbc);

        gbc.gridx = 1;
        professorComboBox = new JComboBox<>();
        loadProfessors();
        panel.add(professorComboBox, gbc);

        // Tarih seçici
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Tarih Seçin:"), gbc);

        gbc.gridx = 1;
        datePicker = createDatePicker();
        panel.add(datePicker, gbc);

        // Saat seçimi
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Saat Seçin:"), gbc);

        gbc.gridx = 1;
        timeSlotComboBox = new JComboBox<>(new String[]{
                "09:00", "09:20", "09:40", "10:00", "10:20", "10:40",
                "11:00", "11:20", "11:40", "13:00", "13:20", "13:40",
                "14:00", "14:20", "14:40", "15:00"
        });
        panel.add(timeSlotComboBox, gbc);

        // Oluştur butonu
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        createAppointmentButton = new JButton("Randevu Oluştur");
        panel.add(createAppointmentButton, gbc);

        createAppointmentButton.addActionListener((ActionEvent e) -> {
            String professorName = (String) professorComboBox.getSelectedItem();

            // Seçilen tarihi al
            java.util.Date selectedDate = (java.util.Date) datePicker.getModel().getValue();
            if (selectedDate == null) {
                JOptionPane.showMessageDialog(this, "Lütfen bir tarih seçin.");
                return;
            }
            // Tarihi String formatına çevir (YYYY-MM-DD)
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            String dateStr = sdf.format(selectedDate);

            String timeSlot = (String) timeSlotComboBox.getSelectedItem();

            createAppointment(professorName, dateStr, timeSlot);
        });

        add(panel);
        setVisible(true);
    }

    private JDatePickerImpl createDatePicker() {
        UtilDateModel model = new UtilDateModel();
        model.setSelected(true);  // Bugünü seçili yapabiliriz
        Properties p = new Properties();
        p.put("text.today", "Bugün");
        p.put("text.month", "Ay");
        p.put("text.year", "Yıl");
        JDatePanelImpl datePanel = new JDatePanelImpl(model, p);
        return new JDatePickerImpl(datePanel, new DateLabelFormatter());
    }

    private void loadProfessors() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM professors")) {

            while (rs.next()) {
                professorComboBox.addItem(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createAppointment(String professorName, String date, String timeSlot) {
        try (Connection conn = DBConnection.getConnection()) {
            int professorId = getUserIdByName(conn, "professors", professorName);
            if (professorId == -1) {
                JOptionPane.showMessageDialog(this, "Geçersiz hoca bilgisi!");
                return;
            }

            if (!isSlotAvailable(conn, professorId, date, timeSlot)) {
                JOptionPane.showMessageDialog(this, "Seçilen tarih ve saat dolu. Lütfen başka bir zaman seçin.");
                return;
            }

            String insertSql = "INSERT INTO appointments (student_id, professor_id, appointment_date, time_slot, status) VALUES (?, ?, ?, ?, 'pending')";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setInt(1, studentId);
                pstmt.setInt(2, professorId);
                pstmt.setString(3, date);
                pstmt.setString(4, timeSlot);
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Randevu talebiniz oluşturuldu.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int getUserIdByName(Connection conn, String table, String name) throws SQLException {
        String sql = "SELECT id FROM " + table + " WHERE name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt("id");
        }
        return -1;
    }

    private boolean isSlotAvailable(Connection conn, int professorId, String date, String timeSlot) throws SQLException {
        String sql = "SELECT COUNT(*) FROM appointments WHERE professor_id = ? AND appointment_date = ? AND time_slot = ? AND status = 'pending'";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, professorId);
            pstmt.setString(2, date);
            pstmt.setString(3, timeSlot);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) == 0;
            }
        }
        return false;
    }

    // Tarih formatlayıcı için iç sınıf
    class DateLabelFormatter extends JFormattedTextField.AbstractFormatter {
        private final java.text.SimpleDateFormat dateFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd");

        @Override
        public Object stringToValue(String text) throws java.text.ParseException {
            return dateFormatter.parseObject(text);
        }

        @Override
        public String valueToString(Object value) throws java.text.ParseException {
            if (value != null) {
                java.util.Calendar cal = (java.util.Calendar) value;
                return dateFormatter.format(cal.getTime());
            }
            return "";
        }
    }
}
