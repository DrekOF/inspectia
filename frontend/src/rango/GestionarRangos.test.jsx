/*
 * Nombre: GestionarRangos.test.jsx
 * Descripción: Pruebas de los criterios de RF-13 en la pantalla de rangos, con la API simulada: abrirla desde el
 *              menú, el resumen «Así quedan los rangos», el orden de los límites con los mismos bordes que las
 *              pruebas del backend (C1), restaurar los valores por defecto con su confirmación (C4), que el usuario
 *              final no la abra (C5) y cómo se escriben y se ven los números en español y en inglés. Que un análisis
 *              nuevo use los rangos nuevos (C2) llega con RF-12.
 * Fecha de creación: 28/10/2026
 * Actualización: 28/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { screen, within } from '@testing-library/react';
import { http } from 'msw';
import { describe, expect, it } from 'vitest';
import i18n from '../i18n/i18n.js';
import { abrirApp, conSesion, USUARIOS } from '../pruebas/ayudas.jsx';
import { apiEnBloqueSimulada } from '../pruebas/crud.js';
import {
  abrirRangos, API_RANGOS, campoDe, CATEGORIAS, enElResumen, enviadas, escribirLimite, guardar, MENSAJES, POR_DEFECTO,
  PROPIOS, RECOMENDACION, RUTA_RANGOS,
} from '../pruebas/rangos.js';
import { problema, servidor } from '../pruebas/servidor.js';

/**
 * El formulario de los rangos, cuando terminan de cargar sus filas.
 *
 * @returns {Promise<HTMLElement>} el formulario
 */
const formulario = () => screen.findByRole('form', { name: 'Editar los rangos' });

describe('configurar los rangos de la empresa (RF-13)', () => {
  it('RF-13 · el administrador de la empresa abre «Rangos de recomendación» desde el menú y ve las 4 categorías, con sus límites, sus recomendaciones y las notas', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    apiEnBloqueSimulada(API_RANGOS, POR_DEFECTO);
    const { usuario } = abrirApp('/');

    await usuario.click(within(await screen.findByRole('navigation', { name: 'Menú principal' }))
      .getByRole('link', { name: 'Rangos de recomendación' }));

    expect(await screen.findByRole('heading', { name: 'Rangos de recomendación', level: 1 })).toBeInTheDocument();
    expect(screen.getByText(/Los cambios solo aplican a los análisis nuevos: los anteriores conservan su resultado\./))
      .toBeInTheDocument();
    expect(within(await formulario()).getAllByRole('group').map((fila) => fila.querySelector('legend').textContent))
      .toEqual(CATEGORIAS);
    expect(await Promise.all(CATEGORIAS.map(async (categoria) => (await campoDe(categoria)).value)))
      .toEqual(['96,0', '90,0', '82,0', '0,0']);
    const recomendacion = await campoDe('Estándar', RECOMENDACION);
    expect(recomendacion).toHaveValue(POR_DEFECTO[1].recomendacion);
    expect(recomendacion.tagName).toBe('TEXTAREA');
    expect(screen.getByText('Los límites van de 0 a 100 %, con un decimal como máximo, y deben bajar en orden.'))
      .toBeInTheDocument();
    expect(screen.getByText('Restaurar vuelve a los límites y textos por defecto, basados en la NTC 1252:2021.'))
      .toBeInTheDocument();
  });

  it('RF-13 · el resumen «Así quedan los rangos» dice qué porcentajes abarca cada categoría y cambia mientras se escriben los límites', async () => {
    const { usuario } = abrirRangos();

    expect(await enElResumen('Premium o Especial')).toBe('96,0 % o más');
    expect(await enElResumen('Estándar')).toBe('de 90,0 a 95,9 %');
    expect(await enElResumen('Corriente')).toBe('de 82,0 a 89,9 %');
    expect(await enElResumen('Fuera de norma')).toBe('menos de 82,0 %');

    await escribirLimite(usuario, 'Estándar', '91');

    expect(await enElResumen('Estándar')).toBe('de 91,0 a 95,9 %');
    expect(await enElResumen('Corriente')).toBe('de 82,0 a 90,9 %');
  });

  it('RF-13 · C1 · si los límites no bajan en orden (Estándar 97 % y Premium 96 %), no guarda, explica el error y marca los dos límites', async () => {
    const { usuario, simulada } = abrirRangos();

    await escribirLimite(usuario, 'Estándar', '97');
    await guardar(usuario);

    expect(await screen.findByRole('alert')).toHaveTextContent(MENSAJES.orden);
    for (const categoria of ['Premium o Especial', 'Estándar']) {
      const campo = await campoDe(categoria);
      expect(campo).toHaveAttribute('aria-invalid', 'true');
      expect(campo).toHaveAccessibleDescription(expect.stringContaining(MENSAJES.orden));
    }
    expect(await campoDe('Corriente')).not.toHaveAttribute('aria-invalid', 'true');
    expect(await enElResumen('Premium o Especial')).toBe('revisa el límite');
    expect(await enElResumen('Estándar')).toBe('revisa el límite');
    expect(enviadas(simulada)).toEqual([]);
  });

  it('RF-13 · C1 · al no guardar por el orden, el foco va al primer límite marcado', async () => {
    const { usuario } = abrirRangos();

    await escribirLimite(usuario, 'Corriente', '95');
    await guardar(usuario);

    expect(await screen.findByRole('alert')).toHaveTextContent(MENSAJES.orden);
    expect(await campoDe('Estándar')).toHaveFocus();
  });

  it.each([
    ['un empate: Estándar igual a Premium, 96,0', 'Estándar', '96', ['Premium o Especial', 'Estándar']],
    ['Corriente en 0, igual a Fuera de norma', 'Corriente', '0', ['Corriente']],
  ])('RF-13 · C1 · %s no baja en orden: no guarda (prueba de límite)', async (_, categoria, limite, marcadas) => {
    const { usuario, simulada } = abrirRangos();

    await escribirLimite(usuario, categoria, limite);
    await guardar(usuario);

    expect(await screen.findByRole('alert')).toHaveTextContent(MENSAJES.orden);
    for (const nombre of CATEGORIAS) {
      const marcado = marcadas.includes(nombre);
      expect(await campoDe(nombre), nombre).toHaveAttribute('aria-invalid', String(marcado));
    }
    expect(enviadas(simulada)).toEqual([]);
  });

  it.each([
    ['Estándar en 95,9, justo debajo de Premium', 'Estándar', '95,9', 95.9],
    ['Corriente en 0,1, justo encima de Fuera de norma', 'Corriente', '0,1', 0.1],
  ])('RF-13 · C1 · %s sí baja en orden: guarda (prueba de límite)', async (_, categoria, limite, numero) => {
    const { usuario, simulada } = abrirRangos();

    await escribirLimite(usuario, categoria, limite);
    await guardar(usuario);

    expect(await screen.findByText(MENSAJES.guardados)).toBeInTheDocument();
    expect(enviadas(simulada)[0][CATEGORIAS.indexOf(categoria)].limiteInferior).toBe(numero);
  });

  it('RF-13 · C1 · si la API responde la regla del orden, su mensaje se ve arriba del formulario, en lugar del mensaje general', async () => {
    const { usuario } = abrirRangos();
    servidor.use(http.put(API_RANGOS, () => problema(400, 'Revisa los datos marcados.', {
      errores: [{ campo: null, mensaje: MENSAJES.orden }],
    })));

    await formulario();
    await guardar(usuario);

    expect(await screen.findByRole('alert')).toHaveTextContent(MENSAJES.orden);
    expect(screen.queryByText('Revisa los datos marcados.')).not.toBeInTheDocument();
  });

  it('RF-13 · C4 · «Restaurar valores por defecto» pide confirmación, dice qué se pierde y vuelve a los rangos de D-10', async () => {
    const { usuario, simulada } = abrirRangos(PROPIOS, POR_DEFECTO);
    expect(await campoDe('Premium o Especial')).toHaveValue('97,5');

    await usuario.click(screen.getByRole('button', { name: 'Restaurar valores por defecto' }));
    const dialogo = screen.getByRole('dialog', { name: '¿Restaurar los valores por defecto?' });
    expect(dialogo).toHaveTextContent(MENSAJES.seQuitan);
    await usuario.click(within(dialogo).getByRole('button', { name: 'Restaurar' }));

    expect(await screen.findByText(MENSAJES.restaurados)).toBeInTheDocument();
    expect(simulada.peticiones).toContainEqual({ metodo: 'POST', ruta: `${API_RANGOS}/restaurar-por-defecto`, cuerpo: null });
    expect(await Promise.all(CATEGORIAS.map(async (categoria) => (await campoDe(categoria)).value)))
      .toEqual(['96,0', '90,0', '82,0', '0,0']);
    expect(await campoDe('Premium o Especial', RECOMENDACION)).toHaveValue(POR_DEFECTO[0].recomendacion);
    expect(await enElResumen('Estándar')).toBe('de 90,0 a 95,9 %');
  });

  it('RF-13 · C4 · al cancelar la confirmación, los rangos propios de la empresa quedan como estaban', async () => {
    const { usuario, simulada } = abrirRangos(PROPIOS, POR_DEFECTO);
    await formulario();

    await usuario.click(screen.getByRole('button', { name: 'Restaurar valores por defecto' }));
    await usuario.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancelar' }));

    expect(await campoDe('Premium o Especial')).toHaveValue('97,5');
    expect(simulada.peticiones.filter(({ metodo }) => metodo === 'POST')).toEqual([]);
  });

  it('RF-13 · C5 · un usuario final no ve «Rangos de recomendación» en el menú', async () => {
    conSesion(USUARIOS.USUARIO_FINAL);
    abrirApp('/');

    const menu = await screen.findByRole('navigation', { name: 'Menú principal' });
    expect(within(menu).queryByRole('link', { name: 'Rangos de recomendación' })).not.toBeInTheDocument();
  });

  it('RF-13 · C5 · un usuario final que escribe la dirección de los rangos ve que no tiene permiso, y la pantalla no pide nada a la API', async () => {
    conSesion(USUARIOS.USUARIO_FINAL);
    const simulada = apiEnBloqueSimulada(API_RANGOS, POR_DEFECTO);
    abrirApp(RUTA_RANGOS);

    expect(await screen.findByRole('heading', { name: 'Sin permiso' })).toBeInTheDocument();
    expect(simulada.peticiones).toEqual([]);
  });

  it('RF-13 · en español los límites se ven con coma, se escriben con coma o con punto y van a la API como números; al guardar quedan como los responde', async () => {
    const { usuario, simulada } = abrirRangos();

    await escribirLimite(usuario, 'Premium o Especial', '96.5');
    await escribirLimite(usuario, 'Estándar', '90,50');
    await guardar(usuario);

    expect(await screen.findByText(MENSAJES.guardados)).toBeInTheDocument();
    expect(enviadas(simulada)[0].map(({ limiteInferior }) => limiteInferior)).toEqual([96.5, 90.5, 82, 0]);
    expect(await campoDe('Premium o Especial')).toHaveValue('96,5');
    expect(await campoDe('Estándar')).toHaveValue('90,5');
  });

  it('RF-13 · un límite que no es un número dice cómo escribirlo, y no se envía nada', async () => {
    const { usuario, simulada } = abrirRangos();

    await escribirLimite(usuario, 'Estándar', 'noventa');
    await guardar(usuario);

    const estandar = await screen.findByRole('group', { name: 'Estándar' });
    expect(await within(estandar).findByText(MENSAJES.numero)).toBeInTheDocument();
    expect(await campoDe('Estándar')).toHaveAttribute('aria-invalid', 'true');
    expect(enviadas(simulada)).toEqual([]);
  });

  it('RF-13 · el límite de Fuera de norma no se puede cambiar y dice por qué', async () => {
    const { usuario } = abrirRangos();

    const fijo = await campoDe('Fuera de norma');
    await usuario.type(fijo, '5');

    expect(fijo).toHaveValue('0,0');
    expect(fijo).toHaveAttribute('readonly');
    expect(fijo).toHaveAccessibleDescription(expect.stringContaining(MENSAJES.fijo));
  });

  it('RF-13 · en inglés, los límites y el resumen van con punto', async () => {
    await i18n.changeLanguage('en');
    try {
      abrirRangos();

      expect(await campoDe('Premium or Specialty', 'Lower limit (% healthy beans)')).toHaveValue('96.0');
      expect(await enElResumen('Standard', 'How the ranges look')).toBe('90.0 to 95.9%');
      expect(await enElResumen('Off-grade', 'How the ranges look')).toBe('less than 82.0%');
    } finally {
      await i18n.changeLanguage('es');
    }
  });
});
