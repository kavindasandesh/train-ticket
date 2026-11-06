package railway.ui;

import railway.model.Booking;
import railway.model.Seat;
import railway.model.SeatClass;
import railway.model.Train;
import railway.service.BookingService;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Full booking details of one train on one date: seat arrangement, class summary and passenger list. */
public class TrainViewPanel extends JPanel implements Refreshable, SeatMapListener {
    private static final long serialVersionUID = 1L;

    private final transient BookingService service;
    private final transient StatusBar status;
    private final TrainSelector selector;
    private final DateSelector dateBox = new DateSelector(14, BookingService.ADVANCE_BOOKING_DAYS);
    private final JLabel heading = new JLabel(" ");
    private final JTable summary = UiStyle.table("Class", "Carriages", "Seats", "Booked", "Free");
    private final JTable bookingTable = UiStyle.table("Booking", "Class", "Journey", "Seats", "Status",
            "Customer", "ID number", "Contact");
    private final SeatMapPanel seatMap = new SeatMapPanel();
    private transient List<Booking> shown = Collections.emptyList();

    public TrainViewPanel(BookingService service, StatusBar status) {
        super(new BorderLayout(10, 0));
        this.service = service;
        this.status = status;
        this.selector = new TrainSelector(service.getTimetable(), true);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel left = new JPanel(new BorderLayout(0, 8));
        JPanel form = new JPanel(new GridBagLayout());
        UiStyle.addWide(form, 0, selector);
        UiStyle.addRow(form, 1, "Travel date", dateBox);
        left.add(form, BorderLayout.NORTH);
        JPanel sum = new JPanel(new BorderLayout(0, 4));
        sum.add(UiStyle.bold("Seats by class (whole journey, any leg)"), BorderLayout.NORTH);
        UiStyle.widths(summary, 110, 70, 50, 60, 50);
        UiStyle.center(summary, 1, 2, 3, 4);
        sum.add(UiStyle.scroll(summary), BorderLayout.CENTER);
        summary.setPreferredScrollableViewportSize(new Dimension(360, 100));
        left.add(sum, BorderLayout.CENTER);
        left.setPreferredSize(new Dimension(400, 100));
        add(left, BorderLayout.WEST);

        JPanel top = new JPanel(new BorderLayout(0, 4));
        heading.setFont(UiStyle.TITLE);
        heading.setForeground(UiStyle.ACCENT);
        top.add(heading, BorderLayout.NORTH);
        top.add(seatMap, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 4));
        bottom.add(UiStyle.bold("Bookings for this train and date (click a booked seat to find its booking)"),
                BorderLayout.NORTH);
        UiStyle.widths(bookingTable, 80, 55, 230, 170, 90, 140, 110, 90);
        bottom.add(UiStyle.scroll(bookingTable), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
        split.setResizeWeight(0.62);
        add(split, BorderLayout.CENTER);

        seatMap.setListener(this);
        selector.setListener(this::refresh);
        dateBox.addActionListener(e -> refresh());
        refresh();
    }

    @Override
    public void refresh() {
        Train train = selector.getTrain();
        UiStyle.model(summary).setRowCount(0);
        UiStyle.model(bookingTable).setRowCount(0);
        if (train == null) {
            heading.setText("No train selected");
            seatMap.setTrain(null);
            return;
        }
        LocalDate date = dateBox.getDate();
        Map<Seat, List<Booking>> occupancy =
                service.getOccupancy(train, date, train.getOrigin(), train.getDestination(), null);
        seatMap.setTrain(train);
        seatMap.update(occupancy, null, false);
        heading.setText(train.getTrainNo() + " " + train.getName() + "   |   " + train.getJourneyLabel()
                + "   |   " + date.format(Booking.DATE_FORMAT));

        int totalBooked = 0;
        for (SeatClass c : train.getClasses()) {
            int cap = train.getSeatCount(c);
            int booked = 0;
            for (Seat s : occupancy.keySet()) {
                if (train.getCarriage(s.getCarriage()).getSeatClass() == c) booked++;
            }
            totalBooked += booked;
            UiStyle.model(summary).addRow(new Object[] {c.getDisplayName(), train.getCarriages(c).size(), cap, booked,
                    cap - booked == 0 ? "FULL" : String.valueOf(cap - booked)});
        }
        UiStyle.model(summary).addRow(new Object[] {"Total", train.getCarriages().size(), train.getTotalSeats(),
                totalBooked, train.getTotalSeats() - totalBooked});

        shown = service.findByTrainAndDate(train, date);
        for (Booking b : shown) {
            UiStyle.model(bookingTable).addRow(new Object[] {b.getBookingId(), b.getSeatClass().getShortName(),
                b.journeyText(), b.seatsAsText(), b.getStatus(), b.getPassenger().getName(),
                b.getPassenger().getIdNumber(), b.getPassenger().getContactNumber()});
        }
        status.info(shown.size() + " booking record(s) for train " + train.getTrainNo() + " on "
                + date.format(Booking.DATE_FORMAT) + ".");
    }

    // ---- seat map events (read-only view) ----

    @Override public void selectionChanged(List<Seat> selected) { }
    @Override public void selectionLimitReached(int max) { }

    @Override
    public void bookedSeatClicked(Seat seat, List<Booking> bookings) {
        Booking b = bookings.get(0);
        for (int i = 0; i < shown.size(); i++) {
            if (shown.get(i) == b) {
                bookingTable.setRowSelectionInterval(i, i);
                bookingTable.scrollRectToVisible(bookingTable.getCellRect(i, 0, true));
                break;
            }
        }
        status.info("Seat " + seat + " - " + b.getBookingId() + ", " + b.getPassenger().getName()
                + ", " + b.journeyText());
    }
}
