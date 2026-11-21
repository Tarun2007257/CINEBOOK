import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** One confirmed (or later cancelled) booking. IDs come from a static counter. */
public class Booking {
    public static final int REFUND_PERCENT = 80;
    private static int nextId = 1001;

    private final int id;
    private final Show show;
    private final List<String> seats;
    private final double amount;
    private final String couponCode;   // null if none applied
    private boolean cancelled;
    private double refund;

    /** New booking: takes the next ID from the static counter. */
    public Booking(Show show, List<String> seats, double amount, String couponCode) {
        this.id = nextId++;
        this.show = show;
        this.seats = new ArrayList<>(seats);
        this.amount = amount;
        this.couponCode = couponCode;
    }

    /** Restores a booking loaded from the database (keeps the counter ahead of stored IDs). */
    public Booking(int id, Show show, List<String> seats, double amount,
                   String couponCode, boolean cancelled, double refund) {
        this.id = id;
        this.show = show;
        this.seats = new ArrayList<>(seats);
        this.amount = amount;
        this.couponCode = couponCode;
        this.cancelled = cancelled;
        this.refund = refund;
        if (id >= nextId) nextId = id + 1;
    }

    public int getId() { return id; }
    public String getIdLabel() { return "BK" + id; }
    public Show getShow() { return show; }
    public List<String> getSeats() { return seats; }
    public double getAmount() { return amount; }
    public String getCouponCode() { return couponCode; }
    public boolean isCancelled() { return cancelled; }
    public double getRefund() { return refund; }

    public double calculateRefund() { return amount * REFUND_PERCENT / 100.0; }

    public void markCancelled(double refundAmount) {
        this.cancelled = true;
        this.refund = refundAmount;
    }

    /** e.g. "C3, C4 (Premium)" or "A1 (Regular); E2 (VIP)". */
    public String getSeatSummary() {
        Map<SeatType, List<String>> groups = new EnumMap<>(SeatType.class);
        for (String s : seats) {
            groups.computeIfAbsent(Show.typeOf(s), k -> new ArrayList<>()).add(s);
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<SeatType, List<String>> e : groups.entrySet()) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(String.join(", ", e.getValue()))
              .append(" (").append(e.getKey().getLabel()).append(")");
        }
        return sb.toString();
    }
}
