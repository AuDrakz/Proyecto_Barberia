// Llamadas a la API de administración. Se encarga del token CSRF:
// la cookie XSRF-TOKEN la deja el servidor y cada acción que modifica algo debe devolverla en el encabezado X-XSRF-TOKEN.
import { pedir, ErrorApi } from "../api.js";

function tokenCsrf() {
  const par = document.cookie.split("; ").find((c) => c.startsWith("XSRF-TOKEN="));
  return par ? decodeURIComponent(par.slice("XSRF-TOKEN=".length)) : null;
}

/** Pide la sesión solo para que el servidor deje (o renueve) la cookie del token CSRF. */
async function renovarToken() {
  await pedir("/api/admin/sesion");
}

async function pedirAdmin(ruta, opciones = {}) {
  const modifica = (opciones.metodo || "GET") !== "GET";
  if (modifica && !tokenCsrf()) {
    await renovarToken();
  }
  try {
    return await pedir(ruta, { ...opciones, csrf: modifica ? tokenCsrf() : undefined });
  } catch (error) {
    // Un 403 puede ser un token vencido: se renueva y se reintenta UNA sola vez.
    if (modifica && error.estado === 403) {
      await renovarToken();
      return pedir(ruta, { ...opciones, csrf: tokenCsrf() });
    }
    throw error;
  }
}

async function iniciarSesion(email, password) {
  if (!tokenCsrf()) {
    await renovarToken();
  }
  const enviar = () => pedir("/api/admin/login", { metodo: "POST", formulario: { email, password }, csrf: tokenCsrf() });
  try {
    try {
      await enviar();
    } catch (error) {
      if (error.estado !== 403) throw error;
      await renovarToken(); // token vencido: se renueva y se reintenta una vez
      await enviar();
    }
  } catch (error) {
    if (error.estado === 401) throw new ErrorApi(401, "Correo o contraseña incorrectos.");
    if (error.estado === 403) throw new ErrorApi(403, "No pudimos validar el inicio de sesión. Recarga la página e inténtalo de nuevo.");
    throw error;
  }
  await renovarToken(); // tras iniciar sesión el servidor renueva el token
}

export const adminApi = {
  sesion: () => pedir("/api/admin/sesion"),
  login: iniciarSesion,
  logout: () => pedirAdmin("/api/admin/logout", { metodo: "POST" }),

  reservas: (estado) => pedirAdmin(`/api/admin/reservas?estado=${encodeURIComponent(estado)}`),
  confirmar: (id) => pedirAdmin(`/api/admin/reservas/${encodeURIComponent(id)}/confirmar`, { metodo: "POST" }),
  rechazar: (id) => pedirAdmin(`/api/admin/reservas/${encodeURIComponent(id)}/rechazar`, { metodo: "POST" }),
  cancelar: (id) => pedirAdmin(`/api/admin/reservas/${encodeURIComponent(id)}/cancelar`, { metodo: "POST" }),

  disponibilidad: () => pedirAdmin("/api/admin/disponibilidad"),
  crearDisponibilidad: (datos) => pedirAdmin("/api/admin/disponibilidad", { metodo: "POST", cuerpo: datos }),
  eliminarDisponibilidad: (id) => pedirAdmin(`/api/admin/disponibilidad/${encodeURIComponent(id)}`, { metodo: "DELETE" }),

  bloqueos: () => pedirAdmin("/api/admin/bloqueos"),
  crearBloqueo: (datos) => pedirAdmin("/api/admin/bloqueos", { metodo: "POST", cuerpo: datos }),
  eliminarBloqueo: (id) => pedirAdmin(`/api/admin/bloqueos/${encodeURIComponent(id)}`, { metodo: "DELETE" }),
};
