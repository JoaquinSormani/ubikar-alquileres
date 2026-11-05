"use strict";

/*
 * Errores de formulario, segun "Miro y Entiendo" (Mordecki), cap. 6:
 * - el mensaje va junto al campo, en rojo, precedido de "Error:", y el campo se marca;
 * - si hay mas de un error (o alguno no pertenece a un campo) se muestra ademas un resumen
 *   arriba del paso, con el foco, donde cada punto lleva al campo;
 * - si hay un solo error de campo, el foco va directo a ese campo;
 * - al corregir un campo se le quita el error, sin esperar a validar de nuevo.
 * Cada error es { campo: elemento | null, mensaje: string, contexto?: string }.
 */
const UX = (() => {
  let contador = 0;

  function nodo(tag, clase, texto) {
    const n = document.createElement(tag);
    if (clase) n.className = clase;
    if (texto !== undefined) n.textContent = texto;
    return n;
  }

  function envoltorio(campo) {
    return campo.closest(".field, .inv__cell") || campo.parentElement;
  }

  function marcar(campo, mensaje) {
    const caja = envoltorio(campo);
    caja.classList.add("field--error");
    const aviso = nodo("span", "field__error");
    aviso.id = `error-${++contador}`;
    aviso.append(nodo("strong", "", "Error: "), document.createTextNode(mensaje));
    caja.append(aviso);
    campo.setAttribute("aria-invalid", "true");
    campo.setAttribute("aria-describedby", aviso.id);
  }

  function quitarMarca(caja) {
    caja.classList.remove("field--error");
    caja.querySelectorAll(".field__error").forEach((n) => n.remove());
    caja.querySelectorAll("[aria-invalid]").forEach((c) => {
      c.removeAttribute("aria-invalid");
      c.removeAttribute("aria-describedby");
    });
  }

  function limpiar(resumen) {
    document.querySelectorAll(".field--error").forEach(quitarMarca);
    resumen.hidden = true;
    resumen.textContent = "";
    delete resumen.dataset.tipo;
  }

  function alEditar(campo, resumen) {
    const caja = campo.closest && campo.closest(".field--error");
    if (caja) quitarMarca(caja);
    if (resumen.dataset.tipo === "campos" && !document.querySelector(".field--error")) {
      resumen.hidden = true;
    }
  }

  function irAlCampo(campo) {
    campo.focus({ preventScroll: true });
    campo.scrollIntoView({ block: "center", behavior: "smooth" });
  }

  function pintarResumen(seccion, resumen, errores) {
    const texto = (e) => (e.contexto ? `${e.contexto}: ${e.mensaje}` : e.mensaje);
    resumen.textContent = "";
    if (errores.length === 1) {
      resumen.textContent = texto(errores[0]);
    } else {
      resumen.append(nodo("p", "wz-error__titulo", `Revisá estos ${errores.length} puntos para seguir:`));
      const lista = nodo("ul", "wz-error__lista");
      errores.forEach((e) => {
        const item = nodo("li");
        if (e.campo) {
          const enlace = nodo("a", "", texto(e));
          enlace.href = "#";
          enlace.addEventListener("click", (ev) => {
            ev.preventDefault();
            irAlCampo(e.campo);
          });
          item.append(enlace);
        } else {
          item.textContent = texto(e);
        }
        lista.append(item);
      });
      resumen.append(lista);
    }
    resumen.dataset.tipo = errores.some((e) => !e.campo) ? "general" : "campos";
    const ancla = seccion.querySelector(".wz-step__hint") || seccion.querySelector(".wz-step__title");
    ancla.after(resumen);
    resumen.hidden = false;
    resumen.focus({ preventScroll: true });
    resumen.scrollIntoView({ block: "nearest", behavior: "smooth" });
  }

  function mostrar(seccion, resumen, errores) {
    limpiar(resumen);
    if (!errores.length) return;
    errores.forEach((e) => {
      if (e.campo) marcar(e.campo, e.mensaje);
    });
    if (errores.length > 1 || errores.some((e) => !e.campo)) {
      pintarResumen(seccion, resumen, errores);
    } else {
      irAlCampo(errores[0].campo);
    }
  }

  function mostrarGeneral(seccion, resumen, mensaje) {
    mostrar(seccion, resumen, [{ campo: null, mensaje }]);
  }

  return { mostrar, mostrarGeneral, limpiar, alEditar };
})();
