package com.example.plangrafico;


import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PlanGraficoDAO {

    public long insert(PlanGrafico p) {
        String sql = """
            INSERT INTO plan_grafico (nombre, fecha_inicio, fecha_fin, tipo, notas, version)
            VALUES (?,?,?,?,?,?)
            """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getFechaInicio().toString());
            ps.setString(3, p.getFechaFin().toString());
            ps.setString(4, p.getTipo().name());
            ps.setString(5, p.getNotas());
            ps.setInt(6, p.getVersion());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error insertando plan", e);
        }
        return -1;
    }

    public List<PlanGrafico> findAll() {
        String sql = "SELECT id, nombre, fecha_inicio, fecha_fin, tipo, notas, version FROM plan_grafico ORDER BY id DESC";
        List<PlanGrafico> out = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                PlanGrafico p = new PlanGrafico();
                p.setId(rs.getLong("id"));
                p.setNombre(rs.getString("nombre"));
                p.setFechaInicio(LocalDate.parse(rs.getString("fecha_inicio")));
                p.setFechaFin(LocalDate.parse(rs.getString("fecha_fin")));
                p.setTipo(TipoPeriodizacion.valueOf(rs.getString("tipo")));
                p.setNotas(rs.getString("notas"));
                p.setVersion(rs.getInt("version"));
                out.add(p);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando planes", e);
        }
        return out;
    }
}

