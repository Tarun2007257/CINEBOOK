/** Coupon codes and their discount percentage. Add new coupons here. */
public enum Coupon {
    CINE10(10);

    private final int percent;

    Coupon(int percent) { this.percent = percent; }

    public int getPercent() { return percent; }

    /** Returns the matching coupon, or null if the code is blank/unknown. */
    public static Coupon find(String code) {
        if (code == null || code.trim().isEmpty()) return null;
        for (Coupon c : values()) {
            if (c.name().equalsIgnoreCase(code.trim())) return c;
        }
        return null;
    }
}
