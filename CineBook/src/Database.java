import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DBMS layer (SQLite via JDBC). All SQL lives here, so the OOP classes stay clean.
 *
 * Tables:
 *   shows        (show_id PK, movie, show_time)
 *   bookings     (booking_id PK, show_id FK, seat_list, total_amount, coupon, status, refund_amount, booked_at)
 *   booked_seats (show_id + seat_code = composite PK, booking_id FK, seat_type, price)
 *
 * The composite PK on booked_seats means the DATABASE itself refuses a double booking.
 */
public class Database implements AutoCloseable {
    private final Connection conn;

    public Database(String fileName) throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite:" + fileName);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");

            st.execute("CREATE TABLE IF NOT EXISTS shows ("
                    + " show_id   INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " movie     TEXT NOT NULL,"
                    + " show_time TEXT NOT NULL)");

            st.execute("CREATE TABLE IF NOT EXISTS bookings ("
                    + " booking_id   INTEGER PRIMARY KEY,"
                    + " show_id      INTEGER NOT NULL REFERENCES shows(show_id),"
                    + " seat_list    TEXT NOT NULL,"
                    + " total_amount REAL NOT NULL,"
                    + " coupon       TEXT,"
                    + " status       TEXT NOT NULL DEFAULT 'CONFIRMED'"
                    + "              CHECK (status IN ('CONFIRMED','CANCELLED')),"
                    + " refund_amount REAL NOT NULL DEFAULT 0,"
                    + " booked_at    TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");

            st.execute("CREATE TABLE IF NOT EXISTS booked_seats ("
                    + " show_id    INTEGER NOT NULL REFERENCES shows(show_id),"
                    + " seat_code  TEXT NOT NULL,"
                    + " booking_id INTEGER NOT NULL REFERENCES bookings(booking_id),"
                    + " seat_type  TEXT NOT NULL,"
                    + " price      REAL NOT NULL,"
                    + " PRIMARY KEY (show_id, seat_code))");
        }
    }

    // ---------- shows ----------

    public void seedShowsIfEmpty() throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM shows")) {
            if (rs.next() && rs.getInt(1) > 0) return;
        }
        String[][] seed = {
                {"Interstellar", "7:30 PM"},
                {"Inception", "4:00 PM"},
                {"The Dark Knight", "10:00 PM"}
        };
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO shows(movie, show_time) VALUES (?, ?)")) {
            for (String[] s : seed) {
                ps.setString(1, s[0]);
                ps.setString(2, s[1]);
                ps.executeUpdate();
            }
        }
    }

    public List<Show> loadShows() throws SQLException {
        List<Show> shows = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT show_id, movie, show_time FROM shows ORDER BY show_id")) {
            while (rs.next()) {
                shows.add(new Show(rs.getInt(1), rs.getString(2), rs.getString(3)));
            }
        }
        for (Show s : shows) restoreSeats(s);
        return shows;
    }

    /** Re-marks already-booked seats in the show's 2D grid after a restart. */
    private void restoreSeats(Show show) throws SQLException {
        List<String> codes = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT seat_code FROM booked_seats WHERE show_id = ?")) {
            ps.setInt(1, show.getShowId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) codes.add(rs.getString(1));
            }
        }
        if (!codes.isEmpty()) show.bookSeats(codes);
    }

    // ---------- bookings ----------

    public List<Booking> loadBookings(List<Show> shows) throws SQLException {
        Map<Integer, Show> byId = new HashMap<>();
        for (Show s : shows) byId.put(s.getShowId(), s);

        List<Booking> list = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT booking_id, show_id, seat_list, total_amount, coupon, status, refund_amount"
                     + " FROM bookings ORDER BY booking_id")) {
            while (rs.next()) {
                List<String> seats = new ArrayList<>(Arrays.asList(rs.getString(3).split(",")));
                list.add(new Booking(rs.getInt(1), byId.get(rs.getInt(2)), seats,
                        rs.getDouble(4), rs.getString(5),
                        "CANCELLED".equals(rs.getString(6)), rs.getDouble(7)));
            }
        }
        return list;
    }

    /** Saves a booking and its seats as ONE transaction (all or nothing). */
    public void saveBooking(Booking b) throws SQLException {
        conn.setAutoCommit(false);
        try (PreparedStatement pb = conn.prepareStatement(
                "INSERT INTO bookings(booking_id, show_id, seat_list, total_amount, coupon, status, refund_amount)"
                + " VALUES (?, ?, ?, ?, ?, 'CONFIRMED', 0)");
             PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO booked_seats(show_id, seat_code, booking_id, seat_type, price)"
                + " VALUES (?, ?, ?, ?, ?)")) {
            pb.setInt(1, b.getId());
            pb.setInt(2, b.getShow().getShowId());
            pb.setString(3, String.join(",", b.getSeats()));
            pb.setDouble(4, b.getAmount());
            pb.setString(5, b.getCouponCode());
            pb.executeUpdate();

            for (String code : b.getSeats()) {
                SeatType type = Show.typeOf(code);
                ps.setInt(1, b.getShow().getShowId());
                ps.setString(2, code);
                ps.setInt(3, b.getId());
                ps.setString(4, type.name());
                ps.setDouble(5, type.getPrice());
                ps.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /** Marks the booking CANCELLED, stores the refund and frees its seats (one transaction). */
    public void cancelBooking(int bookingId, double refund) throws SQLException {
        conn.setAutoCommit(false);
        try (PreparedStatement up = conn.prepareStatement(
                "UPDATE bookings SET status = 'CANCELLED', refund_amount = ? WHERE booking_id = ?");
             PreparedStatement del = conn.prepareStatement(
                "DELETE FROM booked_seats WHERE booking_id = ?")) {
            up.setDouble(1, refund);
            up.setInt(2, bookingId);
            up.executeUpdate();
            del.setInt(1, bookingId);
            del.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ---------- reports (JOIN, GROUP BY, aggregates, CASE) ----------

    public void printSalesReport() throws SQLException {
        System.out.println();
        System.out.println("=== Sales by show ===");
        System.out.printf("%-18s %-9s %9s %9s %13s%n",
                "Movie", "Time", "Confirmed", "Cancelled", "Net revenue");

        String perShow =
                "SELECT s.movie, s.show_time,"
                + " COUNT(CASE WHEN b.status = 'CONFIRMED' THEN 1 END),"
                + " COUNT(CASE WHEN b.status = 'CANCELLED' THEN 1 END),"
                + " SUM(CASE b.status WHEN 'CONFIRMED' THEN b.total_amount"
                + "                   WHEN 'CANCELLED' THEN b.total_amount - b.refund_amount"
                + "                   ELSE 0 END)"
                + " FROM shows s LEFT JOIN bookings b ON b.show_id = s.show_id"
                + " GROUP BY s.show_id ORDER BY s.show_id";
        double grand = 0;
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(perShow)) {
            while (rs.next()) {
                double net = rs.getDouble(5);
                grand += net;
                System.out.printf("%-18s %-9s %9d %9d %13.2f%n",
                        rs.getString(1), rs.getString(2), rs.getInt(3), rs.getInt(4), net);
            }
        }
        System.out.printf("%-18s %-9s %9s %9s %13.2f%n", "TOTAL", "", "", "", grand);
        System.out.println("(Net revenue = confirmed bookings + the 20% kept on cancellations)");

        System.out.println();
        System.out.println("=== Seats currently booked, by category ===");
        System.out.printf("%-10s %6s %14s%n", "Category", "Seats", "Gross (Rs.)");
        String perType =
                "SELECT seat_type, COUNT(*), SUM(price) FROM booked_seats"
                + " GROUP BY seat_type ORDER BY SUM(price)";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(perType)) {
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("%-10s %6d %14.2f%n",
                        rs.getString(1), rs.getInt(2), rs.getDouble(3));
            }
            if (!any) System.out.println("(no seats booked yet)");
        }
    }

    @Override
    public void close() throws SQLException {
        conn.close();
    }
}
