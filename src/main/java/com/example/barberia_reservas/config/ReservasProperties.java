package com.example.barberia_reservas.config;

import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Reglas de negocio configurables (se leen de "app.reservas.*" en application.properties).
 * Cambiar una regla NO requiere tocar el código.
 */
@ConfigurationProperties(prefix = "app.reservas")
public record ReservasProperties(
        String zonaHoraria,
        int duracionMinutos,
        int diasMinimosAnticipacion,
        int diasMaximosAnticipacion,
        int maxSolicitudesPendientesPorTelefono) {

    // Se valida al arrancar: si la configuración está mal, la app NO parte (falla rápido y con mensaje claro).
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
        if (maxSolicitudesPendientesPorTelefono < 1) {
            throw new IllegalArgumentException("app.reservas.max-solicitudes-pendientes-por-telefono debe ser al menos 1");
        }
    }
}
