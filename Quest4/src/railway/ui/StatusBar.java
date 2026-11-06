package railway.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;

public class StatusBar extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JLabel label = new JLabel(" ");

    public StatusBar() {
        super(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(190, 190, 190)),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        add(label, BorderLayout.CENTER);
    }

    public void info(String message) {
        label.setForeground(UiStyle.OK);
        label.setText(message);
    }

    public void error(String message) {
        label.setForeground(UiStyle.ERROR);
        label.setText(message);
    }

    public void clear() {
        label.setText(" ");
    }
}
