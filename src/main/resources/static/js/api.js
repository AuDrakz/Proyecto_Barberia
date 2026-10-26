// Todas las llamadas al servidor viven aquí. Las pantallas no usan fetch directamente:
// así hay un solo lugar donde se manejan los errores de red y el formato de errores de la API.

export class ErrorApi extends Error {
  constructor(estado, mensaje, errores = {}) {
    super(mensaje);
    this.estado = estado;   // código HTTP (0 = no hubo conexión)
    this.errores = errores; // { campo: mensaje } cuando la API devuelve 400 con detalle por campo
  }
}

function mensajeGenerico(estado) {
  if (estado === 404) return "No encontramos lo que buscabas.";
  if (estado === 401) return "Tu sesión no está iniciada o expiró.";
  if (estado >= 500) return "Tuvimos un problema de nuestro lado. Inténtalo de nuevo en unos minutos.";
  return "No pudimos completar la operación.";
}

/**
 * Hace la llamada y devuelve el JSON (o null si la respuesta viene vacía, como un 200 sin cuerpo o un 204).
 * opciones: { metodo, cuerpo (se envía como JSON), formulario (se envía como formulario), csrf (token CSRF) }
 */
export async function pedir(ruta, { metodo = "GET", cuerpo, formulario, csrf } = {}) {
  const opciones = { method: metodo, headers: { Accept: "application/json" } };
  if (cuerpo !== undefined) {
    opciones.headers["Content-Type"] = "application/json";
    opciones.body = JSON.stringify(cuerpo);
  } else if (formulario !== undefined) {
    opciones.headers["Content-Type"] = "application/x-www-form-urlencoded";
    opciones.body = new URLSearchParams(formulario).toString();
  }
  if (csrf) {
    opciones.headers["X-XSRF-TOKEN"] = csrf;
  }

  let respuesta;
  try {
    respuesta = await fetch(ruta, opciones);
  } catch {
    throw new ErrorApi(0, "No pudimos conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.");
  }

  const texto = await respuesta.text();
  let datos = null;
  try {
    datos = texto ? JSON.parse(texto) : null;
  } catch {
    datos = null;
  }

  if (!respuesta.ok) {
    // La API responde errores con el formato estándar ProblemDetail: { title, detail, errores? }
    const detalle = datos && typeof datos.detail === "string" ? datos.detail : mensajeGenerico(respuesta.status);
    const errores = datos && datos.errores && typeof datos.errores === "object" ? datos.errores : {};
    throw new ErrorApi(respuesta.status, detalle, errores);
  }
  return datos;
}

export const api = {
  negocio: () => pedir("/api/negocio"),
  resumen: () => pedir("/api/horarios/resumen"),
  horarios: (fecha) => pedir(`/api/horarios?fecha=${encodeURIComponent(fecha)}`),
  solicitar: (datos) => pedir("/api/reservas", { metodo: "POST", cuerpo: datos }),
  ver: (token) => pedir(`/api/reservas/${encodeURIComponent(token)}`),
  cancelar: (token) => pedir(`/api/reservas/${encodeURIComponent(token)}/cancelar`, { metodo: "POST" }),
};
