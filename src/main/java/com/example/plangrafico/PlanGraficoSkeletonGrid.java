package com.example.plangrafico;

import javafx.application.Application;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PlanGraficoSkeletonGrid – mismo concepto de "esqueleto" pero con **GridPane** en lugar de Canvas.
 * Columnas = semanas; Filas = Periodos, Etapas, Mesociclos, Microciclos, Carga (sRPE), Eventos.
 * Permite definir: inicio/fin, competencia importante, días, duración/RPE por defecto, y agregar
 * segmentos (Periodo/Etapa/Meso/Micro) por semana con **colspan** para el span de semanas.
 */
public class PlanGraficoSkeletonGrid extends BorderPane {

    // ======= Modelo base =======
    public static class Semana { public final int index; public final LocalDate lunes; Semana(int i, LocalDate l){ index=i; lunes=l; } }

    public static abstract class Segmento {
        public final StringProperty nombre = new SimpleStringProperty("");
        public final IntegerProperty semanaInicio = new SimpleIntegerProperty(1); // 1-based
        public final IntegerProperty semanas = new SimpleIntegerProperty(1);
        public final ObjectProperty<Color> color = new SimpleObjectProperty<>(Color.web("#94a3b8"));
        Segmento(String nombre, int inicio, int semanas, Color c){ this.nombre.set(nombre); this.semanaInicio.set(inicio); this.semanas.set(semanas); this.color.set(c);}
        public int fin(){ return semanaInicio.get()+semanas.get()-1; }
        public boolean cubre(int semana){ return semana>=semanaInicio.get() && semana<=fin(); }
    }
    public static class PeriodoSeg extends Segmento { public PeriodoSeg(String n,int i,int s,Color c){ super(n,i,s,c);} }
    public static class EtapaSeg extends Segmento { public EtapaSeg(String n,int i,int s,Color c){ super(n,i,s,c);} }
    public static class MesoSeg extends Segmento { public MesoSeg(String n,int i,int s,Color c){ super(n,i,s,c);} }
    public static class MicroSeg extends Segmento { public final StringProperty tipo = new SimpleStringProperty("C"); public MicroSeg(String n,int i,int s,String t,Color c){ super(n,i,s,c); tipo.set(t);} }

    public static class Sesion {
        public final ObjectProperty<LocalDate> fecha = new SimpleObjectProperty<>();
        public final ObjectProperty<LocalTime> hora = new SimpleObjectProperty<>(LocalTime.of(17,0));
        public final IntegerProperty minutos = new SimpleIntegerProperty(90);
        public final IntegerProperty rpe = new SimpleIntegerProperty(5);
        Sesion(LocalDate f, LocalTime h, int min, int r){ fecha.set(f); hora.set(h); minutos.set(min); rpe.set(r); }
        public int sRPE(){ return Math.max(0,minutos.get())*Math.max(0,rpe.get()); }
    }

    public static class Evento { // controles y competencias
        public final StringProperty tipo = new SimpleStringProperty("CF"); // A,F,T,PS,M, CP, CF
        public final ObjectProperty<LocalDate> fecha = new SimpleObjectProperty<>();
        public final StringProperty nombre = new SimpleStringProperty("");
        public final ObjectProperty<Color> color = new SimpleObjectProperty<>(Color.web("#0ea5e9"));
        Evento(String t, LocalDate f, String n, Color c){ tipo.set(t); fecha.set(f); nombre.set(n); color.set(c);} }

    public static class Plan {
        public final ObjectProperty<LocalDate> inicio = new SimpleObjectProperty<>(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
        public final ObjectProperty<LocalDate> fin = new SimpleObjectProperty<>(LocalDate.now().plusWeeks(16).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
        public final ObjectProperty<LocalDate> competencia = new SimpleObjectProperty<>(null);
        public final ObservableList<Semana> semanas = FXCollections.observableArrayList();

        // parámetros de sesiones
        public final BooleanProperty diaL = new SimpleBooleanProperty(true);
        public final BooleanProperty diaM = new SimpleBooleanProperty(false);
        public final BooleanProperty diaX = new SimpleBooleanProperty(true);
        public final BooleanProperty diaJ = new SimpleBooleanProperty(false);
        public final BooleanProperty diaV = new SimpleBooleanProperty(true);
        public final BooleanProperty diaS = new SimpleBooleanProperty(false);
        public final BooleanProperty diaD = new SimpleBooleanProperty(false);
        public final IntegerProperty durMin = new SimpleIntegerProperty(90);
        public final IntegerProperty rpeDef = new SimpleIntegerProperty(5);
        public final ObjectProperty<LocalTime> horaDef = new SimpleObjectProperty<>(LocalTime.of(17,0));

        public final ObservableList<PeriodoSeg> periodos = FXCollections.observableArrayList();
        public final ObservableList<EtapaSeg> etapas = FXCollections.observableArrayList();
        public final ObservableList<MesoSeg> mesos = FXCollections.observableArrayList();
        public final ObservableList<MicroSeg> micros = FXCollections.observableArrayList();
        public final ObservableList<Sesion> sesiones = FXCollections.observableArrayList();
        public final ObservableList<Evento> eventos = FXCollections.observableArrayList();

        public void regenerarSemanas(){ semanas.clear(); LocalDate d = inicio.get().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); int idx=1; while(!d.isAfter(fin.get())){ semanas.add(new Semana(idx++, d)); d=d.plusWeeks(1);} }
        public int totalSemanas(){ return semanas.size(); }
        public int semanaIndex(LocalDate fecha){ if (semanas.isEmpty()) return 0; for (Semana w: semanas){ LocalDate a=w.lunes, b=w.lunes.plusDays(6); if((!fecha.isBefore(a)) && (!fecha.isAfter(b))) return w.index; } return 0; }
    }

    // ======= UI =======
    private final Plan plan = new Plan();
    private final GridPane grid = new GridPane();
    private final ScrollPane scroller = new ScrollPane(grid);

    // top controls
    private final DatePicker dpInicio = new DatePicker();
    private final DatePicker dpFin = new DatePicker();
    private final DatePicker dpCompetencia = new DatePicker();
    private final CheckBox cbL = new CheckBox("L"), cbM=new CheckBox("M"), cbX=new CheckBox("X"), cbJ=new CheckBox("J"), cbV=new CheckBox("V"), cbS=new CheckBox("S"), cbD=new CheckBox("D");
    private final Spinner<Integer> spMin = new Spinner<>(10,240,90,5);
    private final Spinner<Integer> spRpe = new Spinner<>(0,10,5);
    private final Spinner<Integer> spHora = new Spinner<>(0,23,17), spMinuto = new Spinner<>(0,59,0);

    private final Button btnAddPeriodo = new Button("+ Periodo");
    private final Button btnAddEtapa = new Button("+ Etapa");
    private final Button btnAddMeso = new Button("+ Mesociclo");
    private final Button btnAddMicro = new Button("+ Microciclo");
    private final Button btnGenSes = new Button("Generar sesiones");
    private final Button btnAddEvento = new Button("+ Evento");
    private final Button btnExport = new Button("Exportar PNG");

    // filas (indices de GridPane)
    private static final int ROW_HEADER = 0;
    private static final int ROW_PERIODOS = 1;
    private static final int ROW_ETAPAS = 2;
    private static final int ROW_MESOS = 3;
    private static final int ROW_MICROS = 4;
    private static final int ROW_CARGA = 5;
    private static final int ROW_EVENTOS = 6;

    private static final double LABEL_W = 190;
    private static final double COL_W = 70;
    private static final double ROW_H = 34;

    private final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM");

    public PlanGraficoSkeletonGrid(){
        buildTop();
        buildGridSkeleton();
        setCenter(scroller);
        wire();
        // init
        dpInicio.setValue(plan.inicio.get()); dpFin.setValue(plan.fin.get());
        plan.regenerarSemanas();
        render();
    }

    private void buildTop(){
        // sección de fechas
        HBox fechas = new HBox(8, new Label("Inicio:"), dpInicio, new Label("Fin:"), dpFin, new Label("Competencia:"), dpCompetencia);
        fechas.setAlignment(Pos.CENTER_LEFT);

        // días + parámetros
        cbL.selectedProperty().bindBidirectional(plan.diaL); cbM.selectedProperty().bindBidirectional(plan.diaM); cbX.selectedProperty().bindBidirectional(plan.diaX);
        cbJ.selectedProperty().bindBidirectional(plan.diaJ); cbV.selectedProperty().bindBidirectional(plan.diaV); cbS.selectedProperty().bindBidirectional(plan.diaS); cbD.selectedProperty().bindBidirectional(plan.diaD);
        spMin.getValueFactory().valueProperty().bindBidirectional(plan.durMin.asObject());
        spRpe.getValueFactory().valueProperty().bindBidirectional(plan.rpeDef.asObject());
        spHora.valueProperty().addListener((o,a,b)-> plan.horaDef.set(LocalTime.of(b, spMinuto.getValue())));
        spMinuto.valueProperty().addListener((o,a,b)-> plan.horaDef.set(LocalTime.of(spHora.getValue(), b)));

        HBox dias = new HBox(6, new Label("Días:"), cbL, cbM, cbX, cbJ, cbV, cbS, cbD,
                new Separator(Orientation.VERTICAL), new Label("Min/ sesión:"), spMin,
                new Label("RPE:"), spRpe, new Label("Hora:"), spHora, new Label(":"), spMinuto);
        dias.setAlignment(Pos.CENTER_LEFT);

        // acciones
        HBox acciones = new HBox(8, btnAddPeriodo, btnAddEtapa, btnAddMeso, btnAddMicro, new Separator(Orientation.VERTICAL), btnGenSes, btnAddEvento, new Separator(Orientation.VERTICAL), btnExport);
        acciones.setAlignment(Pos.CENTER_LEFT);

        VBox top = new VBox(6, fechas, dias, acciones); top.setPadding(new Insets(8));
        setTop(top);
    }

    private void buildGridSkeleton(){
        scroller.setFitToWidth(true); scroller.setFitToHeight(true);
        grid.setPadding(new Insets(8)); grid.setHgap(2); grid.setVgap(2);
    }

    private void wire(){
        dpInicio.valueProperty().addListener((o,a,b)->{ plan.inicio.set(b); plan.regenerarSemanas(); render(); });
        dpFin.valueProperty().addListener((o,a,b)->{ plan.fin.set(b); plan.regenerarSemanas(); render(); });
        dpCompetencia.valueProperty().addListener((o,a,b)->{ plan.competencia.set(b); render(); });

        btnAddPeriodo.setOnAction(e-> dialogSegmento("Nuevo Periodo", RowType.PERIODO));
        btnAddEtapa.setOnAction(e-> dialogSegmento("Nueva Etapa", RowType.ETAPA));
        btnAddMeso.setOnAction(e-> dialogSegmento("Nuevo Mesociclo", RowType.MESO));
        btnAddMicro.setOnAction(e-> dialogMicro());
        btnGenSes.setOnAction(e-> generarSesiones());
        btnAddEvento.setOnAction(e-> dialogEvento());
        btnExport.setOnAction(e-> exportPng());
    }

    // ======= Render con GridPane =======
    private enum RowType { PERIODO, ETAPA, MESO, MICRO }

    private void render(){
        grid.getChildren().clear();
        grid.getColumnConstraints().clear();
        // columna 0 fija para etiquetas
        ColumnConstraints c0 = new ColumnConstraints(); c0.setPrefWidth(LABEL_W); c0.setMinWidth(LABEL_W); c0.setMaxWidth(LABEL_W);
        grid.getColumnConstraints().add(c0);
        // columnas por semana
        for (int i=0; i<plan.totalSemanas(); i++){ ColumnConstraints c = new ColumnConstraints(); c.setPrefWidth(COL_W); c.setMinWidth(COL_W); c.setMaxWidth(COL_W); grid.getColumnConstraints().add(c); }

        // encabezado Semanas
        addHeader(ROW_HEADER, "Semanas");
        int col=1; for (Semana w: plan.semanas){
            VBox head = new VBox(2);
            Label l1 = new Label("S"+w.index); l1.setFont(Font.font(12)); l1.setStyle("-fx-text-fill:#0f172a;");
            Label l2 = new Label(DF.format(w.lunes)); l2.setStyle("-fx-text-fill:#64748b; -fx-font-size:11px;");
            head.getChildren().addAll(l1,l2); head.setAlignment(Pos.CENTER); head.setPrefSize(COL_W, ROW_H);
            StackPane cell = cellBox(); cell.getChildren().add(head);
            // marca de competencia
            if (plan.competencia.get()!=null){ int wk = plan.semanaIndex(plan.competencia.get()); if (wk==w.index){ Label star = new Label("★"); star.setStyle("-fx-text-fill:#ef4444; -fx-font-size:12px;"); head.getChildren().add(star);} }
            grid.add(cell, col++, ROW_HEADER);
        }

        // filas de segmentos
        addHeader(ROW_PERIODOS, "Períodos");   addSegmentRow(ROW_PERIODOS, plan.periodos, RowType.PERIODO);
        addHeader(ROW_ETAPAS,   "Etapas");     addSegmentRow(ROW_ETAPAS,   plan.etapas,   RowType.ETAPA);
        addHeader(ROW_MESOS,    "Mesociclos"); addSegmentRow(ROW_MESOS,    plan.mesos,    RowType.MESO);
        addHeader(ROW_MICROS,   "Microciclos");addMicroRow(ROW_MICROS);

        // carga sRPE
        addHeader(ROW_CARGA, "Carga (sRPE)"); addCargaRow(ROW_CARGA);

        // eventos
        addHeader(ROW_EVENTOS, "Eventos"); addEventosRow(ROW_EVENTOS);

        // líneas verticales (simples: bordes de celdas)
        // ya quedan por el estilo de cada celda; si quieres líneas fuertes, podríamos añadir un GridPane overlay.
    }

    private void addHeader(int row, String text){
        Label l = new Label(text);
        l.setStyle("-fx-font-weight:bold; -fx-padding:6 8; -fx-background-color:#f8fafc; -fx-border-color:#e5e7eb; -fx-border-width:0 1 1 0;");
        l.setPrefSize(LABEL_W, ROW_H);
        grid.add(l, 0, row);
    }

    private void addSegmentRow(int row, ObservableList<? extends Segmento> list, RowType type){
        // fondo de celdas vacías
        for (int c=1; c<=plan.totalSemanas(); c++){ grid.add(cellBox(), c, row); }
        for (Segmento s: list){
            int c1 = Math.max(1, s.semanaInicio.get());
            int c2 = Math.min(plan.totalSemanas(), s.fin());
            if (c1>c2) continue;
            StackPane span = spanBox(labelForSegment(s, type), s.color.get().deriveColor(0,1,1,0.22), s.color.get());
            attachSegmentMenu(span, s, type);
            grid.add(span, c1, row, c2-c1+1, 1);
        }
    }

    // Etiqueta para spans de Periodo/Etapa/Meso
    private String labelForSegment(Segmento s, RowType type){
        String base;
        switch (type){
            case PERIODO -> base = "Periodo";
            case ETAPA   -> base = "Etapa";
            case MESO    -> base = "Mesociclo";
            default      -> base = "";
        }
        String name = (s.nombre.get()==null || s.nombre.get().isBlank()) ? base : s.nombre.get();
        return name + " [" + s.semanaInicio.get() + "–" + s.fin() + "]";
    }

    private void addMicroRow(int row){
        for (int c=1; c<=plan.totalSemanas(); c++){ grid.add(cellBox(), c, row); }
        for (MicroSeg s: plan.micros){
            int c1 = Math.max(1, s.semanaInicio.get());
            int c2 = Math.min(plan.totalSemanas(), s.fin());
            if (c1>c2) continue;
            Color base = microColor(s.tipo.get());
            StackPane span = spanBox(s.nombre.get()+" ("+s.tipo.get()+")", base.deriveColor(0,1,1,0.22), base);
            attachMicroMenu(span, s);
            grid.add(span, c1, row, c2-c1+1, 1);
        }
    }

    private void addCargaRow(int row){
        // fondo
        for (int c=1; c<=plan.totalSemanas(); c++){ StackPane cell = cellBox(); grid.add(cell, c, row); }
        Map<Integer,Integer> byWeek = new HashMap<>();
        for (Sesion s: plan.sesiones){ int wk = plan.semanaIndex(s.fecha.get()); if (wk>0) byWeek.merge(wk, s.sRPE(), Integer::sum);} int max = byWeek.values().stream().mapToInt(i->i).max().orElse(1);
        for (int c=1; c<=plan.totalSemanas(); c++){
            int v = byWeek.getOrDefault(c, 0);
            Pane bar = new Pane(); bar.setPrefSize(COL_W-8, ROW_H-8); bar.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE); bar.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            StackPane.setAlignment(bar, Pos.BOTTOM_CENTER);
            Rectangle r = new Rectangle(COL_W-26, Math.max(2, (ROW_H-12) * (v/(double)max)));
            r.setFill(Color.web("#0ea5e9"));
            StackPane sp = new StackPane(r); sp.setAlignment(Pos.BOTTOM_CENTER); sp.setPrefSize(COL_W-8, ROW_H-8);
            Label lbl = new Label(v>0? String.valueOf(v):""); lbl.setStyle("-fx-text-fill:#334155; -fx-font-size:11px;"); StackPane.setAlignment(lbl, Pos.TOP_LEFT);
            StackPane cell = new StackPane(sp, lbl); cell.setStyle(cellStyle()); cell.setPrefSize(COL_W, ROW_H);
            final int week = c; cell.setOnMouseClicked(ev->{ if (ev.getClickCount()==2) showSesiones(week); });
            grid.add(cell, c, row);
        }
    }

    private void addEventosRow(int row){
        for (int c=1; c<=plan.totalSemanas(); c++){
            FlowPane chipPane = new FlowPane(4,2); chipPane.setPrefWrapLength(COL_W-10); chipPane.setAlignment(Pos.CENTER);
            LocalDate a = plan.semanas.get(c-1).lunes, b = a.plusDays(6);
            plan.eventos.stream().filter(ev -> ev.fecha.get()!=null && !ev.fecha.get().isBefore(a) && !ev.fecha.get().isAfter(b))
                    .forEach(ev -> chipPane.getChildren().add(chip(ev.tipo.get() + (ev.nombre.get().isBlank()?"":"-"+ev.nombre.get()), ev.color.get())));
            // también marcamos COMP si coincide con competencia
            if (plan.competencia.get()!=null && !plan.competencia.get().isBefore(a) && !plan.competencia.get().isAfter(b)){
                chipPane.getChildren().add(chip("COMP", Color.web("#ef4444")));
            }
            StackPane cell = new StackPane(chipPane); cell.setStyle(cellStyle()); cell.setPrefSize(COL_W, ROW_H);
            final int week = c; cell.setOnMouseClicked(e->{ if(e.getButton()==MouseButton.SECONDARY) quickEvento(week); });
            grid.add(cell, c, row);
        }
    }

    // ======= Diálogos =======
    private void dialogSegmento(String title, RowType type){
        Dialog<ButtonType> d = new Dialog<>(); d.setTitle(title);
        TextField tfNombre = new TextField(); tfNombre.setPromptText("Nombre");
        Spinner<Integer> spInicio = new Spinner<>(1, Math.max(1, plan.totalSemanas()), 1);
        Spinner<Integer> spSem = new Spinner<>(1, 52, 4);
        ColorPicker cp = new ColorPicker(randomColor());
        GridPane gp = form("Nombre:", tfNombre, "Semana inicio:", spInicio, "Semanas:", spSem, "Color:", cp);
        d.getDialogPane().setContent(gp);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(bt->{ if (bt==ButtonType.OK){
            switch (type){
                case PERIODO -> plan.periodos.add(new PeriodoSeg(tfNombre.getText(), spInicio.getValue(), spSem.getValue(), cp.getValue()));
                case ETAPA -> plan.etapas.add(new EtapaSeg(tfNombre.getText(), spInicio.getValue(), spSem.getValue(), cp.getValue()));
                case MESO -> plan.mesos.add(new MesoSeg(tfNombre.getText(), spInicio.getValue(), spSem.getValue(), cp.getValue()));
                default -> {}
            }
            render();
        } return bt; });
        d.showAndWait();
    }

    private void dialogMicro(){
        Dialog<ButtonType> d = new Dialog<>(); d.setTitle("Nuevo Microciclo");
        TextField tfNombre = new TextField(); tfNombre.setPromptText("Nombre");
        ComboBox<String> cbTipo = new ComboBox<>(FXCollections.observableArrayList("O","C","CH","R","PC","CP")); cbTipo.getSelectionModel().select("C");
        Spinner<Integer> spInicio = new Spinner<>(1, Math.max(1, plan.totalSemanas()), 1);
        Spinner<Integer> spSem = new Spinner<>(1, 8, 1);
        ColorPicker cp = new ColorPicker(microColor(cbTipo.getSelectionModel().getSelectedItem()));
        cbTipo.valueProperty().addListener((o,a,b)-> cp.setValue(microColor(b)));
        GridPane gp = form("Nombre:", tfNombre, "Tipo:", cbTipo, "Semana inicio:", spInicio, "Semanas:", spSem, "Color:", cp);
        d.getDialogPane().setContent(gp); d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(bt->{ if (bt==ButtonType.OK){ plan.micros.add(new MicroSeg(tfNombre.getText(), spInicio.getValue(), spSem.getValue(), cbTipo.getValue(), cp.getValue())); render(); } return bt; });
        d.showAndWait();
    }

    private void dialogEvento(){
        Dialog<ButtonType> d = new Dialog<>(); d.setTitle("Nuevo Evento");
        ComboBox<String> cbTipo = new ComboBox<>(FXCollections.observableArrayList("A","F","T","PS","M","CP","CF")); cbTipo.getSelectionModel().select("CF");
        DatePicker dp = new DatePicker(plan.competencia.get()!=null? plan.competencia.get() : plan.inicio.get());
        TextField tfNombre = new TextField(); tfNombre.setPromptText("Nombre");
        ColorPicker cp = new ColorPicker(eventColor(cbTipo.getValue()));
        cbTipo.valueProperty().addListener((o,a,b)-> cp.setValue(eventColor(b)));
        GridPane gp = form("Tipo:", cbTipo, "Fecha:", dp, "Nombre:", tfNombre, "Color:", cp);
        d.getDialogPane().setContent(gp); d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(bt->{ if (bt==ButtonType.OK){ plan.eventos.add(new Evento(cbTipo.getValue(), dp.getValue(), tfNombre.getText(), cp.getValue())); render(); } return bt; });
        d.showAndWait();
    }

    private void quickEvento(int week){
        Dialog<ButtonType> d = new Dialog<>(); d.setTitle("Evento rápido – Semana "+week);
        ComboBox<String> cbTipo = new ComboBox<>(FXCollections.observableArrayList("A","F","T","PS","M","CP","CF")); cbTipo.getSelectionModel().select("CP");
        TextField tfNombre = new TextField(); tfNombre.setPromptText("Nombre");
        LocalDate base = plan.semanas.get(week-1).lunes.plusDays(5); // sábado por defecto
        DatePicker dp = new DatePicker(base);
        GridPane gp = form("Tipo:", cbTipo, "Fecha:", dp, "Nombre:", tfNombre);
        d.getDialogPane().setContent(gp); d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(bt->{ if (bt==ButtonType.OK){ plan.eventos.add(new Evento(cbTipo.getValue(), dp.getValue(), tfNombre.getText(), eventColor(cbTipo.getValue()))); render(); } return bt; });
        d.showAndWait();
    }

    private void generarSesiones(){
        plan.sesiones.clear();
        for (Semana w: plan.semanas){
            for (DayOfWeek dow: DayOfWeek.values()){
                boolean ok = switch (dow){
                    case MONDAY -> plan.diaL.get(); case TUESDAY -> plan.diaM.get(); case WEDNESDAY -> plan.diaX.get();
                    case THURSDAY -> plan.diaJ.get(); case FRIDAY -> plan.diaV.get(); case SATURDAY -> plan.diaS.get(); case SUNDAY -> plan.diaD.get(); };
                if (ok){ plan.sesiones.add(new Sesion(w.lunes.plusDays(dow.getValue()-1), plan.horaDef.get(), plan.durMin.get(), plan.rpeDef.get())); }
            }
        }
        render();
    }

    // ======= Helpers UI =======
    private StackPane spanBox(String text, Color bg, Color stroke){
        Label l = new Label(text); l.setStyle("-fx-font-size:12px; -fx-text-fill:#0f172a;");
        StackPane box = new StackPane(l);
        box.setPadding(new Insets(4,6,4,6));
        box.setStyle("-fx-background-color:"+toRgba(bg)+"; -fx-border-color:"+toHex(stroke)+"; -fx-background-radius:8; -fx-border-radius:8; ");
        return box;
    }
    private StackPane cellBox(){ StackPane cell = new StackPane(); cell.setPrefSize(COL_W, ROW_H); cell.setStyle(cellStyle()); return cell; }
    private String cellStyle(){ return "-fx-background-color:white; -fx-border-color:#e5e7eb; -fx-border-width:1;"; }
    private Label chip(String text, Color color){ Label l = new Label(text); l.setStyle("-fx-background-radius:10; -fx-padding:2 6; -fx-text-fill:white; -fx-font-size:11px;"); l.setBackground(new Background(new BackgroundFill(color, new CornerRadii(10), Insets.EMPTY))); return l; }
    private String toHex(Color c){ return String.format("#%02x%02x%02x", (int)(c.getRed()*255),(int)(c.getGreen()*255),(int)(c.getBlue()*255)); }
    private String toRgba(Color c){ return String.format("rgba(%d,%d,%d,%.2f)",(int)(c.getRed()*255),(int)(c.getGreen()*255),(int)(c.getBlue()*255), c.getOpacity()); }
    private static Color randomColor(){ String[] pal={"#3b82f6","#22c55e","#ef4444","#a855f7","#f59e0b","#06b6d4","#8b5cf6","#10b981"}; return Color.web(pal[new Random().nextInt(pal.length)]); }
    private static Color microColor(String tipo){ return switch (tipo){ case "O" -> Color.web("#60a5fa"); case "C" -> Color.web("#0ea5e9"); case "CH" -> Color.web("#ef4444"); case "R" -> Color.web("#22c55e"); case "PC" -> Color.web("#f59e0b"); case "CP" -> Color.web("#111827"); default -> Color.web("#64748b"); }; }
    private static Color eventColor(String tipo){ return switch (tipo){ case "A" -> Color.web("#64748b"); case "F" -> Color.web("#22c55e"); case "T" -> Color.web("#0ea5e9"); case "PS" -> Color.web("#eab308"); case "M" -> Color.web("#ef4444"); case "CP" -> Color.web("#334155"); case "CF" -> Color.web("#111827"); default -> Color.web("#94a3b8"); }; }

    private void showSesiones(int week){
        LocalDate a = plan.semanas.get(week-1).lunes, b=a.plusDays(6);
        List<Sesion> list = plan.sesiones.stream().filter(s-> s.fecha.get()!=null && !s.fecha.get().isBefore(a) && !s.fecha.get().isAfter(b)).toList();
        if (list.isEmpty()){ new Alert(Alert.AlertType.INFORMATION, "No hay sesiones en esa semana").showAndWait(); return; }
        DateTimeFormatter H = DateTimeFormatter.ofPattern("HH:mm");
        String text = list.stream().map(s-> String.format("- %s %s | %d min | RPE %d → %d", s.fecha.get(), s.hora.get().format(H), s.minutos.get(), s.rpe.get(), s.sRPE())).collect(Collectors.joining("\n"));
        new Alert(Alert.AlertType.INFORMATION, "Sesiones:\n\n"+text).showAndWait();
    }

    private void attachSegmentMenu(Node node, Segmento seg, RowType type){
        ContextMenu cm = new ContextMenu();
        MenuItem edit = new MenuItem("Editar"); edit.setOnAction(e-> editSegment(seg, type));
        MenuItem del = new MenuItem("Eliminar"); del.setOnAction(e->{ removeSegment(seg, type); render(); });
        cm.getItems().addAll(edit, del); node.setOnMouseClicked(e->{ if(e.getButton()==MouseButton.SECONDARY) cm.show(node, e.getScreenX(), e.getScreenY()); });
    }
    private void attachMicroMenu(Node node, MicroSeg seg){
        ContextMenu cm = new ContextMenu();
        MenuItem edit = new MenuItem("Editar"); edit.setOnAction(e-> editMicro(seg));
        MenuItem del = new MenuItem("Eliminar"); del.setOnAction(e->{ plan.micros.remove(seg); render(); });
        cm.getItems().addAll(edit, del); node.setOnMouseClicked(e->{ if(e.getButton()==MouseButton.SECONDARY) cm.show(node, e.getScreenX(), e.getScreenY()); });
    }

    private void editSegment(Segmento seg, RowType type){
        Dialog<ButtonType> d = new Dialog<>(); d.setTitle("Editar "+type);
        TextField tfNombre = new TextField(seg.nombre.get());
        Spinner<Integer> spInicio = new Spinner<>(1, Math.max(1, plan.totalSemanas()), seg.semanaInicio.get());
        Spinner<Integer> spSem = new Spinner<>(1, 52, seg.semanas.get());
        ColorPicker cp = new ColorPicker(seg.color.get());
        GridPane gp = form("Nombre:", tfNombre, "Semana inicio:", spInicio, "Semanas:", spSem, "Color:", cp);
        d.getDialogPane().setContent(gp); d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(bt->{ if(bt==ButtonType.OK){ seg.nombre.set(tfNombre.getText()); seg.semanaInicio.set(spInicio.getValue()); seg.semanas.set(spSem.getValue()); seg.color.set(cp.getValue()); render(); } return bt; });
        d.showAndWait();
    }

    private void editMicro(MicroSeg seg){
        Dialog<ButtonType> d = new Dialog<>(); d.setTitle("Editar Microciclo");
        TextField tfNombre = new TextField(seg.nombre.get());
        ComboBox<String> cbTipo = new ComboBox<>(FXCollections.observableArrayList("O","C","CH","R","PC","CP")); cbTipo.getSelectionModel().select(seg.tipo.get());
        Spinner<Integer> spInicio = new Spinner<>(1, Math.max(1, plan.totalSemanas()), seg.semanaInicio.get());
        Spinner<Integer> spSem = new Spinner<>(1, 8, seg.semanas.get());
        ColorPicker cp = new ColorPicker(seg.color.get());
        cbTipo.valueProperty().addListener((o,a,b)-> cp.setValue(microColor(b)));
        GridPane gp = form("Nombre:", tfNombre, "Tipo:", cbTipo, "Semana inicio:", spInicio, "Semanas:", spSem, "Color:", cp);
        d.getDialogPane().setContent(gp); d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(bt->{ if(bt==ButtonType.OK){ seg.nombre.set(tfNombre.getText()); seg.tipo.set(cbTipo.getValue()); seg.semanaInicio.set(spInicio.getValue()); seg.semanas.set(spSem.getValue()); seg.color.set(cp.getValue()); render(); } return bt; });
        d.showAndWait();
    }

    private void removeSegment(Segmento seg, RowType type){
        switch (type){
            case PERIODO -> plan.periodos.remove(seg);
            case ETAPA -> plan.etapas.remove(seg);
            case MESO -> plan.mesos.remove(seg);
            default -> {}
        }
    }

    // ======= util =======
    private static GridPane form(Object... kv){ GridPane gp=new GridPane(); gp.setHgap(8); gp.setVgap(8); gp.setPadding(new Insets(10)); for(int i=0;i<kv.length;i+=2){ gp.add(new Label(String.valueOf(kv[i])),0,i/2); gp.add((javafx.scene.Node)kv[i+1],1,i/2); GridPane.setHgrow((javafx.scene.Node)kv[i+1], Priority.ALWAYS);} return gp; }

    private void exportPng(){
        FileChooser fc = new FileChooser(); fc.setTitle("Exportar Plan Gráfico"); fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG","*.png")); fc.setInitialFileName("plan_grid.png"); File f = fc.showSaveDialog(getScene().getWindow()); if(f!=null){ try{ javafx.scene.image.WritableImage img = grid.snapshot(null, null); javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(img, null), "png", f); new Alert(Alert.AlertType.INFORMATION, "Exportado a:\n"+f.getAbsolutePath()).showAndWait(); }catch(Exception ex){ new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait(); } }
    }

    // ======= DEMO =======
    public static class Demo extends Application { @Override public void start(Stage stage){ var ui=new PlanGraficoSkeletonGrid(); Scene sc=new Scene(ui, 1280, 740); stage.setTitle("Plan Gráfico – Esqueleto (GridPane)"); stage.setScene(sc); stage.show(); } }
    public static void main(String[] args){ Application.launch(Demo.class, args); }
}