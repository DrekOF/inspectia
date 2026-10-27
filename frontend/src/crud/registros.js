/*
 * Nombre: registros.js
 * Descripción: Lo que calcula la pantalla de registros generada a partir de lo que dice su JSON (RP-23): qué
 *              acciones ve cada rol (RNF-04), el resumen de la lista en singular y en plural, con el máximo de
 *              activos si la pantalla tiene límite, los valores de una fila para sus textos y los errores de la
 *              API de un campo dentro de un grupo.
 * Fecha de creación: 26/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import { setIn } from 'formik';

/**
 * @typedef {(clave: string, valores?: object) => string} Traducir la función de traducción de i18next
 */

/**
 * Si un rol puede usar una operación, según los roles que le da operaciones en el JSON. La API lo revisa igual: la
 * pantalla solo esconde lo que el rol no puede hacer (RNF-04).
 *
 * @param {Record<string, string[]>} operaciones los roles de cada operación
 * @param {string} rol el rol de quien usa la pantalla
 * @param {string} operacion la operación, como crear o desactivar
 * @returns {boolean} si puede usarla
 */
export function puede(operaciones, rol, operacion) {
  return Boolean(operaciones[operacion]?.includes(rol));
}

/**
 * Un texto que se cuenta, en singular solo con uno y en plural con cualquier otra cantidad: «1 empresa registrada»,
 * «7 empresas registradas».
 *
 * @param {Traducir} t la función de traducción
 * @param {string} clave la clave del texto, sin su forma, como empresa.pantalla.resumen_total
 * @param {number} cantidad cuántos son
 * @param {object} [valores] los demás valores del texto, como el máximo en «5 de 25 usuarios activos»
 * @returns {string} el texto
 */
export function textoContado(t, clave, cantidad, valores = {}) {
  return t(`${clave}.${cantidad === 1 ? 'singular' : 'plural'}`, { ...valores, cantidad });
}

/** Lo que puede contar el resumen: los registros, los activos y los desactivados. */
const PARTES_DEL_RESUMEN = ['total', 'activos', 'inactivos'];

/**
 * El resumen de la lista, como «7 empresas registradas: 6 activas y 1 desactivada.» o «5 de 25 usuarios activos,
 * contándote a ti.», o null si el JSON no trae sus textos. Cuenta solo lo que usa su frase.
 *
 * @param {Traducir} t la función de traducción
 * @param {{ frase: string, total?: string, activos?: string, inactivos?: string }|null} resumen las claves de sus
 *   textos: la frase y lo que cuenta
 * @param {{ total: number, activos?: number, maximo?: number }} cuentas cuántos registros hay; si la entidad se
 *   desactiva, cuántos están activos; y si la pantalla tiene límite de activos, su máximo
 * @returns {string|null} el resumen
 */
export function resumenDeRegistros(t, resumen, { total, activos, maximo }) {
  if (!resumen) {
    return null;
  }
  const cuentas = { total, activos, inactivos: total - activos };
  const partes = Object.fromEntries(PARTES_DEL_RESUMEN.filter((parte) => resumen[parte])
    .map((parte) => [parte, textoContado(t, resumen[parte], cuentas[parte], { maximo })]));
  return t(resumen.frase, { ...partes, maximo });
}

/**
 * Los valores de una fila que pueden ir en sus textos, como {nombre} en «Editar {nombre}»: los textos y los números.
 *
 * @param {object} fila la fila, como la responde la API
 * @returns {Record<string, string|number>} los valores
 */
export function valoresDeFila(fila) {
  return Object.fromEntries(Object.entries(fila ?? {})
    .filter(([, valor]) => typeof valor === 'string' || typeof valor === 'number'));
}

/**
 * Los errores de los campos que responde la API, como los guarda Formik: el de administrador.correo va dentro de
 * administrador.
 *
 * @param {Record<string, string>} errores el mensaje de cada campo, con el nombre que le da la API
 * @returns {object} los errores para Formik
 */
export function erroresAnidados(errores) {
  return Object.entries(errores).reduce((anidados, [campo, mensaje]) => setIn(anidados, campo, mensaje), {});
}
