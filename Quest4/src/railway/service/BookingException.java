package railway.service;

/**
 * Checked exception thrown when a booking rule is violated
 * (e.g. train fully booked, more than four seats requested).
 */
public class BookingException extends Exception {
    private static final long serialVersionUID = 1L;

    public BookingException(String message) {
        super(message);
    }
}
