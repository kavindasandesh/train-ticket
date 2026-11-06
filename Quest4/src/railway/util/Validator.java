package railway.util;

import railway.model.Booking;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class Validator {
    private Validator() { }

    // NIC verify
    public static boolean isValidId(String id) {
        return id != null && id.matches("^(\\d{9}[VvXx]|\\d{12})$");
    }

    public static boolean isValidName(String name) {
        return name != null && name.matches("^[A-Za-z][A-Za-z .'-]{1,49}$");
    }

    // Phone no verify
    public static boolean isValidContact(String contact) {
        return contact != null && contact.matches("^0\\d{9}$");
    }

    public static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text, Booking.DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
