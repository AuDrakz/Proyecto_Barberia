package com.example.barberia_reservas.common;

/** No existe una reserva con ese token. Se responde con HTTP 404. */
public class ReservaNoEncontradaException extends RuntimeException {

    public ReservaNoEncontradaException() {
        super("No encontramos una reserva con ese enlace.");
    }
}
