package com.example.barberia_reservas.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce excepciones del negocio a respuestas HTTP claras.
 * Usa el formato estándar "ProblemDetail" (RFC 9457), para que el frontend reciba siempre la misma forma de error.
 */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(FechaFueraDeVentanaException.class)
    public ProblemDetail fechaFueraDeVentana(FechaFueraDeVentanaException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problema.setTitle("Fecha no disponible para reservar");
        return problema;
    }
}
