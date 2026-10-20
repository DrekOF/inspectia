/*
 * Nombre: cliente.js
 * Descripción: Cliente de la API del backend (D-28, D-31): pide en JSON a /api/v1 en el mismo origen, con el
 *              token de la sesión y el idioma de la app, y convierte los errores application/problem+json en
 *              un ErrorDeApi con sus campos. Un 401 de una petición con sesión la cierra (RNF-04).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */

/** Ruta base de la API: el proxy de Vite en desarrollo y CloudFront en AWS la llevan al backend (D-31). */
const BASE = '/api/v1';

/**
 * Error de una petición a la API. Con estado 0, la petición no llegó al servidor (sin conexión).
 */
export class ErrorDeApi extends Error {
  /**
   * @param {object} datos los datos del error
   * @param {number} datos.estado el código HTTP, o 0 si no hubo respuesta
   * @param {string|null} [datos.detalle] el mensaje del backend (detail), ya en el idioma pedido
   * @param {Array<{ campo: string, mensaje: string }>} [datos.errores] los errores de cada campo
   * @param {string|null} [datos.ayuda] la ayuda del backend, como la regla del bloqueo
   * @param {string|null} [datos.bloqueadoHasta] cuándo termina el bloqueo de la cuenta (ISO 8601)
   * @param {number|null} [datos.reintentarEnSegundos] los segundos de Retry-After
   */
  constructor({ estado, detalle = null, errores = [], ayuda = null, bloqueadoHasta = null,
    reintentarEnSegundos = null }) {
    super(detalle ?? `La API respondió ${estado}`);
    this.name = 'ErrorDeApi';
    this.estado = estado;
    this.detalle = detalle;
    this.errores = errores;
    this.ayuda = ayuda;
    this.bloqueadoHasta = bloqueadoHasta;
    this.reintentarEnSegundos = reintentarEnSegundos;
  }
}

/**
 * El cuerpo de una respuesta en JSON, o null si viene vacío o no es JSON (como la página de un proxy caído).
 *
 * @param {Response} respuesta la respuesta
 * @returns {Promise<object|null>} el cuerpo
 */
async function cuerpoDe(respuesta) {
  const texto = await respuesta.text();
  try {
    return texto ? JSON.parse(texto) : null;
  } catch {
    return null;
  }
}

/**
 * Los segundos del encabezado Retry-After, o null si no viene.
 *
 * @param {string|null} valor el encabezado
 * @returns {number|null} los segundos
 */
function segundos(valor) {
  const numero = Number(valor);
  return valor !== null && Number.isFinite(numero) ? numero : null;
}

/**
 * @typedef {(metodo: string, ruta: string, datos?: object) => Promise<object|null>} Pedir la función para pedir
 *   a la API: el método HTTP, la ruta después de /api/v1 y los datos que van en el cuerpo; lanza ErrorDeApi
 *   si falla
 */

/**
 * Crea el cliente de la API.
 *
 * @param {object} opciones de dónde salen el token y el idioma, y qué hacer con un 401
 * @param {() => string|null} opciones.token el token de la sesión actual, o null sin sesión
 * @param {() => string} opciones.idioma el idioma de la app, como «es», para Accept-Language
 * @param {(mensaje: string|null) => void} opciones.alCerrarSesion se llama con el mensaje del backend cuando
 *   una petición con sesión recibe 401: la sesión ya no sirve (venció, o la contraseña cambió)
 * @returns {Pedir} la función para pedir
 */
export function crearCliente({ token, idioma, alCerrarSesion }) {
  return async function pedir(metodo, ruta, datos) {
    const tokenUsado = token();
    const encabezados = { Accept: 'application/json', 'Accept-Language': idioma() };
    if (tokenUsado) {
      encabezados.Authorization = `Bearer ${tokenUsado}`;
    }
    if (datos !== undefined) {
      encabezados['Content-Type'] = 'application/json';
    }

    let respuesta;
    try {
      respuesta = await fetch(new URL(BASE + ruta, window.location.origin), {
        method: metodo,
        headers: encabezados,
        body: datos === undefined ? undefined : JSON.stringify(datos),
      });
    } catch {
      throw new ErrorDeApi({ estado: 0 });
    }

    const cuerpo = await cuerpoDe(respuesta);
    if (respuesta.ok) {
      return cuerpo;
    }
    const error = new ErrorDeApi({
      estado: respuesta.status,
      detalle: cuerpo?.detail ?? null,
      errores: cuerpo?.errores ?? [],
      ayuda: cuerpo?.ayuda ?? null,
      bloqueadoHasta: cuerpo?.bloqueadoHasta ?? null,
      reintentarEnSegundos: segundos(respuesta.headers.get('Retry-After')),
    });
    // Solo cierra la sesión con la que se pidió: si el token cambió mientras tanto, como al cambiar la
    // contraseña, el 401 es del token viejo y la sesión nueva sigue.
    if (respuesta.status === 401 && tokenUsado && token() === tokenUsado) {
      alCerrarSesion(error.detalle);
    }
    throw error;
  };
}
