package com.example.barberia_reservas.horario;

/** Cuerpo de POST /api/admin/bloqueos. fecha: AAAA-MM-DD. Sin horaInicio ni horaFin, bloquea el día completo. */
public record BloqueoRequest(String fecha, String horaInicio, String horaFin, String motivo) {
}
