/*
 * Nombre: entorno.js
 * Descripción: Entorno aislado de las pruebas de punta a punta (RP-14): una base PostgreSQL en su propio
 *              proyecto de Docker Compose, el backend y la app compilada, cada uno en un puerto libre y con
 *              contraseñas y claves creadas en cada corrida. Crea el administrador de la plataforma con el
 *              script de infra/bd y los datos de prueba por la API, y al apagarse lo borra todo. No usa
 *              infra/.env, la base de desarrollo ni backend/target: compila una copia del backend.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 *
 * Uso: lo encienden y lo apagan las pruebas (e2e/entorno/global.js). Para correr las pruebas varias veces
 * sobre el mismo entorno: «npm run e2e:entorno» en una terminal y, en otra, INSPECTIA_E2E_URL con la
 * dirección que muestra.
 */
import { spawn } from 'node:child_process';
import { randomBytes } from 'node:crypto';
import {
  closeSync, existsSync, mkdirSync, openSync, readdirSync, readFileSync, rmSync, writeFileSync,
} from 'node:fs';
import { createServer } from 'node:net';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { espejar } from '../../herramientas/espejo.js';

const FRONTEND = fileURLToPath(new URL('../../', import.meta.url));
const RAIZ = fileURLToPath(new URL('../../../', import.meta.url));
const BACKEND = join(RAIZ, 'backend');

/** Carpeta de cada corrida: datos, variables y registros. Está en .gitignore. */
export const CARPETA = join(FRONTEND, 'e2e', '.entorno');

/** Proyecto de Docker Compose del entorno: distinto del de desarrollo («inspectia»), con su propio volumen. */
const PROYECTO = 'inspectia-e2e';
const COMPOSE = join(RAIZ, 'infra', 'docker-compose.yml');
const WINDOWS = process.platform === 'win32';

/** Tiempos máximos de espera, en milisegundos. */
const ESPERA = { base: 120_000, backend: 180_000, app: 60_000 };

/**
 * Una contraseña o clave al azar, solo con letras, números, «-» y «_», que docker compose y el backend leen
 * igual (ver infra/.env.example).
 *
 * @param {number} bytes los bytes al azar
 * @returns {string} la clave
 */
function claveAlAzar(bytes) {
  return randomBytes(bytes).toString('base64url');
}

/**
 * Un puerto TCP libre en este equipo.
 *
 * @returns {Promise<number>} el puerto
 */
function puertoLibre() {
  return new Promise((resolver, rechazar) => {
    const servidor = createServer();
    servidor.unref();
    servidor.on('error', rechazar);
    servidor.listen(0, '127.0.0.1', () => {
      const { port } = servidor.address();
      servidor.close(() => resolver(port));
    });
  });
}

/**
 * Corre un programa y espera a que termine bien.
 *
 * @param {string} programa el programa, o el comando completo si opciones.shell es true
 * @param {string[]} argumentos sus argumentos
 * @param {import('node:child_process').SpawnOptions} [opciones] la carpeta, las variables y si usa la consola
 * @returns {Promise<string>} lo que escribió
 */
function ejecutar(programa, argumentos, opciones = {}) {
  return new Promise((resolver, rechazar) => {
    const proceso = spawn(programa, argumentos, { cwd: RAIZ, ...opciones, stdio: ['ignore', 'pipe', 'pipe'] });
    let salida = '';
    proceso.stdout.on('data', (parte) => { salida += parte; });
    proceso.stderr.on('data', (parte) => { salida += parte; });
    proceso.on('error', rechazar);
    proceso.on('close', (codigo) => {
      if (codigo === 0) {
        resolver(salida);
      } else {
        rechazar(new Error(`«${[programa, ...argumentos].join(' ')}» terminó con ${codigo}:\n${salida.slice(-3000)}`));
      }
    });
  });
}

/**
 * Los argumentos de docker compose para el proyecto aislado.
 *
 * @param {string} archivoDeVariables el archivo con las variables de la base de esta corrida
 * @param {...string} comando el comando, como «up -d»
 * @returns {string[]} los argumentos
 */
function compose(archivoDeVariables, ...comando) {
  return ['compose', '-p', PROYECTO, '-f', COMPOSE, '--env-file', archivoDeVariables, ...comando];
}

/**
 * Espera a que una dirección responda como se espera.
 *
 * @param {string} url la dirección
 * @param {(respuesta: Response) => boolean} lista si la respuesta indica que el servicio ya está listo
 * @param {number} limite el tiempo máximo, en milisegundos
 * @param {import('node:child_process').ChildProcess} proceso el proceso del servicio: si termina, no se espera más
 * @param {string} registro el archivo con lo que escribe el servicio, para el mensaje de error
 */
async function esperarServicio(url, lista, limite, proceso, registro) {
  const hasta = Date.now() + limite;
  while (Date.now() < hasta) {
    if (proceso.exitCode !== null) {
      throw new Error(`El servicio de ${url} terminó con ${proceso.exitCode}:\n${readFileSync(registro, 'utf8').slice(-3000)}`);
    }
    try {
      if (lista(await fetch(url))) {
        return;
      }
    } catch {
      // Todavía no escucha.
    }
    await new Promise((resolver) => setTimeout(resolver, 1000));
  }
  throw new Error(`${url} no respondió en ${limite / 1000} s:\n${readFileSync(registro, 'utf8').slice(-3000)}`);
}

/**
 * Arranca un proceso que sigue corriendo, con lo que escribe en un archivo de la corrida.
 *
 * @param {string} programa el programa
 * @param {string[]} argumentos sus argumentos
 * @param {string} registro el archivo donde queda lo que escribe
 * @param {import('node:child_process').SpawnOptions} opciones la carpeta y las variables
 * @returns {import('node:child_process').ChildProcess} el proceso
 */
function arrancar(programa, argumentos, registro, opciones) {
  const archivo = openSync(registro, 'w');
  try {
    return spawn(programa, argumentos, { ...opciones, stdio: ['ignore', archivo, archivo] });
  } finally {
    closeSync(archivo);
  }
}

/**
 * El jar del backend: el de INSPECTIA_E2E_JAR o, si no se da, el que compila Maven en una copia en espejo del
 * backend dentro de e2e/.entorno. Nunca compila en backend/target, que puede estar usando el backend de
 * desarrollo (./mvnw spring-boot:run).
 *
 * @returns {Promise<string>} la ruta del jar
 */
export async function jarDelBackend() {
  if (process.env.INSPECTIA_E2E_JAR) {
    if (!existsSync(process.env.INSPECTIA_E2E_JAR)) {
      throw new Error(`INSPECTIA_E2E_JAR apunta a ${process.env.INSPECTIA_E2E_JAR}, que no existe.`);
    }
    return process.env.INSPECTIA_E2E_JAR;
  }
  const copia = join(CARPETA, 'backend');
  // La copia queda igual a backend/, sin su target: lo nuevo y lo cambiado se copian con sus fechas, así Maven
  // solo compila lo cambiado. Si algo se borró o se renombró, su target se borra entero, porque Maven no
  // quita las clases ni los recursos que dejó, como una migración vieja de Flyway.
  if (espejar(BACKEND, copia, { excluir: ['target'] }).length > 0) {
    rmSync(join(copia, 'target'), { recursive: true, force: true });
  }
  // Con la ruta («.\» o «./»): la consola de Windows no siempre busca programas en la carpeta actual.
  await ejecutar(WINDOWS ? '.\\mvnw.cmd -q -Dmaven.test.skip=true package' : './mvnw', WINDOWS ? [] :
    ['-q', '-Dmaven.test.skip=true', 'package'], { cwd: copia, shell: WINDOWS });
  const target = join(copia, 'target');
  const jar = readdirSync(target).find((archivo) => archivo.endsWith('.jar'));
  if (!jar) {
    throw new Error(`Maven no dejó el jar del backend en ${target}.`);
  }
  return join(target, jar);
}

/**
 * Crea el administrador de la plataforma con el script de infra/bd (RP-15): el .ps1 en Windows y el .sh en
 * los demás. Antes revisa que docker compose, con las variables que recibe el script, apunte a la base del
 * entorno y no a la de desarrollo.
 *
 * @param {string} archivoDeVariables el archivo con las variables de la base de esta corrida
 * @param {{ nombre: string, correo: string, contrasena: string }} admin el administrador
 */
async function crearAdministradorDeLaPlataforma(archivoDeVariables, admin) {
  const variables = {
    ...process.env,
    COMPOSE_PROJECT_NAME: PROYECTO,
    COMPOSE_ENV_FILES: archivoDeVariables,
    INSPECTIA_ADMIN_NOMBRE: admin.nombre,
    INSPECTIA_ADMIN_CORREO: admin.correo,
    INSPECTIA_ADMIN_CONTRASENA: admin.contrasena,
  };
  const delEntorno = await ejecutar('docker', compose(archivoDeVariables, 'ps', '-q', 'postgres'));
  const delScript = await ejecutar('docker', ['compose', '-f', COMPOSE, 'ps', '-q', 'postgres'], { env: variables });
  if (!delEntorno.trim() || delScript.trim() !== delEntorno.trim()) {
    throw new Error('El script del administrador no apuntaría a la base del entorno aislado: no se corre.');
  }
  const script = WINDOWS
    ? ['powershell.exe', ['-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', join(RAIZ, 'infra', 'bd', 'crear-admin-plataforma.ps1')]]
    : ['sh', [join(RAIZ, 'infra', 'bd', 'crear-admin-plataforma.sh')]];
  await ejecutar(...script, { env: variables });
}

/**
 * Pide algo a la API del backend del entorno y espera una respuesta correcta.
 *
 * @param {string} api la dirección de la API, hasta /api/v1
 * @param {string} metodo el método HTTP
 * @param {string} ruta la ruta, como /empresas
 * @param {object} [datos] el cuerpo
 * @param {string} [token] el token de quien pide
 * @returns {Promise<object>} la respuesta
 */
async function pedir(api, metodo, ruta, datos, token) {
  const respuesta = await fetch(api + ruta, {
    method: metodo,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: datos ? JSON.stringify(datos) : undefined,
  });
  const cuerpo = await respuesta.json().catch(() => null);
  if (!respuesta.ok) {
    throw new Error(`${metodo} ${ruta} respondió ${respuesta.status}: ${JSON.stringify(cuerpo)}`);
  }
  return cuerpo;
}

/**
 * Las personas de prueba, con contraseñas nuevas en cada corrida. Los nombres y las empresas son los del
 * prototipo; los correos usan el dominio reservado .test.
 *
 * @returns {object} las personas
 */
function personas() {
  const contrasena = () => `E2e-${claveAlAzar(12)}`;
  return {
    plataforma: { nombre: 'Andrés Gómez', correo: 'andres.gomez@inspectia.test', contrasena: contrasena() },
    adminEmpresa: { nombre: 'Ana Pérez', correo: 'ana.perez@cacaosdelhuila.test', contrasena: contrasena() },
    adminDesactivada: { nombre: 'Marta Ruiz', correo: 'marta.ruiz@cacaodelsur.test', contrasena: contrasena() },
  };
}

/**
 * Enciende el entorno aislado y deja sus datos en e2e/.entorno/datos.json.
 *
 * @returns {Promise<{ app: string, apagar: () => Promise<void> }>} la dirección de la app y cómo apagarlo
 */
export async function encenderEntorno() {
  mkdirSync(CARPETA, { recursive: true });
  const archivoDeVariables = join(CARPETA, 'base.env');
  const [puertoBase, puertoBackend, puertoApp] = [await puertoLibre(), await puertoLibre(), await puertoLibre()];
  const base = { POSTGRES_DB: 'inspectia', POSTGRES_USER: 'inspectia', POSTGRES_PASSWORD: claveAlAzar(24), POSTGRES_PORT: String(puertoBase) };
  writeFileSync(archivoDeVariables, Object.entries(base).map(([nombre, valor]) => `${nombre}=${valor}\n`).join(''));

  const procesos = [];
  const apagar = async () => {
    for (const proceso of procesos) {
      proceso.kill();
    }
    await ejecutar('docker', compose(archivoDeVariables, 'down', '-v', '--remove-orphans')).catch(() => {});
    rmSync(join(CARPETA, 'datos.json'), { force: true });
    rmSync(archivoDeVariables, { force: true });
  };

  try {
    // Si una corrida anterior quedó a medias, su base se borra: la contraseña es otra.
    await ejecutar('docker', compose(archivoDeVariables, 'down', '-v', '--remove-orphans'));
    await ejecutar('docker', compose(archivoDeVariables, 'up', '-d', '--wait', '--wait-timeout', String(ESPERA.base / 1000)));

    const registroBackend = join(CARPETA, 'backend.log');
    // Corre desde e2e/.entorno: así no encuentra infra/.env (application.yml lo busca en ../infra y ./infra).
    const backend = arrancar('java', ['-jar', await jarDelBackend()], registroBackend, {
      cwd: CARPETA,
      env: {
        ...process.env,
        ...base,
        POSTGRES_HOST: '127.0.0.1',
        SERVER_PORT: String(puertoBackend),
        INSPECTIA_JWT_CLAVE: randomBytes(32).toString('base64'),
      },
    });
    procesos.push(backend);
    const api = `http://127.0.0.1:${puertoBackend}/api/v1`;
    await esperarServicio(`${api}/sesion`, (respuesta) => respuesta.status === 401, ESPERA.backend, backend, registroBackend);

    const { plataforma, adminEmpresa, adminDesactivada } = personas();
    await crearAdministradorDeLaPlataforma(archivoDeVariables, plataforma);
    const { token } = await pedir(api, 'POST', '/sesion', { correo: plataforma.correo, contrasena: plataforma.contrasena });
    const empresa = await pedir(api, 'POST', '/empresas',
      { nombre: 'Cacaos del Huila', nit: '900123456-8', ciudad: 'Neiva', administrador: adminEmpresa }, token);
    const desactivada = await pedir(api, 'POST', '/empresas',
      { nombre: 'Cacao del Sur', nit: '900654321-0', ciudad: 'Pitalito', administrador: adminDesactivada }, token);
    await pedir(api, 'POST', `/empresas/${desactivada.id}/desactivar`, undefined, token);

    const vite = join(FRONTEND, 'node_modules', 'vite', 'bin', 'vite.js');
    await ejecutar(process.execPath, [vite, 'build'], { cwd: FRONTEND });
    const registroApp = join(CARPETA, 'app.log');
    const app = arrancar(process.execPath, [vite, 'preview', '--port', String(puertoApp), '--strictPort'], registroApp, {
      cwd: FRONTEND,
      env: { ...process.env, INSPECTIA_API_URL: `http://127.0.0.1:${puertoBackend}` },
    });
    procesos.push(app);
    const direccion = `http://localhost:${puertoApp}`;
    await esperarServicio(direccion, (respuesta) => respuesta.ok, ESPERA.app, app, registroApp);

    writeFileSync(join(CARPETA, 'datos.json'), JSON.stringify({
      app: direccion,
      plataforma,
      empresa: { id: empresa.id, nombre: empresa.nombre },
      adminEmpresa,
      empresaDesactivada: { id: desactivada.id, nombre: desactivada.nombre, usuario: adminDesactivada },
    }, null, 2));
    return { app: direccion, apagar };
  } catch (error) {
    await apagar();
    throw error;
  }
}

/** Con «npm run e2e:entorno», enciende el entorno y lo deja corriendo hasta Ctrl+C. */
if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  console.log('Encendiendo el entorno aislado (base, backend y app)…');
  const { app, apagar } = await encenderEntorno();
  console.log(`\nEntorno listo en ${app}. Datos de prueba: ${join(CARPETA, 'datos.json')}`);
  console.log('Para correr las pruebas sobre él, en otra terminal:');
  console.log(`  PowerShell: $env:INSPECTIA_E2E_URL = '${app}'; npm run e2e:playwright`);
  console.log(`  Bash:       INSPECTIA_E2E_URL=${app} npm run e2e:playwright`);
  console.log('Ctrl+C lo apaga y borra la base.');
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
}

