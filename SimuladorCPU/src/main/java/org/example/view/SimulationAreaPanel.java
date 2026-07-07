package org.example.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class SimulationAreaPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient SimulationEngine engine;

    private JPanel readyContent;
    private JLabel readyInfo;
    private JPanel runningContent;
    private JLabel runningInfo;
    private JPanel waitingContent;
    private JLabel waitingInfo;
    private JPanel finishedContent;
    private JLabel finishedInfo;
    private final JLabel simInfoLabel;

    public SimulationAreaPanel(SimulationEngine engine) {
        super(new BorderLayout());
        this.engine = engine;
        setBackground(Colors.COLOR_BG);
        setBorder(new EmptyBorder(12, 12, 0, 12));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Colors.COLOR_BG);
        header.setBorder(new EmptyBorder(0, 4, 10, 4));
        JLabel title = new JLabel("CPU Simulation Area");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.add(title, BorderLayout.WEST);
        simInfoLabel = new JLabel();
        simInfoLabel.setForeground(Colors.COLOR_TEXT_GRAY);
        header.add(simInfoLabel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setBackground(Colors.COLOR_BG);

        stack.add(buildReadyQueue());
        stack.add(Box.createVerticalStrut(10));
        stack.add(buildRunningProcess());
        stack.add(Box.createVerticalStrut(10));
        stack.add(buildWaitingQueue());
        stack.add(Box.createVerticalStrut(10));
        stack.add(buildFinishedProcesses());

        add(stack, BorderLayout.CENTER);

        refresh();
    }

    private JPanel sectionPanel(String title, String rightInfoText, JLabel rightInfoLabel, int height, Color bg, Component content) {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setAlignmentX(Component.LEFT_ALIGNMENT);
        outer.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        outer.setBorder(new LineBorder(Colors.COLOR_BORDER));
        outer.setBackground(Colors.COLOR_PANEL);

        JPanel head = new JPanel(new BorderLayout());
        head.setBackground(bg);
        head.setBorder(new EmptyBorder(8, 12, 8, 12));
        JLabel lbl = new JLabel(UiHelpers.iconText(new StringBuilder().append("● ").append(title).toString()));
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        head.add(lbl, BorderLayout.WEST);
        rightInfoLabel.setText(rightInfoText);
        rightInfoLabel.setForeground(Colors.COLOR_TEXT_GRAY);
        head.add(rightInfoLabel, BorderLayout.EAST);

        outer.add(head, BorderLayout.NORTH);
        outer.add(content, BorderLayout.CENTER);
        return outer;
    }

    private JPanel buildReadyQueue() {
        readyContent = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        readyContent.setBackground(Colors.COLOR_PANEL);
        readyInfo = new JLabel();
        return sectionPanel("Ready Queue", "0", readyInfo, 90, new Color(0xEA, 0xF1, 0xFD), readyContent);
    }

    private JPanel buildRunningProcess() {
        runningContent = new JPanel();
        runningContent.setBackground(Colors.COLOR_PANEL);
        runningContent.setBorder(new EmptyBorder(10, 10, 10, 10));
        runningInfo = new JLabel();
        return sectionPanel("Running Process", "0", runningInfo, 110, new Color(0xE9, 0xF7, 0xEA), runningContent);
    }

    private JPanel coreCard(int index, Process p) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Colors.COLOR_BG);
        card.setBorder(new LineBorder(Colors.COLOR_BORDER));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(6, 10, 0, 10));
        JLabel lbl = new JLabel(new StringBuilder().append("Core ").append(index).toString());
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        top.add(lbl, BorderLayout.WEST);
        card.add(top, BorderLayout.NORTH);
        JLabel state;
        if (p != null) {
            state = new JLabel(new StringBuilder().append(p.pid).append("  (").append(p.remaining).append("t left)").toString(), SwingConstants.CENTER);
            state.setForeground(p.color);
            state.setFont(new Font("Segoe UI", Font.BOLD, 12));
        } else {
            state = new JLabel("Idle", SwingConstants.CENTER);
            state.setForeground(Colors.COLOR_TEXT_GRAY);
        }
        card.add(state, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildWaitingQueue() {
        waitingContent = new JPanel(new BorderLayout());
        waitingContent.setBackground(Colors.COLOR_PANEL);
        waitingContent.setBorder(new EmptyBorder(15, 15, 15, 15));
        waitingInfo = new JLabel();
        return sectionPanel("Waiting Queue (I/O)", "0", waitingInfo, 90, new Color(0xFF, 0xF6, 0xE5), waitingContent);
    }

    private JPanel buildFinishedProcesses() {
        finishedContent = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        finishedContent.setBackground(Colors.COLOR_PANEL);
        finishedInfo = new JLabel();
        return sectionPanel("Finished Processes", "0", finishedInfo, 90, new Color(0xF0, 0xF0, 0xF0), finishedContent);
    }

    private JPanel finishedChip(Process p) {
        JPanel chip = new JPanel();
        chip.setLayout(new BoxLayout(chip, BoxLayout.Y_AXIS));
        chip.setBackground(Colors.COLOR_BG);
        chip.setBorder(new CompoundBorder(new LineBorder(Colors.COLOR_BORDER), new EmptyBorder(5, 10, 5, 10)));
        JLabel name = new JLabel(UiHelpers.iconText(new StringBuilder().append("✓ ").append(p.pid).toString()));
        name.setFont(new Font("Segoe UI", Font.BOLD, 11));
        name.setForeground(Colors.COLOR_GREEN);
        JLabel sub = new JLabel(new StringBuilder().append("TT:").append(p.turnaroundTime).append("t  W:").append(p.waitingTime).append("t").toString());
        sub.setForeground(Colors.COLOR_TEXT_GRAY);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        chip.add(name);
        chip.add(sub);
        return chip;
    }

    public final void refresh() {
        StringBuilder sb = new StringBuilder();
        sb.append(engine.getAlgorithm().name()).append(" · ").append(engine.getNumCores()).append(" Cores · Quantum = ").append(engine.getQuantum());
        simInfoLabel.setText(sb.toString());

        List<Process> ready = engine.getReadyQueue();
        readyContent.removeAll();
        if (ready.isEmpty()) {
            JLabel empty = new JLabel("Queue is empty");
            empty.setForeground(Colors.COLOR_TEXT_GRAY);
            readyContent.add(empty);
        } else {
            for (Process p : ready) {
                JLabel chip = new JLabel(new StringBuilder().append(p.pid).append(" (rem ").append(p.remaining).append("t)").toString());
                chip.setOpaque(true);
                chip.setBackground(Color.WHITE);
                chip.setForeground(p.color);
                chip.setBorder(new CompoundBorder(new LineBorder(p.color), new EmptyBorder(4, 8, 4, 8)));
                readyContent.add(chip);
            }
        }
        readyInfo.setText(String.valueOf(ready.size()));
        readyContent.revalidate();
        readyContent.repaint();

        Process[] running = engine.getRunning();
        runningContent.removeAll();
        runningContent.setLayout(new GridLayout(1, running.length, 10, 0));
        int busy = 0;
        for (int i = 0; i < running.length; i++) {
            runningContent.add(coreCard(i, running[i]));
            if (running[i] != null) busy++;
        }
        runningInfo.setText(String.valueOf(busy));
        runningContent.revalidate();
        runningContent.repaint();

        waitingInfo.setText("0");

        List<Process> finished = engine.getFinishedProcesses();
        finishedContent.removeAll();
        for (Process p : finished) {
            finishedContent.add(finishedChip(p));
        }
        finishedInfo.setText(String.valueOf(finished.size()));
        finishedContent.revalidate();
        finishedContent.repaint();
    }
}
