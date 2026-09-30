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
const USUARIO = "responsable";
const CLAVE = "ubikar2026";

const $ = (id) => document.getElementById(id);

function haySesion() {
  try {
    return sessionStorage.getItem(CLAVE_SESION) === "1";
  } catch (err) {
    return false;
  }
}

function iniciarSesion() {
  try {
    sessionStorage.setItem(CLAVE_SESION, "1");
  } catch (err) {
    /* sin almacenamiento: la sesion no persiste entre paginas, pero el login sigue funcionando */
  }
}

function cerrarSesion() {
  try {
    sessionStorage.removeItem(CLAVE_SESION);
  } catch (err) {
    /* nada que borrar */
  }
}

function saludoSegunHora() {
  const hora = new Date().getHours();
  if (hora < 12) return "Buenos días";
  if (hora < 20) return "Buenas tardes";
  return "Buenas noches";
}

function mostrarInicio() {
  $("loginView").hidden = true;
  $("inicioView").hidden = false;
  $("saludoHora").textContent = saludoSegunHora();
}

function mostrarLogin() {
  $("inicioView").hidden = true;
  $("loginView").hidden = false;
  $("usuarioInput").focus();
}

$("loginForm").addEventListener("submit", (e) => {
  e.preventDefault();
  const usuario = $("usuarioInput").value.trim().toLowerCase();
  const clave = $("claveInput").value;
  if (usuario === USUARIO && clave === CLAVE) {
    $("loginError").hidden = true;
    iniciarSesion();
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
