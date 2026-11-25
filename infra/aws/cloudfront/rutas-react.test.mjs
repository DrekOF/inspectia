/*
 * Nombre: rutas-react.test.mjs
 * Descripción: Pruebas de la CloudFront Function de las rutas de React (D-16, D-31): una ruta de la página va a
 *              /index.html, un archivo con extensión llega tal cual y /api/ nunca se toca. Corren con Node, sin
 *              dependencias: node --test infra/aws/cloudfront/rutas-react.test.mjs (o bash infra/probar.sh).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';

/** Código de la función, tal como se pega en la consola de CloudFront. */
const CODIGO = readFileSync(new URL('./rutas-react.js', import.meta.url), 'utf8');

/**
 * La función `handler` del archivo. CloudFront la busca por su nombre: el archivo no exporta nada, así que se
 * evalúa aquí como lo haría su entorno.
 */
const handler = new Function(`${CODIGO}\nreturn handler;`)();

/**
 * Arma un evento de «viewer request» como el de CloudFront (cloudfront-js-2.0) y se lo pasa a la función.
 *
 * @param {string} uri la ruta pedida, sin la consulta
 * @param {object} [querystring] la consulta, como la entrega CloudFront
 * @returns {object} la petición que devuelve la función
 */
function pedir(uri, querystring = {}) {
  return handler({
    version: '1.0',
    context: { eventType: 'viewer-request', distributionId: 'EDISTRIBUCION' },
    viewer: { ip: '198.51.100.7' },
    request: { method: 'GET', uri, querystring, headers: { host: { value: 'd111.cloudfront.net' } }, cookies: {} },
  });
}

test('D-16 · una ruta de React va a /index.html', () => {
  for (const ruta of ['/lotes/12', '/lotes/12/fotos', '/acerca-de', '/cambiar-contrasena', '/administracion/empresas']) {
    assert.equal(pedir(ruta).uri, '/index.html', ruta);
  }
});

test('D-16 · la raíz y una ruta con «/» al final van a /index.html', () => {
  assert.equal(pedir('/').uri, '/index.html');
  assert.equal(pedir('/lotes/12/').uri, '/index.html');
});

test('D-16 · un archivo con extensión llega tal cual', () => {
  for (const archivo of ['/index.html', '/favicon.svg', '/assets/index-Bx81c2Qe.js', '/assets/material-ui-D4f0.css']) {
    assert.equal(pedir(archivo).uri, archivo, archivo);
  }
});

test('D-16 · un archivo que falta no se convierte en la página', () => {
  assert.equal(pedir('/assets/no-existe-1234.js').uri, '/assets/no-existe-1234.js');
});

test('D-16 · nunca toca /api/: sus 404 siguen siendo de la API', () => {
  for (const ruta of ['/api/', '/api/v1/sesion', '/api/v1/lotes/12/reporte', '/api/v1/no-existe', '/api']) {
    assert.equal(pedir(ruta).uri, ruta, ruta);
  }
});

test('D-16 · solo el último tramo decide si es un archivo', () => {
  assert.equal(pedir('/v1.2/lotes').uri, '/index.html');
  assert.equal(pedir('/apis/lotes').uri, '/index.html');
});

test('D-16 · conserva la consulta, los encabezados y el método', () => {
  const consulta = { idioma: { value: 'en' } };
  const peticion = pedir('/lotes/12', consulta);
  assert.equal(peticion.uri, '/index.html');
  assert.equal(peticion.querystring, consulta);
  assert.equal(peticion.method, 'GET');
  assert.equal(peticion.headers.host.value, 'd111.cloudfront.net');
});

test('D-16 · cabe en CloudFront Functions (cloudfront-js-2.0, menos de 10 KB y sin módulos)', () => {
  assert.ok(Buffer.byteLength(CODIGO, 'utf8') < 10 * 1024);
  assert.doesNotMatch(CODIGO, /\brequire\s*\(|^\s*import\s|module\.exports|\bexport\s/m);
  assert.match(CODIGO, /^function handler\(event\)/m);
});
