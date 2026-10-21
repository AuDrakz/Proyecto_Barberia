// Pantalla del cliente: elegir día -> elegir hora -> dejar datos -> solicitud enviada.
import { api } from "./api.js";
import { el, vaciar, fechaLarga, numeroDeDia, mesCorto, horaDe, infoEstado } from "./dom.js";

const CLAVE_GUARDADAS = "barberia.solicitudes"; // enlaces de solicitudes hechas desde este dispositivo
const CAMPOS = ["nombreCliente", "telefono", "instagram"];

const seleccion = { dia: null, inicio: null, hora: null };
let negocio = { nombre: "", whatsapp: "" };

const $ = (id) => document.getElementById(id);

// ---------- avisos ----------
function mostrarMensaje(texto, tipo = "error") {
  const caja = $("mensaje");
  caja.textContent = texto;
  caja.className = `aviso aviso-${tipo}`;
  caja.hidden = false;
  caja.scrollIntoView({ block: "nearest" });
}

function ocultarMensaje() {
  const caja = $("mensaje");
  caja.hidden = true;
  caja.textContent = "";
}

function limpiarErrores() {
  for (const campo of CAMPOS) {
    $(`error-${campo}`).textContent = "";
    $(campo).removeAttribute("aria-invalid");
  }
}

// ---------- almacenamiento local (puede no estar disponible: todo va en try/catch) ----------
function leerGuardadas() {
  try {
    const valor = JSON.parse(localStorage.getItem(CLAVE_GUARDADAS) || "[]");
    return Array.isArray(valor) ? valor.filter((t) => typeof t === "string") : [];
  } catch {
    return [];
  }
}

function escribirGuardadas(lista) {
  try {
    localStorage.setItem(CLAVE_GUARDADAS, JSON.stringify(lista.slice(0, 5)));
  } catch {
    // sin almacenamiento disponible: la página funciona igual, solo no recuerda las solicitudes
  }
}

function guardarToken(token) {
  escribirGuardadas([token, ...leerGuardadas().filter((t) => t !== token)]);
}

function olvidarToken(token) {
  escribirGuardadas(leerGuardadas().filter((t) => t !== token));
}

// ---------- marcar el botón elegido dentro de un grupo ----------
function marcar(contenedor, esElegido) {
  for (const boton of contenedor.children) {
    boton.setAttribute("aria-pressed", esElegido(boton) ? "true" : "false");
  }
}

// ---------- datos del negocio ----------
async function cargarNegocio() {
  try {
    negocio = await api.negocio();
    $("nombre-negocio").textContent = negocio.nombre;
    document.title = `Reservar hora · ${negocio.nombre}`;
  } catch {
    // si falla, se queda el título por defecto de la página
  }
}

// ---------- paso 1: días ----------
async function cargarDias() {
  const contenedor = $("dias");
  vaciar(contenedor);
  contenedor.append(el("p", { clase: "suave", texto: "Cargando días…" }));
  try {
    const { dias } = await api.resumen();
    vaciar(contenedor);
    if (dias.length === 0) {
      contenedor.append(el("p", { clase: "suave", texto: "Por ahora no hay horas disponibles. Vuelve a intentarlo más tarde." }));
      return;
    }
    for (const dia of dias) {
      contenedor.append(
        el("button", { type: "button", clase: "dia", "data-fecha": dia.fecha, "aria-pressed": "false", onclick: () => elegirDia(dia) },
          el("span", { clase: "dia-nombre", texto: dia.diaSemana.slice(0, 3) }),
          el("span", { clase: "dia-numero", texto: numeroDeDia(dia.fecha) }),
          el("span", { clase: "dia-mes", texto: mesCorto(dia.fecha) }),
          el("span", { clase: "dia-libres", texto: dia.libres === 1 ? "1 libre" : `${dia.libres} libres` })));
    }
    if (seleccion.dia) {
      marcar(contenedor, (boton) => boton.getAttribute("data-fecha") === seleccion.dia.fecha);
    }
  } catch (error) {
    vaciar(contenedor);
    contenedor.append(
      el("p", { clase: "suave", texto: error.message }),
      el("button", { type: "button", clase: "boton-secundario", texto: "Reintentar", onclick: cargarDias }));
  }
}

// ---------- paso 2: horas ----------
async function elegirDia(dia, conservarMensaje = false) {
  if (!conservarMensaje) ocultarMensaje();
  seleccion.dia = dia;
  seleccion.inicio = null;
  seleccion.hora = null;
  marcar($("dias"), (boton) => boton.getAttribute("data-fecha") === dia.fecha);

  $("paso-datos").hidden = true;
  $("paso-hora").hidden = false;
  $("titulo-horas").textContent = `2. Elige una hora · ${fechaLarga(dia.fecha)}`;

  const contenedor = $("horas");
  vaciar(contenedor);
  contenedor.append(el("p", { clase: "suave", texto: "Cargando horas…" }));
  try {
    const { horarios } = await api.horarios(dia.fecha);
    vaciar(contenedor);
    if (horarios.length === 0) {
      contenedor.append(el("p", { clase: "suave", texto: "Ese día ya no tiene horas libres. Elige otro día." }));
      return;
    }
    for (const horario of horarios) {
      contenedor.append(
        el("button", { type: "button", clase: "hora", "data-inicio": horario.inicio, "aria-pressed": "false", onclick: () => elegirHora(horario) },
          horario.hora));
    }
  } catch (error) {
    vaciar(contenedor);
    contenedor.append(
      el("p", { clase: "suave", texto: error.message }),
      el("button", { type: "button", clase: "boton-secundario", texto: "Reintentar", onclick: () => elegirDia(dia) }));
  }
}

// ---------- paso 3: datos ----------
function elegirHora(horario) {
  ocultarMensaje();
  seleccion.inicio = horario.inicio;
  seleccion.hora = horario.hora;
  marcar($("horas"), (boton) => boton.getAttribute("data-inicio") === horario.inicio);

  $("eleccion").textContent = `${fechaLarga(seleccion.dia.fecha)} a las ${horario.hora}`;
  $("paso-datos").hidden = false;
  $("nombreCliente").focus();
  $("paso-datos").scrollIntoView({ block: "nearest", behavior: "smooth" });
}

async function enviar(evento) {
  evento.preventDefault();
  ocultarMensaje();
  limpiarErrores();
  if (!seleccion.inicio) {
    mostrarMensaje("Elige primero un día y una hora.");
    return;
  }

  const boton = $("enviar");
  boton.disabled = true;
  boton.textContent = "Enviando…";
  try {
    const reserva = await api.solicitar({
      nombreCliente: $("nombreCliente").value,
      telefono: $("telefono").value,
      instagram: $("instagram").value,
      inicio: seleccion.inicio,
    });
    guardarToken(reserva.token);
    mostrarExito(reserva);
  } catch (error) {
    manejarError(error);
  } finally {
    boton.disabled = false;
    boton.textContent = "Solicitar hora";
  }
}

function manejarError(error) {
  const conDetalle = Object.entries(error.errores || {});
  if (error.estado === 400 && conDetalle.length > 0) {
    for (const [campo, mensaje] of conDetalle) {
      if (CAMPOS.includes(campo)) {
        $(`error-${campo}`).textContent = mensaje;
        $(campo).setAttribute("aria-invalid", "true");
      }
    }
    // "inicio" no tiene un campo propio en el formulario: se muestra arriba
    const mensajeInicio = error.errores?.inicio;
    mostrarMensaje(mensajeInicio ? mensajeInicio : "Revisa los datos marcados en rojo.");
    return;
  }

  mostrarMensaje(error.message);
  if (error.estado === 409 || error.estado === 400) {
    // El horario pudo haberse ocupado mientras llenabas el formulario: se refrescan las opciones.
    cargarDias();
    if (seleccion.dia) elegirDia(seleccion.dia, true);
  }
}

// ---------- solicitud enviada ----------
function mostrarExito(reserva) {
  const enlace = `${location.origin}/reserva.html?token=${encodeURIComponent(reserva.token)}`;
  for (const id of ["paso-dia", "paso-hora", "paso-datos", "mis-solicitudes"]) $(id).hidden = true;
  ocultarMensaje();

  const caja = $("exito");
  vaciar(caja);

  const botonCopiar = el("button", { type: "button", clase: "boton-secundario", texto: "Copiar enlace" });
  botonCopiar.addEventListener("click", () => copiar(enlace, botonCopiar));

  const acciones = el("div", { clase: "acciones" }, botonCopiar,
    el("a", { clase: "boton-secundario", href: enlace, texto: "Ver mi solicitud" }));

  if (negocio.whatsapp) {
    const texto = `Hola, soy ${reserva.nombreCliente}. Pedí hora para el ${fechaLarga(reserva.inicio)} a las ${reserva.hora}. Mi solicitud: ${enlace}`;
    acciones.append(el("a", {
      clase: "boton-secundario",
      href: `https://wa.me/${negocio.whatsapp}?text=${encodeURIComponent(texto)}`,
      target: "_blank",
      rel: "noopener noreferrer",
      texto: "Avisar por WhatsApp",
    }));
  }

  caja.append(
    el("h2", { texto: "¡Solicitud enviada!" }),
    el("p", { texto: `${fechaLarga(reserva.inicio)} a las ${reserva.hora}.` }),
    el("p", { clase: "suave", texto: "Tu hora todavía no está confirmada: el barbero revisará tu solicitud y te contactará." }),
    el("p", { texto: "Guarda este enlace. Es la única forma de ver el estado de tu solicitud o de cancelarla:" }),
    el("input", { clase: "enlace-caja", type: "text", readonly: true, value: enlace, "aria-label": "Enlace de tu solicitud" }),
    acciones,
    el("div", { clase: "acciones" },
      el("button", { type: "button", clase: "boton-secundario", texto: "Pedir otra hora", onclick: () => location.reload() })));
  caja.hidden = false;
  caja.scrollIntoView({ block: "start" });
}

async function copiar(texto, boton) {
  try {
    await navigator.clipboard.writeText(texto);
    boton.textContent = "¡Enlace copiado!";
  } catch {
    mostrarMensaje("No pudimos copiarlo automáticamente. Mantén presionado el enlace de arriba para copiarlo.", "aviso");
  }
}

// ---------- solicitudes hechas desde este dispositivo ----------
async function cargarMisSolicitudes() {
  const tokens = leerGuardadas();
  if (tokens.length === 0) return;

  const resultados = await Promise.all(tokens.map(async (token) => {
    try {
      return { token, reserva: await api.ver(token) };
    } catch (error) {
      if (error.estado === 404 || error.estado === 400) olvidarToken(token);
      return null;
    }
  }));
  const vigentes = resultados.filter(Boolean);
  if (vigentes.length === 0) return;

  const lista = $("lista-solicitudes");
  vaciar(lista);
  for (const { token, reserva } of vigentes) {
    const estado = infoEstado(reserva.estado);
    lista.append(el("li", {},
      el("a", { href: `/reserva.html?token=${encodeURIComponent(token)}`, texto: `${fechaLarga(reserva.inicio)} · ${horaDe(reserva.inicio)}` }),
      el("span", { clase: `badge ${estado.clase}`, texto: estado.texto })));
  }
  $("mis-solicitudes").hidden = false;
}

// ---------- arranque ----------
$("formulario").addEventListener("submit", enviar);
cargarNegocio();
cargarDias();
cargarMisSolicitudes();
