import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:pharmacy.db";

    public static class BillSummary {
        public final int id;
        public final String customer;
        public final String payment;
        public final double subtotal;
        public final double tax;
        public final double total;
        public final String billDate;

        public BillSummary(int id, String customer, String payment, double subtotal, double tax, double total, String billDate) {
            this.id = id;
            this.customer = customer;
            this.payment = payment;
            this.subtotal = subtotal;
            this.tax = tax;
            this.total = total;
            this.billDate = billDate;
        }
    }

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
            stmt.execute("CREATE TABLE IF NOT EXISTS bills (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "customer TEXT," +
                    "mobile TEXT," +
                    "payment TEXT," +
                    "subtotal REAL," +
                    "tax REAL," +
                    "total REAL," +
                    "bill_date TEXT DEFAULT CURRENT_TIMESTAMP" +
                    ")");
            stmt.execute("CREATE TABLE IF NOT EXISTS bill_items (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "bill_id INTEGER," +
                    "medicine_id INTEGER," +
                    "medicine_name TEXT," +
                    "qty INTEGER," +
                    "price REAL," +
                    "amount REAL," +
                    "FOREIGN KEY (bill_id) REFERENCES bills(id)" +
                    ")");
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM medicines")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    insertDefaultMedicines(conn);
                } else {
                    updateDefaultMedicineMix(conn);
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

    public static int saveBill(String customer, String mobile, String payment, double subtotal, double tax, double total, List<Object[]> items) {
        String billSql = "INSERT INTO bills (customer, mobile, payment, subtotal, tax, total) VALUES (?, ?, ?, ?, ?, ?)";
        String itemSql = "INSERT INTO bill_items (bill_id, medicine_id, medicine_name, qty, price, amount) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement billPs = conn.prepareStatement(billSql, Statement.RETURN_GENERATED_KEYS)) {
                billPs.setString(1, customer);
                billPs.setString(2, mobile);
                billPs.setString(3, payment);
                billPs.setDouble(4, subtotal);
                billPs.setDouble(5, tax);
                billPs.setDouble(6, total);
                billPs.executeUpdate();
                int billId;
                try (ResultSet keys = billPs.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Bill id was not generated");
                    billId = keys.getInt(1);
                }
                try (PreparedStatement itemPs = conn.prepareStatement(itemSql)) {
                    for (Object[] item : items) {
                        itemPs.setInt(1, billId);
                        itemPs.setInt(2, Integer.parseInt(String.valueOf(item[0])));
                        itemPs.setString(3, String.valueOf(item[1]));
                        itemPs.setInt(4, Integer.parseInt(String.valueOf(item[2])));
                        itemPs.setDouble(5, Double.parseDouble(String.valueOf(item[3])));
                        itemPs.setDouble(6, Double.parseDouble(String.valueOf(item[4])));
                        itemPs.addBatch();
                    }
                    itemPs.executeBatch();
                }
                conn.commit();
                return billId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to save bill", e);
        }
    }

    public static double getTotalRevenue() {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(total), 0) FROM bills");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to load revenue", e);
        }
    }

    public static int getBillCount() {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM bills");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to load bill count", e);
        }
    }

    public static List<BillSummary> getBillHistory() {
        List<BillSummary> rows = new ArrayList<>();
        String sql = "SELECT id, customer, payment, subtotal, tax, total, bill_date FROM bills ORDER BY bill_date DESC, id DESC";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new BillSummary(
                        rs.getInt("id"),
                        rs.getString("customer"),
                        rs.getString("payment"),
                        rs.getDouble("subtotal"),
                        rs.getDouble("tax"),
                        rs.getDouble("total"),
                        rs.getString("bill_date")
                ));
            }
            return rows;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to load bill history", e);
        }
    }

    public static String getTopSellingMedicine() {
        String sql = "SELECT medicine_name, SUM(qty) AS total_qty FROM bill_items GROUP BY medicine_name ORDER BY total_qty DESC LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getString("medicine_name") + " (" + rs.getInt("total_qty") + ")";
            return "No sales yet";
        } catch (SQLException e) {
            throw new RuntimeException("Unable to load top selling medicine", e);
        }
    }

    public static double getInventoryValue() {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(stock * price), 0) FROM medicines");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to load inventory value", e);
        }
    }

    public static int getLowStockCount() {
        return getCount("SELECT COUNT(*) FROM medicines WHERE status = 'Low Stock'");
    }

    public static int getCriticalStockCount() {
        return getCount("SELECT COUNT(*) FROM medicines WHERE status = 'Critical'");
    }

    public static int getCategoryCount() {
        return getCount("SELECT COUNT(DISTINCT category) FROM medicines");
    }

    private static int getCount(String sql) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to load count", e);
        }
    }

    private static void insertDefaultMedicines(Connection conn) throws SQLException {
        Object[][] defaults = {
                {1001, "Dolo 650", "Tablet", 28, "35.00", "Jan 2027", "In Stock", "Default"},
                {1002, "Paracetamol", "Tablet", 50, "20.00", "Dec 2026", "In Stock", "Default"},
                {1003, "Amoxicillin 500mg", "Capsule", 15, "60.00", "Aug 2026", "Low Stock", "Default"},
                {1004, "Azithromycin 250", "Tablet", 9, "45.00", "Jul 2026", "Low Stock", "Default"},
                {1005, "Benadryl Cough Syrup", "Syrup", 2, "85.00", "Jun 2026", "Critical", "Default"},
                {1006, "Ibuprofen 400mg", "Tablet", 35, "30.00", "Mar 2027", "In Stock", "Default"},
                {1007, "Metformin 500mg", "Tablet", 60, "25.00", "Feb 2027", "In Stock", "Default"},
                {1008, "Vitamin B12 Injection", "Injection", 5, "120.00", "Sep 2026", "Critical", "Default"},
                {1009, "Ambroxol Syrup", "Syrup", 22, "75.00", "Nov 2026", "In Stock", "Default"},
                {1010, "Pantoprazole 40mg", "Capsule", 8, "40.00", "Oct 2026", "Low Stock", "Default"}
        };
        for (Object[] row : defaults) {
            addMedicine(conn, row);
        }
    }

    private static void updateDefaultMedicineMix(Connection conn) throws SQLException {
        Object[][] replacements = {
                {1005, "Benadryl Cough Syrup", "Syrup", 2, "85.00", "Jun 2026", "Critical", "Default"},
                {1008, "Vitamin B12 Injection", "Injection", 5, "120.00", "Sep 2026", "Critical", "Default"},
                {1009, "Ambroxol Syrup", "Syrup", 22, "75.00", "Nov 2026", "In Stock", "Default"}
        };
        String sql = "UPDATE medicines SET name = ?, category = ?, stock = ?, price = ?, expiry_date = ?, status = ?, company = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Object[] row : replacements) {
                ps.setString(1, String.valueOf(row[1]));
                ps.setString(2, String.valueOf(row[2]));
                ps.setInt(3, Integer.parseInt(String.valueOf(row[3])));
                ps.setDouble(4, Double.parseDouble(String.valueOf(row[4])));
                ps.setString(5, String.valueOf(row[5]));
                ps.setString(6, String.valueOf(row[6]));
                ps.setString(7, String.valueOf(row[7]));
                ps.setInt(8, Integer.parseInt(String.valueOf(row[0])));
                ps.addBatch();
            }
            ps.executeBatch();
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
