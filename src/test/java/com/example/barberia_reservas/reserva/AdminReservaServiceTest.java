package com.example.barberia_reservas.reserva;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.example.barberia_reservas.common.ConflictoDeNegocioException;
import com.example.barberia_reservas.common.ReservaNoEncontradaException;

/** Reglas del panel del administrador, con el repositorio simulado (sin base de datos ni Spring). */
class AdminReservaServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-13T12:00:00Z");
    private static final Instant INICIO = Instant.parse("2026-10-20T22:00:00Z");

    private final ReservaRepository repositorio = mock(ReservaRepository.class);
    private final AdminReservaService servicio =
            new AdminReservaService(repositorio, Clock.fixed(AHORA, ZoneOffset.UTC));

    private static Reserva solicitud(Instant inicio) {
        return Reserva.solicitar("Juan Pérez", "+56912345678", null, inicio, AHORA.minusSeconds(60));
    }

    @Test
    void confirmaUnaSolicitudYRechazaLasOtrasDelMismoHorario() {
        Reserva elegida = solicitud(INICIO);
        Reserva otra = solicitud(INICIO);
        when(repositorio.findById(1L)).thenReturn(Optional.of(elegida));
        when(repositorio.findByInicioAndEstado(INICIO, EstadoReserva.SOLICITADA)).thenReturn(List.of(otra));

        Reserva resultado = servicio.confirmar(1L);

        assertEquals(EstadoReserva.CONFIRMADA, resultado.getEstado());
        assertEquals(EstadoReserva.RECHAZADA, otra.getEstado());
        verify(repositorio).saveAndFlush(elegida);
    }

    @Test
    void noConfirmaSiYaHayUnaReservaConfirmadaEnEseHorario() {
        Reserva reserva = solicitud(INICIO);
        when(repositorio.findById(1L)).thenReturn(Optional.of(reserva));
        when(repositorio.existsByInicioAndEstado(INICIO, EstadoReserva.CONFIRMADA)).thenReturn(true);

        assertThrows(ConflictoDeNegocioException.class, () -> servicio.confirmar(1L));
        assertEquals(EstadoReserva.SOLICITADA, reserva.getEstado());
        verify(repositorio, never()).saveAndFlush(any(Reserva.class));
    }

    @Test
    void elChoqueQueDetectaLaBaseDeDatosSeTraduceAUnConflicto() {
        // Simula la "red de seguridad": la comprobación previa no vio nada, pero el índice único sí protestó.
        Reserva reserva = solicitud(INICIO);
        when(repositorio.findById(1L)).thenReturn(Optional.of(reserva));
        when(repositorio.saveAndFlush(any(Reserva.class))).thenThrow(new DataIntegrityViolationException("duplicado"));

        assertThrows(ConflictoDeNegocioException.class, () -> servicio.confirmar(1L));
    }

    @Test
    void noConfirmaUnaHoraQueYaPaso() {
        Reserva reserva = solicitud(AHORA.minusSeconds(3600));
        when(repositorio.findById(1L)).thenReturn(Optional.of(reserva));

        assertThrows(ConflictoDeNegocioException.class, () -> servicio.confirmar(1L));
        verify(repositorio, never()).saveAndFlush(any(Reserva.class));
    }

    @Test
    void noConfirmaUnaReservaQueYaNoEstaSolicitada() {
        Reserva reserva = solicitud(INICIO);
        reserva.rechazar(AHORA);
        when(repositorio.findById(1L)).thenReturn(Optional.of(reserva));

        assertThrows(ConflictoDeNegocioException.class, () -> servicio.confirmar(1L));
        verify(repositorio, never()).saveAndFlush(any(Reserva.class));
    }

    @Test
    void confirmarUnaReservaInexistenteDaNoEncontrada() {
        when(repositorio.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ReservaNoEncontradaException.class, () -> servicio.confirmar(99L));
    }

    @Test
    void rechazaUnaSolicitud() {
        Reserva reserva = solicitud(INICIO);
        when(repositorio.findById(1L)).thenReturn(Optional.of(reserva));

        assertEquals(EstadoReserva.RECHAZADA, servicio.rechazar(1L).getEstado());
    }

    @Test
    void elAdministradorPuedeCancelarUnaReservaConfirmada() {
        Reserva reserva = solicitud(INICIO);
        reserva.confirmar(AHORA);
        when(repositorio.findById(1L)).thenReturn(Optional.of(reserva));

        Reserva cancelada = servicio.cancelar(1L);

        assertEquals(EstadoReserva.CANCELADA, cancelada.getEstado());
        assertEquals(CanceladaPor.ADMIN, cancelada.getCanceladaPor());
    }

    @Test
    void listaSoloLasReservasFuturasDelEstadoPedido() {
        Reserva reserva = solicitud(INICIO);
        when(repositorio.findByEstadoAndInicioGreaterThanEqualOrderByInicioAsc(EstadoReserva.SOLICITADA, AHORA))
                .thenReturn(List.of(reserva));

        assertEquals(List.of(reserva), servicio.listar(EstadoReserva.SOLICITADA));
    }
}
