/*
 * Nombre: FotosDeLaMuestra.test.jsx
 * Descripción: Pruebas de la pantalla Fotos de la muestra, que en este paso muestra el lote y la guía de captura
 *              (RF-04 C1, RF-05 C4); un lote que no existe o es de otra empresa (RNF-03).
 * Fecha de creación: 10/11/2026
 * Actualización: 10/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import { abrirApp, conSesion, USUARIOS } from '../pruebas/ayudas.jsx';
import { LOTE_PENDIENTE } from '../pruebas/lotes.js';
import { problema, servidor } from '../pruebas/servidor.js';

const RUTA = `/lotes/${LOTE_PENDIENTE.id}/fotos`;

describe('Fotos de la muestra', () => {
  beforeEach(() => conSesion(USUARIOS.USUARIO_FINAL));

  it('RF-04 · C1 · la página de las fotos nombra el lote: su código y su finca o proveedor', async () => {
    servidor.use(http.get(`/api/v1/lotes/${LOTE_PENDIENTE.id}`, () => HttpResponse.json(LOTE_PENDIENTE)));
    abrirApp(RUTA);

    expect(await screen.findByRole('heading', { name: 'Fotos de la muestra' })).toBeInTheDocument();
    expect(await screen.findByText('Lote L-0143 · La Esperanza. Sube 2 o 3 fotos; si sales ahora, el lote queda '
      + 'pendiente y lo retomas desde el Historial.')).toBeInTheDocument();
  });

  it('RF-05 · C4 · la guía de captura se ve en la página de las fotos, con la foto de ejemplo y sus reglas', async () => {
    servidor.use(http.get(`/api/v1/lotes/${LOTE_PENDIENTE.id}`, () => HttpResponse.json(LOTE_PENDIENTE)));
    abrirApp(RUTA);

    const guia = await screen.findByRole('region', { name: 'Guía de captura' });
    expect(within(guia).getByRole('img', { name: 'Ejemplo de foto correcta: granos enteros y separados sobre '
      + 'papel blanco, vistos desde arriba' })).toBeInTheDocument();
    expect(within(guia).getAllByRole('listitem').map((regla) => regla.textContent)).toEqual([
      'Entre 100 y 150 granos, repartidos en 2 o 3 fotos (unos 50 por foto).',
      'Granos enteros sobre papel blanco, separados entre sí.',
      'Luz difusa, sin sombras fuertes.',
      'Toma la foto desde arriba, con el celular quieto.',
      'JPG o PNG, de hasta 10 MB cada una.',
    ]);
  });

  it('RF-04 · los pasos del análisis muestran el primero listo y el segundo en curso', async () => {
    servidor.use(http.get(`/api/v1/lotes/${LOTE_PENDIENTE.id}`, () => HttpResponse.json(LOTE_PENDIENTE)));
    abrirApp(RUTA);

    const pasos = await screen.findByRole('list', { name: 'Pasos del análisis' });
    expect(within(pasos).getByText('Datos del lote').closest('li')).toHaveTextContent('(listo)');
    expect(within(pasos).getByText('Fotos de la muestra').closest('li')).toHaveAttribute('aria-current', 'step');
  });

  it('RNF-03 · si el lote no existe o es de otra empresa, lo dice con el mensaje del backend', async () => {
    servidor.use(http.get(`/api/v1/lotes/${LOTE_PENDIENTE.id}`,
      () => problema(404, 'No existe un registro con ese identificador.')));
    abrirApp(RUTA);

    expect(await screen.findByRole('alert')).toHaveTextContent('No existe un registro con ese identificador.');
    expect(screen.queryByRole('region', { name: 'Guía de captura' })).not.toBeInTheDocument();
  });
});
