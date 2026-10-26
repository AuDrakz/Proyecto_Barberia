// Pestaña "Pendientes": solicitudes que esperan una respuesta del barbero.
import { adminApi } from "./adminApi.js";
import { el, vaciar, fechaLarga, enlaceWhatsapp } from "../dom.js";
import { $, ctx, mostrarMensaje, manejarError } from "./contexto.js";

export async function cargarPendientes() {
  const contenedor = $("lista-pendientes");
  vaciar(contenedor);
  contenedor.append(el("p", { clase: "suave", texto: "Cargando…" }));
  try {
    const reservas = await adminApi.reservas("SOLICITADA");
    ctx.contadorPendientes(reservas.length);
    vaciar(contenedor);
    if (reservas.length === 0) {
      contenedor.append(el("p", { clase: "suave", texto: "No hay solicitudes pendientes." }));
      return;
    }

    // Las solicitudes del MISMO horario se agrupan: al confirmar una, las demás se rechazan solas.
    const grupos = new Map();
    for (const reserva of reservas) {
      if (!grupos.has(reserva.inicio)) grupos.set(reserva.inicio, []);
      grupos.get(reserva.inicio).push(reserva);
    }
    for (const [inicio, lista] of grupos) {
      const grupo = el("section", { clase: "grupo" },
        el("h3", { clase: "grupo-titulo", texto: `${fechaLarga(inicio)} · ${lista[0].hora}` }));
      if (lista.length > 1) {
        grupo.append(el("p", { clase: "grupo-nota", texto: `Hay ${lista.length} solicitudes para este horario. Al confirmar una, las demás se rechazan automáticamente.` }));
      }
      for (const reserva of lista) grupo.append(tarjeta(reserva));
      contenedor.append(grupo);
    }
  } catch (error) {
    vaciar(contenedor);
    manejarError(error);
  }
}

function tarjeta(reserva) {
  const contacto = el("p", { clase: "suave" }, reserva.telefono);
  if (reserva.instagram) {
    contacto.append(" · ", el("a", {
      href: `https://www.instagram.com/${encodeURIComponent(reserva.instagram)}/`,
      target: "_blank",
      rel: "noopener noreferrer",
      texto: `@${reserva.instagram}`,
    }));
  }
  const saludo = `Hola ${reserva.nombreCliente}, te escribo por tu solicitud de hora del ${fechaLarga(reserva.inicio)} a las ${reserva.hora}. `;

  return el("article", { clase: "solicitud" },
    el("p", { clase: "solicitud-nombre", texto: reserva.nombreCliente }),
    contacto,
    el("div", { clase: "botones" },
      el("button", { type: "button", clase: "boton boton-chico", texto: "Confirmar", onclick: (e) => confirmar(reserva, e.currentTarget) }),
      el("button", { type: "button", clase: "boton-peligro boton-chico", texto: "Rechazar", onclick: (e) => rechazar(reserva, e.currentTarget) }),
      el("a", { clase: "boton-secundario boton-chico", href: enlaceWhatsapp(reserva.telefono, saludo), target: "_blank", rel: "noopener noreferrer", texto: "WhatsApp" })));
}

async function confirmar(reserva, boton) {
  boton.disabled = true;
  try {
    await adminApi.confirmar(reserva.id);
    const texto = `Hola ${reserva.nombreCliente}, tu hora del ${fechaLarga(reserva.inicio)} a las ${reserva.hora} quedó confirmada. ¡Te esperamos!`;
    mostrarMensaje(`Confirmaste la hora de ${reserva.nombreCliente}.`, "ok",
      { texto: "Avisar por WhatsApp", href: enlaceWhatsapp(reserva.telefono, texto) });
  } catch (error) {
    manejarError(error);
  }
  await cargarPendientes(); // se recarga siempre: aunque falle, el estado pudo haber cambiado
}

async function rechazar(reserva, boton) {
  if (!window.confirm(`¿Rechazar la solicitud de ${reserva.nombreCliente}?`)) return;
  boton.disabled = true;
  try {
    await adminApi.rechazar(reserva.id);
    const texto = `Hola ${reserva.nombreCliente}, lamentablemente no puedo tomar tu hora del ${fechaLarga(reserva.inicio)} a las ${reserva.hora}. ¿Quieres que busquemos otro horario?`;
    mostrarMensaje(`Rechazaste la solicitud de ${reserva.nombreCliente}.`, "aviso",
      { texto: "Avisar por WhatsApp", href: enlaceWhatsapp(reserva.telefono, texto) });
  } catch (error) {
    manejarError(error);
  }
  await cargarPendientes();
}
