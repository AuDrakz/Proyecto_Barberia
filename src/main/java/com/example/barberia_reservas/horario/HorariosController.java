package com.example.barberia_reservas.horario;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints PÚBLICOS (solo lectura): los clientes consultan qué horas hay libres. */
@RestController
@RequestMapping("/api/horarios")
public class HorariosController {

    private final HorariosService servicio;

    public HorariosController(HorariosService servicio) {
        this.servicio = servicio;
    }

    // Ejemplo: GET /api/horarios?fecha=2026-10-20
    // Si "fecha" falta o no tiene formato AAAA-MM-DD, Spring responde 400 automáticamente.
    @GetMapping
    public HorariosResponse horariosLibres(
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        List<HorariosResponse.Item> items = servicio.horariosLibres(fecha).stream()
                .map(h -> new HorariosResponse.Item(
                        h.hora().toString(),
                        DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(h.inicio())))
                .toList();

        return new HorariosResponse(fecha.toString(), items);
    }

    // GET /api/horarios/resumen -> los días con al menos un horario libre (para pintar el selector de días)
    @GetMapping("/resumen")
    public ResumenHorariosResponse resumen() {
        return ResumenHorariosResponse.desde(servicio.resumenDias());
    }
}
