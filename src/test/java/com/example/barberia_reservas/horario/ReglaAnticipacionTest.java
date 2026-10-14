package com.example.barberia_reservas.horario;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.example.barberia_reservas.common.FechaFueraDeVentanaException;

/** Ventana: desde mañana (1 día) hasta 30 días hacia adelante. "Hoy" se fija a mano, sin depender del reloj real. */
class ReglaAnticipacionTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 13);
    private final ReglaAnticipacion regla = new ReglaAnticipacion(1, 30);

    @Test
    void noSePuedeReservarElMismoDia() {
        assertThrows(FechaFueraDeVentanaException.class, () -> regla.validar(HOY, HOY));
    }

    @Test
    void noSePuedeReservarUnaFechaPasada() {
        assertThrows(FechaFueraDeVentanaException.class, () -> regla.validar(HOY.minusDays(1), HOY));
    }

    @Test
    void sePuedeReservarDesdeManana() {
        assertDoesNotThrow(() -> regla.validar(HOY.plusDays(1), HOY));
    }

    @Test
    void sePuedeReservarHastaElUltimoDiaDeLaVentana() {
        assertDoesNotThrow(() -> regla.validar(HOY.plusDays(30), HOY));
    }

    @Test
    void noSePuedeReservarMasAllaDeLaVentana() {
        assertThrows(FechaFueraDeVentanaException.class, () -> regla.validar(HOY.plusDays(31), HOY));
    }
}
