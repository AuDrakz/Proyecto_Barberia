package com.example.barberia_reservas.horario;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import org.junit.jupiter.api.Test;

import com.example.barberia_reservas.config.ReservasProperties;
import com.example.barberia_reservas.reserva.ReservaRepository;

/** ¿Una hora sigue dentro de la agenda? Hoy (fijo) es martes 13-oct-2026; el barbero atiende los martes de 19 a 22. */
class HorariosServiceAgendaTest {

    private static final ZoneId SANTIAGO = ZoneId.of("America/Santiago");
    private static final LocalDate MARTES = LocalDate.of(2026, 10, 20);

    private final DisponibilidadRepository disponibilidades = mock(DisponibilidadRepository.class);
    private final BloqueoRepository bloqueos = mock(BloqueoRepository.class);
    private final ReservaRepository reservas = mock(ReservaRepository.class);
    private final HorariosService servicio = new HorariosService(
            disponibilidades, bloqueos, reservas,
            new ReservasProperties("America/Santiago", 60, 1, 30, 2),
            Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC));

    private void atiendeLosMartesDe19a22() {
        when(disponibilidades.findByDiaSemana((short) 2))
                .thenReturn(List.of(Disponibilidad.crear((short) 2, LocalTime.of(19, 0), LocalTime.of(22, 0))));
    }

    private static Instant a(LocalDate fecha, int hora) {
        return ZonedDateTime.of(fecha, LocalTime.of(hora, 0), SANTIAGO).toInstant();
    }

    @Test
    void unHorarioDentroDeLaDisponibilidadEstaEnLaAgenda() {
        atiendeLosMartesDe19a22();

        assertTrue(servicio.estaEnLaAgenda(a(MARTES, 20)));
    }

    @Test
    void unDiaBloqueadoPorCompletoNoEstaEnLaAgenda() {
        atiendeLosMartesDe19a22();
        when(bloqueos.findByFecha(MARTES)).thenReturn(List.of(Bloqueo.crear(MARTES, null, null, null)));

        assertFalse(servicio.estaEnLaAgenda(a(MARTES, 20)));
    }

    @Test
    void unBloqueoParcialSacaDeLaAgendaSoloLasHorasQueCubre() {
        atiendeLosMartesDe19a22();
        when(bloqueos.findByFecha(MARTES))
                .thenReturn(List.of(Bloqueo.crear(MARTES, LocalTime.of(20, 0), LocalTime.of(21, 0), "Trámite")));

        assertFalse(servicio.estaEnLaAgenda(a(MARTES, 20)));
        assertTrue(servicio.estaEnLaAgenda(a(MARTES, 19)));
    }

    @Test
    void unaHoraFueraDelTramoNoEstaEnLaAgenda() {
        atiendeLosMartesDe19a22();

        assertFalse(servicio.estaEnLaAgenda(a(MARTES, 12)));
    }

    @Test
    void unDiaSinDisponibilidadNoEstaEnLaAgenda() {
        atiendeLosMartesDe19a22();

        assertFalse(servicio.estaEnLaAgenda(a(LocalDate.of(2026, 10, 21), 20)));
    }

    @Test
    void noMiraLasReservasConfirmadas() {
        atiendeLosMartesDe19a22();

        assertTrue(servicio.estaEnLaAgenda(a(MARTES, 20)));
        verify(reservas, never()).buscarInicios(any(), any(), any());
    }

    @Test
    void noExigeLaAnticipacionMinima() {
        atiendeLosMartesDe19a22();

        // Hoy es martes 13: horariosLibres rechazaría el mismo día, pero para confirmar basta con que esté en la agenda.
        assertTrue(servicio.estaEnLaAgenda(a(LocalDate.of(2026, 10, 13), 20)));
    }
}