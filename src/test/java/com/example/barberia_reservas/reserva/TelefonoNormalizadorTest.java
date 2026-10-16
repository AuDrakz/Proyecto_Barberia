package com.example.barberia_reservas.reserva;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class TelefonoNormalizadorTest {

    @Test
    void celularChilenoSinPrefijoSeCompletaConElCodigoDePais() {
        assertEquals(Optional.of("+56912345678"), TelefonoNormalizador.normalizar("912345678"));
    }

    @Test
    void ignoraEspaciosYGuiones() {
        assertEquals(Optional.of("+56912345678"), TelefonoNormalizador.normalizar("9 1234-5678"));
    }

    @Test
    void aceptaElPrefijoDePais() {
        assertEquals(Optional.of("+56912345678"), TelefonoNormalizador.normalizar("+56 9 1234 5678"));
    }

    @Test
    void aceptaElCodigoDePaisEscritoSinMas() {
        assertEquals(Optional.of("+56912345678"), TelefonoNormalizador.normalizar("56912345678"));
    }

    @Test
    void elMismoNumeroEscritoDeFormasDistintasDaElMismoResultado() {
        // Esto es lo que hace que el límite de solicitudes por teléfono no se pueda esquivar cambiando el formato.
        var a = TelefonoNormalizador.normalizar("912345678");
        var b = TelefonoNormalizador.normalizar("+56 9 1234 5678");
        var c = TelefonoNormalizador.normalizar("(9) 1234-5678");
        assertTrue(a.isPresent());
        assertEquals(a, b);
        assertEquals(a, c);
    }

    @Test
    void aceptaNumerosInternacionales() {
        assertEquals(Optional.of("+14155552671"), TelefonoNormalizador.normalizar("+1 415 555 2671"));
    }

    @Test
    void rechazaTextoConLetras() {
        assertTrue(TelefonoNormalizador.normalizar("abc").isEmpty());
        assertTrue(TelefonoNormalizador.normalizar("9123x5678").isEmpty());
    }

    @Test
    void rechazaNumerosMuyCortosOAmbiguos() {
        assertTrue(TelefonoNormalizador.normalizar("12345").isEmpty());
        assertTrue(TelefonoNormalizador.normalizar("22345678").isEmpty()); // 8 dígitos sin código de país: ambiguo
    }

    @Test
    void rechazaNuloVacioYSoloSimbolos() {
        assertTrue(TelefonoNormalizador.normalizar(null).isEmpty());
        assertTrue(TelefonoNormalizador.normalizar("   ").isEmpty());
        assertTrue(TelefonoNormalizador.normalizar("+").isEmpty());
        assertTrue(TelefonoNormalizador.normalizar("()").isEmpty());
    }

    @Test
    void rechazaCodigoDePaisQueEmpiezaConCero() {
        assertTrue(TelefonoNormalizador.normalizar("+0123456789").isEmpty());
    }
}
