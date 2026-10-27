/*
 * Nombre: UsuarioPantalla.test.jsx
 * Descripción: Pruebas generadas de la pantalla de usuario: lista, formulario, errores de la API,
 *              acciones y permisos.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.10)
 * Versión: 1.10
 * Entidades: usuario 1.8
 */
import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { abrirApp, conSesion, USUARIOS } from '../../pruebas/ayudas.jsx';
import { apiSimulada } from '../../pruebas/crud.js';
import { problema, servidor } from '../../pruebas/servidor.js';

/** La pantalla en la app y su ruta en la API. */
const RUTA = '/usuarios';
const API = '/api/v1/usuarios';

/** Un registro como lo responde la API, con los ejemplos del JSON. */
const REGISTRO = { id: '0199e2a0-0000-7000-8000-0000000000a1', nombre: 'Ana Pérez', correo: 'ana.perez@example.com', rol: 'USUARIO_FINAL', activo: true };

/** Otro registro, con otros datos y desactivado. */
const OTRO_REGISTRO = { id: '0199e2a0-0000-7000-8000-0000000000a2', nombre: 'Ana Pérez 2', correo: 'ana.perez@example.com 2', rol: 'USUARIO_FINAL', activo: false };

/** Lo que necesita saber la API simulada. */
const OPCIONES = { atributoActivo: 'activo' };

/**
 * El párrafo que dice un texto completo, aunque una parte de él sea un enlace.
 *
 * @param {string} texto el texto
 * @returns {HTMLElement} el párrafo
 */
const parrafoCon = (texto) => screen.getByText((_, elemento) => elemento?.tagName === 'P' && elemento.textContent === texto);

describe('pantalla de usuario (generada)', () => {
  it('RF-03 · la pantalla de usuario muestra sus registros y su resumen', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    apiSimulada(API, [REGISTRO, OTRO_REGISTRO], OPCIONES);
    abrirApp(RUTA);

    const tabla = await screen.findByRole('table', { name: 'Usuarios de la empresa' });
    expect(within(tabla).getByText('Ana Pérez')).toBeInTheDocument();
    expect(within(tabla).getByText('ana.perez@example.com')).toBeInTheDocument();
    expect(within(tabla).getByText('Desactivado')).toBeInTheDocument();
    expect(await screen.findByText('1 de 25 usuarios activos, contándote a ti.')).toBeInTheDocument();
  });

  it('RF-03 · sin registros, la pantalla de usuario lo dice', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    apiSimulada(API, [], OPCIONES);
    abrirApp(RUTA);

    expect(await screen.findByText('Todavía no hay usuarios en la empresa.')).toBeInTheDocument();
  });

  it('RF-03 · la pantalla de usuario registra con los datos del formulario', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    const simulada = apiSimulada(API, [], OPCIONES);
    const { usuario } = abrirApp(RUTA);

    await usuario.click(await screen.findByRole('button', { name: 'Nuevo usuario' }));
    await usuario.type(screen.getByLabelText('Nombre'), 'Ana Pérez');
    await usuario.type(screen.getByLabelText('Correo'), 'ana.perez@example.com');
    await usuario.type(screen.getByLabelText('Contraseña inicial'), 'Prueba-Segura-2026');
    await usuario.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(await screen.findByText('Se guardaron los datos de Ana Pérez.')).toBeInTheDocument();
    expect(simulada.peticiones).toContainEqual({ metodo: 'POST', ruta: API, cuerpo: { nombre: 'Ana Pérez', correo: 'ana.perez@example.com', contrasena: 'Prueba-Segura-2026', rol: 'USUARIO_FINAL' } });
  });

  it('RF-03 · lo que solo revisa el backend de usuario llega a su campo al guardar', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    apiSimulada(API, [], OPCIONES);
    servidor.use(http.post(API, () => problema(400, 'Hay datos que corregir.', {
      errores: [
        { campo: 'correo', mensaje: 'Escribe un correo válido.' },
      ],
    })));
    const { usuario } = abrirApp(RUTA);

    await usuario.click(await screen.findByRole('button', { name: 'Nuevo usuario' }));
    await usuario.type(screen.getByLabelText('Nombre'), 'Ana Pérez');
    await usuario.type(screen.getByLabelText('Correo'), 'ana.perez@example.com');
    await usuario.type(screen.getByLabelText('Contraseña inicial'), 'Prueba-Segura-2026');
    await usuario.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(await screen.findByText('Escribe un correo válido.')).toBeInTheDocument();
  });

  it('RF-03 · un dato repetido de usuario se marca en su campo (409)', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    apiSimulada(API, [], OPCIONES);
    servidor.use(http.post(API, () => problema(409, 'Ya existe un registro con estos datos.', {
      errores: [
        { campo: 'correo', mensaje: 'Ya existe un usuario con este correo.' },
      ],
    })));
    const { usuario } = abrirApp(RUTA);

    await usuario.click(await screen.findByRole('button', { name: 'Nuevo usuario' }));
    await usuario.type(screen.getByLabelText('Nombre'), 'Ana Pérez');
    await usuario.type(screen.getByLabelText('Correo'), 'ana.perez@example.com');
    await usuario.type(screen.getByLabelText('Contraseña inicial'), 'Prueba-Segura-2026');
    await usuario.click(screen.getByRole('button', { name: 'Crear usuario' }));

    expect(await screen.findByText('Ya existe un usuario con este correo.')).toBeInTheDocument();
  });

  it('RF-03 · la pantalla de usuario edita con los datos del formulario', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    const simulada = apiSimulada(API, [REGISTRO], OPCIONES);
    const { usuario } = abrirApp(RUTA);

    await usuario.click(await screen.findByRole('button', { name: 'Editar a Ana Pérez' }));
    const campo = screen.getByLabelText('Nombre');
    await usuario.clear(campo);
    await usuario.type(campo, 'Ana Pérez 2');
    await usuario.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByText('Se guardaron los datos de Ana Pérez 2.')).toBeInTheDocument();
    expect(simulada.peticiones).toContainEqual({
      metodo: 'PUT', ruta: `${API}/${REGISTRO.id}`, cuerpo: { nombre: 'Ana Pérez 2', correo: 'ana.perez@example.com', contrasena: null, rol: 'USUARIO_FINAL' },
    });
  });

  it('RF-03 · desactivar usuario no pide confirmación, y reactivarla no', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    const simulada = apiSimulada(API, [REGISTRO, OTRO_REGISTRO], OPCIONES);
    const { usuario } = abrirApp(RUTA);

    await usuario.click(await screen.findByRole('button', { name: 'Desactivar a Ana Pérez' }));
    expect(await screen.findByText('Se desactivó a Ana Pérez.')).toBeInTheDocument();

    await usuario.click(await screen.findByRole('button', { name: 'Reactivar a Ana Pérez 2' }));
    expect(await screen.findByText('Se reactivó a Ana Pérez 2.')).toBeInTheDocument();
    expect(simulada.peticiones.filter(({ metodo }) => metodo === 'POST').map(({ ruta }) => ruta)).toEqual([
      `${API}/${REGISTRO.id}/desactivar`, `${API}/${OTRO_REGISTRO.id}/reactivar`,
    ]);
  });

  it('RF-03 · al llegar al máximo de activos de su regla, no deja registrar ni reactivar y muestra el mensaje de la regla', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    const activos = Array.from({ length: 25 }, (_, posicion) => ({
      ...REGISTRO, id: `0199e2a0-0000-7000-8000-${String(posicion + 100).padStart(12, '0')}`,
      nombre: `Activo ${posicion + 1}`,
    }));
    apiSimulada(API, [OTRO_REGISTRO, ...activos], OPCIONES);
    abrirApp(RUTA);

    expect(await screen.findByText('La empresa ya tiene 25 usuarios activos: desactiva uno antes de crear o reactivar otro.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Nuevo usuario' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Reactivar a Ana Pérez 2' })).toBeDisabled();
  });

  it('RF-03 · la fila de quien tiene la sesión lleva su marca y no tiene las acciones que impiden sus reglas', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    apiSimulada(API, [{ ...REGISTRO, id: USUARIOS.ADMIN_EMPRESA.id }, { ...OTRO_REGISTRO, activo: true }], OPCIONES);
    abrirApp(RUTA);

    const tabla = await screen.findByRole('table', { name: 'Usuarios de la empresa' });
    const propia = within(tabla).getByText('Ana Pérez').closest('tr');
    expect(within(propia).getByText('Tú')).toBeInTheDocument();
    expect(within(propia).queryByRole('button', { name: 'Desactivar a Ana Pérez' })).toBeNull();
    expect(within(tabla).getByRole('button', { name: 'Desactivar a Ana Pérez 2' })).toBeInTheDocument();
  });

  it('RF-03 · al editar la fila propia, los campos que impiden sus reglas no se cambian y muestran el mensaje de la regla', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    apiSimulada(API, [{ ...REGISTRO, id: USUARIOS.ADMIN_EMPRESA.id }], OPCIONES);
    const { usuario } = abrirApp(RUTA);

    await usuario.click(await screen.findByRole('button', { name: 'Editar a Ana Pérez' }));
    expect(screen.getByLabelText('Rol')).toBeDisabled();
    expect(screen.getByText('No puedes cambiar tu propio rol.')).toBeInTheDocument();
    expect(screen.queryByLabelText('Contraseña nueva')).toBeNull();
    expect(within(parrafoCon('Para cambiar tu propia contraseña, usa «Cambiar contraseña».')).getByRole('link', { name: 'Cambiar contraseña' }))
      .toHaveAttribute('href', '/cambiar-contrasena');
  });

  it('RF-03 · al editar a otra persona, la nota de la contraseña aparece al escribir una nueva', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    apiSimulada(API, [REGISTRO], OPCIONES);
    const { usuario } = abrirApp(RUTA);

    await usuario.click(await screen.findByRole('button', { name: 'Editar a Ana Pérez' }));
    expect(screen.queryByText('Al guardar, se cierran las sesiones abiertas de Ana Pérez y se quita su bloqueo por intentos fallidos, si lo tenía. Compártela con Ana Pérez: podrá cambiarla en «Cambiar contraseña».')).toBeNull();
    await usuario.type(screen.getByLabelText('Contraseña nueva'), 'Prueba-Segura-2026');

    expect(await screen.findByText('Al guardar, se cierran las sesiones abiertas de Ana Pérez y se quita su bloqueo por intentos fallidos, si lo tenía. Compártela con Ana Pérez: podrá cambiarla en «Cambiar contraseña».')).toBeInTheDocument();
  });

  it('RF-03 · al editar sin escribir una contraseña, no la envía y queda la que había', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    const simulada = apiSimulada(API, [REGISTRO], OPCIONES);
    const { usuario } = abrirApp(RUTA);

    await usuario.click(await screen.findByRole('button', { name: 'Editar a Ana Pérez' }));
    await usuario.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByText('Se guardaron los datos de Ana Pérez.')).toBeInTheDocument();
    const { cuerpo } = simulada.peticiones.find(({ metodo }) => metodo === 'PUT');
    expect(cuerpo.contrasena).toBeNull();
  });

  it('RF-03 · el formulario para registrar empieza con los valores iniciales del JSON', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    apiSimulada(API, [], OPCIONES);
    const { usuario } = abrirApp(RUTA);

    await usuario.click(await screen.findByRole('button', { name: 'Nuevo usuario' }));
    expect(screen.getByLabelText('Rol')).toHaveValue('USUARIO_FINAL');
  });

  it.each(['USUARIO_FINAL', 'ADMIN_PLATAFORMA'])(
    'RNF-04 · la pantalla de usuario solo la abren los roles de listar: %s ve que no tiene permiso', async (rol) => {
      conSesion(USUARIOS[rol]);
      abrirApp(RUTA);

      expect(await screen.findByRole('heading', { name: 'Sin permiso' })).toBeInTheDocument();
    });

  it('RNF-06 · sin conexión, la pantalla de usuario dice qué pasó y qué hacer, y se puede reintentar', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    servidor.use(http.get(API, () => HttpResponse.error()));
    const { usuario } = abrirApp(RUTA);

    expect(await screen.findByText('No se pudo conectar con el servidor. Revisa tu conexión a internet e intenta de nuevo.'))
      .toBeInTheDocument();
    apiSimulada(API, [REGISTRO], OPCIONES);
    await usuario.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByRole('table', { name: 'Usuarios de la empresa' })).toBeInTheDocument();
  });

  it('RF-03 · un dato con HTML se ve como texto en la pantalla de usuario: no se interpreta', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    apiSimulada(API, [{ id: '0199e2a0-0000-7000-8000-0000000000a1', nombre: '<img src=x onerror=alert(1)>', correo: 'ana.perez@example.com', rol: 'USUARIO_FINAL', activo: true }], OPCIONES);
    abrirApp(RUTA);

    const tabla = await screen.findByRole('table', { name: 'Usuarios de la empresa' });
    expect(within(tabla).getByText('<img src=x onerror=alert(1)>')).toBeInTheDocument();
    expect(tabla.querySelector('img')).toBeNull();
  });
});
