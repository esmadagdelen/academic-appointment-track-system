package ui;

import util.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class StudentCalendarFrame extends JFrame {

    private int studentId;
    private JTable calendarTable;
    private DefaultTableModel tableModel;
    private JScrollPane scrollPane;

    private final String[] timeSlots = {
        "09:00", "10:00", "11:00", "12:00",
        "13:00", "14:00", "15:00", "16:00"
    };

    private final int DAYS_TO_SHOW = 5;  // Haftalık görünümde gösterilecek gün sayısı

    // Günlük görünüm için sadece 1 gün gösterilir
    private boolean isWeeklyView = true;

    public StudentCalendarFrame(int studentId) {
        this.studentId = studentId;

        setTitle("Haftalık Takvim");
        setSize(900, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Üst kısma butonlar
        JPanel buttonPanel = new JPanel();
        JButton weeklyViewButton = new JButton("Haftalık Görünüm");
        JButton dailyViewButton = new JButton("Günlük Görünüm");
        buttonPanel.add(weeklyViewButton);
        buttonPanel.add(dailyViewButton);
        add(buttonPanel, BorderLayout.NORTH);

        // İlk tablo haftalık olarak oluşturuluyor
        createWeeklyTable();

        calendarTable = new JTable(tableModel);
        calendarTable.setRowHeight(40);
        scrollPane = new JScrollPane(calendarTable);
        add(scrollPane, BorderLayout.CENTER);

        // Butonlara action listener ekle
        weeklyViewButton.addActionListener(e -> switchToWeeklyView());
        dailyViewButton.addActionListener(e -> switchToDailyView());

        loadAppointmentsWeekly();

        setVisible(true);
    }

    private void createWeeklyTable() {
        LocalDate today = LocalDate.now();

        String[] columnNames = new String[DAYS_TO_SHOW + 1];
        columnNames[0] = "Saat";
        for (int i = 1; i <= DAYS_TO_SHOW; i++) {
            columnNames[i] = today.plusDays(i - 1).format(DateTimeFormatter.ofPattern("dd MMM EEE"));
        }

        tableModel = new DefaultTableModel(columnNames, 0);
        for (String time : timeSlots) {
            Object[] row = new Object[DAYS_TO_SHOW + 1];
            row[0] = time;
            for (int i = 1; i <= DAYS_TO_SHOW; i++) {
                row[i] = "";
            }
            tableModel.addRow(row);
        }

        isWeeklyView = true;
    }

    private void createDailyTable() {
        LocalDate today = LocalDate.now();

        String[] columnNames = new String[2];
        columnNames[0] = "Saat";
        columnNames[1] = today.format(DateTimeFormatter.ofPattern("dd MMM EEE"));

        tableModel = new DefaultTableModel(columnNames, 0);
        for (String time : timeSlots) {
            Object[] row = new Object[2];
            row[0] = time;
            row[1] = "";
            tableModel.addRow(row);
        }

        isWeeklyView = false;
    }

    private void loadAppointmentsWeekly() {
        LocalDate startDate = LocalDate.now();

        String sql = "SELECT appointment_date, time_slot, status FROM appointments " +
                "WHERE student_id = ? AND status IN ('approved', 'pending')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String dateStr = rs.getString("appointment_date");
                String timeSlot = rs.getString("time_slot");
                String status = rs.getString("status");

                LocalDate date = LocalDate.parse(dateStr);
                int dayOffset = (int) (date.toEpochDay() - startDate.toEpochDay());

                if (dayOffset >= 0 && dayOffset < DAYS_TO_SHOW) {
                    int rowIndex = getTimeSlotIndex(timeSlot);
                    if (rowIndex != -1) {
                        tableModel.setValueAt(formatStatus(status), rowIndex, dayOffset + 1);
                    }
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Veritabanı hatası oluştu.");
        }
    }

    private void loadAppointmentsDaily() {
        LocalDate today = LocalDate.now();

        String sql = "SELECT appointment_date, time_slot, status FROM appointments " +
                "WHERE student_id = ? AND status IN ('approved', 'pending')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String dateStr = rs.getString("appointment_date");
                String timeSlot = rs.getString("time_slot");
                String status = rs.getString("status");

                LocalDate date = LocalDate.parse(dateStr);

                if (date.equals(today)) {
                    int rowIndex = getTimeSlotIndex(timeSlot);
                    if (rowIndex != -1) {
                        tableModel.setValueAt(formatStatus(status), rowIndex, 1);
                    }
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Veritabanı hatası oluştu.");
        }
    }

    private void switchToWeeklyView() {
        createWeeklyTable();
        calendarTable.setModel(tableModel);
        loadAppointmentsWeekly();
        this.revalidate();
        this.repaint();
    }

    private void switchToDailyView() {
        createDailyTable();
        calendarTable.setModel(tableModel);
        loadAppointmentsDaily();
        this.revalidate();
        this.repaint();
    }

    private int getTimeSlotIndex(String timeSlot) {
        for (int i = 0; i < timeSlots.length; i++) {
            if (timeSlots[i].equals(timeSlot)) {
                return i;
            }
        }
        return -1;
    }

    // Status string'ini daha okunur yapıyoruz
    private String formatStatus(String status) {
        switch (status.toLowerCase()) {
            case "approved":
                return "Onaylandı";
            case "pending":
                return "Beklemede";
            default:
                return status;
        }
    }
}
