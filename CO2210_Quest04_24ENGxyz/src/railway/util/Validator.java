package railway.util;

import railway.model.Booking;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Input validation helpers (static, stateless). */
public final class Validator {
    private Validator() { }

    /** Sri Lankan NIC: old format 9 digits + V/X, new format 12 digits. */
    public static boolean isValidId(String id) {
        return id != null && id.matches("^(\\d{9}[VvXx]|\\d{12})$");
    }

    public static boolean isValidName(String name) {
        return name != null && name.matches("^[A-Za-z][A-Za-z .'-]{1,49}$");
    }

    /** Local number with leading zero, ten digits, e.g. 0771234567. */
    public static boolean isValidContact(String contact) {
        return contact != null && contact.matches("^0\\d{9}$");
    }

    /** Parses dd-MM-yyyy; returns null when the text is not a real date. */
    public static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text, Booking.DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
