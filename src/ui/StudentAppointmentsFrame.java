package ui;

import util.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

//Öğrencinin tüm randevularını listeleyen ve iptal etmeye izin veren ekran
public class StudentAppointmentsFrame extends JFrame {

    private int studentId;
    private JTable table;
    private DefaultTableModel tableModel;

    public StudentAppointmentsFrame(int studentId) {
        this.studentId = studentId;

        setTitle("Mevcut Randevularım");
        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Tablo
        tableModel = new DefaultTableModel(new String[]{"ID", "Tarih", "Saat", "Hoca", "Durum"}, 0);
        table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Buton
        JButton deleteButton = new JButton("Seçili Randevuyu İptal Et");
        add(deleteButton, BorderLayout.SOUTH);

        deleteButton.addActionListener(e -> cancelAppointment());

        loadAppointments();

        setVisible(true);
    }

    private void loadAppointments() {
        tableModel.setRowCount(0);

        String sql = """
            SELECT a.id, a.date, a.time, u.fullname, a.status
            FROM appointments a
            JOIN users u ON a.professor_id = u.id
            WHERE a.student_id = ?
            ORDER BY a.date, a.time
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("date"),
                    rs.getString("time"),
                    rs.getString("fullname"),
                    rs.getString("status")
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cancelAppointment() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Lütfen iptal etmek için bir randevu seçin.");
            return;
        }

        int appointmentId = (int) tableModel.getValueAt(selectedRow, 0);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bu randevuyu iptal etmek istediğinize emin misiniz?",
                "Onay", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            String sql = "DELETE FROM appointments WHERE id = ? AND student_id = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, appointmentId);
                ps.setInt(2, studentId);
                ps.executeUpdate();

                JOptionPane.showMessageDialog(this, "Randevu iptal edildi.");
                loadAppointments();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
