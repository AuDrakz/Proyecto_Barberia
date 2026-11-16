package com.example.barberia_reservas.horario;

import java.util.List;

/**
 * Forma de la respuesta JSON. Es un DTO (objeto de transferencia de datos): lo que la API EXPONE,
 * separado de las entidades de la base de datos. Así puedes cambiar una tabla sin romper el frontend.
 */
public record HorariosResponse(String fecha, List<Item> horarios) {

    /** hora = "19:00" (para mostrar); inicio = instante exacto con zona horaria (para enviar al reservar). */
    public record Item(String hora, String inicio) {
    }
}
