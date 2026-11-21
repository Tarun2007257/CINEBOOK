import java.util.List;

/** Contract for anything that can have seats booked and released. */
public interface Bookable {
    /** All-or-nothing: returns false and changes nothing if any seat is invalid/taken. */
    boolean bookSeats(List<String> seatCodes);

    /** Frees the given seats. */
    void cancelBooking(List<String> seatCodes);
}
