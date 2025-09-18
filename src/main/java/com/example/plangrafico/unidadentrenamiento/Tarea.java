package com.example.plangrafico.unidadentrenamiento;

import java.util.*;
import com.example.plangrafico.unidadentrenamiento.Sesion.*;
/**
 * Representa un ejercicio o actividad ejecutable dentro de una {@link Sesion},
 * asignado a una {@link Fase} (PREPARATORIA, PRINCIPAL o FINAL).
 * <p>
 * Cada tarea pertenece también a un área ({@link Area#PF} o {@link Area#TT}),
 * tiene un tipo ({@link TipoTarea}) e intensidad cualitativa ({@link Intensidad}),
 * una duración en minutos y métricas opcionales (series, repeticiones, pausas, distancia, carga).
 * Puede registrar un RPE local para estimar carga interna (sRPE).
 *
 * <h2>Reglas principales</h2>
 * <ul>
 *   <li>Debe tener {@code fase}, {@code area}, {@code tipo}, {@code intensidad}.</li>
 *   <li>{@code minutos} &gt; 0.</li>
 *   <li>Se recomienda mantener coherencia entre series×(trabajo+pausa) y {@code minutos}.</li>
 * </ul>
 */
public class Tarea {
// ===== Identidad y orden =====
    /** Identificador único de la tarea (UUID v4). */
    private final UUID id = UUID.randomUUID();
    /**
     * Posición relativa para ordenar la tarea dentro de su fase (1..N).
     * No es obligatorio pero altamente recomendado para UI/impresión.
     */
    private int ordenEnFase = 1;
// ===== Clasificación y atributos básicos =====
    /** Fase de la sesión en la que se ejecuta la tarea (PREPARATORIA, PRINCIPAL o FINAL). */
    private final Sesion.Fase fase;
    /** Área de trabajo: Preparación Física (PF) o Técnico-Táctico (TT). */
    private final Area area;
    /** Tipo de tarea (calentamiento, fuerza, velocidad, técnica, táctica, JR, etc.). */
    private final TipoTarea tipo;
    /** Intensidad cualitativa percibida (MUY_BAJA..MUY_ALTA). */
    private final Intensidad intensidad;
    /** Duración de la tarea en minutos (&gt; 0). */
    private int minutos;
// ===== Objetivo, contenido y recursos =====
    /** Objetivo concreto de la tarea (breve, idealmente SMART). */
    private String objetivo;
    /** Contenido: consignas, organización, dimensiones, reglas, progresiones. */
    private String contenido;
    /** Materiales requeridos (conos, balones, escaleras, costales 10 kg, chaleco lastrado, etc.). */
    private List<String> materiales = new ArrayList<>();
    /** Etiquetas libres para búsqueda/filtrado (p. ej., "1v1", "posesión", "aceleración"). */
    private Set<String> tags = new HashSet<>();
// ===== Métricas opcionales =====
    /** Número de series (si aplica). */
    private Integer series;
    /** Número de repeticiones por serie (si aplica). */
    private Integer repeticiones;
    /** Pausa entre esfuerzos, en segundos (si aplica). */
    private Integer pausaSeg;
    /** Distancia acumulada estimada, en metros (si aplica). */
    private Integer distanciaM;
    /** Carga externa estimada, en kg (si aplica, p. ej., fuerza). */
    private Double cargaKg;
// ===== Carga interna =====
    /** RPE local de la tarea (0–10). Opcional. */
    private Integer rpeLocal;
// ------------------------------------------------------------------------------------------------
    /**
     * Crea una tarea con los campos mínimos requeridos.
     *
     * @param fase       fase de la sesión donde se ubica (no nula).
     * @param area       área de trabajo (PF/TT) (no nula).
     * @param tipo       tipo de tarea (no nulo).
     * @param intensidad intensidad cualitativa (no nula).
     * @param minutos    duración &gt; 0.
     * @throws NullPointerException     si algún enum requerido es nulo.
     * @throws IllegalArgumentException si {@code minutos} ≤ 0.
     */
    public Tarea(Sesion.Fase fase, Sesion.Area area, Sesion.TipoTarea tipo, Sesion.Intensidad intensidad, int minutos) {
        this.fase = Objects.requireNonNull(fase, "fase requerida");
        this.area = Objects.requireNonNull(area, "area requerida");
        this.tipo = Objects.requireNonNull(tipo, "tipo requerido");
        this.intensidad = Objects.requireNonNull(intensidad, "intensidad requerida");
        if (minutos <= 0) throw new IllegalArgumentException("minutos debe ser > 0");
        this.minutos = minutos;
    }
// ===== Fábricas de conveniencia (fase explícita) =====
    /**
     * Crea una tarea directamente en la fase PREPARATORIA (calentamiento).
     */
    public static Tarea calentamiento(Area a, TipoTarea t, Intensidad i, int min) {
        return new Tarea(Fase.PREPARATORIA, a, t, i, min);
    }
    /**
     * Crea una tarea directamente en la fase PRINCIPAL (parte principal).
     */
    public static Tarea principal(Area a, TipoTarea t, Intensidad i, int min) {
        return new Tarea(Fase.PRINCIPAL, a, t, i, min);
    }
    /**
     * Crea una tarea directamente en la fase FINAL (vuelta a la calma).
     */
    public static Tarea finalizacion(Area a, TipoTarea t, Intensidad i, int min) {
        return new Tarea(Fase.FINAL, a, t, i, min);
    }
// ===== API fluida (setters) =====
    /**
     * Define/actualiza el orden dentro de la fase (1..N).
     * @param orden entero ≥ 1.
     * @return this (fluido).
     * @throws IllegalArgumentException si &lt; 1.
     */
    public Tarea conOrdenEnFase(int orden) {
        if (orden < 1) throw new IllegalArgumentException("ordenEnFase debe ser ≥ 1");
        this.ordenEnFase = orden;
        return this;
    }
    /**
     * Define/actualiza el objetivo concreto de la tarea.
     * @param s texto (puede ser nulo o vacío).
     * @return this (fluido).
     */
    public Tarea conObjetivo(String s) { this.objetivo = s; return this; }
    /**
     * Define/actualiza el contenido (consignas, organización, dimensiones, reglas).
     * @param s texto (puede ser nulo o vacío).
     * @return this (fluido).
     */
    public Tarea conContenido(String s) { this.contenido = s; return this; }
    /**
     * Define/actualiza la lista de materiales requeridos.
     * @param l lista (nula ⇒ vacía).
     * @return this (fluido).
     */
    public Tarea conMateriales(List<String> l) {
        this.materiales = (l == null) ? new ArrayList<>() : new ArrayList<>(l);
        return this;
    }
    /**
     * Define/actualiza las etiquetas de la tarea.
     * @param t colección de tags (nula ⇒ vacía).
     * @return this (fluido).
     */
    public Tarea conTags(Collection<String> t) {
        this.tags = (t == null) ? new HashSet<>() : new HashSet<>(t);
        return this;
    }
    /**
     * Define/actualiza el RPE local (0–10).
     * @param r valor 0..10 (nulo para limpiar).
     * @return this (fluido).
     * @throws IllegalArgumentException si está fuera de rango.
     */
    public Tarea conRPE(Integer r) {
        if (r != null && (r < 0 || r > 10)) throw new IllegalArgumentException("RPE local fuera de rango (0-10)");
        this.rpeLocal = r;
        return this;
    }
    /** @return this con {@link #series} configurado. */
    public Tarea conSeries(Integer v) { this.series = v; return this; }
    /** @return this con {@link #repeticiones} configurado. */
    public Tarea conReps(Integer v) { this.repeticiones = v; return this; }
    /** @return this con {@link #pausaSeg} configurado (segundos). */
    public Tarea conPausaSeg(Integer v) { this.pausaSeg = v; return this; }
    /** @return this con {@link #distanciaM} configurado (metros). */
    public Tarea conDistM(Integer v) { this.distanciaM = v; return this; }
    /** @return this con {@link #cargaKg} configurado (kilogramos). */
    public Tarea conCargaKg(Double v) { this.cargaKg = v; return this; }
    /**
     * Actualiza la duración de la tarea.
     * @param min minutos &gt; 0.
     * @return this (fluido).
     * @throws IllegalArgumentException si {@code min} ≤ 0.
     */
    public Tarea conMinutos(int min) {
        if (min <= 0) throw new IllegalArgumentException("minutos debe ser > 0");
        this.minutos = min;
        return this;
    }
// ===== Cálculos y validaciones =====
    /**
     * Estima la carga interna local (sRPE) como {@code minutos × RPE local}.
     * @return carga sRPE local; 0 si no hay RPE local.
     */
    public int cargaLocal() { return (rpeLocal == null) ? 0 : minutos * rpeLocal; }
    /**
     * Verifica condiciones básicas de validez de la tarea.
     * @return lista de errores; vacía si la tarea es válida.
     */
    public List<String> validar() {
        var errs = new ArrayList<String>();
        if (minutos <= 0) errs.add("Duración debe ser > 0.");
        if (fase == null) errs.add("Fase requerida (Calentamiento/Principal/Final).");
        if (area == null) errs.add("Área requerida (PF/TT).");
        if (tipo == null) errs.add("Tipo de tarea requerido.");
        if (intensidad == null) errs.add("Intensidad requerida.");
        if (ordenEnFase < 1) errs.add("Orden en fase debe ser ≥ 1.");
        return errs;
    }
// ===== Getters públicos =====
    /** @return ID único de la tarea. */
    public UUID id() { return id; }
    /** @return fase en la que se ubica la tarea. */
    public Fase fase() { return fase; }
    /** @return área (PF/TT). */
    public Area area() { return area; }
    /** @return tipo de tarea. */
    public TipoTarea tipo() { return tipo; }
    /** @return intensidad cualitativa. */
    public Intensidad intensidad() { return intensidad; }
    /** @return minutos de duración (&gt; 0). */
    public int minutos() { return minutos; }
    /** @return orden relativo dentro de la fase (1..N). */
    public int ordenEnFase() { return ordenEnFase; }
    /** @return objetivo concreto (opcional). */
    public Optional<String> objetivo() { return Optional.ofNullable(objetivo); }
    /** @return contenido/consignas (opcional). */
    public Optional<String> contenido() { return Optional.ofNullable(contenido); }
    /** @return lista inmutable de materiales. */
    public List<String> materiales() { return Collections.unmodifiableList(materiales); }
    /** @return conjunto inmutable de etiquetas. */
    public Set<String> tags() { return Collections.unmodifiableSet(tags); }
    /** @return series (opcional). */
    public Optional<Integer> series() { return Optional.ofNullable(series); }
    /** @return repeticiones por serie (opcional). */
    public Optional<Integer> repeticiones() { return Optional.ofNullable(repeticiones); }
    /** @return pausa entre esfuerzos, en segundos (opcional). */
    public Optional<Integer> pausaSeg() { return Optional.ofNullable(pausaSeg); }
    /** @return distancia acumulada estimada, en metros (opcional). */
    public Optional<Integer> distanciaM() { return Optional.ofNullable(distanciaM); }
    /** @return carga externa estimada, en kg (opcional). */
    public Optional<Double> cargaKg() { return Optional.ofNullable(cargaKg); }
    /** @return RPE local (0–10) (opcional). */
    public Optional<Integer> rpeLocal() { return Optional.ofNullable(rpeLocal); }
    // ===== Utilidades =====
    @Override public String toString() {
        return "Tarea{" +
                "id=" + id +
                ", fase=" + fase +
                ", area=" + area +
                ", tipo=" + tipo +
                ", intensidad=" + intensidad +
                ", minutos=" + minutos +
                ", ordenEnFase=" + ordenEnFase +
                '}';
    }
}
