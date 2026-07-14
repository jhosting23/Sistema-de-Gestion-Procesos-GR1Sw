package org.example.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;

import org.example.controller.Controlador;

public class TopBarPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient Controlador controlador;

    private final JButton btnStart;
    private JButton btnPause, btnStop, btnReset;
    private JLabel clockLabel, statusLabel;

    public TopBarPanel(Controlador controlador) {
        super(new BorderLayout());
        this.controlador = controlador;
        setBackground(Colors.COLOR_PANEL);
        setBorder(new MatteBorder(0, 0, 1, 0, Colors.COLOR_BORDER));
        setPreferredSize(new Dimension(100, 50));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        left.setBackground(Colors.COLOR_PANEL);
        JLabel title = new JLabel(UiHelpers.iconText("▣  CPU Scheduler Sim"));
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        left.add(title);
        btnStart = makeTopButton("▷ Start", false);
        btnPause = makeTopButton("⏸ Pause", true);
        btnStop = makeTopButton("■ Stop", true);
        btnReset = makeTopButton("↺ Reset", false);
        JButton btnRandom = makeTopButton("⇄ Generate Random", false);
        left.add(btnStart);
        left.add(btnPause);
        left.add(btnStop);
        left.add(btnReset);
        left.add(btnRandom);

        btnStart.addActionListener(e -> startSimulation());
        btnPause.addActionListener(e -> pauseSimulation());
        btnStop.addActionListener(e -> stopSimulation());
        btnReset.addActionListener(e -> resetSimulation());
        btnRandom.addActionListener(e -> controlador.crearProcesoAleatorio());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        right.setBackground(Colors.COLOR_PANEL);
        clockLabel = new JLabel();
        clockLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusLabel = new JLabel(UiHelpers.iconText("● Idle"));
        statusLabel.setForeground(Colors.COLOR_TEXT_GRAY);
        right.add(clockLabel);
        right.add(Box.createHorizontalStrut(15));
        right.add(statusLabel);

        add(left, BorderLayout.WEST);
        add(right, BorderLayout.EAST);

        updateClockLabel();
    }

    // ---------------- SIMULATION CONTROLS ----------------
    private void startSimulation() {
        controlador.iniciar();
        statusLabel.setText(UiHelpers.iconText("● Running"));
        statusLabel.setForeground(Colors.COLOR_GREEN);
        btnStart.setEnabled(false);
        btnPause.setEnabled(true);
        btnStop.setEnabled(true);
    }

    private void pauseSimulation() {
        controlador.pausar();
        statusLabel.setText(UiHelpers.iconText("● Paused"));
        statusLabel.setForeground(Colors.COLOR_ORANGE);
        btnStart.setEnabled(true);
        btnPause.setEnabled(false);
    }

    private void stopSimulation() {
        controlador.detener();
        statusLabel.setText(UiHelpers.iconText("● Idle"));
        statusLabel.setForeground(Colors.COLOR_TEXT_GRAY);
        btnStart.setEnabled(true);
        btnPause.setEnabled(false);
        btnStop.setEnabled(false);
    }

    private void resetSimulation() {
        controlador.reiniciar();
        updateClockLabel();
        statusLabel.setText(UiHelpers.iconText("● Idle"));
        statusLabel.setForeground(Colors.COLOR_TEXT_GRAY);
        btnStart.setEnabled(true);
        btnPause.setEnabled(false);
        btnStop.setEnabled(false);
    }

    public final void refresh() {
        updateClockLabel();
    }

    private void updateClockLabel() {
        int t = controlador.getCurrentTime();
        int minutes = t / 60;
        int seconds = t % 60;
        clockLabel.setText(UiHelpers.iconText(String.format("⏱ %02d:%02d   t = %d", minutes, seconds, t)));
    }

    private JButton makeTopButton(String text, boolean disabled) {
        JButton b = new JButton(UiHelpers.iconText(text));
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        if (text.startsWith("+")) {
            b.setBackground(Colors.COLOR_BLUE);
            b.setForeground(java.awt.Color.WHITE);
            b.setBorder(new EmptyBorder(6, 14, 6, 14));
        } else {
            b.setBackground(Colors.COLOR_PANEL);
            b.setBorder(new CompoundBorder(new LineBorder(Colors.COLOR_BORDER, 1, true), new EmptyBorder(5, 12, 5, 12)));
        }
        b.setEnabled(!disabled);
        return b;
    }
}
