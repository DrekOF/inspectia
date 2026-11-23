/*
 * Nombre: Historial.test.jsx
 * Descripción: Pruebas de la pantalla Historial de lotes (RF-14): las columnas del prototipo, un lote pendiente
 *              y uno analizado, las páginas de 20 lotes, la lista vacía, con «Nuevo lote» solo para el usuario
 *              final, y las tarjetas del celular.
 * Fecha de creación: 10/11/2026
 * Actualización: 22/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it } from 'vitest';
import { abrirApp, conSesion, USUARIOS } from '../pruebas/ayudas.jsx';
import { LOTE_ANALIZADO, LOTE_PENDIENTE, loteNumero, paginaDeLotes } from '../pruebas/lotes.js';
import { anchoDePantalla } from '../pruebas/preparacion.js';
import { servidor } from '../pruebas/servidor.js';

const COLUMNAS = ['Fecha del resultado', 'Código', 'Finca o proveedor', 'Estado', '% de granos sanos',
  'Categoría probable', 'Registrado por'];

/**
 * Responde las páginas del historial y anota cuáles se pidieron.
 *
 * @param {(pagina: number) => object} pagina la página que responde según su número
 * @returns {{ pedidas: string[] }} las consultas que llegaron, como pagina=0&tamano=20
 */
function conHistorial(pagina) {
  const pedidas = [];
  servidor.use(http.get('/api/v1/lotes', ({ request }) => {
    const parametros = new URL(request.url).searchParams;
    pedidas.push(parametros.toString());
    return HttpResponse.json(pagina(Number(parametros.get('pagina') ?? 0)));
  }));
  return { pedidas };
}

/**
 * Las celdas de cada fila de la tabla, sin la de los títulos.
 *
 * @returns {string[][]} el texto de cada celda, por fila
 */
function filas() {
  const tabla = screen.getByRole('table', { name: 'Lotes de la empresa' });
  return within(tabla).getAllByRole('row').slice(1)
    .map((fila) => within(fila).getAllByRole('cell').map((celda) => celda.textContent));
}

describe('Historial de lotes', () => {
  afterEach(() => anchoDePantalla(1280));

  it('RF-14 · la lista trae las columnas del prototipo: un lote analizado con su resultado y uno pendiente sin él', async () => {
    conSesion(USUARIOS.USUARIO_FINAL);
    conHistorial(() => paginaDeLotes([LOTE_ANALIZADO, LOTE_PENDIENTE]));
    abrirApp('/historial');

    expect(await screen.findByRole('heading', { name: 'Historial de lotes' })).toBeInTheDocument();
    expect(screen.getByText('Los lotes de Cacaos del Huila, del más reciente al más antiguo.')).toBeInTheDocument();
    const tabla = await screen.findByRole('table', { name: 'Lotes de la empresa' });
    expect(within(tabla).getAllByRole('columnheader').map((titulo) => titulo.textContent)).toEqual(COLUMNAS);
    expect(filas()).toEqual([
      ['18/10/2026', 'L-0139', 'La Estrella', 'Analizado', '93,0 %', 'Estándar', 'María Torres'],
      ['—', 'L-0143', 'La Esperanza', 'Pendiente de análisis', '—', '—', 'Carlos Ramírez'],
    ]);
  });

  it('RF-14 · C4 · con más de 20 lotes, la lista se divide en páginas de 20 y se pasa a la siguiente', async () => {
    conSesion(USUARIOS.USUARIO_FINAL);
    const todos = Array.from({ length: 25 }, (_, indice) => loteNumero(25 - indice));
    const { pedidas } = conHistorial((pagina) => paginaDeLotes(todos.slice(pagina * 20, pagina * 20 + 20),
      { pagina, total: 25 }));
    const { usuario } = abrirApp('/historial');

    await screen.findByRole('table', { name: 'Lotes de la empresa' });
    const paginas = screen.getByRole('navigation', { name: 'Páginas del historial' });
    expect(within(paginas).getByText('25 lotes · 20 por página')).toBeInTheDocument();
    expect(filas()).toHaveLength(20);
    expect(filas()[0][1]).toBe('L-0025');
    expect(within(paginas).getByRole('button', { name: 'Página anterior' })).toBeDisabled();

    await usuario.click(within(paginas).getByRole('button', { name: 'Página siguiente' }));

    expect(await screen.findByText('L-0005')).toBeInTheDocument();
    expect(filas().map((fila) => fila[1])).toEqual(['L-0005', 'L-0004', 'L-0003', 'L-0002', 'L-0001']);
    expect(within(paginas).getByRole('button', { name: 'Página siguiente' })).toBeDisabled();
    expect(pedidas).toEqual(['pagina=0&tamano=20', 'pagina=1&tamano=20']);
  });

  it('RF-14 · la administradora de la empresa también ve el historial', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    conHistorial(() => paginaDeLotes([LOTE_PENDIENTE]));
    abrirApp('/historial');

    expect(await screen.findByRole('table', { name: 'Lotes de la empresa' })).toBeInTheDocument();
    expect(filas()).toHaveLength(1);
  });

  it('RF-14 · sin lotes, al usuario final la lista le dice que no hay y le ofrece «Nuevo lote» para registrar el primero', async () => {
    conSesion(USUARIOS.USUARIO_FINAL);
    conHistorial(() => paginaDeLotes([]));
    const { usuario } = abrirApp('/historial');

    expect(await screen.findByText('Tu empresa todavía no tiene lotes.')).toBeInTheDocument();
    expect(screen.queryByRole('navigation', { name: 'Páginas del historial' })).not.toBeInTheDocument();
    await usuario.click(within(screen.getByRole('main')).getByRole('link', { name: 'Nuevo lote' }));
    expect(await screen.findByRole('heading', { name: 'Nuevo lote' })).toBeInTheDocument();
  });

  it('RF-14 · sin lotes, la administradora solo ve el texto: no registra lotes (RF-04)', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    conHistorial(() => paginaDeLotes([]));
    abrirApp('/historial');

    expect(await screen.findByText('Tu empresa todavía no tiene lotes.')).toBeInTheDocument();
    expect(within(screen.getByRole('main')).queryByRole('link', { name: 'Nuevo lote' })).not.toBeInTheDocument();
  });

  it('RNF-05 · en el celular cada lote va en una tarjeta, con su código, su estado y su resultado', async () => {
    anchoDePantalla(375);
    conSesion(USUARIOS.USUARIO_FINAL);
    conHistorial(() => paginaDeLotes([LOTE_ANALIZADO, LOTE_PENDIENTE]));
    abrirApp('/historial');

    const lista = await screen.findByRole('list', { name: 'Lotes de la empresa' });
    const tarjetas = within(lista).getAllByRole('listitem');
    expect(tarjetas).toHaveLength(2);
    expect(tarjetas[0]).toHaveTextContent('L-0139');
    expect(tarjetas[0]).toHaveTextContent('93,0 %');
    expect(tarjetas[0]).toHaveTextContent('Estándar');
    expect(tarjetas[1]).toHaveTextContent('Pendiente de análisis');
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });
});
