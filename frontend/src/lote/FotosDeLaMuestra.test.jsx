/*
 * Nombre: FotosDeLaMuestra.test.jsx
 * Descripción: Pruebas de la pantalla Fotos de la muestra (RF-05, RF-14 C5): el lote y la guía de captura, las 3
 *              casillas, el botón que abre la cámara en el celular y elige archivos en el PC, las fotos que se
 *              rechazan antes de subir, la subida por el enlace firmado sin el token y con el tipo firmado, el
 *              reemplazo, el reintento, que solo confirma si el archivo ya llegó, el foco al reintentar, una casilla
 *              que no sube dos fotos a la vez, retomar el lote con sus fotos, los pasos, un lote que ya no está
 *              pendiente y uno de otra empresa (RNF-03, RNF-04, RNF-05, RNF-06, D-41). La muestra 2 (D-25, RF-14 C6):
 *              un lote analizado que sugiere otra muestra recibe sus fotos, con su título y su subtítulo, y se retoma
 *              con las que ya tiene; uno sin sugerencia o con dos muestras no las recibe (RF-12 C8).
 * Fecha de creación: 10/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.4
 */
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { abrirApp, conSesion, USUARIOS } from '../pruebas/ayudas.jsx';
import {
  archivo, archivoLento, conFotos, conSubidas, ENLACE_NO_VALIDO, fotoCargada,
} from '../pruebas/fotos.js';
import { LOTE_ANALIZADO, LOTE_CON_SUGERENCIA, LOTE_PENDIENTE } from '../pruebas/lotes.js';
import { anchoDePantalla } from '../pruebas/preparacion.js';
import { problema, servidor } from '../pruebas/servidor.js';

const RUTA = `/lotes/${LOTE_PENDIENTE.id}/fotos`;
const RUTA_DE_LA_MUESTRA_2 = `/lotes/${LOTE_CON_SUGERENCIA.id}/fotos`;
const DESCRIPCION_DE_LA_MUESTRA_2 = 'Muestra 2 del lote L-0141 · El Recreo. Sube 2 o 3 fotos de otra muestra del mismo '
  + 'lote; si sales ahora, la retomas desde el resultado del lote.';
const NO_PENDIENTE = 'Este lote ya no está pendiente de análisis: sus fotos no se pueden cambiar.';
const FORMATO_NO_PERMITIDO = 'La foto debe ser JPG o PNG: elige una foto en uno de esos formatos.';
const DEMASIADO_GRANDE =
  'La foto pesa más de 10 MB: tómala de nuevo con la resolución normal de la cámara o elige otra.';

/**
 * Elige un archivo para una foto, como lo deja el selector del navegador. Con un evento de cambio, y no con
 * user-event, que descarta en silencio lo que no cumple «accept», como un PDF.
 *
 * @param {number} numero el número de la foto
 * @param {File} elegido el archivo
 */
async function elegir(numero, elegido) {
  fireEvent.change(await screen.findByLabelText(`Archivo de la foto ${numero}`), { target: { files: [elegido] } });
}

/**
 * La casilla de una foto, cuando aparece.
 *
 * @param {number} numero el número de la foto
 * @returns {Promise<HTMLElement>} la casilla
 */
function casilla(numero) {
  return screen.findByRole('group', { name: numero === 3 ? 'Foto 3 (opcional)' : `Foto ${numero}` });
}

describe('Fotos de la muestra', () => {
  beforeEach(() => {
    conSesion(USUARIOS.USUARIO_FINAL);
    servidor.use(http.get(`/api/v1/lotes/${LOTE_PENDIENTE.id}`, () => HttpResponse.json(LOTE_PENDIENTE)));
    conFotos(LOTE_PENDIENTE.id, []);
  });
  afterEach(() => anchoDePantalla(1280));

  it('RF-04 · la página de las fotos nombra el lote, con su código y su finca o proveedor, y dice que se suben 2 o 3 fotos y que se retoma desde el Historial', async () => {
    abrirApp(RUTA);

    expect(await screen.findByRole('heading', { name: 'Fotos de la muestra' })).toBeInTheDocument();
    expect(await screen.findByText('Lote L-0143 · La Esperanza. Sube 2 o 3 fotos; si sales ahora, el lote queda '
      + 'pendiente y lo retomas desde el Historial.')).toBeInTheDocument();
  });

  it('RF-05 · C4 · la guía de captura se ve en la página de las fotos, con la foto de ejemplo y sus reglas', async () => {
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
    abrirApp(RUTA);

    const pasos = await screen.findByRole('list', { name: 'Pasos del análisis' });
    expect(within(pasos).getByText('Datos del lote').closest('li')).toHaveTextContent('(listo)');
    expect(within(pasos).getByText('Fotos de la muestra').closest('li')).toHaveAttribute('aria-current', 'step');
  });

  it('RNF-05 · en el celular los pasos dicen «Paso 2 de 3» con el nombre del paso en curso, como en el prototipo', async () => {
    anchoDePantalla(375);
    abrirApp(RUTA);

    expect(await screen.findByText('Paso 2 de 3 ·')).toHaveTextContent('Paso 2 de 3 · Fotos de la muestra');
    expect(screen.queryByRole('list', { name: 'Pasos del análisis' })).not.toBeInTheDocument();
  });

  it('RNF-03 · si el lote no existe o es de otra empresa, lo dice con el mensaje del backend', async () => {
    servidor.use(http.get(`/api/v1/lotes/${LOTE_PENDIENTE.id}`,
      () => problema(404, 'No existe un registro con ese identificador.')));
    abrirApp(RUTA);

    expect(await screen.findByRole('alert')).toHaveTextContent('No existe un registro con ese identificador.');
    expect(screen.queryByRole('region', { name: 'Guía de captura' })).not.toBeInTheDocument();
    expect(screen.queryByRole('group', { name: 'Foto 1' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Reintentar' })).not.toBeInTheDocument();
  });

  it('RF-04 · si el lote no llega por otra causa, la página lo dice con «Reintentar», que deja el foco en el título y lo muestra', async () => {
    servidor.use(http.get(`/api/v1/lotes/${LOTE_PENDIENTE.id}`, () => problema(500, 'Error interno.'), { once: true }));
    const { usuario } = abrirApp(RUTA);

    expect(await screen.findByRole('alert')).toHaveTextContent('Error interno.');
    await usuario.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(screen.getByRole('heading', { level: 1, name: 'Fotos de la muestra' })).toHaveFocus();
    expect(await screen.findByText(/^Lote L-0143 · La Esperanza\. Sube 2 o 3 fotos/)).toBeInTheDocument();
    expect(await screen.findByRole('group', { name: 'Foto 1' })).toBeInTheDocument();
  });

  it('RF-05 · C1 · en el PC, cada casilla vacía ofrece «Elegir archivo», sin la cámara, para JPG o PNG', async () => {
    abrirApp(RUTA);

    for (const numero of [1, 2, 3]) {
      expect(within(await casilla(numero)).getByRole('button', { name: 'Elegir archivo' })).toBeInTheDocument();
    }
    const entrada = screen.getByLabelText('Archivo de la foto 1');
    expect(entrada).not.toHaveAttribute('capture');
    expect(entrada).toHaveAttribute('accept', 'image/jpeg,image/png,.jpg,.jpeg,.png');
  });

  it('RF-05 · C1 · en el celular, cada casilla vacía ofrece «Tomar foto», que abre la cámara trasera', async () => {
    anchoDePantalla(375);
    abrirApp(RUTA);

    for (const numero of [1, 2, 3]) {
      expect(within(await casilla(numero)).getByRole('button', { name: 'Tomar foto' })).toBeInTheDocument();
    }
    expect(screen.getByLabelText('Archivo de la foto 1')).toHaveAttribute('capture', 'environment');
  });

  it('RF-05 · C2 · la página tiene 3 casillas, la tercera opcional, y no ofrece una cuarta', async () => {
    abrirApp(RUTA);

    await casilla(1);
    expect(screen.getAllByRole('group', { name: /^Foto \d/ })).toHaveLength(3);
    expect(screen.getByRole('group', { name: 'Foto 2' })).toBeInTheDocument();
    expect(within(await casilla(3)).getByText('Agrégala si los granos no caben separados en dos fotos.'))
      .toBeInTheDocument();
    expect(screen.queryByLabelText('Archivo de la foto 4')).not.toBeInTheDocument();
  });

  it('RF-05 · la foto se sube al enlace sin el token de la sesión y con el tipo firmado, se confirma y se ve en su casilla, «Cargada» y con «Reemplazar»', async () => {
    const anotado = conSubidas(LOTE_PENDIENTE.id);
    abrirApp(RUTA);

    await elegir(1, archivo('granos.jpg', 'image/jpeg', 2048));

    const primera = await casilla(1);
    expect(await within(primera).findByRole('img', { name: 'Foto 1 de la muestra' }))
      .toHaveAttribute('src', expect.stringMatching(/^\/api\/v1\/archivos\/.+foto-1-1\.jpg\?vence=2/));
    expect(within(primera).getByText('Cargada')).toBeInTheDocument();
    expect(within(primera).getByRole('button', { name: 'Reemplazar' })).toBeInTheDocument();
    expect(screen.getByText('Foto 1 cargada.')).toBeInTheDocument();
    expect(anotado.pedidas).toEqual([{ numero: 1, tipo: 'image/jpeg', tamanoBytes: 2048 }]);
    expect(anotado.subidas).toEqual([expect.objectContaining({ autorizacion: null, tipo: 'image/jpeg', bytes: 2048 })]);
    expect(anotado.confirmadas).toEqual([{ numero: 1, llave: expect.stringMatching(/foto-1-1\.jpg$/) }]);
  });

  it('RF-05 · el tipo se reconoce por los primeros bytes: un JPG sin tipo, como lo entregan algunos Android, se sube como JPG', async () => {
    const anotado = conSubidas(LOTE_PENDIENTE.id);
    abrirApp(RUTA);

    await elegir(2, archivo('IMG_0001', '', 512, 'image/jpeg'));

    expect(await within(await casilla(2)).findByText('Cargada')).toBeInTheDocument();
    expect(anotado.pedidas).toEqual([{ numero: 2, tipo: 'image/jpeg', tamanoBytes: 512 }]);
    expect(anotado.subidas).toEqual([expect.objectContaining({ tipo: 'image/jpeg', bytes: 512 })]);
  });

  it('RF-05 · un archivo .jpg que por dentro es un PNG se firma y se sube como PNG, aunque el navegador diga JPG', async () => {
    const anotado = conSubidas(LOTE_PENDIENTE.id);
    abrirApp(RUTA);

    await elegir(1, archivo('captura.jpg', 'image/jpeg', 256, 'image/png'));

    expect(await within(await casilla(1)).findByText('Cargada')).toBeInTheDocument();
    expect(anotado.pedidas).toEqual([{ numero: 1, tipo: 'image/png', tamanoBytes: 256 }]);
    expect(anotado.subidas).toEqual([expect.objectContaining({ tipo: 'image/png', idioma: 'es' })]);
    expect(anotado.confirmadas).toEqual([{ numero: 1, llave: expect.stringMatching(/\.png$/) }]);
  });

  it('RF-05 · C3 · un archivo que no es JPG ni PNG se rechaza antes de subirlo, con el mensaje de qué hacer, y no se pide ningún enlace', async () => {
    const anotado = conSubidas(LOTE_PENDIENTE.id);
    abrirApp(RUTA);

    await elegir(1, archivo('informe.pdf', 'application/pdf'));

    expect(await within(await casilla(1)).findByRole('alert')).toHaveTextContent(FORMATO_NO_PERMITIDO);
    expect(anotado.pedidas).toEqual([]);
  });

  it('RF-05 · C3 · un archivo .jpg que por dentro no es una foto se rechaza antes de subirlo', async () => {
    const anotado = conSubidas(LOTE_PENDIENTE.id);
    abrirApp(RUTA);

    await elegir(1, archivo('falsa.jpg', 'image/jpeg', 64, 'application/pdf'));

    expect(await within(await casilla(1)).findByRole('alert')).toHaveTextContent(FORMATO_NO_PERMITIDO);
    expect(anotado.pedidas).toEqual([]);
  });

  it('RF-05 · C3 · una foto de 10.485.761 bytes se rechaza antes de subirla y una de 10.485.760 se sube (prueba de límite)', async () => {
    const anotado = conSubidas(LOTE_PENDIENTE.id);
    abrirApp(RUTA);

    await elegir(1, archivo('grande.jpg', 'image/jpeg', 10_485_761));
    expect(await within(await casilla(1)).findByRole('alert')).toHaveTextContent(DEMASIADO_GRANDE);
    await elegir(2, archivo('justa.jpg', 'image/jpeg', 10_485_760));

    expect(await within(await casilla(2)).findByText('Cargada')).toBeInTheDocument();
    expect(anotado.pedidas).toEqual([{ numero: 2, tipo: 'image/jpeg', tamanoBytes: 10_485_760 }]);
    expect(anotado.subidas[0].bytes).toBe(10_485_760);
  });

  it('RF-05 · C3 · si el backend rechaza la foto, la casilla muestra su mensaje, que dice qué hacer', async () => {
    servidor.use(http.post(`/api/v1/lotes/${LOTE_PENDIENTE.id}/fotos/1/subida`, () => problema(400,
      'Revisa los datos marcados.', { errores: [{ campo: 'tamanoBytes', mensaje: DEMASIADO_GRANDE }] })));
    abrirApp(RUTA);

    await elegir(1, archivo('granos.jpg', 'image/jpeg'));

    expect(await within(await casilla(1)).findByRole('alert')).toHaveTextContent(DEMASIADO_GRANDE);
  });

  it('RF-05 · con un enlace de otro dominio, como el de S3, la página sube la foto ahí, directo y sin el token', async () => {
    const anotado = conSubidas(LOTE_PENDIENTE.id, { base: 'https://inspectia-fotos.s3.us-east-1.amazonaws.com' });
    abrirApp(RUTA);

    await elegir(1, archivo('granos.png', 'image/png'));

    expect(await within(await casilla(1)).findByText('Cargada')).toBeInTheDocument();
    expect(anotado.subidas[0].url).toMatch(/^https:\/\/inspectia-fotos\.s3\.us-east-1\.amazonaws\.com\/e\//);
    expect(anotado.subidas[0].autorizacion).toBeNull();
  });

  it('RF-05 · «Reemplazar» sube la foto nueva con el mismo número: mientras sube se sigue viendo la anterior y después se ve la nueva', async () => {
    conFotos(LOTE_PENDIENTE.id, [fotoCargada(2)]);
    let soltar;
    const anotado = conSubidas(LOTE_PENDIENTE.id, { espera: new Promise((resolver) => { soltar = resolver; }) });
    abrirApp(RUTA);
    const segunda = await casilla(2);
    expect(await within(segunda).findByRole('img', { name: 'Foto 2 de la muestra' }))
      .toHaveAttribute('src', expect.stringContaining('foto-2.jpg'));

    await elegir(2, archivo('nueva.png', 'image/png'));

    expect(await within(segunda).findByRole('progressbar', { name: 'Subiendo la foto 2…' })).toBeInTheDocument();
    expect(within(segunda).getByRole('img', { name: 'Foto 2 de la muestra' }))
      .toHaveAttribute('src', expect.stringContaining('foto-2.jpg'));
    expect(within(segunda).getByRole('button', { name: 'Reemplazar' })).toHaveAttribute('aria-disabled', 'true');
    soltar();
    await waitFor(() => expect(within(segunda).getByRole('img', { name: 'Foto 2 de la muestra' }))
      .toHaveAttribute('src', expect.stringMatching(/foto-2-1\.png/)));
    expect(within(segunda).queryByRole('progressbar')).not.toBeInTheDocument();
    expect(within(segunda).getByRole('button', { name: 'Reemplazar' })).not.toHaveAttribute('aria-disabled');
    expect(anotado.confirmadas).toEqual([{ numero: 2, llave: expect.stringMatching(/\.png$/) }]);
  });

  it('RF-05 · si la subida falla, la casilla lo dice, y «Reintentar» pide un enlace nuevo y la sube', async () => {
    const anotado = conSubidas(LOTE_PENDIENTE.id);
    servidor.use(http.put('/api/v1/archivos/*', () => HttpResponse.error(), { once: true }));
    const { usuario } = abrirApp(RUTA);

    await elegir(1, archivo('granos.jpg', 'image/jpeg'));
    const primera = await casilla(1);
    expect(await within(primera).findByRole('alert')).toHaveTextContent('No se pudo conectar con el servidor.');
    await usuario.click(within(primera).getByRole('button', { name: 'Reintentar' }));

    expect(await within(primera).findByText('Cargada')).toBeInTheDocument();
    expect(within(primera).queryByRole('alert')).not.toBeInTheDocument();
    expect(anotado.pedidas).toHaveLength(2);
    expect(anotado.confirmadas).toHaveLength(1);
  });

  it('RNF-06 · C4 · al pulsar «Reintentar», el foco queda en el botón de la casilla, que sigue ahí mientras sube', async () => {
    conSubidas(LOTE_PENDIENTE.id);
    servidor.use(http.put('/api/v1/archivos/*', () => HttpResponse.error(), { once: true }));
    const { usuario } = abrirApp(RUTA);

    await elegir(1, archivo('granos.jpg', 'image/jpeg'));
    const primera = await casilla(1);
    await usuario.click(await within(primera).findByRole('button', { name: 'Reintentar' }));

    expect(within(primera).getByRole('button', { name: /^(Elegir archivo|Reemplazar)$/ })).toHaveFocus();
    expect(await within(primera).findByText('Cargada')).toBeInTheDocument();
    expect(within(primera).getByRole('button', { name: 'Reemplazar' })).toHaveFocus();
  });

  it('RF-05 · si la foto llegó pero su confirmación falla, «Reintentar» solo la confirma: no vuelve a subirla', async () => {
    const anotado = conSubidas(LOTE_PENDIENTE.id);
    servidor.use(http.put(`/api/v1/lotes/${LOTE_PENDIENTE.id}/fotos/:numero`, () => HttpResponse.error(),
      { once: true }));
    const { usuario } = abrirApp(RUTA);

    await elegir(1, archivo('granos.jpg', 'image/jpeg'));
    const primera = await casilla(1);
    await usuario.click(await within(primera).findByRole('button', { name: 'Reintentar' }));

    expect(await within(primera).findByText('Cargada')).toBeInTheDocument();
    expect(anotado.pedidas).toHaveLength(1);
    expect(anotado.subidas).toHaveLength(1);
    expect(anotado.confirmadas).toEqual([{ numero: 1, llave: expect.stringMatching(/foto-1-1\.jpg$/) }]);
  });

  it('RF-05 · si al confirmar el backend dice que la foto no llegó, «Reintentar» la sube de nuevo con otro enlace', async () => {
    const anotado = conSubidas(LOTE_PENDIENTE.id);
    servidor.use(http.put(`/api/v1/lotes/${LOTE_PENDIENTE.id}/fotos/:numero`, () => problema(400,
      'Revisa los datos marcados.', { errores: [{ campo: 'llave', mensaje: 'La foto no llegó completa: súbela de nuevo.' }] }),
    { once: true }));
    const { usuario } = abrirApp(RUTA);

    await elegir(1, archivo('granos.jpg', 'image/jpeg'));
    const primera = await casilla(1);
    expect(await within(primera).findByRole('alert')).toHaveTextContent('La foto no llegó completa: súbela de nuevo.');
    await usuario.click(within(primera).getByRole('button', { name: 'Reintentar' }));

    expect(await within(primera).findByText('Cargada')).toBeInTheDocument();
    expect(anotado.pedidas).toHaveLength(2);
    expect(anotado.subidas).toHaveLength(2);
  });

  it('RF-05 · mientras lee la foto elegida, la casilla ya está ocupada: otra elección no empieza una segunda subida', async () => {
    const anotado = conSubidas(LOTE_PENDIENTE.id);
    const lenta = archivoLento('en-la-nube.jpg', 'image/jpeg', 300);
    abrirApp(RUTA);
    const primera = await casilla(1);

    await elegir(1, lenta.archivo);
    expect(await within(primera).findByRole('progressbar', { name: 'Subiendo la foto 1…' })).toBeInTheDocument();
    expect(within(primera).getByRole('button', { name: 'Elegir archivo' })).toHaveAttribute('aria-disabled', 'true');
    await elegir(1, archivo('otra.jpg', 'image/jpeg', 100));
    lenta.soltar();

    expect(await within(primera).findByText('Cargada')).toBeInTheDocument();
    expect(anotado.pedidas).toEqual([{ numero: 1, tipo: 'image/jpeg', tamanoBytes: 300 }]);
  });

  it('RNF-06 · un rechazo repetido se vuelve a anunciar: cada intento muestra su aviso de nuevo', async () => {
    abrirApp(RUTA);
    const primera = await casilla(1);

    await elegir(1, archivo('informe.pdf', 'application/pdf'));
    const primerAviso = await within(primera).findByRole('alert');
    await elegir(1, archivo('otro.pdf', 'application/pdf'));

    await waitFor(() => expect(within(primera).getByRole('alert')).not.toBe(primerAviso));
    expect(within(primera).getByRole('alert')).toHaveTextContent(FORMATO_NO_PERMITIDO);
  });

  it('RF-05 · si el enlace venció, la casilla lo dice con «Reintentar», y un 401 del enlace no cierra la sesión', async () => {
    conSubidas(LOTE_PENDIENTE.id);
    // El primer PUT recibe el 401 y el segundo, el 403: servidor.use pone los dos adelante, en este orden.
    servidor.use(
      http.put('/api/v1/archivos/*', () => problema(401, 'Inicia sesión para continuar.'), { once: true }),
      http.put('/api/v1/archivos/*', () => problema(403, ENLACE_NO_VALIDO), { once: true }),
    );
    const { usuario } = abrirApp(RUTA);

    await elegir(1, archivo('granos.jpg', 'image/jpeg'));
    const primera = await casilla(1);
    expect(await within(primera).findByRole('alert')).toHaveTextContent('Inicia sesión para continuar.');
    await usuario.click(within(primera).getByRole('button', { name: 'Reintentar' }));
    expect(await within(primera).findByText(ENLACE_NO_VALIDO)).toBeInTheDocument();
    await usuario.click(within(primera).getByRole('button', { name: 'Reintentar' }));

    expect(await within(primera).findByText('Cargada')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Fotos de la muestra' })).toBeInTheDocument();
    expect(window.location.pathname).toBe(RUTA);
  });

  it('RF-05 · si las fotos del lote no llegan, la página lo dice con «Reintentar», y al reintentar las muestra', async () => {
    conFotos(LOTE_PENDIENTE.id, [fotoCargada(1)]);
    servidor.use(http.get(`/api/v1/lotes/${LOTE_PENDIENTE.id}/fotos`, () => problema(500, 'Error interno.'),
      { once: true }));
    const { usuario } = abrirApp(RUTA);

    expect(await screen.findByText('Error interno.')).toBeInTheDocument();
    expect(screen.queryByRole('group', { name: 'Foto 1' })).not.toBeInTheDocument();
    await usuario.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(screen.getByRole('heading', { name: 'Tus fotos' })).toHaveFocus();
    expect(await within(await casilla(1)).findByRole('img', { name: 'Foto 1 de la muestra' })).toBeInTheDocument();
  });

  it('RF-05 · si el enlace de vista de una foto venció y la imagen no carga, la página pide las fotos otra vez, una sola vez', async () => {
    const lista = conFotos(LOTE_PENDIENTE.id,
      (vez) => [fotoCargada(1, { url: `/api/v1/archivos/e/l/foto-1.jpg?vence=${vez}&firma=v` })]);
    abrirApp(RUTA);
    const primera = await casilla(1);

    fireEvent.error(await within(primera).findByRole('img', { name: 'Foto 1 de la muestra' }));
    await waitFor(() => expect(within(primera).getByRole('img', { name: 'Foto 1 de la muestra' }))
      .toHaveAttribute('src', expect.stringContaining('vence=2')));
    fireEvent.error(within(primera).getByRole('img', { name: 'Foto 1 de la muestra' }));

    await new Promise((resolver) => { setTimeout(resolver, 50); });
    expect(lista.veces).toBe(2);
  });

  it('RF-14 · C5 · al retomar un lote pendiente, la página muestra las fotos que ya se subieron y deja subir las que faltan', async () => {
    conFotos(LOTE_PENDIENTE.id, [fotoCargada(1), fotoCargada(2, { formato: 'PNG' })]);
    abrirApp(RUTA);

    for (const numero of [1, 2]) {
      const una = await casilla(numero);
      expect(await within(una).findByRole('img', { name: `Foto ${numero} de la muestra` })).toBeInTheDocument();
      expect(within(una).getByText('Cargada')).toBeInTheDocument();
      expect(within(una).getByRole('button', { name: 'Reemplazar' })).toBeInTheDocument();
    }
    expect(within(await casilla(3)).getByRole('button', { name: 'Elegir archivo' })).toBeInTheDocument();
  });

  it('RF-05 · un lote que ya no está pendiente de análisis no ofrece subir fotos: lo dice y dice dónde ver su resultado', async () => {
    servidor.use(http.get(`/api/v1/lotes/${LOTE_PENDIENTE.id}`,
      () => HttpResponse.json({ ...LOTE_ANALIZADO, id: LOTE_PENDIENTE.id })));
    abrirApp(RUTA);

    expect(await screen.findByText('Este lote ya no está pendiente de análisis: sus fotos no se pueden cambiar.'))
      .toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ver el resultado' })).toHaveAttribute('href', `/lotes/${LOTE_PENDIENTE.id}`);
    expect(screen.getByText('Lote L-0139 · La Estrella.')).toBeInTheDocument();
    expect(screen.queryByRole('region', { name: 'Guía de captura' })).not.toBeInTheDocument();
    expect(screen.queryByRole('list', { name: 'Pasos del análisis' })).not.toBeInTheDocument();
    expect(screen.queryByRole('group', { name: 'Foto 1' })).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Archivo de la foto 1')).not.toBeInTheDocument();
  });

  it('RNF-04 · C2 · la administradora que abre la dirección de las fotos ve «Sin permiso»: no sube fotos ni retoma lotes', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    abrirApp(RUTA);

    expect(await screen.findByRole('heading', { name: 'Sin permiso' })).toBeInTheDocument();
    expect(screen.queryByRole('group', { name: 'Foto 1' })).not.toBeInTheDocument();
  });

  describe('la muestra 2', () => {
    beforeEach(() => {
      servidor.use(http.get(`/api/v1/lotes/${LOTE_CON_SUGERENCIA.id}`, () => HttpResponse.json(LOTE_CON_SUGERENCIA)));
      // Sin la muestra 2 todavía, el lote no tiene muestra en curso que reciba fotos (D-25).
      conFotos(LOTE_CON_SUGERENCIA.id, []);
    });

    it('RF-14 · C6 · un lote analizado que sugiere otra muestra recibe sus fotos: «Fotos de la muestra 2», su subtítulo, los pasos, la guía y las 3 casillas vacías', async () => {
      abrirApp(RUTA_DE_LA_MUESTRA_2);

      expect(await screen.findByRole('heading', { level: 1, name: 'Fotos de la muestra 2' })).toBeInTheDocument();
      expect(await screen.findByText(DESCRIPCION_DE_LA_MUESTRA_2)).toBeInTheDocument();
      const pasos = screen.getByRole('list', { name: 'Pasos del análisis' });
      expect(within(pasos).getByText('Fotos de la muestra').closest('li')).toHaveAttribute('aria-current', 'step');
      expect(screen.getByRole('region', { name: 'Guía de captura' })).toBeInTheDocument();
      for (const numero of [1, 2, 3]) {
        expect(within(await casilla(numero)).getByRole('button', { name: 'Elegir archivo' })).toBeInTheDocument();
      }
      expect(screen.queryByText(NO_PENDIENTE)).not.toBeInTheDocument();
      expect(screen.queryByRole('link', { name: 'Ver el resultado' })).not.toBeInTheDocument();
    });

    it('RF-14 · C6 · la muestra 2 sube sus fotos como la primera: la foto se sube, se confirma con su número y se ve en su casilla', async () => {
      const anotado = conSubidas(LOTE_CON_SUGERENCIA.id);
      abrirApp(RUTA_DE_LA_MUESTRA_2);

      await elegir(1, archivo('granos.jpg', 'image/jpeg', 2048));

      const primera = await casilla(1);
      expect(await within(primera).findByText('Cargada')).toBeInTheDocument();
      expect(within(primera).getByRole('img', { name: 'Foto 1 de la muestra' }))
        .toHaveAttribute('src', expect.stringMatching(/foto-1-1\.jpg/));
      expect(anotado.pedidas).toEqual([{ numero: 1, tipo: 'image/jpeg', tamanoBytes: 2048 }]);
      expect(anotado.confirmadas).toEqual([{ numero: 1, llave: expect.stringMatching(/foto-1-1\.jpg$/) }]);
    });

    it('RF-14 · C6 · al retomar la muestra 2, la página muestra las fotos que ya tiene y deja subir las que faltan', async () => {
      conFotos(LOTE_CON_SUGERENCIA.id, [fotoCargada(1)], { muestra: 2 });
      abrirApp(RUTA_DE_LA_MUESTRA_2);

      expect(await screen.findByRole('heading', { level: 1, name: 'Fotos de la muestra 2' })).toBeInTheDocument();
      const primera = await casilla(1);
      expect(await within(primera).findByRole('img', { name: 'Foto 1 de la muestra' })).toBeInTheDocument();
      expect(within(primera).getByRole('button', { name: 'Reemplazar' })).toBeInTheDocument();
      expect(within(await casilla(2)).getByRole('button', { name: 'Elegir archivo' })).toBeInTheDocument();
    });

    it('RF-12 · C8 · un lote analizado sin sugerencia no recibe otra muestra: la página de las fotos no tiene casillas ni archivos, y lleva a su resultado', async () => {
      servidor.use(http.get(`/api/v1/lotes/${LOTE_ANALIZADO.id}`, () => HttpResponse.json(LOTE_ANALIZADO)));
      conFotos(LOTE_ANALIZADO.id, [fotoCargada(1), fotoCargada(2)], { muestra: 1, estado: 'TERMINADA' });
      abrirApp(`/lotes/${LOTE_ANALIZADO.id}/fotos`);

      expect(await screen.findByRole('link', { name: 'Ver el resultado' })).toHaveAttribute('href', `/lotes/${LOTE_ANALIZADO.id}`);
      expect(screen.queryByRole('heading', { name: 'Fotos de la muestra 2' })).not.toBeInTheDocument();
      expect(screen.queryByRole('group', { name: 'Foto 1' })).not.toBeInTheDocument();
      expect(screen.queryByLabelText('Archivo de la foto 1')).not.toBeInTheDocument();
      expect(screen.queryByRole('region', { name: 'Guía de captura' })).not.toBeInTheDocument();
    });

    it('RF-12 · C8 · un lote con dos muestras no recibe una tercera: la página de las fotos no tiene casillas ni archivos, y lleva a su resultado', async () => {
      const conDosMuestras = {
        ...LOTE_CON_SUGERENCIA, porcentajeSanos: 89.5, categoria: 'CORRIENTE', sugerirOtraMuestra: false,
      };
      servidor.use(http.get(`/api/v1/lotes/${LOTE_CON_SUGERENCIA.id}`, () => HttpResponse.json(conDosMuestras)));
      conFotos(LOTE_CON_SUGERENCIA.id, [fotoCargada(1), fotoCargada(2)], { muestra: 2, estado: 'TERMINADA' });
      abrirApp(RUTA_DE_LA_MUESTRA_2);

      expect(await screen.findByRole('link', { name: 'Ver el resultado' }))
        .toHaveAttribute('href', `/lotes/${LOTE_CON_SUGERENCIA.id}`);
      expect(screen.queryByRole('group', { name: 'Foto 1' })).not.toBeInTheDocument();
      expect(screen.queryByLabelText('Archivo de la foto 1')).not.toBeInTheDocument();
      expect(screen.queryByRole('region', { name: 'Guía de captura' })).not.toBeInTheDocument();
    });
  });
});
