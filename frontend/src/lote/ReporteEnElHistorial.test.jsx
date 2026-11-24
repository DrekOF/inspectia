/*
 * Nombre: ReporteEnElHistorial.test.jsx
 * Descripción: Pruebas de la descarga del reporte PDF desde el historial (RF-15: «Desde el resultado o desde el
 *              historial…»), como en el prototipo: el ícono de descargar en cada lote analizado, en la fila de la tabla
 *              del PC y en la tarjeta del celular, con su nombre por lote; el reporte se pide con la sesión en el
 *              idioma de la página, para los dos roles; mientras baja el ícono está ocupado y no pide otro; al
 *              terminar se anuncia, y si falla, la página lo dice con el motivo. Un lote pendiente no lleva el ícono.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import i18n from '../i18n/i18n.js';
import { abrirApp, conSesion, USUARIOS } from '../pruebas/ayudas.jsx';
import { conRegistradores, LOTE_ANALIZADO, LOTE_PENDIENTE, paginaDeLotes } from '../pruebas/lotes.js';
import { anchoDePantalla } from '../pruebas/preparacion.js';
import { problema, servidor } from '../pruebas/servidor.js';

/** El nombre accesible del ícono del lote analizado de las pruebas. */
const DESCARGAR = 'Descargar el PDF del lote L-0139';

/** Responde el historial con un lote analizado y uno pendiente, y la lista «Usuario» de los filtros. */
function conHistorial() {
  conRegistradores();
  servidor.use(http.get('/api/v1/lotes', () => HttpResponse.json(paginaDeLotes([LOTE_ANALIZADO, LOTE_PENDIENTE]))));
}

/**
 * Responde el reporte del lote analizado como el backend: el PDF con su nombre en Content-Disposition. Anota cada
 * petición.
 *
 * @param {{ espera?: Promise<unknown> }} [opciones] lo que espera la respuesta antes de salir
 * @returns {{ peticiones: Request[] }} las peticiones que llegaron
 */
function conReporte({ espera } = {}) {
  const anotado = { peticiones: [] };
  servidor.use(http.get(`/api/v1/lotes/${LOTE_ANALIZADO.id}/reporte`, async ({ request }) => {
    anotado.peticiones.push(request);
    await espera;
    const idioma = new URL(request.url).searchParams.get('idioma');
    const nombre = `InspectIA-L-0139-${idioma}.pdf`;
    return new HttpResponse('%PDF-1.7 reporte', {
      headers: {
        'Content-Type': 'application/pdf',
        'Content-Disposition': `attachment; filename="${nombre}"; filename*=UTF-8''${nombre}`,
      },
    });
  }));
  return anotado;
}

/**
 * Las filas de la tabla del historial, sin la de los títulos.
 *
 * @param {string} [nombre] el nombre de la tabla, el de la página en español si no se dice
 * @returns {Promise<HTMLElement[]>} las filas
 */
async function filasDeLaTabla(nombre = 'Lotes de la empresa') {
  const tabla = await screen.findByRole('table', { name: nombre });
  return within(tabla).getAllByRole('row').slice(1);
}

describe('Descargar el reporte PDF desde el historial', () => {
  let guardados;

  beforeEach(() => {
    guardados = [];
    URL.createObjectURL = vi.fn(() => 'blob:reporte');
    URL.revokeObjectURL = vi.fn();
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function guardar() {
      guardados.push({ nombre: this.download, url: this.href });
    });
  });

  afterEach(async () => {
    vi.restoreAllMocks();
    anchoDePantalla(1280);
    await i18n.changeLanguage('es');
  });

  it('RF-15 · en el PC, la fila del lote analizado lleva el ícono «Descargar el PDF del lote L-0139» junto a «Ver», como en el prototipo, y el reporte se pide con la sesión y se guarda; la del pendiente no lo lleva', async () => {
    conSesion(USUARIOS.USUARIO_FINAL);
    conHistorial();
    const reporte = conReporte();
    const { usuario } = abrirApp('/historial');

    const [analizado, pendiente] = await filasDeLaTabla();
    const boton = within(analizado).getByRole('button', { name: DESCARGAR });
    const ver = within(analizado).getByRole('link', { name: 'Ver el lote L-0139' });
    expect(ver.compareDocumentPosition(boton)).toBe(Node.DOCUMENT_POSITION_FOLLOWING);
    expect(within(pendiente).queryByRole('button', { name: /^Descargar el PDF/ })).not.toBeInTheDocument();
    await usuario.click(boton);

    await waitFor(() => expect(guardados).toEqual([{ nombre: 'InspectIA-L-0139-es.pdf', url: 'blob:reporte' }]));
    expect(reporte.peticiones).toHaveLength(1);
    expect(new URL(reporte.peticiones[0].url).searchParams.get('idioma')).toBe('es');
    expect(reporte.peticiones[0].headers.get('Authorization')).toBe('Bearer token-de-prueba');
    expect(await screen.findByText('Se descargó el PDF del lote L-0139.')).toBeInTheDocument();
  });

  it('RF-15 · la administradora también descarga el PDF desde el historial, y con la página en inglés sale en inglés', async () => {
    await i18n.changeLanguage('en');
    conSesion(USUARIOS.ADMIN_EMPRESA);
    conHistorial();
    const reporte = conReporte();
    const { usuario } = abrirApp('/historial');

    const [analizado] = await filasDeLaTabla('Company lots');
    await usuario.click(within(analizado).getByRole('button', { name: 'Download the PDF of lot L-0139' }));

    await waitFor(() => expect(guardados.map(({ nombre }) => nombre)).toEqual(['InspectIA-L-0139-en.pdf']));
    expect(new URL(reporte.peticiones[0].url).searchParams.get('idioma')).toBe('en');
  });

  it('RF-15 · mientras baja, el ícono está ocupado y no pide otro', async () => {
    conSesion(USUARIOS.USUARIO_FINAL);
    conHistorial();
    let soltar;
    const reporte = conReporte({ espera: new Promise((resolver) => { soltar = resolver; }) });
    const { usuario } = abrirApp('/historial');

    const [analizado] = await filasDeLaTabla();
    const boton = within(analizado).getByRole('button', { name: DESCARGAR });
    await usuario.click(boton);
    await waitFor(() => expect(boton).toHaveAttribute('aria-busy', 'true'));
    expect(boton).toHaveAttribute('aria-disabled', 'true');
    expect(await screen.findByText('Descargando el PDF…')).toBeInTheDocument();
    await usuario.click(boton);
    soltar();

    await waitFor(() => expect(boton).not.toHaveAttribute('aria-busy'));
    expect(reporte.peticiones).toHaveLength(1);
    expect(guardados).toHaveLength(1);
  });

  it('RNF-06 · C3 · si la descarga desde el historial falla, la página dice que no se pudo, con el motivo del backend', async () => {
    conSesion(USUARIOS.USUARIO_FINAL);
    conHistorial();
    servidor.use(http.get(`/api/v1/lotes/${LOTE_ANALIZADO.id}/reporte`, () => problema(404,
      'No encontramos ese lote.')));
    const { usuario } = abrirApp('/historial');

    const [analizado] = await filasDeLaTabla();
    await usuario.click(within(analizado).getByRole('button', { name: DESCARGAR }));

    const alerta = await screen.findByRole('alert');
    expect(alerta).toHaveTextContent('No se pudo descargar el PDF del lote L-0139.');
    expect(alerta).toHaveTextContent('No encontramos ese lote.');
    expect(guardados).toHaveLength(0);
    conReporte();
    await usuario.click(within(analizado).getByRole('button', { name: DESCARGAR }));
    await waitFor(() => expect(guardados).toHaveLength(1));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('RF-15 · en el celular, la tarjeta del lote analizado lleva el ícono de descargar el PDF al pie, junto a «Ver», de 44 px (RNF-05); la del pendiente no', async () => {
    anchoDePantalla(375);
    conSesion(USUARIOS.USUARIO_FINAL);
    conHistorial();
    conReporte();
    const { usuario } = abrirApp('/historial');

    const lista = await screen.findByRole('list', { name: 'Lotes de la empresa' });
    const [analizado, pendiente] = within(lista).getAllByRole('listitem');
    const boton = within(analizado).getByRole('button', { name: DESCARGAR });
    expect(analizado.lastElementChild).toContainElement(boton);
    expect(analizado.lastElementChild).toContainElement(within(analizado).getByRole('link', { name: 'Ver el lote L-0139' }));
    expect(boton).toHaveStyle({ minWidth: '44px', minHeight: '44px' });
    expect(within(pendiente).queryByRole('button', { name: /^Descargar el PDF/ })).not.toBeInTheDocument();
    await usuario.click(boton);

    await waitFor(() => expect(guardados.map(({ nombre }) => nombre)).toEqual(['InspectIA-L-0139-es.pdf']));
  });
});
