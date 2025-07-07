package util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DBInitializer {

    public static void createTables() {
        String createStudentsTable = "CREATE TABLE IF NOT EXISTS students (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "email TEXT UNIQUE NOT NULL," +
                "password TEXT NOT NULL" +
                ");";

        String createProfessorsTable = "CREATE TABLE IF NOT EXISTS professors (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "email TEXT UNIQUE NOT NULL," +
                "password TEXT NOT NULL" +
                ");";

        String createAppointmentsTable = "CREATE TABLE IF NOT EXISTS appointments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "student_id INTEGER NOT NULL," +
                "professor_id INTEGER NOT NULL," +
                "appointment_date TEXT NOT NULL," +
                "time_slot TEXT NOT NULL," +
                "status TEXT NOT NULL," +  // pending, approved, rejected
                "FOREIGN KEY(student_id) REFERENCES students(id)," +
                "FOREIGN KEY(professor_id) REFERENCES professors(id)" +
                ");";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createStudentsTable);
            stmt.execute(createProfessorsTable);
            stmt.execute(createAppointmentsTable);

            System.out.println("Tablolar başarıyla oluşturuldu veya zaten mevcut.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static void insertSampleUsers() {
        String insertStudent = "INSERT OR IGNORE INTO students (name, email, password) VALUES ('Ali Veli', 'ali@example.com', '12345');";
        String insertProfessor = "INSERT OR IGNORE INTO professors (name, email, password) VALUES ('Dr. Ayşe', 'ayse@example.com', '54321');";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(insertStudent);
            stmt.execute(insertProfessor);

            System.out.println("Örnek kullanıcılar eklendi veya zaten mevcut.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
