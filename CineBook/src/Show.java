import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** One screening. Owns its own independent 5 x 8 seat grid (2D array). */
public class Show implements Bookable {
    public static final int ROWS = 5;   // A-E
    public static final int COLS = 8;   // 1-8
    private static final char FREE = 'O';
    private static final char BOOKED = 'X';

    private final int showId;
    private final String movieName;
    private final String time;
    private final char[][] grid = new char[ROWS][COLS];

    public Show(int showId, String movieName, String time) {
        this.showId = showId;
        this.movieName = movieName;
        this.time = time;
        for (char[] row : grid) {
            Arrays.fill(row, FREE);
        }
    }

    public int getShowId() { return showId; }
    public String getMovieName() { return movieName; }
    public String getTime() { return time; }

    @Override
    public String toString() { return movieName + " (" + time + ")"; }

    // ---------- seat code helpers ----------

    /** "C3" -> {2, 2}. Returns null for anything invalid (e.g. "Z9", "A0", "C33"). */
    public static int[] parseSeat(String code) {
        if (code == null) return null;
        String c = code.trim().toUpperCase();
        if (c.length() != 2) return null;
        int r = c.charAt(0) - 'A';
        int col = c.charAt(1) - '1';
        if (r < 0 || r >= ROWS || col < 0 || col >= COLS) return null;
        return new int[]{r, col};
    }

    public static SeatType typeOf(String code) {
        return SeatType.forRow(parseSeat(code)[0]);
    }

    // ---------- Bookable ----------

    /** Returns an error message, or null if every seat is valid, distinct and free. */
    public String validateSeats(List<String> codes) {
        if (codes.isEmpty()) return "No seats entered.";
        Set<String> seen = new HashSet<>();
        for (String raw : codes) {
            String code = raw.trim().toUpperCase();
            int[] p = parseSeat(code);
            if (p == null) return "Invalid seat code: " + code + ".";
            if (!seen.add(code)) return "Seat " + code + " was entered twice.";
            if (grid[p[0]][p[1]] == BOOKED) return "Seat " + code + " is already booked.";
        }
        return null;
    }

    @Override
    public boolean bookSeats(List<String> codes) {
        if (validateSeats(codes) != null) return false;   // all-or-nothing
        for (String c : codes) {
            int[] p = parseSeat(c);
            grid[p[0]][p[1]] = BOOKED;
        }
        return true;
    }

    @Override
    public void cancelBooking(List<String> codes) {
        for (String c : codes) {
            int[] p = parseSeat(c);
            if (p != null) grid[p[0]][p[1]] = FREE;
        }
    }

    // ---------- pricing (method overloading) ----------

    /** Total without any coupon. */
    public double calculateTotal(List<String> codes) {
        double total = 0;
        for (String c : codes) {
            total += typeOf(c).getPrice();
        }
        return total;
    }

    /** Total after applying a coupon (unknown/blank coupon = no discount). */
    public double calculateTotal(List<String> codes, String couponCode) {
        double total = calculateTotal(codes);
        Coupon coupon = Coupon.find(couponCode);
        if (coupon != null) {
            total -= total * coupon.getPercent() / 100.0;
        }
        return total;
    }

    // ---------- display ----------

    public void printSeatMap() {
        System.out.println("Show: " + this);
        System.out.println();
        System.out.print("      ");
        for (int c = 1; c <= COLS; c++) {
            System.out.printf("%d ", c);
        }
        System.out.println();
        for (int r = 0; r < ROWS; r++) {
            System.out.printf("  %c   ", (char) ('A' + r));
            for (int c = 0; c < COLS; c++) {
                System.out.printf("%c ", grid[r][c]);
            }
            SeatType t = SeatType.forRow(r);
            System.out.printf("  %-8s Rs. %.0f%n", t.getLabel(), t.getPrice());
        }
    }
}
