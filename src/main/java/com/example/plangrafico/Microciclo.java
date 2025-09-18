package com.example.plangrafico;

import java.time.LocalDate;
import java.util.*;
import com.example.plangrafico.PlanGrafico.*;
import com.example.plangrafico.unidadentrenamiento.Sesion;


public final class Microciclo {
    private long id;
    private long mesocicloId;
    private MicrocicloTipo tipo;
    private LocalDate inicio;
    private LocalDate fin;           // normalmente 7 días
    private List<Sesion> sesiones = new ArrayList<>();
    private double cargaInternaObjetivo; // sRPE x duración


    private final int porcentajeCarga; // 0–100 (para graficar alto/bajo)
    private final PlanGrafico.Carga carga;         // volumen, intensidad, densidad, frecuencia
    private final Map<String, Integer> minutosPorContenido; // p.ej. "Fuerza", "Técnica", "Táctica", etc.

    private Microciclo(Builder b) {
        this.tipo = Objects.requireNonNull(b.tipo, "tipo");
        this.inicio = Objects.requireNonNull(b.inicio, "inicio");
        this.fin = Objects.requireNonNull(b.fin, "fin");
        if (!this.fin.isAfter(this.inicio)) {
            throw new IllegalArgumentException("Microciclo.fin debe ser posterior al inicio");
        }
        if (b.porcentajeCarga < 0 || b.porcentajeCarga > 100) {
            throw new IllegalArgumentException("porcentajeCarga 0–100");
        }
        this.porcentajeCarga = b.porcentajeCarga;
        this.carga = b.carga != null ? b.carga : PlanGrafico.Carga.vacia();
        this.minutosPorContenido = Map.copyOf(b.minutosPorContenido);
    }

    public static class Builder {
        private MicrocicloTipo tipo;
        private LocalDate inicio;
        private LocalDate fin;
        private int porcentajeCarga = 0;
        private PlanGrafico.Carga carga;
        private final Map<String, Integer> minutosPorContenido = new LinkedHashMap<>();

        public Builder tipo(MicrocicloTipo v) { this.tipo = v; return this; }
        public Builder inicio(LocalDate v) { this.inicio = v; return this; }
        public Builder fin(LocalDate v) { this.fin = v; return this; }
        public Builder porcentajeCarga(int v) { this.porcentajeCarga = v; return this; }
        public Builder carga(PlanGrafico.Carga v) { this.carga = v; return this; }
        /** Ej.: contenido("Fuerza", 180), contenido("Técnica", 120) */
        public Builder contenido(String nombre, int minutos) {
            this.minutosPorContenido.put(nombre, Math.max(0, minutos));
            return this;
        }
        public Microciclo build() { return new Microciclo(this); }
    }

    public MicrocicloTipo getTipo() { return tipo; }
    public LocalDate getInicio() { return inicio; }
    public LocalDate getFin() { return fin; }
    public int getPorcentajeCarga() { return porcentajeCarga; }
    public Carga getCarga() { return carga; }
    public Map<String, Integer> getMinutosPorContenido() { return minutosPorContenido; }

    public int minutosTotales() {
        return minutosPorContenido.values().stream().mapToInt(Integer::intValue).sum();
    }
}

