/*
 * Nombre: rangos.spec.js
 * Descripción: Pruebas de punta a punta de la pantalla de rangos (RF-13) contra el backend real del entorno aislado,
 *              en PC y en celular: si los límites no bajan en orden no se guarda nada (C1); después de guardar unos
 *              rangos propios, «Restaurar valores por defecto» vuelve a los de D-10 (C4); y un límite escrito con dos
 *              decimales, como 90,50, queda como lo guarda la API, 90,5. Cada prueba usa su propia empresa,
 *              registrada por la API, porque guardar cambia los rangos de la empresa. Los permisos (C5), la
 *              accesibilidad y el ancho de celular los revisa su prueba generada (generated/rango_recomendacion.spec.js).
 * Fecha de creación: 28/10/2026
 * Actualización: 28/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { crearEmpresa, expect, iniciarSesion, test } from './ayudas.js';

/** La pantalla en la app. */
const RUTA = '/rangos-recomendacion';

/** Las etiquetas de los campos de cada fila. */
const LIMITE = 'Límite inferior (% de granos sanos)';
const RECOMENDACION = 'Recomendación';

/** Las categorías, en el orden de la pantalla, y sus límites por defecto (D-10), como los muestra. */
const CATEGORIAS = ['Premium o Especial', 'Estándar', 'Corriente', 'Fuera de norma'];
const LIMITES_DE_D10 = ['96,0', '90,0', '82,0', '0,0'];

/** Los mensajes que ve el administrador. */
const MENSAJES = {
  orden: 'Los límites deben bajar en orden: Premium o Especial > Estándar > Corriente > 0 %.',
  guardados: 'Se guardaron los rangos.',
  restaurados: 'Se restauraron los valores por defecto.',
  seQuitan: 'Se pierden los límites y los textos propios de la empresa.',
};

/**
 * Espera la pantalla de rangos con su formulario cargado, al abrirla o al recargarla.
 *
 * @param {import('@playwright/test').Page} page la página
 */
async function esperarElFormulario(page) {
  await expect(page.getByRole('heading', { name: 'Rangos de recomendación', level: 1 })).toBeVisible();
  await expect(page.getByRole('form', { name: 'Editar los rangos' })).toBeVisible();
}

/**
 * Entra como el administrador de la empresa y abre la pantalla de rangos.
 *
 * @param {import('@playwright/test').Page} page la página
 * @param {{ nombre: string, correo: string, contrasena: string }} administrador el administrador de la empresa
 */
async function abrirRangos(page, administrador) {
  await iniciarSesion(page, administrador);
  await page.goto(RUTA);
  await esperarElFormulario(page);
}

/**
 * Recarga la página y espera el formulario: lo que muestra es lo que quedó guardado.
 *
 * @param {import('@playwright/test').Page} page la página
 */
async function recargar(page) {
  await page.reload();
  await esperarElFormulario(page);
}

/**
 * Un campo de una categoría.
 *
 * @param {import('@playwright/test').Page} page la página
 * @param {string} categoria el nombre de la categoría, como «Estándar»
 * @param {string} [etiqueta] la etiqueta del campo; por defecto, el límite inferior
 * @returns {import('@playwright/test').Locator} el campo
 */
function campo(page, categoria, etiqueta = LIMITE) {
  return page.getByRole('group', { name: categoria }).getByLabel(etiqueta, { exact: true });
}

/**
 * Pulsa «Guardar cambios».
 *
 * @param {import('@playwright/test').Page} page la página
 */
async function guardar(page) {
  await page.getByRole('button', { name: 'Guardar cambios' }).click();
}

test('RF-13 · C1 · si los límites no bajan en orden (Estándar 97 % y Premium 96 %), no se guarda: el aviso lo explica y, al recargar, Estándar sigue en 90,0 (de punta a punta)', async ({ page, request }) => {
  const empresa = await crearEmpresa(request);
  await abrirRangos(page, empresa.administrador);

  await campo(page, 'Estándar').fill('97');
  await guardar(page);

  await expect(page.getByRole('alert')).toContainText(MENSAJES.orden);
  await expect(campo(page, 'Premium o Especial')).toHaveAttribute('aria-invalid', 'true');
  await expect(campo(page, 'Estándar')).toHaveAttribute('aria-invalid', 'true');
  await recargar(page);
  await expect(campo(page, 'Estándar')).toHaveValue('90,0');
});

test('RF-13 · C4 · después de guardar unos rangos propios, «Restaurar valores por defecto» vuelve a los de D-10, también al recargar (de punta a punta)', async ({ page, request }) => {
  const empresa = await crearEmpresa(request);
  await abrirRangos(page, empresa.administrador);

  await campo(page, 'Premium o Especial').fill('97,5');
  await campo(page, 'Premium o Especial', RECOMENDACION).fill('Para el comprador de exportación.');
  await guardar(page);
  await expect(page.getByText(MENSAJES.guardados)).toBeVisible();
  await recargar(page);
  await expect(campo(page, 'Premium o Especial')).toHaveValue('97,5');
  await expect(campo(page, 'Premium o Especial', RECOMENDACION)).toHaveValue('Para el comprador de exportación.');

  await page.getByRole('button', { name: 'Restaurar valores por defecto' }).click();
  const dialogo = page.getByRole('dialog', { name: '¿Restaurar los valores por defecto?' });
  await expect(dialogo).toContainText(MENSAJES.seQuitan);
  await dialogo.getByRole('button', { name: 'Restaurar', exact: true }).click();
  await expect(page.getByText(MENSAJES.restaurados)).toBeVisible();

  await recargar(page);
  for (const [posicion, categoria] of CATEGORIAS.entries()) {
    await expect(campo(page, categoria), categoria).toHaveValue(LIMITES_DE_D10[posicion]);
  }
  await expect(campo(page, 'Premium o Especial', RECOMENDACION))
    .toHaveValue('Apto para mercado especial o de exportación.');
});

test('RF-13 · un límite escrito con dos decimales, como 90,50, se guarda y se ve como lo guarda la API, 90,5, también al recargar (de punta a punta)', async ({ page, request }) => {
  const empresa = await crearEmpresa(request);
  await abrirRangos(page, empresa.administrador);

  await campo(page, 'Estándar').fill('90,50');
  await guardar(page);

  await expect(page.getByText(MENSAJES.guardados)).toBeVisible();
  await expect(campo(page, 'Estándar')).toHaveValue('90,5');
  await recargar(page);
  await expect(campo(page, 'Estándar')).toHaveValue('90,5');
});
