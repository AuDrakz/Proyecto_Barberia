package com.example.barberia_reservas.reserva;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Lleva un teléfono escrito de cualquier forma a UN formato único (E.164, por ejemplo +56912345678).
 *
 * ¿Por qué importa? El límite de "2 solicitudes pendientes por teléfono" solo sirve si "9 1234 5678",
 * "912345678" y "+56 9 1234 5678" cuentan como EL MISMO número. Si no, bastaría cambiar el formato para saltarse el límite.
 *
 * Supuesto: sin código de país, se asume un celular chileno (9 dígitos que empiezan con 9 => +56).
 */
public final class TelefonoNormalizador {

    private static final Pattern CARACTERES_PERMITIDOS = Pattern.compile("^\\+?[0-9 ()\\-]+$");
    private static final Pattern E164 = Pattern.compile("^\\+[1-9][0-9]{7,14}$");

    private TelefonoNormalizador() {
    }

    /** Devuelve el teléfono normalizado, o vacío si no se puede interpretar con seguridad. */
    public static Optional<String> normalizar(String crudo) {
        if (crudo == null) {
            return Optional.empty();
        }
        String texto = crudo.trim();
        if (!CARACTERES_PERMITIDOS.matcher(texto).matches()) {
            return Optional.empty();
        }
        String digitos = texto.replaceAll("[^0-9]", "");

        String candidato;
        if (texto.startsWith("+")) {
            candidato = "+" + digitos;
        } else if (digitos.length() == 9 && digitos.startsWith("9")) {
            candidato = "+56" + digitos;          // celular chileno sin código de país
        } else if (digitos.length() == 11 && digitos.startsWith("56")) {
            candidato = "+" + digitos;            // código de país escrito sin el "+"
        } else {
            return Optional.empty();              // ambiguo: mejor pedir el número completo que adivinar
        }
        return E164.matcher(candidato).matches() ? Optional.of(candidato) : Optional.empty();
    }
}
