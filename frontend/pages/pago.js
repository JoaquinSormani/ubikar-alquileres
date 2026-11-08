"use strict";

const MEDIOS = [["EFECTIVO", "Efectivo"], ["TRANSFERENCIA", "Transferencia"], ["TARJETA_CREDITO", "Tarjeta de crédito"]];
const ETIQUETA_MEDIO = Object.fromEntries(MEDIOS);
const $ = (id) => document.getElementById(id);
const dinero = new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" });

let panel = { meses: [] };
let filaEnCobro = null;
let temporizadorFecha = null;
let temporizadorBusqueda = null;

// Las dos tablas (pendientes y realizados) se piden de a una página al servidor. Cada página trae el cursor de la
// siguiente (el último cobro visto), así que un cobro que se paga o un pago que se registra no repite ni saltea
// filas en las otras páginas. "cursores[i]" es con qué se pide la página i (null = la primera).
function crearLista(config) {
  return { ...config, datos: { filas: [], total: 0, siguiente: null }, cursores: [null], pagina: 0, pedido: 0 };
}

const pendientes = crearLista({
  ruta: "/pagos/pendientes", tamanio: 5, fila: filaPendiente,
  cuerpo: "tbPend", vacio: "vacioPend", cantidad: "cantPend",
  paginas: "paginasPend", rango: "rangoPend", numero: "paginaPend", anterior: "anteriorPend", siguiente: "siguientePend",
  textoVacio: (buscando, mes) => buscando ? "Ningún cobro pendiente coincide con la búsqueda."
    : mes ? `No hay pagos pendientes de ${mesTexto(mes)}.` : "No hay pagos pendientes: todos los contratos están al día.",
});

const realizados = crearLista({
  ruta: "/pagos/realizados", tamanio: 10, fila: filaRealizada,
  cuerpo: "tbReal", vacio: "vacioReal", cantidad: "cantReal",
  paginas: "paginas", rango: "rango", numero: "paginaActual", anterior: "anteriorBtn", siguiente: "siguienteBtn",
  textoVacio: (buscando, mes) => buscando ? "Ningún pago coincide con la búsqueda."
    : mes ? `Todavía no se registraron pagos de ${mesTexto(mes)}.` : "Todavía no se registraron pagos.",
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
  // El punitorio va en su propia columna (como en pendientes): así todas las filas miden lo mismo.
  tr.append(celda(f.diasAtraso > 0 ? dinero.format(f.punitorio) : "—", f.diasAtraso > 0 ? "pago__num pago__mora" : "pago__num"));
  tr.append(celda(dinero.format(f.total), "pago__num"));
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
    ? `${pendientes.datos.total} ${plural(pendientes.datos.total, "pendiente", "pendientes")} · ${realizados.datos.total} ${plural(realizados.datos.total, "realizado", "realizados")} que coinciden`
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

// Arma los cobros del mes y trae los meses que tienen cobros (para el selector).
async function cargarPanel() {
  try {
    const r = await pedir("/pagos/panel");
    if (!r.ok) throw new Error("panel");
    $("offline").hidden = true;
    panel = r.cuerpo;
    llenarMeses();
  } catch (err) {
    $("offline").hidden = false;
  }
}

// Pide una página de una de las tablas. Con "reiniciar" vuelve a la primera (cambió el mes o la búsqueda, o se registró un pago).
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
    pintarLista(pendientes);
    pintarLista(realizados);
  } catch (err) {
    $("offline").hidden = false;
  }
}

function cargarListas() {
  cargarLista(pendientes, true);
  cargarLista(realizados, true);
}

// Primero el panel (arma los cobros que falten), después las páginas, para que ya los encuentren.
async function cargarTodo() {
  await cargarPanel();
  cargarListas();
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
      cargarTodo(); // el cobro sale de pendientes y aparece arriba de realizados
    } else if (r.estado === 400) {
      mostrarErrorCobro(r.cuerpo.error || "Los datos no son válidos. Revisalos e intentá de nuevo.");
      cargarTodo();
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

$("buscador").addEventListener("input", () => {
  clearTimeout(temporizadorBusqueda);
  temporizadorBusqueda = setTimeout(cargarListas, 300); // se busca en el servidor, sin pedir con cada letra
});
$("mes").addEventListener("change", cargarListas);
[pendientes, realizados].forEach((lista) => {
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

cargarTodo();
