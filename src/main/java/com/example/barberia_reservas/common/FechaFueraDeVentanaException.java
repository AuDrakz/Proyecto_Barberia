package com.example.barberia_reservas.common;

/** Se lanza cuando se pide una fecha que no se puede reservar (muy pronto o muy lejana). */
public class FechaFueraDeVentanaException extends RuntimeException {

    public FechaFueraDeVentanaException(String mensaje) {
        super(mensaje);
    }
}
