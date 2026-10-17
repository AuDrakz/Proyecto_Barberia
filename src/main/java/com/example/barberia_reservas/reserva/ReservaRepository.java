package com.example.barberia_reservas.reserva;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /**
     * Solo la hora de inicio (no la entidad completa) de las reservas en un estado dado dentro de [desde, hasta).
     * Para calcular horarios libres no hacen falta nombres ni teléfonos: pedir solo lo necesario es más liviano
     * y no expone datos personales sin motivo.
     */
    @Query("select r.inicio from Reserva r where r.estado = :estado and r.inicio >= :desde and r.inicio < :hasta")
    List<Instant> buscarInicios(@Param("estado") EstadoReserva estado,
                                @Param("desde") Instant desde,
                                @Param("hasta") Instant hasta);

    /** Spring Data genera estas consultas a partir del nombre del método. */
    long countByTelefonoAndEstado(String telefono, EstadoReserva estado);

    boolean existsByTelefonoAndInicioAndEstado(String telefono, Instant inicio, EstadoReserva estado);

    Optional<Reserva> findByTokenCancelacion(UUID tokenCancelacion);

    // --- Panel del administrador ---

    /** Reservas de un estado que aún no han pasado, de la más próxima a la más lejana. */
    List<Reserva> findByEstadoAndInicioGreaterThanEqualOrderByInicioAsc(EstadoReserva estado, Instant desde);

    boolean existsByInicioAndEstado(Instant inicio, EstadoReserva estado);

    List<Reserva> findByInicioAndEstado(Instant inicio, EstadoReserva estado);

    /** Reservas de un estado dentro de [desde, hasta): sirve para ver qué choca con un bloqueo. */
    List<Reserva> findByEstadoAndInicioGreaterThanEqualAndInicioLessThan(EstadoReserva estado, Instant desde, Instant hasta);
}
