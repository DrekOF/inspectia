/*
 * Nombre: rutas-react.js
 * Descripción: CloudFront Function «inspectia-rutas» (cloudfront-js-2.0, evento viewer request) del comportamiento
 *              por defecto de la distribución (D-16, D-31). La página es una app de React con rutas propias, como
 *              /lotes/12: S3 no tiene ese archivo, así que la función le pide /index.html y React muestra la
 *              pantalla. Lo que termina en un archivo con extensión (/assets/index-abc.js, /favicon.svg) llega tal
 *              cual, y /api/ nunca se toca: así un 404 de la API sigue siendo un 404 en JSON y no la página, como
 *              pasaría con las páginas de error de CloudFront. Se pega tal cual en la consola de CloudFront.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */

/**
 * Cambia la ruta de una pantalla de React por /index.html.
 *
 * @param {object} event el evento de CloudFront, con la petición en event.request
 * @returns {object} la petición, con la ruta cambiada solo si es una pantalla de React
 */
function handler(event) {
    var request = event.request;
    var uri = request.uri;

    // La API va a la instancia por su propio comportamiento (/api/*); esto es solo por si alguien le asocia la
    // función: la ruta de la API nunca se cambia.
    if (uri === '/api' || uri.indexOf('/api/') === 0) {
        return request;
    }

    // Un archivo: su último tramo tiene un punto, como index-abc.js o favicon.svg.
    var ultimoTramo = uri.substring(uri.lastIndexOf('/') + 1);
    if (ultimoTramo.indexOf('.') !== -1) {
        return request;
    }

    request.uri = '/index.html';
    return request;
}
