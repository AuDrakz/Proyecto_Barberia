package com.example.barberia_reservas.horario;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Bloqueos puntuales. La protección (sesión + ADMIN + CSRF) la da SecurityConfig para todo /api/admin/**. */
@RestController
@RequestMapping("/api/admin/bloqueos")
public class AdminBloqueosController {

    private final BloqueoAdminService servicio;

    public AdminBloqueosController(BloqueoAdminService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<BloqueoResponse> listar() {
        return servicio.listar().stream().map(bloqueo -> BloqueoResponse.desde(bloqueo, null)).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BloqueoResponse crear(@RequestBody BloqueoRequest peticion) {
        ResultadoBloqueo resultado = servicio.crear(
                peticion.fecha(), peticion.horaInicio(), peticion.horaFin(), peticion.motivo());
        return BloqueoResponse.desde(resultado.bloqueo(), resultado.solicitudesPendientesAfectadas());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable("id") Long id) {
        servicio.eliminar(id);
    }
}
