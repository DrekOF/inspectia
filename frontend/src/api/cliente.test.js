/*
 * Nombre: cliente.test.js
 * Descripción: Pruebas del cliente de la API: el token y el idioma en cada petición, los errores del backend
 *              con sus campos y qué pasa con un 401, con sesión y sin ella (RF-01, RF-18, RNF-04, D-28).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { problema, servidor } from '../pruebas/servidor.js';
import { crearCliente, ErrorDeApi } from './cliente.js';

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

  it('RNF-06 · C3 · si el servidor no responde, el error dice que no hubo conexión, sin cerrar la sesión', async () => {
    servidor.use(http.get('/api/v1/sesion', () => HttpResponse.error()));
    const { pedir, alCerrarSesion } = cliente();

    const error = await errorDe(pedir('GET', '/sesion'));

    expect(error).toBeInstanceOf(ErrorDeApi);
    expect(error.estado).toBe(0);
    expect(alCerrarSesion).not.toHaveBeenCalled();
  });
});
