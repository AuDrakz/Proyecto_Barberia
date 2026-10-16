package com.example.barberia_reservas.reserva;

import java.net.URI;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.barberia_reservas.config.ReservasProperties;

/** Endpoints PÚBLICOS del cliente. No hay listados: solo se llega a una reserva si se tiene su token. */
@RestController
@RequestMapping("/api/reservas")
public class ReservasController {

    private final ReservaService servicio;
    private final ZoneId zona;

    public ReservasController(ReservaService servicio, ReservasProperties propiedades) {
        this.servicio = servicio;
        this.zona = ZoneId.of(propiedades.zonaHoraria());
    }

    // POST /api/reservas  -> 201 Created, con la reserva y su token
    @PostMapping
    public ResponseEntity<ReservaResponse> solicitar(@RequestBody SolicitudReservaRequest peticion) {
        Reserva reserva = servicio.solicitar(
                peticion.nombreCliente(), peticion.telefono(), peticion.instagram(), peticion.inicio());
        ReservaResponse respuesta = ReservaResponse.desde(reserva, zona);
        return ResponseEntity.created(URI.create("/api/reservas/" + respuesta.token())).body(respuesta);
    }

    // GET /api/reservas/{token}  -> el cliente consulta cómo va su solicitud
    @GetMapping("/{token}")
    public ReservaResponse ver(@PathVariable("token") UUID token) {
        return ReservaResponse.desde(servicio.obtener(token), zona);
    }

    // POST /api/reservas/{token}/cancelar  -> el cliente cancela con su enlace
    @PostMapping("/{token}/cancelar")
    public ReservaResponse cancelar(@PathVariable("token") UUID token) {
        return ReservaResponse.desde(servicio.cancelarPorCliente(token), zona);
    }
}
