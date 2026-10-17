package com.example.barberia_reservas.horario;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Locale;

public record DisponibilidadResponse(Long id, int diaSemana, String nombreDia, String horaInicio, String horaFin) {

    public static DisponibilidadResponse desde(Disponibilidad disponibilidad) {
        return new DisponibilidadResponse(
                disponibilidad.getId(),
                disponibilidad.getDiaSemana(),
                DayOfWeek.of(disponibilidad.getDiaSemana()).getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es")),
                disponibilidad.getHoraInicio().toString(),
                disponibilidad.getHoraFin().toString());
    }
}
