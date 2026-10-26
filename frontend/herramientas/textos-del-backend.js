/*
 * Nombre: textos-del-backend.js
 * Descripción: Lee las etiquetas y los mensajes del backend (generado.properties y mensajes.properties, en
 *              español y en inglés) y los entrega al frontend como el módulo virtual
 *              «virtual:textos-del-backend», para que los dos usen los mismos textos (RP-11).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { existsSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';

/** Los archivos de cada idioma, desde la raíz del repositorio. Si una clave se repite, manda el último. */
const ARCHIVOS = {
  es: ['backend/src/generated/resources/i18n/generado.properties', 'backend/src/main/resources/i18n/mensajes.properties'],
  en: ['backend/src/generated/resources/i18n/generado_en.properties', 'backend/src/main/resources/i18n/mensajes_en.properties'],
};

const MODULO = 'virtual:textos-del-backend';
const MODULO_RESUELTO = `\0${MODULO}`;

/** Las letras que, escritas después de una barra invertida, son un carácter de control. */
const CONTROLES = { t: '\t', n: '\n', r: '\r', f: '\f' };

/**
 * Si la línea termina en una barra invertida sin pareja, que la une con la siguiente.
 *
 * @param {string} linea la línea
 * @returns {boolean} si sigue en la línea siguiente
 */
function continua(linea) {
  const barras = /\\*$/.exec(linea)[0].length;
  return barras % 2 === 1;
}

/**
 * Quita los escapes de una clave o de un valor: \t, \n, \r, \f, \uXXXX y cualquier otro carácter.
 *
 * @param {string} texto el texto escrito en el archivo
 * @returns {string} el texto
 */
function sinEscapes(texto) {
  return texto.replace(/\\(u[0-9a-fA-F]{4}|.)/g, (_, escape) => {
    if (escape.length === 5) {
      return String.fromCharCode(Number.parseInt(escape.slice(1), 16));
    }
    return CONTROLES[escape] ?? escape;
  });
}

/**
 * Separa una línea en su clave y su valor. La clave termina en el primer «=», «:» o espacio sin escape.
 *
 * @param {string} linea la línea, sin los espacios del comienzo
 * @returns {[string, string]} la clave y el valor, todavía con sus escapes
 */
function separar(linea) {
  let fin = 0;
  while (fin < linea.length && !/[=:\s]/.test(linea[fin])) {
    fin += linea[fin] === '\\' ? 2 : 1;
  }
  const resto = /^\s*[=:]?\s*/.exec(linea.slice(fin))[0].length;
  return [linea.slice(0, fin), linea.slice(fin + resto)];
}

/**
 * Lee el texto de un archivo .properties de Java: comentarios con # o !, escapes y líneas que siguen en la
 * siguiente con una barra invertida al final.
 *
 * @param {string} texto el contenido del archivo, en UTF-8
 * @returns {Record<string, string>} los textos, por clave
 */
export function leerProperties(texto) {
  const textos = {};
  const lineas = texto.split(/\r\n|\r|\n/);
  for (let i = 0; i < lineas.length; i += 1) {
    let linea = lineas[i].trimStart();
    if (linea === '' || linea.startsWith('#') || linea.startsWith('!')) {
      continue;
    }
    while (continua(linea)) {
      linea = linea.slice(0, -1);
      if (i + 1 === lineas.length) {
        break;
      }
      i += 1;
      linea += lineas[i].trimStart();
    }
    const [clave, valor] = separar(linea);
    textos[sinEscapes(clave)] = sinEscapes(valor);
  }
  return textos;
}

/**
 * La raíz del repositorio: la primera carpeta, subiendo desde donde se corre (frontend/, o la copia que hace
 * Stryker en .stryker-tmp/), que tiene los .properties del backend.
 *
 * @param {string} [desde] la carpeta donde empieza a buscar
 * @returns {string} la ruta de la carpeta raíz
 */
export function raizDelRepositorio(desde = process.cwd()) {
  let carpeta = desde;
  while (!ARCHIVOS.es.every((archivo) => existsSync(join(carpeta, archivo)))) {
    const padre = dirname(carpeta);
    if (padre === carpeta) {
      throw new Error(`No se encontraron los textos del backend (${ARCHIVOS.es.join(', ')}) en las carpetas superiores`);
    }
    carpeta = padre;
  }
  return carpeta;
}

/**
 * Los textos del backend en cada idioma.
 *
 * @returns {{ es: Record<string, string>, en: Record<string, string> }} los textos, por idioma y por clave
 */
export function textosDelBackend() {
  const raiz = raizDelRepositorio();
  const leer = (archivos) => Object.assign({}, ...archivos.map((archivo) => leerProperties(
    readFileSync(join(raiz, archivo), 'utf8'))));
  return { es: leer(ARCHIVOS.es), en: leer(ARCHIVOS.en) };
}

/**
 * Plugin de Vite que entrega los textos del backend como el módulo «virtual:textos-del-backend». Vigila los
 * .properties: si el backend cambia un texto, el servidor de desarrollo recarga la página.
 *
 * @returns {import('vite').Plugin} el plugin
 */
export function pluginTextosDelBackend() {
  return {
    name: 'inspectia-textos-del-backend',
    resolveId(id) {
      return id === MODULO ? MODULO_RESUELTO : null;
    },
    load(id) {
      if (id !== MODULO_RESUELTO) {
        return null;
      }
      const raiz = raizDelRepositorio();
      for (const archivo of [...ARCHIVOS.es, ...ARCHIVOS.en]) {
        this.addWatchFile(join(raiz, archivo));
      }
      return `export default ${JSON.stringify(textosDelBackend())};`;
    },
  };
}
