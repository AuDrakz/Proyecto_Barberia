package com.example.barberia_reservas.horario;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BloqueoRepository extends JpaRepository<Bloqueo, Long> {

    List<Bloqueo> findByFecha(LocalDate fecha);

    /** Bloqueos de hoy en adelante (los pasados ya no importan), en orden cronológico. */
    List<Bloqueo> findByFechaGreaterThanEqualOrderByFechaAscHoraInicioAsc(LocalDate desde);
}
