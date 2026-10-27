/*
 * Nombre: empresaApi.test.js
 * Descripción: Pruebas generadas del cliente de la API de empresa: la ruta y el método de cada
 *              operación.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.10)
 * Versión: 1.10
 * Entidades: empresa 1.3, usuario 1.8
 */
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { crearCliente } from '../../api/cliente.js';
import { servidor } from '../../pruebas/servidor.js';
import * as api from './empresaApi.js';

/** El cliente de la API, con una sesión de prueba: estas pruebas solo miran lo que pide. */
const pedir = crearCliente({ token: () => 'token-de-prueba', idioma: () => 'es', alCerrarSesion: () => {} });

/** Un identificador de ejemplo, como los de la API. */
const ID = '0199e2a0-0000-7000-8000-0000000000a1';

/**
 * Atiende cualquier petición a la API y anota su método, su ruta y su cuerpo.
 *
 * @returns {Array<{ metodo: string, ruta: string, cuerpo: object|null }>} las peticiones, en orden
 */
function anotarPeticiones() {
  const peticiones = [];
  servidor.use(http.all('/api/v1/*', async ({ request }) => {
    const direccion = new URL(request.url);
    const texto = await request.text();
    peticiones.push({ metodo: request.method, ruta: direccion.pathname + direccion.search,
      cuerpo: texto ? JSON.parse(texto) : null });
    return HttpResponse.json({});
  }));
  return peticiones;
}

describe('cliente de la API de empresa (generado)', () => {
  it('RF-02 · el cliente de la API de empresa usa la ruta y el método de cada operación del JSON', async () => {
    const peticiones = anotarPeticiones();

    await api.listar(pedir);
    await api.ver(pedir, ID);
    await api.crear(pedir, { dato: 'nuevo' });
    await api.editar(pedir, ID, { dato: 'editado' });
    await api.desactivar(pedir, ID);
    await api.reactivar(pedir, ID);

    expect(peticiones).toEqual([
      { metodo: 'GET', ruta: '/api/v1/empresas?pagina=0&tamano=20', cuerpo: null },
      { metodo: 'GET', ruta: `/api/v1/empresas/${ID}`, cuerpo: null },
      { metodo: 'POST', ruta: '/api/v1/empresas', cuerpo: { dato: 'nuevo' } },
      { metodo: 'PUT', ruta: `/api/v1/empresas/${ID}`, cuerpo: { dato: 'editado' } },
      { metodo: 'POST', ruta: `/api/v1/empresas/${ID}/desactivar`, cuerpo: null },
      { metodo: 'POST', ruta: `/api/v1/empresas/${ID}/reactivar`, cuerpo: null },
    ]);
  });

  it('RF-02 · el listado de empresa pide la página, el tamaño y, si se pide, el filtro activo (D-33)', async () => {
    const peticiones = anotarPeticiones();

    await api.listar(pedir, { pagina: 2, tamano: 50, activo: false });
    await api.listar(pedir, { activo: true });

    expect(peticiones.map(({ ruta }) => ruta)).toEqual([
      '/api/v1/empresas?pagina=2&tamano=50&activo=false',
      '/api/v1/empresas?pagina=0&tamano=20&activo=true',
    ]);
  });
});
