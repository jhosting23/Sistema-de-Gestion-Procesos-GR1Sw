package org.example.view;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Random;

public class SimulationEngine {

    public enum Algorithm { FCFS, SJF, PRIORITY, RR, SRTF }

    private int numCores = 2;
    private double clockSpeed = 2.40;
    private int quantum = 4;
    private Algorithm algorithm = Algorithm.RR;
    private boolean preemption = true;

    private int currentTime = 0;
    private int pidCounter = 1;

    private final List<Process> allProcesses = new ArrayList<>();
    private final Deque<Process> readyQueue = new ArrayDeque<>();
    private Process[] running = new Process[numCores];
    private int[] quantumUsed = new int[numCores];
    private final List<Process[]> ganttHistory = new ArrayList<>();

    private final List<Runnable> listeners = new ArrayList<>();
    private final Random random = new Random();

    public void addListener(Runnable r) {
        listeners.add(r);
    }

    private void notifyListeners() {
        for (Runnable r : listeners) r.run();
    }

    public void applyConfig(int cores, double clockSpeed, int quantum, Algorithm algorithm, boolean preemption) {
        this.clockSpeed = clockSpeed;
        this.quantum = Math.max(1, quantum);
        this.algorithm = algorithm;
        this.preemption = preemption;

        int newCores = Math.max(1, cores);
        Process[] newRunning = new Process[newCores];
        int[] newQuantumUsed = new int[newCores];
        for (int i = 0; i < newCores && i < running.length; i++) {
            newRunning[i] = running[i];
            newQuantumUsed[i] = quantumUsed[i];
        }
        for (int i = newCores; i < running.length; i++) {
            if (running[i] != null) {
                running[i].state = Process.State.READY;
                running[i].coreAssigned = -1;
                readyQueue.addFirst(running[i]);
            }
        }
        this.numCores = newCores;
        running = newRunning;
        quantumUsed = newQuantumUsed;
        notifyListeners();
    }

    public Process addProcess(int arrival, int burst, int priority, int memory, Color color) {
        String pid = new StringBuilder().append('P').append(pidCounter++).toString();
        Process p = new Process(pid, arrival, burst, priority, memory, color);
        allProcesses.add(p);
        notifyListeners();
        return p;
    }

    public Process addRandomProcess() {
        int arrival = currentTime + random.nextInt(4);
        int burst = 1 + random.nextInt(9);
        int priority = 1 + random.nextInt(5);
        int memory = 32 + random.nextInt(480);
        Color[] palette = {
            new Color(0x3F, 0x51, 0xB5), new Color(0x4C, 0xAF, 0x50), new Color(0xFF, 0x98, 0x00),
            new Color(0xF4, 0x43, 0x36), new Color(0x9C, 0x27, 0xB0), new Color(0x00, 0xBC, 0xD4)
        };
        return addProcess(arrival, burst, priority, memory, palette[random.nextInt(palette.length)]);
    }

    public void reset() {
        currentTime = 0;
        pidCounter = 1;
        allProcesses.clear();
        readyQueue.clear();
        running = new Process[numCores];
        quantumUsed = new int[numCores];
        ganttHistory.clear();
        notifyListeners();
    }

    public void tick() {
        for (Process p : allProcesses) {
            if (p.state == Process.State.NEW && p.arrival <= currentTime) {
                p.state = Process.State.READY;
                readyQueue.add(p);
            }
        }

        if (preemption) {
            for (int c = 0; c < numCores; c++) {
                Process cur = running[c];
                if (cur == null) continue;
                switch (algorithm) {
                    case RR:
                        if (quantumUsed[c] >= quantum) {
                            cur.state = Process.State.READY;
                            cur.coreAssigned = -1;
                            readyQueue.add(cur);
                            running[c] = null;
                            quantumUsed[c] = 0;
                        }
                        break;
                    case SRTF: {
                        Process best = minBy(Comparator.comparingInt(p -> p.remaining));
                        if (best != null && best.remaining < cur.remaining) {
                            preempt(c, cur, best);
                        }
                        break;
                    }
                    case PRIORITY: {
                        Process best = minBy(Comparator.comparingInt(p -> p.priority));
                        if (best != null && best.priority < cur.priority) {
                            preempt(c, cur, best);
                        }
                        break;
                    }
                    default:
                        break;
                }
            }
        }

        for (int c = 0; c < numCores; c++) {
            if (running[c] == null && !readyQueue.isEmpty()) {
                Process next = selectNext();
                if (next != null) {
                    readyQueue.remove(next);
                    assignToCore(c, next);
                }
            }
        }

        Process[] slot = running.clone();

        for (int c = 0; c < numCores; c++) {
            Process p = running[c];
            if (p != null) {
                p.remaining--;
                if (algorithm == Algorithm.RR) quantumUsed[c]++;
                if (p.remaining <= 0) {
                    p.state = Process.State.FINISHED;
                    p.finishTime = currentTime + 1;
                    p.turnaroundTime = p.finishTime - p.arrival;
                    p.coreAssigned = -1;
                    running[c] = null;
                    quantumUsed[c] = 0;
                }
            }
        }

        for (Process p : readyQueue) {
            p.waitingTime++;
        }

        ganttHistory.add(slot);
        currentTime++;
        notifyListeners();
    }

    private void preempt(int core, Process outgoing, Process incoming) {
        outgoing.state = Process.State.READY;
        outgoing.coreAssigned = -1;
        readyQueue.add(outgoing);
        readyQueue.remove(incoming);
        assignToCore(core, incoming);
    }

    private Process selectNext() {
        switch (algorithm) {
            case FCFS:
                return minBy(Comparator.comparingInt((Process p) -> p.arrival));
            case SJF:
            case SRTF:
                return minBy(Comparator.comparingInt((Process p) -> p.remaining));
            case PRIORITY:
                return minBy(Comparator.comparingInt((Process p) -> p.priority));
            case RR:
            default:
                return readyQueue.peek();
        }
    }

    private Process minBy(Comparator<Process> cmp) {
        Process best = null;
        for (Process p : readyQueue) {
            if (best == null || cmp.compare(p, best) < 0) best = p;
        }
        return best;
    }

    private void assignToCore(int core, Process p) {
        p.state = Process.State.RUNNING;
        p.coreAssigned = core;
        if (p.startTime < 0) p.startTime = currentTime;
        running[core] = p;
        quantumUsed[core] = 0;
    }

    public int getCurrentTime() { return currentTime; }
    public int getNumCores() { return numCores; }
    public double getClockSpeed() { return clockSpeed; }
    public int getQuantum() { return quantum; }
    public Algorithm getAlgorithm() { return algorithm; }
    public boolean isPreemption() { return preemption; }
    public Process[] getRunning() { return running; }

    public List<Process> getReadyQueue() {
        return new ArrayList<>(readyQueue);
    }

    public List<Process> getAllProcesses() {
        return allProcesses;
    }

    public List<Process[]> getGanttHistory() {
        return ganttHistory;
    }

    public List<Process> getFinishedProcesses() {
        List<Process> out = new ArrayList<>();
        for (Process p : allProcesses) {
            if (p.state == Process.State.FINISHED) out.add(p);
        }
        return out;
    }

    public double getCpuUtilization() {
        if (currentTime == 0) return 0;
        int busyTicks = 0;
        for (Process p : allProcesses) {
            busyTicks += (p.burst - Math.max(p.remaining, 0));
        }
        return Math.min(1.0, (double) busyTicks / (numCores * currentTime));
    }

    public double getAvgWaiting() {
        List<Process> fin = getFinishedProcesses();
        if (fin.isEmpty()) return 0;
        double sum = 0;
        for (Process p : fin) sum += p.waitingTime;
        return sum / fin.size();
    }

    public double getAvgTurnaround() {
        List<Process> fin = getFinishedProcesses();
        if (fin.isEmpty()) return 0;
        double sum = 0;
        for (Process p : fin) sum += p.turnaroundTime;
        return sum / fin.size();
    }

    public double getThroughput() {
        if (currentTime == 0) return 0;
        return (double) getFinishedProcesses().size() / currentTime;
    }
}
