package org.example.model;
import java.util.ArrayList;
import java.util.List;


//La clase Metrics almacena las métricas de rendimiento y el historial del sistema para generar estadísticas y 
// el diagrama de Gantt de forma segura.

public class Metrics { 

    // Registro histórico que consolida todos los procesos que terminaron con éxito
    private ArrayList<Proceso> historialTerminados;

    // Bitácora temporal en formato [instanteTiempo, pid, estado] para el Diagrama de Gantt
    private List<String[]> bitacoraGantt;

    // Reloj interno de la simulación (se usa para marcar tiempoFin y la bitácora)
    private int tiempoActual;

    // Contador de ciclos de reloj en los que la CPU estuvo efectivamente ocupada
    private int ciclosOcupados;

    //Constructor: inicializa las estructuras de historial y bitácora vacías,
    //y pone en cero el reloj interno y el contador de ciclos ocupados.
    public Metrics() {
        this.historialTerminados = new ArrayList<>();
        this.bitacoraGantt = new ArrayList<>();
        this.tiempoActual = 0;
        this.ciclosOcupados = 0;
    }

    // **********Getters y Setters***********

    public synchronized ArrayList<Proceso> getHistorialTerminados() {
        return historialTerminados;
    }

    public synchronized List<String[]> getBitacoraGantt() {
        return bitacoraGantt;
    }

    public synchronized int getCurrentTime() {
        return tiempoActual;
    }

    public synchronized void setCurrentTime(int tiempoActual) {
        this.tiempoActual = tiempoActual;
    }

    //Avanza el reloj interno de la simulación en una unidad de tiempo (un ciclo).
    //Debe ser invocado por la Cpu en cada vuelta de su ciclo de ejecución.

    public synchronized void advanceClock() {
        this.tiempoActual++;
    }

    // Registro para el Diagrama de Gantt y utilización de CPU

    //Registra el tiempo, PID y estado de un proceso en cada ciclo de la simulación.
    public synchronized void recordGanttEntry(Proceso p) {
        recordGanttEntry(p, 0);
    }

    //Registra el tiempo, PID, estado y núcleo de un proceso en cada ciclo de la simulación.
    public synchronized void recordGanttEntry(Proceso p, int core) {
        bitacoraGantt.add(new String[] {
                String.valueOf(tiempoActual),
                String.valueOf(p.getPid()),
                p.getEstado(),
                String.valueOf(core)
        });
    }

    
    //Marca que, en el ciclo de reloj actual, la CPU estuvo ejecutando un
    //proceso (no estuvo ociosa). Necesario para calcular la utilización.
    public synchronized void recordBusyCycle() {
        this.ciclosOcupados++;
    }

    // Finalización de procesos

    //Captura el tiempo actual de simulación como el tiempoFin del proceso
    //y lo almacena de forma definitiva en el historial de terminados.
    public synchronized void registerEnd(Proceso p) {
        p.setTiempoFin(tiempoActual);
        historialTerminados.add(p);
    }

    //**********Fórmulas estadísticas**********


    //Calcula el timepo de retorno en un proceso individual usando la formula:
    //Retorno =TiempoFin - TiempoLlegada

    public double calculateReturnTime(Proceso p) {
        return p.getTiempoFin() - p.getTiempoLlegada();
    }

     //Calcula el Tiempo de espera de un proceso individual usando la formula
     //Espera = Tretorno - TiempoRafagaOriginal
    
    public double calculateWaitTime(Proceso p) {
        double tiempoRetorno = calculateReturnTime(p);
        return tiempoRetorno - p.getTiempoRafaga();
    }

    //Calcula el Tiempo de Retorno promedio de todos los procesos terminados.
    public synchronized double calculateAverageReturnTime() {
        if (historialTerminados.isEmpty()) {
            return 0.0;
        }
        double sumaRetornos = 0;
        for (Proceso p : historialTerminados) {
            sumaRetornos += calculateReturnTime(p);
        }
        return sumaRetornos / historialTerminados.size();
    }

     //Calcula el Tiempo de Espera promedio de todos los procesos terminados.

    public synchronized double calculateAverageWaitTime() {
        if (historialTerminados.isEmpty()) {
            return 0.0;
        }
        double sumaEsperas = 0;
        for (Proceso p : historialTerminados) {
            sumaEsperas += calculateWaitTime(p);
        }
        return sumaEsperas / historialTerminados.size();
    }

    //Calcula el porcentaje de utilización del procesador durante la simulación.
    public synchronized double calculateCpuUtilization(int tiempoTotal, int numCores) {
        if (tiempoTotal <= 0 || numCores <= 0) {
            return 0.0;
        }
        double porcentaje = ((double) ciclosOcupados / (tiempoTotal * (double) numCores)) * 100.0;
        return Math.min(porcentaje, 100.0);
    }

    //Calcula la métrica de Productividad (Throughput) del sistema:
    //Productividad = Procesos Terminados / Tiempo Total
    public synchronized double calculateThroughput(int tiempoTotal) {
        if (tiempoTotal <= 0) {
            return 0.0;
        }
        return (double) historialTerminados.size() / tiempoTotal;
    }
}