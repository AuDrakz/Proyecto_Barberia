package com.example.barberia_reservas.common;

/** Una regla de negocio impide la operación (horario ocupado, límite de solicitudes, estado inválido...). Se responde con HTTP 409. */
public class ConflictoDeNegocioException extends RuntimeException {

    public ConflictoDeNegocioException(String mensaje) {
        super(mensaje);
    }
}
