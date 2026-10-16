package com.example.barberia_reservas.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce excepciones del negocio a respuestas HTTP claras, siempre con el formato estándar "ProblemDetail" (RFC 9457).
 * Códigos: 400 = datos inválidos, 404 = no existe, 409 = una regla de negocio lo impide.
 */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(FechaFueraDeVentanaException.class)
    public ProblemDetail fechaFueraDeVentana(FechaFueraDeVentanaException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problema.setTitle("Fecha no disponible para reservar");
        return problema;
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ProblemDetail solicitudInvalida(SolicitudInvalidaException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problema.setTitle("Datos inválidos");
        problema.setProperty("errores", ex.getErrores()); // { campo: mensaje }
        return problema;
    }

    @ExceptionHandler(ConflictoDeNegocioException.class)
    public ProblemDetail conflicto(ConflictoDeNegocioException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problema.setTitle("No se pudo completar la operación");
        return problema;
    }

    @ExceptionHandler(ReservaNoEncontradaException.class)
    public ProblemDetail noEncontrada(ReservaNoEncontradaException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problema.setTitle("Reserva no encontrada");
        return problema;
    }
}
