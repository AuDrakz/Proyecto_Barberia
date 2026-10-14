package com.example.barberia_reservas.reserva;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad de solo lectura por ahora (etapa 2 solo CONSULTA reservas confirmadas).
 * La creación de reservas, con sus validaciones, llega en la etapa 3.
 *
 * Los nombres de columna son explícitos para que coincidan EXACTAMENTE con la migración V1;
 * con ddl-auto=validate, Hibernate comprueba esto al arrancar.
 */
@Entity
@Table(name = "reserva")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_cliente", nullable = false)
    private String nombreCliente;

    @Column(name = "telefono", nullable = false)
    private String telefono;

    @Column(name = "instagram")
    private String instagram;

    @Column(name = "inicio", nullable = false)
    private Instant inicio;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoReserva estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancelada_por")
    private CanceladaPor canceladaPor;

    @Column(name = "token_cancelacion", nullable = false, updatable = false)
    private UUID tokenCancelacion;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    protected Reserva() {
        // Constructor que exige JPA. No se usa desde el código de la aplicación.
    }

    public Long getId() {
        return id;
    }

    public Instant getInicio() {
        return inicio;
    }

    public EstadoReserva getEstado() {
        return estado;
    }
}
