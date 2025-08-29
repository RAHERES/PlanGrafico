/*
package com.example.plangrafico.ui;



import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

*/
/**
 * Editor integral del Plan Gráfico:
 * - Fecha inicio y fin (fin auto-calc con opción de fijarlo manual).
 * - Estructura jerárquica: Periodos -> Mesociclos -> Microciclos -> Sesiones.
 * - Eventos (competencias, pruebas, etc.) por fecha.
 * - Línea de tiempo semanal: Meses, Periodos, Mesociclos, Microciclos, Carga semanal (barras), Eventos.
 * - Cálculo de fechas por duraciones (semanas) sin solapes.
 *
 * Hooks:
 *   setPlanService(Object service) // PlanService
 *   save/load stubs marcados para conectar a DAO
 *//*

public class PlanGraficoStudio extends BorderPane {

    // ======= Top controls =======
    private final DatePicker dpInicio = new DatePicker(LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
    private final DatePicker dpFin = new DatePicker(LocalDate.now().plusWeeks(12)
            .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
    private final CheckBox cbAutoFin = new CheckBox("Fin automático");
    private final ComboBox<String> cbTipo = new ComboBox<>(
            FXCollections.observableArrayList("Clásica", "ATR", "Bloques"));
    private final Button btnAddPeriodo = new Button("+ Periodo");
    private final Button btnAddMesociclo = new Button("+ Mesociclo");
    private final Button btnAddMicrociclo = new Button("+ Microciclo");
    private final Button btnAddSesion = new Button("+ Sesión");
    private final Button btnAddEvento = new Button("+ Evento");
    private final Button btnExportar = new Button("Exportar PNG");
    private final Button btnGuardar = new Button("Guardar");
    private final Button btnCargar = new Button("Cargar");

    // ======= Left: árbol de estructura =======
    private final TreeTableView<Nodo> tree = new TreeTableView<>();
    private final TreeItem<Nodo> rootItem = new TreeItem<>(Nodo.programa());

    // ======= Center: timeline =======
    private final ScrollPane scroller = new ScrollPane();
    private final GridPane grid = new GridPane();

    // ======= Datos =======
    private final ObservableList<Periodo> periodos = FXCollections.observableArrayList();
    private final ObservableList<Evento> eventos = FXCollections.observableArrayList();
    private final ObservableList<WeekSlot> semanas = FXCollections.observableArrayList();



    // ======= Servicios externos (hook) =======
    private Object planService; // PlanService

    // ======= Formatos =======
    private static final Locale ES_MX = new Locale("es", "MX");
    private static final DateTimeFormatter MES_FMT = DateTimeFormatter.ofPattern("MMMM yyyy", ES_MX);
    private static final DateTimeFormatter DIA_CORTO = DateTimeFormatter.ofPattern("dd MMM", ES_MX);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy", ES_MX);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    public PlanGraficoStudio() {
        buildUI();
        wire();
        // Estado inicial
        cbTipo.getSelectionModel().selectFirst();
        // Semilla: un periodo con 4-4-4 semanas
        Periodo pPrep = new Periodo("Preparación", 8, color("#3b82f6"));
        Periodo pComp = new Periodo("Competitivo", 4, color("#22c55e"));
        periodos.addAll(pPrep, pComp);
        Mesociclo m1 = new Mesociclo("Base", 4, color("#60a5fa"));
        Mesociclo m2 = new Mesociclo("Específico", 4, color("#f59e0b"));
        pPrep.mesociclos.addAll(m1, m2);
        m1.microciclos.addAll(Microciclo.def("Intro", 1), Microciclo.def("Carga", 1),
                Microciclo.def("Impacto", 1), Microciclo.def("Recup", 1));
        m2.microciclos.addAll(Microciclo.def("Carga", 1), Microciclo.def("Impacto", 1),
                Microciclo.def("Carga", 1), Microciclo.def("Recup", 1));
        pComp.mesociclos.addAll(new Mesociclo("Competición", 4, color("#34d399")));
        pComp.mesociclos.get(0).microciclos.addAll(
                Microciclo.def("Pico", 1), Microciclo.def("Mantenimiento", 1),
                Microciclo.def("Mantenimiento", 1), Microciclo.def("Transición", 1)
        );
        rebuildTree();
        recalcAndRedraw();
    }

    // ======= Integración =======
    public void setPlanService(Object service) { this.planService = service; }

    // ======= UI =======
    private void buildUI() {
        getStylesheets().add(inlineCss());
        setPadding(new Insets(10));

        var tb = new ToolBar(
                new Label("Inicio:"), dpInicio,
                new Label("Fin:"), dpFin, cbAutoFin,
                new Separator(),
                new Label("Periodización:"), cbTipo,
                new Separator(),
                btnAddPeriodo, btnAddMesociclo, btnAddMicrociclo, btnAddSesion,
                new Separator(),
                btnAddEvento,
                new Separator(),
                btnGuardar, btnCargar, btnExportar
        );
        setTop(tb);
        cbAutoFin.setSelected(true);
        dpFin.setDisable(true);

        // Árbol
        var colNombre = new TreeTableColumn<Nodo, String>("Nombre");
        colNombre.setCellValueFactory(new TreeItemPropertyValueFactory<>("nombre"));
        colNombre.setPrefWidth(220);

        var colTipo = new TreeTableColumn<Nodo, String>("Tipo");
        colTipo.setCellValueFactory(new TreeItemPropertyValueFactory<>("tipo"));
        colTipo.setPrefWidth(120);

        var colDur = new TreeTableColumn<Nodo, String>("Duración");
        colDur.setCellValueFactory(param ->
                new SimpleStringProperty(param.getValue().getValue().duracionTexto()));
        colDur.setPrefWidth(90);

        var colInicio = new TreeTableColumn<Nodo, String>("Inicio");
        colInicio.setCellValueFactory(param ->
                new SimpleStringProperty(fmt(param.getValue().getValue().inicioProperty().get())));
        colInicio.setPrefWidth(90);

        var colFin = new TreeTableColumn<Nodo, String>("Fin");
        colFin.setCellValueFactory(param ->
                new SimpleStringProperty(fmt(param.getValue().getValue().finProperty().get())));
        colFin.setPrefWidth(90);

        var colCarga = new TreeTableColumn<Nodo, String>("Carga");
        colCarga.setCellValueFactory(param ->
                new SimpleStringProperty(param.getValue().getValue().cargaTexto()));
        colCarga.setPrefWidth(110);

        tree.getColumns().addAll(colNombre, colTipo, colDur, colInicio, colFin, colCarga);
        tree.setRoot(rootItem);
        tree.setShowRoot(false);
        tree.setPrefWidth(740);
        setLeft(new VBox(new Label("Estructura del Plan"), tree) {{
            setSpacing(6);
            setPadding(new Insets(6, 10, 6, 0));
        }});

        // Timeline
        grid.getStyleClass().add("plan-grid");
        scroller.setContent(grid);
        scroller.setFitToWidth(true);
        setCenter(scroller);

        // Pie
        Label pie = new Label();
        pie.textProperty().bind(Bindings.createStringBinding(
                () -> "Semanas: " + semanas.size() + " | Periodos: " + periodos.size(),
                semanas, periodos
        ));
        HBox status = new HBox(pie);
        status.setAlignment(Pos.CENTER_RIGHT);
        status.setPadding(new Insets(6, 0, 0, 0));
        setBottom(status);
    }

    private void wire() {
        dpInicio.valueProperty().addListener((o, a, b) -> { recalcAndRedraw(); });
        dpFin.valueProperty().addListener((o, a, b) -> {
            if (!cbAutoFin.isSelected()) recalcAndRedraw();
        });
        cbAutoFin.selectedProperty().addListener((o, a, b) -> {
            dpFin.setDisable(b);
            recalcAndRedraw();
        });

        btnAddPeriodo.setOnAction(e -> {
            var p = new Periodo("Periodo " + (periodos.size()+1), 4, randomColor());
            periodos.add(p);
            rebuildTree();
            recalcAndRedraw();
        });

        btnAddMesociclo.setOnAction(e -> {
            var sel = selected(Periodo.class);
            if (sel == null) {
                alertInfo("Selecciona un Periodo para agregar un Mesociclo.");
                return;
            }
            sel.mesociclos.add(new Mesociclo("Mesociclo", 4, randomColor()));
            rebuildTree();
            recalcAndRedraw();
        });

        btnAddMicrociclo.setOnAction(e -> {
            var selM = selected(Mesociclo.class);
            if (selM == null) {
                alertInfo("Selecciona un Mesociclo para agregar un Microciclo.");
                return;
            }
            selM.microciclos.add(Microciclo.def("Micro", 1));
            rebuildTree();
            recalcAndRedraw();
        });

        btnAddSesion.setOnAction(e -> {
            var selMi = selected(Microciclo.class);
            if (selMi == null) {
                alertInfo("Selecciona un Microciclo para agregar una Sesión.");
                return;
            }
            // Sesión propuesta: martes 17:00, 90 min
            LocalDate f = selMi.inicio.get() != null ? selMi.inicio.get().with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY)) : dpInicio.getValue();
            selMi.sesiones.add(new Sesion("Entrenamiento", f, LocalTime.of(17,0), 90, 5, 5));
            rebuildTree();
            recalcAndRedraw();
        });

        btnAddEvento.setOnAction(e -> dialogEvento());

        btnExportar.setOnAction(e -> exportPng());
        btnGuardar.setOnAction(e -> guardar());
        btnCargar.setOnAction(e -> cargar());

        // Doble clic para editar propiedades básicas
        tree.setRowFactory(tv -> {
            TreeTableRow<Nodo> row = new TreeTableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) {
                    showEditDialog(row.getItem());
                }
            });
            return row;
        });
    }

    // ======= Recalcular fechas y redibujar =======
    private void recalcAndRedraw() {
        // Fechas por duraciones
        LocalDate start = dpInicio.getValue();
        LocalDate cursor = start;
        for (Periodo p : periodos) {
            p.inicio.set(cursor);
            int w = Math.max(1, p.semanas.get());
            p.fin.set(cursor.plusWeeks(w).minusDays(1));
            // hijos
            LocalDate c2 = cursor;
            for (Mesociclo m : p.mesociclos) {
                m.inicio.set(c2);
                int wm = Math.max(1, m.semanas.get());
                m.fin.set(c2.plusWeeks(wm).minusDays(1));
                // microciclos
                LocalDate c3 = c2;
                for (Microciclo mi : m.microciclos) {
                    int wmi = Math.max(1, mi.semanas.get());
                    mi.inicio.set(c3);
                    mi.fin.set(c3.plusWeeks(wmi).minusDays(1));
                    c3 = c3.plusWeeks(wmi);
                }
                c2 = c2.plusWeeks(wm);
            }
            cursor = cursor.plusWeeks(w);
        }
        // Fin de programa
        LocalDate finAuto = start;
        for (Periodo p : periodos) finAuto = finAuto.plusWeeks(Math.max(1, p.semanas.get()));
        finAuto = finAuto.minusDays(1);
        if (cbAutoFin.isSelected()) {
            dpFin.setValue(finAuto);
        }
        // Recalcular semanas globales
        buildWeeks(start, dpFin.getValue());
        // Redibujar timeline
        redrawGrid();
    }

    private void buildWeeks(LocalDate ini, LocalDate fin) {
        semanas.clear();
        if (ini == null || fin == null || !fin.isAfter(ini)) {
            semanas.add(new WeekSlot(ini, ini != null ? ini.plusDays(6) : LocalDate.now().plusDays(6)));
            return;
        }
        LocalDate s = ini.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        while (!s.isAfter(fin)) {
            semanas.add(new WeekSlot(s, s.plusDays(6)));
            s = s.plusWeeks(1);
        }
    }

    private void redrawGrid() {
        grid.getChildren().clear();
        grid.getColumnConstraints().setAll(new ColumnConstraints(180)); // col 0 títulos
        for (int i = 0; i < semanas.size(); i++) {
            ColumnConstraints c = new ColumnConstraints(70);
            grid.getColumnConstraints().add(c);
        }
        int row = 0;
        addHeaderCell(row, "Mes");
        drawMonthsRow(row++);

        addHeaderCell(row, "Periodos");
        drawPeriodosRow(row++);

        addHeaderCell(row, "Mesociclos");
        drawMesociclosRow(row++);

        addHeaderCell(row, "Microciclos");
        drawMicrociclosRow(row++);

        addHeaderCell(row, "Carga (sRPE)");
        drawCargaRow(row++);

        addHeaderCell(row, "Eventos");
        drawEventosRow(row);
    }

    private void addHeaderCell(int row, String title) {
        Label l = new Label(title);
        l.getStyleClass().add("row-title");
        grid.add(l, 0, row);
    }

    // ======= Draw rows =======
    private void drawMonthsRow(int row) {
        int col = 1;
        YearMonth current = null; int spanStart = col;
        for (int i = 0; i < semanas.size(); i++) {
            YearMonth ym = YearMonth.from(semanas.get(i).inicio());
            if (current == null) current = ym;
            if (!ym.equals(current)) {
                addSpan(row, spanStart, col - 1, current.format(MES_FMT), Color.web("#f1f5f9"), Color.web("#94a3b8"));
                current = ym; spanStart = col;
            }
            col++;
        }
        addSpan(row, spanStart, col - 1, current != null ? current.format(MES_FMT) : "", Color.web("#f1f5f9"), Color.web("#94a3b8"));
    }

    private void drawPeriodosRow(int row) {
        int col = 1;
        for (Periodo p : periodos) {
            int span = weeksWithin(p.inicio.get(), p.fin.get());
            addSpan(row, col, col + span - 1, p.nombre.get(), p.color.get().deriveColor(0,1,1,0.25), p.color.get());
            col += span;
        }
    }

    private void drawMesociclosRow(int row) {
        int colBase = 1;
        for (Periodo p : periodos) {
            for (Mesociclo m : p.mesociclos) {
                int span = weeksWithin(m.inicio.get(), m.fin.get());
                addSpan(row, weekToCol(m.inicio.get()), weekToCol(m.inicio.get()) + span - 1,
                        m.nombre.get(), m.color.get().deriveColor(0,1,1,0.20), m.color.get());
            }
        }
        // separadores suaves
        for (int c = 1; c <= semanas.size(); c++) addWeekHairline(row, c);
    }

    private void drawMicrociclosRow(int row) {
        for (Periodo p : periodos) {
            for (Mesociclo m : p.mesociclos) {
                for (Microciclo mi : m.microciclos) {
                    int span = weeksWithin(mi.inicio.get(), mi.fin.get());
                    Color c = chipColor(mi.tipo.get());
                    addSpan(row, weekToCol(mi.inicio.get()), weekToCol(mi.inicio.get()) + span - 1,
                            mi.nombre.get() + " (" + mi.tipo.get() + ")", c.deriveColor(0,1,1,0.18), c);
                }
            }
        }
        for (int c = 1; c <= semanas.size(); c++) addWeekHairline(row, c);
    }

    private void drawCargaRow(int row) {
        // Calcular carga semanal (suma RPE*min de sesiones en esa semana)
        Map<Integer, Integer> carga = new HashMap<>();
        for (Periodo p : periodos) {
            for (Mesociclo m : p.mesociclos) {
                for (Microciclo mi : m.microciclos) {
                    for (Sesion s : mi.sesiones) {
                        if (s.inicio.get() == null) continue;
                        int wk = weekIndexOf(s.inicio.get());
                        int load = Math.max(0, s.rpe.get()) * Math.max(0, s.minutos.get());
                        carga.merge(wk, load, Integer::sum);
                    }
                }
            }
        }
        int max = carga.values().stream().mapToInt(i->i).max().orElse(1);
        // Dibujo barras
        Canvas cv = new Canvas(semanas.size()*70, 70);
        GraphicsContext g = cv.getGraphicsContext2D();
        g.setFill(Color.gray(0.95));
        g.fillRect(0,0,cv.getWidth(), cv.getHeight());
        for (int i = 0; i < semanas.size(); i++) {
            int val = carga.getOrDefault(i, 0);
            double h = (val/(double)max) * (cv.getHeight()-16);
            double x = i*70 + 20;
            double y = cv.getHeight()-8-h;
            g.setFill(Color.web("#0ea5e9"));
            g.fillRect(x, y, 30, h);
            g.setFill(Color.gray(0.35));
            if (val>0) g.fillText(String.valueOf(val), x, y-2);
        }
        grid.add(new StackPane(cv), 1, row, semanas.size(), 1);
        for (int c = 1; c <= semanas.size(); c++) addWeekHairline(row, c);
        // Doble clic sobre la barra -> abrir listado de sesiones de esa semana
        cv.setOnMouseClicked(e -> {
            if (e.getClickCount()==2) {
                int idx = (int)(e.getX()/70.0);
                showSesionesDeSemana(idx);
            }
        });
    }

    private void drawEventosRow(int row) {
        // Mostrar chips por evento en su semana
        Map<Integer,List<Evento>> byWeek = eventos.stream()
                .filter(ev -> ev.fecha.get()!=null)
                .collect(Collectors.groupingBy(ev -> weekIndexOf(ev.fecha.get())));
        for (int i = 0; i < semanas.size(); i++) {
            VBox box = new VBox(2); box.setAlignment(Pos.CENTER);
            List<Evento> list = byWeek.getOrDefault(i, Collections.emptyList());
            for (Evento ev : list) {
                Label chip = new Label("● " + ev.nombre.get());
                chip.setStyle("-fx-background-color:"+rgba(ev.color.get(), .20)+"; -fx-background-radius: 10; -fx-padding:2 6 2 6;");
                Tooltip t = new Tooltip(ev.tipo.get() + " — " + fmt(ev.fecha.get()));
                Tooltip.install(chip, t);
                box.getChildren().add(chip);
            }
            StackPane sp = new StackPane(box);
            int weekIndex = i;
            sp.setOnMouseClicked(e -> { if (e.getClickCount()==2) dialogEventoForWeek(weekIndex); });
            grid.add(sp, i+1, row);
        }
    }

    // ======= Helpers timeline =======
    private void addSpan(int row, int startCol, int endCol, String text, Color bg, Color stroke) {
        int span = Math.max(1, endCol - startCol + 1);
        StackPane sp = new StackPane(new Label(text));
        sp.getStyleClass().add("span");
        sp.setStyle("-fx-background-color:"+rgba(bg)+"; -fx-border-color:"+hex(stroke)+"; -fx-border-width:1; -fx-background-radius:6; -fx-border-radius:6;");
        grid.add(sp, startCol, row, span, 1);
    }
    private void addWeekHairline(int row, int col) {
        Region r = new Region();
        r.setStyle("-fx-border-color: transparent transparent #e5e7eb transparent; -fx-border-width:0 0 1 0;");
        grid.add(r, col, row);
    }
    private int weekToCol(LocalDate date) { return 1 + Math.max(0, weekIndexOf(date)); }
    private int weekIndexOf(LocalDate date) {
        for (int i=0;i<semanas.size();i++){
            var w = semanas.get(i);
            if ((date.isEqual(w.inicio()) || date.isAfter(w.inicio())) && (date.isEqual(w.fin()) || date.isBefore(w.fin()))) return i;
        }
        return 0;
    }
    private int weeksWithin(LocalDate a, LocalDate b) {
        if (a==null||b==null||b.isBefore(a)) return 1;
        int start = weekIndexOf(a);
        int end = weekIndexOf(b);
        return Math.max(1, end-start+1);
    }

    // ======= Editores =======
    private void showEditDialog(Nodo n) {
        if (n instanceof Periodo p) {
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Editar Periodo");
            var nombre = new TextField(p.nombre.get());
            var spSem = new Spinner<>(1, 52, p.semanas.get());
            var cp = new ColorPicker(p.color.get());
            GridPane gp = gridForm(
                    "Nombre:", nombre,
                    "Semanas:", spSem,
                    "Color:", cp);
            d.getDialogPane().setContent(gp);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            d.setResultConverter(bt -> {
                if (bt==ButtonType.OK){
                    p.nombre.set(nombre.getText());
                    p.semanas.set((Integer) spSem.getValue());
                    p.color.set(cp.getValue());
                    rebuildTree(); recalcAndRedraw();
                }
                return bt;
            });
            d.showAndWait();
        } else if (n instanceof Mesociclo m) {
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Editar Mesociclo");
            var nombre = new TextField(m.nombre.get());
            var spSem = new Spinner<>(1, 52, m.semanas.get());
            var cp = new ColorPicker(m.color.get());
            GridPane gp = gridForm(
                    "Nombre:", nombre,
                    "Semanas:", spSem,
                    "Color:", cp);
            d.getDialogPane().setContent(gp);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            d.setResultConverter(bt -> {
                if (bt==ButtonType.OK){
                    m.nombre.set(nombre.getText());
                    m.semanas.set((Integer) spSem.getValue());
                    m.color.set(cp.getValue());
                    rebuildTree(); recalcAndRedraw();
                }
                return bt;
            });
            d.showAndWait();
        } else if (n instanceof Microciclo mi) {
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Editar Microciclo");
            var nombre = new TextField(mi.nombre.get());
            var tipo = new ComboBox<String>(FXCollections.observableArrayList(
                    "Introductorio","Carga","Impacto","Recuperación","Mantenimiento","Pico","Transición"));
            tipo.getSelectionModel().select(mi.tipo.get());
            var spSem = new Spinner<>(1, 8, mi.semanas.get());
            GridPane gp = gridForm("Nombre:", nombre, "Tipo:", tipo, "Semanas:", spSem);
            d.getDialogPane().setContent(gp);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            d.setResultConverter(bt -> {
                if (bt==ButtonType.OK){
                    mi.nombre.set(nombre.getText());
                    mi.tipo.set(tipo.getValue());
                    mi.semanas.set((Integer) spSem.getValue());
                    rebuildTree(); recalcAndRedraw();
                }
                return bt;
            });
            d.showAndWait();
        } else if (n instanceof Sesion s) {
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Editar Sesión");
            var nombre = new TextField(s.nombre.get());
            var fecha = new DatePicker(s.inicio.get());
            var hora = new Spinner<>(0, 23, s.hora.get().getHour());
            var min = new Spinner<>(0, 59, s.hora.get().getMinute());
            var dur = new Spinner<>(10, 240, s.minutos.get(), 5);
            var rpe = new Spinner<>(0, 10, s.rpe.get());
            var inten = new Spinner<>(0, 10, s.intensidad.get());
            GridPane gp = gridForm(
                    "Nombre:", nombre,
                    "Fecha:", fecha,
                    "Hora (h:m):", new HBox(6, new Label("h:"), hora, new Label("m:"), min),
                    "Minutos:", dur,
                    "RPE:", rpe,
                    "Intensidad:", inten
            );
            d.getDialogPane().setContent(gp);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            d.setResultConverter(bt -> {
                if (bt==ButtonType.OK){
                    s.nombre.set(nombre.getText());
                    s.inicio.set(fecha.getValue());
                    s.hora.set(LocalTime.of((Integer) hora.getValue(), (Integer) min.getValue()));
                    s.minutos.set((Integer) dur.getValue());
                    s.rpe.set((Integer) rpe.getValue());
                    s.intensidad.set((Integer) inten.getValue());
                    rebuildTree(); recalcAndRedraw();
                }
                return bt;
            });
            d.showAndWait();
        }
    }

    private void showSesionesDeSemana(int weekIndex) {
        LocalDate ini = semanas.get(weekIndex).inicio();
        LocalDate fin = semanas.get(weekIndex).fin();
        List<Sesion> list = new ArrayList<>();
        for (Periodo p: periodos) for (Mesociclo m: p.mesociclos) for (Microciclo mi: m.microciclos)
            for (Sesion s: mi.sesiones) if (s.inicio.get()!=null && !s.inicio.get().isBefore(ini) && !s.inicio.get().isAfter(fin)) list.add(s);
        if (list.isEmpty()) { alertInfo("No hay sesiones en esa semana."); return; }
        String text = list.stream().map(s -> "- " + s.nombre.get() + " | " + fmt(s.inicio.get()) + " " + s.hora.get().format(HORA) +
                        " | " + s.minutos.get()+" min | RPE "+s.rpe.get()+" → sRPE "+(s.rpe.get()*s.minutos.get()))
                .collect(Collectors.joining("\n"));
        alertInfo("Sesiones ("+ini.format(FECHA)+" - "+fin.format(FECHA)+"):\n\n"+text);
    }

    private void dialogEvento() {
        dialogEventoForWeek(null);
    }
    private void dialogEventoForWeek(Integer wkIndexOrNull) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Nuevo evento");
        var nombre = new TextField();
        var tipo = new ComboBox<String>(FXCollections.observableArrayList("Partido","Amistoso","Torneo","Prueba física","Otro"));
        tipo.getSelectionModel().selectFirst();
        var fecha = new DatePicker(wkIndexOrNull==null ? dpInicio.getValue() : semanas.get(wkIndexOrNull).inicio());
        var color = new ColorPicker(Color.web("#f59e0b"));
        GridPane gp = gridForm("Nombre:", nombre, "Tipo:", tipo, "Fecha:", fecha, "Color:", color);
        d.getDialogPane().setContent(gp);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(bt -> {
            if (bt==ButtonType.OK && !nombre.getText().isBlank()) {
                eventos.add(new Evento(nombre.getText(), tipo.getValue(), fecha.getValue(), color.getValue()));
                recalcAndRedraw();
            }
            return bt;
        });
        d.showAndWait();
    }

    // ======= Persistencia (stubs) =======
    private void guardar() {
        // TODO: Conecta aquí tu PlanService / DAO:
        // - dpInicio.getValue(), dpFin.getValue(), cbTipo.getValue()
        // - periodos (con sus mesociclos, microciclos, sesiones)
        // - eventos
        alertInfo("Stub de guardado: integra con PlanService/DAO aquí.");
    }
    private void cargar() {
        alertInfo("Stub de carga: integra con PlanService/DAO aquí.");
    }

    private void exportPng() {
        WritableImage img = grid.snapshot(new SnapshotParameters(), null);
        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar Plan Gráfico");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
        fc.setInitialFileName("plan_grafico.png");
        File f = fc.showSaveDialog(getScene().getWindow());
        if (f!=null) {
            try {
                javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(img, null), "png", f);
                alertInfo("Exportado a:\n"+f.getAbsolutePath());
            } catch (Exception ex) { alertError("No se pudo exportar:\n"+ex.getMessage()); }
        }
    }

    // ======= Árbol =======
    private void rebuildTree() {
        rootItem.getChildren().clear();
        for (Periodo p : periodos) {
            TreeItem<Nodo> pItem = new TreeItem<>(p);
            for (Mesociclo m : p.mesociclos) {
                TreeItem<Nodo> mItem = new TreeItem<>(m);
                for (Microciclo mi : m.microciclos) {
                    TreeItem<Nodo> miItem = new TreeItem<>(mi);
                    for (Sesion s : mi.sesiones) miItem.getChildren().add(new TreeItem<>(s));
                    mItem.getChildren().add(miItem);
                }
                pItem.getChildren().add(mItem);
            }
            rootItem.getChildren().add(pItem);
        }
        tree.setRoot(rootItem);
        rootItem.setExpanded(true);
    }

    private <T> T selected(Class<T> cls) {
        var it = tree.getSelectionModel().getSelectedItem();
        if (it==null) return null;
        var n = it.getValue();
        return cls.isInstance(n) ? cls.cast(n) : null;
    }

    // ======= Modelos =======
    private abstract static class Nodo {
        public final StringProperty nombre = new SimpleStringProperty();
        public final StringProperty tipo = new SimpleStringProperty();
        public final ObjectProperty<LocalDate> inicio = new SimpleObjectProperty<>(null);
        public final ObjectProperty<LocalDate> fin = new SimpleObjectProperty<>(null);
        Nodo(String n, String t){ nombre.set(n); tipo.set(t); }
        public String duracionTexto(){ return ""; }
        public String cargaTexto(){ return ""; }
        public StringProperty nombreProperty(){ return nombre; }
        public StringProperty tipoProperty(){ return tipo; }
        public ObjectProperty<LocalDate> inicioProperty(){ return inicio; }
        public ObjectProperty<LocalDate> finProperty(){ return fin; }
        static Nodo programa(){ return new Nodo("Programa",""){}; }
    }

    private static class Periodo extends Nodo {
        public final IntegerProperty semanas = new SimpleIntegerProperty(4);
        public final ObjectProperty<Color> color = new SimpleObjectProperty<>(color("#3b82f6"));
        public final ObservableList<Mesociclo> mesociclos = FXCollections.observableArrayList();
        Periodo(String n, int sem, Color c){ super(n,"Periodo"); semanas.set(sem); color.set(c); }
        @Override public String duracionTexto(){ return semanas.get()+" sem"; }
    }

    private static class Mesociclo extends Nodo {
        public final IntegerProperty semanas = new SimpleIntegerProperty(4);
        public final ObjectProperty<Color> color = new SimpleObjectProperty<>(color("#60a5fa"));
        public final ObservableList<Microciclo> microciclos = FXCollections.observableArrayList();
        Mesociclo(String n, int sem, Color c){ super(n,"Mesociclo"); semanas.set(sem); color.set(c); }
        @Override public String duracionTexto(){ return semanas.get()+" sem"; }
    }

    private static class Microciclo extends Nodo {
        public final IntegerProperty semanas = new SimpleIntegerProperty(1);
        public final StringProperty tipo = new SimpleStringProperty("Carga");
        public final ObservableList<Sesion> sesiones = FXCollections.observableArrayList();
        Microciclo(String n, int sem, String tipo){ super(n,"Microciclo"); semanas.set(sem); this.tipo.set(tipo); }
        static Microciclo def(String n, int sem){ return new Microciclo(n, sem, "Carga"); }
        @Override public String duracionTexto(){ return semanas.get()+" sem"; }
        @Override public String cargaTexto(){
            int total = sesiones.stream().mapToInt(s -> s.rpe.get()*s.minutos.get()).sum();
            return total>0? ("sRPE "+total):"";
        }
    }

    private static class Sesion extends Nodo {
        public final ObjectProperty<LocalTime> hora = new SimpleObjectProperty<>(LocalTime.of(17,0));
        public final IntegerProperty minutos = new SimpleIntegerProperty(90);
        public final IntegerProperty rpe = new SimpleIntegerProperty(5);         // 0..10
        public final IntegerProperty intensidad = new SimpleIntegerProperty(5);  // 0..10
        Sesion(String n, LocalDate f, LocalTime h, int min, int rpe, int inten){
            super(n,"Sesión"); inicio.set(f); fin.set(f);
            hora.set(h); minutos.set(min); this.rpe.set(rpe); this.intensidad.set(inten);
        }
        @Override public String duracionTexto(){ return minutos.get()+" min"; }
        @Override public String cargaTexto(){ return "RPE "+rpe.get()+" | Int "+intensidad.get(); }
    }

    private static class Evento {
        public final StringProperty nombre = new SimpleStringProperty();
        public final StringProperty tipo = new SimpleStringProperty();
        public final ObjectProperty<LocalDate> fecha = new SimpleObjectProperty<>();
        public final ObjectProperty<Color> color = new SimpleObjectProperty<>(Color.web("#f59e0b"));
        Evento(String n, String t, LocalDate f, Color c){ nombre.set(n); tipo.set(t); fecha.set(f); color.set(c); }
    }

    private record WeekSlot(LocalDate inicio, LocalDate fin) {}

    // ======= Util =======
    private static GridPane gridForm(Object... kv) {
        GridPane gp = new GridPane();
        gp.setHgap(8); gp.setVgap(6); gp.setPadding(new Insets(10));
        for (int i = 0; i < kv.length; i+=2) {
            gp.add(new Label(String.valueOf(kv[i])), 0, i/2);
            if (kv[i+1] instanceof Region r) GridPane.setHgrow(r, Priority.ALWAYS);
            gp.add((javafx.scene.Node) kv[i+1], 1, i/2);
        }
        return gp;
    }

    private static void alertInfo(String msg){ new Alert(Alert.AlertType.INFORMATION, msg).showAndWait(); }
    private static void alertError(String msg){ new Alert(Alert.AlertType.ERROR, msg).showAndWait(); }
    private static String fmt(LocalDate d){ return d==null?"":d.format(FECHA); }
    private static Color color(String hex){ return Color.web(hex); }
    private static Color randomColor(){
        String[] pal = {"#3b82f6","#22c55e","#ef4444","#a855f7","#f59e0b","#06b6d4","#8b5cf6","#10b981"};
        return Color.web(pal[new Random().nextInt(pal.length)]);
    }
    private static Color chipColor(String tipo){
        return switch (tipo==null?"":tipo) {
            case "Introductorio" -> Color.web("#60a5fa");
            case "Carga" -> Color.web("#0ea5e9");
            case "Impacto" -> Color.web("#ef4444");
            case "Recuperación" -> Color.web("#a3e635");
            case "Mantenimiento" -> Color.web("#22c55e");
            case "Pico" -> Color.web("#f43f5e");
            case "Transición" -> Color.web("#94a3b8");
            default -> Color.web("#64748b");
        };
    }
    private static String rgba(Color c){ return rgba(c, c.getOpacity()); }
    private static String rgba(Color c, double a){
        return String.format("rgba(%d,%d,%d,%.3f)", (int)(c.getRed()*255),(int)(c.getGreen()*255),(int)(c.getBlue()*255), a);
    }
    private static String hex(Color c){
        return String.format("#%02x%02x%02x", (int)(c.getRed()*255),(int)(c.getGreen()*255),(int)(c.getBlue()*255));
    }

    private String inlineCss() {
        return """
        .root { -fx-font-size: 12px; }
        .plan-grid { -fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-width: 1; -fx-hgap:0; -fx-vgap:0; }
        .row-title { -fx-font-weight: bold; -fx-padding: 6 8; -fx-background-color: #f8fafc; -fx-border-color: #e5e7eb; -fx-border-width: 0 1 1 0; }
        .span { -fx-alignment: center; -fx-padding: 4 6; -fx-font-weight: bold; }
        """;
    }

    // ======= App Demo =======
    public static class Demo extends Application {
        @Override public void start(Stage stage) {
            PlanGraficoStudio ui = new PlanGraficoStudio();
            Scene sc = new Scene(ui, 1280, 720);
            stage.setTitle("Plan Gráfico – Studio");
            stage.setScene(sc);
            stage.show();
        }
    }
    public static void main(String[] args) { Application.launch(Demo.class, args); }
}

*/
package com.example.plangrafico.ui;

import javafx.application.Application;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/*
public class PlanGraficoStudio extends BorderPane {

    // === Barra superior ===
    private final DatePicker dpInicio = new DatePicker(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
    private final DatePicker dpFin = new DatePicker(LocalDate.now().plusWeeks(12).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
    private final CheckBox cbAutoFin = new CheckBox("Fin automático");
    private final ComboBox<String> cbTipo = new ComboBox<>(FXCollections.observableArrayList("Clásica", "ATR", "Bloques"));

    private final Button btnAddPeriodo = new Button("+ Periodo");
    private final Button btnAddMesociclo = new Button("+ Mesociclo");
    private final Button btnAddMicrociclo = new Button("+ Microciclo");
    private final Button btnAddSesion = new Button("+ Sesión");
    private final Button btnAddEvento = new Button("+ Evento");
    private final Button btnGuardar = new Button("Guardar");
    private final Button btnCargar = new Button("Cargar");
    private final Button btnExportar = new Button("Exportar PNG");

    // === Lateral: TreeView (sin TableView) ===
    private final TreeView<Nodo> tree = new TreeView<>();
    private final TreeItem<Nodo> rootItem = new TreeItem<>(Nodo.programa());

    // === Centro: timeline ===
    private final ScrollPane scroller = new ScrollPane();
    private final GridPane grid = new GridPane();

    // === Datos ===
    private final ObservableList<Periodo> periodos = FXCollections.observableArrayList();
    private final ObservableList<Evento> eventos = FXCollections.observableArrayList();
    private final ObservableList<WeekSlot> semanas = FXCollections.observableArrayList();

    // === Formatos ===
    private static final Locale ES_MX = new Locale("es", "MX");
    private static final DateTimeFormatter MES_FMT = DateTimeFormatter.ofPattern("MMMM yyyy", ES_MX);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy", ES_MX);
    private static final DateTimeFormatter DIA_CORTO = DateTimeFormatter.ofPattern("dd MMM", ES_MX);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    public PlanGraficoStudio() {
        buildUI();
        wire();

        // Semilla de ejemplo
        cbTipo.getSelectionModel().selectFirst();
        cbAutoFin.setSelected(true);
        dpFin.setDisable(true);

        Periodo pPrep = new Periodo("Preparación", 8, color("#3b82f6"));
        Periodo pComp = new Periodo("Competitivo", 4, color("#22c55e"));
        periodos.addAll(pPrep, pComp);

        Mesociclo m1 = new Mesociclo("Base", 4, color("#60a5fa"));
        Mesociclo m2 = new Mesociclo("Específico", 4, color("#f59e0b"));
        pPrep.mesociclos.addAll(m1, m2);

        m1.microciclos.addAll(Microciclo.def("Intro", 1), Microciclo.def("Carga", 1),
                Microciclo.def("Impacto", 1), Microciclo.def("Recup", 1));
        m2.microciclos.addAll(Microciclo.def("Carga", 1), Microciclo.def("Impacto", 1),
                Microciclo.def("Carga", 1), Microciclo.def("Recup", 1));

        Mesociclo mc = new Mesociclo("Competición", 4, color("#34d399"));
        pComp.mesociclos.add(mc);
        mc.microciclos.addAll(Microciclo.def("Pico", 1), Microciclo.def("Mantenimiento", 1),
                Microciclo.def("Mantenimiento", 1), Microciclo.def("Transición", 1));

        rebuildTree();
        recalcAndRedraw();
    }

    private void buildUI() {
        setPadding(new Insets(10));

        ToolBar tb = new ToolBar(
                new Label("Inicio:"), dpInicio,
                new Label("Fin:"), dpFin, cbAutoFin,
                new Separator(), new Label("Periodización:"), cbTipo,
                new Separator(), btnAddPeriodo, btnAddMesociclo, btnAddMicrociclo, btnAddSesion,
                new Separator(), btnAddEvento,
                new Separator(), btnGuardar, btnCargar, btnExportar
        );
        setTop(tb);

        // === TreeView con celdas personalizadas ===
        tree.setRoot(rootItem);
        tree.setShowRoot(false);
        tree.setCellFactory(tv -> new NodoCell());
        VBox left = new VBox(new Label("Estructura del Plan"), tree);
        left.setSpacing(6);
        left.setPadding(new Insets(6, 10, 6, 0));
        left.setPrefWidth(360);
        setLeft(left);

        // === Timeline ===
        grid.getStyleClass().add("plan-grid");
        scroller.setContent(grid);
        scroller.setFitToWidth(true);
        setCenter(scroller);

        // Pie
        Label pie = new Label();
        pie.setPadding(new Insets(6, 0, 0, 0));
        setBottom(new HBox(pie) {{
            setAlignment(Pos.CENTER_RIGHT);
        }});
        pie.textProperty().addListener((o, a, b) -> {}); // solo placeholder
    }

    private void wire() {
        dpInicio.valueProperty().addListener((o, a, b) -> recalcAndRedraw());
        dpFin.valueProperty().addListener((o, a, b) -> { if (!cbAutoFin.isSelected()) recalcAndRedraw(); });
        cbAutoFin.selectedProperty().addListener((o, a, b) -> { dpFin.setDisable(b); recalcAndRedraw(); });

        btnAddPeriodo.setOnAction(e -> {
            Periodo p = new Periodo("Periodo " + (periodos.size() + 1), 4, randomColor());
            periodos.add(p);
            rebuildTree(); recalcAndRedraw();
        });
        btnAddMesociclo.setOnAction(e -> {
            var p = selected(Periodo.class);
            if (p == null) { info("Selecciona un Periodo para agregar un Mesociclo."); return; }
            p.mesociclos.add(new Mesociclo("Mesociclo", 4, randomColor()));
            rebuildTree(); recalcAndRedraw();
        });
        btnAddMicrociclo.setOnAction(e -> {
            var m = selected(Mesociclo.class);
            if (m == null) { info("Selecciona un Mesociclo para agregar un Microciclo."); return; }
            m.microciclos.add(Microciclo.def("Micro", 1));
            rebuildTree(); recalcAndRedraw();
        });
        btnAddSesion.setOnAction(e -> {
            var mi = selected(Microciclo.class);
            if (mi == null) { info("Selecciona un Microciclo para agregar una Sesión."); return; }
            LocalDate f = mi.inicio.get() != null ? mi.inicio.get().with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY)) : dpInicio.getValue();
            mi.sesiones.add(new Sesion("Entrenamiento", f, LocalTime.of(17,0), 90, 5, 5));
            rebuildTree(); recalcAndRedraw();
        });
        btnAddEvento.setOnAction(e -> dialogEvento(null));

        btnExportar.setOnAction(e -> exportPng());
        btnGuardar.setOnAction(e -> info("Stub de guardado: conecta PlanService/DAO aquí."));
        btnCargar.setOnAction(e -> info("Stub de carga: conecta PlanService/DAO aquí."));

        // Doble clic para editar
        tree.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                var it = tree.getSelectionModel().getSelectedItem();
                if (it != null) showEditDialog(it.getValue());
            }
        });
    }

    // === Recalcular fechas y redibujar ===
    private void recalcAndRedraw() {
        LocalDate start = dpInicio.getValue();
        LocalDate cursor = start;

        for (Periodo p : periodos) {
            int wP = Math.max(1, p.semanas.get());
            p.inicio.set(cursor);
            p.fin.set(cursor.plusWeeks(wP).minusDays(1));
            LocalDate cursorM = cursor;
            for (Mesociclo m : p.mesociclos) {
                int wM = Math.max(1, m.semanas.get());
                m.inicio.set(cursorM);
                m.fin.set(cursorM.plusWeeks(wM).minusDays(1));
                LocalDate cursorMi = cursorM;
                for (Microciclo mi : m.microciclos) {
                    int wMi = Math.max(1, mi.semanas.get());
                    mi.inicio.set(cursorMi);
                    mi.fin.set(cursorMi.plusWeeks(wMi).minusDays(1));
                    cursorMi = cursorMi.plusWeeks(wMi);
                }
                cursorM = cursorM.plusWeeks(wM);
            }
            cursor = cursor.plusWeeks(wP);
        }

        // Fin del programa
        LocalDate finAuto = start;
        for (Periodo p : periodos) finAuto = finAuto.plusWeeks(Math.max(1, p.semanas.get()));
        finAuto = finAuto.minusDays(1);
        if (cbAutoFin.isSelected()) dpFin.setValue(finAuto);

        buildWeeks(start, dpFin.getValue());
        redrawGrid();
    }

    private void buildWeeks(LocalDate ini, LocalDate fin) {
        semanas.clear();
        if (ini == null || fin == null || !fin.isAfter(ini)) {
            semanas.add(new WeekSlot(ini, ini != null ? ini.plusDays(6) : LocalDate.now().plusDays(6)));
            return;
        }
        LocalDate s = ini.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        while (!s.isAfter(fin)) {
            semanas.add(new WeekSlot(s, s.plusDays(6)));
            s = s.plusWeeks(1);
        }
    }

    private void redrawGrid() {
        grid.getChildren().clear();
        grid.getColumnConstraints().setAll(new ColumnConstraints(180));
        for (int i = 0; i < semanas.size(); i++) grid.getColumnConstraints().add(new ColumnConstraints(70));

        int row = 0;
        addHeader(row, "Mes");          drawMonthsRow(row++);

        addHeader(row, "Periodos");     drawPeriodosRow(row++);

        addHeader(row, "Mesociclos");   drawMesociclosRow(row++);

        addHeader(row, "Microciclos");  drawMicrociclosRow(row++);

        addHeader(row, "Carga (sRPE)"); drawCargaRow(row++);

        addHeader(row, "Eventos");      drawEventosRow(row);
    }

    private void addHeader(int row, String title) {
        Label l = new Label(title);
        l.setStyle("-fx-font-weight: bold; -fx-padding: 6 8; -fx-background-color: #f8fafc; -fx-border-color:#e5e7eb; -fx-border-width:0 1 1 0;");
        grid.add(l, 0, row);
    }

    // === Dibujo de filas ===
    private void drawMonthsRow(int row) {
        int col = 1; YearMonth current = null; int spanStart = col;
        for (int i=0; i<semanas.size(); i++) {
            YearMonth ym = YearMonth.from(semanas.get(i).inicio);
            if (current == null) current = ym;
            if (!ym.equals(current)) {
                addSpan(row, spanStart, col - 1, current.format(MES_FMT), Color.web("#f1f5f9"), Color.web("#94a3b8"));
                current = ym; spanStart = col;
            }
            col++;
        }
        if (current != null) addSpan(row, spanStart, col - 1, current.format(MES_FMT), Color.web("#f1f5f9"), Color.web("#94a3b8"));
    }

    private void drawPeriodosRow(int row) {
        for (Periodo p : periodos) {
            int c0 = weekToCol(p.inicio.get());
            int span = weeksWithin(p.inicio.get(), p.fin.get());
            addSpan(row, c0, c0 + span - 1, p.nombre.get(), p.color.get().deriveColor(0,1,1,0.25), p.color.get());
        }
    }

    private void drawMesociclosRow(int row) {
        for (Periodo p : periodos) {
            for (Mesociclo m : p.mesociclos) {
                int c0 = weekToCol(m.inicio.get());
                int span = weeksWithin(m.inicio.get(), m.fin.get());
                addSpan(row, c0, c0 + span - 1, m.nombre.get(), m.color.get().deriveColor(0,1,1,0.20), m.color.get());
            }
        }
    }

    private void drawMicrociclosRow(int row) {
        for (Periodo p : periodos) {
            for (Mesociclo m : p.mesociclos) {
                for (Microciclo mi : m.microciclos) {
                    int c0 = weekToCol(mi.inicio.get());
                    int span = weeksWithin(mi.inicio.get(), mi.fin.get());
                    Color c = chipColor(mi.tipo.get());
                    addSpan(row, c0, c0 + span - 1, mi.nombre.get() + " (" + mi.tipo.get() + ")", c.deriveColor(0,1,1,0.18), c);
                }
            }
        }
    }

    private void drawCargaRow(int row) {
        Map<Integer, Integer> carga = new HashMap<>();
        for (Periodo p : periodos)
            for (Mesociclo m : p.mesociclos)
                for (Microciclo mi : m.microciclos)
                    for (Sesion s : mi.sesiones) {
                        if (s.inicio.get() == null) continue;
                        int wk = weekIndexOf(s.inicio.get());
                        int load = Math.max(0, s.rpe.get()) * Math.max(0, s.minutos.get());
                        carga.merge(wk, load, Integer::sum);
                    }

        int max = carga.values().stream().mapToInt(i -> i).max().orElse(1);

        Canvas cv = new Canvas(semanas.size() * 70, 70);
        GraphicsContext g = cv.getGraphicsContext2D();
        g.setFill(Color.gray(0.95));
        g.fillRect(0, 0, cv.getWidth(), cv.getHeight());
        for (int i = 0; i < semanas.size(); i++) {
            int val = carga.getOrDefault(i, 0);
            double h = (val / (double) max) * (cv.getHeight() - 16);
            double x = i * 70 + 20, y = cv.getHeight() - 8 - h;
            g.setFill(Color.web("#0ea5e9")); g.fillRect(x, y, 30, h);
            g.setFill(Color.gray(0.35)); if (val > 0) g.fillText(String.valueOf(val), x, y - 2);
        }
        grid.add(new StackPane(cv), 1, row, semanas.size(), 1);

        cv.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) showSesionesDeSemana((int)(e.getX()/70.0));
        });
    }

    private void drawEventosRow(int row) {
        Map<Integer, List<Evento>> byWeek = eventos.stream()
                .filter(ev -> ev.fecha.get() != null)
                .collect(Collectors.groupingBy(ev -> weekIndexOf(ev.fecha.get())));
        for (int i = 0; i < semanas.size(); i++) {
            VBox box = new VBox(2); box.setAlignment(Pos.CENTER);
            for (Evento ev : byWeek.getOrDefault(i, Collections.emptyList())) {
                Label chip = new Label("● " + ev.nombre.get());
                chip.setStyle("-fx-background-color:"+rgba(ev.color.get(), .20)+"; -fx-background-radius: 10; -fx-padding:2 6 2 6;");
                Tooltip.install(chip, new Tooltip(ev.tipo.get() + " — " + fmt(ev.fecha.get())));
                box.getChildren().add(chip);
            }
            StackPane sp = new StackPane(box);
            int weekIndex = i;
            sp.setOnMouseClicked(e -> { if (e.getClickCount()==2) dialogEvento(weekIndex); });
            grid.add(sp, i + 1, row);
        }
    }

    // === Helpers de timeline ===
    private void addSpan(int row, int startCol, int endCol, String text, Color bg, Color stroke) {
        int span = Math.max(1, endCol - startCol + 1);
        StackPane sp = new StackPane(new Label(text));
        sp.setStyle("-fx-background-color:"+rgba(bg)+"; -fx-border-color:"+hex(stroke)+"; -fx-border-width:1; -fx-background-radius:6; -fx-border-radius:6;");
        sp.setPadding(new Insets(4,6,4,6));
        grid.add(sp, startCol, row, span, 1);
    }
    private int weekToCol(LocalDate date) { return 1 + Math.max(0, weekIndexOf(date)); }
    private int weekIndexOf(LocalDate date) {
        for (int i = 0; i < semanas.size(); i++) {
            var w = semanas.get(i);
            if ((date.isEqual(w.inicio) || date.isAfter(w.inicio)) && (date.isEqual(w.fin) || date.isBefore(w.fin))) return i;
        }
        return 0;
    }
    private int weeksWithin(LocalDate a, LocalDate b) {
        if (a == null || b == null || b.isBefore(a)) return 1;
        return Math.max(1, weekIndexOf(b) - weekIndexOf(a) + 1);
    }

    // === Diálogos / edición ===
    private void dialogEvento(Integer wkIndexOrNull) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Nuevo evento");
        var nombre = new TextField();
        var tipo = new ComboBox<String>(FXCollections.observableArrayList("Partido","Amistoso","Torneo","Prueba física","Otro"));
        tipo.getSelectionModel().selectFirst();
        var fecha = new DatePicker(wkIndexOrNull==null ? dpInicio.getValue() : semanas.get(wkIndexOrNull).inicio);
        var color = new ColorPicker(Color.web("#f59e0b"));
        GridPane gp = form("Nombre:", nombre, "Tipo:", tipo, "Fecha:", fecha, "Color:", color);
        d.getDialogPane().setContent(gp);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(bt -> {
            if (bt==ButtonType.OK && !nombre.getText().isBlank()) {
                eventos.add(new Evento(nombre.getText(), tipo.getValue(), fecha.getValue(), color.getValue()));
                recalcAndRedraw();
            }
            return bt;
        });
        d.showAndWait();
    }

    private void showEditDialog(Nodo n) {
        if (n instanceof Periodo p) {
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Editar Periodo");
            var nombre = new TextField(p.nombre.get());
            var spSem = new Spinner<>(1, 52, p.semanas.get());
            var cp = new ColorPicker(p.color.get());
            d.getDialogPane().setContent(form("Nombre:", nombre, "Semanas:", spSem, "Color:", cp));
            d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            d.setResultConverter(bt -> {
                if (bt==ButtonType.OK){ p.nombre.set(nombre.getText()); p.semanas.set((Integer) spSem.getValue()); p.color.set(cp.getValue()); rebuildTree(); recalcAndRedraw(); }
                return bt;
            });
            d.showAndWait();
        } else if (n instanceof Mesociclo m) {
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Editar Mesociclo");
            var nombre = new TextField(m.nombre.get());
            var spSem = new Spinner<>(1, 52, m.semanas.get());
            var cp = new ColorPicker(m.color.get());
            d.getDialogPane().setContent(form("Nombre:", nombre, "Semanas:", spSem, "Color:", cp));
            d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            d.setResultConverter(bt -> {
                if (bt==ButtonType.OK){ m.nombre.set(nombre.getText()); m.semanas.set((Integer) spSem.getValue()); m.color.set(cp.getValue()); rebuildTree(); recalcAndRedraw(); }
                return bt;
            });
            d.showAndWait();
        } else if (n instanceof Microciclo mi) {
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Editar Microciclo");
            var nombre = new TextField(mi.nombre.get());
            var tipo = new ComboBox<String>(FXCollections.observableArrayList("Introductorio","Carga","Impacto","Recuperación","Mantenimiento","Pico","Transición"));
            tipo.getSelectionModel().select(mi.tipo.get());
            var spSem = new Spinner<>(1, 8, mi.semanas.get());
            d.getDialogPane().setContent(form("Nombre:", nombre, "Tipo:", tipo, "Semanas:", spSem));
            d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            d.setResultConverter(bt -> {
                if (bt==ButtonType.OK){ mi.nombre.set(nombre.getText()); mi.tipo.set(tipo.getValue()); mi.semanas.set((Integer) spSem.getValue()); rebuildTree(); recalcAndRedraw(); }
                return bt;
            });
            d.showAndWait();
        } else if (n instanceof Sesion s) {
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Editar Sesión");
            var nombre = new TextField(s.nombre.get());
            var fecha = new DatePicker(s.inicio.get());
            var hora = new Spinner<>(0, 23, s.hora.get().getHour());
            var min = new Spinner<>(0, 59, s.hora.get().getMinute());
            var dur = new Spinner<>(10, 240, s.minutos.get(), 5);
            var rpe = new Spinner<>(0, 10, s.rpe.get());
            var inten = new Spinner<>(0, 10, s.intensidad.get());
            d.getDialogPane().setContent(form(
                    "Nombre:", nombre,
                    "Fecha:", fecha,
                    "Hora (h:m):", new HBox(6, new Label("h:"), hora, new Label("m:"), min),
                    "Minutos:", dur,
                    "RPE:", rpe,
                    "Intensidad:", inten
            ));
            d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            d.setResultConverter(bt -> {
                if (bt==ButtonType.OK){
                    s.nombre.set(nombre.getText());
                    s.inicio.set(fecha.getValue());
                    s.hora.set(LocalTime.of((Integer) hora.getValue(), (Integer) min.getValue()));
                    s.minutos.set((Integer) dur.getValue());
                    s.rpe.set((Integer) rpe.getValue());
                    s.intensidad.set((Integer) inten.getValue());
                    rebuildTree(); recalcAndRedraw();
                }
                return bt;
            });
            d.showAndWait();
        }
    }

    private void showSesionesDeSemana(int weekIndex) {
        if (weekIndex < 0 || weekIndex >= semanas.size()) return;
        LocalDate ini = semanas.get(weekIndex).inicio, fin = semanas.get(weekIndex).fin;
        List<Sesion> list = new ArrayList<>();
        for (Periodo p: periodos) for (Mesociclo m: p.mesociclos) for (Microciclo mi: m.microciclos)
            for (Sesion s: mi.sesiones)
                if (s.inicio.get()!=null && !s.inicio.get().isBefore(ini) && !s.inicio.get().isAfter(fin)) list.add(s);

        if (list.isEmpty()) { info("No hay sesiones en esa semana."); return; }
        String text = list.stream().map(s -> "- " + s.nombre.get() + " | " + fmt(s.inicio.get()) + " " +
                        s.hora.get().format(HORA) + " | " + s.minutos.get()+" min | RPE "+s.rpe.get()+" → sRPE "+(s.rpe.get()*s.minutos.get()))
                .collect(Collectors.joining("\n"));
        info("Sesiones ("+ini.format(FECHA)+" - "+fin.format(FECHA)+"):\n\n"+text);
    }

    // === Tree ===
    private void rebuildTree() {
        rootItem.getChildren().clear();
        for (Periodo p : periodos) {
            TreeItem<Nodo> pItem = new TreeItem<>(p);
            for (Mesociclo m : p.mesociclos) {
                TreeItem<Nodo> mItem = new TreeItem<>(m);
                for (Microciclo mi : m.microciclos) {
                    TreeItem<Nodo> miItem = new TreeItem<>(mi);
                    for (Sesion s : mi.sesiones) miItem.getChildren().add(new TreeItem<>(s));
                    mItem.getChildren().add(miItem);
                }
                pItem.getChildren().add(mItem);
            }
            rootItem.getChildren().add(pItem);
        }
        rootItem.setExpanded(true);
        tree.setRoot(rootItem);
        expandAll(rootItem);
    }
    private void expandAll(TreeItem<?> item) {
        item.setExpanded(true);
        for (TreeItem<?> c : item.getChildren()) expandAll(c);
    }

    private <T> T selected(Class<T> cls) {
        var it = tree.getSelectionModel().getSelectedItem();
        if (it == null) return null;
        var n = it.getValue();
        return cls.isInstance(n) ? cls.cast(n) : null;
    }

    // === Modelos ===
    private abstract static class Nodo {
        public final StringProperty nombre = new SimpleStringProperty();
        public final StringProperty tipo = new SimpleStringProperty();
        public final ObjectProperty<LocalDate> inicio = new SimpleObjectProperty<>(null);
        public final ObjectProperty<LocalDate> fin = new SimpleObjectProperty<>(null);
        Nodo(String n, String t){ nombre.set(n); tipo.set(t); }
        public String duracionTexto(){ return ""; }
        public String cargaTexto(){ return ""; }
        static Nodo programa(){ return new Nodo("Programa",""){}; }
    }
    private static class Periodo extends Nodo {
        public final IntegerProperty semanas = new SimpleIntegerProperty(4);
        public final ObjectProperty<Color> color = new SimpleObjectProperty<>(color("#3b82f6"));
        public final ObservableList<Mesociclo> mesociclos = FXCollections.observableArrayList();
        Periodo(String n, int sem, Color c){ super(n,"Periodo"); semanas.set(sem); color.set(c); }
        @Override public String duracionTexto(){ return semanas.get()+" sem"; }
    }
    private static class Mesociclo extends Nodo {
        public final IntegerProperty semanas = new SimpleIntegerProperty(4);
        public final ObjectProperty<Color> color = new SimpleObjectProperty<>(color("#60a5fa"));
        public final ObservableList<Microciclo> microciclos = FXCollections.observableArrayList();
        Mesociclo(String n, int sem, Color c){ super(n,"Mesociclo"); semanas.set(sem); color.set(c); }
        @Override public String duracionTexto(){ return semanas.get()+" sem"; }
    }
    private static class Microciclo extends Nodo {
        public final IntegerProperty semanas = new SimpleIntegerProperty(1);
        public final StringProperty tipo = new SimpleStringProperty("Carga");
        public final ObservableList<Sesion> sesiones = FXCollections.observableArrayList();
        Microciclo(String n, int sem, String tipo){ super(n,"Microciclo"); semanas.set(sem); this.tipo.set(tipo); }
        static Microciclo def(String n, int sem){ return new Microciclo(n, sem, "Carga"); }
        @Override public String duracionTexto(){ return semanas.get()+" sem"; }
        @Override public String cargaTexto(){
            int total = sesiones.stream().mapToInt(s -> s.rpe.get()*s.minutos.get()).sum();
            return total>0? ("sRPE "+total):"";
        }
    }
    private static class Sesion extends Nodo {
        public final ObjectProperty<LocalTime> hora = new SimpleObjectProperty<>(LocalTime.of(17,0));
        public final IntegerProperty minutos = new SimpleIntegerProperty(90);
        public final IntegerProperty rpe = new SimpleIntegerProperty(5);
        public final IntegerProperty intensidad = new SimpleIntegerProperty(5);
        Sesion(String n, LocalDate f, LocalTime h, int min, int rpe, int inten){
            super(n,"Sesión"); inicio.set(f); fin.set(f);
            hora.set(h); minutos.set(min); this.rpe.set(rpe); this.intensidad.set(inten);
        }
        @Override public String duracionTexto(){ return minutos.get()+" min"; }
        @Override public String cargaTexto(){ return "RPE "+rpe.get()+" | Int "+intensidad.get(); }
    }
    private static class Evento {
        public final StringProperty nombre = new SimpleStringProperty();
        public final StringProperty tipo = new SimpleStringProperty();
        public final ObjectProperty<LocalDate> fecha = new SimpleObjectProperty<>();
        public final ObjectProperty<Color> color = new SimpleObjectProperty<>(Color.web("#f59e0b"));
        Evento(String n, String t, LocalDate f, Color c){ nombre.set(n); tipo.set(t); fecha.set(f); color.set(c); }
    }
    private record WeekSlot(LocalDate inicio, LocalDate fin) {}

    // === Util ===
    private static GridPane form(Object... kv) {
        GridPane gp = new GridPane();
        gp.setHgap(8); gp.setVgap(6); gp.setPadding(new Insets(10));
        for (int i = 0; i < kv.length; i+=2) {
            gp.add(new Label(String.valueOf(kv[i])), 0, i/2);
            gp.add((javafx.scene.Node) kv[i+1], 1, i/2);
            GridPane.setHgrow((javafx.scene.Node) kv[i+1], Priority.ALWAYS);
        }
        return gp;
    }
    private static void info(String msg){ new Alert(Alert.AlertType.INFORMATION, msg).showAndWait(); }
    private static String fmt(LocalDate d){ return d==null?"":d.format(FECHA); }
    private static Color color(String hex){ return Color.web(hex); }
    private static Color randomColor(){
        String[] pal = {"#3b82f6","#22c55e","#ef4444","#a855f7","#f59e0b","#06b6d4","#8b5cf6","#10b981"};
        return Color.web(pal[new Random().nextInt(pal.length)]);
    }
    private static Color chipColor(String tipo){
        return switch (tipo==null?"":tipo) {
            case "Introductorio" -> Color.web("#60a5fa");
            case "Carga" -> Color.web("#0ea5e9");
            case "Impacto" -> Color.web("#ef4444");
            case "Recuperación" -> Color.web("#a3e635");
            case "Mantenimiento" -> Color.web("#22c55e");
            case "Pico" -> Color.web("#f43f5e");
            case "Transición" -> Color.web("#94a3b8");
            default -> Color.web("#64748b");
        };
    }
    private static String rgba(Color c){ return rgba(c, c.getOpacity()); }
    private static String rgba(Color c, double a){
        return String.format("rgba(%d,%d,%d,%.3f)", (int)(c.getRed()*255),(int)(c.getGreen()*255),(int)(c.getBlue()*255), a);
    }
    private static String hex(Color c){
        return String.format("#%02x%02x%02x", (int)(c.getRed()*255),(int)(c.getGreen()*255),(int)(c.getBlue()*255));
    }

    // === Celda personalizada del TreeView ===
    private final class NodoCell extends TreeCell<Nodo> {
        private final Label lblNombre = new Label();
        private final Label lblMeta = new Label();
        private final VBox box = new VBox(2, lblNombre, lblMeta);

        NodoCell() {
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            setPadding(new Insets(2,6,2,6));
            lblNombre.setStyle("-fx-font-weight: bold;");
            lblMeta.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px;");

            // Menú contextual: Editar / Eliminar
            MenuItem editar = new MenuItem("Editar");
            editar.setOnAction(e -> { if (getItem()!=null) showEditDialog(getItem()); });
            MenuItem eliminar = new MenuItem("Eliminar");
            eliminar.setOnAction(e -> {
                TreeItem<Nodo> ti = getTreeItem();
                if (ti==null || ti.getParent()==null) return;
                Nodo n = ti.getValue();
                if (n instanceof Periodo p) periodos.remove(p);
                else if (n instanceof Mesociclo m) ((Periodo)ti.getParent().getValue()).mesociclos.remove(m);
                else if (n instanceof Microciclo mi) ((Mesociclo)ti.getParent().getValue()).microciclos.remove(mi);
                else if (n instanceof Sesion s) ((Microciclo)ti.getParent().getValue()).sesiones.remove(s);
                rebuildTree(); recalcAndRedraw();
            });
            setContextMenu(new ContextMenu(editar, eliminar));
        }

        @Override protected void updateItem(Nodo n, boolean empty) {
            super.updateItem(n, empty);
            if (empty || n == null || n.tipo.get().isBlank()) {
                setGraphic(null);
                return;
            }
            lblNombre.setText(n.nombre.get() + "  •  " + n.tipo.get());
            String meta = n.duracionTexto();
            String fechas = (n.inicio.get()!=null? fmt(n.inicio.get()):"") +
                    (n.fin.get()!=null? " → "+fmt(n.fin.get()):"");
            String carga = n.cargaTexto();
            lblMeta.setText(
                    (meta.isBlank()?"":meta) +
                            (fechas.isBlank()?"": (meta.isBlank()?"": " | ") + fechas) +
                            (carga.isBlank()?"": " | " + carga)
            );
            setGraphic(box);
        }
    }

    // === Exportar PNG del timeline ===
    private void exportPng() {
        WritableImage img = grid.snapshot(new SnapshotParameters(), null);
        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar Plan Gráfico");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
        fc.setInitialFileName("plan_grafico.png");
        File f = fc.showSaveDialog(getScene().getWindow());
        if (f != null) {
            try {
                javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(img, null), "png", f);
                info("Exportado a:\n" + f.getAbsolutePath());
            } catch (Exception ex) { info("No se pudo exportar:\n" + ex.getMessage()); }
        }
    }

    // === App demo ===
    public static class Demo extends Application {
        @Override public void start(Stage stage) {
            PlanGraficoStudio ui = new PlanGraficoStudio();
            Scene sc = new Scene(ui, 1280, 720);
            stage.setTitle("Plan Gráfico – Studio");
            stage.setScene(sc);
            stage.show();
        }
    }
    public static void main(String[] args) { Application.launch(Demo.class, args); }
}
*/


import javafx.application.Application;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PlanGraficoStudio – Editor con RENDERER (Canvas) que se va dibujando en tiempo real.
 *
 * Permite: elegir inicio/fin, crear Periodos/Mesociclos/Microciclos con duración (semanas),
 * crear Sesiones (fecha/hora/min/RPE) y Eventos (controles A/F/T/PS/M y competencias CP/CF).
 * Calcula la carga semanal (sRPE = minutos * RPE) y la dibuja como barras.
 */
public class PlanGraficoStudio extends BorderPane {

    // ===================== MODELO =====================
    private abstract static class Nodo {
        final StringProperty nombre = new SimpleStringProperty("");
        final StringProperty tipo = new SimpleStringProperty("");
        final ObjectProperty<LocalDate> inicio = new SimpleObjectProperty<>(null);
        final ObjectProperty<LocalDate> fin = new SimpleObjectProperty<>(null);
        Nodo(String n, String t){ nombre.set(n); tipo.set(t); }
        String duracionTexto(){ return ""; }
    }
    private static class Periodo extends Nodo {
        final IntegerProperty semanas = new SimpleIntegerProperty(4);
        final ObjectProperty<Color> color = new SimpleObjectProperty<>(Color.web("#3b82f6"));
        final ObservableList<Mesociclo> mesociclos = FXCollections.observableArrayList();
        Periodo(String n, int sem, Color c){ super(n, "Periodo"); semanas.set(sem); color.set(c); }
        @Override String duracionTexto(){ return semanas.get()+" sem"; }
    }
    private static class Mesociclo extends Nodo {
        final IntegerProperty semanas = new SimpleIntegerProperty(4);
        final ObjectProperty<Color> color = new SimpleObjectProperty<>(Color.web("#60a5fa"));
        final ObservableList<Microciclo> microciclos = FXCollections.observableArrayList();
        Mesociclo(String n, int sem, Color c){ super(n, "Mesociclo"); semanas.set(sem); color.set(c); }
        @Override String duracionTexto(){ return semanas.get()+" sem"; }
    }
    private static class Microciclo extends Nodo {
        final IntegerProperty semanas = new SimpleIntegerProperty(1);
        final StringProperty fase = new SimpleStringProperty("Carga"); // Introductorio, Carga, Impacto, Recuperación, Mantenimiento, Pico, Transición
        final ObservableList<Sesion> sesiones = FXCollections.observableArrayList();
        Microciclo(String n, int sem, String fase){ super(n, "Microciclo"); semanas.set(sem); this.fase.set(fase); }
        @Override String duracionTexto(){ return semanas.get()+" sem"; }
    }
    private static class Sesion extends Nodo {
        final ObjectProperty<LocalTime> hora = new SimpleObjectProperty<>(LocalTime.of(17,0));
        final IntegerProperty minutos = new SimpleIntegerProperty(90);
        final IntegerProperty rpe = new SimpleIntegerProperty(5);
        final IntegerProperty intensidad = new SimpleIntegerProperty(5);
        Sesion(String n, LocalDate f, LocalTime h, int min, int rpe, int inten){
            super(n, "Sesión"); inicio.set(f); fin.set(f);
            hora.set(h); minutos.set(min); this.rpe.set(rpe); this.intensidad.set(inten);
        }
    }
    private static class Evento {
        final StringProperty nombre = new SimpleStringProperty("");
        final StringProperty tipo = new SimpleStringProperty("CP"); // A,F,T,PS,M, CP, CF
        final ObjectProperty<LocalDate> fecha = new SimpleObjectProperty<>();
        final ObjectProperty<Color> color = new SimpleObjectProperty<>(Color.web("#f59e0b"));
        Evento(String n, String t, LocalDate f, Color c){ nombre.set(n); tipo.set(t); fecha.set(f); color.set(c); }
    }
    private record WeekSlot(LocalDate inicio, LocalDate fin) {}

    // ===================== UI =====================
    private final DatePicker dpInicio = new DatePicker(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
    private final DatePicker dpFin = new DatePicker(LocalDate.now().plusWeeks(16).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
    private final CheckBox cbAutoFin = new CheckBox("Fin automático");
    private final ComboBox<String> cbPeriodizacion = new ComboBox<>(FXCollections.observableArrayList("Clásica", "ATR", "Bloques"));

    private final Button btnAddPeriodo = new Button("+ Periodo");
    private final Button btnAddMesociclo = new Button("+ Mesociclo");
    private final Button btnAddMicrociclo = new Button("+ Microciclo");
    private final Button btnAddSesion = new Button("+ Sesión");
    private final Button btnAddEvento = new Button("+ Evento");
    private final Button btnExportPNG = new Button("Exportar PNG");

    private final TreeView<Nodo> tree = new TreeView<>();
    private final TreeItem<Nodo> rootItem = new TreeItem<>(new Nodo("Programa",""){});

    private final RendererCanvas renderer = new RendererCanvas();

    private final ObservableList<Periodo> periodos = FXCollections.observableArrayList();
    private final ObservableList<Evento> eventos = FXCollections.observableArrayList();
    private final ObservableList<WeekSlot> semanas = FXCollections.observableArrayList();

    private static final DateTimeFormatter MES_FMT = DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("es","ES"));

    public PlanGraficoStudio(){
        buildTop();
        buildLeft();
        setCenter(new StackPane(renderer){ { setPadding(new Insets(6)); } });

        cbAutoFin.setSelected(true); dpFin.setDisable(true); cbPeriodizacion.getSelectionModel().selectFirst();

        // datos semilla de ejemplo
        Periodo pPrep = new Periodo("Preparación", 8, Color.web("#3b82f6"));
        Periodo pComp = new Periodo("Competitivo", 4, Color.web("#22c55e"));
        periodos.addAll(pPrep, pComp);
        Mesociclo m1 = new Mesociclo("Base", 4, Color.web("#60a5fa"));
        Mesociclo m2 = new Mesociclo("Específico", 4, Color.web("#f59e0b"));
        pPrep.mesociclos.addAll(m1, m2);
        m1.microciclos.addAll(new Microciclo("Intro",1,"Introductorio"), new Microciclo("Carga",1,"Carga"), new Microciclo("Impacto",1,"Impacto"), new Microciclo("Recup",1,"Recuperación"));
        m2.microciclos.addAll(new Microciclo("Carga",1,"Carga"), new Microciclo("Impacto",1,"Impacto"), new Microciclo("Carga",1,"Carga"), new Microciclo("Recup",1,"Recuperación"));
        Mesociclo mc = new Mesociclo("Competición", 4, Color.web("#34d399")); pComp.mesociclos.add(mc);
        mc.microciclos.addAll(new Microciclo("Pico",1,"Pico"), new Microciclo("Mant",1,"Mantenimiento"), new Microciclo("Mant",1,"Mantenimiento"), new Microciclo("Trans",1,"Transición"));

        rebuildTree();
        recalcAndRender();
        wire();
    }

    private void buildTop(){
        ToolBar tb = new ToolBar(
                new Label("Inicio:"), dpInicio,
                new Label("Fin:"), dpFin, cbAutoFin,
                new Separator(), new Label("Periodización:"), cbPeriodizacion,
                new Separator(), btnAddPeriodo, btnAddMesociclo, btnAddMicrociclo, btnAddSesion,
                new Separator(), btnAddEvento,
                new Separator(), btnExportPNG
        );
        setTop(tb);
    }
    private void buildLeft(){
        tree.setRoot(rootItem); tree.setShowRoot(false); tree.setCellFactory(tv -> new NodoCell());
        VBox left = new VBox(new Label("Estructura del Plan"), tree); left.setSpacing(6); left.setPadding(new Insets(6,10,6,0)); left.setPrefWidth(340);
        setLeft(left);
    }

    private void wire(){
        dpInicio.valueProperty().addListener((o,a,b)-> recalcAndRender());
        dpFin.valueProperty().addListener((o,a,b)-> { if(!cbAutoFin.isSelected()) recalcAndRender(); });
        cbAutoFin.selectedProperty().addListener((o,a,b)-> { dpFin.setDisable(b); recalcAndRender(); });

        btnAddPeriodo.setOnAction(e->{ Periodo p = new Periodo("Periodo "+(periodos.size()+1), 4, randomColor()); periodos.add(p); rebuildTree(); recalcAndRender();});
        btnAddMesociclo.setOnAction(e->{ Periodo p = selected(Periodo.class); if(p==null){ info("Selecciona un Periodo."); return;} p.mesociclos.add(new Mesociclo("Mesociclo",4,randomColor())); rebuildTree(); recalcAndRender();});
        btnAddMicrociclo.setOnAction(e->{ Mesociclo m = selected(Mesociclo.class); if(m==null){ info("Selecciona un Mesociclo."); return;} m.microciclos.add(new Microciclo("Micro",1,"Carga")); rebuildTree(); recalcAndRender();});
        btnAddSesion.setOnAction(e->{ Microciclo mi = selected(Microciclo.class); if(mi==null){ info("Selecciona un Microciclo."); return;} LocalDate f = mi.inicio.get()!=null?mi.inicio.get().with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY)):dpInicio.getValue(); mi.sesiones.add(new Sesion("Entrenamiento", f, LocalTime.of(17,0), 90, 5, 5)); recalcAndRender();});
        btnAddEvento.setOnAction(e->{ dialogEvento(null); });
        btnExportPNG.setOnAction(e->{ exportPng(); });

        tree.setOnMouseClicked(ev->{ if(ev.getClickCount()==2){ var it = tree.getSelectionModel().getSelectedItem(); if(it!=null) edit(it.getValue()); }});
    }

    // ===================== CÁLCULO DE FECHAS Y RENDER =====================
    private void recalcAndRender(){
        LocalDate start = dpInicio.getValue();
        // encadenar periodos/mesos/micros por semanas
        LocalDate cursor = start;
        for (Periodo p: periodos){
            int wP = Math.max(1, p.semanas.get());
            p.inicio.set(cursor); p.fin.set(cursor.plusWeeks(wP).minusDays(1));
            LocalDate cM = cursor;
            for (Mesociclo m: p.mesociclos){
                int wM = Math.max(1, m.semanas.get());
                m.inicio.set(cM); m.fin.set(cM.plusWeeks(wM).minusDays(1));
                LocalDate cMi = cM;
                for (Microciclo mi: m.microciclos){
                    int wMi = Math.max(1, mi.semanas.get());
                    mi.inicio.set(cMi); mi.fin.set(cMi.plusWeeks(wMi).minusDays(1));
                    cMi = cMi.plusWeeks(wMi);
                }
                cM = cM.plusWeeks(wM);
            }
            cursor = cursor.plusWeeks(wP);
        }
        // fin automático
        LocalDate finAuto = start; for (Periodo p: periodos) finAuto = finAuto.plusWeeks(Math.max(1,p.semanas.get())); finAuto = finAuto.minusDays(1);
        if (cbAutoFin.isSelected()) dpFin.setValue(finAuto);

        buildWeeks(start, dpFin.getValue());
        renderer.render();
    }

    private void buildWeeks(LocalDate ini, LocalDate fin){
        semanas.clear();
        LocalDate s = ini.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        while(!s.isAfter(fin)){ semanas.add(new WeekSlot(s, s.plusDays(6))); s = s.plusWeeks(1); }
    }

    // ===================== RENDERER (Canvas) =====================
    private final class RendererCanvas extends Region {
        private final Canvas canvas = new Canvas(1200, 520);
        private final double cellW = 70, rowH = 32, labelW = 180;
        private final Font labelFont = Font.font(12);
        private final Font smallFont = Font.font(11);
        RendererCanvas(){ getChildren().add(canvas); widthProperty().addListener((o,a,b)-> layoutChildren()); heightProperty().addListener((o,a,b)-> layoutChildren()); }
        @Override protected void layoutChildren(){ canvas.setWidth(Math.max(getWidth()-12, 600)); canvas.setHeight(Math.max(getHeight()-12, 420)); render(); }

        void render(){
            GraphicsContext g = canvas.getGraphicsContext2D();
            g.setFill(Color.WHITE); g.fillRect(0,0, canvas.getWidth(), canvas.getHeight());
            int cols = semanas.size();
            int rows = 7; // Mes, Periodos, Mesos, Micros, Carga, Eventos, separador
            double contentW = labelW + cols*cellW;
            canvas.setWidth(Math.max(contentW+40, canvas.getWidth()));

            int r = 0; drawMonthsRow(g, r++); drawPeriodsRow(g, r++); drawMesosRow(g, r++); drawMicrosRow(g, r++); drawCargaRow(g, r++); drawEventosRow(g, r++);
            // grid vertical
            g.setStroke(Color.web("#e5e7eb")); g.setLineWidth(1);
            for (int i=0; i<=cols; i++){ double x = labelW + i*cellW; g.strokeLine(x, 0, x, rows*rowH + 180); }
        }

        private void drawLabel(GraphicsContext g, int row, String text){
            double y = row*rowH + 22; g.setFill(Color.web("#0f172a")); g.setFont(labelFont); g.fillText(text, 8, y);
        }
        private void drawSpan(GraphicsContext g, int row, int col1, int col2, String text, Color bg, Color stroke){
            double x = labelW + col1*cellW + 4, w = Math.max(cellW-8, (col2-col1+1)*cellW-8); double y = row*rowH + 6, h = rowH-12;
            g.setFill(bg); g.fillRoundRect(x,y,w,h,10,10); g.setStroke(stroke); g.strokeRoundRect(x,y,w,h,10,10);
            g.setFill(Color.web("#0f172a")); g.setFont(smallFont); g.fillText(text, x+6, y+h/2+4);
        }

        private void drawMonthsRow(GraphicsContext g, int row){
            drawLabel(g,row,"Mes");
            if (semanas.isEmpty()) return; int col=0; YearMonth cur=null; int start=0;
            for (int i=0;i<semanas.size();i++){
                YearMonth ym = YearMonth.from(semanas.get(i).inicio());
                if (cur==null) cur=ym;
                if (!ym.equals(cur)) { drawSpan(g,row,start,i-1, MES_FMT.format(cur), Color.web("#f1f5f9"), Color.web("#94a3b8")); cur=ym; start=i; }
            }
            drawSpan(g,row,start,semanas.size()-1, MES_FMT.format(cur), Color.web("#f1f5f9"), Color.web("#94a3b8"));
            for (int i=0;i<semanas.size();i++){
                String d = semanas.get(i).inicio().format(DateTimeFormatter.ofPattern("dd/MM"));
                g.setFill(Color.web("#475569")); g.fillText(d, labelW + i*cellW + 20, row*rowH + rowH + 10);
            }
        }

        private void drawPeriodsRow(GraphicsContext g, int row){ drawLabel(g,row,"Periodos"); for (Periodo p: periodos) drawSpan(g,row, weekIndex(p.inicio.get()), weekIndex(p.fin.get()), p.nombre.get(), p.color.get().deriveColor(0,1,1,0.25), p.color.get()); }
        private void drawMesosRow(GraphicsContext g, int row){ drawLabel(g,row,"Mesociclos"); for (Periodo p: periodos) for (Mesociclo m: p.mesociclos) drawSpan(g,row, weekIndex(m.inicio.get()), weekIndex(m.fin.get()), m.nombre.get(), m.color.get().deriveColor(0,1,1,0.20), m.color.get()); }
        private void drawMicrosRow(GraphicsContext g, int row){ drawLabel(g,row,"Microciclos"); for (Periodo p: periodos) for(Mesociclo m:p.mesociclos) for (Microciclo mi: m.microciclos){ Color c = microColor(mi.fase.get()); drawSpan(g,row, weekIndex(mi.inicio.get()), weekIndex(mi.fin.get()), mi.nombre.get()+" ("+mi.fase.get()+")", c.deriveColor(0,1,1,0.18), c);} }
        private void drawCargaRow(GraphicsContext g, int row){ drawLabel(g,row, "Carga (sRPE)");
            Map<Integer,Integer> carga = new HashMap<>();
            for (Periodo p: periodos) for (Mesociclo m: p.mesociclos) for (Microciclo mi: m.microciclos) for (Sesion s: mi.sesiones){ if (s.inicio.get()==null) continue; int wi = weekIndex(s.inicio.get()); int val = Math.max(0,s.rpe.get())*Math.max(0,s.minutos.get()); carga.merge(wi,val,Integer::sum);} int max = carga.values().stream().mapToInt(i->i).max().orElse(1);
            for (int i=0;i<semanas.size();i++){ int v = carga.getOrDefault(i,0); double h = (v/(double)max)*(rowH-10); double x=labelW + i*cellW + 22; double y = row*rowH + rowH - 6 - h; g.setFill(Color.web("#0ea5e9")); g.fillRect(x,y,26,h); if (v>0){ g.setFill(Color.web("#334155")); g.fillText(String.valueOf(v), x, y-2); } }
            // doble clic para ver sesiones de la semana
            canvas.setOnMouseClicked(e->{ if (e.getClickCount()==2){ int col = (int)((e.getX()-labelW)/cellW); if (col>=0 && col<semanas.size()){ showSesiones(col); } }});
        }
        private void drawEventosRow(GraphicsContext g, int row){ drawLabel(g,row, "Eventos");
            for (int i=0;i<semanas.size();i++){
                LocalDate ini = semanas.get(i).inicio(), fin = semanas.get(i).fin();
                List<Evento> es = eventos.stream().filter(ev-> ev.fecha.get()!=null && !ev.fecha.get().isBefore(ini) && !ev.fecha.get().isAfter(fin)).collect(Collectors.toList());
                double x = labelW + i*cellW + 6, y = row*rowH + 8;
                for (Evento ev: es){ g.setFill(ev.color.get().deriveColor(0,1,1,0.18)); g.fillRoundRect(x,y,cellW-14, rowH-16, 10,10); g.setStroke(ev.color.get()); g.strokeRoundRect(x,y,cellW-14,rowH-16,10,10); g.setFill(Color.web("#0f172a")); g.setFont(smallFont); g.fillText(ev.tipo.get()+": "+ev.nombre.get(), x+4, y+16); y += rowH-8; }
            }
        }

        private int weekIndex(LocalDate date){ if (date==null) return 0; for (int i=0;i<semanas.size();i++){ var w = semanas.get(i); if ((date.isEqual(w.inicio()) || date.isAfter(w.inicio())) && (date.isEqual(w.fin()) || date.isBefore(w.fin()))) return i; } return Math.max(0, (int)Duration.between(semanas.get(0).inicio().atStartOfDay(), date.atStartOfDay()).toDays()/7); }
        private Color microColor(String tipo){ return switch (tipo==null?"":tipo){ case "Introductorio" -> Color.web("#60a5fa"); case "Carga" -> Color.web("#0ea5e9"); case "Impacto" -> Color.web("#ef4444"); case "Recuperación" -> Color.web("#22c55e"); case "Mantenimiento" -> Color.web("#10b981"); case "Pico" -> Color.web("#f43f5e"); case "Transición" -> Color.web("#94a3b8"); default -> Color.web("#64748b"); }; }
    }

    private void showSesiones(int weekIndex){
        if (weekIndex<0 || weekIndex>=semanas.size()) return; LocalDate ini = semanas.get(weekIndex).inicio(), fin = semanas.get(weekIndex).fin();
        List<Sesion> list = new ArrayList<>(); for (Periodo p:periodos) for (Mesociclo m:p.mesociclos) for (Microciclo mi:m.microciclos) for (Sesion s:mi.sesiones) if (s.inicio.get()!=null && !s.inicio.get().isBefore(ini) && !s.inicio.get().isAfter(fin)) list.add(s);
        if (list.isEmpty()){ info("No hay sesiones en esa semana."); return; }
        DateTimeFormatter H = DateTimeFormatter.ofPattern("HH:mm");
        String text = list.stream().map(s-> "- "+s.nombre.get()+" | "+s.inicio.get()+" "+s.hora.get().format(H)+" | "+s.minutos.get()+" min | RPE "+s.rpe.get()+" → "+(s.rpe.get()*s.minutos.get())).collect(Collectors.joining("\n"));
        info("Sesiones:\n\n"+text);
    }

    // ===================== ÁRBOL Y EDICIÓN =====================
    private void rebuildTree(){
        rootItem.getChildren().clear();
        for (Periodo p: periodos){ TreeItem<Nodo> pItem = new TreeItem<>(p); for (Mesociclo m: p.mesociclos){ TreeItem<Nodo> mItem = new TreeItem<>(m); for (Microciclo mi: m.microciclos){ TreeItem<Nodo> miItem = new TreeItem<>(mi); for (Sesion s: mi.sesiones) miItem.getChildren().add(new TreeItem<>(s)); mItem.getChildren().add(miItem);} pItem.getChildren().add(mItem);} rootItem.getChildren().add(pItem);} rootItem.setExpanded(true); tree.setRoot(rootItem); expandAll(rootItem);
    }
    private void expandAll(TreeItem<?> it){ it.setExpanded(true); for (TreeItem<?> c: it.getChildren()) expandAll(c); }
    private <T> T selected(Class<T> cls){ var it = tree.getSelectionModel().getSelectedItem(); if (it==null) return null; return cls.isInstance(it.getValue())? cls.cast(it.getValue()) : null; }

    private final class NodoCell extends TreeCell<Nodo>{
        private final Label l1 = new Label(); private final Label l2 = new Label(); private final VBox box = new VBox(2,l1,l2);
        NodoCell(){ setContentDisplay(ContentDisplay.GRAPHIC_ONLY); setPadding(new Insets(2,6,2,6)); l1.setStyle("-fx-font-weight:bold;"); l2.setStyle("-fx-text-fill:#475569; -fx-font-size:11px;");
            MenuItem editar = new MenuItem("Editar"); editar.setOnAction(e->{ if(getItem()!=null) edit(getItem()); });
            MenuItem eliminar = new MenuItem("Eliminar"); eliminar.setOnAction(e->{ var ti = getTreeItem(); if(ti==null||ti.getParent()==null) return; var n = ti.getValue(); if(n instanceof Periodo p) periodos.remove(p); else if(n instanceof Mesociclo m) ((Periodo)ti.getParent().getValue()).mesociclos.remove(m); else if(n instanceof Microciclo mi) ((Mesociclo)ti.getParent().getValue()).microciclos.remove(mi); else if(n instanceof Sesion s) ((Microciclo)ti.getParent().getValue()).sesiones.remove(s); rebuildTree(); recalcAndRender(); });
            setContextMenu(new ContextMenu(editar, eliminar)); }
        @Override protected void updateItem(Nodo n, boolean empty){ super.updateItem(n, empty); if(empty||n==null||n.tipo.get().isBlank()){ setGraphic(null); return; } l1.setText(n.nombre.get()+"  •  "+n.tipo.get()); String meta=n.duracionTexto(); String fechas=(n.inicio.get()!=null? n.inicio.get().toString():"") + (n.fin.get()!=null? " → "+n.fin.get():""); l2.setText((meta.isBlank()?"":meta)+(fechas.isBlank()?"":(meta.isBlank()?"":" | ")+fechas)); setGraphic(box); }
    }

    private void edit(Nodo n){
        if (n instanceof Periodo p){ Dialog<ButtonType> d = new Dialog<>(); d.setTitle("Editar Periodo"); var nombre = new TextField(p.nombre.get()); var sp = new Spinner<>(1,52,p.semanas.get()); var cp = new ColorPicker(p.color.get()); GridPane gp = form("Nombre:",nombre, "Semanas:", sp, "Color:", cp); d.getDialogPane().setContent(gp); d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL); d.setResultConverter(bt->{ if(bt==ButtonType.OK){ p.nombre.set(nombre.getText()); p.semanas.set((Integer) sp.getValue()); p.color.set(cp.getValue()); rebuildTree(); recalcAndRender(); } return bt; }); d.showAndWait(); }
        else if (n instanceof Mesociclo m){ Dialog<ButtonType> d = new Dialog<>(); d.setTitle("Editar Mesociclo"); var nombre = new TextField(m.nombre.get()); var sp = new Spinner<>(1,52,m.semanas.get()); var cp = new ColorPicker(m.color.get()); GridPane gp = form("Nombre:",nombre, "Semanas:", sp, "Color:", cp); d.getDialogPane().setContent(gp); d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL); d.setResultConverter(bt->{ if(bt==ButtonType.OK){ m.nombre.set(nombre.getText()); m.semanas.set((Integer) sp.getValue()); m.color.set(cp.getValue()); rebuildTree(); recalcAndRender(); } return bt; }); d.showAndWait(); }
        else if (n instanceof Microciclo mi){ Dialog<ButtonType> d = new Dialog<>(); d.setTitle("Editar Microciclo"); var nombre = new TextField(mi.nombre.get()); var sp = new Spinner<>(1,8,mi.semanas.get()); var tipo = new ComboBox<String>(FXCollections.observableArrayList("Introductorio","Carga","Impacto","Recuperación","Mantenimiento","Pico","Transición")); tipo.getSelectionModel().select(mi.fase.get()); GridPane gp = form("Nombre:",nombre, "Semanas:", sp, "Tipo:", tipo); d.getDialogPane().setContent(gp); d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL); d.setResultConverter(bt->{ if(bt==ButtonType.OK){ mi.nombre.set(nombre.getText()); mi.semanas.set((Integer) sp.getValue()); mi.fase.set(tipo.getValue()); rebuildTree(); recalcAndRender(); } return bt; }); d.showAndWait(); }
        else if (n instanceof Sesion s){ Dialog<ButtonType> d = new Dialog<>(); d.setTitle("Editar Sesión"); var nombre = new TextField(s.nombre.get()); var fecha = new DatePicker(s.inicio.get()); var h = new Spinner<>(0,23,s.hora.get().getHour()); var m = new Spinner<>(0,59,s.hora.get().getMinute()); var dur = new Spinner<>(10,240,s.minutos.get(),5); var rpe = new Spinner<>(0,10,s.rpe.get()); var inten = new Spinner<>(0,10,s.intensidad.get()); GridPane gp = form("Nombre:",nombre, "Fecha:",fecha, "Hora:", new HBox(6,new Label("h:"),h,new Label("m:"),m), "Minutos:",dur, "RPE:",rpe, "Intensidad:",inten); d.getDialogPane().setContent(gp); d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL); d.setResultConverter(bt->{ if(bt==ButtonType.OK){ s.nombre.set(nombre.getText()); s.inicio.set(fecha.getValue()); s.hora.set(LocalTime.of((Integer) h.getValue(), (Integer) m.getValue())); s.minutos.set((Integer) dur.getValue()); s.rpe.set((Integer) rpe.getValue()); s.intensidad.set((Integer) inten.getValue()); rebuildTree(); recalcAndRender(); } return bt; }); d.showAndWait(); }
    }

    private void dialogEvento(Integer weekIndexOrNull){ Dialog<ButtonType> d = new Dialog<>(); d.setTitle("Nuevo evento"); var nombre = new TextField(); var tipo = new ComboBox<String>(FXCollections.observableArrayList("A","F","T","PS","M","CP","CF")); tipo.getSelectionModel().select("CP"); var fecha = new DatePicker(weekIndexOrNull==null? dpInicio.getValue() : semanas.get(weekIndexOrNull).inicio()); var color = new ColorPicker(Color.web("#f59e0b")); GridPane gp = form("Nombre:", nombre, "Tipo:", tipo, "Fecha:", fecha, "Color:", color); d.getDialogPane().setContent(gp); d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL); d.setResultConverter(bt->{ if(bt==ButtonType.OK && !nombre.getText().isBlank()){ eventos.add(new Evento(nombre.getText(), tipo.getValue(), fecha.getValue(), color.getValue())); recalcAndRender(); } return bt; }); d.showAndWait(); }

    // ===================== UTIL =====================
    private static GridPane form(Object... kv){ GridPane gp = new GridPane(); gp.setHgap(8); gp.setVgap(8); gp.setPadding(new Insets(10)); for (int i=0;i<kv.length;i+=2){ gp.add(new Label(String.valueOf(kv[i])),0,i/2); gp.add((javafx.scene.Node)kv[i+1],1,i/2); GridPane.setHgrow((javafx.scene.Node)kv[i+1], Priority.ALWAYS);} return gp; }
    private static Color randomColor(){ String[] pal = {"#3b82f6","#22c55e","#ef4444","#a855f7","#f59e0b","#06b6d4","#8b5cf6","#10b981"}; return Color.web(pal[new Random().nextInt(pal.length)]); }
    private static void info(String msg){ new Alert(Alert.AlertType.INFORMATION, msg).showAndWait(); }

    private void exportPng(){
        FileChooser fc = new FileChooser(); fc.setTitle("Exportar Plan Gráfico"); fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG","*.png")); fc.setInitialFileName("plan_grafico.png"); File f = fc.showSaveDialog(getScene().getWindow()); if (f!=null){ try{ javafx.embed.swing.SwingFXUtils.fromFXImage(renderer.snapshot(null,null), null); javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(renderer.snapshot(null,null), null), "png", f); info("Exportado a:\n"+f.getAbsolutePath()); }catch(Exception ex){ info("No se pudo exportar: "+ex.getMessage()); } }
    }

    // ===================== DEMO =====================
    public static class Demo extends Application {
        @Override public void start(Stage stage){ PlanGraficoStudio ui = new PlanGraficoStudio(); Scene sc = new Scene(ui, 1280, 720); stage.setTitle("Plan Gráfico – Studio (Renderer)"); stage.setScene(sc); stage.show(); }
    }
    public static void main(String[] args){ Application.launch(Demo.class, args); }
}
