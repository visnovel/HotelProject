package org.example.hotelmanager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.*;

public class ClientDatabaseHandler {
    private static final String DB_URL = "jdbc:sqlite:hotel_base.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static ObservableList<Category> getAvailableCategories(String checkIn, String checkOut) {
        ObservableList<Category> list = FXCollections.observableArrayList();
        String sql = "SELECT DISTINCT c.* FROM categories c " +
                "JOIN rooms r ON c.id = r.category_id " +
                "WHERE r.id NOT IN (SELECT room_id FROM bookings WHERE NOT (check_out <= ? OR check_in >= ?))";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, checkIn);
            ps.setString(2, checkOut);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Category(
                        rs.getInt("id"), rs.getString("name"),
                        rs.getString("description"), rs.getDouble("price_per_night"),
                        rs.getString("image_path")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static void createAutoBooking(int guestId, int categoryId, String in, String out) throws SQLException {
        double spent = 0;
        String spentSql = "SELECT SUM(c.price_per_night * (julianday(b.check_out) - julianday(b.check_in))) " +
                "FROM bookings b JOIN rooms r ON b.room_id = r.id JOIN categories c ON r.category_id = c.id " +
                "WHERE b.guest_id = ? AND b.status = 'Выполнено'";

        try (Connection conn = getConnection()) {
            PreparedStatement psSpent = conn.prepareStatement(spentSql);
            psSpent.setInt(1, guestId);
            ResultSet rsSpent = psSpent.executeQuery();
            if (rsSpent.next()) spent = rsSpent.getDouble(1);

            double multiplier = (spent >= 50000) ? 0.90 : ((spent >= 20000) ? 0.95 : 1.0);

            String roomSql = "SELECT r.id, c.price_per_night FROM rooms r JOIN categories c ON r.category_id = c.id " +
                    "WHERE r.category_id = ? AND r.id NOT IN " +
                    "(SELECT room_id FROM bookings WHERE NOT (check_out <= ? OR check_in >= ?)) LIMIT 1";

            PreparedStatement psRoom = conn.prepareStatement(roomSql);
            psRoom.setInt(1, categoryId);
            psRoom.setString(2, in);
            psRoom.setString(3, out);
            ResultSet rsRoom = psRoom.executeQuery();

            if (rsRoom.next()) {
                int roomId = rsRoom.getInt(1);
                double basePrice = rsRoom.getDouble(2);
                long days = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.parse(in), java.time.LocalDate.parse(out));
                if (days <= 0) days = 1;

                double finalPrice = (basePrice * days) * multiplier;

                String bookSql = "INSERT INTO bookings(guest_id, room_id, check_in, check_out, status, total_price) VALUES(?,?,?,?,'Активна',?)";
                PreparedStatement psBook = conn.prepareStatement(bookSql);
                psBook.setInt(1, guestId);
                psBook.setInt(2, roomId);
                psBook.setString(3, in);
                psBook.setString(4, out);
                psBook.setDouble(5, finalPrice);
                psBook.executeUpdate();
            }
        }
    }

    public static Guest login(String email, String password) {
        String sql = "SELECT g.*, (SELECT SUM(c.price_per_night * (julianday(b.check_out) - julianday(b.check_in))) " +
                "FROM bookings b JOIN rooms r ON b.room_id = r.id JOIN categories c ON r.category_id = c.id " +
                "WHERE b.guest_id = g.id AND b.status = 'Выполнено') as spent " +
                "FROM guests g WHERE email = ? AND password = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Guest(rs.getInt("id"), rs.getString("full_name"),
                        rs.getString("phone"), rs.getString("passport"), rs.getDouble("spent"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public static void registerUser(String name, String phone, String passport, String email, String password) throws SQLException {
        String sql = "INSERT INTO guests(full_name, phone, passport, email, password) VALUES(?,?,?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, passport);
            ps.setString(4, email);
            ps.setString(5, password);
            ps.executeUpdate();
        }
    }

    public static ObservableList<BookingReport> getGuestHistory(String passport) {
        ObservableList<BookingReport> history = FXCollections.observableArrayList();
        String sql = "SELECT b.id, r.room_number, b.check_in, b.check_out, b.status, " +
                "c.price_per_night * (julianday(b.check_out) - julianday(b.check_in)) as total " +
                "FROM bookings b JOIN rooms r ON b.room_id = r.id " +
                "JOIN categories c ON r.category_id = c.id " +
                "JOIN guests g ON b.guest_id = g.id WHERE g.passport = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, passport);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                history.add(new BookingReport(
                        rs.getInt(1), "", rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getDouble("total"), rs.getString(5)
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return history;
    }
}