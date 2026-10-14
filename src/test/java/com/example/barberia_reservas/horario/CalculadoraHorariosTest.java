package com.example.barberia_reservas.horario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Pruebas unitarias puras: sin Spring, sin base de datos, corren en milisegundos. */
class CalculadoraHorariosTest {

    private static final ZoneId SANTIAGO = ZoneId.of("America/Santiago");
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 20);

    private final CalculadoraHorarios calculadora = new CalculadoraHorarios(SANTIAGO, 60);

    private static RangoHorario rango(int horaIni, int minIni, int horaFin, int minFin) {
        return new RangoHorario(LocalTime.of(horaIni, minIni), LocalTime.of(horaFin, minFin));
    }

    private static List<LocalTime> horas(List<Horario> horarios) {
        return horarios.stream().map(Horario::hora).toList();
    }

    @Test
    void ofreceUnHorarioPorCadaHoraEntera() {
        var resultado = calculadora.calcular(FECHA, List.of(rango(19, 0, 22, 0)), List.of(), Set.of());

        assertEquals(List.of(LocalTime.of(19, 0), LocalTime.of(20, 0), LocalTime.of(21, 0)), horas(resultado));
    }

    @Test
    void noOfreceUnHorarioQueNoCabeCompletoEnElTramo() {
        // 19:00 a 21:30 => caben 19:00 y 20:00; el de las 21:00 terminaría a las 22:00, fuera del tramo.
        var resultado = calculadora.calcular(FECHA, List.of(rango(19, 0, 21, 30)), List.of(), Set.of());

        assertEquals(List.of(LocalTime.of(19, 0), LocalTime.of(20, 0)), horas(resultado));
    }

    @Test
    void unBloqueoQuitaElHorarioQueCubre() {
        var resultado = calculadora.calcular(FECHA,
                List.of(rango(19, 0, 22, 0)), List.of(rango(20, 0, 21, 0)), Set.of());

        assertEquals(List.of(LocalTime.of(19, 0), LocalTime.of(21, 0)), horas(resultado));
    }

    @Test
    void unBloqueoParcialQuitaTodosLosHorariosQueSolapa() {
        // 20:30 a 21:30 roza el horario de las 20:00 (hasta 21:00) y el de las 21:00 (desde 21:00).
        var resultado = calculadora.calcular(FECHA,
                List.of(rango(19, 0, 22, 0)), List.of(rango(20, 30, 21, 30)), Set.of());

        assertEquals(List.of(LocalTime.of(19, 0)), horas(resultado));
    }

    @Test
    void unBloqueoQueSoloTocaElBordeNoQuitaNada() {
        // El bloqueo termina a las 20:00 justo cuando empieza ese horario: no se solapan.
        var resultado = calculadora.calcular(FECHA,
                List.of(rango(19, 0, 21, 0)), List.of(rango(18, 0, 20, 0)), Set.of());

        assertEquals(List.of(LocalTime.of(20, 0)), horas(resultado));
    }

    @Test
    void unaReservaConfirmadaQuitaSuHorario() {
        Instant reservada = ZonedDateTime.of(FECHA, LocalTime.of(20, 0), SANTIAGO).toInstant();

        var resultado = calculadora.calcular(FECHA, List.of(rango(19, 0, 22, 0)), List.of(), Set.of(reservada));

        assertEquals(List.of(LocalTime.of(19, 0), LocalTime.of(21, 0)), horas(resultado));
    }

    @Test
    void sinDisponibilidadNoHayHorarios() {
        var resultado = calculadora.calcular(FECHA, List.of(), List.of(), Set.of());

        assertTrue(resultado.isEmpty());
    }

    @Test
    void tramosSuperpuestosNoDuplicanHorarios() {
        // 19-21 y 20-22 se superponen en las 20:00: ese horario debe aparecer UNA sola vez.
        var resultado = calculadora.calcular(FECHA,
                List.of(rango(19, 0, 21, 0), rango(20, 0, 22, 0)), List.of(), Set.of());

        assertEquals(List.of(LocalTime.of(19, 0), LocalTime.of(20, 0), LocalTime.of(21, 0)), horas(resultado));
    }
}
