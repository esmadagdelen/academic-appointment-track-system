package ui;

import util.DBConnection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Properties;

import org.jdatepicker.impl.*;

public class ProfessorPanel extends JFrame {

    private int professorId;
    private JDatePickerImpl datePicker;
    private JComboBox<String> timeSlotCombo;
    private JButton addAvailabilityButton;
    private JButton viewRequestsButton;  // <<-- Burada tanımladık

    public ProfessorPanel(int professorId) {
        this.professorId = professorId;

        setTitle("Öğretim Üyesi Paneli");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(240, 248, 255));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Tarih seçimi
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Tarih Seç:"), gbc);

        gbc.gridx = 1;
        UtilDateModel model = new UtilDateModel();
        Properties p = new Properties();
        p.put("text.today", "Bugün");
        p.put("text.month", "Ay");
        p.put("text.year", "Yıl");
        JDatePanelImpl datePanel = new JDatePanelImpl(model, p);
        datePicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());
        panel.add(datePicker, gbc);

        // Saat seçimi
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Saat Seç:"), gbc);

        gbc.gridx = 1;
        timeSlotCombo = new JComboBox<>(new String[] {
                "09:00", "09:20",
                "11:00", "11:20",
                "13:00", "13:20", "13:40"
        });
        panel.add(timeSlotCombo, gbc);

        // Ekleme Butonu
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        addAvailabilityButton = new JButton("Müsait Saat Ekle");
        addAvailabilityButton.addActionListener(this::addAvailability);
        panel.add(addAvailabilityButton, gbc);

        // Randevu Taleplerini Görüntüleme Butonu
        gbc.gridy = 3;
        viewRequestsButton = new JButton("Randevu Taleplerini Gör");
        viewRequestsButton.addActionListener(e -> new AppointmentRequestFrame(professorId));
        panel.add(viewRequestsButton, gbc);

        add(panel);
        setVisible(true);
    }

    private void addAvailability(ActionEvent e) {
        java.util.Date selectedDate = (java.util.Date) datePicker.getModel().getValue();
        if (selectedDate == null) {
            JOptionPane.showMessageDialog(this, "Lütfen bir tarih seçiniz.");
            return;
        }

        String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(selectedDate);
        String timeSlot = (String) timeSlotCombo.getSelectedItem();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT OR IGNORE INTO availabilities (professor_id, available_date, time_slot) VALUES (?, ?, ?)")) {

            pstmt.setInt(1, professorId);
            pstmt.setString(2, dateStr);
            pstmt.setString(3, timeSlot);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                JOptionPane.showMessageDialog(this, "Müsaitlik eklendi.");
            } else {
                JOptionPane.showMessageDialog(this, "Bu saat zaten tanımlı.");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Veritabanı hatası oluştu.");
        }
    }
}
