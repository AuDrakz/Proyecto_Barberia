// Piezas compartidas por las vistas del panel: acceso a elementos, avisos y errores de formulario.
import { el, vaciar } from "../dom.js";

export const $ = (id) => document.getElementById(id);

/** Puntos de conexión entre las vistas y la pantalla principal (admin.js los completa al arrancar). */
export const ctx = {
  negocio: { nombre: "", whatsapp: "" },
  sesionExpirada: () => {},
  irA: () => {},
  contadorPendientes: () => {},
};

/** Aviso arriba de la pantalla. "accion" es opcional: { texto, href } abre un enlace; { texto, alClick } ejecuta algo. */
export function mostrarMensaje(texto, tipo = "error", accion = null) {
  const caja = $("mensaje");
  vaciar(caja);
  caja.className = `aviso aviso-${tipo}`;
  caja.append(el("span", { texto }));
  if (accion) {
    caja.append(accion.href
      ? el("a", { clase: "boton-secundario boton-chico", href: accion.href, target: "_blank", rel: "noopener noreferrer", texto: accion.texto })
      : el("button", { type: "button", clase: "boton-secundario boton-chico", texto: accion.texto, onclick: accion.alClick }));
  }
  caja.hidden = false;
  caja.scrollIntoView({ block: "nearest" });
}

export function ocultarMensaje() {
  const caja = $("mensaje");
  caja.hidden = true;
  vaciar(caja);
}

/** Errores de la API: si la sesión expiró se vuelve al login; el resto se muestra tal cual. */
export function manejarError(error) {
  if (error.estado === 401) {
    ctx.sesionExpirada();
    return;
  }
  mostrarMensaje(error.message);
}

// ---- errores por campo en los formularios ----
// Convención: el campo "fecha" del formulario "blo" es el elemento id="blo-fecha" y su error va en id="error-blo-fecha".

export function limpiarErroresForm(prefijo, campos) {
  for (const campo of campos) {
    $(`error-${prefijo}-${campo}`).textContent = "";
    $(`${prefijo}-${campo}`).removeAttribute("aria-invalid");
  }
}

/** Muestra cada error junto a su campo. Devuelve true si mostró alguno. */
export function pintarErroresForm(prefijo, campos, errores) {
  let alguno = false;
  for (const campo of campos) {
    const mensaje = errores?.[campo];
    if (mensaje) {
      $(`error-${prefijo}-${campo}`).textContent = mensaje;
      $(`${prefijo}-${campo}`).setAttribute("aria-invalid", "true");
      alguno = true;
    }
  }
  return alguno;
}
