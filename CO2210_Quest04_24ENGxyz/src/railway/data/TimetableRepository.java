package railway.data;

import railway.model.Carriage;
import railway.model.Direction;
import railway.model.Route;
import railway.model.SeatClass;
import railway.model.Stop;
import railway.model.Train;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Loads the express train timetable from trains.csv.
 * Columns: trainNo,name,routeCode,direction,departure,arrival,carriages,stops
 * carriages = "CLASS:seats;CLASS:seats;..." in carriage order (carriage 1 first)
 * stops     = "Station@HH:mm;Station@HH:mm" (intermediate halts in travel order)
 */
public class TimetableRepository {
    private final Path file;
    private final List<Train> trains = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public TimetableRepository(String filePath) throws IOException {
        this.file = Paths.get(filePath);
        load();
    }

    private void load() throws IOException {
        trains.clear();
        List<String> lines = Files.readAllLines(file);
        for (int i = 1; i < lines.size(); i++) {              // line 0 is the header
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            try {
                trains.add(parseTrain(line));
            } catch (RuntimeException e) {
                warnings.add("Timetable line " + (i + 1) + " skipped: " + e.getMessage());
            }
        }
    }

    private Train parseTrain(String line) {
        String[] p = line.split(",", -1);
        Route route = Route.fromCode(p[2]);
        Direction dir = Direction.fromCode(p[3]);
        List<String> order = new ArrayList<>(route.getStations());
        if (dir == Direction.INBOUND) Collections.reverse(order);

        List<Stop> stops = new ArrayList<>();
        stops.add(new Stop(order.get(0), LocalTime.parse(p[4].trim())));
        int lastIndex = 0;
        if (!p[7].trim().isEmpty()) {
            for (String s : p[7].split(";")) {
                String[] parts = s.split("@");
                int idx = indexIgnoreCase(order, parts[0]);
                if (idx <= lastIndex) {
                    throw new IllegalArgumentException("station '" + parts[0] + "' is not in route order");
                }
                lastIndex = idx;
                stops.add(new Stop(order.get(idx), LocalTime.parse(parts[1].trim())));
            }
        }
        stops.add(new Stop(order.get(order.size() - 1), LocalTime.parse(p[5].trim())));

        List<Carriage> carriages = new ArrayList<>();
        int number = 1;
        for (String c : p[6].split(";")) {
            String[] parts = c.split(":");
            carriages.add(new Carriage(number++, SeatClass.fromName(parts[0]), Integer.parseInt(parts[1].trim())));
        }
        return new Train(p[0].trim(), p[1].trim(), route, dir, stops, carriages);
    }

    private static int indexIgnoreCase(List<String> list, String name) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equalsIgnoreCase(name.trim())) return i;
        }
        throw new IllegalArgumentException("unknown station '" + name + "'");
    }

    /** Problems found while reading the file (invalid lines are skipped). */
    public List<String> getWarnings() { return warnings; }

    public List<Train> getAllTrains() { return new ArrayList<>(trains); }

    /** Trains of one route and direction, ordered by departure time. */
    public List<Train> getTrains(Route route, Direction direction) {
        return trains.stream()
                .filter(t -> t.getRoute() == route && t.getDirection() == direction)
                .sorted((a, b) -> a.getDeparture().compareTo(b.getDeparture()))
                .collect(Collectors.toList());
    }

    public Train findByNumber(String trainNo) {
        for (Train t : trains) {
            if (t.getTrainNo().equalsIgnoreCase(trainNo.trim())) return t;
        }
        return null;
    }
}
