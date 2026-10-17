package com.example.barberia_reservas.common;

/** No existe el recurso pedido (una reserva, un tramo de disponibilidad, un bloqueo...). Se responde con HTTP 404. */
public class NoEncontradoException extends RuntimeException {

    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
