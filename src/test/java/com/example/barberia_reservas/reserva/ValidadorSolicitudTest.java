package com.example.barberia_reservas.reserva;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.example.barberia_reservas.common.SolicitudInvalidaException;

class ValidadorSolicitudTest {

    private static final String INICIO = "2026-10-20T19:00:00-03:00";

    private final ValidadorSolicitud validador = new ValidadorSolicitud();

    @Test
    void datosValidosSeLimpianYSeNormalizan() {
        SolicitudNormalizada s = validador.validar("  Juan   Pérez ", "9 1234 5678", " @Juan.Barbero ", INICIO);

        assertEquals("Juan Pérez", s.nombreCliente());
        assertEquals("+56912345678", s.telefono());
        assertEquals("juan.barbero", s.instagram());
        assertEquals(Instant.parse("2026-10-20T22:00:00Z"), s.inicio()); // 19:00 en UTC-3 son las 22:00 UTC
    }

    @Test
    void instagramEsOpcional() {
        assertNull(validador.validar("Ana", "912345678", null, INICIO).instagram());
        assertNull(validador.validar("Ana", "912345678", "   ", INICIO).instagram());
    }

    @Test
    void rechazaNombresConNumerosOSimbolos() {
        SolicitudInvalidaException e = assertThrows(SolicitudInvalidaException.class,
                () -> validador.validar("<script>alert(1)</script>", "912345678", null, INICIO));
        assertTrue(e.getErrores().containsKey("nombreCliente"));
    }

    @Test
    void rechazaNombreDeUnaSolaLetra() {
        SolicitudInvalidaException e = assertThrows(SolicitudInvalidaException.class,
                () -> validador.validar("A", "912345678", null, INICIO));
        assertTrue(e.getErrores().containsKey("nombreCliente"));
    }

    @Test
    void aceptaNombresConTildesApostrofeYGuion() {
        assertEquals("María José O'Higgins-Soto",
                validador.validar("María José O'Higgins-Soto", "912345678", null, INICIO).nombreCliente());
    }

    @Test
    void rechazaTelefonoInvalido() {
        SolicitudInvalidaException e = assertThrows(SolicitudInvalidaException.class,
                () -> validador.validar("Ana", "abc", null, INICIO));
        assertTrue(e.getErrores().containsKey("telefono"));
    }

    @Test
    void rechazaInstagramInvalido() {
        SolicitudInvalidaException e = assertThrows(SolicitudInvalidaException.class,
                () -> validador.validar("Ana", "912345678", "no valido!", INICIO));
        assertTrue(e.getErrores().containsKey("instagram"));
    }

    @Test
    void rechazaHorarioFaltanteOMalFormado() {
        assertTrue(assertThrows(SolicitudInvalidaException.class,
                () -> validador.validar("Ana", "912345678", null, null)).getErrores().containsKey("inicio"));
        assertTrue(assertThrows(SolicitudInvalidaException.class,
                () -> validador.validar("Ana", "912345678", null, "mañana a las 7")).getErrores().containsKey("inicio"));
    }

    @Test
    void reuneTodosLosErroresDeUnaVez() {
        SolicitudInvalidaException e = assertThrows(SolicitudInvalidaException.class,
                () -> validador.validar("1", "abc", "mal usuario", "nada"));

        assertEquals(4, e.getErrores().size());
    }
}
