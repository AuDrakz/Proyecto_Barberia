package com.example.barberia_reservas.reserva;

import java.time.Instant;

/** Datos de una solicitud YA validados y limpios (nombre sin espacios de más, teléfono en E.164, etc.). */
public record SolicitudNormalizada(String nombreCliente, String telefono, String instagram, Instant inicio) {
}
