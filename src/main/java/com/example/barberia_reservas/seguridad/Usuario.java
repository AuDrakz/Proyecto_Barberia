package com.example.barberia_reservas.seguridad;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Cuenta del administrador. Guarda SOLO el hash de la contraseña (BCrypt), nunca la contraseña.
 * No hay tabla de roles: hoy todo usuario de esta tabla es administrador.
 * A propósito no tiene toString(), para que el hash no termine en un log por accidente.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected Usuario() {
        // Constructor que exige JPA.
    }

    public static Usuario crear(String email, String passwordHash, Instant ahora) {
        Usuario usuario = new Usuario();
        usuario.email = Objects.requireNonNull(email);
        usuario.passwordHash = Objects.requireNonNull(passwordHash);
        usuario.creadoEn = Objects.requireNonNull(ahora);
        return usuario;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
