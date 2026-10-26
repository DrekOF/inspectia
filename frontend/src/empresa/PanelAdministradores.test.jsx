/*
 * Nombre: PanelAdministradores.test.jsx
 * Descripción: Pruebas del panel de administradores de la pantalla de empresas (RF-02 C12), con la API simulada:
 *              el administrador de la plataforma ve los administradores de una empresa y restablece la
 *              contraseña de uno, con las mismas validaciones de la contraseña inicial y el aviso de D-30. Al
 *              abrir el panel y su formulario, el foco va a su título y, al cerrarlos, vuelve al botón que los
 *              abrió (RNF-06 C4).
 * Fecha de creación: 26/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { abrirApp, conSesion, USUARIOS } from '../pruebas/ayudas.jsx';
import { apiSimulada } from '../pruebas/crud.js';
import { API_EMPRESAS, EMPRESA, OPCIONES_EMPRESAS, RUTA_EMPRESAS } from '../pruebas/empresas.js';
import { problema, servidor } from '../pruebas/servidor.js';

/** Los administradores de Cacaos del Huila, como los responde la API. */
const ADMINISTRADORES = [
  { id: '0199e2a0-0000-7000-8000-0000000000c1', nombre: 'Ana Pérez', correo: 'ana.perez@cacaosdelhuila.co', activo: true },
  { id: '0199e2a0-0000-7000-8000-0000000000c2', nombre: 'Jorge Medina', correo: 'jorge.medina@cacaosdelhuila.co', activo: false },
];

/** La ruta de los administradores de la empresa en la API. */
const API_ADMINISTRADORES = `${API_EMPRESAS}/${EMPRESA.id}/administradores`;

/**
 * Abre la pantalla de empresas con los administradores en la API simulada, y anota cada contraseña restablecida.
 *
 * @returns {{ usuario: import('@testing-library/user-event').UserEvent, restablecidas: object[] }} el usuario de la
 *   prueba y lo que recibió la API al restablecer: el administrador y el cuerpo
 */
function abrirEmpresas() {
  conSesion(USUARIOS.ADMIN_PLATAFORMA);
  apiSimulada(API_EMPRESAS, [EMPRESA], OPCIONES_EMPRESAS);
  const restablecidas = [];
  servidor.use(
    http.get(API_ADMINISTRADORES, () => HttpResponse.json(ADMINISTRADORES)),
    http.put(`${API_ADMINISTRADORES}/:usuarioId/contrasena`, async ({ request, params }) => {
      restablecidas.push({ usuarioId: params.usuarioId, cuerpo: await request.json() });
      return new HttpResponse(null, { status: 204 });
    }),
  );
  const { usuario } = abrirApp(RUTA_EMPRESAS);
  return { usuario, restablecidas };
}

/**
 * Abre el panel de administradores de Cacaos del Huila y el formulario para restablecerle la contraseña a Jorge.
 *
 * @param {import('@testing-library/user-event').UserEvent} usuario el usuario de la prueba
 * @returns {Promise<HTMLElement>} el panel
 */
async function abrirFormularioDeJorge(usuario) {
  await usuario.click(await screen.findByRole('button', { name: 'Administradores de Cacaos del Huila' }));
  const panel = await screen.findByRole('region', { name: 'Administradores de Cacaos del Huila' });
  await usuario.click(await within(panel).findByRole('button', { name: 'Restablecer la contraseña de Jorge Medina' }));
  return panel;
}

/**
 * Escribe la contraseña nueva y la envía.
 *
 * @param {import('@testing-library/user-event').UserEvent} usuario el usuario de la prueba
 * @param {HTMLElement} panel el panel de administradores
 * @param {string} contrasena la contraseña nueva
 */
async function restablecer(usuario, panel, contrasena) {
  await usuario.type(within(panel).getByLabelText('Contraseña nueva'), contrasena);
  await usuario.click(within(panel).getByRole('button', { name: 'Restablecer contraseña' }));
}

describe('panel de administradores (RF-02 C12)', () => {
  it('RF-02 · C12 · el administrador de la plataforma ve los administradores de una empresa y restablece la contraseña de uno', async () => {
    const { usuario, restablecidas } = abrirEmpresas();

    await usuario.click(await screen.findByRole('button', { name: 'Administradores de Cacaos del Huila' }));
    const panel = await screen.findByRole('region', { name: 'Administradores de Cacaos del Huila' });
    expect(within(panel).getByText('Elige a quién restablecerle la contraseña.')).toBeInTheDocument();
    expect(await within(panel).findByText('jorge.medina@cacaosdelhuila.co')).toBeInTheDocument();
    expect(within(panel).getByText('Activo')).toBeInTheDocument();
    expect(within(panel).getByText('Desactivado')).toBeInTheDocument();
    await usuario.click(within(panel).getByRole('button', { name: 'Restablecer la contraseña de Jorge Medina' }));
    expect(within(panel).getByRole('heading', { name: 'Contraseña nueva de Jorge Medina' })).toBeInTheDocument();
    await restablecer(usuario, panel, 'Clave-Nueva-2026');

    expect(await within(panel).findByText('Se restableció la contraseña de Jorge Medina.')).toBeInTheDocument();
    expect(restablecidas).toEqual([{ usuarioId: ADMINISTRADORES[1].id, cuerpo: { contrasena: 'Clave-Nueva-2026' } }]);
  });

  it('RF-02 · C12 · antes de restablecer, el panel dice que se cierran sus sesiones abiertas y se le quita el bloqueo (D-30)', async () => {
    const { usuario } = abrirEmpresas();

    const panel = await abrirFormularioDeJorge(usuario);

    expect(within(panel).getByText('Al restablecerla, se cierran sus sesiones abiertas y se quita el bloqueo por intentos '
      + 'fallidos, si lo tenía. Compártela con Jorge: podrá cambiarla en «Cambiar contraseña».')).toBeInTheDocument();
    expect(within(panel).getByLabelText('Contraseña nueva'))
      .toHaveAccessibleDescription('De 8 a 64 caracteres; cada tilde o eñe ocupa 2 de los 72 bytes.');
  });

  it.each([
    ['7 caracteres', 'a'.repeat(7), 'La contraseña debe tener al menos 8 caracteres.'],
    ['8 caracteres', 'a'.repeat(8), null],
    ['64 caracteres con 8 eñes (72 bytes)', 'ñ'.repeat(8) + 'a'.repeat(56), null],
    ['64 caracteres con 9 eñes (73 bytes)', 'ñ'.repeat(9) + 'a'.repeat(55),
      'La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes'],
    ['65 caracteres', 'a'.repeat(65), 'La contraseña puede tener como máximo 64 caracteres.'],
    ['solo espacios', ' '.repeat(8), 'Este dato es obligatorio.'],
  ])('RF-02 · C12 · la contraseña nueva pasa por las mismas validaciones de la contraseña inicial (prueba de límite): %s',
    async (_, contrasena, mensaje) => {
      const { usuario, restablecidas } = abrirEmpresas();
      const panel = await abrirFormularioDeJorge(usuario);

      await restablecer(usuario, panel, contrasena);

      if (mensaje) {
        expect(await within(panel).findByText(mensaje)).toBeInTheDocument();
        expect(restablecidas).toEqual([]);
      } else {
        expect(await within(panel).findByText('Se restableció la contraseña de Jorge Medina.')).toBeInTheDocument();
      }
    });

  it('RF-02 · un error de la API al restablecer se ve en el campo, o arriba del formulario si no es de un campo', async () => {
    const { usuario } = abrirEmpresas();
    const panel = await abrirFormularioDeJorge(usuario);
    servidor.use(http.put(`${API_ADMINISTRADORES}/:usuarioId/contrasena`, () => problema(400, 'Hay datos que corregir.', {
      errores: [{ campo: 'contrasena', mensaje: 'La contraseña debe tener al menos 8 caracteres.' }],
    })));

    await restablecer(usuario, panel, 'Clave-Nueva-2026');
    expect(await within(panel).findByText('La contraseña debe tener al menos 8 caracteres.')).toBeInTheDocument();

    servidor.use(http.put(`${API_ADMINISTRADORES}/:usuarioId/contrasena`, () => problema(404, 'El registro no existe.')));
    await usuario.click(within(panel).getByRole('button', { name: 'Restablecer contraseña' }));
    expect(await within(panel).findByRole('alert')).toHaveTextContent('El registro no existe.');
  });

  it('RF-02 · «Cerrar» cierra el panel de administradores y la lista de empresas sigue a la vista', async () => {
    const { usuario } = abrirEmpresas();

    await usuario.click(await screen.findByRole('button', { name: 'Administradores de Cacaos del Huila' }));
    const panel = await screen.findByRole('region', { name: 'Administradores de Cacaos del Huila' });
    await usuario.click(within(panel).getByRole('button', { name: 'Cerrar' }));

    expect(screen.queryByRole('region', { name: 'Administradores de Cacaos del Huila' })).not.toBeInTheDocument();
    expect(screen.getByRole('table', { name: 'Empresas registradas' })).toBeInTheDocument();
  });

  it('RNF-06 · C4 · al abrir el panel, el foco va a su título; al cerrarlo, vuelve al botón de la fila', async () => {
    const { usuario } = abrirEmpresas();
    const boton = await screen.findByRole('button', { name: 'Administradores de Cacaos del Huila' });

    await usuario.click(boton);
    const panel = await screen.findByRole('region', { name: 'Administradores de Cacaos del Huila' });
    expect(within(panel).getByRole('heading', { name: 'Administradores de Cacaos del Huila' })).toHaveFocus();
    await usuario.click(within(panel).getByRole('button', { name: 'Cerrar' }));

    expect(boton).toHaveFocus();
  });

  it('RNF-06 · C4 · el formulario de la contraseña recibe el foco en su título; al cancelar o al restablecer, vuelve al botón del administrador', async () => {
    const { usuario } = abrirEmpresas();

    const panel = await abrirFormularioDeJorge(usuario);
    const boton = within(panel).getByRole('button', { name: 'Restablecer la contraseña de Jorge Medina' });
    expect(within(panel).getByRole('heading', { name: 'Contraseña nueva de Jorge Medina' })).toHaveFocus();
    await usuario.click(within(panel).getByRole('button', { name: 'Cancelar' }));
    expect(boton).toHaveFocus();
    await usuario.click(boton);
    await restablecer(usuario, panel, 'Clave-Nueva-2026');

    expect(await within(panel).findByText('Se restableció la contraseña de Jorge Medina.')).toBeInTheDocument();
    expect(boton).toHaveFocus();
  });
});
