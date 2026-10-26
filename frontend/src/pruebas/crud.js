/*
 * Nombre: crud.js
 * Descripción: API simulada (MSW) de una entidad con pantalla generada, para las pruebas: guarda los registros en
 *              memoria y responde como la API generada: el listado por páginas y con el filtro activo (D-33),
 *              registrar, editar, desactivar y reactivar. Anota cada petición.
 * Fecha de creación: 26/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { http, HttpResponse } from 'msw';
import { servidor } from './servidor.js';

/**
 * @typedef {object} Peticion una petición que recibió la API simulada
 * @property {string} metodo el método HTTP
 * @property {string} ruta la ruta, con sus parámetros, como /api/v1/empresas?pagina=0&tamano=20
 * @property {object|null} cuerpo el cuerpo en JSON, o null si no tiene
 */

/**
 * @typedef {object} ApiSimulada lo que tiene la API simulada y lo que le pidieron
 * @property {object[]} registros los registros, como quedan después de cada petición
 * @property {Peticion[]} peticiones las peticiones, en orden
 */

/** Cuántos registros ha creado la API simulada, para darle a cada uno un identificador distinto. */
let creados = 0;

/**
 * Los datos de un registro, sin los grupos de crear_con, como el primer administrador: la API no los responde.
 *
 * @param {object} cuerpo lo que se envió
 * @returns {object} los datos del registro
 */
function sinGrupos(cuerpo) {
  return Object.fromEntries(Object.entries(cuerpo).filter(([, valor]) => valor === null || typeof valor !== 'object'));
}

/**
 * Simula en memoria la API generada de una entidad: lo que le pidan cambia sus registros, como en el backend.
 *
 * @param {string} ruta la ruta de la API, como /api/v1/empresas
 * @param {object[]} registros los registros iniciales, como los responde la API
 * @param {object} [opciones] cómo es la entidad
 * @param {string|null} [opciones.atributoActivo] el atributo que cambian desactivar y reactivar, como activa
 * @returns {ApiSimulada} sus registros y las peticiones que recibe
 */
export function apiSimulada(ruta, registros, { atributoActivo = null } = {}) {
  const simulada = { registros: structuredClone(registros), peticiones: [] };

  /**
   * Anota una petición, con su cuerpo.
   *
   * @param {Request} peticion la petición
   */
  const anotar = async (peticion) => {
    const direccion = new URL(peticion.url);
    const texto = await peticion.clone().text();
    simulada.peticiones.push({ metodo: peticion.method, ruta: direccion.pathname + direccion.search,
      cuerpo: texto ? JSON.parse(texto) : null });
  };

  /**
   * La respuesta de un registro que no existe, como la de la API.
   *
   * @returns {Response} la respuesta 404
   */
  const noExiste = () => HttpResponse.json({ status: 404, detail: 'El registro no existe.' }, { status: 404 });

  servidor.use(
    http.get(ruta, async ({ request }) => {
      await anotar(request);
      const parametros = new URL(request.url).searchParams;
      const pagina = Number(parametros.get('pagina') ?? 0);
      const tamano = Number(parametros.get('tamano') ?? 20);
      const activo = parametros.get('activo');
      const filtrados = activo === null || !atributoActivo ? simulada.registros
        : simulada.registros.filter((registro) => String(registro[atributoActivo]) === activo);
      return HttpResponse.json({
        contenido: filtrados.slice(pagina * tamano, (pagina + 1) * tamano),
        pagina,
        tamano,
        totalElementos: filtrados.length,
        totalPaginas: Math.ceil(filtrados.length / tamano),
      });
    }),
    http.post(ruta, async ({ request }) => {
      await anotar(request);
      creados += 1;
      const nuevo = { id: `0199e2a0-0000-7000-8000-${String(creados).padStart(12, '0')}`, ...sinGrupos(await request.json()) };
      if (atributoActivo) {
        nuevo[atributoActivo] = true;
      }
      simulada.registros.push(nuevo);
      return HttpResponse.json(nuevo, { status: 201 });
    }),
    http.put(`${ruta}/:id`, async ({ request, params }) => {
      await anotar(request);
      const registro = simulada.registros.find(({ id }) => id === params.id);
      if (!registro) {
        return noExiste();
      }
      Object.assign(registro, await request.json());
      return HttpResponse.json(registro);
    }),
    http.post(`${ruta}/:id/:accion`, async ({ request, params }) => {
      await anotar(request);
      const registro = simulada.registros.find(({ id }) => id === params.id);
      if (!registro || !['desactivar', 'reactivar'].includes(params.accion)) {
        return noExiste();
      }
      registro[atributoActivo] = params.accion === 'reactivar';
      return HttpResponse.json(registro);
    }),
  );
  return simulada;
}
