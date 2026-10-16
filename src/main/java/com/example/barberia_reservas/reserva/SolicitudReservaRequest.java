package com.example.barberia_reservas.reserva;

/**
 * Cuerpo JSON de POST /api/reservas. "inicio" es el valor EXACTO que entregó GET /api/horarios
 * (por ejemplo "2026-10-20T19:00:00-03:00"). Todo llega como texto y se valida en ValidadorSolicitud.
 */
public record SolicitudReservaRequest(String nombreCliente, String telefono, String instagram, String inicio) {
}
