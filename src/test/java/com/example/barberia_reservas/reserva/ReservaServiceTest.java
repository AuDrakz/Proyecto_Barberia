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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.example.barberia_reservas.common.ConflictoDeNegocioException;
import com.example.barberia_reservas.common.ReservaNoEncontradaException;
import com.example.barberia_reservas.config.ReservasProperties;
import com.example.barberia_reservas.horario.Horario;
import com.example.barberia_reservas.horario.HorariosService;

/**
 * Prueba las REGLAS del servicio con dependencias "de mentira" (mocks de Mockito):
 * sin base de datos y sin Spring. El repositorio y el servicio de horarios se simulan.
 */
class ReservaServiceTest {

    private static final ZoneId SANTIAGO = ZoneId.of("America/Santiago");
    private static final Instant AHORA = Instant.parse("2026-10-13T12:00:00Z");
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 20);
    private static final String INICIO_TEXTO = "2026-10-20T19:00:00-03:00";
    private static final Instant INICIO = Instant.parse("2026-10-20T22:00:00Z");
    private static final String TELEFONO = "+56912345678";

    private final ReservaRepository repositorio = mock(ReservaRepository.class);
    private final HorariosService horarios = mock(HorariosService.class);
    private final ReservaService servicio = new ReservaService(
            repositorio,
            horarios,
            new ReservasProperties("America/Santiago", 60, 1, 30, 2),
            Clock.fixed(AHORA, ZoneOffset.UTC));

    private void haceFaltaQueLas19EstenLibres() {
        when(horarios.horariosLibres(FECHA)).thenReturn(
                List.of(new Horario(LocalTime.of(19, 0), ZonedDateTime.ofInstant(INICIO, SANTIAGO))));
        when(repositorio.save(any(Reserva.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void solicitaUnHorarioLibreYQuedaPendiente() {
        haceFaltaQueLas19EstenLibres();

        Reserva reserva = servicio.solicitar("Juan Pérez", "9 1234 5678", "@juan", INICIO_TEXTO);

        assertEquals(EstadoReserva.SOLICITADA, reserva.getEstado());
        assertEquals(TELEFONO, reserva.getTelefono());
        verify(repositorio).save(any(Reserva.class));
    }

    @Test
    void rechazaUnHorarioQueNoEstaEnLaLista() {
        when(horarios.horariosLibres(FECHA)).thenReturn(List.of());

        assertThrows(ConflictoDeNegocioException.class,
                () -> servicio.solicitar("Juan Pérez", "912345678", null, INICIO_TEXTO));
        verify(repositorio, never()).save(any(Reserva.class));
    }

    @Test
    void rechazaUnaSolicitudRepetidaParaLaMismaHora() {
        haceFaltaQueLas19EstenLibres();
        when(repositorio.existsByTelefonoAndInicioAndEstado(TELEFONO, INICIO, EstadoReserva.SOLICITADA)).thenReturn(true);

        assertThrows(ConflictoDeNegocioException.class,
                () -> servicio.solicitar("Juan Pérez", "912345678", null, INICIO_TEXTO));
        verify(repositorio, never()).save(any(Reserva.class));
    }

    @Test
    void rechazaCuandoElTelefonoYaTieneElMaximoDeSolicitudesPendientes() {
        haceFaltaQueLas19EstenLibres();
        when(repositorio.countByTelefonoAndEstado(TELEFONO, EstadoReserva.SOLICITADA)).thenReturn(2L);

        assertThrows(ConflictoDeNegocioException.class,
                () -> servicio.solicitar("Juan Pérez", "912345678", null, INICIO_TEXTO));
        verify(repositorio, never()).save(any(Reserva.class));
    }

    @Test
    void permiteUnaSegundaSolicitudPendiente() {
        haceFaltaQueLas19EstenLibres();
        when(repositorio.countByTelefonoAndEstado(TELEFONO, EstadoReserva.SOLICITADA)).thenReturn(1L);

        Reserva reserva = servicio.solicitar("Juan Pérez", "912345678", null, INICIO_TEXTO);

        assertEquals(EstadoReserva.SOLICITADA, reserva.getEstado());
    }

    @Test
    void cancelarConUnTokenQueNoExisteLanzaNoEncontrada() {
        when(repositorio.findByTokenCancelacion(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ReservaNoEncontradaException.class, () -> servicio.cancelarPorCliente(UUID.randomUUID()));
    }

    @Test
    void elClienteCancelaUnaSolicitudPendiente() {
        Reserva reserva = Reserva.solicitar("Juan Pérez", TELEFONO, null, INICIO, AHORA);
        when(repositorio.findByTokenCancelacion(reserva.getTokenCancelacion())).thenReturn(Optional.of(reserva));

        Reserva cancelada = servicio.cancelarPorCliente(reserva.getTokenCancelacion());

        assertEquals(EstadoReserva.CANCELADA, cancelada.getEstado());
        assertEquals(CanceladaPor.CLIENTE, cancelada.getCanceladaPor());
    }

    @Test
    void noSePuedeCancelarUnaHoraQueYaPaso() {
        Instant yaPaso = AHORA.minusSeconds(3600);
        Reserva reserva = Reserva.solicitar("Juan Pérez", TELEFONO, null, yaPaso, AHORA.minusSeconds(7200));
        when(repositorio.findByTokenCancelacion(reserva.getTokenCancelacion())).thenReturn(Optional.of(reserva));

        assertThrows(ConflictoDeNegocioException.class, () -> servicio.cancelarPorCliente(reserva.getTokenCancelacion()));
        assertEquals(EstadoReserva.SOLICITADA, reserva.getEstado());
    }

    @Test
    void noSePuedeCancelarDosVeces() {
        Reserva reserva = Reserva.solicitar("Juan Pérez", TELEFONO, null, INICIO, AHORA);
        when(repositorio.findByTokenCancelacion(reserva.getTokenCancelacion())).thenReturn(Optional.of(reserva));
        servicio.cancelarPorCliente(reserva.getTokenCancelacion());

        assertThrows(ConflictoDeNegocioException.class, () -> servicio.cancelarPorCliente(reserva.getTokenCancelacion()));
    }
}
