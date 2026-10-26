// Utilidades para construir la pantalla y dar formato.
// Regla de seguridad: NUNCA se usa innerHTML con datos que vienen del servidor o del usuario.
// Todo texto entra con textContent (o como nodo de texto), así un nombre como "<script>" se muestra tal cual y no se ejecuta.

/** Crea un elemento: el("button", { clase: "boton", texto: "Hola", onclick: fn }, ...hijos) */
export function el(etiqueta, propiedades = {}, ...hijos) {
  const nodo = document.createElement(etiqueta);
  for (const [clave, valor] of Object.entries(propiedades)) {
    if (valor === undefined || valor === null || valor === false) continue;
    if (clave === "clase") nodo.className = valor;
    else if (clave === "texto") nodo.textContent = valor;
    else if (clave.startsWith("on") && typeof valor === "function") nodo.addEventListener(clave.slice(2), valor);
    else nodo.setAttribute(clave, valor === true ? "" : String(valor));
  }
  for (const hijo of hijos.flat()) {
    if (hijo === undefined || hijo === null || hijo === false) continue;
    nodo.append(hijo); // un texto se agrega como nodo de texto, no como HTML
  }
  return nodo;
}

export function vaciar(nodo) {
  nodo.replaceChildren();
}

const DIAS = ["domingo", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado"];
const MESES = ["enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"];

function partes(fechaIso) {
  const [anio, mes, dia] = fechaIso.slice(0, 10).split("-").map(Number);
  return { anio, mes, dia };
}

/** "2026-10-20" (o un instante ISO con desfase) -> "martes 20 de octubre". Se usa la fecha tal como viene, sin convertir zonas horarias. */
export function fechaLarga(fechaIso) {
  const { anio, mes, dia } = partes(fechaIso);
  const diaSemana = new Date(Date.UTC(anio, mes - 1, dia)).getUTCDay();
  return `${DIAS[diaSemana]} ${dia} de ${MESES[mes - 1]}`;
}

export function numeroDeDia(fechaIso) {
  return String(partes(fechaIso).dia);
}

export function mesCorto(fechaIso) {
  return MESES[partes(fechaIso).mes - 1].slice(0, 3);
}

/** "2026-10-20T19:00:00-03:00" -> "19:00" */
export function horaDe(instanteIso) {
  return instanteIso.slice(11, 16);
}

export const ESTADOS = {
  SOLICITADA: {
    texto: "Pendiente",
    clase: "badge-pendiente",
    explicacion: "El barbero todavía no responde tu solicitud. Hasta que la confirme, esta hora no está asegurada.",
  },
  CONFIRMADA: {
    texto: "Confirmada",
    clase: "badge-ok",
    explicacion: "¡Tu hora está confirmada! Si no puedes asistir, cancélala desde esta página para liberar el horario.",
  },
  RECHAZADA: {
    texto: "No disponible",
    clase: "badge-error",
    explicacion: "El barbero no pudo tomar esta hora. Puedes pedir otra distinta.",
  },
  CANCELADA: {
    texto: "Cancelada",
    clase: "badge-neutro",
    explicacion: "Esta hora fue cancelada. Puedes pedir otra cuando quieras.",
  },
};

export function infoEstado(estado) {
  return ESTADOS[estado] ?? { texto: estado, clase: "badge-neutro", explicacion: "" };
}

/** Nombres de los días de la semana, en el orden de la API: 1 = lunes ... 7 = domingo. */
export const NOMBRES_DIA = ["lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"];

/** Enlace que abre WhatsApp con un chat y un texto ya escrito (el usuario decide si lo envía). */
export function enlaceWhatsapp(telefono, texto) {
  const digitos = String(telefono).replace(/\D/g, "");
  return `https://wa.me/${digitos}?text=${encodeURIComponent(texto)}`;
}

/** La fecha de hoy del dispositivo, como AAAA-MM-DD (sirve para el atributo min de un campo de fecha). */
export function hoyIso() {
  const ahora = new Date();
  const dos = (n) => String(n).padStart(2, "0");
  return `${ahora.getFullYear()}-${dos(ahora.getMonth() + 1)}-${dos(ahora.getDate())}`;
}
