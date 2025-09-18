package com.example.plangrafico;



import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class PlanGrafico2 extends Application {

    // Filas del grid
    private enum Row {
        SEM, INICIO, MES, PERIODO, ETAPA, MESOCICLO, MICRO,   PCT_MICRO,      VOL_INT, CONTROLES, COMP, SES, MIN
    }

    // Qué parte del plan se selecciona
    public enum ZoneType { MES, PERIODO, ETAPA, MESOCICLO, MICRO, VOL_INT, SES, MIN, CONTROLES, COMP }

    // Etiqueta para cualquier nodo dibujado en el grid
    public static class CellTag {
        final ZoneType type;
        final int startCol;     // columna 1..N (inicio del span o la semana)
        final int endCol;       // columna fin (== startCol si es semana)
        CellTag(ZoneType type, int startCol, int endCol) {
            this.type = type; this.startCol = startCol; this.endCol = endCol;
        }
    }

    // Colores por tipo de micro (hex)
    private static final Map<MicrocicloTipo, String> MICRO_BG = Map.of(
            MicrocicloTipo.ORDINARIO,       "#E0F7FA",
            //MicrocicloTipo.CARGA,           "#BBDEFB",
            MicrocicloTipo.CHOQUE,          "#EF9A9A",
            MicrocicloTipo.APROXIMACION,    "#FFF59D",
            MicrocicloTipo.COMPETICION,     "#C5E1A5",
            MicrocicloTipo.RESTABLECIMIENTO,"#E0E0E0"
    );
    private static final Map<MicrocicloTipo, String> MICRO_BORDER = Map.of(
            MicrocicloTipo.ORDINARIO,       "#0097A7",
          //  MicrocicloTipo.CARGA,           "#1976D2",
            MicrocicloTipo.CHOQUE,          "#C62828",
            MicrocicloTipo.APROXIMACION,    "#F9A825",
            MicrocicloTipo.COMPETICION,     "#2E7D32",
            MicrocicloTipo.RESTABLECIMIENTO,"#757575"
    );

    public enum MesocicloTipo {
        ENTRANTE("Entrante"),
        BASICO_DESARROLLADOR("Básico desarrollador"),
        BASICO_ESTABILIZADOR("Básico estabilizador"),
        PRECOMPETITIVO("Precompetitivo"),
        COMPETITIVO("Competitivo"),
        ACUMULACION("Acumulación"),
        TRANSFORMACION("Transformación"),
        REALIZACION("Realización");

        private final String etiqueta;
        MesocicloTipo(String etiqueta){ this.etiqueta = etiqueta; }
        public String etiqueta(){ return etiqueta; }
        @Override public String toString(){ return etiqueta; }
    }


    // Representa un span de mesociclo en la fila MESOCICLO
    public static class Mesociclo {
        public int startCol;       // columna de inicio (1-based)
        public int len;            // duración en semanas
        public MesocicloTipo tipo; // tipo

        public Mesociclo() {}      // para JSON
        public Mesociclo(int startCol, int len, MesocicloTipo tipo) {
            this.startCol = startCol; this.len = len; this.tipo = tipo;
        }
    }

    private static final Map<MesocicloTipo, Color> MESO_BG = Map.of(
            MesocicloTipo.ENTRANTE,           Color.web("#D1C4E9"),
            MesocicloTipo.BASICO_DESARROLLADOR,     Color.web("#B39DDB"),
            MesocicloTipo.PRECOMPETITIVO, Color.web("#CE93D8"),
            MesocicloTipo.COMPETITIVO,   Color.web("#B0BEC5")
    );


    // Fases ATR
    public enum ATRFase { ACUMULACION, TRANSFORMACION, REALIZACION }

    // Tipos
    public enum EtapaTipo { GENERAL, ESPECIAL, PRECOMPETITIVA, COMPETITIVA, RECUPERACION }



    // Modelo minimal
    public static class PlanGrafico {

        // Asignaciones de tiempo por semana (columna = semana 1..N)
        Map<Integer, Integer> minutosTotal = new HashMap<>();   // total por micro (P.F. + Téc-Tác)
        Map<Integer, Integer> minutosPF    = new HashMap<>();   // minutos de Preparación Física
        Map<Integer, Integer> minutosTT    = new HashMap<>();   // minutos Técnico-Tácticos
        Map<Integer, Integer> microCargaPct= new HashMap<>();   // % de carga de cada micro (p.ej. 60, 70, 75, 50)
        Map<Integer, MicrocicloTipo> microAsignaciones = new HashMap<>();

        public List<Mesociclo> mesociclosManuales = new ArrayList<>();


        public Map<Integer, List<LocalDate>> sesionesPorSemana = new HashMap<>();
        public Map<Integer, Integer> diasTotal = new HashMap<>();

        LocalDate inicio; //inicio del plan
        LocalDate fin;     // fin del plan


        int semanas;
        int sesionesSem;
        // dentro de PlanGrafico
        // Porcentajes por periodo (suman 100)
        int pctPrep, pctComp, pctTrans;

        // Porcentajes por etapa dentro de cada periodo (suman 100 en su periodo)
        int pctPrepGeneral,
            pctPrepEspecial;
        int pctCompPrecomp,
            pctCompComp;
        int pctTransRecup; // el resto en transitorio lo dejamos como descanso activo (implícito)

        // === Tipo de periodización ===
        TipoPeriodizacion tipo = TipoPeriodizacion.LINEAL;

        // === ATR ===
        int pctATR_A = 50;   // Acumulación
        int pctATR_T = 30;   // Transformación
        int pctATR_R = 20;   // Realización

        // Tendencia vol/int (%)
        int volIni = 100, volFin = 40;
        int intIni = 40, intFin = 95;

        // Sesiones por micro se toma de sesionesSem (promedio)

        private List<DayOfWeek> diasEntrenamiento;
        private Map<DayOfWeek, LocalTime[]> horariosEntrenamiento;

        public void setDiasEntrenamiento(List<DayOfWeek> dias) { this.diasEntrenamiento = dias; }
        public void setHorariosEntrenamiento(Map<DayOfWeek, LocalTime[]> h) { this.horariosEntrenamiento = h; }

        public List<DayOfWeek> getDiasEntrenamiento() { return diasEntrenamiento; }
        public Map<DayOfWeek, LocalTime[]> getHorariosEntrenamiento() { return horariosEntrenamiento; }


        // === Microciclo por días ===
        public static class Micro {
            public LocalDate start;      // día de inicio (inclusive)
            public int lenDays;          // duración en días
            public MicrocicloTipo tipo;  // tipo de micro
            public int cargaPct;         // % de carga del micro (0..120)

            public Micro() {}
            public Micro(LocalDate start, int lenDays, MicrocicloTipo tipo, int cargaPct) {
                this.start = start; this.lenDays = Math.max(1, lenDays);
                this.tipo = tipo; this.cargaPct = cargaPct;
            }
        }
        public List<Micro> microDiasManuales = new ArrayList<>();

    }

    // ====== Config base ======
    private static final int COL_TITULOS_ANCHO = 140;
    private static final int COL_SEMANA_ANCHO = 64;

    private static final DateTimeFormatter DF_D_MMM = DateTimeFormatter.ofPattern("d MMM", new Locale("es", "MX"));
    private static final DateTimeFormatter DF_DD_MM = DateTimeFormatter.ofPattern("dd/MM");

    /**
     * Fecha de nicio del programa de entrenamiento
     * */
    private DatePicker dpInicio;

    /**
     * Fecha en la que finaliza el plan de entrenamiento
     */
    private DatePicker dpFin;

    private Spinner<Integer> spSemanas, spSes;

    private boolean updatingUI = false;   // <—— evita bucles de eventos
    private ComboBox<TipoPeriodizacion> cbTipo;

    // Grupos (para mostrar/ocultar según selección)
    private VBox grpClasicaPeriodos;
    private VBox grpClasicaEtapas;
    private VBox grpATR;

    // Spinners ATR
    private Spinner<Integer> spATR_A, spATR_T, spATR_R;

    private Node selectedNode;                         // último nodo seleccionado
    private final String SELECT_CSS = "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 15, 0.2, 0, 0);";

    // Estado UI
    private final ObjectProperty<PlanGrafico> plan = new SimpleObjectProperty<>(defaultPlan());

    // UI nodes
    private GridPane grid;
    private BorderPane root;

    // Inputs
  /*private DatePicker dpInicio;
    private Spinner<Integer> spSemanas, spSes;*/
    private Spinner<Integer> spPrep,
                            spComp,
                            spTrans;
    private Spinner<Integer> spPrepGen,
                            spPrepEsp,
                            spCompPre,
                            spCompComp,
                            spTransRec;
    private Spinner<Integer> spVolIni,
                            spVolFin,
                            spIntIni,
                            spIntFin;


    // --- campos nuevos arriba de la clase ---
    private StackPane sidebarWrap;
    private ToggleButton handleBtn;
    private final double W_EXP = 360;   // ancho expandido
    private final double W_RAIL = 56;   // ancho en modo rail
    private final BooleanProperty rail = new SimpleBooleanProperty(false);
    private SplitPane split;

    private Region panelIzquierdo;
    private Node contenido;



    private final DoubleProperty lastDividerPos = new SimpleDoubleProperty(0.22); // recuerda pos previa

    @Override
    public void start(Stage stage) {
       /* root = new BorderPane();
        root.setLeft(panelInputs(stage));
        root.setCenter(new StackPane(new Label("Genera el plan para ver el gráfico →")) {{
            setPadding(new Insets(24));
        }});


        Scene scene = new Scene(root, 1280, 720);
        stage.setTitle("Plan Gráfico – MVP");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();*/
        root = new BorderPane();
        // --- en start(...) ---
        panelIzquierdo = (Region) panelInputs(stage);

        sidebarWrap = new StackPane(panelIzquierdo);
        sidebarWrap.setMinWidth(W_RAIL);           // para que el rail no desaparezca
        sidebarWrap.setPrefWidth(W_EXP);
        sidebarWrap.setMaxWidth(Double.MAX_VALUE);
        sidebarWrap.setStyle("""
    -fx-background-color: transparent;   /* antes tenías un gradiente */
    -fx-border-color: transparent;       /* quita el 1px del borde derecho */
""");



        // Botón “pill” flotante
        handleBtn = new ToggleButton("⮜");
        handleBtn.setFocusTraversable(false);
        handleBtn.setOnAction(e -> toggleRail());
        handleBtn.setStyle("""
    -fx-background-color: white;
    -fx-background-radius: 999;
    -fx-border-radius: 999;
    -fx-border-color: rgba(0,0,0,0.15);
    -fx-padding: 4 8 4 8;
    -fx-opacity: 0.85;
""");
        handleBtn.setOnMouseEntered(e -> handleBtn.setStyle(handleBtn.getStyle() + "-fx-opacity:1;"));
        handleBtn.setOnMouseExited (e -> handleBtn.setStyle(handleBtn.getStyle().replace("-fx-opacity:1;", "-fx-opacity:0.85;")));

        StackPane.setAlignment(handleBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(handleBtn, new Insets(8));
        sidebarWrap.getChildren().add(handleBtn);

        contenido = new StackPane(new Label("Genera el plan para ver el gráfico →")) {{
            setPadding(new Insets(24));
        }};

        split = new SplitPane(sidebarWrap, contenido);
        root = new BorderPane(split);
        Scene scene = new Scene(root, 1280, 720);
        stage.setScene(scene);
        stage.setTitle("Plan Gráfico – MVP");
        stage.setMaximized(true);
        stage.show();

// coloca el divisor acorde al ancho expandido deseado
        Platform.runLater(() -> {
            double init = Math.min(0.4, W_EXP / split.getWidth());
            split.setDividerPositions(init);
            lastDividerPos.set(init);
            beautifySplit();  // opcional: afina el divisor
        });

    }

    // Monday de la columna (1-based)
    private LocalDate mondayOfCol(int col) {
        LocalDate firstMonday = plan.get().inicio.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return firstMonday.plusWeeks(col - 1);
    }

    private void insertarMicroConConfirmacion(PlanGrafico.Micro nuevo, PlanGrafico.Micro editando) {
        var pg = plan.get();

        // copia sin el que se edita
        List<PlanGrafico.Micro> lista = new ArrayList<>(pg.microDiasManuales);
        if (editando != null) lista.remove(editando);
        lista.sort(Comparator.comparing(m -> m.start));

        // Clamp a rango del plan
        LocalDate planIni = pg.inicio, planFin = pg.fin;
        LocalDate ns = nuevo.start.isBefore(planIni) ? planIni : nuevo.start;
        LocalDate ne = end(nuevo).isAfter(planFin) ? planFin : end(nuevo);
        if (ne.isBefore(ns)) { alert("Fuera de rango."); return; }
        nuevo.start = ns; nuevo.lenDays = (int) ChronoUnit.DAYS.between(ns, ne) + 1;

        // Vecinos
        PlanGrafico.Micro prev = null, next = null;
        for (PlanGrafico.Micro m : lista) {
            if (end(m).isBefore(ns)) prev = m;
            if (!m.start.isBefore(ns)) { next = m; break; }
        }

        // no invadir al anterior → mover el inicio al día siguiente
        if (prev != null && !ns.isAfter(end(prev))) {
            ns = end(prev).plusDays(1);
            if (ns.isAfter(planFin)) { alert("No hay espacio disponible."); return; }
            LocalDate tmpEnd = ns.plusDays(nuevo.lenDays - 1);
            if (tmpEnd.isAfter(planFin)) tmpEnd = planFin;
            nuevo.start = ns; nuevo.lenDays = (int) ChronoUnit.DAYS.between(ns, tmpEnd) + 1;
            ne = tmpEnd;
        }

        // invado al siguiente → preguntar
        if (next != null && !ne.isBefore(next.start)) {
            ButtonType BTN_ACORTAR_NUEVO  = new ButtonType("Acortar nuevo", ButtonBar.ButtonData.YES);
            ButtonType BTN_RECORTAR_SIG   = new ButtonType("Recortar siguiente", ButtonBar.ButtonData.NO);
            ButtonType BTN_CANCELAR       = ButtonType.CANCEL;

            Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                    "El nuevo [" + ns + " – " + ne + "] invade al siguiente [" +
                            next.start + " – " + end(next) + "].\n\n¿Qué deseas hacer?",
                    BTN_ACORTAR_NUEVO, BTN_RECORTAR_SIG, BTN_CANCELAR);
            a.setHeaderText("Resolver solape de microciclos");
            var r = a.showAndWait();
            if (r.isEmpty() || r.get() == BTN_CANCELAR) return;

            if (r.get() == BTN_ACORTAR_NUEVO) {
                LocalDate finNuevo = next.start.minusDays(1);
                if (finNuevo.isBefore(ns)) { alert("Sin espacio suficiente."); return; }
                nuevo.lenDays = (int) ChronoUnit.DAYS.between(ns, finNuevo) + 1;

            } else if (r.get() == BTN_RECORTAR_SIG) {
                LocalDate newStartNext = ne.plusDays(1);
                if (newStartNext.isAfter(end(next))) {
                    pg.microDiasManuales.remove(next);
                } else {
                    int newLenNext = (int) ChronoUnit.DAYS.between(newStartNext, end(next)) + 1;
                    next.start = newStartNext;
                    next.lenDays = newLenNext;
                }
            }
        }

        // “carva” los solapes restantes (no destructivo)
        List<PlanGrafico.Micro> out = new ArrayList<>();
        for (PlanGrafico.Micro m : pg.microDiasManuales) {
            if (editando != null && m == editando) continue;

            LocalDate ms = m.start, me = end(m);
            if (!overlaps(ms, me, nuevo.start, end(nuevo))) { out.add(m); continue; }

            // cubierto por completo
            if (!ms.isBefore(nuevo.start) && !me.isAfter(end(nuevo))) continue;

            // partido en dos
            if (ms.isBefore(nuevo.start) && me.isAfter(end(nuevo))) {
                LocalDate ls = ms, le = nuevo.start.minusDays(1);
                LocalDate rs = end(nuevo).plusDays(1), re = me;
                if (!le.isBefore(ls)) out.add(new PlanGrafico.Micro(ls, (int) ChronoUnit.DAYS.between(ls, le) + 1, m.tipo, m.cargaPct));
                if (!re.isBefore(rs)) out.add(new PlanGrafico.Micro(rs, (int) ChronoUnit.DAYS.between(rs, re) + 1, m.tipo, m.cargaPct));
                continue;
            }
            // recortes simples
            if (ms.isBefore(nuevo.start) && !me.isBefore(nuevo.start)) {
                LocalDate le = nuevo.start.minusDays(1);
                if (!le.isBefore(ms)) out.add(new PlanGrafico.Micro(ms, (int) ChronoUnit.DAYS.between(ms, le) + 1, m.tipo, m.cargaPct));
            } else if (!ms.isAfter(end(nuevo)) && me.isAfter(end(nuevo))) {
                LocalDate rs = end(nuevo).plusDays(1);
                if (!me.isBefore(rs)) out.add(new PlanGrafico.Micro(rs, (int) ChronoUnit.DAYS.between(rs, me) + 1, m.tipo, m.cargaPct));
            }
        }
        out.add(new PlanGrafico.Micro(nuevo.start, nuevo.lenDays, nuevo.tipo, nuevo.cargaPct));
        out.sort(Comparator.comparing(m -> m.start));
        pg.microDiasManuales.clear();
        pg.microDiasManuales.addAll(out);

        // actualiza % semanal y repaint
        recalcularPctCargaSemanalDesdeMicros();
        pintarMicrosDias();
    }

    private void abrirDialogoMicro(LocalDate semanaLunes) {
        var pg = plan.get();

        // fecha “representativa” clicada (mitad de la semana)
        LocalDate probe = semanaLunes.plusDays(3);
        PlanGrafico.Micro existente = buscarMicroEnFecha(probe);

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(existente == null ? "Nuevo microciclo" : "Editar microciclo");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(8); gp.setPadding(new Insets(10));

        DatePicker dpInicio = new DatePicker(existente == null ? semanaLunes : existente.start);
        dpInicio.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);   // ¡siempre!
                if (empty || item == null) {
                    setDisable(true);
                    setText(null);
                    return;
                }

                // Ejemplo: solo permitir LUNES y dentro del rango del plan
                boolean isMonday = item.getDayOfWeek() == DayOfWeek.MONDAY;
                boolean inRange = !item.isBefore(plan.get().inicio) && !item.isAfter(plan.get().fin);

                setDisable(!(isMonday && inRange));
                setOpacity(isMonday && inRange ? 1.0 : 0.35);
            }
        });

        int maxDays = (int) ChronoUnit.DAYS.between(dpInicio.getValue(), pg.fin) + 1;
        Spinner<Integer> spDias = spinner(1, Math.max(1, maxDays), existente == null ? 7 : existente.lenDays, 1);

        ComboBox<MicrocicloTipo> cbTipo = new ComboBox<>();
        cbTipo.getItems().setAll(MicrocicloTipo.values());
        cbTipo.setValue(existente == null ? MicrocicloTipo.ORDINARIO : existente.tipo);

        Spinner<Integer> spCarga = spinner(0, 120,
                existente == null ? defaultPctFor(cbTipo.getValue()) : existente.cargaPct, 1);

        cbTipo.valueProperty().addListener((o,ov,nv)-> spCarga.getValueFactory().setValue(defaultPctFor(nv)));

        gp.add(new Label("Inicio (fecha):"), 0, 0); gp.add(dpInicio, 1, 0);
        gp.add(new Label("Duración (días):"), 0, 1); gp.add(spDias, 1, 1);
        gp.add(new Label("Tipo:"),            0, 2); gp.add(cbTipo, 1, 2);
        gp.add(new Label("% de carga:"),      0, 3); gp.add(spCarga,1, 3);

        dlg.getDialogPane().setContent(gp);
        var res = dlg.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        PlanGrafico.Micro nuevo = new PlanGrafico.Micro(
                dpInicio.getValue(),
                spDias.getValue(),
                cbTipo.getValue(),
                spCarga.getValue()
        );

        insertarMicroConConfirmacion(nuevo, existente);
    }

    private void pintarPorcentajeMicroEnFila(Row fila) {
        var pg = plan.get();
        for (int col = 1; col <= pg.semanas; col++) {
            clearCell(fila, col);
            Integer pct = pg.microCargaPct.get(col);
            if (pct == null) continue;
            Label l = new Label(String.valueOf(pct));
            l.setStyle("-fx-font-size: 11px; -fx-text-fill: #455A64;");
            l.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            l.setAlignment(Pos.CENTER);
            GridPane.setRowIndex(l, fila.ordinal());
            GridPane.setColumnIndex(l, col);
            grid.getChildren().add(l);
        }
    }


    private void pintarMicrosDias() {
        var pg = plan.get();
        // limpia fila
        for (int col = 1; col <= pg.semanas; col++) clearCell(Row.MICRO, col);

        for (PlanGrafico.Micro m : pg.microDiasManuales) {
            LocalDate ms = m.start, me = end(m);

            for (int col = 1; col <= pg.semanas; col++) {
                LocalDate ws = mondayOfCol(col), we = sundayOfCol(col);
                LocalDate s = ms.isAfter(ws) ? ms : ws;
                LocalDate e = me.isBefore(we) ? me : we;
                if (s.isAfter(e)) continue;

                int days = (int) ChronoUnit.DAYS.between(s, e) + 1;
                int offset = (int) ChronoUnit.DAYS.between(ws, s); // 0..6

                StackPane cell = getCell(Row.MICRO, col);

                // Spacer + barra proporcional
                Region spacer = new Region();
                Region bar = new Region();
                spacer.prefWidthProperty().bind(cell.widthProperty().multiply(offset / 7.0));
                bar.prefWidthProperty().bind(cell.widthProperty().multiply(days / 7.0).subtract(6));
                bar.setMinHeight(18);

                String bg = MICRO_BG.getOrDefault(m.tipo, "#f0f0f0");
                String bd = MICRO_BORDER.getOrDefault(m.tipo, "#9E9E9E");
                bar.setStyle("""
                -fx-background-color: %s;
                -fx-border-color: %s;
                -fx-border-radius: 8; -fx-background-radius: 8;
                """.formatted(bg, bd));

                Label lb = new Label(m.tipo.getCodigo() + " " + m.cargaPct + "%");
                lb.setStyle("-fx-font-size: 11px; -fx-text-fill: #263238; -fx-font-weight: bold;");
                StackPane barWrap = new StackPane(bar, lb);
                StackPane.setAlignment(lb, Pos.CENTER);

                HBox h = new HBox(spacer, barWrap);
                h.setAlignment(Pos.CENTER_LEFT);
                cell.getChildren().add(h);

                Tooltip.install(barWrap, new Tooltip(
                        "%carga " + m.cargaPct + " | " + m.tipo.getCodigo() + "\n" + s + " – " + e));
                barWrap.setCursor(javafx.scene.Cursor.HAND);

                // click → editar ese micro
                barWrap.setOnMouseClicked(ev -> {
                    if (ev.getButton() == MouseButton.PRIMARY) abrirDialogoMicro(ws); // usa semana de ese tramo
                    else if (ev.getButton() == MouseButton.SECONDARY) {
                        ContextMenu cm = new ContextMenu();
                        MenuItem ed = new MenuItem("Editar microciclo");
                        ed.setOnAction(a -> abrirDialogoMicro(ws));
                        MenuItem del = new MenuItem("Eliminar microciclo");
                        del.setOnAction(a -> { pg.microDiasManuales.remove(m); recalcularPctCargaSemanalDesdeMicros(); pintarMicrosDias(); });
                        cm.getItems().addAll(ed, del);
                        cm.show(barWrap, ev.getScreenX(), ev.getScreenY());
                    }
                });
            }
        }
    }

    private LocalDate sundayOfCol(int col) { return mondayOfCol(col).plusDays(6); }

    // Fin de un micro (inclusive)
    private static LocalDate end(PlanGrafico.Micro m) { return m.start.plusDays(m.lenDays - 1); }

    // Solape por fechas
    private static boolean overlaps(LocalDate a1, LocalDate a2, LocalDate b1, LocalDate b2) {
        return !a1.isAfter(b2) && !b1.isAfter(a2);
    }

    // Busca micro que contenga una fecha
    private PlanGrafico.Micro buscarMicroEnFecha(LocalDate d) {
        for (PlanGrafico.Micro m : plan.get().microDiasManuales) {
            if (!d.isBefore(m.start) && !d.isAfter(end(m))) return m;
        }
        return null;
    }

    // Recalcula % de carga por semana (promedio ponderado por días)
    private void recalcularPctCargaSemanalDesdeMicros() {
        var pg = plan.get();
        pg.microCargaPct.clear();

        for (int w = 1; w <= pg.semanas; w++) {
            LocalDate ws = mondayOfCol(w), we = sundayOfCol(w);
            int sum = 0, days = 0;

            for (PlanGrafico.Micro m : pg.microDiasManuales) {
                LocalDate ms = m.start, me = end(m);
                LocalDate s = ms.isAfter(ws) ? ms : ws;
                LocalDate e = me.isBefore(we) ? me : we;
                if (!s.isAfter(e)) {
                    int d = (int) ChronoUnit.DAYS.between(s, e) + 1;
                    sum += d * m.cargaPct;
                    days += d;
                }
            }
            if (days > 0) {
                pg.microCargaPct.put(w, (int) Math.round(sum / (double) days));
            }
        }
        // si usas minutos/tooltip, refresca fila MIN:
        pintarMinutosFila();

        pintarPorcentajeMicroEnFila(Row.PCT_MICRO);   // refresca cuando cambian micros
    }


    private void toggleRailPretty() {
        boolean toRail = !rail.get();
        rail.set(toRail);
        double target = toRail ? W_RAIL : W_EXP;

        // Cambia flecha
        handleBtn.setText(toRail ? "⮞" : "⮜");

        // Oculta/gestiona el contenido del panel cuando está colapsado
        panelIzquierdo.setVisible(!toRail);
        panelIzquierdo.setManaged(!toRail);

        // Anima el ancho del wrapper (queda súper limpio)
        Timeline t = new Timeline(
                new KeyFrame(Duration.millis(240),
                        new KeyValue(sidebarWrap.prefWidthProperty(), target, Interpolator.EASE_BOTH),
                        new KeyValue(sidebarWrap.maxWidthProperty(),  target, Interpolator.EASE_BOTH)
                )
        );
        t.play();
    }

    private void beautifySplit() {
        // Fondo del SplitPane
        split.setStyle("-fx-background-color: transparent;");

        // Divisor invisible y sin ancho
        split.lookupAll(".split-pane-divider").forEach(div -> {
            div.setMouseTransparent(true); // opcional: que no capture clicks
            div.setStyle("""
            -fx-background-color: transparent;
            -fx-border-color: transparent;
            -fx-background-insets: 0;
            -fx-padding: 0;            /* quita el “grosor” */
        """);

            // Elimina el “grabber” (las rayitas dobles)
            div.lookupAll(".vertical-grabber").forEach(g ->
                    g.setStyle("-fx-background-color: transparent; -fx-padding: 0;")
            );
            div.lookupAll(".horizontal-grabber").forEach(g ->
                    g.setStyle("-fx-background-color: transparent; -fx-padding: 0;")
            );
        });
    }

    private void toggleRail() {
        SplitPane.Divider div = split.getDividers().get(0);

        if (!rail.get()) {                // → colapsar a rail
            lastDividerPos.set(div.getPosition());         // recuerda posición actual
            double railPos = Math.max(0.0, Math.min(0.35, W_RAIL / Math.max(1.0, split.getWidth())));
            animateDivider(div.positionProperty(), railPos);
            // esconde el contenido pesado del panel si quieres:
            panelIzquierdo.setVisible(false);
            panelIzquierdo.setManaged(false);
            rail.set(true);
        } else {                          // → expandir a la posición previa
            panelIzquierdo.setManaged(true);
            panelIzquierdo.setVisible(true);
            double target = (lastDividerPos.get() < 0.08 ? 0.22 : lastDividerPos.get());
            animateDivider(div.positionProperty(), target);
            rail.set(false);
        }
    }



    // ====== UI: Panel de entradas ======
    private Node panelInputs(Stage stage) {
        var p = plan.get();

        dpInicio = new DatePicker(p.inicio);
        spSemanas = spinner(4, 104, p.semanas, 1);
        spSes = spinner(1, 14, p.sesionesSem, 1);

        spPrep = spinner(0, 100, p.pctPrep, 1);
        spComp = spinner(0, 100, p.pctComp, 1);
        spTrans = spinner(0, 100, p.pctTrans, 1);

        spPrepGen = spinner(0, 100, p.pctPrepGeneral, 1);
        spPrepEsp = spinner(0, 100, p.pctPrepEspecial, 1);
        spCompPre = spinner(0, 100, p.pctCompPrecomp, 1);
        spCompComp = spinner(0, 100, p.pctCompComp, 1);
        spTransRec = spinner(0, 100, p.pctTransRecup, 1);

        spVolIni = spinner(0, 100, p.volIni, 1);
        spVolFin = spinner(0, 100, p.volFin, 1);
        spIntIni = spinner(0, 100, p.intIni, 1);
        spIntFin = spinner(0, 100, p.intFin, 1);

        cbTipo = new ComboBox<>();
        cbTipo.getItems().addAll(TipoPeriodizacion.CLASICA, TipoPeriodizacion.ATR);
        cbTipo.setValue(plan.get().tipo);

        cbTipo.getItems().addAll(TipoPeriodizacion.CLASICA, TipoPeriodizacion.ATR);
        cbTipo.setValue(plan.get().tipo);

        Button btnTiemposBloque = new Button("Distribuir tiempos (bloque)");
        btnTiemposBloque.setOnAction(e -> abrirEditorTiemposRango());

        Button btnGenerar = new Button("Generar plan");
        btnGenerar.setDefaultButton(true);
        /*btnGenerar.setOnAction(e -> {
            if (validarPercentajes()) {
                plan.set(leerPlanDeUI());
                dibujarPlan();
            } else {
                alert("Los porcentajes de periodos deben sumar 100.\n" +
                        "Dentro de PREPARATORIO (General+Especial) deben sumar 100.\n" +
                        "Dentro de COMPETITIVO (Precomp+Competitiva) deben sumar 100.");
            }
        });*/

        btnGenerar.setOnAction(e -> {
            if (validarPercentajes()) {
                // preserva asignaciones dentro del nuevo número de semanas
                var anterior = plan.get();
                var nuevo = leerPlanDeUI();
                if (anterior != null && anterior.microAsignaciones != null) {
                    nuevo.microAsignaciones = new HashMap<>();
                    for (var en : anterior.microAsignaciones.entrySet()) {
                        if (en.getKey() >= 1 && en.getKey() <= nuevo.semanas) {
                            nuevo.microAsignaciones.put(en.getKey(), en.getValue());
                        }
                    }
                }

                // preserva tiempos
                for (var map : List.of(anterior.minutosTotal, anterior.minutosPF, anterior.minutosTT, anterior.microCargaPct)) {
                    if (map == null) continue;
                    Map<Integer,Integer> target = map == anterior.minutosTotal ? (nuevo.minutosTotal = new HashMap<>())
                            : map == anterior.minutosPF    ? (nuevo.minutosPF    = new HashMap<>())
                            : map == anterior.minutosTT    ? (nuevo.minutosTT    = new HashMap<>())
                            :                                 (nuevo.microCargaPct= new HashMap<>());
                    for (var en : map.entrySet()) {
                        if (en.getKey() >= 1 && en.getKey() <= nuevo.semanas) {
                            target.put(en.getKey(), en.getValue());
                        }
                    }
                }
                plan.set(nuevo);
                dibujarPlan();
            } else {
                alert("Revisa que los porcentajes sumen 100 según el tipo de periodización seleccionado.");
            }
        });


        // Fin por defecto: inicio + (semanas-1) semanas
        LocalDate finDef = (p.fin != null) ? p.fin : p.inicio.plusWeeks(Math.max(0, p.semanas - 1));
        dpFin = new DatePicker(finDef);  // <—— NUEVO

        spSemanas = spinner(4, 104, p.semanas, 1);

// ===== Sincronización Inicio/Fin/Semanas =====
        dpInicio.valueProperty().addListener((obs, oldV, ini) -> {
            if (ini == null || updatingUI) return;
            updatingUI = true;
            // Mantén la duración en semanas al cambiar el inicio
            int sem = spSemanas.getValue();
            dpFin.setValue(ini.plusWeeks(Math.max(0, sem - 1)));
            updatingUI = false;
        });

        dpFin.valueProperty().addListener((obs, oldV, fin) -> {
            if (fin == null || dpInicio.getValue() == null || updatingUI) return;
            updatingUI = true;
            LocalDate ini = dpInicio.getValue();
            if (fin.isBefore(ini)) {
                // corrige automáticamente: no permitir fin < inicio
                fin = ini;
                dpFin.setValue(fin);
            }
            int semanas = (int) (ChronoUnit.WEEKS.between(ini, fin) + 1); // inclusivo
            semanas = Math.max(1, semanas);
            spSemanas.getValueFactory().setValue(semanas);
            updatingUI = false;
        });


        // === Botón para abrir CalendarioAnualDialog ===
        Button btnCalendario = new Button("Calendario anual…");
        btnCalendario.setOnAction(e -> {
            CalendarioAnualDialog cal = new CalendarioAnualDialog();
            cal.setRangoInicial(dpInicio.getValue(), dpFin.getValue());
            cal.mostrar(stage, new Label());

            LocalDate ini = cal.getFechaInicioSeleccionada();
            LocalDate fin = cal.getFechaFinSeleccionada();
            if (ini != null && fin != null) {
                dpInicio.setValue(ini);
                dpFin.setValue(fin);
            }



            // === Nuevo: obtener días y horarios ===
            List<DayOfWeek> dias = cal.getDiasSeleccionados();
            Map<DayOfWeek, LocalTime[]> horarios = cal.getHorariosSeleccionados();

            // Aquí puedes guardarlos en tu objeto Plan o mostrarlos en UI
            System.out.println("Días seleccionados: " + dias);
            for (DayOfWeek d : horarios.keySet()) {
                LocalTime[] h = horarios.get(d);
                System.out.println(d + " de " + h[0] + " a " + h[1]);
            }

            // Ejemplo: guardarlos en tu modelo PlanGrafico
            p.setDiasEntrenamiento(dias);
            p.setHorariosEntrenamiento(horarios);

            Map<Integer, int[]> datos = cal.getDiasYMinutosPorSemana(ini, fin);

            p.diasTotal.clear();
            p.minutosTotal.clear();



            for (Map.Entry<Integer, int[]> en : datos.entrySet()) {
                int semana = en.getKey();
                int _dias = en.getValue()[0];
                int minutos = en.getValue()[1];
                p.diasTotal.put(semana, _dias);
                p.minutosTotal.put(semana, minutos);
            }

            dibujarPlan(); // refresca el grid

        });

        spSemanas.valueProperty().addListener((obs, oldV, sem) -> {
            if (sem == null || dpInicio.getValue() == null || updatingUI) return;
            updatingUI = true;
            LocalDate ini = dpInicio.getValue();
            int s = Math.max(1, sem.intValue());
            dpFin.setValue(ini.plusWeeks(s - 1));
            updatingUI = false;
        });

        grpClasicaPeriodos = new VBox(
                titulo("Periodización CLÁSICA – Periodos (%) [suma 100]"),
                fila("Preparatorio", spPrep),
                fila("Competitivo", spComp),
                fila("Transitorio", spTrans)
        );
        grpClasicaPeriodos.setSpacing(6);

        grpClasicaPeriodos = new VBox(
                titulo("Periodización CLÁSICA – Periodos (%) [suma 100]"),
                fila("Preparatorio", spPrep),
                fila("Competitivo", spComp),
                fila("Transitorio", spTrans)
        );
        grpClasicaPeriodos.setSpacing(6);

        spATR_A = spinner(0, 100, plan.get().pctATR_A, 1);
        spATR_T = spinner(0, 100, plan.get().pctATR_T, 1);
        spATR_R = spinner(0, 100, plan.get().pctATR_R, 1);

        grpATR = new VBox(
                titulo("Periodización ATR – % global por fases [suma 100]"),
                fila("Acumulación (A)", spATR_A),
                fila("Transformación (T)", spATR_T),
                fila("Realización (R)", spATR_R)
        );
        grpATR.setSpacing(6);


        Button btnGuardar = new Button("Guardar JSON");
        btnGuardar.setOnAction(e -> guardarJSON(stage));

        Button btnCargar = new Button("Cargar JSON");
        btnCargar.setOnAction(e -> cargarJSON(stage));

        VBox box = new VBox(10,
                titulo("Datos del macrociclo"),
                fila("Tipo de periodización", cbTipo),        // ← NUEVO

                fila("Inicio", dpInicio),
                fila("Fin", dpFin),                 // <—— NUEVO

                // ⬇️ NUEVO: abre el calendario anual para elegir rango
                fila("Seleccionar en calendario", btnCalendario),

                fila("Semanas", spSemanas),
                fila("Sesiones/sem", spSes),

                sep(),
                titulo("Periodos (%) – debe sumar 100"),
                fila("Preparatorio", spPrep),
                fila("Competitivo", spComp),
                fila("Transitorio", spTrans),

                sep(),
                titulo("Etapas por periodo (%)"),

                subt("Preparatorio (100%)"),
                fila("General", spPrepGen),
                fila("Especial", spPrepEsp),

                subt("Competitivo (100%)"),
                fila("Precompetitiva", spCompPre),
                fila("Competitiva", spCompComp),

                subt("Transitorio"),
                fila("Recuperación (resto es descanso activo)", spTransRec),

                sep(),
                titulo("Tendencia Vol/Int (%)"),
                fila("Volumen: inicio → fin", new HBox(6, spVolIni, new Label("→"), spVolFin)),
                fila("Intensidad: inicio → fin", new HBox(6, spIntIni, new Label("→"), spIntFin)),

                sep(),
                new HBox(8, btnGenerar, btnGuardar, btnCargar, btnTiemposBloque)  // ← añade aquí
        );

        /*Runnable aplicarTipoUI = () -> {
            boolean esClasica = cbTipo.getValue() == PeriodizacionTipo.CLASICA;
            grpClasicaPeriodos.setManaged(esClasica);
            grpClasicaPeriodos.setVisible(esClasica);
            grpClasicaEtapas.setManaged(esClasica);
            grpClasicaEtapas.setVisible(esClasica);

            grpATR.setManaged(!esClasica);
            grpATR.setVisible(!esClasica);
        };
        cbTipo.valueProperty().addListener((o, ov, nv) -> aplicarTipoUI.run());
        aplicarTipoUI.run();
*/
        box.setPadding(new Insets(16));
        box.setPrefWidth(360);
        return new ScrollPane(box) {{
            setFitToWidth(true);
            setHbarPolicy(ScrollBarPolicy.NEVER);
        }};
    }

    private int defaultPctFor(MicrocicloTipo t) {
        return switch (t) {
            case ORDINARIO       -> 60;
          //  case CARGA           -> 70;
            case CHOQUE          -> 95;
            case APROXIMACION    -> 40; // precompetitivo 35–45
            case COMPETICION     -> 25; // competencia 20–30
            case RESTABLECIMIENTO-> 50; // o <60 según prefieras
        };
    }

    /**
    * Separador dentro del VBox
    * */
    private static Node sep() {
        var sp = new Separator();
        sp.setPadding(new Insets(6, 0, 6, 0));
        return sp;
    }

    private static Label titulo(String s) {
        var lb = new Label(s);
        lb.setFont(Font.font(16));
        lb.setStyle("-fx-font-weight: bold;");
        return lb;
    }

    private static Label subt(String s) {
        var lb = new Label(s);
        lb.setFont(Font.font(13));
        lb.setStyle("-fx-text-fill: #444;");
        return lb;
    }

    private static HBox fila(String label, Node input) {
        var l = new Label(label);
        l.setPrefWidth(180);
        return new HBox(10, l, input) {{
            setAlignment(Pos.CENTER_LEFT);
        }};
    }

    private static <T extends Number> Spinner<Integer> spinner(int min, int max, int val, int step) {
        Spinner<Integer> sp = new Spinner<>(min, max, val, step);
        sp.setEditable(true);
        sp.setPrefWidth(120);
        return sp;
    }

    private boolean validarPercentajes() {
        /*int p = spPrep.getValue() + spComp.getValue() + spTrans.getValue();
        int a = spPrepGen.getValue() + spPrepEsp.getValue();
        int c = spCompPre.getValue() + spCompComp.getValue();
        return p == 100 && a == 100 && c == 100;*/

        if (cbTipo.getValue() == TipoPeriodizacion.CLASICA) {
            int p = spPrep.getValue() + spComp.getValue() + spTrans.getValue();
            int a = spPrepGen.getValue() + spPrepEsp.getValue();
            int c = spCompPre.getValue() + spCompComp.getValue();
            return p == 100 && a == 100 && c == 100;
        } else { // ATR
            int atr = spATR_A.getValue() + spATR_T.getValue() + spATR_R.getValue();
            return atr == 100;
        }
    }

    private PlanGrafico leerPlanDeUI() {
        /*PlanGrafico pg = new PlanGrafico();
        pg.inicio = dpInicio.getValue();
        pg.fin = dpFin.getValue();                 // <—— NUEVO

        pg.semanas = spSemanas.getValue();
        pg.sesionesSem = spSes.getValue();
        pg.pctPrep = spPrep.getValue();
        pg.pctComp = spComp.getValue();
        pg.pctTrans = spTrans.getValue();

        pg.pctPrepGeneral = spPrepGen.getValue();
        pg.pctPrepEspecial = spPrepEsp.getValue();
        pg.pctCompPrecomp = spCompPre.getValue();
        pg.pctCompComp = spCompComp.getValue();
        pg.pctTransRecup = spTransRec.getValue();

        pg.volIni = spVolIni.getValue();
        pg.volFin = spVolFin.getValue();
        pg.intIni = spIntIni.getValue();
        pg.intFin = spIntFin.getValue();
        return pg;*/


            PlanGrafico pg = new PlanGrafico();
            pg.tipo = cbTipo.getValue();

            pg.inicio = dpInicio.getValue();
            pg.fin = dpFin.getValue();
            pg.semanas = spSemanas.getValue();
       //     pg.sesionesSem = spSes.getValue();

            if (pg.tipo == TipoPeriodizacion.CLASICA) {
                pg.pctPrep = spPrep.getValue();
                pg.pctComp = spComp.getValue();
                pg.pctTrans = spTrans.getValue();

                pg.pctPrepGeneral = spPrepGen.getValue();
                pg.pctPrepEspecial = spPrepEsp.getValue();
                pg.pctCompPrecomp = spCompPre.getValue();
                pg.pctCompComp = spCompComp.getValue();
                pg.pctTransRecup = spTransRec.getValue();
            } else {
                pg.pctATR_A = spATR_A.getValue();
                pg.pctATR_T = spATR_T.getValue();
                pg.pctATR_R = spATR_R.getValue();
                // Los campos de Clásica se ignoran
            }

            pg.volIni = spVolIni.getValue();

            pg.volFin = spVolFin.getValue();
            pg.intIni = spIntIni.getValue();
            pg.intFin = spIntFin.getValue();
            return pg;
        }

    // Mes que tiene más días dentro de la semana (lunes–domingo) que contiene 'd'
    private Month mesDominante(LocalDate d) {
        LocalDate lunes   = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate domingo = d.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        Month mesA = lunes.getMonth();
        Month mesB = domingo.getMonth();
        if (mesA == mesB) return mesA; // toda la semana cae en el mismo mes

        // Días de la semana que pertenecen al mes del lunes
        LocalDate finRango = lunes.plusDays(6);
        LocalDate finMesA  = lunes.with(TemporalAdjusters.lastDayOfMonth());
        LocalDate to       = finRango.isBefore(finMesA) ? finRango : finMesA;

        long diasMesA = ChronoUnit.DAYS.between(lunes, to) + 1; // inclusivo
        long diasMesB = 7 - diasMesA;                           // resto de la semana

        return (diasMesA > diasMesB) ? mesA : mesB; // mayoría
    }


    // ====== Dibujo del plan ======
    private void dibujarPlan() {
        var pg = plan.get();

        // Layout base
        grid = new GridPane();
        grid.getStyleClass().add("plan-grid");
        grid.setGridLinesVisible(false);
        grid.setPadding(new Insets(16));
        grid.setVgap(2);
        grid.setHgap(2);

        // Columnas
        grid.getColumnConstraints().clear();
        ColumnConstraints c0 = new ColumnConstraints(COL_TITULOS_ANCHO);
        grid.getColumnConstraints().add(c0);

      /*  for (int i = 0; i < pg.semanas; i++) {
            ColumnConstraints c = new ColumnConstraints(COL_SEMANA_ANCHO);
            grid.getColumnConstraints().add(c);
        }*/


        // SES (igual) y MIN con tiempos asignados
      /*  for (int col = 1; col <= pg.semanas; col++) {
            addCenterText(Row.SES, col, String.valueOf(pg.sesionesSem));
        }*/

        for (int col = 1; col <= pg.semanas; col++) {

            int sesiones = pg.sesionesPorSemana.getOrDefault(col, List.of()).size();
            addCenterText(Row.SES, col, String.valueOf(sesiones));
        }

        pintarMinutosFila();

        // Fondo de celdas "semana"
        for (int col = 1; col <= pg.semanas; col++) {
            for (Row row : Row.values()) {
                var cell = new StackPane();
                cell.setMinSize(COL_SEMANA_ANCHO, 28);
                cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                String base = (row.ordinal() % 2 == 0) ? "#ffffff" : "#fafafa";
                String baseStyle = "-fx-background-color: " + base + "; -fx-border-color: #e6e6e6; -fx-border-width: 0 1 1 0;";
                cell.setStyle(baseStyle);
                cell.getProperties().put("baseStyle", baseStyle);

                GridPane.setRowIndex(cell, row.ordinal());
                GridPane.setColumnIndex(cell, col);
                grid.getChildren().add(cell);
            }
        }

        // Click/hover en fila MICRO (para crear/editar)
        for (int col = 1; col <= plan.get().semanas; col++) {
            StackPane cell = getCell(Row.MICRO, col);
            cell.setCursor(javafx.scene.Cursor.HAND);
            Tooltip.install(cell, new Tooltip("Agregar/editar microciclo (por días)"));
            final int c = col;
            cell.setOnMouseClicked(ev -> {
                if (ev.getButton() == MouseButton.PRIMARY) {
                    abrirDialogoMicro(mondayOfCol(c));
                }
            });
        }

        // Columna títulos
        int r = 0;
        addTitulo(grid, r++, "SEM");
        addTitulo(grid, r++, "FECHA");
        addTitulo(grid, r++, "MES");
        addTitulo(grid, r++, "PERIODO");
        addTitulo(grid, r++, "ETAPA");
        addTitulo(grid, r++, "MESOCICLO");
        addTitulo(grid, r++, "MICRO");
        addTitulo(grid, r++, "% MICRO");   // o "% carga"

        addTitulo(grid, r++, "VOL / INT");
        addTitulo(grid, r++, "CONTROLES");
        addTitulo(grid, r++, "COMP");
        addTitulo(grid, r++, "SES");
        addTitulo(grid, r++, "MIN");

      /*  // SEM + INICIO
        LocalDate start = pg.inicio;
        List<LocalDate> semanaInicios = new ArrayList<>();
        for (int i = 0; i < pg.semanas; i++) {
            LocalDate s = start.plusWeeks(i);
            semanaInicios.add(s);
            addCenterText(Row.SEM, i+1, String.valueOf(i+1));
            addCenterText(Row.INICIO, i+1, s.format(DF_DD_MM));
        }*/

        LocalDate start = pg.inicio;
        List<LocalDate> semanaInicios = new ArrayList<>();
        for (int i = 0; i < pg.semanas; i++) {
            LocalDate d = start.plusWeeks(i);
            LocalDate lunes = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            semanaInicios.add(lunes);                    // ← importante

            addCenterText(Row.SEM,    i+1, String.valueOf(i+1));
            addCenterText(Row.INICIO, i+1, etiquetaLunesDomingo(d)); // "dd / dd"
        }



        // MES: spans agrupados por mes
        agruparMesesComoSpans(semanaInicios);

       /* // Periodos → semanas por % (regla de 3 con ajuste)
        Map<PeriodoTipo, Integer> semanasPeriodo = distribuirPorcentajes(pg.semanas, Map.of(
                PeriodoTipo.PREPARATORIO, pg.pctPrep,
                PeriodoTipo.COMPETITIVO, pg.pctComp,
                PeriodoTipo.TRANSITORIO, pg.pctTrans
        ));*/

        // Pinta PERIODO
       /* int colBase = 1;
        Map<PeriodoTipo, Color> colPeriodo = Map.of(
                PeriodoTipo.PREPARATORIO, Color.web("#B3E5FC"),
                PeriodoTipo.COMPETITIVO, Color.web("#FFE0B2"),
                PeriodoTipo.TRANSITORIO, Color.web("#C8E6C9")
        );

        // ETAPA por periodo
        Map<EtapaTipo, Color> colEtapa = Map.of(
                EtapaTipo.GENERAL, Color.web("#81D4FA"),
                EtapaTipo.ESPECIAL, Color.web("#4FC3F7"),
                EtapaTipo.PRECOMPETITIVA, Color.web("#FFCC80"),
                EtapaTipo.COMPETITIVA, Color.web("#FFB74D"),
                EtapaTipo.RECUPERACION, Color.web("#A5D6A7")
        );*/

        // Guardamos mesociclos (4-6 semanas aprox.) para graficar luego
        List<int[]> mesociclos = new ArrayList<>();

        /*for (PeriodoTipo periodo : List.of(PeriodoTipo.PREPARATORIO, PeriodoTipo.COMPETITIVO, PeriodoTipo.TRANSITORIO)) {
            int len = semanasPeriodo.getOrDefault(periodo, 0);
            if (len <= 0) continue;
            int cIni = colBase;
            int cFin = colBase + len - 1;

            span(Row.PERIODO, cIni, len, label(periodo.name()), colPeriodo.get(periodo));

            // Etapas dentro del periodo
            if (periodo == PeriodoTipo.PREPARATORIO) {
                var m = distribuirPorcentajes(len, Map.of(
                        EtapaTipo.GENERAL, plan.get().pctPrepGeneral,
                        EtapaTipo.ESPECIAL, plan.get().pctPrepEspecial
                ));
                colBase = pintarEtapas(EtapaTipo.GENERAL, EtapaTipo.ESPECIAL, colEtapa, mesociclos, colBase, m);
            } else if (periodo == PeriodoTipo.COMPETITIVO) {
                var m = distribuirPorcentajes(len, Map.of(
                        EtapaTipo.PRECOMPETITIVA, plan.get().pctCompPrecomp,
                        EtapaTipo.COMPETITIVA, plan.get().pctCompComp
                ));
                colBase = pintarEtapas(EtapaTipo.PRECOMPETITIVA, EtapaTipo.COMPETITIVA, colEtapa, mesociclos, colBase, m);
            } else { // TRANSITORIO
                // Recuperación explícita y el resto descanso activo (no lo pintamos, solo etiqueta RECUP)
                var m = distribuirPorcentajes(len, Map.of(
                        EtapaTipo.RECUPERACION, plan.get().pctTransRecup
                ));
                int rec = m.getOrDefault(EtapaTipo.RECUPERACION, 0);
                if (rec > 0) {
                    span(Row.ETAPA, colBase, rec, label("RECUPERACIÓN"), colEtapa.get(EtapaTipo.RECUPERACION));
                    // Un mesociclo corto de recuperación:
                    mesociclos.add(new int[]{colBase, colBase + Math.max(1, rec) - 1});
                    colBase += rec;
                    // Resto: descanso activo (etiqueta suave)
                    int rest = len - rec;
                    if (rest > 0) {
                        span(Row.ETAPA, colBase, rest, label("DESCANSO ACTIVO"), Color.web("#E0E0E0"));
                        // no contamos como mesociclo “duro”
                        colBase += rest;
                    }
                } else {
                    span(Row.ETAPA, colBase, len, label("TRANSITORIO"), Color.web("#E0E0E0"));
                    colBase += len;
                }
            }
        }*/

        int colBase = 1;
      //  List<int[]> mesociclos = new ArrayList<>();

        if (plan.get().tipo == TipoPeriodizacion.CLASICA) {
            // ==== CLÁSICA: igual que antes ====
            Map<PeriodoTipo, Integer> semanasPeriodo = distribuirPorcentajes(plan.get().semanas, Map.of(
                    PeriodoTipo.PREPARATORIO, plan.get().pctPrep,
                    PeriodoTipo.COMPETITIVO, plan.get().pctComp,
                    PeriodoTipo.TRANSICION, plan.get().pctTrans
            ));

            Map<PeriodoTipo, Color> colPeriodo = Map.of(
                    PeriodoTipo.PREPARATORIO, Color.web("#B3E5FC"),
                    PeriodoTipo.COMPETITIVO, Color.web("#FFE0B2"),
                    PeriodoTipo.TRANSICION, Color.web("#C8E6C9")
            );
            Map<EtapaTipo, Color> colEtapa = Map.of(
                    EtapaTipo.GENERAL, Color.web("#81D4FA"),
                    EtapaTipo.ESPECIAL, Color.web("#4FC3F7"),
                    EtapaTipo.PRECOMPETITIVA, Color.web("#FFCC80"),
                    EtapaTipo.COMPETITIVA, Color.web("#FFB74D"),
                    EtapaTipo.RECUPERACION, Color.web("#A5D6A7")
            );

            for (PeriodoTipo periodo : PeriodoTipo.values()) {
                int len = semanasPeriodo.getOrDefault(periodo, 0);
                if (len <= 0) continue;

                span(Row.PERIODO, colBase, len, label(periodo.name()), colPeriodo.get(periodo));

                if (periodo == PeriodoTipo.PREPARATORIO) {
                    var m = distribuirPorcentajes(len, Map.of(
                            EtapaTipo.GENERAL, plan.get().pctPrepGeneral,
                            EtapaTipo.ESPECIAL, plan.get().pctPrepEspecial
                    ));
                    colBase = pintarEtapas(EtapaTipo.GENERAL, EtapaTipo.ESPECIAL, colEtapa, mesociclos, colBase, m);

                } else if (periodo == PeriodoTipo.COMPETITIVO) {
                    var m = distribuirPorcentajes(len, Map.of(
                            EtapaTipo.PRECOMPETITIVA, plan.get().pctCompPrecomp,
                            EtapaTipo.COMPETITIVA, plan.get().pctCompComp
                    ));
                    colBase = pintarEtapas(EtapaTipo.PRECOMPETITIVA, EtapaTipo.COMPETITIVA, colEtapa, mesociclos, colBase, m);

                } else { // TRANSITORIO
                    var m = distribuirPorcentajes(len, Map.of(EtapaTipo.RECUPERACION, plan.get().pctTransRecup));
                    int rec = m.getOrDefault(EtapaTipo.RECUPERACION, 0);
                    if (rec > 0) {
                        span(Row.ETAPA, colBase, rec, label("RECUPERACIÓN"), colEtapa.get(EtapaTipo.RECUPERACION));
                        mesociclos.add(new int[]{colBase, colBase + Math.max(1, rec) - 1});
                        colBase += rec;
                    }
                    int rest = len - rec;
                    if (rest > 0) {
                        span(Row.ETAPA, colBase, rest, label("DESCANSO ACTIVO"), Color.web("#E0E0E0"));
                        colBase += rest;
                    }
                }
            }

        } else {
            // ==== ATR ====
            Map<ATRFase, Integer> semanasATR = distribuirPorcentajes(plan.get().semanas, Map.of(
                    ATRFase.ACUMULACION, plan.get().pctATR_A,
                    ATRFase.TRANSFORMACION, plan.get().pctATR_T,
                    ATRFase.REALIZACION, plan.get().pctATR_R
            ));
            Map<ATRFase, Color> colATR = Map.of(
                    ATRFase.ACUMULACION,   Color.web("#90CAF9"),
                    ATRFase.TRANSFORMACION,Color.web("#FFE082"),
                    ATRFase.REALIZACION,   Color.web("#A5D6A7")
            );

            for (ATRFase f : List.of(ATRFase.ACUMULACION, ATRFase.TRANSFORMACION, ATRFase.REALIZACION)) {
                int len = semanasATR.getOrDefault(f, 0);
                if (len <= 0) continue;

                // PERIODO = Fase ATR
                span(Row.PERIODO, colBase, len, label(nombreFase(f)), colATR.get(f));

                // En ATR no subdividimos en "etapas"; replicamos la fase como etiqueta suave en ETAPA
                span(Row.ETAPA, colBase, len, label("Bloque " + siglaFase(f)), Color.web("#EEEEEE"));

                // Guardamos para partir mesociclos (4–6 semanas) dentro de cada bloque
                mesociclos.add(new int[]{colBase, colBase + len - 1});
                colBase += len;
            }
        }


        // MESOCICLO (4–6 semanas aprox.) a partir de etapas: usamos lo almacenado
       /* for (int[] mc : mesociclos) {
            int startCol = mc[0];
            int endCol = mc[1];
            int len = endCol - startCol + 1;

            // Partimos en trozos de 4–6 con preferencia 4 o 5
            int idx = startCol;
            while (idx <= endCol) {
                int remain = endCol - idx + 1;
                int take = (remain >= 6) ? 5 : Math.min(remain, Math.max(4, remain));
             //   span(Row.MESOCICLO, idx, take, label("Meso"), Color.web("#E1BEE7"));
                idx += take;
            }
        }*/

       /* // MICRO: etiqueta por semana, sin solapar (una celda = un micro)
        for (int col = 1; col <= pg.semanas; col++) {
            addChip(Row.MICRO, col, "μ", Color.web("#f0f0f0"), "#9E9E9E");
        }*/
        /*pintarMicroFila();*/
        pintarMicrosDias();
        recalcularPctCargaSemanalDesdeMicros();

        pintarPorcentajeMicroEnFila(Row.PCT_MICRO);   // <- pinta los % en la nueva fila
        pintarMesociclosManuales();

     /*   for (int col = 1; col <= pg.semanas; col++) {
            StackPane cell = getCell(Row.MESOCICLO, col);
            cell.setCursor(javafx.scene.Cursor.HAND);
            Tooltip.install(cell, new Tooltip("Agregar/editar mesociclo"));
            final int c = col;
            cell.setOnMouseClicked(ev -> {
                if (ev.getButton() == MouseButton.PRIMARY) {
                    abrirDialogoMesociclo(c);
                } else if (ev.getButton() == MouseButton.SECONDARY) {
                    abrirMenuMesocicloContextual(c, cell, ev.getScreenX(), ev.getScreenY());
                }
            });
        }
*/
        for (int col = 1; col <= pg.semanas; col++) {
            StackPane cell = getCell(Row.MESOCICLO, col);

            // si encima hay un span de mesociclo, dejamos pasar los eventos al cell
            // (haz esto solo si tu método span no lo hace ya; ver punto 3)
            cell.setMouseTransparent(false);

            cell.setCursor(javafx.scene.Cursor.HAND);
            Tooltip.install(cell, new Tooltip("Agregar/editar mesociclo"));
            final int c = col;

            // HOVER: resalta solo la celda del meso
            cell.setOnMouseEntered(e -> showPrettyMesoHover(cell));
            cell.setOnMouseExited (e -> { if (hoverMesoCell == cell) clearPrettyMesoHover(); });


            // CLICK: abre diálogo o menú contextual
            cell.setOnMouseClicked(ev -> {
                if (ev.getButton() == MouseButton.PRIMARY) {
                    abrirDialogoMesociclo(c);
                } else if (ev.getButton() == MouseButton.SECONDARY) {
                    abrirMenuMesocicloContextual(c, cell, ev.getScreenX(), ev.getScreenY());
                }
            });
        }

        // VOL / INT en la MISMA FILA con barras diferenciadas
        pintarVolumenIntensidad(pg);

        // SES y MIN (por semana)
        for (int col = 1; col <= pg.semanas; col++) {
            addCenterText(Row.SES, col, String.valueOf(pg.sesionesSem));
            // minutos estimados por semana: 90’ por sesión (ajústalo si quieres)
            int minutos = pg.sesionesSem * 90;
            addCenterText(Row.MIN, col, String.valueOf(minutos));
        }

        // CONTROLES y COMP (click derecho para añadir fichas)
        ContextMenu ctx = new ContextMenu();
        MenuItem miControl = new MenuItem("Agregar control (semana)");
        MenuItem miComp = new MenuItem("Agregar competencia (semana)");
        MenuItem miEditSem = new MenuItem("Editar tiempos (semana)...");
        MenuItem miClearSem = new MenuItem("Limpiar tiempos (semana)");
        MenuItem miClearRango = new MenuItem("Limpiar tiempos (rango)...");
        Menu mMicro = new Menu("Asignar microciclo");
        for (MicrocicloTipo t : MicrocicloTipo.values()) {
            MenuItem mi = new MenuItem((t.getCodigo()) + " (" + t.getCodigo() + ")");
            mi.setOnAction(a -> {
                int col = (int) mi.getUserData(); // lo setemos abajo
                asignarMicro(col, t);
            });
            mMicro.getItems().add(mi);
        }
        MenuItem miRango = new MenuItem("Asignar a rango…");



        ctx.getItems().setAll(
                mMicro, new SeparatorMenuItem(),
                miControl, miComp, new SeparatorMenuItem(),
                miEditSem, miClearSem, miClearRango
        );

// … y en el handler del contexto (donde calculas `col`):
        grid.addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, ev -> {
            Point2D pxy = new Point2D(ev.getSceneX(), ev.getSceneY());
            int col = pickCol(pxy);
            if (col >= 1 && col <= pg.semanas) {

                // Actualiza el userData del submenú con la columna clicada
                for (var it : mMicro.getItems()) it.setUserData(col);

                miControl.setOnAction(a -> addChip(Row.CONTROLES, col, "TEST", Color.web("#BBDEFB"), "#1976D2"));
                miComp.setOnAction(a -> addChip(Row.COMP, col, "COMP", Color.web("#FFCDD2"), "#C62828"));

                miRango.setOnAction(a -> asignarMicroRangoDialog(col));

                miEditSem.setOnAction(a -> editarTiempoSemana(col));
                miClearSem.setOnAction(a -> limpiarTiempoSemana(col));
                miClearRango.setOnAction(a -> limpiarTiempoRangoDialog(col));

                ctx.getItems().setAll(mMicro, new SeparatorMenuItem(), miControl, miComp, miRango);
                ctx.show(grid, ev.getScreenX(), ev.getScreenY());
            } else {
                ev.consume();
            }
        });

     /*   grid.addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, ev -> {
            // aproximar a columna
            Point2D pxy = new Point2D(ev.getSceneX(), ev.getSceneY());
            int col = pickCol(pxy);
            if (col >= 1 && col <= pg.semanas) {
                miControl.setOnAction(a -> addChip(Row.CONTROLES, col, "TEST", Color.web("#BBDEFB"), "#1976D2"));
                miComp.setOnAction(a -> addChip(Row.COMP, col, "COMP", Color.web("#FFCDD2"), "#C62828"));
                ctx.show(grid, ev.getScreenX(), ev.getScreenY());
            } else {
                ev.consume();
            }
        });
*/
       /* root.setCenter(new ScrollPane(grid) {{
            setFitToWidth(true);
            setFitToHeight(true);
        }});*/

        ScrollPane sc = new ScrollPane(grid);
        sc.setFitToWidth(true);
        sc.setFitToHeight(true);

        // reemplaza el panel derecho del SplitPane
        contenido = sc;
        if (split != null) {
            if (split.getItems().size() < 2) {
                split.getItems().add(contenido);
            } else {
                split.getItems().set(1, contenido);
            }
        } else {
            // fallback (no debería ocurrir si llamaste a crearUI() en start)

            sc.setFitToWidth(true);
            sc.setFitToHeight(true);
            if (split.getItems().size() < 2) split.getItems().add(sc);
            else split.getItems().set(1, sc);
            contenido = sc;
        }

       /* grid.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            int col = pickCol(new Point2D(e.getSceneX(), e.getSceneY()));
            if (col != hoverCol) {
                clearHighlight(hoverCol);
                hoverCol = col;
                applyHighlight(hoverCol);
            }
        });
        grid.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            clearHighlight(hoverCol);
            hoverCol = -1;
        });*/

    }

    // campo de la clase
    private StackPane hoverMesoCell;

    // helper
    private void clearMesoHover() {
        if (hoverMesoCell != null) {
            String base = (String) hoverMesoCell.getProperties().get("baseStyle");
            hoverMesoCell.setStyle(base);
            hoverMesoCell = null;
        }
    }
    private void showPrettyMesoHover(StackPane cell){
        clearPrettyMesoHover();

        // Cápsula
        Rectangle capsule = new Rectangle();
        capsule.arcWidthProperty().set(14);
        capsule.arcHeightProperty().set(14);
        capsule.setFill(Color.web("#42A5F533"));     // azul muy suave (alpha 0x33)
        capsule.setStroke(Color.web("#42A5F5"));
        capsule.setStrokeWidth(1.8);
        capsule.setEffect(new DropShadow(12, Color.web("#42A5F522")));

        // Se ajusta al tamaño real de la celda
        capsule.widthProperty().bind(cell.widthProperty().subtract(6));
        capsule.heightProperty().bind(cell.heightProperty().subtract(6));

        // Marquita superior
        Rectangle topBar = new Rectangle(20, 3, Color.web("#42A5F5"));
        topBar.setArcWidth(3); topBar.setArcHeight(3);
        StackPane.setAlignment(topBar, Pos.TOP_CENTER);
        StackPane.setMargin(topBar, new Insets(2,0,0,0));

        // Overlay (no debe capturar eventos)
        StackPane overlay = new StackPane(capsule, topBar);
        overlay.setMouseTransparent(true);
        overlay.setOpacity(0);

        cell.getChildren().add(overlay);
        hoverMesoCell = cell;

        FadeTransition ft = new FadeTransition(Duration.millis(140), overlay);
        ft.setToValue(1);
        ft.play();
    }

    private void clearPrettyMesoHover(){
        if (hoverMesoCell == null) return;
        // quita cualquier overlay que hayamos agregado a esa celda
        hoverMesoCell.getChildren().removeIf(n ->
                n instanceof StackPane sp &&
                        sp.getChildren().stream().anyMatch(c -> c instanceof Rectangle));
        hoverMesoCell = null;
    }


/*

    private int hoverCol = -1;
    private void applyHighlight(int col) {
        if (col < 1 || col > plan.get().semanas) return;
        for (Row r : Row.values()) {
            StackPane baseCell = getBaseCell(r, col);
            if (baseCell == null) continue;
            String bs = (String) baseCell.getProperties().get("baseStyle");
            // leve tinte azul
            baseCell.setStyle(bs + "; -fx-background-color: linear-gradient(to bottom, rgba(100,181,246,0.25), rgba(100,181,246,0.12));");
        }
    }
    private void clearHighlight(int col) {
        if (col < 1 || col > plan.get().semanas) return;
        for (Row r : Row.values()) {
            StackPane baseCell = getBaseCell(r, col);
            if (baseCell == null) continue;
            String bs = (String) baseCell.getProperties().get("baseStyle");
            if (bs != null) baseCell.setStyle(bs);
        }
    }
    private StackPane getBaseCell(Row row, int col) {
        for (Node n : grid.getChildren()) {
            Integer r = GridPane.getRowIndex(n);
            Integer c = GridPane.getColumnIndex(n);
            Integer cs = GridPane.getColumnSpan(n);
            if (r!=null && c!=null && r==row.ordinal() && c==col && n instanceof StackPane && (cs==null || cs==1)) {
                return (StackPane) n; // celda de fondo (no un span)
            }
        }
        return null;
    }*/


    private void abrirMenuMesocicloContextual(int col, Node owner, double sx, double sy) {
        var pg = plan.get();
        Mesociclo m = buscarMesocicloEnCol(col);

        ContextMenu cm = new ContextMenu();

        MenuItem nuevo = new MenuItem("Nuevo mesociclo aquí…");
        nuevo.setOnAction(e -> abrirDialogoMesociclo(col));
        cm.getItems().add(nuevo);

        if (m != null) {
            MenuItem editar = new MenuItem("Editar mesociclo");
            editar.setOnAction(e -> abrirDialogoMesociclo(m.startCol));

            MenuItem eliminar = new MenuItem("Eliminar mesociclo");
            eliminar.setOnAction(e -> {
                pg.mesociclosManuales.remove(m);
                pintarMesociclosManuales();
            });

            cm.getItems().addAll(editar, eliminar);
        }

        cm.show(owner, sx, sy);
    }
    private void abrirDialogoMesociclo(int colInicio) {
        var pg = plan.get();
        Mesociclo existente = buscarMesocicloEnCol(colInicio);

        // Si se edita, el "inicio" real del diálogo debe ser el del mesociclo encontrado
        int inicioDialog = (existente != null) ? existente.startCol : colInicio;

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(existente == null ? "Nuevo mesociclo" : "Editar mesociclo");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(8); gp.setPadding(new Insets(10));

        int maxDur = Math.max(1, pg.semanas - inicioDialog + 1);
        Spinner<Integer> spDur = spinner(1, maxDur, existente == null ? 4 : existente.len, 1);

        ComboBox<MesocicloTipo> cbTipo = new ComboBox<>();
        cbTipo.getItems().setAll(MesocicloTipo.values());
        cbTipo.setValue(existente == null ? MesocicloTipo.ENTRANTE : existente.tipo);

        gp.add(new Label("Columna inicio:"), 0, 0); gp.add(new Label(String.valueOf(inicioDialog)), 1, 0);
        gp.add(new Label("Duración (semanas):"), 0, 1); gp.add(spDur, 1, 1);
        gp.add(new Label("Tipo:"), 0, 2); gp.add(cbTipo, 1, 2);

        dlg.getDialogPane().setContent(gp);

        var res = dlg.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        int nuevoLen  = spDur.getValue();
        MesocicloTipo nuevoTipo = cbTipo.getValue();
        int nuevoIni  = inicioDialog;

        Mesociclo nuevo = new Mesociclo(nuevoIni, nuevoLen, nuevoTipo);

        // Inserta con confirmación si invade al siguiente
        insertarMesocicloConConfirmacion(nuevo, existente);
    }

/*    private void abrirDialogoMesociclo(int colInicio) {
        var pg = plan.get();

        // ¿Hay uno existente que abarque esta columna? -> precargar para editar
        Mesociclo existente = buscarMesocicloEnCol(colInicio);

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(existente == null ? "Nuevo mesociclo" : "Editar mesociclo");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(8); gp.setPadding(new Insets(10));

        int maxDur = Math.max(1, pg.semanas - colInicio + 1);
        Spinner<Integer> spDur = spinner(1, maxDur, existente == null ? 4 : existente.len, 1);

        ComboBox<MesocicloTipo> cbTipo = new ComboBox<>();
        cbTipo.getItems().setAll(MesocicloTipo.values());
        cbTipo.setValue(existente == null ? MesocicloTipo.ENTRANTE : existente.tipo);

        gp.add(new Label("Columna inicio:"), 0, 0); gp.add(new Label(String.valueOf(colInicio)), 1, 0);
        gp.add(new Label("Duración (semanas):"), 0, 1); gp.add(spDur, 1, 1);
        gp.add(new Label("Tipo:"), 0, 2); gp.add(cbTipo, 1, 2);

        dlg.getDialogPane().setContent(gp);

        var res = dlg.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        Mesociclo nuevo = new Mesociclo(colInicio, spDur.getValue(), cbTipo.getValue());

        // Elimina solapamientos con el nuevo/actualizado
        int start = nuevo.startCol, end = nuevo.startCol + nuevo.len - 1;
        pg.mesociclosManuales.removeIf(m -> overlaps(m.startCol, m.startCol + m.len - 1, start, end));

        // Si estaba editando, reemplaza, si no, añade
        pg.mesociclosManuales.add(nuevo);

        pintarMesociclosManuales();
    }*/


    private Mesociclo buscarMesocicloEnCol(int col) {
        for (Mesociclo m : plan.get().mesociclosManuales) {
            int s = m.startCol, e = m.startCol + m.len - 1;
            if (col >= s && col <= e) return m;
        }
        return null;
    }

    private boolean overlaps(int a1, int a2, int b1, int b2) {
        return a1 <= b2 && b1 <= a2;
    }
    private void pintarMesociclosManuales() {
        var pg = plan.get();

        // 1) Elimina SOLO los spans de mesociclo previamente pintados
        grid.getChildren().removeIf(n ->
                Objects.equals(GridPane.getRowIndex(n), Row.MESOCICLO.ordinal()) &&
                        (n.getUserData() instanceof CellTag ct) &&
                        ct.type == ZoneType.MESOCICLO
        );

        // 2) Repinta los mesociclos del modelo
        for (Mesociclo m : pg.mesociclosManuales) {
            if (m.len <= 0) continue;
            int start = Math.max(1, m.startCol);
            int end   = Math.min(pg.semanas, m.startCol + m.len - 1);
            if (end < start) continue;
            int len = end - start + 1;

            Color bg = MESO_BG.getOrDefault(m.tipo, Color.web("#E1BEE7"));
            String texto = m.tipo.etiqueta();
            spanMeso(Row.MESOCICLO, start, len, label(texto), bg, start, end);
        }
    }
    private static int end(Mesociclo m) { return m.startCol + m.len - 1; }
    private static Mesociclo clip(int s, int e, MesocicloTipo t) {
        return new Mesociclo(s, e - s + 1, t);
    }
    private void insertarMesocicloConConfirmacion(Mesociclo nuevo, Mesociclo aReemplazar) {
        // Lista de existentes (si estoy editando, sácalo para recalcular limpio)
        var lista = new ArrayList<>(plan.get().mesociclosManuales);
        if (aReemplazar != null) lista.remove(aReemplazar);
        lista.sort(Comparator.comparingInt(m -> m.startCol));

        // Limita el nuevo a [1..semanas]
        int ns = Math.max(1, nuevo.startCol);
        int ne = Math.min(plan.get().semanas, nuevo.startCol + nuevo.len - 1);
        nuevo.startCol = ns;
        nuevo.len = Math.max(1, ne - ns + 1);

        // Meso inmediatamente a la derecha (el “que sigue”)
        Mesociclo next = lista.stream()
                .filter(m -> m.startCol >= ns)
                .findFirst()
                .orElse(null);

        // ¿El nuevo invade/solapa al siguiente? (ej. nuevo 14–16 y next 16–20)
        if (next != null && ne >= next.startCol) {
            ButtonType BTN_ACORTAR_NUEVO  = new ButtonType("Acortar nuevo", ButtonBar.ButtonData.YES);
            ButtonType BTN_RECORTAR_EXIST = new ButtonType("Recortar existente", ButtonBar.ButtonData.NO);
            ButtonType BTN_CANCELAR       = ButtonType.CANCEL;

            Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                    "El nuevo ("+ns+"–"+ne+") invade al siguiente ("+next.startCol+"–"+end(next)+").\n\n" +
                            "¿Qué deseas hacer?",
                    BTN_ACORTAR_NUEVO, BTN_RECORTAR_EXIST, BTN_CANCELAR);
            a.setHeaderText("Resolver solape con el mesociclo siguiente");
            Optional<ButtonType> r = a.showAndWait();
            if (r.isEmpty() || r.get()==BTN_CANCELAR) return;

            if (r.get()==BTN_ACORTAR_NUEVO) {
                // Opción 1: adaptar el nuevo para que termine justo antes del siguiente
                int nuevoFin = next.startCol - 1;
                int nuevaLen = nuevoFin - ns + 1;
                if (nuevaLen <= 0) {
                    alert("No hay espacio: el siguiente comienza en la misma semana.\nCambia el inicio o edita/elimina el meso siguiente.");
                    return;
                }
                nuevo.len = nuevaLen;

            } else if (r.get()==BTN_RECORTAR_EXIST) {
                // Opción 2: recortar el existente por la izquierda
                int newStartNext = ne + 1;
                int newLenNext   = end(next) - newStartNext + 1;
                if (newLenNext <= 0) {
                    // Si queda cubierto por completo, elimínalo
                    plan.get().mesociclosManuales.remove(next);
                } else {
                    next.startCol = newStartNext;
                    next.len = newLenNext;
                }
            }
        }

        // Inserta/ajusta el resto sin destruir (recorta lo que haga falta a izquierda/derecha)
        insertarMesocicloNoDestructivo(nuevo, aReemplazar);
    }

    private void insertarMesocicloNoDestructivo(Mesociclo nuevo, Mesociclo aReemplazar) {
        var lista = new ArrayList<>(plan.get().mesociclosManuales);
        if (aReemplazar != null) lista.remove(aReemplazar);

        // Limita a rango válido
        int ns = Math.max(1, nuevo.startCol);
        int ne = Math.min(plan.get().semanas, nuevo.startCol + nuevo.len - 1);
        nuevo.startCol = ns;
        nuevo.len = Math.max(1, ne - ns + 1);

        List<Mesociclo> res = new ArrayList<>();
        boolean colocado = false;

        for (Mesociclo m : lista) {
            int ms = m.startCol, me = end(m);

            if (me < ns) {                     // viejo totalmente antes
                res.add(m);
                continue;
            }
            if (ms > ne) {                     // viejo totalmente después
                if (!colocado) { res.add(nuevo); colocado = true; }
                res.add(m);
                continue;
            }
            // HAY SOLAPE ⇒ recortar el viejo
            if (ms < ns) {                     // porción izquierda del viejo
                res.add(clip(ms, ns - 1, m.tipo));
            }
            if (!colocado) {                   // coloca el nuevo una sola vez
                res.add(nuevo);
                colocado = true;
            }
            if (me > ne) {                     // porción derecha del viejo
                res.add(clip(ne + 1, me, m.tipo));
            }
            // si el viejo queda cubierto, no se agrega
        }
        if (!colocado) res.add(nuevo);

        // Ordena y fusiona contiguos del mismo tipo
        res.sort(Comparator.comparingInt(a -> a.startCol));
        for (int i = 0; i < res.size() - 1; ) {
            Mesociclo a = res.get(i), b = res.get(i + 1);
            if (a.tipo == b.tipo && end(a) + 1 == b.startCol) {
                a.len += b.len;
                res.remove(i + 1);
            } else i++;
        }

        plan.get().mesociclosManuales = res;
        pintarMesociclosManuales();
    }


    private void spanMeso(Row row, int startCol, int spanCols, Node content, Color bg, int start, int end) {
        StackPane box = new StackPane(content);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(4,6,4,6));
        String base = toHex(bg);
        box.setStyle("-fx-background-color: " + base + "; -fx-border-color: derive(" + base + ", -20%); -fx-border-radius: 8; -fx-background-radius: 8;");
        box.setOnMouseEntered(e -> box.setStyle(
                box.getStyle() + "; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.20), 14, 0.2, 0, 2); -fx-translate-y: -1;"));
        box.setOnMouseExited(e -> box.setStyle(
                box.getStyle().replaceAll("; -fx-effect:[^;]*", "").replaceAll("; -fx-translate-y:[^;]*", "")));

        GridPane.setRowIndex(box, row.ordinal());
        GridPane.setColumnIndex(box, startCol);
        GridPane.setColumnSpan(box, spanCols);

        box.setUserData(new CellTag(ZoneType.MESOCICLO, start, end));

        // 👇 clave: deja pasar eventos a la celda de fondo
      /*  box.setMouseTransparent(true);

        grid.getChildren().add(box);*/
        box.setCursor(javafx.scene.Cursor.HAND);
        Tooltip.install(box, new Tooltip("Editar mesociclo (" + start + "–" + end + ")"));
        box.setOnMouseClicked(ev -> {
            if (ev.getButton() == MouseButton.PRIMARY) abrirDialogoMesociclo(start);
            else if (ev.getButton() == MouseButton.SECONDARY) abrirMenuMesocicloContextual(start, box, ev.getScreenX(), ev.getScreenY());
        });

        grid.getChildren().add(box);
        box.toFront();
    }


    // Formatos
    private static final DateTimeFormatter DF_D    = DateTimeFormatter.ofPattern("dd");
    private static final DateTimeFormatter DF_DDMM = DateTimeFormatter.ofPattern("dd/MM");

    // Devuelve "dd / dd" si es el mismo mes, si no "dd/MM / dd/MM".
// Ajusta el último día para no pasar de pg.fin si el plan termina antes.
    private String etiquetaRangoSemana(LocalDate ini, LocalDate finPlan) {
        LocalDate fin = ini.plusDays(6);
        if (finPlan != null && fin.isAfter(finPlan)) fin = finPlan;

        if (ini.getMonth() == fin.getMonth()) {
            // compacto: "15 / 21" (mismo mes)
            return ini.format(DF_D) + " / " + fin.format(DF_DDMM);
        } else {
            // cruza de mes: "29/09 / 05/10"
            return ini.format(DF_DDMM) + " / " + fin.format(DF_DDMM);
        }
    }
    // Devuelve "dd / dd" usando LUNES como inicio y DOMINGO como fin
    private String etiquetaLunesDomingo(LocalDate anyDayInWeek) {
        LocalDate lunes   = anyDayInWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate domingo = anyDayInWeek.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return lunes.format(DF_D) + " / " + domingo.format(DF_D);
    }

    private void pintarMinutosFila() {
        var pg = plan.get();
        for (int col = 1; col <= pg.semanas; col++) {
            clearCell(Row.MIN, col);
            int base = pg.sesionesSem * 90;
            int tot  = pg.minutosTotal.getOrDefault(col, base);
            int pf   = pg.minutosPF.getOrDefault(col, -1);
            int tt   = pg.minutosTT.getOrDefault(col, -1);
            int pct  = pg.microCargaPct.getOrDefault(col, -1);


            addCenterText(Row.MIN, col, String.valueOf(tot));

            var cell = getCell(Row.MIN, col);
            String tip = "Total: " + tot + " min";
            if (pf >= 0 && tt >= 0) tip += "  |  P.F.: " + pf + "  |  Téc-Tác: " + tt;
            if (pct >= 0) tip += "  |  %carga: " + pct;
            Tooltip.install(cell, new Tooltip(tip));
        }
    }

    // Campos:

    private final BooleanProperty collapsed = new SimpleBooleanProperty(false);

    private Node crearUI() {
        // panelIzquierdo = construirPanelIzquierdo();  // YA lo tienes
        // contenido      = construirContenido();       // YA lo tienes

        split = new SplitPane(panelIzquierdo, contenido);
        split.setDividerPositions(lastDividerPos.get());
        split.setFocusTraversable(false);

        // Botón para colapsar/expandir
        ToggleButton btn = new ToggleButton("⮜"); // cambia a "⮞" cuando está colapsado
        btn.selectedProperty().bindBidirectional(collapsed);
        btn.selectedProperty().addListener((obs, was, is) -> btn.setText(is ? "⮞" : "⮜"));

        btn.setOnAction(e -> toggleSidebar());

        // Acceso rápido con teclado (Ctrl+B)
        split.addEventFilter(KeyEvent.KEY_PRESSED, ke -> {
            if (new KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN).match(ke)) {
                collapsed.set(!collapsed.get());
                ke.consume();
            }
        });

        // Overlay del botón en la esquina superior izquierda
        StackPane overlay = new StackPane(split);
        StackPane.setAlignment(btn, Pos.TOP_LEFT);
        overlay.getChildren().add(btn);
        overlay.setPadding(new Insets(6)); // separa el botón del borde

        return overlay;
    }

    private void toggleSidebar() {
        // Guardar/restaurar posición del divisor
        final SplitPane.Divider div = split.getDividers().get(0);

        if (collapsed.get()) { // estamos colapsando
            lastDividerPos.set(div.getPosition());
            animateDivider(div.positionProperty(), 0.0);
            // (opcional) ocultar el nodo para que no “robe” eventos
            panelIzquierdo.setMouseTransparent(true);
        } else { // estamos expandiendo
            double destino = lastDividerPos.get() <= 0.05 ? 0.25 : lastDividerPos.get();
            animateDivider(div.positionProperty(), destino);
            panelIzquierdo.setMouseTransparent(false);
        }
    }

    private void animateDivider(DoubleProperty prop, double target) {
        Timeline t = new Timeline(
                new KeyFrame(Duration.millis(220),
                        new KeyValue(prop, target, Interpolator.EASE_BOTH))
        );
        t.play();
    }



    private void abrirEditorTiemposRango() {
        var pg = plan.get();

        // 1) Pide rango
        TextInputDialog rangoDlg = new TextInputDialog("1-" + pg.semanas);
        rangoDlg.setTitle("Distribuir tiempos por bloque");
        rangoDlg.setHeaderText("Rango de semanas (ej.: 3-6)");
        rangoDlg.setContentText("Rango:");
        var r = rangoDlg.showAndWait();
        if (r.isEmpty()) return;

        int a, b;
        try {
            String s = r.get().trim();
            int k = s.indexOf('-');
            a = Integer.parseInt(s.substring(0, k).trim());
            b = Integer.parseInt(s.substring(k+1).trim());
            if (a>b) {int t=a;a=b;b=t;}
            a = Math.max(1, a); b = Math.min(pg.semanas, b);
        } catch (Exception ex) {
            alert("Formato inválido. Usa por ejemplo: 4-9");
            return;
        }

        // 2) Crea diálogo con parámetros del bloque
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Distribución de tiempos por bloque");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(8); gp.setPadding(new Insets(10));

        // Minutos base por micro en este bloque
        Spinner<Integer> spMinPorMicro = spinner(30, 1200, 600, 10);
        // % P.F. del bloque (el resto = Téc-Tác)
        Spinner<Integer> spPF = spinner(0, 100, (cbTipo.getValue()==TipoPeriodizacion.ATR? 60 : 50), 1);

        gp.add(new Label("Semanas: " + a + "–" + b), 0, 0, 2, 1);
        gp.add(new Label("Minutos por micro (total)"), 0, 1); gp.add(spMinPorMicro, 1, 1);
        gp.add(new Label("% Preparación Física (bloque)"), 0, 2); gp.add(spPF, 1, 2);

        // 3) Tabla simple de % de carga por semana del rango
        VBox lista = new VBox(6);
        Map<Integer, Spinner<Integer>> mapPct = new LinkedHashMap<>();
        for (int w=a, row=0; w<=b; w++, row++) {
            int def = Optional.ofNullable(pg.microCargaPct.get(w)).orElse(60); // default 60
            var sp = spinner(0, 120, def, 1);
            HBox fila = new HBox(10, new Label("Semana " + w + " (% carga)"), sp);
            fila.setAlignment(Pos.CENTER_LEFT);
            mapPct.put(w, sp);
            lista.getChildren().add(fila);
        }
        TitledPane tp = new TitledPane("Porcentaje de carga por micro (bloque)", lista);
        tp.setExpanded(true);
        gp.add(tp, 0, 3, 2, 1);

        // Nota metodológica
        Label nota = new Label("Método: CI = (MinTotBloque × %PF) / Σ%micro  → MinPF_micro = CI × %micro;  MinTT_micro = MinPorMicro − MinPF_micro");
        nota.setWrapText(true);
        gp.add(nota, 0, 4, 2, 1);

        dlg.getDialogPane().setContent(gp);

        var res = dlg.showAndWait();
        if (res.isEmpty() || res.get()!=ButtonType.OK) return;

        // 4) Cálculo según metodología (con posibilidad de ajuste manual posterior)
        int minMicro = spMinPorMicro.getValue();
        int n = (b - a + 1);
        int minTotBloque = minMicro * n;
        int pctPF = spPF.getValue();

        int sumaPct = mapPct.values().stream().mapToInt(Spinner::getValue).sum();
        if (sumaPct <= 0) {
            alert("La suma de % de carga debe ser > 0.");
            return;
        }
        // CI (coeficiente indicativo) con redondeo a 2 decimales
        double CI = (minTotBloque * (pctPF / 100.0)) / (double) sumaPct;

        // 5) Guarda resultados por semana
        for (var en : mapPct.entrySet()) {
            int w = en.getKey();
            int p = en.getValue().getValue();
            int minPF = (int) Math.round(CI * p);
            int minTT = Math.max(0, minMicro - minPF);

            plan.get().microCargaPct.put(w, p);
            plan.get().minutosTotal.put(w, minMicro);
            plan.get().minutosPF.put(w, minPF);
            plan.get().minutosTT.put(w, minTT);
        }
        // repinta fila MIN y tooltips
        pintarMinutosFila();
    }

    private void editarTiempoSemana(int col) {
        var pg = plan.get();
        int curTotal = pg.minutosTotal.getOrDefault(col, pg.sesionesSem * 90);
        int curPF    = pg.minutosPF.getOrDefault(col, -1);
        int curTT    = pg.minutosTT.getOrDefault(col, -1);
        int curPct   = pg.microCargaPct.getOrDefault(col, 60);

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Editar tiempos – semana " + col);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(8); gp.setPadding(new Insets(10));

        Spinner<Integer> spTot = spinner(10, 1800, curTotal, 10);
        Spinner<Integer> spPct = spinner(0, 120, curPct, 1);
        Spinner<Integer> spPF  = spinner(0, 1800, curPF < 0 ? (int)Math.round(curTotal*0.5) : curPF, 5);

        CheckBox cbAutoTT = new CheckBox("TT = Total − PF (automático)");
        cbAutoTT.setSelected(true);
        Spinner<Integer> spTT  = spinner(0, 1800, curTT < 0 ? (curTotal - spPF.getValue()) : curTT, 5);
        spTT.setDisable(true);

        spPF.valueProperty().addListener((o,ov,nv)-> { if (cbAutoTT.isSelected()) spTT.getValueFactory().setValue(Math.max(0, spTot.getValue() - nv)); });
        spTot.valueProperty().addListener((o,ov,nv)-> { if (cbAutoTT.isSelected()) spTT.getValueFactory().setValue(Math.max(0, nv - spPF.getValue())); });
        cbAutoTT.selectedProperty().addListener((o,ov,nv)-> spTT.setDisable(nv));

        gp.add(new Label("Total (min)"), 0, 0); gp.add(spTot, 1, 0);
        gp.add(new Label("% carga (micro)"), 0, 1); gp.add(spPct, 1, 1);
        gp.add(new Label("P. Física (min)"), 0, 2); gp.add(spPF, 1, 2);
        gp.add(cbAutoTT, 0, 3, 2, 1);
        gp.add(new Label("Téc-Táctica (min)"), 0, 4); gp.add(spTT, 1, 4);

        dlg.getDialogPane().setContent(gp);
        var res = dlg.showAndWait();
        if (res.isEmpty() || res.get()!=ButtonType.OK) return;

        int tot = spTot.getValue();
        int pf  = spPF.getValue();
        int tt  = cbAutoTT.isSelected()? Math.max(0, tot - pf) : spTT.getValue();

        pg.minutosTotal.put(col, tot);
        pg.minutosPF.put(col, pf);
        pg.minutosTT.put(col, tt);
        pg.microCargaPct.put(col, spPct.getValue());

        pintarMinutosFila();
    }

    private void limpiarTiempoSemana(int col) {
        var pg = plan.get();
        pg.minutosTotal.remove(col);
        pg.minutosPF.remove(col);
        pg.minutosTT.remove(col);
        pg.microCargaPct.remove(col);
        pintarMinutosFila();
    }

    private void limpiarTiempoRangoDialog(int colBase) {
        var pg = plan.get();
        TextInputDialog rango = new TextInputDialog(colBase + "-" + colBase);
        rango.setTitle("Limpiar tiempos por rango");
        rango.setHeaderText("Escribe el rango (ej.: 5-10)");
        rango.setContentText("Rango:");
        var r = rango.showAndWait();
        if (r.isEmpty()) return;
        try {
            String s = r.get().trim();
            int k = s.indexOf('-');
            int a = Integer.parseInt(s.substring(0,k).trim());
            int b = Integer.parseInt(s.substring(k+1).trim());
            if (a>b) {int t=a;a=b;b=t;}
            a = Math.max(1,a); b = Math.min(pg.semanas,b);
            for (int w=a; w<=b; w++) {
                pg.minutosTotal.remove(w);
                pg.minutosPF.remove(w);
                pg.minutosTT.remove(w);
                pg.microCargaPct.remove(w);
            }
            pintarMinutosFila();
        } catch (Exception ex) {
            alert("Formato inválido. Ej.: 4-9");
        }
    }

    private void asignarMicro(int col, MicrocicloTipo tipo) {
        var pg = plan.get();
        if (col < 1 || col > pg.semanas) return;
        pg.microAsignaciones.put(col, tipo);
        plan.get().microCargaPct.putIfAbsent(col, defaultPctFor(tipo));
        pintarMicroFila();
    }

    private void asignarMicroRangoDialog(int colBase) {
        var pg = plan.get();
        TextInputDialog dlg = new TextInputDialog(colBase + "-" + colBase);
        dlg.setTitle("Asignar microciclo a rango");
        dlg.setHeaderText("Escribe el rango de semanas (ej.: 3-8)");
        dlg.setContentText("Rango:");
        Optional<String> res = dlg.showAndWait();
        if (res.isEmpty()) return;

        String s = res.get().trim();
        int guion = s.indexOf('-');
        if (guion <= 0) {
            alert("Formato inválido. Usa: 3-8");
            return;
        }
        try {
            int a = Integer.parseInt(s.substring(0, guion).trim());
            int b = Integer.parseInt(s.substring(guion + 1).trim());
            if (a > b) { int tmp = a; a = b; b = tmp; }
            ChoiceDialog<MicrocicloTipo> ch = new ChoiceDialog<>(MicrocicloTipo.ORDINARIO, MicrocicloTipo.values());
            ch.setTitle("Tipo de microciclo");
            ch.setHeaderText("Selecciona el tipo para semanas " + a + " a " + b);
            ch.setContentText("Tipo:");
            Optional<MicrocicloTipo> tipo = ch.showAndWait();
            if (tipo.isEmpty()) return;
            MicrocicloTipo t = tipo.get();
            for (int c = Math.max(1, a); c <= Math.min(pg.semanas, b); c++) {
                pg.microAsignaciones.put(c, t);
            }
            pintarMicroFila();
        } catch (Exception ex) {
            alert("No se pudo interpretar el rango. Ejemplo válido: 4-9");
        }
    }


/*    private void pintarMicroFila() {
        var pg = plan.get();
        for (int col = 1; col <= pg.semanas; col++) {
            clearCell(Row.MICRO, col);
            MicrocicloTipo t = pg.microAsignaciones.get(col);
            if (t == null) {
                addChip(Row.MICRO, col, "μ", Color.web("#f0f0f0"), "#9E9E9E");
            } else {
                addChip(Row.MICRO, col, t.getCodigo(),
                        Color.web(MICRO_BG.get(t)),
                        MICRO_BORDER.get(t));
                var cell = getCell(Row.MICRO, col);
                Tooltip.install(cell, new Tooltip(t.getCodigo() + " (sem " + col + ")"));
            }
        }
    }*/
private void pintarMicroFila() {
    var pg = plan.get();

    for (int col = 1; col <= pg.semanas; col++) {
        // limpia overlays de esa celda (pero NO el StackPane base)
        clearCell(Row.MICRO, col);

        StackPane cell = getCell(Row.MICRO, col);
        cell.setPadding(Insets.EMPTY);
        cell.setCursor(javafx.scene.Cursor.HAND);

        MicrocicloTipo t = pg.microAsignaciones.get(col);
        Integer pct = plan.get().microCargaPct.get(col); // puede ser null

        // === Fondo que ocupa TODA la celda ===
        Region bg = new Region();
        // deja un margen visual de 2px alrededor
        bg.prefWidthProperty().bind(cell.widthProperty().subtract(4));
        bg.prefHeightProperty().bind(cell.heightProperty().subtract(4));
        bg.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        String bgHex = (t == null) ? "#f0f0f0" : MICRO_BG.getOrDefault(t, "#f0f0f0");
        String bdHex = (t == null) ? "#9E9E9E" : MICRO_BORDER.getOrDefault(t, "#9E9E9E");

        bg.setStyle(
                "-fx-background-color:" + bgHex + ";" +
                        "-fx-background-radius:8;" +
                        "-fx-border-color:" + bdHex + ";" +
                        "-fx-border-radius:8;"
        );

        // === Badge con la letra/código del micro ===
        String code = (t == null) ? "μ" : t.getCodigo();   // usas el mismo getCodigo() que ya tienes
        Label badge = new Label(code);
        badge.setStyle("""
            -fx-font-size: 11px;
            -fx-font-weight: bold;
            -fx-background-color: white;
            -fx-text-fill: #263238;
            -fx-padding: 2 6 2 6;
            -fx-background-radius: 999;
            -fx-border-radius: 999;
            -fx-border-color: rgba(0,0,0,0.18);
        """);
        StackPane.setAlignment(badge, Pos.TOP_CENTER);
        StackPane.setMargin(badge, new Insets(3,0,0,0));

        // tooltip: tipo + % (sin pintarlo en la celda)
        String tip = (t == null ? "Sin asignar" : t.getCodigo()) + (pct != null ? " · Carga " + pct + "%" : "");
        Tooltip.install(cell, new Tooltip(tip));

        // clic para editar micro (si ya tienes tu diálogo de micro, llámalo aquí)
        final int c = col;
        cell.setOnMouseClicked(ev -> {
            if (ev.getButton() == MouseButton.PRIMARY) {
                abrirDialogoMicro(mondayOfCol(c));
            }
        });

        // monta todo
        cell.getChildren().addAll(bg, badge);
    }
}


    private void clearCell(Row row, int col) {
        // elimina contenidos “flotantes” (chips/labels) pero preserva el StackPane de fondo
        grid.getChildren().removeIf(n ->
                Objects.equals(GridPane.getRowIndex(n), row.ordinal())
                        && Objects.equals(GridPane.getColumnIndex(n), col)
                        && !(n instanceof StackPane));
    }


    private int pickCol(Point2D scenePoint) {
        for (var node : grid.getChildren()) {
            Integer col = GridPane.getColumnIndex(node);
            if (col == null) continue;
            if (node.localToScene(node.getBoundsInLocal()).contains(scenePoint)) return col;
        }
        return -1;
    }

    private static String nombreFase(ATRFase f) {
        return switch (f) {
            case ACUMULACION -> "Acumulación";
            case TRANSFORMACION -> "Transformación";
            case REALIZACION -> "Realización";
        };
    }
    private static String siglaFase(ATRFase f) {
        return switch (f) {
            case ACUMULACION -> "A";
            case TRANSFORMACION -> "T";
            case REALIZACION -> "R";
        };
    }

    private void agruparMesesComoSpans(List<LocalDate> semanaInicios) {
        if (semanaInicios.isEmpty()) return;

        int startCol = 1;
        Locale esMX = new Locale("es", "MX");
        Month mActual = mesDominante(semanaInicios.get(0));

        for (int i = 1, n = semanaInicios.size(); i <= n; i++) {
            boolean finDeBloque = (i == n) || (mesDominante(semanaInicios.get(i)) != mActual);
            if (finDeBloque) {
                int len = i - (startCol - 1);
                String etiqueta = capitalize(mActual.getDisplayName(TextStyle.FULL_STANDALONE, esMX));
                span(Row.MES, startCol, len, label(etiqueta), Color.web("#EEEEEE"));

                if (i < n) {
                    startCol = i + 1;
                    mActual = mesDominante(semanaInicios.get(i));
                }
            }
        }
    }


    private int pintarEtapas(EtapaTipo a, EtapaTipo b, Map<EtapaTipo, Color> colEtapa,
                             List<int[]> mesociclos, int colBase, Map<EtapaTipo, Integer> m) {

        int la = m.getOrDefault(a, 0);
        int lb = m.getOrDefault(b, 0);

        if (la > 0) {
            span(Row.ETAPA, colBase, la, label(etq(a)), colEtapa.get(a));
            mesociclos.add(new int[]{colBase, colBase + la - 1});
            colBase += la;
        }

        if (lb > 0) {
            span(Row.ETAPA, colBase, lb, label(etq(b)), colEtapa.get(b));
            mesociclos.add(new int[]{colBase, colBase + lb - 1});
            colBase += lb;
        }

        return colBase;
    }

    private String etq(EtapaTipo e) {
        return e.name();

    }

    private void pintarVolumenIntensidad(PlanGrafico pg) {
        // Interpolación lineal de vol/int por semana
        for (int i = 0; i < pg.semanas; i++) {
            double t = (pg.semanas == 1) ? 0 : (double) i / (pg.semanas - 1);
            int vol = (int) Math.round(lerp(pg.volIni, pg.volFin, t));
            int in  = (int) Math.round(lerp(pg.intIni, pg.intFin, t));

            // Contenedor por semana
            StackPane cell = getCell(Row.VOL_INT, i+1);

            // Barra de volumen (baja)
            Rectangle rVol = new Rectangle(COL_SEMANA_ANCHO - 10, Math.max(6, vol * 0.22));
            rVol.setArcWidth(8); rVol.setArcHeight(8);
            rVol.setFill(Color.web("#B2DFDB"));

            // Barra de intensidad (alta), encima
            Rectangle rInt = new Rectangle(COL_SEMANA_ANCHO - 18, Math.max(6, in * 0.22));
            rInt.setArcWidth(8); rInt.setArcHeight(8);
            rInt.setFill(Color.web("#FFCCBC"));

            VBox bars = new VBox(2,
                    new StackPane(rInt) {{ setAlignment(Pos.BOTTOM_CENTER); setPrefHeight(32); }},
                    new StackPane(rVol) {{ setAlignment(Pos.TOP_CENTER); setPrefHeight(32); }}
            );
            bars.setAlignment(Pos.CENTER);
            cell.getChildren().add(bars);
            Tooltip.install(cell, new Tooltip("Vol " + vol + "%, Int " + in + "%"));
        }
    }

    // ====== Helpers Grid ======
    private void addTitulo(GridPane g, int row, String text) {
        var n = new Label(text);
        n.setFont(Font.font(13));
        n.setStyle("-fx-font-weight: bold;");
        n.setAlignment(Pos.CENTER_RIGHT);
        n.setPadding(new Insets(0, 8, 0, 0));
        n.setMinHeight(28);
        GridPane.setRowIndex(n, row);
        GridPane.setColumnIndex(n, 0);
        g.getChildren().add(n);
    }

    private void addCenterText(Row row, int col, String text) {
        var n = new Label(text);
        n.setFont(Font.font(12));
        n.setAlignment(Pos.CENTER);
        n.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        GridPane.setRowIndex(n, row.ordinal());
        GridPane.setColumnIndex(n, col);
        grid.getChildren().add(n);
    }

    private void addChip(Row row, int col, String text, Color bg, String borderHex) {
        var chip = new Label(text);
        chip.setPadding(new Insets(2,6,2,6));
        chip.setStyle("-fx-background-color: " + toHex(bg) + "; -fx-border-color: " + borderHex + "; -fx-border-radius: 6; -fx-background-radius: 6;");
        GridPane.setRowIndex(chip, row.ordinal());
        GridPane.setColumnIndex(chip, col);
        grid.getChildren().add(chip);
    }

    private void span(Row row, int startCol, int spanCols, Node content, Color bg) {
        StackPane box = new StackPane(content);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(4,6,4,6));
        String base = toHex(bg);
        box.setStyle("-fx-background-color: " + base + "; -fx-border-color: derive(" + base + ", -20%); -fx-border-radius: 8; -fx-background-radius: 8;");
        GridPane.setRowIndex(box, row.ordinal());
        GridPane.setColumnIndex(box, startCol);
        GridPane.setColumnSpan(box, spanCols);
        grid.getChildren().add(box);
    }

    private static Label label(String s) {
        var l = new Label(s);
        l.setFont(Font.font(12));
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: #263238;");
        return l;
    }

    private StackPane getCell(Row row, int col) {
        for (var n : grid.getChildren()) {
            Integer r = GridPane.getRowIndex(n);
            Integer c = GridPane.getColumnIndex(n);
            if (r != null && c != null && r == row.ordinal() && c == col && n instanceof StackPane sp) return sp;
        }
        // si no estaba, creamos
        StackPane sp = new StackPane();
        sp.setMinSize(COL_SEMANA_ANCHO, 28);
        GridPane.setRowIndex(sp, row.ordinal());
        GridPane.setColumnIndex(sp, col);
        grid.getChildren().add(sp);
        return sp;
    }

    // ====== Utilidades de distribución por % (con ajuste de residuo) ======
    private <K> Map<K, Integer> distribuirPorcentajes(int totalUnidades, Map<K, Integer> porcentajes) {
        Map<K, Integer> res = new LinkedHashMap<>();
        int asignado = 0;
        K ultimo = null;
        for (var e : porcentajes.entrySet()) {
            ultimo = e.getKey();
            int v = (int) Math.round(totalUnidades * e.getValue() / 100.0);
            res.put(e.getKey(), v);
            asignado += v;
        }
        // Ajusta residuo sumando/restando al último
        int diff = totalUnidades - asignado;
        if (ultimo != null && diff != 0) {
            res.put(ultimo, res.get(ultimo) + diff);
        }
        return res;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int)(c.getRed()*255),
                (int)(c.getGreen()*255),
                (int)(c.getBlue()*255));
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }

    private static void alert(String s) {
        new Alert(Alert.AlertType.INFORMATION, s, ButtonType.OK).showAndWait();
    }

    // ====== Guardar/Cargar JSON (rápido para iterar) ======
    private void guardarJSON(Stage st) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar plan (JSON)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File f = fc.showSaveDialog(st);
        if (f == null) return;
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            Gson g = new GsonBuilder().setPrettyPrinting().create();
            g.toJson(plan.get(), w);
        } catch (Exception ex) {
            alert("Error al guardar: " + ex.getMessage());
        }
    }

    private void cargarJSON(Stage st) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Cargar plan (JSON)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File f = fc.showOpenDialog(st);
        if (f == null) return;
        try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
            Gson g = new Gson();
            PlanGrafico pg = g.fromJson(r, PlanGrafico.class);
            plan.set(pg);
            // reflect en UI
            dpInicio.setValue(pg.inicio);
            spSemanas.getValueFactory().setValue(pg.semanas);
            spSes.getValueFactory().setValue(pg.sesionesSem);
            spPrep.getValueFactory().setValue(pg.pctPrep);
            spComp.getValueFactory().setValue(pg.pctComp);
            spTrans.getValueFactory().setValue(pg.pctTrans);
            spPrepGen.getValueFactory().setValue(pg.pctPrepGeneral);
            spPrepEsp.getValueFactory().setValue(pg.pctPrepEspecial);
            spCompPre.getValueFactory().setValue(pg.pctCompPrecomp);
            spCompComp.getValueFactory().setValue(pg.pctCompComp);
            spTransRec.getValueFactory().setValue(pg.pctTransRecup);
            spVolIni.getValueFactory().setValue(pg.volIni);
            spVolFin.getValueFactory().setValue(pg.volFin);
            spIntIni.getValueFactory().setValue(pg.intIni);
            spIntFin.getValueFactory().setValue(pg.intFin);
            dibujarPlan();
        } catch (Exception ex) {
            alert("Error al cargar: " + ex.getMessage());
        }
    }

    private static PlanGrafico defaultPlan() {
      /*  PlanGrafico p = new PlanGrafico();
        p.inicio = LocalDate.now();
        p.semanas = 24;
        p.fin = p.inicio.plusWeeks(p.semanas - 1);   // <—— NUEVO
        p.sesionesSem = 4;
        p.pctPrep = 45; p.pctComp = 45; p.pctTrans = 10;
        p.pctPrepGeneral = 70; p.pctPrepEspecial = 30;
        p.pctCompPrecomp = 40; p.pctCompComp = 60;
        p.pctTransRecup = 60;
        return p;*/



        PlanGrafico p = new PlanGrafico();
        p.tipo = TipoPeriodizacion.CLASICA;       // valor por defecto
        p.inicio = LocalDate.now();
        p.semanas = 24;
        p.fin = p.inicio.plusWeeks(p.semanas - 1);
        p.sesionesSem = 4;

        p.minutosTotal = new HashMap<>();
        p.minutosPF    = new HashMap<>();
        p.minutosTT    = new HashMap<>();
        p.microCargaPct= new HashMap<>();


        // Clásica
        p.pctPrep = 45; p.pctComp = 45; p.pctTrans = 10;
        p.pctPrepGeneral = 70; p.pctPrepEspecial = 30;
        p.pctCompPrecomp = 40; p.pctCompComp = 60;
        p.pctTransRecup = 60;

        // ATR
        p.pctATR_A = 50; p.pctATR_T = 30; p.pctATR_R = 20;

        return p;
    }

    public static void main(String[] args) {
        launch(args);
    }
}

