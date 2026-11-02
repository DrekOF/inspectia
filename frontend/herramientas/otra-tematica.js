/*
 * Nombre: otra-tematica.js
 * Descripción: Demostración de otra temática para la sustentación (RP-23): muestra que, cambiando solo los JSON, el
 *              generador saca otro CRUD completo. En una copia del proyecto, dentro de frontend/.demostracion, que Git
 *              ignora, agrega a las entidades de InspectIA la de un JSON y la suma al alcance: por defecto, el
 *              producto de la pauta del docente para el back-end (generador/pruebas/ejemplos/producto.json), y con
 *              --entidad, otra, como el café especial del Huila (cafe.json). Genera su tabla, su API, su pantalla y
 *              sus pruebas; lista lo generado por capa; y enciende la copia como el entorno aislado de las pruebas
 *              de punta a punta, con su propia base (proyecto inspectia-demo). El repositorio no cambia. Los textos
 *              de la consola y las pruebas de --con-pruebas salen del JSON: su nombre, su clase, su ruta y su rol.
 * Fecha de creación: 28/10/2026
 * Actualización: 02/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 *
 * Uso, desde frontend/ y con Docker Desktop abierto: «npm run demo:otra-tematica» agrega producto, y
 * «npm run demo:otra-tematica -- --entidad ../generador/pruebas/ejemplos/cafe.json», el café. Con --con-pruebas,
 * antes de encender la copia corre en ella las pruebas generadas de la entidad. Las opciones van después de «--»:
 * sin él, npm se queda con ellas, y la demostración se detiene y lo dice. Ctrl+C la apaga y borra su base; la copia
 * queda, y la vez siguiente se pone al día con el repositorio.
 */
import { copyFileSync, existsSync, lstatSync, mkdirSync, readFileSync, symlinkSync, writeFileSync } from 'node:fs';
import { basename, dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { ejecutar, encenderEntorno } from '../e2e/entorno/entorno.js';
import { espejar } from './espejo.js';
import { raizDelRepositorio } from './textos-del-backend.js';

// Sin new URL(…, import.meta.url): en las pruebas de Vitest, Vite lo cambia por una dirección de su servidor.
const FRONTEND = join(dirname(fileURLToPath(import.meta.url)), '..');
// Subiendo hasta encontrarla: Stryker corre las pruebas en una copia del frontend (.stryker-tmp), donde la carpeta de
// arriba no tiene generador/ ni specs/.
const RAIZ = raizDelRepositorio(FRONTEND);
const WINDOWS = process.platform === 'win32';

/** La carpeta de la copia, con su base, sus registros y sus datos. Está en .gitignore. */
const CARPETA = join(FRONTEND, '.demostracion');

/** El proyecto de Docker Compose de la demostración: distinto del de desarrollo y del de las pruebas. */
const PROYECTO = 'inspectia-demo';

/** La entidad si no se pide otra: el producto de la pauta del docente, el mismo de las pruebas del generador. */
const POR_DEFECTO = join(RAIZ, 'generador', 'pruebas', 'ejemplos', 'producto.json');

/** Las opciones de la demostración. */
const OPCIONES = ['--entidad', '--con-pruebas'];

/** Las entidades de InspectIA: un JSON que se llame como una de ellas la reemplazaría en la copia. */
const ENTIDADES_DE_INSPECTIA = join(RAIZ, 'specs', 'entidades');

/**
 * Con quién se entra a la copia según el rol: la persona del entorno aislado que lo tiene (e2e/entorno/entorno.js),
 * cómo se nombra su rol y si es de la empresa de prueba.
 */
const PERSONAS = {
  ADMIN_PLATAFORMA: { persona: 'plataforma', rol: 'administrador de la plataforma', deLaEmpresa: false },
  ADMIN_EMPRESA: { persona: 'adminEmpresa', rol: 'administrador de empresa', deLaEmpresa: true },
  USUARIO_FINAL: { persona: 'usuarioFinal', rol: 'usuario final', deLaEmpresa: true },
};

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
 * Una lista en español, como «--entidad y --con-pruebas».
 *
 * @param {string[]} textos lo que se lista
 * @param {'conjunction'|'disjunction'} [union] con «y» o con «o»
 * @returns {string} la lista
 */
function enLista(textos, union = 'conjunction') {
  return new Intl.ListFormat('es', { type: union }).format(textos);
}

/**
 * Las opciones de la demostración, de lo que se escribió después del comando. Lo que no entiende la detiene, en vez
 * de seguir sin ello: un error al escribir una opción en la sustentación no pasa sin que se note.
 *
 * @param {string[]} argumentos lo que se escribió después del comando, como process.argv.slice(2)
 * @param {object} donde dónde se corrió
 * @param {string} donde.carpeta la carpeta desde donde se corrió el comando: una ruta relativa se lee desde allí
 * @param {object} [donde.entorno] las variables de entorno. Si faltó «--» antes de las opciones, npm se queda con
 *   ellas y solo deja una variable por cada una, como npm_config_con_pruebas
 * @returns {{ json: string, conPruebas: boolean }} la ruta del JSON de la entidad y si antes de encender la copia se
 *   corren sus pruebas generadas
 * @throws {Error} si npm se quedó con una opción, si a --entidad le falta la ruta o si no entiende algo
 */
export function leerOpciones(argumentos, { carpeta, entorno = {} }) {
  const deNpm = OPCIONES.filter((opcion) => `npm_config_${opcion.slice(2).replaceAll('-', '_')}` in entorno);
  if (deNpm.length > 0) {
    throw new Error(`npm se quedó con ${enLista(deNpm)}: falta «--» antes de las opciones, como en `
      + 'npm run demo:otra-tematica -- --entidad <ruta del JSON> --con-pruebas.');
  }
  const opciones = { json: POR_DEFECTO, conPruebas: false };
  for (let i = 0; i < argumentos.length; i += 1) {
    if (argumentos[i] === '--con-pruebas') {
      opciones.conPruebas = true;
    } else if (argumentos[i] === '--entidad') {
      const ruta = argumentos[i + 1];
      if (ruta === undefined || ruta.startsWith('--')) {
        throw new Error('Falta la ruta del JSON después de --entidad, como en '
          + '--entidad ../generador/pruebas/ejemplos/cafe.json.');
      }
      opciones.json = resolve(carpeta, ruta);
      i += 1;
    } else {
      throw new Error(`No entiendo «${argumentos[i]}»: las opciones son --entidad <ruta del JSON> y --con-pruebas.`);
    }
  }
  return opciones;
}

/**
 * Lee el JSON de la entidad. Lo demás, como que tenga pantalla y operaciones, lo revisa el generador en la copia, con
 * sus propios mensajes.
 *
 * @param {string} archivo la ruta del JSON
 * @returns {object} el JSON
 * @throws {Error} si el archivo no existe, si se llama como una entidad de InspectIA, que reemplazaría en la copia, o
 *   si no es un JSON
 */
export function leerEntidad(archivo) {
  if (!existsSync(archivo)) {
    throw new Error(`No encuentro ${archivo}.`);
  }
  const entidad = basename(archivo, '.json');
  if (existsSync(join(ENTIDADES_DE_INSPECTIA, `${entidad}.json`))) {
    throw new Error(`${entidad} ya es una entidad de InspectIA (specs/entidades/${entidad}.json): la demostración `
      + 'agrega una nueva. Ponle otro nombre al archivo y a su entidad.');
  }
  try {
    // Sin la codificación: JSON.parse lee el archivo como texto en UTF-8.
    return JSON.parse(readFileSync(archivo));
  } catch (error) {
    throw new Error(`${archivo} no es un JSON válido: ${error.message}`, { cause: error });
  }
}

/**
 * Lo que la demostración saca del JSON de la entidad, ya revisado por el generador: su nombre en el menú, el texto
 * del botón para registrar, si lo tiene, su ruta en la API, sus pruebas generadas y el rol con que se entra a verla.
 *
 * @param {object} json el JSON de la entidad
 * @returns {{ entidad: string, nombre: string, nuevo: string|undefined, api: string, vitest: string,
 *   integracion: string, rol: string }} lo que usan la consola y --con-pruebas
 * @throws {Error} si nadie del entorno de la demostración tiene ese rol
 */
export function datosDeLaEntidad(json) {
  const { entidad, clase, modulo, ruta, etiquetas, operaciones, pantalla } = json;
  // Quien registra ve todo: la lista, el formulario y lo que guardó. Si nadie registra, quien lista.
  const [rol] = operaciones.crear ?? operaciones.listar;
  if (!Object.hasOwn(PERSONAS, rol)) {
    throw new Error(`Nadie del entorno de la demostración tiene el rol ${rol}, que usa ${entidad}: se entra con `
      + `${enLista(Object.keys(PERSONAS), 'disjunction')}.`);
  }
  return {
    entidad,
    nombre: etiquetas.es.plural,
    nuevo: pantalla.textos.nuevo?.es,
    api: `/api/v1/${ruta}`,
    vitest: `src/generated/${modulo}`,
    integracion: `${clase}ApiGeneradaIT`,
    rol,
  };
}

/**
 * Con quién entrar a la copia según el rol: su nombre, su rol, su empresa si es de una, su correo y su contraseña,
 * que solo sirve en esta corrida.
 *
 * @param {string} rol el rol, como ADMIN_EMPRESA
 * @param {object} datos los datos de prueba del entorno
 * @returns {string} como «Ana Pérez, administrador de empresa de Cacaos del Huila: ana.perez@…, contraseña …»
 */
export function conQuienEntrar(rol, datos) {
  const { persona, rol: nombreDelRol, deLaEmpresa } = PERSONAS[rol];
  const { nombre, correo, contrasena } = datos[persona];
  const empresa = deLaEmpresa ? ` de ${datos.empresa.nombre}` : '';
  return `${nombre}, ${nombreDelRol}${empresa}: ${correo}, contraseña ${contrasena}`;
}

/**
 * El alcance de la demostración: el de la app, con la entidad al final de las entidades con API y con pantalla.
 *
 * @param {{ api: string[], pantallas: string[] }} alcance el alcance de la app (generador/alcance.json)
 * @param {string} entidad la entidad que se agrega, como cafe
 * @returns {object} el alcance de la copia; el de la app queda igual
 */
export function alcanceCon(alcance, entidad) {
  return { ...alcance, api: [...alcance.api, entidad], pantallas: [...alcance.pantallas, entidad] };
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
 * Corre en la copia las pruebas generadas de la entidad: las de Vitest (el cliente de la API, las validaciones y la
 * pantalla) y la prueba de integración de su API, con PostgreSQL en Testcontainers.
 *
 * @param {string} carpeta la carpeta de la copia
 * @param {string} frontend la copia del frontend
 * @param {ReturnType<typeof datosDeLaEntidad>} entidad lo que la demostración saca del JSON
 */
async function probar(carpeta, frontend, entidad) {
  console.log(`\n3. Las pruebas generadas de ${entidad.entidad}, en la copia:`);
  const vitest = await ejecutar(process.execPath,
    [join(frontend, 'node_modules', 'vitest', 'vitest.mjs'), 'run', entidad.vitest], { cwd: frontend });
  console.log(`   Vitest:\n${lineasDe(vitest, /Test Files|Tests /)}`);
  const argumentos = ['verify', `-Dit.test=${entidad.integracion}`, '-Dtest=NingunaUnitaria',
    '-Dsurefire.failIfNoSpecifiedTests=false'];
  // Con la ruta («.\» o «./»): la consola de Windows no siempre busca programas en la carpeta actual.
  const maven = await ejecutar(WINDOWS ? `.\\mvnw.cmd ${argumentos.join(' ')}` : './mvnw', WINDOWS ? [] : argumentos,
    { cwd: join(carpeta, 'backend'), shell: WINDOWS });
  console.log(`   ${entidad.integracion}:\n${lineasDe(maven, /Tests run:.*Fail|BUILD/)}`);
}

/**
 * El paso de preparación del entorno: con la copia del backend ya hecha, copia el frontend y specs, agrega el JSON de
 * la entidad y el alcance, genera en la copia y, si se pide, corre las pruebas generadas de la entidad.
 *
 * @param {string} carpeta la carpeta de la copia
 * @param {string} archivo el JSON de la entidad
 * @param {object} json su contenido
 * @param {boolean} conPruebas si corre las pruebas generadas de la entidad
 * @returns {Promise<string>} la copia del frontend, que el entorno compila
 */
async function prepararCopia(carpeta, archivo, json, conPruebas) {
  const frontend = join(carpeta, 'frontend');
  espejar(FRONTEND, frontend, { excluir: SIN_COPIAR });
  enlazarModulos(FRONTEND, frontend);
  const entidades = join(carpeta, 'specs', 'entidades');
  espejar(join(RAIZ, 'specs'), join(carpeta, 'specs'));

  // Con el nombre del archivo: el generador revisa que su entidad se llame igual.
  const copiaDelJson = join(entidades, basename(archivo));
  copyFileSync(archivo, copiaDelJson);
  const alcance = alcanceCon(JSON.parse(readFileSync(join(RAIZ, 'generador', 'alcance.json'), 'utf8')),
    basename(archivo, '.json'));
  const archivoDeAlcance = join(carpeta, 'generador', 'alcance.json');
  mkdirSync(join(carpeta, 'generador'), { recursive: true });
  writeFileSync(archivoDeAlcance, `${JSON.stringify(alcance, null, 2)}\n`);

  const python = join(RAIZ, 'generador', '.venv', ...(WINDOWS ? ['Scripts', 'python.exe'] : ['bin', 'python']));
  if (!existe(python)) {
    throw new Error(`Falta el entorno del generador (${paraMostrar(python)}): créalo como dice generador/README.md.`);
  }
  // El generador revisa el JSON antes de escribir: si le falta algo, se detiene aquí y dice qué.
  const salida = await ejecutar(python, [join('generador', 'generar.py'), 'generar', '--entidades', entidades,
    '--alcance', archivoDeAlcance, '--raiz', carpeta], { cwd: RAIZ, env: { ...process.env, PYTHONIOENCODING: 'utf-8' } });
  const entidad = datosDeLaEntidad(json);
  console.log(`\n1. Lo único que cambia, en la copia (${paraMostrar(carpeta)}), para agregar «${entidad.nombre}»:`);
  console.log(`   - ${paraMostrar(copiaDelJson)}, de ${paraMostrar(archivo)};`);
  console.log(`   - ${paraMostrar(archivoDeAlcance)}, con ${entidad.entidad} en la API y en las pantallas:`);
  console.log(`       "api": ${JSON.stringify(alcance.api)}`);
  console.log(`       "pantallas": ${JSON.stringify(alcance.pantallas)}`);

  const escritos = [...salida.matchAll(/^\s+escrito: (.+)$/gm)].map(([, ruta]) => ruta.trim());
  console.log(`\n2. Lo que escribió el generador en la copia (${escritos.length} archivos):`);
  for (const { capa, archivos } of porCapa(escritos)) {
    console.log(`   ${capa}:\n${archivos.map((ruta) => `     ${ruta}`).join('\n')}`);
  }

  if (conPruebas) {
    await probar(carpeta, frontend, entidad);
  }
  console.log('\nEncendiendo la copia: la base, el backend, el administrador de la plataforma y la página…');
  return frontend;
}

/** Con «npm run demo:otra-tematica», prepara la copia, la enciende y la deja corriendo hasta Ctrl+C. */
if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  try {
    // npm corre el comando en frontend/; INIT_CWD es la carpeta desde donde se escribió.
    const { json: archivo, conPruebas } = leerOpciones(process.argv.slice(2),
      { carpeta: process.env.INIT_CWD ?? process.cwd(), entorno: process.env });
    const json = leerEntidad(archivo);
    console.log(`Demostración de otra temática (RP-23): InspectIA más la entidad de ${paraMostrar(archivo)}.`);
    const { app, api, datos, apagar } = await encenderEntorno({
      carpeta: CARPETA, proyecto: PROYECTO, preparar: (carpeta) => prepararCopia(carpeta, archivo, json, conPruebas),
    });
    const entidad = datosDeLaEntidad(json);
    console.log(`\nLa copia está encendida en ${app}`);
    console.log(`  - Entra como ${conQuienEntrar(entidad.rol, datos)} (solo de esta corrida).`);
    console.log(`  - «${entidad.nombre}» está al final de su menú`
      + `${entidad.nuevo ? `: prueba «${entidad.nuevo}» y edita lo que registres` : ''}.`);
    console.log(`  - La API, en Swagger UI: ${api.replace(/\/api\/v1$/, '')}/swagger-ui.html, en ${entidad.api}. Inicia`
      + ' sesión con POST /api/v1/sesion y pega el token en «Authorize».');
    console.log(`  - La tabla, en psql: docker exec -it ${PROYECTO}-postgres-1 psql -U inspectia -d inspectia -c "\\d ${entidad.entidad}"`);
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
    console.error(`\nLa demostración se detuvo: ${error.message}`);
    process.exit(1);
  }
}

// Stryker restore all
