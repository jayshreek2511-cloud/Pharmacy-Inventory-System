import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:pharmacy.db";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found. Add sqlite-jdbc-3.42.0.0.jar to the classpath.", e);
        }
        return DriverManager.getConnection(DB_URL);
    }

    public static void initializeDB() {
        String createTable = "CREATE TABLE IF NOT EXISTS medicines (" +
                "id INTEGER PRIMARY KEY," +
                "name TEXT NOT NULL," +
                "category TEXT," +
                "stock INTEGER," +
                "price REAL," +
                "expiry_date TEXT," +
                "status TEXT," +
                "company TEXT" +
                ")";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTable);
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM medicines")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    insertDefaultMedicines(conn);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to initialize database", e);
        }
    }

    public static List<Object[]> getAllMedicines() {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT id, name, category, stock, price, expiry_date, status, company FROM medicines ORDER BY id";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new Object[]{
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getInt("stock"),
                        String.format("%.2f", rs.getDouble("price")),
                        rs.getString("expiry_date"),
                        rs.getString("status"),
                        ""
                });
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to load medicines", e);
        }
        return rows;
    }

    public static void addMedicine(Object[] row) {
        String sql = "INSERT INTO medicines (id, name, category, stock, price, expiry_date, status, company) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindMedicine(ps, row);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to add medicine", e);
        }
    }

    public static void updateMedicine(Object[] row) {
        String sql = row.length > 7
                ? "UPDATE medicines SET name = ?, category = ?, stock = ?, price = ?, expiry_date = ?, status = ?, company = ? WHERE id = ?"
                : "UPDATE medicines SET name = ?, category = ?, stock = ?, price = ?, expiry_date = ?, status = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, String.valueOf(row[1]));
            ps.setString(2, String.valueOf(row[2]));
            ps.setInt(3, Integer.parseInt(String.valueOf(row[3])));
            ps.setDouble(4, Double.parseDouble(String.valueOf(row[4])));
            ps.setString(5, String.valueOf(row[5]));
            ps.setString(6, String.valueOf(row[6]));
            if (row.length > 7) {
                ps.setString(7, String.valueOf(row[7]));
                ps.setInt(8, Integer.parseInt(String.valueOf(row[0])));
            } else {
                ps.setInt(7, Integer.parseInt(String.valueOf(row[0])));
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to update medicine", e);
        }
    }

    public static void deleteMedicine(int id) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM medicines WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to delete medicine", e);
        }
    }

    private static void insertDefaultMedicines(Connection conn) throws SQLException {
        Object[][] defaults = {
                {1001, "Dolo 650", "Tablet", 28, "35.00", "Jan 2027", "In Stock", "Default"},
                {1002, "Paracetamol", "Tablet", 50, "20.00", "Dec 2026", "In Stock", "Default"},
                {1003, "Amoxicillin 500mg", "Capsule", 15, "60.00", "Aug 2026", "Low Stock", "Default"},
                {1004, "Azithromycin 250", "Tablet", 9, "45.00", "Jul 2026", "Low Stock", "Default"},
                {1005, "Cetrizine 10mg", "Tablet", 2, "18.00", "Jun 2026", "Critical", "Default"},
                {1006, "Ibuprofen 400mg", "Tablet", 35, "30.00", "Mar 2027", "In Stock", "Default"},
                {1007, "Metformin 500mg", "Tablet", 60, "25.00", "Feb 2027", "In Stock", "Default"},
                {1008, "Omeprazole 20mg", "Capsule", 5, "45.00", "Sep 2026", "Critical", "Default"},
                {1009, "Atorvastatin 10mg", "Tablet", 22, "55.00", "Nov 2026", "In Stock", "Default"},
                {1010, "Pantoprazole 40mg", "Capsule", 8, "40.00", "Oct 2026", "Low Stock", "Default"}
        };
        for (Object[] row : defaults) {
            addMedicine(conn, row);
        }
    }

    private static void addMedicine(Connection conn, Object[] row) throws SQLException {
        String sql = "INSERT INTO medicines (id, name, category, stock, price, expiry_date, status, company) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindMedicine(ps, row);
            ps.executeUpdate();
        }
    }

    private static void bindMedicine(PreparedStatement ps, Object[] row) throws SQLException {
        ps.setInt(1, Integer.parseInt(String.valueOf(row[0])));
        ps.setString(2, String.valueOf(row[1]));
        ps.setString(3, String.valueOf(row[2]));
        ps.setInt(4, Integer.parseInt(String.valueOf(row[3])));
        ps.setDouble(5, Double.parseDouble(String.valueOf(row[4])));
        ps.setString(6, String.valueOf(row[5]));
        ps.setString(7, String.valueOf(row[6]));
        ps.setString(8, row.length > 7 ? String.valueOf(row[7]) : "");
    }
}
