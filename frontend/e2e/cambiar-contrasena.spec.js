/*
 * Nombre: cambiar-contrasena.spec.js
 * Descripción: Pruebas de punta a punta de Cambiar contraseña (RF-18) contra el backend real del entorno
 *              aislado, en PC y en celular: el cambio, la contraseña actual errada con el bloqueo, los límites
 *              de la nueva y la sesión que sigue mientras las demás dejan de servir.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { escribirYEntrar, expect, iniciarSesion, test } from './ayudas.js';

const AYUDA = 'Si fallas 5 veces seguidas, la cuenta se bloquea 15 minutos.';

/**
 * Escribe la contraseña actual, la nueva y la repetida en Cambiar contraseña y pulsa «Guardar».
 *
 * @param {import('@playwright/test').Page} page la página, ya en Cambiar contraseña
 * @param {string} actual la contraseña actual
 * @param {string} nueva la contraseña nueva
 */
async function cambiar(page, actual, nueva) {
  await page.getByLabel('Contraseña actual', { exact: true }).fill(actual);
  await page.getByLabel('Contraseña nueva', { exact: true }).fill(nueva);
  await page.getByLabel('Repite la contraseña nueva', { exact: true }).fill(nueva);
  await page.getByRole('button', { name: 'Guardar' }).click();
}

test('RF-18 · C1 · cambia la contraseña: la nueva inicia sesión y la anterior ya no', async ({ page, usuarioFinal }) => {
  const nueva = 'Granos del Huila 2026';
  await iniciarSesion(page, usuarioFinal);
  await page.goto('/cambiar-contrasena');

  await cambiar(page, usuarioFinal.contrasena, nueva);
  await expect(page.getByRole('status')).toContainText('Tu contraseña cambió.');

  await page.getByRole('button', { name: /^Mi cuenta:/ }).click();
  await page.getByRole('menuitem', { name: 'Cerrar sesión' }).click();
  await expect(page.getByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();
  await escribirYEntrar(page, usuarioFinal);
  await expect(page.getByRole('alert')).toContainText('Correo o contraseña incorrectos');
  await escribirYEntrar(page, { ...usuarioFinal, contrasena: nueva });
  await expect(page.getByRole('heading', { name: 'Hola, Luis' })).toBeVisible();
});

test('RF-18 · C2 · con la contraseña actual errada no cambia nada, y después de 5 fallos seguidos avisa el bloqueo con la hora en que termina', async ({ page, usuarioFinal }) => {
  await iniciarSesion(page, usuarioFinal);
  await page.goto('/cambiar-contrasena');

  for (let intento = 1; intento <= 4; intento += 1) {
    await cambiar(page, `no-es-${intento}`, 'Granos del Huila 2026');
    await expect(page.getByText(`La contraseña actual no es correcta. ${AYUDA}`)).toBeVisible();
  }
  await cambiar(page, 'no-es-5', 'Granos del Huila 2026');

  const aviso = page.getByRole('alert');
  await expect(aviso).toContainText('Tu cuenta está bloqueada por intentos fallidos');
  await expect(aviso).toContainText(/Podrás intentarlo a las \d{1,2}:\d{2}/);
});

test('RF-18 · C3 · la contraseña nueva acepta de 8 a 64 caracteres: con 7 o con 65 se rechaza (prueba de límite)', async ({ page, usuarioFinal }) => {
  await iniciarSesion(page, usuarioFinal);
  await page.goto('/cambiar-contrasena');

  await cambiar(page, usuarioFinal.contrasena, 'a'.repeat(7));
  await expect(page.getByText('La contraseña debe tener al menos 8 caracteres.')).toBeVisible();
  await cambiar(page, usuarioFinal.contrasena, 'a'.repeat(65));
  await expect(page.getByText('La contraseña puede tener como máximo 64 caracteres.')).toBeVisible();
  await cambiar(page, usuarioFinal.contrasena, 'a'.repeat(8));
  await expect(page.getByRole('status')).toContainText('Tu contraseña cambió.');
});

test('RF-18 · C4 · una contraseña nueva de 64 caracteres con 8 eñes (72 bytes) se acepta; con 9 eñes (73 bytes) se rechaza (prueba de límite)', async ({ page, usuarioFinal }) => {
  await iniciarSesion(page, usuarioFinal);
  await page.goto('/cambiar-contrasena');

  await cambiar(page, usuarioFinal.contrasena, 'ñ'.repeat(9) + 'a'.repeat(55));
  await expect(page.getByText('La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes')).toBeVisible();
  await cambiar(page, usuarioFinal.contrasena, 'ñ'.repeat(8) + 'a'.repeat(56));
  await expect(page.getByRole('status')).toContainText('Tu contraseña cambió.');
});

test('RF-18 · C5 · al cambiarla, la sesión desde la que se cambió sigue abierta y la de otro navegador deja de servir', async ({ browser, page, usuarioFinal }) => {
  const otroNavegador = await browser.newContext();
  const otra = await otroNavegador.newPage();
  await iniciarSesion(otra, usuarioFinal);
  await iniciarSesion(page, usuarioFinal);
  await page.goto('/cambiar-contrasena');

  await cambiar(page, usuarioFinal.contrasena, 'Granos del Huila 2026');
  await expect(page.getByRole('status')).toContainText('Tu contraseña cambió.');

  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Hola, Luis' })).toBeVisible();
  await otra.reload();
  await expect(otra.getByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();
  await expect(otra.getByText('Tu contraseña cambió: inicia sesión de nuevo.')).toBeVisible();
  await otroNavegador.close();
});
