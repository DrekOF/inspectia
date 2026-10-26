/*
 * Nombre: ayudas.js (e2e)
 * Descripción: Ayudas de las pruebas de punta a punta con Playwright: los datos del entorno aislado, crear
 *              usuarios propios por la API para las pruebas que cambian datos, e iniciar sesión.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { randomUUID } from 'node:crypto';
import { test as base, expect } from '@playwright/test';
import { datos } from './entorno/datos.js';

/*
 * Los datos del entorno aislado que arma e2e/entorno (backend, base y usuarios de cada corrida): el
 * administrador de la plataforma, una empresa con su administrador y un usuario de una empresa
 * desactivada. Las contraseñas se crean en cada corrida y nunca quedan en el repositorio.
 */
export { datos, expect };

/** Contraseña de los usuarios que crean las pruebas. */
export const CONTRASENA = 'Clave-Segura-2026';

/**
 * El token de una persona, pedido directamente a la API.
 *
 * @param {import('@playwright/test').APIRequestContext} api el cliente de la API de Playwright
 * @param {{ correo: string, contrasena: string }} persona quién inicia sesión
 * @returns {Promise<string>} el token
 */
export async function tokenDe(api, persona) {
  const respuesta = await api.post('/api/v1/sesion', { data: { correo: persona.correo, contrasena: persona.contrasena } });
  expect(respuesta.status()).toBe(200);
  return (await respuesta.json()).token;
}

/**
 * Crea un usuario nuevo en la empresa de prueba, con el administrador de empresa: así cada prueba que
 * cambia datos (contraseña o bloqueo) tiene el suyo y no choca con las demás.
 *
 * @param {import('@playwright/test').APIRequestContext} api el cliente de la API de Playwright
 * @param {string} [rol] el rol: USUARIO_FINAL o ADMIN_EMPRESA
 * @param {string} [contrasena] su contraseña; por defecto, CONTRASENA
 * @returns {Promise<{ id: string, nombre: string, correo: string, contrasena: string }>} el usuario
 */
export async function crearUsuario(api, rol = 'USUARIO_FINAL', contrasena = CONTRASENA) {
  const persona = { nombre: 'Luis Gómez', correo: `luis.${randomUUID().slice(0, 8)}@cacaosdelhuila.test`,
    contrasena, rol };
  const respuesta = await api.post('/api/v1/usuarios', {
    data: persona, headers: { Authorization: `Bearer ${await tokenDe(api, datos.adminEmpresa)}` },
  });
  expect(respuesta.status()).toBe(201);
  return { ...persona, id: (await respuesta.json()).id };
}

/**
 * Desactiva un usuario de la empresa de prueba, con el administrador de empresa.
 *
 * @param {import('@playwright/test').APIRequestContext} api el cliente de la API de Playwright
 * @param {string} id el usuario
 */
export async function desactivarUsuario(api, id) {
  const respuesta = await api.post(`/api/v1/usuarios/${id}/desactivar`, {
    headers: { Authorization: `Bearer ${await tokenDe(api, datos.adminEmpresa)}` },
  });
  expect(respuesta.status()).toBe(200);
}

/**
 * Las pruebas con un usuario final propio, que se crea antes de cada prueba y se desactiva al terminar:
 * así no chocan con las de otros navegadores y la empresa de prueba no llega a los 25 activos (RF-03).
 */
export const test = base.extend({
  usuarioFinal: async ({ request }, usar) => {
    const persona = await crearUsuario(request);
    await usar(persona);
    await desactivarUsuario(request, persona.id);
  },
});

/**
 * Escribe el correo y la contraseña en Iniciar sesión y pulsa el botón.
 *
 * @param {import('@playwright/test').Page} page la página
 * @param {{ correo: string, contrasena: string }} persona quién entra
 */
export async function escribirYEntrar(page, persona) {
  await page.getByLabel('Correo', { exact: true }).fill(persona.correo);
  await page.getByLabel('Contraseña', { exact: true }).fill(persona.contrasena);
  await page.getByRole('button', { name: 'Iniciar sesión' }).click();
}

/**
 * Inicia sesión desde la pantalla Iniciar sesión y espera el saludo del inicio.
 *
 * @param {import('@playwright/test').Page} page la página
 * @param {{ nombre: string, correo: string, contrasena: string }} persona quién entra
 */
export async function iniciarSesion(page, persona) {
  await page.goto('/iniciar-sesion');
  await escribirYEntrar(page, persona);
  await expect(page.getByRole('heading', { name: `Hola, ${persona.nombre.split(' ')[0]}` })).toBeVisible();
}

/**
 * El menú principal. En el celular está detrás del botón «Abrir el menú»: lo abre primero.
 *
 * @param {import('@playwright/test').Page} page la página
 * @param {boolean} esCelular si la prueba corre en tamaño de celular
 * @returns {Promise<import('@playwright/test').Locator>} el menú
 */
export async function abrirMenu(page, esCelular) {
  if (esCelular) {
    await page.getByRole('button', { name: 'Abrir el menú' }).click();
  }
  const menu = page.getByRole('navigation', { name: 'Menú principal' });
  await expect(menu).toBeVisible();
  return menu;
}
