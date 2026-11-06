package railway.ui;

import railway.model.Booking;
import railway.model.Train;
import railway.service.BookingService;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

/** The single main window of the booking application. */
public class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private final transient BookingService service;
    private final StatusBar status = new StatusBar();
    private final JTabbedPane tabs = new JTabbedPane();
    private final TimetablePanel timetablePanel;
    private final BookingPanel bookingPanel;
    private final ManagePanel managePanel;
    private final TrainViewPanel trainViewPanel;

    public MainFrame(BookingService service) {
        super("Sri Lanka Railways - Express Train Booking (Ticketing Officer)");
        this.service = service;

        bookingPanel = new BookingPanel(service, status, this::editFinished);
        managePanel = new ManagePanel(service, status, this::alterBooking);
        trainViewPanel = new TrainViewPanel(service, status);
        timetablePanel = new TimetablePanel(service, this::bookTrain);

        tabs.addTab("Timetable", timetablePanel);
        tabs.addTab("New Booking", bookingPanel);
        tabs.addTab("Manage Bookings", managePanel);
        tabs.addTab("Train Bookings", trainViewPanel);
        tabs.setFont(UiStyle.BOLD);
        tabs.addChangeListener(e -> {
            Component c = tabs.getSelectedComponent();
            if (c instanceof Refreshable) ((Refreshable) c).refresh();
        });

        setJMenuBar(buildMenu());
        setLayout(new BorderLayout());
        add(tabs, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 680));
        setSize(1320, 800);
        setLocationRelativeTo(null);
        status.info("Welcome. Use the tabs above to view the timetable, make or manage bookings.");
    }

    private JMenuBar buildMenu() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem demo = new JMenuItem("Add demo bookings");
        demo.addActionListener(e -> {
            int n = service.addDemoBookings();
            status.info(n + " demo booking(s) added (dated in the next few days).");
            Component c = tabs.getSelectedComponent();
            if (c instanceof Refreshable) ((Refreshable) c).refresh();
        });
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> dispose());
        file.add(demo);
        file.addSeparator();
        file.add(exit);

        JMenu help = new JMenu("Help");
        JMenuItem guide = new JMenuItem("How to use");
        guide.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Timetable        : trains, halts, carriages (outbound and return).\n"
              + "New Booking      : choose train, date, stations and class, enter the customer details,\n"
              + "                   click free (green) seats or use Auto-select, then Confirm.\n"
              + "Manage Bookings  : search by booking ID or customer ID, then Alter or Cancel.\n"
              + "Train Bookings   : seat arrangement and all bookings of a train on a date.\n\n"
              + "Seat colours: green = free, blue = selected, red = booked, grey = other class.\n"
              + "A customer can hold at most 4 seats on a train and date.\n"
              + "Seats can be re-sold for a part of the journey nobody else is using.",
                "How to use", JOptionPane.INFORMATION_MESSAGE));
        help.add(guide);
        bar.add(file);
        bar.add(help);
        return bar;
    }

    private void bookTrain(Train train) {
        bookingPanel.preselect(train);
        tabs.setSelectedComponent(bookingPanel);
    }

    private void alterBooking(Booking booking) {
        if (bookingPanel.startEdit(booking)) tabs.setSelectedComponent(bookingPanel);
    }

    private void editFinished() {
        tabs.setSelectedComponent(managePanel);
    }
}
