package org.example.model;

public class Proceso {
    private int id;
    private String nombre;
    private int tiempoRafaga;
    private String estado;

    public Proceso(int id, String nombre, int tiempoRafaga) {
        this.id = id;
        this.nombre = nombre;
        this.tiempoRafaga = tiempoRafaga;
        this.estado = "Nuevo"; // Todos los procesos inician en este estado
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
}