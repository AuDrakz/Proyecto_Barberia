package com.example.barberia_reservas.common;

/** Se intentó un cambio de estado que el ciclo de vida de la reserva no permite (por ejemplo, confirmar una cancelada). */
public class TransicionInvalidaException extends ConflictoDeNegocioException {

    public TransicionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
