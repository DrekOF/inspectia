/*
 * Nombre: contexto.js
 * Descripción: El contexto de React con la sesión de la app y el hook para leerlo desde las pantallas.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { createContext, useContext } from 'react';

/**
 * @typedef {object} ValorDeSesion lo que entrega ProveedorDeSesion
 * @property {import('./almacen.js').Sesion|null} sesion la sesión abierta, o null
 * @property {{ mensaje: string|null, regreso: { ruta: string, usuarioId: string }|null }|null} cierre cómo
 *   se cerró la última sesión: el mensaje para Iniciar sesión y, si se cerró sin pedirlo, a dónde volver
 * @property {import('../api/cliente.js').Pedir} pedir el cliente de la API
 * @property {(respuesta: import('../api/sesion.js').RespuestaDeSesion) => void} iniciar guarda la sesión que
 *   responde el backend al iniciar sesión o al cambiar la contraseña
 * @property {() => void} cerrarSesion cierra la sesión porque la persona lo pidió
 */

/** El contexto de la sesión. */
export const ContextoDeSesion = createContext(null);

/**
 * La sesión de la app y lo que se puede hacer con ella.
 *
 * @returns {ValorDeSesion} la sesión
 */
export function useSesion() {
  return useContext(ContextoDeSesion);
}
