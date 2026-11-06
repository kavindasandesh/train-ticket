package railway.ui;

import railway.model.Booking;
import railway.model.Seat;

import java.util.List;

/** Receives events from the seat map. */
public interface SeatMapListener {
    /** The set of selected seats changed. */
    void selectionChanged(List<Seat> selected);

    /** A booked seat was clicked (view mode and booking mode). */
    void bookedSeatClicked(Seat seat, List<Booking> bookings);

    /** The officer tried to select more seats than allowed. */
    void selectionLimitReached(int max);
}
