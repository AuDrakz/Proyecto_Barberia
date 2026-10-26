// Pestaña "Bloqueos": días u horas puntuales en que el barbero NO atiende ("este martes no puedo").
import { adminApi } from "./adminApi.js";
import { el, vaciar, fechaLarga, hoyIso } from "../dom.js";
import { $, ctx, mostrarMensaje, ocultarMensaje, manejarError, limpiarErroresForm, pintarErroresForm } from "./contexto.js";

const CAMPOS = ["fecha", "horaInicio", "horaFin", "motivo"];

export async function cargarBloqueos() {
  const contenedor = $("lista-bloqueos");
  vaciar(contenedor);
  contenedor.append(el("p", { clase: "suave", texto: "Cargando…" }));
  try {
    const bloqueos = await adminApi.bloqueos();
    vaciar(contenedor);
    if (bloqueos.length === 0) {
      contenedor.append(el("p", { clase: "suave", texto: "No hay bloqueos próximos." }));
      return;
    }
    for (const bloqueo of bloqueos) {
      const cuando = bloqueo.diaCompleto ? "Todo el día" : `${bloqueo.horaInicio} – ${bloqueo.horaFin}`;
      contenedor.append(el("div", { clase: "fila-panel" },
        el("div", { clase: "fila-texto" },
          el("strong", { texto: fechaLarga(bloqueo.fecha) }),
          el("div", { clase: "suave", texto: bloqueo.motivo ? `${cuando} · ${bloqueo.motivo}` : cuando })),
        el("button", { type: "button", clase: "boton-peligro boton-chico", texto: "Eliminar", onclick: (e) => eliminar(bloqueo, e.currentTarget) })));
    }
  } catch (error) {
    vaciar(contenedor);
    manejarError(error);
  }
}

async function eliminar(bloqueo, boton) {
  if (!window.confirm(`¿Eliminar el bloqueo del ${fechaLarga(bloqueo.fecha)}? Esas horas volverán a ofrecerse a los clientes.`)) return;
  boton.disabled = true;
  try {
    await adminApi.eliminarBloqueo(bloqueo.id);
    mostrarMensaje("Bloqueo eliminado.", "ok");
  } catch (error) {
    manejarError(error);
  }
  await cargarBloqueos();
}

export function iniciarFormularioBloqueos() {
  $("blo-fecha").min = hoyIso();
  $("blo-todo").addEventListener("change", () => {
    $("blo-horas").hidden = $("blo-todo").checked;
  });

  $("form-bloqueos").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    ocultarMensaje();
    limpiarErroresForm("blo", CAMPOS);
    const boton = $("blo-enviar");
    boton.disabled = true;

    const cuerpo = { fecha: $("blo-fecha").value, motivo: $("blo-motivo").value };
    if (!$("blo-todo").checked) {
      cuerpo.horaInicio = $("blo-horaInicio").value;
      cuerpo.horaFin = $("blo-horaFin").value;
    }
    try {
      const resultado = await adminApi.crearBloqueo(cuerpo);
      const pendientes = resultado.solicitudesPendientesAfectadas ?? 0;
      if (pendientes > 0) {
        const cuantas = pendientes === 1 ? "hay 1 solicitud pendiente" : `hay ${pendientes} solicitudes pendientes`;
        mostrarMensaje(`Bloqueo creado. Atención: ${cuantas} dentro de este bloqueo. Revísala(s) y recházala(s) si corresponde.`, "aviso",
          { texto: "Ver pendientes", alClick: () => ctx.irA("pendientes") });
      } else {
        mostrarMensaje("Bloqueo creado.", "ok");
      }
      $("blo-fecha").value = "";
      $("blo-motivo").value = "";
      await cargarBloqueos();
    } catch (error) {
      if (error.estado === 400 && pintarErroresForm("blo", CAMPOS, error.errores)) {
        mostrarMensaje("Revisa los campos marcados en rojo.");
      } else {
        manejarError(error); // por ejemplo un 409 que nombra las reservas confirmadas que chocan
      }
    } finally {
      boton.disabled = false;
    }
  });
}
