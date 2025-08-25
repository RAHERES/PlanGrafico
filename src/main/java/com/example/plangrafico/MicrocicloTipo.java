package com.example.plangrafico;

public enum MicrocicloTipo {

    // “Ordinario/Corriente” ≈ “Carga” (semana base de trabajo)
    ORDINARIO("O", "Ordinario/Corriente",
            70, 85,
            new BloqueATR[]{BloqueATR.ACUMULACION, BloqueATR.TRANSFORMACION},
            "Semana base con carga media-alta y progresión controlada.",
            5, 8),

    // Impacto = Choque (sobrecarga puntual)
    CHOQUE("CH", "Choque/Impacto",
            90, 100,
            new BloqueATR[]{BloqueATR.ACUMULACION, BloqueATR.TRANSFORMACION},
            "Sobrecarga muy alta de volumen/intensidad para provocar adaptación.",
            7, 14),

    // Precompetitivo ≈ Aproximación / Modelaje
    APROXIMACION("AP", "Aproximación/Precompetitivo",
            35, 45,
            new BloqueATR[]{BloqueATR.REALIZACION},
            "Modela demandas del partido; afina la forma sin acumular fatiga.",
            5, 7),

    // Competición
    COMPETICION("PC", "Competición",
            20, 30,
            new BloqueATR[]{BloqueATR.REALIZACION},
            "Organizado en torno a uno o varios partidos oficiales.",
            5, 7),

    // Restablecimiento / Recuperación
    RESTABLECIMIENTO("R", "Restablecimiento/Recuperación",
            0, 59,
            new BloqueATR[]{BloqueATR.ACUMULACION, BloqueATR.TRANSFORMACION, BloqueATR.REALIZACION},
            "Recuperación activa: bajo volumen e intensidad.",
            3, 7);

    private final String codigo;               // O, CH, AP, PC, R
    private final String nombreVisible;        // Etiqueta para UI
    private final int cargaMinPct;             // % de carga sugerido (tabla)
    private final int cargaMaxPct;             // % de carga sugerido (tabla)
    private final BloqueATR[] bloquesSugeridos;// ATR donde se usa más
    private final String descripcion;          // Tooltip / ayuda
    private final int duracionMinDias;         // Para validaciones
    private final int duracionMaxDias;

    MicrocicloTipo(String codigo, String nombreVisible,
                   int cargaMinPct, int cargaMaxPct,
                   BloqueATR[] bloquesSugeridos,
                   String descripcion,
                   int duracionMinDias, int duracionMaxDias) {
        this.codigo = codigo;
        this.nombreVisible = nombreVisible;
        this.cargaMinPct = cargaMinPct;
        this.cargaMaxPct = cargaMaxPct;
        this.bloquesSugeridos = bloquesSugeridos;
        this.descripcion = descripcion;
        this.duracionMinDias = duracionMinDias;
        this.duracionMaxDias = duracionMaxDias;
    }

    public String getCodigo() { return codigo; }
    public String getNombreVisible() { return nombreVisible; }
    public int getCargaMinPct() { return cargaMinPct; }
    public int getCargaMaxPct() { return cargaMaxPct; }
    public BloqueATR[] getBloquesSugeridos() { return bloquesSugeridos; }
    public String getDescripcion() { return descripcion; }
    public int getDuracionMinDias() { return duracionMinDias; }
    public int getDuracionMaxDias() { return duracionMaxDias; }

    // Alias comunes (compatible con tu plan gráfico y diferentes fuentes)
    public static MicrocicloTipo fromCodigo(String code) {
        if (code == null) return null;
        String c = code.trim().toUpperCase();
        return switch (c) {
            case "O", "ORD", "ORDINARIO", "CORRIENTE", "CARGA" -> ORDINARIO;
            case "CH", "CHOQUE", "IMPACTO", "I" -> CHOQUE;
            case "AP", "APROX", "APROXIMACION", "PRECOMP", "PCP", "MODELADO", "MODELAJE", "ACTIVACION" -> APROXIMACION;
            case "PC", "COMP", "COMPETICION", "C" -> COMPETICION;
            case "R", "REST", "RESTABLECIMIENTO", "RECUP", "RECUPERACION" -> RESTABLECIMIENTO;
            default -> null;
        };
    }

    // Sugerencia rápida de minutos según % de carga y tiempo de microciclo
    public int minutosSugeridos(int minutosBloque, int porcentajeDelBloque) {
        // minutos asignados al micro dentro del bloque (metodología del “coeficiente indicativo”)
        // Este método te da una distribución simple proporcional.
        return (minutosBloque * porcentajeDelBloque) / 100;
    }

    public enum BloqueATR { ACUMULACION, TRANSFORMACION, REALIZACION }
}
