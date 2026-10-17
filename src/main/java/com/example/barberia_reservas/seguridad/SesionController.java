package com.example.barberia_reservas.seguridad;

import java.security.Principal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/admin/sesion es público: responde si hay una sesión iniciada.
 * El frontend lo llama al cargar. De paso, esa primera visita deja en el navegador la cookie XSRF-TOKEN
 * que hace falta para poder iniciar sesión y para las acciones del panel.
 */
@RestController
@RequestMapping("/api/admin")
public class SesionController {

    @GetMapping("/sesion")
    public SesionResponse sesion(Principal usuario) {
        boolean autenticado = usuario != null;
        return new SesionResponse(autenticado, autenticado ? usuario.getName() : null);
    }
}
