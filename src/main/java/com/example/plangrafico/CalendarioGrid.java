package com.example.plangrafico;

import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

import java.time.LocalDate;
import java.util.List;

public final class CalendarioGrid extends GridPane {
    private final LocalDate inicio;
    private final LocalDate fin;
    private final RangeSelectionManager selectionManager = new RangeSelectionManager();

    public CalendarioGrid(LocalDate inicio, LocalDate fin) {
        this.inicio = inicio;
        this.fin = fin;
        setHgap(2); setVgap(2);
        build();
    }

    private void build() {
        List<LocalDate> dias = inicio.datesUntil(fin.plusDays(1)).toList();
        int col = 0, row = 0; // p.ej. 7 columnas (L-D), filas = semanas
        for (LocalDate d : dias) {
            StackPane cell = createCell(d);
            add(cell, col, row);
            col++;
            if (col == 7) { col = 0; row++; }
        }
    }

    /*private StackPane createCell(LocalDate fecha) {
        StackPane cell = new StackPane();
        cell.getStyleClass().add("cal-cell");
        cell.setOnMouseClicked(evt -> {
            if (selectionManager.isInsideCurrentRange(fecha)) {
                // No togglear; mostrar menú contextual
                if (evt.getButton().name().equals("SECONDARY")) {
                    ContextMenu menu = selectionManager.buildContextMenuFor(fecha, cell);
                    menu.show(cell, evt.getScreenX(), evt.getScreenY());
                }
                return;
            }
            if (evt.isShiftDown()) {
                selectionManager.extendRangeTo(fecha);
            } else {
                selectionManager.startNewRangeAt(fecha);
            }
            selectionManager.paintSelection(this);
        });
        cell.setOnContextMenuRequested(evt -> {
            ContextMenu menu = selectionManager.buildContextMenuFor(fecha, cell);
            menu.show(cell, evt.getScreenX(), evt.getScreenY());
        });
        return cell;
    }*/

    private final RangeSelectionManager selection = new RangeSelectionManager();
    private StackPane createCell(LocalDate fecha) {
        StackPane cell = new StackPane();
        cell.getStyleClass().add("cal-cell");
        cell.setUserData(fecha); // para paintSelection
        cell.setPrefSize(44, 32);
        Text t = new Text(String.valueOf(fecha.getDayOfMonth()));
        cell.getChildren().add(t);
        StackPane.setAlignment(t, Pos.TOP_LEFT);

        cell.setOnMouseClicked(evt -> {
            if (evt.getButton() == MouseButton.SECONDARY) {
                ContextMenu menu = selection.buildContextMenuFor(fecha, cell, () -> selection.paintSelection(this));
                menu.show(cell, evt.getScreenX(), evt.getScreenY());
                return;
            }
            // Click izquierdo
            if (selection.isInsideCurrentRange(fecha)) {
                // No togglear si está dentro del rango (regla solicitada)
                return;
            }
            if (evt.isShiftDown()) selection.extendRangeTo(fecha);
            else selection.startNewRangeAt(fecha);
            selection.paintSelection(this);
        });

        cell.setOnContextMenuRequested(evt -> {
            ContextMenu menu = selection.buildContextMenuFor(fecha, cell, () -> selection.paintSelection(this));
            menu.show(cell, evt.getScreenX(), evt.getScreenY());
        });

        return cell;
    }
}

