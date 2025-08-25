package com.example.plangrafico;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class Periodo {
    private long id;
    private long planId;
    private PeriodoTipo tipo;
    private LocalDate inicio;
    private LocalDate fin;           // derivado por duración o seteado directo
    private int semanas;             // redundante para consultas rápidas
    private List<Mesociclo> mesociclos = new ArrayList<>();
}
