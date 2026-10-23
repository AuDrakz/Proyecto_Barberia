package com.example.barberia_reservas.negocio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.barberia_reservas.reserva.TelefonoNormalizador;

/**
 * GET /api/negocio: el nombre de la barbería y su WhatsApp (opcional) para la página del cliente.
 * Se configuran en application.properties (app.negocio.*), no en el código. Se validan al arrancar:
 * si el teléfono está mal escrito, la app no parte (en vez de mostrar un botón de WhatsApp roto).
 */
@RestController
@RequestMapping("/api/negocio")
public class NegocioController {

    private final NegocioResponse datos;

    public NegocioController(@Value("${app.negocio.nombre:Barbería}") String nombre,
                             @Value("${app.negocio.whatsapp:}") String whatsapp) {
        String nombreLimpio = nombre == null ? "" : nombre.trim();
        if (nombreLimpio.isEmpty() || nombreLimpio.length() > 60) {
            throw new IllegalStateException("app.negocio.nombre debe tener entre 1 y 60 caracteres.");
        }

        String soloDigitos = "";
        if (whatsapp != null && !whatsapp.isBlank()) {
            soloDigitos = TelefonoNormalizador.normalizar(whatsapp)
                    .orElseThrow(() -> new IllegalStateException(
                            "app.negocio.whatsapp no es un teléfono válido (ejemplo: +56912345678)."))
                    .substring(1); // sin el '+', como lo pide wa.me
        }
        this.datos = new NegocioResponse(nombreLimpio, soloDigitos);
    }

    @GetMapping
    public NegocioResponse obtener() {
        return datos;
    }
}
