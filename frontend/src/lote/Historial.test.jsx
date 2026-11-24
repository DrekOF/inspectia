/*
 * Nombre: Historial.test.jsx
 * Descripción: Pruebas de la pantalla Historial de lotes (RF-14): las columnas del prototipo, un lote pendiente
 *              y uno analizado, las páginas de 20 lotes, la lista vacía, con «Nuevo lote» solo para el usuario
 *              final, «Ver» en los lotes analizados, para los dos roles, que abre su detalle (C3), «Continuar» en los
 *              pendientes, solo para el usuario final (C5), el estado con su ícono, la tabla que se recorre con el
 *              teclado, y en el celular, las tarjetas y las páginas que caben desde 360 px.
 * Fecha de creación: 10/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.3
 */
import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it } from 'vitest';
import { abrirApp, conSesion, USUARIOS } from '../pruebas/ayudas.jsx';
import { conFotos, fotoCargada } from '../pruebas/fotos.js';
import { LOTE_ANALIZADO, LOTE_PENDIENTE, loteNumero, paginaDeLotes } from '../pruebas/lotes.js';
import { anchoDePantalla } from '../pruebas/preparacion.js';
import { conResultado, resultadoAnalizado } from '../pruebas/resultados.js';
import { servidor } from '../pruebas/servidor.js';

/** Las columnas del prototipo, las mismas para los dos roles: la de las acciones tiene «Ver» y «Continuar». */
const COLUMNAS = ['Fecha del resultado', 'Código', 'Finca o proveedor', 'Estado', '% de granos sanos',
  'Categoría probable', 'Registrado por', 'Acciones'];

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

  it('RF-14 · la lista trae las columnas del prototipo: un lote analizado con su resultado y «Ver», y uno pendiente sin él, con «Continuar»', async () => {
    conSesion(USUARIOS.USUARIO_FINAL);
    conHistorial(() => paginaDeLotes([LOTE_ANALIZADO, LOTE_PENDIENTE]));
    abrirApp('/historial');

    expect(await screen.findByRole('heading', { name: 'Historial de lotes' })).toBeInTheDocument();
    expect(screen.getByText('Los lotes de Cacaos del Huila, del más reciente al más antiguo.')).toBeInTheDocument();
    const tabla = await screen.findByRole('table', { name: 'Lotes de la empresa' });
    expect(within(tabla).getAllByRole('columnheader').map((titulo) => titulo.textContent)).toEqual(COLUMNAS);
    expect(filas()).toEqual([
      ['18/10/2026', 'L-0139', 'La Estrella', 'Analizado', '93,0 %', 'Estándar', 'María Torres', 'Ver'],
      ['—', 'L-0143', 'La Esperanza', 'Pendiente de análisis', '—', '—', 'Carlos Ramírez', 'Continuar'],
    ]);
  });

  it('RF-14 · C5 · en un lote pendiente, el usuario final pulsa «Continuar» y retoma la captura de fotos de ese lote, con las que ya subió', async () => {
    conSesion(USUARIOS.USUARIO_FINAL);
    conHistorial(() => paginaDeLotes([LOTE_ANALIZADO, LOTE_PENDIENTE]));
    servidor.use(http.get(`/api/v1/lotes/${LOTE_PENDIENTE.id}`, () => HttpResponse.json(LOTE_PENDIENTE)));
    conFotos(LOTE_PENDIENTE.id, [fotoCargada(1)]);
    const { usuario } = abrirApp('/historial');

    const tabla = await screen.findByRole('table', { name: 'Lotes de la empresa' });
    expect(within(tabla).getAllByRole('link', { name: /^Continuar/ })).toHaveLength(1);
    await usuario.click(within(tabla).getByRole('link', { name: 'Continuar el lote L-0143' }));

    expect(await screen.findByRole('heading', { name: 'Fotos de la muestra' })).toBeInTheDocument();
    expect(window.location.pathname).toBe(`/lotes/${LOTE_PENDIENTE.id}/fotos`);
    expect(await screen.findByRole('img', { name: 'Foto 1 de la muestra' })).toBeInTheDocument();
  });

  it('RF-14 · el estado de cada lote va con su ícono, como en el prototipo: un chulo si está analizado y un reloj si está pendiente', async () => {
    conSesion(USUARIOS.USUARIO_FINAL);
    conHistorial(() => paginaDeLotes([LOTE_ANALIZADO, LOTE_PENDIENTE]));
    abrirApp('/historial');

    const tabla = await screen.findByRole('table', { name: 'Lotes de la empresa' });
    const [, analizado, pendiente] = within(tabla).getAllByRole('row');
    expect(within(analizado).getByTestId('CheckIcon')).toBeInTheDocument();
    expect(within(pendiente).getByTestId('ScheduleIcon')).toBeInTheDocument();
  });

  it('RNF-06 · C4 · la caja de la tabla recibe el foco y lleva su nombre, para recorrerla con el teclado si no cabe en la ventana', async () => {
    conSesion(USUARIOS.USUARIO_FINAL);
    conHistorial(() => paginaDeLotes([LOTE_PENDIENTE]));
    abrirApp('/historial');

    const caja = await screen.findByRole('region', { name: 'Lotes de la empresa' });
    expect(caja).toHaveAttribute('tabindex', '0');
    expect(within(caja).getByRole('table', { name: 'Lotes de la empresa' })).toBeInTheDocument();
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

  it('RF-14 · la administradora de la empresa también ve el historial, con «Ver» en el lote analizado y el pendiente sin «Continuar»', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    conHistorial(() => paginaDeLotes([LOTE_ANALIZADO, LOTE_PENDIENTE]));
    abrirApp('/historial');

    const tabla = await screen.findByRole('table', { name: 'Lotes de la empresa' });
    expect(within(tabla).getAllByRole('columnheader').map((titulo) => titulo.textContent)).toEqual(COLUMNAS);
    expect(filas()).toEqual([
      ['18/10/2026', 'L-0139', 'La Estrella', 'Analizado', '93,0 %', 'Estándar', 'María Torres', 'Ver'],
      ['—', 'L-0143', 'La Esperanza', 'Pendiente de análisis', '—', '—', 'Carlos Ramírez', ''],
    ]);
    expect(within(tabla).getByRole('link', { name: 'Ver el lote L-0139' }))
      .toHaveAttribute('href', `/lotes/${LOTE_ANALIZADO.id}`);
    expect(screen.queryByRole('link', { name: /^Continuar/ })).not.toBeInTheDocument();
  });

  it('RF-14 · C3 · en un lote analizado, el usuario final pulsa «Ver» y abre su detalle, con las cifras guardadas y sin los pasos del análisis', async () => {
    conSesion(USUARIOS.USUARIO_FINAL);
    conHistorial(() => paginaDeLotes([LOTE_ANALIZADO, LOTE_PENDIENTE]));
    const detalle = resultadoAnalizado({
      id: LOTE_ANALIZADO.id, codigo: 'L-0139', origen: 'La Estrella', porcentajeSanos: 93.0, margen: 5.7,
    });
    conResultado(LOTE_ANALIZADO.id, detalle);
    const { usuario } = abrirApp('/historial');

    const tabla = await screen.findByRole('table', { name: 'Lotes de la empresa' });
    expect(within(tabla).getAllByRole('link', { name: /^Ver/ })).toHaveLength(1);
    await usuario.click(within(tabla).getByRole('link', { name: 'Ver el lote L-0139' }));

    expect(await screen.findByRole('heading', { level: 1, name: 'Resultado del lote L-0139' })).toBeInTheDocument();
    expect(window.location.pathname).toBe(`/lotes/${LOTE_ANALIZADO.id}`);
    expect(screen.getByText('93,0 %')).toBeInTheDocument();
    expect(screen.getByText('Rango probable: entre 87,3 y 98,7 %')).toBeInTheDocument();
    expect(screen.queryByRole('list', { name: 'Pasos del análisis' })).not.toBeInTheDocument();
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

  it('RF-14 · C5 · en el celular, «Continuar» va al pie de la tarjeta del lote pendiente, y la del analizado lleva «Ver»', async () => {
    anchoDePantalla(375);
    conSesion(USUARIOS.USUARIO_FINAL);
    conHistorial(() => paginaDeLotes([LOTE_ANALIZADO, LOTE_PENDIENTE]));
    abrirApp('/historial');

    const lista = await screen.findByRole('list', { name: 'Lotes de la empresa' });
    const [analizado, pendiente] = within(lista).getAllByRole('listitem');
    expect(within(analizado).queryByRole('link', { name: /^Continuar/ })).not.toBeInTheDocument();
    const ver = within(analizado).getByRole('link', { name: 'Ver el lote L-0139' });
    expect(ver).toHaveAttribute('href', `/lotes/${LOTE_ANALIZADO.id}`);
    expect(analizado.lastElementChild).toContainElement(ver);
    const continuar = within(pendiente).getByRole('link', { name: 'Continuar el lote L-0143' });
    expect(continuar).toHaveAttribute('href', `/lotes/${LOTE_PENDIENTE.id}/fotos`);
    expect(pendiente.lastElementChild).toContainElement(continuar);
  });

  it('RF-14 · en el celular, la tarjeta del lote pendiente de la administradora no tiene «Continuar», y la del analizado lleva «Ver»', async () => {
    anchoDePantalla(375);
    conSesion(USUARIOS.ADMIN_EMPRESA);
    conHistorial(() => paginaDeLotes([LOTE_ANALIZADO, LOTE_PENDIENTE]));
    abrirApp('/historial');

    const lista = await screen.findByRole('list', { name: 'Lotes de la empresa' });
    const [analizado, pendiente] = within(lista).getAllByRole('listitem');
    expect(pendiente).toHaveTextContent('Pendiente de análisis');
    expect(within(pendiente).queryByRole('link')).not.toBeInTheDocument();
    expect(within(analizado).getByRole('link', { name: 'Ver el lote L-0139' })).toBeInTheDocument();
  });

  it('RNF-05 · C1 · en el celular, con 8 páginas, solo se ven la primera, la última y las vecinas de la que se ve, para que quepan desde 360 px', async () => {
    anchoDePantalla(375);
    conSesion(USUARIOS.USUARIO_FINAL);
    const todos = Array.from({ length: 141 }, (_, indice) => loteNumero(141 - indice));
    conHistorial((pagina) => paginaDeLotes(todos.slice(pagina * 20, pagina * 20 + 20), { pagina, total: 141 }));
    abrirApp('/historial');

    await screen.findByRole('list', { name: 'Lotes de la empresa' });
    const paginas = screen.getByRole('navigation', { name: 'Páginas del historial' });
    expect(within(paginas).getAllByRole('button', { name: /^Página \d+$/ }).map((boton) => boton.textContent))
      .toEqual(['1', '2', '3', '8']);
    expect(within(paginas).getByRole('button', { name: 'Página 1' })).toHaveAttribute('aria-current', 'page');
  });
});
