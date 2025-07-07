package ui;

import util.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;

public class SuggestedAppointmentsFrame extends JFrame {

    private int studentId;
    private JTable table;
    private DefaultTableModel tableModel;

    public SuggestedAppointmentsFrame(int studentId) {
        this.studentId = studentId;

        setTitle("Önerilen Randevular");
        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        tableModel = new DefaultTableModel(new String[]{
            "Randevu ID", "Öğretim Üyesi", "Önerilen Tarih", "Önerilen Saat", "Durum"
        }, 0);

        table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton approveBtn = new JButton("Öneriyi Onayla");
        JButton rejectBtn = new JButton("Öneriyi Reddet");

        approveBtn.addActionListener(this::approveSuggestion);
        rejectBtn.addActionListener(this::rejectSuggestion);

        buttonPanel.add(approveBtn);
        buttonPanel.add(rejectBtn);
        add(buttonPanel, BorderLayout.SOUTH);

        loadSuggestedAppointments();
        setVisible(true);
    }

    private void loadSuggestedAppointments() {
        tableModel.setRowCount(0);
        String sql = "SELECT a.id, p.name AS professor_name, a.suggested_date, a.suggested_time_slot, a.status " +
                     "FROM appointments a " +
                     "JOIN professors p ON a.professor_id = p.id " +
                     "WHERE a.student_id = ? AND a.status = 'suggested'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                tableModel.addRow(new Object[] {
                    rs.getInt("id"),
                    rs.getString("professor_name"),
                    rs.getString("suggested_date"),
                    rs.getString("suggested_time_slot"),
                    rs.getString("status")
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Veritabanı hatası oluştu.");
        }
    }

    private void approveSuggestion(ActionEvent e) {
        changeSuggestionStatus("approved");
    }

    private void rejectSuggestion(ActionEvent e) {
        changeSuggestionStatus("rejected");
    }

    private void changeSuggestionStatus(String newStatus) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Lütfen bir öneri seçiniz.");
            return;
        }

        int appointmentId = (int) tableModel.getValueAt(selectedRow, 0);

        String sql = "UPDATE appointments SET " +
                     "appointment_date = suggested_date, " +
                     "time_slot = suggested_time_slot, " +
                     "status = ?," +
                     "suggested_date = NULL, " +
                     "suggested_time_slot = NULL " +
                     "WHERE id = ?";

        // Eğer reddederse, sadece status = 'rejected' yapabiliriz (ve öneriyi temizleyebiliriz)
        if (newStatus.equals("rejected")) {
            sql = "UPDATE appointments SET status = ?, suggested_date = NULL, suggested_time_slot = NULL WHERE id = ?";
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus);
            pstmt.setInt(2, appointmentId);
            pstmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "İşlem başarılı.");
            loadSuggestedAppointments();

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Veritabanı hatası oluştu.");
        }
    }
}
