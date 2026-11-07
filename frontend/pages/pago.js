"use strict";

const MEDIOS = [["EFECTIVO", "Efectivo"], ["TRANSFERENCIA", "Transferencia"], ["TARJETA_CREDITO", "Tarjeta de crédito"]];
const ETIQUETA_MEDIO = Object.fromEntries(MEDIOS);
const $ = (id) => document.getElementById(id);
const dinero = new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" });

let panel = { pendientes: [], realizados: [], meses: [] };
let filaEnCobro = null;
let temporizadorFecha = null;

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

// Fecha de hoy en hora local (YYYY-MM-DD), para el calendario y para comparar.
function hoyISO() {
  const h = new Date();
  const dos = (n) => String(n).padStart(2, "0");
  return `${h.getFullYear()}-${dos(h.getMonth() + 1)}-${dos(h.getDate())}`;
}

function fechaTexto(iso) {
  const [a, m, d] = iso.split("-");
  return `${d}/${m}/${a}`;
}

function capitalizar(texto) {
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

// "2026-07" -> "julio de 2026"
function mesTexto(periodo) {
  const [a, m] = periodo.split("-").map(Number);
  return new Date(a, m - 1, 1).toLocaleDateString("es-AR", { month: "long", year: "numeric" });
}

function nombres(inquilinos) {
  return inquilinos.map((i) => `${i.nombre} ${i.apellido}`).join(" y ");
}

function plural(n, uno, varios) {
  return n === 1 ? uno : varios;
}

async function pedir(ruta, opciones) {
  const respuesta = await fetch(`${API}${ruta}`, opciones);
  const cuerpo = await respuesta.json().catch(() => ({}));
  return { ok: respuesta.ok, estado: respuesta.status, cuerpo };
}

// ---------- buscador en vivo ----------

// Con cada letra o número se acotan las filas: el DNI coincide por el principio; el nombre y la propiedad, en cualquier parte.
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
  const nombre = el("span", "pago__quien", nombres(fila.inquilinos));
  const dni = el("span", "pago__sub", fila.inquilinos.map((i) => `DNI ${i.dni}`).join(" · "));
  return [nombre, dni];
}

function filaPendiente(f) {
  const tr = el("tr");
  tr.append(celda(quien(f)), celda(f.direccion), celda(capitalizar(f.periodoTexto)));
  const vence = [el("span", "", fechaTexto(f.fechaVencimiento))];
  vence.push(el("span", f.diasAtraso > 0 ? "pago__sub pago__mora" : "pago__sub",
    f.diasAtraso > 0 ? `${f.diasAtraso} ${plural(f.diasAtraso, "día", "días")} de atraso` : "en término"));
  tr.append(celda(vence));
  tr.append(celda(dinero.format(f.monto), "pago__num"));
  tr.append(celda(f.diasAtraso > 0 ? dinero.format(f.punitorio) : "—", f.diasAtraso > 0 ? "pago__num pago__mora" : "pago__num"));
  tr.append(celda(dinero.format(f.total), "pago__num"));
  const cobrar = el("button", "btn btn--primary", "Cobrar");
  cobrar.type = "button";
  cobrar.setAttribute("aria-label", `Cobrar ${f.periodoTexto} de ${nombres(f.inquilinos)}`);
  cobrar.addEventListener("click", () => abrirCobro(f));
  tr.append(celda([cobrar], "pago__num"));
  return tr;
}

function filaRealizada(f) {
  const tr = el("tr");
  tr.append(celda(String(f.numeroRecibo ?? f.idPago).padStart(8, "0")), celda(fechaTexto(f.fechaPago)), celda(quien(f)), celda(f.direccion), celda(capitalizar(f.periodoTexto)),
    celda(ETIQUETA_MEDIO[f.medioPago] || "—"));
  const total = [el("span", "", dinero.format(f.total))];
  if (f.diasAtraso > 0) total.push(el("span", "pago__sub pago__mora", `incluye ${dinero.format(f.punitorio)} de punitorio`));
  tr.append(celda(total, "pago__num"));
  const recibo = el("a", "btn btn--ghost", "Recibo");
  recibo.href = `${API}/pagos/${f.idPago}/recibo`;
  recibo.target = "_blank";
  recibo.rel = "noopener";
  recibo.setAttribute("aria-label", `Ver el recibo N.º ${f.numeroRecibo ?? f.idPago}, ${f.periodoTexto} de ${nombres(f.inquilinos)}`);
  tr.append(celda([recibo], "pago__num"));
  return tr;
}

function pintarTabla(cuerpo, filas, crear, vacio, textoVacio) {
  cuerpo.innerHTML = "";
  filas.forEach((f) => cuerpo.append(crear(f)));
  cuerpo.closest(".pago__scroll").hidden = filas.length === 0;
  vacio.hidden = filas.length !== 0;
  vacio.textContent = textoVacio;
}

function pintarTablas() {
  const consulta = $("buscador").value;
  const pendientes = panel.pendientes.filter((f) => coincide(f, consulta));
  const realizados = panel.realizados.filter((f) => coincide(f, consulta));
  const mes = $("mes").value;
  const buscando = consulta.trim() !== "";
  pintarTabla($("tbPend"), pendientes, filaPendiente, $("vacioPend"),
    buscando ? "Ningún cobro pendiente coincide con la búsqueda."
      : mes ? `No hay pagos pendientes de ${mesTexto(mes)}.` : "No hay pagos pendientes: todos los contratos están al día.");
  pintarTabla($("tbReal"), realizados, filaRealizada, $("vacioReal"),
    buscando ? "Ningún pago coincide con la búsqueda."
      : mes ? `Todavía no se registraron pagos de ${mesTexto(mes)}.` : "Todavía no se registraron pagos.");
  $("cantPend").textContent = pendientes.length;
  $("cantReal").textContent = realizados.length;
  $("conteo").textContent = buscando
    ? `${pendientes.length} de ${panel.pendientes.length} pendientes · ${realizados.length} de ${panel.realizados.length} realizados`
    : "";
}

// El selector ofrece los meses que tienen cobros (los manda el servidor), del más nuevo al más viejo.
function llenarMeses() {
  const select = $("mes");
  const elegido = select.value;
  select.innerHTML = "";
  select.append(new Option("Todos los meses", ""));
  (panel.meses || []).forEach((m) => select.append(new Option(capitalizar(mesTexto(m)), m)));
  select.value = (panel.meses || []).includes(elegido) ? elegido : "";
}

async function cargarPanel() {
  try {
    const mes = $("mes").value;
    const r = await pedir(`/pagos/panel${mes ? `?mes=${mes}` : ""}`);
    if (!r.ok) throw new Error("panel");
    $("offline").hidden = true;
    panel = r.cuerpo;
    llenarMeses();
    pintarTablas();
  } catch (err) {
    $("offline").hidden = false;
  }
}

// ---------- cuadro para cobrar ----------

function pintarCalculo(c) {
  $("calcAlquiler").textContent = dinero.format(filaEnCobro.monto);
  const hayMora = c.diasAtraso > 0;
  $("filaPunitorio").hidden = !hayMora;
  $("filaPunitorio").classList.toggle("pago__fila--mora", hayMora);
  $("calcPunitorioEtiqueta").textContent = `Punitorio por mora (${c.diasAtraso} ${plural(c.diasAtraso, "día", "días")} × 1 %)`;
  $("calcPunitorio").textContent = dinero.format(c.punitorio);
  $("calcTotal").textContent = dinero.format(c.total);
}

function pintarMedios() {
  const contenedor = document.querySelector("#medioPago .radios__opciones");
  contenedor.innerHTML = "";
  MEDIOS.forEach(([valor, texto]) => {
    const etiqueta = el("label", "radio");
    const input = el("input");
    input.type = "radio";
    input.name = "medioPago";
    input.value = valor;
    input.checked = valor === "EFECTIVO";
    const cuerpo = el("span", "radio__cuerpo");
    cuerpo.append(el("span", "radio__texto", texto));
    etiqueta.append(input, cuerpo);
    contenedor.append(etiqueta);
  });
}

function medioElegido() {
  const marcado = document.querySelector('input[name="medioPago"]:checked');
  return marcado ? marcado.value : "";
}

function mostrarErrorCobro(texto) {
  $("cobroError").textContent = texto;
  $("cobroError").hidden = !texto;
}

function abrirCobro(fila) {
  filaEnCobro = fila;
  $("cobroResumen").textContent = `${nombres(fila.inquilinos)} · ${fila.direccion} · ${capitalizar(fila.periodoTexto)}`;
  $("fechaPago").max = hoyISO();
  $("fechaPago").value = hoyISO();
  pintarMedios();
  pintarCalculo({ diasAtraso: fila.diasAtraso, punitorio: fila.punitorio, total: fila.total });
  mostrarErrorCobro("");
  $("cobroForm").hidden = false;
  $("cobroExito").hidden = true;
  $("cobroDialog").showModal();
  $("fechaPago").focus();
}

// El punitorio depende de la fecha de pago: se recalcula en el servidor para que la regla sea una sola.
function alCambiarFecha() {
  clearTimeout(temporizadorFecha);
  const fecha = $("fechaPago").value;
  if (!fecha || fecha > hoyISO() || !filaEnCobro) return;
  const fila = filaEnCobro;
  temporizadorFecha = setTimeout(async () => {
    const r = await pedir(`/pagos/${fila.idPago}/calculo?fecha=${fecha}`);
    if (r.ok && filaEnCobro === fila) {
      pintarCalculo(r.cuerpo);
      mostrarErrorCobro("");
    } else if (!r.ok) {
      mostrarErrorCobro(r.cuerpo.error || "No pudimos calcular el punitorio.");
    }
  }, 200);
}

// Número de WhatsApp en formato internacional argentino (549 + área + número, sin 0 ni 15).
function telefonoWhatsApp(telefono) {
  const n = soloDigitos(telefono).replace(/^0+/, "");
  if (n.length < 8) return "";
  return n.startsWith("54") ? n : `549${n}`;
}

async function registrar(evento) {
  evento.preventDefault();
  const fecha = $("fechaPago").value;
  if (!fecha) return mostrarErrorCobro("Elegí la fecha de pago.");
  if (fecha > hoyISO()) return mostrarErrorCobro("La fecha de pago no puede ser posterior a hoy.");
  if (!medioElegido()) return mostrarErrorCobro("Elegí la forma de pago.");
  mostrarErrorCobro("");
  const boton = $("registrarBtn");
  boton.disabled = true;
  boton.textContent = "Registrando…";
  try {
    const r = await pedir("/pagos", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ idPago: filaEnCobro.idPago, fechaPago: fecha, medioPago: medioElegido() }),
    });
    if (r.ok && r.cuerpo.id !== undefined) {
      mostrarExito(r.cuerpo);
      cargarPanel(); // el cobro sale de pendientes y aparece en realizados
    } else if (r.estado === 400) {
      mostrarErrorCobro(r.cuerpo.error || "Los datos no son válidos. Revisalos e intentá de nuevo.");
      cargarPanel();
    } else {
      mostrarErrorCobro("El servidor no pudo registrar el pago. Intentá de nuevo en unos minutos.");
    }
  } catch (err) {
    mostrarErrorCobro("No pudimos conectar con el servidor. ¿Está corriendo el backend en localhost:8081?");
  } finally {
    boton.disabled = false;
    boton.textContent = "Registrar pago";
  }
}

// El cuadro de "Pago registrado" se achica, en bloque y solo lo necesario, para entrar en la ventana sin desplazarse.
function ajustarAlto() {
  const bloque = $("cobroExito");
  const dialogo = $("cobroDialog");
  if (bloque.hidden) return;
  bloque.style.zoom = "";
  const natural = dialogo.scrollHeight;
  const propio = bloque.offsetHeight;
  const libre = window.innerHeight - 24;
  if (natural <= libre || propio === 0) return;
  const factor = Math.max(0.55, (libre - (natural - propio)) / propio);
  bloque.style.zoom = factor.toFixed(3);
}

window.addEventListener("resize", ajustarAlto);

function mostrarExito(respuesta) {
  const f = filaEnCobro;
  const numero = String(respuesta.numeroRecibo ?? respuesta.id).padStart(8, "0");
  const conMora = respuesta.diasAtraso > 0;
  $("exitoTitulo").textContent = `Pago registrado · Recibo N.º ${numero}`;
  $("exitoDetalle").textContent = conMora
    ? `Se cobró ${f.periodoTexto} con ${respuesta.diasAtraso} ${plural(respuesta.diasAtraso, "día", "días")} de punitorio.`
    : `Se cobró ${f.periodoTexto} en término.`;
  const filas = [
    ["Propiedad", f.direccion],
    ["Inquilino", nombres(f.inquilinos)],
    ["Mes pagado", capitalizar(f.periodoTexto)],
    ["Fecha de pago", fechaTexto(respuesta.fechaPago)],
    ["Forma de pago", ETIQUETA_MEDIO[medioElegido()]],
    ["Alquiler", dinero.format(respuesta.monto)],
  ];
  if (conMora) filas.push(["Punitorio", dinero.format(respuesta.punitorio)]);
  filas.push(["Total cobrado", dinero.format(respuesta.total)]);
  const dl = $("exitoDatos");
  dl.innerHTML = "";
  filas.forEach(([clave, valor]) => {
    const fila = el("div", "review__row");
    fila.append(el("dt", "", clave), el("dd", "", valor));
    dl.append(fila);
  });
  $("reciboLink").href = `${API_BASE}${respuesta.recibo}`;
  // Aviso por WhatsApp: abre el chat con el mensaje armado (no hay envío automático).
  const conTelefono = f.inquilinos.find((i) => telefonoWhatsApp(i.telefono));
  $("whatsappLink").hidden = !conTelefono;
  if (conTelefono) {
    const mensaje = `Hola ${conTelefono.nombre}, registramos el pago del alquiler de ${f.periodoTexto} de ${f.direccion} `
      + `por ${dinero.format(respuesta.total)}. Recibo N.º ${numero}. ¡Gracias! — UBIKAR Propiedades`;
    $("whatsappLink").href = `https://wa.me/${telefonoWhatsApp(conTelefono.telefono)}?text=${encodeURIComponent(mensaje)}`;
  }
  $("cobroForm").hidden = true;
  $("cobroExito").hidden = false;
  ajustarAlto();
  $("exitoTitulo").focus();
}

// ---------- eventos ----------

$("buscador").addEventListener("input", pintarTablas);
$("mes").addEventListener("change", cargarPanel);
$("cobroForm").addEventListener("submit", registrar);
$("fechaPago").addEventListener("input", alCambiarFecha);
$("fechaPago").addEventListener("change", alCambiarFecha);
$("cancelarBtn").addEventListener("click", () => $("cobroDialog").close());
$("cerrarBtn").addEventListener("click", () => $("cobroDialog").close());
// Clic en el fondo oscuro: cierra el cuadro.
$("cobroDialog").addEventListener("click", (e) => {
  const caja = $("cobroDialog").getBoundingClientRect();
  const fuera = e.clientX < caja.left || e.clientX > caja.right || e.clientY < caja.top || e.clientY > caja.bottom;
  // Solo cuenta el clic sobre el fondo: pulsar Enter en un botón también genera un "clic" con coordenadas 0,0.
  if (e.target === $("cobroDialog") && fuera) $("cobroDialog").close();
});
$("cobroDialog").addEventListener("close", () => {
  filaEnCobro = null;
  clearTimeout(temporizadorFecha);
});

cargarPanel();
