package com.example.barberia_reservas.horario;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.barberia_reservas.config.ReservasProperties;
import com.example.barberia_reservas.reserva.EstadoReserva;
import com.example.barberia_reservas.reserva.ReservaRepository;

/**
 * Orquesta: valida la regla, LEE los datos de la base y se los pasa a la calculadora.
 * Separación de responsabilidades: este servicio sabe de base de datos; la calculadora, no.
 */
@Service
public class HorariosService {

    private final DisponibilidadRepository disponibilidadRepository;
    private final BloqueoRepository bloqueoRepository;
    private final ReservaRepository reservaRepository;
    private final Clock clock;
    private final ZoneId zona;
    private final ReglaAnticipacion regla;
    private final CalculadoraHorarios calculadora;

    public HorariosService(DisponibilidadRepository disponibilidadRepository,
                           BloqueoRepository bloqueoRepository,
                           ReservaRepository reservaRepository,
                           ReservasProperties propiedades,
                           Clock clock) {
        this.disponibilidadRepository = disponibilidadRepository;
        this.bloqueoRepository = bloqueoRepository;
        this.reservaRepository = reservaRepository;
        this.clock = clock;
        this.zona = ZoneId.of(propiedades.zonaHoraria());
        this.regla = new ReglaAnticipacion(propiedades.diasMinimosAnticipacion(), propiedades.diasMaximosAnticipacion());
        this.calculadora = new CalculadoraHorarios(zona, propiedades.duracionMinutos());
    }

    @Transactional(readOnly = true)
    public List<Horario> horariosLibres(LocalDate fecha) {
        // "Hoy" se calcula en la zona horaria del negocio, no en la del servidor.
        LocalDate hoy = LocalDate.now(clock.withZone(zona));
        regla.validar(fecha, hoy);

        // Un bloqueo de día completo gana sobre todo lo demás: ahorra el resto de las consultas.
        List<Bloqueo> bloqueos = bloqueoRepository.findByFecha(fecha);
        if (bloqueos.stream().anyMatch(Bloqueo::esDiaCompleto)) {
            return List.of();
        }

        // getDayOfWeek().getValue() es 1 = lunes ... 7 = domingo, igual que dia_semana en la base de datos.
        short diaSemana = (short) fecha.getDayOfWeek().getValue();
        List<RangoHorario> disponibles = disponibilidadRepository.findByDiaSemana(diaSemana).stream()
                .map(d -> new RangoHorario(d.getHoraInicio(), d.getHoraFin()))
                .toList();
        if (disponibles.isEmpty()) {
            return List.of();
        }

        List<RangoHorario> bloqueados = bloqueos.stream()
                .map(b -> new RangoHorario(b.getHoraInicio(), b.getHoraFin()))
                .toList();

        // Rango del día en la zona del negocio, convertido a instantes: [00:00 de hoy, 00:00 de mañana)
        Instant desde = fecha.atStartOfDay(zona).toInstant();
        Instant hasta = fecha.plusDays(1).atStartOfDay(zona).toInstant();
        Set<Instant> ocupados = new HashSet<>(
                reservaRepository.buscarInicios(EstadoReserva.CONFIRMADA, desde, hasta));

        return calculadora.calcular(fecha, disponibles, bloqueados, ocupados);
    }
}
