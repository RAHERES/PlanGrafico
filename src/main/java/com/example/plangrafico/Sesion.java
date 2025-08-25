package com.example.plangrafico;

import java.time.LocalDate;

public final class Sesion {
    private long id;
    private long microcicloId;
    private LocalDate fecha;
    private String foco;             // p.ej. Resistencia, Velocidad, Técnica
    private int duracionMin;
    private String notas;
    // En futuro: ejercicios, series, etc.
}
