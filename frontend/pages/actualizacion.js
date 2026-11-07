"use strict";

/*
 * Actualizar valor de alquiler: primera versión del frontend.
 *
 * La actualización es AUTOMÁTICA: el backend la aplica solo cuando llega la fecha de ajuste de cada contrato
 * (tarea programada). Esta pantalla no actualiza nada: muestra lo que ya se aplicó, lo que viene, y deja avisar
 * al inquilino por WhatsApp (el contrato dice que la corredora le comunica el nuevo valor).
 *
 * Contrato con el backend (ver docs/Actualizar_Valor_Alquiler_Guia.md):
 *   GET /api/actualizaciones/panel -> { aplicadas: [...] }   (el filtro por mes se hace acá, con periodoDesde)
 *
 * Mientras el servidor no tenga esa ruta (responde 404), la pantalla funciona en MODO DEMOSTRACIÓN con datos de
 * ejemplo. Apenas el backend la implemente, pasa sola a usar los datos reales.
 * Con ?demo en la dirección se fuerza el modo demostración (por si no hay servidor ni internet).
 */

const $ = (id) => document.getElementById(id);
const dinero = new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" });
const decimal = new Intl.NumberFormat("es-AR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

let panel = { aplicadas: [] };
let demo = false;

// ---------- datos de demostración (personas y direcciones inventadas; índices reales del BCRA a noviembre de 2026) ----------

const CAMILA = { nombre: "Camila", apellido: "Rojas", dni: "30111222", telefono: "2664111222" };
const DIEGO = { nombre: "Diego", apellido: "Pereyra", dni: "28333444", telefono: "" };

const DEMO = {
  aplicadas: [
    { id: 3, fecha: "2026-10-15", idContrato: 101, direccion: "Belgrano 1250, 2.º B", inquilinos: [CAMILA], indice: "ICL",
      periodoDesde: "2026-10", periodicidad: 3, valorAnterior: 275818.25, valorNuevo: 292818.58, porcentaje: 6.16 },
    { id: 2, fecha: "2026-10-01", idContrato: 102, direccion: "Colón 480, depto 1", inquilinos: [DIEGO], indice: "IPC",
      periodoDesde: "2026-10", periodicidad: 3, valorAnterior: 180000, valorNuevo: 190455.48, porcentaje: 5.81 },
    { id: 1, fecha: "2026-07-15", idContrato: 101, direccion: "Belgrano 1250, 2.º B", inquilinos: [CAMILA], indice: "ICL",
      periodoDesde: "2026-07", periodicidad: 3, valorAnterior: 250000, valorNuevo: 275818.25, porcentaje: 10.33 },
  ],
};

// ---------- utilidades ----------

function el(tag, clase, texto) {
  const nodo = document.createElement(tag);
  if (clase) nodo.className = clase;
  if (texto !== undefined) nodo.textContent = texto;
  return nodo;
}

function soloDigitos(valor) {
  return (valor || "").replace(/\D/g, "");
}

// Sin mayúsculas ni tildes, para que "jose" encuentre a "José".
function limpiar(texto) {
  return (texto || "").toLowerCase().normalize("NFD").replace(/[̀-ͯ]/g, "").trim();
}

function fechaTexto(iso) {
  const [a, m, d] = iso.split("-");
  return `${d}/${m}/${a}`;
}

// "2026-07" -> "julio de 2026"
function mesTexto(periodo) {
  const [a, m] = periodo.split("-").map(Number);
  return new Date(a, m - 1, 1).toLocaleDateString("es-AR", { month: "long", year: "numeric" });
}

function nombres(inquilinos) {
  return inquilinos.map((i) => `${i.nombre} ${i.apellido}`).join(" y ");
}

// Cada cuántos meses se actualiza el contrato: aclara de qué período es el porcentaje (1 mes no es lo mismo que 6).
function nombrePeriodo(meses) {
  const nombres = { 1: "mensual", 2: "bimestral", 3: "trimestral", 4: "cuatrimestral", 6: "semestral", 12: "anual" };
  return nombres[meses] || `cada ${meses} meses`;
}

function plural(n, uno, varios) {
  return n === 1 ? uno : varios;
}

function porcentaje(valor) {
  return `${decimal.format(valor)} %`;
}

async function pedir(ruta, opciones) {
  const respuesta = await fetch(`${API}${ruta}`, opciones);
  const cuerpo = await respuesta.json().catch(() => ({}));
  return { ok: respuesta.ok, estado: respuesta.status, cuerpo };
}

// Número de WhatsApp en formato internacional argentino (549 + área + número, sin 0 ni 15).
function telefonoWhatsApp(telefono) {
  const n = soloDigitos(telefono).replace(/^0+/, "");
  if (n.length < 8) return "";
  return n.startsWith("54") ? n : `549${n}`;
}

// ---------- buscador en vivo ----------

// El DNI coincide por el principio; el nombre y la propiedad, en cualquier parte.
function coincide(fila, consulta) {
  if (!consulta) return true;
  const digitos = soloDigitos(consulta);
  const texto = limpiar(consulta);
  return fila.inquilinos.some((i) => (digitos && soloDigitos(i.dni).startsWith(digitos))
      || limpiar(`${i.nombre} ${i.apellido}`).includes(texto))
    || limpiar(fila.direccion).includes(texto);
}

function celda(contenido, clase) {
  const td = el("td", clase);
  if (typeof contenido === "string") td.textContent = contenido;
  else td.append(...contenido);
  return td;
}

function quien(fila) {
  return [el("span", "pago__quien", nombres(fila.inquilinos)),
    el("span", "pago__sub", fila.inquilinos.map((i) => `DNI ${i.dni}`).join(" · "))];
}

// ---------- filas ----------

function filaAplicada(a) {
  const tr = el("tr");
  tr.append(celda(fechaTexto(a.fecha)), celda(quien(a)), celda(a.direccion), celda(a.indice === "ICL" || a.indice === "IPC" ? [el("span", `act__indice act__indice--${a.indice.toLowerCase()}`, a.indice)] : a.indice));
  tr.append(celda(mesTexto(a.periodoDesde)));
  tr.append(celda(dinero.format(a.valorAnterior), "pago__num"));
  tr.append(celda([el("span", "", dinero.format(a.valorNuevo)), el("span", "pago__sub act__aumento", `+ ${porcentaje(a.porcentaje)}${a.periodicidad ? ` · ${nombrePeriodo(a.periodicidad)}` : ""}`)], "pago__num"));
  const avisar = el("a", "btn btn--ghost", "Avisar");
  const conTelefono = a.inquilinos.find((i) => telefonoWhatsApp(i.telefono));
  if (conTelefono) {
    const mensaje = `Hola ${conTelefono.nombre}, te avisamos que desde ${mesTexto(a.periodoDesde)} el alquiler de ${a.direccion} `
      + `pasa a ser de ${dinero.format(a.valorNuevo)} (aumento del ${porcentaje(a.porcentaje)} por ${a.indice}). — UBIKAR Propiedades`;
    avisar.href = `https://wa.me/${telefonoWhatsApp(conTelefono.telefono)}?text=${encodeURIComponent(mensaje)}`;
    avisar.target = "_blank";
    avisar.rel = "noopener";
    avisar.setAttribute("aria-label", `Avisar por WhatsApp a ${conTelefono.nombre} ${conTelefono.apellido} el nuevo valor`);
    tr.append(celda([avisar], "pago__num"));
  } else {
    tr.append(celda([el("span", "pago__sub", "sin teléfono")], "pago__num"));
  }
  return tr;
}

function pintarTabla(cuerpo, filas, crear, vacio, textoVacio) {
  cuerpo.innerHTML = "";
  filas.forEach((f) => cuerpo.append(crear(f)));
  cuerpo.closest(".pago__scroll").hidden = filas.length === 0;
  vacio.hidden = filas.length !== 0;
  vacio.textContent = textoVacio;
}

function capitalizar(texto) {
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

// El selector ofrece los meses (periodoDesde) en los que hubo actualizaciones, del más nuevo al más viejo.
function llenarMeses() {
  const elegido = $("mes").value;
  const meses = [...new Set(panel.aplicadas.map((a) => a.periodoDesde))].sort().reverse();
  const select = $("mes");
  select.innerHTML = "";
  select.append(new Option("Todos los meses", ""));
  meses.forEach((m) => select.append(new Option(capitalizar(mesTexto(m)), m)));
  select.value = meses.includes(elegido) ? elegido : "";
}

function pintarTablas() {
  const consulta = $("buscador").value;
  const mes = $("mes").value;
  const aplicadas = panel.aplicadas.filter((f) => (!mes || f.periodoDesde === mes) && coincide(f, consulta));
  const buscando = consulta.trim() !== "";
  let vacio = "Todavía no se aplicó ninguna actualización.";
  if (buscando) vacio = "Ninguna actualización coincide con la búsqueda.";
  else if (mes) vacio = `No se aplicaron actualizaciones en ${mesTexto(mes)}.`;
  pintarTabla($("tbAplic"), aplicadas, filaAplicada, $("vacioAplic"), vacio);
  $("cantAplic").textContent = aplicadas.length;
  $("conteo").textContent = buscando || mes ? `${aplicadas.length} de ${panel.aplicadas.length} actualizaciones` : "";
}

async function cargarPanel() {
  if (new URLSearchParams(location.search).has("demo")) {
    demo = true;
    panel = DEMO;
    $("demo").hidden = false;
    llenarMeses();
    pintarTablas();
    return;
  }
  try {
    let r;
    try {
      r = await pedir("/actualizaciones/panel");
    } catch (err) {
      // Una ruta que no existe responde 404 sin cabeceras CORS y el navegador lo ve como una caída de red.
      // Para distinguirlo de un backend apagado se prueba una ruta que sí existe.
      await pedir("/pagos/panel"); // si también falla, el backend está apagado y se muestra el aviso
      r = { ok: false, estado: 404, cuerpo: {} };
    }
    $("offline").hidden = true;
    if (r.ok) {
      demo = false;
      panel = r.cuerpo;
    } else if (r.estado === 404 || r.estado === 405) {
      // el servidor está andando pero todavía no tiene este caso de uso
      demo = true;
      panel = DEMO;
    } else {
      throw new Error("panel");
    }
    $("demo").hidden = !demo;
    llenarMeses();
    pintarTablas();
  } catch (err) {
    $("offline").hidden = false;
  }
}

$("buscador").addEventListener("input", pintarTablas);
$("mes").addEventListener("change", pintarTablas);

cargarPanel();
