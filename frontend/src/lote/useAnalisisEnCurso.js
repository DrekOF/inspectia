/*
 * Nombre: useAnalisisEnCurso.js
 * Descripción: Sigue el análisis de la muestra en curso de un lote mientras espera en la cola o se procesa (RNF-01 C4,
 *              RNF-02 C3): consulta su avance cada segundo, con una consulta a la vez, y se detiene cuando termina,
 *              cuando vuelve a quedar pendiente o al salir de la página. Con la pestaña oculta no consulta, para no
 *              sumar carga al servidor (RNF-02 C2), y al volver consulta enseguida. Si la consulta falla varias veces
 *              seguidas, deja de intentar y ofrece reintentar.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { useCallback, useEffect, useEffectEvent, useState } from 'react';
import { avanceDelAnalisis } from '../api/analisis.js';
import { EN_CURSO } from './resultado.js';

/** Cada cuánto se consulta el avance, en milisegundos. */
export const INTERVALO_DE_CONSULTA = 1000;

/** Cuántas consultas seguidas pueden fallar antes de dejar de intentar y ofrecer «Reintentar». */
export const FALLOS_ANTES_DE_PARAR = 3;

/**
 * @typedef {object} AnalisisEnCurso lo que se sabe del análisis
 * @property {import('../api/analisis.js').AvanceDelAnalisis|null} avance el último avance consultado; null antes de
 *   la primera respuesta
 * @property {Error|null} error el error con el que se dejó de consultar, si se dejó
 * @property {() => void} reintentar vuelve a consultar después de un error
 */

/**
 * Sigue el análisis de la muestra en curso de un lote. Consulta con setTimeout encadenado: la siguiente consulta se
 * programa cuando llega la respuesta de la anterior, así nunca hay dos a la vez.
 *
 * @param {import('../api/cliente.js').Pedir} pedir la función del cliente de la API
 * @param {string} loteId el lote
 * @param {object} opciones cuándo consultar y qué hacer al terminar
 * @param {boolean} opciones.activo si la muestra está en la cola o en el análisis: solo entonces se consulta
 * @param {() => void} [opciones.alTerminar] se llama una vez, cuando la muestra termina
 * @param {number} [opciones.intervalo] cada cuánto se consulta, en milisegundos
 * @returns {AnalisisEnCurso} el avance, el error y cómo reintentar
 */
export function useAnalisisEnCurso(pedir, loteId, { activo, alTerminar, intervalo = INTERVALO_DE_CONSULTA }) {
  const [avance, setAvance] = useState(null);
  const [error, setError] = useState(null);
  const [intentos, setIntentos] = useState(0);
  const avisarQueTermino = useEffectEvent(() => alTerminar?.());

  useEffect(() => {
    if (!activo) {
      return undefined;
    }
    let vigente = true;
    let temporizador = null;
    let enVuelo = false;
    let detenido = false;
    let fallos = 0;

    /**
     * Programa la siguiente consulta, si sigue en curso y la pestaña se ve.
     *
     * @param {number} espera en cuántos milisegundos
     */
    function programar(espera) {
      clearTimeout(temporizador);
      temporizador = null;
      if (vigente && !detenido && !document.hidden) {
        temporizador = setTimeout(consultar, espera);
      }
    }

    /** Consulta el avance, si no hay otra consulta en vuelo, y programa la siguiente. */
    async function consultar() {
      temporizador = null;
      if (!vigente || enVuelo || detenido) {
        return;
      }
      enVuelo = true;
      try {
        const datos = await avanceDelAnalisis(pedir, loteId);
        if (!vigente) {
          return;
        }
        fallos = 0;
        detenido = !EN_CURSO.includes(datos.estado);
        setAvance(datos);
        if (datos.estado === 'TERMINADA') {
          avisarQueTermino();
        }
      } catch (rechazo) {
        if (!vigente) {
          return;
        }
        fallos += 1;
        // Un lote que ya no existe no aparece reintentando (RNF-03); otro error se reintenta unas veces.
        if (rechazo.estado === 404 || fallos >= FALLOS_ANTES_DE_PARAR) {
          detenido = true;
          setError(rechazo);
        }
      } finally {
        enVuelo = false;
      }
      programar(intervalo);
    }

    /** Con la pestaña oculta no consulta; al volver, consulta enseguida. */
    function alCambiarLaVisibilidad() {
      if (document.hidden) {
        clearTimeout(temporizador);
        temporizador = null;
      } else if (!temporizador) {
        consultar();
      }
    }

    document.addEventListener('visibilitychange', alCambiarLaVisibilidad);
    // Al reintentar, la primera consulta va enseguida; al empezar, después del intervalo: el resultado recién pedido ya
    // dijo el estado.
    programar(intentos > 0 ? 0 : intervalo);
    return () => {
      vigente = false;
      clearTimeout(temporizador);
      document.removeEventListener('visibilitychange', alCambiarLaVisibilidad);
    };
  }, [activo, pedir, loteId, intervalo, intentos]);

  const reintentar = useCallback(() => {
    setError(null);
    setIntentos((veces) => veces + 1);
  }, []);

  return { avance, error, reintentar };
}
