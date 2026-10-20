/*
 * Nombre: iniciar-sesion.spec.js
 * Descripción: Pruebas de punta a punta de Iniciar sesión (RF-01, RNF-04) contra el backend real del entorno
 *              aislado, en PC y en celular: el menú de cada rol, los datos errados, las cuentas desactivadas,
 *              el bloqueo, el vencimiento de la sesión y la contraseña de más de 72 bytes.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import {
  abrirMenu, crearUsuario, datos, desactivarUsuario, escribirYEntrar, expect, iniciarSesion, test,
} from './ayudas.js';

const INCORRECTOS = 'Correo o contraseña incorrectos';
const AYUDA = 'Si fallas 5 veces seguidas, la cuenta se bloquea 15 minutos.';

/**
 * Entra con una persona y revisa las entradas de su menú, en orden, sin las de otros roles.
 *
 * @param {import('@playwright/test').Page} page la página
 * @param {boolean} esCelular si corre en tamaño de celular
 * @param {object} persona quién entra
 * @param {string[]} entradas las entradas que debe ver
 */
async function veSuMenu(page, esCelular, persona, entradas) {
  await iniciarSesion(page, persona);
  const menu = await abrirMenu(page, esCelular);
  await expect(menu.getByRole('link')).toHaveText([...entradas, 'Cambiar contraseña', 'Acerca de']);
  await expect(menu.getByRole('button', { name: 'Cerrar sesión' })).toBeVisible();
}

test('RF-01 · C1 · el administrador de la plataforma entra y ve el menú de su rol', async ({ page, isMobile }) => {
  await veSuMenu(page, isMobile, datos.plataforma, ['Inicio', 'Empresas']);
});

test('RF-01 · C1 · el administrador de empresa entra y ve el menú de su rol', async ({ page, isMobile }) => {
  await veSuMenu(page, isMobile, datos.adminEmpresa,
    ['Inicio', 'Historial', 'Usuarios', 'Rangos de recomendación', 'Estadísticas', 'Modelo de IA']);
});

test('RF-01 · C1 · el usuario final entra y ve el menú de su rol', async ({ page, isMobile, usuarioFinal }) => {
  await veSuMenu(page, isMobile, usuarioFinal, ['Inicio', 'Nuevo lote', 'Historial']);
});

test('RF-01 · C2 · con datos errados ve «Correo o contraseña incorrectos» y la regla del bloqueo, igual con un correo que no existe', async ({ page, usuarioFinal }) => {
  // Un usuario propio: los fallos cuentan para el bloqueo, y el administrador lo usan las demás pruebas.
  for (const correo of [usuarioFinal.correo, 'nadie@cacaosdelhuila.test']) {
    await page.goto('/iniciar-sesion');
    await escribirYEntrar(page, { correo, contrasena: 'no-es-la-clave' });

    const aviso = page.getByRole('alert');
    await expect(aviso).toContainText(INCORRECTOS);
    await expect(aviso).toContainText(AYUDA);
    await expect(page).toHaveURL(/\/iniciar-sesion$/);
  }
});

test('RF-01 · C3 · un usuario desactivado no entra y ve el motivo', async ({ page, request, usuarioFinal }) => {
  await desactivarUsuario(request, usuarioFinal.id);

  await page.goto('/iniciar-sesion');
  await escribirYEntrar(page, usuarioFinal);

  await expect(page.getByRole('alert'))
    .toContainText('Tu usuario está desactivado: pide al administrador de tu empresa que lo reactive.');
});

test('RF-01 · C4 · un usuario de una empresa desactivada no entra y ve el motivo', async ({ page }) => {
  await page.goto('/iniciar-sesion');
  await escribirYEntrar(page, datos.empresaDesactivada.usuario);

  await expect(page.getByRole('alert')).toContainText('Tu empresa está desactivada: comunícate con el equipo de InspectIA.');
});

test('RF-01 · C5 · después de 5 intentos fallidos seguidos la cuenta se bloquea: el sexto no entra aunque la contraseña sea la correcta', async ({ page, usuarioFinal }) => {
  await page.goto('/iniciar-sesion');

  for (let intento = 1; intento <= 5; intento += 1) {
    await escribirYEntrar(page, { correo: usuarioFinal.correo, contrasena: `no-es-${intento}` });
    await expect(page.getByRole('alert')).toContainText(INCORRECTOS);
  }
  await escribirYEntrar(page, usuarioFinal);

  await expect(page.getByRole('alert')).toContainText(INCORRECTOS);
  await expect(page).toHaveURL(/\/iniciar-sesion$/);
});

test('RF-01 · C6 · la sesión vence a las 8 horas y la app pide iniciar sesión de nuevo', async ({ page }) => {
  await page.clock.install();
  await iniciarSesion(page, datos.adminEmpresa);

  await page.clock.fastForward('08:00:01');

  await expect(page.getByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();
  await expect(page.getByText('Tu sesión venció: inicia sesión de nuevo.')).toBeVisible();
});

test('RF-01 · C8 · una contraseña de más de 72 bytes no entra aunque sus primeros 72 coincidan con la guardada: ve lo mismo que con cualquier fallo', async ({ page, request }) => {
  // 64 caracteres y 72 bytes, el máximo que se puede guardar; la que se escribe tiene 1 byte más.
  const guardada = `${'ñ'.repeat(8)}${'a'.repeat(56)}`;
  const persona = await crearUsuario(request, 'USUARIO_FINAL', guardada);
  try {
    await page.goto('/iniciar-sesion');
    await escribirYEntrar(page, { correo: persona.correo, contrasena: `${guardada}x` });

    const aviso = page.getByRole('alert');
    await expect(aviso).toContainText(INCORRECTOS);
    await expect(aviso).toContainText(AYUDA);
    await expect(page).toHaveURL(/\/iniciar-sesion$/);
  } finally {
    await desactivarUsuario(request, persona.id);
  }
});

test('RNF-04 · sin sesión, una pantalla interna lleva a Iniciar sesión', async ({ page }) => {
  await page.goto('/cambiar-contrasena');

  await expect(page.getByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();
});
