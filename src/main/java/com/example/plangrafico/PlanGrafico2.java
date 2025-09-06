package com.example.plangrafico;



import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.application.Application;
import javafx.beans.property.*;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class PlanGrafico2 extends Application {

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

    // Filas del grid
    private enum Row {
        SEM, INICIO, MES, PERIODO, ETAPA, MESOCICLO, MICRO, VOL_INT, CONTROLES, COMP, SES, MIN
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

        public Map<Integer, Integer> diasTotal = new HashMap<>();

        LocalDate inicio;
        LocalDate fin;     // <—— NUEVO

        Map<Integer, MicrocicloTipo> microAsignaciones = new HashMap<>();

        int semanas;
        int sesionesSem;
        // dentro de PlanGrafico
        public Map<Integer, List<LocalDate>> sesionesPorSemana = new HashMap<>();
        // Porcentajes por periodo (suman 100)
        int pctPrep, pctComp, pctTrans;

        // Porcentajes por etapa dentro de cada periodo (suman 100 en su periodo)
        int pctPrepGeneral, pctPrepEspecial;
        int pctCompPrecomp, pctCompComp;
        int pctTransRecup; // el resto en transitorio lo dejamos como descanso activo (implícito)

        /*// Tendencia vol/int en % (0-100)
        int volIni = 100, volFin = 40;
        int intIni = 40, intFin = 95;
*/
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

    }

    // Estado UI
    private final ObjectProperty<PlanGrafico> plan = new SimpleObjectProperty<>(defaultPlan());

    // UI nodes
    private GridPane grid;
    private BorderPane root;

    // Inputs
  /*  private DatePicker dpInicio;
    private Spinner<Integer> spSemanas, spSes;*/
    private Spinner<Integer> spPrep, spComp, spTrans;
    private Spinner<Integer> spPrepGen, spPrepEsp, spCompPre, spCompComp, spTransRec;
    private Spinner<Integer> spVolIni, spVolFin, spIntIni, spIntFin;

    @Override
    public void start(Stage stage) {
        root = new BorderPane();
        root.setLeft(panelInputs(stage));
        root.setCenter(new StackPane(new Label("Genera el plan para ver el gráfico →")) {{
            setPadding(new Insets(24));
        }});

        Scene scene = new Scene(root, 1280, 720);
        stage.setTitle("Plan Gráfico – MVP");
        stage.setScene(scene);
        stage.show();
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
            setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
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
                cell.setStyle("-fx-background-color: " + base + "; -fx-border-color: #e6e6e6; -fx-border-width: 0 1 1 0;");
                GridPane.setRowIndex(cell, row.ordinal());
                GridPane.setColumnIndex(cell, col);
                grid.getChildren().add(cell);
            }
        }

        // Columna títulos
        int r = 0;
        addTitulo(grid, r++, "SEM");
        addTitulo(grid, r++, "INICIO");
        addTitulo(grid, r++, "MES");
        addTitulo(grid, r++, "PERIODO");
        addTitulo(grid, r++, "ETAPA");
        addTitulo(grid, r++, "MESOCICLO");
        addTitulo(grid, r++, "MICRO");
        addTitulo(grid, r++, "VOL / INT");
        addTitulo(grid, r++, "CONTROLES");
        addTitulo(grid, r++, "COMP");
        addTitulo(grid, r++, "SES");
        addTitulo(grid, r++, "MIN");

        // SEM + INICIO
        LocalDate start = pg.inicio;
        List<LocalDate> semanaInicios = new ArrayList<>();
        for (int i = 0; i < pg.semanas; i++) {
            LocalDate s = start.plusWeeks(i);
            semanaInicios.add(s);
            addCenterText(Row.SEM, i+1, String.valueOf(i+1));
            addCenterText(Row.INICIO, i+1, s.format(DF_DD_MM));
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
        for (int[] mc : mesociclos) {
            int startCol = mc[0];
            int endCol = mc[1];
            int len = endCol - startCol + 1;

            // Partimos en trozos de 4–6 con preferencia 4 o 5
            int idx = startCol;
            while (idx <= endCol) {
                int remain = endCol - idx + 1;
                int take = (remain >= 6) ? 5 : Math.min(remain, Math.max(4, remain));
                span(Row.MESOCICLO, idx, take, label("Meso"), Color.web("#E1BEE7"));
                idx += take;
            }
        }

       /* // MICRO: etiqueta por semana, sin solapar (una celda = un micro)
        for (int col = 1; col <= pg.semanas; col++) {
            addChip(Row.MICRO, col, "μ", Color.web("#f0f0f0"), "#9E9E9E");
        }*/
        pintarMicroFila();


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
        root.setCenter(new ScrollPane(grid) {{
            setFitToWidth(true);
            setFitToHeight(true);
        }});
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


    private void pintarMicroFila() {
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
        Month mActual = semanaInicios.get(0).getMonth();
        for (int i = 1; i < semanaInicios.size(); i++) {
            Month m = semanaInicios.get(i).getMonth();
            if (m != mActual) {
                int len = i - (startCol - 1);
                String etiqueta = mActual.getDisplayName(TextStyle.FULL_STANDALONE, new Locale("es", "MX"));
                span(Row.MES, startCol, len, label(capitalize(etiqueta)), Color.web("#EEEEEE"));
                startCol = i + 1;
                mActual = m;
            }
        }
        // último
      /*  int len = semanaInicios.size() - (startCol - 1);
        String etiqueta = mActual.getDisplayName(TextStyle.FULL_STANDALONE, new Locale("es", "MX"));
        span(Row.MES, startCol, len, label(capitalize(etiqueta)), Color.web("#EEEEEE"));*/
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
        return switch (e) {
            case GENERAL -> "General";
            case ESPECIAL -> "Especial";
            case PRECOMPETITIVA -> "Precomp.";
            case COMPETITIVA -> "Competitiva";
            case RECUPERACION -> "Recup.";
        };
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

