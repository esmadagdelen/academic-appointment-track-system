package ui;

import util.DBConnection;
import org.jdatepicker.impl.*;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

public class NewTimeSuggestionDialog extends JDialog {

    private int appointmentId;
    private JDatePickerImpl datePicker;
    private JComboBox<String> timeSlotCombo;
    private JButton sendSuggestionButton;

    public NewTimeSuggestionDialog(JFrame parent, int appointmentId) {
        super(parent, "Yeni Saat Öner", true);
        this.appointmentId = appointmentId;

        setSize(300, 200);
        setLocationRelativeTo(parent);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("Yeni Tarih:"), gbc);

        gbc.gridx = 1;
        UtilDateModel model = new UtilDateModel();
        Properties p = new Properties();
        p.put("text.today", "Bugün");
        p.put("text.month", "Ay");
        p.put("text.year", "Yıl");
        JDatePanelImpl datePanel = new JDatePanelImpl(model, p);
        datePicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());
        add(datePicker, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("Yeni Saat:"), gbc);

        gbc.gridx = 1;
        timeSlotCombo = new JComboBox<>(new String[] {
            "09:00", "09:20", "09:40", "10:00", "10:20",
            "11:00", "11:20", "11:40",
            "13:00", "13:20", "13:40", "14:00", "14:20"
        });
        add(timeSlotCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        sendSuggestionButton = new JButton("Öneriyi Gönder");
        sendSuggestionButton.addActionListener(e -> sendSuggestion());
        add(sendSuggestionButton, gbc);
    }

    private void sendSuggestion() {
        java.util.Date selectedDate = (java.util.Date) datePicker.getModel().getValue();
        if (selectedDate == null) {
            JOptionPane.showMessageDialog(this, "Lütfen bir tarih seçin.");
            return;
        }

        String dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd").format(selectedDate);
        String timeStr = (String) timeSlotCombo.getSelectedItem();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "UPDATE appointments SET status = ?, suggested_date = ?, suggested_time_slot = ? WHERE id = ?")) {

            pstmt.setString(1, "suggested");  // öneri durumu
            pstmt.setString(2, dateStr);
            pstmt.setString(3, timeStr);
            pstmt.setInt(4, appointmentId);

            int updated = pstmt.executeUpdate();
            if (updated > 0) {
                JOptionPane.showMessageDialog(this, "Yeni saat önerisi gönderildi.");
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Güncelleme başarısız.");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Veritabanı hatası.");
        }
    }
}
