package org.example.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;

public class LogPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient SimulationEngine engine;
    private final JTextArea logArea;

    public LogPanel(SimulationEngine engine) {
        super(new BorderLayout());
        this.engine = engine;
        setBackground(Colors.COLOR_PANEL);
        setBorder(new CompoundBorder(
            new MatteBorder(0, 1, 0, 0, Colors.COLOR_BORDER),
            new EmptyBorder(8, 12, 8, 12)
        ));
        setPreferredSize(new Dimension(230, 160));

        JLabel title = new JLabel("EVENT LOG");
        title.setFont(new Font("Segoe UI", Font.BOLD, 11));
        title.setForeground(Colors.COLOR_TEXT_GRAY);
        title.setBorder(new EmptyBorder(0, 0, 6, 0));
        add(title, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 10));
        logArea.setBackground(Colors.COLOR_BG);
        logArea.setForeground(Colors.COLOR_TEXT_GRAY);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(false);

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(new LineBorder(Colors.COLOR_BORDER));
        add(scroll, BorderLayout.CENTER);

        refresh();
    }

    public void refresh() {
        List<String> logs = engine.getEventLog();
        StringBuilder sb = new StringBuilder();
        for (int i = logs.size() - 1; i >= 0; i--) {
            sb.append(logs.get(i)).append('\n');
        }
        logArea.setText(sb.toString());
        logArea.setCaretPosition(0);
    }
}
