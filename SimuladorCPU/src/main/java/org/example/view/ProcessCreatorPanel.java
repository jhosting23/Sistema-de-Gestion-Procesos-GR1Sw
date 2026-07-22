package org.example.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;

import org.example.controller.Controlador;
import org.example.model.Proceso;
import org.example.model.Queue;

public class ProcessCreatorPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Color[] PALETTE = {
        new Color(0x3F, 0x51, 0xB5), new Color(0x4C, 0xAF, 0x50), new Color(0xFF, 0x98, 0x00),
        new Color(0xF4, 0x43, 0x36), new Color(0x9C, 0x27, 0xB0), new Color(0x00, 0xBC, 0xD4),
        new Color(0xE9, 0x1E, 0x63), new Color(0x8B, 0xC3, 0x4A), new Color(0xFF, 0x57, 0x22),
        new Color(0x67, 0x3A, 0xB7)
    };

    private final transient Controlador controlador;

    private final JSpinner arrivalSpinner;
    private final JSpinner burstSpinner;
    private final JSpinner prioritySpinner;
    private final transient List<JPanel> colorDots = new ArrayList<>();
    private Color selectedColor = PALETTE[0];

    private final JLabel poolCountLabel;
    private final JPanel poolContainer;

    public ProcessCreatorPanel(Controlador controlador) {
        super();
        this.controlador = controlador;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Colors.COLOR_PANEL);
        setBorder(new CompoundBorder(new MatteBorder(0, 0, 0, 1, Colors.COLOR_BORDER), new EmptyBorder(15, 15, 15, 15)));
        setPreferredSize(new Dimension(220, 100));

        add(UiHelpers.sectionLabel("PROCESS CREATOR"));
        add(Box.createVerticalStrut(10));

        add(UiHelpers.fieldLabel("PROCESS ID"));
        JTextField pid = new JTextField("Auto");
        pid.setEnabled(false);
        UiHelpers.styleField(pid);
        add(pid);
        add(Box.createVerticalStrut(8));

        add(UiHelpers.fieldLabel("ARRIVAL TIME"));
        arrivalSpinner = UiHelpers.spinnerField(3);
        add(arrivalSpinner);
        add(Box.createVerticalStrut(8));

        add(UiHelpers.fieldLabel("BURST TIME (T)"));
        burstSpinner = UiHelpers.spinnerField(2);
        ((javax.swing.SpinnerNumberModel) burstSpinner.getModel()).setMinimum(1);
        add(burstSpinner);
        add(Box.createVerticalStrut(8));

        add(UiHelpers.fieldLabel("PRIORITY"));
        prioritySpinner = UiHelpers.spinnerField(4);
        add(prioritySpinner);
        add(Box.createVerticalStrut(10));

        add(UiHelpers.fieldLabel("COLOR"));
        add(Box.createVerticalStrut(5));
        add(buildColorPicker());
        add(Box.createVerticalStrut(12));

        JButton create = new JButton("+ Create Process");
        create.setAlignmentX(Component.LEFT_ALIGNMENT);
        create.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        create.setBackground(Colors.COLOR_BLUE);
        create.setForeground(java.awt.Color.BLACK);
        create.setFocusPainted(false);
        create.addActionListener(e -> createProcess());
        add(create);
        add(Box.createVerticalStrut(15));

        JPanel poolHeader = new JPanel(new BorderLayout());
        poolHeader.setBackground(Colors.COLOR_PANEL);
        poolHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        poolHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        poolHeader.add(UiHelpers.fieldLabel("PROCESS POOL"), BorderLayout.WEST);
        poolCountLabel = new JLabel("0");
        poolCountLabel.setOpaque(true);
        poolCountLabel.setBackground(Colors.COLOR_BG);
        poolCountLabel.setBorder(new EmptyBorder(1, 7, 1, 7));
        poolHeader.add(poolCountLabel, BorderLayout.EAST);
        add(poolHeader);
        add(Box.createVerticalStrut(8));

        poolContainer = new JPanel();
        poolContainer.setLayout(new BoxLayout(poolContainer, BoxLayout.Y_AXIS));
        poolContainer.setBackground(Colors.COLOR_PANEL);
        poolContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(poolContainer);
        add(Box.createVerticalGlue());

        refresh();
    }

    private void createProcess() {
        int arrival = (Integer) arrivalSpinner.getValue();
        int burst = (Integer) burstSpinner.getValue();
        int priority = (Integer) prioritySpinner.getValue();
        controlador.crearProceso(arrival, burst, priority, selectedColor);
    }

    public final void refresh() {
        List<Proceso> all = controlador.getTodosLosProcesos();
        poolCountLabel.setText(String.valueOf(all.size()));

        poolContainer.removeAll();
        for (int i = 0; i < all.size(); i++) {
            Proceso p = all.get(i);
            poolContainer.add(buildPoolCard(p));
            if (i < all.size() - 1) poolContainer.add(Box.createVerticalStrut(8));
        }
        poolContainer.revalidate();
        poolContainer.repaint();
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

    private JPanel buildPoolCard(Proceso p) {
        JPanel card = new JPanel(new BorderLayout());
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        card.setBackground(Colors.COLOR_BG);
        card.setBorder(new CompoundBorder(new LineBorder(Colors.COLOR_BORDER), new EmptyBorder(6, 10, 6, 10)));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel name = new JLabel(new StringBuilder().append(p.getNombre()).append("   ").append(stateLabel(p.getEstado())).toString());
        name.setFont(new Font("Segoe UI", Font.BOLD, 12));
        name.setForeground(controlador.getColor(p.getPid()));
        top.add(name, BorderLayout.WEST);

        JLabel sub = new JLabel(new StringBuilder().append("Burst: ").append(p.getTiempoRafaga()).append("t   Arr: ").append(p.getTiempoLlegada()).append("t").toString());
        sub.setForeground(Colors.COLOR_TEXT_GRAY);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        card.add(top, BorderLayout.NORTH);
        card.add(sub, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildColorPicker() {
        JPanel panel = new JPanel(new GridLayout(2, 5, 6, 6));
        panel.setBackground(Colors.COLOR_PANEL);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(200, 50));
        for (Color c : PALETTE) {
            JPanel dot = new JPanel();
            dot.setBackground(c);
            dot.setPreferredSize(new Dimension(20, 20));
            dot.setBorder(c.equals(selectedColor) ? new LineBorder(Color.BLACK, 2) : new LineBorder(Colors.COLOR_BORDER, 1));
            dot.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    selectedColor = c;
                    for (JPanel d : colorDots) {
                        Color dc = d.getBackground();
                        d.setBorder(dc.equals(selectedColor) ? new LineBorder(Color.BLACK, 2) : new LineBorder(Colors.COLOR_BORDER, 1));
                    }
                }
            });
            colorDots.add(dot);
            panel.add(dot);
        }
        return panel;
    }
}
