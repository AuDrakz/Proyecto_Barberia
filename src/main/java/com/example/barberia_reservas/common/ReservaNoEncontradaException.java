package com.example.barberia_reservas.common;

/** No existe la reserva pedida (por token o por id). Se responde con HTTP 404. */
public class ReservaNoEncontradaException extends RuntimeException {

    public ReservaNoEncontradaException() {
        this("No encontramos una reserva con ese enlace.");
    }

    public ReservaNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
