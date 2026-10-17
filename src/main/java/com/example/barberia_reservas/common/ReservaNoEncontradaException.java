package com.example.barberia_reservas.common;

/** No existe la reserva pedida (por token o por id). */
public class ReservaNoEncontradaException extends NoEncontradoException {

    public ReservaNoEncontradaException() {
        this("No encontramos una reserva con ese enlace.");
    }

    public ReservaNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
