/*
 * Nombre: Historial.test.jsx
 * Descripción: Pruebas de la pantalla Historial de lotes (RF-14): las columnas del prototipo, un lote pendiente
 *              y uno analizado, las páginas de 20 lotes, la lista vacía, con «Nuevo lote» solo para el usuario
 *              final, «Ver» en los lotes analizados, para los dos roles, que abre su detalle (C3), «Continuar» en los
 *              pendientes, solo para el usuario final (C5), el estado con su ícono y el chip «Sugiere otra muestra»
 *              (D-25), la tabla que se recorre con el teclado, y en el celular, las tarjetas y las páginas que caben
 *              desde 360 px. Los filtros (C2): cada campo del prototipo con su etiqueta, cada filtro solo y
 *              combinados, los parámetros que llegan a la API, la lista «Usuario» del servicio de quienes registraron
 *              lotes, «Hasta» antes de «Resultado desde» y el código de más de 30 caracteres, que se marcan en su campo,
 *              como los errores del backend; los filtros y la página en la dirección, «Aplicar filtros», que vuelve a
 *              la página 1, «Limpiar», la lista sin coincidencias con «Limpiar filtros», el total anunciado y, en el
 *              celular, los filtros detrás del botón «Filtros».
 * Fecha de creación: 10/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.4
 */
import { act, fireEvent, screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it } from 'vitest';
import { abrirApp, conSesion, USUARIOS } from '../pruebas/ayudas.jsx';
import { conFotos, fotoCargada } from '../pruebas/fotos.js';
import {
  conRegistradores, LOTE_ANALIZADO, LOTE_CON_SUGERENCIA, LOTE_PENDIENTE, loteNumero, paginaDeLotes, REGISTRADORES,
} from '../pruebas/lotes.js';
import { anchoDePantalla } from '../pruebas/preparacion.js';
import { conResultado, resultadoAnalizado } from '../pruebas/resultados.js';
import { problema, servidor } from '../pruebas/servidor.js';

/** Las columnas del prototipo, las mismas para los dos roles: la de las acciones tiene «Ver» y «Continuar». */
const COLUMNAS = ['Fecha del resultado', 'Código', 'Finca o proveedor', 'Estado', '% de granos sanos',
  'Categoría probable', 'Registrado por', 'Acciones'];

/** Las etiquetas de los filtros, en el orden del prototipo. */
const FILTROS = ['Resultado desde', 'Hasta', 'Estado', 'Categoría', 'Código del lote', 'Usuario'];

/** El mensaje de «Hasta» anterior a «Resultado desde», el mismo del backend (lote.filtro.hasta_antes_de_desde). */
const HASTA_ANTES = 'La fecha «Hasta» no puede ser anterior a «Resultado desde».';

/** La ayuda bajo las fechas: un lote pendiente no sale al filtrar por fecha. */
const AYUDA_DE_LAS_FECHAS = 'Los lotes pendientes todavía no tienen fecha del resultado.';

/** Lo que dice la lista cuando ningún lote cumple los filtros. */
const SIN_COINCIDENCIAS = 'Ningún lote cumple los filtros.';

/** Los identificadores de María Torres, que registró el lote analizado, y de Luis Gómez, que está desactivado. */
const MARIA = LOTE_ANALIZADO.usuario.id;
const LUIS = REGISTRADORES[1].id;

/**
 * Responde las páginas del historial y quienes registraron lotes, para el filtro «Usuario», y anota qué se pidió.
 *
 * @param {(pagina: number, filtros: Record<string, string>) => object} pagina la página que responde según su número
 *   y todos los parámetros de la consulta
 * @returns {{ pedidas: string[], consultas: Record<string, string>[], registradores: { veces: number } }} las
 *   consultas que llegaron, como pagina=0&tamano=20 y como objeto, y cuántas veces se pidió la lista «Usuario»
 */
function conHistorial(pagina) {
  const pedidas = [];
  const consultas = [];
  const registradores = conRegistradores();
  servidor.use(http.get('/api/v1/lotes', ({ request }) => {
    const parametros = new URL(request.url).searchParams;
    pedidas.push(parametros.toString());
    consultas.push(Object.fromEntries(parametros));
    return HttpResponse.json(pagina(Number(parametros.get('pagina') ?? 0), Object.fromEntries(parametros)));
  }));
  return { pedidas, consultas, registradores };
}

/**
 * El formulario de los filtros.
 *
 * @returns {HTMLElement} el formulario
 */
function filtros() {
  return screen.getByRole('form', { name: 'Filtros del historial' });
}

/**
 * Un campo de los filtros, por su etiqueta.
 *
 * @param {string} etiqueta la etiqueta, como «Estado»
 * @returns {HTMLInputElement|HTMLSelectElement} el campo
 */
function campo(etiqueta) {
  return within(filtros()).getByLabelText(etiqueta);
}

/**
 * Las opciones de una lista de los filtros.
 *
 * @param {string} etiqueta la etiqueta de la lista
 * @returns {string[][]} el valor y el texto de cada opción, en orden
 */
function opciones(etiqueta) {
  return Array.from(campo(etiqueta).options).map((opcion) => [opcion.value, opcion.textContent]);
}

/**
 * Escribe una fecha en un campo de los filtros, como la deja el selector de fecha del navegador.
 *
 * @param {string} etiqueta la etiqueta del campo
 * @param {string} valor la fecha (aaaa-mm-dd)
 */
function escribirFecha(etiqueta, valor) {
  fireEvent.change(campo(etiqueta), { target: { value: valor } });
}

/**
 * Pulsa un botón de los filtros.
 *
 * @param {import('@testing-library/user-event').UserEvent} usuario quien usa la pantalla
 * @param {string} nombre el botón: «Aplicar filtros» o «Limpiar»
 */
async function pulsar(usuario, nombre) {
  await usuario.click(within(filtros()).getByRole('button', { name: nombre }));
}

/**
 * Los parámetros de la dirección de la página.
 *
 * @returns {Record<string, string>} los parámetros
 */
function direccion() {
  return Object.fromEntries(new URLSearchParams(window.location.search));
}

/**
 * Lo que dicen los role="status" de la página, que el lector de pantalla anuncia cuando cambian.
 *
 * @returns {string} sus textos, separados por « | »
 */
function anunciado() {
  return screen.queryAllByRole('status').map((region) => region.textContent).join(' | ');
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
      sugerirOtraMuestra: false,
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

  it('RF-14 · un lote que sugiere otra muestra lleva el chip «Sugiere otra muestra» junto a su estado, como en el prototipo; los demás no', async () => {
    conSesion(USUARIOS.USUARIO_FINAL);
    conHistorial(() => paginaDeLotes([LOTE_CON_SUGERENCIA, LOTE_ANALIZADO, LOTE_PENDIENTE]));
    abrirApp('/historial');

    const tabla = await screen.findByRole('table', { name: 'Lotes de la empresa' });
    const [, conSugerencia, analizado, pendiente] = within(tabla).getAllByRole('row');
    const estado = within(conSugerencia).getAllByRole('cell')[3];
    expect(within(estado).getByText('Analizado')).toBeInTheDocument();
    expect(within(estado).getByText('Sugiere otra muestra')).toBeInTheDocument();
    expect(within(conSugerencia).getByRole('link', { name: 'Ver el lote L-0141' }))
      .toHaveAttribute('href', `/lotes/${LOTE_CON_SUGERENCIA.id}`);
    expect(within(analizado).queryByText('Sugiere otra muestra')).not.toBeInTheDocument();
    expect(within(pendiente).queryByText('Sugiere otra muestra')).not.toBeInTheDocument();
  });

  it('RF-14 · en el celular, la tarjeta del lote que sugiere otra muestra lleva el chip junto a su estado, también para la administradora', async () => {
    anchoDePantalla(375);
    conSesion(USUARIOS.ADMIN_EMPRESA);
    conHistorial(() => paginaDeLotes([LOTE_CON_SUGERENCIA, LOTE_ANALIZADO]));
    abrirApp('/historial');

    const lista = await screen.findByRole('list', { name: 'Lotes de la empresa' });
    const [conSugerencia, analizado] = within(lista).getAllByRole('listitem');
    expect(within(conSugerencia).getByText('Sugiere otra muestra')).toBeInTheDocument();
    expect(within(conSugerencia).getByText('Analizado')).toBeInTheDocument();
    expect(within(analizado).queryByText('Sugiere otra muestra')).not.toBeInTheDocument();
  });

  describe('filtros', () => {
    it('RF-14 · los filtros van en el formulario «Filtros del historial», antes de la lista, con los campos del prototipo en su orden, «Todos» o «Todas» en cada lista y la ayuda de las fechas', async () => {
      conSesion(USUARIOS.USUARIO_FINAL);
      conHistorial(() => paginaDeLotes([LOTE_ANALIZADO, LOTE_PENDIENTE]));
      abrirApp('/historial');

      const formulario = await screen.findByRole('form', { name: 'Filtros del historial' });
      expect(Array.from(formulario.querySelectorAll('label')).map((etiqueta) => etiqueta.textContent)).toEqual(FILTROS);
      expect(campo('Resultado desde')).toHaveAttribute('type', 'date');
      expect(campo('Hasta')).toHaveAttribute('type', 'date');
      expect(within(formulario).getByText(AYUDA_DE_LAS_FECHAS)).toBeInTheDocument();
      expect(opciones('Estado')).toEqual([['', 'Todos'], ['PENDIENTE', 'Pendiente de análisis'], ['ANALIZADO', 'Analizado']]);
      expect(opciones('Categoría')).toEqual([['', 'Todas'], ['PREMIUM', 'Premium o Especial'], ['ESTANDAR', 'Estándar'],
        ['CORRIENTE', 'Corriente'], ['FUERA_DE_NORMA', 'Fuera de norma']]);
      expect(within(formulario).getByRole('searchbox', { name: 'Código del lote' })).toHaveAttribute('placeholder', 'L-0142');
      await waitFor(() => expect(opciones('Usuario')).toEqual([['', 'Todos'],
        ...REGISTRADORES.map(({ id, nombre }) => [id, nombre])]));
      expect(within(formulario).getByRole('button', { name: 'Aplicar filtros' })).toHaveAttribute('type', 'submit');
      expect(within(formulario).getByRole('button', { name: 'Limpiar' })).toBeInTheDocument();
      // En el PC el formulario se ve siempre: no va detrás de un botón.
      expect(screen.queryByRole('button', { name: 'Filtros' })).not.toBeInTheDocument();
      expect(formulario.compareDocumentPosition(screen.getByRole('table', { name: 'Lotes de la empresa' })))
        .toBe(Node.DOCUMENT_POSITION_FOLLOWING);
    });

    it('RF-14 · C2 · cada filtro funciona solo: la fecha del resultado desde y hasta, el estado, la categoría, el código y el usuario piden la lista con ese filtro y nada más, desde la primera página', async () => {
      conSesion(USUARIOS.USUARIO_FINAL);
      const { consultas } = conHistorial(() => paginaDeLotes([LOTE_ANALIZADO]));
      const { usuario } = abrirApp('/historial');
      await screen.findByRole('table', { name: 'Lotes de la empresa' });
      await within(filtros()).findByRole('option', { name: 'María Torres' });
      const casos = [
        [() => escribirFecha('Resultado desde', '2026-10-01'), { desde: '2026-10-01' }],
        [() => escribirFecha('Hasta', '2026-10-20'), { hasta: '2026-10-20' }],
        [() => usuario.selectOptions(campo('Estado'), 'Pendiente de análisis'), { estado: 'PENDIENTE' }],
        [() => usuario.selectOptions(campo('Categoría'), 'Corriente'), { categoria: 'CORRIENTE' }],
        [() => usuario.type(campo('Código del lote'), 'L-01'), { codigo: 'L-01' }],
        [() => usuario.selectOptions(campo('Usuario'), 'María Torres'), { usuario: MARIA }],
      ];

      for (const [elegir, filtro] of casos) {
        await pulsar(usuario, 'Limpiar');
        await elegir();
        await pulsar(usuario, 'Aplicar filtros');
        await waitFor(() => expect(consultas.at(-1)).toEqual({ pagina: '0', tamano: '20', ...filtro }));
      }
    });

    it('RF-14 · C2 · los filtros se combinan: con los seis puestos, la lista se pide con todos, muestra lo que responde el backend y la dirección los guarda', async () => {
      conSesion(USUARIOS.USUARIO_FINAL);
      const { consultas } = conHistorial((pagina, consulta) => paginaDeLotes(consulta.estado
        ? [LOTE_ANALIZADO] : [LOTE_ANALIZADO, LOTE_PENDIENTE]));
      const { usuario } = abrirApp('/historial');
      await screen.findByRole('table', { name: 'Lotes de la empresa' });
      await within(filtros()).findByRole('option', { name: 'María Torres' });

      escribirFecha('Resultado desde', '2026-10-01');
      escribirFecha('Hasta', '2026-10-20');
      await usuario.selectOptions(campo('Estado'), 'Analizado');
      await usuario.selectOptions(campo('Categoría'), 'Estándar');
      await usuario.type(campo('Código del lote'), 'L-01');
      await usuario.selectOptions(campo('Usuario'), 'María Torres');
      await pulsar(usuario, 'Aplicar filtros');

      const combinados = {
        desde: '2026-10-01', hasta: '2026-10-20', estado: 'ANALIZADO', categoria: 'ESTANDAR', codigo: 'L-01', usuario: MARIA,
      };
      await waitFor(() => expect(consultas.at(-1)).toEqual({ pagina: '0', tamano: '20', ...combinados }));
      await waitFor(() => expect(filas().map((fila) => fila[1])).toEqual(['L-0139']));
      expect(direccion()).toMatchObject(combinados);
    });

    it('RF-14 · C2 · la lista «Usuario» trae a quienes registraron lotes de la empresa, como los responde su servicio, y filtrar por uno pide solo sus lotes', async () => {
      conSesion(USUARIOS.USUARIO_FINAL);
      const deLuis = { ...loteNumero(7), usuario: { id: LUIS, nombre: 'Luis Gómez' } };
      const { consultas, registradores } = conHistorial((pagina, consulta) => paginaDeLotes(consulta.usuario === LUIS
        ? [deLuis] : [LOTE_ANALIZADO, LOTE_PENDIENTE]));
      const { usuario } = abrirApp('/historial');
      await screen.findByRole('table', { name: 'Lotes de la empresa' });

      await within(filtros()).findByRole('option', { name: 'Luis Gómez' });
      expect(registradores.veces).toBeGreaterThan(0);
      await usuario.selectOptions(campo('Usuario'), 'Luis Gómez');
      await pulsar(usuario, 'Aplicar filtros');

      await waitFor(() => expect(consultas.at(-1)).toEqual({ pagina: '0', tamano: '20', usuario: LUIS }));
      await waitFor(() => expect(filas()).toEqual([
        ['—', 'L-0007', 'La Esperanza', 'Pendiente de análisis', '—', '—', 'Luis Gómez', 'Continuar'],
      ]));
    });

    it('RF-14 · C2 · la administradora también filtra: por quién registró el lote y por su estado, a la vez', async () => {
      conSesion(USUARIOS.ADMIN_EMPRESA);
      const { consultas } = conHistorial((pagina, consulta) => paginaDeLotes(consulta.usuario
        ? [LOTE_ANALIZADO] : [LOTE_ANALIZADO, LOTE_PENDIENTE]));
      const { usuario } = abrirApp('/historial');
      await screen.findByRole('table', { name: 'Lotes de la empresa' });
      await within(filtros()).findByRole('option', { name: 'María Torres' });

      await usuario.selectOptions(campo('Usuario'), 'María Torres');
      await usuario.selectOptions(campo('Estado'), 'Analizado');
      await pulsar(usuario, 'Aplicar filtros');

      await waitFor(() => expect(consultas.at(-1)).toEqual({ pagina: '0', tamano: '20', usuario: MARIA, estado: 'ANALIZADO' }));
      await waitFor(() => expect(filas().map((fila) => fila[1])).toEqual(['L-0139']));
    });

    it('RF-14 · C2 · si «Hasta» es anterior a «Resultado desde», «Hasta» se marca con el mensaje y la lista no se pide con esas fechas; el mismo día en los dos sí se busca (prueba de límite)', async () => {
      conSesion(USUARIOS.USUARIO_FINAL);
      const { consultas } = conHistorial(() => paginaDeLotes([LOTE_ANALIZADO]));
      const { usuario } = abrirApp('/historial');
      await screen.findByRole('table', { name: 'Lotes de la empresa' });

      escribirFecha('Resultado desde', '2026-10-20');
      escribirFecha('Hasta', '2026-10-19');
      await pulsar(usuario, 'Aplicar filtros');

      expect(await within(filtros()).findByText(HASTA_ANTES)).toBeInTheDocument();
      expect(campo('Hasta')).toHaveAttribute('aria-invalid', 'true');
      expect(campo('Hasta')).toHaveAccessibleDescription(expect.stringContaining(HASTA_ANTES));
      expect(campo('Resultado desde')).not.toHaveAttribute('aria-invalid', 'true');
      expect(consultas.some((consulta) => 'desde' in consulta || 'hasta' in consulta)).toBe(false);
      expect(direccion()).toEqual({});

      escribirFecha('Hasta', '2026-10-20');
      await pulsar(usuario, 'Aplicar filtros');

      await waitFor(() => expect(consultas.at(-1))
        .toEqual({ pagina: '0', tamano: '20', desde: '2026-10-20', hasta: '2026-10-20' }));
      expect(within(filtros()).queryByText(HASTA_ANTES)).not.toBeInTheDocument();
    });

    it('RF-14 · C2 · el código del lote admite hasta 30 caracteres: con 31 se marca antes de pedir la lista, y con 30 se busca (prueba de límite)', async () => {
      conSesion(USUARIOS.USUARIO_FINAL);
      const { consultas } = conHistorial(() => paginaDeLotes([LOTE_ANALIZADO]));
      const { usuario } = abrirApp('/historial');
      await screen.findByRole('table', { name: 'Lotes de la empresa' });

      fireEvent.change(campo('Código del lote'), { target: { value: 'L'.repeat(31) } });
      await pulsar(usuario, 'Aplicar filtros');

      expect(await within(filtros()).findByText('Puede tener como máximo 30 caracteres.')).toBeInTheDocument();
      expect(campo('Código del lote')).toHaveAttribute('aria-invalid', 'true');
      expect(consultas.some((consulta) => 'codigo' in consulta)).toBe(false);

      fireEvent.change(campo('Código del lote'), { target: { value: 'L'.repeat(30) } });
      await pulsar(usuario, 'Aplicar filtros');

      await waitFor(() => expect(consultas.at(-1)).toEqual({ pagina: '0', tamano: '20', codigo: 'L'.repeat(30) }));
    });

    it('RF-14 · si el backend rechaza un filtro, su mensaje queda en ese campo', async () => {
      conSesion(USUARIOS.USUARIO_FINAL);
      conHistorial(() => paginaDeLotes([LOTE_ANALIZADO]));
      // Con el código, el backend responde 400 con el error del campo; sin él, sigue el historial de arriba.
      servidor.use(http.get('/api/v1/lotes', ({ request }) => (new URL(request.url).searchParams.has('codigo')
        ? problema(400, 'Revisa los datos marcados.', { errores: [{ campo: 'codigo', mensaje: 'El valor no es válido.' }] })
        : undefined)));
      const { usuario } = abrirApp('/historial');
      await screen.findByRole('table', { name: 'Lotes de la empresa' });

      await usuario.type(campo('Código del lote'), 'L-01');
      await pulsar(usuario, 'Aplicar filtros');

      expect(await within(filtros()).findByText('El valor no es válido.')).toBeInTheDocument();
      expect(campo('Código del lote')).toHaveAttribute('aria-invalid', 'true');
      expect(campo('Código del lote')).toHaveAccessibleDescription(expect.stringContaining('El valor no es válido.'));
    });

    it('RF-14 · los filtros y la página quedan en la dirección: al abrirla, la lista se pide con ellos desde la página que dice, y el formulario los muestra', async () => {
      conSesion(USUARIOS.USUARIO_FINAL);
      const todos = Array.from({ length: 25 }, (_, indice) => loteNumero(25 - indice));
      const { consultas } = conHistorial((pagina) => paginaDeLotes(todos.slice(pagina * 20, pagina * 20 + 20),
        { pagina, total: 25 }));
      abrirApp(`/historial?desde=2026-10-01&hasta=2026-10-20&estado=ANALIZADO&categoria=ESTANDAR&codigo=L-01&usuario=${MARIA}&pagina=2`);

      await screen.findByRole('table', { name: 'Lotes de la empresa' });
      expect(consultas[0]).toEqual({
        pagina: '1', tamano: '20', desde: '2026-10-01', hasta: '2026-10-20', estado: 'ANALIZADO', categoria: 'ESTANDAR',
        codigo: 'L-01', usuario: MARIA,
      });
      expect(campo('Resultado desde')).toHaveValue('2026-10-01');
      expect(campo('Hasta')).toHaveValue('2026-10-20');
      expect(campo('Estado')).toHaveValue('ANALIZADO');
      expect(campo('Categoría')).toHaveValue('ESTANDAR');
      expect(campo('Código del lote')).toHaveValue('L-01');
      await waitFor(() => expect(campo('Usuario')).toHaveValue(MARIA));
      const paginas = screen.getByRole('navigation', { name: 'Páginas del historial' });
      expect(within(paginas).getByRole('button', { name: 'Página 2' })).toHaveAttribute('aria-current', 'page');
    });

    it('RF-14 · al pasar de página, la dirección guarda la página nueva sin perder los filtros', async () => {
      conSesion(USUARIOS.USUARIO_FINAL);
      const todos = Array.from({ length: 25 }, (_, indice) => loteNumero(25 - indice));
      const { consultas } = conHistorial((pagina) => paginaDeLotes(todos.slice(pagina * 20, pagina * 20 + 20),
        { pagina, total: 25 }));
      const { usuario } = abrirApp('/historial?estado=PENDIENTE');
      await screen.findByRole('table', { name: 'Lotes de la empresa' });

      await usuario.click(within(screen.getByRole('navigation', { name: 'Páginas del historial' }))
        .getByRole('button', { name: 'Página siguiente' }));

      await waitFor(() => expect(consultas.at(-1)).toEqual({ pagina: '1', tamano: '20', estado: 'PENDIENTE' }));
      expect(direccion()).toEqual({ estado: 'PENDIENTE', pagina: '2' });
      expect(campo('Estado')).toHaveValue('PENDIENTE');
    });

    it('RF-14 · «Aplicar filtros» vuelve a la página 1', async () => {
      conSesion(USUARIOS.USUARIO_FINAL);
      const todos = Array.from({ length: 25 }, (_, indice) => loteNumero(25 - indice));
      const { consultas } = conHistorial((pagina) => paginaDeLotes(todos.slice(pagina * 20, pagina * 20 + 20),
        { pagina, total: 25 }));
      const { usuario } = abrirApp('/historial?pagina=2');
      await screen.findByRole('table', { name: 'Lotes de la empresa' });
      expect(consultas[0]).toEqual({ pagina: '1', tamano: '20' });

      await usuario.selectOptions(campo('Estado'), 'Pendiente de análisis');
      await pulsar(usuario, 'Aplicar filtros');

      await waitFor(() => expect(consultas.at(-1)).toEqual({ pagina: '0', tamano: '20', estado: 'PENDIENTE' }));
      expect(direccion().estado).toBe('PENDIENTE');
      expect(direccion().pagina ?? '1').toBe('1');
      const paginas = screen.getByRole('navigation', { name: 'Páginas del historial' });
      await waitFor(() => expect(within(paginas).getByRole('button', { name: 'Página 1' }))
        .toHaveAttribute('aria-current', 'page'));
    });

    it('RF-14 · «Limpiar» vacía los campos y la dirección, y pide la lista sin filtros desde la página 1', async () => {
      conSesion(USUARIOS.USUARIO_FINAL);
      const todos = Array.from({ length: 25 }, (_, indice) => loteNumero(25 - indice));
      const { consultas } = conHistorial((pagina) => paginaDeLotes(todos.slice(pagina * 20, pagina * 20 + 20),
        { pagina, total: 25 }));
      const { usuario } = abrirApp('/historial?desde=2026-10-01&estado=PENDIENTE&codigo=L-00&pagina=2');
      await screen.findByRole('table', { name: 'Lotes de la empresa' });

      await pulsar(usuario, 'Limpiar');

      await waitFor(() => expect(consultas.at(-1)).toEqual({ pagina: '0', tamano: '20' }));
      expect(window.location.search).toBe('');
      expect(campo('Resultado desde')).toHaveValue('');
      expect(campo('Estado')).toHaveValue('');
      expect(campo('Código del lote')).toHaveValue('');
    });

    it('RF-14 · los valores de la dirección que no existen se ignoran: la lista se pide sin ellos y desde la página 1', async () => {
      conSesion(USUARIOS.USUARIO_FINAL);
      const { consultas } = conHistorial(() => paginaDeLotes([LOTE_ANALIZADO, LOTE_PENDIENTE]));
      abrirApp('/historial?estado=BORRADO&categoria=EXTRA&desde=ayer&hasta=2026-99-99&pagina=cero');

      await screen.findByRole('table', { name: 'Lotes de la empresa' });
      expect(consultas[0]).toEqual({ pagina: '0', tamano: '20' });
      expect(campo('Estado')).toHaveValue('');
      expect(campo('Categoría')).toHaveValue('');
    });

    it('RF-14 · si ningún lote cumple los filtros, la lista lo dice y ofrece «Limpiar filtros» en lugar de «Nuevo lote»; los filtros siguen a la vista', async () => {
      conSesion(USUARIOS.USUARIO_FINAL);
      const { consultas } = conHistorial((pagina, consulta) => paginaDeLotes(consulta.codigo ? [] : [LOTE_ANALIZADO]));
      const { usuario } = abrirApp('/historial?codigo=X-99');

      expect(await screen.findByText(SIN_COINCIDENCIAS)).toBeInTheDocument();
      expect(screen.queryByText('Tu empresa todavía no tiene lotes.')).not.toBeInTheDocument();
      expect(within(screen.getByRole('main')).queryByRole('link', { name: 'Nuevo lote' })).not.toBeInTheDocument();
      expect(campo('Código del lote')).toHaveValue('X-99');
      await usuario.click(screen.getByRole('button', { name: 'Limpiar filtros' }));

      expect(await screen.findByRole('table', { name: 'Lotes de la empresa' })).toBeInTheDocument();
      expect(filas().map((fila) => fila[1])).toEqual(['L-0139']);
      expect(consultas.at(-1)).toEqual({ pagina: '0', tamano: '20' });
      expect(window.location.search).toBe('');
      expect(campo('Código del lote')).toHaveValue('');
      expect(screen.queryByText(SIN_COINCIDENCIAS)).not.toBeInTheDocument();
    });

    it('RF-14 · el total de lotes se anuncia en un role="status": al aplicar un filtro, el lector de pantalla dice cuántos quedan', async () => {
      conSesion(USUARIOS.USUARIO_FINAL);
      const todos = Array.from({ length: 25 }, (_, indice) => loteNumero(25 - indice));
      conHistorial((pagina, consulta) => (consulta.estado === 'ANALIZADO'
        ? paginaDeLotes([LOTE_ANALIZADO])
        : paginaDeLotes(todos.slice(pagina * 20, pagina * 20 + 20), { pagina, total: 25 })));
      const { usuario } = abrirApp('/historial');
      await screen.findByRole('table', { name: 'Lotes de la empresa' });
      await waitFor(() => expect(anunciado()).toMatch(/(^|\D)25 lotes\b/));

      await usuario.selectOptions(campo('Estado'), 'Analizado');
      await pulsar(usuario, 'Aplicar filtros');

      await waitFor(() => expect(anunciado()).toMatch(/(^|\D)1 lote\b/));
    });

    it('RF-14 · al volver con «Atrás» desde «Ver», la lista sigue con sus filtros', async () => {
      conSesion(USUARIOS.USUARIO_FINAL);
      const { consultas } = conHistorial(() => paginaDeLotes([LOTE_ANALIZADO]));
      conResultado(LOTE_ANALIZADO.id, resultadoAnalizado({
        id: LOTE_ANALIZADO.id, codigo: 'L-0139', origen: 'La Estrella', porcentajeSanos: 93.0, margen: 5.7,
        sugerirOtraMuestra: false,
      }));
      const { usuario } = abrirApp('/historial');
      await screen.findByRole('table', { name: 'Lotes de la empresa' });
      await usuario.selectOptions(campo('Estado'), 'Analizado');
      await usuario.selectOptions(campo('Categoría'), 'Estándar');
      await pulsar(usuario, 'Aplicar filtros');
      const filtrada = { pagina: '0', tamano: '20', estado: 'ANALIZADO', categoria: 'ESTANDAR' };
      await waitFor(() => expect(consultas.at(-1)).toEqual(filtrada));

      await usuario.click(within(screen.getByRole('table', { name: 'Lotes de la empresa' }))
        .getByRole('link', { name: 'Ver el lote L-0139' }));
      expect(await screen.findByRole('heading', { level: 1, name: 'Resultado del lote L-0139' })).toBeInTheDocument();
      await act(async () => {
        window.history.back();
      });

      expect(await screen.findByRole('heading', { name: 'Historial de lotes' })).toBeInTheDocument();
      await waitFor(() => expect(campo('Estado')).toHaveValue('ANALIZADO'));
      expect(campo('Categoría')).toHaveValue('ESTANDAR');
      await waitFor(() => expect(consultas.at(-1)).toEqual(filtrada));
    });

    it('RNF-05 · en el celular, los filtros van detrás del botón «Filtros», cerrado si no hay ninguno puesto, que dice si están abiertos y qué abre', async () => {
      anchoDePantalla(375);
      conSesion(USUARIOS.USUARIO_FINAL);
      const { consultas } = conHistorial(() => paginaDeLotes([LOTE_ANALIZADO, LOTE_PENDIENTE]));
      const { usuario } = abrirApp('/historial');
      await screen.findByRole('list', { name: 'Lotes de la empresa' });

      const boton = screen.getByRole('button', { name: 'Filtros' });
      expect(boton).toHaveAttribute('aria-expanded', 'false');
      expect(screen.queryByRole('form', { name: 'Filtros del historial' })).not.toBeInTheDocument();
      await usuario.click(boton);

      expect(boton).toHaveAttribute('aria-expanded', 'true');
      const formulario = await screen.findByRole('form', { name: 'Filtros del historial' });
      expect(boton).toHaveAttribute('aria-controls');
      expect(document.getElementById(boton.getAttribute('aria-controls'))).toContainElement(formulario);
      await usuario.selectOptions(campo('Estado'), 'Analizado');
      await pulsar(usuario, 'Aplicar filtros');
      await waitFor(() => expect(consultas.at(-1)).toEqual({ pagina: '0', tamano: '20', estado: 'ANALIZADO' }));
    });

    it('RNF-05 · en el celular, con un filtro puesto en la dirección, los filtros empiezan abiertos y lo muestran', async () => {
      anchoDePantalla(375);
      conSesion(USUARIOS.USUARIO_FINAL);
      conHistorial(() => paginaDeLotes([LOTE_ANALIZADO]));
      abrirApp('/historial?categoria=ESTANDAR');
      await screen.findByRole('list', { name: 'Lotes de la empresa' });

      expect(screen.getByRole('button', { name: 'Filtros' })).toHaveAttribute('aria-expanded', 'true');
      expect(campo('Categoría')).toHaveValue('ESTANDAR');
    });
  });
});
