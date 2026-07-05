package org.example.model;

import java.util.LinkedList;

public class Queue {

    private LinkedList<Proceso> colaNuevos;
    private LinkedList<Proceso> colaListos;
    private LinkedList<Proceso> colaBloqueados;

    // Estados usados  
    public static final String ESTADO_NUEVO = "Nuevo";
    public static final String ESTADO_LISTO = "Listo";
    public static final String ESTADO_EJECUTANDO = "Ejecutando";
    public static final String ESTADO_BLOQUEADO = "Bloqueado";
    public static final String ESTADO_TERMINADO = "Terminado";

    // Algoritmos soportados 
    public static final String FIFO = "FIFO";
    public static final String SJF = "SJF";
    public static final String PRIORIDADES = "Prioridades";

    public Queue() {
        this.colaNuevos = new LinkedList<>();
        this.colaListos = new LinkedList<>();
        this.colaBloqueados = new LinkedList<>();
    }


    public synchronized void agregarNuevo(Proceso p) {
        colaNuevos.add(p);
    }

    /**
     * Transiciona un proceso de Nuevos a Listos
     * actualizando su estado interno a Listo
     */
    public synchronized void admitirProceso(Proceso p) {
        if (colaNuevos.remove(p)) {
            p.setEstado(ESTADO_LISTO);
            colaListos.add(p);
        }
    }

    /**
     * Inserta de nuevo un proceso en la cola de listos tras una interrupcion por tiempo o la resolucion de un
     * evento de bloqueo
     */
    public synchronized void agregarAListos(Proceso p) {
        p.setEstado(ESTADO_LISTO);
        colaListos.add(p);
    }

    /**
     * Aplica la logica de seleccion del algoritmo activo y retorna el siguiente proceso a ejecutar
     * Retorna null si no hay procesos disponibles.
     */
    public synchronized Proceso despacharSiguiente(String algoritmo) {
        if (colaListos.isEmpty()) {
            return null;
        }

        switch (algoritmo) {
            case FIFO:
                return colaListos.removeFirst();

            case SJF:
                return extraerPorCriterio(true);

            case PRIORIDADES:
                return extraerPorCriterio(false);

            default:
                return colaListos.removeFirst();
        }
    }


    private Proceso extraerPorCriterio(boolean porTiempoRestante) {
        Proceso seleccionado = colaListos.getFirst();

        for (Proceso candidato : colaListos) {
            if (porTiempoRestante) {
                if (candidato.getTiempoRestante() < seleccionado.getTiempoRestante()) {
                    seleccionado = candidato;
                }
            } else {
                if (candidato.getPrioridad() > seleccionado.getPrioridad()) {
                    seleccionado = candidato;
                }
            }
        }

        colaListos.remove(seleccionado);
        return seleccionado;
    }


    public synchronized void bloquearProceso(Proceso p) {
        p.setEstado(ESTADO_BLOQUEADO);
        colaBloqueados.add(p);
    }

  
     // Extrae el proceso de la cola de bloqueados y lo mueve nuevamente a la cola de listos

    public synchronized void desbloquearProceso(Proceso p) {
        if (colaBloqueados.remove(p)) {
            agregarAListos(p);
        }
    }


    /**
     * Busca un proceso por id en cualquiera de las tres colas.
     */
    public synchronized Proceso buscarPorId(int id) {
        for (Proceso p : colaNuevos) {
            if (p.getId() == id) return p;
        }
        for (Proceso p : colaListos) {
            if (p.getId() == id) return p;
        }
        for (Proceso p : colaBloqueados) {
            if (p.getId() == id) return p;
        }
        return null;
    }

    public synchronized LinkedList<Proceso> getColaNuevos() {
        return new LinkedList<>(colaNuevos);
    }

    public synchronized LinkedList<Proceso> getColaListos() {
        return new LinkedList<>(colaListos);
    }

    public synchronized LinkedList<Proceso> getColaBloqueados() {
        return new LinkedList<>(colaBloqueados);
    }

    public synchronized boolean hayProcesosActivos() {
        return !colaNuevos.isEmpty() || !colaListos.isEmpty() || !colaBloqueados.isEmpty();
    }
}