package com.example.barberia_reservas.reserva;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Lo que la API DEVUELVE de una reserva. A propósito NO incluye el teléfono ni el id interno:
 * se expone lo mínimo necesario. El token es la "llave" del cliente para ver y cancelar su reserva.
 */
public record ReservaResponse(String token, String nombreCliente, String hora, String inicio, String estado) {

    public static ReservaResponse desde(Reserva reserva, ZoneId zona) {
        ZonedDateTime inicio = reserva.getInicio().atZone(zona);
        return new ReservaResponse(
                reserva.getTokenCancelacion().toString(),
                reserva.getNombreCliente(),
                inicio.toLocalTime().toString(),
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(inicio),
                reserva.getEstado().name());
    }
}
