/*
 * Nombre: claseInicial.js
 * Descripción: La clase del tema claro u oscuro que lleva la página desde antes de dibujar la app, la misma que
 *              después pone Material UI (RP-08). Sin ella, un componente que mide sus estilos al aparecer, como el
 *              campo de varias líneas de Nuevo lote, los medía antes de que el tema pusiera la clase, y con el tema
 *              oscuro los botones pasaban del color claro al oscuro en un cuarto de segundo, con poco contraste
 *              mientras tanto (RNF-06 C4).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */

/** Claves del navegador donde queda el tema elegido. App.jsx se las pasa a Material UI. */
export const CLAVES_DEL_TEMA = { modo: 'inspectia.tema', esquema: 'inspectia.esquema' };

/**
 * El esquema con que arranca la app, como lo decide Material UI: el que eligió la persona o, si eligió el del
 * sistema o todavía no eligió, el del sistema.
 *
 * @param {Storage} [almacen] dónde quedó el tema elegido; por defecto, el del navegador
 * @param {Window} [ventana] la ventana, para preguntar el tema del sistema
 * @returns {'light'|'dark'} el esquema
 */
export function esquemaInicial(almacen, ventana = window) {
  let modo = null;
  try {
    modo = (almacen ?? ventana.localStorage).getItem(CLAVES_DEL_TEMA.modo);
  } catch {
    // Sin acceso al almacenamiento del navegador, el del sistema, como Material UI.
  }
  if (modo === 'light' || modo === 'dark') {
    return modo;
  }
  return ventana.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

/**
 * Pone en la página la clase del esquema con que arranca la app.
 *
 * @param {Document} [documento] la página
 */
export function ponerClaseInicial(documento = document) {
  documento.documentElement.classList.add(esquemaInicial());
}
