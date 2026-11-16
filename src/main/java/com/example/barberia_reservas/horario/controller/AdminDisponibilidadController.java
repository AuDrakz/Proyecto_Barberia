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

/** Disponibilidad semanal. La protección (sesión + ADMIN + CSRF) la da SecurityConfig para todo /api/admin/**. */
@RestController
@RequestMapping("/api/admin/disponibilidad")
public class AdminDisponibilidadController {

    private final DisponibilidadAdminService servicio;

    public AdminDisponibilidadController(DisponibilidadAdminService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<DisponibilidadResponse> listar() {
        return servicio.listar().stream().map(DisponibilidadResponse::desde).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DisponibilidadResponse crear(@RequestBody DisponibilidadRequest peticion) {
        return DisponibilidadResponse.desde(
                servicio.crear(peticion.diaSemana(), peticion.horaInicio(), peticion.horaFin()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable("id") Long id) {
        servicio.eliminar(id);
    }
}
