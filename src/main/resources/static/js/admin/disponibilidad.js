// Pestaña "Horarios": los días y horas en que atiende el barbero (se repiten cada semana).
import { adminApi } from "./adminApi.js";
import { el, vaciar, NOMBRES_DIA } from "../dom.js";
import { $, mostrarMensaje, ocultarMensaje, manejarError, limpiarErroresForm, pintarErroresForm } from "./contexto.js";

const CAMPOS = ["diaSemana", "horaInicio", "horaFin"];

export async function cargarDisponibilidad() {
  const contenedor = $("lista-disponibilidad");
  vaciar(contenedor);
  contenedor.append(el("p", { clase: "suave", texto: "Cargando…" }));
  try {
    const tramos = await adminApi.disponibilidad();
    vaciar(contenedor);
    if (tramos.length === 0) {
      contenedor.append(el("p", { clase: "suave", texto: "Todavía no hay horarios de atención. Agrega el primero abajo." }));
      return;
    }
    for (let dia = 1; dia <= 7; dia++) {
      const delDia = tramos.filter((t) => t.diaSemana === dia);
      if (delDia.length === 0) continue;
      const grupo = el("section", { clase: "grupo" }, el("h3", { clase: "grupo-titulo", texto: NOMBRES_DIA[dia - 1] }));
      for (const tramo of delDia) {
        grupo.append(el("div", { clase: "fila-panel" },
          el("div", { clase: "fila-texto", texto: `${tramo.horaInicio} – ${tramo.horaFin}` }),
          el("button", { type: "button", clase: "boton-peligro boton-chico", texto: "Eliminar", onclick: (e) => eliminar(tramo, e.currentTarget) })));
      }
      contenedor.append(grupo);
    }
  } catch (error) {
    vaciar(contenedor);
    manejarError(error);
  }
}

async function eliminar(tramo, boton) {
  const cual = `${NOMBRES_DIA[tramo.diaSemana - 1]} de ${tramo.horaInicio} a ${tramo.horaFin}`;
  if (!window.confirm(`¿Eliminar el horario del ${cual}? Los clientes dejarán de ver esas horas. Las reservas ya confirmadas no se tocan.`)) return;
  boton.disabled = true;
  try {
    await adminApi.eliminarDisponibilidad(tramo.id);
    mostrarMensaje("Horario eliminado.", "ok");
  } catch (error) {
    manejarError(error);
  }
  await cargarDisponibilidad();
}

export function iniciarFormularioDisponibilidad() {
  $("form-disponibilidad").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    ocultarMensaje();
    limpiarErroresForm("disp", CAMPOS);
    const boton = $("disp-enviar");
    boton.disabled = true;
    try {
      await adminApi.crearDisponibilidad({
        diaSemana: Number($("disp-diaSemana").value),
        horaInicio: $("disp-horaInicio").value,
        horaFin: $("disp-horaFin").value,
      });
      mostrarMensaje("Horario agregado.", "ok");
      await cargarDisponibilidad();
    } catch (error) {
      if (error.estado === 400 && pintarErroresForm("disp", CAMPOS, error.errores)) {
        mostrarMensaje("Revisa los campos marcados en rojo.");
      } else {
        manejarError(error); // por ejemplo un 409 "se superpone con otro tramo"
      }
    } finally {
      boton.disabled = false;
    }
  });
}
