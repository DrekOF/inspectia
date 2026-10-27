/*
 * Nombre: registros.test.js
 * Descripción: Pruebas de lo que calcula la pantalla de registros generada: qué acciones ve cada rol según su
 *              JSON (RNF-04), el resumen de la lista en singular y en plural, también con el máximo de activos,
 *              los valores de una fila para sus textos y los errores de la API de un campo dentro de un grupo,
 *              como el correo del primer administrador (RP-23).
 * Fecha de creación: 26/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import { describe, expect, it } from 'vitest';
import i18n from '../i18n/i18n.js';
import { erroresAnidados, puede, resumenDeRegistros, textoContado, valoresDeFila } from './registros.js';

/** Las claves del resumen de empresas, como las escribe su pantalla generada. */
const RESUMEN = {
  frase: 'empresa.pantalla.resumen',
  total: 'empresa.pantalla.resumen_total',
  activos: 'empresa.pantalla.resumen_activos',
  inactivos: 'empresa.pantalla.resumen_inactivos',
};

describe('acciones de cada rol', () => {
  const operaciones = { crear: ['ADMIN_EMPRESA'], editar: ['ADMIN_EMPRESA', 'USUARIO_FINAL'] };

  it.each([
    ['ADMIN_EMPRESA', 'crear', true],
    ['USUARIO_FINAL', 'crear', false],
    ['USUARIO_FINAL', 'editar', true],
    ['ADMIN_EMPRESA', 'desactivar', false],
  ])('RNF-04 · cada acción de la pantalla aparece solo para los roles de su operación en el JSON: %s %s',
    (rol, operacion, permitida) => {
      expect(puede(operaciones, rol, operacion)).toBe(permitida);
    });
});

describe('resumen de la lista', () => {
  it.each([
    [2, 1, '2 empresas registradas: 1 activa y 1 desactivada.'],
    [7, 6, '7 empresas registradas: 6 activas y 1 desactivada.'],
    [1, 1, '1 empresa registrada: 1 activa y 0 desactivadas.'],
  ])('RP-23 · el resumen cuenta en singular y en plural con los textos de pantalla.textos: %i registros, %i activos',
    (total, activos, frase) => {
      expect(resumenDeRegistros(i18n.t, RESUMEN, { total, activos })).toBe(frase);
    });

  it('RP-23 · un texto que se cuenta va en singular solo con uno', () => {
    expect(textoContado(i18n.t, 'empresa.pantalla.resumen_total', 1)).toBe('1 empresa registrada');
    expect(textoContado(i18n.t, 'empresa.pantalla.resumen_total', 0)).toBe('0 empresas registradas');
  });

  it('RP-23 · el resumen sale en el idioma de la app', async () => {
    await i18n.changeLanguage('en');
    try {
      expect(resumenDeRegistros(i18n.t, RESUMEN, { total: 2, activos: 1 }))
        .toBe('2 registered companies: 1 active and 1 deactivated.');
    } finally {
      await i18n.changeLanguage('es');
    }
  });

  it.each([
    [5, '5 de 25 usuarios activos, contándote a ti.'],
    [1, '1 de 25 usuarios activos, contándote a ti.'],
  ])('RP-23 · con límite de activos, el resumen cuenta los activos contra el máximo y solo lo que usa su frase: %i activos',
    (activos, frase) => {
      const resumen = { frase: 'usuario.pantalla.resumen', activos: 'usuario.pantalla.resumen_activos' };

      expect(resumenDeRegistros(i18n.t, resumen, { total: activos + 1, activos, maximo: 25 })).toBe(frase);
    });

  it('RP-23 · la frase del resumen también recibe el máximo de activos, y solo se piden los textos que ella usa', () => {
    const pedidos = [];
    const t = (clave, valores) => {
      pedidos.push(clave);
      return clave === 'frase' ? `${valores.activos}; máximo ${valores.maximo}` : `${valores.cantidad}`;
    };

    expect(resumenDeRegistros(t, { frase: 'frase', activos: 'activos' }, { total: 6, activos: 5, maximo: 25 }))
      .toBe('5; máximo 25');
    expect(pedidos).toEqual(['activos.plural', 'frase']);
  });

  it('RP-23 · si el JSON no trae los textos del resumen, la pantalla no muestra resumen', () => {
    expect(resumenDeRegistros(i18n.t, null, { total: 2, activos: 1 })).toBeNull();
  });
});

describe('valores de una fila', () => {
  it('RP-23 · los textos de una fila, como «Editar {nombre}», toman solo sus textos y sus números', () => {
    const fila = {
      nombre: 'Cacaos del Huila', usuarios: 3, activa: true, ciudad: null, administrador: { nombre: 'Ana Pérez' },
    };

    expect(valoresDeFila(fila)).toEqual({ nombre: 'Cacaos del Huila', usuarios: 3 });
  });
});

describe('errores de la API en los campos', () => {
  it('RP-23 · un error de un campo dentro de un grupo, como administrador.correo, queda en ese campo del formulario', () => {
    expect(erroresAnidados({ nit: 'Ya existe una empresa con este NIT.', 'administrador.correo': 'Escribe un correo válido.' }))
      .toEqual({ nit: 'Ya existe una empresa con este NIT.', administrador: { correo: 'Escribe un correo válido.' } });
  });
});
