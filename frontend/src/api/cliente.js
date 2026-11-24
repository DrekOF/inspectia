/*
 * Nombre: cliente.js
 * Descripción: Cliente de la API del backend (D-28, D-31): pide en JSON a /api/v1 en el mismo origen, con el
 *              token de la sesión y el idioma de la app, y convierte los errores application/problem+json en
 *              un ErrorDeApi con sus campos. Un 401 de una petición con sesión la cierra (RNF-04). También baja un
 *              archivo con la sesión, como el reporte PDF del lote, con su nombre de Content-Disposition (RF-15).
 * Fecha de creación: 20/10/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
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
 * El nombre de archivo de un encabezado Content-Disposition: el de filename* (RFC 5987, en UTF-8) si viene, y si no,
 * el de filename.
 *
 * @param {string|null} encabezado el encabezado
 * @returns {string|null} el nombre, o null si no trae
 */
export function nombreDelArchivo(encabezado) {
  if (!encabezado) {
    return null;
  }
  const extendido = /filename\*\s*=\s*([^']*)'[^']*'([^;]+)/i.exec(encabezado);
  if (extendido) {
    try {
      return decodeURIComponent(extendido[2].trim());
    } catch {
      // Un nombre mal codificado: se usa el de filename.
    }
  }
  const simple = /filename\s*=\s*("((?:[^"\\]|\\.)*)"|[^;]+)/i.exec(encabezado);
  if (!simple) {
    return null;
  }
  return simple[2] === undefined ? simple[1].trim() : simple[2].replace(/\\(.)/g, '$1');
}

/**
 * @typedef {object} OpcionesDePeticion opciones de una petición
 * @property {boolean} [comoArchivo] si la respuesta es un archivo, como un PDF: la petición devuelve
 *   { archivo: Blob, nombre: string|null } en lugar del JSON
 */

/**
 * @typedef {(metodo: string, ruta: string, datos?: object, opciones?: OpcionesDePeticion) => Promise<object|null>}
 *   Pedir la función para pedir a la API: el método HTTP, la ruta después de /api/v1, los datos que van en el cuerpo
 *   y sus opciones; lanza ErrorDeApi si falla
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
  return async function pedir(metodo, ruta, datos, { comoArchivo = false } = {}) {
    const tokenUsado = token();
    // Un archivo se pide en su tipo o, si falla, en el JSON de los errores (application/problem+json).
    const encabezados = {
      Accept: comoArchivo ? '*/*' : 'application/json',
      'Accept-Language': idioma(),
    };
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

    if (respuesta.ok && comoArchivo) {
      return {
        archivo: await respuesta.blob(),
        nombre: nombreDelArchivo(respuesta.headers.get('Content-Disposition')),
      };
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
