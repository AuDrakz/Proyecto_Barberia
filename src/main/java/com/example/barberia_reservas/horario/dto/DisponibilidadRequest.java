package com.example.barberia_reservas.horario;

/** Cuerpo de POST /api/admin/disponibilidad. diaSemana: 1 = lunes ... 7 = domingo. Las horas, en formato HH:mm. */
public record DisponibilidadRequest(Integer diaSemana, String horaInicio, String horaFin) {
}
