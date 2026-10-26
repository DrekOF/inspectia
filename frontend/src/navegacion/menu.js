/*
 * Nombre: menu.js
 * Descripción: Las secciones de la app y qué roles las usan (RF-01 C1, RNF-04): de aquí salen el menú de
 *              cada rol, en el orden del prototipo, y las rutas que cada rol puede abrir. «Mi cuenta» es igual
 *              para todos. Las secciones de las pantallas generadas toman su ruta, su etiqueta, su ícono y sus
 *              roles de src/generated/pantallas.js (RP-23).
 * Fecha de creación: 20/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import { PANTALLAS } from '../generated/pantallas.js';

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
 * @property {string} [pantalla] la entidad de su pantalla generada, como empresa: mientras no la tenga, la sección
 *   se ve como pendiente
 * @property {string} [entidad] la entidad de la pantalla generada que la sección ya tiene
 * @property {import('react').ComponentType<object>} [Pantalla] la pantalla generada
 */

/**
 * Las secciones de la app, en el orden del menú del prototipo. Las de una pantalla generada la nombran con
 * pantalla; lo que diga de ella src/generated/pantallas.js reemplaza lo de aquí.
 *
 * @type {Seccion[]}
 */
const SECCIONES_DEL_MENU = [
  { etiqueta: 'menu.inicio', ruta: '/', icono: 'inicio', roles: [USUARIO_FINAL, ADMIN_EMPRESA, ADMIN_PLATAFORMA] },
  { etiqueta: 'menu.nuevoLote', ruta: '/lotes/nuevo', icono: 'nuevoLote', roles: [USUARIO_FINAL] },
  { etiqueta: 'menu.historial', ruta: '/historial', icono: 'historial', roles: [USUARIO_FINAL, ADMIN_EMPRESA] },
  { etiqueta: 'usuario.etiqueta.plural', ruta: '/usuarios', icono: 'usuarios', roles: [ADMIN_EMPRESA], pantalla: 'usuario' },
  {
    etiqueta: 'rango_recomendacion.etiqueta.plural', ruta: '/rangos-recomendacion', icono: 'rangos',
    roles: [ADMIN_EMPRESA], pantalla: 'rango_recomendacion',
  },
  { etiqueta: 'menu.estadisticas', ruta: '/estadisticas', icono: 'estadisticas', roles: [ADMIN_EMPRESA] },
  { etiqueta: 'modelo.etiqueta.singular', ruta: '/modelo', icono: 'modelo', roles: [ADMIN_EMPRESA] },
  { etiqueta: 'empresa.etiqueta.plural', ruta: '/empresas', icono: 'empresas', roles: [ADMIN_PLATAFORMA], pantalla: 'empresa' },
];

/**
 * Las secciones de la app con sus pantallas generadas: cada una que el menú nombra toma de pantallas.js su ruta,
 * su etiqueta, su ícono, sus roles (los de listar en su JSON) y su pantalla, y las que no nombra van al final, como
 * la de otra temática.
 *
 * @type {Seccion[]}
 */
export const SECCIONES = [
  ...SECCIONES_DEL_MENU.map((seccion) => ({ ...seccion, ...PANTALLAS.find(({ entidad }) => entidad === seccion.pantalla) })),
  ...PANTALLAS.filter(({ entidad }) => !SECCIONES_DEL_MENU.some(({ pantalla }) => pantalla === entidad)),
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
