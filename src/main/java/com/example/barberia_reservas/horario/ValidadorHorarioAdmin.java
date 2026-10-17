package com.example.barberia_reservas.horario;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.example.barberia_reservas.common.SolicitudInvalidaException;

/**
 * Valida lo que envía el administrador al crear disponibilidad o bloqueos. Aunque el administrador sea de confianza,
 * sus datos también se validan: un error de tipeo no debe dejar datos absurdos (un tramo de fin antes del inicio),
 * y un frontend con un error no debe poder corromper la base de datos. Reúne TODOS los errores a la vez.
 * Lógica pura: sin Spring ni base de datos.
 */
public final class ValidadorHorarioAdmin {

    private static final Pattern HORA = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$"); // HH:mm, sin segundos
    private static final String MENSAJE_HORA = "Usa el formato HH:mm (por ejemplo 19:00).";
    private static final int MAX_MOTIVO = 200;

    private final int duracionMinutos;

    public ValidadorHorarioAdmin(int duracionMinutos) {
        if (duracionMinutos <= 0) {
            throw new IllegalArgumentException("La duración debe ser mayor que 0");
        }
        this.duracionMinutos = duracionMinutos;
    }

    public NuevaDisponibilidad validarDisponibilidad(Integer diaSemana, String horaInicio, String horaFin) {
        Map<String, String> errores = new LinkedHashMap<>();

        if (diaSemana == null || diaSemana < 1 || diaSemana > 7) {
            errores.put("diaSemana", "Elige un día de la semana (1 = lunes ... 7 = domingo).");
        }
        LocalTime inicio = leerHora(horaInicio);
        LocalTime fin = leerHora(horaFin);
        if (inicio == null) {
            errores.put("horaInicio", MENSAJE_HORA);
        }
        if (fin == null) {
            errores.put("horaFin", MENSAJE_HORA);
        }
        if (inicio != null && fin != null) {
            if (!fin.isAfter(inicio)) {
                errores.put("horaFin", "La hora de fin debe ser posterior a la de inicio.");
            } else if (Duration.between(inicio, fin).toMinutes() < duracionMinutos) {
                errores.put("horaFin", "El tramo debe durar al menos " + duracionMinutos
                        + " minutos (el largo de una cita).");
            }
        }

        if (!errores.isEmpty()) {
            throw new SolicitudInvalidaException(errores);
        }
        return new NuevaDisponibilidad(diaSemana.shortValue(), inicio, fin);
    }

    public NuevoBloqueo validarBloqueo(String fecha, String horaInicio, String horaFin, String motivo, LocalDate hoy) {
        Map<String, String> errores = new LinkedHashMap<>();

        LocalDate dia = null;
        if (fecha == null || fecha.isBlank()) {
            errores.put("fecha", "Indica la fecha (AAAA-MM-DD).");
        } else {
            try {
                dia = LocalDate.parse(fecha.trim());
                if (dia.isBefore(hoy)) {
                    errores.put("fecha", "No se pueden bloquear fechas pasadas.");
                }
            } catch (DateTimeParseException e) {
                errores.put("fecha", "La fecha debe tener el formato AAAA-MM-DD.");
            }
        }

        boolean hayInicio = horaInicio != null && !horaInicio.isBlank();
        boolean hayFin = horaFin != null && !horaFin.isBlank();
        LocalTime inicio = null;
        LocalTime fin = null;
        if (hayInicio != hayFin) {
            errores.put(hayInicio ? "horaFin" : "horaInicio",
                    "Indica inicio y fin, o deja ambos vacíos para bloquear el día completo.");
        } else if (hayInicio) {
            inicio = leerHora(horaInicio);
            fin = leerHora(horaFin);
            if (inicio == null) {
                errores.put("horaInicio", MENSAJE_HORA);
            }
            if (fin == null) {
                errores.put("horaFin", MENSAJE_HORA);
            }
            if (inicio != null && fin != null && !fin.isAfter(inicio)) {
                errores.put("horaFin", "La hora de fin debe ser posterior a la de inicio.");
            }
        }

        String motivoLimpio = motivo == null ? null : motivo.trim();
        if (motivoLimpio != null && motivoLimpio.isEmpty()) {
            motivoLimpio = null;
        }
        if (motivoLimpio != null && motivoLimpio.length() > MAX_MOTIVO) {
            errores.put("motivo", "El motivo no puede superar los " + MAX_MOTIVO + " caracteres.");
        }

        if (!errores.isEmpty()) {
            throw new SolicitudInvalidaException(errores);
        }
        return new NuevoBloqueo(dia, inicio, fin, motivoLimpio);
    }

    private static LocalTime leerHora(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim();
        return HORA.matcher(limpio).matches() ? LocalTime.parse(limpio) : null;
    }
}
