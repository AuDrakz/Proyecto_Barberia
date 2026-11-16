package com.example.barberia_reservas.horario;

import java.time.LocalTime;
import java.time.ZonedDateTime;

/** Un horario libre: la hora "de pared" (19:00) y el instante exacto con su zona horaria. */
public record Horario(LocalTime hora, ZonedDateTime inicio) {
}
