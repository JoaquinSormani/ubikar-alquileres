"use strict";

/*
 * Visualizar actualizaciones.
 *
 * La actualización es AUTOMÁTICA: el backend la aplica solo cuando llega la fecha de ajuste de cada contrato
 * (tarea programada). Esta pantalla no actualiza nada: muestra lo que ya se aplicó, lo que viene, y deja avisar
 * al inquilino por WhatsApp (el contrato dice que la corredora le comunica el nuevo valor).
 *
 * Contrato con el backend (ver docs/Actualizar_Valor_Alquiler_Guia.md):
 *   GET /api/actualizaciones/panel                  -> { fecha, proximoMes, meses: ["AAAA-MM", ...] }
 *   GET /api/actualizaciones/aplicadas?mes&q&despues&tamanio  -> { filas, total, tamanio, siguiente }
 *   GET /api/actualizaciones/proximas?mes&q&despues&tamanio   -> { filas, total, tamanio, siguiente }
 * Las dos tablas se piden de a una página, con el mismo cursor que Registrar pago.
 */

const $ = (id) => document.getElementById(id);
const dinero = new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" });
const decimal = new Intl.NumberFormat("es-AR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

let panel = { meses: [], proximoMes: "" };
let temporizadorBusqueda = null;

// Las dos tablas (aplicadas y próximas) se piden de a una página al servidor. Cada página trae el cursor de la
// siguiente (la última fila vista), así que una actualización nueva no repite ni saltea filas en las otras páginas.
// "cursores[i]" es con qué se pide la página i (null = la primera).
function crearLista(config) {
  return { ...config, datos: { filas: [], total: 0, siguiente: null }, cursores: [null], pagina: 0, pedido: 0 };
}

const aplicadas = crearLista({
  ruta: "/actualizaciones/aplicadas", tamanio: 10, fila: filaAplicada,
  cuerpo: "tbAplic", vacio: "vacioAplic", cantidad: "cantAplic",
  paginas: "paginasAplic", rango: "rangoAplic", numero: "paginaAplic", anterior: "anteriorAplic", siguiente: "siguienteAplic",
  textoVacio: (buscando, mes) => buscando ? "Ninguna actualización aplicada coincide con la búsqueda."
    : mes ? `No se aplicaron actualizaciones en ${mesTexto(mes)}.` : "Todavía no se aplicó ninguna actualización.",
});

const proximas = crearLista({
  ruta: "/actualizaciones/proximas", tamanio: 5, fila: filaProxima,
  cuerpo: "tbProx", vacio: "vacioProx", cantidad: "cantProx",
  paginas: "paginasProx", rango: "rangoProx", numero: "paginaProx", anterior: "anteriorProx", siguiente: "siguienteProx",
  textoVacio: (buscando, mes) => buscando ? "Ninguna próxima actualización coincide con la búsqueda."
    : mes ? `No hay actualizaciones programadas para ${mesTexto(mes)}.` : "No hay actualizaciones próximas.",
});

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

function pintarLista(lista) {
  const buscando = $("buscador").value.trim() !== "";
  const mes = $("mes").value;
  const { filas, total, siguiente } = lista.datos;
  pintarTabla($(lista.cuerpo), filas, lista.fila, $(lista.vacio), lista.textoVacio(buscando, mes));
  $(lista.cantidad).textContent = total;
  const desde = lista.pagina * lista.tamanio + 1;
  const totalPaginas = Math.max(1, Math.ceil(total / lista.tamanio), lista.pagina + 1);
  $(lista.paginas).hidden = filas.length === 0 || (lista.pagina === 0 && !siguiente);
  $(lista.rango).textContent = filas.length ? `Mostrando ${desde}–${desde + filas.length - 1} de ${total}` : "";
  $(lista.numero).textContent = `Página ${lista.pagina + 1} de ${totalPaginas}`;
  $(lista.anterior).disabled = lista.pagina === 0;
  $(lista.siguiente).disabled = !siguiente;
  $("conteo").textContent = buscando
    ? `${aplicadas.datos.total} ${plural(aplicadas.datos.total, "aplicada", "aplicadas")} · ${proximas.datos.total} ${plural(proximas.datos.total, "próxima", "próximas")} que coinciden`
    : "";
}

// El selector ofrece el mes que viene (primero) y los meses con actualizaciones aplicadas o esperando el índice (los manda
// el servidor), del más nuevo al más viejo.
function llenarMeses() {
  const select = $("mes");
  const elegido = select.value;
  select.innerHTML = "";
  select.append(new Option("Todos los meses", ""));
  (panel.meses || []).forEach((m) => select.append(new Option(`${capitalizar(mesTexto(m))}${m === panel.proximoMes ? " · próximo mes" : ""}`, m)));
  select.value = (panel.meses || []).includes(elegido) ? elegido : "";
}

async function cargarPanel() {
  try {
    const r = await pedir("/actualizaciones/panel");
    if (!r.ok) throw new Error("panel");
    $("offline").hidden = true;
    panel = r.cuerpo;
    llenarMeses();
  } catch (err) {
    $("offline").hidden = false;
  }
}

// Pide una página de una de las tablas. Con "reiniciar" vuelve a la primera (cambió el mes o la búsqueda).
async function cargarLista(lista, reiniciar) {
  if (reiniciar) {
    lista.cursores = [null];
    lista.pagina = 0;
  }
  const mio = ++lista.pedido;
  const params = new URLSearchParams({ tamanio: lista.tamanio });
  const mes = $("mes").value;
  if (mes) params.set("mes", mes);
  const texto = $("buscador").value.trim();
  if (texto) params.set("q", texto);
  if (lista.cursores[lista.pagina]) params.set("despues", lista.cursores[lista.pagina]);
  try {
    const r = await pedir(`${lista.ruta}?${params}`);
    if (mio !== lista.pedido) return; // mientras tanto se pidió otra cosa: esta respuesta ya no vale
    if (!r.ok) throw new Error(lista.ruta);
    $("offline").hidden = true;
    lista.datos = r.cuerpo;
    if (lista.datos.siguiente) lista.cursores[lista.pagina + 1] = lista.datos.siguiente;
    else lista.cursores.length = lista.pagina + 1;
    pintarLista(aplicadas);
    pintarLista(proximas);
  } catch (err) {
    $("offline").hidden = false;
  }
}

function cargarListas() {
  cargarLista(aplicadas, true);
  cargarLista(proximas, true);
}

async function cargarTodo() {
  await cargarPanel();
  cargarListas();
}

// ---------- eventos ----------

$("buscador").addEventListener("input", () => {
  clearTimeout(temporizadorBusqueda);
  temporizadorBusqueda = setTimeout(cargarListas, 300); // se busca en el servidor, sin pedir con cada letra
});
$("mes").addEventListener("change", cargarListas);
[aplicadas, proximas].forEach((lista) => {
  $(lista.anterior).addEventListener("click", () => {
    if (lista.pagina > 0) {
      lista.pagina--;
      cargarLista(lista, false);
    }
  });
  $(lista.siguiente).addEventListener("click", () => {
    if (lista.datos.siguiente) {
      lista.pagina++;
      cargarLista(lista, false);
    }
  });
});

cargarTodo();
