package com.example.barberia_reservas.horario;

import java.time.LocalTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Disponibilidad SEMANAL recurrente: por ejemplo, "martes de 19:00 a 22:00" (dia_semana 1 = lunes ... 7 = domingo). */
@Entity
@Table(name = "disponibilidad")
public class Disponibilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dia_semana", nullable = false)
    private short diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    protected Disponibilidad() {
        // Constructor que exige JPA.
    }

    public static Disponibilidad crear(short diaSemana, LocalTime horaInicio, LocalTime horaFin) {
        if (diaSemana < 1 || diaSemana > 7) {
            throw new IllegalArgumentException("diaSemana debe estar entre 1 y 7");
        }
        Objects.requireNonNull(horaInicio);
        Objects.requireNonNull(horaFin);
        if (!horaFin.isAfter(horaInicio)) {
            throw new IllegalArgumentException("horaFin debe ser posterior a horaInicio");
        }
        Disponibilidad disponibilidad = new Disponibilidad();
        disponibilidad.diaSemana = diaSemana;
        disponibilidad.horaInicio = horaInicio;
        disponibilidad.horaFin = horaFin;
        return disponibilidad;
    }

    public Long getId() {
        return id;
    }

    public short getDiaSemana() {
        return diaSemana;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }
}
