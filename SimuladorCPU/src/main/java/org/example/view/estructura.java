package org.example.view;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class estructura extends JFrame {

    private static final long serialVersionUID = 1L;

    public estructura() {
        setTitle("CPU Scheduler Sim");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1620, 620);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Colors.COLOR_BG);

        SimulationEngine engine = new SimulationEngine();

        TopBarPanel topBar = new TopBarPanel(engine);
        add(topBar, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(Colors.COLOR_BG);
        ProcessCreatorPanel processCreator = new ProcessCreatorPanel(engine);
        SimulationAreaPanel simulationArea = new SimulationAreaPanel(engine);
        CpuConfigPanel cpuConfig = new CpuConfigPanel(engine);
        center.add(processCreator, BorderLayout.WEST);
        center.add(simulationArea, BorderLayout.CENTER);
        center.add(cpuConfig, BorderLayout.EAST);
        add(center, BorderLayout.CENTER);

        BottomBarPanel bottomBar = new BottomBarPanel(engine);
        add(bottomBar, BorderLayout.SOUTH);

        engine.addListener(() -> {
            processCreator.refresh();
            simulationArea.refresh();
            cpuConfig.refresh();
            bottomBar.refresh();
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException ignored) {}
            estructura frame = new estructura();
            frame.setVisible(true);
        });
    }
}
