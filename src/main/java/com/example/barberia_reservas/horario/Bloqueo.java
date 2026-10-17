package com.example.barberia_reservas.horario;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Excepción PUNTUAL a la disponibilidad ("este martes no puedo"). Sin horas = bloquea el día completo. */
@Entity
@Table(name = "bloqueo")
public class Bloqueo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_fin")
    private LocalTime horaFin;

    @Column(name = "motivo")
    private String motivo;

    protected Bloqueo() {
        // Constructor que exige JPA.
    }

    /** horaInicio y horaFin van las dos o ninguna (ninguna = día completo); la base de datos también lo exige. */
    public static Bloqueo crear(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, String motivo) {
        Objects.requireNonNull(fecha);
        if ((horaInicio == null) != (horaFin == null)) {
            throw new IllegalArgumentException("horaInicio y horaFin deben venir juntas o ninguna");
        }
        if (horaInicio != null && !horaFin.isAfter(horaInicio)) {
            throw new IllegalArgumentException("horaFin debe ser posterior a horaInicio");
        }
        Bloqueo bloqueo = new Bloqueo();
        bloqueo.fecha = fecha;
        bloqueo.horaInicio = horaInicio;
        bloqueo.horaFin = horaFin;
        bloqueo.motivo = motivo;
        return bloqueo;
    }

    public boolean esDiaCompleto() {
        return horaInicio == null && horaFin == null;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public String getMotivo() {
        return motivo;
    }
}
