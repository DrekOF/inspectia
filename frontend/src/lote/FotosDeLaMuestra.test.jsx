/*
 * Nombre: FotosDeLaMuestra.test.jsx
 * Descripción: Pruebas de la pantalla Fotos de la muestra (RF-05, RF-14 C5): el lote y la guía de captura, las 3
 *              casillas, el botón que abre la cámara en el celular y elige archivos en el PC, las fotos que se
 *              rechazan antes de subir, la subida por el enlace firmado sin el token, el reemplazo, el reintento,
 *              retomar el lote con sus fotos, los pasos y un lote de otra empresa (RNF-03, RNF-04, RNF-05, D-41).
 * Fecha de creación: 10/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { abrirApp, conSesion, USUARIOS } from '../pruebas/ayudas.jsx';
import { archivo, conFotos, conSubidas, fotoCargada } from '../pruebas/fotos.js';
import { LOTE_ANALIZADO, LOTE_PENDIENTE } from '../pruebas/lotes.js';
import { anchoDePantalla } from '../pruebas/preparacion.js';
import { problema, servidor } from '../pruebas/servidor.js';

const RUTA = `/lotes/${LOTE_PENDIENTE.id}/fotos`;
const FORMATO_NO_PERMITIDO = 'La foto debe ser JPG o PNG: elige una foto en uno de esos formatos.';
const DEMASIADO_GRANDE =
  'La foto pesa más de 10 MB: tómala de nuevo con la resolución normal de la cámara o elige otra.';
const ENLACE_NO_VALIDO = 'El enlace de la foto venció o no es válido: vuelve a intentarlo.';

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

  it('RF-05 · un lote que ya no está pendiente de análisis no ofrece subir fotos: lo dice', async () => {
    servidor.use(http.get(`/api/v1/lotes/${LOTE_PENDIENTE.id}`,
      () => HttpResponse.json({ ...LOTE_ANALIZADO, id: LOTE_PENDIENTE.id })));
    abrirApp(RUTA);

    expect(await screen.findByText('Este lote ya no está pendiente de análisis: sus fotos no se pueden cambiar.'))
      .toBeInTheDocument();
    expect(screen.queryByRole('group', { name: 'Foto 1' })).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Archivo de la foto 1')).not.toBeInTheDocument();
  });

  it('RNF-04 · C2 · la administradora que abre la dirección de las fotos ve «Sin permiso»: no sube fotos ni retoma lotes', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    abrirApp(RUTA);

    expect(await screen.findByRole('heading', { name: 'Sin permiso' })).toBeInTheDocument();
    expect(screen.queryByRole('group', { name: 'Foto 1' })).not.toBeInTheDocument();
  });
});
