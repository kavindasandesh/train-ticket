package railway.ui;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import java.awt.Component;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Drop-down of consecutive dates, so the officer can never type an invalid date. */
public class DateSelector extends JComboBox<LocalDate> {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("EEE dd-MM-yyyy", Locale.ENGLISH);

    public DateSelector(int daysBack, int daysAhead) {
        LocalDate today = LocalDate.now();
        for (int i = -daysBack; i <= daysAhead; i++) addItem(today.plusDays(i));
        setSelectedItem(today);
        setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof LocalDate) {
                    LocalDate d = (LocalDate) value;
                    l.setText(FORMAT.format(d) + (d.equals(LocalDate.now()) ? "  (today)" : ""));
                }
                return l;
            }
        });
    }

    public LocalDate getDate() {
        return (LocalDate) getSelectedItem();
    }

    /** Selects a date if it is inside the offered range; returns false otherwise. */
    public boolean setDate(LocalDate date) {
        for (int i = 0; i < getItemCount(); i++) {
            if (getItemAt(i).equals(date)) {
                setSelectedIndex(i);
                return true;
            }
        }
        return false;
    }
}
