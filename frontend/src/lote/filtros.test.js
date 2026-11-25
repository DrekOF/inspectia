/*
 * Nombre: filtros.test.js (lote)
 * Descripción: Pruebas de los filtros del historial sin dibujar nada (RF-14 C2, restricciones 5 y 8): qué texto es un
 *              día, cómo se leen la dirección y la página ignorando los valores que no sirven, también una página
 *              mayor que la que acepta la API, el usuario en minúsculas, cómo se escribe la dirección, si hay filtros
 *              puestos, el código recortado o que falta y el esquema de Yup del formulario.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { describe, expect, it } from 'vitest';
import {
  PAGINA_MAXIMA, SIN_FILTROS, direccionDe, esDia, esquemaDeFiltros, filtrosParaAplicar, hayFiltros, leerDireccion,
} from './filtros.js';

/** Un identificador de usuario como los de la API. */
const LUIS = '0193a1b2-c3d4-7e5f-8a9b-0c1d2e3f4a5b';

/**
 * Una traducción de prueba que deja ver la clave y sus valores.
 *
 * @param {string} clave la clave del texto
 * @param {object} [valores] los valores del texto
 * @returns {string} la clave, con sus valores si los hay
 */
const t =(clave, valores) => (valores ? `${clave} ${JSON.stringify(valores)}` : clave);

/**
 * Los errores del esquema de los filtros con unos valores, por campo.
 *
 * @param {object} valores los valores del formulario
 * @returns {Promise<Record<string, string>>} el mensaje de cada campo con error
 */
async function erroresDe(valores) {
  try {
    await esquemaDeFiltros(t).validate(valores, { abortEarly: false });
    return {};
  } catch (error) {
    return Object.fromEntries(error.inner.map((detalle) => [detalle.path, detalle.message]));
  }
}

/**
 * Lee una dirección escrita como texto.
 *
 * @param {string} texto los parámetros, como «estado=ANALIZADO&pagina=2»
 * @returns {ReturnType<typeof leerDireccion>} los filtros y la página
 */
const leer = (texto) => leerDireccion(new URLSearchParams(texto));

describe('Qué texto es un día', () => {
  it('RF-14 · un día que existe, con el formato de un campo de fecha (aaaa-mm-dd), es un día; el 29 de febrero, solo en un año bisiesto (prueba de límite)', () => {
    expect(esDia('2026-11-24')).toBe(true);
    expect(esDia('2028-02-29')).toBe(true);
    expect(esDia('2026-12-31')).toBe(true);
    expect(esDia('2026-02-29')).toBe(false);
  });

  it('RF-14 · un día que no existe no es un día: el 31 de abril, el día 0, el mes 0 o el 13 (prueba de límite)', () => {
    expect(esDia('2026-04-31')).toBe(false);
    expect(esDia('2026-01-00')).toBe(false);
    expect(esDia('2026-00-15')).toBe(false);
    expect(esDia('2026-13-01')).toBe(false);
  });

  it('RF-14 · un año de menos de 100 no es un día, aunque su mes y su día existan: Date lo leería como de 1900', () => {
    expect(esDia('0050-05-05')).toBe(false);
  });

  it('RF-14 · un texto con algo antes o después de la fecha, con otro formato o vacío no es un día', () => {
    expect(esDia('x2026-11-24')).toBe(false);
    expect(esDia('2026-11-24x')).toBe(false);
    expect(esDia('2026-11-24T00:00')).toBe(false);
    expect(esDia('2026-1-5')).toBe(false);
    expect(esDia('24/11/2026')).toBe(false);
    expect(esDia('')).toBe(false);
    expect(esDia(null)).toBe(false);
  });
});

describe('Leer la dirección del historial', () => {
  it('RF-14 · sin parámetros no hay filtros y la página es la primera (0 en la API)', () => {
    expect(leer('')).toEqual({ filtros: { ...SIN_FILTROS }, pagina: 0 });
    expect(hayFiltros(leer('').filtros)).toBe(false);
  });

  it('RF-14 · lee los seis filtros y la página, que en la dirección va desde 1 y en la API desde 0', () => {
    const { filtros, pagina } = leer(`desde=2026-11-01&hasta=2026-11-24&estado=ANALIZADO&categoria=ESTANDAR&codigo=L-01&usuario=${LUIS}&pagina=3`);
    expect(filtros).toEqual({
      desde: '2026-11-01', hasta: '2026-11-24', estado: 'ANALIZADO', categoria: 'ESTANDAR', codigo: 'L-01', usuario: LUIS,
    });
    expect(pagina).toBe(2);
  });

  it('RF-14 · un estado, una categoría, una fecha o un usuario que no sirven se ignoran, como si no estuvieran', () => {
    expect(leer('estado=BORRADO&categoria=BUENA&desde=2026-02-30&hasta=ayer&usuario=luis').filtros).toEqual({ ...SIN_FILTROS });
    expect(leer('estado=analizado').filtros.estado).toBe('');
    expect(leer(`usuario=${LUIS}x`).filtros.usuario).toBe('');
  });

  it('RF-14 · una página que no es un entero desde 1 se ignora y queda la primera (prueba de límite)', () => {
    expect(leer('pagina=1').pagina).toBe(0);
    for (const pagina of ['0', '-1', '2.5', 'dos', '01', '']) {
      expect(leer(`pagina=${pagina}`).pagina).toBe(0);
    }
  });

  it('RF-14 · la página más alta es la que acepta la API, la Pagina.PAGINA_MAXIMA del backend (21474836 desde 0): esa se lee y una mayor se ignora y queda la primera (prueba de límite)', () => {
    expect(PAGINA_MAXIMA).toBe(21_474_836);
    expect(leer('pagina=21474837').pagina).toBe(PAGINA_MAXIMA);
    expect(leer('pagina=21474838').pagina).toBe(0);
    expect(leer('pagina=30000000').pagina).toBe(0);
    expect(leer('pagina=999999999').pagina).toBe(0);
  });

  it('RF-14 · el usuario de la dirección se lee en minúsculas, como los identificadores que da la API, aunque venga en mayúsculas, y así se vuelve a escribir', () => {
    const { filtros } = leer(`usuario=${LUIS.toUpperCase()}`);
    expect(filtros.usuario).toBe(LUIS);
    expect(direccionDe(filtros, 0).toString()).toBe(`usuario=${LUIS}`);
    expect(leer(`usuario=${LUIS}`).filtros.usuario).toBe(LUIS);
  });

  it('RF-14 · el código se lee sin los espacios del inicio y del final; con más de 30 caracteres se ignora (prueba de límite)', () => {
    const treinta = 'L'.repeat(30);
    expect(leer('codigo=%20%20l-01%20').filtros.codigo).toBe('l-01');
    expect(leer(`codigo=${treinta}`).filtros.codigo).toBe(treinta);
    expect(leer(`codigo=%20${treinta}%20`).filtros.codigo).toBe(treinta);
    expect(leer(`codigo=${treinta}L`).filtros.codigo).toBe('');
  });

  it('RF-14 · un «Hasta» anterior a «Resultado desde» se ignora y «desde» queda; el mismo día, o una sola de las dos, sí sirven (prueba de límite)', () => {
    expect(leer('desde=2026-11-24&hasta=2026-11-23').filtros).toMatchObject({ desde: '2026-11-24', hasta: '' });
    expect(leer('desde=2026-11-24&hasta=2026-11-24').filtros).toMatchObject({ desde: '2026-11-24', hasta: '2026-11-24' });
    expect(leer('hasta=2026-11-23').filtros).toMatchObject({ desde: '', hasta: '2026-11-23' });
    expect(leer('desde=2026-11-24').filtros).toMatchObject({ desde: '2026-11-24', hasta: '' });
  });
});

describe('Escribir la dirección del historial', () => {
  it('RF-14 · lleva solo los filtros puestos, y la página desde 1 solo si no es la primera', () => {
    const filtros = { ...SIN_FILTROS, estado: 'PENDIENTE', codigo: 'L-01' };
    expect(direccionDe(filtros, 0).toString()).toBe('estado=PENDIENTE&codigo=L-01');
    expect(direccionDe(filtros, 1).toString()).toBe('estado=PENDIENTE&codigo=L-01&pagina=2');
    expect(direccionDe({ ...SIN_FILTROS }, 0).toString()).toBe('');
  });

  it('RF-14 · lo que se escribe en la dirección se vuelve a leer igual: filtros y página', () => {
    const filtros = {
      desde: '2026-11-01', hasta: '2026-11-24', estado: 'ANALIZADO', categoria: 'FUERA_DE_NORMA', codigo: 'l-0142', usuario: LUIS,
    };
    expect(leerDireccion(direccionDe(filtros, 4))).toEqual({ filtros, pagina: 4 });
  });
});

describe('Filtros puestos y filtros para aplicar', () => {
  it('RF-14 · hay filtros si alguno tiene valor', () => {
    expect(hayFiltros({ ...SIN_FILTROS })).toBe(false);
    for (const nombre of Object.keys(SIN_FILTROS)) {
      expect(hayFiltros({ ...SIN_FILTROS, [nombre]: 'x' })).toBe(true);
    }
  });

  it('RF-14 · el código se aplica sin los espacios del inicio y del final, y los demás filtros quedan igual', () => {
    const valores = { ...SIN_FILTROS, estado: 'ANALIZADO', codigo: '  L-01 ' };
    expect(filtrosParaAplicar(valores)).toEqual({ ...valores, codigo: 'L-01' });
  });

  it('RF-14 · un código que falta se aplica vacío, como si no se hubiera escrito', () => {
    expect(filtrosParaAplicar({ ...SIN_FILTROS, codigo: undefined }).codigo).toBe('');
    expect(filtrosParaAplicar({ ...SIN_FILTROS, codigo: null }).codigo).toBe('');
  });
});

describe('Esquema de los filtros', () => {
  it('RF-14 · «Hasta» anterior a «Resultado desde» se marca en «Hasta» con el mensaje del backend; el mismo día, o una sola de las dos, no (prueba de límite)', async () => {
    expect(await erroresDe({ desde: '2026-11-24', hasta: '2026-11-23', codigo: '' }))
      .toEqual({ hasta: 'lote.filtro.hasta_antes_de_desde' });
    expect(await erroresDe({ desde: '2026-11-24', hasta: '2026-11-24', codigo: '' })).toEqual({});
    expect(await erroresDe({ desde: '', hasta: '2026-11-23', codigo: '' })).toEqual({});
    expect(await erroresDe({ desde: '2026-11-24', hasta: '', codigo: '' })).toEqual({});
  });

  it('RF-14 · el código tiene hasta 30 caracteres, contados sin los espacios del inicio y del final (prueba de límite)', async () => {
    const treinta = 'L'.repeat(30);
    expect(await erroresDe({ desde: '', hasta: '', codigo: `  ${treinta} ` })).toEqual({});
    expect(await erroresDe({ desde: '', hasta: '', codigo: `${treinta}L` }))
      .toEqual({ codigo: 'validacion.longitud_max {"max":30}' });
  });

  it('RF-14 · sin valores, «Hasta» y el código quedan vacíos y no tienen errores', async () => {
    expect(esquemaDeFiltros(t).cast({})).toEqual({ hasta: '', codigo: '' });
    expect(await erroresDe({})).toEqual({});
  });
});
