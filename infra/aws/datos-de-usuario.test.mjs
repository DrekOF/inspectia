/*
 * Nombre: datos-de-usuario.test.mjs
 * Descripción: Pruebas del script de arranque de la instancia (D-16): revisan sin red lo que deja escrito, como la
 *              configuración del agente de CloudWatch, los servicios de systemd y el temporizador de la copia diaria.
 *              Corren con Node, sin dependencias: node --test infra/aws/datos-de-usuario.test.mjs.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';

const SCRIPT = readFileSync(new URL('./datos-de-usuario.sh', import.meta.url), 'utf8');

/**
 * El texto de un heredoc del script, entre «<<'MARCA'» y la línea «MARCA».
 *
 * @param {string} marca el delimitador del heredoc
 * @returns {string} su contenido
 */
function heredoc(marca) {
  const inicio = SCRIPT.indexOf(`<<'${marca}'\n`);
  assert.notEqual(inicio, -1, `falta el heredoc ${marca}`);
  const desde = SCRIPT.indexOf('\n', inicio) + 1;
  const hasta = SCRIPT.indexOf(`\n${marca}\n`, desde);
  assert.notEqual(hasta, -1, `el heredoc ${marca} no se cierra`);
  return SCRIPT.slice(desde, hasta);
}

test('D-16 · es un script de bash que se detiene ante el primer error', () => {
  assert.match(SCRIPT, /^#!\/bin\/bash\n/);
  assert.match(SCRIPT, /^set -euo pipefail$/m);
});

test('D-16 · instala Docker, su plugin compose verificado con SHA-256 y el agente de CloudWatch', () => {
  assert.match(SCRIPT, /dnf install -y [^\n]*\bdocker\b/);
  assert.match(SCRIPT, /dnf install -y [^\n]*\bamazon-cloudwatch-agent\b/);
  assert.match(SCRIPT, /docker-compose-linux-/);
  assert.match(SCRIPT, /sha256sum -c/);
  assert.match(SCRIPT, /cli-plugins\/docker-compose/);
});

test('D-16 · crea 2 GB de swap que sobreviven al reinicio', () => {
  assert.match(SCRIPT, /SWAP_MB=2048/);
  assert.match(SCRIPT, /mkswap/);
  assert.match(SCRIPT, /\/swapfile none swap/);
});

test('D-16 · el agente de CloudWatch mide memoria, disco y swap, y envía los registros del servidor', () => {
  const configuracion = JSON.parse(heredoc('JSON_CLOUDWATCH'));
  const metricas = configuracion.metrics.metrics_collected;
  assert.deepEqual(metricas.mem.measurement, ['mem_used_percent']);
  assert.deepEqual(metricas.disk.measurement, ['used_percent']);
  assert.deepEqual(metricas.disk.resources, ['/']);
  assert.deepEqual(metricas.swap.measurement, ['swap_used_percent']);
  assert.equal(configuracion.metrics.append_dimensions.InstanceId, '${aws:InstanceId}');
  assert.equal(configuracion.agent.omit_hostname, true);
  const archivos = configuracion.logs.logs_collected.files.collect_list;
  const rutas = archivos.map((a) => a.file_path);
  assert.ok(rutas.includes('/var/log/inspectia/actualizar.log'));
  assert.ok(rutas.includes('/var/log/inspectia/respaldo.log'));
  for (const archivo of archivos) {
    assert.equal(archivo.log_group_name, '/inspectia/servidor');
    assert.ok(archivo.retention_in_days > 0);
  }
});

test('D-16 · un servicio de systemd levanta InspectIA al encender, después de Docker y si ya hubo despliegue', () => {
  const unidad = heredoc('UNIDAD_INSPECTIA');
  assert.match(unidad, /^Requires=docker\.service$/m);
  assert.match(unidad, /^After=[^\n]*docker\.service/m);
  assert.match(unidad, /^ConditionPathExists=\/opt\/inspectia\/\.env$/m);
  assert.match(unidad, /^ExecStart=\/opt\/inspectia\/actualizar\.sh$/m);
  assert.match(unidad, /^ExecStop=[^\n]*docker compose[^\n]* stop$/m);
  assert.match(unidad, /^WantedBy=multi-user\.target$/m);
  assert.match(SCRIPT, /systemctl enable inspectia\.service/);
});

test('D-16 · la copia diaria de la base corre con un temporizador, también si la instancia estaba apagada', () => {
  const servicio = heredoc('UNIDAD_RESPALDO');
  assert.match(servicio, /^ExecStart=\/opt\/inspectia\/respaldar\.sh$/m);
  assert.match(servicio, /^After=inspectia\.service$/m);
  const temporizador = heredoc('TEMPORIZADOR_RESPALDO');
  assert.match(temporizador, /^OnCalendar=\*-\*-\* \d\d:\d\d:00 America\/Bogota$/m);
  assert.match(temporizador, /^Persistent=true$/m);
  assert.match(SCRIPT, /systemctl enable --now inspectia-respaldo\.timer/);
});

test('D-16 · deja las carpetas del despliegue, de los registros y de los respaldos', () => {
  for (const carpeta of ['/opt/inspectia', '/var/log/inspectia', '/var/lib/inspectia/respaldos']) {
    assert.ok(SCRIPT.includes(carpeta), carpeta);
  }
});
