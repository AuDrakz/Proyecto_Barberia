package com.example.barberia_reservas.horario;

/** Un bloqueo recién creado y cuántas solicitudes PENDIENTES quedaron dentro de él (el administrador debe responderlas). */
public record ResultadoBloqueo(Bloqueo bloqueo, int solicitudesPendientesAfectadas) {
}
