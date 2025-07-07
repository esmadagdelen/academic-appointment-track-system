package ui;

import util.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import org.jdatepicker.impl.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Properties;

public class AvailabilityFrame extends JFrame {

    private int professorId;
    private JTable table;
    private DefaultTableModel tableModel;
    private JDatePickerImpl datePicker;
    private JComboBox<String> startTimeBox, endTimeBox;

    public AvailabilityFrame(int professorId) {
        this.professorId = professorId;
        setTitle("Müsaitlik Tanımla");
        setSize(500, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        initUI();
        loadAvailabilities();

        setVisible(true);
    }

    private void initUI() {
        JPanel topPanel = new JPanel(new GridLayout(3, 2, 5, 5));

        // Date picker
        UtilDateModel model = new UtilDateModel();
        Properties p = new Properties();
        p.put("text.today", "Bugün");
        JDatePanelImpl datePanel = new JDatePanelImpl(model, p);
        datePicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());

        topPanel.add(new JLabel("Tarih:"));
        topPanel.add(datePicker);

        // Time selection
        String[] times = generateTimeOptions();
        startTimeBox = new JComboBox<>(times);
        endTimeBox = new JComboBox<>(times);
        topPanel.add(new JLabel("Başlangıç Saati:"));
        topPanel.add(startTimeBox);
        topPanel.add(new JLabel("Bitiş Saati:"));
        topPanel.add(endTimeBox);

        add(topPanel, BorderLayout.NORTH);

        JButton addButton = new JButton("Müsaitlik Ekle");
        add(addButton, BorderLayout.CENTER);

        addButton.addActionListener(e -> addAvailability());

        // Table
        tableModel = new DefaultTableModel(new String[]{"Tarih", "Başlangıç", "Bitiş"}, 0);
        table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.SOUTH);
    }

    private String[] generateTimeOptions() {
        String[] times = new String[24];
        for (int i = 0; i < 24; i++) {
            times[i] = String.format("%02d:00", i);
        }
        return times;
    }

    private void addAvailability() {
        java.util.Date selectedDate = (java.util.Date) datePicker.getModel().getValue();
        if (selectedDate == null) {
            JOptionPane.showMessageDialog(this, "Lütfen bir tarih seçin.");
            return;
        }

        String date = new java.text.SimpleDateFormat("yyyy-MM-dd").format(selectedDate);
        String start = (String) startTimeBox.getSelectedItem();
        String end = (String) endTimeBox.getSelectedItem();

        if (start.compareTo(end) >= 0) {
            JOptionPane.showMessageDialog(this, "Bitiş saati, başlangıç saatinden sonra olmalı.");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "INSERT INTO availabilities (professor_id, date, start_time, end_time) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, professorId);
            ps.setString(2, date);
            ps.setString(3, start);
            ps.setString(4, end);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Müsaitlik eklendi.");
            loadAvailabilities();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadAvailabilities() {
        tableModel.setRowCount(0);
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT date, start_time, end_time FROM availabilities WHERE professor_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, professorId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getString("date"),
                        rs.getString("start_time"),
                        rs.getString("end_time")
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
