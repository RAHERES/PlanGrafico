package com.example.plangrafico;
/*


import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

public class CalendarioAnualDialog {

    private LocalDate selectedDate = LocalDate.now();
    private Label yearLabel;
    private TilePane monthsGrid; // 3x4
    private final List<String> monthsES = Arrays.asList(
            "Enero","Febrero","Marzo","Abril","Mayo","Junio",
            "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"
    );

    private LocalDate fechaInicio = null;
    private LocalDate fechaFin = null;


    */
/** Muestra el calendario anual y devuelve la fecha seleccionada en el Label recibido. *//*

    public void mostrar(Stage owner, Label fechaCaducidad) {
        Stage ventana = new Stage();
        ventana.setTitle("Calendario anual");
        ventana.initModality(Modality.WINDOW_MODAL);
        ventana.initOwner(owner);

        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #F7F7F7;");

        // Header: año con navegación
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER);

        Button prevYear = new Button("<");
        prevYear.setOnAction(e -> cambiarAnio(-1));
        estilizarBotonHeader(prevYear);

        yearLabel = new Label(String.valueOf(selectedDate.getYear()));
        yearLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Button nextYear = new Button(">");
        nextYear.setOnAction(e -> cambiarAnio(1));
        estilizarBotonHeader(nextYear);

        header.getChildren().addAll(prevYear, yearLabel, nextYear);

        // Contenedor 3x4 de meses (en un ScrollPane por si el alto de pantalla es corto)
     */
/*   monthsGrid = new GridPane();
        monthsGrid.setHgap(12);
        monthsGrid.setVgap(12);
*//*

         monthsGrid = new TilePane();
        monthsGrid.setHgap(12);
        monthsGrid.setVgap(12);
        monthsGrid.setPrefColumns(3); // número preferido, pero se ajusta dinámicamente
        monthsGrid.setTileAlignment(Pos.TOP_CENTER);
        monthsGrid.setPadding(new Insets(10));

        ScrollPane scroller = new ScrollPane(monthsGrid);
        scroller.setFitToWidth(true);
        scroller.setStyle("-fx-background-color:transparent;");

        reconstruirMeses(); // construye los 12 meses del año actual

        // Footer
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.BOTTOM_RIGHT);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setOnAction(e -> ventana.close());
        btnCancelar.setStyle("-fx-background-color:#E0E0E0; -fx-text-fill:black;");

        Button btnOk = new Button("OK");
        btnOk.setOnAction(e -> {
            fechaCaducidad.setText(selectedDate.toString());
            ventana.close();
        });
        btnOk.setStyle("-fx-background-color:#E0E0E0; -fx-text-fill:black;");

        footer.getChildren().addAll(btnCancelar, btnOk);

        root.getChildren().addAll(header, scroller, footer);

        Scene scene = new Scene(root, 920, 680);
        ventana.setScene(scene);
        ventana.showAndWait();
    }

    private void cambiarAnio(int delta) {
        selectedDate = selectedDate.plusYears(delta);
        yearLabel.setText(String.valueOf(selectedDate.getYear()));
        reconstruirMeses();
    }

*/
/*    *//*
*/
/** Construye/actualiza las 12 vistas de mes del año mostrado. *//*
*/
/*
    private void reconstruirMeses() {
        monthsGrid.getChildren().clear();
        int year = selectedDate.getYear();

        // 3 columnas x 4 filas
        int col = 0, row = 0;
        for (int m = 1; m <= 12; m++) {
            YearMonth ym = YearMonth.of(year, m);
            VBox monthPane = crearMiniCalendario(ym);
            monthsGrid.add(monthPane, col, row);

            col++;
            if (col == 3) { col = 0; row++; }
        }
    }*//*


    private void reconstruirMeses() {
        monthsGrid.getChildren().clear();
        int year = selectedDate.getYear();

        for (int m = 1; m <= 12; m++) {
            YearMonth ym = YearMonth.of(year, m);
            VBox monthPane = crearMiniCalendario(ym);
            monthPane.setPrefWidth(250); // ancho mínimo de cada "tarjeta" de mes
            monthsGrid.getChildren().add(monthPane);
        }
    }

    */
/** Crea un panel de un mes con su grilla de días. *//*

    private VBox crearMiniCalendario(YearMonth ym) {
        VBox box = new VBox(6);
        box.setPadding(new Insets(8));
        box.setStyle("-fx-background-color:white; -fx-border-color:#DDD; -fx-border-radius:8; -fx-background-radius:8;");
        box.setPrefWidth(280); // ancho sugerido para 3 por fila

        // Encabezado del mes
        Label lblMes = new Label(monthsES.get(ym.getMonthValue() - 1) + " " + ym.getYear());
        lblMes.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        // Grilla del mes (7x6) con encabezados de días
        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);

        String[] weekDays = {"D","L","M","M","J","V","S"}; // Domingo…Sábado
        for (int i = 0; i < weekDays.length; i++) {
            Label d = new Label(weekDays[i]);
            d.setMinWidth(32);
            d.setAlignment(Pos.CENTER);
            d.setStyle("-fx-font-weight:bold;");
            grid.add(d, i, 0);
        }

        // Calcular offset: DayOfWeek.getValue(): L=1..D=7, queremos D=0..S=6
        int firstDayColumn = mapSundayZero(ym.atDay(1).getDayOfWeek());
        int daysInMonth = ym.lengthOfMonth();

        int col = firstDayColumn;
        int row = 1;

        for (int day = 1; day <= daysInMonth; day++) {
            Button b = new Button(String.valueOf(day));
            b.setMinSize(32, 32);
            b.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            b.setStyle("-fx-background-color:white; -fx-text-fill:black; -fx-border-color:transparent;");

            LocalDate thisDate = ym.atDay(day);

            // resaltado si es la fecha seleccionada
            if (thisDate.equals(selectedDate)) {
                b.setStyle("-fx-background-color:#616161; -fx-text-fill:white; -fx-border-radius:50; -fx-background-radius:50;");
            }

            // hover
            b.addEventHandler(MouseEvent.MOUSE_ENTERED, e ->
                    b.setStyle("-fx-background-color:#EEEEEE; -fx-text-fill:black; -fx-border-color:transparent;"));
            b.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
                if (thisDate.equals(selectedDate)) {
                    b.setStyle("-fx-background-color:#616161; -fx-text-fill:white; -fx-border-radius:50; -fx-background-radius:50;");
                } else {
                    b.setStyle("-fx-background-color:white; -fx-text-fill:black; -fx-border-color:transparent;");
                }
            });

            b.setOnAction(e -> {
                selectedDate = thisDate;
                // reconstruimos todo para refrescar el highlight consistente en los 12 meses
                yearLabel.setText(String.valueOf(selectedDate.getYear()));
                reconstruirMeses();
            });

            grid.add(b, col, row);
            col++;
            if (col == 7) { col = 0; row++; }
        }

        // resaltado si es inicio o fin
        if (thisDate.equals(fechaInicio) || thisDate.equals(fechaFin)) {
            b.setStyle("-fx-background-color:#1976D2; -fx-text-fill:white; -fx-border-radius:50; -fx-background-radius:50;");
        }
// resaltado si está dentro del rango
        else if (fechaInicio != null && fechaFin != null &&
                (thisDate.isAfterx(fechaInicio) && thisDate.isBefore(fechaFin))) {
            b.setStyle("-fx-background-color:#90CAF9; -fx-text-fill:black;");
        }


        box.getChildren().addAll(lblMes, grid);
        return box;
    }

    */
/** Mapea DayOfWeek a columnas con domingo como 0. L=1..D=7 -> D=0,L=1,...,S=6 *//*

    private int mapSundayZero(DayOfWeek dow) {
        int v = dow.getValue(); // L=1..D=7
        return v % 7;           // L=1->1 ... S=6->6, D=7->0
    }

    private void estilizarBotonHeader(Button b) {
        b.setStyle("-fx-background-color:#E0E0E0; -fx-text-fill:black;");
        b.setMinWidth(36);
    }

    public LocalDate getSelectedDate() { return selectedDate; }
}

*/
/*
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;

public class CalendarioAnualDialog {

    private LocalDate selectedDate = LocalDate.now();
    private LocalDate fechaInicio = null;
    private LocalDate fechaFin = null;

    private Label yearLabel;
    private TilePane monthsGrid;

    // Fechas seleccionadas (se usan para “días de entrenamiento en rango” y/o doble clic)
    private final Set<LocalDate> fechasMultiples = new HashSet<>();

    // Días de entrenamiento que el usuario marca con checkboxes
    private final Set<DayOfWeek> diasEntrenamientoSeleccionados = new HashSet<>();

    private final List<String> monthsES = Arrays.asList(
            "Enero","Febrero","Marzo","Abril","Mayo","Junio",
            "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"
    );

    // Estilos (para no repetir strings)
    private static final String STYLE_NORMAL =
            "-fx-background-color:white; -fx-text-fill:black; -fx-border-color:transparent;";
    private static final String STYLE_RANGE_END =
            "-fx-background-color:#1976D2; -fx-text-fill:white; -fx-border-radius:50; -fx-background-radius:50;";
    private static final String STYLE_RANGE_INNER =
            "-fx-background-color:#90CAF9; -fx-text-fill:black;";
    private static final String STYLE_TRAINING =
            "-fx-background-color:#4CAF50; -fx-text-fill:white; -fx-border-radius:50; -fx-background-radius:50;";

    public void mostrar(Stage owner, Label fechaCaducidad) {
        Stage ventana = new Stage();
        ventana.setTitle("Calendario anual");
        ventana.initModality(Modality.WINDOW_MODAL);
        ventana.initOwner(owner);

        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #F7F7F7;");

        // === Header (año) ===
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER);

        Button prevYear = new Button("<");
        prevYear.setOnAction(e -> cambiarAnio(-1));
        estilizarBotonHeader(prevYear);

        yearLabel = new Label(String.valueOf(selectedDate.getYear()));
        yearLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Button nextYear = new Button(">");
        nextYear.setOnAction(e -> cambiarAnio(1));
        estilizarBotonHeader(nextYear);

        header.getChildren().addAll(prevYear, yearLabel, nextYear);

        // === Selector de días de entrenamiento (checkboxes) ===
        HBox selectorDias = new HBox(10);
        selectorDias.setAlignment(Pos.CENTER);
        // Orden L–D
        DayOfWeek[] orden = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY};
        for (DayOfWeek dow : orden) {
            String etiqueta = dow.getDisplayName(TextStyle.SHORT, new Locale("es", "ES"));
            CheckBox check = new CheckBox(etiqueta);
            check.setOnAction(e -> {
                if (check.isSelected()) diasEntrenamientoSeleccionados.add(dow);
                else diasEntrenamientoSeleccionados.remove(dow);
                aplicarFiltroDiasEntrenamiento();  // recalcula fechasMultiples según rango + días marcados
                reconstruirMeses();                 // repinta
            });
            selectorDias.getChildren().add(check);
        }

        // === Contenedor de meses ===
        monthsGrid = new TilePane();
        monthsGrid.setHgap(12);
        monthsGrid.setVgap(12);
        monthsGrid.setPrefColumns(3);
        monthsGrid.setTileAlignment(Pos.TOP_CENTER);
        monthsGrid.setPadding(new Insets(10));

        reconstruirMeses();

        // === Footer ===
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.BOTTOM_RIGHT);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setOnAction(e -> ventana.close());
        btnCancelar.setStyle("-fx-background-color:#E0E0E0; -fx-text-fill:black;");

        Button btnOk = new Button("OK");
        btnOk.setOnAction(e -> {
            if (fechaInicio != null && fechaFin != null) {
                fechaCaducidad.setText(fechaInicio + " hasta " + fechaFin);
            } else if (fechaInicio != null) {
                fechaCaducidad.setText(fechaInicio.toString());
            }
            ventana.close();
        });
        btnOk.setStyle("-fx-background-color:#E0E0E0; -fx-text-fill:black;");

        footer.getChildren().addAll(btnCancelar, btnOk);

        // Layout: encabezado, selector, meses, footer
        root.getChildren().addAll(header, selectorDias, monthsGrid, footer);

        Scene scene = new Scene(root, 1100, 700);
        ventana.setScene(scene);
        ventana.showAndWait();
    }

    // Recalcula las fechas seleccionadas (fechasMultiples) según el rango y los días de entrenamiento marcados
    private void aplicarFiltroDiasEntrenamiento() {
        fechasMultiples.clear();
        if (fechaInicio == null || fechaFin == null) return;
        if (diasEntrenamientoSeleccionados.isEmpty()) return;

        LocalDate cur = fechaInicio.isBefore(fechaFin) ? fechaInicio : fechaFin;
        LocalDate end = fechaFin.isAfter(fechaInicio) ? fechaFin : fechaInicio;

        while (!cur.isAfter(end)) {
            if (diasEntrenamientoSeleccionados.contains(cur.getDayOfWeek())) {
                fechasMultiples.add(cur);
            }
            cur = cur.plusDays(1);
        }
    }

    private void cambiarAnio(int delta) {
        selectedDate = selectedDate.plusYears(delta);
        yearLabel.setText(String.valueOf(selectedDate.getYear()));
        reconstruirMeses();
    }

    private void reconstruirMeses() {
        monthsGrid.getChildren().clear();
        int year = selectedDate.getYear();
        for (int m = 1; m <= 12; m++) {
            YearMonth ym = YearMonth.of(year, m);
            VBox monthPane = crearMiniCalendario(ym);
            monthPane.setPrefWidth(250);
            monthsGrid.getChildren().add(monthPane);
        }
    }

    private VBox crearMiniCalendario(YearMonth ym) {
        VBox box = new VBox(6);
        box.setPadding(new Insets(8));
        box.setStyle("-fx-background-color:white; -fx-border-color:#DDD; -fx-border-radius:8; -fx-background-radius:8;");
        box.setPrefWidth(260);

        Label lblMes = new Label(monthsES.get(ym.getMonthValue() - 1) + " " + ym.getYear());
        lblMes.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);

        String[] weekDays = {"D","L","M","M","J","V","S"};
        for (int i = 0; i < weekDays.length; i++) {
            Label d = new Label(weekDays[i]);
            d.setMinWidth(32);
            d.setAlignment(Pos.CENTER);
            d.setStyle("-fx-font-weight:bold;");
            grid.add(d, i, 0);
        }

        int firstDayColumn = mapSundayZero(ym.atDay(1).getDayOfWeek());
        int daysInMonth = ym.lengthOfMonth();

        int col = firstDayColumn;
        int row = 1;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate thisDate = ym.atDay(day);

            Button b = new Button(String.valueOf(day));
            b.setMinSize(32, 32);
            b.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

            aplicarEstilo(b, thisDate);

            // Clic simple = define rango (inicio/fin)
            b.setOnAction(e -> {
                if (fechaInicio == null || fechaFin != null) {
                    fechaInicio = thisDate;
                    fechaFin = null;
                } else {
                    if (thisDate.isBefore(fechaInicio)) {
                        fechaFin = fechaInicio;
                        fechaInicio = thisDate;
                    } else {
                        fechaFin = thisDate;
                    }
                }
                // si hay días de entrenamiento seleccionados, filtra inmediatamente
                aplicarFiltroDiasEntrenamiento();
                reconstruirMeses();
            });

            // Doble clic = alternar esa fecha en “fechasMultiples” (opcional)
            b.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    if (fechasMultiples.contains(thisDate)) fechasMultiples.remove(thisDate);
                    else fechasMultiples.add(thisDate);
                    aplicarEstilo(b, thisDate);
                }
            });

            grid.add(b, col, row);
            col++;
            if (col == 7) { col = 0; row++; }
        }

        box.getChildren().addAll(lblMes, grid);
        return box;
    }

    private void aplicarEstilo(Button b, LocalDate thisDate) {
        boolean hayRango = (fechaInicio != null && fechaFin != null);
        boolean filtroEntrenamientoActivo = hayRango && !diasEntrenamientoSeleccionados.isEmpty();

        // --- PRIORIDAD 1: cuando el filtro de entrenamiento está activo,
        //     SOLO pintamos los días que estén en fechasMultiples (entrenamiento).
        if (filtroEntrenamientoActivo) {
            if (fechasMultiples.contains(thisDate)) {
                b.setStyle(STYLE_TRAINING);
            } else {
                b.setStyle(STYLE_NORMAL);
            }
            return;
        }

        // --- PRIORIDAD 2: sin filtro activo, mostrar rango normal + múltiples (si las hay)
        if (thisDate.equals(fechaInicio)) {
            // Fecha de inicio (azul fuerte)
            b.setStyle("-fx-background-color:#1976D2; -fx-text-fill:white; -fx-border-radius:50; -fx-background-radius:50;");
        }
        else if (thisDate.equals(fechaFin)) {
            // Fecha final (rojo)
            b.setStyle("-fx-background-color:#E53935; -fx-text-fill:white; -fx-border-radius:50; -fx-background-radius:50;");
        }
        else if (fechaInicio != null && fechaFin != null &&
                (thisDate.isAfter(fechaInicio) && thisDate.isBefore(fechaFin))) {
            // Rango entre inicio y fin (azul claro)
            b.setStyle("-fx-background-color:#90CAF9; -fx-text-fill:black;");
        }
        else if (fechasMultiples.contains(thisDate)) {
            // Selecciones múltiples manuales (doble clic)
            b.setStyle("-fx-background-color:#4CAF50; -fx-text-fill:white; -fx-border-radius:50; -fx-background-radius:50;");
        }
        else {
            // Estado normal
            b.setStyle("-fx-background-color:white; -fx-text-fill:black; -fx-border-color:transparent;");
        }
    }

    private int mapSundayZero(DayOfWeek dow) {
        int v = dow.getValue(); // L=1..D=7
        return v % 7;           // L=1->1 ... S=6->6, D=7->0
    }

    private void estilizarBotonHeader(Button b) {
        b.setStyle("-fx-background-color:#E0E0E0; -fx-text-fill:black;");
        b.setMinWidth(36);
    }

    public LocalDate getSelectedDate() { return selectedDate; }
}
*/
/*
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.*;
import java.time.format.TextStyle;
import java.util.*;

public class CalendarioAnualDialog {

    private LocalDate selectedDate = LocalDate.now();
    private LocalDate fechaInicio = null;
    private LocalDate fechaFin = null;

    private Label yearLabel;
    private TilePane monthsGrid;

    // Selección manual por doble clic (si NO usas filtro de entrenamiento)
    private final Set<LocalDate> fechasMultiples = new HashSet<>();

    // Días de entrenamiento elegidos por el usuario
    private final Set<DayOfWeek> diasEntrenamientoSeleccionados = new HashSet<>();

    private final List<String> monthsES = Arrays.asList(
            "Enero","Febrero","Marzo","Abril","Mayo","Junio",
            "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"
    );

    // Estilos (misma “figura” azul de tu segunda imagen)
    private static final String ROUNDED = "-fx-background-radius:6; -fx-border-radius:6;";
    private static final String STYLE_NORMAL     = "-fx-background-color:white; -fx-text-fill:black; -fx-border-color:transparent;";
    private static final String STYLE_START      = "-fx-background-color:#1976D2; -fx-text-fill:white;" + ROUNDED; // inicio azul fuerte
    private static final String STYLE_END        = "-fx-background-color:#E53935; -fx-text-fill:white;" + ROUNDED; // fin rojo
    private static final String STYLE_SELECTED   = "-fx-background-color:#90CAF9; -fx-text-fill:black;" + ROUNDED; // “azul de selección”

    public void mostrar(Stage owner, Label fechaCaducidad) {
        Stage ventana = new Stage();
        ventana.setTitle("Calendario anual");
        ventana.initModality(Modality.WINDOW_MODAL);
        ventana.initOwner(owner);

        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #F7F7F7;");

        // === Header ===
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER);

        Button prevYear = new Button("<");
        prevYear.setOnAction(e -> cambiarAnio(-1));
        estilizarBotonHeader(prevYear);

        yearLabel = new Label(String.valueOf(selectedDate.getYear()));
        yearLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Button nextYear = new Button(">");
        nextYear.setOnAction(e -> cambiarAnio(1));
        estilizarBotonHeader(nextYear);

        header.getChildren().addAll(prevYear, yearLabel, nextYear);

        // === Checkboxes de días de entrenamiento ===
        HBox selectorDias = new HBox(10);
        selectorDias.setAlignment(Pos.CENTER);
        DayOfWeek[] orden = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY};
        for (DayOfWeek dow : orden) {
            String etq = dow.getDisplayName(TextStyle.SHORT, new Locale("es", "ES"));
            CheckBox check = new CheckBox(etq);
            check.setOnAction(e -> {
                if (check.isSelected()) diasEntrenamientoSeleccionados.add(dow);
                else diasEntrenamientoSeleccionados.remove(dow);
                reconstruirMeses(); // repinta según el filtro activo
            });
            selectorDias.getChildren().add(check);
        }

        // === Meses ===
        monthsGrid = new TilePane();
        monthsGrid.setHgap(12);
        monthsGrid.setVgap(12);
        monthsGrid.setPrefColumns(3);
        monthsGrid.setTileAlignment(Pos.TOP_CENTER);
        monthsGrid.setPadding(new Insets(10));

        reconstruirMeses();

        // === Footer ===
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.BOTTOM_RIGHT);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setOnAction(e -> ventana.close());
        btnCancelar.setStyle("-fx-background-color:#E0E0E0; -fx-text-fill:black;");

        Button btnOk = new Button("OK");
        btnOk.setOnAction(e -> {
            if (fechaInicio != null && fechaFin != null) {
                fechaCaducidad.setText(fechaInicio + " hasta " + fechaFin);
            } else if (fechaInicio != null) {
                fechaCaducidad.setText(fechaInicio.toString());
            }
            ventana.close();
        });
        btnOk.setStyle("-fx-background-color:#E0E0E0; -fx-text-fill:black;");

        footer.getChildren().addAll(btnCancelar, btnOk);

        root.getChildren().addAll(header, selectorDias, monthsGrid, footer);

        Scene scene = new Scene(root, 1200, 720);
        ventana.setScene(scene);
        ventana.showAndWait();
    }

    private void cambiarAnio(int delta) {
        selectedDate = selectedDate.plusYears(delta);
        yearLabel.setText(String.valueOf(selectedDate.getYear()));
        reconstruirMeses();
    }

    private void reconstruirMeses() {
        monthsGrid.getChildren().clear();
        int year = selectedDate.getYear();
        for (int m = 1; m <= 12; m++) {
            YearMonth ym = YearMonth.of(year, m);
            VBox monthPane = crearMiniCalendario(ym);
            monthPane.setPrefWidth(250);
            monthsGrid.getChildren().add(monthPane);
        }
    }

    private VBox crearMiniCalendario(YearMonth ym) {
        VBox box = new VBox(6);
        box.setPadding(new Insets(8));
        box.setStyle("-fx-background-color:white; -fx-border-color:#DDD; -fx-border-radius:8; -fx-background-radius:8;");
        box.setPrefWidth(260);

        Label lblMes = new Label(monthsES.get(ym.getMonthValue() - 1) + " " + ym.getYear());
        lblMes.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);

        String[] weekDays = {"D","L","M","M","J","V","S"};
        for (int i = 0; i < weekDays.length; i++) {
            Label d = new Label(weekDays[i]);
            d.setMinWidth(32);
            d.setAlignment(Pos.CENTER);
            d.setStyle("-fx-font-weight:bold;");
            grid.add(d, i, 0);
        }

        int firstDayColumn = mapSundayZero(ym.atDay(1).getDayOfWeek());
        int daysInMonth = ym.lengthOfMonth();

        int col = firstDayColumn;
        int row = 1;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate thisDate = ym.atDay(day);

            Button b = new Button(String.valueOf(day));
            b.setMinSize(32, 32);
            b.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

            aplicarEstilo(b, thisDate);

            // Clic simple = define rango (inicio/fin)
            b.setOnAction(e -> {
                if (fechaInicio == null || fechaFin != null) {
                    fechaInicio = thisDate;
                    fechaFin = null;
                } else {
                    if (thisDate.isBefore(fechaInicio)) {
                        fechaFin = fechaInicio;
                        fechaInicio = thisDate;
                    } else {
                        fechaFin = thisDate;
                    }
                }
                reconstruirMeses();
            });

            // Doble clic = alternar manual (solo si NO hay filtro activo)
            b.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && diasEntrenamientoSeleccionados.isEmpty()) {
                    if (fechasMultiples.contains(thisDate)) fechasMultiples.remove(thisDate);
                    else fechasMultiples.add(thisDate);
                    aplicarEstilo(b, thisDate);
                }
            });

            grid.add(b, col, row);
            col++;
            if (col == 7) { col = 0; row++; }
        }

        box.getChildren().addAll(lblMes, grid);
        return box;
    }

    private void aplicarEstilo(Button b, LocalDate thisDate) {
        boolean hayRango = (fechaInicio != null && fechaFin != null);
        LocalDate min = null, max = null;
        if (hayRango) {
            min = fechaInicio.isBefore(fechaFin) ? fechaInicio : fechaFin;
            max = fechaFin.isAfter(fechaInicio) ? fechaFin : fechaInicio;
        }

        // 1) inicio/fin siempre visibles
        if (thisDate.equals(fechaInicio)) { b.setStyle(STYLE_START); return; }
        if (thisDate.equals(fechaFin))     { b.setStyle(STYLE_END);   return; }

        // 2) si hay filtro de entrenamiento activo -> solo pintamos esos días del rango en AZUL (misma figura)
        boolean filtroActivo = hayRango && !diasEntrenamientoSeleccionados.isEmpty();
        if (filtroActivo) {
            boolean dentro = !thisDate.isBefore(min) && !thisDate.isAfter(max);
            if (dentro && diasEntrenamientoSeleccionados.contains(thisDate.getDayOfWeek())) {
                b.setStyle(STYLE_SELECTED);  // azul
            } else {
                b.setStyle(STYLE_NORMAL);
            }
            return;
        }

        // 3) sin filtro: mostrar rango completo en azul (como antes) + manuales (doble clic)
        if (hayRango && thisDate.isAfter(min) && thisDate.isBefore(max)) {
            b.setStyle(STYLE_SELECTED);
        } else if (fechasMultiples.contains(thisDate)) {
            b.setStyle(STYLE_SELECTED);
        } else {
            b.setStyle(STYLE_NORMAL);
        }
    }

    private int mapSundayZero(DayOfWeek dow) {
        int v = dow.getValue(); // L=1..D=7
        return v % 7;           // L=1->1 ... S=6->6, D=7->0
    }

    private void estilizarBotonHeader(Button b) {
        b.setStyle("-fx-background-color:#E0E0E0; -fx-text-fill:black;");
        b.setMinWidth(36);
    }

    private void mostrarSelectorHorario(DayOfWeek dow) {
        Stage dialog = new Stage();
        dialog.setTitle("Asignar horario para " + dow);
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));

        // Spinners para hora inicio
        Spinner<Integer> horaInicio = new Spinner<>(0, 23, 16);
        Spinner<Integer> minutoInicio = new Spinner<>(0, 59, 30);

        // Spinners para hora fin
        Spinner<Integer> horaFin = new Spinner<>(0, 23, 18);
        Spinner<Integer> minutoFin = new Spinner<>(0, 59, 0);

        HBox inicioBox = new HBox(5, new Label("Inicio:"), horaInicio, new Label(":"), minutoInicio);
        HBox finBox = new HBox(5, new Label("Fin:"), horaFin, new Label(":"), minutoFin);

        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            String horario = String.format("%02d:%02d - %02d:%02d",
                    horaInicio.getValue(), minutoInicio.getValue(),
                    horaFin.getValue(), minutoFin.getValue());
            horariosPorDia.put(dow, horario);
            aplicarFiltroDiasEntrenamiento();
            reconstruirMeses();
            dialog.close();
        });

        box.getChildren().addAll(inicioBox, finBox, okBtn);

        Scene scene = new Scene(box, 300, 200);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    public LocalDate getSelectedDate() { return selectedDate; }
}
*/
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.*;
import java.time.format.TextStyle;
import java.util.*;

public class CalendarioAnualDialog {

    private LocalDate selectedDate = LocalDate.now();
    private LocalDate fechaInicio = null;
    private LocalDate fechaFin = null;

    private Label yearLabel;
    private TilePane monthsGrid;

    // Selección manual
    private final Set<LocalDate> fechasMultiples = new HashSet<>();

    // Días de entrenamiento elegidos
    private final Set<DayOfWeek> diasEntrenamientoSeleccionados = new HashSet<>();

    // Horarios asociados a cada día de la semana
    private final Map<DayOfWeek, String> horariosPorDia = new HashMap<>();

    private final List<String> monthsES = Arrays.asList(
            "Enero","Febrero","Marzo","Abril","Mayo","Junio",
            "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"
    );
    // Debajo del calendario, en vez de un Label, usamos un VBox con secciones
    private VBox resumenBox;

    // Estilos
    private static final String ROUNDED = "-fx-background-radius:6; -fx-border-radius:6;";
    private static final String STYLE_NORMAL     = "-fx-background-color:white; -fx-text-fill:black; -fx-border-color:transparent;";
    private static final String STYLE_START      = "-fx-background-color:#1976D2; -fx-text-fill:white;" + ROUNDED;
    private static final String STYLE_END        = "-fx-background-color:#E53935; -fx-text-fill:white;" + ROUNDED;
    private static final String STYLE_SELECTED   = "-fx-background-color:#90CAF9; -fx-text-fill:black;" + ROUNDED;

    private Label lblResumen = new Label("Selecciona fechas y días para ver resumen..."); // 👈 nuevo label para mostrar info del programa

    // Overrides manuales (no se pierden al tener rango activo)
    private final Set<LocalDate> overrideOn  = new HashSet<>(); // forzar seleccionar un día
    private final Set<LocalDate> overrideOff = new HashSet<>(); // forzar deseleccionar un día

    /**Revisa si hay una fecha seleccionada*/
    private boolean hayRango() {
        return fechaInicio != null && fechaFin != null;
    }

    private LocalDate minDate() {
        return (fechaInicio.isBefore(fechaFin) ? fechaInicio : fechaFin);
    }

    private LocalDate maxDate() {
        return (fechaFin.isAfter(fechaInicio) ? fechaFin : fechaInicio);
    }

    /** Selección calculada por rango y filtro de días de entrenamiento */
    private boolean seleccionCalculada(LocalDate d) {
        if (!hayRango()) return false;
        if (d.isBefore(minDate()) || d.isAfter(maxDate())) return false;

        // Si NO hay filtro de días de entrenamiento, todo el tramo es "seleccionado" (como antes)
        if (diasEntrenamientoSeleccionados.isEmpty()) return true;

        // Con filtro activo: solo los días seleccionados
        return diasEntrenamientoSeleccionados.contains(d.getDayOfWeek());
    }

/*
    */
    /** Selección efectiva = (overrideOn) o (selecciónCalculada y no overrideOff) *//*

    private boolean seleccionadoEfectivo(LocalDate d) {
        if (overrideOn.contains(d)) return true;
        if (overrideOff.contains(d)) return false;
        return seleccionCalculada(d);
    }
*/

    /*private boolean seleccionadoEfectivo(LocalDate d) {
        if (diasAnclados.contains(d)) return true;   // fuerza ON
        if (diasExcluidos.contains(d)) return false; // fuerza OFF
        return seleccionCalculada(d);                // rango ± filtro
    }*/
    private boolean seleccionadoEfectivo(LocalDate d) {
        // 1) Anclados mandan
        if (diasAnclados.contains(d)) return true;

        // 2) Overrides explícitos
        if (overrideOff.contains(d)) return false;
        if (overrideOn.contains(d))  return true;

        // 3) Lógica del rango/filtro normal
        return seleccionCalculada(d);
    }


    private void toggleDia(LocalDate d) {
        if (seleccionadoEfectivo(d)) {
            // estaba seleccionado -> fuerzo OFF
            diasAnclados.remove(d);
            diasExcluidos.add(d);
        } else {
            // no estaba seleccionado -> fuerzo ON
            diasExcluidos.remove(d);
            diasAnclados.add(d);
        }
    }

  /*  private boolean seleccionadoEfectivo(LocalDate d) {
        // 1) Si está anclado, siempre se pinta
        if (diasAnclados.contains(d)) return true;

        // 2) Si hay rango, aplica la lógica de rango + filtro (lo que ya tienes)
        return seleccionCalculada(d);
    }
*/

    /** Alterna con Ctrl+Click: si hoy está seleccionado -> forzar OFF; si no -> forzar ON */
    private void toggleCtrl(LocalDate d) {
        if (seleccionadoEfectivo(d)) {
            // estaba seleccionado -> forzar deselección
            overrideOn.remove(d);
            overrideOff.add(d);
        } else {
            // no estaba seleccionado -> forzar selección
            overrideOff.remove(d);
            overrideOn.add(d);
        }
    }

    public void mostrar(Stage owner, Label fechaCaducidad) {
        Stage ventana = new Stage();
        ventana.setTitle("Calendario anual");
        ventana.initModality(Modality.WINDOW_MODAL);
        ventana.initOwner(owner);

        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #F7F7F7;");

        // === Header ===
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER);

        Button prevYear = new Button("<");
        prevYear.setOnAction(e -> cambiarAnio(-1));
        estilizarBotonHeader(prevYear);

        yearLabel = new Label(String.valueOf(selectedDate.getYear()));
        yearLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Button nextYear = new Button(">");
        nextYear.setOnAction(e -> cambiarAnio(1));
        estilizarBotonHeader(nextYear);

        header.getChildren().addAll(prevYear, yearLabel, nextYear);

        // === Checkboxes de días de entrenamiento ===
        HBox selectorDias = new HBox(10);
        selectorDias.setAlignment(Pos.CENTER);

        DayOfWeek[] orden = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY};

        for (DayOfWeek dow : orden) {
            String etq = dow.getDisplayName(TextStyle.SHORT, new Locale("es", "ES"));
            CheckBox check = new CheckBox(etq);

            /*check.setOnAction(e -> {
                if (check.isSelected()) {
                    diasEntrenamientoSeleccionados.add(dow);
                    mostrarSelectorHorario(dow); // pedir horario al marcar
                } else {
                    diasEntrenamientoSeleccionados.remove(dow);
                    horariosPorDia.remove(dow);
                    reconstruirMeses();
                }
            });*/
            check.setOnAction(e -> {
                if (check.isSelected()) {
                    diasEntrenamientoSeleccionados.add(dow);
                    /*mostrarSelectorHorario(dow);*/
                    if (!cargandoDesdeArchivo) { // << evita abrir diálogo al cargar
                        mostrarSelectorHorario(dow);
                    }
                } else {
                    diasEntrenamientoSeleccionados.remove(dow);
                    horariosPorDia.remove(dow);
                }
                reconstruirMeses(); // repinta y recalcula resumen
            });

            selectorDias.getChildren().add(check);
        }

        // === Meses ===
        monthsGrid = new TilePane();
        monthsGrid.setHgap(12);
        monthsGrid.setVgap(12);
        monthsGrid.setPrefColumns(3);
        monthsGrid.setTileAlignment(Pos.TOP_CENTER);
        monthsGrid.setPadding(new Insets(10));

        reconstruirMeses();
        resumenBox = new VBox(8);
        resumenBox.setPadding(new Insets(10, 12, 10, 12));
        resumenBox.setStyle("-fx-background-color:#FFFFFF; -fx-border-color:#DDD; -fx-border-radius:8; -fx-background-radius:8;");
        resumenBox.getChildren().add(new Label("Selecciona rango, días y horarios para ver el resumen…"));

// coloca resumenBox debajo del calendario
/*

// un poquito más de alto para que quepa
        ((BorderPane) ventana.getScene().getRoot());

*/


        // === Resumen del programa (nuevo) ===

        lblResumen.setFont(Font.font("Arial", 14));
        lblResumen.setStyle("-fx-text-fill:#333;");

        // === Footer ===
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.BOTTOM_RIGHT);
        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setOnAction(e -> ventana.close());
        Button btnOk = new Button("OK");
        btnOk.setOnAction(e -> ventana.close());
       // footer.getChildren().addAll(btnCancelar, btnOk);

        // === Layout final ===
    //    root.getChildren().addAll(header, selectorDias, monthsGrid, lblResumen, footer);


        btnCancelar.setOnAction(e -> ventana.close());
        btnCancelar.setStyle("-fx-background-color:#E0E0E0; -fx-text-fill:black;");

        btnOk.setOnAction(e -> {
            if (fechaInicio != null && fechaFin != null) {
                fechaCaducidad.setText(fechaInicio + " hasta " + fechaFin);
            } else if (fechaInicio != null) {
                fechaCaducidad.setText(fechaInicio.toString());
            }
            ventana.close();
        });
        btnOk.setStyle("-fx-background-color:#E0E0E0; -fx-text-fill:black;");
        Button btnGuardar = new Button("Guardar…");
        btnGuardar.setOnAction(e -> guardarPrograma());
        //footer.getChildren().add(0, btnGuardar); // lo pone a la izquierda de Cancelar/OK
        Button btnExportIcs = new Button("Exportar .ics");
        btnExportIcs.setOnAction(e -> exportarICS());
        //footer.getChildren().add(0, btnExportIcs); // lo coloca a la izquierda

        Button btnCargar = new Button("Cargar…");
        btnCargar.setOnAction(e -> cargarPrograma());
        //footer.getChildren().add(0, btnCargar); // lo pone a la izquierda


        footer.getChildren().addAll(btnCargar,btnCancelar, btnOk, btnGuardar, btnExportIcs);

/*
        root.getChildren().addAll(header, selectorDias, monthsGrid, footer);
*/
        /*root.getChildren().addAll(header, selectorDias, monthsGrid, lblResumen, footer);*/
        resumenBox.setMaxWidth(Double.MAX_VALUE);
        root.getChildren().addAll(header, selectorDias, monthsGrid, new ScrollPane(resumenBox), footer);

        Scene scene = new Scene(new ScrollPane(root), 1200, 720);
        ventana.setScene(scene);
        ventana.showAndWait();
    }

    private void cambiarAnio(int delta) {
        selectedDate = selectedDate.plusYears(delta);
        yearLabel.setText(String.valueOf(selectedDate.getYear()));
        reconstruirMeses();
    }

    // =================== CARGAR DESDE JSON ===================

    private void cargarPrograma() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Cargar programa (JSON)");
        fc.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Archivo JSON (*.json)", "*.json")
        );
        java.io.File f = fc.showOpenDialog(yearLabel.getScene().getWindow());
        if (f == null) return;

        try {
            String json = java.nio.file.Files.readString(
                    f.toPath(), java.nio.charset.StandardCharsets.UTF_8);

            aplicarDesdeJSON(json);
            alerta("Programa cargado");

        } catch (Exception ex) {
            alerta("Error al cargar:\n" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void aplicarDesdeJSON(String json) {
        // Evita efectos (no abrir diálogos ni repintar intermedio)
        cargandoDesdeArchivo = true;
        try {
            // Limpia estado actual
            diasEntrenamientoSeleccionados.clear();
            horariosPorDia.clear();
            overrideOn.clear();
            overrideOff.clear();

            // ---- Rango ----
            String ini = extraerCampoTexto(json, "\"inicio\"");
            String fin = extraerCampoTexto(json, "\"fin\"");
            if (ini == null || fin == null) {
                throw new IllegalArgumentException("Faltan fechas de inicio/fin en el JSON.");
            }
            fechaInicio = LocalDate.parse(ini);
            fechaFin    = LocalDate.parse(fin);
            selectedDate = fechaInicio; // por si quieres centrar año

            // ---- Días de entrenamiento ----
            List<String> dias = extraerArrayTexto(json, "\"diasEntrenamiento\"");
            for (String s : dias) {
                try {
                    DayOfWeek dow = DayOfWeek.valueOf(s);
                    diasEntrenamientoSeleccionados.add(dow);
                } catch (Exception ignore) {}
            }

            // ---- Horarios por día ----
            Map<String,String> hzMap = extraerObjetoTexto(json, "\"horariosPorDia\"");
            for (Map.Entry<String,String> e : hzMap.entrySet()) {
                try {
                    DayOfWeek dow = DayOfWeek.valueOf(e.getKey());
                    horariosPorDia.put(dow, e.getValue());
                } catch (Exception ignore) {}
            }

            // ---- Overrides ----
            List<String> on  = extraerArrayTexto(json, "\"overrideOn\"");
            List<String> off = extraerArrayTexto(json, "\"overrideOff\"");
            for (String s : on)  overrideOn.add(LocalDate.parse(s));
            for (String s : off) overrideOff.add(LocalDate.parse(s));

            // ---- Reflejar en los checkboxes sin disparar diálogos ----
            for (DayOfWeek d : checksPorDia.keySet()) {
                CheckBox cb = checksPorDia.get(d);
                boolean sel = diasEntrenamientoSeleccionados.contains(d);
                cb.setSelected(sel);
            }

        } finally {
            cargandoDesdeArchivo = false;
        }

        // Repintar y recalcular todo
        yearLabel.setText(String.valueOf(selectedDate.getYear()));

        // limpia eventos anteriores
        eventosPorDia.clear();

// ... otros parseos ...

// eventos
        List<String> bloques = extraerArrayObjetos(json, "\"eventos\"");
        for (String bloque : bloques) {
            String fStr  = extraerCampoTexto(bloque, "\"fecha\"");
            String tit   = extraerCampoTexto(bloque, "\"titulo\"");
            String desc  = extraerCampoTexto(bloque, "\"descripcion\"");
            String ubi   = extraerCampoTexto(bloque, "\"ubicacion\"");
            String iniS  = extraerCampoTexto(bloque, "\"inicio\"");
            String finS  = extraerCampoTexto(bloque, "\"fin\"");
            String recS  = extraerCampoTexto(bloque, "\"recordar\"");
            if (fStr == null || tit == null || iniS == null || finS == null) continue;
            LocalDate f = LocalDate.parse(fStr);
            Evento ev = new Evento(
                    tit,
                    desc == null ? "" : desc,
                    ubi == null ? "" : ubi,
                    LocalTime.parse(iniS),
                    LocalTime.parse(finS),
                    "true".equalsIgnoreCase(recS)
            );
            eventosPorDia.computeIfAbsent(f, k -> new ArrayList<>()).add(ev);
        }

        reconstruirMeses();
    }

    /** Extrae array de objetos JSON plano: "clave": [ { ... }, { ... } ] -> lista de strings con cada {...} */
    private List<String> extraerArrayObjetos(String json, String claveConComillas) {
        List<String> out = new ArrayList<>();
        int p = json.indexOf(claveConComillas);
        if (p < 0) return out;
        int b1 = json.indexOf('[', p);
        int b2 = json.indexOf(']', b1+1);
        if (b1 < 0 || b2 < 0) return out;
        String body = json.substring(b1+1, b2).trim();
        if (body.isEmpty()) return out;

        // separa objetos { ... } de primer nivel
        int depth = 0, start = -1;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    out.add(body.substring(start, i+1).trim());
                    start = -1;
                }
            }
        }
        return out;
    }

// =================== UTILIDADES DE PARSEO SENCILLO ===================

    /** Devuelve el valor de un campo de texto: "clave": "valor" (o null si no existe). */
    private String extraerCampoTexto(String json, String claveConComillas) {
        // Busca "clave": "...."
        int p = json.indexOf(claveConComillas);
        if (p < 0) return null;
        int c = json.indexOf(':', p);
        if (c < 0) return null;
        int q1 = json.indexOf('"', c+1);
        if (q1 < 0) return null;
        int q2 = json.indexOf('"', q1+1);
        if (q2 < 0) return null;
        return json.substring(q1+1, q2);
    }

    /** Extrae array de strings: "clave": ["A","B","C"] -> lista */
    private List<String> extraerArrayTexto(String json, String claveConComillas) {
        List<String> out = new ArrayList<>();
        int p = json.indexOf(claveConComillas);
        if (p < 0) return out;
        int b1 = json.indexOf('[', p);
        int b2 = json.indexOf(']', b1+1);
        if (b1 < 0 || b2 < 0) return out;
        String body = json.substring(b1+1, b2).trim();
        if (body.isEmpty()) return out;

        // separa por comas de alto nivel (no esperamos comillas escapadas en nuestro formato)
        String[] parts = body.split(",");
        for (String item : parts) {
            item = item.trim();
            if (item.startsWith("\"") && item.endsWith("\"") && item.length() >= 2) {
                out.add(item.substring(1, item.length()-1));
            }
        }
        return out;
    }

    /** Extrae objeto { "K":"V", "K2":"V2" } en un mapa String->String */
    private Map<String,String> extraerObjetoTexto(String json, String claveConComillas) {
        Map<String,String> out = new LinkedHashMap<>();
        int p = json.indexOf(claveConComillas);
        if (p < 0) return out;
        int b1 = json.indexOf('{', p);
        int b2 = json.indexOf('}', b1+1);
        if (b1 < 0 || b2 < 0) return out;
        String body = json.substring(b1+1, b2).trim();
        if (body.isEmpty()) return out;

        // Simples pares clave:valor separados por comas (sin anidamiento en nuestro JSON)
        String[] parts = body.split(",");
        for (String kv : parts) {
            int c = kv.indexOf(':');
            if (c < 0) continue;
            String k = kv.substring(0, c).trim();
            String v = kv.substring(c+1).trim();
            if (k.startsWith("\"") && k.endsWith("\"")) k = k.substring(1, k.length()-1);
            if (v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length()-1);
            out.put(k, v);
        }
        return out;
    }

    // =================== EXPORTAR ICS ===================

    private void exportarICS() {
        if (fechaInicio == null || fechaFin == null) {
            alerta("Primero elige un rango de fechas.");
            return;
        }
        ProgramaCalc calc = calcularPrograma();         // respeta overrides
        if (calc.dias.isEmpty()) {
            alerta("No hay días de entrenamiento con horario para exportar.");
            return;
        }

        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Exportar calendario iCalendar (.ics)");
        fc.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("iCalendar (*.ics)", "*.ics")
        );
        java.io.File f = fc.showSaveDialog(yearLabel.getScene().getWindow());
        if (f == null) return;
        if (!f.getName().toLowerCase(Locale.ROOT).endsWith(".ics")) {
            f = new java.io.File(f.getAbsolutePath() + ".ics");
        }

        try {
            escribirICS(f.toPath(), calc);
            alerta("Exportado:\n" + f.getAbsolutePath());
        } catch (Exception ex) {
            alerta("Error al exportar:\n" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /** Genera un .ics con un VEVENT por cada día seleccionado que tenga horario. */
    private void escribirICS(java.nio.file.Path path, ProgramaCalc c) throws Exception {
        String tzid = ZoneId.systemDefault().getId(); // p.ej. "America/Mexico_City"
        String PRODID = "-//CalendarioEntrenamiento//1.0//ES";

        StringBuilder sb = new StringBuilder(16_384);
        // Encabezado VCALENDAR
        vline(sb, "BEGIN:VCALENDAR");
        vline(sb, "PRODID:" + PRODID);
        vline(sb, "VERSION:2.0");
        vline(sb, "CALSCALE:GREGORIAN");
        vline(sb, "METHOD:PUBLISH");

        // (Opcional) declaramos TZID como texto; los clientes modernos lo resuelven bien
        // Para máxima compatibilidad habría que incluir un bloque VTIMEZONE; lo omitimos por simplicidad.

        // Un VEVENT por sesión con minutos > 0
        for (DiaSel d : c.dias) {
            if (d.minutos <= 0) continue; // sólo eventos con horario

            LocalDate ld = d.fecha;
            String summary = "Entrenamiento (" + d.dow.getDisplayName(TextStyle.SHORT, new Locale("es","ES")) + ")";
            String uid = ld.toString().replace("-","") + "-" + UUID.randomUUID();

            // Parseo de horario
            LocalTime ini = null, fin = null;
            if (d.horario != null) {
                String[] p = d.horario.split(" - ");
                if (p.length == 2) {
                    ini = LocalTime.parse(p[0].trim());
                    fin = LocalTime.parse(p[1].trim());
                }
            }
            if (ini == null || fin == null) continue;

            // Manejar cruce de medianoche (fin < inicio => día siguiente)
            LocalDateTime startDT = LocalDateTime.of(ld, ini);
            LocalDateTime endDT   = (fin.isBefore(ini) ? LocalDateTime.of(ld.plusDays(1), fin)
                    : LocalDateTime.of(ld, fin));

            // iCal en hora "local con TZID" (no UTC), muy cómodo para calendarios personales
            String dtStart = fmtIcsLocal(startDT);
            String dtEnd   = fmtIcsLocal(endDT);

            vline(sb, "BEGIN:VEVENT");
            vline(sb, "UID:" + uid);
            vline(sb, "SUMMARY:" + esc(summary));
            vline(sb, "DTSTAMP:" + fmtIcsUtc(LocalDateTime.now(ZoneId.of("UTC")))); // sello en UTC
            vline(sb, "DTSTART;TZID=" + tzid + ":" + dtStart);
            vline(sb, "DTEND;TZID=" + tzid + ":" + dtEnd);

            // Opcionales
            // vline(sb, "LOCATION:" + esc("Cancha / Gimnasio"));
            // vline(sb, "DESCRIPTION:" + esc("Microciclo / Objetivo ..."));

            vline(sb, "END:VEVENT");
        }

        vline(sb, "END:VCALENDAR");

        // Escribir con CRLF como pide el estándar
        String ics = sb.toString().replace("\n", "\r\n");
        java.nio.file.Files.writeString(path, ics, java.nio.charset.StandardCharsets.UTF_8);

        // === Exportar eventos personalizados ===
        for (Map.Entry<LocalDate, List<Evento>> ent : eventosPorDia.entrySet()) {
            LocalDate fecha = ent.getKey();
            for (Evento ev : ent.getValue()) {
                String uid = fecha.toString().replace("-", "") + "-evt-" + UUID.randomUUID();
                LocalDateTime startDT = LocalDateTime.of(fecha, ev.inicio);
                // cruza medianoche si fin < inicio
                LocalDateTime endDT = ev.fin.isBefore(ev.inicio)
                        ? LocalDateTime.of(fecha.plusDays(1), ev.fin)
                        : LocalDateTime.of(fecha, ev.fin);

                vline(sb, "BEGIN:VEVENT");
                vline(sb, "UID:" + uid);
                vline(sb, "SUMMARY:" + esc(ev.titulo));
                if (ev.descripcion != null && !ev.descripcion.isBlank()) vline(sb, "DESCRIPTION:" + esc(ev.descripcion));
                if (ev.ubicacion != null && !ev.ubicacion.isBlank()) vline(sb, "LOCATION:" + esc(ev.ubicacion));
                vline(sb, "DTSTAMP:" + fmtIcsUtc(LocalDateTime.now(ZoneId.of("UTC"))));
                vline(sb, "DTSTART;TZID=" + tzid + ":" + fmtIcsLocal(startDT));
                vline(sb, "DTEND;TZID=" + tzid + ":" + fmtIcsLocal(endDT));

                if (ev.recordar) {
                    vline(sb, "BEGIN:VALARM");
                    vline(sb, "TRIGGER:-PT10M");
                    vline(sb, "ACTION:DISPLAY");
                    vline(sb, "DESCRIPTION:" + esc(ev.titulo));
                    vline(sb, "END:VALARM");
                }
                vline(sb, "END:VEVENT");
            }
        }

    }
    // Para poder marcar/desmarcar los checkboxes al cargar sin disparar diálogos
    private final EnumMap<DayOfWeek, CheckBox> checksPorDia = new EnumMap<>(DayOfWeek.class);
    private boolean cargandoDesdeArchivo = false;

    /** Línea iCal con folding básico (escapa comas y punto y coma). */
    private static String esc(String s) {
        return s.replace("\\","\\\\").replace(";","\\;").replace(",","\\,");
    }
    private static void vline(StringBuilder sb, String line) {
        sb.append(line).append('\n');
    }
    private static String fmtIcsLocal(LocalDateTime ldt) {
        return String.format("%04d%02d%02dT%02d%02d%02d",
                ldt.getYear(), ldt.getMonthValue(), ldt.getDayOfMonth(),
                ldt.getHour(), ldt.getMinute(), ldt.getSecond());
    }
    private static String fmtIcsUtc(LocalDateTime utc) {
        return String.format("%04d%02d%02dT%02d%02d%02dZ",
                utc.getYear(), utc.getMonthValue(), utc.getDayOfMonth(),
                utc.getHour(), utc.getMinute(), utc.getSecond());
    }

    /*private void actualizarResumen() {
        if (lblResumen == null) return;

        if (fechaInicio == null || fechaFin == null || diasEntrenamientoSeleccionados.isEmpty()) {
            lblResumen.setText("Selecciona rango, días y horarios para ver el resumen...");
            return;
        }

        // Normaliza el rango
        LocalDate start = fechaInicio.isBefore(fechaFin) ? fechaInicio : fechaFin;
        LocalDate end   = fechaFin.isAfter(fechaInicio) ? fechaFin : fechaInicio;

        // Semanas (contando ambas puntas dentro): ejemplo simple
        long diasTotalesRango = Duration.between(start.atStartOfDay(), end.plusDays(1).atStartOfDay()).toDays();
        long semanas = (diasTotalesRango + 6) / 7; // redondeo hacia arriba a semanas calendario

        int diasEntreno = 0;
        int minutosTotales = 0;

        // Recorre el rango y suma solo días de entrenamiento con horario asignado
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            DayOfWeek dow = d.getDayOfWeek();
            if (diasEntrenamientoSeleccionados.contains(dow) && horariosPorDia.containsKey(dow)) {
                String[] partes = horariosPorDia.get(dow).split(" - ");
                if (partes.length == 2) {
                    try {
                        LocalTime ini = LocalTime.parse(partes[0].trim());
                        LocalTime fin = LocalTime.parse(partes[1].trim());
                        int mins = minutosEntre(ini, fin); // maneja cruce de medianoche
                        if (mins > 0) {
                            diasEntreno++;
                            minutosTotales += mins;
                        }
                    } catch (Exception ignore) {
                        // Si el formato no es válido, se ignora ese día
                    }
                }
            }
        }

        // Total en horas (dos formatos: decimal y HH:MM)
        double horasTotalesDec = minutosTotales / 60.0;
        int hh = minutosTotales / 60;
        int mm = minutosTotales % 60;

        // Duración típica por sesión (promedio en minutos)
        String promedioSesion = diasEntreno > 0
                ? String.format("%d min", Math.round(minutosTotales * 1.0 / diasEntreno))
                : "0 min";

        lblResumen.setText(
                String.format("📅 Semanas: %d   |   🗓 Días de entrenamiento: %d   |   ⏱ Total: %d min (%.2f h = %02d:%02d)   |   Promedio por sesión: %s",
                        semanas, diasEntreno, minutosTotales, horasTotalesDec, hh, mm, promedioSesion)
        );
    }*/

  /*  private void actualizarResumen() {
        if (lblResumen == null) return;

        if (fechaInicio == null || fechaFin == null) {
            lblResumen.setText("Selecciona rango, días y horarios para ver el resumen...");
            return;
        }

        LocalDate start = minDate();
        LocalDate end   = maxDate();

        long diasTotalesRango = Duration.between(start.atStartOfDay(), end.plusDays(1).atStartOfDay()).toDays();
        long semanas = (diasTotalesRango + 6) / 7;

        int diasEntreno = 0;
        int minutosTotales = 0;

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (!seleccionadoEfectivo(d)) continue;

            // Si hay horario para el day-of-week, sumar minutos
            DayOfWeek dow = d.getDayOfWeek();
            String horario = horariosPorDia.get(dow);
            if (horario != null) {
                String[] partes = horario.split(" - ");
                if (partes.length == 2) {
                    try {
                        LocalTime ini = LocalTime.parse(partes[0].trim());
                        LocalTime fin = LocalTime.parse(partes[1].trim());
                        int mins = minutosEntre(ini, fin);
                        if (mins > 0) {
                            diasEntreno++;
                            minutosTotales += mins;
                        }
                    } catch (Exception ignore) {}
                }
            } else {
                // No hay horario para ese DOW: cuenta como seleccionado visualmente pero 0 minutos.
                diasEntreno++;
            }
        }

        double horasDec = minutosTotales / 60.0;
        int hh = minutosTotales / 60;
        int mm = minutosTotales % 60;
        String promedio = (diasEntreno > 0) ? (minutosTotales / diasEntreno) + " min" : "0 min";

        lblResumen.setText(String.format(
                "📅 Semanas: %d   |   🗓 Días seleccionados: %d   |   ⏱ Total: %d min (%.2f h = %02d:%02d)   |   Promedio sesión: %s",
                semanas, diasEntreno, minutosTotales, horasDec, hh, mm, promedio
        ));
    }*/
  /*private void actualizarResumen() {
      if (fechaInicio == null || fechaFin == null) {
          lblResumen.setText("Seleccione un rango de fechas.");
          return;
      }

      Map<YearMonth, Integer> minutosPorMes = new HashMap<>();
      Map<Integer, Integer> minutosPorSemana = new HashMap<>();

      int totalDias = 0;
      int totalMinutos = 0;

      LocalDate current = fechaInicio;
      while (!current.isAfter(fechaFin)) {
          if (diasEntrenamientoSeleccionados.contains(current.getDayOfWeek())) {
              String horario = horariosPorDia.get(current.getDayOfWeek());
              if (horario != null) {
                  int minutos = calcularDuracion(horario);
                  totalDias++;
                  totalMinutos += minutos;

                  // Por mes
                  YearMonth ym = YearMonth.from(current);
                  minutosPorMes.merge(ym, minutos, Integer::sum);

                  // Por semana
                  int semana = current.get(java.time.temporal.WeekFields.ISO.weekOfYear());
                  minutosPorSemana.merge(semana, minutos, Integer::sum);
              }
          }
          current = current.plusDays(1);
      }

      int totalHoras = totalMinutos / 60;
      int totalSemanas = (int) ChronoUnit.WEEKS.between(fechaInicio, fechaFin) + 1;

      StringBuilder sb = new StringBuilder();
      sb.append("📊 Resumen del programa:\n")
              .append("Semanas: ").append(totalSemanas).append("\n")
              .append("Días de entrenamiento: ").append(totalDias).append("\n")
              .append("Total horas: ").append(totalHoras).append("\n")
              .append("Total minutos: ").append(totalMinutos).append("\n\n");

      sb.append("⏱ Tiempo por mes:\n");
      for (var entry : minutosPorMes.entrySet()) {
          sb.append(entry.getKey()).append(": ")
                  .append(entry.getValue() / 60).append("h ")
                  .append(entry.getValue() % 60).append("min\n");
      }

      sb.append("\n⏱ Tiempo por semana:\n");
      for (var entry : minutosPorSemana.entrySet()) {
          sb.append("Semana ").append(entry.getKey()).append(": ")
                  .append(entry.getValue() / 60).append("h ")
                  .append(entry.getValue() % 60).append("min\n");
      }

      lblResumen.setText(sb.toString());
  }*/

    // =================== GUARDAR ===================

    private void guardarPrograma() {
        if (fechaInicio == null || fechaFin == null) {
            alerta("Primero elige un rango de fechas.");
            return;
        }

        // 1) Calcula la selección efectiva y los totales
        ProgramaCalc calc = calcularPrograma();

        // 2) FileChooser: JSON o CSV
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Guardar programa");
        fc.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Archivo JSON (*.json)", "*.json"),
                new javafx.stage.FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv")
        );

        java.io.File f = fc.showSaveDialog(yearLabel.getScene().getWindow());
        if (f == null) return;

        String name = f.getName().toLowerCase(Locale.ROOT);
        try {
            if (name.endsWith(".csv")) {
                escribirCSV(f.toPath(), calc);
            } else {
                if (!name.endsWith(".json")) {
                    f = new java.io.File(f.getAbsolutePath() + ".json");
                }
                escribirJSON(f.toPath(), calc);
            }
            alerta("Guardado con éxito:\n" + f.getAbsolutePath());
        } catch (Exception ex) {
            alerta("Error al guardar:\n" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void alerta(String mensaje) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, mensaje, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle("Información");
        a.showAndWait();
    }

    // Estructura para pasar todo lo calculado
    private static class DiaSel {
        LocalDate fecha;
        DayOfWeek dow;
        String horario; // puede ser null
        int minutos;    // 0 si no hay horario
        DiaSel(LocalDate f, DayOfWeek d, String h, int m) { fecha=f; dow=d; horario=h; minutos=m; }
    }

    private static class ProgramaCalc {
        LocalDate start, end;
        List<DiaSel> dias = new ArrayList<>();
        Map<YearMonth, Integer> minPorMes = new TreeMap<>();
        Map<Integer, Integer> minPorSemanaProg = new TreeMap<>();
        int diasSeleccionados;
        int minutosTotales;
        int semanasEfectivas;
        long semanasDelRango;
    }

    // Cálculo unificado (respeta overrides)
    private ProgramaCalc calcularPrograma() {
        ProgramaCalc res = new ProgramaCalc();
        res.start = fechaInicio.isBefore(fechaFin) ? fechaInicio : fechaFin;
        res.end   = fechaFin.isAfter(fechaInicio) ? fechaFin : fechaInicio;

        long diasRango = Duration.between(res.start.atStartOfDay(), res.end.plusDays(1).atStartOfDay()).toDays();
        res.semanasDelRango = (diasRango + 6) / 7;

        Set<Integer> semanasConSeleccion = new HashSet<>();
        for (LocalDate d = res.start; !d.isAfter(res.end); d = d.plusDays(1)) {
            if (!seleccionadoEfectivo(d)) continue;

            String hz = horariosPorDia.get(d.getDayOfWeek());
            int mins = 0;
            if (hz != null) {
                try {
                    String[] p = hz.split(" - ");
                    LocalTime ini = LocalTime.parse(p[0].trim());
                    LocalTime fin = LocalTime.parse(p[1].trim());
                    mins = Math.max(0, minutosEntre(ini, fin));
                } catch (Exception ignore) {}
            }
            res.dias.add(new DiaSel(d, d.getDayOfWeek(), hz, mins));
            res.diasSeleccionados++;
            res.minutosTotales += mins;

            YearMonth ym = YearMonth.from(d);
            res.minPorMes.merge(ym, mins, Integer::sum);

            int wk = semanaPrograma(res.start, d);
            semanasConSeleccion.add(wk);
            res.minPorSemanaProg.merge(wk, mins, Integer::sum);
        }
        res.semanasEfectivas = semanasConSeleccion.size();
        return res;
    }

// =================== ESCRITURA JSON ===================

    private void escribirJSON(java.nio.file.Path path, ProgramaCalc c) throws Exception {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("{\n");
        sb.append("  \"rango\": {\"inicio\": \"").append(c.start).append("\", \"fin\": \"").append(c.end).append("\"},\n");

        // días de entrenamiento y horarios por día de semana
        sb.append("  \"diasEntrenamiento\": [");
        boolean first = true;
        for (DayOfWeek d : diasEntrenamientoSeleccionados) {
            if (!first) sb.append(", ");
            sb.append("\"").append(d).append("\"");
            first = false;
        }
        sb.append("],\n");

        sb.append("  \"horariosPorDia\": {");
        first = true;
        for (Map.Entry<DayOfWeek,String> e : horariosPorDia.entrySet()) {
            if (!first) sb.append(", ");
            sb.append("\"").append(e.getKey()).append("\": ")
                    .append("\"").append(e.getValue()).append("\"");
            first = false;
        }
        sb.append("},\n");

        // overrides
        sb.append("  \"overrideOn\": [");
        first = true;
        for (LocalDate d : overrideOn) { if (!first) sb.append(", "); sb.append("\"").append(d).append("\""); first=false; }
        sb.append("],\n");
        sb.append("  \"overrideOff\": [");
        first = true;
        for (LocalDate d : overrideOff) { if (!first) sb.append(", "); sb.append("\"").append(d).append("\""); first=false; }
        sb.append("],\n");

        // totales
        sb.append("  \"totales\": {");
        sb.append("\"semanasDelRango\": ").append(c.semanasDelRango).append(", ");
        sb.append("\"semanasConEntrenamiento\": ").append(c.semanasEfectivas).append(", ");
        sb.append("\"diasSeleccionados\": ").append(c.diasSeleccionados).append(", ");
        sb.append("\"minutosTotales\": ").append(c.minutosTotales).append(", ");
        sb.append("\"horasTotalesDecimal\": ").append(String.format(Locale.US, "%.2f", c.minutosTotales/60.0));
        sb.append("},\n");

        // por mes
        sb.append("  \"minutosPorMes\": {");
        first = true;
        for (Map.Entry<YearMonth,Integer> e : c.minPorMes.entrySet()) {
            if (!first) sb.append(", ");
            sb.append("\"").append(e.getKey()).append("\": ").append(e.getValue());
            first = false;
        }
        sb.append("},\n");

        // por semana del programa
        sb.append("  \"minutosPorSemanaPrograma\": {");
        first = true;
        for (Map.Entry<Integer,Integer> e : c.minPorSemanaProg.entrySet()) {
            if (!first) sb.append(", ");
            sb.append("\"").append(e.getKey()).append("\": ").append(e.getValue());
            first = false;
        }
        sb.append("},\n");

        // fechas seleccionadas
        sb.append("  \"fechas\": [\n");
        for (int i = 0; i < c.dias.size(); i++) {
            DiaSel d = c.dias.get(i);
            sb.append("    {\"fecha\": \"").append(d.fecha).append("\", ")
                    .append("\"dow\": \"").append(d.dow).append("\", ")
                    .append("\"horario\": ").append(d.horario==null? "null" : "\""+d.horario+"\"").append(", ")
                    .append("\"minutos\": ").append(d.minutos).append("}");
            if (i < c.dias.size()-1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");

        // ... después de "fechas": [ ... ],
        sb.append(",\n  \"eventos\": [\n");
        boolean firstEv = true;
        for (Map.Entry<LocalDate, List<Evento>> ent : eventosPorDia.entrySet()) {
            for (Evento ev : ent.getValue()) {
                if (!firstEv) sb.append(",\n");
                firstEv = false;
                sb.append("    {")
                        .append("\"fecha\":\"").append(ent.getKey()).append("\",")
                        .append("\"titulo\":\"").append(esc(ev.titulo)).append("\",")
                        .append("\"descripcion\":\"").append(esc(ev.descripcion == null ? "" : ev.descripcion)).append("\",")
                        .append("\"ubicacion\":\"").append(esc(ev.ubicacion == null ? "" : ev.ubicacion)).append("\",")
                        .append("\"inicio\":\"").append(ev.inicio).append("\",")
                        .append("\"fin\":\"").append(ev.fin).append("\",")
                        .append("\"recordar\":").append(ev.recordar)
                        .append("}");
            }
        }
        sb.append("\n  ]\n");


        java.nio.file.Files.writeString(path, sb.toString(), java.nio.charset.StandardCharsets.UTF_8);
    }

// =================== ESCRITURA CSV ===================

    private void escribirCSV(java.nio.file.Path path, ProgramaCalc c) throws Exception {
        StringBuilder sb = new StringBuilder(4096);
        // encabezado
        sb.append("fecha,dow,horario,minutos\n");
        for (DiaSel d : c.dias) {
            sb.append(d.fecha).append(",")
                    .append(d.dow).append(",")
                    .append(d.horario == null ? "" : d.horario).append(",")
                    .append(d.minutos).append("\n");
        }
        // una sección final con totales
        sb.append("\n# resumen\n");
        sb.append("semanas_del_rango,").append(c.semanasDelRango).append("\n");
        sb.append("semanas_con_entrenamiento,").append(c.semanasEfectivas).append("\n");
        sb.append("dias_seleccionados,").append(c.diasSeleccionados).append("\n");
        sb.append("minutos_totales,").append(c.minutosTotales).append("\n");
        sb.append("horas_totales_decimal,").append(String.format(Locale.US, "%.2f", c.minutosTotales/60.0)).append("\n");

        java.nio.file.Files.writeString(path, sb.toString(), java.nio.charset.StandardCharsets.UTF_8);
    }


    private void actualizarResumen() {
        if (resumenBox == null) return;

        resumenBox.getChildren().clear();

        if (fechaInicio == null || fechaFin == null) {
            resumenBox.getChildren().add(new Label("Selecciona rango, días y horarios para ver el resumen…"));
            return;
        }

        // Normalizar rango
        LocalDate start = fechaInicio.isBefore(fechaFin) ? fechaInicio : fechaFin;
        LocalDate end   = fechaFin.isAfter(fechaInicio) ? fechaFin : fechaInicio;

        // Acumuladores
        Map<YearMonth, Integer> minPorMes = new TreeMap<>();
        Map<Integer, Integer>   minPorSemanaProg = new TreeMap<>(); // Semana 1, 2, … (desde el start)
        int diasSeleccionados = 0;
        int minutosTotales = 0;

        Set<Integer> semanasConSeleccion = new HashSet<>();   // << NUEVO


        // Recorremos el rango usando SIEMPRE la selección efectiva (rango ± overrides)
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (!seleccionadoEfectivo(d)) continue;    // << CLAVE: respeta Ctrl+Click

            // minutos de la sesión (si hay horario para ese DOW; si no, cuenta día pero 0 min)
            int mins = 0;
            String hz = horariosPorDia.get(d.getDayOfWeek());
            if (hz != null) {
                try {
                    String[] p = hz.split(" - ");
                    LocalTime ini = LocalTime.parse(p[0].trim());
                    LocalTime fin = LocalTime.parse(p[1].trim());
                    mins = Math.max(0, minutosEntre(ini, fin));
                } catch (Exception ignore) {}
            }

            diasSeleccionados++;
            minutosTotales += mins;

            // Por mes
            YearMonth ym = YearMonth.from(d);
            minPorMes.merge(ym, mins, Integer::sum);

            // Por semana del programa
            int wk = semanaPrograma(start, d);
            semanasConSeleccion.add(wk);               // << NUEVO: marca que esta semana tiene algo
            minPorSemanaProg.merge(wk, mins, Integer::sum);
        }



       /* // Recorremos el rango con la lógica de “selección efectiva” (si la usas)
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            // Si usas overrides/selección efectiva:
            // if (!seleccionadoEfectivo(d)) continue;
            // Si NO usas overrides, filtra por días de entrenamiento en rango:
            boolean dentro = true; // ya estamos iterando dentro
            boolean entreno = diasEntrenamientoSeleccionados.isEmpty() ||
                    diasEntrenamientoSeleccionados.contains(d.getDayOfWeek());
            if (!dentro || !entreno) continue;

            // minutos de la sesión (si hay horario para ese DOW; si no, cuenta día pero 0 min)
            int mins = 0;
            String hz = horariosPorDia.get(d.getDayOfWeek());
            if (hz != null) {
                try {
                    String[] p = hz.split(" - ");
                    LocalTime ini = LocalTime.parse(p[0].trim());
                    LocalTime fin = LocalTime.parse(p[1].trim());
                    mins = Math.max(0, minutosEntre(ini, fin));
                } catch (Exception ignore) {}
            }

            diasSeleccionados++;
            minutosTotales += mins;

            // Por mes
            YearMonth ym = YearMonth.from(d);
            minPorMes.merge(ym, mins, Integer::sum);

            // Por semana del programa
            int wk = semanaPrograma(start, d);
            minPorSemanaProg.merge(wk, mins, Integer::sum);
        }*/

        long diasRango = Duration.between(start.atStartOfDay(), end.plusDays(1).atStartOfDay()).toDays();
        long semanasRango = (diasRango + 6) / 7; // semanas calendario del programa

        // ===== Bloque 1: Totales =====
        Label tituloTot = new Label("📊 Resumen del programa");
        tituloTot.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        GridPane gTot = new GridPane();
        gTot.setHgap(18); gTot.setVgap(6);

        int semanasEfectivas = semanasConSeleccion.size();     // << NUEVO


        int fila = 0;
        // Antes mostrabas semanas del rango. Ahora muestra las efectivas:
        gTot.add(new Label("Semanas con entrenamiento:"), 0, fila);
        gTot.add(new Label(String.valueOf(semanasEfectivas)), 1, fila++);
        gTot.add(new Label("Semanas del programa:"), 0, fila); gTot.add(new Label(String.valueOf(semanasRango)), 1, fila++);
        gTot.add(new Label("Días seleccionados:"),  0, fila); gTot.add(new Label(String.valueOf(diasSeleccionados)), 1, fila++);
        gTot.add(new Label("Total (min):"),         0, fila); gTot.add(new Label(String.valueOf(minutosTotales)), 1, fila++);
        gTot.add(new Label("Total (h):"),           0, fila); gTot.add(new Label(fmtHHMM(minutosTotales) + "  (" + String.format("%.2f h", minutosTotales/60.0) + ")"), 1, fila++);

        // ===== Bloque 2: Por mes =====
        Label tituloMes = new Label("⏱ Tiempo por mes");
        tituloMes.setFont(Font.font("Arial", FontWeight.BOLD, 13));

        GridPane gMes = new GridPane();
        gMes.setHgap(18); gMes.setVgap(4);

        int r = 0;
        for (Map.Entry<YearMonth, Integer> e : minPorMes.entrySet()) {
            gMes.add(new Label(this.monthsES.get(e.getKey().getMonthValue()-1).toString()), 0, r);
            gMes.add(new Label(fmtHHMM(e.getValue()) + "  (" + fmtHM(e.getValue()) + ")"), 1, r);
            r++;
        }
        if (r == 0) gMes.add(new Label("Sin minutos asignados."), 0, 0);

        // ===== Bloque 3: Por semana del programa =====
        Label tituloSem = new Label("⏱ Tiempo por semana del programa");
        tituloSem.setFont(Font.font("Arial", FontWeight.BOLD, 13));

        GridPane gSem = new GridPane();
        gSem.setHgap(18); gSem.setVgap(4);

        int r2 = 0;
        for (Map.Entry<Integer, Integer> e : minPorSemanaProg.entrySet()) {
            gSem.add(new Label("Semana " + e.getKey()), 0, r2);
            gSem.add(new Label(fmtHHMM(e.getValue()) + "  (" + fmtHM(e.getValue()) + ")"), 1, r2);
            r2++;
        }
        if (r2 == 0) gSem.add(new Label("Sin minutos asignados."), 0, 0);

        // Montar el panel
        resumenBox.getChildren().addAll(tituloTot, gTot, new Separator(),
                tituloMes, gMes, new Separator(),
                tituloSem, gSem);
    }
    private int semanaPrograma(LocalDate start, LocalDate d) {
        long dias = Duration.between(start.atStartOfDay(), d.atStartOfDay()).toDays();
        return (int)(dias / 7) + 1;
    }
    private String fmtHM(int minutos) {                 // 130 -> "2 h 10 m"
        int h = minutos / 60, m = minutos % 60;
        if (h == 0) return m + " m";
        if (m == 0) return h + " h";
        return h + " h " + m + " m";
    }
    private String fmtHHMM(int minutos) {               // 130 -> "02:10"
        int h = minutos / 60, m = minutos % 60;
        //return String.format("%02d:%02d", h, m);
        return String.format("%02d", h);
    }
   /* private int minutosEntre(LocalTime ini, LocalTime fin) {
        if (fin.isBefore(ini)) fin = fin.plusHours(24);
        return (int) Duration.between(ini, fin).toMinutes();
    }
*/

    private int calcularDuracion(String horario) {
        String[] partes = horario.split(" - ");
        String[] inicio = partes[0].split(":");
        String[] fin = partes[1].split(":");

        int hIni = Integer.parseInt(inicio[0]);
        int mIni = Integer.parseInt(inicio[1]);
        int hFin = Integer.parseInt(fin[0]);
        int mFin = Integer.parseInt(fin[1]);

        return (hFin * 60 + mFin) - (hIni * 60 + mIni);
    }

/*

    private int minutosEntre(LocalTime inicio, LocalTime fin) {
        if (fin.isBefore(inicio)) fin = fin.plusHours(24);
        return (int) Duration.between(inicio, fin).toMinutes();
    }
*/


    /** Diferencia en minutos; si fin < inicio, asume que termina al día siguiente. */
    private int minutosEntre(LocalTime inicio, LocalTime fin) {
        if (fin.isBefore(inicio)) fin = fin.plusHours(24);
        return (int) Duration.between(inicio, fin).toMinutes();
    }

   /* private void actualizarResumen() {
        if (fechaInicio == null || fechaFin == null || diasEntrenamientoSeleccionados.isEmpty()) {
            lblResumen.setText("Selecciona rango de fechas y días de entrenamiento.");
            return;
        }

        LocalDate start = fechaInicio.isBefore(fechaFin) ? fechaInicio : fechaFin;
        LocalDate end   = fechaFin.isAfter(fechaInicio) ? fechaFin : fechaInicio;

        long semanas = java.time.temporal.ChronoUnit.WEEKS.between(start, end) + 1;

        int totalDias = 0;
        int totalMinutos = 0;
        LocalDate d = start;
        while (!d.isAfter(end)) {
            if (diasEntrenamientoSeleccionados.contains(d.getDayOfWeek()) &&
                    horariosPorDia.containsKey(d.getDayOfWeek())) {

                totalDias++;
                // calcular minutos de la sesión
                String[] partes = horariosPorDia.get(d.getDayOfWeek()).split(" - ");
                if (partes.length == 2) {
                    LocalTime inicio = LocalTime.parse(partes[0]);
                    LocalTime fin    = LocalTime.parse(partes[1]);
                    int duracion = (int) Duration.between(inicio, fin).toMinutes();
                    totalMinutos += duracion;
                }
            }
            d = d.plusDays(1);
        }

        lblResumen.setText(
                "📅 Semanas: " + semanas +
                        "   |   🗓 Días de entrenamiento: " + totalDias +
                        "   |   ⏱ Total minutos: " + totalMinutos
        );
    }*/

    private void reconstruirMeses() {
        monthsGrid.getChildren().clear();
        int year = selectedDate.getYear();
        for (int m = 1; m <= 12; m++) {
            YearMonth ym = YearMonth.of(year, m);
            VBox monthPane = crearMiniCalendario(ym);
            monthPane.setPrefWidth(250);
            monthsGrid.getChildren().add(monthPane);
        }
        actualizarResumen(); // 👈 cada vez que se repinta, recalculamos

    }

    private VBox crearMiniCalendario(YearMonth ym) {
        VBox box = new VBox(6);
        box.setPadding(new Insets(8));
        box.setStyle("-fx-background-color:white; -fx-border-color:#DDD; -fx-border-radius:8; -fx-background-radius:8;");
        box.setPrefWidth(260);

        Label lblMes = new Label(monthsES.get(ym.getMonthValue() - 1) + " " + ym.getYear());
        lblMes.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);

        String[] weekDays = {"D","L","M","M","J","V","S"};
        for (int i = 0; i < weekDays.length; i++) {
            Label d = new Label(weekDays[i]);
            d.setMinWidth(32);
            d.setAlignment(Pos.CENTER);
            d.setStyle("-fx-font-weight:bold;");
            grid.add(d, i, 0);
        }

        int firstDayColumn = mapSundayZero(ym.atDay(1).getDayOfWeek());
        int daysInMonth = ym.lengthOfMonth();

        int col = firstDayColumn;
        int row = 1;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate thisDate = ym.atDay(day);

            Button b = new Button(String.valueOf(day));
            b.setMinSize(32, 32);
            b.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

            aplicarEstilo(b, thisDate);

            // badge de eventos
            Label badge = null;
            List<Evento> evs = eventosPorDia.get(thisDate);
            if (evs != null && !evs.isEmpty()) {
                badge = new Label(String.valueOf(evs.size())); // o "•"
                badge.setStyle("-fx-background-color:#FF7043; -fx-text-fill:white; -fx-font-size:10; -fx-padding:1 4 1 4; -fx-background-radius:10;");
                StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            }

// tooltip con títulos
            if (evs != null && !evs.isEmpty()) {
                StringBuilder tt = new StringBuilder("Eventos:\n");
                for (Evento ev : evs) {
                    tt.append("• ").append(ev.titulo).append(" (")
                            .append(String.format("%02d:%02d", ev.inicio.getHour(), ev.inicio.getMinute()))
                            .append(")\n");
                }
                b.setTooltip(new Tooltip(tt.toString()));
            }

// wrap del botón + badge
            StackPane sp = new StackPane(b);
            if (badge != null) sp.getChildren().add(badge);

            /*// Clic simple = define rango
            b.setOnAction(e -> {
                if (fechaInicio == null || fechaFin != null) {
                    fechaInicio = thisDate;
                    fechaFin = null;
                } else {
                    if (thisDate.isBefore(fechaInicio)) {
                        fechaFin = fechaInicio;
                        fechaInicio = thisDate;
                    } else {
                        fechaFin = thisDate;
                    }
                }
                reconstruirMeses();
            });

            b.setOnMouseClicked(e -> {
                if (e.isControlDown()) {
                    // Ctrl + Click -> selección/deselección individual
                    if (fechasMultiples.contains(thisDate)) {
                        fechasMultiples.remove(thisDate);
                    } else {
                        fechasMultiples.add(thisDate);
                    }
                    aplicarEstilo(b, thisDate);
                } else if (e.getClickCount() == 2) {
                    // Doble clic normal -> alternar
                    if (fechasMultiples.contains(thisDate)) {
                        fechasMultiples.remove(thisDate);
                    } else {
                        fechasMultiples.add(thisDate);
                    }
                    aplicarEstilo(b, thisDate);
                }
            });
*/
// Click simple = define rango
           /* b.setOnAction(e -> {
                if (fechaInicio == null || fechaFin != null) {
                    // Comienzo de un rango NUEVO: (opcional) limpiar overrides para empezar limpio
                    overrideOn.clear();
                    overrideOff.clear();

                    fechaInicio = thisDate;
                    fechaFin = null;
                } else {
                    if (thisDate.isBefore(fechaInicio)) {
                        fechaFin = fechaInicio;
                        fechaInicio = thisDate;
                    } else {
                        fechaFin = thisDate;
                    }
                }
                reconstruirMeses();
            });

// Ctrl + Click = alternar selección efectiva del día (aunque haya rango activo)
            b.setOnMouseClicked(e -> {
                if (e.isControlDown()) {
                    toggleCtrl(thisDate);
                    // repintar SOLO este botón (si prefieres performance)
                    aplicarEstilo(b, thisDate);
                    // o repintar todo y refrescar resumen:
                    // reconstruirMeses();
                    actualizarResumen();
                }
            });*/

          /*  // CLICK normal (sin modificadores): si hay fechaInicio y no hay fin -> cierra rango, si no -> inicia rango nuevo
            b.setOnAction(e -> {
                if (e.isControlDown() || e.isShiftDown()) return; // lo manejamos en mouseClicked
                if (fechaInicio == null || fechaFin != null) {
                    // inicia rango nuevo
                    overrideOn.clear();
                    overrideOff.clear();
                    fechaInicio = thisDate;
                    fechaFin = null;
                } else {
                    // completa el rango
                    if (thisDate.isBefore(fechaInicio)) {
                        fechaFin = fechaInicio;
                        fechaInicio = thisDate;
                    } else {
                        fechaFin = thisDate;
                    }
                }
                dragging = false; previewStart = previewEnd = null;
                reconstruirMeses();
            });*/

            b.setOnAction(e -> {
                // 1) Si está dentro del rango: NO hacer nada (se maneja con menú contextual)
                if (isInsideRange(thisDate)) return;

                // 2) Fuera del rango: clic = seleccionar un único “extra”
                //    - si había uno previo y no estaba fijado, se quita
                if (ultimoClickExtra != null
                        && !ultimoClickExtra.equals(thisDate)
                        && !diasAnclados.contains(ultimoClickExtra)) {
                    overrideOn.remove(ultimoClickExtra);
                }

                // 3) Si el actual está fijado, NO lo toques con clic normal
                if (diasAnclados.contains(thisDate)) {
                    // no cambia nada
                } else {
                    // alterna este día como "extra"
                    if (overrideOn.contains(thisDate)) {
                        overrideOn.remove(thisDate);
                        if (ultimoClickExtra != null && ultimoClickExtra.equals(thisDate)) {
                            ultimoClickExtra = null;
                        }
                    } else {
                        overrideOn.add(thisDate);
                        ultimoClickExtra = thisDate;
                    }
                }

                reconstruirMeses();
            });

            /*b.setOnAction(e -> {
                if (!hayRango()) {
                    // Construcción del rango por primera vez
                    if (fechaInicio == null) {
                        fechaInicio = thisDate;
                        fechaFin = null;
                    } else {
                        if (thisDate.isBefore(fechaInicio)) {
                            fechaFin = fechaInicio;
                            fechaInicio = thisDate;
                        } else {
                            fechaFin = thisDate;
                        }
                    }
                } else {
                    // ¡Ya hay rango! No lo borres: solo alterna este día como “anclado”
                    if (diasAnclados.contains(thisDate)) {
                        diasAnclados.remove(thisDate); // lo quitas del marcado extra
                    } else {
                        diasAnclados.add(thisDate);    // lo fuerzas a quedar marcado
                    }
                }
                reconstruirMeses();
            });*/
         /*  b.setOnAction(e -> {
                // Clic normal (o ENTER/SPACE desde teclado) -> inicia/cierra rango
                if (fechaInicio == null || fechaFin != null) {
                    overrideOn.clear();
                    overrideOff.clear();
                    fechaInicio = thisDate;
                    fechaFin = null;
                } else {
                    if (thisDate.isBefore(fechaInicio)) {
                        fechaFin = fechaInicio;
                        fechaInicio = thisDate;
                    } else {
                        fechaFin = thisDate;
                    }
                }
                dragging = false; previewStart = previewEnd = null;
                reconstruirMeses();
            });*/

            // CLICK normal: solo alterna este día (no toca fechaInicio/fechaFin)
          /*  b.setOnAction(e -> {
                if (seleccionadoEfectivo(thisDate)) {
                    overrideOn.remove(thisDate);
                    overrideOff.add(thisDate);
                } else {
                    overrideOff.remove(thisDate);
                    overrideOn.add(thisDate);
                }
                aplicarEstilo(b, thisDate);
                actualizarResumen();
            });*/

         /*   b.setOnAction(e -> {
                // Clic normal: selección de UN solo día
                // — Limpia overrides y rango anterior (pero respeta los anclados)
                overrideOn.clear();
                overrideOff.clear();
                fechaInicio = thisDate;
                fechaFin    = thisDate;

                // Si no quieres usar rango para 1 día, también puedes dejar fechaFin = fechaInicio
                // y tu lógica de estilo ya lo pintará como inicio/fin iguales

                reconstruirMeses();
            });*/


// CLICK con modificadores (Shift/Ctrl) y ContextMenu
          /*  b.setOnMouseClicked(e -> {
                if (e.isControlDown() && e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                    // Ctrl + Click: toggle override
                    toggleCtrl(thisDate);
                    aplicarEstilo(b, thisDate);
                    actualizarResumen();
                    return;
                }
                if (e.isShiftDown() && e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                    // Shift + Click: extender/ajustar rango desde fechaInicio
                    if (fechaInicio != null) {
                        if (fechaFin == null) {
                            fechaFin = (thisDate.isBefore(fechaInicio)) ? fechaInicio : thisDate;
                            if (thisDate.isBefore(fechaInicio)) fechaInicio = thisDate;
                        } else {
                            // si ya hay fin, reajusta tomando fechaInicio fijo y este como nuevo fin
                            if (thisDate.isBefore(fechaInicio)) {
                                fechaFin = fechaInicio;
                                fechaInicio = thisDate;
                            } else {
                                fechaFin = thisDate;
                            }
                        }
                        dragging = false; previewStart = previewEnd = null;
                        reconstruirMeses();
                    }
                    return;
                }
                if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                    // Context menu
                    ContextMenu cm = crearContextMenuDia(thisDate, b);
                    cm.show(b, e.getScreenX(), e.getScreenY());
                }
            });*/
            b.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.isControlDown()) {
                    toggleDia(thisDate);
                    aplicarEstilo(b, thisDate);
                    actualizarResumen();
                    e.consume();
                    return;
                }
                if (e.getButton() == MouseButton.PRIMARY && e.isShiftDown()) {
                    // Ajustar/extender rango sin borrarlo
                    if (fechaInicio != null) {
                        if (fechaFin == null) {
                            if (thisDate.isBefore(fechaInicio)) {
                                fechaFin = fechaInicio; fechaInicio = thisDate;
                            } else {
                                fechaFin = thisDate;
                            }
                        } else {
                            if (thisDate.isBefore(fechaInicio)) {
                                fechaFin = fechaInicio; fechaInicio = thisDate;
                            } else {
                                fechaFin = thisDate;
                            }
                        }
                        reconstruirMeses();
                    }
                    e.consume();
                    return;
                }
                if (e.getButton() == MouseButton.SECONDARY) {
                    ContextMenu cm = crearContextMenuDia(thisDate, b);
                    cm.show(b, e.getScreenX(), e.getScreenY());
                    e.consume();
                }
            });

            b.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.isControlDown()) {
                    toggleCtrl(thisDate);       // alterna usando overrideOn/overrideOff
                    aplicarEstilo(b, thisDate); // repinta ese botón (o llama reconstruirMeses() si prefieres)
                    actualizarResumen();
                    e.consume();
                    return;
                }
                if (e.getButton() == MouseButton.PRIMARY && e.isShiftDown()) {
                    // extender/ajustar rango (tu lógica actual)
                    if (fechaInicio != null) {
                        if (fechaFin == null) {
                            if (thisDate.isBefore(fechaInicio)) {
                                fechaFin = fechaInicio; fechaInicio = thisDate;
                            } else {
                                fechaFin = thisDate;
                            }
                        } else {
                            if (thisDate.isBefore(fechaInicio)) {
                                fechaFin = fechaInicio; fechaInicio = thisDate;
                            } else {
                                fechaFin = thisDate;
                            }
                        }
                        reconstruirMeses();
                    }
                    e.consume();
                    return;
                }
                if (e.getButton() == MouseButton.SECONDARY) {
                    ContextMenu cm = crearContextMenuDia(thisDate, b);
                    cm.show(b, e.getScreenX(), e.getScreenY());
                    e.consume();
                }
            });

 /*           b.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
                // Ctrl + Click: override ON/OFF
                if (e.getButton() == MouseButton.PRIMARY && e.isControlDown()) {
                    if (diasAnclados.contains(thisDate)) {
                        diasAnclados.remove(thisDate);
                    } else {
                        diasAnclados.add(thisDate);
                    }
                    // No cambies la selección única; solo repinta
                    aplicarEstilo(b, thisDate);
                    actualizarResumen();
                }
                // Shift + Click: extender/ajustar rango desde fechaInicio
                if (e.getButton() == MouseButton.PRIMARY && e.isShiftDown()) {
                    if (fechaInicio != null) {
                        if (fechaFin == null) {
                            if (thisDate.isBefore(fechaInicio)) {
                                fechaFin = fechaInicio;
                                fechaInicio = thisDate;
                            } else {
                                fechaFin = thisDate;
                            }
                        } else {
                            if (thisDate.isBefore(fechaInicio)) {
                                fechaFin = fechaInicio;
                                fechaInicio = thisDate;
                            } else {
                                fechaFin = thisDate;
                            }
                        }
                        dragging = false; previewStart = previewEnd = null;
                        reconstruirMeses();
                    }
                    return;
                }
                // Clic derecho: menú contextual
                if (e.getButton() == MouseButton.SECONDARY) {
                    ContextMenu cm = crearContextMenuDia(thisDate, b);
                    cm.show(b, e.getScreenX(), e.getScreenY());
                }
            });*/

// DRAG: preview de rango
            b.setOnDragDetected(e -> {
                dragging = true;
                previewStart = thisDate;
                previewEnd = thisDate;
                b.startFullDrag();
                aplicarEstilo(b, thisDate);
            });

            b.setOnMouseDragEntered(e -> {
                if (!dragging) return;
                previewEnd = thisDate;
                // para mejor rendimiento, solo re-estilamos este botón:
                aplicarEstilo(b, thisDate);
            });

            b.setOnMouseReleased(e -> {
                if (!dragging) return;
                // al soltar, fijamos el rango
                LocalDate a = pMin(), z = pMax();
                if (fechaInicio == null || fechaFin != null) {
                    fechaInicio = a; fechaFin = z;
                } else {
                    // había inicio sin fin -> cierra rango
                    if (a.isBefore(fechaInicio)) { fechaFin = fechaInicio; fechaInicio = a; }
                    else                         { fechaFin = z; }
                }
                dragging = false; previewStart = previewEnd = null;
                reconstruirMeses();
            });


            // Tooltip con horario si aplica
            if (diasEntrenamientoSeleccionados.contains(thisDate.getDayOfWeek())
                    && horariosPorDia.containsKey(thisDate.getDayOfWeek())) {
                b.setTooltip(new Tooltip("Horario: " + horariosPorDia.get(thisDate.getDayOfWeek())));
            }

            //grid.add(b, col, row);
            grid.add(sp, col, row);
            col++;
            if (col == 7) { col = 0; row++; }
        }

        box.getChildren().addAll(lblMes, grid);
        return box;
    }

    /*private void aplicarEstilo(Button b, LocalDate thisDate) {
        boolean hayRango = (fechaInicio != null && fechaFin != null);
        LocalDate min = null, max = null;
        if (hayRango) {
            min = fechaInicio.isBefore(fechaFin) ? fechaInicio : fechaFin;
            max = fechaFin.isAfter(fechaInicio) ? fechaFin : fechaInicio;
        }

        if (thisDate.equals(fechaInicio)) { b.setStyle(STYLE_START); return; }
        if (thisDate.equals(fechaFin))     { b.setStyle(STYLE_END);   return; }

        boolean filtroActivo = hayRango && !diasEntrenamientoSeleccionados.isEmpty();
        if (filtroActivo) {
            boolean dentro = !thisDate.isBefore(min) && !thisDate.isAfter(max);
            if (dentro && diasEntrenamientoSeleccionados.contains(thisDate.getDayOfWeek())) {
                b.setStyle(STYLE_SELECTED);
            } else {
                b.setStyle(STYLE_NORMAL);
            }
            return;
        }

        if (hayRango && thisDate.isAfter(min) && thisDate.isBefore(max)) {
            b.setStyle(STYLE_SELECTED);
        } else if (fechasMultiples.contains(thisDate)) {
            b.setStyle(STYLE_SELECTED);
        } else {
            b.setStyle(STYLE_NORMAL);
        }
    }*/
/*
    private void aplicarEstilo(Button b, LocalDate d) {
        // inicio / fin tienen prioridad visual
        if (d.equals(fechaInicio)) { b.setStyle(STYLE_START); return; }
        if (d.equals(fechaFin))    { b.setStyle(STYLE_END);   return; }

        // resto según selección efectiva
        if (seleccionadoEfectivo(d)) {
            b.setStyle(STYLE_SELECTED);
        } else {
            b.setStyle(STYLE_NORMAL);
        }
    }
*/
/*    private ContextMenu crearContextMenuDia(LocalDate d, Button bRef) {
        MenuItem on  = new MenuItem("Seleccionar este día");
        MenuItem off = new MenuItem("Deseleccionar este día");
        MenuItem horario = new MenuItem("Asignar horario…");
        MenuItem limpiarSemana = new MenuItem("Limpiar overrides de esta semana");

        on.setOnAction(e -> { overrideOff.remove(d); overrideOn.add(d); aplicarEstilo(bRef, d); actualizarResumen(); });
        off.setOnAction(e -> { overrideOn.remove(d); overrideOff.add(d); aplicarEstilo(bRef, d); actualizarResumen(); });
        horario.setOnAction(e -> { mostrarSelectorHorario(d.getDayOfWeek()); reconstruirMeses(); });
        limpiarSemana.setOnAction(e -> {
            LocalDate monday = d.with(java.time.DayOfWeek.MONDAY);
            for (int i = 0; i < 7; i++) {
                LocalDate x = monday.plusDays(i);
                overrideOn.remove(x);
                overrideOff.remove(x);
            }
            reconstruirMeses();
        });

        ContextMenu cm = new ContextMenu(on, off, new SeparatorMenuItem(), horario, new SeparatorMenuItem(), limpiarSemana);
        return cm;
    }*/
    /*private ContextMenu crearContextMenuDia(LocalDate d, Button bRef) {
        MenuItem on  = new MenuItem("Seleccionar este día");
        MenuItem off = new MenuItem("Deseleccionar este día");
        MenuItem beginRange = new MenuItem("Comenzar rango aquí");
        MenuItem endRange   = new MenuItem("Terminar rango aquí");
        MenuItem horario = new MenuItem("Asignar horario…");
        MenuItem limpiarSemana = new MenuItem("Limpiar overrides de esta semana");

        on.setOnAction(e -> { overrideOff.remove(d); overrideOn.add(d); aplicarEstilo(bRef, d); actualizarResumen(); });
        off.setOnAction(e -> { overrideOn.remove(d); overrideOff.add(d); aplicarEstilo(bRef, d); actualizarResumen(); });

        // NUEVO: rango por menú contextual
        beginRange.setOnAction(e -> {
            fechaInicio = d;
            fechaFin = null;
            // opcional: limpiar overrides del rango anterior si quieres empezar “limpio”:
            // overrideOn.clear(); overrideOff.clear();
            reconstruirMeses();
        });
        endRange.setOnAction(e -> {
            if (fechaInicio == null) {
                // si no había inicio, haz que este día sea el inicio
                fechaInicio = d; fechaFin = null;
            } else {
                if (d.isBefore(fechaInicio)) { fechaFin = fechaInicio; fechaInicio = d; }
                else                         { fechaFin = d; }
            }
            reconstruirMeses();
        });

        horario.setOnAction(e -> { mostrarSelectorHorario(d.getDayOfWeek()); reconstruirMeses(); });

        limpiarSemana.setOnAction(e -> {
            LocalDate monday = d.with(java.time.DayOfWeek.MONDAY);
            for (int i = 0; i < 7; i++) {
                LocalDate x = monday.plusDays(i);
                overrideOn.remove(x);
                overrideOff.remove(x);
            }
            reconstruirMeses();
        });

        return new ContextMenu(
                on, off,
                new SeparatorMenuItem(),
                beginRange, endRange,
                new SeparatorMenuItem(),
                horario,
                new SeparatorMenuItem(),
                limpiarSemana
        );
    }
*/
    // junto a diasAnclados
    private final Set<LocalDate> diasExcluidos = new HashSet<>();

/*
    private ContextMenu crearContextMenuDia(LocalDate d, Button bRef) {
        ContextMenu cm = new ContextMenu();

        MenuItem on  = new MenuItem("Seleccionar este día");
        MenuItem off = new MenuItem("Deseleccionar este día");
        MenuItem beginRange = new MenuItem("Comenzar rango aquí");
        MenuItem endRange   = new MenuItem("Terminar rango aquí");
        MenuItem horario    = new MenuItem("Asignar horario…");
        MenuItem limpiarSemana = new MenuItem("Limpiar selección de esta semana");

        boolean yaSel = seleccionadoEfectivo(d);
        on.setDisable(yaSel);      // si ya está seleccionado, no tiene sentido “Seleccionar”
        off.setDisable(!yaSel);    // si no está seleccionado, no tiene sentido “Deseleccionar”

        on.setOnAction(e -> {
            diasExcluidos.remove(d);
            diasAnclados.add(d);
            aplicarEstilo(bRef, d);
            actualizarResumen();
            cm.hide(); // <- CIERRA el menú
        });

        off.setOnAction(e -> {
            diasAnclados.remove(d);
            diasExcluidos.add(d);
            aplicarEstilo(bRef, d);
            actualizarResumen();
            cm.hide(); // <- CIERRA el menú
        });

        beginRange.setOnAction(e -> {
            fechaInicio = d;
            fechaFin = null;
            reconstruirMeses();
            cm.hide();
        });

        endRange.setOnAction(e -> {
            if (fechaInicio == null) {
                fechaInicio = d;
            } else {
                if (d.isBefore(fechaInicio)) { fechaFin = fechaInicio; fechaInicio = d; }
                else                         { fechaFin = d; }
            }
            reconstruirMeses();
            cm.hide();
        });

        horario.setOnAction(e -> {
            mostrarSelectorHorario(d.getDayOfWeek());
            reconstruirMeses();
            cm.hide();
        });

        limpiarSemana.setOnAction(e -> {
            LocalDate monday = d.with(java.time.DayOfWeek.MONDAY);
            for (int i = 0; i < 7; i++) {
                LocalDate x = monday.plusDays(i);
                diasAnclados.remove(x);
                diasExcluidos.remove(x);
            }
            reconstruirMeses();
            cm.hide();
        });

        cm.getItems().addAll(
                on, off,
                new SeparatorMenuItem(),
                beginRange, endRange,
                new SeparatorMenuItem(),
                horario,
                new SeparatorMenuItem(),
                limpiarSemana
        );
        return cm;
    }*/

    private ContextMenu crearContextMenuDia(LocalDate d, Button bRef) {
        ContextMenu cm = new ContextMenu();

        MenuItem on  = new MenuItem("Seleccionar este día");
        MenuItem off = new MenuItem("Deseleccionar este día");
        MenuItem beginRange = new MenuItem("Comenzar rango aquí");
        MenuItem endRange   = new MenuItem("Terminar rango aquí");

        MenuItem addEvent  = new MenuItem("Agregar evento…");
        MenuItem viewEdit  = new MenuItem("Ver / editar eventos…");
        MenuItem delAll    = new MenuItem("Eliminar todos los eventos del día");

        MenuItem horario = new MenuItem("Asignar horario…");
        MenuItem limpiarSemana = new MenuItem("Limpiar overrides de esta semana");

        boolean dentroRango = isInsideRange(d);
        boolean sel = seleccionadoEfectivo(d);
        boolean fijado = diasAnclados.contains(d);

        MenuItem miFix      = new MenuItem(fijado ? "Desfijar este día" : "Fijar este día");

        //on.setOnAction(e -> { overrideOff.remove(d); overrideOn.add(d); aplicarEstilo(bRef, d); actualizarResumen(); });
        //off.setOnAction(e -> { overrideOn.remove(d); overrideOff.add(d); aplicarEstilo(bRef, d); actualizarResumen(); });

        on.setDisable(  seleccionadoEfectivo(d)); // si ya está seleccionado, no tiene sentido "Seleccionar"
        off.setDisable(!seleccionadoEfectivo(d)); // si NO está seleccionado, no tiene sentido "Deseleccionar"

       /* on.setOnAction(e -> {
            overrideOff.remove(d);
            overrideOn.add(d);
            aplicarEstilo(bRef, d);
            actualizarResumen();
        });
        off.setOnAction(e -> {
            overrideOn.remove(d);
            overrideOff.add(d);
            aplicarEstilo(bRef, d);
            actualizarResumen();
        });


        beginRange.setOnAction(e -> { fechaInicio = d; fechaFin = null; reconstruirMeses(); });
        endRange.setOnAction(e -> {
            if (fechaInicio == null) { fechaInicio = d; fechaFin = null; }
            else { if (d.isBefore(fechaInicio)) { fechaFin = fechaInicio; fechaInicio = d; } else { fechaFin = d; } }
            reconstruirMeses();
        });*/
        // --- Acciones ---
        on.setOnAction(e -> {
            // Selección puntual por menú (si no está fijado)
            if (!fijado) {
                overrideOn.add(d);
                ultimoClickExtra = d;
            }
            aplicarEstilo(bRef, d);
            actualizarResumen();
            cm.hide();
        });

        off.setOnAction(e -> {
            // Quitar selección puntual (si no está fijado)
            if (!fijado) {
                overrideOn.remove(d);
                if (d.equals(ultimoClickExtra)) ultimoClickExtra = null;
            }
            aplicarEstilo(bRef, d);
            actualizarResumen();
            cm.hide();
        });

        miFix.setOnAction(e -> {
            if (fijado) {
                diasAnclados.remove(d);
            } else {
                diasAnclados.add(d);
                // si fijamos, aseguramos que esté seleccionado
                overrideOn.add(d);
            }
            aplicarEstilo(bRef, d);
            actualizarResumen();
            cm.hide();
        });

        beginRange.setOnAction(e -> {
            // El rango solo se asigna por menú
            fechaInicio = d;
            fechaFin = null; // se cierra con "Terminar rango aquí"
            reconstruirMeses();
            cm.hide();
        });

        endRange.setOnAction(e -> {
            if (fechaInicio == null) {
                fechaInicio = d;
                fechaFin = null;
            } else {
                if (d.isBefore(fechaInicio)) {
                    fechaFin = fechaInicio;
                    fechaInicio = d;
                } else {
                    fechaFin = d;
                }
            }
            reconstruirMeses();
            cm.hide();
        });

        // --- Habilitar/Deshabilitar según reglas ---
        // Dentro del rango: no se permite seleccionar/deseleccionar con clic,
        // pero sí por menú. Aun así, si quieres obligar menú solo para anclar,
        // puedes deshabilitarlos cuando esté dentro del rango:
        on.setDisable(dentroRango || sel);                  // ya seleccionado ⇒ no tiene sentido
        off.setDisable(dentroRango || !sel || fijado);     // si no está sel o está fijado ⇒ no se quita aquí
        // Rango siempre por menú, así que estas opciones siempre habilitadas:
        beginRange.setDisable(false);
        endRange.setDisable(false);

        cm.getItems().addAll(
                on, off,
                new SeparatorMenuItem(),
                miFix,
                new SeparatorMenuItem(),
                beginRange, endRange
        );

        // Al abrir, refresca estados por si algo cambió
        cm.setOnShowing(ev -> {
            boolean _dentro = isInsideRange(d);
            boolean _sel = seleccionadoEfectivo(d);
            boolean _fix = diasAnclados.contains(d);
            on.setDisable(_dentro || _sel);
            off.setDisable(_dentro || !_sel || _fix);
            miFix.setText(_fix ? "Desfijar este día" : "Fijar este día");
        });
        addEvent.setOnAction(e -> mostrarDialogoEvento(d, null, this::reconstruirMeses));
        viewEdit.setOnAction(e -> mostrarEventosDelDia(d));
        delAll.setOnAction(e -> { eventosPorDia.remove(d); reconstruirMeses(); });

        horario.setOnAction(e -> { mostrarSelectorHorario(d.getDayOfWeek()); reconstruirMeses(); });
        limpiarSemana.setOnAction(e -> {
            LocalDate monday = d.with(DayOfWeek.MONDAY);
            for (int i = 0; i < 7; i++) {
                LocalDate x = monday.plusDays(i);
                overrideOn.remove(x);
                overrideOff.remove(x);
            }
            reconstruirMeses();
        });
        return cm;



        /*return new ContextMenu(
                on, off,
                new SeparatorMenuItem(),
                beginRange, endRange,
                new SeparatorMenuItem(),
                addEvent, viewEdit, delAll,
                new SeparatorMenuItem(),
                horario,
                new SeparatorMenuItem(),
                limpiarSemana
        );*/
    }
    // Añade esto cerca de tus helpers
    private boolean dentroDelRango(LocalDate d) {
        if (!hayRango()) return false;
        return !d.isBefore(minDate()) && !d.isAfter(maxDate());
    }
    // ¿Está dentro del rango real?
    private boolean isInsideRange(LocalDate d) {
        if (fechaInicio == null || fechaFin == null) return false;
        LocalDate a = fechaInicio.isBefore(fechaFin) ? fechaInicio : fechaFin;
        LocalDate z = fechaFin.isAfter(fechaInicio) ? fechaFin : fechaInicio;
        return !d.isBefore(a) && !d.isAfter(z);
    }

    // Último “clic extra” fuera de rango (no fijado)
    private LocalDate ultimoClickExtra = null;

    // Días que deben quedar marcados aunque cambie la selección con un clic normal
    private final Set<LocalDate> diasAnclados = new HashSet<>();

    private void aplicarEstilo(Button b, LocalDate d) {
        // inicio/fin reales tienen prioridad
        if (d.equals(fechaInicio)) { b.setStyle(STYLE_START); return; }
        if (d.equals(fechaFin))    { b.setStyle(STYLE_END);   return; }

        // preview del drag (mientras arrastras)
        if (hayPreview() && !d.isBefore(pMin()) && !d.isAfter(pMax())) {
            b.setStyle(STYLE_SELECTED); // mismo azul de selección
            return;
        }

        // resto según selección efectiva (rango ± overrides ± filtro)
        if (seleccionadoEfectivo(d)) b.setStyle(STYLE_SELECTED);
        else                         b.setStyle(STYLE_NORMAL);
    }

    private int mapSundayZero(DayOfWeek dow) {
        int v = dow.getValue();
        return v % 7;
    }

    private void estilizarBotonHeader(Button b) {
        b.setStyle("-fx-background-color:#E0E0E0; -fx-text-fill:black;");
        b.setMinWidth(36);
    }

    // === Nuevo método para asignar horario con Spinners ===
    private void mostrarSelectorHorario(DayOfWeek dow) {
        Stage dialog = new Stage();
        dialog.setTitle("Asignar horario para " + dow);
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));

        Spinner<Integer> horaInicio = new Spinner<>(0, 23, 16);
        Spinner<Integer> minutoInicio = new Spinner<>(0, 59, 30);

        Spinner<Integer> horaFin = new Spinner<>(0, 23, 18);
        Spinner<Integer> minutoFin = new Spinner<>(0, 59, 0);
        horaFin.setEditable(true);
        minutoFin.setEditable(true);
        horaInicio.setEditable(true);
        minutoInicio.setEditable(true);

        HBox inicioBox = new HBox(5, new Label("Inicio:"), horaInicio, new Label(":"), minutoInicio);
        HBox finBox = new HBox(5, new Label("Fin:"), horaFin, new Label(":"), minutoFin);

        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            String horario = String.format("%02d:%02d - %02d:%02d",
                    horaInicio.getValue(), minutoInicio.getValue(),
                    horaFin.getValue(), minutoFin.getValue());
            horariosPorDia.put(dow, horario);
            reconstruirMeses();
            dialog.close();
        });

        box.getChildren().addAll(inicioBox, finBox, okBtn);

        Scene scene = new Scene(box, 300, 200);
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ESCAPE -> {
                    if (dragging) {
                        dragging = false; previewStart = previewEnd = null;
                        reconstruirMeses();
                    } else {
                        // opcional: limpiar selección
                        // fechaInicio = fechaFin = null; overrideOn.clear(); overrideOff.clear(); reconstruirMeses();
                    }
                }
            }
        });

        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // ==== Drag & Preview de rango ====
    private boolean dragging = false;
    private LocalDate previewStart = null;
    private LocalDate previewEnd   = null;

    private boolean hayPreview() {
        return dragging && previewStart != null && previewEnd != null;
    }
    private LocalDate pMin() {
        return (previewStart.isBefore(previewEnd) ? previewStart : previewEnd);
    }
    private LocalDate pMax() {
        return (previewEnd.isAfter(previewStart) ? previewEnd : previewStart);
    }
    public LocalDate getSelectedDate() { return selectedDate; }

    // === Eventos ===
    private static class Evento {
        String titulo;
        String descripcion;
        String ubicacion;
        LocalTime inicio;
        LocalTime fin;
        boolean recordar; // simple flag, puede mapearse a VALARM en ICS

        Evento(String titulo, String descripcion, String ubicacion, LocalTime inicio, LocalTime fin, boolean recordar) {
            this.titulo = titulo;
            this.descripcion = descripcion;
            this.ubicacion = ubicacion;
            this.inicio = inicio;
            this.fin = fin;
            this.recordar = recordar;
        }
    }

    private final Map<LocalDate, List<Evento>> eventosPorDia = new HashMap<>();

    private void mostrarDialogoEvento(LocalDate fecha, Evento existente, Runnable postSave) {
        Stage dlg = new Stage();
        dlg.setTitle((existente == null ? "Agregar" : "Editar") + " evento - " + fecha);
        dlg.initModality(Modality.APPLICATION_MODAL);

        TextField txtTitulo = new TextField();
        txtTitulo.setPromptText("Título del evento");
        TextField txtUbicacion = new TextField();
        txtUbicacion.setPromptText("Ubicación (opcional)");
        TextArea txtDescripcion = new TextArea();
        txtDescripcion.setPromptText("Descripción (opcional)");
        txtDescripcion.setPrefRowCount(3);

        Spinner<Integer> hIni = new Spinner<>(0, 23, 9);
        Spinner<Integer> mIni = new Spinner<>(0, 59, 0);
        Spinner<Integer> hFin = new Spinner<>(0, 23, 10);
        Spinner<Integer> mFin = new Spinner<>(0, 59, 0);
        hIni.setEditable(true); mIni.setEditable(true);
        hFin.setEditable(true); mFin.setEditable(true);

        CheckBox chkRecordar = new CheckBox("Recordatorio 10 min antes");

        if (existente != null) {
            txtTitulo.setText(existente.titulo);
            txtUbicacion.setText(existente.ubicacion);
            txtDescripcion.setText(existente.descripcion);
            hIni.getValueFactory().setValue(existente.inicio.getHour());
            mIni.getValueFactory().setValue(existente.inicio.getMinute());
            hFin.getValueFactory().setValue(existente.fin.getHour());
            mFin.getValueFactory().setValue(existente.fin.getMinute());
            chkRecordar.setSelected(existente.recordar);
        }

        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(8);
        grid.add(new Label("Título:"), 0, 0); grid.add(txtTitulo, 1, 0, 3, 1);
        grid.add(new Label("Ubicación:"), 0, 1); grid.add(txtUbicacion, 1, 1, 3, 1);
        grid.add(new Label("Descripción:"), 0, 2); grid.add(txtDescripcion, 1, 2, 3, 1);
        grid.add(new Label("Inicio:"), 0, 3); grid.add(hIni, 1, 3); grid.add(new Label(":"), 2, 3); grid.add(mIni, 3, 3);
        grid.add(new Label("Fin:"), 0, 4); grid.add(hFin, 1, 4); grid.add(new Label(":"), 2, 4); grid.add(mFin, 3, 4);
        grid.add(chkRecordar, 1, 5, 3, 1);

        HBox acciones = new HBox(10);
        acciones.setAlignment(Pos.CENTER_RIGHT);
        Button btnCancelar = new Button("Cancelar");
        Button btnGuardar = new Button("Guardar");
        acciones.getChildren().addAll(btnCancelar, btnGuardar);

        VBox root = new VBox(12, grid, acciones);
        root.setPadding(new Insets(16));

        btnCancelar.setOnAction(e -> dlg.close());
        btnGuardar.setOnAction(e -> {
            String t = txtTitulo.getText().trim();
            if (t.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "El título es obligatorio.", ButtonType.OK).showAndWait();
                return;
            }
            LocalTime ini = LocalTime.of(hIni.getValue(), mIni.getValue());
            LocalTime fin = LocalTime.of(hFin.getValue(), mFin.getValue());
            // si fin < inicio, asumimos que cruza medianoche (permitido)
            Evento ev = new Evento(
                    t,
                    txtDescripcion.getText().trim(),
                    txtUbicacion.getText().trim(),
                    ini, fin,
                    chkRecordar.isSelected()
            );

            List<Evento> lista = eventosPorDia.computeIfAbsent(fecha, k -> new ArrayList<>());
            if (existente != null) {
                lista.remove(existente);
            }
            lista.add(ev);
            // orden opcional por inicio
            lista.sort(Comparator.comparing(a -> a.inicio));

            if (postSave != null) postSave.run();
            dlg.close();
        });

        dlg.setScene(new Scene(root, 520, 320));
        dlg.showAndWait();
    }
    private void mostrarEventosDelDia(LocalDate fecha) {
        List<Evento> lista = eventosPorDia.getOrDefault(fecha, Collections.emptyList());
        Stage dlg = new Stage();
        dlg.setTitle("Eventos - " + fecha);
        dlg.initModality(Modality.APPLICATION_MODAL);

        ListView<Evento> lv = new ListView<>();
        lv.getItems().setAll(lista);
        lv.setCellFactory(v -> new ListCell<>() {
            @Override protected void updateItem(Evento it, boolean empty) {
                super.updateItem(it, empty);
                if (empty || it == null) { setText(null); }
                else {
                    setText(String.format("%s  (%02d:%02d - %02d:%02d)%s",
                            it.titulo,
                            it.inicio.getHour(), it.inicio.getMinute(),
                            it.fin.getHour(), it.fin.getMinute(),
                            it.ubicacion == null || it.ubicacion.isBlank() ? "" : " @ " + it.ubicacion
                    ));
                }
            }
        });

        Button btnAgregar = new Button("Agregar");
        Button btnEditar = new Button("Editar");
        Button btnEliminar = new Button("Eliminar");
        Button btnCerrar = new Button("Cerrar");
        HBox acc = new HBox(10, btnAgregar, btnEditar, btnEliminar, btnCerrar);
        acc.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(10, lv, acc);
        root.setPadding(new Insets(12));

        btnAgregar.setOnAction(e -> mostrarDialogoEvento(fecha, null, () -> {
            lv.getItems().setAll(eventosPorDia.getOrDefault(fecha, Collections.emptyList()));
            reconstruirMeses();
        }));
        btnEditar.setOnAction(e -> {
            Evento sel = lv.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            mostrarDialogoEvento(fecha, sel, () -> {
                lv.getItems().setAll(eventosPorDia.getOrDefault(fecha, Collections.emptyList()));
                reconstruirMeses();
            });
        });
        btnEliminar.setOnAction(e -> {
            Evento sel = lv.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            eventosPorDia.getOrDefault(fecha, new ArrayList<>()).remove(sel);
            lv.getItems().setAll(eventosPorDia.getOrDefault(fecha, Collections.emptyList()));
            if (eventosPorDia.getOrDefault(fecha, Collections.emptyList()).isEmpty()) {
                eventosPorDia.remove(fecha);
            }
            reconstruirMeses();
        });
        btnCerrar.setOnAction(e -> dlg.close());

        dlg.setScene(new Scene(root, 520, 360));
        dlg.showAndWait();
    }

}
