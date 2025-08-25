package com.example.plangrafico;





import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.*;
import java.util.function.Consumer;

/**
 * Representa el plan gráfico completo (macrociclo) y su timeline:
 *  - Modelo de periodización: TRADICIONAL o ATR
 *  - Rango temporal del macrociclo
 *  - Lista de mesociclos (tipo, fechas, resumen de carga)
 *  - Cada mesociclo contiene microciclos (tipo, fechas, % de carga, métricas)
 *  - Eventos: competencias, evaluaciones/controles
 */

public final class PlanGrafico {
    private long id;

    // —— Datos base del plan ——
    private final String deporte;
    private final String categoria; // p.ej. Sub 13 / Sub 15 / Sub 17
    private final String equipo;    // p.ej. Villa de las Niñas
    private final ModeloPeriodizacion modelo;
    private String nombre;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private TipoPeriodizacion tipo;
    private List<Periodo> periodos = new ArrayList<>();
    private String notas;
    private int version;             // versionado del plan
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;


    public static Builder builder() { return new Builder(); }

    // —— Estructura ——
    private final List<Mesociclo> mesociclos = new ArrayList<>();
    private final List<EventoDeportivo> eventos = new ArrayList<>();

    public  PlanGrafico(){
        this( PlanGrafico.builder()
                .deporte("")
                .categoria("")
                .equipo("")
                .inicio(LocalDate.now())
                .fin(LocalDate.now()));

    }
    private PlanGrafico(Builder b) {
        this.deporte = Objects.requireNonNull(b.deporte, "deporte");
        this.categoria = Objects.requireNonNull(b.categoria, "categoria");
        this.equipo = Objects.requireNonNull(b.equipo, "equipo");
        this.modelo = Objects.requireNonNull(b.modelo, "modelo");
        this.fechaInicio = Objects.requireNonNull(b.inicio, "inicio");
        this.fechaFin = Objects.requireNonNull(b.fin, "fin");
        if (!this.fechaFin.isAfter(this.fechaInicio)) {
            throw new IllegalArgumentException("La fecha fin debe ser posterior al inicio");
        }
        this.mesociclos.addAll(b.mesociclos);
        this.eventos.addAll(b.eventos);
    }
    // getters/setters

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public TipoPeriodizacion getTipo() { return tipo; }
    public void setTipo(TipoPeriodizacion tipo) { this.tipo = tipo; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(LocalDateTime actualizadoEn) { this.actualizadoEn = actualizadoEn; }

    // =========================================================
    // =====================   BUILDER   =======================
    // =========================================================
    public static class Builder {
        private String deporte;
        private String categoria;
        private String equipo;
        private ModeloPeriodizacion modelo = ModeloPeriodizacion.TRADICIONAL;
        private LocalDate inicio;
        private LocalDate fin;
        private final List<Mesociclo> mesociclos = new ArrayList<>();
        private final List<EventoDeportivo> eventos = new ArrayList<>();

        public Builder deporte(String v) { this.deporte = v; return this; }
        public Builder categoria(String v) { this.categoria = v; return this; }
        public Builder equipo(String v) { this.equipo = v; return this; }
        public Builder modelo(ModeloPeriodizacion v) { this.modelo = v; return this; }
        public Builder inicio(LocalDate v) { this.inicio = v; return this; }
        public Builder fin(LocalDate v) { this.fin = v; return this; }

        public Builder addMesociclo(Consumer<Mesociclo.Builder> cfg) {
            Mesociclo.Builder mb = new Mesociclo.Builder();
            cfg.accept(mb);
            this.mesociclos.add(mb.build());
            return this;
        }

        public Builder addEvento(Consumer<EventoDeportivo.Builder> cfg) {
            EventoDeportivo.Builder eb = new EventoDeportivo.Builder();
            cfg.accept(eb);
            this.eventos.add(eb.build());
            return this;
        }

        public PlanGrafico build() {
            return new PlanGrafico(this);
        }
    }

    /** Métricas de carga por microciclo. */
    public record Carga(
            int volumenMinutos,      // minutos totales del micro
            double intensidadRel,    // 0–1 (puedes mapear %1RM, %MAS, RPE)
            double densidad,         // trabajo/pausa (p.ej. 2.0 equivale a 2:1)
            int frecuenciaSesiones   // nº de sesiones en la semana
    ) {
        public static Carga vacia() { return new Carga(0, 0.0, 0.0, 0); }
    }

    /** Promedios de mesociclo (para pintar “líneas” o tooltips). */
    public record ResumenCarga(
            double volumenPromedioMin, double intensidadPromedio, double densidadPromedio, double frecuenciaPromedio
    ) {
        public static ResumenCarga cero() { return new ResumenCarga(0,0,0,0); }
    }

    // =========================================================
// ==================   VALIDACIONES   =====================
// =========================================================

    /** Retorna lista de problemas del plan (para mostrar en UI). */
    public List<String> validar() {
        List<String> issues = new ArrayList<>();
        // 1) Mesociclos dentro del macrociclo y sin solaparse (opcional permitir solapes)
        for (Mesociclo m : mesociclos) {
            if (m.getInicio().isBefore(fechaInicio) || m.getFin().isAfter(fechaFin)) {
                issues.add("Mesociclo fuera de rango: " + m.getNombre());
            }
        }
        // solapamientos
        List<Mesociclo> orden = new ArrayList<>(mesociclos);
        orden.sort(Comparator.comparing(Mesociclo::getInicio));
        for (int i = 1; i < orden.size(); i++) {
            Mesociclo prev = orden.get(i - 1);
            Mesociclo curr = orden.get(i);
            if (!curr.getInicio().isAfter(prev.getFin())) {
                issues.add("Solapamiento entre mesociclos: " + prev.getNombre() + " y " + curr.getNombre());
            }
        }
        // 2) Eventos dentro del macrociclo
        for (EventoDeportivo e : eventos) {
            if (e.getFecha().isBefore(fechaInicio) || e.getFecha().isAfter(fechaFin)) {
                issues.add("Evento fuera del rango del macrociclo: " + e.getNombre());
            }
        }
        // 3) Microciclos consecutivos (opcional): mismo mesociclo sin huecos
        for (Mesociclo m : mesociclos) {
            List<Microciclo> ms = new ArrayList<>(m.getMicrociclos());
            ms.sort(Comparator.comparing(Microciclo::getInicio));
            for (int i = 1; i < ms.size(); i++) {
                LocalDate expected = ms.get(i - 1).getFin().plusDays(1);
                if (!ms.get(i).getInicio().equals(expected)) {
                    issues.add("Hueco entre microciclos en " + m.getNombre() + ": "
                            + ms.get(i - 1).getFin() + " → " + ms.get(i).getInicio());
                }
            }
        }
        return issues;
    }

    // =========================================================
    // ==================   UTILIDADES   =======================
    // =========================================================

    /** Semanas totales del macrociclo. */
    public long semanasTotales() {
        return Duration.between(fechaInicio.atStartOfDay(), fechaFin.plusDays(1).atStartOfDay()).toDays() / 7;
    }

    /** Distribuye minutos totales de un mesociclo entre microciclos siguiendo su % de carga. */
    public static Map<Microciclo, Integer> distribuirMinutosPorcentaje(int minutosMesociclo, List<Microciclo> micros) {
        int sumaPct = micros.stream().mapToInt(Microciclo::getPorcentajeCarga).sum();
        if (sumaPct <= 0) return Collections.emptyMap();
        Map<Microciclo, Integer> out = new LinkedHashMap<>();
        int asignados = 0;
        for (int i = 0; i < micros.size(); i++) {
            Microciclo m = micros.get(i);
            int min = (int)Math.round(minutosMesociclo * (m.getPorcentajeCarga() / (double)sumaPct));
            // Ajuste en el último para cuadrar redondeos
            if (i == micros.size() - 1) min = minutosMesociclo - asignados;
            out.put(m, min);
            asignados += min;
        }
        return out;
    }

    /** Devuelve una vista “flat” para graficar en JavaFX (eje X = días, Y = % carga). */
    public List<PuntoCarga> serieCargaDiaria() {
        List<PuntoCarga> serie = new ArrayList<>();
        for (Mesociclo meso : mesociclos) {
            for (Microciclo micro : meso.getMicrociclos()) {
                LocalDate d = micro.getInicio();
                while (!d.isAfter(micro.getFin())) {
                    serie.add(new PuntoCarga(d, micro.getPorcentajeCarga()));
                    d = d.plusDays(1);
                }
            }
        }
        serie.sort(Comparator.comparing(PuntoCarga::fecha));
        return serie;
    }

    /** Agrupa minutos por contenido (Fuerza/Técnica/Táctica/…) en todo el macrociclo. */
    public Map<String, Integer> resumenMinutosPorContenido() {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (Mesociclo meso : mesociclos) {
            for (Microciclo micro : meso.getMicrociclos()) {
                micro.getMinutosPorContenido().forEach((k, v) -> map.merge(k, v, Integer::sum));
            }
        }
        return map;
    }

    // =========================================================
    // ==================   DTOs de salida   ===================
    // =========================================================
    public record PuntoCarga(LocalDate fecha, int porcentaje) {}

    // =========================================================
    // ==================   GETTERS BASE   =====================
    // =========================================================
    public String getDeporte() { return deporte; }
    public String getCategoria() { return categoria; }
    public String getEquipo() { return equipo; }
    public ModeloPeriodizacion getModelo() { return modelo; }
    public LocalDate getInicio() { return fechaInicio; }
    public LocalDate getFin() { return fechaFin; }
    public List<Mesociclo> getMesociclos() { return Collections.unmodifiableList(mesociclos); }
    public List<EventoDeportivo> getEventos() { return Collections.unmodifiableList(eventos); }


}


