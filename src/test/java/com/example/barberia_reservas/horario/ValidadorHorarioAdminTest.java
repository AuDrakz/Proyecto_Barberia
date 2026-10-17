package com.example.barberia_reservas.horario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.example.barberia_reservas.common.SolicitudInvalidaException;

class ValidadorHorarioAdminTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 13);

    private final ValidadorHorarioAdmin validador = new ValidadorHorarioAdmin(60);

    // ---------- disponibilidad ----------

    @Test
    void aceptaUnTramoValido() {
        NuevaDisponibilidad tramo = validador.validarDisponibilidad(2, "19:00", " 22:00 ");

        assertEquals((short) 2, tramo.diaSemana());
        assertEquals(LocalTime.of(19, 0), tramo.horaInicio());
        assertEquals(LocalTime.of(22, 0), tramo.horaFin());
    }

    @Test
    void rechazaUnDiaFueraDeRango() {
        assertTrue(assertThrows(SolicitudInvalidaException.class,
                () -> validador.validarDisponibilidad(0, "19:00", "22:00")).getErrores().containsKey("diaSemana"));
        assertTrue(assertThrows(SolicitudInvalidaException.class,
                () -> validador.validarDisponibilidad(8, "19:00", "22:00")).getErrores().containsKey("diaSemana"));
        assertTrue(assertThrows(SolicitudInvalidaException.class,
                () -> validador.validarDisponibilidad(null, "19:00", "22:00")).getErrores().containsKey("diaSemana"));
    }

    @Test
    void rechazaHorasConFormatoInvalido() {
        SolicitudInvalidaException e = assertThrows(SolicitudInvalidaException.class,
                () -> validador.validarDisponibilidad(2, "25:00", "7pm"));

        assertTrue(e.getErrores().containsKey("horaInicio"));
        assertTrue(e.getErrores().containsKey("horaFin"));
    }

    @Test
    void rechazaHorasConSegundos() {
        assertTrue(assertThrows(SolicitudInvalidaException.class,
                () -> validador.validarDisponibilidad(2, "19:00:30", "22:00")).getErrores().containsKey("horaInicio"));
    }

    @Test
    void rechazaUnFinAnteriorOIgualAlInicio() {
        assertTrue(assertThrows(SolicitudInvalidaException.class,
                () -> validador.validarDisponibilidad(2, "22:00", "19:00")).getErrores().containsKey("horaFin"));
        assertTrue(assertThrows(SolicitudInvalidaException.class,
                () -> validador.validarDisponibilidad(2, "19:00", "19:00")).getErrores().containsKey("horaFin"));
    }

    @Test
    void rechazaUnTramoMasCortoQueUnaCita() {
        SolicitudInvalidaException e = assertThrows(SolicitudInvalidaException.class,
                () -> validador.validarDisponibilidad(2, "19:00", "19:30"));

        assertTrue(e.getErrores().get("horaFin").contains("60"));
    }

    @Test
    void aceptaUnTramoDeExactamenteUnaCita() {
        assertEquals(LocalTime.of(20, 0), validador.validarDisponibilidad(2, "19:00", "20:00").horaFin());
    }

    // ---------- bloqueos ----------

    @Test
    void bloqueoDeDiaCompletoNoLlevaHoras() {
        NuevoBloqueo bloqueo = validador.validarBloqueo("2026-10-22", null, "  ", " Cumpleaños ", HOY);

        assertEquals(LocalDate.of(2026, 10, 22), bloqueo.fecha());
        assertTrue(bloqueo.esDiaCompleto());
        assertEquals("Cumpleaños", bloqueo.motivo());
    }

    @Test
    void bloqueoParcialLlevaInicioYFin() {
        NuevoBloqueo bloqueo = validador.validarBloqueo("2026-10-22", "20:00", "21:00", null, HOY);

        assertEquals(LocalTime.of(20, 0), bloqueo.horaInicio());
        assertEquals(LocalTime.of(21, 0), bloqueo.horaFin());
        assertNull(bloqueo.motivo());
    }

    @Test
    void rechazaUnBloqueoConSoloUnaDeLasDosHoras() {
        assertTrue(assertThrows(SolicitudInvalidaException.class,
                () -> validador.validarBloqueo("2026-10-22", "20:00", null, null, HOY)).getErrores().containsKey("horaFin"));
        assertTrue(assertThrows(SolicitudInvalidaException.class,
                () -> validador.validarBloqueo("2026-10-22", null, "21:00", null, HOY)).getErrores().containsKey("horaInicio"));
    }

    @Test
    void rechazaFechasPasadasOMalFormadas() {
        assertTrue(assertThrows(SolicitudInvalidaException.class,
                () -> validador.validarBloqueo("2026-10-12", null, null, null, HOY)).getErrores().containsKey("fecha"));
        assertTrue(assertThrows(SolicitudInvalidaException.class,
                () -> validador.validarBloqueo("22-10-2026", null, null, null, HOY)).getErrores().containsKey("fecha"));
        assertTrue(assertThrows(SolicitudInvalidaException.class,
                () -> validador.validarBloqueo(null, null, null, null, HOY)).getErrores().containsKey("fecha"));
    }

    @Test
    void sePuedeBloquearHoy() {
        assertTrue(validador.validarBloqueo("2026-10-13", null, null, null, HOY).esDiaCompleto());
    }

    @Test
    void rechazaUnMotivoDemasiadoLargo() {
        assertTrue(assertThrows(SolicitudInvalidaException.class,
                () -> validador.validarBloqueo("2026-10-22", null, null, "x".repeat(201), HOY))
                .getErrores().containsKey("motivo"));
    }
}
