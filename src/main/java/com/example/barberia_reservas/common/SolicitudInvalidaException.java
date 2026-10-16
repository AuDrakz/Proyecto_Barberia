package com.example.barberia_reservas.common;

import java.util.Map;

/** Los datos enviados no son válidos. Lleva el detalle por campo para que el frontend lo muestre. Se responde con HTTP 400. */
public class SolicitudInvalidaException extends RuntimeException {

    private final Map<String, String> errores;

    public SolicitudInvalidaException(Map<String, String> errores) {
        super("Hay datos que corregir.");
        this.errores = Map.copyOf(errores);
    }

    public Map<String, String> getErrores() {
        return errores;
    }
}
