// Pestaña "Agenda": las citas confirmadas que vienen, agrupadas por día.
import { adminApi } from "./adminApi.js";
import { el, vaciar, fechaLarga, enlaceWhatsapp } from "../dom.js";
import { $, mostrarMensaje, manejarError } from "./contexto.js";

export async function cargarAgenda() {
  const contenedor = $("lista-agenda");
  vaciar(contenedor);
  contenedor.append(el("p", { clase: "suave", texto: "Cargando…" }));
  try {
    const reservas = await adminApi.reservas("CONFIRMADA");
    vaciar(contenedor);
    if (reservas.length === 0) {
      contenedor.append(el("p", { clase: "suave", texto: "No hay citas confirmadas próximas." }));
      return;
    }
    const dias = new Map();
    for (const reserva of reservas) {
      const dia = reserva.inicio.slice(0, 10);
      if (!dias.has(dia)) dias.set(dia, []);
      dias.get(dia).push(reserva);
    }
    for (const [dia, lista] of dias) {
      const grupo = el("section", { clase: "grupo" }, el("h3", { clase: "grupo-titulo", texto: fechaLarga(dia) }));
      for (const reserva of lista) grupo.append(fila(reserva));
      contenedor.append(grupo);
    }
  } catch (error) {
    vaciar(contenedor);
    manejarError(error);
  }
}

function fila(reserva) {
  const instagram = reserva.instagram ? ` · @${reserva.instagram}` : "";
  return el("div", { clase: "fila-panel" },
    el("div", { clase: "fila-texto" },
      el("strong", { texto: `${reserva.hora} · ${reserva.nombreCliente}` }),
      el("div", { clase: "suave", texto: `${reserva.telefono}${instagram}` })),
    el("div", { clase: "botones" },
      el("a", { clase: "boton-secundario boton-chico", href: enlaceWhatsapp(reserva.telefono, `Hola ${reserva.nombreCliente}, sobre tu hora del ${fechaLarga(reserva.inicio)} a las ${reserva.hora}: `), target: "_blank", rel: "noopener noreferrer", texto: "WhatsApp" }),
      el("button", { type: "button", clase: "boton-peligro boton-chico", texto: "Cancelar", onclick: (e) => cancelar(reserva, e.currentTarget) })));
}

async function cancelar(reserva, boton) {
  if (!window.confirm(`¿Cancelar la hora de ${reserva.nombreCliente}? El horario quedará libre para otros clientes.`)) return;
  boton.disabled = true;
  try {
    await adminApi.cancelar(reserva.id);
    const texto = `Hola ${reserva.nombreCliente}, tengo que cancelar tu hora del ${fechaLarga(reserva.inicio)} a las ${reserva.hora}. Lo siento mucho. ¿Quieres que busquemos otro horario?`;
    mostrarMensaje(`Cancelaste la hora de ${reserva.nombreCliente}.`, "aviso",
      { texto: "Avisar por WhatsApp", href: enlaceWhatsapp(reserva.telefono, texto) });
  } catch (error) {
    manejarError(error);
  }
  await cargarAgenda();
}
