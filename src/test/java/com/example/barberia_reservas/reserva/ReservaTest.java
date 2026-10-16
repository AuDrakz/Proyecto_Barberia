package com.example.barberia_reservas.reserva;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.example.barberia_reservas.common.TransicionInvalidaException;

/** Prueba el ciclo de vida de la reserva, sin Spring ni base de datos. */
class ReservaTest {

    private static final Instant AHORA = Instant.parse("2026-10-13T12:00:00Z");
    private static final Instant INICIO = Instant.parse("2026-10-20T22:00:00Z");

    private static Reserva nueva() {
        return Reserva.solicitar("Juan Pérez", "+56912345678", null, INICIO, AHORA);
    }

    @Test
    void unaReservaNuevaNaceSolicitadaYConTokenPropio() {
        Reserva a = nueva();
        Reserva b = nueva();

        assertEquals(EstadoReserva.SOLICITADA, a.getEstado());
        assertNull(a.getCanceladaPor());
        assertNotEquals(a.getTokenCancelacion(), b.getTokenCancelacion());
    }

    @Test
    void unaSolicitudSePuedeConfirmar() {
        Reserva r = nueva();
        r.confirmar(AHORA);
        assertEquals(EstadoReserva.CONFIRMADA, r.getEstado());
    }

    @Test
    void unaSolicitudSePuedeRechazar() {
        Reserva r = nueva();
        r.rechazar(AHORA);
        assertEquals(EstadoReserva.RECHAZADA, r.getEstado());
    }

    @Test
    void elClientePuedeCancelarUnaSolicitudPendiente() {
        Reserva r = nueva();
        r.cancelar(CanceladaPor.CLIENTE, AHORA);

        assertEquals(EstadoReserva.CANCELADA, r.getEstado());
        assertEquals(CanceladaPor.CLIENTE, r.getCanceladaPor());
    }

    @Test
    void unaReservaConfirmadaSePuedeCancelar() {
        Reserva r = nueva();
        r.confirmar(AHORA);
        r.cancelar(CanceladaPor.ADMIN, AHORA);

        assertEquals(EstadoReserva.CANCELADA, r.getEstado());
        assertEquals(CanceladaPor.ADMIN, r.getCanceladaPor());
    }

    @Test
    void unaReservaCanceladaNoSePuedeConfirmarNiCancelarDeNuevo() {
        Reserva r = nueva();
        r.cancelar(CanceladaPor.CLIENTE, AHORA);

        assertThrows(TransicionInvalidaException.class, () -> r.confirmar(AHORA));
        assertThrows(TransicionInvalidaException.class, () -> r.cancelar(CanceladaPor.CLIENTE, AHORA));
    }

    @Test
    void unaReservaRechazadaNoSePuedeCancelar() {
        Reserva r = nueva();
        r.rechazar(AHORA);

        assertThrows(TransicionInvalidaException.class, () -> r.cancelar(CanceladaPor.ADMIN, AHORA));
    }

    @Test
    void unaReservaConfirmadaNoSePuedeConfirmarNiRechazarDeNuevo() {
        Reserva r = nueva();
        r.confirmar(AHORA);

        assertThrows(TransicionInvalidaException.class, () -> r.confirmar(AHORA));
        assertThrows(TransicionInvalidaException.class, () -> r.rechazar(AHORA));
    }
}
