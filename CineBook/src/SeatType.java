/** Seat categories. Prices live ONLY here (never hard-coded in booking logic). */
public enum SeatType {
    REGULAR("Regular", 150.0),
    PREMIUM("Premium", 250.0),
    VIP("VIP", 400.0);

    private final String label;
    private final double price;

    SeatType(String label, double price) {
        this.label = label;
        this.price = price;
    }

    public String getLabel() { return label; }
    public double getPrice() { return price; }

    /** Rows A-B (0,1) = Regular, C-D (2,3) = Premium, E (4) = VIP. */
    public static SeatType forRow(int rowIndex) {
        switch (rowIndex) {
            case 0:
            case 1:  return REGULAR;
            case 2:
            case 3:  return PREMIUM;
            default: return VIP;
        }
    }
}
