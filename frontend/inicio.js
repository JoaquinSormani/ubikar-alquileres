"use strict";

/*
 * Acceso del Responsable de UBIKAR. Todavia no hay backend de autenticacion
 * (queda como Requerimiento Especial pendiente), asi que esto es una puerta
 * de acceso solo del lado del front, pensada para la demo de despliegue.
 * La "sesion" se guarda en sessionStorage: dura mientras la pestaña esta
 * abierta, para que volver al inicio (por el logo o por el link explicito)
 * no obligue a loguearse de nuevo en cada paso.
 */
const CLAVE_SESION = "ubikar.responsable.sesion";
const CLAVE_NOMBRE = "ubikar.responsable.nombre";
const USUARIO = "responsable";
const CLAVE = "ubikar2026";

// Mismo trazo que los iconos de las tarjetas (document/edificio), para que el
// saludo use el mismo lenguaje visual: sol de dia, atardecer, luna de noche.
const ICONO_SOL = '<svg viewBox="0 0 48 48" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><circle cx="24" cy="24" r="9"/><path d="M24 4v6M24 38v6M4 24h6M38 24h6M9.5 9.5l4.2 4.2M34.3 34.3l4.2 4.2M9.5 38.5l4.2-4.2M34.3 13.7l4.2-4.2"/></svg>';
const ICONO_ATARDECER = '<svg viewBox="0 0 48 48" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M8 32h32"/><path d="M15 32a9 9 0 0 1 18 0"/><path d="M24 10v6M11 20l4.2 4.2M37 20l-4.2 4.2"/></svg>';
const ICONO_LUNA = '<svg viewBox="0 0 48 48" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M42 25.58A18 18 0 1 1 22.42 6 14 14 0 0 0 42 25.58Z"/></svg>';

const $ = (id) => document.getElementById(id);

function haySesion() {
  try {
    return sessionStorage.getItem(CLAVE_SESION) === "1";
  } catch (err) {
    return false;
  }
}

function iniciarSesion(nombre) {
  try {
    sessionStorage.setItem(CLAVE_SESION, "1");
    sessionStorage.setItem(CLAVE_NOMBRE, nombre);
  } catch (err) {
    /* sin almacenamiento: la sesion no persiste entre paginas, pero el login sigue funcionando */
  }
}

function cerrarSesion() {
  try {
    sessionStorage.removeItem(CLAVE_SESION);
    sessionStorage.removeItem(CLAVE_NOMBRE);
  } catch (err) {
    /* nada que borrar */
  }
}

function nombreGuardado() {
  try {
    return sessionStorage.getItem(CLAVE_NOMBRE) || USUARIO;
  } catch (err) {
    return USUARIO;
  }
}

function capitalizar(texto) {
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

// 3 franjas horarias, cada una con su icono: sol de dia, atardecer, luna de noche.
function saludoSegunHora() {
  const hora = new Date().getHours();
  if (hora < 12) return { texto: "Buenos días", icono: ICONO_SOL };
  if (hora < 20) return { texto: "Buenas tardes", icono: ICONO_ATARDECER };
  return { texto: "Buenas noches", icono: ICONO_LUNA };
}

function mostrarInicio() {
  $("loginView").hidden = true;
  $("inicioBar").hidden = false;
  $("inicioView").hidden = false;
  const saludo = saludoSegunHora();
  $("saludoHora").textContent = saludo.texto;
  $("iconoHora").innerHTML = saludo.icono;
  $("nombreUsuario").textContent = capitalizar(nombreGuardado());
}

function mostrarLogin() {
  $("inicioBar").hidden = true;
  $("inicioView").hidden = true;
  $("loginView").hidden = false;
  // preventScroll: el panel de marca es alto en mobile; sin esto, enfocar el campo
  // scrollea de entrada hasta el formulario y tapa el logo/la frase/el skyline.
  $("usuarioInput").focus({ preventScroll: true });
}

$("loginForm").addEventListener("submit", (e) => {
  e.preventDefault();
  const usuario = $("usuarioInput").value.trim().toLowerCase();
  const clave = $("claveInput").value;
  if (usuario === USUARIO && clave === CLAVE) {
    $("loginError").hidden = true;
    iniciarSesion(usuario);
    mostrarInicio();
    return;
  }
  $("loginError").textContent = "Usuario o contraseña incorrectos.";
  $("loginError").hidden = false;
  $("claveInput").value = "";
  $("claveInput").focus();
});

$("logoutBtn").addEventListener("click", () => {
  cerrarSesion();
  $("loginForm").reset();
  mostrarLogin();
});

if (haySesion()) mostrarInicio();
else mostrarLogin();
