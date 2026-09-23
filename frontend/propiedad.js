"use strict";

const API_BASE = "http://localhost:8081";
const API = `${API_BASE}/api`;

const PASOS = ["Propietario", "Propiedad"];
const catalogo = { propietarios: [], inquilinos: [], garantes: [], propiedades: [] };
const $ = (id) => document.getElementById(id);

let paso = 1;
let propietario;

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

function crearPersona() {
  const nodo = $("tplPersona").content.firstElementChild.cloneNode(true);
  const campo = (nombre) => nodo.querySelector(`[data-f="${nombre}"]`);
  const estado = nodo.querySelector(".persona__estado");
  const persona = { nodo, id: null };
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
    ultimoBuscado = dni;
    campo("dni").value = dni;
    const propio = catalogo.propietarios.find((p) => p.dni === dni);
    if (propio) {
      persona.id = propio.id;
      llenar(propio);
      mensaje("Ya registrado en UBIKAR. Si corregís algún dato, se actualiza al guardar la propiedad.", "ok");
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

  persona.error = () => {
    const d = persona.datos();
    if (d.dni.length < 7) return "Ingresá un DNI válido para el propietario.";
    if (!d.nombre || !d.apellido) return "Completá el nombre y el apellido del propietario.";
    if (!d.domicilio) return "Falta el domicilio del propietario.";
    if (d.email && !/^\S+@\S+\.\S+$/.test(d.email)) return "El correo electrónico no parece válido.";
    return "";
  };

  return persona;
}

// ---------- paso 2: propiedad ----------

function propiedadesDelPropietario() {
  const id = propietario.datos().id;
  return id === undefined ? [] : catalogo.propiedades.filter((p) => p.idPropietario === id);
}

function pintarPaso2() {
  const d = propietario.datos();
  $("resumenNombre").textContent = `${d.nombre} ${d.apellido}`;
  $("resumenDni").textContent = d.dni;
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

// ---------- validacion y armado del pedido ----------

function errorDelPaso(n) {
  if (n === 1) return propietario.error();
  if (n === 2) {
    const direccion = $("direccionInput").value.trim();
    if (direccion.length < 5) return "Ingresá la dirección del inmueble (calle, número y, si corresponde, piso o departamento).";
    if (propiedadesDelPropietario().some((p) => normalizar(p.direccion) === normalizar(direccion))) {
      return "Este propietario ya tiene una propiedad registrada con esa dirección.";
    }
  }
  return "";
}

function armarSolicitud() {
  return {
    propietario: propietario.datos(),
    direccion: $("direccionInput").value.trim().replace(/\s+/g, " "),
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

function irAPaso(n) {
  paso = n;
  document.querySelectorAll(".wz-step").forEach((s) => {
    s.hidden = Number(s.dataset.step) !== n;
  });
  $("stepError").hidden = true;
  $("prevBtn").hidden = n === 1;
  $("nextBtn").textContent = n === PASOS.length ? "Registrar propiedad" : "Siguiente";
  if (n === 2) pintarPaso2();
  pintarPasos();
  const titulo = document.querySelector(`.wz-step[data-step="${n}"] .wz-step__title`);
  window.scrollTo({ top: 0, behavior: "smooth" });
  if (titulo) titulo.focus({ preventScroll: true });
  if (n === 2) $("direccionInput").focus({ preventScroll: true });
}

function mostrarError(texto) {
  const caja = $("stepError");
  caja.textContent = texto;
  caja.hidden = false;
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
  $("wizard").hidden = true;
  $("steps").hidden = true;
  $("exitoTitulo").textContent = `Propiedad N.º ${respuesta.id} registrada`;
  $("exitoDetalle").textContent = eraNuevo
    ? "Se dio de alta la propiedad y también al propietario."
    : "La propiedad quedó asociada al propietario.";
  const p = solicitud.propietario;
  const filas = [
    ["Dirección", respuesta.direccion || solicitud.direccion],
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
  const error = errorDelPaso(paso);
  if (error) {
    mostrarError(error);
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
$("wizard").addEventListener("submit", (e) => e.preventDefault());
$("direccionInput").addEventListener("keydown", (e) => {
  if (e.key === "Enter") {
    e.preventDefault();
    $("nextBtn").click();
  }
});
$("otraBtn").addEventListener("click", () => window.location.reload());

// ---------- arranque ----------

async function cargarCatalogos() {
  try {
    await Promise.all(Object.keys(catalogo).map(async (clave) => {
      const r = await fetch(`${API}/${clave}`);
      if (!r.ok) throw new Error(clave);
      catalogo[clave] = await r.json();
    }));
  } catch (err) {
    $("offline").hidden = false;
  }
}

propietario = crearPersona();
$("propietarioSlot").append(propietario.nodo);
irAPaso(1);
cargarCatalogos();
