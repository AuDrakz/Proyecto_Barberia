package com.example.barberia_reservas.horario;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.barberia_reservas.common.ConflictoDeNegocioException;
import com.example.barberia_reservas.common.NoEncontradoException;
import com.example.barberia_reservas.config.ReservasProperties;
import com.example.barberia_reservas.reserva.EstadoReserva;
import com.example.barberia_reservas.reserva.Reserva;
import com.example.barberia_reservas.reserva.ReservaRepository;

/** El administrador bloquea un día completo o unas horas ("este martes no puedo"). */
@Service
public class BloqueoAdminService {

    private final BloqueoRepository bloqueoRepository;
    private final ReservaRepository reservaRepository;
    private final Clock clock;
    private final ZoneId zona;
    private final int duracionMinutos;
    private final ValidadorHorarioAdmin validador;

    public BloqueoAdminService(BloqueoRepository bloqueoRepository,
                               ReservaRepository reservaRepository,
                               ReservasProperties propiedades,
                               Clock clock) {
        this.bloqueoRepository = bloqueoRepository;
        this.reservaRepository = reservaRepository;
        this.clock = clock;
        this.zona = ZoneId.of(propiedades.zonaHoraria());
        this.duracionMinutos = propiedades.duracionMinutos();
        this.validador = new ValidadorHorarioAdmin(propiedades.duracionMinutos());
    }

    @Transactional(readOnly = true)
    public List<Bloqueo> listar() {
        return bloqueoRepository.findByFechaGreaterThanEqualOrderByFechaAscHoraInicioAsc(hoy());
    }

    @Transactional
    public ResultadoBloqueo crear(String fecha, String horaInicio, String horaFin, String motivo) {
        NuevoBloqueo nuevo = validador.validarBloqueo(fecha, horaInicio, horaFin, motivo, hoy());

        if (bloqueoRepository.findByFecha(nuevo.fecha()).stream().anyMatch(Bloqueo::esDiaCompleto)) {
            throw new ConflictoDeNegocioException("Ese día ya está bloqueado por completo.");
        }

        // Reservas de ese día en la zona horaria del negocio: [00:00 de la fecha, 00:00 del día siguiente)
        Instant desde = nuevo.fecha().atStartOfDay(zona).toInstant();
        Instant hasta = nuevo.fecha().plusDays(1).atStartOfDay(zona).toInstant();

        List<Reserva> confirmadasQueChocan = reservaRepository
                .findByEstadoAndInicioGreaterThanEqualAndInicioLessThan(EstadoReserva.CONFIRMADA, desde, hasta).stream()
                .filter(reserva -> chocaConElBloqueo(reserva, nuevo))
                .toList();
        if (!confirmadasQueChocan.isEmpty()) {
            String detalle = confirmadasQueChocan.stream()
                    .map(r -> r.getInicio().atZone(zona).toLocalTime() + " (" + r.getNombreCliente() + ")")
                    .collect(Collectors.joining(", "));
            throw new ConflictoDeNegocioException("Hay reservas confirmadas dentro de ese bloqueo: " + detalle
                    + ". Cancélalas primero.");
        }

        // Las solicitudes pendientes NO impiden el bloqueo (todavía no son una cita), pero se avisa cuántas hay.
        long pendientes = reservaRepository
                .findByEstadoAndInicioGreaterThanEqualAndInicioLessThan(EstadoReserva.SOLICITADA, desde, hasta).stream()
                .filter(reserva -> chocaConElBloqueo(reserva, nuevo))
                .count();

        Bloqueo guardado = bloqueoRepository.save(
                Bloqueo.crear(nuevo.fecha(), nuevo.horaInicio(), nuevo.horaFin(), nuevo.motivo()));
        return new ResultadoBloqueo(guardado, (int) pendientes);
    }

    @Transactional
    public void eliminar(Long id) {
        Bloqueo bloqueo = bloqueoRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe ese bloqueo."));
        bloqueoRepository.delete(bloqueo);
    }

    /**
     * Una reserva choca con el bloqueo si se pisan en el tiempo. Se compara en MINUTOS desde la medianoche
     * (sin LocalTime.plus) para que una cita a las 23:30 no "dé la vuelta" y parezca que termina a las 00:30.
     */
    private boolean chocaConElBloqueo(Reserva reserva, NuevoBloqueo bloqueo) {
        if (bloqueo.esDiaCompleto()) {
            return true;
        }
        int inicioReserva = reserva.getInicio().atZone(zona).toLocalTime().toSecondOfDay() / 60;
        int finReserva = inicioReserva + duracionMinutos;
        int inicioBloqueo = bloqueo.horaInicio().toSecondOfDay() / 60;
        int finBloqueo = bloqueo.horaFin().toSecondOfDay() / 60;
        return inicioReserva < finBloqueo && inicioBloqueo < finReserva;
    }

    private LocalDate hoy() {
        return LocalDate.now(clock.withZone(zona));
    }
}
