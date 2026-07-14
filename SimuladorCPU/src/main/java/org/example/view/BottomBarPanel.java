package org.example.view;

import java.awt.BorderLayout;
import java.awt.Color;
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

import org.example.controller.Controlador;
import org.example.model.Proceso;
import org.example.model.Queue;

public class BottomBarPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient Controlador controlador;

    private final JLabel utilValue, waitingValue, turnaroundValue, throughputValue;
    private JLabel clockLabel;
    private final GanttPanel ganttBody;
    private DefaultTableModel tableModel;
    private final LogPanel logPanel;

    public BottomBarPanel(Controlador controlador) {
        super(new BorderLayout());
        this.controlador = controlador;
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
        logPanel = new LogPanel(controlador);

        lowerSplit.add(gantt, BorderLayout.WEST);
        lowerSplit.add(tablePanel, BorderLayout.CENTER);
        lowerSplit.add(logPanel, BorderLayout.EAST);

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
        int t = controlador.getCurrentTime();
        int minutes = t / 60;
        int seconds = t % 60;
        clockLabel.setText(UiHelpers.iconText(String.format("⏱ Simulation Clock: %02d:%02d   (t = %d)", minutes, seconds, t)));

        int pct = (int) Math.round(controlador.getCpuUtilization() * 100);
        utilValue.setText(String.format("%d%%", pct));
        waitingValue.setText(String.format("%.1f t", controlador.getAvgWaiting()));
        turnaroundValue.setText(String.format("%.1f t", controlador.getAvgTurnaround()));
        throughputValue.setText(String.format("%.2f proc/t", controlador.getThroughput()));

        tableModel.setRowCount(0);
        for (Proceso p : controlador.getTodosLosProcesos()) {
            tableModel.addRow(new Object[]{
                p.getNombre(), stateLabel(p.getEstado()), p.getTiempoLlegada(), p.getTiempoRafaga(), p.getTiempoRestante(),
                p.getPrioridad(), p.getTiempoEsperaAcumulado(),
                Queue.ESTADO_TERMINADO.equals(p.getEstado()) ? p.getTiempoRetorno() : "-"
            });
        }

        ganttBody.refresh();
        logPanel.refresh();
    }

    private String stateLabel(String estado) {
        switch (estado) {
            case Queue.ESTADO_EJECUTANDO: return "Running";
            case Queue.ESTADO_LISTO: return "Ready";
            case Queue.ESTADO_TERMINADO: return "Finished";
            case Queue.ESTADO_BLOQUEADO: return "Blocked";
            case Queue.ESTADO_NUEVO:
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
            List<String[]> history = controlador.getGanttHistory();
            int cores = Math.max(1, controlador.getNumCores());
            int maxTime = controlador.getCurrentTime();
            for (String[] entry : history) {
                int t = Integer.parseInt(entry[0]);
                if (t > maxTime) maxTime = t;
            }
            int width = Math.max(300, (maxTime + 1) * CELL_W + 10);
            int height = Math.max(80, cores * CELL_H + 10);
            setPreferredSize(new Dimension(width, height));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            List<String[]> history = controlador.getGanttHistory();
            g.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            for (String[] entry : history) {
                int t = Integer.parseInt(entry[0]);
                int pid = Integer.parseInt(entry[1]);
                int core = Integer.parseInt(entry[3]);
                int x = t * CELL_W;
                int y = core * CELL_H;
                Color color = controlador.getColor(pid);
                g.setColor(color);
                g.fillRect(x, y, CELL_W - 1, CELL_H - 2);
                g.setColor(Color.WHITE);
                g.drawString("P" + pid, x + 2, y + CELL_H - 8);
            }
        }
    }
}
