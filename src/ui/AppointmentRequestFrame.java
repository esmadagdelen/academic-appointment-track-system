package ui;

import util.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;

public class AppointmentRequestFrame extends JFrame {

    private int professorId;
    private JTable appointmentTable;
    private DefaultTableModel tableModel;

    private JButton suggestNewTimeButton;  // Yeni saat önerme butonu

    public AppointmentRequestFrame(int professorId) {
        this.professorId = professorId;

        setTitle("Randevu Talepleri");
        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        tableModel = new DefaultTableModel(new String[]{"ID", "Öğrenci ID", "Tarih", "Saat", "Durum"}, 0);
        appointmentTable = new JTable(tableModel);

        JScrollPane scrollPane = new JScrollPane(appointmentTable);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton approveButton = new JButton("Onayla");
        JButton rejectButton = new JButton("Reddet");
        suggestNewTimeButton = new JButton("Yeni Saat Öner");  // Yeni buton oluşturuldu

        approveButton.addActionListener(this::approveAppointment);
        rejectButton.addActionListener(this::rejectAppointment);
        suggestNewTimeButton.addActionListener(this::suggestNewTime);  // Butonun aksiyonu

        buttonPanel.add(approveButton);
        buttonPanel.add(rejectButton);
        buttonPanel.add(suggestNewTimeButton);  // Butonu panele ekle

        add(buttonPanel, BorderLayout.SOUTH);

        loadAppointments();
        setVisible(true);
    }

    private void loadAppointments() {
        tableModel.setRowCount(0);
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT a.id, s.name AS student_name, a.appointment_date, a.time_slot, a.status " +
                     "FROM appointments a " +
                     "JOIN students s ON a.student_id = s.id " +
                     "WHERE a.professor_id = ? AND a.status IN ('pending', 'suggested')")) {

            pstmt.setInt(1, professorId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("student_name"),
                        rs.getString("appointment_date"),
                        rs.getString("time_slot"),
                        rs.getString("status")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Randevular yüklenirken hata oluştu: " + e.getMessage());
        }
    }


    private void approveAppointment(ActionEvent e) {
        changeStatus("Onaylandı");
    }

    private void rejectAppointment(ActionEvent e) {
        changeStatus("Reddedildi");
    }

    private void changeStatus(String newStatus) {
        int selectedRow = appointmentTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Lütfen bir randevu seçin.");
            return;
        }

        int appointmentId = (int) tableModel.getValueAt(selectedRow, 0);
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE appointments SET status = ?, suggested_date = NULL, suggested_time_slot = NULL WHERE id = ?")) {
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, appointmentId);
            pstmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "Durum güncellendi.");
            loadAppointments();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void suggestNewTime(ActionEvent e) {
        int selectedRow = appointmentTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Lütfen bir randevu seçin.");
            return;
        }

        int appointmentId = (int) tableModel.getValueAt(selectedRow, 0);
        NewTimeSuggestionDialog dialog = new NewTimeSuggestionDialog(this, appointmentId);
        dialog.setVisible(true);
        loadAppointments();  // dialog kapandıktan sonra tabloyu yenile
    }
}
