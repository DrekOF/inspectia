/*
 * Nombre: lotes.js
 * Descripción: Lotes de ejemplo como los responde el backend, con la sugerencia de otra muestra (D-25), páginas del
 *              historial y la lista de quienes registraron lotes para el filtro «Usuario», para las pruebas de Nuevo
 *              lote, el Historial y las fotos de la muestra (RF-04, RF-05, RF-14).
 * Fecha de creación: 10/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
import { http, HttpResponse } from 'msw';
import { USUARIOS } from './ayudas.jsx';
import { servidor } from './servidor.js';

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
  sugerirOtraMuestra: null,
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
  sugerirOtraMuestra: false,
  fechaResultado: '2026-10-18T16:00:00Z',
  fechaCreacion: '2026-10-17T14:00:00Z',
};

/**
 * Un lote analizado con una muestra que sugiere otra (D-24, D-25): 90,6 %, Estándar, a 0,6 puntos del límite de 90,
 * registrado por el usuario final de ejemplo.
 */
export const LOTE_CON_SUGERENCIA = {
  id: '0199f0a0-0000-7000-8000-000000000141',
  codigo: 'L-0141',
  fechaRecepcion: '2026-10-18',
  origen: 'El Recreo',
  observaciones: null,
  estado: 'ANALIZADO',
  usuario: { id: USUARIOS.USUARIO_FINAL.id, nombre: USUARIOS.USUARIO_FINAL.nombre },
  porcentajeSanos: 90.6,
  categoria: 'ESTANDAR',
  sugerirOtraMuestra: true,
  fechaResultado: '2026-10-19T15:00:00Z',
  fechaCreacion: '2026-10-18T13:00:00Z',
};

/**
 * Quienes registraron lotes de la empresa, como los responde GET /api/v1/lotes/registradores: solo el identificador
 * y el nombre, ordenados por nombre. Luis Gómez está desactivado y sigue en la lista (RF-14, restricciones).
 */
export const REGISTRADORES = [
  { id: USUARIOS.USUARIO_FINAL.id, nombre: USUARIOS.USUARIO_FINAL.nombre },
  { id: '0199e2a0-0000-7000-8000-000000000015', nombre: 'Luis Gómez' },
  { id: LOTE_ANALIZADO.usuario.id, nombre: LOTE_ANALIZADO.usuario.nombre },
];

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
 * Responde quienes registraron lotes de la empresa (GET /api/v1/lotes/registradores), para el filtro «Usuario» del
 * historial, y anota cuántas veces se pidió.
 *
 * @param {{ id: string, nombre: string }[]} [lista] las personas, en el orden en que las responde el backend
 * @returns {{ veces: number }} cuántas veces se pidió
 */
export function conRegistradores(lista = REGISTRADORES) {
  const pedidas = { veces: 0 };
  servidor.use(http.get('/api/v1/lotes/registradores', () => {
    pedidas.veces += 1;
    return HttpResponse.json(lista);
  }));
  return pedidas;
}
