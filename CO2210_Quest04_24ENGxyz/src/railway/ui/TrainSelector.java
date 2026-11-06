package railway.ui;

import railway.data.TimetableRepository;
import railway.model.Direction;
import railway.model.Route;
import railway.model.Train;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.List;

/**
 * Route -> direction -> train chooser used by several screens.
 * The train box can be left out (the timetable screen only needs route and direction).
 */
public class TrainSelector extends JPanel {
    private static final long serialVersionUID = 1L;

    private final TimetableRepository timetable;
    private final JComboBox<Route> routeBox = new JComboBox<>(Route.values());
    private final JComboBox<Direction> directionBox = new JComboBox<>(Direction.values());
    private final JComboBox<Train> trainBox = new JComboBox<>();
    private final boolean withTrain;
    private Runnable listener = () -> { };
    private boolean updating;

    public TrainSelector(TimetableRepository timetable, boolean withTrain) {
        super(new GridBagLayout());
        this.timetable = timetable;
        this.withTrain = withTrain;

        directionBox.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof Direction) {
                    Route r = (Route) routeBox.getSelectedItem();
                    Direction d = (Direction) value;
                    l.setText(d.getLabel() + ": " + (r == null ? "" : r.getJourneyLabel(d)));
                }
                return l;
            }
        });

        int row = 0;
        UiStyle.addRow(this, row++, "Route", routeBox);
        UiStyle.addRow(this, row++, "Direction", directionBox);
        if (withTrain) UiStyle.addRow(this, row, "Train", trainBox);

        routeBox.addActionListener(e -> { if (!updating) reloadTrains(); });
        directionBox.addActionListener(e -> { if (!updating) reloadTrains(); });
        trainBox.addActionListener(e -> { if (!updating) listener.run(); });
        reloadTrains();
    }

    /** Called whenever the chosen route, direction or train changes. */
    public void setListener(Runnable listener) {
        this.listener = listener;
    }

    public Route getRoute() { return (Route) routeBox.getSelectedItem(); }
    public Direction getDirection() { return (Direction) directionBox.getSelectedItem(); }
    public Train getTrain() { return (Train) trainBox.getSelectedItem(); }

    /** Selects the route, direction and train of the given train (without firing the listener). */
    public void setTrain(Train train) {
        updating = true;
        routeBox.setSelectedItem(train.getRoute());
        directionBox.setSelectedItem(train.getDirection());
        fillTrains();
        trainBox.setSelectedItem(train);
        updating = false;
        listener.run();
    }

    public void setSelectorEnabled(boolean enabled) {
        routeBox.setEnabled(enabled);
        directionBox.setEnabled(enabled);
        trainBox.setEnabled(enabled);
    }

    private void reloadTrains() {
        updating = true;
        fillTrains();
        updating = false;
        listener.run();
    }

    private void fillTrains() {
        if (!withTrain) return;
        List<Train> trains = timetable.getTrains(getRoute(), getDirection());
        trainBox.setModel(new DefaultComboBoxModel<>(trains.toArray(new Train[0])));
    }
}
