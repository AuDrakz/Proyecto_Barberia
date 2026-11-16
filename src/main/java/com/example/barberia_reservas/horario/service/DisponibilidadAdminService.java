package com.example.barberia_reservas.horario;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.barberia_reservas.common.ConflictoDeNegocioException;
import com.example.barberia_reservas.common.NoEncontradoException;
import com.example.barberia_reservas.config.ReservasProperties;

/** El administrador define en qué días y horas atiende (la disponibilidad semanal). */
@Service
public class DisponibilidadAdminService {

    private final DisponibilidadRepository repositorio;
    private final ValidadorHorarioAdmin validador;

    public DisponibilidadAdminService(DisponibilidadRepository repositorio, ReservasProperties propiedades) {
        this.repositorio = repositorio;
        this.validador = new ValidadorHorarioAdmin(propiedades.duracionMinutos());
    }

    @Transactional(readOnly = true)
    public List<Disponibilidad> listar() {
        return repositorio.findAllByOrderByDiaSemanaAscHoraInicioAsc();
    }

    @Transactional
    public Disponibilidad crear(Integer diaSemana, String horaInicio, String horaFin) {
        NuevaDisponibilidad nueva = validador.validarDisponibilidad(diaSemana, horaInicio, horaFin);

        // Dos tramos se superponen si cada uno empieza antes de que termine el otro.
        // Si solo se "tocan" (19:00-20:00 y 20:00-21:00) NO se superponen y es válido.
        for (Disponibilidad existente : repositorio.findByDiaSemana(nueva.diaSemana())) {
            boolean seSuperpone = existente.getHoraInicio().isBefore(nueva.horaFin())
                    && nueva.horaInicio().isBefore(existente.getHoraFin());
            if (seSuperpone) {
                throw new ConflictoDeNegocioException("Se superpone con otro tramo de ese día ("
                        + existente.getHoraInicio() + " a " + existente.getHoraFin() + "). Elimínalo primero o ajusta las horas.");
            }
        }
        return repositorio.save(Disponibilidad.crear(nueva.diaSemana(), nueva.horaInicio(), nueva.horaFin()));
    }

    /**
     * Eliminar un tramo solo deja de OFRECER esas horas hacia adelante. Las reservas ya confirmadas no se tocan:
     * si hay que anular un día concreto, se usa un bloqueo, que sí revisa las reservas confirmadas.
     */
    @Transactional
    public void eliminar(Long id) {
        Disponibilidad disponibilidad = repositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe ese tramo de disponibilidad."));
        repositorio.delete(disponibilidad);
    }
}
