package com.example.barberia_reservas.horario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
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
import com.example.barberia_reservas.reserva.EstadoReserva;
import com.example.barberia_reservas.reserva.ReservaRepository;

/** Hoy (fijo) es martes 13-oct-2026. Ventana de reservas: del 14-oct al 12-nov. */
class HorariosServiceResumenTest {

    private static final ZoneId SANTIAGO = ZoneId.of("America/Santiago");

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

    private static List<String> fechas(List<DiaConHorarios> dias) {
        return dias.stream().map(dia -> dia.fecha().toString()).toList();
    }

    @Test
    void listaSoloLosDiasQueTienenHorariosLibres() {
        atiendeLosMartesDe19a22();

        List<DiaConHorarios> dias = servicio.resumenDias();

        assertEquals(List.of("2026-10-20", "2026-10-27", "2026-11-03", "2026-11-10"), fechas(dias));
        assertTrue(dias.stream().allMatch(dia -> dia.libres() == 3));
    }

    @Test
    void unDiaBloqueadoPorCompletoNoAparece() {
        atiendeLosMartesDe19a22();
        LocalDate bloqueado = LocalDate.of(2026, 10, 27);
        when(bloqueos.findByFecha(bloqueado)).thenReturn(List.of(Bloqueo.crear(bloqueado, null, null, null)));

        assertEquals(List.of("2026-10-20", "2026-11-03", "2026-11-10"), fechas(servicio.resumenDias()));
    }

    @Test
    void unaReservaConfirmadaReduceLosHorariosLibresDeEseDia() {
        atiendeLosMartesDe19a22();
        LocalDate dia = LocalDate.of(2026, 10, 20);
        when(reservas.buscarInicios(EstadoReserva.CONFIRMADA,
                dia.atStartOfDay(SANTIAGO).toInstant(), dia.plusDays(1).atStartOfDay(SANTIAGO).toInstant()))
                .thenReturn(List.of(ZonedDateTime.of(dia, LocalTime.of(20, 0), SANTIAGO).toInstant()));

        List<DiaConHorarios> dias = servicio.resumenDias();

        assertEquals(2, dias.get(0).libres());
        assertEquals(3, dias.get(1).libres());
    }

    @Test
    void sinDisponibilidadElResumenEstaVacio() {
        assertTrue(servicio.resumenDias().isEmpty());
    }

    @Test
    void laRespuestaTraeElNombreDelDiaEnEspanol() {
        atiendeLosMartesDe19a22();

        ResumenHorariosResponse respuesta = ResumenHorariosResponse.desde(servicio.resumenDias());

        assertEquals("martes", respuesta.dias().get(0).diaSemana());
        assertEquals("2026-10-20", respuesta.dias().get(0).fecha());
    }
}
