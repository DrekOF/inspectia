/*
 * Nombre: compose.test.mjs
 * Descripción: Pruebas del docker-compose.yml de producción (D-16): las arma con «docker compose config», sin red y
 *              con valores de ejemplo, y revisan lo que pide el despliegue: solo el backend publicado (en el 80),
 *              imágenes de ECR con la etiqueta pedida, las variables de entorno del contrato, los secretos fuera del
 *              entorno, reinicio, límites de memoria, healthchecks y registros en CloudWatch Logs. Corren con Node
 *              y el Docker del equipo o de la CI: node --test infra/produccion/pruebas/compose.test.mjs.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { after, test } from 'node:test';
import { fileURLToPath } from 'node:url';

const COMPOSE = fileURLToPath(new URL('../docker-compose.yml', import.meta.url));
const EJEMPLO = fileURLToPath(new URL('./ejemplo.env', import.meta.url));
const CUENTA = '123456789012';
const ETIQUETA = '0123456789abcdef0123456789abcdef01234567';
const REGISTRO = `${CUENTA}.dkr.ecr.us-east-1.amazonaws.com`;

/** 4 GB de la t4g.medium, menos lo que necesitan el sistema, Docker y el agente de CloudWatch. */
const MEMORIA_PARA_CONTENEDORES = 3.25 * 1024 ** 3;

// Los secretos de ejemplo van en una carpeta temporal, como los de /run/inspectia/secretos en la instancia.
const SECRETOS = mkdtempSync(join(tmpdir(), 'inspectia-secretos-'));
writeFileSync(join(SECRETOS, 'POSTGRES_PASSWORD'), 'contrasena-de-ejemplo-no-es-real');
writeFileSync(join(SECRETOS, 'INSPECTIA_JWT_CLAVE'), 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=');
after(() => rmSync(SECRETOS, { recursive: true, force: true }));

/**
 * Corre «docker compose config» sobre el archivo de producción.
 *
 * @param {string[]} argumentos lo que va después de «config»
 * @param {object} [entorno] variables de entorno que se agregan o quitan (undefined la quita)
 * @returns {import('node:child_process').SpawnSyncReturns<string>} el resultado
 */
function config(argumentos, entorno = {}) {
  const env = { ...process.env, INSPECTIA_SECRETOS: SECRETOS, ...entorno };
  for (const [nombre, valor] of Object.entries(env)) {
    if (valor === undefined) delete env[nombre];
  }
  return spawnSync('docker', ['compose', '-f', COMPOSE, ...argumentos], { encoding: 'utf8', env });
}

const sinDocker = spawnSync('docker', ['compose', 'version'], { encoding: 'utf8' }).status !== 0;
const opciones = { skip: sinDocker && 'docker compose no está disponible' };

/** El compose ya armado, con los valores de ejemplo. */
let armado;
if (!sinDocker) {
  const resultado = config(['--env-file', EJEMPLO, 'config', '--format', 'json']);
  assert.equal(resultado.status, 0, resultado.stderr);
  armado = JSON.parse(resultado.stdout);
}
const servicio = (nombre) => armado.services[nombre];

test('D-16 · tiene los tres servicios: postgres, backend e ia-servicio', opciones, () => {
  assert.deepEqual(Object.keys(armado.services).sort(), ['backend', 'ia-servicio', 'postgres']);
  assert.equal(armado.name, 'inspectia-produccion');
});

test('D-16 · solo el backend se publica, en el puerto 80 del host, para CloudFront', opciones, () => {
  assert.deepEqual(servicio('backend').ports.map((p) => [String(p.published), p.target]), [['80', 8080]]);
  assert.equal(servicio('postgres').ports, undefined);
  assert.equal(servicio('ia-servicio').ports, undefined);
});

test('D-16 · el backend y el servicio de IA vienen de ECR con la etiqueta del commit', opciones, () => {
  assert.equal(servicio('backend').image, `${REGISTRO}/inspectia/backend:${ETIQUETA}`);
  assert.equal(servicio('ia-servicio').image, `${REGISTRO}/inspectia/ia-servicio:${ETIQUETA}`);
  assert.match(servicio('postgres').image, /^postgres:18(-alpine)?$/);
});

test('D-16 · el backend recibe las variables del contrato de producción', opciones, () => {
  assert.deepEqual(servicio('backend').environment, {
    SPRING_PROFILES_ACTIVE: 'produccion',
    INSPECTIA_ARCHIVOS_ALMACEN: 's3',
    INSPECTIA_ARCHIVOS_BUCKET: `inspectia-archivos-${CUENTA}`,
    AWS_REGION: 'us-east-1',
    INSPECTIA_IA_CLASIFICADOR: 'servicio',
    INSPECTIA_IA_URL: 'http://ia-servicio:8000',
    POSTGRES_HOST: 'postgres',
    POSTGRES_PORT: '5432',
    POSTGRES_DB: 'inspectia',
    POSTGRES_USER: 'inspectia',
  });
});

test('D-16 · el servicio de IA recibe el modelo elegido y dónde están los modelos', opciones, () => {
  assert.deepEqual(servicio('ia-servicio').environment, {
    INSPECTIA_MODELO: 'EfficientNet-B0',
    INSPECTIA_MODELOS_URI: `s3://inspectia-archivos-${CUENTA}/modelos/`,
    AWS_REGION: 'us-east-1',
  });
  assert.ok(servicio('ia-servicio').volumes.some((v) => v.target === '/var/lib/inspectia/modelos'));
});

test('D-16 · los secretos llegan como archivos de /run/secrets y nunca en el entorno', opciones, () => {
  const texto = JSON.stringify(armado);
  assert.ok(!texto.includes('contrasena-de-ejemplo-no-es-real'));
  assert.ok(!texto.includes('AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA='));
  assert.deepEqual(servicio('backend').secrets.map((s) => s.target).sort(), ['INSPECTIA_JWT_CLAVE', 'POSTGRES_PASSWORD']);
  assert.equal(servicio('postgres').environment.POSTGRES_PASSWORD_FILE, '/run/secrets/postgres_contrasena');
  assert.equal(servicio('postgres').environment.POSTGRES_PASSWORD, undefined);
  assert.equal(servicio('ia-servicio').secrets, undefined);
  for (const secreto of Object.values(armado.secrets)) {
    assert.ok(secreto.file.startsWith(SECRETOS), secreto.file);
  }
});

test('D-16 · en la instancia, los secretos se leen de /run/inspectia/secretos', opciones, () => {
  const resultado = config(['--env-file', EJEMPLO, 'config', '--no-path-resolution', '--format', 'json'],
    { INSPECTIA_SECRETOS: undefined });
  assert.equal(resultado.status, 0, resultado.stderr);
  for (const secreto of Object.values(JSON.parse(resultado.stdout).secrets)) {
    assert.match(secreto.file, /^\/run\/inspectia\/secretos\/(POSTGRES_PASSWORD|INSPECTIA_JWT_CLAVE)$/);
  }
});

test('D-16 · cada servicio reinicia solo y escribe sus registros en CloudWatch Logs', opciones, () => {
  for (const [nombre, definicion] of Object.entries(armado.services)) {
    assert.equal(definicion.restart, 'on-failure', nombre);
    assert.equal(definicion.logging.driver, 'awslogs', nombre);
    assert.equal(definicion.logging.options['awslogs-region'], 'us-east-1', nombre);
    assert.match(definicion.logging.options['awslogs-group'], /^\/inspectia\//, nombre);
  }
});

test('D-16 · los límites de memoria caben en la t4g.medium de 4 GB', opciones, () => {
  let total = 0;
  for (const [nombre, definicion] of Object.entries(armado.services)) {
    const limite = Number(definicion.deploy?.resources?.limits?.memory);
    assert.ok(limite > 0, `${nombre} sin límite de memoria`);
    total += limite;
  }
  assert.ok(total <= MEMORIA_PARA_CONTENEDORES, `los límites suman ${total} bytes`);
});

test('D-16 · el backend espera a la base y al servicio de IA sanos, y los dos tienen healthcheck', opciones, () => {
  const dependencias = servicio('backend').depends_on;
  assert.equal(dependencias.postgres.condition, 'service_healthy');
  assert.equal(dependencias['ia-servicio'].condition, 'service_healthy');
  assert.ok(servicio('postgres').healthcheck.test.join(' ').includes('pg_isready'));
  assert.ok(servicio('ia-servicio').healthcheck.test.join(' ').includes('/salud'));
});

test('D-16 · sin la cuenta o sin la etiqueta, el compose no se arma', opciones, () => {
  const sinValores = { AWS_CUENTA: undefined, ETIQUETA: undefined };
  const resultado = config(['config', '--quiet'], sinValores);
  assert.notEqual(resultado.status, 0);
  assert.match(resultado.stderr, /AWS_CUENTA|ETIQUETA/);
});
