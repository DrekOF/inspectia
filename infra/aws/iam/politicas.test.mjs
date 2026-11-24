/*
 * Nombre: politicas.test.mjs
 * Descripción: Pruebas de las políticas IAM del despliegue (D-16): que el rol de GitHub solo se asuma desde main
 *              de DrekOF/inspectia y que cada rol tenga solo lo que usa, sobre sus recursos. Corren con Node, sin
 *              dependencias: node --test infra/aws/iam/politicas.test.mjs (o bash infra/probar.sh).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';

/**
 * Lee una política de esta carpeta.
 *
 * @param {string} nombre el archivo
 * @returns {object} la política
 */
function politica(nombre) {
  return JSON.parse(readFileSync(new URL(`./${nombre}`, import.meta.url), 'utf8'));
}

const EC2 = politica('inspectia-ec2-permisos.json');
const GITHUB = politica('inspectia-github-permisos.json');
const CONFIANZA_GITHUB = politica('inspectia-github-confianza.json');

/** Acciones de AWS que no aceptan un recurso concreto: son las únicas que pueden ir con «*». */
const SIN_RECURSO = new Set([
  'ecr:GetAuthorizationToken',
  'ec2:DescribeInstances',
  'ssm:DescribeInstanceInformation',
  'ssm:GetCommandInvocation',
  'ssm:ListCommandInvocations',
  'cloudwatch:PutMetricData',
]);

/**
 * Una lista, aunque la política traiga un solo valor.
 *
 * @param {string|string[]|undefined} valor el valor del JSON
 * @returns {string[]} la lista
 */
const lista = (valor) => (valor === undefined ? [] : [].concat(valor));

/**
 * Los recursos con los que la política permite una acción.
 *
 * @param {object} doc la política
 * @param {string} accion la acción, como «s3:PutObject»
 * @returns {string[]} los recursos de las sentencias que la permiten
 */
function recursosDe(doc, accion) {
  return doc.Statement.filter((s) => s.Effect === 'Allow' && lista(s.Action).includes(accion))
    .flatMap((s) => lista(s.Resource));
}

test('D-16 · las políticas son JSON de IAM válidos, con Sid y sin comodines en las acciones', () => {
  for (const doc of [EC2, GITHUB, CONFIANZA_GITHUB]) {
    assert.equal(doc.Version, '2012-10-17');
    assert.ok(Array.isArray(doc.Statement) && doc.Statement.length > 0);
    for (const sentencia of doc.Statement) {
      assert.match(sentencia.Sid, /^[A-Za-z0-9]+$/, 'el Sid de IAM solo admite letras y números');
      for (const accion of lista(sentencia.Action)) {
        assert.doesNotMatch(accion, /\*/, `${sentencia.Sid}: ${accion}`);
      }
    }
  }
});

test('D-16 · «*» como recurso solo en las acciones que no aceptan otro', () => {
  for (const doc of [EC2, GITHUB]) {
    for (const sentencia of doc.Statement) {
      if (lista(sentencia.Resource).includes('*')) {
        for (const accion of lista(sentencia.Action)) {
          assert.ok(SIN_RECURSO.has(accion), `${sentencia.Sid}: ${accion} con Resource «*»`);
        }
      }
    }
  }
});

test('D-16 · el rol de GitHub solo se asume por OIDC desde main de DrekOF/inspectia', () => {
  assert.equal(CONFIANZA_GITHUB.Statement.length, 1);
  const [sentencia] = CONFIANZA_GITHUB.Statement;
  assert.equal(sentencia.Effect, 'Allow');
  assert.equal(sentencia.Action, 'sts:AssumeRoleWithWebIdentity');
  assert.equal(sentencia.Principal.Federated,
    'arn:aws:iam::<cuenta>:oidc-provider/token.actions.githubusercontent.com');
  assert.deepEqual(sentencia.Condition, {
    StringEquals: {
      'token.actions.githubusercontent.com:aud': 'sts.amazonaws.com',
      'token.actions.githubusercontent.com:sub': 'repo:DrekOF/inspectia:ref:refs/heads/main',
    },
  });
});

test('D-16 · GitHub sube solo a los dos repositorios de ECR y escribe solo en el bucket web', () => {
  assert.deepEqual(recursosDe(GITHUB, 'ecr:PutImage').sort(), [
    'arn:aws:ecr:us-east-1:<cuenta>:repository/inspectia/backend',
    'arn:aws:ecr:us-east-1:<cuenta>:repository/inspectia/ia-servicio',
  ]);
  assert.deepEqual(recursosDe(GITHUB, 's3:PutObject'), ['arn:aws:s3:::inspectia-web-<cuenta>/*']);
  assert.deepEqual(recursosDe(GITHUB, 's3:DeleteObject'), ['arn:aws:s3:::inspectia-web-<cuenta>/*']);
  assert.ok(!JSON.stringify(GITHUB).includes('inspectia-archivos'), 'GitHub no toca el bucket de archivos');
});

test('D-16 · GitHub invalida solo su distribución y manda comandos solo a la instancia', () => {
  assert.deepEqual(recursosDe(GITHUB, 'cloudfront:CreateInvalidation'),
    ['arn:aws:cloudfront::<cuenta>:distribution/<distribucion>']);
  assert.deepEqual(recursosDe(GITHUB, 'ssm:SendCommand').sort(), [
    'arn:aws:ec2:us-east-1:<cuenta>:instance/<instancia>',
    'arn:aws:ssm:us-east-1::document/AWS-RunShellScript',
  ]);
  assert.deepEqual(recursosDe(GITHUB, 'ec2:StartInstances'), ['arn:aws:ec2:us-east-1:<cuenta>:instance/<instancia>']);
  assert.deepEqual(recursosDe(GITHUB, 'ec2:StopInstances'), []);
});

test('D-16 · la instancia usa el bucket de archivos solo en sus prefijos y no borra los respaldos', () => {
  const bucket = 'arn:aws:s3:::inspectia-archivos-<cuenta>';
  const objetos = ['s3:GetObject', 's3:PutObject', 's3:DeleteObject'].flatMap((a) => recursosDe(EC2, a));
  for (const recurso of objetos) {
    assert.match(recurso, /^arn:aws:s3:::inspectia-archivos-<cuenta>\/(fotos|reportes|modelos|respaldos)\/\*$/);
  }
  assert.deepEqual(recursosDe(EC2, 's3:PutObject').sort(),
    [`${bucket}/fotos/*`, `${bucket}/reportes/*`, `${bucket}/respaldos/*`]);
  assert.deepEqual(recursosDe(EC2, 's3:DeleteObject').sort(), [`${bucket}/fotos/*`, `${bucket}/reportes/*`]);
  assert.ok(recursosDe(EC2, 's3:GetObject').includes(`${bucket}/modelos/*`));
  assert.deepEqual(recursosDe(EC2, 's3:ListBucket'), [bucket]);
  assert.ok(!JSON.stringify(EC2).includes('inspectia-web'), 'la instancia no toca el bucket web');
});

test('D-16 · la instancia lee solo sus dos parámetros de SSM y baja solo de los repositorios de inspectia', () => {
  assert.deepEqual(recursosDe(EC2, 'ssm:GetParameter').sort(), [
    'arn:aws:ssm:us-east-1:<cuenta>:parameter/inspectia/jwt-clave',
    'arn:aws:ssm:us-east-1:<cuenta>:parameter/inspectia/postgres-contrasena',
  ]);
  const descifrar = EC2.Statement.find((s) => lista(s.Action).includes('kms:Decrypt'));
  assert.equal(descifrar.Condition.StringEquals['kms:ViaService'], 'ssm.us-east-1.amazonaws.com');
  assert.deepEqual(recursosDe(EC2, 'ecr:BatchGetImage'), ['arn:aws:ecr:us-east-1:<cuenta>:repository/inspectia/*']);
  assert.deepEqual(recursosDe(EC2, 'ecr:PutImage'), []);
  assert.deepEqual(recursosDe(EC2, 'sns:Publish'), ['arn:aws:sns:us-east-1:<cuenta>:inspectia-alertas']);
  for (const recurso of recursosDe(EC2, 'logs:PutLogEvents')) {
    assert.match(recurso, /^arn:aws:logs:us-east-1:<cuenta>:log-group:\/inspectia\//);
  }
});
