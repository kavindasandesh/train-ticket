package railway.ui;

import railway.model.Booking;
import railway.model.Seat;

import java.util.List;

public interface SeatMapListener {
    void selectionChanged(List<Seat> selected);

    void bookedSeatClicked(Seat seat, List<Booking> bookings);

    void selectionLimitReached(int max);
}
