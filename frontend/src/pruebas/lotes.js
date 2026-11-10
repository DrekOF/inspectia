/*
 * Nombre: lotes.js
 * Descripción: Lotes de ejemplo como los responde el backend, páginas del historial y la fecha de hoy en
 *              Colombia, para las pruebas de Nuevo lote, el Historial y las fotos de la muestra (RF-04, RF-05,
 *              RF-14).
 * Fecha de creación: 10/11/2026
 * Actualización: 10/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { USUARIOS } from './ayudas.jsx';

/** Un lote pendiente de análisis, registrado por el usuario final de ejemplo, como en el prototipo. */
export const LOTE_PENDIENTE = {
  id: '0199f0a0-0000-7000-8000-000000000143',
  codigo: 'L-0143',
  fechaRecepcion: '2026-10-20',
  origen: 'La Esperanza',
  observaciones: null,
  estado: 'PENDIENTE',
  usuario: { id: USUARIOS.USUARIO_FINAL.id, nombre: USUARIOS.USUARIO_FINAL.nombre },
  porcentajeSanos: null,
  categoria: null,
  fechaResultado: null,
  fechaCreacion: '2026-10-20T15:30:00Z',
};

/** Un lote analizado el 18 de octubre, registrado por otra usuaria de la empresa, como en el prototipo. */
export const LOTE_ANALIZADO = {
  id: '0199f0a0-0000-7000-8000-000000000139',
  codigo: 'L-0139',
  fechaRecepcion: '2026-10-16',
  origen: 'La Estrella',
  observaciones: 'Cosecha de octubre, secado al sol.',
  estado: 'ANALIZADO',
  usuario: { id: '0199e2a0-0000-7000-8000-000000000014', nombre: 'María Torres' },
  porcentajeSanos: 93.0,
  categoria: 'ESTANDAR',
  fechaResultado: '2026-10-18T16:00:00Z',
  fechaCreacion: '2026-10-17T14:00:00Z',
};

/**
 * Un lote pendiente con un número, para llenar páginas del historial: L-0001, L-0002…
 *
 * @param {number} numero el número del lote
 * @returns {object} el lote
 */
export function loteNumero(numero) {
  const codigo = `L-${String(numero).padStart(4, '0')}`;
  return { ...LOTE_PENDIENTE, id: `0199f0a0-0000-7000-8000-${String(numero).padStart(12, '0')}`, codigo };
}

/**
 * Una página del historial como la responde GET /api/v1/lotes.
 *
 * @param {object[]} lotes los lotes de la página
 * @param {{ pagina?: number, tamano?: number, total?: number }} [opciones] la página, su tamaño y cuántos lotes hay
 * @returns {object} la página
 */
export function paginaDeLotes(lotes, { pagina = 0, tamano = 20, total = lotes.length } = {}) {
  return { contenido: lotes, pagina, tamano, totalElementos: total, totalPaginas: Math.ceil(total / tamano) };
}

/**
 * Hoy en Colombia, en el formato de un campo de fecha (aaaa-mm-dd), más unos días.
 *
 * @param {number} [dias] los días que se suman; negativos para el pasado
 * @returns {string} la fecha
 */
export function hoyEnColombia(dias = 0) {
  const hoy = new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Bogota' }).format(new Date());
  const fecha = new Date(`${hoy}T12:00:00Z`);
  fecha.setUTCDate(fecha.getUTCDate() + dias);
  return fecha.toISOString().slice(0, 10);
}
