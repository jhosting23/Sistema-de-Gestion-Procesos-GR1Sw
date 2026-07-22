package org.example.controller;

import org.example.model.Cpu;
import org.example.model.Metrics;
import org.example.model.Proceso;
import org.example.model.Queue;

import javax.swing.Timer;
import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * Orquesta el modelo (Queue, Metrics, Cpu) y expone al view un punto único
 * de acceso al estado de la simulación, evitando que la vista conozca
 * detalles de hilos o sincronización.
 */
public class Controlador {

    private static final int TICK_MS = 500;
    private static final String LOG_FILE = "log.txt";

    private static final Color[] PALETTE = {
        new Color(0x3F, 0x51, 0xB5), new Color(0x4C, 0xAF, 0x50), new Color(0xFF, 0x98, 0x00),
        new Color(0xF4, 0x43, 0x36), new Color(0x9C, 0x27, 0xB0), new Color(0x00, 0xBC, 0xD4),
        new Color(0xE9, 0x1E, 0x63), new Color(0x8B, 0xC3, 0x4A), new Color(0xFF, 0x57, 0x22),
        new Color(0x67, 0x3A, 0xB7)
    };

    private Queue queue = new Queue();
    private Metrics metrics = new Metrics();
    private final Semaphore recursoCompartido = new Semaphore(1);

    private final List<Proceso> todosLosProcesos = new ArrayList<>();
    private final Map<Integer, Color> coloresProcesos = new ConcurrentHashMap<>();
    private final List<Cpu> cpus = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private final List<String> eventLog = new ArrayList<>();

    private int numCores = 2;
    private int quantum = 4;
    private String algoritmoActivo = Queue.ROUND_ROBIN;
    private boolean corriendo = false;
    private int pidCounter = 1;

    private final Random random = new Random();
    private final Timer clockTimer = new Timer(TICK_MS, e -> onClockTick());

    public Controlador() {
        clockTimer.setRepeats(true);
    }

    // ---------------- Listeners ----------------

    public void addListener(Runnable r) {
        listeners.add(r);
    }

    private void notifyListeners() {
        for (Runnable r : listeners) {
            r.run();
        }
    }

    // ---------------- Creación de procesos ----------------

    public synchronized Proceso crearProceso(int tiempoLlegada, int tiempoRafaga, int prioridad, Color color) {
        int pid = pidCounter++;
        Proceso p = new Proceso(pid, "P" + pid, tiempoRafaga, prioridad, tiempoLlegada);
        todosLosProcesos.add(p);
        coloresProcesos.put(pid, color != null ? color : PALETTE[pid % PALETTE.length]);

        queue.agregarNuevo(p);
        if (tiempoLlegada <= metrics.getCurrentTime()) {
            queue.admitirProceso(p);
        }

        log("Proceso " + p.getNombre() + " creado (ráfaga=" + tiempoRafaga + ", prioridad=" + prioridad
                + ", llegada=" + tiempoLlegada + ")");
        notifyListeners();
        return p;
    }

    public Proceso crearProcesoAleatorio() {
        int llegada = metrics.getCurrentTime() + random.nextInt(4);
        int rafaga = 1 + random.nextInt(9);
        int prioridad = 1 + random.nextInt(5);
        return crearProceso(llegada, rafaga, prioridad, PALETTE[random.nextInt(PALETTE.length)]);
    }

    public Color getColor(int pid) {
        Color c = coloresProcesos.get(pid);
        return c != null ? c : PALETTE[Math.abs(pid) % PALETTE.length];
    }

    // ---------------- Configuración ----------------

    public synchronized void aplicarConfiguracion(int cores, int quantum, String algoritmo) {
        this.numCores = Math.max(1, cores);
        this.quantum = Math.max(1, quantum);
        this.algoritmoActivo = algoritmo;
        for (Cpu c : cpus) {
            c.setAlgoritmoActivo(algoritmo);
        }
        log("Config: " + algoritmo + ", " + this.numCores + " core(s), quantum=" + this.quantum);
        notifyListeners();
    }

    // ---------------- Control de ejecución ----------------

    public synchronized void iniciar() {
        if (corriendo) {
            return;
        }
        corriendo = true;
        if (cpus.isEmpty()) {
            for (int i = 0; i < numCores; i++) {
                Cpu cpu = new Cpu(queue, metrics, quantum, recursoCompartido, algoritmoActivo, i);
                cpus.add(cpu);
                cpu.start();
            }
        }
        clockTimer.start();
        log("Simulación iniciada");
        notifyListeners();
    }

    public synchronized void pausar() {
        if (!corriendo) {
            return;
        }
        corriendo = false;
        clockTimer.stop();
        for (Cpu c : cpus) {
            c.interrupt();
        }
        cpus.clear();
        log("Simulación pausada");
        notifyListeners();
    }

    public synchronized void detener() {
        pausar();
        log("Simulación detenida");
        notifyListeners();
    }

    public synchronized void reiniciar() {
        pausar();
        queue = new Queue();
        metrics = new Metrics();
        todosLosProcesos.clear();
        coloresProcesos.clear();
        eventLog.clear();
        pidCounter = 1;
        log("Simulación reiniciada");
        notifyListeners();
    }

    public boolean isCorriendo() {
        return corriendo;
    }

    // ---------------- Ciclo de reloj compartido ----------------

    private void onClockTick() {
        metrics.advanceClock();

        for (Proceso p : queue.getColaNuevos()) {
            if (p.getTiempoLlegada() <= metrics.getCurrentTime()) {
                queue.admitirProceso(p);
                log(p.getNombre() + " llegó → LISTO");
            }
        }

        for (Proceso p : queue.getColaListos()) {
            p.incrementarTiempoEspera();
        }

        for (Cpu c : cpus) {
            Proceso actual = c.getProcesoActual();
            if (actual != null) {
                metrics.recordBusyCycle();
                metrics.recordGanttEntry(actual, c.getCore());
            }
        }

        notifyListeners();
    }

    // ---------------- Bitácora ----------------

    private void log(String message) {
        String entry = String.format("[t=%d] %s", metrics.getCurrentTime(), message);
        eventLog.add(entry);
        writeToLogFile(entry);
    }

    private void writeToLogFile(String entry) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            writer.println(entry);
        } catch (IOException e) {
            System.err.println("No se pudo escribir en " + LOG_FILE + ": " + e.getMessage());
        }
    }

    public List<String> getEventLog() {
        return new ArrayList<>(eventLog);
    }

    // ---------------- Consultas de estado para la vista ----------------

    public int getCurrentTime() {
        return metrics.getCurrentTime();
    }

    public int getNumCores() {
        return numCores;
    }

    public int getQuantum() {
        return quantum;
    }

    public String getAlgoritmoActivo() {
        return algoritmoActivo;
    }

    public List<Cpu> getCpus() {
        return new ArrayList<>(cpus);
    }

    public LinkedList<Proceso> getColaListos() {
        return queue.getColaListos();
    }

    public LinkedList<Proceso> getColaBloqueados() {
        return queue.getColaBloqueados();
    }

    public LinkedList<Proceso> getColaNuevos() {
        return queue.getColaNuevos();
    }

    public List<Proceso> getTodosLosProcesos() {
        return new ArrayList<>(todosLosProcesos);
    }

    public List<Proceso> getProcesosTerminados() {
        return new ArrayList<>(metrics.getHistorialTerminados());
    }

    public List<String[]> getGanttHistory() {
        return metrics.getBitacoraGantt();
    }

    public double getCpuUtilization() {
        return metrics.calculateCpuUtilization(metrics.getCurrentTime(), numCores) / 100.0;
    }

    public double getAvgWaiting() {
        return metrics.calculateAverageWaitTime();
    }

    public double getAvgTurnaround() {
        return metrics.calculateAverageReturnTime();
    }

    public double getThroughput() {
        return metrics.calculateThroughput(metrics.getCurrentTime());
    }
}
