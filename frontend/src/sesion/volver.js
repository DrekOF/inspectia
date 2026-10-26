/*
 * Nombre: volver.js
 * Descripción: A dónde va la persona al iniciar sesión de nuevo después de que la sesión se cerró sin pedirlo
 *              (un 401 o el vencimiento): a la pantalla donde estaba, solo si entra el mismo usuario y la ruta
 *              es interna; si no, al inicio de su rol.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */

/** El inicio de cada rol. */
const INICIO = '/';

/** La ruta de Iniciar sesión: volver a ella no tiene sentido. */
export const RUTA_INICIAR_SESION = '/iniciar-sesion';

/**
 * Si una ruta es de esta app: empieza por «/» y no por «//» ni «/\», que el navegador lee como otro sitio.
 *
 * @param {string|null} ruta la ruta, como /cambiar-contrasena
 * @returns {boolean} si es interna
 */
export function rutaInterna(ruta) {
  return typeof ruta === 'string' && ruta.startsWith('/') && !ruta.startsWith('//') && !ruta.startsWith('/\\');
}

/**
 * A dónde ir al iniciar sesión.
 *
 * @param {{ id: string }} usuario quien acaba de entrar
 * @param {{ ruta: string, usuarioId: string }|null} regreso la pantalla donde estaba la sesión que se cerró y
 *   de quién era, o null
 * @returns {string} la ruta
 */
export function destinoTrasIniciarSesion(usuario, regreso) {
  if (!regreso || regreso.usuarioId !== usuario.id || !rutaInterna(regreso.ruta)) {
    return INICIO;
  }
  const camino = regreso.ruta.split(/[?#]/)[0];
  return camino === RUTA_INICIAR_SESION ? INICIO : regreso.ruta;
}
