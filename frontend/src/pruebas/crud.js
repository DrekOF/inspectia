/*
 * Nombre: crud.js
 * Descripción: API simulada (MSW) de una entidad con pantalla generada, para las pruebas: guarda los registros en
 *              memoria y responde como la API generada: el listado por páginas y con el filtro activo (D-33),
 *              registrar, editar, desactivar y reactivar. La de una entidad que se edita en bloque, como los rangos
 *              (RF-13), lista sus filas, las edita todas juntas y vuelve a las de por defecto. Anota cada petición.
 * Fecha de creación: 26/10/2026
 * Actualización: 28/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
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

/**
 * @typedef {object} ApiEnBloqueSimulada lo que tiene la API simulada de una entidad en bloque y lo que le pidieron
 * @property {object[]} filas las filas, como quedan después de cada petición
 * @property {Peticion[]} peticiones las peticiones, en orden
 */

/** Cuántos registros ha creado la API simulada, para darle a cada uno un identificador distinto. */
let creados = 0;

/**
 * Anota una petición en una API simulada, con su cuerpo.
 *
 * @param {{ peticiones: Peticion[] }} simulada la API simulada
 * @param {Request} peticion la petición
 */
async function anotar(simulada, peticion) {
  const direccion = new URL(peticion.url);
  const texto = await peticion.clone().text();
  simulada.peticiones.push({ metodo: peticion.method, ruta: direccion.pathname + direccion.search,
    cuerpo: texto ? JSON.parse(texto) : null });
}

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
   * La respuesta de un registro que no existe, como la de la API.
   *
   * @returns {Response} la respuesta 404
   */
  const noExiste = () => HttpResponse.json({ status: 404, detail: 'El registro no existe.' }, { status: 404 });

  servidor.use(
    http.get(ruta, async ({ request }) => {
      await anotar(simulada, request);
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
      await anotar(simulada, request);
      creados += 1;
      const nuevo = { id: `0199e2a0-0000-7000-8000-${String(creados).padStart(12, '0')}`, ...sinGrupos(await request.json()) };
      if (atributoActivo) {
        nuevo[atributoActivo] = true;
      }
      simulada.registros.push(nuevo);
      return HttpResponse.json(nuevo, { status: 201 });
    }),
    http.put(`${ruta}/:id`, async ({ request, params }) => {
      await anotar(simulada, request);
      const registro = simulada.registros.find(({ id }) => id === params.id);
      if (!registro) {
        return noExiste();
      }
      Object.assign(registro, await request.json());
      return HttpResponse.json(registro);
    }),
    http.post(`${ruta}/:id/:accion`, async ({ request, params }) => {
      await anotar(simulada, request);
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

/**
 * Simula en memoria la API generada de una entidad que se edita en bloque, como los rangos (RF-13): lista sus filas,
 * las edita todas juntas y vuelve a las de por defecto. Responde las filas como quedan, igual que el backend.
 *
 * @param {string} ruta la ruta de la API, como /api/v1/rangos-recomendacion
 * @param {object[]} filas las filas iniciales, como las responde la API
 * @param {object[]} [porDefecto] las filas a las que vuelve al restaurar; por defecto, las iniciales
 * @returns {ApiEnBloqueSimulada} sus filas y las peticiones que recibe
 */
export function apiEnBloqueSimulada(ruta, filas, porDefecto = filas) {
  const simulada = { filas: structuredClone(filas), peticiones: [] };

  servidor.use(
    http.get(ruta, async ({ request }) => {
      await anotar(simulada, request);
      return HttpResponse.json(simulada.filas);
    }),
    http.put(ruta, async ({ request }) => {
      await anotar(simulada, request);
      simulada.filas = (await request.json()).filas;
      return HttpResponse.json(simulada.filas);
    }),
    http.post(`${ruta}/restaurar-por-defecto`, async ({ request }) => {
      await anotar(simulada, request);
      simulada.filas = structuredClone(porDefecto);
      return HttpResponse.json(simulada.filas);
    }),
  );
  return simulada;
}
