package com.example.barberia_reservas.horario;

import java.time.LocalDate;

import com.example.barberia_reservas.common.FechaFueraDeVentanaException;

/**
 * Regla de negocio: solo se puede reservar dentro de una "ventana" de fechas.
 * Con diasMinimos = 1, la última oportunidad para reservar un día es el día anterior.
 * Es una clase pura (sin Spring ni base de datos): se prueba en milisegundos.
 */
public record ReglaAnticipacion(int diasMinimos, int diasMaximos) {

    public ReglaAnticipacion {
        if (diasMinimos < 0 || diasMaximos < diasMinimos) {
            throw new IllegalArgumentException("Ventana inválida: " + diasMinimos + " - " + diasMaximos);
        }
    }

    public void validar(LocalDate fecha, LocalDate hoy) {
        LocalDate primera = hoy.plusDays(diasMinimos);
        LocalDate ultima = hoy.plusDays(diasMaximos);
        if (fecha.isBefore(primera) || fecha.isAfter(ultima)) {
            throw new FechaFueraDeVentanaException(
                    "Solo se puede reservar entre el " + primera + " y el " + ultima + ".");
        }
    }
}
