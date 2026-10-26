// Pantalla principal del panel: inicio de sesión y pestañas.
import { api } from "../api.js";
import { adminApi } from "./adminApi.js";
import { $, ctx, mostrarMensaje, ocultarMensaje, manejarError } from "./contexto.js";
import { cargarPendientes } from "./pendientes.js";
import { cargarAgenda } from "./agenda.js";
import { cargarDisponibilidad, iniciarFormularioDisponibilidad } from "./disponibilidad.js";
import { cargarBloqueos, iniciarFormularioBloqueos } from "./bloqueos.js";

const PESTANAS = ["pendientes", "agenda", "disponibilidad", "bloqueos"];
const CARGADORES = {
  pendientes: cargarPendientes,
  agenda: cargarAgenda,
  disponibilidad: cargarDisponibilidad,
  bloqueos: cargarBloqueos,
};

async function irA(nombre) {
  ocultarMensaje();
  for (const pestana of PESTANAS) {
    const activa = pestana === nombre;
    $(`tab-${pestana}`).setAttribute("aria-selected", activa ? "true" : "false");
    $(`tab-${pestana}`).setAttribute("tabindex", activa ? "0" : "-1");
    $(`panel-${pestana}`).hidden = !activa;
  }
  await CARGADORES[nombre]();
}

function mostrarLogin(mensaje = null, tipo = "aviso") {
  $("vista-app").hidden = true;
  $("vista-login").hidden = false;
  ocultarMensaje();
  if (mensaje) mostrarMensaje(mensaje, tipo);
  $("login-email").focus();
}

function mostrarApp(email) {
  $("vista-login").hidden = true;
  $("vista-app").hidden = false;
  $("usuario").textContent = email;
  irA("pendientes");
}

async function enviarLogin(evento) {
  evento.preventDefault();
  ocultarMensaje();
  const boton = $("login-enviar");
  boton.disabled = true;
  try {
    await adminApi.login($("login-email").value, $("login-clave").value);
    $("login-clave").value = "";
    const sesion = await adminApi.sesion();
    if (sesion.autenticado) {
      mostrarApp(sesion.email);
    } else {
      mostrarMensaje("No pudimos iniciar la sesión. Inténtalo de nuevo.");
    }
  } catch (error) {
    $("login-clave").value = "";
    mostrarMensaje(error.message);
  } finally {
    boton.disabled = false;
  }
}

async function cerrarSesion() {
  try {
    await adminApi.logout();
  } catch (error) {
    if (error.estado !== 401) {
      manejarError(error);
      return;
    }
  }
  mostrarLogin("Cerraste sesión.", "ok");
}

function activarPestanas() {
  PESTANAS.forEach((pestana, indice) => {
    const boton = $(`tab-${pestana}`);
    boton.addEventListener("click", () => irA(pestana));
    // Flechas izquierda y derecha para moverse entre pestañas (accesibilidad con teclado)
    boton.addEventListener("keydown", (evento) => {
      const paso = evento.key === "ArrowRight" ? 1 : evento.key === "ArrowLeft" ? -1 : 0;
      if (paso === 0) return;
      evento.preventDefault();
      const siguiente = PESTANAS[(indice + paso + PESTANAS.length) % PESTANAS.length];
      $(`tab-${siguiente}`).focus();
      irA(siguiente);
    });
  });
}

async function iniciar() {
  ctx.sesionExpirada = () => mostrarLogin("Tu sesión expiró. Vuelve a iniciar sesión.");
  ctx.irA = irA;
  ctx.contadorPendientes = (cantidad) => {
    const contador = $("contador-pendientes");
    contador.textContent = String(cantidad);
    contador.hidden = cantidad === 0;
  };

  $("form-login").addEventListener("submit", enviarLogin);
  $("cerrar-sesion").addEventListener("click", cerrarSesion);
  activarPestanas();
  iniciarFormularioDisponibilidad();
  iniciarFormularioBloqueos();

  try {
    ctx.negocio = await api.negocio();
    $("nombre-negocio").textContent = `${ctx.negocio.nombre} · Panel`;
    document.title = `Panel · ${ctx.negocio.nombre}`;
  } catch {
    // si falla, se queda el título por defecto
  }

  try {
    const sesion = await adminApi.sesion();
    if (sesion.autenticado) {
      mostrarApp(sesion.email);
    } else {
      mostrarLogin();
    }
  } catch (error) {
    mostrarLogin();
    mostrarMensaje(error.message);
  }
}

iniciar();
