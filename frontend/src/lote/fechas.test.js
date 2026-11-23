/*
 * Nombre: fechas.test.js (lote)
 * Descripción: Pruebas de las fechas de los lotes en la hora de Colombia (RF-04, RF-14): el día de hoy y el de la
 *              fecha del resultado, en momentos en que el día de Colombia y el de UTC no coinciden, y el día de
 *              hace un año, la fecha de recepción más antigua que se acepta.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { describe, expect, it } from 'vitest';
import { diaEnColombia, haceUnAnio, hoyEnColombia } from './fechas.js';

describe('Fechas de los lotes en Colombia', () => {
  it('RF-04 · C4 · a las 10 de la noche del 9 de noviembre en Colombia hoy todavía es el 9, aunque en UTC ya sea el 10 (prueba de límite)', () => {
    expect(hoyEnColombia(new Date('2026-11-10T03:00:00Z'))).toBe('2026-11-09');
  });

  it('RF-04 · C4 · el día cambia a la medianoche de Colombia, a las 5 de la mañana en UTC (prueba de límite)', () => {
    expect(hoyEnColombia(new Date('2026-11-10T04:59:59Z'))).toBe('2026-11-09');
    expect(hoyEnColombia(new Date('2026-11-10T05:00:00Z'))).toBe('2026-11-10');
  });

  it('RF-04 · hace un año es el mismo día del año anterior: la fecha de recepción más antigua que se acepta (prueba de límite)', () => {
    expect(haceUnAnio('2026-11-09')).toBe('2025-11-09');
    expect(haceUnAnio('2027-01-01')).toBe('2026-01-01');
  });

  it('RF-04 · si hoy es 29 de febrero, hace un año es el 28 de febrero, como en el backend (prueba de límite)', () => {
    expect(haceUnAnio('2028-02-29')).toBe('2027-02-28');
    expect(haceUnAnio('2028-03-01')).toBe('2027-03-01');
  });

  it('RF-14 · la fecha del resultado se muestra como día de Colombia, en el formato del idioma', () => {
    expect(diaEnColombia('2026-10-19T03:00:00Z', 'es-CO')).toBe('18/10/2026');
    expect(diaEnColombia('2026-10-19T03:00:00Z', 'en-US')).toBe('10/18/2026');
  });
});
