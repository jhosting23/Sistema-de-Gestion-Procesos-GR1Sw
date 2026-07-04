package org.example.model;

public class Proceso {
    private int id;
    private String nombre;
    private int tiempoRafaga;
    private String estado;
    private int tiempoRestante;
    private int prioridad;
    private int contadorPrograma;
    private int tiempoLlegada;
    private int tiempoFin;
    private int tiempoEsperaAcumulado;

    public Proceso(int id, String nombre, int tiempoRafaga, int prioridad, int tiempoLlegada) {
        this.id = id;
        this.nombre = nombre;
        this.tiempoRafaga = tiempoRafaga;
        this.tiempoRestante = tiempoRafaga;
        this.prioridad = prioridad;
        this.tiempoLlegada = tiempoLlegada;
        this.estado = "Nuevo";
        this.contadorPrograma = 0;
        this.tiempoFin = 0;
        this.tiempoEsperaAcumulado = 0;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getTiempoRafaga() {
        return tiempoRafaga;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public int getTiempoRestante() {
        return tiempoRestante;
    }

    public void decrementarTiempoRestante(int unidades) {
        this.tiempoRestante -= unidades;
        if (this.tiempoRestante < 0) {
            this.tiempoRestante = 0;
        }
    }

    public int getPrioridad() {
        return prioridad;
    }

    public int getContadorPrograma() {
        return contadorPrograma;
    }

    public void incrementarContadorPrograma() {
        this.contadorPrograma++;
    }

    public int getTiempoLlegada() {
        return tiempoLlegada;
    }

    public int getTiempoFin() {
        return tiempoFin;
    }

    public void setTiempoFin(int tiempoFin) {
        this.tiempoFin = tiempoFin;
    }

    public int getTiempoEsperaAcumulado() {
        return tiempoEsperaAcumulado;
    }

    public void incrementarTiempoEspera() {
        this.tiempoEsperaAcumulado++;
    }

    public int getTiempoRetorno() {
        if (!"Terminado".equals(this.estado)) {
            return 0;
        }
        return this.tiempoFin - this.tiempoLlegada;
    }
}