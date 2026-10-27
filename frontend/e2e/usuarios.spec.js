/*
 * Nombre: usuarios.spec.js
 * Descripción: Pruebas de punta a punta de la pantalla de usuarios (RF-03) contra el backend real del entorno
 *              aislado, en PC y en celular: crear un usuario que entra, el correo repetido, los límites de la
 *              contraseña al crearla y al restablecerla, el máximo de 25 activos, desactivar y reactivar, lo que
 *              el administrador no puede hacer consigo mismo, los usuarios de otra empresa, los roles que se
 *              pueden asignar y dónde va el formulario, también en una ventana de PC de 1250 px. Cada prueba usa
 *              su propia empresa, registrada por la API, y entra con su administrador: así no choca con las de
 *              los otros navegadores ni llena los 25 activos de la empresa de prueba. Los permisos, la
 *              accesibilidad y el ancho de celular de la pantalla los revisa su prueba generada
 *              (generated/usuario.spec.js).
 * Fecha de creación: 27/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { randomUUID } from 'node:crypto';
import {
  botonDeLaFila, cerrarSesion, CONTRASENA, crearEmpresa, datos, escribirYEntrar, expect, iniciarSesion, test,
  tokenDe,
} from './ayudas.js';

/** La pantalla en la app. */
const RUTA = '/usuarios';

/** Los mensajes de las reglas y validaciones de usuario, como los responde la API. */
const MENSAJES = {
  maximo: 'La empresa ya tiene 25 usuarios activos: desactiva uno antes de crear o reactivar otro.',
  rolPropio: 'No puedes cambiar tu propio rol.',
  contrasenaPropia: 'Para cambiar tu propia contraseña, usa «Cambiar contraseña».',
  rolNoPermitido: 'Solo puedes asignar los roles Usuario final o Administrador de empresa.',
  correoRepetido: 'Ya existe un usuario con este correo.',
  bytes: 'La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes',
  desactivado: 'Tu usuario está desactivado: pide al administrador de tu empresa que lo reactive.',
};

/** Contraseñas del borde (RF-03 C3 y C4): todas de 64 caracteres; con 8 eñes ocupan 72 bytes y con 9, 73. */
const CON_64 = 'Cacao-del-Huila-'.repeat(4);
const CON_8_ENES = `${'ñ'.repeat(8)}${'a'.repeat(56)}`;
const CON_9_ENES = `${'ñ'.repeat(9)}${'a'.repeat(55)}`;

/**
 * Los datos de un usuario nuevo, con un correo distinto en cada prueba, como pedro.salas.1a2b3c4d@cacaoandino.test.
 *
 * @param {string} nombre su nombre, como «Pedro Salas»
 * @param {object} [cambios] lo que cambia de los datos por defecto: la contraseña, el rol o el correo
 * @returns {{ nombre: string, correo: string, contrasena: string, rol: string }} el usuario
 */
function usuarioNuevo(nombre, cambios = {}) {
  const local = nombre.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().replace(/\s+/g, '.');
  return {
    nombre, correo: `${local}.${randomUUID().slice(0, 8)}@cacaoandino.test`, contrasena: CONTRASENA, rol: 'USUARIO_FINAL',
    ...cambios,
  };
}

/**
 * Crea usuarios por la API en la empresa de un administrador, con un solo inicio de sesión.
 *
 * @param {import('@playwright/test').APIRequestContext} request el cliente de la API de Playwright
 * @param {{ correo: string, contrasena: string }} administrador el administrador de la empresa
 * @param {object[]} usuarios los usuarios, como los arma usuarioNuevo
 * @returns {Promise<object[]>} los usuarios, con su id
 */
async function crearUsuarios(request, administrador, usuarios) {
  const headers = { Authorization: `Bearer ${await tokenDe(request, administrador)}` };
  const creados = [];
  for (const usuario of usuarios) {
    const respuesta = await request.post('/api/v1/usuarios', { data: usuario, headers });
    expect(respuesta.status(), `crear a ${usuario.nombre}`).toBe(201);
    creados.push({ ...usuario, id: (await respuesta.json()).id });
  }
  return creados;
}

/**
 * Desactiva usuarios por la API, con el administrador de su empresa.
 *
 * @param {import('@playwright/test').APIRequestContext} request el cliente de la API de Playwright
 * @param {{ correo: string, contrasena: string }} administrador el administrador de la empresa
 * @param {Array<{ id: string }>} usuarios los usuarios
 */
async function desactivarUsuarios(request, administrador, usuarios) {
  const headers = { Authorization: `Bearer ${await tokenDe(request, administrador)}` };
  for (const usuario of usuarios) {
    const respuesta = await request.post(`/api/v1/usuarios/${usuario.id}/desactivar`, { headers });
    expect(respuesta.status()).toBe(200);
  }
}

/**
 * Entra como el administrador de la empresa, abre la pantalla de usuarios y espera su lista.
 *
 * @param {import('@playwright/test').Page} page la página
 * @param {{ nombre: string, correo: string, contrasena: string }} administrador el administrador de la empresa
 */
async function abrirUsuarios(page, administrador) {
  await iniciarSesion(page, administrador);
  await page.goto(RUTA);
  await expect(page.getByRole('heading', { name: 'Usuarios', level: 1 })).toBeVisible();
  await expect(page.getByRole('progressbar')).toHaveCount(0);
}

/**
 * La lista de usuarios: una tabla en el PC y tarjetas en el celular.
 *
 * @param {import('@playwright/test').Page} page la página
 * @param {boolean} esCelular si la prueba corre en tamaño de celular
 * @returns {import('@playwright/test').Locator} la lista
 */
function listaDeUsuarios(page, esCelular) {
  return page.getByRole(esCelular ? 'list' : 'table', { name: 'Usuarios de la empresa' });
}

/**
 * Los usuarios de la lista: las filas de la tabla, sin la de los títulos, o las tarjetas.
 *
 * @param {import('@playwright/test').Page} page la página
 * @param {boolean} esCelular si la prueba corre en tamaño de celular
 * @returns {import('@playwright/test').Locator} las filas o las tarjetas
 */
function usuariosDeLaLista(page, esCelular) {
  const lista = listaDeUsuarios(page, esCelular);
  return esCelular ? lista.getByRole('listitem') : lista.locator('tbody').getByRole('row');
}

/**
 * La fila o la tarjeta de un usuario, por su nombre.
 *
 * @param {import('@playwright/test').Page} page la página
 * @param {boolean} esCelular si la prueba corre en tamaño de celular
 * @param {string} nombre el nombre del usuario
 * @returns {import('@playwright/test').Locator} la fila o la tarjeta
 */
function usuarioDeLaLista(page, esCelular, nombre) {
  return usuariosDeLaLista(page, esCelular).filter({ hasText: nombre });
}

/**
 * Abre el formulario «Nuevo usuario» y escribe los datos; el rol solo se elige si no es el inicial, Usuario final.
 *
 * @param {import('@playwright/test').Page} page la página
 * @param {{ nombre: string, correo: string, contrasena: string, rol: string }} usuario los datos
 * @returns {Promise<import('@playwright/test').Locator>} el formulario
 */
async function llenarNuevoUsuario(page, usuario) {
  await page.getByRole('button', { name: 'Nuevo usuario' }).click();
  const formulario = page.getByRole('form', { name: 'Nuevo usuario' });
  await formulario.getByLabel('Nombre', { exact: true }).fill(usuario.nombre);
  await formulario.getByLabel('Correo', { exact: true }).fill(usuario.correo);
  await formulario.getByLabel('Contraseña inicial', { exact: true }).fill(usuario.contrasena);
  if (usuario.rol !== 'USUARIO_FINAL') {
    await formulario.getByLabel('Rol', { exact: true }).selectOption(usuario.rol);
  }
  return formulario;
}

/**
 * Abre «Nuevo usuario» y mide dónde quedaron el formulario y la lista.
 *
 * @param {import('@playwright/test').Page} page la página, con la lista ya cargada
 * @param {boolean} esCelular si la prueba corre en tamaño de celular
 * @returns {Promise<{ formulario: object, lista: object }>} sus cajas, como las da boundingBox
 */
async function cajasDelFormularioYLaLista(page, esCelular) {
  await page.getByRole('button', { name: 'Nuevo usuario' }).click();
  return {
    formulario: await page.getByRole('form', { name: 'Nuevo usuario' }).boundingBox(),
    lista: await listaDeUsuarios(page, esCelular).boundingBox(),
  };
}

/**
 * Revisa que el formulario esté al lado de la lista: empieza a su derecha y a su altura, no debajo.
 *
 * @param {{ formulario: object, lista: object }} cajas las del formulario y la lista
 */
function revisarQueVaAlLado({ formulario, lista }) {
  expect(formulario.x, 'el formulario empieza a la derecha de la lista').toBeGreaterThanOrEqual(lista.x + lista.width);
  expect(formulario.y, 'el formulario empieza a la altura de la lista, no debajo').toBeLessThan(lista.y + lista.height);
}

/**
 * Cierra la sesión y entra con otra persona, sin esperar el resultado: puede no entrar.
 *
 * @param {import('@playwright/test').Page} page la página
 * @param {{ correo: string, contrasena: string }} persona quién entra
 */
async function entrarComo(page, persona) {
  await cerrarSesion(page);
  await escribirYEntrar(page, persona);
}

test('RF-03 · C1 · el usuario creado en la pantalla queda activo, con el rol elegido, e inicia sesión con su contraseña inicial (de punta a punta)', async ({ page, request, isMobile }) => {
  const empresa = await crearEmpresa(request);
  const pedro = usuarioNuevo('Pedro Salas', { rol: 'ADMIN_EMPRESA' });
  await abrirUsuarios(page, empresa.administrador);

  const formulario = await llenarNuevoUsuario(page, pedro);
  await formulario.getByRole('button', { name: 'Crear usuario' }).click();
  await expect(page.getByRole('status')).toContainText('Se guardaron los datos de Pedro Salas.');
  const fila = usuarioDeLaLista(page, isMobile, 'Pedro Salas');
  await expect(fila).toContainText(pedro.correo);
  await expect(fila).toContainText('Administrador de empresa');
  await expect(fila).toContainText('Activo');
  await expect(page.getByText('2 de 25 usuarios activos, contándote a ti.')).toBeVisible();

  await entrarComo(page, pedro);
  await expect(page.getByRole('heading', { name: 'Hola, Pedro' })).toBeVisible();
});

test('RF-03 · C2 · la pantalla no crea un usuario con el correo de alguien de otra empresa: lo avisa en el campo Correo (de punta a punta)', async ({ page, request, isMobile }) => {
  const empresa = await crearEmpresa(request);
  await abrirUsuarios(page, empresa.administrador);

  const formulario = await llenarNuevoUsuario(page, usuarioNuevo('Pedro Salas', { correo: datos.adminEmpresa.correo }));
  await formulario.getByRole('button', { name: 'Crear usuario' }).click();

  await expect(formulario.getByText(MENSAJES.correoRepetido)).toBeVisible();
  await expect(formulario.getByLabel('Correo', { exact: true })).toHaveAttribute('aria-invalid', 'true');
  // No se guardó: al volver a cargar la pantalla, la lista sigue solo con el administrador.
  await page.reload();
  await expect(usuariosDeLaLista(page, isMobile)).toHaveCount(1);
  await expect(page.getByText('1 de 25 usuarios activos, contándote a ti.')).toBeVisible();
});

test('RF-03 · C3 · la contraseña inicial con 7 o con 65 caracteres no se guarda; con 64 sí, y el usuario entra con ella (prueba de límite, de punta a punta)', async ({ page, request }) => {
  const empresa = await crearEmpresa(request);
  const pedro = usuarioNuevo('Pedro Salas', { contrasena: CON_64 });
  await abrirUsuarios(page, empresa.administrador);
  const formulario = await llenarNuevoUsuario(page, { ...pedro, contrasena: 'Cacao-1' });
  const contrasena = formulario.getByLabel('Contraseña inicial', { exact: true });
  const crear = formulario.getByRole('button', { name: 'Crear usuario' });

  await crear.click();
  await expect(formulario.getByText('La contraseña debe tener al menos 8 caracteres.')).toBeVisible();
  await contrasena.fill(`${CON_64}x`);
  await crear.click();
  await expect(formulario.getByText('La contraseña puede tener como máximo 64 caracteres.')).toBeVisible();
  await contrasena.fill(CON_64);
  await crear.click();
  await expect(page.getByRole('status')).toContainText('Se guardaron los datos de Pedro Salas.');

  await entrarComo(page, pedro);
  await expect(page.getByRole('heading', { name: 'Hola, Pedro' })).toBeVisible();
});

test('RF-03 · C4 · una contraseña inicial de 64 caracteres con 9 eñes no se guarda; con 8 eñes (72 bytes) sí, y el usuario entra con ella (prueba de límite, de punta a punta)', async ({ page, request }) => {
  const empresa = await crearEmpresa(request);
  const pedro = usuarioNuevo('Pedro Salas', { contrasena: CON_8_ENES });
  await abrirUsuarios(page, empresa.administrador);
  const formulario = await llenarNuevoUsuario(page, { ...pedro, contrasena: CON_9_ENES });
  const crear = formulario.getByRole('button', { name: 'Crear usuario' });

  await crear.click();
  await expect(formulario.getByText(MENSAJES.bytes)).toBeVisible();
  await formulario.getByLabel('Contraseña inicial', { exact: true }).fill(CON_8_ENES);
  await crear.click();
  await expect(page.getByRole('status')).toContainText('Se guardaron los datos de Pedro Salas.');

  await entrarComo(page, pedro);
  await expect(page.getByRole('heading', { name: 'Hola, Pedro' })).toBeVisible();
});

test('RF-03 · C4 · al restablecer en la pantalla la contraseña de otra persona, con 9 eñes no se guarda; con 8 eñes (72 bytes) sí, y esa persona entra con la nueva y ya no con la anterior (prueba de límite, de punta a punta)', async ({ page, request }) => {
  const empresa = await crearEmpresa(request);
  const [pedro] = await crearUsuarios(request, empresa.administrador, [usuarioNuevo('Pedro Salas')]);
  await abrirUsuarios(page, empresa.administrador);

  await page.getByRole('button', { name: 'Editar a Pedro Salas' }).click();
  const formulario = page.getByRole('form', { name: 'Editar usuario' });
  const nueva = formulario.getByLabel('Contraseña nueva', { exact: true });
  const guardar = formulario.getByRole('button', { name: 'Guardar cambios' });
  await nueva.fill(CON_9_ENES);
  await guardar.click();
  await expect(formulario.getByText(MENSAJES.bytes)).toBeVisible();
  await nueva.fill(CON_8_ENES);
  await guardar.click();
  await expect(page.getByRole('status')).toContainText('Se guardaron los datos de Pedro Salas.');

  await entrarComo(page, pedro);
  await expect(page.getByRole('alert')).toContainText('Correo o contraseña incorrectos');
  await escribirYEntrar(page, { ...pedro, contrasena: CON_8_ENES });
  await expect(page.getByRole('heading', { name: 'Hola, Pedro' })).toBeVisible();
});

test('RF-03 · C5 · al llegar a 25 usuarios activos, la pantalla no deja crear ni reactivar otro, y explica por qué (prueba de límite, de punta a punta)', async ({ page, request }) => {
  const empresa = await crearEmpresa(request);
  // El administrador y 23 activos son 24; Adriana queda desactivada, para intentar reactivarla.
  const [adriana] = await crearUsuarios(request, empresa.administrador, [usuarioNuevo('Adriana Rojas')]);
  await desactivarUsuarios(request, empresa.administrador, [adriana]);
  await crearUsuarios(request, empresa.administrador,
    Array.from({ length: 23 }, (_, i) => usuarioNuevo(`Usuario ${String(i + 1).padStart(2, '0')}`)));
  await abrirUsuarios(page, empresa.administrador);
  await expect(page.getByText('24 de 25 usuarios activos, contándote a ti.')).toBeVisible();

  const formulario = await llenarNuevoUsuario(page, usuarioNuevo('Pedro Salas'));
  await formulario.getByRole('button', { name: 'Crear usuario' }).click();
  await expect(page.getByText('25 de 25 usuarios activos, contándote a ti.')).toBeVisible();

  await expect(page.getByRole('status').filter({ hasText: MENSAJES.maximo })).toBeVisible();
  const nuevo = page.getByRole('button', { name: 'Nuevo usuario' });
  await expect(nuevo).toBeDisabled();
  await expect(nuevo).toHaveAccessibleDescription(MENSAJES.maximo);
  const reactivar = await botonDeLaFila(page, 'Reactivar a Adriana Rojas');
  await expect(reactivar).toBeDisabled();
  await expect(reactivar).toHaveAccessibleDescription(MENSAJES.maximo);
});

test('RF-03 · C6 · al desactivar un usuario en la pantalla, ya no puede iniciar sesión (de punta a punta)', async ({ page, request, isMobile }) => {
  const empresa = await crearEmpresa(request);
  const [pedro] = await crearUsuarios(request, empresa.administrador, [usuarioNuevo('Pedro Salas')]);
  await abrirUsuarios(page, empresa.administrador);

  await page.getByRole('button', { name: 'Desactivar a Pedro Salas' }).click();
  await expect(page.getByRole('status')).toContainText('Se desactivó a Pedro Salas.');
  await expect(usuarioDeLaLista(page, isMobile, 'Pedro Salas')).toContainText('Desactivado');

  await entrarComo(page, pedro);
  await expect(page.getByRole('alert')).toContainText(MENSAJES.desactivado);
});

test('RF-03 · al reactivar un usuario en la pantalla, vuelve a iniciar sesión (de punta a punta)', async ({ page, request, isMobile }) => {
  const empresa = await crearEmpresa(request);
  const [pedro] = await crearUsuarios(request, empresa.administrador, [usuarioNuevo('Pedro Salas')]);
  await desactivarUsuarios(request, empresa.administrador, [pedro]);
  await abrirUsuarios(page, empresa.administrador);

  await page.getByRole('button', { name: 'Reactivar a Pedro Salas' }).click();
  await expect(page.getByRole('status')).toContainText('Se reactivó a Pedro Salas.');
  await expect(usuarioDeLaLista(page, isMobile, 'Pedro Salas')).toContainText('Activo');

  await entrarComo(page, pedro);
  await expect(page.getByRole('heading', { name: 'Hola, Pedro' })).toBeVisible();
});

test('RF-03 · C7 · la fila del administrador lleva «Tú» y no tiene «Desactivar»; la de otro usuario activo sí lo tiene (de punta a punta)', async ({ page, request, isMobile }) => {
  const empresa = await crearEmpresa(request);
  await crearUsuarios(request, empresa.administrador, [usuarioNuevo('Pedro Salas')]);
  await abrirUsuarios(page, empresa.administrador);

  const propia = usuarioDeLaLista(page, isMobile, empresa.administrador.nombre);
  await expect(propia.getByText('Tú', { exact: true })).toBeVisible();
  await expect(propia.getByRole('button', { name: `Editar a ${empresa.administrador.nombre}` })).toBeVisible();
  await expect(propia.getByRole('button', { name: `Desactivar a ${empresa.administrador.nombre}` })).toHaveCount(0);
  await expect(usuarioDeLaLista(page, isMobile, 'Pedro Salas').getByRole('button', { name: 'Desactivar a Pedro Salas' }))
    .toBeVisible();
});

test('RF-03 · C8 · al editar su propia fila, el rol está deshabilitado y explica por qué, y al guardar sigue igual (de punta a punta)', async ({ page, request, isMobile }) => {
  const empresa = await crearEmpresa(request);
  await abrirUsuarios(page, empresa.administrador);

  await page.getByRole('button', { name: `Editar a ${empresa.administrador.nombre}` }).click();
  const formulario = page.getByRole('form', { name: 'Editar usuario' });
  const rol = formulario.getByLabel('Rol', { exact: true });
  await expect(rol).toBeDisabled();
  await expect(rol).toHaveValue('ADMIN_EMPRESA');
  await expect(rol).toHaveAccessibleDescription(new RegExp(MENSAJES.rolPropio));
  await formulario.getByLabel('Nombre', { exact: true }).fill('Laura María Gómez');
  await formulario.getByRole('button', { name: 'Guardar cambios' }).click();

  await expect(page.getByRole('status')).toContainText('Se guardaron los datos de Laura María Gómez.');
  await expect(usuarioDeLaLista(page, isMobile, 'Laura María Gómez')).toContainText('Administrador de empresa');
});

test('RF-03 · C9 · la lista solo tiene los usuarios de su empresa: no aparecen los de otra (de punta a punta)', async ({ page, request, isMobile }) => {
  const empresa = await crearEmpresa(request);
  await abrirUsuarios(page, empresa.administrador);

  await expect(usuariosDeLaLista(page, isMobile)).toHaveCount(1);
  await expect(usuarioDeLaLista(page, isMobile, empresa.administrador.correo)).toHaveCount(1);
  for (const deOtraEmpresa of [datos.adminEmpresa, datos.usuarioFinal]) {
    await expect(page.getByText(deOtraEmpresa.correo)).toHaveCount(0);
  }
  await expect(page.getByText('1 de 25 usuarios activos, contándote a ti.')).toBeVisible();
});

test('RF-03 · C10 · el formulario solo ofrece Usuario final y Administrador de empresa, y la API responde «prohibido» (403) al rol ADMIN_PLATAFORMA sin guardar nada (prueba de seguridad, de punta a punta)', async ({ page, request, isMobile }) => {
  const empresa = await crearEmpresa(request);
  await abrirUsuarios(page, empresa.administrador);

  await page.getByRole('button', { name: 'Nuevo usuario' }).click();
  await expect(page.getByRole('form', { name: 'Nuevo usuario' }).getByLabel('Rol', { exact: true }).getByRole('option'))
    .toHaveText(['Usuario final', 'Administrador de empresa']);

  const respuesta = await request.post('/api/v1/usuarios', {
    data: usuarioNuevo('Pedro Salas', { rol: 'ADMIN_PLATAFORMA' }),
    headers: { Authorization: `Bearer ${await tokenDe(request, empresa.administrador)}` },
  });
  expect(respuesta.status()).toBe(403);
  expect((await respuesta.json()).errores).toEqual([{ campo: 'rol', mensaje: MENSAJES.rolNoPermitido }]);
  // No se guardó: al volver a cargar la pantalla, la lista sigue solo con el administrador.
  await page.reload();
  await expect(usuariosDeLaLista(page, isMobile)).toHaveCount(1);
});

test('RF-03 · C11 · al editar su propia fila no aparece la contraseña: el mensaje de la regla lleva a «Cambiar contraseña» y su enlace la abre (de punta a punta)', async ({ page, request }) => {
  const empresa = await crearEmpresa(request);
  await abrirUsuarios(page, empresa.administrador);

  await page.getByRole('button', { name: `Editar a ${empresa.administrador.nombre}` }).click();
  const formulario = page.getByRole('form', { name: 'Editar usuario' });
  await expect(formulario.getByLabel('Contraseña nueva', { exact: true })).toHaveCount(0);
  await expect(formulario.getByText(MENSAJES.contrasenaPropia)).toBeVisible();
  await formulario.getByRole('link', { name: 'Cambiar contraseña' }).click();

  await expect(page).toHaveURL(/\/cambiar-contrasena$/);
  await expect(page.getByRole('heading', { name: 'Cambiar contraseña', level: 1 })).toBeVisible();
});

test('RF-03 · el formulario va al lado de la lista en el PC y encima de ella en el celular (de punta a punta)', async ({ page, request, isMobile }) => {
  const empresa = await crearEmpresa(request);
  await abrirUsuarios(page, empresa.administrador);

  const cajas = await cajasDelFormularioYLaLista(page, isMobile);
  if (isMobile) {
    expect(cajas.formulario.y + cajas.formulario.height, 'el formulario termina antes de la lista')
      .toBeLessThanOrEqual(cajas.lista.y);
  } else {
    revisarQueVaAlLado(cajas);
  }
});

test('RF-03 · en una ventana de PC de 1250 px de ancho, el formulario también va al lado de la lista (de punta a punta)', async ({ page, request, isMobile }) => {
  test.skip(isMobile, 'En el celular, el formulario va encima de la lista');
  await page.setViewportSize({ width: 1250, height: 800 });
  const empresa = await crearEmpresa(request);
  await abrirUsuarios(page, empresa.administrador);

  revisarQueVaAlLado(await cajasDelFormularioYLaLista(page, false));
});
