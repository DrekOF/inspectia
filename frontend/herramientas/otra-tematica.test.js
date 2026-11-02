/*
 * Nombre: otra-tematica.test.js
 * Descripción: Pruebas de lo que calcula la demostración de otra temática (RP-23): sus opciones, que la detienen si
 *              no las entiende; el JSON de la entidad, como cafe.json, o producto.json si no se pide otro; lo que
 *              saca de ese JSON para la consola y las pruebas; con quién entrar según su rol; el alcance con la
 *              entidad; lo que escribió el generador listado por capa; y el enlace a node_modules en la copia del
 *              frontend, que en Windows es un junction. Encender la copia (Docker, Maven y la página) se prueba
 *              corriendo la demostración.
 * Fecha de creación: 28/10/2026
 * Actualización: 02/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import { lstatSync, mkdirSync, mkdtempSync, readFileSync, realpathSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import {
  alcanceCon, conQuienEntrar, datosDeLaEntidad, enlazarModulos, leerEntidad, leerOpciones, porCapa, tipoDeEnlace,
} from './otra-tematica.js';
import { raizDelRepositorio } from './textos-del-backend.js';

// Sin new URL(…, import.meta.url): en las pruebas de Vitest, Vite lo cambia por una dirección de su servidor. Y
// subiendo hasta la raíz: Stryker corre las pruebas en una copia del frontend (.stryker-tmp), sin el resto del
// repositorio, y los JSON de los ejemplos están en generador/.
const RAIZ = raizDelRepositorio(dirname(fileURLToPath(import.meta.url)));
const FRONTEND = join(RAIZ, 'frontend');
const CAFE = join(RAIZ, 'generador', 'pruebas', 'ejemplos', 'cafe.json');
const PRODUCTO = join(RAIZ, 'generador', 'pruebas', 'ejemplos', 'producto.json');

describe('opciones de la demostración', () => {
  it('RP-23 · sin opciones, la demostración agrega producto.json y no corre las pruebas generadas', () => {
    expect(leerOpciones([], { carpeta: FRONTEND })).toEqual({ json: PRODUCTO, conPruebas: false });
  });

  it('RP-23 · --entidad recibe la ruta del JSON, relativa a la carpeta donde se corrió el comando, y --con-pruebas va antes o después', () => {
    expect(leerOpciones(['--entidad', '../generador/pruebas/ejemplos/cafe.json'], { carpeta: FRONTEND }))
      .toEqual({ json: CAFE, conPruebas: false });
    expect(leerOpciones(['--con-pruebas', '--entidad', 'generador/pruebas/ejemplos/cafe.json'], { carpeta: RAIZ }))
      .toEqual({ json: CAFE, conPruebas: true });
    expect(leerOpciones(['--entidad', CAFE, '--con-pruebas'], { carpeta: tmpdir() }))
      .toEqual({ json: CAFE, conPruebas: true });
  });

  it('RP-23 · --entidad sin la ruta del JSON detiene la demostración y dice cómo se escribe', () => {
    const falta = 'Falta la ruta del JSON después de --entidad, como en --entidad ../generador/pruebas/ejemplos/cafe.json.';

    expect(() => leerOpciones(['--entidad'], { carpeta: FRONTEND })).toThrow(falta);
    expect(() => leerOpciones(['--entidad', '--con-pruebas'], { carpeta: FRONTEND })).toThrow(falta);
  });

  it('RP-23 · una opción que la demostración no entiende, como --con-prueba mal escrito, la detiene en vez de ignorarla', () => {
    expect(() => leerOpciones(['--con-prueba'], { carpeta: FRONTEND }))
      .toThrow('No entiendo «--con-prueba»: las opciones son --entidad <ruta del JSON> y --con-pruebas.');
    expect(() => leerOpciones(['--entidad', CAFE, 'producto.json'], { carpeta: FRONTEND }))
      .toThrow('No entiendo «producto.json»: las opciones son --entidad <ruta del JSON> y --con-pruebas.');
  });

  it('RP-23 · si npm se quedó con las opciones porque faltó «--» antes de ellas, la demostración se detiene en vez de seguir sin ellas', () => {
    const ejemplo = 'falta «--» antes de las opciones, como en npm run demo:otra-tematica -- --entidad <ruta del JSON> --con-pruebas.';

    expect(() => leerOpciones([], { carpeta: FRONTEND, entorno: { npm_config_con_pruebas: 'true' } }))
      .toThrow(`npm se quedó con --con-pruebas: ${ejemplo}`);
    expect(() => leerOpciones(['../generador/pruebas/ejemplos/cafe.json'],
      { carpeta: FRONTEND, entorno: { npm_config_entidad: 'true', npm_config_con_pruebas: 'true' } }))
      .toThrow(`npm se quedó con --entidad y --con-pruebas: ${ejemplo}`);
    expect(leerOpciones([], { carpeta: FRONTEND, entorno: { npm_config_cache: 'C:\\npm-cache' } }))
      .toEqual({ json: PRODUCTO, conPruebas: false });
  });
});

describe('el JSON de la entidad', () => {
  let base;

  beforeEach(() => {
    base = mkdtempSync(join(tmpdir(), 'demostracion-'));
  });

  afterEach(() => {
    rmSync(base, { recursive: true, force: true });
  });

  it('RP-23 · la demostración lee el JSON de la entidad; si no existe o no es un JSON, se detiene y lo dice', () => {
    const roto = join(base, 'roto.json');
    writeFileSync(roto, '{ "entidad": "roto", ');

    expect(leerEntidad(CAFE)).toMatchObject({ entidad: 'cafe', clase: 'Cafe', ruta: 'cafes' });
    expect(() => leerEntidad(join(base, 'te.json'))).toThrow(`No encuentro ${join(base, 'te.json')}.`);
    expect(() => leerEntidad(roto)).toThrow(`${roto} no es un JSON válido: `);
    expect(() => leerEntidad(roto)).toThrow(expect.objectContaining({ cause: expect.any(SyntaxError) }));
  });

  it('RP-23 · un JSON con el nombre de una entidad de InspectIA, como empresa.json, detiene la demostración: reemplazaría la de la app', () => {
    const empresa = join(base, 'empresa.json');
    writeFileSync(empresa, readFileSync(CAFE));

    expect(() => leerEntidad(empresa)).toThrow('empresa ya es una entidad de InspectIA (specs/entidades/empresa.json): '
      + 'la demostración agrega una nueva. Ponle otro nombre al archivo y a su entidad.');
    expect(() => leerEntidad(join(RAIZ, 'specs', 'entidades', 'usuario.json')))
      .toThrow('usuario ya es una entidad de InspectIA');
  });
});

describe('lo que la demostración saca del JSON', () => {
  it('RP-23 · con cafe.json, los textos y las pruebas de la demostración salen de su JSON: su nombre, su clase, su ruta y su rol', () => {
    expect(datosDeLaEntidad(leerEntidad(CAFE))).toEqual({
      entidad: 'cafe', nombre: 'Cafés especiales', nuevo: 'Nuevo café especial', api: '/api/v1/cafes',
      vitest: 'src/generated/cafe', integracion: 'CafeApiGeneradaIT', rol: 'ADMIN_EMPRESA',
    });
  });

  it('RP-23 · sin --entidad, los textos y las pruebas de la demostración salen de producto.json', () => {
    expect(datosDeLaEntidad(leerEntidad(PRODUCTO))).toEqual({
      entidad: 'producto', nombre: 'Productos', nuevo: 'Nuevo producto', api: '/api/v1/productos',
      vitest: 'src/generated/producto', integracion: 'ProductoApiGeneradaIT', rol: 'ADMIN_PLATAFORMA',
    });
  });

  it('RP-23 · la demostración entra con el primer rol que registra en la entidad o, si nadie registra, con el primero que la lista', () => {
    const cafe = leerEntidad(CAFE);
    const soloSeLista = { ...cafe, operaciones: { listar: ['USUARIO_FINAL', 'ADMIN_EMPRESA'] },
      pantalla: { ...cafe.pantalla, textos: { vacia: cafe.pantalla.textos.vacia } } };

    expect(datosDeLaEntidad({ ...cafe, operaciones: { listar: ['ADMIN_EMPRESA'], crear: ['USUARIO_FINAL', 'ADMIN_EMPRESA'] } }))
      .toMatchObject({ rol: 'USUARIO_FINAL', nuevo: 'Nuevo café especial' });
    expect(datosDeLaEntidad(soloSeLista)).toMatchObject({ rol: 'USUARIO_FINAL' });
    expect(datosDeLaEntidad(soloSeLista).nuevo).toBeUndefined();
  });

  it('RP-23 · si nadie del entorno de la demostración tiene el rol de la entidad, se detiene y dice con qué roles se entra', () => {
    const cafe = leerEntidad(CAFE);

    expect(() => datosDeLaEntidad({ ...cafe, operaciones: { listar: ['AUDITOR'] } })).toThrow('Nadie del entorno de la '
      + 'demostración tiene el rol AUDITOR, que usa cafe: se entra con ADMIN_PLATAFORMA, ADMIN_EMPRESA o USUARIO_FINAL.');
  });
});

describe('con quién entrar', () => {
  const datos = {
    plataforma: { nombre: 'Andrés Gómez', correo: 'andres.gomez@inspectia.test', contrasena: 'Clave-1' },
    empresa: { id: 'e1', nombre: 'Cacaos del Huila' },
    adminEmpresa: { nombre: 'Ana Pérez', correo: 'ana.perez@cacaosdelhuila.test', contrasena: 'Clave-2' },
    usuarioFinal: { nombre: 'Carlos Ramírez', correo: 'carlos.ramirez@cacaosdelhuila.test', contrasena: 'Clave-3' },
  };

  it('RP-23 · la demostración dice con quién entrar según el rol de la entidad: el administrador de la plataforma, el de la empresa o el usuario final', () => {
    expect(conQuienEntrar('ADMIN_PLATAFORMA', datos))
      .toBe('Andrés Gómez, administrador de la plataforma: andres.gomez@inspectia.test, contraseña Clave-1');
    expect(conQuienEntrar('ADMIN_EMPRESA', datos))
      .toBe('Ana Pérez, administrador de empresa de Cacaos del Huila: ana.perez@cacaosdelhuila.test, contraseña Clave-2');
    expect(conQuienEntrar('USUARIO_FINAL', datos))
      .toBe('Carlos Ramírez, usuario final de Cacaos del Huila: carlos.ramirez@cacaosdelhuila.test, contraseña Clave-3');
  });
});

describe('alcance de la demostración', () => {
  it('RP-23 · el alcance de la demostración agrega la entidad al final de la API y de las pantallas, y deja lo demás igual', () => {
    const alcance = { _header: { version: '1.5' }, api: ['empresa', 'usuario'], pantallas: ['empresa'] };

    expect(alcanceCon(alcance, 'cafe')).toEqual({
      _header: { version: '1.5' }, api: ['empresa', 'usuario', 'cafe'], pantallas: ['empresa', 'cafe'],
    });
    expect(alcance).toEqual({ _header: { version: '1.5' }, api: ['empresa', 'usuario'], pantallas: ['empresa'] });
  });
});

describe('lo generado, por capa', () => {
  it('RP-23 · lo que escribió el generador para producto se lista por capa, en orden: de la base a las pruebas de la pantalla', () => {
    const escritos = [
      'frontend/src/generated/producto/ProductoPantalla.jsx',
      'backend/src/generated/java/co/edu/usco/inspectia/producto/ProductoControlador.java',
      'backend/src/generated/resources/db/migration/V1__esquema_inicial.sql',
      'frontend/e2e/generated/producto.spec.js',
      'backend/src/test-generated/java/co/edu/usco/inspectia/producto/ProductoApiGeneradaIT.java',
      'backend/src/generated/resources/i18n/generado_en.properties',
      'frontend/src/generated/producto/productoEsquemas.test.js',
      'frontend/src/generated/textos.js',
      'backend/src/generated/java/co/edu/usco/inspectia/producto/Producto.java',
      'frontend/src/generated/pantallas.js',
      'frontend/src/generated/producto/ProductoPantalla.test.jsx',
    ];

    expect(porCapa(escritos)).toEqual([
      { capa: 'Base de datos', archivos: ['backend/src/generated/resources/db/migration/V1__esquema_inicial.sql'] },
      { capa: 'API del backend', archivos: [
        'backend/src/generated/java/co/edu/usco/inspectia/producto/Producto.java',
        'backend/src/generated/java/co/edu/usco/inspectia/producto/ProductoControlador.java',
      ] },
      { capa: 'Pruebas de la API', archivos: [
        'backend/src/test-generated/java/co/edu/usco/inspectia/producto/ProductoApiGeneradaIT.java',
      ] },
      { capa: 'Textos, en español y en inglés', archivos: [
        'backend/src/generated/resources/i18n/generado_en.properties', 'frontend/src/generated/textos.js',
      ] },
      { capa: 'Pantalla', archivos: [
        'frontend/src/generated/pantallas.js', 'frontend/src/generated/producto/ProductoPantalla.jsx',
      ] },
      { capa: 'Pruebas de la pantalla', archivos: [
        'frontend/e2e/generated/producto.spec.js', 'frontend/src/generated/producto/ProductoPantalla.test.jsx',
        'frontend/src/generated/producto/productoEsquemas.test.js',
      ] },
    ]);
  });

  it('RP-23 · una capa en la que el generador no escribió nada no se lista', () => {
    expect(porCapa(['frontend/src/generated/textos.js'])).toEqual([
      { capa: 'Textos, en español y en inglés', archivos: ['frontend/src/generated/textos.js'] },
    ]);
  });
});

describe('tipo de enlace', () => {
  it('RP-23 · en Windows el enlace a node_modules es un junction, que no pide permisos de administrador; en los demás, un enlace simbólico', () => {
    expect(tipoDeEnlace('win32')).toBe('junction');
    expect(tipoDeEnlace('linux')).toBe('dir');
    expect(tipoDeEnlace('darwin')).toBe('dir');
  });
});

describe('enlace a node_modules', () => {
  let base;

  afterEach(() => {
    rmSync(base, { recursive: true, force: true });
  });

  it('RP-23 · la copia del frontend usa el node_modules del original por un enlace, sin copiarlo; si ya lo tiene, lo deja', () => {
    base = mkdtempSync(join(tmpdir(), 'demostracion-'));
    const frontend = join(base, 'frontend');
    const copia = join(base, 'copia');
    mkdirSync(join(frontend, 'node_modules', 'vite'), { recursive: true });
    writeFileSync(join(frontend, 'node_modules', 'vite', 'package.json'), '{}');
    mkdirSync(copia);

    enlazarModulos(frontend, copia);
    enlazarModulos(frontend, copia);

    const enlace = join(copia, 'node_modules');
    expect(lstatSync(enlace).isSymbolicLink()).toBe(true);
    expect(realpathSync(enlace)).toBe(realpathSync(join(frontend, 'node_modules')));
    expect(readFileSync(join(enlace, 'vite', 'package.json'), 'utf8')).toBe('{}');
  });
});
