package railway.ui;

import railway.model.Booking;
import railway.model.Carriage;
import railway.model.Seat;
import railway.model.SeatClass;
import railway.model.Train;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Shows the seat arrangement of a whole train: every carriage side by side
 * (like the train itself), with each seat drawn in the colour of its status.
 * All components are created in code; nothing comes from a GUI-builder canvas.
 */
public class SeatMapPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private enum State { FREE, BOOKED, SELECTED, UNAVAILABLE }

    private final JPanel carriageRow = new JPanel();
    private final JScrollPane scroll;
    private final Map<Seat, SeatCell> cells = new HashMap<>();
    private final Set<Seat> selected = new TreeSet<>();

    private Train train;
    private Map<Seat, List<Booking>> occupancy = Collections.emptyMap();
    private SeatClass selectableClass;          // null = view only
    private boolean selectionEnabled;
    private int maxSelection = 4;
    private SeatMapListener listener;

    public SeatMapPanel() {
        super(new BorderLayout());
        carriageRow.setLayout(new BoxLayout(carriageRow, BoxLayout.X_AXIS));
        carriageRow.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        scroll = new JScrollPane(carriageRow);
        scroll.getHorizontalScrollBar().setUnitIncrement(24);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        add(scroll, BorderLayout.CENTER);
        add(createLegend(), BorderLayout.SOUTH);
    }

    public void setListener(SeatMapListener listener) {
        this.listener = listener;
    }

    public void setMaxSelection(int max) {
        this.maxSelection = max;
    }

    /** Builds the carriages of a train. Call update(...) afterwards to colour the seats. */
    public void setTrain(Train train) {
        if (train == this.train) return;
        this.train = train;
        carriageRow.removeAll();
        cells.clear();
        selected.clear();
        if (train != null) {
            for (Carriage c : train.getCarriages()) {
                carriageRow.add(buildCarriage(c));
                carriageRow.add(Box.createHorizontalStrut(10));
            }
        }
        carriageRow.revalidate();
        carriageRow.repaint();
        scroll.getHorizontalScrollBar().setValue(0);
    }

    /**
     * Applies the occupancy of the chosen date/journey.
     * @param selectableClass the class the officer is booking (seats of other classes are greyed out),
     *                        or null for a read-only overview
     * @param selectionEnabled whether free seats can be clicked
     */
    public void update(Map<Seat, List<Booking>> occupancy, SeatClass selectableClass, boolean selectionEnabled) {
        this.occupancy = occupancy;
        this.selectableClass = selectableClass;
        this.selectionEnabled = selectionEnabled;
        int before = selected.size();
        selected.removeIf(s -> !isSelectable(s));
        refreshCells();
        if (selected.size() != before) fireSelectionChanged();
    }

    public List<Seat> getSelected() {
        return new ArrayList<>(selected);
    }

    public void setSelected(Collection<Seat> seats) {
        selected.clear();
        for (Seat s : seats) {
            if (isSelectable(s) && selected.size() < maxSelection) selected.add(s);
        }
        refreshCells();
        fireSelectionChanged();
    }

    public void clearSelection() {
        if (selected.isEmpty()) return;
        selected.clear();
        refreshCells();
        fireSelectionChanged();
    }

    // ---------------------------------------------------------------- building

    private JComponent buildCarriage(Carriage carriage) {
        SeatClass cls = carriage.getSeatClass();
        JPanel box = new JPanel(new BorderLayout(0, 4));
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(classColor(cls), 2, true),
                BorderFactory.createEmptyBorder(4, 6, 6, 6)));
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel header = new JLabel("<html><div style='text-align:center'><b>Carriage " + carriage.getNumber()
                + "</b><br>" + cls.getDisplayName() + " &middot; " + carriage.getSeatCount() + " seats</div></html>",
                SwingConstants.CENTER);
        header.setOpaque(true);
        header.setBackground(classColor(cls));
        box.add(header, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridBagLayout());
        int perRow = cls.getSeatsPerRow();
        int aisle = cls.getAisleAfter();
        for (int n = 1; n <= carriage.getSeatCount(); n++) {
            int row = (n - 1) / perRow;
            int col = (n - 1) % perRow;
            GridBagConstraints gc = new GridBagConstraints();
            gc.gridx = col < aisle ? col : col + 1;           // column "aisle" is the gangway
            gc.gridy = row;
            gc.insets = new java.awt.Insets(2, 2, 2, 2);
            Seat seat = new Seat(carriage.getNumber(), n);
            SeatCell cell = new SeatCell(seat);
            cells.put(seat, cell);
            grid.add(cell, gc);
        }
        GridBagConstraints gap = new GridBagConstraints();
        gap.gridx = aisle;
        gap.gridy = 0;
        grid.add(Box.createHorizontalStrut(16), gap);
        JPanel holder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        holder.add(grid);
        box.add(holder, BorderLayout.CENTER);
        return box;
    }

    private static Color classColor(SeatClass cls) {
        switch (cls) {
            case FIRST: return new Color(255, 224, 130);
            case SECOND: return new Color(179, 229, 252);
            default: return new Color(215, 215, 215);
        }
    }

    private static JComponent createLegend() {
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 4));
        legend.add(legendItem(UiStyle.FREE, "Free"));
        legend.add(legendItem(UiStyle.SELECTED, "Selected"));
        legend.add(legendItem(UiStyle.BOOKED, "Booked"));
        legend.add(legendItem(UiStyle.UNAVAILABLE, "Other class"));
        return legend;
    }

    private static JComponent legendItem(Color color, String text) {
        JLabel l = new JLabel(text);
        l.setIcon(new javax.swing.Icon() {
            @Override public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
                g.setColor(color);
                g.fillRoundRect(x, y, 16, 16, 5, 5);
            }
            @Override public int getIconWidth() { return 16; }
            @Override public int getIconHeight() { return 16; }
        });
        return l;
    }

    // ------------------------------------------------------------------ state

    private boolean isSelectable(Seat s) {
        if (!selectionEnabled || selectableClass == null || train == null) return false;
        return train.isSeatOfClass(s, selectableClass) && !occupancy.containsKey(s);
    }

    private State stateOf(Seat s) {
        if (selected.contains(s)) return State.SELECTED;
        if (occupancy.containsKey(s)) return State.BOOKED;
        if (selectableClass != null) {
            Carriage c = train.getCarriage(s.getCarriage());
            if (c.getSeatClass() != selectableClass) return State.UNAVAILABLE;
        }
        return State.FREE;
    }

    private void refreshCells() {
        for (SeatCell cell : cells.values()) cell.refresh();
        repaint();
    }

    private void fireSelectionChanged() {
        if (listener != null) listener.selectionChanged(getSelected());
    }

    private void seatClicked(Seat seat) {
        List<Booking> holders = occupancy.get(seat);
        if (holders != null) {
            if (listener != null) listener.bookedSeatClicked(seat, holders);
            return;
        }
        if (!isSelectable(seat)) return;
        if (selected.contains(seat)) {
            selected.remove(seat);
        } else {
            if (selected.size() >= maxSelection) {
                if (listener != null) listener.selectionLimitReached(maxSelection);
                return;
            }
            selected.add(seat);
        }
        refreshCells();
        fireSelectionChanged();
    }

    // -------------------------------------------------------------- one seat

    /** A single seat: a small rounded square with the seat number, drawn in code. */
    private class SeatCell extends JComponent {
        private static final long serialVersionUID = 1L;
        private final Seat seat;
        private State state = State.FREE;

        SeatCell(Seat seat) {
            this.seat = seat;
            setPreferredSize(new Dimension(32, 28));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    seatClicked(seat);
                }
            });
        }

        void refresh() {
            state = stateOf(seat);
            boolean clickable = state == State.BOOKED || state == State.SELECTED
                    || (state == State.FREE && isSelectable(seat));
            setCursor(clickable ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            List<Booking> holders = occupancy.get(seat);
            if (holders == null || holders.isEmpty()) {
                setToolTipText("Seat " + seat);
            } else {
                StringBuilder sb = new StringBuilder("<html><b>Seat " + seat + "</b>");
                for (Booking b : holders) {
                    sb.append("<br>").append(b.getBookingId()).append(" - ").append(b.getPassenger().getName())
                      .append("<br>&nbsp;&nbsp;").append(b.getFromStation()).append(" &rarr; ").append(b.getToStation());
                }
                setToolTipText(sb.append("</html>").toString());
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill;
            switch (state) {
                case BOOKED: fill = UiStyle.BOOKED; break;
                case SELECTED: fill = UiStyle.SELECTED; break;
                case UNAVAILABLE: fill = UiStyle.UNAVAILABLE; break;
                default: fill = UiStyle.FREE;
            }
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.setColor(fill.darker());
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.setColor(state == State.UNAVAILABLE ? Color.DARK_GRAY : Color.WHITE);
            g2.setFont(UiStyle.BASE.deriveFont(Font.BOLD, 11f));
            String text = String.valueOf(seat.getNumber());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }
}
