/*
 * Nombre: useFotosDelLote.js
 * Descripción: Las fotos de la muestra en curso de un lote, para la página de las fotos (RF-05, RF-14 C5): las pide
 *              al entrar, otra vez con «Reintentar» y una sola vez más si una imagen no carga porque su enlace
 *              venció, y suma las que se confirman. Una lista pedida antes de una confirmación no la deshace.
 *              También dice el número y el estado de la muestra, para saber si ya se envió al análisis (RNF-02 C3), y
 *              lo que dejó en ella el servicio de IA: por qué volvió sin resultado (D-44), cuántas fotos pidió repetir
 *              por granos dudosos y el máximo de dudosos que usó (RF-09); sin esos datos, van vacíos.
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
import { useCallback, useEffect, useReducer, useRef, useState } from 'react';
import { fotosDelLote } from '../api/fotos.js';

/**
 * @typedef {object} Entrada una foto y cuándo se supo de ella
 * @property {import('../api/fotos.js').Foto} foto la foto
 * @property {number} orden el turno de la lista o de la confirmación que la trajo: entre dos, vale la más nueva
 */

/**
 * @typedef {object} Estado lo que se sabe de las fotos
 * @property {number} respondida la recarga de la última respuesta de la lista; -1 antes de la primera
 * @property {boolean} cargada si ya llegó una lista
 * @property {Error|null} error el error de la última lista, si falló
 * @property {Record<number, Entrada>} porNumero las fotos, por su número
 * @property {number|null} muestra el número de la muestra en curso, según la última lista
 * @property {string|null} estado el estado de la muestra en curso, según la última lista, como EN_ESPERA
 * @property {string|null} motivoDevolucion por qué volvió la muestra sin resultado (D-44), según la última lista
 * @property {number} repeticiones cuántas fotos pidió repetir RF-09 por granos dudosos, según la última lista
 * @property {number|null} porcentajeMaxDudosos el máximo de dudosos que usó la muestra (P-15), según la última lista
 */

/** @type {Estado} */
const INICIAL = {
  respondida: -1, cargada: false, error: null, porNumero: {}, muestra: null, estado: null, motivoDevolucion: null,
  repeticiones: 0, porcentajeMaxDudosos: null,
};

/**
 * Suma lo que pasó a lo que se sabe de las fotos.
 *
 * @param {Estado} estado lo que se sabía
 * @param {{ tipo: 'lista', recarga: number, orden: number, datos: import('../api/fotos.js').FotosDeLaMuestra }
 *   | { tipo: 'error', recarga: number, error: Error }
 *   | { tipo: 'confirmada', orden: number, foto: import('../api/fotos.js').Foto }} accion lo que pasó
 * @returns {Estado} lo que se sabe ahora
 */
function reducir(estado, accion) {
  if (accion.tipo === 'lista') {
    // Las fotos confirmadas después de pedir la lista siguen: la lista todavía no las tenía.
    const porNumero = Object.fromEntries(Object.entries(estado.porNumero)
      .filter(([, entrada]) => entrada.orden > accion.orden));
    for (const foto of accion.datos.fotos) {
      porNumero[foto.numero] ??= { foto, orden: accion.orden };
    }
    return {
      respondida: accion.recarga, cargada: true, error: null, porNumero, muestra: accion.datos.muestra,
      estado: accion.datos.estado, motivoDevolucion: accion.datos.motivoDevolucion ?? null,
      repeticiones: accion.datos.repeticiones ?? 0, porcentajeMaxDudosos: accion.datos.porcentajeMaxDudosos ?? null,
    };
  }
  if (accion.tipo === 'error') {
    return { ...estado, respondida: accion.recarga, error: accion.error };
  }
  return { ...estado, porNumero: { ...estado.porNumero, [accion.foto.numero]: { foto: accion.foto, orden: accion.orden } } };
}

/**
 * Las fotos de la muestra en curso de un lote.
 *
 * @param {import('../api/cliente.js').Pedir} pedir la función del cliente de la API
 * @param {string} loteId el lote
 * @returns {{ cargada: boolean, cargando: boolean, error: Error|null,
 *   fotos: Record<number, import('../api/fotos.js').Foto>, muestra: number|null, estado: string|null,
 *   motivoDevolucion: string|null, repeticiones: number, porcentajeMaxDudosos: number|null,
 *   recargar: () => void, confirmada: (foto: import('../api/fotos.js').Foto) => void, vistaFallida: () => void }} las
 *   fotos, si ya llegaron o se están pidiendo, el error de la lista, el número y el estado de la muestra, lo que dejó
 *   en ella el servicio de IA, y qué hacer
 *   para pedirlas otra vez, al confirmar una foto y cuando una imagen no carga
 */
export function useFotosDelLote(pedir, loteId) {
  const [estado, despachar] = useReducer(reducir, INICIAL);
  const [recargas, setRecargas] = useState(0);
  const turnos = useRef(0);
  const recargoPorLaVista = useRef(false);

  useEffect(() => {
    let vigente = true;
    turnos.current += 1;
    const orden = turnos.current;
    fotosDelLote(pedir, loteId)
      .then((datos) => vigente && despachar({ tipo: 'lista', recarga: recargas, orden, datos }))
      .catch((error) => vigente && despachar({ tipo: 'error', recarga: recargas, error }));
    return () => {
      vigente = false;
    };
  }, [pedir, loteId, recargas]);

  const recargar = useCallback(() => setRecargas((veces) => veces + 1), []);
  const confirmada = useCallback((foto) => {
    turnos.current += 1;
    despachar({ tipo: 'confirmada', orden: turnos.current, foto });
  }, []);
  // Si una imagen no carga, lo normal es que su enlace venció: la lista trae enlaces nuevos. Una sola vez, para no
  // pedirla sin fin si la foto no está.
  const vistaFallida = useCallback(() => {
    if (!recargoPorLaVista.current) {
      recargoPorLaVista.current = true;
      setRecargas((veces) => veces + 1);
    }
  }, []);

  const fotos = Object.fromEntries(Object.entries(estado.porNumero).map(([numero, { foto }]) => [numero, foto]));
  return {
    cargada: estado.cargada, cargando: estado.respondida !== recargas, error: estado.error, fotos,
    muestra: estado.muestra, estado: estado.estado, motivoDevolucion: estado.motivoDevolucion,
    repeticiones: estado.repeticiones, porcentajeMaxDudosos: estado.porcentajeMaxDudosos, recargar, confirmada,
    vistaFallida,
  };
}
