package railway.ui;

import railway.model.Booking;
import railway.model.BookingStatus;
import railway.service.BookingException;
import railway.service.BookingService;
import railway.util.Validator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/** Search bookings, then alter or cancel the selected one. */
public class ManagePanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;

    private final transient BookingService service;
    private final transient StatusBar status;
    private final transient Consumer<Booking> onAlter;

    private final JTextField idField = new JTextField(10);
    private final JTextField nicField = new JTextField(12);
    private final JTable table = UiStyle.table("Booking", "Train", "Date", "Journey", "Class", "Seats", "Status", "Customer");
    private final JTextArea details = new JTextArea();
    private final JButton alterBtn = UiStyle.button("Alter selected booking");
    private final JButton cancelBtn = UiStyle.button("Cancel selected booking");

    private transient List<Booking> shown = new ArrayList<>();
    private transient Runnable lastSearch = this::showRecent;

    public ManagePanel(BookingService service, StatusBar status, Consumer<Booking> onAlter) {
        super(new BorderLayout(0, 8));
        this.service = service;
        this.status = status;
        this.onAlter = onAlter;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(buildSearchBar(), BorderLayout.NORTH);

        UiStyle.widths(table, 80, 60, 90, 250, 50, 170, 80, 140);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus,
                                                           int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, focus, row, col);
                boolean cancelled = row < shown.size() && shown.get(row).getStatus() == BookingStatus.CANCELLED;
                c.setForeground(sel ? t.getSelectionForeground() : cancelled ? Color.GRAY : t.getForeground());
                return c;
            }
        });
        table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) onSelect(); });

        details.setEditable(false);
        details.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        details.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.add(UiStyle.bold("Booking details"), BorderLayout.NORTH);
        bottom.add(new JScrollPane(details), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        alterBtn.addActionListener(e -> alterSelected());
        cancelBtn.addActionListener(e -> cancelSelected());
        buttons.add(alterBtn);
        buttons.add(cancelBtn);
        bottom.add(buttons, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, UiStyle.scroll(table), bottom);
        split.setResizeWeight(0.55);
        add(split, BorderLayout.CENTER);
        onSelect();
    }

    private JPanel buildSearchBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.add(new JLabel("Booking ID"));
        bar.add(idField);
        JButton byId = new JButton("Find");
        byId.addActionListener(e -> searchById());
        idField.addActionListener(e -> searchById());
        bar.add(byId);
        bar.add(new JLabel("     Customer ID (NIC)"));
        bar.add(nicField);
        JButton byNic = new JButton("Find bookings");
        byNic.addActionListener(e -> searchByNic());
        nicField.addActionListener(e -> searchByNic());
        bar.add(byNic);
        JButton all = new JButton("Show recent bookings");
        all.addActionListener(e -> { lastSearch = this::showRecent; lastSearch.run(); });
        bar.add(all);
        return bar;
    }

    // ---------------------------------------------------------------- search

    private void searchById() {
        String id = idField.getText().trim();
        if (id.isEmpty()) {
            status.error("Enter a booking ID such as BK00001.");
            return;
        }
        lastSearch = () -> {
            Booking b = service.findById(id);
            show(b == null ? new ArrayList<>() : List.of(b));
            if (b == null) status.error("No booking found with ID " + id.toUpperCase() + ".");
            else status.info("Booking " + b.getBookingId() + " found.");
        };
        lastSearch.run();
    }

    private void searchByNic() {
        String nic = nicField.getText().trim();
        if (!Validator.isValidId(nic)) {
            status.error("Invalid ID number. Use 9 digits + V/X or 12 digits.");
            return;
        }
        lastSearch = () -> {
            List<Booking> list = service.findByPassengerId(nic);
            show(list);
            if (list.isEmpty()) status.error("No bookings found for ID " + nic.toUpperCase() + ".");
            else status.info(list.size() + " booking(s) found for ID " + nic.toUpperCase() + ".");
        };
        lastSearch.run();
    }

    private void showRecent() {
        List<Booking> all = service.getAllBookings();
        all.sort(Comparator.comparing(Booking::getBookedAt).reversed());
        show(all);
        status.info(all.size() + " booking(s) on record.");
    }

    private void show(List<Booking> list) {
        shown = new ArrayList<>(list);
        UiStyle.model(table).setRowCount(0);
        for (Booking b : shown) {
            UiStyle.model(table).addRow(new Object[] {
                b.getBookingId(), b.getTrain().getTrainNo(), b.getTravelDate().format(Booking.DATE_FORMAT),
                b.journeyText(), b.getSeatClass().getShortName(), b.seatsAsText(), b.getStatus(),
                b.getPassenger().getName()});
        }
        if (!shown.isEmpty()) table.setRowSelectionInterval(0, 0);
        onSelect();
    }

    @Override
    public void refresh() {
        int row = table.getSelectedRow();
        lastSearch.run();
        if (row >= 0 && row < table.getRowCount()) table.setRowSelectionInterval(row, row);
    }

    // ---------------------------------------------------------------- actions

    private Booking selected() {
        int row = table.getSelectedRow();
        return row >= 0 && row < shown.size() ? shown.get(row) : null;
    }

    private void onSelect() {
        Booking b = selected();
        details.setText(b == null ? "Select a booking from the list." : b.details());
        details.setCaretPosition(0);
        boolean active = b != null && b.isActive();
        alterBtn.setEnabled(active);
        cancelBtn.setEnabled(active);
    }

    private void alterSelected() {
        Booking b = selected();
        if (b != null) onAlter.accept(b);
    }

    private void cancelSelected() {
        Booking b = selected();
        if (b == null) return;
        int answer = JOptionPane.showConfirmDialog(this,
                "Cancel booking " + b.getBookingId() + " (" + b.getPassenger().getName() + ", "
                        + b.journeyText() + ", seats " + b.seatsAsText() + ")?\nThe seats will become available again.",
                "Confirm cancellation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;
        try {
            service.cancelBooking(b.getBookingId());
            status.info("Booking " + b.getBookingId() + " cancelled. Seats " + b.seatsAsText() + " are free again.");
            refresh();
        } catch (BookingException e) {
            status.error(e.getMessage());
            JOptionPane.showMessageDialog(this, e.getMessage(), "Cancellation failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}
