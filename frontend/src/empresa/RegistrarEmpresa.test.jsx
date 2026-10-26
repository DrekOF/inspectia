/*
 * Nombre: RegistrarEmpresa.test.jsx
 * Descripción: Pruebas de los criterios de RF-02 en la pantalla de empresas, con la API simulada: registrar una
 *              empresa con su primer administrador (C1), datos repetidos (C2), datos que faltan (C3), el dígito de
 *              verificación del NIT (C4), los límites de la contraseña inicial (C5 y C6) y editar el NIT (C11).
 *              Que el administrador entre (C8) y desactivar y reactivar (C9 y C10) se prueban de punta a punta.
 * Fecha de creación: 26/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { screen, within } from '@testing-library/react';
import { http } from 'msw';
import { describe, expect, it } from 'vitest';
import { abrirApp, conSesion, USUARIOS } from '../pruebas/ayudas.jsx';
import { apiSimulada } from '../pruebas/crud.js';
import {
  API_EMPRESAS, DATOS_DE_EMPRESA, EMPRESA, llenarEmpresa, OPCIONES_EMPRESAS, RUTA_EMPRESAS,
} from '../pruebas/empresas.js';
import { problema, servidor } from '../pruebas/servidor.js';

/**
 * Abre la pantalla de empresas como administrador de la plataforma, con la API simulada.
 *
 * @param {object[]} [registros] las empresas que tiene la API
 * @returns {{ usuario: import('@testing-library/user-event').UserEvent, simulada: import('../pruebas/crud.js').ApiSimulada }}
 *   el usuario de la prueba y la API simulada
 */
function abrirEmpresas(registros = [EMPRESA]) {
  conSesion(USUARIOS.ADMIN_PLATAFORMA);
  const simulada = apiSimulada(API_EMPRESAS, registros, OPCIONES_EMPRESAS);
  const { usuario } = abrirApp(RUTA_EMPRESAS);
  return { usuario, simulada };
}

/**
 * Las peticiones que registraron una empresa.
 *
 * @param {import('../pruebas/crud.js').ApiSimulada} simulada la API simulada
 * @returns {import('../pruebas/crud.js').Peticion[]} las peticiones
 */
const enviadas = (simulada) => simulada.peticiones.filter(({ metodo }) => metodo === 'POST');

/**
 * Un campo del grupo de la empresa o del de su primer administrador, por su etiqueta.
 *
 * @param {string} grupo el nombre del grupo, como Primer administrador
 * @param {string} etiqueta la etiqueta del campo
 * @returns {HTMLElement} el campo
 */
const campoDe = (grupo, etiqueta) => within(screen.getByRole('group', { name: grupo })).getByLabelText(etiqueta);

/**
 * Llena el formulario con una contraseña inicial y lo envía.
 *
 * @param {import('@testing-library/user-event').UserEvent} usuario el usuario de la prueba
 * @param {string} contrasena la contraseña inicial
 */
async function registrarConContrasena(usuario, contrasena) {
  await llenarEmpresa(usuario, { ...DATOS_DE_EMPRESA, administrador: { ...DATOS_DE_EMPRESA.administrador, contrasena } });
  await usuario.click(screen.getByRole('button', { name: 'Registrar empresa' }));
}

describe('registrar una empresa (RF-02)', () => {
  it('RF-02 · C1 · con datos válidos, la pantalla registra la empresa y su primer administrador, y la muestra en la lista', async () => {
    const { usuario, simulada } = abrirEmpresas();

    await llenarEmpresa(usuario);
    await usuario.click(screen.getByRole('button', { name: 'Registrar empresa' }));

    expect(await screen.findByText('Se guardó Cacao Andino.')).toBeInTheDocument();
    expect(enviadas(simulada).map(({ cuerpo }) => cuerpo)).toEqual([DATOS_DE_EMPRESA]);
    const tabla = await screen.findByRole('table', { name: 'Empresas registradas' });
    expect(await within(tabla).findByText('NIT 901245780-7')).toBeInTheDocument();
  });

  it('RF-02 · C2 · si el NIT o el correo del administrador ya existen, no la registra y lo avisa en cada campo', async () => {
    const { usuario } = abrirEmpresas();
    servidor.use(http.post(API_EMPRESAS, () => problema(409, 'Ya existe un registro con estos datos.', {
      errores: [
        { campo: 'nit', mensaje: 'Ya existe una empresa con este NIT.' },
        { campo: 'administrador.correo', mensaje: 'Ya existe un usuario con este correo.' },
      ],
    })));

    await llenarEmpresa(usuario);
    await usuario.click(screen.getByRole('button', { name: 'Registrar empresa' }));

    expect(await screen.findByText('Ya existe una empresa con este NIT.')).toBeInTheDocument();
    expect(campoDe('Empresa', 'NIT')).toHaveAccessibleDescription(expect.stringContaining('Ya existe una empresa con este NIT.'));
    expect(campoDe('Primer administrador', 'Correo'))
      .toHaveAccessibleDescription(expect.stringContaining('Ya existe un usuario con este correo.'));
    expect(screen.getByRole('button', { name: 'Registrar empresa' })).toBeInTheDocument();
    expect(screen.queryByText('Se guardó Cacao Andino.')).not.toBeInTheDocument();
  });

  it('RF-02 · C3 · si faltan datos obligatorios, el formulario marca los campos que faltan y no envía nada', async () => {
    const { usuario, simulada } = abrirEmpresas();

    await usuario.click(await screen.findByRole('button', { name: 'Nueva empresa' }));
    await usuario.click(screen.getByRole('button', { name: 'Registrar empresa' }));

    expect(await screen.findAllByText('Este dato es obligatorio.')).toHaveLength(6);
    for (const [grupo, etiqueta] of [['Empresa', 'Nombre'], ['Empresa', 'NIT'], ['Empresa', 'Ciudad'],
      ['Primer administrador', 'Nombre'], ['Primer administrador', 'Correo'], ['Primer administrador', 'Contraseña inicial']]) {
      expect(campoDe(grupo, etiqueta)).toHaveAttribute('aria-invalid', 'true');
    }
    expect(enviadas(simulada)).toEqual([]);
  });

  it('RF-02 · C4 · si el dígito de verificación del NIT no corresponde, el formulario lo marca y no registra la empresa', async () => {
    const { usuario } = abrirEmpresas();
    servidor.use(http.post(API_EMPRESAS, () => problema(400, 'Hay datos que corregir.', {
      errores: [{ campo: 'nit', mensaje: 'El dígito de verificación del NIT no corresponde.' }],
    })));

    await llenarEmpresa(usuario, { ...DATOS_DE_EMPRESA, nit: '901245780-6' });
    await usuario.click(screen.getByRole('button', { name: 'Registrar empresa' }));

    expect(await screen.findByText('El dígito de verificación del NIT no corresponde.')).toBeInTheDocument();
    expect(campoDe('Empresa', 'NIT')).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByRole('button', { name: 'Registrar empresa' })).toBeInTheDocument();
    expect(within(screen.getByRole('table', { name: 'Empresas registradas' })).queryByText('Cacao Andino')).not.toBeInTheDocument();
  });

  it.each([
    ['7 caracteres', 'a'.repeat(7), 'La contraseña debe tener al menos 8 caracteres.'],
    ['8 caracteres', 'a'.repeat(8), null],
    ['64 caracteres', 'a'.repeat(64), null],
    ['65 caracteres', 'a'.repeat(65), 'La contraseña puede tener como máximo 64 caracteres.'],
  ])('RF-02 · C5 · la contraseña inicial acepta de 8 a 64 caracteres: con 7 o con 65 se rechaza (prueba de límite): %s',
    async (_, contrasena, mensaje) => {
      const { usuario, simulada } = abrirEmpresas();

      await registrarConContrasena(usuario, contrasena);

      if (mensaje) {
        expect(await screen.findByText(mensaje)).toBeInTheDocument();
        expect(enviadas(simulada)).toEqual([]);
      } else {
        expect(await screen.findByText('Se guardó Cacao Andino.')).toBeInTheDocument();
        expect(enviadas(simulada)[0].cuerpo.administrador.contrasena).toBe(contrasena);
      }
    });

  it.each([
    ['64 caracteres con 8 eñes (72 bytes)', 'ñ'.repeat(8) + 'a'.repeat(56), null],
    ['64 caracteres con 9 eñes (73 bytes)', 'ñ'.repeat(9) + 'a'.repeat(55),
      'La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes'],
  ])('RF-02 · C6 · una contraseña inicial de 64 caracteres con 8 eñes se acepta; con 9 eñes se rechaza con su mensaje (prueba de límite): %s',
    async (_, contrasena, mensaje) => {
      const { usuario, simulada } = abrirEmpresas();

      await registrarConContrasena(usuario, contrasena);

      if (mensaje) {
        expect(await screen.findByText(mensaje)).toBeInTheDocument();
        expect(enviadas(simulada)).toEqual([]);
      } else {
        expect(await screen.findByText('Se guardó Cacao Andino.')).toBeInTheDocument();
      }
    });

  it('RF-02 · C11 · al editar una empresa, el NIT pasa por las mismas validaciones del registro: su formato, su dígito y que no se repita', async () => {
    const { usuario, simulada } = abrirEmpresas();
    const editarNit = async (nit) => {
      const campo = screen.getByLabelText('NIT');
      await usuario.clear(campo);
      await usuario.type(campo, nit);
      await usuario.click(screen.getByRole('button', { name: 'Guardar cambios' }));
    };

    await usuario.click(await screen.findByRole('button', { name: 'Editar Cacaos del Huila' }));
    await editarNit('900.123.456-8');
    expect(await screen.findByText('Escribe el NIT sin puntos y con el dígito de verificación, por ejemplo 900123456-8.'))
      .toBeInTheDocument();
    expect(simulada.peticiones.filter(({ metodo }) => metodo === 'PUT')).toEqual([]);

    servidor.use(http.put(`${API_EMPRESAS}/:id`, () => problema(400, 'Hay datos que corregir.', {
      errores: [{ campo: 'nit', mensaje: 'El dígito de verificación del NIT no corresponde.' }],
    })));
    await editarNit('900123456-9');
    expect(await screen.findByText('El dígito de verificación del NIT no corresponde.')).toBeInTheDocument();

    servidor.use(http.put(`${API_EMPRESAS}/:id`, () => problema(409, 'Ya existe un registro con estos datos.', {
      errores: [{ campo: 'nit', mensaje: 'Ya existe una empresa con este NIT.' }],
    })));
    await editarNit('800215478-2');
    expect(await screen.findByText('Ya existe una empresa con este NIT.')).toBeInTheDocument();
  });
});
