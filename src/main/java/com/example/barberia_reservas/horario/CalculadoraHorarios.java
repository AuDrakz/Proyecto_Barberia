package com.example.barberia_reservas.horario;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Corazón de la etapa 2. Calcula los horarios libres de UN día:
 *
 *     disponibilidad semanal  -  bloqueos puntuales  -  reservas confirmadas
 *
 * Es lógica PURA: recibe datos y devuelve datos. No usa Spring, ni base de datos, ni reloj.
 * Por eso se puede probar con pruebas unitarias rápidas, sin levantar nada.
 */
public final class CalculadoraHorarios {

    private final ZoneId zona;
    private final int duracionMinutos;

    public CalculadoraHorarios(ZoneId zona, int duracionMinutos) {
        if (zona == null || duracionMinutos <= 0) {
            throw new IllegalArgumentException("Zona horaria y duración (> 0) son obligatorias");
        }
        this.zona = zona;
        this.duracionMinutos = duracionMinutos;
    }

    /**
     * @param fecha       el día a calcular
     * @param disponibles tramos en que se atiende ese día de la semana
     * @param bloqueados  tramos bloqueados en esa fecha (los bloqueos de día completo se resuelven antes, en el servicio)
     * @param ocupados    instantes de inicio de las reservas CONFIRMADAS de ese día
     */
    public List<Horario> calcular(LocalDate fecha,
                                  List<RangoHorario> disponibles,
                                  List<RangoHorario> bloqueados,
                                  Set<Instant> ocupados) {
        List<Horario> libres = new ArrayList<>();

        for (RangoHorario disponible : disponibles) {
            // Cuántos horarios ENTEROS caben en el tramo. División entera: lo que sobra se descarta.
            // (19:00-21:30 con horas de 60 min => caben 2, y la 3.ª no cabe.)
            // Se cuenta con un bucle de largo conocido, así nunca hay riesgo de pasarse de la medianoche.
            long minutosTotales = Duration.between(disponible.inicio(), disponible.fin()).toMinutes();
            long cantidad = minutosTotales / duracionMinutos;

            for (long i = 0; i < cantidad; i++) {
                LocalTime inicio = disponible.inicio().plusMinutes(i * duracionMinutos);
                LocalTime fin = inicio.plusMinutes(duracionMinutos);
                ZonedDateTime inicioConZona = ZonedDateTime.of(fecha, inicio, zona);

                boolean bloqueado = bloqueados.stream().anyMatch(b -> b.seSolapaCon(inicio, fin));
                boolean ocupado = ocupados.contains(inicioConZona.toInstant());

                if (!bloqueado && !ocupado) {
                    libres.add(new Horario(inicio, inicioConZona));
                }
            }
        }

        // distinct(): si dos tramos de disponibilidad se superponen, el mismo horario no se ofrece dos veces.
        return libres.stream()
                .distinct()
                .sorted(Comparator.comparing(Horario::hora))
                .toList();
    }
}
