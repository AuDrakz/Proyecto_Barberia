package com.example.barberia_reservas.reserva;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Lo que ve el ADMINISTRADOR de una reserva. A diferencia de la respuesta pública, incluye el teléfono
 * (y un enlace de WhatsApp) porque el administrador necesita contactar al cliente. Solo se expone detrás del login.
 */
public record ReservaAdminResponse(
        Long id,
        String nombreCliente,
        String telefono,
        String whatsapp,
        String instagram,
        String hora,
        String inicio,
        String estado,
        String canceladaPor,
        String creadoEn) {

    public static ReservaAdminResponse desde(Reserva reserva, ZoneId zona) {
        ZonedDateTime inicio = reserva.getInicio().atZone(zona);
        String soloDigitos = reserva.getTelefono().replaceAll("[^0-9]", "");
        return new ReservaAdminResponse(
                reserva.getId(),
                reserva.getNombreCliente(),
                reserva.getTelefono(),
                "https://wa.me/" + soloDigitos,
                reserva.getInstagram(),
                inicio.toLocalTime().toString(),
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(inicio),
                reserva.getEstado().name(),
                reserva.getCanceladaPor() == null ? null : reserva.getCanceladaPor().name(),
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(reserva.getCreadoEn().atZone(zona)));
    }
}
