package com.example.barberia_reservas.horario;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
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
    private final int diasMinimos;
    private final int diasMaximos;
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
        this.diasMinimos = propiedades.diasMinimosAnticipacion();
        this.diasMaximos = propiedades.diasMaximosAnticipacion();
        this.regla = new ReglaAnticipacion(diasMinimos, diasMaximos);
        this.calculadora = new CalculadoraHorarios(zona, propiedades.duracionMinutos());
    }

    @Transactional(readOnly = true)
    public List<Horario> horariosLibres(LocalDate fecha) {
        // "Hoy" se calcula en la zona horaria del negocio, no en la del servidor.
        LocalDate hoy = LocalDate.now(clock.withZone(zona));
        regla.validar(fecha, hoy);
        return horariosDelDia(fecha, true);
    }

    /**
     * ¿Esa hora sigue siendo parte de lo que el barbero atiende? Es decir: cae en un tramo de su disponibilidad
     * semanal y no está bloqueada. A propósito IGNORA la regla de anticipación y las reservas ya confirmadas:
     * la usa el administrador al confirmar, y esas dos cosas se comprueban por otro lado.
     */
    @Transactional(readOnly = true)
    public boolean estaEnLaAgenda(Instant inicio) {
        LocalDate fecha = inicio.atZone(zona).toLocalDate();
        return horariosDelDia(fecha, false).stream()
                .anyMatch(horario -> horario.inicio().toInstant().equals(inicio));
    }

    /**
     * Los días de la ventana de reservas que tienen al menos un horario libre. Lo usa la pantalla del cliente para
     * mostrar solo días con opciones, en UNA llamada (en vez de que el navegador pregunte día por día).
     * La ventana tiene como máximo unas decenas de días, así que repetir el cálculo diario es barato a esta escala.
     */
    @Transactional(readOnly = true)
    public List<DiaConHorarios> resumenDias() {
        LocalDate hoy = LocalDate.now(clock.withZone(zona));
        LocalDate ultimo = hoy.plusDays(diasMaximos);

        List<DiaConHorarios> dias = new ArrayList<>();
        for (LocalDate fecha = hoy.plusDays(diasMinimos); !fecha.isAfter(ultimo); fecha = fecha.plusDays(1)) {
            int libres = horariosLibres(fecha).size();
            if (libres > 0) {
                dias.add(new DiaConHorarios(fecha, libres));
            }
        }
        return dias;
    }

    /** Disponibilidad - bloqueos (- reservas confirmadas, si se pide) de UN día. Sin validar la anticipación. */
    private List<Horario> horariosDelDia(LocalDate fecha, boolean descontarReservas) {
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

        Set<Instant> ocupados = descontarReservas ? iniciosConfirmados(fecha) : Set.of();
        return calculadora.calcular(fecha, disponibles, bloqueados, ocupados);
    }

    private Set<Instant> iniciosConfirmados(LocalDate fecha) {
        // Rango del día en la zona del negocio, convertido a instantes: [00:00 de hoy, 00:00 de mañana)
        Instant desde = fecha.atStartOfDay(zona).toInstant();
        Instant hasta = fecha.plusDays(1).atStartOfDay(zona).toInstant();
        return new HashSet<>(reservaRepository.buscarInicios(EstadoReserva.CONFIRMADA, desde, hasta));
    }
}