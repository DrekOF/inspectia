/*
 * Nombre: cliente.test.js
 * Descripción: Pruebas del cliente de la API: el token y el idioma en cada petición, los errores del backend
 *              con sus campos, qué pasa con un 401, con sesión y sin ella o con un token que ya se cambió, y
 *              las fallas sin conexión o sin mensaje (RF-01, RF-18, RNF-04, RNF-06, D-28); y un archivo que baja con la
 *              sesión, con su nombre de Content-Disposition (RF-15).
 * Fecha de creación: 20/10/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { problema, servidor } from '../pruebas/servidor.js';
import { crearCliente, ErrorDeApi, nombreDelArchivo } from './cliente.js';

/**
 * Un cliente con una sesión de prueba y el idioma dado.
 *
 * @param {{ token?: string|null, idioma?: string }} [opciones] el token (null: sin sesión) y el idioma
 * @returns {{ pedir: (metodo: string, ruta: string, datos?: object) => Promise<object|null>,
 *   alCerrarSesion: import('vitest').Mock }} el cliente y su aviso de cierre
 */
function cliente({ token = 'token-de-prueba', idioma = 'es' } = {}) {
  const alCerrarSesion = vi.fn();
  const pedir = crearCliente({ token: () => token, idioma: () => idioma, alCerrarSesion });
  return { pedir, alCerrarSesion };
}

/**
 * El error que lanza una petición.
 *
 * @param {Promise} promesa la petición
 * @returns {Promise<ErrorDeApi>} el error
 */
async function errorDe(promesa) {
  try {
    await promesa;
  } catch (error) {
    return error;
  }
  throw new Error('La petición no falló');
}

describe('cliente de la API', () => {
  it('RF-01 · cada petición lleva el token en Authorization: Bearer y el idioma de la app en Accept-Language', async () => {
    let encabezados;
    servidor.use(http.get('/api/v1/sesion', ({ request }) => {
      encabezados = request.headers;
      return HttpResponse.json({ vence: '2026-10-20T20:00:00Z' });
    }));
    const { pedir } = cliente({ idioma: 'en' });

    expect(await pedir('GET', '/sesion')).toEqual({ vence: '2026-10-20T20:00:00Z' });
    expect(encabezados.get('Authorization')).toBe('Bearer token-de-prueba');
    expect(encabezados.get('Accept-Language')).toBe('en');
    expect(encabezados.get('Accept')).toBe('application/json');
    expect(encabezados.get('Content-Type')).toBeNull();
  });

  it('RF-01 · sin sesión no envía Authorization y envía los datos en JSON', async () => {
    let peticion;
    servidor.use(http.post('/api/v1/sesion', async ({ request }) => {
      peticion = { autorizacion: request.headers.get('Authorization'), tipo: request.headers.get('Content-Type'),
        cuerpo: await request.json() };
      return HttpResponse.json({ token: 'nuevo' });
    }));
    const { pedir } = cliente({ token: null });

    await pedir('POST', '/sesion', { correo: 'ana.perez@cacaosdelhuila.co', contrasena: 'Clave-Segura-2026' });

    expect(peticion).toEqual({ autorizacion: null, tipo: 'application/json',
      cuerpo: { correo: 'ana.perez@cacaosdelhuila.co', contrasena: 'Clave-Segura-2026' } });
  });

  it('RF-18 · un 400 del backend llega con su mensaje, el error de cada campo y la ayuda', async () => {
    servidor.use(http.put('/api/v1/sesion/contrasena', () => problema(400, 'Revisa los datos marcados.', {
      errores: [{ campo: 'contrasenaActual', mensaje: 'La contraseña actual no es correcta.' }],
      ayuda: 'Si fallas 5 veces seguidas, la cuenta se bloquea 15 minutos.',
    })));

    const error = await errorDe(cliente().pedir('PUT', '/sesion/contrasena', {}));

    expect(error).toBeInstanceOf(ErrorDeApi);
    expect(error.estado).toBe(400);
    expect(error.detalle).toBe('Revisa los datos marcados.');
    expect(error.errores).toEqual([{ campo: 'contrasenaActual', mensaje: 'La contraseña actual no es correcta.' }]);
    expect(error.ayuda).toBe('Si fallas 5 veces seguidas, la cuenta se bloquea 15 minutos.');
    expect(error.bloqueadoHasta).toBeNull();
    expect(error.reintentarEnSegundos).toBeNull();
  });

  it('RF-18 · C2 · el 429 del bloqueo trae la hora en que termina y los segundos que faltan', async () => {
    servidor.use(http.put('/api/v1/sesion/contrasena', () => problema(429,
      'Tu cuenta está bloqueada por intentos fallidos: podrás cambiar la contraseña cuando termine el bloqueo.',
      { bloqueadoHasta: '2026-10-20T20:45:00Z' }, { 'Retry-After': '540' })));

    const error = await errorDe(cliente().pedir('PUT', '/sesion/contrasena', {}));

    expect(error.estado).toBe(429);
    expect(error.bloqueadoHasta).toBe('2026-10-20T20:45:00Z');
    expect(error.reintentarEnSegundos).toBe(540);
  });

  it('RNF-04 · C6 · un 401 con sesión la cierra con el mensaje del backend, como «Tu contraseña cambió»', async () => {
    servidor.use(http.get('/api/v1/sesion', () => problema(401, 'Tu contraseña cambió: inicia sesión de nuevo.')));
    const { pedir, alCerrarSesion } = cliente();

    const error = await errorDe(pedir('GET', '/sesion'));

    expect(error.estado).toBe(401);
    expect(alCerrarSesion).toHaveBeenCalledWith('Tu contraseña cambió: inicia sesión de nuevo.');
  });

  it('RF-01 · C2 · un 401 sin sesión, como el de datos errados al iniciar sesión, no cierra nada: solo responde el error', async () => {
    servidor.use(http.post('/api/v1/sesion', () => problema(401, 'Correo o contraseña incorrectos',
      { ayuda: 'Si fallas 5 veces seguidas, la cuenta se bloquea 15 minutos.' })));
    const { pedir, alCerrarSesion } = cliente({ token: null });

    const error = await errorDe(pedir('POST', '/sesion', { correo: 'x@y.co', contrasena: 'no-es' }));

    expect(error.detalle).toBe('Correo o contraseña incorrectos');
    expect(alCerrarSesion).not.toHaveBeenCalled();
  });

  it('RNF-06 · C3 · si el servidor no responde, el error queda marcado como falta de conexión (estado 0) y no cierra la sesión', async () => {
    servidor.use(http.get('/api/v1/sesion', () => HttpResponse.error()));
    const { pedir, alCerrarSesion } = cliente();

    const error = await errorDe(pedir('GET', '/sesion'));

    expect(error).toBeInstanceOf(ErrorDeApi);
    expect(error.estado).toBe(0);
    expect(error.detalle).toBeNull();
    expect(alCerrarSesion).not.toHaveBeenCalled();
  });

  it('RNF-06 · una respuesta de error que no es JSON, como la de un proxy caído, llega sin mensaje del backend', async () => {
    servidor.use(http.get('/api/v1/sesion', () => new HttpResponse('<html>Bad Gateway</html>', { status: 502 })));

    const error = await errorDe(cliente().pedir('GET', '/sesion'));

    expect(error.estado).toBe(502);
    expect(error.detalle).toBeNull();
    expect(error.errores).toEqual([]);
  });

  it('RF-18 · un 401 de una petición hecha con un token que ya se cambió no cierra la sesión nueva', async () => {
    let token = 'token-viejo';
    const alCerrarSesion = vi.fn();
    const pedir = crearCliente({ token: () => token, idioma: () => 'es', alCerrarSesion });
    servidor.use(http.get('/api/v1/sesion', async () => {
      token = 'token-nuevo';
      return problema(401, 'Tu contraseña cambió: inicia sesión de nuevo.');
    }));

    await errorDe(pedir('GET', '/sesion'));

    expect(alCerrarSesion).not.toHaveBeenCalled();
  });

  it('RF-15 · un archivo, como el reporte PDF, se pide con la sesión y llega como Blob con su nombre', async () => {
    const { pedir } = cliente();
    let encabezados;
    servidor.use(http.get('/api/v1/lotes/1/reporte', ({ request }) => {
      encabezados = request.headers;
      return new HttpResponse('%PDF', {
        headers: { 'Content-Type': 'application/pdf', 'Content-Disposition': 'attachment; filename="L-1-es.pdf"' },
      });
    }));

    const { archivo, nombre } = await pedir('GET', '/lotes/1/reporte', undefined, { comoArchivo: true });

    expect(await archivo.text()).toBe('%PDF');
    expect(nombre).toBe('L-1-es.pdf');
    expect(encabezados.get('Authorization')).toBe('Bearer token-de-prueba');
  });

  it('RF-15 · si el archivo no se puede bajar, el error llega como los demás, con el mensaje del backend', async () => {
    const { pedir } = cliente();
    servidor.use(http.get('/api/v1/lotes/1/reporte', () => problema(404, 'No existe un registro con ese identificador.')));

    const error = await errorDe(pedir('GET', '/lotes/1/reporte', undefined, { comoArchivo: true }));

    expect(error).toBeInstanceOf(ErrorDeApi);
    expect(error.estado).toBe(404);
    expect(error.detalle).toBe('No existe un registro con ese identificador.');
  });

  it('RF-15 · el nombre del archivo sale de filename* en UTF-8 si viene, y si no de filename, con o sin comillas', () => {
    expect(nombreDelArchivo('attachment; filename="a.pdf"; filename*=UTF-8\'\'InspectIA-L%C3%91-es.pdf'))
      .toBe('InspectIA-LÑ-es.pdf');
    expect(nombreDelArchivo('attachment; filename="Lote \\"7\\".pdf"')).toBe('Lote "7".pdf');
    expect(nombreDelArchivo('attachment; filename=L-1.pdf')).toBe('L-1.pdf');
    expect(nombreDelArchivo('attachment')).toBeNull();
    expect(nombreDelArchivo(null)).toBeNull();
  });
});
