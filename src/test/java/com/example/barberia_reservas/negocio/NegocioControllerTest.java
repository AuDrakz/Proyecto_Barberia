package com.example.barberia_reservas.negocio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class NegocioControllerTest {

    @Test
    void sinWhatsappElCampoQuedaVacio() {
        NegocioResponse datos = new NegocioController("Barbería Don Pepe", "").obtener();

        assertEquals("Barbería Don Pepe", datos.nombre());
        assertEquals("", datos.whatsapp());
    }

    @Test
    void elWhatsappSeNormalizaYSeEntregaSoloConDigitos() {
        assertEquals("56912345678", new NegocioController("Mi Barbería", "+56 9 1234 5678").obtener().whatsapp());
        assertEquals("56912345678", new NegocioController("Mi Barbería", "912345678").obtener().whatsapp());
    }

    @Test
    void fallaAlArrancarSiElWhatsappNoEsValido() {
        assertThrows(IllegalStateException.class, () -> new NegocioController("Mi Barbería", "no-es-un-telefono"));
    }

    @Test
    void fallaAlArrancarSiElNombreEstaVacioOEsMuyLargo() {
        assertThrows(IllegalStateException.class, () -> new NegocioController("   ", ""));
        assertThrows(IllegalStateException.class, () -> new NegocioController("x".repeat(61), ""));
    }

    @Test
    void elNombreSeLimpiaDeEspacios() {
        assertEquals("Mi Barbería", new NegocioController("  Mi Barbería ", "").obtener().nombre());
    }
}
