/*
 * Nombre: Estadisticas.test.jsx
 * Descripción: Pruebas de la pantalla Estadísticas (RF-16): la entrada del menú del administrador de empresa, el
 *              periodo de los últimos 12 meses al abrirla, las cifras que da el backend (lotes analizados y promedio),
 *              cada gráfica con su tabla (lotes por categoría, evolución por mes y promedio por finca o proveedor), el
 *              mensaje sin lotes en lugar de gráficas vacías (C2), el usuario final sin permiso (C3), cada finca una
 *              vez (C4), el periodo en la dirección, sus errores en el campo, los del backend, el error que se
 *              reintenta, el celular y el inglés.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { fireEvent, screen, within } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import i18n from '../i18n/i18n.js';
import { hoyEnColombia } from '../lote/fechas.js';
import { abrirApp, conSesion, USUARIOS } from '../pruebas/ayudas.jsx';
import { conEstadisticas, ESTADISTICAS, SIN_LOTES } from '../pruebas/estadisticas.js';
import { anchoDePantalla } from '../pruebas/preparacion.js';
import { problema } from '../pruebas/servidor.js';

/** El mensaje sin lotes en el periodo (RF-16 C2). */
const SIN_LOTES_EN_EL_PERIODO = 'No hay lotes analizados en este periodo. Elige otro periodo para ver sus estadísticas.';

/**
 * El periodo de los últimos 12 meses: del primer día del mes de hace 11 meses hasta hoy, en Colombia.
 *
 * @returns {{ desde: string, hasta: string }} el periodo
 */
function ultimos12Meses() {
  const hoy = hoyEnColombia();
  const [anio, mes] = hoy.split('-').map(Number);
  const inicio = new Date(Date.UTC(anio, mes - 1 - 11, 1));
  return { desde: inicio.toISOString().slice(0, 10), hasta: hoy };
}

/**
 * Un mes (aaaa-mm) como lo muestra la tabla, en el idioma dado.
 *
 * @param {string} mes el mes
 * @param {string} [region] la región del idioma
 * @returns {string} el mes, como «septiembre de 2026»
 */
function mesComoSeVe(mes, region = 'es-CO') {
  const [anio, numero] = mes.split('-').map(Number);
  return new Intl.DateTimeFormat(region, { month: 'long', year: 'numeric', timeZone: 'UTC' })
    .format(new Date(Date.UTC(anio, numero - 1, 1)));
}

/**
 * Las celdas de cada fila de una tabla, sin la de los títulos.
 *
 * @param {string} nombre el nombre de la tabla
 * @returns {string[][]} el texto de cada celda, por fila
 */
function filas(nombre) {
  const tabla = screen.getByRole('table', { name: nombre });
  return within(tabla).getAllByRole('row').slice(1)
    .map((fila) => within(fila).getAllByRole('cell').map((celda) => celda.textContent));
}

/**
 * Los títulos de las columnas de una tabla.
 *
 * @param {string} nombre el nombre de la tabla
 * @returns {string[]} los títulos
 */
function columnas(nombre) {
  return within(screen.getByRole('table', { name: nombre })).getAllByRole('columnheader')
    .map((titulo) => titulo.textContent);
}

/**
 * Las cifras de arriba: cada nombre con su valor.
 *
 * @returns {string[][]} el nombre y el valor de cada cifra
 */
function cifras() {
  const nombres = screen.getAllByRole('term').map((nombre) => nombre.textContent);
  const valores = screen.getAllByRole('definition').map((valor) => valor.textContent);
  return nombres.map((nombre, indice) => [nombre, valores[indice]]);
}

/**
 * El formulario del periodo.
 *
 * @returns {HTMLElement} el formulario
 */
function periodo() {
  return screen.getByRole('form', { name: 'Periodo de las estadísticas' });
}

/**
 * Escribe un periodo y pulsa «Ver estadísticas».
 *
 * @param {import('@testing-library/user-event').UserEvent} usuario quien usa la pantalla
 * @param {string} desde el primer día (aaaa-mm-dd), o vacío
 * @param {string} hasta el último día (aaaa-mm-dd), o vacío
 */
async function verPeriodo(usuario, desde, hasta) {
  fireEvent.change(within(periodo()).getByLabelText('Desde'), { target: { value: desde } });
  fireEvent.change(within(periodo()).getByLabelText('Hasta'), { target: { value: hasta } });
  await usuario.click(within(periodo()).getByRole('button', { name: 'Ver estadísticas' }));
}

describe('Estadísticas', () => {
  afterEach(() => anchoDePantalla(1280));

  it('RF-16 · el administrador de empresa abre «Estadísticas» desde su menú y ve los últimos 12 meses, con el periodo en los campos', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    const pedidos = conEstadisticas(() => ESTADISTICAS);
    const { usuario } = abrirApp('/');

    await usuario.click(within(await screen.findByRole('navigation', { name: 'Menú principal' }))
      .getByRole('link', { name: 'Estadísticas' }));

    expect(await screen.findByRole('heading', { level: 1, name: 'Estadísticas' })).toBeInTheDocument();
    expect(screen.getByText('Los lotes analizados de Cacaos del Huila, según la fecha de su resultado.'))
      .toBeInTheDocument();
    expect(await screen.findByRole('table', { name: 'Lotes por categoría' })).toBeInTheDocument();
    expect(pedidos).toEqual([ultimos12Meses()]);
    expect(within(periodo()).getByLabelText('Desde')).toHaveValue(ultimos12Meses().desde);
    expect(within(periodo()).getByLabelText('Hasta')).toHaveValue(ultimos12Meses().hasta);
    expect(screen.getByRole('main').textContent).not.toMatch(/\b(RF|RNF|RP|D|P)-\d{2}\b/);
  });

  it('RF-16 · C1 · la pantalla muestra las cifras del backend: lotes analizados, promedio y lotes por categoría con su parte del total', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    conEstadisticas(() => ESTADISTICAS);
    abrirApp('/estadisticas?desde=2026-09-01&hasta=2026-11-30');

    await screen.findByRole('table', { name: 'Lotes por categoría' });
    expect(cifras()).toEqual([['Lotes analizados', '10'], ['Promedio de granos sanos', '90,4 %']]);
    expect(columnas('Lotes por categoría')).toEqual(['Categoría probable', 'Lotes', '% de los lotes']);
    expect(filas('Lotes por categoría')).toEqual([
      ['Premium o Especial', '4', '40,0 %'],
      ['Estándar', '3', '30,0 %'],
      ['Corriente', '2', '20,0 %'],
      ['Fuera de norma', '1', '10,0 %'],
    ]);
    expect(screen.getByText('La categoría es probable: la oficial se confirma con la prueba de corte.'))
      .toBeInTheDocument();
    // Lo anuncia a los lectores de pantalla, como el total del historial.
    expect(screen.getByRole('status')).toHaveTextContent('10 lotes analizados en el periodo');
  });

  it('RF-16 · la evolución del promedio por mes tiene su gráfica y su tabla, y un mes sin lotes dice «Sin lotes» en lugar de un promedio', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    conEstadisticas(() => ESTADISTICAS);
    abrirApp('/estadisticas?desde=2026-09-01&hasta=2026-11-30');

    await screen.findByRole('table', { name: 'Evolución del promedio por mes' });
    expect(columnas('Evolución del promedio por mes')).toEqual(['Mes', 'Lotes', 'Promedio de granos sanos']);
    expect(filas('Evolución del promedio por mes')).toEqual([
      [mesComoSeVe('2026-09'), '4', '88,5 %'],
      [mesComoSeVe('2026-10'), '0', 'Sin lotes'],
      [mesComoSeVe('2026-11'), '6', '91,7 %'],
    ]);
    // La gráfica es un dibujo: su tabla dice lo mismo, así que los lectores de pantalla la saltan.
    const grafica = screen.getByTestId('grafica-por-mes');
    expect(grafica).toHaveAttribute('aria-hidden', 'true');
    expect(grafica.querySelectorAll('[data-columna]')).toHaveLength(2);
  });

  it('RF-16 · C4 · el promedio por finca o proveedor muestra cada finca una vez, como la agrupa el backend, de mayor a menor promedio', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    conEstadisticas(() => ESTADISTICAS);
    abrirApp('/estadisticas?desde=2026-09-01&hasta=2026-11-30');

    await screen.findByRole('table', { name: 'Promedio por finca o proveedor' });
    expect(columnas('Promedio por finca o proveedor'))
      .toEqual(['Finca o proveedor', 'Promedio de granos sanos', 'Lotes']);
    expect(filas('Promedio por finca o proveedor')).toEqual([
      ['La Esperanza', '93,2 %', '6'],
      ['Paraíso', '86,2 %', '4'],
    ]);
  });

  it('RF-16 · C2 · sin lotes en el periodo, muestra un mensaje en lugar de gráficas vacías', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    conEstadisticas(() => SIN_LOTES);
    abrirApp('/estadisticas?desde=2026-09-01&hasta=2026-11-30');

    expect(await screen.findByText(SIN_LOTES_EN_EL_PERIODO)).toBeInTheDocument();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
    expect(screen.queryByRole('term')).not.toBeInTheDocument();
    expect(screen.queryByTestId('grafica-por-mes')).not.toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('0 lotes analizados en el periodo');
    // El periodo sigue a la vista, para elegir otro.
    expect(within(periodo()).getByLabelText('Desde')).toHaveValue('2026-09-01');
  });

  it('RF-16 · C3 · un usuario final no puede entrar a esta pantalla: no la tiene en su menú y, si escribe la dirección, ve que no tiene permiso y no se piden las estadísticas', async () => {
    conSesion(USUARIOS.USUARIO_FINAL);
    const pedidos = conEstadisticas(() => ESTADISTICAS);

    abrirApp('/estadisticas');

    expect(await screen.findByText('No tienes permiso para esta operación.')).toBeInTheDocument();
    expect(within(await screen.findByRole('navigation', { name: 'Menú principal' }))
      .queryByRole('link', { name: 'Estadísticas' })).not.toBeInTheDocument();
    expect(pedidos).toEqual([]);
  });

  it('RF-16 · al elegir otro periodo y pulsar «Ver estadísticas», lo pide al backend y lo deja en la dirección, así recargar lo conserva', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    const pedidos = conEstadisticas(() => ESTADISTICAS);
    const { usuario } = abrirApp('/estadisticas');
    await screen.findByRole('table', { name: 'Lotes por categoría' });

    await verPeriodo(usuario, '2026-01-01', '2026-06-30');

    await screen.findByText('Lotes analizados');
    expect(pedidos.at(-1)).toEqual({ desde: '2026-01-01', hasta: '2026-06-30' });
    expect(window.location.search).toBe('?desde=2026-01-01&hasta=2026-06-30');
  });

  it('RF-16 · si «Hasta» es anterior a «Desde», o falta una fecha, lo marca en su campo y no pide las estadísticas', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    const pedidos = conEstadisticas(() => ESTADISTICAS);
    const { usuario } = abrirApp('/estadisticas?desde=2026-09-01&hasta=2026-11-30');
    await screen.findByRole('table', { name: 'Lotes por categoría' });

    await verPeriodo(usuario, '2026-11-20', '2026-11-19');

    const hasta = within(periodo()).getByLabelText('Hasta');
    expect(await within(periodo()).findByText('La fecha «Hasta» no puede ser anterior a «Desde».')).toBeInTheDocument();
    expect(hasta).toHaveAttribute('aria-invalid', 'true');
    expect(hasta).toHaveFocus();

    await verPeriodo(usuario, '', '2026-11-19');

    expect(await within(periodo()).findByText('Este dato es obligatorio.')).toBeInTheDocument();
    expect(within(periodo()).getByLabelText('Desde')).toHaveAttribute('aria-invalid', 'true');
    expect(pedidos).toHaveLength(1);
  });

  it('RF-16 · un periodo de la dirección que no sirve, como un día que no existe o «Hasta» antes de «Desde», se ignora y se ven los últimos 12 meses', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    const pedidos = conEstadisticas(() => ESTADISTICAS);

    abrirApp('/estadisticas?desde=2026-02-30&hasta=2026-01-01');

    await screen.findByRole('table', { name: 'Lotes por categoría' });
    expect(pedidos).toEqual([ultimos12Meses()]);
  });

  it('RNF-06 · C3 · un error del backend en una fecha se marca en su campo, con su mensaje', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    conEstadisticas(() => problema(400, 'Revisa los datos marcados.',
      { errores: [{ campo: 'desde', mensaje: 'El valor no es válido.' }] }));

    abrirApp('/estadisticas?desde=2026-09-01&hasta=2026-11-30');

    expect(await within(periodo()).findByText('El valor no es válido.')).toBeInTheDocument();
    expect(within(periodo()).getByLabelText('Desde')).toHaveAttribute('aria-invalid', 'true');
    expect(screen.queryByRole('button', { name: 'Reintentar' })).not.toBeInTheDocument();
  });

  it('RNF-06 · C3 · si el backend falla, lo dice y ofrece reintentar, que vuelve a pedir las estadísticas', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    let veces = 0;
    conEstadisticas(() => {
      veces += 1;
      return veces === 1 ? problema(500, 'Ocurrió un error inesperado. Intenta de nuevo más tarde.') : ESTADISTICAS;
    });
    const { usuario } = abrirApp('/estadisticas?desde=2026-09-01&hasta=2026-11-30');

    expect(await screen.findByText('Ocurrió un error inesperado. Intenta de nuevo más tarde.')).toBeInTheDocument();
    await usuario.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByRole('table', { name: 'Lotes por categoría' })).toBeInTheDocument();
    expect(veces).toBe(2);
  });

  it('RNF-05 · en el celular se ven las mismas cifras y tablas, con el botón del periodo a todo el ancho', async () => {
    anchoDePantalla(390);
    conSesion(USUARIOS.ADMIN_EMPRESA);
    conEstadisticas(() => ESTADISTICAS);
    abrirApp('/estadisticas?desde=2026-09-01&hasta=2026-11-30');

    await screen.findByRole('table', { name: 'Lotes por categoría' });
    expect(cifras()).toEqual([['Lotes analizados', '10'], ['Promedio de granos sanos', '90,4 %']]);
    expect(screen.getByRole('table', { name: 'Evolución del promedio por mes' })).toBeInTheDocument();
    expect(screen.getByRole('table', { name: 'Promedio por finca o proveedor' })).toBeInTheDocument();
  });

  it('RP-11 · en inglés, la pantalla usa sus textos, el formato de los números y el de los meses del inglés', async () => {
    await i18n.changeLanguage('en');
    conSesion(USUARIOS.ADMIN_EMPRESA);
    conEstadisticas(() => ESTADISTICAS);
    abrirApp('/estadisticas?desde=2026-09-01&hasta=2026-11-30');

    expect(await screen.findByRole('heading', { level: 1, name: 'Statistics' })).toBeInTheDocument();
    await screen.findByRole('table', { name: 'Lots by category' });
    expect(cifras()).toEqual([['Analyzed lots', '10'], ['Average healthy beans', '90.4 %']]);
    expect(filas('Average by month')[1]).toEqual([mesComoSeVe('2026-10', 'en-US'), '0', 'No lots']);
  });
});
