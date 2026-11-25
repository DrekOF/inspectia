/*
 * Nombre: DescargarReporte.test.jsx
 * Descripción: Pruebas de «Descargar PDF» en el resultado del lote (RF-15): el botón para los dos roles junto al título
 *              en el PC y al final en el celular, la descarga con la sesión en el idioma de la página, el nombre del
 *              archivo, el estado mientras baja, el anuncio al terminar y el error; y un lote pendiente, sin botón.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import i18n from '../i18n/i18n.js';
import { abrirApp, conSesion, USUARIOS } from '../pruebas/ayudas.jsx';
import { LOTE_PENDIENTE } from '../pruebas/lotes.js';
import { anchoDePantalla } from '../pruebas/preparacion.js';
import { conResultado, resultadoAnalizado, resultadoPendiente } from '../pruebas/resultados.js';
import { problema, servidor } from '../pruebas/servidor.js';

const LOTE = LOTE_PENDIENTE.id;
const RUTA = `/lotes/${LOTE}`;

/**
 * Responde el reporte del lote como el backend: el PDF con su nombre en Content-Disposition. Anota cada petición.
 *
 * @param {{ espera?: Promise<unknown> }} [opciones] lo que espera la respuesta antes de salir
 * @returns {{ peticiones: Request[] }} las peticiones que llegaron
 */
function conReporte({ espera } = {}) {
  const anotado = { peticiones: [] };
  servidor.use(http.get(`/api/v1/lotes/${LOTE}/reporte`, async ({ request }) => {
    anotado.peticiones.push(request);
    await espera;
    const idioma = new URL(request.url).searchParams.get('idioma');
    const nombre = `InspectIA-L-0143-${idioma}.pdf`;
    return new HttpResponse('%PDF-1.7 reporte', {
      headers: {
        'Content-Type': 'application/pdf',
        'Content-Disposition': `attachment; filename="${nombre}"; filename*=UTF-8''${nombre}`,
      },
    });
  }));
  return anotado;
}

describe('Descargar el reporte PDF', () => {
  let guardados;

  beforeEach(() => {
    guardados = [];
    URL.createObjectURL = vi.fn(() => 'blob:reporte');
    URL.revokeObjectURL = vi.fn();
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function guardar() {
      guardados.push({ nombre: this.download, url: this.href });
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
    anchoDePantalla(1280);
  });

  it('RF-15 · C1 · el usuario final pulsa «Descargar PDF», junto al título como en el prototipo, y el reporte se pide con su sesión y se guarda como InspectIA-L-0143-es.pdf', async () => {
    conSesion(USUARIOS.USUARIO_FINAL);
    conResultado(LOTE, resultadoAnalizado());
    const reporte = conReporte();
    const { usuario } = abrirApp(RUTA);

    const boton = await screen.findByRole('button', { name: 'Descargar PDF' });
    const resumen = screen.getByRole('region', { name: 'Resumen del resultado' });
    expect(resumen.compareDocumentPosition(boton)).toBe(Node.DOCUMENT_POSITION_PRECEDING);
    await usuario.click(boton);

    await waitFor(() => expect(guardados).toEqual([{ nombre: 'InspectIA-L-0143-es.pdf', url: 'blob:reporte' }]));
    expect(reporte.peticiones).toHaveLength(1);
    expect(new URL(reporte.peticiones[0].url).searchParams.get('idioma')).toBe('es');
    expect(reporte.peticiones[0].headers.get('Authorization')).toBe('Bearer token-de-prueba');
    expect(URL.createObjectURL).toHaveBeenCalledWith(expect.any(Blob));
    expect(screen.getByText('Se descargó el PDF del lote L-0143.')).toBeInTheDocument();
  });

  it('RF-15 · la administradora de empresa también lo descarga, y con la página en inglés el reporte sale en inglés', async () => {
    await i18n.changeLanguage('en');
    conSesion(USUARIOS.ADMIN_EMPRESA);
    conResultado(LOTE, resultadoAnalizado());
    const reporte = conReporte();
    const { usuario } = abrirApp(RUTA);

    await usuario.click(await screen.findByRole('button', { name: 'Download PDF' }));

    await waitFor(() => expect(guardados.map(({ nombre }) => nombre)).toEqual(['InspectIA-L-0143-en.pdf']));
    expect(new URL(reporte.peticiones[0].url).searchParams.get('idioma')).toBe('en');
  });

  it('RF-15 · mientras baja, el botón dice «Descargando el PDF…», está ocupado y no pide otro', async () => {
    conSesion(USUARIOS.USUARIO_FINAL);
    conResultado(LOTE, resultadoAnalizado());
    let soltar;
    const reporte = conReporte({ espera: new Promise((resolver) => { soltar = resolver; }) });
    const { usuario } = abrirApp(RUTA);

    await usuario.click(await screen.findByRole('button', { name: 'Descargar PDF' }));
    const ocupado = await screen.findByRole('button', { name: 'Descargando el PDF…' });
    expect(ocupado).toHaveAttribute('aria-busy', 'true');
    expect(ocupado).toHaveAttribute('aria-disabled', 'true');
    await usuario.click(ocupado);
    soltar();

    expect(await screen.findByRole('button', { name: 'Descargar PDF' })).not.toHaveAttribute('aria-busy');
    expect(reporte.peticiones).toHaveLength(1);
    expect(guardados).toHaveLength(1);
  });

  it('RNF-06 · C3 · si la descarga falla, la página dice que no se pudo con el motivo del backend, y se puede intentar otra vez', async () => {
    conSesion(USUARIOS.USUARIO_FINAL);
    conResultado(LOTE, resultadoAnalizado());
    servidor.use(http.get(`/api/v1/lotes/${LOTE}/reporte`, () => problema(409,
      'Este lote todavía no tiene resultado: su reporte se descarga cuando termine el análisis.')));
    const { usuario } = abrirApp(RUTA);

    await usuario.click(await screen.findByRole('button', { name: 'Descargar PDF' }));

    const alerta = await screen.findByRole('alert');
    expect(alerta).toHaveTextContent('No se pudo descargar el PDF.');
    expect(alerta).toHaveTextContent('Este lote todavía no tiene resultado: su reporte se descarga cuando termine el análisis.');
    expect(guardados).toHaveLength(0);
    conReporte();
    await usuario.click(screen.getByRole('button', { name: 'Descargar PDF' }));
    await waitFor(() => expect(guardados).toHaveLength(1));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('RF-15 · en el celular, «Descargar PDF» va al final, a todo el ancho, antes de «Nuevo lote» (RNF-05)', async () => {
    anchoDePantalla(390);
    conSesion(USUARIOS.USUARIO_FINAL);
    conResultado(LOTE, resultadoAnalizado());
    abrirApp(RUTA);

    const boton = await screen.findByRole('button', { name: 'Descargar PDF' });
    expect(boton).toHaveStyle({ alignSelf: 'stretch' });
    expect(screen.getByRole('region', { name: 'Fotos marcadas' }).compareDocumentPosition(boton))
      .toBe(Node.DOCUMENT_POSITION_FOLLOWING);
    const nuevoLote = within(screen.getByRole('main')).getByRole('link', { name: 'Nuevo lote' });
    expect(boton.compareDocumentPosition(nuevoLote)).toBe(Node.DOCUMENT_POSITION_FOLLOWING);
  });

  it('RF-15 · un lote pendiente todavía no tiene reporte: no se ofrece «Descargar PDF»', async () => {
    conSesion(USUARIOS.USUARIO_FINAL);
    conResultado(LOTE, resultadoPendiente('PENDIENTE'));
    abrirApp(RUTA);

    expect(await screen.findByText('Este lote todavía no tiene resultado.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Descargar PDF' })).not.toBeInTheDocument();
  });
});
