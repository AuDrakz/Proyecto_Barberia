package com.example.barberia_reservas.horario;

import java.time.LocalDate;

/** Un día de la ventana de reservas que tiene al menos un horario libre. */
public record DiaConHorarios(LocalDate fecha, int libres) {
}
