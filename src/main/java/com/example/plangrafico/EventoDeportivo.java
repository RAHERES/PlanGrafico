package com.example.plangrafico;


import java.time.LocalDate;
import java.util.Objects;

/** Eventos relevantes: partidos/torneos (COMPETENCIA) y controles (EVALUACION). */
public class EventoDeportivo {
    public enum Tipo { COMPETENCIA, EVALUACION }
    private  Tipo tipo;
    private  String nombre;
    private  LocalDate fecha;
    private  String notas;

    public EventoDeportivo(Builder b) {
        this.tipo = Objects.requireNonNull(b.tipo, "tipo");
        this.nombre = Objects.requireNonNullElse(b.nombre, b.tipo.name());
        this.fecha = Objects.requireNonNull(b.fecha, "fecha");
        this.notas = b.notas;
    }

    public EventoDeportivo() {
       /* this.tipo = Objects.requireNonNull(b.tipo, "tipo");
        this.nombre = Objects.requireNonNullElse(b.nombre, b.tipo.name());
        this.fecha = Objects.requireNonNull(b.fecha, "fecha");
        this.notas = b.notas;*/
    }

    public static class Builder {
        private Tipo tipo;
        private String nombre;
        private LocalDate fecha;
        private String notas;

        public Builder tipo(Tipo v) { this.tipo = v; return this; }
        public Builder nombre(String v) { this.nombre = v; return this; }
        public Builder fecha(LocalDate v) { this.fecha = v; return this; }
        public Builder notas(String v) { this.notas = v; return this; }

        public EventoDeportivo build() { return new EventoDeportivo(this); }
    }

    public Tipo getTipo() { return tipo; }
    public String getNombre() { return nombre; }
    public LocalDate getFecha() { return fecha; }
    public String getNotas() { return notas; }
}

