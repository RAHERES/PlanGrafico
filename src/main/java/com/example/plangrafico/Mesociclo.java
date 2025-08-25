package com.example.plangrafico;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import com.example.plangrafico.PlanGrafico.*;

public final class Mesociclo {
    private long id;
    private long periodoId;
    private MesocicloTipo tipo;
    private LocalDate inicio;
    private LocalDate fin;
    private int semanas;
    private List<Microciclo> microciclos = new ArrayList<>();
    private String objetivo;         // p.ej. fuerza máxima, potencia, etc.


    private final String nombre; // opcional: “M1 – Básico Desarrollador”, etc.
    private final ResumenCarga resumen; // promedios del mesociclo


    private Mesociclo(Builder b) {
        this.tipo = Objects.requireNonNull(b.tipo, "tipo");
        this.inicio = Objects.requireNonNull(b.inicio, "inicio");
        this.fin = Objects.requireNonNull(b.fin, "fin");
        if (!this.fin.isAfter(this.inicio)) {
            throw new IllegalArgumentException("Mesociclo.fin debe ser posterior al inicio");
        }
        this.nombre = b.nombre != null ? b.nombre : this.tipo.name();
        this.resumen = b.resumen != null ? b.resumen : ResumenCarga.cero();
        this.microciclos = List.copyOf(b.microciclos);
        // Validación básica: microciclos dentro del rango del mesociclo
        for (Microciclo m : microciclos) {
            if (m.getInicio().isBefore(this.inicio) || m.getFin().isAfter(this.fin)) {
                throw new IllegalArgumentException("Microciclo fuera del rango del mesociclo: " + m);
            }
        }
    }

    public static class Builder {
        private MesocicloTipo tipo;
        private LocalDate inicio;
        private LocalDate fin;
        private String nombre;
        private ResumenCarga resumen;
        private final List<Microciclo> microciclos = new ArrayList<>();

        public Builder tipo(MesocicloTipo v) { this.tipo = v; return this; }
        public Builder inicio(LocalDate v) { this.inicio = v; return this; }
        public Builder fin(LocalDate v) { this.fin = v; return this; }
        public Builder nombre(String v) { this.nombre = v; return this; }
        public Builder resumen(ResumenCarga v) { this.resumen = v; return this; }

        public Builder addMicrociclo(Consumer<Microciclo.Builder> cfg) {
            Microciclo.Builder micB = new Microciclo.Builder();
            cfg.accept(micB);
            this.microciclos.add(micB.build());
            return this;
        }

        public Mesociclo build() { return new Mesociclo(this); }
    }

    public MesocicloTipo getTipo() { return tipo; }
    public LocalDate getInicio() { return inicio; }
    public LocalDate getFin() { return fin; }
    public String getNombre() { return nombre; }
    public ResumenCarga getResumen() { return resumen; }
    public List<Microciclo> getMicrociclos() { return microciclos; }

    public long semanas() { return Duration.between(inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay()).toDays() / 7; }

}
