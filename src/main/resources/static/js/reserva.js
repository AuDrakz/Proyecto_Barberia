// Pantalla "Mi solicitud": se llega con el enlace que recibió el cliente (reserva.html?token=...).
import { api } from "./api.js";
import { el, vaciar, fechaLarga, infoEstado } from "./dom.js";

const $ = (id) => document.getElementById(id);
const token = new URLSearchParams(location.search).get("token");

function mostrarMensaje(texto, tipo = "error") {
  const caja = $("mensaje");
  caja.textContent = texto;
  caja.className = `aviso aviso-${tipo}`;
  caja.hidden = false;
}

function volverAReservar() {
  return el("a", { clase: "boton-secundario", href: "/", texto: "Pedir otra hora" });
}

function pintarNoEncontrada() {
  const contenido = $("contenido");
  vaciar(contenido);
  contenido.append(
    el("h2", { texto: "No encontramos esta solicitud" }),
    el("p", { clase: "suave", texto: "El enlace puede estar incompleto o ser incorrecto. Revisa que lo hayas copiado completo." }),
    el("div", { clase: "acciones" }, volverAReservar()));
}

function pintar(reserva) {
  const info = infoEstado(reserva.estado);
  const contenido = $("contenido");
  vaciar(contenido);

  contenido.append(
    el("h2", {}, "Estado: ", el("span", { clase: `badge ${info.clase}`, texto: info.texto })),
    el("p", { texto: info.explicacion }),
    el("dl", { clase: "detalle" },
      el("dt", { texto: "Día" }), el("dd", { texto: fechaLarga(reserva.inicio) }),
      el("dt", { texto: "Hora" }), el("dd", { texto: reserva.hora }),
      el("dt", { texto: "A nombre de" }), el("dd", { texto: reserva.nombreCliente })));

  const acciones = el("div", { clase: "acciones" });
  if (reserva.estado === "SOLICITADA" || reserva.estado === "CONFIRMADA") {
    acciones.append(el("button", { type: "button", clase: "boton-peligro", texto: "Cancelar mi hora", onclick: cancelar }));
  }
  acciones.append(volverAReservar());
  contenido.append(acciones);
}

async function cancelar(evento) {
  if (!window.confirm("¿Seguro que quieres cancelar tu hora?")) return;
  const boton = evento.currentTarget;
  boton.disabled = true;
  try {
    const reserva = await api.cancelar(token);
    $("mensaje").hidden = true;
    pintar(reserva);
    mostrarMensaje("Tu hora fue cancelada.", "ok");
  } catch (error) {
    boton.disabled = false;
    mostrarMensaje(error.message);
  }
}

async function cargar() {
  if (!token) {
    pintarNoEncontrada();
    return;
  }
  try {
    pintar(await api.ver(token));
  } catch (error) {
    if (error.estado === 404 || error.estado === 400) {
      pintarNoEncontrada();
    } else {
      vaciar($("contenido"));
      mostrarMensaje(error.message);
    }
  }
}

cargar();
