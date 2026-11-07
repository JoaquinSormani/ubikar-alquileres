"use strict";

/*
 * Actualizar valor de alquiler: primera versión del frontend.
 *
 * La actualización es AUTOMÁTICA: el backend la aplica solo cuando llega la fecha de ajuste de cada contrato
 * (tarea programada). Esta pantalla no actualiza nada: muestra lo que ya se aplicó, lo que viene, y deja avisar
 * al inquilino por WhatsApp (el contrato dice que la corredora le comunica el nuevo valor).
 *
 * Contrato con el backend (ver docs/Actualizar_Valor_Alquiler_Guia.md):
 *   GET /api/actualizaciones/panel -> { aplicadas: [...], proximas: [...] }
 *   (el filtro por mes se hace acá, con periodoDesde; las próximas se ven al elegir un mes)
 *
 * Mientras el servidor no tenga esa ruta (responde 404), la pantalla funciona en MODO DEMOSTRACIÓN con datos de
 * ejemplo. Apenas el backend la implemente, pasa sola a usar los datos reales.
 * Con ?demo en la dirección se fuerza el modo demostración (por si no hay servidor ni internet).
 */

const $ = (id) => document.getElementById(id);
const dinero = new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" });
const decimal = new Intl.NumberFormat("es-AR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

let panel = { aplicadas: [], proximas: [] };
let demo = false;

// ---------- datos de demostración (personas y direcciones inventadas; índices reales del BCRA a noviembre de 2026) ----------

const CAMILA = { nombre: "Camila", apellido: "Rojas", dni: "30111222", telefono: "2664111222" };
const DIEGO = { nombre: "Diego", apellido: "Pereyra", dni: "28333444", telefono: "" };
const JUAN = { nombre: "Juan", apellido: "Perez", dni: "30555123", telefono: "2664001122" };
const VALERIA = { nombre: "Valeria", apellido: "Sosa", dni: "33555666", telefono: "2664333444" };

const DEMO = {
  aplicadas: [
    { id: 3, fecha: "2026-10-15", idContrato: 101, direccion: "Belgrano 1250, 2.º B", inquilinos: [CAMILA], indice: "ICL",
      periodoDesde: "2026-10", periodicidad: 3, valorAnterior: 275818.25, valorNuevo: 292818.58, porcentaje: 6.16 },
    { id: 2, fecha: "2026-10-01", idContrato: 102, direccion: "Colón 480, depto 1", inquilinos: [DIEGO], indice: "IPC",
      periodoDesde: "2026-10", periodicidad: 3, valorAnterior: 180000, valorNuevo: 190455.48, porcentaje: 5.81 },
    { id: 1, fecha: "2026-07-15", idContrato: 101, direccion: "Belgrano 1250, 2.º B", inquilinos: [CAMILA], indice: "ICL",
      periodoDesde: "2026-07", periodicidad: 3, valorAnterior: 250000, valorNuevo: 275818.25, porcentaje: 10.33 },
  ],
  proximas: [
    { idContrato: 103, direccion: "Junin 700, depto 3", inquilinos: [JUAN], indice: "ICL", periodicidad: 3,
      fechaAjuste: "2026-12-01", periodoDesde: "2026-12", valorActual: 350621.31, estado: "PROGRAMADA" },
    { idContrato: 104, direccion: "San Martín 95, local 3", inquilinos: [VALERIA], indice: "IPC", periodicidad: 3,
      fechaAjuste: "2026-12-01", periodoDesde: "2026-12", valorActual: 320000, estado: "PROGRAMADA" },
    { idContrato: 102, direccion: "Colón 480, depto 1", inquilinos: [DIEGO], indice: "IPC", periodicidad: 3,
      fechaAjuste: "2027-01-01", periodoDesde: "2027-01", valorActual: 190455.48, estado: "PROGRAMADA" },
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
  tr.append(celdaAviso(a.inquilinos, (nombre) => `Hola ${nombre}, te avisamos que desde ${mesTexto(a.periodoDesde)} el alquiler de ${a.direccion} `
    + `pasa a ser de ${dinero.format(a.valorNuevo)} (aumento del ${porcentaje(a.porcentaje)} por ${a.indice}). — UBIKAR Propiedades`));
  return tr;
}

// Botón "Avisar": abre WhatsApp con el mensaje armado (no se envía solo). Sin teléfono, lo aclara.
function celdaAviso(inquilinos, armarMensaje) {
  const conTelefono = inquilinos.find((i) => telefonoWhatsApp(i.telefono));
  if (!conTelefono) return celda([el("span", "pago__sub", "sin teléfono")], "pago__num");
  const avisar = el("a", "btn btn--ghost", "Avisar");
  avisar.href = `https://wa.me/${telefonoWhatsApp(conTelefono.telefono)}?text=${encodeURIComponent(armarMensaje(conTelefono.nombre))}`;
  avisar.target = "_blank";
  avisar.rel = "noopener";
  avisar.setAttribute("aria-label", `Avisar por WhatsApp a ${conTelefono.nombre} ${conTelefono.apellido}`);
  return celda([avisar], "pago__num");
}

// Una actualización que todavía no se aplicó: se muestra el valor actual y cuándo se va a calcular el nuevo.
function filaProxima(p) {
  const espera = p.estado === "ESPERANDO_INDICE";
  const tr = el("tr");
  tr.append(celda([el("span", "", fechaTexto(p.fechaAjuste)), el("span", "pago__sub", espera ? "esperando el índice" : "se calcula ese día")]),
    celda(quien(p)), celda(p.direccion),
    celda([el("span", `act__indice act__indice--${p.indice.toLowerCase()}`, p.indice)]), celda(mesTexto(p.periodoDesde)));
  tr.append(celda(dinero.format(p.valorActual), "pago__num"));
  tr.append(celda([el("span", espera ? "act__estado act__estado--espera" : "act__estado act__estado--programada", espera ? "Esperando el índice" : "Programada"),
    ...(p.periodicidad ? [el("span", "pago__sub", `actualización ${nombrePeriodo(p.periodicidad)}`)] : [])], "pago__num"));
  tr.append(celdaAviso(p.inquilinos, (nombre) => `Hola ${nombre}, te avisamos que desde ${mesTexto(p.periodoDesde)} el alquiler de ${p.direccion} `
    + `se actualiza por ${p.indice} (${nombrePeriodo(p.periodicidad)}). Te informamos el nuevo valor apenas se calcule. — UBIKAR Propiedades`));
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

// "AAAA-MM" del mes que viene.
function mesSiguiente() {
  const h = new Date();
  const s = new Date(h.getFullYear(), h.getMonth() + 1, 1);
  return `${s.getFullYear()}-${String(s.getMonth() + 1).padStart(2, "0")}`;
}

// El selector ofrece los meses con actualizaciones (aplicadas o programadas) y siempre el mes que viene,
// del más nuevo al más viejo.
function llenarMeses() {
  const elegido = $("mes").value;
  const proximo = mesSiguiente();
  // Del futuro solo se ofrece el mes que viene (los más lejanos no interesan); sí se ofrece cualquier mes pasado,
  // aunque tenga una actualización esperando el índice.
  const otros = [...new Set([...panel.aplicadas.map((a) => a.periodoDesde), ...(panel.proximas || []).map((p) => p.periodoDesde)])]
    .filter((m) => m < proximo).sort().reverse();
  const meses = [proximo, ...otros]; // el mes que viene va primero
  const select = $("mes");
  select.innerHTML = "";
  select.append(new Option("Todos los meses", ""));
  meses.forEach((m) => select.append(new Option(`${capitalizar(mesTexto(m))}${m === proximo ? " · próximo mes" : ""}`, m)));
  select.value = meses.includes(elegido) ? elegido : "";
}

function pintarTablas() {
  const consulta = $("buscador").value;
  const mes = $("mes").value;
  const aplicadas = panel.aplicadas.filter((f) => (!mes || f.periodoDesde === mes) && coincide(f, consulta));
  // Las que todavía no se aplicaron se ven al elegir un mes o al buscar (con "Todos los meses" y sin búsqueda solo va el historial).
  const buscando = consulta.trim() !== "";
  const proximas = mes || buscando ? (panel.proximas || []).filter((f) => (!mes || f.periodoDesde === mes) && coincide(f, consulta)) : [];
  const filas = [...aplicadas.map((f) => ({ f, aplicada: true })), ...proximas.map((f) => ({ f, aplicada: false }))];
  let vacio = "Todavía no se aplicó ninguna actualización.";
  if (buscando) vacio = "Ninguna actualización coincide con la búsqueda.";
  else if (mes) vacio = mes >= mesSiguiente() ? `No hay actualizaciones programadas para ${mesTexto(mes)}.` : `No se aplicaron actualizaciones en ${mesTexto(mes)}.`;
  pintarTabla($("tbAplic"), filas, (x) => (x.aplicada ? filaAplicada(x.f) : filaProxima(x.f)), $("vacioAplic"), vacio);
  $("cantAplic").textContent = filas.length;
  $("conteo").textContent = buscando || mes ? `${filas.length} de ${panel.aplicadas.length + (panel.proximas || []).length} actualizaciones` : "";
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
