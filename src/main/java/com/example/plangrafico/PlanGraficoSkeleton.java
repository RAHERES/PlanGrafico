package com.example.plangrafico;



import javafx.application.Application;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
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

/**
 * PlanGraficoSkeleton – interfaz "esqueleto" para armar el Plan Gráfico por semanas
 * mostrando columnas=semanas y filas=(Periodos, Etapas, Mesociclos, Microciclos, Carga, Eventos).
 * Permite definir: inicio/fin del plan, competencia importante, días de entrenamiento,
 * duración/RPE por defecto, y agregar segmentos (Periodo/Etapa/Meso/Micro) por semana.
 * Se dibuja en vivo con Canvas.
 */
public class PlanGraficoSkeleton extends BorderPane {

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

        public void regenerarSemanas(){
            semanas.clear();
            LocalDate d = inicio.get().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            int idx=1; while(!d.isAfter(fin.get())){ semanas.add(new Semana(idx++, d)); d=d.plusWeeks(1);} }

        public int totalSemanas(){ return semanas.size(); }
        public int semanaIndex(LocalDate fecha){ if (semanas.isEmpty()) return 0; for (Semana w: semanas){ LocalDate a=w.lunes, b=w.lunes.plusDays(6); if((!fecha.isBefore(a)) && (!fecha.isAfter(b))) return w.index; } return 0; }
        public LocalDate diaDeSemana(int semana, DayOfWeek dow){ Semana w = semanas.get(Math.max(0, Math.min(semanas.size()-1, semana-1))); return w.lunes.plusDays(dow.getValue()-1); }
    }

    // ======= UI =======
    private final Plan plan = new Plan();
    private final Renderer renderer = new Renderer();

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

    public PlanGraficoSkeleton(){
        buildTop();
        setCenter(new ScrollPane(renderer){ { setFitToWidth(true); setFitToHeight(true);} });
        wire();
        // init
        dpInicio.setValue(plan.inicio.get()); dpFin.setValue(plan.fin.get());
        plan.regenerarSemanas(); renderer.render();
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
        spHora.getValueFactory().valueProperty().addListener((o,a,b)-> plan.horaDef.set(LocalTime.of(b, spMinuto.getValue())));
        spMinuto.getValueFactory().valueProperty().addListener((o,a,b)-> plan.horaDef.set(LocalTime.of(spHora.getValue(), b)));

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

    private void wire(){
        dpInicio.valueProperty().addListener((o,a,b)->{ plan.inicio.set(b); plan.regenerarSemanas(); renderer.render(); });
        dpFin.valueProperty().addListener((o,a,b)->{ plan.fin.set(b); plan.regenerarSemanas(); renderer.render(); });
        dpCompetencia.valueProperty().addListener((o,a,b)->{ plan.competencia.set(b); renderer.render(); });

        btnAddPeriodo.setOnAction(e-> dialogSegmento("Nuevo Periodo", SegRow.PERIODO));
        btnAddEtapa.setOnAction(e-> dialogSegmento("Nueva Etapa", SegRow.ETAPA));
        btnAddMeso.setOnAction(e-> dialogSegmento("Nuevo Mesociclo", SegRow.MESO));
        btnAddMicro.setOnAction(e-> dialogMicro());
        btnGenSes.setOnAction(e-> generarSesiones());
        btnAddEvento.setOnAction(e-> dialogEvento());
        btnExport.setOnAction(e-> exportPng());
    }

    private enum SegRow { PERIODO, ETAPA, MESO, MICRO }

    private void dialogSegmento(String title, SegRow row){
        Dialog<ButtonType> d = new Dialog<>(); d.setTitle(title);
        TextField tfNombre = new TextField(); tfNombre.setPromptText("Nombre");
        Spinner<Integer> spInicio = new Spinner<>(1, Math.max(1, plan.totalSemanas()), 1);
        Spinner<Integer> spSem = new Spinner<>(1, 52, 4);
        ColorPicker cp = new ColorPicker(randomColor());
        GridPane gp = form("Nombre:", tfNombre, "Semana inicio:", spInicio, "Semanas:", spSem, "Color:", cp);
        d.getDialogPane().setContent(gp);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(bt->{ if (bt==ButtonType.OK){
            switch (row){
                case PERIODO -> plan.periodos.add(new PeriodoSeg(tfNombre.getText(), spInicio.getValue(), spSem.getValue(), cp.getValue()));
                case ETAPA -> plan.etapas.add(new EtapaSeg(tfNombre.getText(), spInicio.getValue(), spSem.getValue(), cp.getValue()));
                case MESO -> plan.mesos.add(new MesoSeg(tfNombre.getText(), spInicio.getValue(), spSem.getValue(), cp.getValue()));
                default -> {}
            }
            renderer.render();
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
        d.setResultConverter(bt->{ if (bt==ButtonType.OK){ plan.micros.add(new MicroSeg(tfNombre.getText(), spInicio.getValue(), spSem.getValue(), cbTipo.getValue(), cp.getValue())); renderer.render(); } return bt; });
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
        d.setResultConverter(bt->{ if (bt==ButtonType.OK){ plan.eventos.add(new Evento(cbTipo.getValue(), dp.getValue(), tfNombre.getText(), cp.getValue())); renderer.render(); } return bt; });
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
        renderer.render();
    }

    // ======= Renderer =======
    private final class Renderer extends Region {
        private final Canvas cv = new Canvas(1200, 560);
        private final double labelW=180, colW=70, rowH=34;
        private final Font f12 = Font.font(12), f11 = Font.font(11);
        private final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM");
        Renderer(){ getChildren().add(cv); widthProperty().addListener((o,a,b)-> layoutChildren()); heightProperty().addListener((o,a,b)-> layoutChildren()); }
        @Override protected void layoutChildren(){ cv.setWidth(Math.max(getWidth()-16, 600)); cv.setHeight(Math.max(getHeight()-16, 420)); render(); }

        void render(){
            GraphicsContext g = cv.getGraphicsContext2D();
            g.setFill(Color.WHITE); g.fillRect(0,0, cv.getWidth(), cv.getHeight());
            int cols = plan.totalSemanas();
            int rows = 7; // Semanas, Periodos, Etapas, Mesos, Micros, Carga, Eventos
            double contentW = labelW + cols*colW; cv.setWidth(Math.max(contentW+40, cv.getWidth()));

            int r=0; drawSemanas(g, r++); drawRowSeg(g, r++, plan.periodos, "Periodos"); drawRowSeg(g, r++, plan.etapas, "Etapas"); drawRowSeg(g, r++, plan.mesos, "Mesociclos"); drawRowMicros(g, r++); drawCarga(g, r++); drawEventos(g, r);

            // líneas verticales
            g.setStroke(Color.web("#e5e7eb")); for (int i=0;i<=cols;i++){ double x=labelW + i*colW; g.strokeLine(x, 0, x, rows*rowH + 180); }
        }

        private void label(GraphicsContext g, int row, String text){ double y=row*rowH + 22; g.setFill(Color.web("#0f172a")); g.setFont(f12); g.fillText(text, 8, y); }
        private void span(GraphicsContext g, int row, int c1, int c2, String text, Color bg, Color stroke){ double x=labelW + (c1-1)*colW + 4, w= (c2-c1+1)*colW - 8; double y=row*rowH + 6, h=rowH-12; g.setFill(bg); g.fillRoundRect(x,y,w,h,10,10); g.setStroke(stroke); g.strokeRoundRect(x,y,w,h,10,10); g.setFill(Color.web("#0f172a")); g.setFont(f11); g.fillText(text, x+6, y+h/2+4); }

        private void drawSemanas(GraphicsContext g, int row){
            label(g,row,"Semanas");
            for (Semana w: plan.semanas){
                int col=w.index; double x=labelW + (col-1)*colW; g.setFill(Color.web("#334155")); g.setFont(f11); g.fillText("S"+w.index, x+26, row*rowH + 14); g.setFill(Color.web("#64748b")); g.fillText(DF.format(w.lunes), x+16, row*rowH + 28);
            }
        }
        private void drawRowSeg(GraphicsContext g, int row, ObservableList<? extends Segmento> list, String title){ label(g,row,title); for (Segmento s: list){ span(g,row, s.semanaInicio.get(), s.fin(), s.nombre.get(), s.color.get().deriveColor(0,1,1,0.22), s.color.get()); } }
        private void drawRowMicros(GraphicsContext g, int row){ label(g,row,"Microciclos"); for (MicroSeg s: plan.micros){ Color c = microColor(s.tipo.get()); span(g,row, s.semanaInicio.get(), s.fin(), s.nombre.get()+" ("+s.tipo.get()+")", c.deriveColor(0,1,1,0.2), c); } }
        private void drawCarga(GraphicsContext g, int row){ label(g,row, "Carga (sRPE)"); Map<Integer,Integer> byWeek = new HashMap<>(); for (Sesion s: plan.sesiones){ int wk = plan.semanaIndex(s.fecha.get()); if (wk>0) byWeek.merge(wk, s.sRPE(), Integer::sum);} int max = byWeek.values().stream().mapToInt(i->i).max().orElse(1); for (Semana w: plan.semanas){ int v = byWeek.getOrDefault(w.index, 0); double h = (v/(double)max)*(rowH-10); double x=labelW + (w.index-1)*colW + 22; double y=row*rowH + rowH - 6 - h; g.setFill(Color.web("#0ea5e9")); g.fillRect(x,y,26,h); if (v>0){ g.setFill(Color.web("#334155")); g.fillText(String.valueOf(v), x, y-2);} } }
        private void drawEventos(GraphicsContext g, int row){ label(g,row, "Eventos"); for (Evento ev: plan.eventos){ if(ev.fecha.get()==null) continue; int wk=plan.semanaIndex(ev.fecha.get()); if (wk<=0) continue; double x=labelW + (wk-1)*colW + 10, y=row*rowH + 8; g.setFill(ev.color.get().deriveColor(0,1,1,0.18)); g.fillRoundRect(x,y,colW-20,rowH-16,10,10); g.setStroke(ev.color.get()); g.strokeRoundRect(x,y,colW-20,rowH-16,10,10); g.setFill(Color.web("#0f172a")); g.setFont(f11); g.fillText(ev.tipo.get()+ (ev.nombre.get().isBlank()?"":" – "+ev.nombre.get()), x+6, y+16); }
            // Competencia importante (si está seteada)
            if (plan.competencia.get()!=null){ int wk=plan.semanaIndex(plan.competencia.get()); if(wk>0){ double x=labelW + (wk-1)*colW + colW/2.0; g.setStroke(Color.web("#ef4444")); g.setLineWidth(2); g.strokeLine(x, 0, x, cv.getHeight()); g.setFill(Color.web("#ef4444")); g.fillText("COMP", x-18, 14); }} }
    }

    // ======= util =======
    private static GridPane form(Object... kv){ GridPane gp=new GridPane(); gp.setHgap(8); gp.setVgap(8); gp.setPadding(new Insets(10)); for(int i=0;i<kv.length;i+=2){ gp.add(new Label(String.valueOf(kv[i])),0,i/2); gp.add((javafx.scene.Node)kv[i+1],1,i/2); GridPane.setHgrow((javafx.scene.Node)kv[i+1], Priority.ALWAYS);} return gp; }
    private static Color randomColor(){ String[] pal={"#3b82f6","#22c55e","#ef4444","#a855f7","#f59e0b","#06b6d4","#8b5cf6","#10b981"}; return Color.web(pal[new Random().nextInt(pal.length)]); }
    private static Color microColor(String tipo){ return switch (tipo){ case "O" -> Color.web("#60a5fa"); case "C" -> Color.web("#0ea5e9"); case "CH" -> Color.web("#ef4444"); case "R" -> Color.web("#22c55e"); case "PC" -> Color.web("#f59e0b"); case "CP" -> Color.web("#111827"); default -> Color.web("#64748b"); }; }
    private static Color eventColor(String tipo){ return switch (tipo){ case "A" -> Color.web("#64748b"); case "F" -> Color.web("#22c55e"); case "T" -> Color.web("#0ea5e9"); case "PS" -> Color.web("#eab308"); case "M" -> Color.web("#ef4444"); case "CP" -> Color.web("#334155"); case "CF" -> Color.web("#111827"); default -> Color.web("#94a3b8"); }; }

    private void exportPng(){
        FileChooser fc = new FileChooser(); fc.setTitle("Exportar Plan Gráfico"); fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG","*.png")); fc.setInitialFileName("plan_esqueleto.png"); File f = fc.showSaveDialog(getScene().getWindow()); if(f!=null){ try{ javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(((Region)((ScrollPane)getCenter()).getContent()).snapshot(null,null), null), "png", f); new Alert(Alert.AlertType.INFORMATION, "Exportado a:\n"+f.getAbsolutePath()).showAndWait(); }catch(Exception ex){ new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait(); } }
    }

    // ======= DEMO =======
    public static class Demo extends Application { @Override public void start(Stage stage){ var ui=new PlanGraficoSkeleton(); Scene sc=new Scene(ui, 1280, 740); stage.setTitle("Plan Gráfico – Esqueleto"); stage.setScene(sc); stage.show(); } }
    public static void main(String[] args){ Application.launch(Demo.class, args); }
}
