package railway.model;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An express train that runs daily in one direction on one route.
 * It has a list of stops (origin, halts, terminus) and its own set of
 * carriages; trains differ in the number of carriages and seats per carriage.
 */
public class Train {
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final String trainNo;
    private final String name;
    private final Route route;
    private final Direction direction;
    private final List<Stop> stops;
    private final List<Carriage> carriages;

    public Train(String trainNo, String name, Route route, Direction direction,
                 List<Stop> stops, List<Carriage> carriages) {
        this.trainNo = trainNo;
        this.name = name;
        this.route = route;
        this.direction = direction;
        this.stops = new ArrayList<>(stops);
        this.carriages = new ArrayList<>(carriages);
    }

    public String getTrainNo() { return trainNo; }
    public String getName() { return name; }
    public Route getRoute() { return route; }
    public Direction getDirection() { return direction; }
    public List<Stop> getStops() { return Collections.unmodifiableList(stops); }
    public Stop getStop(int index) { return stops.get(index); }
    public int getStopCount() { return stops.size(); }
    public List<Carriage> getCarriages() { return Collections.unmodifiableList(carriages); }

    public String getOrigin() { return stops.get(0).getStation(); }
    public String getDestination() { return stops.get(stops.size() - 1).getStation(); }
    public LocalTime getDeparture() { return stops.get(0).getTime(); }
    public LocalTime getArrival() { return stops.get(stops.size() - 1).getTime(); }

    public Carriage getCarriage(int number) {
        for (Carriage c : carriages) {
            if (c.getNumber() == number) return c;
        }
        return null;
    }

    public List<Carriage> getCarriages(SeatClass cls) {
        return carriages.stream().filter(c -> c.getSeatClass() == cls).collect(Collectors.toList());
    }

    public int getSeatCount(SeatClass cls) {
        return getCarriages(cls).stream().mapToInt(Carriage::getSeatCount).sum();
    }

    public int getTotalSeats() {
        return carriages.stream().mapToInt(Carriage::getSeatCount).sum();
    }

    /** Classes this train actually carries. */
    public List<SeatClass> getClasses() {
        List<SeatClass> list = new ArrayList<>();
        for (SeatClass c : SeatClass.values()) {
            if (getSeatCount(c) > 0) list.add(c);
        }
        return list;
    }

    /** Every seat of one class, ordered by carriage then number. */
    public List<Seat> getSeats(SeatClass cls) {
        List<Seat> seats = new ArrayList<>();
        for (Carriage c : getCarriages(cls)) {
            for (int n = 1; n <= c.getSeatCount(); n++) seats.add(new Seat(c.getNumber(), n));
        }
        return seats;
    }

    /** True if the seat exists on this train and belongs to a carriage of the given class. */
    public boolean isSeatOfClass(Seat seat, SeatClass cls) {
        Carriage c = getCarriage(seat.getCarriage());
        return c != null && c.getSeatClass() == cls && c.hasSeat(seat);
    }

    /** Position of a station in this train's stop list, or -1 if the train does not halt there. */
    public int getStopIndex(String station) {
        for (int i = 0; i < stops.size(); i++) {
            if (stops.get(i).getStation().equalsIgnoreCase(station.trim())) return i;
        }
        return -1;
    }

    public String getJourneyLabel() {
        return getOrigin() + " -> " + getDestination();
    }

    @Override
    public String toString() {
        return trainNo + "  " + name + "   " + getDeparture().format(TIME_FORMAT)
                + " - " + getArrival().format(TIME_FORMAT);
    }
}
