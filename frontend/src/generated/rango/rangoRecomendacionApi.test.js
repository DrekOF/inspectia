/*
 * Nombre: rangoRecomendacionApi.test.js
 * Descripción: Pruebas generadas del cliente de la API de rango_recomendacion: la ruta y el método de
 *              cada operación.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.10)
 * Versión: 1.10
 * Entidades: rango_recomendacion 1.3
 */
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { crearCliente } from '../../api/cliente.js';
import { servidor } from '../../pruebas/servidor.js';
import * as api from './rangoRecomendacionApi.js';

/** El cliente de la API, con una sesión de prueba: estas pruebas solo miran lo que pide. */
const pedir = crearCliente({ token: () => 'token-de-prueba', idioma: () => 'es', alCerrarSesion: () => {} });

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
    return HttpResponse.json([]);
  }));
  return peticiones;
}

describe('cliente de la API de rango_recomendacion (generado)', () => {
  it('RF-13 · el cliente de la API de rango_recomendacion usa la ruta y el método de cada operación del JSON: las filas van todas juntas', async () => {
    const peticiones = anotarPeticiones();

    await api.listar(pedir);
    await api.editar(pedir, [{ dato: 'editado' }]);
    await api.restaurarPorDefecto(pedir);

    expect(peticiones).toEqual([
      { metodo: 'GET', ruta: '/api/v1/rangos-recomendacion', cuerpo: null },
      { metodo: 'PUT', ruta: '/api/v1/rangos-recomendacion', cuerpo: { filas: [{ dato: 'editado' }] } },
      { metodo: 'POST', ruta: '/api/v1/rangos-recomendacion/restaurar-por-defecto', cuerpo: null },
    ]);
  });
});
