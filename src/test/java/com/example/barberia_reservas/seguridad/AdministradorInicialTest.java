package com.example.barberia_reservas.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AdministradorInicialTest {

    private static final String CLAVE = "una-contrasena-larga-1";

    private final UsuarioRepository repositorio = mock(UsuarioRepository.class);
    private final BCryptPasswordEncoder codificador = new BCryptPasswordEncoder();
    private final Clock reloj = Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC);

    private AdministradorInicial con(String email, String password) {
        return new AdministradorInicial(repositorio, codificador, reloj, email, password);
    }

    @Test
    void creaElAdministradorGuardandoSoloElHashDeLaContrasena() {
        when(repositorio.count()).thenReturn(0L);

        con("  Admin@Barberia.CL ", CLAVE).inicializar();

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(repositorio).save(guardado.capture());
        assertEquals("admin@barberia.cl", guardado.getValue().getEmail());
        assertNotEquals(CLAVE, guardado.getValue().getPasswordHash());
        assertTrue(codificador.matches(CLAVE, guardado.getValue().getPasswordHash()));
    }

    @Test
    void noHaceNadaSiYaExisteUnAdministrador() {
        when(repositorio.count()).thenReturn(1L);

        con("admin@barberia.cl", CLAVE).inicializar();

        verify(repositorio, never()).save(any(Usuario.class));
    }

    @Test
    void noHaceNadaSiFaltanLasVariablesDeEntorno() {
        when(repositorio.count()).thenReturn(0L);

        con("", "").inicializar();
        con(null, null).inicializar();

        verify(repositorio, never()).save(any(Usuario.class));
    }

    @Test
    void fallaAlArrancarSiLaContrasenaEsMuyCorta() {
        when(repositorio.count()).thenReturn(0L);

        assertThrows(IllegalStateException.class, () -> con("admin@barberia.cl", "corta123").inicializar());
        verify(repositorio, never()).save(any(Usuario.class));
    }

    @Test
    void fallaSiLaContrasenaSuperaElLimiteDeBCrypt() {
        when(repositorio.count()).thenReturn(0L);

        assertThrows(IllegalStateException.class, () -> con("admin@barberia.cl", "a".repeat(73)).inicializar());
    }

    @Test
    void fallaSiElCorreoNoEsValido() {
        when(repositorio.count()).thenReturn(0L);

        assertThrows(IllegalStateException.class, () -> con("esto-no-es-un-correo", CLAVE).inicializar());
    }
}
