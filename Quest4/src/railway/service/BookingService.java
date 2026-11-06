package railway.service;

import railway.data.BookingRepository;
import railway.data.TimetableRepository;
import railway.model.Booking;
import railway.model.BookingStatus;
import railway.model.Carriage;
import railway.model.Passenger;
import railway.model.Seat;
import railway.model.SeatClass;
import railway.model.Train;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Business rules of the booking system. The GUI only talks to this class; it
 * never touches the files directly.
 *
 * A seat is identified by (train, date, carriage, seat number). Because a
 * passenger may board and leave at intermediate stations, a seat can be sold
 * to different passengers for non-overlapping parts of the same journey.
 */
public class BookingService {
    public static final int MAX_SEATS_PER_CUSTOMER = 4;
    public static final int ADVANCE_BOOKING_DAYS = 30;

    private final TimetableRepository timetable;
    private final BookingRepository repository;
    private final List<Booking> bookings;

    public BookingService(TimetableRepository timetable, BookingRepository repository) {
        this.timetable = timetable;
        this.repository = repository;
        this.bookings = repository.loadAll(timetable);
    }

    public TimetableRepository getTimetable() { return timetable; }

    // ------------------------------------------------------------ queries

    /**
     * Every occupied seat of a train/date on the journey from..to, with the
     * confirmed bookings that hold it. A booking being altered can be ignored.
     */
    public Map<Seat, List<Booking>> getOccupancy(Train train, LocalDate date, String from, String to,
                                                 Booking ignore) {
        Map<Seat, List<Booking>> map = new TreeMap<>();
        int fi = train.getStopIndex(from);
        int ti = train.getStopIndex(to);
        if (fi < 0 || ti < 0 || fi >= ti) return map;
        for (Booking b : bookings) {
            if (b.isActive() && b != ignore && b.getTrain() == train && b.getTravelDate().equals(date)
                    && b.overlaps(fi, ti)) {
                for (Seat s : b.getSeats()) {
                    map.computeIfAbsent(s, k -> new ArrayList<>()).add(b);
                }
            }
        }
        return map;
    }

    /** Free seats of one class on the journey, ordered by carriage then number. */
    public List<Seat> getFreeSeats(Train train, LocalDate date, SeatClass cls, String from, String to,
                                   Booking ignore) {
        Set<Seat> taken = getOccupancy(train, date, from, to, ignore).keySet();
        return train.getSeats(cls).stream().filter(s -> !taken.contains(s)).collect(Collectors.toList());
    }

    public int countFreeSeats(Train train, LocalDate date, SeatClass cls, String from, String to,
                              Booking ignore) {
        return getFreeSeats(train, date, cls, from, to, ignore).size();
    }

    /** True if no seat of any class is free on the journey. */
    public boolean isFullyBooked(Train train, LocalDate date, String from, String to, Booking ignore) {
        for (SeatClass c : train.getClasses()) {
            if (countFreeSeats(train, date, c, from, to, ignore) > 0) return false;
        }
        return true;
    }

    public Booking findById(String id) {
        for (Booking b : bookings) {
            if (b.getBookingId().equalsIgnoreCase(id.trim())) return b;
        }
        return null;
    }

    public List<Booking> findByPassengerId(String idNumber) {
        return bookings.stream()
                .filter(b -> b.getPassenger().getIdNumber().equalsIgnoreCase(idNumber.trim()))
                .collect(Collectors.toList());
    }

    /** Bookings for one train on one date (confirmed and cancelled), ordered by class then first seat. */
    public List<Booking> findByTrainAndDate(Train train, LocalDate date) {
        return bookings.stream()
                .filter(b -> b.getTrain() == train && b.getTravelDate().equals(date))
                .sorted((a, b) -> a.getSeatClass() != b.getSeatClass()
                        ? a.getSeatClass().compareTo(b.getSeatClass())
                        : a.getSeats().get(0).compareTo(b.getSeats().get(0)))
                .collect(Collectors.toList());
    }

    public List<Booking> getAllBookings() {
        return new ArrayList<>(bookings);
    }

    // ----------------------------------------------------------- commands

    public Booking createBooking(Passenger passenger, Train train, LocalDate date, String from, String to,
                                 SeatClass cls, List<Seat> seats) throws BookingException {
        int[] journey = resolveJourney(train, from, to);
        validateClass(train, cls);
        validateDate(date, train, journey[0]);
        checkNotFull(train, date, cls, from, to, null);
        checkCustomerLimit(passenger.getIdNumber(), train, date, seats.size(), null);
        validateSeats(train, date, cls, from, to, seats, null);

        Booking booking = new Booking(nextBookingId(), passenger, train, date,
                train.getStop(journey[0]).getStation(), train.getStop(journey[1]).getStation(), cls,
                seats, BookingStatus.CONFIRMED, LocalDateTime.now().withNano(0));
        bookings.add(booking);
        if (!repository.saveAll(bookings)) {
            bookings.remove(booking);
            throw new BookingException("Could not save the booking file. Please try again.");
        }
        return booking;
    }

    /**
     * Changes a confirmed booking: customer name/contact, date, stations, class and seats.
     * Passing null for name or contact keeps the current value.
     */
    public Booking alterBooking(String bookingId, String newName, String newContact, LocalDate date,
                                String from, String to, SeatClass cls, List<Seat> seats)
            throws BookingException {
        Booking b = requireConfirmed(bookingId);
        Train train = b.getTrain();
        int[] journey = resolveJourney(train, from, to);
        validateClass(train, cls);
        validateDate(date, train, journey[0]);
        checkNotFull(train, date, cls, from, to, b);
        checkCustomerLimit(b.getPassenger().getIdNumber(), train, date, seats.size(), b);
        validateSeats(train, date, cls, from, to, seats, b);

        LocalDate oldDate = b.getTravelDate();
        String oldFrom = b.getFromStation();
        String oldTo = b.getToStation();
        SeatClass oldClass = b.getSeatClass();
        List<Seat> oldSeats = new ArrayList<>(b.getSeats());
        String oldName = b.getPassenger().getName();
        String oldContact = b.getPassenger().getContactNumber();

        b.reschedule(date, train.getStop(journey[0]).getStation(), train.getStop(journey[1]).getStation(),
                cls, seats);
        if (newName != null) b.getPassenger().setName(newName);
        if (newContact != null) b.getPassenger().setContactNumber(newContact);
        if (!repository.saveAll(bookings)) {                  // roll back if the file write failed
            b.reschedule(oldDate, oldFrom, oldTo, oldClass, oldSeats);
            b.getPassenger().setName(oldName);
            b.getPassenger().setContactNumber(oldContact);
            throw new BookingException("Could not save the change. Booking left unchanged.");
        }
        return b;
    }

    public Booking cancelBooking(String bookingId) throws BookingException {
        Booking b = requireConfirmed(bookingId);
        validateNotDeparted(b.getTravelDate(), b.getTrain(), b.getTrain().getStopIndex(b.getFromStation()));
        b.setStatus(BookingStatus.CANCELLED);
        if (!repository.saveAll(bookings)) {
            b.setStatus(BookingStatus.CONFIRMED);
            throw new BookingException("Could not save the cancellation. Booking left unchanged.");
        }
        return b;
    }

    /**
     * Suggests seats for the officer: a run of neighbouring seats in one carriage if possible,
     * otherwise seats in one carriage, otherwise seats spread over carriages.
     * Seats in {@code preferred} (e.g. the seats of the booking being altered) are tried first.
     */
    public List<Seat> suggestSeats(Train train, LocalDate date, SeatClass cls, String from, String to,
                                   int count, Booking ignore, List<Seat> preferred) throws BookingException {
        validateSeatCount(count);
        List<Seat> free = getFreeSeats(train, date, cls, from, to, ignore);
        if (free.isEmpty()) {
            throw new BookingException("No " + cls.getDisplayName().toLowerCase() + " seat is free for this journey.");
        }
        if (free.size() < count) {
            throw new BookingException("Only " + free.size() + " "+ cls.getDisplayName().toLowerCase()
                    + " seat(s) are free, but " + count + " requested.");
        }
        List<Seat> result = new ArrayList<>();
        if (preferred != null) {
            for (Seat s : preferred) {
                if (result.size() < count && free.contains(s)) result.add(s);
            }
            if (result.size() == count) return result;
            result.clear();
        }
        // 1) a run of neighbouring seat numbers inside one carriage
        for (Carriage c : train.getCarriages(cls)) {
            List<Seat> inCarriage = free.stream().filter(s -> s.getCarriage() == c.getNumber())
                    .collect(Collectors.toList());
            for (int i = 0; i + count <= inCarriage.size(); i++) {
                if (inCarriage.get(i + count - 1).getNumber() - inCarriage.get(i).getNumber() == count - 1) {
                    return new ArrayList<>(inCarriage.subList(i, i + count));
                }
            }
        }
        // 2) any seats in one carriage, 3) lowest free seats overall
        for (Carriage c : train.getCarriages(cls)) {
            List<Seat> inCarriage = free.stream().filter(s -> s.getCarriage() == c.getNumber())
                    .collect(Collectors.toList());
            if (inCarriage.size() >= count) return new ArrayList<>(inCarriage.subList(0, count));
        }
        return new ArrayList<>(free.subList(0, count));
    }

    /**
     * Adds a few sample bookings relative to today so a new installation has something to show.
     * Returns the number of bookings added.
     */
    public int addDemoBookings() {
        String[][] specs = {
            // trainNo, days ahead, from, to, class, seats wanted, NIC, name, contact
            {"1015", "2", "Colombo Fort", "Peradeniya Junction", "SECOND", "3", "912345678V", "Nimal Perera", "0771234567"},
            {"1015", "2", "Ragama", "Kandy", "SECOND", "4", "885551234V", "Ruwan Fernando", "0765551234"},
            {"1015", "2", "Colombo Fort", "Kandy", "FIRST", "2", "200012345678", "Kumari Silva", "0712345678"},
            {"1015", "2", "Colombo Fort", "Kandy", "THIRD", "4", "761234567V", "Sunil Wickramasinghe", "0752233445"},
            {"1002", "3", "Peradeniya Junction", "Colombo Fort", "THIRD", "2", "992345678V", "Anusha Rajapaksa", "0719988776"},
            {"1002", "3", "Rambukkana", "Gampaha", "SECOND", "1", "947654321X", "Dilini Jayasuriya", "0701122334"},
            {"8056", "1", "Panadura", "Hikkaduwa", "THIRD", "4", "831112223V", "Chamara Gunawardena", "0778899001"},
            {"4077", "4", "Colombo Fort", "Jaffna", "FIRST", "2", "199512345678", "Thilaga Selvarajah", "0764455667"},
            {"4077", "4", "Anuradhapura", "Kankesanthurai", "FIRST", "3", "780123456V", "Rajan Kumar", "0753344556"},
        };
        int added = 0;
        for (String[] s : specs) {
            try {
                Train train = timetable.findByNumber(s[0]);
                if (train == null) continue;
                LocalDate date = LocalDate.now().plusDays(Integer.parseInt(s[1]));
                SeatClass cls = SeatClass.fromName(s[4]);
                List<Seat> seats = suggestSeats(train, date, cls, s[2], s[3], Integer.parseInt(s[5]), null, null);
                createBooking(new Passenger(s[6], s[7], s[8]), train, date, s[2], s[3], cls, seats);
                added++;
            } catch (BookingException | RuntimeException e) {
                // a sample that cannot be placed (e.g. already added) is simply skipped
            }
        }
        return added;
    }

    // ------------------------------------------------------------ helpers

    private Booking requireConfirmed(String bookingId) throws BookingException {
        Booking b = findById(bookingId);
        if (b == null) throw new BookingException("No booking found with ID " + bookingId.trim().toUpperCase() + ".");
        if (!b.isActive()) throw new BookingException("Booking " + b.getBookingId() + " is already cancelled.");
        return b;
    }

    /** Returns {fromIndex, toIndex}; the boarding station must come before the alighting station. */
    private int[] resolveJourney(Train train, String from, String to) throws BookingException {
        int fi = train.getStopIndex(from);
        int ti = train.getStopIndex(to);
        if (fi < 0) throw new BookingException("Train " + train.getTrainNo() + " does not stop at " + from + ".");
        if (ti < 0) throw new BookingException("Train " + train.getTrainNo() + " does not stop at " + to + ".");
        if (fi >= ti) throw new BookingException("The alighting station must come after the boarding station.");
        return new int[] {fi, ti};
    }

    private void validateClass(Train train, SeatClass cls) throws BookingException {
        if (train.getSeatCount(cls) == 0) {
            throw new BookingException("Train " + train.getTrainNo() + " has no " + cls.getDisplayName().toLowerCase() + ".");
        }
    }

    private void validateSeatCount(int count) throws BookingException {
        if (count < 1 || count > MAX_SEATS_PER_CUSTOMER) {
            throw new BookingException("A customer can book between 1 and " + MAX_SEATS_PER_CUSTOMER + " seats.");
        }
    }

    private void validateDate(LocalDate date, Train train, int boardingIdx) throws BookingException {
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) throw new BookingException("The travel date is in the past.");
        if (date.isAfter(today.plusDays(ADVANCE_BOOKING_DAYS))) {
            throw new BookingException("Bookings open only " + ADVANCE_BOOKING_DAYS + " days in advance.");
        }
        validateNotDeparted(date, train, boardingIdx);
    }

    /** The train must not already have left the passenger's boarding station today. */
    private void validateNotDeparted(LocalDate date, Train train, int boardingIdx) throws BookingException {
        if (date.equals(LocalDate.now()) && !train.getStop(boardingIdx).getTime().isAfter(LocalTime.now())) {
            throw new BookingException("Train " + train.getTrainNo() + " has already left "
                    + train.getStop(boardingIdx).getStation() + " today.");
        }
    }

    private void checkNotFull(Train train, LocalDate date, SeatClass cls, String from, String to,
                              Booking ignore) throws BookingException {
        if (countFreeSeats(train, date, cls, from, to, ignore) == 0) {
            throw new BookingException("The " + cls.getDisplayName().toLowerCase() + " of train "
                    + train.getTrainNo() + " is FULLY BOOKED for this journey. No seat can be booked.");
        }
    }

    /** A customer (by ID number) may hold at most four seats on the same train and date. */
    private void checkCustomerLimit(String idNumber, Train train, LocalDate date,
                                    int newSeats, Booking ignore) throws BookingException {
        validateSeatCount(newSeats);
        int held = 0;
        for (Booking b : bookings) {
            if (b.isActive() && b != ignore && b.getTrain() == train && b.getTravelDate().equals(date)
                    && b.getPassenger().getIdNumber().equalsIgnoreCase(idNumber)) {
                held += b.getSeatCount();
            }
        }
        if (held + newSeats > MAX_SEATS_PER_CUSTOMER) {
            throw new BookingException("ID " + idNumber + " already holds " + held
                    + " seat(s) on this train and date; the limit is " + MAX_SEATS_PER_CUSTOMER + ".");
        }
    }

    /** The chosen seats must exist, belong to the chosen class, be distinct and be free. */
    private void validateSeats(Train train, LocalDate date, SeatClass cls, String from, String to,
                               List<Seat> seats, Booking ignore) throws BookingException {
        if (seats == null || seats.isEmpty()) throw new BookingException("Please select at least one seat.");
        validateSeatCount(seats.size());
        if (new HashSet<>(seats).size() != seats.size()) throw new BookingException("A seat was selected twice.");
        Set<Seat> taken = getOccupancy(train, date, from, to, ignore).keySet();
        for (Seat s : seats) {
            if (!train.isSeatOfClass(s, cls)) {
                throw new BookingException("Seat " + s + " is not a " + cls.getDisplayName().toLowerCase() + " seat.");
            }
            if (taken.contains(s)) throw new BookingException("Seat " + s + " is no longer available.");
        }
    }

    private String nextBookingId() {
        int max = 0;
        for (Booking b : bookings) {
            try {
                max = Math.max(max, Integer.parseInt(b.getBookingId().substring(2)));
            } catch (NumberFormatException ignored) { }
        }
        return String.format("BK%05d", max + 1);
    }
}
