package com.example.barberia_reservas.horario;

import java.time.LocalTime;

/** Datos de un tramo semanal YA validados. */
public record NuevaDisponibilidad(short diaSemana, LocalTime horaInicio, LocalTime horaFin) {
}
