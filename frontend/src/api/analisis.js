/*
 * Nombre: analisis.js (api)
 * Descripción: Las peticiones del análisis de un lote: enviar la muestra en curso a la cola (RF-05 C2, RNF-02 C3),
 *              consultar su avance mientras espera o se procesa (RNF-01 C4) y traer el resultado guardado del lote,
 *              con sus muestras, sus fotos y los granos marcados de cada una (RF-10, RF-11, RF-12, RF-14 C3), y el
 *              límite cercano de la sugerencia de otra muestra (D-47). La muestra en curso puede ser la segunda (D-25).
 *              También baja el reporte PDF del lote con la sesión (RF-15).
 *              El avance y la muestra dicen por qué volvió sin resultado, según el servicio de IA (D-44, RF-09).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.3
 */

/** @typedef {'PENDIENTE'|'EN_ESPERA'|'EN_PROCESO'|'TERMINADA'} EstadoDeMuestra el estado de una muestra */

/**
 * @typedef {object} AvanceDelAnalisis el avance de la muestra en curso de un lote, sin enlaces ni granos
 * @property {number|null} muestra el número de la muestra en curso; null si el lote no tiene muestra
 * @property {EstadoDeMuestra|null} estado su estado; null si el lote no tiene muestra
 * @property {string|null} fechaEnvio cuándo se envió a la cola (ISO 8601)
 * @property {string|null} fechaInicio cuándo empezó a procesarse (ISO 8601)
 * @property {string|null} fechaFin cuándo terminó (ISO 8601)
 * @property {boolean} fallo si el último envío volvió sin resultado: la muestra quedó pendiente otra vez
 * @property {'POCOS_GRANOS'|'SIN_RESPUESTA_DE_LA_IA'|null} [motivoDevolucion] por qué volvió (D-44); null si la
 *   devolvió una foto o fue otra falla
 * @property {number} [repeticiones] cuántas fotos pidió repetir RF-09 por granos dudosos, de 0 a 2
 * @property {{ numero: number, motivo: string }[]} [fotosPorRepetir] las fotos «Por repetir» de la muestra
 */

/**
 * @typedef {object} GranoMarcado un grano de una foto, en píxeles de la foto original, sin su confianza (RF-11 C4)
 * @property {number} x el borde izquierdo
 * @property {number} y el borde de arriba
 * @property {number} ancho el ancho
 * @property {number} alto el alto
 * @property {'SANO'|'NO_SANO'} clase su clase
 * @property {boolean} dudoso si su confianza quedó por debajo del umbral de su muestra (D-42, P-15)
 */

/**
 * @typedef {object} FotoMarcada una foto de una muestra con sus granos
 * @property {number} numero su número, de 1 a 3
 * @property {import('./fotos.js').EnlaceTemporal} vista el enlace para verla (D-41)
 * @property {GranoMarcado[]} granos sus granos, en el orden en que se guardaron; vacío mientras no termina
 */

/**
 * @typedef {object} MuestraDelLote una muestra del lote con sus cifras y sus fotos
 * @property {number} numero su número, 1 o 2 (D-25)
 * @property {EstadoDeMuestra} estado su estado
 * @property {{ id: string, nombre: string }|null} analizadaPor quién la envió
 * @property {{ nombre: string, version: string }|null} modelo el modelo que la analizó; null mientras no termina
 * @property {number|null} totalGranos los granos que contó el modelo
 * @property {number|null} granosSanos los que marcó como sanos
 * @property {number|null} porcentajeContado el porcentaje contado, con 1 decimal
 * @property {string|null} fechaEnvio cuándo se envió (ISO 8601)
 * @property {string|null} fechaFin cuándo terminó (ISO 8601)
 * @property {FotoMarcada[]} fotos sus fotos
 * @property {'POCOS_GRANOS'|'SIN_RESPUESTA_DE_LA_IA'|null} [motivoDevolucion] por qué volvió sin resultado (D-44)
 * @property {number} [repeticiones] cuántas fotos pidió repetir RF-09 por granos dudosos, de 0 a 2
 * @property {{ numero: number, motivo: string }[]} [fotosPorRepetir] sus fotos «Por repetir»
 * @property {boolean|null} [avisoConfianzaBaja] si salió con confianza baja (RF-09 C4); null mientras no termina
 * @property {number|null} [porcentajeMaxDudosos] el máximo de dudosos que usó (P-15), con 1 decimal
 */

/**
 * @typedef {object} LoteConResultado los datos y el resultado guardado de un lote; los del resultado van en null
 *   mientras el lote está pendiente
 * @property {string} id su identificador
 * @property {string} codigo su código
 * @property {string} fechaRecepcion la fecha de recepción (aaaa-mm-dd)
 * @property {string} origen la finca o el proveedor
 * @property {'PENDIENTE'|'ANALIZADO'} estado su estado
 * @property {string|null} fechaResultado la fecha del resultado (ISO 8601)
 * @property {number|null} porcentajeSanos el porcentaje corregido, con 1 decimal (D-39)
 * @property {number|null} margen el margen del porcentaje, con 1 decimal (P-19, D-43)
 * @property {string|null} categoria la categoría probable, como ESTANDAR
 * @property {string|null} recomendacion el texto de la recomendación, como se guardó
 * @property {boolean|null} sugerirOtraMuestra si sugiere analizar otra muestra (D-24); con 2 muestras, nunca (D-25)
 * @property {number|null} limiteSugerencia el límite de la categoría más cercano al porcentaje, el que hizo sugerir
 *   otra muestra, como se guardó (D-47); null sin la sugerencia
 * @property {string|null} categoriaDelLimite la categoría que empieza en ese límite, como ESTANDAR, que puede ser
 *   otra que la del lote; null sin la sugerencia
 * @property {boolean|null} avisoConfianzaBaja si alguna muestra quedó con confianza baja (RF-09)
 * @property {number|null} totalGranos los granos de sus muestras terminadas
 * @property {number|null} granosSanos los marcados como sanos en sus muestras terminadas
 * @property {number|null} porcentajeContado el porcentaje contado, con 1 decimal
 */

/** @typedef {{ lote: LoteConResultado, muestras: MuestraDelLote[] }} ResultadoDelLote el resultado y el detalle */

/**
 * Envía la muestra en curso a la cola del análisis (POST /api/v1/lotes/{id}/analisis). Con menos de 2 fotos, la API
 * responde 400 con el mensaje de qué hacer; si la muestra ya estaba en la cola, responde igual que la primera vez.
 *
 * @param {import('./cliente.js').Pedir} pedir la función del cliente de la API
 * @param {string} loteId el lote
 * @returns {Promise<AvanceDelAnalisis>} el avance, con la muestra en espera
 */
export function enviarMuestra(pedir, loteId) {
  return pedir('POST', `/lotes/${encodeURIComponent(loteId)}/analisis`);
}

/**
 * El avance del análisis de la muestra en curso (GET /api/v1/lotes/{id}/analisis). Es una consulta liviana, para
 * repetirla mientras la muestra espera o se procesa.
 *
 * @param {import('./cliente.js').Pedir} pedir la función del cliente de la API
 * @param {string} loteId el lote
 * @returns {Promise<AvanceDelAnalisis>} el avance
 */
export function avanceDelAnalisis(pedir, loteId) {
  return pedir('GET', `/lotes/${encodeURIComponent(loteId)}/analisis`);
}

/**
 * El resultado guardado de un lote y el detalle de sus muestras (GET /api/v1/lotes/{id}/resultado), con enlaces
 * nuevos para ver sus fotos. Si el lote no existe o es de otra empresa, la API responde 404 (RNF-03).
 *
 * @param {import('./cliente.js').Pedir} pedir la función del cliente de la API
 * @param {string} loteId el lote
 * @returns {Promise<ResultadoDelLote>} el resultado
 */
export function resultadoDelLote(pedir, loteId) {
  return pedir('GET', `/lotes/${encodeURIComponent(loteId)}/resultado`);
}

/**
 * @typedef {object} ArchivoDescargado un archivo que bajó la API
 * @property {Blob} archivo su contenido
 * @property {string|null} nombre su nombre, de Content-Disposition
 */

/**
 * El reporte PDF de un lote analizado, en un idioma (GET /api/v1/lotes/{id}/reporte?idioma=es|en, RF-15). Se pide con
 * la sesión, como todo lo demás: el PDF no tiene un enlace público. La primera vez el servidor lo arma y lo guarda;
 * después entrega el mismo. Un lote pendiente responde 409, y uno de otra empresa, 404 (RNF-03).
 *
 * @param {import('./cliente.js').Pedir} pedir la función del cliente de la API
 * @param {string} loteId el lote
 * @param {'es'|'en'} idioma el idioma del reporte, el de la página
 * @returns {Promise<ArchivoDescargado>} el PDF y su nombre, como InspectIA-L-0143-es.pdf
 */
export function reporteDelLote(pedir, loteId, idioma) {
  return pedir('GET', `/lotes/${encodeURIComponent(loteId)}/reporte?idioma=${encodeURIComponent(idioma)}`,
    undefined, { comoArchivo: true });
}
