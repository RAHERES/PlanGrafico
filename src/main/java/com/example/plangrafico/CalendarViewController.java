package com.example.plangrafico;

import com.calendarfx.view.RequestEvent;
import com.calendarfx.view.YearView;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.Region;

import java.net.URL;
import java.time.LocalDate;
import java.time.Month;
import java.util.ResourceBundle;

public class CalendarViewController implements Initializable {

    @FXML
    public YearView yearView;

    private PlanGraficoCalendarAdapter adapter;

    public void setPlan(PlanGrafico plan) {
        this.adapter = new PlanGraficoCalendarAdapter(plan);
        refrescarVista(); // fuerza repintado del calendario
    }

    // Llamado por tu motor de celdas al “pintar” cada día:
    public void decorateDay(LocalDate date, Region cell) {
        var meta = adapter.get(date);
        if (meta == null) {
            cell.getStyleClass().removeIf(s-> s.startsWith("micro-") || s.startsWith("borde-") || s.equals("tiene-evento"));
       //     cell.setTooltip(null);
            cell.setStyle(""); // reset
            return;
        }

        // Fondo (si tu celda acepta inline style)
        cell.setStyle("-fx-background-color: " + toRgba(meta.fondo) + ";");

        // Clases CSS
        cell.getStyleClass().addAll(meta.styleClasses);

        // Tooltip
     //   cell.setTooltip(adapter.buildTooltip(date));

        // Context menu
        cell.setOnContextMenuRequested(ev -> {
            var cm = adapter.buildContextMenu(
                    date,
                    () -> mostrarDialogoMicro(meta.microciclo),
                    () -> mostrarDialogoMeso(meta.mesociclo),
                    () -> abrirDialogoAgregarEvento(date)
            );
            if (cm != null) cm.show(cell, ev.getScreenX(), ev.getScreenY());
            ev.consume();
        });

        // (Opcional) dibujar borde grueso con pseudoclases o estilos extra:
        // usa tus CSS .borde-inicio-micro, etc.
    }

    private String toRgba(javafx.scene.paint.Color c) {
        return String.format("rgba(%d,%d,%d,%.3f)",
                (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255), c.getOpacity());
    }

    private void mostrarDialogoMicro(Microciclo mic) {
        // abre tu detalle de micro: contenidos, minutos, %carga, etc.
    }

    private void mostrarDialogoMeso(Mesociclo meso) {
        // abre tu detalle de meso: tipo, semanas, promedios, etc.
    }

    private void abrirDialogoAgregarEvento(LocalDate date) {
        // abre un diálogo y luego haces plan.getEventos().add(...)
        // y adapter.reload() si creas el método para reindexar.
    }

    private void refrescarVista() {
        // llama al método que haga “layout”/repintado en tu calendario
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LocalDate date1 = LocalDate.of(2023, Month.DECEMBER, 20);
        yearView.setDate(date1);
        yearView.addEventHandler(RequestEvent.ANY, e->{
            System.out.println("YearView Event: " + e.toString());
            System.out.println("YearView EventType: " + e.getEventType());
            System.out.println("YearView Sourece: " + e.getSource());
            LocalDate date = yearView.getDate();
            LocalDate today = yearView.getToday();
            System.out.println("LocalDate today: "+ today);
            yearView.setToday(date1);
            yearView.goToday();


        });

    }
}
