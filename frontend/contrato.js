"use strict";

const API_BASE = "http://localhost:8081";
const API = `${API_BASE}/api`;

const PASOS = ["Propiedad", "Inquilinos", "Garantía", "Condiciones", "Inventario", "Revisión"];
const TIPOS_GARANTIA = [
  ["FIADOR_SOLIDARIO", "Fiador solidario"],
  ["RECIBO_SUELDO", "Recibo de sueldo"],
  ["TITULO_PROPIEDAD", "Título de propiedad"],
  ["SEGURO_CAUCION", "Seguro de caución"],
];
const EXIGE_GARANTE = new Set(["FIADOR_SOLIDARIO", "RECIBO_SUELDO"]);
const ESTADOS = [["NUEVO", "Nuevo"], ["BUENO", "Bueno"], ["REGULAR", "Regular"], ["MALO", "Malo"]];
const PRESETS = {
  Cocina: ["Anafe", "Mesada", "Bajo mesada", "Alacenas", "Grifería"],
  Baño: ["Inodoro", "Bidet", "Lavatorio", "Ducha", "Espejo"],
  Living: ["Ventanas", "Puerta de acceso", "Luces", "Tomacorrientes"],
  Dormitorio: ["Placard", "Ventana", "Puerta", "Luces"],
};
const MESES = ["enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"];

const catalogo = { propietarios: [], inquilinos: [], garantes: [], propiedades: [] };
const $ = (id) => document.getElementById(id);
const dinero = new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" });

let paso = 1;
let propietario;
const inquilinos = [];
const garantes = [];

// ---------- utilidades ----------

function el(tag, clase, texto) {
  const nodo = document.createElement(tag);
  if (clase) nodo.className = clase;
  if (texto !== undefined) nodo.textContent = texto;
  return nodo;
}

function soloDigitos(valor) {
  return valor.replace(/\D/g, "");
}

function sumarMeses(y, m, d, meses) {
  const total = m - 1 + meses;
  const anio = y + Math.floor(total / 12);
  const mes = ((total % 12) + 12) % 12;
  const ultimoDia = new Date(Date.UTC(anio, mes + 1, 0)).getUTCDate();
  return new Date(Date.UTC(anio, mes, Math.min(d, ultimoDia)));
}

function fechaFin(inicioISO, duracion) {
  const [y, m, d] = inicioISO.split("-").map(Number);
  const fin = sumarMeses(y, m, d, duracion);
  fin.setUTCDate(fin.getUTCDate() - 1);
  return fin;
}

function textoFecha(fecha) {
  return `${fecha.getUTCDate()} de ${MESES[fecha.getUTCMonth()]} de ${fecha.getUTCFullYear()}`;
}

function textoFechaISO(iso) {
  const [y, m, d] = iso.split("-").map(Number);
  return textoFecha(new Date(Date.UTC(y, m - 1, d)));
}

function opciones(select, lista) {
  lista.forEach(([valor, texto]) => select.append(new Option(texto, valor)));
}

// ---------- persona (bloque reutilizable) ----------

function crearPersona({ titulo, tipo, conDetalle = false, quitable = false, onQuitar, onBuscar }) {
  const nodo = $("tplPersona").content.firstElementChild.cloneNode(true);
  const campo = (nombre) => nodo.querySelector(`[data-f="${nombre}"]`);
  const estado = nodo.querySelector(".persona__estado");
  const persona = { nodo, id: null, tipo, titulo };
  let ultimoBuscado = "";

  nodo.querySelector(".persona__title").textContent = titulo;
  if (conDetalle) nodo.querySelector(".persona__detalle").hidden = false;
  if (quitable) {
    const quitar = nodo.querySelector(".persona__remove");
    quitar.hidden = false;
    quitar.addEventListener("click", () => onQuitar(persona));
  }

  function mensaje(texto, clase) {
    estado.textContent = texto;
    estado.className = `persona__estado${clase ? ` persona__estado--${clase}` : ""}`;
  }

  function llenar(p) {
    ["nombre", "apellido", "domicilio", "telefono", "email"].forEach((n) => {
      campo(n).value = p[n] || "";
    });
    if (conDetalle && p.detalle !== undefined) campo("detalle").value = p.detalle || "";
  }

  function buscar() {
    const dni = soloDigitos(campo("dni").value);
    if (dni.length < 7) {
      mensaje("Ingresá un DNI de 7 u 8 dígitos.", "");
      return;
    }
    ultimoBuscado = dni;
    campo("dni").value = dni;
    const propio = catalogo[tipo].find((p) => p.dni === dni);
    if (propio) {
      persona.id = propio.id;
      llenar(propio);
      mensaje("Ya registrado en UBIKAR. Si corregís algún dato, se actualiza al guardar el contrato.", "ok");
    } else {
      persona.id = null;
      const otro = ["propietarios", "inquilinos", "garantes"].filter((t) => t !== tipo)
        .map((t) => ({ t, p: catalogo[t].find((x) => x.dni === dni) })).find((x) => x.p);
      if (otro) {
        llenar(otro.p);
        mensaje("Esta persona ya figura en UBIKAR con otro rol: se copian sus datos y se da de alta con este contrato.", "new");
      } else {
        mensaje("DNI nuevo: se da de alta junto con este contrato.", "new");
      }
    }
    if (onBuscar) onBuscar(persona);
  }

  nodo.querySelector(".persona__buscar").addEventListener("click", buscar);
  campo("dni").addEventListener("keydown", (e) => {
    if (e.key === "Enter") {
      e.preventDefault();
      buscar();
    }
  });
  campo("dni").addEventListener("input", () => {
    if (persona.id !== null || ultimoBuscado) {
      persona.id = null;
      ultimoBuscado = "";
      mensaje("", "");
      if (onBuscar) onBuscar(persona);
    }
  });
  campo("dni").addEventListener("blur", () => {
    const dni = soloDigitos(campo("dni").value);
    if (dni.length >= 7 && dni !== ultimoBuscado) buscar();
  });

  persona.datos = () => {
    const d = {
      nombre: campo("nombre").value.trim(),
      apellido: campo("apellido").value.trim(),
      dni: soloDigitos(campo("dni").value),
      domicilio: campo("domicilio").value.trim(),
      telefono: campo("telefono").value.trim(),
      email: campo("email").value.trim(),
    };
    if (persona.id !== null) d.id = persona.id;
    if (conDetalle) d.detalle = campo("detalle").value.trim();
    return d;
  };

  persona.error = () => {
    const d = persona.datos();
    if (d.dni.length < 7) return `Ingresá un DNI válido para: ${titulo}.`;
    if (!d.nombre || !d.apellido) return `Completá nombre y apellido de: ${titulo}.`;
    if (!d.domicilio) return `Falta el domicilio de: ${titulo} (figura en el contrato).`;
    return "";
  };

  return persona;
}

// ---------- paso 1: propietario y propiedad ----------

function cargarPropiedades(p) {
  const select = $("propiedadSelect");
  select.innerHTML = "";
  const propias = p && p.id !== null ? catalogo.propiedades.filter((x) => x.idPropietario === p.id) : [];
  select.append(new Option("Nueva propiedad", ""));
  propias.forEach((x) => select.append(new Option(x.direccion, x.id)));
  $("propiedadSelectField").hidden = propias.length === 0;
  alternarDireccion();
}

function alternarDireccion() {
  $("direccionField").hidden = $("propiedadSelect").value !== "";
}

function iniciarPropietario() {
  propietario = crearPersona({ titulo: "Propietario", tipo: "propietarios", onBuscar: cargarPropiedades });
  $("propietarioSlot").append(propietario.nodo);
  $("propiedadSelect").addEventListener("change", alternarDireccion);
  cargarPropiedades(null);
}

// ---------- pasos 2 y 3: listas de personas ----------

function agregarPersona(lista, slot, config) {
  const persona = crearPersona({
    ...config,
    quitable: lista.length > 0,
    onQuitar: (p) => {
      lista.splice(lista.indexOf(p), 1);
      p.nodo.remove();
      renumerar(lista, config.prefijo);
    },
  });
  lista.push(persona);
  slot.append(persona.nodo);
  renumerar(lista, config.prefijo);
  return persona;
}

function renumerar(lista, prefijo) {
  lista.forEach((p, i) => {
    p.titulo = lista.length > 1 ? `${prefijo} ${i + 1}` : prefijo;
    p.nodo.querySelector(".persona__title").textContent = p.titulo;
  });
}

const CONFIG_INQUILINO = { titulo: "Inquilino", prefijo: "Inquilino", tipo: "inquilinos" };
const CONFIG_GARANTE = { titulo: "Garante", prefijo: "Garante", tipo: "garantes", conDetalle: true };

function tipoGarantiaExigeGarante() {
  return EXIGE_GARANTE.has($("tipoGarantia").value);
}

function actualizarGarantes() {
  const exige = tipoGarantiaExigeGarante();
  $("garantesBloque").hidden = !exige;
  if (exige && garantes.length === 0) agregarPersona(garantes, $("garantesSlot"), CONFIG_GARANTE);
}

// ---------- paso 4: condiciones ----------

function actualizarDerivados() {
  const inicio = $("fechaInicio").value;
  const duracion = Number($("duracionMeses").value);
  const valor = Number($("valorInicial").value);
  const depositoTexto = $("deposito").value;
  const partes = [];
  if (inicio && duracion >= 1) {
    partes.push(`El contrato finaliza el ${textoFecha(fechaFin(inicio, duracion))}.`);
  }
  if (valor > 0) {
    const deposito = depositoTexto !== "" ? Number(depositoTexto) : valor;
    partes.push(`Depósito en garantía: ${dinero.format(deposito)}${depositoTexto === "" ? " (un mes de alquiler)" : ""}.`);
  }
  $("derivados").textContent = partes.join(" ");
}

// ---------- paso 5: inventario ----------

function agregarRenglon(valores = {}) {
  const fila = el("div", "inv__row");
  const ambiente = el("input", "field__input");
  ambiente.value = valores.ambiente || "";
  ambiente.setAttribute("aria-label", "Ambiente");
  ambiente.setAttribute("list", "ambientes");
  const objeto = el("input", "field__input");
  objeto.value = valores.objeto || "";
  objeto.setAttribute("aria-label", "Elemento");
  const estado = el("select", "field__input");
  estado.setAttribute("aria-label", "Estado");
  opciones(estado, ESTADOS);
  estado.value = valores.estado || "BUENO";
  const obs = el("input", "field__input");
  obs.value = valores.observaciones || "";
  obs.setAttribute("aria-label", "Observaciones");
  const borrar = el("button", "inv__del", "×");
  borrar.type = "button";
  borrar.setAttribute("aria-label", "Quitar elemento");
  borrar.addEventListener("click", () => fila.remove());
  fila.append(ambiente, objeto, estado, obs, borrar);
  fila.datos = () => ({
    ambiente: ambiente.value.trim(),
    objeto: objeto.value.trim(),
    estado: estado.value,
    observaciones: obs.value.trim(),
  });
  $("invSlot").append(fila);
  return fila;
}

function renglones() {
  return [...$("invSlot").querySelectorAll(".inv__row")].map((f) => f.datos()).filter((r) => r.objeto);
}

function iniciarInventario() {
  const lista = el("datalist");
  lista.id = "ambientes";
  ["Cocina", "Baño", "Living", "Comedor", "Dormitorio", "Patio", "Cochera", "Lavadero"].forEach((a) => lista.append(new Option(a)));
  document.body.append(lista);

  Object.entries(PRESETS).forEach(([ambiente, items]) => {
    const chip = el("button", "chip", `+ ${ambiente}`);
    chip.type = "button";
    chip.addEventListener("click", () => {
      vaciarRenglonesEnBlanco();
      items.forEach((objeto) => agregarRenglon({ ambiente, objeto, estado: "BUENO" }));
    });
    $("presets").append(chip);
  });
  agregarRenglon();
}

function vaciarRenglonesEnBlanco() {
  $("invSlot").querySelectorAll(".inv__row").forEach((f) => {
    if (!f.datos().objeto) f.remove();
  });
}

// ---------- validación, armado y revisión ----------

function errorDelPaso(n) {
  if (n === 1) {
    const e = propietario.error();
    if (e) return e;
    if ($("propiedadSelect").value === "" && !$("direccionInput").value.trim()) return "Ingresá la dirección de la propiedad.";
  }
  if (n === 2) {
    for (const p of inquilinos) {
      const e = p.error();
      if (e) return e;
      if (p.datos().dni === propietario.datos().dni) return "El propietario no puede ser inquilino de su propia propiedad.";
    }
  }
  if (n === 3 && tipoGarantiaExigeGarante()) {
    for (const g of garantes) {
      const e = g.error();
      if (e) return e;
      if (inquilinos.some((i) => i.datos().dni === g.datos().dni)) return "Un garante no puede ser también inquilino del mismo contrato.";
    }
  }
  if (n === 4) {
    const duracion = Number($("duracionMeses").value);
    const periodicidad = Number($("periodicidad").value);
    if (!$("fechaInicio").value) return "Elegí la fecha de inicio.";
    if (!(duracion >= 1)) return "La duración debe ser de al menos 1 mes.";
    if (!(Number($("valorInicial").value) > 0)) return "Ingresá el alquiler mensual inicial.";
    if (!(periodicidad >= 1 && periodicidad <= duracion)) return "La actualización debe ser cada 1 mes como mínimo y no superar la duración.";
  }
  if (n === 5 && renglones().length === 0) return "Cargá al menos un elemento del inventario (podés usar los ambientes de arriba).";
  return "";
}

function armarSolicitud() {
  const propiedadId = $("propiedadSelect").value;
  const deposito = $("deposito").value;
  return {
    propietario: propietario.datos(),
    propiedad: propiedadId ? { id: Number(propiedadId) } : { direccion: $("direccionInput").value.trim() },
    inquilinos: inquilinos.map((p) => p.datos()),
    garantes: tipoGarantiaExigeGarante() ? garantes.map((p) => p.datos()) : [],
    fechaInicio: $("fechaInicio").value,
    duracionMeses: Number($("duracionMeses").value),
    valorInicial: Number($("valorInicial").value),
    periodicidadActualizacion: Number($("periodicidad").value),
    indiceActualizacion: $("indice").value,
    destino: $("destino").value,
    depositoGarantia: deposito === "" ? null : Number(deposito),
    tipoGarantia: $("tipoGarantia").value,
    renglones: renglones(),
    plantilla: $("plantilla").value,
  };
}

function bloqueRevision(titulo, filas) {
  const seccion = el("section");
  seccion.append(el("h3", "review__title", titulo));
  const dl = el("dl");
  filas.forEach(([clave, valor]) => {
    const fila = el("div", "review__row");
    fila.append(el("dt", "", clave), el("dd", "", valor));
    dl.append(fila);
  });
  seccion.append(dl);
  return seccion;
}

function resumenPersona(d) {
  const partes = [`${d.nombre} ${d.apellido}`, `DNI ${d.dni}`, d.domicilio];
  return partes.join(" · ");
}

function renderRevision() {
  const s = armarSolicitud();
  const fin = textoFecha(fechaFin(s.fechaInicio, s.duracionMeses));
  const deposito = s.depositoGarantia === null ? s.valorInicial : s.depositoGarantia;
  const tipo = TIPOS_GARANTIA.find(([v]) => v === s.tipoGarantia)[1];
  const review = $("review");
  review.innerHTML = "";
  review.append(
    bloqueRevision("Partes", [
      ["Propietario", resumenPersona(s.propietario)],
      ["Propiedad", s.propiedad.id ? $("propiedadSelect").selectedOptions[0].textContent : s.propiedad.direccion],
      ...s.inquilinos.map((i, n) => [s.inquilinos.length > 1 ? `Inquilino ${n + 1}` : "Inquilino", resumenPersona(i)]),
    ]),
    bloqueRevision("Garantía", [
      ["Tipo", tipo],
      ...s.garantes.map((g, n) => [s.garantes.length > 1 ? `Garante ${n + 1}` : "Garante", resumenPersona(g) + (g.detalle ? ` (${g.detalle})` : "")]),
    ]),
    bloqueRevision("Condiciones", [
      ["Vigencia", `${textoFechaISO(s.fechaInicio)} al ${fin} (${s.duracionMeses} meses)`],
      ["Alquiler inicial", dinero.format(s.valorInicial)],
      ["Actualización", `Cada ${s.periodicidadActualizacion} meses por ${s.indiceActualizacion}`],
      ["Depósito", dinero.format(deposito)],
      ["Destino", s.destino === "VIVIENDA" ? "Vivienda" : "Comercial"],
      ["Modelo de contrato", s.plantilla === "completa" ? "Completo (20 cláusulas)" : "Estándar UBIKAR (14 cláusulas)"],
    ]),
    bloqueRevision("Inventario", [["Elementos", `${s.renglones.length} cargados en ${new Set(s.renglones.map((r) => r.ambiente || "General")).size} ambiente(s)`]]),
  );
}

// ---------- navegación ----------

function pintarPasos() {
  const lista = $("steps");
  lista.innerHTML = "";
  PASOS.forEach((nombre, i) => {
    const n = i + 1;
    const item = el("li", `steps__item${n === paso ? " steps__item--current" : n < paso ? " steps__item--done" : ""}`);
    const btn = el("button", "steps__btn");
    btn.type = "button";
    btn.disabled = n >= paso;
    btn.setAttribute("aria-label", `${n}. ${nombre}${n < paso ? " (completado)" : n === paso ? " (actual)" : ""}`);
    if (n === paso) btn.setAttribute("aria-current", "step");
    btn.append(el("span", "steps__bar"), el("span", "steps__label", `${n}. ${nombre}`));
    btn.addEventListener("click", () => irAPaso(n));
    item.append(btn);
    lista.append(item);
  });
}

function irAPaso(n) {
  paso = n;
  document.querySelectorAll(".wz-step").forEach((s) => {
    s.hidden = Number(s.dataset.step) !== n;
  });
  $("stepError").hidden = true;
  $("prevBtn").hidden = n === 1;
  $("nextBtn").textContent = n === PASOS.length ? "Generar contrato" : "Siguiente";
  if (n === 3) actualizarGarantes();
  if (n === 4) actualizarDerivados();
  if (n === PASOS.length) renderRevision();
  pintarPasos();
  const titulo = document.querySelector(`.wz-step[data-step="${n}"] .wz-step__title`);
  window.scrollTo({ top: 0, behavior: "smooth" });
  if (titulo) titulo.focus({ preventScroll: true });
}

function mostrarError(texto) {
  const caja = $("stepError");
  caja.textContent = texto;
  caja.hidden = false;
}

async function generar() {
  const boton = $("nextBtn");
  boton.disabled = true;
  boton.textContent = "Generando…";
  try {
    const respuesta = await fetch(`${API}/contratos`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(armarSolicitud()),
    });
    const cuerpo = await respuesta.json().catch(() => ({}));
    if (!respuesta.ok) {
      mostrarError(cuerpo.error || "El servidor no pudo registrar el contrato. Revisá los datos e intentá de nuevo.");
      return;
    }
    mostrarExito(cuerpo);
  } catch (err) {
    mostrarError("No pudimos conectar con el servidor. ¿Está corriendo el backend en localhost:8081?");
  } finally {
    boton.disabled = false;
    boton.textContent = "Generar contrato";
  }
}

function mostrarExito(contrato) {
  $("wizard").hidden = true;
  $("steps").hidden = true;
  $("exitoTitulo").textContent = `Contrato N.º ${contrato.id} registrado`;
  $("exitoDetalle").textContent =
    `Vigente del ${textoFechaISO(contrato.fechaInicio)} al ${textoFechaISO(contrato.fechaFin)}. ` +
    "Se guardó el contrato, el inventario y el documento en Word.";
  $("descargarLink").href = `${API_BASE}${contrato.documento}`;
  $("exito").hidden = false;
  $("exitoTitulo").focus();
  window.scrollTo({ top: 0, behavior: "smooth" });
}

$("nextBtn").addEventListener("click", () => {
  const error = errorDelPaso(paso);
  if (error) {
    mostrarError(error);
    return;
  }
  if (paso === PASOS.length) {
    generar();
    return;
  }
  irAPaso(paso + 1);
});

$("prevBtn").addEventListener("click", () => irAPaso(paso - 1));
$("wizard").addEventListener("submit", (e) => e.preventDefault());
$("otroBtn").addEventListener("click", () => window.location.reload());
$("addInquilinoBtn").addEventListener("click", () => agregarPersona(inquilinos, $("inquilinosSlot"), CONFIG_INQUILINO));
$("addGaranteBtn").addEventListener("click", () => agregarPersona(garantes, $("garantesSlot"), CONFIG_GARANTE));
$("addRenglonBtn").addEventListener("click", () => agregarRenglon());
$("tipoGarantia").addEventListener("change", actualizarGarantes);
$("plantilla").addEventListener("change", () => {
  $("plantillaHint").hidden = $("plantilla").value !== "completa";
});
["fechaInicio", "duracionMeses", "valorInicial", "deposito"].forEach((id) => $(id).addEventListener("input", actualizarDerivados));

// ---------- arranque ----------

async function cargarCatalogos() {
  const rutas = { propietarios: "propietarios", inquilinos: "inquilinos", garantes: "garantes", propiedades: "propiedades" };
  try {
    await Promise.all(Object.entries(rutas).map(async ([clave, ruta]) => {
      const r = await fetch(`${API}/${ruta}`);
      if (!r.ok) throw new Error(ruta);
      catalogo[clave] = await r.json();
    }));
  } catch (err) {
    $("offline").hidden = false;
  }
}

opciones($("tipoGarantia"), TIPOS_GARANTIA);
iniciarPropietario();
agregarPersona(inquilinos, $("inquilinosSlot"), CONFIG_INQUILINO);
iniciarInventario();
irAPaso(1);
cargarCatalogos();
