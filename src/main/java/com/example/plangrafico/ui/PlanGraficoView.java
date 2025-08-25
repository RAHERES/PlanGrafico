package com.example.plangrafico.ui;

import com.example.plangrafico.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Vista de Plan Gráfico (por semanas) con:
 * - Cabeceras de SEMANA, INICIO, MES
 * - Etiquetas de MESOCICLO y MICRO
 * - Barras de Volumen e Intensidad (niveles 1-4)
 * - Spans/merges por Mes, Meso y Etapa/Periodo
 */
public final class PlanGraficoView {

    // ===== Enums (locales para no chocar con tu dominio) =====
    public enum Etapa { GENERAL, ESPECIAL, PRECOMPETITIVA, COMPETITIVA, RECUPERACION }
    public enum Mesociclo { ACUMULACION, TRANSFORMACION, REALIZACION, COMPETITIVO, RECUPERATORIO }
    public enum Microciclo { CARGA, IMPACTO, COMPETICION, RESTAURATIVO }
    public enum Control { TEST, RPE, AMISTOSO, TORNEO }
    public enum Competencia { CP, CF }

    // ===== Modelo =====
    public static final class SemanaPlan {
        public final LocalDate lunes;
        public PeriodoTipo periodo;
        public Etapa etapa;
        public Mesociclo meso;
        public Microciclo micro;
        public Integer nivelVolumen;    // 1..4
        public Integer nivelIntensidad; // 1..4
        public Integer sesiones;        // p.ej. 5
        public Integer minutos;         // p.ej. 300
        public EnumSet<Control> controles = EnumSet.noneOf(Control.class);
        public Competencia competencia; // null | CP | CF
        public SemanaPlan(LocalDate lunes) { this.lunes = lunes; }
    }

    public static final class Plan {
        public final LocalDate inicioLunes;
        public final int semanas;
        public final List<SemanaPlan> detalle; // tamaño = semanas
        public final String tipoPeriodizacion;
        public Plan(LocalDate inicioLunes, int semanas, String tipo, List<SemanaPlan> det) {
            this.inicioLunes = inicioLunes; this.semanas = semanas; this.tipoPeriodizacion = tipo; this.detalle = det;
        }
    }

    // ====== API principal ======
    /** Crea el Node con el grid del plan. */
    public static Node crearVista(Plan plan) {
        GridPane grid = baseGrid(plan.semanas);
        // Cabecera izquierda (títulos de filas)
        String[] filas = {
                "SEM", "INICIO", "MES",
                "PERÍODO", "ETAPA", "MESOCICLO", "MICRO",
                "VOL", "INT", "CONTROLES", "COMP", "SES", "MIN"
        };
        for (int r = 0; r < filas.length; r++) {
            grid.add(celdaTituloFila(filas[r]), 0, r);
        }

        // Columnas por semana (desde 1)
        for (int i = 0; i < plan.semanas; i++) {
            int col = i + 1;
            SemanaPlan sp = plan.detalle.get(i);

            // Semana #
            grid.add(celdaTexto(String.valueOf(i + 1)), col, 0);
            // Inicio (fecha)
            grid.add(celdaTexto(sp.lunes.toString()), col, 1);
            // Mes (abreviado)
            grid.add(celdaTexto(mesCorto(sp.lunes)), col, 2);

            // Periodo / Etapa / Meso / Micro
            grid.add(celdaBadge(sp.periodo == null ? "" : nombrePeriodo(sp.periodo), colorPeriodo(sp.periodo)), col, 3);
            grid.add(celdaBadge(sp.etapa == null ? "" : nombreEtapa(sp.etapa), colorEtapa(sp.etapa)), col, 4);
            grid.add(celdaBadge(sp.meso == null ? "" : nombreMeso(sp.meso), colorMeso(sp.meso)), col, 5);
            grid.add(celdaBadge(sp.micro == null ? "" : sp.micro.name(), colorMicro(sp.micro)), col, 6);

            // Volumen / Intensidad
            grid.add(celdaBarra(sp.nivelVolumen), col, 7);
            grid.add(celdaBarra(sp.nivelIntensidad), col, 8);

            // Controles / Competencias
            grid.add(celdaTexto(sp.controles.isEmpty() ? "" : joinEnum(sp.controles)), col, 9);
            grid.add(celdaBadge(sp.competencia == null ? "" : sp.competencia.name(), Color.web("#0ea5e9")), col, 10);

            // Sesiones / Minutos
            grid.add(celdaTexto(sp.sesiones == null ? "" : sp.sesiones + ""), col, 11);
            grid.add(celdaTexto(sp.minutos == null ? "" : sp.minutos + ""), col, 12);
        }

        // Spans (mes, mesociclo y etapa)
        renderSpans(grid, plan);
        // Encabezado superior (título)
        VBox root = new VBox(6, header(plan), grid);
        root.setPadding(new Insets(10));
        return root;
    }

    /** Snapshot rápido a PNG transparente del nodo. */
    public static WritableImage snapshot(Node vista, double scale) {
        vista.setScaleX(scale);
        vista.setScaleY(scale);
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        WritableImage img = vista.snapshot(sp, null);
        vista.setScaleX(1); vista.setScaleY(1);
        return img;
    }

    // ====== Helpers de UI ======
    private static GridPane baseGrid(int semanas) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("plan-grid");
        ColumnConstraints c0 = new ColumnConstraints(150); // títulos
        grid.getColumnConstraints().add(c0);
        for (int i = 0; i < semanas; i++) {
            ColumnConstraints c = new ColumnConstraints(72);
            grid.getColumnConstraints().add(c);
        }
        int rows = 13; // total filas (según cabeceras definidas)
        for (int i = 0; i < rows; i++) {
            RowConstraints r = new RowConstraints(32);
            grid.getRowConstraints().add(r);
        }
        return grid;
    }

    private static HBox header(Plan p) {
        Text t1 = new Text("Plan Gráfico (" + p.tipoPeriodizacion + ")");
        t1.setFont(Font.font(18));
        Text t2 = new Text("  •  " + p.inicioLunes + " → " + p.inicioLunes.plusWeeks(p.semanas - 1));
        t2.setFill(Color.web("#64748b"));
        HBox hb = new HBox(8, t1, t2);
        hb.setAlignment(Pos.CENTER_LEFT);
        return hb;
    }

    private static StackPane celdaTituloFila(String s) {
        StackPane p = new StackPane(new Text(s));
        p.setPadding(new Insets(4,8,4,8));
        p.setAlignment(Pos.CENTER_LEFT);
        p.setBackground(new Background(new BackgroundFill(Color.web("#f1f5f9"), CornerRadii.EMPTY, Insets.EMPTY)));
        p.setBorder(new Border(new BorderStroke(Color.web("#cbd5e1"), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(0,0,1,0))));
        return p;
    }

    private static StackPane celdaTexto(String s) {
        StackPane p = new StackPane(new Text(s == null ? "" : s));
        p.setPadding(new Insets(4));
        p.setAlignment(Pos.CENTER);
        p.setBorder(new Border(new BorderStroke(Color.web("#e5e7eb"), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(0,0,1,0))));
        return p;
    }

    private static StackPane celdaBadge(String text, Color color) {
        Text t = new Text(text == null ? "" : text);
        StackPane badge = new StackPane(t);
        badge.setPadding(new Insets(4,6,4,6));
        badge.setAlignment(Pos.CENTER);
        if (text != null && !text.isBlank()) {
            badge.setBackground(new Background(new BackgroundFill(color.deriveColor(0,1,1,0.25), new CornerRadii(8), Insets.EMPTY)));
            badge.setBorder(new Border(new BorderStroke(color, BorderStrokeStyle.SOLID, new CornerRadii(8), BorderWidths.DEFAULT)));
        }
        return badge;
    }

    private static StackPane celdaBarra(Integer nivel) {
        HBox bars = new HBox(2);
        bars.setAlignment(Pos.CENTER);
        int n = nivel == null ? 0 : Math.max(0, Math.min(4, nivel));
        for (int i = 0; i < 4; i++) {
            Rectangle r = new Rectangle(10, 14);
            r.setArcWidth(6); r.setArcHeight(6);
            r.setFill(i < n ? Color.web("#2563eb") : Color.web("#e5e7eb"));
            bars.getChildren().add(r);
        }
        StackPane p = new StackPane(bars);
        p.setPadding(new Insets(2));
        p.setAlignment(Pos.CENTER);
        return p;
    }

    private static String mesCorto(LocalDate d) {
        return d.getMonth().toString().substring(0,3);
    }
    private static String joinEnum(Set<Control> c) {
        if (c==null || c.isEmpty()) return "";
        List<String> s = new ArrayList<>();
        for (Control x : c) s.add(x.name());
        return String.join(" ", s);
    }
    private static String nombrePeriodo(PeriodoTipo p) {
        if (p==null) return "";
        return switch (p) {
            case PREPARATORIO -> "Prep.";
            case COMPETITIVO -> "Compet.";
            case TRANSICION -> "Trans.";
        };
    }
    private static String nombreEtapa(Etapa e){
        if (e==null) return "";
        return switch(e){
            case GENERAL -> "General";
            case ESPECIAL -> "Especial";
            case PRECOMPETITIVA -> "Precomp.";
            case COMPETITIVA -> "Compet.";
            case RECUPERACION -> "Recup.";
        };
    }
    private static String nombreMeso(Mesociclo m){
        if (m==null) return "";
        return switch(m){
            case ACUMULACION -> "Acum.";
            case TRANSFORMACION -> "Transf.";
            case REALIZACION -> "Realiz.";
            case COMPETITIVO -> "Compet.";
            case RECUPERATORIO -> "Restabl.";
        };
    }
    private static Color colorPeriodo(PeriodoTipo p){
        if (p==null) return Color.web("#e2e8f0");
        return switch(p){
            case PREPARATORIO -> Color.web("#86efac");
            case COMPETITIVO -> Color.web("#fde68a");
            case TRANSICION -> Color.web("#c7d2fe");
        };
    }
    private static Color colorEtapa(Etapa e){
        if (e==null) return Color.web("#e5e7eb");
        return switch(e){
            case GENERAL -> Color.web("#bbf7d0");
            case ESPECIAL -> Color.web("#a7f3d0");
            case PRECOMPETITIVA -> Color.web("#fde68a");
            case COMPETITIVA -> Color.web("#fca5a5");
            case RECUPERACION -> Color.web("#e5e7eb");
        };
    }
    private static Color colorMeso(Mesociclo m){
        if (m==null) return Color.web("#e5e7eb");
        return switch(m){
            case ACUMULACION -> Color.web("#a7f3d0");
            case TRANSFORMACION -> Color.web("#93c5fd");
            case REALIZACION -> Color.web("#fca5a5");
            case COMPETITIVO -> Color.web("#6ee7b7");
            case RECUPERATORIO -> Color.web("#cbd5e1");
        };
    }
    private static Color colorMicro(Microciclo m){
        if (m==null) return Color.web("#e5e7eb");
        return switch(m){
            case CARGA -> Color.web("#60a5fa");
            case IMPACTO -> Color.web("#22c55e");
            case COMPETICION -> Color.web("#f97316");
            case RESTAURATIVO -> Color.web("#94a3b8");
        };
    }

    // ====== Spans (mes, mesociclo, etapa) ======
    private record Span(int startCol, int endCol, String label, Color color) {
        public Color colorEtapa(String label) {
            return Color.RED;
        }
    }

    private static void addSpanCell(GridPane grid, String text, int row, int startCol, int endCol, Color color) {
        StackPane p = new StackPane(new Text(text));
        p.setPadding(new Insets(2));
        p.setAlignment(Pos.CENTER);
        p.setBackground(new Background(new BackgroundFill(color.deriveColor(0,1,1,0.18), CornerRadii.EMPTY, Insets.EMPTY)));
        p.setBorder(new Border(new BorderStroke(color, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        grid.add(p, startCol, row);
        GridPane.setColumnSpan(p, endCol - startCol + 1);
    }

    private static List<Span> monthSpans(Plan plan) {
        List<Span> out = new ArrayList<>();
        int start = 1;
        String m = mesCorto(plan.detalle.get(0).lunes);
        Color color = Color.web("#93c5fd");
        for (int i=1;i<plan.semanas;i++){
            String mi = mesCorto(plan.detalle.get(i).lunes);
            if (!mi.equals(m)) {
                out.add(new Span(start, i, m, color));
                start = i+1; m = mi;
            }
        }
        out.add(new Span(start, plan.semanas, m, color));
        return out;
    }

    private static List<Span> mesoSpans(Plan plan) {
        List<Span> out = new ArrayList<>();
        int start = 1;
        Mesociclo cur = plan.detalle.get(0).meso;
        for (int i=1;i<plan.semanas;i++){
            Mesociclo mi = plan.detalle.get(i).meso;
            if (mi != cur) {
                out.add(new Span(start, i, nombreMeso(cur), colorMeso(cur)));
                start = i+1; cur = mi;
            }
        }
        out.add(new Span(start, plan.semanas, nombreMeso(cur), colorMeso(cur)));
        return out;
    }

    private static List<Span> etapaSpans(Plan plan) {
        List<Span> out = new ArrayList<>();
        int start = 1;
        Etapa cur = plan.detalle.get(0).etapa;
        for (int i=1;i<plan.semanas;i++){
            Etapa ei = plan.detalle.get(i).etapa;
            if (ei != cur) {
                out.add(new Span(start, i, nombreEtapa(cur), colorEtapa(cur)));
                start = i+1; cur = ei;
            }
        }
        out.add(new Span(start, plan.semanas, nombreEtapa(cur), colorEtapa(cur)));
        return out;
    }

    private static void renderSpans(GridPane grid, Plan plan) {
        // Limpia filas de spans si fuera necesario (no esencial en primera carga)
        // Mes (fila 2 ya está con celdas por semana; el span va encima en fila 2)
        for (Span s : monthSpans(plan)) addSpanCell(grid, s.label, 2, s.startCol, s.endCol, s.color);
        // Mesociclo (fila 5)
        for (Span s : mesoSpans(plan)) addSpanCell(grid, s.label, 5, s.startCol, s.endCol, s.color);
        // Etapa (fila 4)
        for (Span s : etapaSpans(plan)) addSpanCell(grid, s.label, 4, s.startCol, s.endCol, s.colorEtapa(s.label));
    }

    private Color colorEtapa(String nombre) {
        return Color.web("#e5e7eb");
    }

    // ====== Builders de ejemplo ======
    /** Construye un plan tipo ejemplo (22 semanas) parecido al clásico. */
    public static Plan ejemplo() {
        LocalDate inicio = siguienteLunes(LocalDate.now());
        int N = 22;
        List<SemanaPlan> list = new ArrayList<>(N);
        for (int i=0;i<N;i++) list.add(new SemanaPlan(inicio.plusWeeks(i)));

        // Períodos
        for (int i=0;i<14;i++) list.get(i).periodo = PeriodoTipo.PREPARATORIO;
        for (int i=14;i<20;i++) list.get(i).periodo = PeriodoTipo.COMPETITIVO;
        for (int i=20;i<22;i++) list.get(i).periodo = PeriodoTipo.TRANSICION;

        // Etapas
        for (int i=0;i<10;i++) list.get(i).etapa = Etapa.GENERAL;
        for (int i=10;i<14;i++) list.get(i).etapa = Etapa.ESPECIAL;
        for (int i=14;i<18;i++) list.get(i).etapa = Etapa.PRECOMPETITIVA;
        for (int i=18;i<20;i++) list.get(i).etapa = Etapa.COMPETITIVA;
        for (int i=20;i<22;i++) list.get(i).etapa = Etapa.RECUPERACION;

        // Mesociclos (4-3-3-2-2-2-2-2 ídem ejemplo común)
        int[][] bloques = {{0,3},{4,6},{7,9},{10,12},{13,14},{15,16},{17,18},{19,19}};
        Mesociclo[] tipos = {
                Mesociclo.ACUMULACION, Mesociclo.TRANSFORMACION, Mesociclo.REALIZACION,
                Mesociclo.ACUMULACION, Mesociclo.TRANSFORMACION, Mesociclo.REALIZACION,
                Mesociclo.COMPETITIVO, Mesociclo.RECUPERATORIO
        };
        for (int b=0;b<bloques.length;b++){
            int s = bloques[b][0], e = bloques[b][1];
            for (int i=s;i<=e;i++) list.get(i).meso = tipos[b];
        }

        // Microciclos y barras (simple demo)
        for (int i=0;i<N;i++){
            SemanaPlan sp = list.get(i);
            sp.micro = switch (i % 4) {
                case 0,1 -> Microciclo.CARGA;
                case 2 -> Microciclo.IMPACTO;
                default -> Microciclo.RESTAURATIVO;
            };
            sp.nivelVolumen = switch (sp.micro) {
                case CARGA -> 3;
                case IMPACTO -> 4;
                case COMPETICION -> 2;
                case RESTAURATIVO -> 1;
            };
            sp.nivelIntensidad = switch (sp.micro) {
                case CARGA -> 2;
                case IMPACTO -> 3;
                case COMPETICION -> 4;
                case RESTAURATIVO -> 1;
            };
            sp.sesiones = 5; sp.minutos = 300;
        }

        return new Plan(inicio, N, "ATR", list);
    }

    /** Construye un plan simple a partir de un rango [inicio, fin], semanas completas lunes-lunes. */
    public static Plan desdeRango(LocalDate inicio, LocalDate fin, String tipo) {
        LocalDate ini = siguienteLunes(inicio);
        int semanas = (int) (ChronoUnit.WEEKS.between(ini, fin.plusDays(1)));
        semanas = Math.max(1, semanas);
        List<SemanaPlan> list = new ArrayList<>(semanas);
        for (int i=0;i<semanas;i++){
            SemanaPlan sp = new SemanaPlan(ini.plusWeeks(i));
            // defaults básicos
            sp.periodo = i < semanas * 0.6 ? PeriodoTipo.PREPARATORIO : PeriodoTipo.COMPETITIVO;
            sp.etapa = i < semanas * 0.45 ? Etapa.GENERAL : (i < semanas * 0.7 ? Etapa.ESPECIAL : Etapa.PRECOMPETITIVA);
            sp.meso = i % 4 == 3 ? Mesociclo.RECUPERATORIO : Mesociclo.ACUMULACION;
            sp.micro = (i % 4 == 2) ? Microciclo.IMPACTO : (i % 4 == 3 ? Microciclo.RESTAURATIVO : Microciclo.CARGA);
            sp.nivelVolumen = switch (sp.micro) { case CARGA -> 3; case IMPACTO -> 4; case RESTAURATIVO -> 1; default -> 2; };
            sp.nivelIntensidad = switch (sp.micro) { case CARGA -> 2; case IMPACTO -> 3; case RESTAURATIVO -> 1; default -> 3; };
            sp.sesiones = 5; sp.minutos = 300;
            list.add(sp);
        }
        return new Plan(ini, semanas, (tipo==null?"ATR":tipo), list);
    }

    private static LocalDate siguienteLunes(LocalDate d) {
        while (d.getDayOfWeek().getValue() != 1) d = d.plusDays(1);
        return d;
    }
}

