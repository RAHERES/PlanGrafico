package com.example.plangrafico;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.util.*;

public class PlanGraficoCalendarAdapter {

    private final PlanGrafico plan;
    private final Map<LocalDate, DiaMeta> cache = new HashMap<>();

    public PlanGraficoCalendarAdapter(PlanGrafico plan) {
        this.plan = Objects.requireNonNull(plan);
        indexar();
    }

    // =================== API que consumirá tu Calendar ===================
    /** Datos de estilo y metadatos para una celda/día del calendario. */
    public static final class DiaMeta {
        public final LocalDate date;
        public final Mesociclo mesociclo;   // puede ser null
        public final Microciclo microciclo; // puede ser null
        public final List<EventoDeportivo> eventos;  // 0..n
        public final Set<String> styleClasses = new LinkedHashSet<>();
        public final Color fondo;
        public final boolean bordeInicioMicro;
        public final boolean bordeFinMicro;
        public final boolean bordeInicioMeso;
        public final boolean bordeFinMeso;

        DiaMeta(LocalDate date, Mesociclo meso, Microciclo micro,
                List<EventoDeportivo> eventos,
                Color fondo,
                boolean biMic, boolean bfMic, boolean biMes, boolean bfMes) {
            this.date = date;
            this.mesociclo = meso;
            this.microciclo = micro;
            this.eventos = eventos;
            this.fondo = fondo;
            this.bordeInicioMicro = biMic;
            this.bordeFinMicro = bfMic;
            this.bordeInicioMeso = biMes;
            this.bordeFinMeso = bfMes;
        }
    }

    /** Devuelve los metadatos/estilos del día (o null si está fuera del plan). */
    public DiaMeta get(LocalDate date) { return cache.get(date); }

    /** Tooltip sugerido para la celda del día. */
    public Tooltip buildTooltip(LocalDate date) {
        DiaMeta d = cache.get(date);
        if (d == null) return null;
        StringBuilder sb = new StringBuilder();
        if (d.microciclo != null) {
            sb.append("Micro: ").append(d.microciclo.getTipo().getNombreVisible())
                    .append(" | %Carga: ").append(d.microciclo.getPorcentajeCarga());
            if (d.microciclo.getMinutosPorContenido() != null && !d.microciclo.getMinutosPorContenido().isEmpty()) {
                sb.append("\nContenidos:");
                d.microciclo.getMinutosPorContenido().forEach((k,v)-> sb.append("\n • ").append(k).append(": ").append(v).append(" min"));
            }
        }
        if (d.mesociclo != null) {
            sb.append(d.microciclo != null ? "\n" : "")
                    .append("Meso: ").append(d.mesociclo.getNombre())
                    .append(" (").append(d.mesociclo.getTipo().getNombre()).append(")");
        }
        if (!d.eventos.isEmpty()) {
            sb.append("\nEventos:");
            for (EventoDeportivo e : d.eventos) {
                sb.append("\n ★ ").append(e.getTipo()).append(" - ").append(e.getNombre());
            }
        }
        return new Tooltip(sb.toString());
    }

    /** Context menu sugerido para la celda (puedes enganchar tus handlers). */
    public ContextMenu buildContextMenu(LocalDate date, Runnable onVerMicro, Runnable onVerMeso, Runnable onAgregarEvento) {
        DiaMeta d = cache.get(date);
        if (d == null) return null;
        ContextMenu cm = new ContextMenu();

        if (d.microciclo != null) {
            MenuItem verMicro = new MenuItem("Ver microciclo");
            if (onVerMicro != null) verMicro.setOnAction(ev -> onVerMicro.run());
            cm.getItems().add(verMicro);
        }
        if (d.mesociclo != null) {
            MenuItem verMeso = new MenuItem("Ver mesociclo");
            if (onVerMeso != null) verMeso.setOnAction(ev -> onVerMeso.run());
            cm.getItems().add(verMeso);
        }
        MenuItem agregarEvento = new MenuItem("Agregar evento…");
        if (onAgregarEvento != null) agregarEvento.setOnAction(ev -> onAgregarEvento.run());
        cm.getItems().add(agregarEvento);

        return cm;
    }

    // =================== Indexado interno (rápido) ===================
    private void indexar() {
        // Rellenamos todos los días entre inicio y fin
        LocalDate d = plan.getInicio();
        while (!d.isAfter(plan.getFin())) {
            Mesociclo meso = findMesociclo(d);
            Microciclo micro = findMicrociclo(meso, d);
            List<EventoDeportivo> eventos = findEventos(d);

            // Color de fondo por microciclo (o por mesociclo si no hay micro asignado)
            Color fondo = colorFondo(micro, meso);

            boolean biMic = micro != null && d.equals(micro.getInicio());
            boolean bfMic = micro != null && d.equals(micro.getFin());
            boolean biMes = meso != null && d.equals(meso.getInicio());
            boolean bfMes = meso != null && d.equals(meso.getFin());

            DiaMeta meta = new DiaMeta(d, meso, micro, eventos, fondo, biMic, bfMic, biMes, bfMes);

            // Clases CSS sugeridas (puedes mapearlas en tu stylesheet)
            if (micro != null) meta.styleClasses.add("micro-" + micro.getTipo().name().toLowerCase());
            if (biMic) meta.styleClasses.add("borde-inicio-micro");
            if (bfMic) meta.styleClasses.add("borde-fin-micro");
            if (biMes) meta.styleClasses.add("borde-inicio-meso");
            if (bfMes) meta.styleClasses.add("borde-fin-meso");
            if (!eventos.isEmpty()) meta.styleClasses.add("tiene-evento");

            cache.put(d, meta);
            d = d.plusDays(1);
        }
    }

    private Mesociclo findMesociclo(LocalDate date) {
        for (Mesociclo m : plan.getMesociclos()) {
            if (!date.isBefore(m.getInicio()) && !date.isAfter(m.getFin())) return m;
        }
        return null;
    }

    private Microciclo findMicrociclo(Mesociclo meso, LocalDate date) {
        if (meso == null) return null;
        for (Microciclo mic : meso.getMicrociclos()) {
            if (!date.isBefore(mic.getInicio()) && !date.isAfter(mic.getFin())) return mic;
        }
        return null;
    }

    private List<EventoDeportivo> findEventos(LocalDate date) {
        List<EventoDeportivo> out = new ArrayList<>();
        for (EventoDeportivo e : plan.getEventos()) {
            if (date.equals(e.getFecha())) out.add(e);
        }
        return out;
    }

    /** Paleta por tipo de microciclo (ajústala a tus estilos). */
    private Color colorFondo(Microciclo mic, Mesociclo meso) {
        if (mic != null) {
            return switch (mic.getTipo()) {
                case ORDINARIO -> parse("#E3F2FD");       // azul claro
                case CHOQUE -> parse("#FFEBEE");          // rojo claro
                case APROXIMACION -> parse("#FFF8E1");    // ámbar claro
                case COMPETICION -> parse("#E8F5E9");     // verde claro
                case RESTABLECIMIENTO -> parse("#F3E5F5");// lila claro
            };
        }
        if (meso != null) {
            return switch (meso.getTipo()) {
                case ENTRANTE, BASICO_DESARROLLADOR -> parse("#E0F7FA"); // cian
                case BASICO_ESTABILIZADOR -> parse("#F1F8E9");           // lima
                case PRECOMPETITIVO -> parse("#FFFDE7");                 // amarillo
                case COMPETITIVO -> parse("#E8F5E9");                    // verde
                case ACUMULACION -> parse("#E3F2FD");
                case TRANSFORMACION -> parse("#EDE7F6");
                case REALIZACION -> parse("#FCE4EC");
                default -> parse("#FFFFFF");
            };
        }
        return parse("#FFFFFF");
    }

    private Color parse(String hex) { return Color.web(hex); }
}
