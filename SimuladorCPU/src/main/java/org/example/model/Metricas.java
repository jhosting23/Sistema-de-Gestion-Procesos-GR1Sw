import java.util.ArrayList;
import java.util.List;

/**
 * Clase Metrics (Evaluación de Rendimiento e Historial)
 *
 * Componente pasivo del Modelo encargado de auditar de forma matemática
 * el rendimiento global del sistema operativo simulado. Almacena las
 * variables de telemetría para que la Vista consuma los datos finales
 * (estadísticas de retorno, espera, utilización de CPU y productividad),
 * además de la bitácora necesaria para construir el Diagrama de Gantt.
 *
 * Todos los métodos que pueden ser invocados concurrentemente desde el
 * hilo de la Cpu están sincronizados para evitar condiciones de carrera.
 */
public class Metrics {

    // Registro histórico que consolida todos los procesos que terminaron con éxito
    private ArrayList<Proceso> historialTerminados;

    // Bitácora temporal en formato [instanteTiempo, pid, estado] para el Diagrama de Gantt
    private List<String[]> bitacoraGantt;

    // Reloj interno de la simulación (se usa para marcar tiempoFin y la bitácora)
    private int tiempoActual;

    // Contador de ciclos de reloj en los que la CPU estuvo efectivamente ocupada
    private int ciclosOcupados;

    /**
     * Constructor: inicializa las estructuras de historial y bitácora vacías,
     * y pone en cero el reloj interno y el contador de ciclos ocupados.
     */
    public Metrics() {
        this.historialTerminados = new ArrayList<>();
        this.bitacoraGantt = new ArrayList<>();
        this.tiempoActual = 0;
        this.ciclosOcupados = 0;
    }

    // ---------------------------------------------------------------
    // Getters y Setters
    // ---------------------------------------------------------------

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

    /**
     * Avanza el reloj interno de la simulación en una unidad de tiempo (un ciclo).
     * Debe ser invocado por la Cpu en cada vuelta de su ciclo de ejecución.
     */
    public synchronized void advanceClock() {
        this.tiempoActual++;
    }

    // ---------------------------------------------------------------
    // Registro para el Diagrama de Gantt y utilización de CPU
    // ---------------------------------------------------------------

    /**
     * Registra en la bitácora un pulso de reloj para el proceso indicado,
     * guardando [instanteTiempo, pid, estado]. Debe llamarse en cada ciclo
     * en el que un proceso ocupa o cambia su condición en la simulación.
     */
    public synchronized void recordGanttEntry(Proceso p) {
        bitacoraGantt.add(new String[] {
                String.valueOf(tiempoActual),
                String.valueOf(p.getPid()),
                p.getEstado()
        });
    }

    /**
     * Marca que, en el ciclo de reloj actual, la CPU estuvo ejecutando un
     * proceso (no estuvo ociosa). Necesario para calcular la utilización.
     */
    public synchronized void recordBusyCycle() {
        this.ciclosOcupados++;
    }

    // ---------------------------------------------------------------
    // Finalización de procesos
    // ---------------------------------------------------------------

    /**
     * Captura el tiempo actual de simulación como el tiempoFin del proceso
     * y lo almacena de forma definitiva en el historial de terminados.
     */
    public synchronized void registerEnd(Proceso p) {
        p.setTiempoFin(tiempoActual);
        historialTerminados.add(p);
    }

    // ---------------------------------------------------------------
    // Fórmulas estadísticas
    // ---------------------------------------------------------------

    /**
     * Calcula el Tiempo de Retorno de un proceso individual:
     * Tretorno = TiempoFin - TiempoLlegada
     */
    public double calculateReturnTime(Proceso p) {
        return p.getTiempoFin() - p.getTiempoLlegada();
    }

    /**
     * Calcula el Tiempo de Espera de un proceso individual:
     * Tespera = Tretorno - TiempoRafagaOriginal
     */
    public double calculateWaitTime(Proceso p) {
        double tiempoRetorno = calculateReturnTime(p);
        return tiempoRetorno - p.getTiempoRafaga();
    }

    /**
     * Calcula el Tiempo de Retorno promedio de todos los procesos terminados.
     */
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

    /**
     * Calcula el Tiempo de Espera promedio de todos los procesos terminados.
     */
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

    /**
     * Retorna la tasa porcentual de eficiencia del procesador, comparando
     * los ciclos donde estuvo ejecutando tareas versus el tiempo total
     * transcurrido de la simulación (incluye el tiempo ocioso o idle).
     */
    public synchronized double calculateCpuUtilization(int tiempoTotal) {
        if (tiempoTotal <= 0) {
            return 0.0;
        }
        return ((double) ciclosOcupados / tiempoTotal) * 100.0;
    }

    /**
     * Calcula la métrica de Productividad (Throughput) del sistema:
     * Productividad = Procesos Terminados / Tiempo Total
     */
    public synchronized double calculateThroughput(int tiempoTotal) {
        if (tiempoTotal <= 0) {
            return 0.0;
        }
        return (double) historialTerminados.size() / tiempoTotal;
    }
}