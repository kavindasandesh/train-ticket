package railway.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A reservation of one to four seats of one class on a train, for a given
 * date and for a journey between two of the train's stations.
 */
public class Booking {
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final String bookingId;
    private final Passenger passenger;
    private final Train train;
    private LocalDate travelDate;
    private String fromStation;
    private String toStation;
    private SeatClass seatClass;
    private final List<Seat> seats = new ArrayList<>();
    private BookingStatus status;
    private final LocalDateTime bookedAt;

    public Booking(String bookingId, Passenger passenger, Train train, LocalDate travelDate,
                   String fromStation, String toStation, SeatClass seatClass,
                   List<Seat> seats, BookingStatus status, LocalDateTime bookedAt) {
        this.bookingId = bookingId;
        this.passenger = passenger;
        this.train = train;
        this.travelDate = travelDate;
        this.fromStation = fromStation;
        this.toStation = toStation;
        this.seatClass = seatClass;
        this.seats.addAll(seats);
        Collections.sort(this.seats);
        this.status = status;
        this.bookedAt = bookedAt;
    }

    public String getBookingId() { return bookingId; }
    public Passenger getPassenger() { return passenger; }
    public Train getTrain() { return train; }
    public LocalDate getTravelDate() { return travelDate; }
    public String getFromStation() { return fromStation; }
    public String getToStation() { return toStation; }
    public SeatClass getSeatClass() { return seatClass; }
    public List<Seat> getSeats() { return Collections.unmodifiableList(seats); }
    public int getSeatCount() { return seats.size(); }
    public BookingStatus getStatus() { return status; }
    public LocalDateTime getBookedAt() { return bookedAt; }
    public boolean isActive() { return status == BookingStatus.CONFIRMED; }

    public void setStatus(BookingStatus status) { this.status = status; }

    /** Changes date, journey, class and seats in one step (used when altering a booking). */
    public void reschedule(LocalDate date, String from, String to, SeatClass cls, List<Seat> newSeats) {
        this.travelDate = date;
        this.fromStation = from;
        this.toStation = to;
        this.seatClass = cls;
        this.seats.clear();
        this.seats.addAll(newSeats);
        Collections.sort(this.seats);
    }

    /** True if this booking occupies seats on any part of the stop range [fromIdx, toIdx). */
    public boolean overlaps(int fromIdx, int toIdx) {
        int myFrom = train.getStopIndex(fromStation);
        int myTo = train.getStopIndex(toStation);
        return myFrom < toIdx && fromIdx < myTo;
    }

    public String journeyText() {
        return fromStation + " -> " + toStation;
    }

    public String seatsAsText() {
        return seats.stream().map(Seat::toString).collect(Collectors.joining(", "));
    }

    /** One line of the bookings.csv file. Seats are written as carriage-number and separated by ';'. */
    public String toCsv() {
        return String.join(",", bookingId, passenger.getIdNumber(), passenger.getName(),
                passenger.getContactNumber(), train.getTrainNo(), travelDate.toString(),
                fromStation, toStation, seatClass.name(),
                seats.stream().map(Seat::toCsv).collect(Collectors.joining(";")),
                status.name(), bookedAt.toString());
    }

    /** Multi-line description shown when an officer views one booking. */
    public String details() {
        int fi = train.getStopIndex(fromStation);
        int ti = train.getStopIndex(toStation);
        StringBuilder sb = new StringBuilder();
        sb.append("Booking ID   : ").append(bookingId).append('\n');
        sb.append("Status       : ").append(status).append('\n');
        sb.append("Passenger    : ").append(passenger.getName()).append('\n');
        sb.append("ID number    : ").append(passenger.getIdNumber()).append('\n');
        sb.append("Contact      : ").append(passenger.getContactNumber()).append('\n');
        sb.append("Train        : ").append(train.getTrainNo()).append(' ').append(train.getName()).append('\n');
        sb.append("Journey      : ").append(journeyText()).append('\n');
        sb.append("Travel date  : ").append(travelDate.format(DATE_FORMAT)).append('\n');
        sb.append("Departs      : ").append(fromStation).append(' ')
          .append(train.getStop(fi).getTime().format(Train.TIME_FORMAT)).append('\n');
        sb.append("Arrives      : ").append(toStation).append(' ')
          .append(train.getStop(ti).getTime().format(Train.TIME_FORMAT)).append('\n');
        sb.append("Class        : ").append(seatClass.getDisplayName()).append('\n');
        sb.append("Seat(s)      : ").append(seatsAsText()).append("  (").append(seats.size()).append(")\n");
        sb.append("Booked on    : ").append(bookedAt.format(DATE_TIME_FORMAT));
        return sb.toString();
    }
}
