package com.example.barberia_reservas.config;

import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Reglas de negocio configurables (se leen de "app.reservas.*" en application.properties).
 * Así, cambiar una regla (por ejemplo, cuántos días de anticipación) NO requiere tocar el código.
 */
@ConfigurationProperties(prefix = "app.reservas")
public record ReservasProperties(
        String zonaHoraria,
        int duracionMinutos,
        int diasMinimosAnticipacion,
        int diasMaximosAnticipacion) {

    // Validamos al arrancar: si la configuración está mal, la app NO parte (falla rápido y con mensaje claro).
    public ReservasProperties {
        if (zonaHoraria == null || zonaHoraria.isBlank()) {
            throw new IllegalArgumentException("Falta app.reservas.zona-horaria");
        }
        ZoneId.of(zonaHoraria); // lanza excepción si la zona horaria no existe
        if (duracionMinutos <= 0) {
            throw new IllegalArgumentException("app.reservas.duracion-minutos debe ser mayor que 0");
        }
        if (diasMinimosAnticipacion < 0 || diasMaximosAnticipacion < diasMinimosAnticipacion) {
            throw new IllegalArgumentException("Revisa app.reservas.dias-minimos/maximos-anticipacion");
        }
    }
}
