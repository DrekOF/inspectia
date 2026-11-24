/*
 * Nombre: periodo.js (estadistica)
 * Descripción: El periodo de las estadísticas sin dibujar nada (RF-16): los últimos 12 meses por defecto, el periodo
 *              de la dirección de la página, que se ignora si no sirve, la dirección de un periodo y el esquema de
 *              Yup del formulario, con las reglas del backend: las dos fechas, y «Hasta» no anterior a «Desde». También el
 *              periodo opcional de la confianza promedio del modelo (RF-17): sin fechas, o con una o las dos.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import { object, string } from 'yup';
import { CAMPOS_DEL_PERIODO } from '../api/estadisticas.js';
import { esDia } from '../lote/filtros.js';

/** Cuántos meses abarca el periodo por defecto, contando el de hoy. */
export const MESES_POR_DEFECTO = 12;

/**
 * El periodo por defecto: los últimos 12 meses, del primer día del mes de hace 11 meses hasta hoy. Así la evolución por
 * mes trae meses completos, salvo el de hoy.
 *
 * @param {string} hoy el día de hoy en Colombia (aaaa-mm-dd)
 * @returns {{ desde: string, hasta: string }} el periodo
 */
export function periodoPorDefecto(hoy) {
  const [anio, mes] = hoy.split('-').map(Number);
  // Date.UTC pasa al año anterior si el mes queda negativo.
  const inicio = new Date(Date.UTC(anio, mes - MESES_POR_DEFECTO, 1));
  return { desde: inicio.toISOString().slice(0, 10), hasta: hoy };
}

/**
 * El periodo que dice la dirección de la página. Si falta una fecha, alguna no es un día o «Hasta» es anterior a
 * «Desde», se ignora y queda el periodo por defecto.
 *
 * @param {URLSearchParams} parametros los parámetros de la dirección
 * @param {string} hoy el día de hoy en Colombia (aaaa-mm-dd)
 * @returns {{ desde: string, hasta: string }} el periodo
 */
export function leerPeriodo(parametros, hoy) {
  const desde = parametros.get('desde') ?? '';
  const hasta = parametros.get('hasta') ?? '';
  if (esDia(desde) && esDia(hasta) && hasta >= desde) {
    return { desde, hasta };
  }
  return periodoPorDefecto(hoy);
}

/**
 * La dirección de un periodo: sus dos fechas.
 *
 * @param {{ desde: string, hasta: string }} periodo el periodo
 * @returns {URLSearchParams} los parámetros de la dirección
 */
export function direccionDelPeriodo(periodo) {
  return new URLSearchParams(CAMPOS_DEL_PERIODO.map((nombre) => [nombre, periodo[nombre]]));
}

/**
 * El periodo opcional que dice la dirección de la página, como el de la confianza promedio del modelo (RF-17). Una
 * fecha que no es un día se ignora; si «Hasta» es anterior a «Desde», se ignoran las dos.
 *
 * @param {URLSearchParams} parametros los parámetros de la dirección
 * @returns {{ desde: string, hasta: string }} el periodo, con '' en la fecha que no tiene
 */
export function leerPeriodoOpcional(parametros) {
  const desde = esDia(parametros.get('desde')) ? parametros.get('desde') : '';
  const hasta = esDia(parametros.get('hasta')) ? parametros.get('hasta') : '';
  if (desde && hasta && hasta < desde) {
    return { desde: '', hasta: '' };
  }
  return { desde, hasta };
}

/**
 * La dirección de un periodo opcional: solo las fechas que tiene.
 *
 * @param {{ desde: string, hasta: string }} periodo el periodo
 * @returns {URLSearchParams} los parámetros de la dirección
 */
export function direccionDelPeriodoOpcional(periodo) {
  return new URLSearchParams(CAMPOS_DEL_PERIODO.filter((nombre) => periodo[nombre])
    .map((nombre) => [nombre, periodo[nombre]]));
}

/**
 * La regla de «Hasta»: no puede ser anterior a «Desde»; el mismo día en las dos sí. Si falta una, se cumple.
 *
 * @param {(clave: string) => string} t la función de traducción de i18next
 * @returns {import('yup').StringSchema} el esquema de «Hasta», sin decir si es obligatorio
 */
function hastaNoAntesDeDesde(t) {
  return string().default('')
    .test('noAntesDeDesde', () => t('estadistica.hasta_antes_de_desde'), function noAntesDeDesde(hasta) {
      const { desde } = this.parent;
      return !hasta || !desde || hasta >= desde;
    });
}

/**
 * Esquema de Yup del periodo opcional (RP-07), como el de la confianza promedio del modelo (RF-17): las dos fechas
 * pueden faltar, y «Hasta» no puede ser anterior a «Desde», con el mensaje del backend.
 *
 * @param {(clave: string) => string} t la función de traducción de i18next
 * @returns {import('yup').ObjectSchema} el esquema del formulario
 */
export function esquemaDelPeriodoOpcional(t) {
  return object({ desde: string().default(''), hasta: hastaNoAntesDeDesde(t) });
}

/**
 * Esquema de Yup del periodo (RP-07), con los mensajes del backend: las dos fechas son obligatorias en la pantalla,
 * y «Hasta» no puede ser anterior a «Desde»; el mismo día en las dos sí.
 *
 * @param {(clave: string) => string} t la función de traducción de i18next
 * @returns {import('yup').ObjectSchema} el esquema del formulario
 */
export function esquemaDelPeriodo(t) {
  return object({
    desde: string().default('').required(() => t('validacion.obligatorio')),
    hasta: hastaNoAntesDeDesde(t).required(() => t('validacion.obligatorio')),
  });
}
