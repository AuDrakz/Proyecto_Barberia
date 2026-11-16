package com.example.barberia_reservas.reserva;

import java.time.ZoneId;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.barberia_reservas.config.ReservasProperties;

/**
 * Endpoints del ADMINISTRADOR. La regla "solo con sesión y rol ADMIN" NO está aquí: está en SecurityConfig
 * (todo lo que cuelga de /api/admin/**). Así la seguridad vive en un solo lugar y no depende de que alguien
 * se acuerde de proteger cada método.
 */
@RestController
@RequestMapping("/api/admin/reservas")
public class AdminReservasController {

    private final AdminReservaService servicio;
    private final ZoneId zona;

    public AdminReservasController(AdminReservaService servicio, ReservasProperties propiedades) {
        this.servicio = servicio;
        this.zona = ZoneId.of(propiedades.zonaHoraria());
    }

    // GET /api/admin/reservas?estado=SOLICITADA  (por defecto: las pendientes). Solo reservas futuras.
    @GetMapping
    public List<ReservaAdminResponse> listar(
            @RequestParam(name = "estado", defaultValue = "SOLICITADA") EstadoReserva estado) {
        return servicio.listar(estado).stream()
                .map(reserva -> ReservaAdminResponse.desde(reserva, zona))
                .toList();
    }

    @PostMapping("/{id}/confirmar")
    public ReservaAdminResponse confirmar(@PathVariable("id") Long id) {
        return ReservaAdminResponse.desde(servicio.confirmar(id), zona);
    }

    @PostMapping("/{id}/rechazar")
    public ReservaAdminResponse rechazar(@PathVariable("id") Long id) {
        return ReservaAdminResponse.desde(servicio.rechazar(id), zona);
    }

    @PostMapping("/{id}/cancelar")
    public ReservaAdminResponse cancelar(@PathVariable("id") Long id) {
        return ReservaAdminResponse.desde(servicio.cancelar(id), zona);
    }
}
