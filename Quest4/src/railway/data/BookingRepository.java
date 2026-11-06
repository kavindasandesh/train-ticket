package railway.data;

import railway.model.Booking;
import railway.model.BookingStatus;
import railway.model.Passenger;
import railway.model.Seat;
import railway.model.SeatClass;
import railway.model.Train;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Reads and writes booking records in bookings.csv
public class BookingRepository {
    private static final String HEADER =
            "bookingId,idNumber,name,contact,trainNo,travelDate,fromStation,toStation,class,seats,status,bookedAt";
    private final Path file;
    private final List<String> warnings = new ArrayList<>();

    public BookingRepository(String filePath) {
        this.file = Paths.get(filePath);
    }

    public List<Booking> loadAll(TimetableRepository timetable) {
        List<Booking> result = new ArrayList<>();
        if (!Files.exists(file)) {
            return result;    
        }
        try {
            List<String> lines = Files.readAllLines(file);
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;
                try {
                    String[] p = line.split(",");
                    Train train = timetable.findByNumber(p[4]);
                    if (train == null) throw new IllegalArgumentException("unknown train " + p[4]);
                    if (train.getStopIndex(p[6]) < 0 || train.getStopIndex(p[7]) < 0
                            || train.getStopIndex(p[6]) >= train.getStopIndex(p[7])) {
                        throw new IllegalArgumentException("invalid journey " + p[6] + " -> " + p[7]);
                    }
                    List<Seat> seats = new ArrayList<>();
                    for (String s : p[9].split(";")) seats.add(Seat.parse(s));
                    result.add(new Booking(p[0], new Passenger(p[1], p[2], p[3]), train,
                            LocalDate.parse(p[5]), p[6].trim(), p[7].trim(), SeatClass.fromName(p[8]),
                            seats, BookingStatus.valueOf(p[10]), LocalDateTime.parse(p[11])));
                } catch (RuntimeException e) {
                    warnings.add("Booking line " + (i + 1) + " skipped: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            warnings.add("Cannot read booking file " + file + " (" + e.getMessage() + ")");
        }
        return result;
    }

    public List<String> getWarnings() { return warnings; }

    public boolean saveAll(List<Booking> bookings) {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (Booking b : bookings) lines.add(b.toCsv());
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            Files.write(file, lines);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
