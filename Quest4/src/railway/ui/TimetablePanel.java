package railway.ui;

import railway.data.TimetableRepository;
import railway.model.Carriage;
import railway.model.Stop;
import railway.model.Train;
import railway.service.BookingService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// Express train timetable by route and direction
public class TimetablePanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;

    private final transient TimetableRepository timetable;
    private final TrainSelector selector;
    private final JTable trains = UiStyle.table("Train", "Name", "Departs", "Arrives", "Carriages", "Seats");
    private final JTable stops = UiStyle.table("Station", "Time");
    private final JTable carriages = UiStyle.table("Carriage", "Class", "Seats");
    private final JButton bookBtn = UiStyle.button("Book this train");
    private transient List<Train> shown = new ArrayList<>();

    public TimetablePanel(BookingService service, Consumer<Train> onBook) {
        super(new BorderLayout(10, 0));
        this.timetable = service.getTimetable();
        this.selector = new TrainSelector(timetable, false);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel left = new JPanel(new BorderLayout(0, 8));
        JPanel form = new JPanel(new GridBagLayout());
        UiStyle.addWide(form, 0, selector);
        left.add(form, BorderLayout.NORTH);
        JPanel hint = new JPanel(new FlowLayout(FlowLayout.LEFT));
        hint.add(new JLabel("<html>All express trains run daily.<br>Select a train to see its halts<br>and carriages.</html>"));
        left.add(hint, BorderLayout.CENTER);
        left.setPreferredSize(new Dimension(400, 100));
        add(left, BorderLayout.WEST);

        UiStyle.widths(trains, 60, 190, 70, 70, 80, 60);
        UiStyle.center(trains, 0, 2, 3, 4, 5);
        JPanel details = new JPanel(new GridLayout(1, 2, 10, 0));
        JPanel stopsBox = new JPanel(new BorderLayout(0, 4));
        stopsBox.add(UiStyle.bold("Halts and times"), BorderLayout.NORTH);
        stopsBox.add(UiStyle.scroll(stops), BorderLayout.CENTER);
        JPanel carBox = new JPanel(new BorderLayout(0, 4));
        carBox.add(UiStyle.bold("Carriages"), BorderLayout.NORTH);
        carBox.add(UiStyle.scroll(carriages), BorderLayout.CENTER);
        details.add(stopsBox);
        details.add(carBox);

        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.add(details, BorderLayout.CENTER);
        JPanel btn = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bookBtn.addActionListener(e -> {
            int row = trains.getSelectedRow();
            if (row >= 0 && row < shown.size()) onBook.accept(shown.get(row));
        });
        btn.add(bookBtn);
        bottom.add(btn, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, UiStyle.scroll(trains), bottom);
        split.setResizeWeight(0.4);
        add(split, BorderLayout.CENTER);

        selector.setListener(this::loadTrains);
        trains.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) showDetails(); });
        loadTrains();
    }

    private void loadTrains() {
        shown = timetable.getTrains(selector.getRoute(), selector.getDirection());
        UiStyle.model(trains).setRowCount(0);
        for (Train t : shown) {
            UiStyle.model(trains).addRow(new Object[] {t.getTrainNo(), t.getName(),
                t.getDeparture().format(Train.TIME_FORMAT), t.getArrival().format(Train.TIME_FORMAT),
                t.getCarriages().size(), t.getTotalSeats()});
        }
        if (!shown.isEmpty()) trains.setRowSelectionInterval(0, 0);
        showDetails();
    }

    private void showDetails() {
        UiStyle.model(stops).setRowCount(0);
        UiStyle.model(carriages).setRowCount(0);
        int row = trains.getSelectedRow();
        bookBtn.setEnabled(row >= 0);
        if (row < 0 || row >= shown.size()) return;
        Train t = shown.get(row);
        for (Stop s : t.getStops()) {
            UiStyle.model(stops).addRow(new Object[] {s.getStation(), s.getTime().format(Train.TIME_FORMAT)});
        }
        for (Carriage c : t.getCarriages()) {
            UiStyle.model(carriages).addRow(new Object[] {c.getNumber(), c.getSeatClass().getDisplayName(), c.getSeatCount()});
        }
    }

    @Override
    public void refresh() {
    }
}
