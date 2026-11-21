import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Holds all shows and bookings; keeps memory (2D grids) and database in step. */
public class Theatre {
    private final Database db;
    private final List<Show> shows;
    private final List<Booking> bookings;   // ArrayList of every booking

    public Theatre(Database db) throws SQLException {
        this.db = db;
        db.seedShowsIfEmpty();
        this.shows = db.loadShows();                 // grids restored from booked_seats
        this.bookings = db.loadBookings(shows);      // history restored from bookings
    }

    public List<Show> getShows() { return shows; }
    public List<Booking> getBookings() { return bookings; }

    /** Books seats all-or-nothing. Throws IllegalArgumentException with a user-friendly message. */
    public Booking book(Show show, List<String> seats, String couponInput) {
        String error = show.validateSeats(seats);
        if (error != null) throw new IllegalArgumentException(error);

        List<String> codes = new ArrayList<>();
        for (String s : seats) codes.add(s.trim().toUpperCase());

        // overloaded calculateTotal: with and without coupon
        Coupon coupon = Coupon.find(couponInput);
        double total = (coupon == null)
                ? show.calculateTotal(codes)
                : show.calculateTotal(codes, coupon.name());

        Booking booking = new Booking(show, codes, total, coupon == null ? null : coupon.name());
        try {
            db.saveBooking(booking);                 // one transaction: booking row + seat rows
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save booking: " + e.getMessage(), e);
        }
        show.bookSeats(codes);                       // only after the DB accepted it
        bookings.add(booking);
        return booking;
    }

    /** Cancels a booking by numeric ID, frees seats, refunds 80%. */
    public Booking cancel(int bookingId) {
        for (Booking b : bookings) {
            if (b.getId() == bookingId) {
                if (b.isCancelled()) {
                    throw new IllegalArgumentException(b.getIdLabel() + " is already cancelled.");
                }
                double refund = b.calculateRefund();
                try {
                    db.cancelBooking(b.getId(), refund);
                } catch (SQLException e) {
                    throw new IllegalStateException("Could not cancel booking: " + e.getMessage(), e);
                }
                b.getShow().cancelBooking(b.getSeats());
                b.markCancelled(refund);
                return b;
            }
        }
        throw new IllegalArgumentException("No booking found with ID BK" + bookingId + ".");
    }

    public void printSalesReport() {
        try {
            db.printSalesReport();
        } catch (SQLException e) {
            System.out.println("Could not generate report: " + e.getMessage());
        }
    }
}
