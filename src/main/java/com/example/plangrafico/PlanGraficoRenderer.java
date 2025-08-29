package com.example.plangrafico;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;

public final class PlanGraficoRenderer {

    public record WeekCell(int weekIdx,
                           LocalDate start,
                           LocalDate end,
                           String mes,
                           String periodo,
                           String etapa,
                           String mesociclo,
                           String micro,
                           String vol,
                           String intn,
                           String controles,
                           String comp,
                           String ses,
                           String min) {

    }

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd");


    private static LocalDate mondayOf(LocalDate d) {
        return d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static Month majorityMonthOfWeek(LocalDate weekStartMonday) {
        int[] c = new int[12];
        LocalDate d = weekStartMonday;
        for (int i = 0; i < 7; i++) {
            c[d.getMonthValue() - 1]++;
            d = d.plusDays(1);
        }
        int best = 0, idx = 0;
        for (int i = 0; i < 12; i++) if (c[i] > best) { best = c[i]; idx = i; }
        return Month.of(idx + 1);
    }

    /**
     * Si la semana que contiene `plan.getInicio()` tiene mayoría del mes anterior,
     * arranca en ese lunes; si la mayoría es del mes siguiente, salta al lunes siguiente.
     */
    private static LocalDate anchorByMajorityMonth(LocalDate planStart) {
        LocalDate w = mondayOf(planStart);
        Month m0 = planStart.getMonth();
        Month maj = majorityMonthOfWeek(w);
        if (maj == m0) return w;
        // si la mayoría es del mes siguiente al de planStart, inicia en el próximo lunes
        if (maj == planStart.plusMonths(1).getMonth()) return w.plusWeeks(1);
        // si la mayoría es del mes previo, inicia en “w” (ya está en ese lunes)
        return w;
    }

    /** Construye el GridPane del plan gráfico con celdas unidas por bloques consecutivos. */
    public GridPane build(PlanGrafico plan) {
        // 1) Expandir el macrociclo a semanas (columnas)
        List<WeekCell> weeks = expandWeeks(plan);

        // 2) Crear grilla base
        GridPane grid = new GridPane();
        grid.getStyleClass().add("plan-grafico");
        grid.setHgap(1);
        grid.setVgap(1);
        grid.setPadding(new Insets(8));

        // Ancho uniforme por semana
        final double weekWidth = 60; // ajusta a tu gusto/viewport
        for (int c = 0; c < weeks.size() + 1; c++) { // +1 por la columna de títulos
            ColumnConstraints cc = new ColumnConstraints(weekWidth);
            cc.setPrefWidth(weekWidth);
            grid.getColumnConstraints().add(cc);
        }

        // Filas fijas (títulos + 11 filas de datos del ejemplo)
        String[] rowTitles = {
                "SEM", "INICIO", "MES", "PERÍODO", "ETAPA",
                "MESOCICLO", "MICRO", "VOL", "INT", "CONTROLES",
                "COMP", "SES", "MIN"
        };
        for (int r = 0; r < rowTitles.length; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setPrefHeight(r == 0 ? 28 : 32);
            grid.getRowConstraints().add(rc);
        }

        // 3) Columna 0: títulos de fila
        for (int r = 0; r < rowTitles.length; r++) {
            grid.add(header(rowTitles[r]), 0, r);
        }

        // 4) Fila 0: SEM (número de semana)
        for (int i = 0; i < weeks.size(); i++) {
            grid.add(cellCenter(String.valueOf(i + 1)), i + 1, 0);
        }

        // 5) Fila 1: INICIO (rango fecha inicio–fin de la semana)
        for (int i = 0; i < weeks.size(); i++) {
            WeekCell w = weeks.get(i);
            grid.add(cellCenter(w.start.format(DF) + " / " + w.end.format(DF)), i + 1, 1);
        }

        // 6) Filas fusionadas por categoría (MES / PERÍODO / ETAPA / MESOCICLO / MICRO / …)
        addMergedRow(grid, 2, weeks, WeekCell::mes);        // MES
        addMergedRow(grid, 3, weeks, WeekCell::periodo);    // PERÍODO
        addMergedRow(grid, 4, weeks, WeekCell::etapa);      // ETAPA
        addMergedRow(grid, 5, weeks, WeekCell::mesociclo);  // MESOCICLO
        addMergedRow(grid, 6, weeks, WeekCell::micro);      // MICRO

        // 7) Filas simples (puntos, números…) – sin fusionar
        addSimpleRow(grid, 7, weeks, WeekCell::vol);
        addSimpleRow(grid, 8, weeks, WeekCell::intn);
        addSimpleRow(grid, 9, weeks, WeekCell::controles);
        addSimpleRow(grid, 10, weeks, WeekCell::comp);
        addSimpleRow(grid, 11, weeks, WeekCell::ses);
        addSimpleRow(grid, 12, weeks, WeekCell::min);

        // Bordes exteriores
        grid.setBorder(new Border(new BorderStroke(
                javafx.scene.paint.Color.web("#D0D7DE"), BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, new BorderWidths(1))));

        return grid;
    }

    // ---------------- helpers de render ----------------

    private void addSimpleRow(GridPane grid, int rowIndex, List<WeekCell> weeks,
                              Function<WeekCell, String> field) {
        for (int i = 0; i < weeks.size(); i++) {
            grid.add(cellCenter(field.apply(weeks.get(i))), i + 1, rowIndex);
        }
    }

    /** Funde celdas contiguas con el mismo valor para esa fila. */
    private void addMergedRow(GridPane grid, int rowIndex, List<WeekCell> weeks,
                              Function<WeekCell, String> field) {
        int c = 0;
        while (c < weeks.size()) {
            String val = safe(field.apply(weeks.get(c)));
            int start = c;
            int end = c;
            // agrupar mientras el valor sea igual
            while (end + 1 < weeks.size() &&
                    Objects.equals(val, safe(field.apply(weeks.get(end + 1))))) {
                end++;
            }
            int span = (end - start + 1);
            Node n = mergedCell(val);
            grid.add(n, start + 1, rowIndex, span, 1); // +1 por la columna de títulos
            c = end + 1;
        }
    }

    // ---------------- helpers de UI ----------------

    private Label header(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("System", FontWeight.SEMI_BOLD, 12));
        l.setAlignment(Pos.CENTER_LEFT);
        l.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        l.getStyleClass().add("pg-header");
        l.setPadding(new Insets(0, 6, 0, 6));
        setBaseCellBorder(l);
        return l;
    }

    private StackPane cellCenter(String text) {
        StackPane p = new StackPane(new Label(safe(text)));
        p.setAlignment(Pos.CENTER);
        p.setPadding(new Insets(4));
        p.getStyleClass().add("pg-cell");
        setBaseCellBorder(p);
        return p;
    }

    private StackPane mergedCell(String text) {
        StackPane p = cellCenter(text);
        p.getStyleClass().add("pg-merged");
        return p;
    }

    private void setBaseCellBorder(Region r) {
        r.setBorder(new Border(new BorderStroke(
                javafx.scene.paint.Color.web("#E6E8EB"),
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                new BorderWidths(0.5))));
        r.setBackground(new Background(new BackgroundFill(
                javafx.scene.paint.Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
    }

    private String safe(String s) { return s == null ? "" : s; }

    // ---------------- datos: expandir a semanas ----------------

    /** Crea una lista de columnas (semanas) con los textos que vas a pintar. */
    private List<WeekCell> expandWeeks(PlanGrafico plan) {
        List<WeekCell> out = new ArrayList<>();
//        LocalDate cursor = plan.getInicio();
        LocalDate cursor = anchorByMajorityMonth(plan.getInicio());
        // Asegurar que el cursor siempre comience en lunes
        if (cursor.getDayOfWeek() != DayOfWeek.MONDAY) {
            cursor = cursor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }


        int idx = 0;
        while (!cursor.isAfter(plan.getFin())) {
            LocalDate weekStart = cursor;
            LocalDate weekEnd = cursor.plusDays(6);
            if (weekEnd.isAfter(plan.getFin())) weekEnd = plan.getFin();

            // Buscar a qué mesociclo / microciclo cae la semana (usa el día de inicio como referencia)
            var meso = findMesociclo(plan, weekStart);
            var micro = meso != null ? findMicrociclo(meso, weekStart) : null;

            String mes = majorityMonthOfWeek(weekStart).toString().substring(0,3); // "JUL", "AUG", ...
            String periodo = (meso != null) ? periodoFrom(meso) : "";
            String etapa   = (meso != null) ? etapaFrom(meso)   : "";

            String mesociclo = (meso != null) ? meso.getNombre() : "";
            String microTxt  = (micro != null) ? micro.getTipo().getNombreVisible() : "";

            // Si quieres dibujar “puntos” de VOL/INT, puedes convertirlos a cadenas
            String vol = micro != null ? dotsFor(micro.getPorcentajeCarga(), 5) : "";
            String intn = micro != null ? dotsFor(intensidadAprox(micro), 5) : "";
            String controles = ""; // rellena según tu plan
            String comp = "";      // idem
            String ses = micro != null ? String.valueOf(micro.getCarga().frecuenciaSesiones()) : "";
            String min = micro != null ? String.valueOf(micro.minutosTotales()) : "";

            out.add(new WeekCell(++idx, weekStart, weekEnd, mes, periodo, etapa,
                    mesociclo, microTxt, vol, intn, controles, comp, ses, min));

            cursor = cursor.plusWeeks(1);
            idx++;
        }
        return out;
    }

    private Mesociclo findMesociclo(PlanGrafico plan, LocalDate d) {
        for (var m : plan.getMesociclos()) {
            if (!d.isBefore(m.getInicio()) && !d.isAfter(m.getFin())) return m;
        }
        return null;
    }

    private Microciclo findMicrociclo(Mesociclo meso, LocalDate d) {
        if (meso == null) return null;
        for (var m : meso.getMicrociclos()) {
            if (!d.isBefore(m.getInicio()) && !d.isAfter(m.getFin())) return m;
        }
        return null;
    }

    // Estas dos funciones son “marcadores” para tu lógica real de periodo/etapa.
    private String periodoFrom(Mesociclo meso) {
        // Ejemplo: si usas Tradicional, podrías devolver "Prep.", "Compet.", "Trans."
        return switch (meso.getTipo()) {
            case ENTRANTE, BASICO_DESARROLLADOR, BASICO_ESTABILIZADOR -> "Prep.";
            case PRECOMPETITIVO, COMPETITIVO -> "Compet.";
            case ACUMULACION, TRANSFORMACION, REALIZACION -> "Prep."; // o mapéalo a ATR si lo prefieres distinto
            default -> "";
        };
    }
    private String etapaFrom(Mesociclo meso) {
        return switch (meso.getTipo()) {
            case ENTRANTE, BASICO_DESARROLLADOR, BASICO_ESTABILIZADOR -> "General";
            case PRECOMPETITIVO -> "Especial";
            case COMPETITIVO -> "Compet.";
            case ACUMULACION -> "General";
            case TRANSFORMACION -> "Especial";
            case REALIZACION -> "Precomp.";
            default -> "";
        };
    }

    // Visual de “puntos” según un valor 0–100 mapeado a n slots:
    private String dotsFor(int value, int slots) {
        int filled = Math.max(0, Math.min(slots, (int)Math.round(value / (100.0/slots))));
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<filled;i++) sb.append("●");
        for (int i=filled;i<slots;i++) sb.append("○");
        return sb.toString();
    }
    private int intensidadAprox(Microciclo m) {
        // ejemplo simple: intensidad ≈ %carga (cámbialo si tienes un campo propio)
        return m.getPorcentajeCarga();
    }
}
