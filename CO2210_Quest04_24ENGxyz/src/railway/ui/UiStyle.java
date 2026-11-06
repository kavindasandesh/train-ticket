package railway.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;

/** Colours, fonts and small helpers shared by all screens. */
public final class UiStyle {
    public static final Color FREE = new Color(102, 187, 106);
    public static final Color BOOKED = new Color(229, 115, 115);
    public static final Color SELECTED = new Color(41, 121, 255);
    public static final Color UNAVAILABLE = new Color(200, 200, 200);
    public static final Color ACCENT = new Color(0, 87, 146);
    public static final Color ERROR = new Color(183, 28, 28);
    public static final Color OK = new Color(27, 94, 32);
    public static final Font BASE = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font BOLD = BASE.deriveFont(Font.BOLD);
    public static final Font TITLE = BASE.deriveFont(Font.BOLD, 16f);

    private UiStyle() { }

    /** Uses the Nimbus look and feel when it exists, otherwise the system one. */
    public static void install() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // keep the default look and feel
        }
        for (Object key : UIManager.getDefaults().keySet()) {
            if (key.toString().endsWith(".font")) UIManager.put(key, BASE);
        }
    }

    public static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(TITLE);
        l.setForeground(ACCENT);
        return l;
    }

    public static JLabel bold(String text) {
        JLabel l = new JLabel(text);
        l.setFont(BOLD);
        return l;
    }

    public static JButton button(String text) {
        JButton b = new JButton(text);
        b.setFont(BOLD);
        b.setFocusPainted(false);
        return b;
    }

    /** Adds "label : component" as one row of a GridBagLayout form. */
    public static void addRow(JPanel form, int row, String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 4, 3, 8);
        form.add(new JLabel(label), c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = row;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 0, 3, 4);
        form.add(field, c);
    }

    /** Adds a component spanning both columns of the form. */
    public static void addWide(JPanel form, int row, JComponent comp) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 4, 4, 4);
        form.add(comp, c);
    }

    /** A read-only table with the given column names. */
    public static JTable table(String... columns) {
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable t = new JTable(model);
        t.setRowHeight(24);
        t.setFillsViewportHeight(true);
        t.getTableHeader().setReorderingAllowed(false);
        return t;
    }

    public static DefaultTableModel model(JTable t) {
        return (DefaultTableModel) t.getModel();
    }

    public static JScrollPane scroll(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createLineBorder(new Color(190, 190, 190)));
        return sp;
    }

    /** Centres the text of the given table columns. */
    public static void center(JTable t, int... columns) {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setHorizontalAlignment(SwingConstants.CENTER);
        for (int c : columns) t.getColumnModel().getColumn(c).setCellRenderer(r);
    }

    public static void widths(JTable t, int... widths) {
        for (int i = 0; i < widths.length; i++) t.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
    }
}
