/*
 * Nombre: fotos.test.js (api)
 * Descripción: Pruebas de las peticiones de las fotos de un lote (RF-05, D-41): las rutas de la API, y la subida al
 *              enlace firmado, que va sin el token de la sesión y convierte sus errores en un ErrorDeApi.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { conSesion, USUARIOS } from '../pruebas/ayudas.jsx';
import { archivo } from '../pruebas/fotos.js';
import { problema, servidor } from '../pruebas/servidor.js';
import { ErrorDeApi } from './cliente.js';
import { confirmarFoto, fotosDelLote, pedirSubida, subirArchivo } from './fotos.js';

const LOTE = '0199f0a0-0000-7000-8000-000000000143';
const ENLACE = {
  llave: 'e/l/a.jpg', url: '/api/v1/archivos/e/l/a.jpg?vence=1&firma=s', metodo: 'PUT',
  encabezados: { 'Content-Type': 'image/jpeg' }, vence: '2026-11-23T15:10:00Z',
};
const ENLACE_NO_VALIDO = 'El enlace de la foto venció o no es válido: vuelve a intentarlo.';

describe('Peticiones de las fotos', () => {
  it('RF-05 · las fotos del lote, el enlace de subida y la confirmación van a sus rutas de la API, con sus datos', async () => {
    const pedir = vi.fn().mockResolvedValue({});

    await fotosDelLote(pedir, LOTE);
    await pedirSubida(pedir, LOTE, 2, { tipo: 'image/png', tamanoBytes: 2048 });
    await confirmarFoto(pedir, LOTE, 2, 'e/l/a.png');

    expect(pedir.mock.calls).toEqual([
      ['GET', `/lotes/${LOTE}/fotos`],
      ['POST', `/lotes/${LOTE}/fotos/2/subida`, { tipo: 'image/png', tamanoBytes: 2048 }],
      ['PUT', `/lotes/${LOTE}/fotos/2`, { llave: 'e/l/a.png' }],
    ]);
  });

  it('D-41 · la foto sube al enlace con su método y el tipo firmado, aunque el archivo no diga su tipo, sin el token de la sesión y con el idioma de la app', async () => {
    conSesion(USUARIOS.USUARIO_FINAL);
    let recibido;
    servidor.use(http.put('/api/v1/archivos/*', async ({ request }) => {
      recibido = { url: request.url, tipo: request.headers.get('Content-Type'), idioma: request.headers.get('Accept-Language'),
        autorizacion: request.headers.get('Authorization'), bytes: (await request.arrayBuffer()).byteLength };
      return new HttpResponse(null, { status: 200 });
    }));

    await subirArchivo(ENLACE, archivo('IMG_0001', '', 100, 'image/jpeg'), 'en');

    expect(recibido).toEqual({ url: `${window.location.origin}/api/v1/archivos/e/l/a.jpg?vence=1&firma=s`,
      tipo: 'image/jpeg', idioma: 'en', autorizacion: null, bytes: 100 });
  });

  it('D-41 · si el enlace responde un error, es un ErrorDeApi con su estado y su mensaje; sin conexión, con estado 0', async () => {
    const foto = archivo('a.jpg', 'image/jpeg');
    servidor.use(
      http.put('/api/v1/archivos/*', () => problema(403, ENLACE_NO_VALIDO), { once: true }),
      http.put('/api/v1/archivos/*', () => HttpResponse.error(), { once: true }),
    );

    const vencido = await subirArchivo(ENLACE, foto).catch((error) => error);
    const sinConexion = await subirArchivo(ENLACE, foto).catch((error) => error);

    expect(vencido).toBeInstanceOf(ErrorDeApi);
    expect(vencido).toMatchObject({ estado: 403, detalle: ENLACE_NO_VALIDO });
    expect(sinConexion).toBeInstanceOf(ErrorDeApi);
    expect(sinConexion).toMatchObject({ estado: 0 });
  });

  it('D-41 · un error de S3, que responde en XML, es un ErrorDeApi con su estado y sin mensaje', async () => {
    servidor.use(http.put('https://inspectia-fotos.s3.us-east-1.amazonaws.com/*', () => new HttpResponse(
      '<Error><Code>AccessDenied</Code></Error>', { status: 403, headers: { 'Content-Type': 'application/xml' } })));

    const error = await subirArchivo({ ...ENLACE, url: 'https://inspectia-fotos.s3.us-east-1.amazonaws.com/e/l/a.jpg' },
      archivo('a.jpg', 'image/jpeg')).catch((rechazo) => rechazo);

    expect(error).toBeInstanceOf(ErrorDeApi);
    expect(error).toMatchObject({ estado: 403, detalle: null });
  });
});
