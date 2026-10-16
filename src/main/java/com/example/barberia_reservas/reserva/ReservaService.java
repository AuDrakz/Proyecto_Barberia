package com.example.barberia_reservas.reserva;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.barberia_reservas.common.ConflictoDeNegocioException;
import com.example.barberia_reservas.common.ReservaNoEncontradaException;
import com.example.barberia_reservas.config.ReservasProperties;
import com.example.barberia_reservas.horario.HorariosService;

/**
 * Casos de uso del CLIENTE: solicitar una hora, ver su solicitud y cancelarla (con el enlace/token).
 * El administrador (confirmar y rechazar) llega en la etapa 4.
 */
@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final HorariosService horariosService;
    private final Clock clock;
    private final ZoneId zona;
    private final int maxSolicitudesPendientes;
    private final ValidadorSolicitud validador = new ValidadorSolicitud();

    public ReservaService(ReservaRepository reservaRepository,
                          HorariosService horariosService,
                          ReservasProperties propiedades,
                          Clock clock) {
        this.reservaRepository = reservaRepository;
        this.horariosService = horariosService;
        this.clock = clock;
        this.zona = ZoneId.of(propiedades.zonaHoraria());
        this.maxSolicitudesPendientes = propiedades.maxSolicitudesPendientesPorTelefono();
    }

    @Transactional
    public Reserva solicitar(String nombre, String telefono, String instagram, String inicioTexto) {
        // 1) Validar y normalizar lo que escribió el cliente (lanza 400 si hay algo mal).
        SolicitudNormalizada solicitud = validador.validar(nombre, telefono, instagram, inicioTexto);

        // 2) El horario pedido DEBE estar en la lista de horarios libres. Se recalcula en el servidor:
        //    nunca se confía en que el navegador solo manda horas que mostró la pantalla.
        //    (Si la fecha está fuera de la ventana de anticipación, horariosLibres lanza el error correspondiente.)
        LocalDate fecha = solicitud.inicio().atZone(zona).toLocalDate();
        boolean estaEnLaLista = horariosService.horariosLibres(fecha).stream()
                .anyMatch(horario -> horario.inicio().toInstant().equals(solicitud.inicio()));
        if (!estaEnLaLista) {
            throw new ConflictoDeNegocioException("Ese horario no está disponible. Elige otro de la lista.");
        }

        // 3) Defensas contra las "horas fantasma": sin solicitudes repetidas y con un máximo de pendientes por teléfono.
        if (reservaRepository.existsByTelefonoAndInicioAndEstado(
                solicitud.telefono(), solicitud.inicio(), EstadoReserva.SOLICITADA)) {
            throw new ConflictoDeNegocioException("Ya tienes una solicitud pendiente para ese horario.");
        }
        long pendientes = reservaRepository.countByTelefonoAndEstado(solicitud.telefono(), EstadoReserva.SOLICITADA);
        if (pendientes >= maxSolicitudesPendientes) {
            throw new ConflictoDeNegocioException("Ya tienes " + pendientes
                    + " solicitudes pendientes. Espera a que se respondan o cancela alguna.");
        }

        // 4) Crear. Nace SOLICITADA: todavía NO bloquea el horario (solo CONFIRMADA bloquea).
        Reserva reserva = Reserva.solicitar(solicitud.nombreCliente(), solicitud.telefono(),
                solicitud.instagram(), solicitud.inicio(), clock.instant());
        return reservaRepository.save(reserva);
    }

    @Transactional(readOnly = true)
    public Reserva obtener(UUID token) {
        return buscar(token);
    }

    @Transactional
    public Reserva cancelarPorCliente(UUID token) {
        Reserva reserva = buscar(token);
        Instant ahora = clock.instant();
        if (!reserva.getInicio().isAfter(ahora)) {
            throw new ConflictoDeNegocioException("No se puede cancelar una hora que ya pasó.");
        }
        reserva.cancelar(CanceladaPor.CLIENTE, ahora);
        // No hace falta save(): la entidad está "gestionada" dentro de la transacción y JPA guarda el cambio al terminar.
        return reserva;
    }

    private Reserva buscar(UUID token) {
        return reservaRepository.findByTokenCancelacion(token).orElseThrow(ReservaNoEncontradaException::new);
    }
}
