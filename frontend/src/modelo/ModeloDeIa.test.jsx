/*
 * Nombre: ModeloDeIa.test.jsx
 * Descripción: Pruebas de la pantalla «Modelo de IA» (RF-17): la tabla comparativa con las métricas de cada modelo
 *              como las responde la API y el modelo en uso marcado (C1), sus métricas macro y su matriz de confusión en
 *              una tabla accesible (D-48), la confianza promedio de los análisis de la empresa (C2), el usuario final que
 *              no la ve en su menú ni la abre por su dirección (C3), sin modelo en uso, sin análisis, el error con
 *              «Reintentar», el celular, con una tarjeta por modelo (RNF-05), y el inglés (RP-11). El periodo
 *              opcional de la confianza promedio: sin fechas, de todos los análisis; con ellas, pedido a la API, en la
 *              dirección y dicho junto a la cifra; «Todos los análisis», «Hasta» antes de «Desde» y los errores del
 *              backend en su campo.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import { act, fireEvent, screen, waitFor, within } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import i18n from '../i18n/i18n.js';
import { abrirApp, conSesion, USUARIOS } from '../pruebas/ayudas.jsx';
import {
  conMetricas, EFFICIENTNET, ERROR_DE_LA_API, FALSO, RESNET, SIN_ANALISIS, SWIN,
} from '../pruebas/modelo.js';
import { anchoDePantalla } from '../pruebas/preparacion.js';

/** La ruta de la pantalla. */
const RUTA = '/modelo';

/** Las columnas de la tabla comparativa. */
const COLUMNAS = ['Modelo', 'Tipo', 'F1-score macro (%)', 'Precisión macro (%)', 'Recall macro (%)',
  'Precisión de sano (%)', 'Recall de sano (%)', 'Precisión de no sano (%)', 'Recall de no sano (%)',
  'Tiempo por grano (ms)'];

/**
 * Abre la pantalla con la administradora de la empresa y espera su título.
 *
 * @returns {Promise<{ usuario: import('@testing-library/user-event').UserEvent }>} quien la usa
 */
async function abrirModelo() {
  conSesion(USUARIOS.ADMIN_EMPRESA);
  const abierta = abrirApp(RUTA);
  expect(await screen.findByRole('heading', { name: 'Modelo de IA', level: 1 })).toBeInTheDocument();
  return abierta;
}

/**
 * Las filas de la tabla comparativa: el texto de su título (el modelo) y de cada celda.
 *
 * @returns {string[][]} las filas, sin la de los títulos
 */
function filasDeLaComparacion() {
  const tabla = screen.getByRole('table', { name: 'Métricas de los modelos' });
  return within(tabla).getAllByRole('row').slice(1).map((fila) => [
    within(fila).getByRole('rowheader').textContent,
    ...within(fila).getAllByRole('cell').map((celda) => celda.textContent),
  ]);
}

/**
 * Una tarjeta de la pantalla, por su título.
 *
 * @param {string} titulo el título de la tarjeta
 * @returns {HTMLElement} la tarjeta
 */
function tarjeta(titulo) {
  return screen.getByRole('region', { name: titulo });
}

/**
 * El formulario del periodo de la confianza promedio.
 *
 * @returns {HTMLElement} el formulario
 */
function periodo() {
  return screen.getByRole('form', { name: 'Periodo de la confianza promedio' });
}

/**
 * Escribe las fechas del periodo, como las deja el selector de fecha del navegador.
 *
 * @param {string} desde «Desde» (aaaa-mm-dd), o vacío
 * @param {string} hasta «Hasta» (aaaa-mm-dd), o vacío
 */
function escribirPeriodo(desde, hasta) {
  fireEvent.change(within(periodo()).getByLabelText('Desde'), { target: { value: desde } });
  fireEvent.change(within(periodo()).getByLabelText('Hasta'), { target: { value: hasta } });
}

describe('Modelo de IA', () => {
  afterEach(() => anchoDePantalla(1280));

  it('RF-17 · la administradora de la empresa abre «Modelo de IA» desde su menú', async () => {
    conMetricas();
    conSesion(USUARIOS.ADMIN_EMPRESA);
    const { usuario } = abrirApp('/');

    const menu = await screen.findByRole('navigation', { name: 'Menú principal' });
    await usuario.click(within(menu).getByRole('link', { name: 'Modelo de IA' }));

    expect(await screen.findByRole('heading', { name: 'Modelo de IA', level: 1 })).toBeInTheDocument();
    expect(within(menu).getByRole('link', { name: 'Modelo de IA' })).toHaveAttribute('aria-current', 'page');
    expect(await screen.findByRole('table', { name: 'Métricas de los modelos' })).toBeInTheDocument();
    expect(screen.getByRole('main').textContent).not.toMatch(/\b(RF|RNF|RP|D|P)-\d{2}\b/);
  });

  it('RF-17 · C1 · la tabla comparativa muestra las métricas de cada modelo como las responde la API, del mayor F1-score macro al menor, y marca solo el modelo en uso', async () => {
    conMetricas();
    await abrirModelo();

    const tabla = await screen.findByRole('table', { name: 'Métricas de los modelos' });
    expect(within(tabla).getAllByRole('columnheader').map((titulo) => titulo.textContent)).toEqual(COLUMNAS);
    expect(filasDeLaComparacion()).toEqual([
      ['EfficientNet-B0Versión 1.0En uso', 'Red convolucional (CNN)', '73,50', '75,20', '72,31', '87,50', '91,20',
        '62,90', '53,40', '19,00'],
      ['ResNet-50Versión 1.0', 'Red convolucional (CNN)', '69,90', '70,11', '69,71', '86,69', '87,40', '53,52',
        '52,10', '50,80'],
      ['Swin-TinyVersión 1.0', 'Transformer', '66,40', '68,05', '65,90', '84,30', '90,10', '51,80', '41,70', '61,25'],
      ['clasificador-falsoVersión 1.0', '—', '—', '—', '—', '—', '91,20', '—', '53,40', '—'],
    ]);
    expect(within(tabla).getAllByText('En uso')).toHaveLength(1);
  });

  it('RF-17 · C1 · el modelo en uso muestra su nombre, su versión, su tipo y su F1-score, su precisión y su recall macro, medidos con los granos del conjunto de prueba', async () => {
    conMetricas();
    await abrirModelo();

    const enUso = await screen.findByRole('region', { name: 'Modelo en uso' });
    expect(within(enUso).getByText('EfficientNet-B0')).toBeInTheDocument();
    expect(within(enUso).getByText('Versión 1.0 · Red convolucional (CNN)')).toBeInTheDocument();
    const metricas = within(enUso).getAllByRole('term').map((termino) => [termino.textContent,
      termino.nextElementSibling.textContent]);
    // Entre la cifra y el signo va un espacio que no se parte.
    expect(metricas).toEqual([['F1-score macro', '73,50\u00a0%'], ['Precisión macro', '75,20\u00a0%'],
      ['Recall macro', '72,31\u00a0%']]);
    expect(within(enUso).getByText('Medidas con los 334 granos del conjunto de prueba.')).toBeInTheDocument();
  });

  it('RF-17 · C1 · la matriz de confusión del modelo en uso va en una tabla con la clase real en las filas y la que dio el modelo en las columnas, y dice cuáles son aciertos', async () => {
    conMetricas();
    await abrirModelo();

    const matriz = await screen.findByRole('table', { name: 'Matriz de confusión de EfficientNet-B0' });
    expect(within(matriz).getAllByRole('columnheader').map((titulo) => titulo.textContent))
      .toEqual(['Clase real', 'Clasificados como sanos', 'Clasificados como no sanos']);
    const filas = within(matriz).getAllByRole('row').slice(1).map((fila) => [
      within(fila).getByRole('rowheader').textContent,
      ...within(fila).getAllByRole('cell').map((celda) => celda.textContent),
    ]);
    expect(filas).toEqual([['Sanos', '238Acierto', '23Error'], ['No sanos', '34Error', '39Acierto']]);
  });

  it('RF-17 · C2 · la confianza promedio es la que responde la API para la empresa de la sesión, con cuántos granos y muestras la dan', async () => {
    conMetricas();
    await abrirModelo();

    const confianza = await screen.findByRole('region', { name: 'Confianza promedio de tus análisis' });
    expect(within(confianza).getByText('88,60 %')).toBeInTheDocument();
    expect(within(confianza).getByText('De 200 granos en 2 muestras analizadas de Cacaos del Huila.'))
      .toBeInTheDocument();
  });

  it('RF-17 · C2 · sin análisis en la empresa, la confianza promedio lo dice, sin una cifra', async () => {
    conMetricas({ confianza: SIN_ANALISIS });
    await abrirModelo();

    const confianza = await screen.findByRole('region', { name: 'Confianza promedio de tus análisis' });
    expect(within(confianza).getByText('Tu empresa todavía no tiene análisis terminados.')).toBeInTheDocument();
    expect(within(confianza).queryByText(/%/)).not.toBeInTheDocument();
  });

  it('RF-17 · C1 · sin modelo en uso, como con el clasificador de prueba, lo dice, sin matriz de confusión, y la tabla trae el modelo sin métricas', async () => {
    conMetricas({ modelos: [FALSO], confianza: SIN_ANALISIS });
    await abrirModelo();

    const enUso = await screen.findByRole('region', { name: 'Modelo en uso' });
    expect(within(enUso).getByText('Ningún modelo está en uso.')).toBeInTheDocument();
    expect(screen.queryByRole('region', { name: 'Matriz de confusión' })).not.toBeInTheDocument();
    expect(filasDeLaComparacion()).toEqual([
      ['clasificador-falsoVersión 1.0', '—', '—', '—', '—', '—', '91,20', '—', '53,40', '—'],
    ]);
  });

  it('RF-17 · C3 · un usuario final no ve «Modelo de IA» en su menú y, si escribe la dirección, la app le dice que no tiene permiso, sin pedir las métricas', async () => {
    const pedidas = conMetricas();
    conSesion(USUARIOS.USUARIO_FINAL);
    abrirApp(RUTA);

    expect(await screen.findByText('No tienes permiso para esta operación.')).toBeInTheDocument();
    const menu = screen.getByRole('navigation', { name: 'Menú principal' });
    expect(within(menu).queryByRole('link', { name: 'Modelo de IA' })).not.toBeInTheDocument();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
    expect(pedidas.veces).toBe(0);
  });

  it('RF-17 · si la API falla, la pantalla lo dice y «Reintentar» vuelve a pedir las métricas', async () => {
    conMetricas({ estado: 500 });
    const { usuario } = await abrirModelo();

    expect(await screen.findByRole('alert')).toHaveTextContent(ERROR_DE_LA_API);
    const pedidas = conMetricas();
    await usuario.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByRole('table', { name: 'Métricas de los modelos' })).toBeInTheDocument();
    expect(pedidas.veces).toBe(1);
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('RNF-05 · en el celular, la comparación va en tarjetas, una por modelo, con el modelo en uso marcado', async () => {
    anchoDePantalla(390);
    conMetricas({ modelos: [EFFICIENTNET, RESNET] });
    await abrirModelo();

    const lista = await screen.findByRole('list', { name: 'Métricas de los modelos' });
    const tarjetas = within(lista).getAllByRole('listitem');
    expect(tarjetas).toHaveLength(2);
    expect(within(tarjetas[0]).getByText('En uso')).toBeInTheDocument();
    expect(within(tarjetas[1]).queryByText('En uso')).not.toBeInTheDocument();
    const metricas = within(tarjetas[1]).getAllByRole('term').map((termino) => [termino.textContent,
      termino.nextElementSibling.textContent]);
    expect(metricas).toEqual([
      ['Tipo', 'Red convolucional (CNN)'], ['F1-score macro (%)', '69,90'], ['Precisión macro (%)', '70,11'],
      ['Recall macro (%)', '69,71'], ['Precisión de sano (%)', '86,69'], ['Recall de sano (%)', '87,40'],
      ['Precisión de no sano (%)', '53,52'], ['Recall de no sano (%)', '52,10'], ['Tiempo por grano (ms)', '50,80'],
    ]);
    expect(screen.queryByRole('table', { name: 'Métricas de los modelos' })).not.toBeInTheDocument();
  });

  it('RF-17 · sin periodo, la confianza es de todos los análisis: se pide sin fechas y «Desde» y «Hasta» van vacíos, porque son opcionales', async () => {
    const pedidas = conMetricas();
    await abrirModelo();

    await screen.findByRole('region', { name: 'Confianza promedio de tus análisis' });
    expect(pedidas.consultas).toEqual([{}]);
    expect(within(periodo()).getByLabelText('Desde')).toHaveValue('');
    expect(within(periodo()).getByLabelText('Hasta')).toHaveValue('');
    expect(within(periodo()).getByLabelText('Desde')).not.toBeRequired();
    expect(within(periodo()).queryByRole('button', { name: 'Todos los análisis' })).not.toBeInTheDocument();
  });

  it('RF-17 · con «Desde» y «Hasta», «Ver confianza» pide la confianza de ese periodo, lo deja en la dirección y lo dice junto a la cifra; las métricas del modelo no cambian', async () => {
    const pedidas = conMetricas();
    const { usuario } = await abrirModelo();
    await screen.findByRole('region', { name: 'Confianza promedio de tus análisis' });

    const delPeriodo = conMetricas({ confianza: { promedio: 91.25, granos: 100, muestras: 1 } });
    escribirPeriodo('2026-11-01', '2026-11-30');
    await usuario.click(within(periodo()).getByRole('button', { name: 'Ver confianza' }));

    const confianza = tarjeta('Confianza promedio de tus análisis');
    expect(await within(confianza).findByText('91,25 %')).toBeInTheDocument();
    expect(within(confianza).getByText('De los análisis terminados del 01/11/2026 al 30/11/2026.')).toBeInTheDocument();
    expect(new URLSearchParams(window.location.search).toString()).toBe('desde=2026-11-01&hasta=2026-11-30');
    expect(pedidas.consultas).toEqual([{}]);
    expect(delPeriodo.consultas).toEqual([{ desde: '2026-11-01', hasta: '2026-11-30' }]);
    expect(screen.getByRole('table', { name: 'Métricas de los modelos' })).toBeInTheDocument();
  });

  it('RF-17 · el periodo de la dirección se usa al abrir la pantalla, con una sola fecha si solo hay una, y «Todos los análisis» lo quita', async () => {
    const pedidas = conMetricas();
    conSesion(USUARIOS.ADMIN_EMPRESA);
    const { usuario } = abrirApp(`${RUTA}?desde=2026-11-20`);

    const confianza = await screen.findByRole('region', { name: 'Confianza promedio de tus análisis' });
    expect(pedidas.consultas).toEqual([{ desde: '2026-11-20' }]);
    expect(within(confianza).getByText('De los análisis terminados desde el 20/11/2026.')).toBeInTheDocument();
    expect(within(periodo()).getByLabelText('Desde')).toHaveValue('2026-11-20');
    await usuario.click(within(periodo()).getByRole('button', { name: 'Todos los análisis' }));

    await waitFor(() => expect(pedidas.consultas).toEqual([{ desde: '2026-11-20' }, {}]));
    expect(window.location.search).toBe('');
    expect(within(periodo()).getByLabelText('Desde')).toHaveValue('');
  });

  it('RF-17 · un periodo de la dirección que no sirve, como un día que no existe o «Hasta» antes de «Desde», se ignora y la confianza es de todos los análisis', async () => {
    const pedidas = conMetricas();
    conSesion(USUARIOS.ADMIN_EMPRESA);
    abrirApp(`${RUTA}?desde=2026-11-20&hasta=2026-11-19`);

    await screen.findByRole('region', { name: 'Confianza promedio de tus análisis' });
    expect(pedidas.consultas).toEqual([{}]);
  });

  it('RF-17 · si «Hasta» es anterior a «Desde», se marca en «Hasta» con el mensaje del backend y no se pide; el mismo día en las dos sí (prueba de límite)', async () => {
    const pedidas = conMetricas();
    const { usuario } = await abrirModelo();
    await screen.findByRole('region', { name: 'Confianza promedio de tus análisis' });

    escribirPeriodo('2026-11-20', '2026-11-19');
    await usuario.click(within(periodo()).getByRole('button', { name: 'Ver confianza' }));

    const hasta = within(periodo()).getByLabelText('Hasta');
    await waitFor(() => expect(hasta).toHaveAccessibleDescription(
      expect.stringContaining('La fecha «Hasta» no puede ser anterior a «Desde».')));
    expect(hasta).toHaveAttribute('aria-invalid', 'true');
    expect(pedidas.consultas).toHaveLength(1);
    escribirPeriodo('2026-11-20', '2026-11-20');
    await usuario.click(within(periodo()).getByRole('button', { name: 'Ver confianza' }));
    await waitFor(() => expect(pedidas.consultas.at(-1)).toEqual({ desde: '2026-11-20', hasta: '2026-11-20' }));
  });

  it('RF-17 · si el backend rechaza una fecha, su mensaje queda en ese campo', async () => {
    conMetricas();
    const { usuario } = await abrirModelo();
    await screen.findByRole('region', { name: 'Confianza promedio de tus análisis' });
    conMetricas({ estado: 400, cuerpo: { errores: [{ campo: 'desde', mensaje: 'El valor no es válido.' }] } });

    escribirPeriodo('2026-11-01', '');
    await usuario.click(within(periodo()).getByRole('button', { name: 'Ver confianza' }));

    // El formulario se rehace con el periodo de la dirección: el campo se busca otra vez.
    const desde = () => within(periodo()).getByLabelText('Desde');
    await waitFor(() => expect(desde()).toHaveAttribute('aria-invalid', 'true'));
    expect(desde()).toHaveAccessibleDescription(expect.stringContaining('El valor no es válido.'));
    expect(desde()).toHaveValue('2026-11-01');
    expect(screen.queryByRole('button', { name: 'Reintentar' })).not.toBeInTheDocument();
  });

  it('RP-11 · en inglés, los textos y los números van en inglés', async () => {
    conMetricas({ modelos: [EFFICIENTNET, SWIN] });
    await act(() => i18n.changeLanguage('en'));
    conSesion(USUARIOS.ADMIN_EMPRESA);
    abrirApp(RUTA);

    expect(await screen.findByRole('heading', { name: 'AI model', level: 1 })).toBeInTheDocument();
    const tabla = await screen.findByRole('table', { name: 'Model metrics' });
    expect(within(tabla).getAllByRole('row')[1]).toHaveTextContent('73.50');
    expect(within(tarjeta('Average confidence of your analyses')).getByText('88.60 %')).toBeInTheDocument();
    expect(screen.getByRole('table', { name: 'Confusion matrix of EfficientNet-B0' })).toBeInTheDocument();
  });
});
