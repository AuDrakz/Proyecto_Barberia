package com.example.barberia_reservas.seguridad;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Locale;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea la cuenta del administrador al arrancar, a partir de variables de entorno (ADMIN_EMAIL y ADMIN_PASSWORD).
 *
 * Por qué así y no con un endpoint de registro: un "crear cuenta" abierto en internet es una puerta que alguien
 * puede intentar usar. Aquí no existe esa puerta, y la contraseña nunca queda escrita en el código ni en GitHub.
 *
 * Solo actúa si NO hay ningún usuario. Si ya existe un administrador, no toca nada
 * (cambiar la variable de entorno después no cambia la contraseña guardada).
 */
@Component
public class AdministradorInicial implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AdministradorInicial.class);
    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int LARGO_MINIMO = 12;
    private static final int BYTES_MAXIMOS = 72; // límite técnico de BCrypt

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;
    private final Clock clock;
    private final String email;
    private final String password;

    public AdministradorInicial(UsuarioRepository usuarios,
                                PasswordEncoder codificador,
                                Clock clock,
                                @Value("${app.admin.email:}") String email,
                                @Value("${app.admin.password:}") String password) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.clock = clock;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        inicializar();
    }

    void inicializar() {
        if (usuarios.count() > 0) {
            return;
        }
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            LOG.warn("No existe ningún administrador. Define las variables de entorno ADMIN_EMAIL y ADMIN_PASSWORD "
                    + "y reinicia la aplicación.");
            return;
        }

        String correo = email.trim().toLowerCase(Locale.ROOT);
        if (correo.length() > 255 || !CORREO.matcher(correo).matches()) {
            throw new IllegalStateException("ADMIN_EMAIL no es un correo válido.");
        }
        // Se falla al arrancar (en vez de crear una cuenta débil). El mensaje NO incluye la contraseña.
        if (password.length() < LARGO_MINIMO || password.getBytes(StandardCharsets.UTF_8).length > BYTES_MAXIMOS) {
            throw new IllegalStateException("ADMIN_PASSWORD debe tener al menos " + LARGO_MINIMO
                    + " caracteres y no más de " + BYTES_MAXIMOS + " bytes.");
        }

        usuarios.save(Usuario.crear(correo, codificador.encode(password), clock.instant()));
        LOG.info("Administrador creado: {}", correo);
    }
}
