package com.example.plangrafico;

import java.util.Arrays;
import java.util.List;

/*
public enum MesocicloTipo { ACUMULACION, TRANSFORMACION, REALIZACION, BASE, ESPECIAL }
*/
public enum MesocicloTipo {

    // —— Tradicional ——
    ENTRANTE("Entrante/Gradual", "Adaptación inicial con cargas progresivas", ModeloPeriodizacion.TRADICIONAL),
    BASICO_DESARROLLADOR("Básico Desarrollador", "Desarrollo general de las capacidades", ModeloPeriodizacion.TRADICIONAL),
    BASICO_ESTABILIZADOR("Básico Estabilizador", "Consolidación de los logros alcanzados", ModeloPeriodizacion.TRADICIONAL),
    PRECOMPETITIVO("Precompetitivo", "Puesta a punto con cargas similares a la competencia", ModeloPeriodizacion.TRADICIONAL),
    COMPETITIVO("Competitivo", "Orientado al calendario de competencias oficiales", ModeloPeriodizacion.TRADICIONAL),

    // —— ATR ——
    ACUMULACION("Acumulación (A)", "Alto volumen, baja-moderada intensidad, preparación general", ModeloPeriodizacion.ATR),
    TRANSFORMACION("Transformación (T)", "Mayor intensidad, trabajo específico", ModeloPeriodizacion.ATR),
    REALIZACION("Realización (R)", "Puesta a punto, volumen bajo e intensidad específica", ModeloPeriodizacion.ATR);

    private final String nombre;
    private final String descripcion;
    private final ModeloPeriodizacion modelo;

    MesocicloTipo(String nombre, String descripcion, ModeloPeriodizacion modelo) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.modelo = modelo;
    }

    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public ModeloPeriodizacion getModelo() { return modelo; }

    // Filtra los mesociclos según el modelo de periodización elegido
    public static List<MesocicloTipo> getPorModelo(ModeloPeriodizacion modelo) {
        return Arrays.stream(values())
                .filter(m -> m.modelo == modelo)
                .toList();
    }


}
