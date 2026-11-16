package com.example.barberia_reservas.horario;

import java.time.LocalTime;

/** Un tramo de horas dentro de un mismo día, por ejemplo 19:00 a 22:00. */
public record RangoHorario(LocalTime inicio, LocalTime fin) {

    public RangoHorario {
        if (inicio == null || fin == null || !fin.isAfter(inicio)) {
            throw new IllegalArgumentException("El fin debe ser posterior al inicio: " + inicio + " - " + fin);
        }
    }

    /**
     * Dos tramos se solapan si cada uno empieza ANTES de que termine el otro.
     * Si solo se "tocan" (uno termina justo cuando empieza el otro) NO se solapan.
     */
    public boolean seSolapaCon(LocalTime otroInicio, LocalTime otroFin) {
        return inicio.isBefore(otroFin) && otroInicio.isBefore(fin);
    }
}
