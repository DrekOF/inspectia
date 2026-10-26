/*
 * Nombre: menu.js
 * Descripción: Las secciones de la app y qué roles las usan (RF-01 C1, RNF-04): de aquí salen el menú de
 *              cada rol, en el orden del prototipo, y las rutas que cada rol puede abrir. «Mi cuenta» es igual
 *              para todos.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */

/** Los roles de los usuarios, como los llama el backend. */
export const ROLES = {
  USUARIO_FINAL: 'USUARIO_FINAL',
  ADMIN_EMPRESA: 'ADMIN_EMPRESA',
  ADMIN_PLATAFORMA: 'ADMIN_PLATAFORMA',
};

const { USUARIO_FINAL, ADMIN_EMPRESA, ADMIN_PLATAFORMA } = ROLES;

/**
 * @typedef {object} Seccion una entrada del menú
 * @property {string} etiqueta la clave de su texto; las de los datos salen del backend, como
 *   usuario.etiqueta.plural
 * @property {string} [ruta] su ruta; «Cerrar sesión» no tiene
 * @property {string} icono el nombre de su ícono en MenuLateral.jsx
 * @property {string[]} [roles] los roles que la usan
 */

/**
 * Las secciones de la app, en el orden del menú. Filtradas por rol dan el menú de cada uno, en el orden del
 * prototipo.
 *
 * @type {Seccion[]}
 */
export const SECCIONES = [
  { etiqueta: 'menu.inicio', ruta: '/', icono: 'inicio', roles: [USUARIO_FINAL, ADMIN_EMPRESA, ADMIN_PLATAFORMA] },
  { etiqueta: 'menu.nuevoLote', ruta: '/lotes/nuevo', icono: 'nuevoLote', roles: [USUARIO_FINAL] },
  { etiqueta: 'menu.historial', ruta: '/historial', icono: 'historial', roles: [USUARIO_FINAL, ADMIN_EMPRESA] },
  { etiqueta: 'usuario.etiqueta.plural', ruta: '/usuarios', icono: 'usuarios', roles: [ADMIN_EMPRESA] },
  {
    etiqueta: 'rango_recomendacion.etiqueta.plural', ruta: '/rangos-recomendacion', icono: 'rangos',
    roles: [ADMIN_EMPRESA],
  },
  { etiqueta: 'menu.estadisticas', ruta: '/estadisticas', icono: 'estadisticas', roles: [ADMIN_EMPRESA] },
  { etiqueta: 'modelo.etiqueta.singular', ruta: '/modelo', icono: 'modelo', roles: [ADMIN_EMPRESA] },
  { etiqueta: 'empresa.etiqueta.plural', ruta: '/empresas', icono: 'empresas', roles: [ADMIN_PLATAFORMA] },
];

/**
 * El menú principal de un rol: solo sus secciones, sin las de los otros roles.
 *
 * @param {string} rol el rol del usuario
 * @returns {Seccion[]} las secciones; ninguna si la app no conoce el rol
 */
export function menuPrincipal(rol) {
  return SECCIONES.filter((seccion) => seccion.roles.includes(rol));
}

/**
 * «Mi cuenta», igual para todos los roles.
 *
 * @type {Seccion[]}
 */
export const MENU_DE_CUENTA = [
  { etiqueta: 'cuenta.cambiarContrasena', ruta: '/cambiar-contrasena', icono: 'contrasena' },
  { etiqueta: 'cuenta.acercaDe', ruta: '/acerca-de', icono: 'acercaDe' },
  { etiqueta: 'cuenta.cerrarSesion', icono: 'cerrarSesion' },
];
