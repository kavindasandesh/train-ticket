package railway.ui;

import railway.model.Booking;
import railway.model.Passenger;
import railway.model.Seat;
import railway.model.SeatClass;
import railway.model.Train;
import railway.service.BookingException;
import railway.service.BookingService;
import railway.util.Validator;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * New booking screen. The same screen is used to alter a booking: the officer
 * picks "Alter" on the Manage Bookings tab and the booking is loaded here.
 */
public class BookingPanel extends JPanel implements Refreshable, SeatMapListener {
    private static final long serialVersionUID = 1L;

    private final transient BookingService service;
    private final transient StatusBar status;
    private final transient Runnable onEditFinished;

    private final TrainSelector selector;
    private final DateSelector dateBox = new DateSelector(0, BookingService.ADVANCE_BOOKING_DAYS);
    private final JComboBox<String> fromBox = new JComboBox<>();
    private final JComboBox<String> toBox = new JComboBox<>();
    private final JComboBox<SeatClass> classBox = new JComboBox<>();
    private final JLabel availability = new JLabel(" ");
    private final JLabel banner = new JLabel(" ");

    private final JTextField idField = new JTextField(14);
    private final JTextField nameField = new JTextField(14);
    private final JTextField contactField = new JTextField(14);

    private final JSpinner autoCount = new JSpinner(
            new SpinnerNumberModel(1, 1, BookingService.MAX_SEATS_PER_CUSTOMER, 1));
    private final JLabel selectedLabel = new JLabel(" ");
    private final SeatMapPanel seatMap = new SeatMapPanel();

    private final JButton confirmBtn = UiStyle.button("Confirm booking");
    private final JButton clearBtn = new JButton("Clear form");
    private final JButton cancelEditBtn = new JButton("Stop editing");

    private transient Booking editing;
    private boolean updating;

    public BookingPanel(BookingService service, StatusBar status, Runnable onEditFinished) {
        super(new BorderLayout(10, 0));
        this.service = service;
        this.status = status;
        this.onEditFinished = onEditFinished;
        this.selector = new TrainSelector(service.getTimetable(), true);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel left = buildForm();
        left.setPreferredSize(new Dimension(400, 100));
        add(left, BorderLayout.WEST);
        add(buildSeatArea(), BorderLayout.CENTER);

        seatMap.setListener(this);
        seatMap.setMaxSelection(BookingService.MAX_SEATS_PER_CUSTOMER);
        wireEvents();
        onTrainChanged();
    }

    // ---------------------------------------------------------------- layout

    private JPanel buildForm() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel form = new JPanel(new GridBagLayout());

        banner.setFont(UiStyle.BOLD);
        banner.setOpaque(true);
        banner.setHorizontalAlignment(JLabel.CENTER);
        banner.setVisible(false);
        UiStyle.addWide(form, 0, banner);

        JPanel journey = new JPanel(new GridBagLayout());
        journey.setBorder(BorderFactory.createTitledBorder("Journey"));
        UiStyle.addWide(journey, 0, selector);
        UiStyle.addRow(journey, 1, "Travel date", dateBox);
        UiStyle.addRow(journey, 2, "Boarding at", fromBox);
        UiStyle.addRow(journey, 3, "Alighting at", toBox);
        UiStyle.addRow(journey, 4, "Class", classBox);
        UiStyle.addWide(journey, 5, availability);
        UiStyle.addWide(form, 1, journey);

        JPanel customer = new JPanel(new GridBagLayout());
        customer.setBorder(BorderFactory.createTitledBorder("Customer"));
        UiStyle.addRow(customer, 0, "ID number (NIC)", idField);
        UiStyle.addRow(customer, 1, "Name", nameField);
        UiStyle.addRow(customer, 2, "Contact number", contactField);
        UiStyle.addWide(form, 2, customer);

        JPanel seats = new JPanel(new GridBagLayout());
        seats.setBorder(BorderFactory.createTitledBorder("Seats (max " + BookingService.MAX_SEATS_PER_CUSTOMER + ")"));
        JPanel auto = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        auto.add(new JLabel("Auto-select"));
        auto.add(autoCount);
        auto.add(new JLabel("seat(s)"));
        JButton autoBtn = new JButton("Auto-select");
        autoBtn.addActionListener(e -> autoSelect());
        auto.add(autoBtn);
        UiStyle.addWide(seats, 0, auto);
        selectedLabel.setFont(UiStyle.BOLD);
        UiStyle.addWide(seats, 1, selectedLabel);
        UiStyle.addWide(form, 3, seats);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        confirmBtn.addActionListener(e -> submit());
        clearBtn.addActionListener(e -> clearCustomerAndSeats());
        cancelEditBtn.addActionListener(e -> stopEditing(true));
        cancelEditBtn.setVisible(false);
        buttons.add(confirmBtn);
        buttons.add(clearBtn);
        buttons.add(cancelEditBtn);
        UiStyle.addWide(form, 4, buttons);

        panel.add(new JScrollPane(form, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildSeatArea() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.add(UiStyle.title("Seat arrangement - click free seats to select them"), BorderLayout.NORTH);
        p.add(seatMap, BorderLayout.CENTER);
        return p;
    }

    private void wireEvents() {
        selector.setListener(this::onTrainChanged);
        dateBox.addActionListener(e -> refreshSeats());
        fromBox.addActionListener(e -> { if (!updating) { fillToBox(null); refreshSeats(); } });
        toBox.addActionListener(e -> refreshSeats());
        classBox.addActionListener(e -> refreshSeats());
    }

    // ------------------------------------------------------- combo handling

    private void onTrainChanged() {
        Train train = selector.getTrain();
        seatMap.setTrain(train);
        updating = true;
        fromBox.removeAllItems();
        classBox.removeAllItems();
        if (train != null) {
            for (int i = 0; i < train.getStopCount() - 1; i++) fromBox.addItem(train.getStop(i).getStation());
            for (SeatClass c : train.getClasses()) classBox.addItem(c);
            fillToBox(null);
            toBox.setSelectedIndex(toBox.getItemCount() - 1);
        } else {
            toBox.removeAllItems();
        }
        updating = false;
        refreshSeats();
    }

    /** Alighting stations are the stops after the chosen boarding station. */
    private void fillToBox(String keep) {
        Train train = selector.getTrain();
        boolean was = updating;
        updating = true;
        Object previous = keep != null ? keep : toBox.getSelectedItem();
        toBox.removeAllItems();
        if (train != null && fromBox.getSelectedItem() != null) {
            int from = train.getStopIndex((String) fromBox.getSelectedItem());
            for (int i = from + 1; i < train.getStopCount(); i++) toBox.addItem(train.getStop(i).getStation());
            if (previous != null && ((DefaultComboBoxModel<String>) toBox.getModel()).getIndexOf(previous) >= 0) {
                toBox.setSelectedItem(previous);
            } else if (toBox.getItemCount() > 0) {
                toBox.setSelectedIndex(toBox.getItemCount() - 1);
            }
        }
        updating = was;
    }

    // ---------------------------------------------------------- seat refresh

    @Override
    public void refresh() {
        refreshSeats();
    }

    private void refreshSeats() {
        if (updating) return;
        Train train = selector.getTrain();
        String from = (String) fromBox.getSelectedItem();
        String to = (String) toBox.getSelectedItem();
        SeatClass cls = (SeatClass) classBox.getSelectedItem();
        if (train == null || from == null || to == null || cls == null) {
            seatMap.update(Collections.emptyMap(), null, false);
            availability.setText(" ");
            confirmBtn.setEnabled(false);
            return;
        }
        LocalDate date = dateBox.getDate();
        Map<Seat, List<Booking>> occupancy = service.getOccupancy(train, date, from, to, editing);
        seatMap.update(occupancy, cls, true);

        StringBuilder sb = new StringBuilder("<html>Free seats: ");
        for (SeatClass c : train.getClasses()) {
            int free = service.countFreeSeats(train, date, c, from, to, editing);
            sb.append(c.getShortName()).append(" ")
              .append(free == 0 ? "<font color='#B71C1C'><b>FULL</b></font>" : String.valueOf(free)).append("&nbsp;&nbsp; ");
        }
        availability.setText(sb.append("</html>").toString());

        boolean full = service.isFullyBooked(train, date, from, to, editing);
        confirmBtn.setEnabled(!full);
        if (full) status.error("Train " + train.getTrainNo() + " is FULLY BOOKED for this journey - no seat can be booked.");
        updateSelectedLabel();
    }

    private void updateSelectedLabel() {
        List<Seat> sel = seatMap.getSelected();
        selectedLabel.setText(sel.isEmpty() ? "Selected: none"
                : "Selected (" + sel.size() + "/" + BookingService.MAX_SEATS_PER_CUSTOMER + "): "
                  + sel.stream().map(Seat::toString).collect(Collectors.joining(", ")));
    }

    // --------------------------------------------------------- seat map events

    @Override
    public void selectionChanged(List<Seat> selected) {
        updateSelectedLabel();
    }

    @Override
    public void bookedSeatClicked(Seat seat, List<Booking> bookings) {
        Booking b = bookings.get(0);
        status.info("Seat " + seat + " is booked: " + b.getBookingId() + " - " + b.getPassenger().getName()
                + " (" + b.journeyText() + ")");
    }

    @Override
    public void selectionLimitReached(int max) {
        status.error("A customer can book at most " + max + " seats.");
    }

    // ---------------------------------------------------------------- actions

    private void autoSelect() {
        Train train = selector.getTrain();
        if (train == null) return;
        try {
            List<Seat> seats = service.suggestSeats(train, dateBox.getDate(), (SeatClass) classBox.getSelectedItem(),
                    (String) fromBox.getSelectedItem(), (String) toBox.getSelectedItem(),
                    (Integer) autoCount.getValue(), editing, editing != null ? editing.getSeats() : null);
            seatMap.setSelected(seats);
            status.info("Seats selected automatically: " + seats.stream().map(Seat::toString)
                    .collect(Collectors.joining(", ")));
        } catch (BookingException e) {
            status.error(e.getMessage());
            JOptionPane.showMessageDialog(this, e.getMessage(), "Cannot select seats", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void submit() {
        Train train = selector.getTrain();
        String from = (String) fromBox.getSelectedItem();
        String to = (String) toBox.getSelectedItem();
        SeatClass cls = (SeatClass) classBox.getSelectedItem();
        if (train == null || from == null || to == null || cls == null) {
            invalid("Please choose a train, stations and class.");
            return;
        }
        String id = editing != null ? editing.getPassenger().getIdNumber() : idField.getText().trim();
        String name = nameField.getText().trim();
        String contact = contactField.getText().trim();
        if (!Validator.isValidId(id)) {
            invalid("Invalid ID number. Use 9 digits followed by V or X (e.g. 912345678V), or 12 digits.");
            idField.requestFocus();
            return;
        }
        if (!Validator.isValidName(name)) {
            invalid("Invalid name. Use letters, spaces, '.' and '-' only (2-50 characters).");
            nameField.requestFocus();
            return;
        }
        if (!Validator.isValidContact(contact)) {
            invalid("Invalid contact number. Enter 10 digits starting with 0 (e.g. 0771234567).");
            contactField.requestFocus();
            return;
        }
        List<Seat> seats = seatMap.getSelected();
        if (seats.isEmpty()) {
            invalid("Select at least one seat on the seat map (or use Auto-select).");
            return;
        }
        try {
            if (editing == null) {
                Booking b = service.createBooking(new Passenger(id, name, contact), train, dateBox.getDate(),
                        from, to, cls, seats);
                showBooking("Booking confirmed", b);
                status.info("Booking " + b.getBookingId() + " confirmed.");
                clearCustomerAndSeats();
                refreshSeats();
            } else {
                Booking b = service.alterBooking(editing.getBookingId(), name, contact, dateBox.getDate(),
                        from, to, cls, seats);
                showBooking("Booking updated", b);
                status.info("Booking " + b.getBookingId() + " updated.");
                stopEditing(false);
            }
        } catch (BookingException e) {
            status.error(e.getMessage());
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    editing == null ? "Booking failed" : "Change failed", JOptionPane.ERROR_MESSAGE);
            refreshSeats();
        }
    }

    private void invalid(String message) {
        status.error(message);
        JOptionPane.showMessageDialog(this, message, "Please check the form", JOptionPane.WARNING_MESSAGE);
    }

    private void showBooking(String title, Booking b) {
        JTextArea area = new JTextArea(b.details());
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setBackground(new Color(245, 245, 245));
        JOptionPane.showMessageDialog(this, area, title + " (payment is not handled by this system)",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void clearCustomerAndSeats() {
        if (editing == null) {
            idField.setText("");
            nameField.setText("");
            contactField.setText("");
        }
        seatMap.clearSelection();
        updateSelectedLabel();
    }

    // ---------------------------------------------------------------- edit mode

    /** Loads a confirmed booking so the officer can change it. Returns false if it cannot be altered. */
    public boolean startEdit(Booking booking) {
        if (booking.getTravelDate().isBefore(LocalDate.now())) {
            status.error("Booking " + booking.getBookingId() + " is for a past date and cannot be altered.");
            return false;
        }
        editing = booking;
        selector.setSelectorEnabled(false);
        selector.setTrain(booking.getTrain());

        updating = true;
        dateBox.setDate(booking.getTravelDate());
        fromBox.setSelectedItem(booking.getFromStation());
        fillToBox(booking.getToStation());
        classBox.setSelectedItem(booking.getSeatClass());
        updating = false;

        idField.setText(booking.getPassenger().getIdNumber());
        idField.setEnabled(false);
        nameField.setText(booking.getPassenger().getName());
        contactField.setText(booking.getPassenger().getContactNumber());
        banner.setText("Editing booking " + booking.getBookingId());
        banner.setBackground(new Color(255, 236, 179));
        banner.setVisible(true);
        confirmBtn.setText("Save changes");
        cancelEditBtn.setVisible(true);
        clearBtn.setVisible(false);

        refreshSeats();
        seatMap.setSelected(booking.getSeats());
        status.info("Change the date, stations, class or seats, then press 'Save changes'.");
        return true;
    }

    private void stopEditing(boolean userCancelled) {
        editing = null;
        selector.setSelectorEnabled(true);
        idField.setEnabled(true);
        idField.setText("");
        nameField.setText("");
        contactField.setText("");
        banner.setVisible(false);
        confirmBtn.setText("Confirm booking");
        cancelEditBtn.setVisible(false);
        clearBtn.setVisible(true);
        seatMap.clearSelection();
        refreshSeats();
        if (userCancelled) status.info("Editing cancelled. The booking was not changed.");
        onEditFinished.run();
    }

    /** Used by the timetable screen: start a new booking on a given train. */
    public void preselect(Train train) {
        if (editing != null) {
            status.error("Finish or stop editing the current booking first.");
            return;
        }
        selector.setTrain(train);
    }
}
