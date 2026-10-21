/*
 * Nombre: espejo.test.js
 * Descripción: Pruebas de la copia en espejo de una carpeta (RP-14), con la que el entorno aislado de las
 *              pruebas de punta a punta compila su propia copia del backend: copia lo nuevo y lo cambiado, y
 *              borra de la copia lo que se borró o se renombró en el original.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { existsSync, mkdirSync, mkdtempSync, readFileSync, renameSync, rmSync, statSync, utimesSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { espejar } from './espejo.js';

let base;
let origen;
let copia;

/**
 * Escribe un archivo, con sus carpetas.
 *
 * @param {string} carpeta la carpeta
 * @param {string} ruta la ruta dentro de ella, como sub/b.txt
 * @param {string} [texto] el contenido
 */
function escribir(carpeta, ruta, texto = ruta) {
  mkdirSync(join(carpeta, ruta, '..'), { recursive: true });
  writeFileSync(join(carpeta, ruta), texto);
}

beforeEach(() => {
  base = mkdtempSync(join(tmpdir(), 'espejo-'));
  origen = join(base, 'origen');
  copia = join(base, 'copia');
  for (const ruta of ['pom.xml', join('src', 'A.java'), join('src', 'paquete', 'B.java'), join('target', 'A.class')]) {
    escribir(origen, ruta);
  }
});

afterEach(() => {
  rmSync(base, { recursive: true, force: true });
});

describe('copia en espejo', () => {
  it('RP-14 · la primera vez copia todo, menos las carpetas excluidas, como target', () => {
    expect(espejar(origen, copia, { excluir: ['target'] })).toEqual([]);

    expect(readFileSync(join(copia, 'src', 'paquete', 'B.java'), 'utf8')).toBe(join('src', 'paquete', 'B.java'));
    expect(existsSync(join(copia, 'pom.xml'))).toBe(true);
    expect(existsSync(join(copia, 'target'))).toBe(false);
  });

  it('RP-14 · un archivo borrado o renombrado en el original desaparece de la copia, y se informa', () => {
    espejar(origen, copia, { excluir: ['target'] });
    rmSync(join(origen, 'pom.xml'));
    renameSync(join(origen, 'src', 'A.java'), join(origen, 'src', 'C.java'));

    const borradas = espejar(origen, copia, { excluir: ['target'] });

    expect(borradas.sort()).toEqual(['pom.xml', join('src', 'A.java')].sort());
    expect(existsSync(join(copia, 'pom.xml'))).toBe(false);
    expect(existsSync(join(copia, 'src', 'A.java'))).toBe(false);
    expect(existsSync(join(copia, 'src', 'C.java'))).toBe(true);
  });

  it('RP-14 · una carpeta borrada en el original desaparece entera de la copia', () => {
    espejar(origen, copia, { excluir: ['target'] });
    rmSync(join(origen, 'src', 'paquete'), { recursive: true });

    expect(espejar(origen, copia, { excluir: ['target'] })).toEqual([join('src', 'paquete')]);
    expect(existsSync(join(copia, 'src', 'paquete'))).toBe(false);
  });

  it('RP-14 · un archivo que pasó a ser carpeta se reemplaza en la copia', () => {
    espejar(origen, copia, { excluir: ['target'] });
    rmSync(join(origen, 'pom.xml'));
    escribir(origen, join('pom.xml', 'dentro.txt'));

    expect(espejar(origen, copia, { excluir: ['target'] })).toEqual(['pom.xml']);
    expect(existsSync(join(copia, 'pom.xml', 'dentro.txt'))).toBe(true);
  });

  it('RP-14 · la carpeta excluida de la copia no se toca al espejar: lo que se hace con ella lo decide quien llama', () => {
    espejar(origen, copia, { excluir: ['target'] });
    escribir(copia, join('target', 'Vieja.class'));

    espejar(origen, copia, { excluir: ['target'] });

    expect(existsSync(join(copia, 'target', 'Vieja.class'))).toBe(true);
  });

  it('RP-14 · un archivo cambiado se copia con su contenido nuevo y su fecha, para que Maven compile solo lo cambiado', () => {
    espejar(origen, copia, { excluir: ['target'] });
    escribir(origen, join('src', 'A.java'), 'cambiado');
    const fecha = new Date('2026-10-20T15:00:00Z');
    utimesSync(join(origen, 'src', 'A.java'), fecha, fecha);

    espejar(origen, copia, { excluir: ['target'] });

    expect(readFileSync(join(copia, 'src', 'A.java'), 'utf8')).toBe('cambiado');
    expect(statSync(join(copia, 'src', 'A.java')).mtime).toEqual(fecha);
  });
});
