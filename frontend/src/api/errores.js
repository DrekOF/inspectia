/*
 * Nombre: errores.js
 * Descripción: El texto que muestra la app para un error de la API (RNF-06 C3): el mensaje del backend, que
 *              ya dice qué pasó y qué hacer, el de las reglas sin campo que no dejan guardar, o uno propio si
 *              no hubo conexión o el servidor no dio mensaje. También reparte los errores de cada campo entre
 *              los campos de un formulario.
 * Fecha de creación: 20/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */

/**
 * El texto de un error para mostrarlo en la pantalla. Si no dejan guardar reglas que no son de un campo, como el
 * máximo de usuarios activos de una empresa, son sus mensajes, que dicen por qué y qué hacer, en lugar del general
 * «Revisa los datos marcados.».
 *
 * @param {Error} error el error, normalmente un ErrorDeApi
 * @param {(clave: string) => string} t la función de traducción de i18next
 * @returns {string} el texto
 */
export function textoDelError(error, t) {
  if (error.estado === 0) {
    return t('conexion.sinConexion');
  }
  const deReglas = (error.errores ?? []).filter(({ campo }) => !campo).map(({ mensaje }) => mensaje);
  if (deReglas.length > 0) {
    return deReglas.join(' ');
  }
  return error.detalle || t('error.interno');
}

/**
 * Los errores del backend que son de los campos de un formulario, para marcarlos en cada campo. La ayuda del
 * backend, como la regla del bloqueo, va con el primero.
 *
 * @param {Error} error el error, normalmente un ErrorDeApi
 * @param {string[]} campos los campos del formulario, en su orden
 * @returns {Record<string, string>|null} el mensaje de cada campo, o null si ninguno es del formulario
 */
export function erroresPorCampo(error, campos) {
  const propios = (error.errores ?? []).filter(({ campo }) => campos.includes(campo));
  if (propios.length === 0) {
    return null;
  }
  propios.sort((uno, otro) => campos.indexOf(uno.campo) - campos.indexOf(otro.campo));
  return Object.fromEntries(propios.map(({ campo, mensaje }, posicion) => [
    campo, posicion === 0 && error.ayuda ? `${mensaje} ${error.ayuda}` : mensaje,
  ]));
}
