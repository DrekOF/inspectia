/*
 * Nombre: empresas.spec.js
 * Descripción: Pruebas de punta a punta de la pantalla de empresas (RF-02) contra el backend real del entorno
 *              aislado, en PC y en celular: registrar una empresa y que su administrador entre, desactivarla,
 *              reactivarla y restablecer la contraseña de su administrador. Cada prueba usa su propia
 *              empresa, para no chocar con las de los otros navegadores. Los permisos, la accesibilidad y el
 *              ancho de celular de la pantalla los revisa su prueba generada (generated/empresa.spec.js).
 * Fecha de creación: 26/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import {
  botonDeLaFila, cerrarSesion, CONTRASENA, crearEmpresa, datos, desactivarEmpresa, empresaNueva, escribirYEntrar,
  expect, iniciarSesion, test,
} from './ayudas.js';

/**
 * Abre la pantalla de empresas como administrador de la plataforma y espera su lista.
 *
 * @param {import('@playwright/test').Page} page la página
 */
async function abrirEmpresas(page) {
  await iniciarSesion(page, datos.plataforma);
  await page.goto('/empresas');
  await expect(page.getByRole('heading', { name: 'Empresas', level: 1 })).toBeVisible();
  await expect(page.getByRole('progressbar')).toHaveCount(0);
}

/**
 * Cierra la sesión y entra con el administrador de la empresa.
 *
 * @param {import('@playwright/test').Page} page la página
 * @param {{ correo: string, contrasena: string }} administrador el administrador de la empresa
 */
async function entrarComoSuAdministrador(page, administrador) {
  await cerrarSesion(page);
  await escribirYEntrar(page, administrador);
}

test('RF-02 · C8 · la empresa registrada en la pantalla queda con su administrador, que entra con la contraseña inicial (de punta a punta)', async ({ page }) => {
  const empresa = empresaNueva(CONTRASENA);
  await abrirEmpresas(page);

  await page.getByRole('button', { name: 'Nueva empresa' }).click();
  const deLaEmpresa = page.getByRole('group', { name: 'Empresa' });
  await deLaEmpresa.getByLabel('Nombre', { exact: true }).fill(empresa.nombre);
  await deLaEmpresa.getByLabel('NIT', { exact: true }).fill(empresa.nit);
  await deLaEmpresa.getByLabel('Ciudad', { exact: true }).fill(empresa.ciudad);
  const administrador = page.getByRole('group', { name: 'Primer administrador' });
  await administrador.getByLabel('Nombre', { exact: true }).fill(empresa.administrador.nombre);
  await administrador.getByLabel('Correo', { exact: true }).fill(empresa.administrador.correo);
  await administrador.getByLabel('Contraseña inicial', { exact: true }).fill(empresa.administrador.contrasena);
  await page.getByRole('button', { name: 'Registrar empresa' }).click();
  await expect(page.getByRole('status')).toContainText(`Se guardó ${empresa.nombre}.`);

  await entrarComoSuAdministrador(page, empresa.administrador);
  await expect(page.getByRole('heading', { name: 'Hola, Laura' })).toBeVisible();
});

test('RF-02 · C9 · al desactivar una empresa en la pantalla, después de confirmarlo, su administrador ya no puede iniciar sesión (de punta a punta)', async ({ page, request }) => {
  const empresa = await crearEmpresa(request);
  await abrirEmpresas(page);

  await (await botonDeLaFila(page, `Desactivar ${empresa.nombre}`)).click();
  const confirmar = page.getByRole('dialog', { name: `¿Desactivar ${empresa.nombre}?` });
  await expect(confirmar).toContainText('Sus usuarios no podrán iniciar sesión hasta que la reactives. No se borra ningún dato.');
  await confirmar.getByRole('button', { name: 'Desactivar' }).click();
  await expect(page.getByRole('status')).toContainText(`Se desactivó ${empresa.nombre}.`);

  await entrarComoSuAdministrador(page, empresa.administrador);
  await expect(page.getByRole('alert')).toContainText('Tu empresa está desactivada: comunícate con el equipo de InspectIA.');
});

test('RF-02 · C10 · al reactivar una empresa en la pantalla, su administrador vuelve a iniciar sesión (de punta a punta)', async ({ page, request }) => {
  const empresa = await crearEmpresa(request);
  await desactivarEmpresa(request, empresa.id);
  await abrirEmpresas(page);

  await (await botonDeLaFila(page, `Reactivar ${empresa.nombre}`)).click();
  await expect(page.getByRole('status')).toContainText(`Se reactivó ${empresa.nombre}.`);

  await entrarComoSuAdministrador(page, empresa.administrador);
  await expect(page.getByRole('heading', { name: 'Hola, Laura' })).toBeVisible();
});

test('RF-02 · C12 · el administrador de la plataforma restablece en la pantalla la contraseña del administrador de una empresa: la nueva entra y la anterior ya no (de punta a punta)', async ({ page, request }) => {
  const empresa = await crearEmpresa(request);
  const nueva = 'Mazorca-Nueva-2026';
  await abrirEmpresas(page);

  await (await botonDeLaFila(page, `Administradores de ${empresa.nombre}`)).click();
  const panel = page.getByRole('region', { name: `Administradores de ${empresa.nombre}` });
  await panel.getByRole('button', { name: `Restablecer la contraseña de ${empresa.administrador.nombre}` }).click();
  await panel.getByLabel('Contraseña nueva', { exact: true }).fill(nueva);
  await panel.getByRole('button', { name: 'Restablecer contraseña' }).click();
  await expect(panel.getByRole('status')).toContainText(`Se restableció la contraseña de ${empresa.administrador.nombre}.`);

  await entrarComoSuAdministrador(page, empresa.administrador);
  await expect(page.getByRole('alert')).toContainText('Correo o contraseña incorrectos');
  await escribirYEntrar(page, { ...empresa.administrador, contrasena: nueva });
  await expect(page.getByRole('heading', { name: 'Hola, Laura' })).toBeVisible();
});
