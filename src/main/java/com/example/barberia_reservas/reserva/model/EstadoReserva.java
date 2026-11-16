package com.example.barberia_reservas.reserva;

/** Ciclo de vida: SOLICITADA -> CONFIRMADA | RECHAZADA, y CONFIRMADA -> CANCELADA. Solo CONFIRMADA bloquea el horario. */
public enum EstadoReserva {
    SOLICITADA,
    CONFIRMADA,
    RECHAZADA,
    CANCELADA
}
