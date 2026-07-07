package org.example.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;

public class BottomBarPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient SimulationEngine engine;

    private final JLabel utilValue, waitingValue, turnaroundValue, throughputValue;
    private JLabel clockLabel;
    private final GanttPanel ganttBody;
    private DefaultTableModel tableModel;

    public BottomBarPanel(SimulationEngine engine) {
        super(new BorderLayout());
        this.engine = engine;
        setBackground(Colors.COLOR_BG);
        setBorder(new EmptyBorder(0, 0, 0, 0));

        JPanel stats = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 8));
        stats.setBackground(Colors.COLOR_PANEL);
        stats.setBorder(new MatteBorder(1, 0, 1, 0, Colors.COLOR_BORDER));
        utilValue = new JLabel("0%");
        waitingValue = new JLabel("0 t");
        turnaroundValue = new JLabel("0 t");
        throughputValue = new JLabel("0 proc/t");
        stats.add(statLabel("CPU Utilization:", utilValue));
        stats.add(statLabel("Avg Waiting:", waitingValue));
        stats.add(statLabel("Avg Turnaround:", turnaroundValue));
        stats.add(statLabel("Throughput:", throughputValue));

        JPanel lowerSplit = new JPanel(new BorderLayout());
        lowerSplit.setBackground(Colors.COLOR_BG);

        JPanel gantt = new JPanel(new BorderLayout());
        gantt.setBackground(Colors.COLOR_PANEL);
        gantt.setBorder(new CompoundBorder(new MatteBorder(0, 0, 0, 1, Colors.COLOR_BORDER), new EmptyBorder(8, 12, 8, 12)));
        gantt.setPreferredSize(new Dimension(800, 160));
        JLabel ganttTitle = new JLabel("GANTT CHART");
        ganttTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        gantt.add(ganttTitle, BorderLayout.NORTH);
        ganttBody = new GanttPanel();
        JScrollPane ganttScroll = new JScrollPane(ganttBody);
        ganttScroll.setBorder(null);
        gantt.add(ganttScroll, BorderLayout.CENTER);

        JPanel tablePanel = buildProcessTable();

        lowerSplit.add(gantt, BorderLayout.WEST);
        lowerSplit.add(tablePanel, BorderLayout.CENTER);

        add(stats, BorderLayout.NORTH);
        add(lowerSplit, BorderLayout.CENTER);

        refresh();
    }

    private JPanel statLabel(String label, JLabel valueLabel) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        p.setBackground(Colors.COLOR_PANEL);
        JLabel l = new JLabel(label);
        l.setForeground(Colors.COLOR_TEXT_GRAY);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        p.add(l);
        p.add(valueLabel);
        return p;
    }

    private JPanel buildProcessTable() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Colors.COLOR_PANEL);
        panel.setBorder(new EmptyBorder(8, 12, 8, 12));

        clockLabel = new JLabel();
        clockLabel.setForeground(Colors.COLOR_BLUE);
        clockLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        clockLabel.setBorder(new EmptyBorder(0, 0, 6, 0));
        panel.add(clockLabel, BorderLayout.NORTH);

        String[] cols = {"PID", "STATE", "ARRIVAL", "BURST", "REMAINING", "PRIORITY", "WAITING", "TURNAROUND"};
        tableModel = new DefaultTableModel(cols, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(tableModel);
        table.setRowHeight(24);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        table.setShowGrid(false);
        table.setFillsViewportHeight(true);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(700, 120));
        scroll.setBorder(null);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    public final void refresh() {
        int t = engine.getCurrentTime();
        int minutes = t / 60;
        int seconds = t % 60;
        clockLabel.setText(UiHelpers.iconText(String.format("⏱ Simulation Clock: %02d:%02d   (t = %d)", minutes, seconds, t)));

        int pct = (int) Math.round(engine.getCpuUtilization() * 100);
        utilValue.setText(String.format("%d%%", pct));
        waitingValue.setText(String.format("%.1f t", engine.getAvgWaiting()));
        turnaroundValue.setText(String.format("%.1f t", engine.getAvgTurnaround()));
        throughputValue.setText(String.format("%.2f proc/t", engine.getThroughput()));

        tableModel.setRowCount(0);
        for (Process p : engine.getAllProcesses()) {
            tableModel.addRow(new Object[]{
                p.pid, stateLabel(p.state), p.arrival, p.burst, Math.max(p.remaining, 0),
                p.priority, p.waitingTime, p.state == Process.State.FINISHED ? p.turnaroundTime : "-"
            });
        }

        ganttBody.refresh();
    }

    private String stateLabel(Process.State state) {
        switch (state) {
            case RUNNING: return "Running";
            case READY: return "Ready";
            case FINISHED: return "Finished";
            case NEW:
            default: return "Waiting";
        }
    }

    private class GanttPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private static final int CELL_W = 26;
        private static final int CELL_H = 22;

        GanttPanel() {
            setBackground(Colors.COLOR_PANEL);
        }

        void refresh() {
            List<Process[]> history = engine.getGanttHistory();
            int cores = engine.getNumCores();
            int width = Math.max(300, history.size() * CELL_W + 10);
            int height = Math.max(80, cores * CELL_H + 10);
            setPreferredSize(new Dimension(width, height));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            List<Process[]> history = engine.getGanttHistory();
            g.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            for (int t = 0; t < history.size(); t++) {
                Process[] slot = history.get(t);
                for (int c = 0; c < slot.length; c++) {
                    int x = t * CELL_W;
                    int y = c * CELL_H;
                    Process p = slot[c];
                    if (p != null) {
                        g.setColor(p.color);
                        g.fillRect(x, y, CELL_W - 1, CELL_H - 2);
                        g.setColor(java.awt.Color.WHITE);
                        g.drawString(p.pid, x + 2, y + CELL_H - 8);
                    } else {
                        g.setColor(Colors.COLOR_BORDER);
                        g.drawRect(x, y, CELL_W - 1, CELL_H - 2);
                    }
                }
            }
        }
    }
}
