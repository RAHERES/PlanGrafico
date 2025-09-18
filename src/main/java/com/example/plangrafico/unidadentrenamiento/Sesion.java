package com.example.plangrafico.unidadentrenamiento;

/*
public final class Sesion {
    private long id;
    private long microcicloId;
    private LocalDate fecha;
    private String foco;             // p.ej. Resistencia, Velocidad, Técnica
    private int duracionMin;
    private String notas;
    // En futuro: ejercicios, series, etc.
}
*/

import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

/**
 * Representa una sesión dentro de una {@code UnidadEntrenamiento} (un día concreto).
 * <p>
 * La sesión se compone de una colección de {@link Tarea} distribuidas en tres fases:
 * {@link Fase#PREPARATORIA} (calentamiento), {@link Fase#PRINCIPAL} (parte principal)
 * y {@link Fase#FINAL} (vuelta a la calma). Además, dispone de horario planificado
 * (obligatorio) y horario real (opcional al cierre), y permite calcular carga
 * interna tipo sRPE a nivel de sesión.
 *
 * <h2>Reglas principales</h2>
 * <ul>
 *   <li>Horario planificado válido: {@code horaFinPlan > horaInicioPlan}.</li>
 *   <li>Coherencia tiempo: la suma de minutos de las tareas debe aproximarse
 *       a la duración planificada dentro de una tolerancia configurable.</li>
 *   <li>Cobertura de fases (opcional/según política): al menos 1 tarea en PREPARATORIA,
 *       PRINCIPAL y FINAL.</li>
 *   <li>Al marcar como REALIZADA, debe existir duración real &gt; 0.</li>
 * </ul>
 */
public class Sesion {
// ===== Ciclo de vida =====
    /**
     * Estados del ciclo de vida de la sesión.
     */
    public enum Estado {
        /** La sesión está programada pero aún no se ejecuta. */
        PLANIFICADA,
        /** La sesión se está ejecutando en el momento. */
        EN_CURSO,
        /** La sesión terminó y ya se registraron los datos reales. */
        REALIZADA,
        /** La sesión no se ejecutó; requiere {@link #motivoCancelacion}. */
        CANCELADA
    }

    public enum Fase { PREPARATORIA, PRINCIPAL, FINAL }
    public enum Area { PF, TT }
    public enum TipoTarea {
        CALENTAMIENTO, PREVENCION, FUERZA, RESISTENCIA, VELOCIDAD,
        TECNICA, TACTICA, JUEGO_REDUCIDO, REGENERATIVO, VIDEO, OTRO
    }
    public enum Intensidad { MUY_BAJA, BAJA, MEDIA, ALTA, MUY_ALTA }
    public enum EstadoSesion { PLANIFICADA, EN_CURSO, REALIZADA, CANCELADA }
// ===== Identidad y vínculo =====
    /** Identificador único de la sesión (UUID v4). */
    private final UUID id = UUID.randomUUID();
    /** Identificador de la {@code UnidadEntrenamiento} (día) a la que pertenece esta sesión. */
    private final UUID unidadId;
    /** Orden de la sesión dentro del día (1..N). Se usa en UI para ordenar cronológicamente. */
    private int orden;
// ===== Estado, objetivo y notas =====
    /** Estado del ciclo de vida (por defecto {@link Estado#PLANIFICADA}). */
    private Estado estado = Estado.PLANIFICADA;
    /** Objetivo pedagógico general de la sesión (formato SMART recomendado). */
    private String objetivoGeneral;
    /** Observaciones e incidencias (lesiones, clima, adaptaciones, etc.). */
    private String notas;
    /** Motivo en caso de cancelación. Requerido si {@link #estado} = {@link Estado#CANCELADA}. */
    private String motivoCancelacion;
// ===== Horario planificado y real =====
    /** Hora de inicio planificada (HH:mm). Obligatoria. */
    private LocalTime horaInicioPlan;
    /** Hora de fin planificada (HH:mm). Debe ser posterior a {@link #horaInicioPlan}. */
    private LocalTime horaFinPlan;
    /** Hora de inicio real (HH:mm). Opcional; se registra al ejecutar/cerrar. */
    private LocalTime horaInicioReal;
    /** Hora de fin real (HH:mm). Opcional; se registra al ejecutar/cerrar. */
    private LocalTime horaFinReal;
// ===== Control de carga =====
    /**
     * RPE global de la sesión (0–10). Opcional.
     * <p>Si está presente y la sesión está realizada, {@link #cargaSRPE()}
     * utilizará {@code duracionRealMin * rpeSesion}. Si no hay horario real,
     * usa {@code duracionPlanMin * rpeSesion}. En ausencia de este valor,
     * la carga se calcula como la suma de {@code cargaLocal} de las tareas.
     */
    private Integer rpeSesion;
// ===== Contenido =====
    /**
     * Tareas (ejercicios) de la sesión. Cada tarea pertenece a una fase
     * (PREPARATORIA/PRINCIPAL/FINAL). No deben existir tareas sin fase.
     */
    private final List<Tarea> tareas = new ArrayList<>();
// --------------------------------------------------------------------------------------------
    /**
     * Crea una sesión con horario planificado obligatorio.
     *
     * @param unidadId  ID de la unidad (día) a la que pertenece esta sesión (no nulo).
     * @param orden     posición dentro del día (1..N). Si &lt; 1, se ajusta a 1.
     * @param iniPlan   hora de inicio planificada (no nula).
     * @param finPlan   hora de fin planificada (no nula y &gt; iniPlan).
     * @throws NullPointerException     si {@code unidadId}, {@code iniPlan} o {@code finPlan} son nulos.
     * @throws IllegalArgumentException si el horario planificado es inválido.
     */
    public Sesion(UUID unidadId, int orden, LocalTime iniPlan, LocalTime finPlan) {
        this.unidadId = Objects.requireNonNull(unidadId, "unidadId requerido");
        this.orden = Math.max(1, orden);
        this.horaInicioPlan = Objects.requireNonNull(iniPlan, "horaInicioPlan requerida");
        this.horaFinPlan = Objects.requireNonNull(finPlan, "horaFinPlan requerida");
        if (!horaFinPlan.isAfter(horaInicioPlan)) {
            throw new IllegalArgumentException("Horario planificado inválido: fin debe ser > inicio");
        }
    }
// ===== Operaciones de dominio =====
    /**
     * Agrega una tarea a la sesión.
     *
     * @param t tarea no nula, con fase definida y minutos &gt; 0.
     * @return this (fluido).
     * @throws NullPointerException     si {@code t} es nula.
     * @throws IllegalArgumentException si la tarea no tiene fase o minutos válidos.
     */
    public Sesion agregarTarea(Tarea t) {
        Objects.requireNonNull(t, "tarea requerida");
        if (t.fase() == null) throw new IllegalArgumentException("La tarea debe tener fase (Calentamiento/Principal/Final).");
        if (t.minutos() <= 0) throw new IllegalArgumentException("La tarea debe tener minutos > 0.");
        this.tareas.add(t);
        return this;
    }
    /**
     * Reprograma el horario planificado de la sesión.
     *
     * @param ini nueva hora de inicio planificada (no nula).
     * @param fin nueva hora de fin planificada (no nula y &gt; ini).
     * @return this (fluido).
     * @throws IllegalArgumentException si el horario es inválido.
     */
    public Sesion reprogramarPlan(LocalTime ini, LocalTime fin) {
        Objects.requireNonNull(ini, "inicio requerido");
        Objects.requireNonNull(fin, "fin requerido");
        if (!fin.isAfter(ini)) throw new IllegalArgumentException("Horario planificado inválido: fin debe ser > inicio");
        this.horaInicioPlan = ini;
        this.horaFinPlan = fin;
        return this;
    }
    /**
     * Marca la sesión como realizada registrando el horario real.
     *
     * @param ini hora real de inicio (no nula).
     * @param fin hora real de fin (no nula y &gt; ini).
     * @return this (fluido).
     * @throws IllegalArgumentException si el horario real es inválido.
     */
    public Sesion marcarRealizada(LocalTime ini, LocalTime fin) {
        Objects.requireNonNull(ini, "inicio real requerido");
        Objects.requireNonNull(fin, "fin real requerido");
        if (!fin.isAfter(ini)) throw new IllegalArgumentException("Horario real inválido: fin debe ser > inicio");
        this.horaInicioReal = ini;
        this.horaFinReal = fin;
        this.estado = Estado.REALIZADA;
        return this;
    }
    /**
     * Marca la sesión como cancelada e informa el motivo.
     *
     * @param motivo texto no vacío que explique la cancelación.
     * @return this (fluido).
     * @throws IllegalArgumentException si el motivo es nulo o en blanco.
     */
    public Sesion cancelar(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Motivo de cancelación requerido");
        }
        this.estado = Estado.CANCELADA;
        this.motivoCancelacion = motivo;
        return this;
    }
    /**
     * Cambia el estado a EN_CURSO sin registrar horario real (opcional).
     *
     * @return this (fluido).
     */
    public Sesion iniciar() {
        this.estado = Estado.EN_CURSO;
        return this;
    }
// ===== Derivados y cálculos =====
    /**
     * Devuelve la duración planificada en minutos ({@code finPlan - iniPlan}).
     *
     * @return minutos planificados (siempre &gt; 0).
     */
    public int duracionPlanMin() {
        return (int) Duration.between(horaInicioPlan, horaFinPlan).toMinutes();
    }
    /**
     * Devuelve la duración real en minutos si existe horario real registrado.
     *
     * @return minutos reales; 0 si no hay horario real válido.
     */
    public int duracionRealMin() {
        if (horaInicioReal == null || horaFinReal == null) return 0;
        if (!horaFinReal.isAfter(horaInicioReal)) return 0;
        return (int) Duration.between(horaInicioReal, horaFinReal).toMinutes();
    }
    /**
     * Suma de minutos de todas las tareas de la sesión.
     *
     * @return minutos agregados de tareas.
     */
    public int minutosTareas() {
        return tareas.stream().mapToInt(Tarea::minutos).sum();
    }
    /**
     * Minutos totales por fase.
     *
     * @param f fase objetivo (no nula).
     * @return suma de minutos de tareas en la fase indicada.
     */
    public int minutosFase(Fase f) {
        Objects.requireNonNull(f, "fase requerida");
        return tareas.stream().filter(t -> t.fase() == f).mapToInt(Tarea::minutos).sum();
    }
    /**
     * Minutos totales por área (PF/TT).
     *
     * @param a área objetivo (no nula).
     * @return suma de minutos de tareas en el área indicada.
     */
    public int minutosPorArea(Area a) {
        Objects.requireNonNull(a, "área requerida");
        return tareas.stream().filter(t -> t.area() == a).mapToInt(Tarea::minutos).sum();
    }
    /**
     * Calcula la carga de la sesión (sRPE).
     * <ul>
     *   <li>Si existe {@link #rpeSesion}, usa {@code duracion * rpeSesion} (real si está disponible, si no plan).</li>
     *   <li>En otro caso, devuelve la suma de {@code cargaLocal} de las tareas.</li>
     * </ul>
     *
     * @return carga interna tipo sRPE.
     */
    public int cargaSRPE() {
        if (rpeSesion != null) {
            int dur = duracionRealMin() > 0 ? duracionRealMin() : duracionPlanMin();
            return dur * rpeSesion;
        }
        return tareas.stream().mapToInt(Tarea::cargaLocal).sum();
    }
    /**
     * Valida consistencia de la sesión respecto a fases, tiempos y estado.
     *
     * @param toleranciaRelativa tolerancia proporcional sobre la duración planificada (ej.: 0.10 = 10%).
     * @param toleranciaAbsMin   tolerancia mínima absoluta en minutos (ej.: 5).
     * @return lista de errores (vacía si la sesión es válida).
     */
    public List<String> validar(double toleranciaRelativa, int toleranciaAbsMin) {
        var errs = new ArrayList<String>();
// Cobertura de fases (puedes volverla obligatoria quitando los condicionales)
        for (var f : Fase.values()) {
            if (tareas.stream().noneMatch(t -> t.fase() == f)) {
                errs.add("Falta al menos una tarea en la fase: " + f);
            }
        }
// Coherencia minutos de tareas vs duración planificada
        int plan = duracionPlanMin();
        int sum = minutosTareas();
        int tol = Math.max((int) Math.round(plan * toleranciaRelativa), toleranciaAbsMin);
        if (Math.abs(sum - plan) > tol) {
            errs.add("Minutos de tareas (" + sum + ") no concuerdan con la duración planificada (" + plan + ") ±" + tol + ".");
        }
// Estado REALIZADA exige duración real
        if (estado == Estado.REALIZADA && duracionRealMin() <= 0) {
            errs.add("Para estado REALIZADA debe existir duración real > 0 (inicio/fin reales).");
        }
// Cancelada exige motivo
        if (estado == Estado.CANCELADA && (motivoCancelacion == null || motivoCancelacion.isBlank())) {
            errs.add("Motivo de cancelación requerido.");
        }
        return errs;
    }
// ===== Setters fluidos de campos no críticos (estilo DSL) =====
    /**
     * Define/actualiza el objetivo general de la sesión.
     * @param objetivo texto objetivo (puede ser nulo o vacío).
     * @return this (fluido).
     */
    public Sesion conObjetivoGeneral(String objetivo) { this.objetivoGeneral = objetivo; return this; }
    /**
     * Define/actualiza notas de la sesión.
     * @param n texto libre (puede ser nulo o vacío).
     * @return this (fluido).
     */
    public Sesion conNotas(String n) { this.notas = n; return this; }
    /**
     * Define/actualiza el RPE global de la sesión (0–10).
     * @param rpe valor entero 0..10 (puede ser nulo para desactivar uso global).
     * @return this (fluido).
     * @throws IllegalArgumentException si está fuera de rango.
     */
    public Sesion conRpeSesion(Integer rpe) {
        if (rpe != null && (rpe < 0 || rpe > 10)) throw new IllegalArgumentException("RPE de sesión fuera de rango (0-10)");
        this.rpeSesion = rpe;
        return this;
    }
    /**
     * Cambia el orden de la sesión dentro del día.
     * @param nuevoOrden entero ≥ 1.
     * @return this (fluido).
     * @throws IllegalArgumentException si {@code nuevoOrden} &lt; 1.
     */
    public Sesion conOrden(int nuevoOrden) {
        if (nuevoOrden < 1) throw new IllegalArgumentException("orden debe ser ≥ 1");
        this.orden = nuevoOrden;
        return this;
    }
    /**
     * Cambia el estado (sin efectos colaterales). Preferir {@link #iniciar()},
     * {@link #marcarRealizada(LocalTime, LocalTime)} o {@link #cancelar(String)}.
     * @param e nuevo estado (no nulo).
     * @return this (fluido).
     */
    public Sesion conEstado(Estado e) { this.estado = Objects.requireNonNull(e); return this; }
// ===== Getters públicos =====
    /** @return ID único de la sesión. */
    public UUID id() { return id; }
    /** @return ID de la unidad (día) a la que pertenece. */
    public UUID unidadId() { return unidadId; }
    /** @return orden de la sesión dentro del día (1..N). */
    public int orden() { return orden; }
    /** @return estado actual de la sesión. */
    public Estado estado() { return estado; }
    /** @return objetivo general (puede ser nulo). */
    public Optional<String> objetivoGeneral() { return Optional.ofNullable(objetivoGeneral); }
    /** @return notas (pueden ser nulas). */
    public Optional<String> notas() { return Optional.ofNullable(notas); }
    /** @return motivo de cancelación (si aplica). */
    public Optional<String> motivoCancelacion() { return Optional.ofNullable(motivoCancelacion); }
    /** @return hora de inicio planificada (no nula). */
    public LocalTime horaInicioPlan() { return horaInicioPlan; }
    /** @return hora de fin planificada (no nula). */
    public LocalTime horaFinPlan() { return horaFinPlan; }
    /** @return hora de inicio real (opcional). */
    public Optional<LocalTime> horaInicioReal() { return Optional.ofNullable(horaInicioReal); }
    /** @return hora de fin real (opcional). */
    public Optional<LocalTime> horaFinReal() { return Optional.ofNullable(horaFinReal); }
    /** @return RPE global de la sesión (opcional). */
    public Optional<Integer> rpeSesion() { return Optional.ofNullable(rpeSesion); }
    /**
     * Tareas inmutables (copia de solo lectura).
     * @return lista no modificable de tareas.
     */
    public List<Tarea> tareas() { return Collections.unmodifiableList(tareas); }
}
