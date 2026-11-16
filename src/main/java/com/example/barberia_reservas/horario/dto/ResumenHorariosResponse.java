package com.example.barberia_reservas.horario;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/** Respuesta de GET /api/horarios/resumen: los días con horarios libres, en orden. */
public record ResumenHorariosResponse(List<Dia> dias) {

    public record Dia(String fecha, String diaSemana, int libres) {
    }

    public static ResumenHorariosResponse desde(List<DiaConHorarios> dias) {
        Locale es = Locale.forLanguageTag("es");
        return new ResumenHorariosResponse(dias.stream()
                .map(dia -> new Dia(
                        dia.fecha().toString(),
                        dia.fecha().getDayOfWeek().getDisplayName(TextStyle.FULL, es),
                        dia.libres()))
                .toList());
    }
}
