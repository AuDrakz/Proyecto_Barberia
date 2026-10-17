package com.example.barberia_reservas.horario;

import java.time.LocalDate;
import java.time.LocalTime;

/** Datos de un bloqueo YA validados. Sin horas = día completo. */
public record NuevoBloqueo(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, String motivo) {

    public boolean esDiaCompleto() {
        return horaInicio == null;
    }
}
