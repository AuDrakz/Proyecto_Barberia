package com.example.barberia_reservas.negocio;

/** Datos públicos para mostrar en la página. whatsapp = solo dígitos con código de país (vacío si no hay). */
public record NegocioResponse(String nombre, String whatsapp) {
}
