package com.example.plangrafico.ui;


import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.IntStream;

/**
 * Editor visual del Plan Gráfico (autónomo, sin FXML).
 * Integra: selector de fechas, tipo de periodización, CRUD de mesociclos,
 * grilla de meses/mesociclos/semanas, fila Intensidad/Volumen incrustada,
 * registro de eventos y exportación PNG.
 *
 * Hooks previstos:
 *  - setPlanService(PlanService service)
 *  - setRenderer(PlanGraficoRenderer renderer)  // si ya tienes un renderer propio
 */
public class PlanGraficoEditor extends BorderPane {

    // ==== Top bar ====
    private final DatePicker dpInicio = new DatePicker(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
    private final ComboBox<TipoPeriodizacion> cbTipo = new ComboBox<>();
    private final Button btnAddMesociclo = new Button("+ Mesociclo");
    private final Button btnGuardar = new Button("Guardar");
    private final Button btnCargar = new Button("Cargar");
    private final Button btnExportar = new Button("Exportar PNG");
    private final Button btnEvento = new Button("Nuevo evento");

    // ==== Lateral: lista de mesociclos ====
    private final ListView<MesocicloVM> lvMesociclos = new ListView<>();
    private final ObservableList<MesocicloVM> mesociclos = FXCollections.observableArrayList();

    // ==== Centro: plan ====
    private final ScrollPane scroll = new ScrollPane();
    private final GridPane grid = new GridPane();
    private final ColumnConstraints colTitulo = new ColumnConstraints(160); // Columna 0 (títulos de fila)
    private final ObservableList<WeekSlot> semanas = FXCollections.observableArrayList();

    // Fila Intensidad/Volumen
    private final Canvas ivCanvas = new Canvas(800, 90); // se reescala dinámicamente

    // Eventos por semana
    private final Map<Integer, List<EventoDeportivoVM>> eventosPorSemana = new HashMap<>();

    // ==== Servicios externos opcionales (hook) ====
    private Object planService;         // PlanService
    private Object planGraficoRenderer; // PlanGraficoRenderer

    // ==== Formato ====
    private static final DateTimeFormatter MES_FMT = DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("es", "MX"));
    private static final DateTimeFormatter RANGO_SEMANA_FMT = DateTimeFormatter.ofPattern("dd MMM", new Locale("es", "MX"));

    public PlanGraficoEditor() {
        buildUI();
        wireActions();
        // Estado inicial
        cbTipo.setItems(FXCollections.observableArrayList(TipoPeriodizacion.values()));
        cbTipo.getSelectionModel().select(TipoPeriodizacion.CLASICA);
        lvMesociclos.setItems(mesociclos);
        lvMesociclos.setCellFactory(list -> new MesocicloCell());
        // Un primer mesociclo por comodidad:
        mesociclos.add(new MesocicloVM("Base", 4, Color.web("#3b82f6")));
        mesociclos.add(new MesocicloVM("Específico", 4, Color.web("#ef4444")));
        mesociclos.add(new MesocicloVM("Competitivo", 4, Color.web("#22c55e")));
        rebuild();
    }

    // ====== Hooks de integración opcional ======
    public void setPlanService(Object planService) {
        this.planService = planService;
    }
    public void setRenderer(Object planGraficoRenderer) {
        this.planGraficoRenderer = planGraficoRenderer;
    }

    // ====== Construcción de UI ======
    private void buildUI() {
        getStylesheets().add(inlineCss());
        setPadding(new Insets(10));

        // Top bar
        ToolBar tb = new ToolBar(
                new Label("Inicio:"), dpInicio,
                new Separator(),
                new Label("Periodización:"), cbTipo,
                new Separator(),
                btnAddMesociclo,
                new Separator(),
                btnEvento,
                new Separator(),
                btnGuardar, btnCargar, btnExportar
        );
        setTop(tb);

        // Lateral (lista de mesociclos)
        VBox left = new VBox(8,
                new Label("Mesociclos"),
                lvMesociclos,
                help("• Doble clic: renombrar\n• Spinner: semanas\n• 🎨: color\n• ↑/↓: reordenar\n• ❌: eliminar")
        );
        left.setPadding(new Insets(10));
        left.setPrefWidth(280);
        setLeft(left);

        // Grid del plan
        grid.getStyleClass().add("plan-grid");
        grid.setHgap(0);
        grid.setVgap(0);
        grid.getColumnConstraints().add(colTitulo);
        scroll.setContent(grid);
        scroll.setFitToWidth(true);
        setCenter(scroll);

        // Estado / pie
        Label pie = new Label();
        pie.textProperty().bind(Bindings.createStringBinding(
                () -> "Semanas: " + semanas.size() + " | Mesociclos: " + mesociclos.size(),
                semanas, mesociclos
        ));
        HBox status = new HBox(pie);
        status.setAlignment(Pos.CENTER_RIGHT);
        status.setPadding(new Insets(8, 0, 0, 0));
        setBottom(status);
    }

    private Label help(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("help");
        return l;
    }

    private void wireActions() {
        dpInicio.valueProperty().addListener((obs, o, n) -> rebuild());
        cbTipo.valueProperty().addListener((obs, o, n) -> rebuild());
        btnAddMesociclo.setOnAction(e -> {
            MesocicloVM vm = new MesocicloVM("Nuevo", 4, randomColor());
            mesociclos.add(vm);
            lvMesociclos.getSelectionModel().select(vm);
            rebuild();
        });
        btnExportar.setOnAction(e -> exportPng());
        btnGuardar.setOnAction(e -> guardarPlan());
        btnCargar.setOnAction(e -> cargarPlan());
        btnEvento.setOnAction(e -> dialogEvento());
        // Rebuild al editar la lista
        mesociclos.addListener((javafx.collections.ListChangeListener<? super MesocicloVM>) c -> rebuild());
    }

    // ====== Núcleo: construir el plan ======
    private void rebuild() {
        // recalcular semanas desde fecha inicio y duración total (suma semanas mesociclos)
        int totalSemanas = mesociclos.stream().mapToInt(m -> m.semanas.get()).sum();
        if (totalSemanas <= 0) totalSemanas = 1;
        semanas.setAll(buildWeeks(dpInicio.getValue(), totalSemanas));

        // limpiar grid
        grid.getChildren().clear();
        grid.getColumnConstraints().setAll(colTitulo);
        // columna por cada semana
        for (int i = 0; i < semanas.size(); i++) {
            ColumnConstraints c = new ColumnConstraints(70);
            grid.getColumnConstraints().add(c);
        }

        int row = 0;
        // Fila Meses
        addHeaderCell(row, "Mes");
        addMonthsRow(row); row++;

        // Fila Mesociclos
        addHeaderCell(row, "Mesociclos");
        addMesociclosRow(row); row++;

        // Fila Semanas
        addHeaderCell(row, "Semanas");
        addWeeksRow(row); row++;

        // Fila Intensidad/Volumen (en la misma grilla)
        addHeaderCell(row, "Intensidad / Volumen");
        addIvRow(row); row++;

        // Fila Eventos
        addHeaderCell(row, "Eventos");
        addEventosRow(row);
    }

    private void addHeaderCell(int row, String text) {
        Label l = new Label(text);
        l.getStyleClass().add("row-title");
        l.setMinHeight(40);
        l.setAlignment(Pos.CENTER_LEFT);
        grid.add(l, 0, row);
    }

    private void addMonthsRow(int row) {
        // Detectar cambios de mes, crear spans
        int col = 1;
        YearMonth current = null;
        int spanStartCol = col;
        for (int i = 0; i < semanas.size(); i++) {
            YearMonth ym = YearMonth.from(semanas.get(i).inicio());
            if (current == null) current = ym;
            if (!ym.equals(current)) {
                // cerrar span anterior
                addMonthSpan(row, spanStartCol, col - 1, current);
                current = ym;
                spanStartCol = col;
            }
            col++;
        }
        // cerrar último
        addMonthSpan(row, spanStartCol, col - 1, current);

        // líneas verticales sutiles por semana
        for (int c = 1; c <= semanas.size(); c++) {
            Region r = new Region();
            r.getStyleClass().add("wk-divider");
            grid.add(r, c, row);
        }
    }

    private void addMonthSpan(int row, int startCol, int endCol, YearMonth ym) {
        int span = Math.max(1, endCol - startCol + 1);
        StackPane sp = new StackPane(new Label(ym.format(MES_FMT)));
        sp.getStyleClass().add("month-span");
        sp.setPrefHeight(36);
        grid.add(sp, startCol, row, span, 1);
    }

    private void addMesociclosRow(int row) {
        int col = 1;
        for (MesocicloVM m : mesociclos) {
            int span = Math.max(1, m.semanas.get());
            StackPane sp = new StackPane(new Label(m.nombre.get()));
            sp.getStyleClass().add("meso-span");
            // color de fondo sutil
            String bg = toRgb(m.color.get(), 0.18);
            sp.setStyle("-fx-background-color: " + bg + "; -fx-border-color: transparent transparent -fx-control-inner-background transparent; -fx-border-width: 0 0 1 0;");
            sp.setPrefHeight(38);
            grid.add(sp, col, row, span, 1);
            col += span;
        }
        // separación por semana
        for (int c = 1; c <= semanas.size(); c++) {
            Region r = new Region();
            r.getStyleClass().add("wk-light");
            grid.add(r, c, row);
        }
    }

    private void addWeeksRow(int row) {
        for (int i = 0; i < semanas.size(); i++) {
            WeekSlot w = semanas.get(i);
            String label = w.inicio().format(RANGO_SEMANA_FMT) + " – " + w.fin().format(RANGO_SEMANA_FMT);
            Label l = new Label(label);
            l.getStyleClass().add("week");
            grid.add(l, i + 1, row);
        }
    }

    private void addIvRow(int row) {
        // Canvas ocupa todas las columnas de semanas
        int totalCols = Math.max(1, semanas.size());
        double width = totalCols * 70.0;
        ivCanvas.setWidth(width);
        ivCanvas.setHeight(90);
        drawIv();
        // envolver para que ocupe el grid
        StackPane wrap = new StackPane(ivCanvas);
        wrap.getStyleClass().add("iv-wrap");
        grid.add(wrap, 1, row, totalCols, 1);

        // líneas débiles por semana
        for (int c = 1; c <= semanas.size(); c++) {
            Region r = new Region();
            r.getStyleClass().add("wk-iv");
            grid.add(r, c, row);
        }

        // Doble clic para editar valores (simple)
        wrap.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) dialogIv();
        });
    }

    private void addEventosRow(int row) {
        // Muestra un punto o etiqueta por semana si hay eventos
        for (int i = 0; i < semanas.size(); i++) {
            int weekIndex = i;
            VBox cell = new VBox(2);
            cell.setAlignment(Pos.CENTER);
            List<EventoDeportivoVM> list = eventosPorSemana.getOrDefault(weekIndex, Collections.emptyList());
            if (!list.isEmpty()) {
                for (EventoDeportivoVM ev : list) {
                    Label chip = new Label("● " + ev.nombre.get());
                    chip.setFont(Font.font("System", 11));
                    chip.setTextFill(Color.web("#0f172a"));
                    chip.setStyle("-fx-background-color: " + toRgb(ev.color.get(), 0.20) + "; -fx-background-radius: 8; -fx-padding: 2 6 2 6;");
                    Tooltip.install(chip, new Tooltip(ev.nombre.get() + " (" + ev.tipo.get() + ")"));
                    cell.getChildren().add(chip);
                }
            }
            StackPane sp = new StackPane(cell);
            sp.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) dialogEvento(weekIndex);
            });
            grid.add(sp, i + 1, row);
        }
    }

    // ====== Intensidad/Volumen (modelo simple paramétrico) ======
    // Valores 0..10 por semana
    private final List<Integer> intensidad = new ArrayList<>();
    private final List<Integer> volumen = new ArrayList<>();

    private void ensureIvSize() {
        int n = semanas.size();
        while (intensidad.size() < n) intensidad.add(5);
        while (volumen.size() < n) volumen.add(5);
        if (intensidad.size() > n) intensidad.subList(n, intensidad.size()).clear();
        if (volumen.size() > n) volumen.subList(n, volumen.size()).clear();
    }

    private void drawIv() {
        ensureIvSize();
        GraphicsContext g = ivCanvas.getGraphicsContext2D();
        double W = ivCanvas.getWidth();
        double H = ivCanvas.getHeight();
        g.clearRect(0, 0, W, H);

        // Ejes suaves
        g.setStroke(Color.gray(0.85));
        for (int i = 0; i <= 10; i += 2) {
            double y = map(i, 0, 10, H - 8, 8);
            g.strokeLine(0, y, W, y);
        }

        // Volumen: línea continua
        g.setStroke(Color.web("#0ea5e9"));
        g.setLineWidth(2.0);
        drawSeries(g, volumen, false);

        // Intensidad: línea discontinua
        g.setStroke(Color.web("#ef4444"));
        g.setLineWidth(2.0);
        double[] dash = {8, 6};
        g.setLineDashes(dash);
        drawSeries(g, intensidad, true);
        g.setLineDashes(null);

        // Leyenda
        g.setFill(Color.web("#0ea5e9"));
        g.fillRect(8, 8, 10, 3);
        g.setFill(Color.web("#0f172a"));
        g.fillText("Volumen", 24, 12);
        g.setStroke(Color.web("#ef4444"));
        g.setLineDashes(dash);
        g.strokeLine(90, 10, 110, 10);
        g.setLineDashes(null);
        g.setFill(Color.web("#0f172a"));
        g.fillText("Intensidad", 116, 12);
    }

    private void drawSeries(GraphicsContext g, List<Integer> series, boolean offsetUp) {
        int n = semanas.size();
        if (n == 0) return;
        double step = 70.0;
        double ox = step / 2.0;
        double baseY;
        for (int i = 0; i < n; i++) {
            double x1 = i * step + ox;
            double y1 = map(series.get(i), 0, 10, ivCanvas.getHeight() - 8, 18);
            if (i == 0) {
                baseY = y1;
            } else {
                double x0 = (i - 1) * step + ox;
                double y0 = map(series.get(i - 1), 0, 10, ivCanvas.getHeight() - 8, 18);
                g.strokeLine(x0, y0, x1, y1);
            }
        }
    }

    private static double map(double v, double inMin, double inMax, double outMin, double outMax) {
        if (inMax - inMin == 0) return outMin;
        return outMin + (v - inMin) * (outMax - outMin) / (inMax - inMin);
    }

    // ====== Diálogos ======
    private void dialogIv() {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Editar Intensidad/Volumen por semana");
        ButtonType ok = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

        GridPane gp = new GridPane();
        gp.setHgap(8); gp.setVgap(6);
        gp.setPadding(new Insets(10));

        ensureIvSize();
        for (int i = 0; i < semanas.size(); i++) {
            WeekSlot w = semanas.get(i);
            Label lw = new Label((i+1) + " (" + w.inicio().format(RANGO_SEMANA_FMT) + ")");
            Spinner<Integer> spV = new Spinner<>(0, 10, volumen.get(i));
            spV.setEditable(true);
            Spinner<Integer> spI = new Spinner<>(0, 10, intensidad.get(i));
            spI.setEditable(true);
            gp.addRow(i, lw, new Label("Vol:"), spV, new Label("Int:"), spI);
            int idx = i;
            spV.valueProperty().addListener((o, a, b) -> volumen.set(idx, b));
            spI.valueProperty().addListener((o, a, b) -> intensidad.set(idx, b));
        }

        dlg.getDialogPane().setContent(new ScrollPane(gp));
        dlg.showAndWait();
        drawIv();
    }

    private void dialogEvento() {
        dialogEvento(null);
    }

    private void dialogEvento(Integer semanaIndexOrNull) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Nuevo evento deportivo");
        ButtonType ok = new ButtonType("Agregar", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

        TextField tfNombre = new TextField();
        ComboBox<String> cbTipoEv = new ComboBox<>(FXCollections.observableArrayList("Partido", "Amistoso", "Torneo", "Prueba física", "Otro"));
        cbTipoEv.getSelectionModel().selectFirst();
        ColorPicker cp = new ColorPicker(Color.web("#f59e0b"));
        Spinner<Integer> spSemana = new Spinner<>(1, Math.max(1, semanas.size()), semanaIndexOrNull == null ? 1 : semanaIndexOrNull + 1);
        spSemana.setEditable(true);

        GridPane gp = new GridPane();
        gp.setHgap(8); gp.setVgap(6);
        gp.addRow(0, new Label("Nombre:"), tfNombre);
        gp.addRow(1, new Label("Tipo:"), cbTipoEv);
        gp.addRow(2, new Label("Semana:"), spSemana);
        gp.addRow(3, new Label("Color:"), cp);
        gp.setPadding(new Insets(10));

        dlg.getDialogPane().setContent(gp);
        dlg.setResultConverter(bt -> {
            if (bt == ok && !tfNombre.getText().isBlank()) {
                int idx = spSemana.getValue() - 1;
                eventosPorSemana.computeIfAbsent(idx, k -> new ArrayList<>())
                        .add(new EventoDeportivoVM(tfNombre.getText(), cbTipoEv.getValue(), cp.getValue()));
            }
            return null;
        });
        dlg.showAndWait();
        rebuild();
    }

    private void exportPng() {
        // snapshot solo del grid (con barra lateral oculta)
        WritableImage img = grid.snapshot(new SnapshotParameters(), null);
        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar Plan Gráfico");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagen PNG", "*.png"));
        fc.setInitialFileName("plan_grafico.png");
        File f = fc.showSaveDialog(getScene().getWindow());
        if (f != null) {
            try {
                javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(img, null), "png", f);
                new Alert(Alert.AlertType.INFORMATION, "Exportado en:\n" + f.getAbsolutePath()).showAndWait();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "No se pudo exportar:\n" + ex.getMessage()).showAndWait();
            }
        }
    }

    private void guardarPlan() {
        // Hook: integra con tu PlanService/DAO
        new Alert(Alert.AlertType.INFORMATION, "Aquí llamarías a tu PlanService.save(PlanGrafico). Se dejó como stub.").showAndWait();
    }

    private void cargarPlan() {
        // Hook: integra con tu PlanService/DAO
        new Alert(Alert.AlertType.INFORMATION, "Aquí llamarías a tu PlanService.load(id). Se dejó como stub.").showAndWait();
    }

    // ====== Utilidades ======
    private static List<WeekSlot> buildWeeks(LocalDate start, int totalSemanas) {
        LocalDate s = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<WeekSlot> list = new ArrayList<>(totalSemanas);
        for (int i = 0; i < totalSemanas; i++) {
            LocalDate wStart = s.plusWeeks(i);
            list.add(new WeekSlot(wStart, wStart.plusDays(6)));
        }
        return list;
    }

    private static String toRgb(Color c, double alpha) {
        return String.format("rgba(%d,%d,%d,%.3f)", (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255), alpha);
    }

    private static Color randomColor() {
        Color[] palette = new Color[]{
                Color.web("#3b82f6"), Color.web("#ef4444"), Color.web("#22c55e"),
                Color.web("#a855f7"), Color.web("#f59e0b"), Color.web("#06b6d4")
        };
        return palette[new Random().nextInt(palette.length)];
    }

    private String inlineCss() {
        // Estilos integrados: puedes pasarlos a .css
        return getClass().getResource("plan-graphic-inline.css") == null ? """
            .root { -fx-font-size: 12px; }
            .plan-grid { -fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-width: 1; }
            .row-title { -fx-font-weight: bold; -fx-padding: 0 8 0 8; -fx-background-color: #f8fafc; -fx-border-color: #e5e7eb; -fx-border-width: 0 1 1 0; }
            .month-span { -fx-background-color: #f1f5f9; -fx-border-color: #e5e7eb; -fx-border-width: 0 0 1 0; }
            .wk-divider { -fx-border-color: transparent transparent #e5e7eb transparent; -fx-border-width: 0 0 1 0; }
            .wk-light { -fx-background-color: rgba(15,23,42,0.02); }
            .week { -fx-alignment: center; -fx-padding: 6 0 6 0; -fx-border-color: #f1f5f9; -fx-border-width: 0 0 1 0; }
            .help { -fx-text-fill: #475569; -fx-font-size: 11px; }
            .iv-wrap { -fx-background-color: white; }
            .wk-iv { -fx-border-color: #f1f5f9; -fx-border-width: 0 0 1 0; }
            """ : "";
    }

    // ====== VM / tipos simples ======
    private enum TipoPeriodizacion { CLASICA, ATR, BLOQUES }

    private static final class WeekSlot {
        private final LocalDate inicio, fin;
        WeekSlot(LocalDate inicio, LocalDate fin) { this.inicio = inicio; this.fin = fin; }
        LocalDate inicio() { return inicio; }
        LocalDate fin() { return fin; }
    }

    public static final class MesocicloVM {
        public final StringProperty nombre = new SimpleStringProperty();
        public final IntegerProperty semanas = new SimpleIntegerProperty();
        public final ObjectProperty<Color> color = new SimpleObjectProperty<>();
        public MesocicloVM(String n, int s, Color c) {
            nombre.set(n); semanas.set(Math.max(1, s)); color.set(c);
        }
    }

    public static final class EventoDeportivoVM {
        public final StringProperty nombre = new SimpleStringProperty();
        public final StringProperty tipo = new SimpleStringProperty();
        public final ObjectProperty<Color> color = new SimpleObjectProperty<>();
        public EventoDeportivoVM(String nombre, String tipo, Color color) {
            this.nombre.set(nombre); this.tipo.set(tipo); this.color.set(color);
        }
    }

    // ====== Celda personalizable para mesociclos ======
    private final class MesocicloCell extends ListCell<MesocicloVM> {
        private final TextField tfNombre = new TextField();
        private final Spinner<Integer> spSemanas = new Spinner<>(1, 64, 4);
        private final ColorPicker cp = new ColorPicker();
        private final Button btnUp = new Button("↑");
        private final Button btnDown = new Button("↓");
        private final Button btnDel = new Button("❌");
        private final HBox root = new HBox(6, tfNombre, spSemanas, cp, btnUp, btnDown, btnDel);

        MesocicloCell() {
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(4));
            spSemanas.setPrefWidth(70);
            tfNombre.setPrefColumnCount(10);
            setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && getItem() != null) {
                    tfNombre.requestFocus();
                    tfNombre.selectAll();
                }
            });
            btnUp.setOnAction(e -> move(-1));
            btnDown.setOnAction(e -> move(1));
            btnDel.setOnAction(e -> {
                MesocicloVM it = getItem();
                if (it != null) {
                    mesociclos.remove(it);
                }
            });
            // Enter → recalcular
            root.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) rebuild();
            });
        }

        private void move(int dir) {
            MesocicloVM it = getItem();
            if (it == null) return;
            int idx = mesociclos.indexOf(it);
            int nidx = idx + dir;
            if (nidx >= 0 && nidx < mesociclos.size()) {
                mesociclos.remove(idx);
                mesociclos.add(nidx, it);
                lvMesociclos.getSelectionModel().select(nidx);
            }
        }

        @Override
        protected void updateItem(MesocicloVM item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                tfNombre.textProperty().unbindBidirectional(item.nombre);
                spSemanas.getValueFactory().valueProperty().unbindBidirectional(item.semanas.asObject());
                cp.valueProperty().unbindBidirectional(item.color);

                tfNombre.textProperty().bindBidirectional(item.nombre);
                spSemanas.getValueFactory().valueProperty().bindBidirectional(item.semanas.asObject());
                cp.valueProperty().bindBidirectional(item.color);

                setGraphic(root);
            }
        }
    }

  /*  // ====== Demo App ======
    public static class Demo extends Application {
        @Override
        public void start(Stage stage) {
            PlanGraficoEditor editor = new PlanGraficoEditor();
            Scene scene = new Scene(editor, 1200, 650);
            stage.setTitle("Plan Gráfico - Editor");
            stage.setScene(scene);
            stage.show();
        }
    }

    public static void main(String[] args) {
        Application.launch(Demo.class, args);
    }*/
}

