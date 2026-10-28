/*
 * Nombre: otra-tematica.js
 * Descripción: Demostración de otra temática para la sustentación (RP-23): muestra que, cambiando solo los JSON, el
 *              generador saca otro CRUD completo. En una copia del proyecto, dentro de frontend/.demostracion, que Git
 *              ignora, agrega a las entidades de InspectIA el producto de la pauta del docente para el back-end
 *              (generador/pruebas/ejemplos/producto.json) y lo suma al alcance; genera su tabla, su API, su pantalla
 *              y sus pruebas; lista lo generado por capa; y enciende la copia como el entorno aislado de las pruebas
 *              de punta a punta, con su propia base (proyecto inspectia-demo). El repositorio no cambia. Con
 *              --con-pruebas, antes de encenderla corre en la copia las pruebas generadas de producto.
 * Fecha de creación: 28/10/2026
 * Actualización: 28/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 *
 * Uso, desde frontend/ y con Docker Desktop abierto: «npm run demo:otra-tematica», o con las pruebas,
 * «npm run demo:otra-tematica -- --con-pruebas». Ctrl+C la apaga y borra su base; la copia queda, y la vez siguiente
 * se pone al día con el repositorio.
 */
import { copyFileSync, lstatSync, mkdirSync, readFileSync, symlinkSync, writeFileSync } from 'node:fs';
import { dirname, join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';
import { ejecutar, encenderEntorno } from '../e2e/entorno/entorno.js';
import { espejar } from './espejo.js';

// Sin new URL(…, import.meta.url): en las pruebas de Vitest, Vite lo cambia por una dirección de su servidor.
const FRONTEND = join(dirname(fileURLToPath(import.meta.url)), '..');
const RAIZ = join(FRONTEND, '..');
const WINDOWS = process.platform === 'win32';

/** La carpeta de la copia, con su base, sus registros y sus datos. Está en .gitignore. */
const CARPETA = join(FRONTEND, '.demostracion');

/** El proyecto de Docker Compose de la demostración: distinto del de desarrollo y del de las pruebas. */
const PROYECTO = 'inspectia-demo';

/** La entidad de la otra temática: el producto de la pauta del docente, el mismo de las pruebas del generador. */
const ENTIDAD = 'producto';
const JSON_DE_LA_ENTIDAD = join(RAIZ, 'generador', 'pruebas', 'ejemplos', 'producto.json');

/** Lo que no se copia del frontend: lo que se instala o se compila, los reportes y las carpetas de las copias. */
const SIN_COPIAR = ['node_modules', 'dist', 'coverage', 'test-results', 'playwright-report', 'blob-report', 'reports',
  '.stryker-tmp', '.demostracion', 'e2e/.entorno'];

/** Las capas en que se explica lo generado, en el orden en que se muestran. */
const CAPAS = {
  base: 'Base de datos',
  api: 'API del backend',
  pruebasDeLaApi: 'Pruebas de la API',
  textos: 'Textos, en español y en inglés',
  pantalla: 'Pantalla',
  pruebasDeLaPantalla: 'Pruebas de la pantalla',
};

/**
 * El alcance de la demostración: el de la app, con producto al final de las entidades con API y con pantalla.
 *
 * @param {{ api: string[], pantallas: string[] }} alcance el alcance de la app (generador/alcance.json)
 * @returns {object} el alcance de la copia; el de la app queda igual
 */
export function alcanceConProducto(alcance) {
  return { ...alcance, api: [...alcance.api, ENTIDAD], pantallas: [...alcance.pantallas, ENTIDAD] };
}

/**
 * La capa de un archivo generado, por su ruta.
 *
 * @param {string} ruta la ruta, relativa a la raíz y con /, como la escribe el generador
 * @returns {string} el nombre de la capa
 */
function capaDe(ruta) {
  if (ruta.startsWith('backend/src/generated/resources/db/')) {
    return CAPAS.base;
  }
  if (ruta.includes('/i18n/') || ruta.endsWith('/textos.js')) {
    return CAPAS.textos;
  }
  if (ruta.startsWith('backend/src/test-generated/')) {
    return CAPAS.pruebasDeLaApi;
  }
  if (ruta.startsWith('backend/')) {
    return CAPAS.api;
  }
  if (ruta.startsWith('frontend/e2e/') || ruta.endsWith('.test.js') || ruta.endsWith('.test.jsx')) {
    return CAPAS.pruebasDeLaPantalla;
  }
  return CAPAS.pantalla;
}

/**
 * Lo que escribió el generador, por capa: de la base de datos a las pruebas de la pantalla, sin las capas vacías.
 *
 * @param {string[]} rutas las rutas que escribió, relativas a la raíz y con /
 * @returns {Array<{ capa: string, archivos: string[] }>} las capas, con sus archivos en orden alfabético
 */
export function porCapa(rutas) {
  return Object.values(CAPAS)
    .map((capa) => ({ capa, archivos: rutas.filter((ruta) => capaDe(ruta) === capa).sort() }))
    .filter(({ archivos }) => archivos.length > 0);
}

/**
 * El tipo de enlace a una carpeta en un sistema: en Windows, un junction, que no pide permisos de administrador; en
 * los demás, un enlace simbólico.
 *
 * @param {string} sistema el sistema, como lo da process.platform
 * @returns {'junction'|'dir'} el tipo, para symlinkSync
 */
export function tipoDeEnlace(sistema) {
  return sistema === 'win32' ? 'junction' : 'dir';
}

/**
 * Si existe una ruta, aunque sea un enlace a algo que ya no está.
 *
 * @param {string} ruta la ruta
 * @returns {boolean} si existe
 */
function existe(ruta) {
  return lstatSync(ruta, { throwIfNoEntry: false }) !== undefined;
}

/**
 * Enlaza en la copia del frontend la carpeta node_modules del original, en lugar de copiarla: pesa cientos de megas y
 * es la misma. Si la copia ya la tiene, la deja.
 *
 * @param {string} frontend la carpeta del frontend original
 * @param {string} copia la carpeta de la copia
 */
export function enlazarModulos(frontend, copia) {
  const enlace = join(copia, 'node_modules');
  if (!existe(enlace)) {
    symlinkSync(join(frontend, 'node_modules'), enlace, tipoDeEnlace(process.platform));
  }
}

// Stryker disable all: encender la copia (Docker, Maven y la página) se prueba corriendo la demostración.

/**
 * Una ruta para mostrarla: relativa a la raíz del repositorio y con /.
 *
 * @param {string} ruta la ruta
 * @returns {string} la ruta para mostrar
 */
function paraMostrar(ruta) {
  return relative(RAIZ, ruta).replaceAll('\\', '/');
}

/**
 * Las líneas de lo que escribió un programa que dicen cómo le fue, como «Tests run: 30, Failures: 0».
 *
 * @param {string} salida lo que escribió
 * @param {RegExp} patron las líneas que se muestran
 * @returns {string} esas líneas, con sangría
 */
function lineasDe(salida, patron) {
  return salida.split(/\r?\n/).filter((linea) => patron.test(linea)).map((linea) => `     ${linea.trim()}`).join('\n');
}

/**
 * Corre en la copia las pruebas generadas de producto: las de Vitest (el cliente de la API, las validaciones y la
 * pantalla) y la prueba de integración de su API, con PostgreSQL en Testcontainers.
 *
 * @param {string} carpeta la carpeta de la copia
 * @param {string} frontend la copia del frontend
 */
async function probar(carpeta, frontend) {
  console.log('\n3. Las pruebas generadas de producto, en la copia:');
  const vitest = await ejecutar(process.execPath,
    [join(frontend, 'node_modules', 'vitest', 'vitest.mjs'), 'run', `src/generated/${ENTIDAD}`], { cwd: frontend });
  console.log(`   Vitest:\n${lineasDe(vitest, /Test Files|Tests /)}`);
  const argumentos = ['verify', '-Dit.test=ProductoApiGeneradaIT', '-Dtest=NingunaUnitaria',
    '-Dsurefire.failIfNoSpecifiedTests=false'];
  // Con la ruta («.\» o «./»): la consola de Windows no siempre busca programas en la carpeta actual.
  const maven = await ejecutar(WINDOWS ? `.\\mvnw.cmd ${argumentos.join(' ')}` : './mvnw', WINDOWS ? [] : argumentos,
    { cwd: join(carpeta, 'backend'), shell: WINDOWS });
  console.log(`   ProductoApiGeneradaIT:\n${lineasDe(maven, /Tests run:.*Fail|BUILD/)}`);
}

/**
 * El paso de preparación del entorno: con la copia del backend ya hecha, copia el frontend y specs, agrega el JSON de
 * producto y el alcance, genera en la copia y, si se pide, corre las pruebas generadas de producto.
 *
 * @param {string} carpeta la carpeta de la copia
 * @param {boolean} conPruebas si corre las pruebas generadas de producto
 * @returns {Promise<string>} la copia del frontend, que el entorno compila
 */
async function prepararCopia(carpeta, conPruebas) {
  const frontend = join(carpeta, 'frontend');
  espejar(FRONTEND, frontend, { excluir: SIN_COPIAR });
  enlazarModulos(FRONTEND, frontend);
  const entidades = join(carpeta, 'specs', 'entidades');
  espejar(join(RAIZ, 'specs'), join(carpeta, 'specs'));

  const json = join(entidades, `${ENTIDAD}.json`);
  copyFileSync(JSON_DE_LA_ENTIDAD, json);
  const alcance = alcanceConProducto(JSON.parse(readFileSync(join(RAIZ, 'generador', 'alcance.json'), 'utf8')));
  const archivoDeAlcance = join(carpeta, 'generador', 'alcance.json');
  mkdirSync(join(carpeta, 'generador'), { recursive: true });
  writeFileSync(archivoDeAlcance, `${JSON.stringify(alcance, null, 2)}\n`);
  console.log(`\n1. Lo único que cambia, en la copia (${paraMostrar(carpeta)}):`);
  console.log(`   - ${paraMostrar(json)}: el producto de la pauta del docente, de ${paraMostrar(JSON_DE_LA_ENTIDAD)};`);
  console.log(`   - ${paraMostrar(archivoDeAlcance)}, con producto en la API y en las pantallas:`);
  console.log(`       "api": ${JSON.stringify(alcance.api)}`);
  console.log(`       "pantallas": ${JSON.stringify(alcance.pantallas)}`);

  const python = join(RAIZ, 'generador', '.venv', ...(WINDOWS ? ['Scripts', 'python.exe'] : ['bin', 'python']));
  if (!existe(python)) {
    throw new Error(`Falta el entorno del generador (${paraMostrar(python)}): créalo como dice generador/README.md.`);
  }
  const salida = await ejecutar(python, [join('generador', 'generar.py'), 'generar', '--entidades', entidades,
    '--alcance', archivoDeAlcance, '--raiz', carpeta], { cwd: RAIZ, env: { ...process.env, PYTHONIOENCODING: 'utf-8' } });
  const escritos = [...salida.matchAll(/^\s+escrito: (.+)$/gm)].map(([, ruta]) => ruta.trim());
  console.log(`\n2. Lo que escribió el generador en la copia (${escritos.length} archivos):`);
  for (const { capa, archivos } of porCapa(escritos)) {
    console.log(`   ${capa}:\n${archivos.map((archivo) => `     ${archivo}`).join('\n')}`);
  }

  if (conPruebas) {
    await probar(carpeta, frontend);
  }
  console.log('\nEncendiendo la copia: la base, el backend, el administrador de la plataforma y la página…');
  return frontend;
}

/** Con «npm run demo:otra-tematica», prepara la copia, la enciende y la deja corriendo hasta Ctrl+C. */
if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const conPruebas = process.argv.includes('--con-pruebas');
  console.log('Demostración de otra temática (RP-23): InspectIA más el producto de la pauta del docente.');
  try {
    const { app, api, datos, apagar } = await encenderEntorno({
      carpeta: CARPETA, proyecto: PROYECTO, preparar: (carpeta) => prepararCopia(carpeta, conPruebas),
    });
    const { correo, contrasena } = datos.plataforma;
    console.log(`\nLa copia está encendida en ${app}`);
    console.log(`  - Entra como administrador de la plataforma: ${correo}, contraseña ${contrasena} (solo de esta corrida).`);
    console.log('  - «Productos» está al final del menú, con el ícono genérico: registra un producto, edítalo y pásalo a'
      + ' Inactivo.');
    console.log(`  - La API, en Swagger UI: ${api.replace(/\/api\/v1$/, '')}/swagger-ui.html. Inicia sesión con`
      + ' POST /api/v1/sesion y pega el token en «Authorize».');
    console.log(`  - La tabla, en psql: docker exec -it ${PROYECTO}-postgres-1 psql -U inspectia -d inspectia -c "\\d ${ENTIDAD}"`);
    console.log('Ctrl+C la apaga y borra su base.');
    let apagando = false;
    const salir = async () => {
      if (apagando) {
        return;
      }
      apagando = true;
      await apagar();
      process.exit(0);
    };
    process.on('SIGINT', salir);
    process.on('SIGTERM', salir);
  } catch (error) {
    console.error(`\nLa demostración no pudo encenderse: ${error.message}`);
    process.exit(1);
  }
}

// Stryker restore all
