/*
 * Nombre: espejo.js
 * Descripción: Copia en espejo de una carpeta (RP-14): deja la copia igual al original, con lo nuevo y lo
 *              cambiado con sus fechas, y borra de la copia lo que ya no está en el original. El entorno
 *              aislado de las pruebas de punta a punta la usa para compilar su propia copia del backend, que
 *              lleva también los scripts de infra/bd y nunca infra/.env.
 * Fecha de creación: 20/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import { cpSync, existsSync, readdirSync, rmSync, statSync } from 'node:fs';
import { join } from 'node:path';

/**
 * Borra de la copia lo que no está en el original, o que allí es de otro tipo (archivo o carpeta).
 *
 * @param {string} origen la carpeta original
 * @param {string} copia la copia
 * @param {string} relativa la carpeta que se revisa, relativa a las dos
 * @param {string[]} excluir los nombres del primer nivel que no se revisan
 * @param {string[]} borradas donde se anota cada ruta borrada, relativa a la copia
 */
function borrarLoQueSobra(origen, copia, relativa, excluir, borradas) {
  for (const entrada of readdirSync(join(copia, relativa), { withFileTypes: true })) {
    const ruta = join(relativa, entrada.name);
    if (relativa === '' && excluir.includes(entrada.name)) {
      continue;
    }
    const original = join(origen, ruta);
    if (!existsSync(original) || statSync(original).isDirectory() !== entrada.isDirectory()) {
      rmSync(join(copia, ruta), { recursive: true, force: true });
      borradas.push(ruta);
    } else if (entrada.isDirectory()) {
      borrarLoQueSobra(origen, copia, ruta, excluir, borradas);
    }
  }
}

/**
 * Deja la copia igual al original: borra lo que ya no está y copia lo nuevo y lo cambiado, con sus fechas.
 *
 * @param {string} origen la carpeta original
 * @param {string} copia la copia; si no existe, se crea
 * @param {{ excluir?: string[] }} [opciones] nombres del primer nivel que ni se copian ni se borran, como target
 * @returns {string[]} lo que se borró de la copia, relativo a ella: un archivo o una carpeta que se borró o
 *   se renombró en el original
 */
export function espejar(origen, copia, { excluir = [] } = {}) {
  const borradas = [];
  if (existsSync(copia)) {
    borrarLoQueSobra(origen, copia, '', excluir, borradas);
  }
  const excluidas = excluir.map((nombre) => join(origen, nombre));
  cpSync(origen, copia, {
    recursive: true,
    preserveTimestamps: true,
    filter: (ruta) => !excluidas.includes(ruta),
  });
  return borradas;
}

/**
 * Deja en una carpeta la copia en espejo del backend, sin su target, y la de infra/bd, con los scripts de la base
 * que algunas pruebas del backend leen desde ../infra/bd. Nunca copia infra/.env, con las contraseñas locales,
 * ni lo demás de infra; si quedó uno de antes en la carpeta, lo borra.
 *
 * @param {string} raiz la raíz del repositorio
 * @param {string} carpeta dónde quedan las copias: carpeta/backend y carpeta/infra/bd
 * @returns {{ backend: string[], scripts: string[] }} lo que se borró de cada copia, relativo a ella
 */
export function espejarBackend(raiz, carpeta) {
  rmSync(join(carpeta, 'infra', '.env'), { force: true });
  return {
    backend: espejar(join(raiz, 'backend'), join(carpeta, 'backend'), { excluir: ['target'] }),
    scripts: espejar(join(raiz, 'infra', 'bd'), join(carpeta, 'infra', 'bd'), { excluir: ['.env'] }),
  };
}
