package com.example.plangrafico;



import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class PlanService {
    private final PlanGraficoDAO dao = new PlanGraficoDAO();
    public static  PlanGrafico plan;

    public PlanService() {
        Database.initSchema();
    }

    public PlanGrafico crearPlanBasico(String nombre, LocalDate inicio, LocalDate fin, TipoPeriodizacion tipo) {
       /* PlanGrafico p = PlanGrafico.builder()
                .categoria("")
                .deporte(nombre)
                .inicio(inicio)
                .fin(fin)
                .equipo("Stub equipo")
                .build();
        p.setNombre(nombre);
        p.setTipo(tipo);
        p.setNotas("Plan creado automáticamente");
        p.setVersion(1);
        long id = dao.insert(p);
        p.setId(id);*/
       plan = PlanGrafico.builder()
                .deporte("Fútbol")
                .categoria("Sub 17")
                .equipo("Villa de las Niñas")
                .modelo(ModeloPeriodizacion.ATR)
                .inicio(LocalDate.of(2025, 8, 1))
                .fin(LocalDate.of(2026, 6, 30))
                .addMesociclo(m -> m
                        .tipo(MesocicloTipo.ACUMULACION)
                        .nombre("M1 – Acumulación")
                        .inicio(LocalDate.of(2025, 8, 1))
                        .fin(LocalDate.of(2025, 9, 12))
                        .resumen(new PlanGrafico.ResumenCarga(900, 0.6, 1.8, 5))
                        .addMicrociclo(x -> x
                                .tipo(MicrocicloTipo.ORDINARIO)
                                .inicio(LocalDate.of(2025, 8, 1))
                                .fin(LocalDate.of(2025, 8, 7))
                                .porcentajeCarga(70)
                                .contenido("Fuerza", 180)
                                .contenido("Técnica", 120)
                                .contenido("Táctica", 60))
                        .addMicrociclo(x -> x
                                .tipo(MicrocicloTipo.CHOQUE)
                                .inicio(LocalDate.of(2025, 8, 8))
                                .fin(LocalDate.of(2025, 8, 14))
                                .porcentajeCarga(90)
                                .contenido("Fuerza", 210)
                                .contenido("Resistencia", 180)
                                .contenido("Técnica", 90))
                        .addMicrociclo(x -> x
                                .tipo(MicrocicloTipo.RESTABLECIMIENTO)
                                .inicio(LocalDate.of(2025, 8, 15))
                                .fin(LocalDate.of(2025, 8, 21))
                                .porcentajeCarga(40)
                                .contenido("Movilidad", 90)
                                .contenido("Recuperación", 120))
                )
                .addEvento(e -> e
                        .tipo(EventoDeportivo.Tipo.COMPETENCIA)
                        .nombre("Amistoso vs León Elite")
                        .fecha(LocalDate.of(2025, 9, 6))
                        .notas("Prueba de control de juego posicional"))
                .build();

        List<String> problemas = plan.validar();
        System.out.println("Validación: " + (problemas.isEmpty() ? "OK" : problemas));
        System.out.println("Semanas totales: " + plan.semanasTotales());
        System.out.println("Serie carga (primeros 5 pts): " +
                plan.serieCargaDiaria().stream().limit(5).collect(Collectors.toList()));
        System.out.println("Minutos por contenido: " + plan.resumenMinutosPorContenido());

        /*CalendarViewController calendarViewController = new CalendarViewController();
        calendarViewController.setPlan(plan); // esto construirá el adapter y repintará*/

        CalendarioAnualDialog anualDialog = new CalendarioAnualDialog();
            anualDialog.mostrar(new Stage(), new Label("Stub label calendario anual"));
        return plan;
    }



    /**
     * Prepara la base de datos para uso de la app.
     * Idempotente: sólo ejecuta la preparación una vez por proceso.
     * Lanza RuntimeException si algo falla (para que el caller lo vea).
     */
    public void ensureReady() {
       /* if (!INITIALIZED.compareAndSet(false, true)) {
            return; // ya estaba listo
        }*/
        try {
            // 1) Crear/actualizar esquema
            Database.initSchema();

            // 2) Ajustes de conexión recomendados
            try (var c = Database.getConnection(); Statement st = c.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }

            // 3) Prueba de humo (opcional pero útil): que responda la DB
            try (var c = Database.getConnection();
                 var st = c.createStatement();
                 var rs = st.executeQuery("SELECT sqlite_version()")) {
                if (!rs.next()) {
                    throw new IllegalStateException("No pude leer versión de SQLite.");
                }
                // System.out.println("SQLite OK: " + rs.getString(1)); // si quieres log
            }
        } catch (Exception e) {
            // Permitir reintentos si falló
         //   INITIALIZED.set(false);
            throw new RuntimeException("Fallo al preparar la base de datos", e);
        }
    }

    public List<PlanGrafico> listarPlanes() {
        return dao.findAll();
    }
}

