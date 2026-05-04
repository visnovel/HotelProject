package org.example.hotelmanager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;

public class AdminDatabaseHandler {
    private static final String DB_URL = "jdbc:sqlite:hotel_base.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static ObservableList<Object> getRoomsList() {
        ObservableList<Object> rooms = FXCollections.observableArrayList();
        String sql = "SELECT r.id, r.room_number, c.name, r.status, c.price_per_night " +
                "FROM rooms r LEFT JOIN categories c ON r.category_id = c.id";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                rooms.add(new Room(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getDouble(5)));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return rooms;
    }

    public static void addRoom(String roomNumber, int categoryId) throws SQLException {
        String sql = "INSERT INTO rooms(room_number, category_id, status) VALUES(?,?, 'Доступен')";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomNumber);
            ps.setInt(2, categoryId);
            ps.executeUpdate();
        }
    }

    public static ObservableList<String> getAllRoomNumbers() {
        ObservableList<String> rooms = FXCollections.observableArrayList();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT room_number FROM rooms ORDER BY room_number")) {
            while (rs.next()) rooms.add(rs.getString(1));
        } catch (Exception e) { e.printStackTrace(); }
        return rooms;
    }

    public static ObservableList<String> getAvailableRooms(String checkIn, String checkOut) {
        ObservableList<String> rooms = FXCollections.observableArrayList();
        String sql = "SELECT room_number FROM rooms WHERE id NOT IN (" +
                "SELECT room_id FROM bookings WHERE NOT (check_out <= ? OR check_in >= ?)) ORDER BY room_number";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, checkIn);
            ps.setString(2, checkOut);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) rooms.add(rs.getString(1));
        } catch (Exception e) { e.printStackTrace(); }
        return rooms;
    }

    public static ObservableList<Object> getGuestsList() {
        ObservableList<Object> guests = FXCollections.observableArrayList();
        String sql = "SELECT g.*, " +
                "(SELECT COUNT(*) FROM bookings WHERE guest_id = g.id) as visits_count, " +
                "(SELECT SUM(c.price_per_night * (julianday(b.check_out) - julianday(b.check_in))) " +
                " FROM bookings b " +
                " JOIN rooms r ON b.room_id = r.id " +
                " JOIN categories c ON r.category_id = c.id " +
                " WHERE b.guest_id = g.id AND b.status = 'Выполнено') as total_ltv " +
                "FROM guests g";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                guests.add(new Guest(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("phone"),
                        rs.getString("passport"),
                        rs.getInt("visits_count"),
                        rs.getDouble("total_ltv")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return guests;
    }

    public static int getOrCreateGuest(String fullName, String passport, String phone) throws SQLException {
        try (Connection conn = getConnection()) {
            PreparedStatement psFind = conn.prepareStatement("SELECT id FROM guests WHERE passport = ?");
            psFind.setString(1, passport);
            ResultSet rs = psFind.executeQuery();
            if (rs.next()) return rs.getInt(1);

            PreparedStatement psInsert = conn.prepareStatement(
                    "INSERT INTO guests(full_name, passport, phone) VALUES(?,?,?)");
            psInsert.setString(1, fullName);
            psInsert.setString(2, passport);
            psInsert.setString(3, phone);
            psInsert.executeUpdate();

            try (Statement st = conn.createStatement();
                 ResultSet keys = st.executeQuery("SELECT last_insert_rowid()")) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    public static java.util.Map<Integer, String> getRoomsMap() {
        java.util.Map<Integer, String> map = new java.util.LinkedHashMap<>();
        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, room_number FROM rooms ORDER BY room_number")) {
            while (rs.next()) map.put(rs.getInt(1), rs.getString(2));
        } catch (SQLException e) { e.printStackTrace(); }
        return map;
    }

    public static ObservableList<String[]> getChessboardBookings() {
        ObservableList<String[]> list = FXCollections.observableArrayList();
        String sql = "SELECT b.check_in, b.check_out, b.room_id, g.full_name " +
                "FROM bookings b JOIN guests g ON b.guest_id = g.id";
        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new String[]{rs.getString(1), rs.getString(2), String.valueOf(rs.getInt(3)), rs.getString(4)});
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static boolean isRoomBooked(String roomNumber, String checkIn, String checkOut) throws SQLException {
        String sql = "SELECT COUNT(*) FROM bookings b JOIN rooms r ON b.room_id = r.id " +
                "WHERE r.room_number = ? AND NOT (b.check_out <= ? OR b.check_in >= ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomNumber);
            ps.setString(2, checkIn);
            ps.setString(3, checkOut);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    public static void createBooking(int guestId, String roomNumber, String checkIn, String checkOut) throws SQLException {
        String sql = "INSERT INTO bookings(guest_id, room_id, check_in, check_out, status) " +
                "VALUES(?, (SELECT id FROM rooms WHERE room_number=?), ?, ?, 'Активна')";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, guestId);
            ps.setString(2, roomNumber);
            ps.setString(3, checkIn);
            ps.setString(4, checkOut);
            ps.executeUpdate();
        }
    }

    public static void deleteRecord(String table, int id) throws SQLException {
        String sql = "DELETE FROM " + table + " WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public static ObservableList<Object> getReportsList() {
        return fetchReportsData("SELECT b.id, g.full_name, r.room_number, b.check_in, b.check_out, c.price_per_night, b.status, " +
                "(SELECT COUNT(*) FROM bookings WHERE guest_id = g.id) as visits " +
                "FROM bookings b JOIN guests g ON b.guest_id = g.id JOIN rooms r ON b.room_id = r.id JOIN categories c ON r.category_id = c.id", null);
    }

    public static ObservableList<Object> getGuestReportsList(int guestId) {
        return fetchReportsData("SELECT b.id, g.full_name, r.room_number, b.check_in, b.check_out, c.price_per_night, b.status, " +
                "(SELECT COUNT(*) FROM bookings WHERE guest_id = g.id) as visits " +
                "FROM bookings b JOIN guests g ON b.guest_id = g.id JOIN rooms r ON b.room_id = r.id JOIN categories c ON r.category_id = c.id " +
                "WHERE g.id = ?", guestId);
    }

    private static ObservableList<Object> fetchReportsData(String sql, Integer guestId) {
        ObservableList<Object> reports = FXCollections.observableArrayList();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (guestId != null) ps.setInt(1, guestId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String guestName = rs.getString("full_name");
                String room = rs.getString("room_number");
                String checkIn = rs.getString("check_in");
                String checkOut = rs.getString("check_out");
                double basePrice = rs.getDouble("price_per_night");
                String status = rs.getString("status");
                int visits = rs.getInt("visits");

                long days = Math.abs(java.time.temporal.ChronoUnit.DAYS.between(
                        java.time.LocalDate.parse(checkIn),
                        java.time.LocalDate.parse(checkOut)));
                if (days == 0) days = 1;

                double discountMultiplier = (visits > 5) ? 0.90 : ((visits > 2) ? 0.95 : 1.0);
                double totalPrice = (basePrice * days) * discountMultiplier;

                reports.add(new BookingReport(id, guestName, room, checkIn, checkOut, totalPrice, status));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return reports;
    }

    public static void updateBookingStatus(int bookingId, String newStatus) throws SQLException {
        String sql = "UPDATE bookings SET status = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, bookingId);
            ps.executeUpdate();
        }
    }

    public static java.util.Map<String, Double> getRevenueData() {
        java.util.Map<String, Double> data = new java.util.TreeMap<>();
        String sql = "SELECT b.check_out, c.price_per_night, " +
                "(SELECT COUNT(*) FROM bookings WHERE guest_id = b.guest_id) as visits, " +
                "julianday(b.check_out) - julianday(b.check_in) as days " +
                "FROM bookings b " +
                "JOIN rooms r ON b.room_id = r.id " +
                "JOIN categories c ON r.category_id = c.id " +
                "WHERE b.status = 'Выполнено' OR b.status = 'Подтверждение оплаты'";

        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String date = rs.getString("check_out");
                double basePrice = rs.getDouble("price_per_night");
                double days = rs.getDouble("days");
                if (days <= 0) days = 1;
                int visits = rs.getInt("visits");

                double discountMultiplier = (visits > 5) ? 0.90 : ((visits > 2) ? 0.95 : 1.0);
                double totalAmount = (basePrice * days) * discountMultiplier;

                data.put(date, data.getOrDefault(date, 0.0) + totalAmount);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return data;
    }

    public static double getTotalGuestSpent(int guestId) {
        String sql = "SELECT SUM(c.price_per_night * (julianday(b.check_out) - julianday(b.check_in))) " +
                "FROM bookings b JOIN rooms r ON b.room_id = r.id " +
                "JOIN categories c ON r.category_id = c.id " +
                "WHERE b.guest_id = ? AND b.status = 'Выполнено'";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, guestId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public static double getDiscountMultiplier(double totalSpent) {
        if (totalSpent >= 50000) return 0.90;
        if (totalSpent >= 20000) return 0.95;
        return 1.0;
    }

    public static void completeBooking(int bookingId, int roomId) throws SQLException {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sql1 = "UPDATE bookings SET status = 'Выполнено' WHERE id = ?";
                PreparedStatement ps1 = conn.prepareStatement(sql1);
                ps1.setInt(1, bookingId);
                ps1.executeUpdate();

                String sql2 = "UPDATE rooms SET status = 'Уборка' WHERE id = ?";
                PreparedStatement ps2 = conn.prepareStatement(sql2);
                ps2.setInt(1, roomId);
                ps2.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public static int getRoomIdByNumber(String roomNumber) throws SQLException {
        String sql = "SELECT id FROM rooms WHERE room_number = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomNumber);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : -1;
        }
    }
}