package com.example.barberia_reservas.horario;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DisponibilidadRepository extends JpaRepository<Disponibilidad, Long> {

    /** Spring Data genera la consulta a partir del nombre del método: WHERE dia_semana = ? */
    List<Disponibilidad> findByDiaSemana(short diaSemana);

    List<Disponibilidad> findAllByOrderByDiaSemanaAscHoraInicioAsc();
}
