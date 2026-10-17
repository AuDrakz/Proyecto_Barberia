package com.example.barberia_reservas.horario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.example.barberia_reservas.common.ConflictoDeNegocioException;
import com.example.barberia_reservas.common.NoEncontradoException;
import com.example.barberia_reservas.common.SolicitudInvalidaException;
import com.example.barberia_reservas.config.ReservasProperties;
import com.example.barberia_reservas.reserva.EstadoReserva;
import com.example.barberia_reservas.reserva.Reserva;
import com.example.barberia_reservas.reserva.ReservaRepository;

class BloqueoAdminServiceTest {

    private static final ZoneId SANTIAGO = ZoneId.of("America/Santiago");
    private static final Instant AHORA = Instant.parse("2026-10-13T12:00:00Z");
    private static final LocalDate DIA = LocalDate.of(2026, 10, 22);

    private final BloqueoRepository bloqueos = mock(BloqueoRepository.class);
    private final ReservaRepository reservas = mock(ReservaRepository.class);
    private final BloqueoAdminService servicio = new BloqueoAdminService(
            bloqueos, reservas,
            new ReservasProperties("America/Santiago", 60, 1, 30, 2),
            Clock.fixed(AHORA, ZoneOffset.UTC));

    /** Una reserva el DIA a la hora local dada (por ejemplo 20 = 20:00 hora de Santiago). */
    private static Reserva reservaALas(int hora, String nombre) {
        Instant inicio = ZonedDateTime.of(DIA, LocalTime.of(hora, 0), SANTIAGO).toInstant();
        return Reserva.solicitar(nombre, "+56912345678", null, inicio, AHORA);
    }

    private void hayReservas(EstadoReserva estado, Reserva... lista) {
        Instant desde = DIA.atStartOfDay(SANTIAGO).toInstant();
        Instant hasta = DIA.plusDays(1).atStartOfDay(SANTIAGO).toInstant();
        when(reservas.findByEstadoAndInicioGreaterThanEqualAndInicioLessThan(estado, desde, hasta))
                .thenReturn(List.of(lista));
    }

    @Test
    void bloqueaUnDiaSinReservasConfirmadas() {
        when(bloqueos.save(any(Bloqueo.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        ResultadoBloqueo resultado = servicio.crear("2026-10-22", null, null, "Cumpleaños");

        assertTrue(resultado.bloqueo().esDiaCompleto());
        assertEquals(0, resultado.solicitudesPendientesAfectadas());
    }

    @Test
    void rechazaBloquearUnDiaConUnaReservaConfirmada() {
        hayReservas(EstadoReserva.CONFIRMADA, reservaALas(20, "Juan Pérez"));

        ConflictoDeNegocioException e = assertThrows(ConflictoDeNegocioException.class,
                () -> servicio.crear("2026-10-22", null, null, null));

        assertTrue(e.getMessage().contains("Juan Pérez"));
        assertTrue(e.getMessage().contains("20:00"));
        verify(bloqueos, never()).save(any(Bloqueo.class));
    }

    @Test
    void rechazaUnBloqueoParcialQueSolapaUnaConfirmada() {
        // Bloqueo 20:30-21:30 roza la cita de las 20:00 (que dura hasta las 21:00)
        hayReservas(EstadoReserva.CONFIRMADA, reservaALas(20, "Juan Pérez"));

        assertThrows(ConflictoDeNegocioException.class, () -> servicio.crear("2026-10-22", "20:30", "21:30", null));
    }

    @Test
    void permiteUnBloqueoParcialQueNoTocaLasConfirmadas() {
        hayReservas(EstadoReserva.CONFIRMADA, reservaALas(19, "Juan Pérez"));
        when(bloqueos.save(any(Bloqueo.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // La cita de las 19:00 termina a las 20:00, justo cuando empieza el bloqueo: no se pisan.
        ResultadoBloqueo resultado = servicio.crear("2026-10-22", "20:00", "21:00", null);

        assertEquals(LocalTime.of(20, 0), resultado.bloqueo().getHoraInicio());
    }

    @Test
    void avisaCuantasSolicitudesPendientesQuedanDentroDelBloqueo() {
        hayReservas(EstadoReserva.SOLICITADA, reservaALas(19, "Ana"), reservaALas(20, "Luis"), reservaALas(21, "Eva"));
        when(bloqueos.save(any(Bloqueo.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // El bloqueo 20:00-22:00 cubre las solicitudes de las 20:00 y las 21:00, pero no la de las 19:00.
        ResultadoBloqueo resultado = servicio.crear("2026-10-22", "20:00", "22:00", null);

        assertEquals(2, resultado.solicitudesPendientesAfectadas());
    }

    @Test
    void rechazaBloquearUnDiaQueYaEstaBloqueadoPorCompleto() {
        Bloqueo existente = Bloqueo.crear(DIA, null, null, null);
        when(bloqueos.findByFecha(DIA)).thenReturn(List.of(existente));

        assertThrows(ConflictoDeNegocioException.class, () -> servicio.crear("2026-10-22", "20:00", "21:00", null));
        verify(bloqueos, never()).save(any(Bloqueo.class));
    }

    @Test
    void rechazaFechasPasadasSinTocarLaBaseDeDatos() {
        assertThrows(SolicitudInvalidaException.class, () -> servicio.crear("2026-10-12", null, null, null));
        verify(bloqueos, never()).save(any(Bloqueo.class));
    }

    @Test
    void eliminaUnBloqueoExistente() {
        Bloqueo existente = Bloqueo.crear(DIA, null, null, null);
        when(bloqueos.findById(1L)).thenReturn(Optional.of(existente));

        servicio.eliminar(1L);

        verify(bloqueos).delete(existente);
    }

    @Test
    void eliminarUnBloqueoInexistenteDaNoEncontrado() {
        when(bloqueos.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class, () -> servicio.eliminar(99L));
    }

    @Test
    void listaLosBloqueosDeHoyEnAdelante() {
        Bloqueo uno = Bloqueo.crear(DIA, null, null, null);
        when(bloqueos.findByFechaGreaterThanEqualOrderByFechaAscHoraInicioAsc(LocalDate.of(2026, 10, 13)))
                .thenReturn(List.of(uno));

        assertEquals(List.of(uno), servicio.listar());
    }
}
