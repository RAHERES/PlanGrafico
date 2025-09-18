module com.example.plangrafico {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires java.sql;
    requires java.desktop;
    requires javafx.swing;

    // nombre del módulo automático del jar de sqlite-jdbc:
    requires org.xerial.sqlitejdbc;
    requires com.calendarfx.view;
    requires com.google.gson;

    opens com.example.plangrafico to javafx.fxml;
    exports com.example.plangrafico;
    exports com.example.plangrafico.unidadentrenamiento;
    opens com.example.plangrafico.unidadentrenamiento to javafx.fxml;
}