package com.example.barberia_reservas.horario;

/** solicitudesPendientesAfectadas solo viene al CREAR un bloqueo (en los listados es null). */
public record BloqueoResponse(
        Long id,
        String fecha,
        String horaInicio,
        String horaFin,
        boolean diaCompleto,
        String motivo,
        Integer solicitudesPendientesAfectadas) {

    public static BloqueoResponse desde(Bloqueo bloqueo, Integer solicitudesPendientesAfectadas) {
        return new BloqueoResponse(
                bloqueo.getId(),
                bloqueo.getFecha().toString(),
                bloqueo.getHoraInicio() == null ? null : bloqueo.getHoraInicio().toString(),
                bloqueo.getHoraFin() == null ? null : bloqueo.getHoraFin().toString(),
                bloqueo.esDiaCompleto(),
                bloqueo.getMotivo(),
                solicitudesPendientesAfectadas);
    }
}
