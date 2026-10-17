package com.example.barberia_reservas.seguridad;

import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Seguridad de la API:
 *  - Zona pública: horarios y solicitudes/cancelaciones del cliente (el token del enlace es su llave).
 *  - Zona de administración (/api/admin/**): exige sesión iniciada (cookie) y rol ADMIN.
 *  - Todo lo demás: DENEGADO por defecto (lo que no se permite explícitamente, no se puede usar).
 *
 * Es una API JSON, así que login, logout y errores responden con códigos HTTP (200, 401, 403), sin páginas HTML ni redirecciones.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** BCrypt es lento a propósito: si se filtra la base de datos, probar contraseñas por fuerza bruta resulta carísimo. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Cómo Spring Security busca a un usuario al iniciar sesión.
     * El mensaje de "no existe" es el mismo que el de "contraseña incorrecta" (Spring los unifica en un 401),
     * para que nadie pueda averiguar qué correos existen.
     */
    @Bean
    public UserDetailsService userDetailsService(UsuarioRepository usuarios) {
        return correo -> usuarios.findByEmail(correo.trim().toLowerCase(Locale.ROOT))
                .map(usuario -> User.withUsername(usuario.getEmail())
                        .password(usuario.getPasswordHash())
                        .roles("ADMIN")
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
    }

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/horarios").permitAll()
                        .requestMatchers("/api/reservas/**").permitAll()
                        .requestMatchers("/api/admin/sesion", "/api/admin/login").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().denyAll())

                // CSRF: un sitio malicioso no puede hacer que el navegador del administrador envíe acciones sin que
                // lo sepa, porque exigimos un token que solo el nuestro puede leer. csrf.spa() lo deja listo para
                // un frontend en JavaScript: deja el token en la cookie XSRF-TOKEN y el frontend lo devuelve en
                // el encabezado X-XSRF-TOKEN.
                .csrf(csrf -> {
                    csrf.spa();
                    // Endpoints públicos del cliente: no usan la sesión del administrador, no hay nada que "falsificar".
                    csrf.ignoringRequestMatchers("/api/reservas/**");
                })

                .formLogin(form -> form
                        .loginPage("/api/admin/login")
                        .loginProcessingUrl("/api/admin/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_OK))
                        .failureHandler((request, response, exception) ->
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)))

                .logout(logout -> logout
                        .logoutUrl("/api/admin/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()))

                // Sin sesión: 401 (no una redirección a una página de login).
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

        return http.build();
    }
}
