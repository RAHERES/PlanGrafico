package com.example.plangrafico;


import java.nio.file.*;
import java.sql.*;

public final class Database {
    private static final String DB_DIR = System.getProperty("user.home") + "/.plan_grafico";
    private static final String DB_URL = "jdbc:sqlite:" + DB_DIR + "/plan_grafico.db";

    static {
        try {
            Files.createDirectories(Path.of(DB_DIR));
        } catch (Exception ignored) { }
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        conn.createStatement().execute("PRAGMA foreign_keys = ON");
        return conn;
    }

    public static void initSchema() {
        String ddlPlan = """
            CREATE TABLE IF NOT EXISTS plan_grafico (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              nombre TEXT NOT NULL,
              fecha_inicio TEXT NOT NULL,
              fecha_fin TEXT NOT NULL,
              tipo TEXT NOT NULL,
              notas TEXT,
              version INTEGER DEFAULT 1,
              creado_en TEXT DEFAULT CURRENT_TIMESTAMP,
              actualizado_en TEXT DEFAULT CURRENT_TIMESTAMP
            );
            """;
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            st.execute(ddlPlan);
        } catch (SQLException e) {
            throw new RuntimeException("Error creando esquema SQLite", e);
        }
    }
}

