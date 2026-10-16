package com.example.barberia_reservas.reserva;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.example.barberia_reservas.common.TransicionInvalidaException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Una reserva y su ciclo de vida:
 *
 *   SOLICITADA --confirmar--> CONFIRMADA --cancelar--> CANCELADA
 *       |  \--rechazar--> RECHAZADA                       ^
 *       \----------------cancelar-------------------------/
 *
 * Las reglas de transición viven AQUÍ, dentro de la entidad: nadie puede dejar una reserva en un estado
 * inconsistente desde afuera (no hay setters). Es el principio de "encapsulamiento" aplicado al dominio.
 *
 * Los nombres de columna son explícitos para que coincidan EXACTAMENTE con la migración V1.
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
        // Constructor que exige JPA. Desde el código de la aplicación se usa Reserva.solicitar(...).
    }

    /** Única forma de crear una reserva: siempre nace SOLICITADA y con un token imposible de adivinar. */
    public static Reserva solicitar(String nombreCliente, String telefono, String instagram, Instant inicio, Instant ahora) {
        Reserva reserva = new Reserva();
        reserva.nombreCliente = Objects.requireNonNull(nombreCliente);
        reserva.telefono = Objects.requireNonNull(telefono);
        reserva.instagram = instagram; // opcional
        reserva.inicio = Objects.requireNonNull(inicio);
        reserva.estado = EstadoReserva.SOLICITADA;
        // randomUUID usa un generador criptográficamente seguro: 122 bits aleatorios, no se puede adivinar.
        reserva.tokenCancelacion = UUID.randomUUID();
        reserva.creadoEn = Objects.requireNonNull(ahora);
        reserva.actualizadoEn = ahora;
        return reserva;
    }

    public void confirmar(Instant ahora) {
        exigirEstado("confirmar", EstadoReserva.SOLICITADA);
        estado = EstadoReserva.CONFIRMADA;
        actualizadoEn = ahora;
    }

    public void rechazar(Instant ahora) {
        exigirEstado("rechazar", EstadoReserva.SOLICITADA);
        estado = EstadoReserva.RECHAZADA;
        actualizadoEn = ahora;
    }

    public void cancelar(CanceladaPor quien, Instant ahora) {
        Objects.requireNonNull(quien);
        exigirEstado("cancelar", EstadoReserva.SOLICITADA, EstadoReserva.CONFIRMADA);
        estado = EstadoReserva.CANCELADA;
        canceladaPor = quien; // la base de datos exige: cancelada_por tiene valor si y solo si el estado es CANCELADA
        actualizadoEn = ahora;
    }

    private void exigirEstado(String accion, EstadoReserva... permitidos) {
        for (EstadoReserva permitido : permitidos) {
            if (estado == permitido) {
                return;
            }
        }
        throw new TransicionInvalidaException("No se puede " + accion + " una reserva en estado " + estado + ".");
    }

    public Long getId() {
        return id;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getInstagram() {
        return instagram;
    }

    public Instant getInicio() {
        return inicio;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public CanceladaPor getCanceladaPor() {
        return canceladaPor;
    }

    public UUID getTokenCancelacion() {
        return tokenCancelacion;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }
}
