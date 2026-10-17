package com.example.barberia_reservas.horario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.example.barberia_reservas.common.ConflictoDeNegocioException;
import com.example.barberia_reservas.common.NoEncontradoException;
import com.example.barberia_reservas.common.SolicitudInvalidaException;
import com.example.barberia_reservas.config.ReservasProperties;

class DisponibilidadAdminServiceTest {

    private final DisponibilidadRepository repositorio = mock(DisponibilidadRepository.class);
    private final DisponibilidadAdminService servicio = new DisponibilidadAdminService(
            repositorio, new ReservasProperties("America/Santiago", 60, 1, 30, 2));

    private static Disponibilidad tramo(int dia, int horaIni, int horaFin) {
        return Disponibilidad.crear((short) dia, LocalTime.of(horaIni, 0), LocalTime.of(horaFin, 0));
    }

    @Test
    void creaUnTramoSinSuperposiciones() {
        when(repositorio.findByDiaSemana((short) 2)).thenReturn(List.of());
        when(repositorio.save(any(Disponibilidad.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Disponibilidad creada = servicio.crear(2, "19:00", "22:00");

        assertEquals(LocalTime.of(19, 0), creada.getHoraInicio());
        verify(repositorio).save(any(Disponibilidad.class));
    }

    @Test
    void rechazaUnTramoQueSeSuperponeConOtroDelMismoDia() {
        when(repositorio.findByDiaSemana((short) 2)).thenReturn(List.of(tramo(2, 19, 22)));

        assertThrows(ConflictoDeNegocioException.class, () -> servicio.crear(2, "21:00", "23:00"));
        verify(repositorio, never()).save(any(Disponibilidad.class));
    }

    @Test
    void permiteUnTramoQueSoloTocaElBordeDeOtro() {
        when(repositorio.findByDiaSemana((short) 2)).thenReturn(List.of(tramo(2, 19, 21)));
        when(repositorio.save(any(Disponibilidad.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Disponibilidad creada = servicio.crear(2, "21:00", "23:00");

        assertEquals(LocalTime.of(21, 0), creada.getHoraInicio());
    }

    @Test
    void losDatosInvalidosSeRechazanAntesDeTocarLaBaseDeDatos() {
        assertThrows(SolicitudInvalidaException.class, () -> servicio.crear(9, "19:00", "22:00"));
        verify(repositorio, never()).save(any(Disponibilidad.class));
    }

    @Test
    void eliminaUnTramoExistente() {
        Disponibilidad existente = tramo(2, 19, 22);
        when(repositorio.findById(1L)).thenReturn(Optional.of(existente));

        servicio.eliminar(1L);

        verify(repositorio).delete(existente);
    }

    @Test
    void eliminarUnTramoInexistenteDaNoEncontrado() {
        when(repositorio.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class, () -> servicio.eliminar(99L));
    }
}
