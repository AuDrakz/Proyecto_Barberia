package com.example.barberia_reservas.seguridad;

/** Estado de la sesión, para que el frontend sepa si mostrar el login o el panel. */
public record SesionResponse(boolean autenticado, String email) {
}
