/*
 * Nombre: periodo.js (estadistica)
 * Descripción: El periodo de las estadísticas sin dibujar nada (RF-16): los últimos 12 meses por defecto, el periodo
 *              de la dirección de la página, que se ignora si no sirve, la dirección de un periodo y el esquema de
 *              Yup del formulario, con las reglas del backend: las dos fechas, y «Hasta» no anterior a «Desde».
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
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
 * Esquema de Yup del periodo (RP-07), con los mensajes del backend: las dos fechas son obligatorias en la pantalla,
 * y «Hasta» no puede ser anterior a «Desde»; el mismo día en las dos sí.
 *
 * @param {(clave: string) => string} t la función de traducción de i18next
 * @returns {import('yup').ObjectSchema} el esquema del formulario
 */
export function esquemaDelPeriodo(t) {
  return object({
    desde: string().default('').required(() => t('validacion.obligatorio')),
    hasta: string().default('').required(() => t('validacion.obligatorio'))
      .test('noAntesDeDesde', () => t('estadistica.hasta_antes_de_desde'), function noAntesDeDesde(hasta) {
        const { desde } = this.parent;
        return !hasta || !desde || hasta >= desde;
      }),
  });
}
