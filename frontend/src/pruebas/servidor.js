/*
 * Nombre: servidor.js
 * Descripción: API simulada con MSW para las pruebas unitarias y de componentes: responde como el
 *              backend real, con los mismos errores application/problem+json (D-28).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { HttpResponse } from 'msw';
import { setupServer } from 'msw/node';

/** El servidor simulado. Cada prueba le agrega las respuestas que necesita con servidor.use(...). */
export const servidor = setupServer();

/**
 * Una respuesta de error como las del backend (RFC 9457).
 *
 * @param {number} estado el código HTTP, como 400 o 401
 * @param {string} detalle el mensaje, en el campo detail
 * @param {object} [extra] los demás campos, como errores, ayuda o bloqueadoHasta
 * @param {object} [encabezados] encabezados de la respuesta, como Retry-After
 * @returns {Response} la respuesta
 */
export function problema(estado, detalle, extra = {}, encabezados = {}) {
  return HttpResponse.json(
    { type: 'about:blank', title: 'Error', status: estado, detail: detalle, ...extra },
    { status: estado, headers: { 'Content-Type': 'application/problem+json', ...encabezados } },
  );
}
