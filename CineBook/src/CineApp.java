import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** Console menu and input only. All rules live in the other classes. */
public class CineApp {
    private static final Scanner in = new Scanner(System.in);

    public static void main(String[] args) {
        try (Database db = new Database("cinebook.db")) {
            Theatre theatre = new Theatre(db);
            System.out.println("=== CineBook - Movie Ticket Booking ===");
            boolean running = true;
            while (running) {
                System.out.println();
                System.out.println("1. Book tickets");
                System.out.println("2. Cancel a booking");
                System.out.println("3. View all bookings");
                System.out.println("4. Sales report");
                System.out.println("5. Exit");
                int choice = readInt("Choose: ", 1, 5);
                switch (choice) {
                    case 1: bookTickets(theatre); break;
                    case 2: cancelBooking(theatre); break;
                    case 3: viewBookings(theatre); break;
                    case 4: theatre.printSalesReport(); break;
                    default: running = false;
                }
            }
            System.out.println("Thank you for using CineBook!");
        } catch (SQLException e) {
            System.out.println("Database problem: " + e.getMessage());
        }
    }

    // ---------- flows ----------

    private static void bookTickets(Theatre theatre) {
        List<Show> shows = theatre.getShows();
        System.out.println();
        System.out.println("Now showing:");
        for (int i = 0; i < shows.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, shows.get(i));
        }
        int pick = readInt("Choose a show (0 to go back): ", 0, shows.size());
        if (pick == 0) return;
        Show show = shows.get(pick - 1);

        System.out.println();
        show.printSeatMap();
        System.out.print("\nEnter seats (e.g. C3,C4): ");
        List<String> seats = parseSeats(in.nextLine());

        String error = show.validateSeats(seats);
        if (error != null) {
            System.out.println(error + " No seats were booked.");
            return;
        }

        String coupon = askCoupon();
        try {
            Booking b = theatre.book(show, seats, coupon);
            printSummary(b);
        } catch (RuntimeException e) {
            System.out.println("Booking failed: " + e.getMessage());
        }
    }

    private static void cancelBooking(Theatre theatre) {
        System.out.print("\nEnter booking ID (e.g. BK1001): ");
        String text = in.nextLine().trim().toUpperCase();
        if (text.startsWith("BK")) text = text.substring(2);
        try {
            int id = Integer.parseInt(text);
            Booking b = theatre.cancel(id);
            System.out.println();
            System.out.println("Booking " + b.getIdLabel() + " cancelled. Seats released.");
            System.out.printf("Refund : Rs. %.2f (%d%% of Rs. %.2f)%n",
                    b.getRefund(), Booking.REFUND_PERCENT, b.getAmount());
        } catch (NumberFormatException e) {
            System.out.println("That is not a valid booking ID.");
        } catch (RuntimeException e) {
            System.out.println("Cancellation failed: " + e.getMessage());
        }
    }

    private static void viewBookings(Theatre theatre) {
        List<Booking> all = theatre.getBookings();
        if (all.isEmpty()) {
            System.out.println("\nNo bookings yet.");
            return;
        }
        System.out.println();
        System.out.printf("%-8s %-26s %-28s %10s  %s%n",
                "ID", "Show", "Seats", "Amount", "Status");
        for (Booking b : all) {
            System.out.printf("%-8s %-26s %-28s %10.2f  %s%n",
                    b.getIdLabel(), b.getShow(), String.join(",", b.getSeats()),
                    b.getAmount(), b.isCancelled() ? "CANCELLED" : "CONFIRMED");
        }
    }

    private static void printSummary(Booking b) {
        System.out.println();
        System.out.println("Booking ID : " + b.getIdLabel());
        System.out.println("Movie      : " + b.getShow());
        System.out.println("Seats      : " + b.getSeatSummary());
        String note = "";
        Coupon c = Coupon.find(b.getCouponCode());
        if (c != null) note = " (after " + c.getPercent() + "% discount)";
        System.out.printf("Total      : Rs. %.2f%s%n", b.getAmount(), note);
    }

    // ---------- input helpers ----------

    private static String askCoupon() {
        while (true) {
            System.out.print("Coupon code (Enter to skip): ");
            String code = in.nextLine().trim();
            if (code.isEmpty()) return null;
            if (Coupon.find(code) != null) return code;
            System.out.println("Invalid coupon. Try again or press Enter to skip.");
        }
    }

    private static List<String> parseSeats(String line) {
        List<String> seats = new ArrayList<>();
        for (String part : line.split(",")) {
            String s = part.trim().toUpperCase();
            if (!s.isEmpty()) seats.add(s);
        }
        return seats;
    }

    private static int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = in.nextLine().trim();
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) return value;
            } catch (NumberFormatException ignored) {
                // fall through to the message below
            }
            System.out.println("Please enter a number between " + min + " and " + max + ".");
        }
    }
}
