package com.example.barberia_reservas.reserva;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.barberia_reservas.common.ConflictoDeNegocioException;
import com.example.barberia_reservas.common.ReservaNoEncontradaException;

/** Casos de uso del ADMINISTRADOR: ver solicitudes, confirmar, rechazar y cancelar. */
@Service
public class AdminReservaService {

    private final ReservaRepository reservaRepository;
    private final Clock clock;

    public AdminReservaService(ReservaRepository reservaRepository, Clock clock) {
        this.reservaRepository = reservaRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Reserva> listar(EstadoReserva estado) {
        return reservaRepository.findByEstadoAndInicioGreaterThanEqualOrderByInicioAsc(estado, clock.instant());
    }

    /**
     * Confirma una solicitud. Todo ocurre en UNA transacción: o se hace todo, o nada.
     *   1) La reserva pasa a CONFIRMADA (y entonces bloquea el horario).
     *   2) Las demás solicitudes pendientes del MISMO horario quedan RECHAZADAS.
     * Doble protección contra dos confirmadas en el mismo horario: una comprobación previa (mensaje amable)
     * y el índice único parcial de la base de datos (la red de seguridad final).
     */
    @Transactional
    public Reserva confirmar(Long id) {
        Reserva reserva = buscar(id);
        Instant ahora = clock.instant();

        if (!reserva.getInicio().isAfter(ahora)) {
            throw new ConflictoDeNegocioException("No se puede confirmar una hora que ya pasó.");
        }
        if (reservaRepository.existsByInicioAndEstado(reserva.getInicio(), EstadoReserva.CONFIRMADA)) {
            throw new ConflictoDeNegocioException("Ese horario ya tiene una reserva confirmada.");
        }

        reserva.confirmar(ahora); // lanza TransicionInvalidaException si no estaba SOLICITADA
        try {
            // saveAndFlush envía el cambio a la base de datos AHORA (no al final), para detectar el choque aquí mismo.
            reservaRepository.saveAndFlush(reserva);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictoDeNegocioException("Ese horario ya tiene una reserva confirmada.");
        }

        for (Reserva otra : reservaRepository.findByInicioAndEstado(reserva.getInicio(), EstadoReserva.SOLICITADA)) {
            otra.rechazar(ahora);
        }
        return reserva;
    }

    @Transactional
    public Reserva rechazar(Long id) {
        Reserva reserva = buscar(id);
        reserva.rechazar(clock.instant());
        return reserva;
    }

    @Transactional
    public Reserva cancelar(Long id) {
        Reserva reserva = buscar(id);
        reserva.cancelar(CanceladaPor.ADMIN, clock.instant());
        return reserva;
    }

    private Reserva buscar(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new ReservaNoEncontradaException("No existe esa reserva."));
    }
}
