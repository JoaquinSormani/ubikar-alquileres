"use strict";

const API_BASE = "http://localhost:8081";
const API = `${API_BASE}/api`;

const PASOS = ["Propiedad", "Características", "Propietario"];
// Zonas típicas que se ofrecen para agregar con un toque (cada una arranca con cantidad 1).
const PRESETS = ["Dormitorio", "Baño", "Cocina", "Living"];
const ZONAS_SUGERIDAS = ["Comedor", "Patio", "Terraza", "Balcón", "Cochera", "Lavadero", "Jardín", "Quincho"];
const catalogo = { propietarios: [], inquilinos: [], garantes: [], propiedades: [] };
const $ = (id) => document.getElementById(id);

let paso = 1;
let propietario;
let terminado = false;
const caracteristicas = [];

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

function normalizar(texto) {
  return texto.trim().replace(/\s+/g, " ").toLowerCase();
}

// ---------- bloque de datos del propietario ----------

function crearPersona({ onBuscar } = {}) {
  const nodo = $("tplPersona").content.firstElementChild.cloneNode(true);
  const campo = (nombre) => nodo.querySelector(`[data-f="${nombre}"]`);
  const estado = nodo.querySelector(".persona__estado");
  const persona = { nodo, id: null, buscado: false };
  let ultimoBuscado = "";

  function mensaje(texto, clase) {
    estado.textContent = texto;
    estado.className = `persona__estado${clase ? ` persona__estado--${clase}` : ""}`;
  }

  function llenar(p) {
    ["nombre", "apellido", "domicilio", "telefono", "email"].forEach((n) => {
      campo(n).value = p[n] || "";
    });
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
    persona.buscado = true;
    const propio = catalogo.propietarios.find((p) => p.dni === dni);
    if (propio) {
      persona.id = propio.id;
      llenar(propio);
      mensaje("Ya registrado en UBIKAR. Si corregís algún dato, se actualiza al guardar la propiedad.", "ok");
      if (onBuscar) onBuscar();
      return;
    }
    persona.id = null;
    const otroRol = [...catalogo.inquilinos, ...catalogo.garantes].find((p) => p.dni === dni);
    if (otroRol) {
      llenar(otroRol);
      mensaje("Esta persona ya figura en UBIKAR con otro rol: se copian sus datos y se da de alta como propietario.", "new");
    } else {
      mensaje("DNI nuevo: se da de alta junto con la propiedad.", "new");
    }
    if (onBuscar) onBuscar();
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
      persona.buscado = false;
      mensaje("", "");
      if (onBuscar) onBuscar();
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
    return d;
  };

  persona.errores = () => {
    const d = persona.datos();
    const e = [];
    const falta = (nombre, mensaje) => e.push({ campo: campo(nombre), mensaje });
    if (d.dni.length < 7) falta("dni", "Ingresá un DNI de 7 u 8 dígitos.");
    if (!d.nombre) falta("nombre", "Falta el nombre.");
    if (!d.apellido) falta("apellido", "Falta el apellido.");
    if (!d.domicilio) falta("domicilio", "Falta el domicilio.");
    if (d.email && !/^\S+@\S+\.\S+$/.test(d.email)) falta("email", "El correo no parece válido. Ejemplo: nombre@correo.com.");
    return e;
  };

  return persona;
}

// ---------- paso 2: características de la propiedad ----------
// Cada característica es una zona con su cantidad (3 Baños). No se repite el nombre: agregar una zona
// que ya está cargada la resalta en lugar de duplicarla.

// Las zonas se guardan en singular (Baño) y se muestran según la cantidad (1 Baño, 3 Baños).
const PLURALES_ESPECIALES = { living: "Livings", comedor: "Comedores", balcón: "Balcones", jardín: "Jardines", altillo: "Altillos" };

function plural(nombre) {
  const n = nombre.trim();
  const especial = PLURALES_ESPECIALES[normalizar(n)];
  if (especial) return especial;
  // varias palabras (Terraza grande): no hay regla segura, se marca con (s)
  if (/\s/.test(n)) return `${n}(s)`;
  if (/[aeiouáéíóú]$/i.test(n)) return `${n}s`;
  if (/z$/i.test(n)) return `${n.slice(0, -1)}ces`;
  if (/[lrndj]$/i.test(n)) return `${n}es`;
  return `${n}(s)`;
}

// Para comparar zonas se ignoran mayúsculas, espacios de más y tildes (Jardin = Jardín).
function clave(texto) {
  return normalizar(texto).normalize("NFD").replace(/[̀-ͯ]/g, "");
}

function etiquetaZona(nombre, cantidad) {
  return cantidad === 1 ? nombre : plural(nombre);
}

// "Baños" o "baño" -> "Baño": si lo escrito es el plural de una zona conocida, se guarda el singular.
function canonico(texto) {
  const buscada = clave(texto);
  const conocidas = [...PRESETS, ...ZONAS_SUGERIDAS, ...Object.keys(PLURALES_ESPECIALES)];
  const hallada = conocidas.find((z) => clave(z) === buscada || clave(plural(z)) === buscada);
  return hallada ? nombreZona(hallada) : nombreZona(texto);
}

function nombreZona(texto) {
  const limpio = texto.trim().replace(/\s+/g, " ");
  return limpio.charAt(0).toUpperCase() + limpio.slice(1);
}

function buscarZona(nombre) {
  const buscada = clave(canonico(nombre));
  return caracteristicas.find((c) => clave(c.nombre) === buscada);
}

function resaltarZona(nombre) {
  const clave = normalizar(nombre);
  const fila = [...$("carLista").children].find((f) => normalizar(f.dataset.nombre) === clave);
  if (!fila) return;
  fila.scrollIntoView({ behavior: "smooth", block: "center" });
  fila.classList.add("car__fila--resaltada");
  setTimeout(() => fila.classList.remove("car__fila--resaltada"), 900);
}

function agregarZona(nombre) {
  const existente = buscarZona(nombre);
  if (existente) {
    resaltarZona(existente.nombre);
    return;
  }
  caracteristicas.push({ nombre: canonico(nombre), cantidad: 1 });
  pintarCaracteristicas();
}

function cambiarCantidad(zona, valor) {
  const n = Math.round(Number(valor));
  zona.cantidad = Number.isFinite(n) ? Math.min(99, Math.max(1, n)) : zona.cantidad;
  pintarCaracteristicas();
}

function pintarCaracteristicas() {
  const lista = $("carLista");
  lista.innerHTML = "";
  caracteristicas.forEach((zona) => {
    const fila = el("li", "car__fila");
    fila.dataset.nombre = zona.nombre;
    const nombre = el("span", "car__nombre", etiquetaZona(zona.nombre, zona.cantidad));
    const menos = el("button", "car__paso", "−");
    menos.type = "button";
    menos.setAttribute("aria-label", `Una ${zona.nombre} menos`);
    menos.disabled = zona.cantidad <= 1;
    menos.addEventListener("click", () => cambiarCantidad(zona, zona.cantidad - 1));
    const cantidad = el("input", "field__input field__input--mono car__cantidad");
    cantidad.type = "number";
    cantidad.min = "1";
    cantidad.max = "99";
    cantidad.inputMode = "numeric";
    cantidad.value = String(zona.cantidad);
    cantidad.setAttribute("aria-label", `Cantidad de ${zona.nombre}`);
    cantidad.addEventListener("change", () => cambiarCantidad(zona, cantidad.value));
    const mas = el("button", "car__paso", "+");
    mas.type = "button";
    mas.setAttribute("aria-label", `Una ${zona.nombre} más`);
    mas.disabled = zona.cantidad >= 99;
    mas.addEventListener("click", () => cambiarCantidad(zona, zona.cantidad + 1));
    const quitar = el("button", "linklike", "Quitar");
    quitar.type = "button";
    quitar.setAttribute("aria-label", `Quitar ${zona.nombre}`);
    quitar.addEventListener("click", () => {
      caracteristicas.splice(caracteristicas.indexOf(zona), 1);
      pintarCaracteristicas();
    });
    const control = el("div", "car__control");
    control.append(menos, cantidad, mas);
    fila.append(nombre, control, quitar);
    lista.append(fila);
  });
  document.querySelectorAll("#carPresets .chip").forEach((chip) => {
    chip.classList.toggle("chip--cargado", !!buscarZona(chip.dataset.zona));
  });
  const resumen = $("carResumen");
  resumen.hidden = caracteristicas.length === 0;
  resumen.textContent = textoCaracteristicas();
  $("carVacio").hidden = caracteristicas.length > 0;
}

function textoCaracteristicas() {
  return caracteristicas.map((c) => `${c.cantidad} ${etiquetaZona(c.nombre, c.cantidad)}`).join(" · ");
}

function iniciarCaracteristicas() {
  const sugeridas = el("datalist");
  sugeridas.id = "zonas";
  ZONAS_SUGERIDAS.forEach((z) => sugeridas.append(new Option(z)));
  document.body.append(sugeridas);
  PRESETS.forEach((zona) => {
    const chip = el("button", "chip", `+ ${plural(zona)}`);
    chip.type = "button";
    chip.dataset.zona = zona;
    chip.addEventListener("click", () => agregarZona(zona));
    $("carPresets").append(chip);
  });
  $("otraCarBtn").addEventListener("click", () => {
    const nombre = $("otraCarInput").value;
    if (!nombre.trim()) {
      $("otraCarInput").focus();
      return;
    }
    agregarZona(nombre);
    $("otraCarInput").value = "";
    $("otraCarInput").focus();
  });
  $("otraCarInput").addEventListener("keydown", (e) => {
    if (e.key === "Enter") {
      e.preventDefault();
      $("otraCarBtn").click();
    }
  });
  pintarCaracteristicas();
}

// ---------- paso 3: propietario ----------

function propiedadesDelPropietario() {
  const id = propietario.datos().id;
  return id === undefined ? [] : catalogo.propiedades.filter((p) => p.idPropietario === id);
}

function actualizarInfoPropietario() {
  // Antes de buscar un DNI no hay nada que mostrar todavía.
  if (!propietario.buscado) {
    $("avisoNuevo").hidden = true;
    $("existentes").hidden = true;
    return;
  }
  const d = propietario.datos();
  const esNuevo = d.id === undefined;
  $("avisoNuevo").hidden = !esNuevo;

  const lista = $("existentesLista");
  lista.innerHTML = "";
  const propias = propiedadesDelPropietario();
  $("existentes").hidden = esNuevo;
  if (!esNuevo) {
    if (propias.length === 0) {
      lista.append(el("li", "existentes__vacio", "Todavía no tiene propiedades registradas."));
    } else {
      propias.forEach((p, i) => {
        const item = el("li", "existentes__item", p.direccion);
        item.style.animationDelay = `${i * 40}ms`;
        lista.append(item);
      });
    }
  }
}

function pintarPaso3() {
  $("resumenDireccion").textContent = $("direccionInput").value.trim();
  actualizarInfoPropietario();
}

// ---------- validacion y armado del pedido ----------

function erroresDelPaso(n) {
  if (n === 1) {
    const campoDireccion = $("direccionInput");
    const direccion = campoDireccion.value.trim();
    if (direccion.length < 5) {
      return [{ campo: campoDireccion, mensaje: "Ingresá la dirección de la propiedad: calle, número y, si corresponde, piso o departamento." }];
    }
    return [];
  }
  if (n === 2) {
    if (caracteristicas.length === 0) {
      return [{ campo: $("otraCarInput"), mensaje: "Cargá al menos una característica de la propiedad (podés usar las zonas de arriba)." }];
    }
    return [];
  }
  if (n === 3) {
    const errores = propietario.errores();
    if (errores.length) return errores;
    // Recien acá se sabe quién es el propietario, así que el duplicado se controla en este paso.
    const direccion = $("direccionInput").value.trim();
    if (propiedadesDelPropietario().some((p) => normalizar(p.direccion) === normalizar(direccion))) {
      return [{ campo: null, mensaje: "Este propietario ya tiene una propiedad registrada con esa dirección." }];
    }
  }
  return [];
}

function armarSolicitud() {
  return {
    propietario: propietario.datos(),
    direccion: $("direccionInput").value.trim().replace(/\s+/g, " "),
    caracteristicas: caracteristicas.map((c) => ({ nombre: c.nombre, cantidad: c.cantidad })),
  };
}

// ---------- navegacion ----------

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
  $("nextBtn").textContent = n === PASOS.length ? "Registrar propiedad" : "Siguiente";
  if (n === 2) $("resumenDireccionCar").textContent = $("direccionInput").value.trim();
  if (n === 3) pintarPaso3();
  pintarPasos();
  // Cada paso es una entrada del historial: "Atras" del navegador vuelve al paso anterior.
  if (registrar) history.pushState({ paso: n }, "", `#paso-${n}`);
  const titulo = document.querySelector(`.wz-step[data-step="${n}"] .wz-step__title`);
  window.scrollTo({ top: 0, behavior: "smooth" });
  // en escritorio solo se desplaza el panel del paso: se lo vuelve a subir
  document.querySelectorAll(".wz-step").forEach((s) => { s.scrollTop = 0; });
  if (titulo) titulo.focus({ preventScroll: true });
  if (n === 1) $("direccionInput").focus({ preventScroll: true });
  if (n === 3) {
    const campoDni = $("propietarioSlot").querySelector('[data-f="dni"]');
    if (campoDni) campoDni.focus({ preventScroll: true });
  }
}

function seccionActual() {
  return document.querySelector(`.wz-step[data-step="${paso}"]`);
}

function mostrarError(texto) {
  UX.mostrarGeneral(seccionActual(), $("stepError"), texto);
}

async function registrar() {
  const boton = $("nextBtn");
  boton.disabled = true;
  boton.textContent = "Registrando…";
  const solicitud = armarSolicitud();
  const eraNuevo = solicitud.propietario.id === undefined;
  try {
    const respuesta = await fetch(`${API}/propiedades`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(solicitud),
    });
    const cuerpo = await respuesta.json().catch(() => ({}));
    if (respuesta.ok && cuerpo.id !== undefined) {
      mostrarExito(cuerpo, solicitud, eraNuevo);
    } else if (respuesta.ok) {
      mostrarError("El servidor respondió sin el número de la propiedad registrada. Revisá que el backend devuelva el campo «id».");
    } else if (respuesta.status === 404 || respuesta.status === 405) {
      mostrarError("El servidor todavía no tiene implementado el registro de propiedades (POST /api/propiedades).");
    } else if (respuesta.status === 400) {
      mostrarError(cuerpo.error || "Los datos no son válidos. Revisalos e intentá de nuevo.");
    } else {
      mostrarError("El servidor no pudo registrar la propiedad. Intentá de nuevo en unos minutos.");
    }
  } catch (err) {
    mostrarError("No pudimos conectar con el servidor. ¿Está corriendo el backend en localhost:8081?");
  } finally {
    boton.disabled = false;
    boton.textContent = "Registrar propiedad";
  }
}

function mostrarExito(respuesta, solicitud, eraNuevo) {
  terminado = true;
  $("wizard").hidden = true;
  $("steps").hidden = true;
  $("exitoTitulo").textContent = `Propiedad N.º ${respuesta.id} registrada`;
  $("exitoDetalle").textContent = eraNuevo
    ? "Se dio de alta la propiedad y también al propietario."
    : "La propiedad quedó asociada al propietario.";
  const p = solicitud.propietario;
  const filas = [
    ["Dirección", respuesta.direccion || solicitud.direccion],
    ["Características", solicitud.caracteristicas.map((c) => `${c.cantidad} ${etiquetaZona(c.nombre, c.cantidad)}`).join(" · ")],
    ["Propietario", `${p.nombre} ${p.apellido}`],
    ["DNI", p.dni],
  ];
  const dl = $("exitoDatos");
  dl.innerHTML = "";
  filas.forEach(([clave, valor]) => {
    const fila = el("div", "review__row");
    fila.append(el("dt", "", clave), el("dd", "", valor));
    dl.append(fila);
  });
  $("exito").hidden = false;
  $("exitoTitulo").focus();
  window.scrollTo({ top: 0, behavior: "smooth" });
}

$("nextBtn").addEventListener("click", () => {
  const errores = erroresDelPaso(paso);
  if (errores.length) {
    UX.mostrar(seccionActual(), $("stepError"), errores);
    return;
  }
  if (paso === PASOS.length) {
    registrar();
    return;
  }
  irAPaso(paso + 1);
});

$("prevBtn").addEventListener("click", () => irAPaso(paso - 1));
$("cambiarBtn").addEventListener("click", () => irAPaso(1));
$("cambiarCarBtn").addEventListener("click", () => irAPaso(1));
document.querySelectorAll(".wz-step").forEach((s, i) => {
  const t = s.querySelector(".wz-step__title");
  t.before(el("p", "wz-step__eyebrow", `Paso ${i + 1} de ${PASOS.length}`));
});
$("wizard").addEventListener("submit", (e) => e.preventDefault());
$("wizard").addEventListener("input", (e) => UX.alEditar(e.target, $("stepError")));
$("wizard").addEventListener("change", (e) => UX.alEditar(e.target, $("stepError")));
// Enter en cualquier campo hace lo mismo que el boton principal.
$("wizard").addEventListener("keydown", (e) => {
  if (e.key === "Enter" && !e.defaultPrevented && e.target.tagName === "INPUT") {
    e.preventDefault();
    $("nextBtn").click();
  }
});
window.addEventListener("popstate", (e) => {
  if (terminado) {
    window.location.replace(window.location.pathname);
    return;
  }
  if (e.state && e.state.paso) irAPaso(e.state.paso, { registrar: false });
});
$("otraBtn").addEventListener("click", () => window.location.reload());

// ---------- arranque ----------

async function cargarCatalogos() {
  document.body.classList.add("cargando");
  try {
    await Promise.all(Object.keys(catalogo).map(async (clave) => {
      const r = await fetch(`${API}/${clave}`);
      if (!r.ok) throw new Error(clave);
      catalogo[clave] = await r.json();
    }));
  } catch (err) {
    $("offline").hidden = false;
  } finally {
    document.body.classList.remove("cargando");
  }
}

propietario = crearPersona({ onBuscar: actualizarInfoPropietario });
$("propietarioSlot").append(propietario.nodo);
iniciarCaracteristicas();
history.replaceState({ paso: 1 }, "", "#paso-1");
irAPaso(1, { registrar: false });
cargarCatalogos();
