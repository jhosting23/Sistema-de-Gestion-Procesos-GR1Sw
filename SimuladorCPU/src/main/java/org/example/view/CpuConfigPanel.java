package org.example.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

public class CpuConfigPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient SimulationEngine engine;

    private final JSpinner coresSpinner;
    private final JSpinner clockSpinner;
    private final JSpinner quantumSpinner;
    private final JComboBox<String> algoCombo;
    private final JCheckBox preemptCheck;
    private final JPanel coreStatusContainer;
    private final JLabel utilLabel;

    public CpuConfigPanel(SimulationEngine engine) {
        super();
        this.engine = engine;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Colors.COLOR_PANEL);
        setBorder(new CompoundBorder(new MatteBorder(0, 1, 0, 0, Colors.COLOR_BORDER), new EmptyBorder(15, 15, 15, 15)));
        setPreferredSize(new Dimension(220, 100));

        add(UiHelpers.sectionLabel("CPU CONFIGURATION"));
        add(Box.createVerticalStrut(10));

        add(UiHelpers.fieldLabel("NUMBER OF CORES"));
        coresSpinner = UiHelpers.spinnerField(engine.getNumCores());
        ((SpinnerNumberModel) coresSpinner.getModel()).setMinimum(1);
        add(coresSpinner);
        add(Box.createVerticalStrut(8));

        add(UiHelpers.fieldLabel("CLOCK SPEED (GHZ)"));
        clockSpinner = new JSpinner(new SpinnerNumberModel(engine.getClockSpeed(), 0.1, 10.0, 0.1));
        clockSpinner.setAlignmentX(Component.LEFT_ALIGNMENT);
        clockSpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        add(clockSpinner);
        add(Box.createVerticalStrut(8));

        add(UiHelpers.fieldLabel("TIME QUANTUM"));
        quantumSpinner = UiHelpers.spinnerField(engine.getQuantum());
        ((SpinnerNumberModel) quantumSpinner.getModel()).setMinimum(1);
        add(quantumSpinner);
        add(Box.createVerticalStrut(8));

        add(UiHelpers.fieldLabel("SCHEDULING ALGORITHM"));
        algoCombo = new JComboBox<>(new String[]{"Round Robin", "FCFS", "SJF", "Priority", "SRTF"});
        algoCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        algoCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        add(algoCombo);
        add(Box.createVerticalStrut(8));

        preemptCheck = new JCheckBox("Enable Preemption");
        preemptCheck.setSelected(engine.isPreemption());
        preemptCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        preemptCheck.setBackground(Colors.COLOR_PANEL);
        add(preemptCheck);
        add(Box.createVerticalStrut(10));

        JButton apply = new JButton("Apply Configuration");
        apply.setAlignmentX(Component.LEFT_ALIGNMENT);
        apply.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        apply.setBackground(Colors.COLOR_BLUE);
        apply.setForeground(java.awt.Color.WHITE);
        apply.setFocusPainted(false);
        apply.addActionListener(e -> applyConfiguration());
        add(apply);
        add(Box.createVerticalStrut(18));

        add(UiHelpers.sectionLabel("CORE STATUS"));
        add(Box.createVerticalStrut(8));
        coreStatusContainer = new JPanel();
        coreStatusContainer.setLayout(new BoxLayout(coreStatusContainer, BoxLayout.Y_AXIS));
        coreStatusContainer.setBackground(Colors.COLOR_PANEL);
        coreStatusContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(coreStatusContainer);
        add(Box.createVerticalStrut(15));

        JPanel utilHeader = new JPanel(new BorderLayout());
        utilHeader.setBackground(Colors.COLOR_PANEL);
        utilHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        utilHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        utilHeader.add(UiHelpers.fieldLabel("CPU Utilization"), BorderLayout.WEST);
        utilLabel = new JLabel("0%");
        utilHeader.add(utilLabel, BorderLayout.EAST);
        add(utilHeader);

        add(Box.createVerticalGlue());

        // call non-overridable refresh during construction to avoid calling an overridable
        // method from the constructor.
        refreshInternal();
    }

    private void applyConfiguration() {
        int cores = (Integer) coresSpinner.getValue();
        double clock = (Double) clockSpinner.getValue();
        int quantum = (Integer) quantumSpinner.getValue();
        boolean preemption = preemptCheck.isSelected();
        SimulationEngine.Algorithm algorithm = algorithmFromLabel((String) algoCombo.getSelectedItem());
        engine.applyConfig(cores, clock, quantum, algorithm, preemption);
    }

    private SimulationEngine.Algorithm algorithmFromLabel(String label) {
        switch (label) {
            case "FCFS": return SimulationEngine.Algorithm.FCFS;
            case "SJF": return SimulationEngine.Algorithm.SJF;
            case "Priority": return SimulationEngine.Algorithm.PRIORITY;
            case "SRTF": return SimulationEngine.Algorithm.SRTF;
            case "Round Robin":
            default: return SimulationEngine.Algorithm.RR;
        }
    }

    /**
     * Public refresh entry point. Can be called externally.
     */
    public void refresh() {
        refreshInternal();
    }

    /**
     * Internal non-overridable refresh implementation used during construction
     * to avoid calling an overridable method from the constructor.
     */
    private void refreshInternal() {
        coreStatusContainer.removeAll();
        Process[] running = engine.getRunning();
        for (int i = 0; i < running.length; i++) {
            String status = running[i] != null ? String.valueOf(running[i].pid).concat(" running") : "Idle";
            coreStatusContainer.add(coreStatusLine(new StringBuilder("Core ").append(i).toString(), status, running[i] != null));
            if (i < running.length - 1) coreStatusContainer.add(Box.createVerticalStrut(4));
        }
        coreStatusContainer.revalidate();
        coreStatusContainer.repaint();

        int pct = (int) Math.round(engine.getCpuUtilization() * 100);
        utilLabel.setText(Integer.toString(pct).concat("%"));
    }

    private JLabel coreStatusLine(String name, String status, boolean busy) {
        JLabel l = new JLabel(UiHelpers.iconText(new StringBuilder("● ").append(name).append("   ").append(status).toString()));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setForeground(busy ? Colors.COLOR_GREEN : Colors.COLOR_TEXT_GRAY);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return l;
    }
}
