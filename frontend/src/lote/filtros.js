/*
 * Nombre: filtros.js (lote)
 * Descripción: Los filtros del historial sin dibujar nada (RF-14 C2): sus valores en la dirección de la página, que
 *              se leen y se escriben con la página desde 1, ignorando los que no existen y una página mayor que la que
 *              acepta la API, y con el usuario en minúsculas, como los da la API; si hay alguno puesto, y el esquema
 *              de Yup del formulario, con las mismas reglas que valida el backend: «Hasta» no anterior a «Resultado
 *              desde» y el código del lote de hasta 30 caracteres.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { object, string } from 'yup';
import { NOMBRES_DE_LOS_FILTROS } from '../api/lotes.js';
import { recortar } from '../validacion/reglas.js';
import { LIMITES_DEL_LOTE } from './esquemaDeLote.js';

/** Los filtros sin nada puesto: así quedan al abrir el historial sin filtros y con «Limpiar». */
export const SIN_FILTROS = Object.freeze(Object.fromEntries(NOMBRES_DE_LOS_FILTROS.map((nombre) => [nombre, ''])));

/** Los estados de un lote, en el orden del prototipo (lote.json). */
export const ESTADOS_DEL_LOTE = ['PENDIENTE', 'ANALIZADO'];

/**
 * Las categorías, en el orden de los rangos de recomendación (rango_recomendacion.json). Van aquí y no se piden a la
 * API: el usuario final no lista los rangos, y el módulo de análisis está escrito a mano (D-21; RP-23 cubre los CRUD).
 */
export const CATEGORIAS = ['PREMIUM', 'ESTANDAR', 'CORRIENTE', 'FUERA_DE_NORMA'];

/** Un identificador como los de la API (UUID). */
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/** Una página de la dirección: un número entero desde 1. */
const PAGINA = /^[1-9]\d{0,8}$/;

/**
 * La página más alta que acepta la API, desde 0: la misma Pagina.PAGINA_MAXIMA del backend (Integer.MAX_VALUE / 100,
 * el tamaño máximo de una página), con la que la fila donde empieza la página todavía cabe en un int. Una página mayor
 * respondería 400, así que la de la dirección se ignora.
 */
export const PAGINA_MAXIMA = Math.floor(2147483647 / 100);

/**
 * Si un texto es un día del calendario que existe, en el formato de un campo de fecha (aaaa-mm-dd).
 *
 * @param {string|null} texto el texto
 * @returns {boolean} si es un día
 */
export function esDia(texto) {
  const partes = /^(\d{4})-(\d{2})-(\d{2})$/.exec(texto ?? '');
  if (!partes) {
    return false;
  }
  const [anio, mes, dia] = partes.slice(1).map(Number);
  const fecha = new Date(Date.UTC(anio, mes - 1, dia));
  return fecha.getUTCFullYear() === anio && fecha.getUTCMonth() === mes - 1 && fecha.getUTCDate() === dia;
}

/**
 * El código del lote como se busca: sin los espacios del inicio y del final, como en el backend.
 *
 * @param {string} codigo lo que se escribió
 * @returns {string} el código
 */
function codigoParaBuscar(codigo) {
  return recortar(codigo ?? '');
}

/**
 * Los filtros y la página que dice la dirección del historial. Un valor que no existe, como un estado que no es de un
 * lote, una fecha que no es un día, un «Hasta» anterior a «Resultado desde» o una página mayor que la que acepta la
 * API, se ignora, como si no estuviera. El usuario queda en minúsculas, como los identificadores que da la API, para
 * encontrarlo en la lista «Usuario».
 *
 * @param {URLSearchParams} parametros los parámetros de la dirección
 * @returns {{ filtros: import('../api/lotes.js').FiltrosDelHistorial, pagina: number }} los filtros y la página,
 *   desde 0
 */
export function leerDireccion(parametros) {
  const valor = (nombre) => parametros.get(nombre) ?? '';
  const codigo = codigoParaBuscar(valor('codigo'));
  const filtros = {
    desde: esDia(valor('desde')) ? valor('desde') : '',
    hasta: esDia(valor('hasta')) ? valor('hasta') : '',
    estado: ESTADOS_DEL_LOTE.includes(valor('estado')) ? valor('estado') : '',
    categoria: CATEGORIAS.includes(valor('categoria')) ? valor('categoria') : '',
    codigo: codigo.length <= LIMITES_DEL_LOTE.codigo ? codigo : '',
    usuario: UUID.test(valor('usuario')) ? valor('usuario').toLowerCase() : '',
  };
  if (filtros.desde && filtros.hasta && filtros.hasta < filtros.desde) {
    filtros.hasta = '';
  }
  const pagina = PAGINA.test(valor('pagina')) ? Number(valor('pagina')) - 1 : 0;
  return { filtros, pagina: pagina <= PAGINA_MAXIMA ? pagina : 0 };
}

/**
 * La dirección del historial con unos filtros y una página: solo los filtros puestos y la página desde 1, que no va
 * si es la primera.
 *
 * @param {import('../api/lotes.js').FiltrosDelHistorial} filtros los filtros
 * @param {number} pagina la página, desde 0
 * @returns {URLSearchParams} los parámetros de la dirección
 */
export function direccionDe(filtros, pagina) {
  const parametros = new URLSearchParams();
  for (const nombre of NOMBRES_DE_LOS_FILTROS) {
    if (filtros[nombre]) {
      parametros.set(nombre, filtros[nombre]);
    }
  }
  if (pagina > 0) {
    parametros.set('pagina', String(pagina + 1));
  }
  return parametros;
}

/**
 * Si hay algún filtro puesto.
 *
 * @param {import('../api/lotes.js').FiltrosDelHistorial} filtros los filtros
 * @returns {boolean} si hay alguno
 */
export function hayFiltros(filtros) {
  return NOMBRES_DE_LOS_FILTROS.some((nombre) => Boolean(filtros[nombre]));
}

/**
 * Los filtros del formulario como se aplican: el código, sin los espacios del inicio y del final.
 *
 * @param {import('../api/lotes.js').FiltrosDelHistorial} valores los valores del formulario
 * @returns {import('../api/lotes.js').FiltrosDelHistorial} los filtros
 */
export function filtrosParaAplicar(valores) {
  return { ...valores, codigo: codigoParaBuscar(valores.codigo) };
}

/**
 * Esquema de Yup de los filtros del historial (RP-07), con las reglas y los mensajes del backend: «Hasta» no puede
 * ser anterior a «Resultado desde», el mismo día en los dos sí, y el código tiene hasta 30 caracteres, contados sin
 * los espacios del inicio y del final.
 *
 * @param {(clave: string, valores?: object) => string} t la función de traducción de i18next
 * @returns {import('yup').ObjectSchema} el esquema del formulario
 */
export function esquemaDeFiltros(t) {
  return object({
    hasta: string().default('')
      .test('noAntesDeDesde', () => t('lote.filtro.hasta_antes_de_desde'), function noAntesDeDesde(hasta) {
        const { desde } = this.parent;
        return !hasta || !desde || hasta >= desde;
      }),
    codigo: string().default('')
      .test('largo', () => t('validacion.longitud_max', { max: LIMITES_DEL_LOTE.codigo }),
        (codigo) => codigoParaBuscar(codigo).length <= LIMITES_DEL_LOTE.codigo),
  });
}
