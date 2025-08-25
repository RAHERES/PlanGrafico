package com.example.plangrafico;


import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public final class RangeSelectionManager {
    private LocalDate anchor;
    private LocalDate start, end;
    private final Set<LocalDate> fixedDates = new HashSet<>();

    public boolean isInsideCurrentRange(LocalDate d) {
        if (start == null || end == null) return false;
        return !d.isBefore(start) && !d.isAfter(end);
    }

    public void startNewRangeAt(LocalDate d) {
        anchor = d; start = d; end = d;
    }

    public void extendRangeTo(LocalDate d) {
        if (anchor == null) { startNewRangeAt(d); return; }
        start = d.isBefore(anchor) ? d : anchor;
        end   = d.isAfter(anchor)  ? d : anchor;
    }

    public ContextMenu buildContextMenuFor(LocalDate d, Node owner, Runnable repaint) {
        ContextMenu cm = new ContextMenu();

        MenuItem fijar = new MenuItem(fixedDates.contains(d) ? "Desfijar fecha" : "Fijar fecha");
        fijar.setOnAction(e -> {
            if (fixedDates.contains(d)) fixedDates.remove(d); else fixedDates.add(d);
            repaint.run();
        });

        MenuItem ajustar4Sem = new MenuItem("Ajustar rango a 4 semanas");
        ajustar4Sem.setOnAction(e -> {
            if (start == null) return;
            end = start.plusDays(27); // 4*7 -1
            repaint.run();
        });

        MenuItem eliminar = new MenuItem("Eliminar rango");
        eliminar.setOnAction(e -> { start = end = anchor = null; repaint.run(); });

        cm.getItems().addAll(fijar, ajustar4Sem, eliminar);
        return cm;
    }

    /** Marca visual: agrega/quita estilos CSS en celdas del GridPane */
    public void paintSelection(GridPane grid) {
        grid.getChildren().forEach(n -> n.getStyleClass().removeAll("cal-selected","cal-fixed"));
        if (start == null || end == null) return;
        for (Node n : grid.getChildren()) {
            LocalDate d = (LocalDate) n.getUserData();
            if (d == null) continue;
            if (!d.isBefore(start) && !d.isAfter(end)) {
                n.getStyleClass().add("cal-selected");
            }
            if (fixedDates.contains(d)) {
                n.getStyleClass().add("cal-fixed");
            }
        }
    }
}
