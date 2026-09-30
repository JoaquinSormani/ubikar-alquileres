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
const DESCRIPCION_GARANTIA = {
  FIADOR_SOLIDARIO: "Requiere cargar un garante",
  RECIBO_SUELDO: "Requiere cargar un garante",
  TITULO_PROPIEDAD: "Se respalda con una propiedad",
  SEGURO_CAUCION: "Lo respalda una aseguradora",
};
const INDICES = [["ICL", "ICL", "Banco Central"], ["IPC", "IPC", "INDEC"]];
const DESTINOS = [["VIVIENDA", "Vivienda"], ["COMERCIAL", "Comercial"]];
const MODELOS = [
  ["estandar", "Estándar UBIKAR", "14 cláusulas"],
  ["completa", "Completo", "20 cláusulas. Deja líneas en blanco (__________) para completar en Word: horario y cuenta de cobro, pintura, penalidades y sellado."],
];
const CLAVE_BORRADOR = "ubikar.borrador.contrato";
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
let sucio = false;
let terminado = false;
let restaurando = false;
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

function renderRadios(idContenedor, nombre, lista, defecto) {
  const contenedor = $(idContenedor).querySelector(".radios__opciones");
  lista.forEach(([valor, texto, descripcion]) => {
    const etiqueta = el("label", "radio");
    const input = el("input");
    input.type = "radio";
    input.name = nombre;
    input.value = valor;
    input.checked = valor === defecto;
    const cuerpo = el("span", "radio__cuerpo");
    cuerpo.append(el("span", "radio__texto", texto));
    if (descripcion) cuerpo.append(el("span", "radio__desc", descripcion));
    etiqueta.append(input, cuerpo);
    contenedor.append(etiqueta);
  });
}

function valorRadio(nombre) {
  const marcado = document.querySelector(`input[name="${nombre}"]:checked`);
  return marcado ? marcado.value : "";
}

function fijarRadio(nombre, valor) {
  const radio = document.querySelector(`input[name="${nombre}"][value="${valor}"]`);
  if (radio) radio.checked = true;
}

// ---------- persona (bloque reutilizable) ----------

function crearPersona({ titulo, tipo, conDetalle = false, quitable = false, soloLectura = false, onQuitar, onBuscar }) {
  const nodo = $("tplPersona").content.firstElementChild.cloneNode(true);
  const campo = (nombre) => nodo.querySelector(`[data-f="${nombre}"]`);
  const estado = nodo.querySelector(".persona__estado");
  const persona = { nodo, id: null, tipo, titulo };
  let ultimoBuscado = "";

  nodo.querySelector(".persona__title").textContent = titulo;
  let tarjeta = null;
  if (soloLectura) {
    nodo.classList.add("persona--lectura");
    nodo.querySelectorAll("[data-f]").forEach((i) => {
      if (i.dataset.f !== "dni") i.readOnly = true;
    });
    tarjeta = el("div", "pcard");
    tarjeta.hidden = true;
    estado.after(tarjeta);
  }

  function pintarTarjeta(p) {
    tarjeta.innerHTML = "";
    if (!p) {
      tarjeta.hidden = true;
      return;
    }
    const iniciales = `${(p.nombre || "?")[0]}${(p.apellido || "")[0] || ""}`.toUpperCase();
    const cabecera = el("div", "pcard__head");
    const nombres = el("div", "pcard__id");
    nombres.append(el("p", "pcard__nombre", `${p.nombre} ${p.apellido}`), el("p", "pcard__dni", `DNI ${p.dni}`));
    cabecera.append(el("span", "pcard__avatar", iniciales), nombres);
    const datos = el("dl", "pcard__datos");
    [["Domicilio", p.domicilio], ["Teléfono", p.telefono], ["Correo electrónico", p.email]].forEach(([clave, valor]) => {
      const fila = el("div", "pcard__dato");
      fila.append(el("dt", "", clave), el("dd", valor ? "" : "pcard__vacio", valor || "No informado"));
      datos.append(fila);
    });
    tarjeta.append(cabecera, datos);
    tarjeta.hidden = false;
  }
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
    if (document.body.classList.contains("cargando")) {
      mensaje("Cargando datos de UBIKAR… probá de nuevo en un instante.", "");
      return;
    }
    ultimoBuscado = dni;
    campo("dni").value = dni;
    const propio = catalogo[tipo].find((p) => p.dni === dni);
    if (soloLectura) {
      persona.id = propio ? propio.id : null;
      llenar(propio || {});
      pintarTarjeta(propio);
      mensaje(propio
        ? "Propietario registrado en UBIKAR."
        : "No hay un propietario registrado con ese DNI. Registralo primero desde «Registrar propiedad».", propio ? "ok" : "error");
    } else if (propio) {
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
  // Enter busca el DNI recien escrito; con el DNI ya buscado hace lo mismo que "Siguiente".
  campo("dni").addEventListener("keydown", (e) => {
    if (e.key !== "Enter") return;
    e.preventDefault();
    if (soloDigitos(campo("dni").value) !== ultimoBuscado) buscar();
    else $("nextBtn").click();
  });
  campo("dni").addEventListener("input", () => {
    if (persona.id !== null || ultimoBuscado) {
      persona.id = null;
      ultimoBuscado = "";
      if (tarjeta) pintarTarjeta(null);
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

  persona.errores = () => {
    const dni = campo("dni");
    if (soloLectura) {
      return persona.id === null
        ? [{ campo: dni, contexto: persona.titulo, mensaje: `Buscá por DNI a un ${titulo.toLowerCase()} ya registrado.` }]
        : [];
    }
    const d = persona.datos();
    const e = [];
    const falta = (campoNombre, mensaje) => e.push({ campo: campo(campoNombre), contexto: persona.titulo, mensaje });
    if (d.dni.length < 7) falta("dni", "Ingresá un DNI de 7 u 8 dígitos.");
    if (!d.nombre) falta("nombre", "Falta el nombre.");
    if (!d.apellido) falta("apellido", "Falta el apellido.");
    if (!d.domicilio) falta("domicilio", "Falta el domicilio (figura en el contrato).");
    return e;
  };

  persona.campoDni = campo("dni");

  persona.poner = (dni) => {
    campo("dni").value = dni;
    buscar();
  };

  persona.cargar = (d) => {
    campo("dni").value = d.dni || "";
    llenar(d);
    persona.id = d.id === undefined ? null : d.id;
    ultimoBuscado = soloDigitos(d.dni || "");
    if (persona.id !== null) mensaje("Ya registrado en UBIKAR. Si corregís algún dato, se actualiza al guardar el contrato.", "ok");
    else if (ultimoBuscado) mensaje("DNI nuevo: se da de alta junto con este contrato.", "new");
  };

  return persona;
}

// ---------- paso 1: propietario y propiedad ----------

function cargarPropiedades(p) {
  const select = $("propiedadSelect");
  select.innerHTML = "";
  const propias = p && p.id !== null ? catalogo.propiedades.filter((x) => x.idPropietario === p.id) : [];
  propias.forEach((x) => select.append(new Option(x.direccion, x.id)));
  $("propiedadSelectField").hidden = propias.length === 0;
  if (typeof pintarFicha === "function" && inquilinos) pintarFicha();
  const sinPropiedades = p && p.id !== null && propias.length === 0;
  $("sinPropiedades").hidden = !sinPropiedades;
}

function iniciarPropietario() {
  propietario = crearPersona({ titulo: "Propietario", tipo: "propietarios", soloLectura: true, onBuscar: cargarPropiedades });
  $("propietarioSlot").append(propietario.nodo);
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
  return EXIGE_GARANTE.has(valorRadio("tipoGarantia"));
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
  $("derivados").hidden = partes.length === 0;
}

// ---------- paso 5: inventario ----------
// El inventario se agrupa por ambiente: el ambiente se fija una vez por grupo
// (no se retipea en cada fila), lo que evita duplicados y errores de tipeo al
// contarlos en la Revisión.

function normalizarAmbiente(texto) {
  return texto.trim().replace(/\s+/g, " ").toLowerCase();
}

// "cocina  grande" -> "Cocina Grande" (para que "Cocina" y "cocina" sean el mismo grupo)
function tituloAmbiente(texto) {
  return texto.trim().replace(/\s+/g, " ").replace(/\p{L}+/gu, (w) => w[0].toUpperCase() + w.slice(1).toLowerCase());
}

function buscarGrupo(ambiente) {
  const clave = normalizarAmbiente(ambiente);
  return [...$("invSlot").children].find((g) => normalizarAmbiente(g.dataset.ambiente) === clave);
}

function agregarItem(grupo, valores = {}) {
  const cuerpo = grupo.querySelector(".inv-grupo__body");
  const fila = el("div", "inv__row");
  const objeto = el("input", "field__input");
  objeto.value = valores.objeto || "";
  objeto.setAttribute("aria-label", "Elemento");
  const estadoWrap = el("div", "inv__estado");
  const punto = el("span", "inv__punto");
  const estado = el("select", "field__input");
  estado.setAttribute("aria-label", "Estado");
  opciones(estado, ESTADOS);
  estado.value = valores.estado || "BUENO";
  const pintarPunto = () => {
    punto.className = `inv__punto inv__punto--${estado.value.toLowerCase()}`;
  };
  pintarPunto();
  estado.addEventListener("change", pintarPunto);
  estadoWrap.append(punto, estado);
  const obs = el("input", "field__input");
  obs.value = valores.observaciones || "";
  obs.setAttribute("aria-label", "Observaciones");
  const acciones = el("div", "inv__acciones");
  const duplicar = el("button", "inv__dup", "⧉");
  duplicar.type = "button";
  duplicar.setAttribute("aria-label", "Duplicar elemento");
  duplicar.addEventListener("click", () => {
    agregarItem(grupo, fila.datos());
    actualizarInventarioUI();
  });
  const borrar = el("button", "inv__del", "×");
  borrar.type = "button";
  borrar.setAttribute("aria-label", "Quitar elemento");
  borrar.addEventListener("click", () => {
    fila.remove();
    actualizarInventarioUI();
  });
  acciones.append(duplicar, borrar);
  const celda = (texto, control) => {
    const c = el("label", "inv__cell");
    c.append(el("span", "inv__lbl", texto), control);
    return c;
  };
  fila.append(celda("Elemento", objeto), celda("Estado", estadoWrap), celda("Observaciones", obs), acciones);
  fila.datos = () => ({
    ambiente: grupo.dataset.ambiente,
    objeto: objeto.value.trim(),
    estado: estado.value,
    observaciones: obs.value.trim(),
  });
  cuerpo.append(fila);
  return fila;
}

function crearGrupo(nombreOriginal) {
  const nombre = tituloAmbiente(nombreOriginal);
  const grupo = el("div", "inv-grupo");
  grupo.dataset.ambiente = nombre;
  const head = el("div", "inv-grupo__head");
  const titulo = el("h3", "inv-grupo__titulo", nombre);
  titulo.append(el("span", "inv-grupo__contador"));
  const quitar = el("button", "linklike", "Quitar ambiente");
  quitar.type = "button";
  quitar.addEventListener("click", () => {
    grupo.remove();
    actualizarInventarioUI();
  });
  head.append(titulo, quitar);
  const colHead = el("div", "inv__head");
  colHead.setAttribute("aria-hidden", "true");
  ["Elemento", "Estado", "Observaciones", ""].forEach((t) => colHead.append(el("span", "", t)));
  const cuerpo = el("div", "inv-grupo__body");
  const agregarBtn = el("button", "btn btn--ghost", "+ Agregar ítem");
  agregarBtn.type = "button";
  agregarBtn.addEventListener("click", () => {
    const fila = agregarItem(grupo, {});
    actualizarInventarioUI();
    fila.querySelector('input[aria-label="Elemento"]').focus();
  });
  grupo.append(head, colHead, cuerpo, agregarBtn);
  $("invSlot").append(grupo);
  return grupo;
}

function obtenerOCrearGrupo(nombre) {
  return buscarGrupo(nombre) || crearGrupo(nombre);
}

function renglones() {
  return [...$("invSlot").querySelectorAll(".inv__row")].map((f) => f.datos()).filter((r) => r.objeto);
}

// Cuenta por grupo, resumen general, marca en los chips el ambiente ya cargado
// y muestra/oculta el aviso de "todavía no cargaste nada". Se llama despues de
// cualquier cambio (alta, baja, edicion de un campo).
function actualizarInventarioUI() {
  document.querySelectorAll(".inv-grupo").forEach((grupo) => {
    const cantidad = [...grupo.querySelectorAll(".inv__row")].filter((f) => f.datos().objeto).length;
    grupo.querySelector(".inv-grupo__contador").textContent = cantidad ? ` (${cantidad})` : " (vacío)";
  });
  document.querySelectorAll(".chip").forEach((chip) => {
    chip.classList.toggle("chip--cargado", !!buscarGrupo(chip.dataset.ambiente));
  });
  const lista = renglones();
  const resumen = $("invResumen");
  if (lista.length === 0) {
    resumen.hidden = true;
  } else {
    const cantAmbientes = new Set(lista.map((r) => r.ambiente)).size;
    resumen.hidden = false;
    resumen.textContent = `${lista.length} elemento${lista.length === 1 ? "" : "s"} cargado${lista.length === 1 ? "" : "s"} en ${cantAmbientes} ambiente${cantAmbientes === 1 ? "" : "s"}.`;
  }
  $("invVacio").hidden = $("invSlot").children.length > 0;
}

function iniciarInventario() {
  const lista = el("datalist");
  lista.id = "ambientes";
  ["Cocina", "Baño", "Living", "Comedor", "Dormitorio", "Patio", "Cochera", "Lavadero"].forEach((a) => lista.append(new Option(a)));
  document.body.append(lista);

  Object.entries(PRESETS).forEach(([ambiente, items]) => {
    const chip = el("button", "chip", `+ ${ambiente}`);
    chip.type = "button";
    chip.dataset.ambiente = ambiente;
    chip.addEventListener("click", () => {
      const existente = buscarGrupo(ambiente);
      if (existente) {
        // Ya está cargado: llevar ahi en vez de duplicarlo (antes, tocar el chip
        // de nuevo agregaba los mismos elementos otra vez).
        existente.scrollIntoView({ behavior: "smooth", block: "center" });
        existente.classList.add("inv-grupo--resaltado");
        setTimeout(() => existente.classList.remove("inv-grupo--resaltado"), 900);
        return;
      }
      const grupo = crearGrupo(ambiente);
      items.forEach((objeto) => agregarItem(grupo, { objeto, estado: "BUENO" }));
      actualizarInventarioUI();
    });
    $("presets").append(chip);
  });

  $("otroAmbienteBtn").addEventListener("click", () => {
    const nombre = $("otroAmbienteInput").value.trim();
    if (!nombre) {
      $("otroAmbienteInput").focus();
      return;
    }
    const grupo = obtenerOCrearGrupo(nombre);
    const fila = agregarItem(grupo, {});
    $("otroAmbienteInput").value = "";
    actualizarInventarioUI();
    fila.querySelector('input[aria-label="Elemento"]').focus();
  });
  $("otroAmbienteInput").addEventListener("keydown", (e) => {
    if (e.key === "Enter") {
      e.preventDefault();
      $("otroAmbienteBtn").click();
    }
  });

  actualizarInventarioUI();
}

// ---------- validación, armado y revisión ----------

function erroresDelPaso(n) {
  const e = [];
  const dniPropietario = propietario.datos().dni;
  if (n === 1) {
    e.push(...propietario.errores());
    if (propietario.id !== null && $("propiedadSelect").value === "") {
      e.push($("propiedadSelectField").hidden
        ? { campo: null, mensaje: "Este propietario todavía no tiene propiedades. Registrala primero desde «Registrar propiedad»." }
        : { campo: $("propiedadSelect"), mensaje: "Elegí la propiedad del contrato." });
    }
  }
  if (n === 2) {
    inquilinos.forEach((p) => {
      e.push(...p.errores());
      if (p.datos().dni.length >= 7 && p.datos().dni === dniPropietario) {
        e.push({ campo: p.campoDni, contexto: p.titulo, mensaje: "El propietario no puede ser inquilino de su propia propiedad." });
      }
    });
  }
  if (n === 3 && tipoGarantiaExigeGarante()) {
    garantes.forEach((g) => {
      e.push(...g.errores());
      if (g.datos().dni.length >= 7 && inquilinos.some((i) => i.datos().dni === g.datos().dni)) {
        e.push({ campo: g.campoDni, contexto: g.titulo, mensaje: "Un garante no puede ser también inquilino del mismo contrato." });
      }
    });
  }
  if (n === 4) {
    const duracion = Number($("duracionMeses").value);
    const periodicidad = Number($("periodicidad").value);
    if (!$("fechaInicio").value) e.push({ campo: $("fechaInicio"), mensaje: "Elegí la fecha de inicio." });
    if (!(duracion >= 1)) e.push({ campo: $("duracionMeses"), mensaje: "La duración debe ser de al menos 1 mes." });
    if (!(Number($("valorInicial").value) > 0)) e.push({ campo: $("valorInicial"), mensaje: "Ingresá el alquiler mensual inicial." });
    if (!(periodicidad >= 1 && periodicidad <= duracion)) {
      e.push({ campo: $("periodicidad"), mensaje: "Tiene que ser de 1 mes como mínimo y no superar la duración." });
    }
  }
  if (n === 5 && renglones().length === 0) {
    e.push({
      campo: $("invSlot").querySelector('.inv__row input[aria-label="Elemento"]'),
      mensaje: "Cargá al menos un elemento del inventario (podés usar los ambientes de arriba).",
    });
  }
  return e;
}

function armarSolicitud() {
  const deposito = $("deposito").value;
  return {
    propietario: propietario.datos(),
    propiedad: { id: Number($("propiedadSelect").value) },
    inquilinos: inquilinos.map((p) => p.datos()),
    garantes: tipoGarantiaExigeGarante() ? garantes.map((p) => p.datos()) : [],
    fechaInicio: $("fechaInicio").value,
    duracionMeses: Number($("duracionMeses").value),
    valorInicial: Number($("valorInicial").value),
    periodicidadActualizacion: Number($("periodicidad").value),
    indiceActualizacion: valorRadio("indice"),
    destino: valorRadio("destino"),
    depositoGarantia: deposito === "" ? null : Number(deposito),
    tipoGarantia: valorRadio("tipoGarantia"),
    renglones: renglones(),
    plantilla: valorRadio("plantilla"),
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
  const cantAmbientesRev = new Set(s.renglones.map((r) => r.ambiente)).size;
  const review = $("review");
  review.innerHTML = "";
  const cifra = (num, cap, extra = "") => {
    const c = el("div", "review__cifra");
    c.append(el("span", `review__num${extra}`, num), el("span", "review__cap", cap));
    return c;
  };
  const cifras = el("div", "review__cifras");
  cifras.append(
    cifra(dinero.format(s.valorInicial), "por mes"),
    cifra(`${textoFechaISO(s.fechaInicio)} al ${fin}`, `${s.duracionMeses} meses de contrato`, " review__num--txt"),
  );
  const hero = el("div", "review__hero");
  hero.append(el("p", "review__prop", $("propiedadSelect").selectedOptions[0].textContent), cifras);
  review.append(
    hero,
    bloqueRevision("Partes", [
      ["Propietario", resumenPersona(s.propietario)],
      ...s.inquilinos.map((i, n) => [s.inquilinos.length > 1 ? `Inquilino ${n + 1}` : "Inquilino", resumenPersona(i)]),
    ]),
    bloqueRevision("Garantía", [
      ["Tipo", tipo],
      ...s.garantes.map((g, n) => [s.garantes.length > 1 ? `Garante ${n + 1}` : "Garante", resumenPersona(g) + (g.detalle ? ` (${g.detalle})` : "")]),
    ]),
    bloqueRevision("Condiciones", [
      ["Actualización", `Cada ${s.periodicidadActualizacion} meses por ${s.indiceActualizacion}`],
      ["Depósito", dinero.format(deposito)],
      ["Destino", s.destino === "VIVIENDA" ? "Vivienda" : "Comercial"],
      ["Modelo de contrato", s.plantilla === "completa" ? "Completo (20 cláusulas)" : "Estándar UBIKAR (14 cláusulas)"],
    ]),
    bloqueRevision("Inventario", [["Elementos", `${s.renglones.length} elemento${s.renglones.length === 1 ? "" : "s"} en ${cantAmbientesRev} ambiente${cantAmbientesRev === 1 ? "" : "s"}`]]),
  );
}

// ---------- ficha lateral ----------

function pintarFicha() {
  const p = propietario.datos();
  const select = $("propiedadSelect");
  const inquilinosCargados = inquilinos
    .map((x) => x.datos())
    .filter((d) => d.nombre || d.apellido)
    .map((d) => `${d.nombre} ${d.apellido}`.trim());
  const inicio = $("fechaInicio").value;
  const duracion = Number($("duracionMeses").value);
  const valor = Number($("valorInicial").value);
  $("fichaMonto").textContent = valor > 0 ? dinero.format(valor) : "—";
  $("fichaMonto").classList.toggle("ficha__monto--vacio", !(valor > 0));
  const filas = [
    ["Propiedad", select.value ? select.selectedOptions[0].textContent : ""],
    ["Propietario", propietario.id !== null ? `${p.nombre} ${p.apellido}` : ""],
    ["Inquilinos", inquilinosCargados.join(", ")],
    ["Vigencia", inicio && duracion >= 1 ? `${textoFechaISO(inicio)} al ${textoFecha(fechaFin(inicio, duracion))}` : ""],
  ];
  const dl = $("fichaFilas");
  dl.innerHTML = "";
  filas.forEach(([clave, valorFila]) => {
    const fila = el("div", "ficha__fila");
    fila.append(el("dt", "", clave), el("dd", valorFila ? "" : "ficha__vacio", valorFila || "Pendiente"));
    dl.append(fila);
  });
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

function irAPaso(n, { registrar = true } = {}) {
  paso = n;
  document.querySelectorAll(".wz-step").forEach((s) => {
    s.hidden = Number(s.dataset.step) !== n;
  });
  UX.limpiar($("stepError"));
  $("prevBtn").hidden = n === 1;
  $("nextBtn").textContent = n === PASOS.length ? "Generar contrato" : "Siguiente";
  if (n === 3) actualizarGarantes();
  if (n === 4) actualizarDerivados();
  if (n === PASOS.length) renderRevision();
  pintarFicha();
  pintarPasos();
  // Cada paso es una entrada del historial: "Atras" del navegador vuelve al paso anterior.
  if (registrar) history.pushState({ paso: n }, "", `#paso-${n}`);
  programarGuardado();
  const titulo = document.querySelector(`.wz-step[data-step="${n}"] .wz-step__title`);
  window.scrollTo({ top: 0, behavior: "smooth" });
  if (titulo) titulo.focus({ preventScroll: true });
}

function seccionActual() {
  return document.querySelector(`.wz-step[data-step="${paso}"]`);
}

function mostrarError(texto) {
  UX.mostrarGeneral(seccionActual(), $("stepError"), texto);
}

async function generar() {
  const boton = $("nextBtn");
  const solicitud = armarSolicitud();
  const propiedadTexto = $("propiedadSelect").selectedOptions[0].textContent;
  boton.disabled = true;
  boton.textContent = "Generando…";
  try {
    const respuesta = await fetch(`${API}/contratos`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(solicitud),
    });
    const cuerpo = await respuesta.json().catch(() => ({}));
    if (!respuesta.ok) {
      mostrarError(cuerpo.error || "El servidor no pudo registrar el contrato. Revisá los datos e intentá de nuevo.");
      return;
    }
    mostrarExito(cuerpo, solicitud, propiedadTexto);
  } catch (err) {
    mostrarError("No pudimos conectar con el servidor. ¿Está corriendo el backend en localhost:8081?");
  } finally {
    boton.disabled = false;
    boton.textContent = "Generar contrato";
  }
}

function mostrarExito(contrato, solicitud, propiedadTexto) {
  terminado = true;
  sucio = false;
  try {
    sessionStorage.removeItem(CLAVE_BORRADOR);
  } catch (err) {
    /* sin almacenamiento: no hay borrador que borrar */
  }
  $("cols").hidden = true;
  $("steps").hidden = true;
  $("borrador").hidden = true;
  $("exitoTitulo").textContent = `Contrato N.º ${contrato.id} registrado`;
  $("exitoDetalle").textContent =
    "Se guardó el contrato, el inventario y el documento en Word. Descargá el Word para imprimirlo y firmarlo.";
  const modelo = MODELOS.find(([v]) => v === solicitud.plantilla);
  const filas = [
    ["Propiedad", propiedadTexto],
    ["Propietario", `${solicitud.propietario.nombre} ${solicitud.propietario.apellido}`],
    ["Inquilinos", solicitud.inquilinos.map((i) => `${i.nombre} ${i.apellido}`).join(", ")],
    ["Alquiler mensual", dinero.format(solicitud.valorInicial)],
    ["Vigencia", `${textoFechaISO(contrato.fechaInicio)} al ${textoFechaISO(contrato.fechaFin)}`],
    ["Modelo de contrato", modelo ? modelo[1] : solicitud.plantilla],
  ];
  const dl = $("exitoDatos");
  dl.innerHTML = "";
  filas.forEach(([clave, valor]) => {
    const fila = el("div", "review__row");
    fila.append(el("dt", "", clave), el("dd", "", valor));
    dl.append(fila);
  });
  $("descargarLink").href = `${API_BASE}${contrato.documento}`;
  $("exito").hidden = false;
  $("exitoTitulo").focus();
  window.scrollTo({ top: 0, behavior: "smooth" });
}

// ---------- borrador: no perder lo cargado si se recarga o se toca "Atras" ----------
// Se guarda en sessionStorage (solo esta pestaña, se borra al cerrarla): son datos personales.

function estadoActual() {
  return {
    paso,
    propietarioDni: soloDigitos(propietario.campoDni.value),
    propiedadId: $("propiedadSelect").value,
    inquilinos: inquilinos.map((p) => p.datos()),
    garantes: garantes.map((p) => p.datos()),
    tipoGarantia: valorRadio("tipoGarantia"),
    fechaInicio: $("fechaInicio").value,
    duracionMeses: $("duracionMeses").value,
    valorInicial: $("valorInicial").value,
    periodicidad: $("periodicidad").value,
    deposito: $("deposito").value,
    indice: valorRadio("indice"),
    destino: valorRadio("destino"),
    plantilla: valorRadio("plantilla"),
    renglones: [...$("invSlot").querySelectorAll(".inv__row")].map((f) => f.datos()),
  };
}

function guardarBorrador() {
  if (!sucio || terminado || restaurando) return;
  try {
    sessionStorage.setItem(CLAVE_BORRADOR, JSON.stringify(estadoActual()));
  } catch (err) {
    /* sin almacenamiento: se sigue sin borrador */
  }
}

let temporizadorBorrador;
function programarGuardado() {
  clearTimeout(temporizadorBorrador);
  temporizadorBorrador = setTimeout(guardarBorrador, 300);
}

function ajustarPersonas(lista, slot, config, cantidad) {
  while (lista.length < cantidad) agregarPersona(lista, slot, config);
}

function restaurarBorrador() {
  let d = null;
  try {
    d = JSON.parse(sessionStorage.getItem(CLAVE_BORRADOR));
  } catch (err) {
    d = null;
  }
  if (!d) return;
  restaurando = true;
  if (d.propietarioDni) propietario.poner(d.propietarioDni);
  if (d.propiedadId && $("propiedadSelect").querySelector(`option[value="${d.propiedadId}"]`)) {
    $("propiedadSelect").value = d.propiedadId;
  }
  ajustarPersonas(inquilinos, $("inquilinosSlot"), CONFIG_INQUILINO, d.inquilinos.length);
  d.inquilinos.forEach((x, i) => inquilinos[i].cargar(x));
  ajustarPersonas(garantes, $("garantesSlot"), CONFIG_GARANTE, d.garantes.length);
  d.garantes.forEach((x, i) => garantes[i].cargar(x));
  fijarRadio("tipoGarantia", d.tipoGarantia);
  fijarRadio("indice", d.indice);
  fijarRadio("destino", d.destino);
  fijarRadio("plantilla", d.plantilla);
  $("fechaInicio").value = d.fechaInicio || "";
  $("duracionMeses").value = d.duracionMeses;
  $("valorInicial").value = d.valorInicial;
  $("periodicidad").value = d.periodicidad;
  $("deposito").value = d.deposito;
  $("invSlot").innerHTML = "";
  d.renglones.forEach((r) => agregarItem(obtenerOCrearGrupo(r.ambiente || "Sin especificar"), r));
  actualizarInventarioUI();
  actualizarGarantes();
  actualizarDerivados();
  restaurando = false;
  sucio = true;
  const destino = Math.min(Math.max(d.paso || 1, 1), PASOS.length - 1);
  history.replaceState({ paso: destino }, "", `#paso-${destino}`);
  irAPaso(destino, { registrar: false });
  $("borrador").hidden = false;
}

function iniciarBorrador() {
  const texto = "Recuperamos lo que estabas cargando.";
  $("descartarBtn").addEventListener("click", () => {
    $("descartarBtn").hidden = true;
    $("confirmarBloque").hidden = false;
    $("borradorTexto").textContent = "¿Descartar todo lo que cargaste? No se puede deshacer.";
    $("confirmarNo").focus();
  });
  $("confirmarNo").addEventListener("click", () => {
    $("confirmarBloque").hidden = true;
    $("descartarBtn").hidden = false;
    $("borradorTexto").textContent = texto;
    $("descartarBtn").focus();
  });
  $("confirmarSi").addEventListener("click", () => {
    sucio = false;
    try {
      sessionStorage.removeItem(CLAVE_BORRADOR);
    } catch (err) {
      /* nada que borrar */
    }
    history.replaceState({ paso: 1 }, "", "#paso-1");
    window.location.reload();
  });
}

// ---------- eventos ----------

function marcarCambio() {
  sucio = true;
  programarGuardado();
}

$("nextBtn").addEventListener("click", () => {
  const errores = erroresDelPaso(paso);
  if (errores.length) {
    UX.mostrar(seccionActual(), $("stepError"), errores);
    return;
  }
  if (paso === PASOS.length) {
    generar();
    return;
  }
  irAPaso(paso + 1);
});

$("prevBtn").addEventListener("click", () => irAPaso(paso - 1));
$("wizard").addEventListener("input", (e) => {
  UX.alEditar(e.target, $("stepError"));
  pintarFicha();
  actualizarInventarioUI();
  marcarCambio();
});
$("wizard").addEventListener("change", (e) => {
  UX.alEditar(e.target, $("stepError"));
  pintarFicha();
  actualizarInventarioUI();
  marcarCambio();
});
$("wizard").addEventListener("click", programarGuardado);
// Enter en cualquier campo hace lo mismo que el boton principal.
$("wizard").addEventListener("keydown", (e) => {
  if (e.key === "Enter" && !e.defaultPrevented && e.target.tagName === "INPUT") {
    e.preventDefault();
    $("nextBtn").click();
  }
});
$("wizard").addEventListener("submit", (e) => e.preventDefault());

window.addEventListener("popstate", (e) => {
  if (terminado) {
    window.location.replace(window.location.pathname);
    return;
  }
  if (e.state && e.state.paso) irAPaso(e.state.paso, { registrar: false });
});
window.addEventListener("beforeunload", (e) => {
  if (sucio && !terminado) {
    e.preventDefault();
    e.returnValue = "";
  }
});

document.querySelectorAll(".wz-step").forEach((s, i) => {
  const t = s.querySelector(".wz-step__title");
  t.before(el("p", "wz-step__eyebrow", `Paso ${i + 1} de ${PASOS.length}`));
});
$("otroBtn").addEventListener("click", () => window.location.reload());
$("addInquilinoBtn").addEventListener("click", () => agregarPersona(inquilinos, $("inquilinosSlot"), CONFIG_INQUILINO));
$("addGaranteBtn").addEventListener("click", () => agregarPersona(garantes, $("garantesSlot"), CONFIG_GARANTE));
$("tipoGarantia").addEventListener("change", actualizarGarantes);
["fechaInicio", "duracionMeses", "valorInicial", "deposito"].forEach((id) => $(id).addEventListener("input", actualizarDerivados));

// ---------- arranque ----------

async function cargarCatalogos() {
  document.body.classList.add("cargando");
  const rutas = { propietarios: "propietarios", inquilinos: "inquilinos", garantes: "garantes", propiedades: "propiedades" };
  try {
    await Promise.all(Object.entries(rutas).map(async ([clave, ruta]) => {
      const r = await fetch(`${API}/${ruta}`);
      if (!r.ok) throw new Error(ruta);
      catalogo[clave] = await r.json();
    }));
    return true;
  } catch (err) {
    $("offline").hidden = false;
    return false;
  } finally {
    document.body.classList.remove("cargando");
  }
}

renderRadios("tipoGarantia", "tipoGarantia", TIPOS_GARANTIA.map(([v, t]) => [v, t, DESCRIPCION_GARANTIA[v]]), "FIADOR_SOLIDARIO");
renderRadios("indice", "indice", INDICES, "ICL");
renderRadios("destino", "destino", DESTINOS, "VIVIENDA");
renderRadios("plantilla", "plantilla", MODELOS, "estandar");
iniciarPropietario();
agregarPersona(inquilinos, $("inquilinosSlot"), CONFIG_INQUILINO);
iniciarInventario();
iniciarBorrador();
history.replaceState({ paso: 1 }, "", "#paso-1");
irAPaso(1, { registrar: false });
cargarCatalogos().then((cargado) => {
  if (cargado) restaurarBorrador();
});
