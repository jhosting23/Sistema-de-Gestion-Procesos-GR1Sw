package org.example.model;

import java.util.Random;
import java.util.concurrent.Semaphore;

public class Cpu extends Thread {

    private Queue queue;
    private Metrics metrics;
    private int quantum;
    private Semaphore recursoCompartido;

    private String algoritmoActivo;
    private final int core;

    private volatile Proceso procesoActual;

    private Random random = new Random();

    public Cpu(Queue queue,
               Metrics metrics,
               int quantum,
               Semaphore recursoCompartido,
               String algoritmoActivo) {
        this(queue, metrics, quantum, recursoCompartido, algoritmoActivo, 0);
    }

    public Cpu(Queue queue,
               Metrics metrics,
               int quantum,
               Semaphore recursoCompartido,
               String algoritmoActivo,
               int core) {

        this.queue = queue;
        this.metrics = metrics;
        this.quantum = quantum;
        this.recursoCompartido = recursoCompartido;
        this.algoritmoActivo = algoritmoActivo;
        this.core = core;
    }

    @Override
    public void run() {

        while (!isInterrupted()) {

            Proceso proceso = queue.despacharSiguiente(algoritmoActivo);

            if (proceso == null) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    interrupt();
                    break;
                }
                continue;
            }

            proceso.setEstado(Queue.ESTADO_EJECUTANDO);
            procesoActual = proceso;

            int ciclosQuantum = 0;

            while (proceso.getTiempoRestante() > 0) {

                try {

                    recursoCompartido.acquire();

                    Thread.sleep(500);

                    proceso.decrementarTiempoRestante(1);
                    proceso.incrementarContadorPrograma();

                    ciclosQuantum++;

                } catch (InterruptedException e) {
                    queue.agregarAListos(proceso);
                    interrupt();
                    break;
                } finally {
                    recursoCompartido.release();
                }

                if (verificarTransiciones(proceso, ciclosQuantum)) {
                    break;
                }
            }

            procesoActual = null;
        }
    }

    public Proceso getProcesoActual() {
        return procesoActual;
    }

    public int getCore() {
        return core;
    }

    /**
     * Devuelve true cuando el proceso deja la CPU.
     */
    private boolean verificarTransiciones(Proceso p, int ciclosQuantum) {

        // Finalización
        if (p.getTiempoRestante() == 0) {

            p.setEstado(Queue.ESTADO_TERMINADO);

            if (metrics != null) {
                metrics.registerEnd(p);
            }

            return true;
        }

        // Evento aleatorio de E/S (20%). No se especificó la funcion probabilística, así que se asumió una probabilidad del 20% para simular un evento de E/S.
        if (random.nextDouble() < 0.20) {

            queue.bloquearProceso(p);
            return true;
        }

        // Round Robin
        if (algoritmoActivo.equals(Queue.ROUND_ROBIN)
                && ciclosQuantum >= quantum) {

            queue.agregarAListos(p);
            return true;
        }

        return false;
    }

    public void setAlgoritmoActivo(String algoritmoActivo) {
        this.algoritmoActivo = algoritmoActivo;
    }
}
