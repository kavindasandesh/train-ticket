package railway;

import railway.data.BookingRepository;
import railway.data.TimetableRepository;
import railway.service.BookingService;
import railway.ui.MainFrame;
import railway.ui.UiStyle;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Sri Lanka Railways - Express Train Booking Management System (Swing GUI).
 * Usage: java railway.Main [dataFolder]      (default folder: data)
 */
public class Main {
    public static void main(String[] args) {
        String dir = findDataFolder(args.length > 0 ? args[0] : "data");
        SwingUtilities.invokeLater(() -> start(dir));
    }

    /** Uses the given folder if it holds trains.csv, otherwise tries the parent folder. */
    private static String findDataFolder(String preferred) {
        for (String d : new String[] {preferred, "../" + preferred}) {
            if (Files.exists(Paths.get(d, "trains.csv"))) return d;
        }
        return preferred;
    }

    private static void start(String dir) {
        UiStyle.install();
        try {
            TimetableRepository timetable = new TimetableRepository(dir + "/trains.csv");
            BookingRepository bookingRepo = new BookingRepository(dir + "/bookings.csv");
            BookingService service = new BookingService(timetable, bookingRepo);

            List<String> warnings = new ArrayList<>(timetable.getWarnings());
            warnings.addAll(bookingRepo.getWarnings());
            new MainFrame(service).setVisible(true);
            if (!warnings.isEmpty()) {
                JOptionPane.showMessageDialog(null, String.join("\n", warnings),
                        "Some data lines were skipped", JOptionPane.WARNING_MESSAGE);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                    "Cannot read the timetable file:\n" + Paths.get(dir, "trains.csv").toAbsolutePath()
                    + "\n\nRun the program from the project folder (the one that contains the 'data' folder).",
                    "Timetable not found", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}
