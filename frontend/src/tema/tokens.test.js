/*
 * Nombre: tokens.test.js
 * Descripción: Pruebas de los tokens de color: cada combinación de texto y fondo que usa la app cumple el
 *              contraste WCAG AA en los dos temas (RNF-06 C4, RP-08).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { describe, expect, it } from 'vitest';
import { colores } from './tokens.js';

/**
 * Luminancia relativa de un color, según WCAG 2.2.
 *
 * @param {string} hex el color, como #6B3F22
 * @returns {number} la luminancia, de 0 a 1
 */
function luminancia(hex) {
  const [r, g, b] = [1, 3, 5].map((i) => {
    const canal = parseInt(hex.slice(i, i + 2), 16) / 255;
    return canal <= 0.04045 ? canal / 12.92 : ((canal + 0.055) / 1.055) ** 2.4;
  });
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

/**
 * Contraste entre dos colores, según WCAG 2.2.
 *
 * @param {string} uno un color
 * @param {string} otro el otro color
 * @returns {number} el contraste, de 1 a 21
 */
function contraste(uno, otro) {
  const [claro, oscuro] = [luminancia(uno), luminancia(otro)].sort((a, b) => b - a);
  return (claro + 0.05) / (oscuro + 0.05);
}

/** Texto normal: 4,5:1. Bordes de los campos y del foco: 3:1 (WCAG 1.4.3 y 1.4.11). */
const TEXTO = 4.5;
const CONTORNO = 3;

/**
 * Las combinaciones de color que usa la app en un tema: [qué es, color, fondo, mínimo].
 *
 * @param {object} c los colores del tema
 * @returns {Array<[string, string, string, number]>} las combinaciones
 */
function combinaciones(c) {
  const avisos = ['exito', 'advertencia', 'error', 'informacion'].flatMap((aviso) => [
    [`${aviso}: texto sobre su fondo`, c[aviso].texto, c[aviso].fondo, TEXTO],
    [`${aviso}: borde sobre su fondo`, c[aviso].principal, c[aviso].fondo, CONTORNO],
    [`${aviso}: texto sobre la superficie`, c[aviso].principal, c.superficie, TEXTO],
  ]);
  return [
    ['texto sobre el fondo', c.texto, c.fondo, TEXTO],
    ['texto sobre la superficie', c.texto, c.superficie, TEXTO],
    ['texto secundario sobre el fondo', c.textoSecundario, c.fondo, TEXTO],
    ['texto secundario sobre la superficie', c.textoSecundario, c.superficie, TEXTO],
    ['enlace sobre el fondo', c.primario, c.fondo, TEXTO],
    ['enlace sobre la superficie', c.primario, c.superficie, TEXTO],
    ['enlace con el puntero encima', c.enlaceHover, c.superficie, TEXTO],
    ['texto de un botón principal', c.sobrePrimario, c.primario, TEXTO],
    ['texto de la barra', c.barra.texto, c.barra.fondo, TEXTO],
    ['marca de la barra', c.barra.marca, c.barra.fondo, TEXTO],
    ['subtítulo de la barra', c.barra.subtitulo, c.barra.fondo, TEXTO],
    ['opción elegida del menú', c.seleccion.texto, c.seleccion.fondo, TEXTO],
    ['texto sobre la selección', c.texto, c.seleccion.fondo, TEXTO],
    ['iniciales del usuario', c.avatar.texto, c.avatar.fondo, TEXTO],
    ['borde de un campo', c.bordeCampo, c.superficie, CONTORNO],
    ['borde de un campo sobre el fondo', c.bordeCampo, c.fondo, CONTORNO],
    ['anillo del foco', c.primario, c.superficie, CONTORNO],
    ...avisos,
  ];
}

describe.each(['claro', 'oscuro'])('tema %s', (tema) => {
  it.each(combinaciones(colores[tema]))(
    'RNF-06 · C4 · los textos y bordes de los dos temas cumplen el contraste WCAG AA: %s',
    (_, color, fondo, minimo) => {
      expect(contraste(color, fondo)).toBeGreaterThanOrEqual(minimo);
    },
  );
});
