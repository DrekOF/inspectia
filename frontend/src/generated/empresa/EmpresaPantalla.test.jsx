/*
 * Nombre: EmpresaPantalla.test.jsx
 * Descripción: Pruebas generadas de la pantalla de empresa: lista, formulario, errores de la API,
 *              acciones y permisos.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.9)
 * Versión: 1.9
 * Entidades: empresa 1.3, usuario 1.8
 */
import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { abrirApp, conSesion, USUARIOS } from '../../pruebas/ayudas.jsx';
import { apiSimulada } from '../../pruebas/crud.js';
import { problema, servidor } from '../../pruebas/servidor.js';

/** La pantalla en la app y su ruta en la API. */
const RUTA = '/empresas';
const API = '/api/v1/empresas';

/** Un registro como lo responde la API, con los ejemplos del JSON. */
const REGISTRO = { id: '0199e2a0-0000-7000-8000-0000000000a1', nombre: 'Cacaos del Huila', nit: '900123456-8', ciudad: 'Neiva', activa: true };

/** Otro registro, con otros datos y desactivado. */
const OTRO_REGISTRO = { id: '0199e2a0-0000-7000-8000-0000000000a2', nombre: 'Cacaos del Huila 2', nit: '011234567-9', ciudad: 'Neiva 2', activa: false };

/** Lo que necesita saber la API simulada. */
const OPCIONES = { atributoActivo: 'activa' };

describe('pantalla de empresa (generada)', () => {
  it('RF-02 · la pantalla de empresa muestra sus registros y su resumen', async () => {
    conSesion(USUARIOS.ADMIN_PLATAFORMA);
    apiSimulada(API, [REGISTRO, OTRO_REGISTRO], OPCIONES);
    abrirApp(RUTA);

    const tabla = await screen.findByRole('table', { name: 'Empresas registradas' });
    expect(within(tabla).getByText('Cacaos del Huila')).toBeInTheDocument();
    expect(within(tabla).getByText('NIT 900123456-8')).toBeInTheDocument();
    expect(within(tabla).getByText('Desactivada')).toBeInTheDocument();
    expect(await screen.findByText('2 empresas registradas: 1 activa y 1 desactivada.')).toBeInTheDocument();
  });

  it('RF-02 · sin registros, la pantalla de empresa lo dice', async () => {
    conSesion(USUARIOS.ADMIN_PLATAFORMA);
    apiSimulada(API, [], OPCIONES);
    abrirApp(RUTA);

    expect(await screen.findByText('Todavía no hay empresas registradas.')).toBeInTheDocument();
  });

  it('RF-02 · la pantalla de empresa registra con los datos del formulario', async () => {
    conSesion(USUARIOS.ADMIN_PLATAFORMA);
    const simulada = apiSimulada(API, [], OPCIONES);
    const { usuario } = abrirApp(RUTA);

    await usuario.click(await screen.findByRole('button', { name: 'Nueva empresa' }));
    await usuario.type(within(screen.getByRole('group', { name: 'Empresa' })).getByLabelText('Nombre'), 'Cacaos del Huila');
    await usuario.type(within(screen.getByRole('group', { name: 'Empresa' })).getByLabelText('NIT'), '900123456-8');
    await usuario.type(within(screen.getByRole('group', { name: 'Empresa' })).getByLabelText('Ciudad'), 'Neiva');
    await usuario.type(within(screen.getByRole('group', { name: 'Primer administrador' })).getByLabelText('Nombre'), 'Ana Pérez');
    await usuario.type(within(screen.getByRole('group', { name: 'Primer administrador' })).getByLabelText('Correo'), 'ana.perez@example.com');
    await usuario.type(within(screen.getByRole('group', { name: 'Primer administrador' })).getByLabelText('Contraseña inicial'), 'Prueba-Segura-2026');
    await usuario.click(screen.getByRole('button', { name: 'Registrar empresa' }));

    expect(await screen.findByText('Se guardó Cacaos del Huila.')).toBeInTheDocument();
    expect(simulada.peticiones).toContainEqual({ metodo: 'POST', ruta: API, cuerpo: { nombre: 'Cacaos del Huila', nit: '900123456-8', ciudad: 'Neiva', administrador: { nombre: 'Ana Pérez', correo: 'ana.perez@example.com', contrasena: 'Prueba-Segura-2026' } } });
  });

  it('RF-02 · lo que solo revisa el backend de empresa llega a su campo al guardar', async () => {
    conSesion(USUARIOS.ADMIN_PLATAFORMA);
    apiSimulada(API, [], OPCIONES);
    servidor.use(http.post(API, () => problema(400, 'Hay datos que corregir.', {
      errores: [
        { campo: 'nit', mensaje: 'El dígito de verificación del NIT no corresponde.' },
        { campo: 'administrador.correo', mensaje: 'Escribe un correo válido.' },
      ],
    })));
    const { usuario } = abrirApp(RUTA);

    await usuario.click(await screen.findByRole('button', { name: 'Nueva empresa' }));
    await usuario.type(within(screen.getByRole('group', { name: 'Empresa' })).getByLabelText('Nombre'), 'Cacaos del Huila');
    await usuario.type(within(screen.getByRole('group', { name: 'Empresa' })).getByLabelText('NIT'), '900123456-8');
    await usuario.type(within(screen.getByRole('group', { name: 'Empresa' })).getByLabelText('Ciudad'), 'Neiva');
    await usuario.type(within(screen.getByRole('group', { name: 'Primer administrador' })).getByLabelText('Nombre'), 'Ana Pérez');
    await usuario.type(within(screen.getByRole('group', { name: 'Primer administrador' })).getByLabelText('Correo'), 'ana.perez@example.com');
    await usuario.type(within(screen.getByRole('group', { name: 'Primer administrador' })).getByLabelText('Contraseña inicial'), 'Prueba-Segura-2026');
    await usuario.click(screen.getByRole('button', { name: 'Registrar empresa' }));

    expect(await screen.findByText('El dígito de verificación del NIT no corresponde.')).toBeInTheDocument();
    expect(await screen.findByText('Escribe un correo válido.')).toBeInTheDocument();
  });

  it('RF-02 · un dato repetido de empresa se marca en su campo (409)', async () => {
    conSesion(USUARIOS.ADMIN_PLATAFORMA);
    apiSimulada(API, [], OPCIONES);
    servidor.use(http.post(API, () => problema(409, 'Ya existe un registro con estos datos.', {
      errores: [
        { campo: 'nit', mensaje: 'Ya existe una empresa con este NIT.' },
        { campo: 'administrador.correo', mensaje: 'Ya existe un usuario con este correo.' },
      ],
    })));
    const { usuario } = abrirApp(RUTA);

    await usuario.click(await screen.findByRole('button', { name: 'Nueva empresa' }));
    await usuario.type(within(screen.getByRole('group', { name: 'Empresa' })).getByLabelText('Nombre'), 'Cacaos del Huila');
    await usuario.type(within(screen.getByRole('group', { name: 'Empresa' })).getByLabelText('NIT'), '900123456-8');
    await usuario.type(within(screen.getByRole('group', { name: 'Empresa' })).getByLabelText('Ciudad'), 'Neiva');
    await usuario.type(within(screen.getByRole('group', { name: 'Primer administrador' })).getByLabelText('Nombre'), 'Ana Pérez');
    await usuario.type(within(screen.getByRole('group', { name: 'Primer administrador' })).getByLabelText('Correo'), 'ana.perez@example.com');
    await usuario.type(within(screen.getByRole('group', { name: 'Primer administrador' })).getByLabelText('Contraseña inicial'), 'Prueba-Segura-2026');
    await usuario.click(screen.getByRole('button', { name: 'Registrar empresa' }));

    expect(await screen.findByText('Ya existe una empresa con este NIT.')).toBeInTheDocument();
    expect(await screen.findByText('Ya existe un usuario con este correo.')).toBeInTheDocument();
  });

  it('RF-02 · la pantalla de empresa edita con los datos del formulario', async () => {
    conSesion(USUARIOS.ADMIN_PLATAFORMA);
    const simulada = apiSimulada(API, [REGISTRO], OPCIONES);
    const { usuario } = abrirApp(RUTA);

    await usuario.click(await screen.findByRole('button', { name: 'Editar Cacaos del Huila' }));
    const campo = screen.getByLabelText('Nombre');
    await usuario.clear(campo);
    await usuario.type(campo, 'Cacaos del Huila 2');
    await usuario.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByText('Se guardó Cacaos del Huila 2.')).toBeInTheDocument();
    expect(simulada.peticiones).toContainEqual({
      metodo: 'PUT', ruta: `${API}/${REGISTRO.id}`, cuerpo: { nombre: 'Cacaos del Huila 2', nit: '900123456-8', ciudad: 'Neiva' },
    });
  });

  it('RF-02 · desactivar empresa pide confirmación, y reactivarla no', async () => {
    conSesion(USUARIOS.ADMIN_PLATAFORMA);
    const simulada = apiSimulada(API, [REGISTRO, OTRO_REGISTRO], OPCIONES);
    const { usuario } = abrirApp(RUTA);

    await usuario.click(await screen.findByRole('button', { name: 'Desactivar Cacaos del Huila' }));
    const dialogo = await screen.findByRole('dialog', { name: '¿Desactivar Cacaos del Huila?' });
    expect(within(dialogo).getByText('Sus usuarios no podrán iniciar sesión hasta que la reactives. No se borra ningún dato.')).toBeInTheDocument();
    await usuario.click(within(dialogo).getByRole('button', { name: 'Desactivar' }));
    expect(await screen.findByText('Se desactivó Cacaos del Huila.')).toBeInTheDocument();

    await usuario.click(await screen.findByRole('button', { name: 'Reactivar Cacaos del Huila 2' }));
    expect(await screen.findByText('Se reactivó Cacaos del Huila 2.')).toBeInTheDocument();
    expect(simulada.peticiones.filter(({ metodo }) => metodo === 'POST').map(({ ruta }) => ruta)).toEqual([
      `${API}/${REGISTRO.id}/desactivar`, `${API}/${OTRO_REGISTRO.id}/reactivar`,
    ]);
  });

  it.each(['USUARIO_FINAL', 'ADMIN_EMPRESA'])(
    'RNF-04 · la pantalla de empresa solo la abren los roles de listar: %s ve que no tiene permiso', async (rol) => {
      conSesion(USUARIOS[rol]);
      abrirApp(RUTA);

      expect(await screen.findByRole('heading', { name: 'Sin permiso' })).toBeInTheDocument();
    });

  it('RNF-06 · sin conexión, la pantalla de empresa dice qué pasó y qué hacer, y se puede reintentar', async () => {
    conSesion(USUARIOS.ADMIN_PLATAFORMA);
    servidor.use(http.get(API, () => HttpResponse.error()));
    const { usuario } = abrirApp(RUTA);

    expect(await screen.findByText('No se pudo conectar con el servidor. Revisa tu conexión a internet e intenta de nuevo.'))
      .toBeInTheDocument();
    apiSimulada(API, [REGISTRO], OPCIONES);
    await usuario.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByRole('table', { name: 'Empresas registradas' })).toBeInTheDocument();
  });

  it('RF-02 · un dato con HTML se ve como texto en la pantalla de empresa: no se interpreta', async () => {
    conSesion(USUARIOS.ADMIN_PLATAFORMA);
    apiSimulada(API, [{ id: '0199e2a0-0000-7000-8000-0000000000a1', nombre: '<img src=x onerror=alert(1)>', nit: '900123456-8', ciudad: 'Neiva', activa: true }], OPCIONES);
    abrirApp(RUTA);

    const tabla = await screen.findByRole('table', { name: 'Empresas registradas' });
    expect(within(tabla).getByText('<img src=x onerror=alert(1)>')).toBeInTheDocument();
    expect(tabla.querySelector('img')).toBeNull();
  });
});
