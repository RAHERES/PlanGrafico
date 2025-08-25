package com.example.plangrafico;


import com.example.plangrafico.ui.PlanGraficoView;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.io.File;
import java.time.LocalDate;

public class EditorController {

    @FXML private Button btnNuevoPlan, btnAutoAtr, btnExportar, btnGuardarNotas, btnVistaCalendario, btnVistaPlan;
    @FXML private DatePicker dpInicio, dpFin;
    @FXML private ChoiceBox<TipoPeriodizacion> cbTipo;
    @FXML private TextArea taNotas;
    @FXML private ScrollPane scroll;

    // Guarda referencia de la vista actual
    private enum Vista { CALENDARIO, PLAN }
    private Vista vistaActual = Vista.CALENDARIO;

    private Node vistaCalendario; // tu CalendarGrid
    private Node vistaPlan;       // PlanGraficoView.crearVista(plan)

    private final PlanService service = new PlanService();
    private CalendarioGrid grid;

    @FXML
    public void initialize() {
        cbTipo.getItems().setAll(TipoPeriodizacion.values());
        cbTipo.setValue(TipoPeriodizacion.ATR);
        dpInicio.setValue(LocalDate.now().withDayOfMonth(1));
        dpFin.setValue(dpInicio.getValue().plusWeeks(16));

        btnNuevoPlan.setOnAction(e -> nuevoPlan());
        btnAutoAtr.setOnAction(e -> autoATR());
        btnExportar.setOnAction(e -> exportarPNG());
        btnGuardarNotas.setOnAction(e -> guardarNotas());

        construirGrid(); // crea vistaCalendario
        btnVistaCalendario.setOnAction(e -> mostrarCalendario());
        btnVistaPlan.setOnAction(e -> mostrarPlan());

        // si ya tienes btnExportar, ajústalo:
        btnExportar.setOnAction(e -> exportarVistaActualPNG());

        this.service.ensureReady();
        construirGrid();
    }

    private void exportarVistaActualPNG() {
        try {
            Node node = (vistaActual == Vista.PLAN && vistaPlan != null) ? vistaPlan : scroll.getContent();
            var img = PlanGraficoView.snapshot(node, 2.0); // escala 2x para mejor nitidez
            var fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagen PNG","*.png"));
            fc.setInitialFileName(vistaActual == Vista.PLAN ? "plan_grafico.png" : "calendario.png");
            File f = fc.showSaveDialog(scroll.getScene().getWindow());
            if (f != null) {
                ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", f);
                new Alert(Alert.AlertType.INFORMATION, "Exportado: " + f.getAbsolutePath()).showAndWait();
            }
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Error exportando: " + ex.getMessage()).showAndWait();
        }
    }
    private void mostrarCalendario() {
        vistaActual = Vista.CALENDARIO;
        scroll.setContent(vistaCalendario);
    }

    private void mostrarPlan() {
        // Construye el plan desde el rango o usa uno ejemplo:
        var plan = PlanGraficoView.desdeRango(dpInicio.getValue(), dpFin.getValue(), cbTipo.getValue().name());
        this.vistaPlan = PlanGraficoView.crearVista(PlanGraficoView.ejemplo());
        vistaActual = Vista.PLAN;

        PlanGraficoRenderer renderer = new PlanGraficoRenderer();
        GridPane build = renderer.build(PlanGrafico.builder()
                .deporte("Fútbol")
                .categoria("Sub 17")
                .equipo("Villa de las Niñas")
                .modelo(ModeloPeriodizacion.ATR)
                .inicio(LocalDate.of(2025, 8, 26))
                .fin(LocalDate.of(2025, 12, 13))
                .addMesociclo(m -> m
                        .tipo(MesocicloTipo.ACUMULACION)
                        .nombre("M1 – Acumulación")
                        .inicio(LocalDate.of(2025, 8, 1))
                        .fin(LocalDate.of(2025, 9, 12))
                        .resumen(new PlanGrafico.ResumenCarga(900, 0.6, 1.8, 5))
                        .addMicrociclo(x -> x
                                .tipo(MicrocicloTipo.ORDINARIO)
                                .inicio(LocalDate.of(2025, 8, 1))
                                .fin(LocalDate.of(2025, 8, 7))
                                .porcentajeCarga(70)
                                .contenido("Fuerza", 180)
                                .contenido("Técnica", 120)
                                .contenido("Táctica", 60))
                        .addMicrociclo(x -> x
                                .tipo(MicrocicloTipo.CHOQUE)
                                .inicio(LocalDate.of(2025, 8, 8))
                                .fin(LocalDate.of(2025, 8, 14))
                                .porcentajeCarga(90)
                                .contenido("Fuerza", 210)
                                .contenido("Resistencia", 180)
                                .contenido("Técnica", 90))
                        .addMicrociclo(x -> x
                                .tipo(MicrocicloTipo.RESTABLECIMIENTO)
                                .inicio(LocalDate.of(2025, 8, 15))
                                .fin(LocalDate.of(2025, 8, 21))
                                .porcentajeCarga(40)
                                .contenido("Movilidad", 90)
                                .contenido("Recuperación", 120))
                )
                .addEvento(e -> e
                        .tipo(EventoDeportivo.Tipo.COMPETENCIA)
                        .nombre("Amistoso vs León Elite")
                        .fecha(LocalDate.of(2025, 9, 6))
                        .notas("Prueba de control de juego posicional"))
                .build());

        scroll.setContent(build);
    }
    private void construirGrid() {
        LocalDate ini = dpInicio.getValue();
        LocalDate fin = dpFin.getValue();
        grid = new CalendarioGrid(ini, fin);
        Region content = grid;
        content.setStyle("-fx-padding: 10;");
       // scroll.setContent(content);

        // ... tu creación de CalendarGrid
        this.vistaCalendario = grid; // donde grid es tu CalendarGrid
        if (vistaActual == Vista.CALENDARIO) scroll.setContent(vistaCalendario);
    }

    private void nuevoPlan() {
        if (dpInicio.getValue() == null || dpFin.getValue() == null) return;
        var plan = service.crearPlanBasico("Plan " + System.currentTimeMillis(),
                dpInicio.getValue(), dpFin.getValue(), cbTipo.getValue());
        taNotas.setText(plan.getNotas());
        construirGrid();
        new Alert(Alert.AlertType.INFORMATION, "Plan creado (ID: " + plan.getId() + ")").showAndWait();
    }

    private void autoATR() {
        // Versión inicial: solo asegura 16 semanas y reconstruye grid
        dpFin.setValue(dpInicio.getValue().plusWeeks(16));
        construirGrid();
        new Alert(Alert.AlertType.INFORMATION, "Plantilla ATR básica de 16 semanas. Ajusta rangos con Shift+Click.").showAndWait();
    }

    private void exportarPNG() {
        try {
            WritableImage img = scroll.getContent().snapshot(null, null);
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagen PNG","*.png"));
            fc.setInitialFileName("plan_grafico.png");
            File f = fc.showSaveDialog(scroll.getScene().getWindow());
            if (f != null) {
                ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(img, null), "png", f);
                new Alert(Alert.AlertType.INFORMATION, "Exportado: " + f.getAbsolutePath()).showAndWait();
            }
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Error exportando: " + ex.getMessage()).showAndWait();
        }
    }

    private void guardarNotas() {
        new Alert(Alert.AlertType.INFORMATION, "Notas guardadas (ejemplo).").showAndWait();
    }
}

