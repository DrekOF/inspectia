/*
 * Nombre: GestionarUsuarios.test.jsx
 * Descripción: Pruebas de los criterios de RF-03 en la pantalla de usuarios, con la API simulada: abrirla desde el
 *              menú, crear un usuario (C1), el correo repetido (C2), los límites de la contraseña al crearla y al
 *              restablecerla (C3 y C4), el máximo de 25 activos (C5), lo que el administrador no puede hacer
 *              consigo mismo (C7, C8 y C11), los roles que se pueden asignar (C10) y la nota de D-30. Que el usuario
 *              creado entre (C1), que el desactivado no entre (C6) y que no se vean los de otra empresa (C9) se
 *              prueban de punta a punta.
 * Fecha de creación: 27/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { screen, within } from '@testing-library/react';
import { http } from 'msw';
import { describe, expect, it } from 'vitest';
import i18n from '../i18n/i18n.js';
import { abrirApp, conSesion, USUARIOS } from '../pruebas/ayudas.jsx';
import { apiSimulada } from '../pruebas/crud.js';
import { anchoDePantalla } from '../pruebas/preparacion.js';
import { problema, servidor } from '../pruebas/servidor.js';
import {
  abrirUsuarios, API_USUARIOS, CARLOS, DATOS_DE_USUARIO, llenarUsuario, LUISA, MENSAJES, OPCIONES_USUARIOS,
  reglaIncumplida, usuariosActivos, YO,
} from '../pruebas/usuarios.js';

/** El mensaje de la API cuando la contraseña ocupa más de 72 bytes. */
const BYTES = 'La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes';

/**
 * La nota de D-30 al restablecer la contraseña de otra persona.
 *
 * @param {string} nombre el nombre de la persona
 * @returns {string} la nota
 */
const notaDe = (nombre) => `Al guardar, se cierran las sesiones abiertas de ${nombre} y se quita su bloqueo por intentos `
  + `fallidos, si lo tenía. Compártela con ${nombre}: podrá cambiarla en «Cambiar contraseña».`;

/**
 * La tabla de usuarios, cuando termina de cargar.
 *
 * @returns {Promise<HTMLElement>} la tabla
 */
const tablaDeUsuarios = () => screen.findByRole('table', { name: 'Usuarios de la empresa' });

/**
 * La fila de un usuario en la tabla, cuando aparece.
 *
 * @param {string} nombre el nombre del usuario
 * @returns {Promise<HTMLElement>} la fila
 */
const filaDe = async (nombre) => (await within(await tablaDeUsuarios()).findByText(nombre)).closest('tr');

/**
 * Lo que se envió a la API para crear (POST) o editar (PUT) usuarios: desactivar y reactivar no llevan cuerpo.
 *
 * @param {import('../pruebas/crud.js').ApiSimulada} simulada la API simulada
 * @param {'POST'|'PUT'} metodo el método
 * @returns {object[]} los cuerpos, en orden
 */
const enviados = (simulada, metodo) => simulada.peticiones
  .filter((peticion) => peticion.metodo === metodo && peticion.cuerpo !== null)
  .map(({ cuerpo }) => cuerpo);

/**
 * Si un elemento va antes que otro en la página.
 *
 * @param {Node} uno el primero
 * @param {Node} otro el que debería ir después
 * @returns {boolean} si uno va antes que otro
 */
const vaAntes = (uno, otro) => Boolean(uno.compareDocumentPosition(otro) & Node.DOCUMENT_POSITION_FOLLOWING);

/**
 * Restablece la contraseña de otra persona: abre su formulario, escribe la contraseña nueva y guarda.
 *
 * @param {import('@testing-library/user-event').UserEvent} usuario el usuario de la prueba
 * @param {string} nombre el nombre de la persona
 * @param {string} contrasena la contraseña nueva
 */
async function restablecer(usuario, nombre, contrasena) {
  await usuario.click(await screen.findByRole('button', { name: `Editar a ${nombre}` }));
  await usuario.type(screen.getByLabelText('Contraseña nueva'), contrasena);
  await usuario.click(screen.getByRole('button', { name: 'Guardar cambios' }));
}

/**
 * Revisa cómo terminó guardar una contraseña: con su mensaje y sin enviar nada, o guardada y enviada tal cual.
 *
 * @param {import('../pruebas/crud.js').ApiSimulada} simulada la API simulada
 * @param {'POST'|'PUT'} metodo el método con que se guarda
 * @param {string} nombre el nombre de la persona, para el mensaje de resultado
 * @param {string} contrasena la contraseña que se escribió
 * @param {string|null} mensaje el mensaje del rechazo, o null si se acepta
 */
async function revisarGuardado(simulada, metodo, nombre, contrasena, mensaje) {
  if (mensaje) {
    expect(await screen.findByText(mensaje)).toBeInTheDocument();
    expect(enviados(simulada, metodo)).toEqual([]);
  } else {
    expect(await screen.findByText(`Se guardaron los datos de ${nombre}.`)).toBeInTheDocument();
    expect(enviados(simulada, metodo)[0].contrasena).toBe(contrasena);
  }
}

/** Los límites del largo de la contraseña (RF-03 C3). */
const LARGOS = [
  ['7 caracteres', 'a'.repeat(7), 'La contraseña debe tener al menos 8 caracteres.'],
  ['8 caracteres', 'a'.repeat(8), null],
  ['64 caracteres', 'a'.repeat(64), null],
  ['65 caracteres', 'a'.repeat(65), 'La contraseña puede tener como máximo 64 caracteres.'],
];

/** Los límites de los bytes de la contraseña (RF-03 C4). */
const ENES = [
  ['64 caracteres con 8 eñes (72 bytes)', 'ñ'.repeat(8) + 'a'.repeat(56), null],
  ['64 caracteres con 9 eñes (73 bytes)', 'ñ'.repeat(9) + 'a'.repeat(55), BYTES],
];

describe('gestionar los usuarios de la empresa (RF-03)', () => {
  it('RF-03 · el administrador de la empresa abre «Usuarios» desde el menú y ve los usuarios de su empresa, con los activos contra el máximo', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    apiSimulada(API_USUARIOS, [YO, CARLOS, LUISA], OPCIONES_USUARIOS);
    const { usuario } = abrirApp('/');

    await usuario.click(within(await screen.findByRole('navigation', { name: 'Menú principal' }))
      .getByRole('link', { name: 'Usuarios' }));

    expect(await screen.findByRole('heading', { name: 'Usuarios', level: 1 })).toBeInTheDocument();
    const tabla = await tablaDeUsuarios();
    expect(within(tabla).getByText('Carlos Ramírez')).toBeInTheDocument();
    expect(within(tabla).getByText('Luisa Cárdenas')).toBeInTheDocument();
    expect(await screen.findByText('2 de 25 usuarios activos, contándote a ti.')).toBeInTheDocument();
  });

  it('RF-03 · C1 · con datos válidos, la pantalla crea el usuario con el rol elegido y lo muestra activo en la lista', async () => {
    const { usuario, simulada } = abrirUsuarios();

    await llenarUsuario(usuario, { ...DATOS_DE_USUARIO, rol: 'ADMIN_EMPRESA' });
    await usuario.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(await screen.findByText('Se guardaron los datos de Pedro Salas.')).toBeInTheDocument();
    expect(enviados(simulada, 'POST')).toEqual([{ ...DATOS_DE_USUARIO, rol: 'ADMIN_EMPRESA' }]);
    const fila = await filaDe('Pedro Salas');
    expect(within(fila).getByText('pedro.salas@cacaosdelhuila.co')).toBeInTheDocument();
    expect(within(fila).getByText('Administrador de empresa')).toBeInTheDocument();
    expect(within(fila).getByText('Activo')).toBeInTheDocument();
  });

  it('RF-03 · C2 · si el correo ya existe en la plataforma, no crea el usuario y lo avisa en el campo Correo', async () => {
    const { usuario, simulada } = abrirUsuarios();
    servidor.use(http.post(API_USUARIOS, () => problema(409, 'Ya existe un registro con esos datos.', {
      errores: [{ campo: 'correo', mensaje: MENSAJES.correoRepetido }],
    })));

    await llenarUsuario(usuario, { ...DATOS_DE_USUARIO, correo: CARLOS.correo });
    await usuario.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(await screen.findByText(MENSAJES.correoRepetido)).toBeInTheDocument();
    expect(screen.getByLabelText('Correo')).toHaveAccessibleDescription(expect.stringContaining(MENSAJES.correoRepetido));
    expect(screen.queryByText('Se guardaron los datos de Pedro Salas.')).not.toBeInTheDocument();
    expect(simulada.registros).toHaveLength(3);
  });

  it.each(LARGOS)('RF-03 · C3 · la contraseña inicial acepta de 8 a 64 caracteres: con 7 o con 65 se rechaza (prueba de límite): %s',
    async (_, contrasena, mensaje) => {
      const { usuario, simulada } = abrirUsuarios();

      await llenarUsuario(usuario, { ...DATOS_DE_USUARIO, contrasena });
      await usuario.click(screen.getByRole('button', { name: 'Crear usuario' }));

      await revisarGuardado(simulada, 'POST', 'Pedro Salas', contrasena, mensaje);
    });

  it.each(LARGOS)('RF-03 · C3 · al restablecer la contraseña de otra persona, la nueva acepta de 8 a 64 caracteres: con 7 o con 65 se rechaza (prueba de límite): %s',
    async (_, contrasena, mensaje) => {
      const { usuario, simulada } = abrirUsuarios();

      await restablecer(usuario, 'Carlos Ramírez', contrasena);

      await revisarGuardado(simulada, 'PUT', 'Carlos Ramírez', contrasena, mensaje);
    });

  it.each(ENES)('RF-03 · C4 · una contraseña inicial de 64 caracteres con 8 eñes se acepta; con 9 eñes se rechaza con su mensaje (prueba de límite): %s',
    async (_, contrasena, mensaje) => {
      const { usuario, simulada } = abrirUsuarios();

      await llenarUsuario(usuario, { ...DATOS_DE_USUARIO, contrasena });
      await usuario.click(screen.getByRole('button', { name: 'Crear usuario' }));

      await revisarGuardado(simulada, 'POST', 'Pedro Salas', contrasena, mensaje);
    });

  it.each(ENES)('RF-03 · C4 · al restablecer la contraseña de otra persona, con 8 eñes en 64 caracteres se acepta; con 9 eñes se rechaza con su mensaje (prueba de límite): %s',
    async (_, contrasena, mensaje) => {
      const { usuario, simulada } = abrirUsuarios();

      await restablecer(usuario, 'Carlos Ramírez', contrasena);

      await revisarGuardado(simulada, 'PUT', 'Carlos Ramírez', contrasena, mensaje);
    });

  it('RF-03 · C5 · con 24 usuarios activos todavía se puede crear o reactivar uno, sin aviso (prueba de límite)', async () => {
    abrirUsuarios([LUISA, ...usuariosActivos(24)]);

    expect(await screen.findByText('24 de 25 usuarios activos, contándote a ti.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Nuevo usuario' })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Reactivar a Luisa Cárdenas' })).toBeEnabled();
    expect(screen.queryByText(MENSAJES.maximo)).not.toBeInTheDocument();
  });

  it('RF-03 · C5 · con 25 usuarios activos no deja crear ni reactivar otro, y un aviso que no interrumpe explica por qué (prueba de límite)', async () => {
    abrirUsuarios([LUISA, ...usuariosActivos(25)]);

    expect(await screen.findByText('25 de 25 usuarios activos, contándote a ti.')).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent(MENSAJES.maximo);
    for (const nombre of ['Nuevo usuario', 'Reactivar a Luisa Cárdenas']) {
      const boton = screen.getByRole('button', { name: nombre });
      expect(boton).toBeDisabled();
      expect(boton).toHaveAccessibleDescription(MENSAJES.maximo);
    }
  });

  it('RF-03 · C5 · al crear el usuario 25, la lista se vuelve a pedir y ya no deja crear otro', async () => {
    const { usuario } = abrirUsuarios(usuariosActivos(24));

    await llenarUsuario(usuario);
    await usuario.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(await screen.findByText('25 de 25 usuarios activos, contándote a ti.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Nuevo usuario' })).toBeDisabled();
    expect(screen.getByText(MENSAJES.maximo)).toBeInTheDocument();
  });

  it('RF-03 · C5 · si al crear la API responde que la empresa ya tiene 25 activos, el formulario muestra el mensaje de la regla y sigue abierto', async () => {
    const { usuario } = abrirUsuarios(usuariosActivos(24));
    servidor.use(http.post(API_USUARIOS, () => reglaIncumplida(MENSAJES.maximo)));

    await llenarUsuario(usuario);
    await usuario.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(MENSAJES.maximo);
    expect(screen.queryByText('Revisa los datos marcados.')).not.toBeInTheDocument();
    expect(screen.getByRole('form', { name: 'Nuevo usuario' })).toBeInTheDocument();
  });

  it('RF-03 · C5 · si al reactivar la API responde que la empresa ya tiene 25 activos, la pantalla muestra el mensaje de la regla y la fila sigue desactivada', async () => {
    const { usuario } = abrirUsuarios([LUISA, ...usuariosActivos(24)]);
    servidor.use(http.post(`${API_USUARIOS}/:id/reactivar`, () => reglaIncumplida(MENSAJES.maximo)));

    await usuario.click(await screen.findByRole('button', { name: 'Reactivar a Luisa Cárdenas' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(MENSAJES.maximo);
    expect(screen.queryByText('Revisa los datos marcados.')).not.toBeInTheDocument();
    expect(within(await filaDe('Luisa Cárdenas')).getByText('Desactivado')).toBeInTheDocument();
  });

  it('RF-03 · C7 · en el celular, la tarjeta propia lleva «Tú» y se puede editar, pero no tiene «Desactivar»; las demás activas sí', async () => {
    anchoDePantalla(390);
    abrirUsuarios();

    const lista = await screen.findByRole('list', { name: 'Usuarios de la empresa' });
    const propia = within(lista).getByText('Ana Pérez').closest('li');
    expect(within(propia).getByText('Tú')).toBeInTheDocument();
    expect(within(propia).getByRole('button', { name: 'Editar a Ana Pérez' })).toBeInTheDocument();
    expect(within(propia).queryByRole('button', { name: 'Desactivar a Ana Pérez' })).not.toBeInTheDocument();
    expect(within(lista).getByRole('button', { name: 'Desactivar a Carlos Ramírez' })).toBeInTheDocument();
  });

  it('RF-03 · C7 · la API también revisa que nadie se desactive a sí mismo: si responde con esa regla, la pantalla muestra su mensaje y la fila sigue activa', async () => {
    const { usuario } = abrirUsuarios();
    servidor.use(http.post(`${API_USUARIOS}/:id/desactivar`, () => reglaIncumplida(MENSAJES.desactivarse)));

    await usuario.click(await screen.findByRole('button', { name: 'Desactivar a Carlos Ramírez' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(MENSAJES.desactivarse);
    expect(within(await filaDe('Carlos Ramírez')).getByText('Activo')).toBeInTheDocument();
  });

  it('RF-03 · C8 · al editar su propia fila, el rol se ve deshabilitado, con su rol y el mensaje de la regla, y se guarda sin cambios', async () => {
    const { usuario, simulada } = abrirUsuarios();

    await usuario.click(await screen.findByRole('button', { name: 'Editar a Ana Pérez' }));
    const rol = screen.getByLabelText('Rol');
    expect(rol).toBeDisabled();
    expect(rol).toHaveValue('ADMIN_EMPRESA');
    expect(rol).toHaveAccessibleDescription(expect.stringContaining(MENSAJES.rolPropio));
    const nombre = screen.getByLabelText('Nombre');
    await usuario.clear(nombre);
    await usuario.type(nombre, 'Ana María Pérez');
    await usuario.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByText('Se guardaron los datos de Ana María Pérez.')).toBeInTheDocument();
    expect(enviados(simulada, 'PUT')).toEqual([{ nombre: 'Ana María Pérez', correo: YO.correo, contrasena: null, rol: 'ADMIN_EMPRESA' }]);
  });

  it('RF-03 · C8 · si la API rechaza el rol al guardar la fila propia, el mensaje de la regla queda en el campo Rol y no se guarda', async () => {
    const { usuario } = abrirUsuarios();
    servidor.use(http.put(`${API_USUARIOS}/:id`, () => reglaIncumplida(MENSAJES.rolPropio, 'rol')));

    await usuario.click(await screen.findByRole('button', { name: 'Editar a Ana Pérez' }));
    await usuario.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByLabelText('Rol')).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByLabelText('Rol')).toHaveAccessibleDescription(expect.stringContaining(MENSAJES.rolPropio));
    expect(screen.getByRole('form', { name: 'Editar usuario' })).toBeInTheDocument();
    expect(screen.queryByText('Se guardaron los datos de Ana Pérez.')).not.toBeInTheDocument();
  });

  it('RF-03 · C10 · el formulario solo ofrece los roles Usuario final y Administrador de empresa, al crear y al editar', async () => {
    const { usuario } = abrirUsuarios();
    const opciones = () => within(screen.getByLabelText('Rol')).getAllByRole('option').map((opcion) => opcion.textContent);

    await usuario.click(await screen.findByRole('button', { name: 'Nuevo usuario' }));
    expect(opciones()).toEqual(['Usuario final', 'Administrador de empresa']);
    await usuario.click(screen.getByRole('button', { name: 'Cancelar' }));
    await usuario.click(screen.getByRole('button', { name: 'Editar a Carlos Ramírez' }));
    expect(opciones()).toEqual(['Usuario final', 'Administrador de empresa']);
  });

  it('RF-03 · C10 · si la API responde «prohibido» (403) por el rol, su mensaje queda en el campo Rol y no se crea el usuario', async () => {
    const { usuario, simulada } = abrirUsuarios();
    servidor.use(http.post(API_USUARIOS, () => problema(403, 'No tienes permiso para esta operación.', {
      errores: [{ campo: 'rol', mensaje: MENSAJES.rolNoPermitido }],
    })));

    await llenarUsuario(usuario);
    await usuario.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(await screen.findByText(MENSAJES.rolNoPermitido)).toBeInTheDocument();
    expect(screen.getByLabelText('Rol')).toHaveAttribute('aria-invalid', 'true');
    expect(simulada.registros).toHaveLength(3);
  });

  it('RF-03 · C11 · al editar su propia fila no aparece la contraseña: el mensaje de la regla lleva a «Cambiar contraseña», con el enlace en su nombre', async () => {
    const { usuario } = abrirUsuarios();

    await usuario.click(await screen.findByRole('button', { name: 'Editar a Ana Pérez' }));
    const formulario = screen.getByRole('form', { name: 'Editar usuario' });
    expect(within(formulario).queryByLabelText('Contraseña nueva')).not.toBeInTheDocument();
    const enlace = within(formulario).getByRole('link', { name: 'Cambiar contraseña' });
    expect(enlace.closest('p').textContent).toBe(MENSAJES.contrasenaPropia);
    expect(enlace).toHaveAttribute('href', '/cambiar-contrasena');
    await usuario.click(enlace);

    expect(await screen.findByRole('heading', { name: 'Cambiar contraseña', level: 1 })).toBeInTheDocument();
  });

  it('RF-03 · C11 · en inglés, el enlace del mensaje también va en el nombre de la página, «Change password»', async () => {
    await i18n.changeLanguage('en');
    const { usuario } = abrirUsuarios();

    await usuario.click(await screen.findByRole('button', { name: 'Edit Ana Pérez' }));
    const enlace = within(screen.getByRole('form', { name: 'Edit user' })).getByRole('link', { name: 'Change password' });

    expect(enlace.closest('p').textContent).toBe('To change your own password, use “Change password”.');
    expect(enlace).toHaveAttribute('href', '/cambiar-contrasena');
  });

  it('RF-03 · al restablecer la contraseña de otra persona, la nota de D-30 aparece al escribirla, debajo de los campos, y se va si se borra', async () => {
    const { usuario } = abrirUsuarios();

    await usuario.click(await screen.findByRole('button', { name: 'Editar a Carlos Ramírez' }));
    const campo = screen.getByLabelText('Contraseña nueva');
    await usuario.type(campo, 'Clave-Segura-2026');

    const nota = await screen.findByRole('status');
    expect(nota).toHaveTextContent(notaDe('Carlos Ramírez'));
    expect(vaAntes(screen.getByLabelText('Rol'), nota)).toBe(true);
    expect(vaAntes(nota, screen.getByRole('button', { name: 'Guardar cambios' }))).toBe(true);
    await usuario.clear(campo);
    expect(screen.queryByText(notaDe('Carlos Ramírez'))).not.toBeInTheDocument();
  });

  it('RF-03 · la nota de D-30 no aparece al crear un usuario, que no tiene sesiones, ni en la fila propia, que no cambia su contraseña aquí', async () => {
    const { usuario } = abrirUsuarios();

    await llenarUsuario(usuario);
    expect(screen.queryByText(/se cierran las sesiones abiertas/)).not.toBeInTheDocument();
    await usuario.click(screen.getByRole('button', { name: 'Cancelar' }));
    await usuario.click(screen.getByRole('button', { name: 'Editar a Ana Pérez' }));
    expect(screen.queryByText(/se cierran las sesiones abiertas/)).not.toBeInTheDocument();
  });
});
