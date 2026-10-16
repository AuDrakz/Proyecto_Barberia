package com.example.barberia_reservas.reserva;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import com.example.barberia_reservas.common.SolicitudInvalidaException;

/**
 * Valida y NORMALIZA lo que escribe el cliente. Es la primera defensa: todo dato externo es "no confiable"
 * hasta que se comprueba. Reúne TODOS los errores a la vez (no solo el primero) para que el cliente corrija de una vez.
 *
 * Lógica pura: sin Spring ni base de datos.
 */
public final class ValidadorSolicitud {

    // Letras (con tildes y ñ), espacios, punto, apóstrofe y guion. De 2 a 100 caracteres. Sin números ni símbolos raros.
    private static final Pattern NOMBRE = Pattern.compile("^\\p{L}[\\p{L} .'\\-]{1,99}$");
    // Reglas de nombres de usuario de Instagram: letras, números, punto y guion bajo; hasta 30 caracteres.
    private static final Pattern INSTAGRAM = Pattern.compile("^[A-Za-z0-9._]{1,30}$");

    public SolicitudNormalizada validar(String nombre, String telefono, String instagram, String inicio) {
        Map<String, String> errores = new LinkedHashMap<>();

        String nombreLimpio = nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");
        if (!NOMBRE.matcher(nombreLimpio).matches()) {
            errores.put("nombreCliente", "Escribe tu nombre (solo letras, de 2 a 100 caracteres).");
        }

        Optional<String> telefonoNormalizado = TelefonoNormalizador.normalizar(telefono);
        if (telefonoNormalizado.isEmpty()) {
            errores.put("telefono", "Ingresa un celular chileno (por ejemplo 9 1234 5678) o un número internacional que empiece con +.");
        }

        String instagramLimpio = null; // es opcional
        if (instagram != null && !instagram.isBlank()) {
            String usuario = instagram.trim();
            if (usuario.startsWith("@")) {
                usuario = usuario.substring(1);
            }
            if (INSTAGRAM.matcher(usuario).matches()) {
                instagramLimpio = usuario.toLowerCase(Locale.ROOT); // en Instagram no se distinguen mayúsculas
            } else {
                errores.put("instagram", "El usuario de Instagram no es válido (letras, números, punto y guion bajo).");
            }
        }

        Instant inicioInstante = null;
        if (inicio == null || inicio.isBlank()) {
            errores.put("inicio", "Elige un horario de la lista.");
        } else {
            try {
                inicioInstante = OffsetDateTime.parse(inicio.trim()).toInstant();
            } catch (DateTimeParseException e) {
                errores.put("inicio", "El horario no tiene un formato válido.");
            }
        }

        if (!errores.isEmpty()) {
            throw new SolicitudInvalidaException(errores);
        }
        return new SolicitudNormalizada(nombreLimpio, telefonoNormalizado.get(), instagramLimpio, inicioInstante);
    }
}
