/*
 * Nombre: i18n.js
 * Descripción: Idiomas de la app con i18next (RP-11): español por defecto e inglés. Junta los textos del
 *              backend (sus .properties) con los propios del frontend, recuerda el idioma elegido y lo pone
 *              en <html lang> para los lectores de pantalla.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import textosDelBackend from 'virtual:textos-del-backend';
import en from './en.js';
import es from './es.js';

/**
 * Los idiomas de la app. El nombre va en su propio idioma, y la región da el formato de fechas y horas.
 */
export const IDIOMAS = [
  { codigo: 'es', nombre: 'Español', region: 'es-CO' },
  { codigo: 'en', nombre: 'English', region: 'en-US' },
];

/** Clave del navegador donde queda el idioma elegido. */
export const CLAVE_IDIOMA = 'inspectia.idioma';

/**
 * El idioma que la persona eligió antes, si el navegador lo guardó.
 *
 * @returns {string|null} el código, como «en», o null
 */
function idiomaGuardado() {
  try {
    const codigo = localStorage.getItem(CLAVE_IDIOMA);
    return IDIOMAS.some((idioma) => idioma.codigo === codigo) ? codigo : null;
  } catch {
    return null;
  }
}

/**
 * Los datos de un idioma de la app.
 *
 * @param {string} codigo el código, como «es»
 * @returns {{ codigo: string, nombre: string, region: string }} el idioma; si no existe, el español
 */
export function idiomaDe(codigo) {
  return IDIOMAS.find((idioma) => idioma.codigo === codigo) ?? IDIOMAS[0];
}

// Las claves llevan puntos, como en el backend («error.credenciales_incorrectas»), así que no se separan.
// Los marcadores van entre llaves, como {0} o {max}: los mismos del backend.
i18n.use(initReactI18next).init({
  resources: {
    es: { translation: { ...textosDelBackend.es, ...es } },
    en: { translation: { ...textosDelBackend.en, ...en } },
  },
  lng: idiomaGuardado() ?? 'es',
  fallbackLng: 'es',
  supportedLngs: IDIOMAS.map((idioma) => idioma.codigo),
  keySeparator: false,
  nsSeparator: false,
  interpolation: { escapeValue: false, prefix: '{', suffix: '}' },
  initAsync: false,
});

document.documentElement.lang = i18n.language;

i18n.on('languageChanged', (codigo) => {
  document.documentElement.lang = codigo;
  try {
    localStorage.setItem(CLAVE_IDIOMA, codigo);
  } catch {
    // Sin almacenamiento, el idioma dura hasta cerrar la página.
  }
});

export default i18n;
