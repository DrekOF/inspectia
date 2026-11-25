/*
 * Nombre: PantallaDeRegistros.test.jsx
 * Descripción: Pruebas de lo que las pantallas generadas tienen en común (RP-23), sobre la de empresas: las
 *              páginas de 20, las tarjetas en el celular (RNF-05), el foco en el primer error, el foco al abrir y
 *              cerrar el formulario y al desactivar o reactivar, la caja de la tabla que recibe el foco
 *              (RNF-06 C4), los errores de la API que no son de un campo, cancelar, volver a pedir la lista
 *              después de registrar y las notas del JSON. Lo propio
 *              de cada entidad lo revisan sus pruebas generadas.
 * Fecha de creación: 26/10/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import { screen, waitFor, within } from '@testing-library/react';
import { http } from 'msw';
import { describe, expect, it } from 'vitest';
import { abrirApp, conSesion, USUARIOS } from '../pruebas/ayudas.jsx';
import { apiSimulada } from '../pruebas/crud.js';
import {
  API_EMPRESAS, DATOS_DE_EMPRESA, EMPRESA, EMPRESA_DESACTIVADA, llenarEmpresa, OPCIONES_EMPRESAS, RUTA_EMPRESAS,
} from '../pruebas/empresas.js';
import { anchoDePantalla } from '../pruebas/preparacion.js';
import { problema, servidor } from '../pruebas/servidor.js';

/**
 * La tabla de empresas, cuando termina de cargar.
 *
 * @returns {Promise<HTMLElement>} la tabla
 */
const tablaDeEmpresas = () => screen.findByRole('table', { name: 'Empresas registradas' });

/**
 * Empresas numeradas, como «Empresa 01», para llenar más de una página.
 *
 * @param {number} cantidad cuántas
 * @returns {object[]} las empresas, como las responde la API
 */
const empresasNumeradas = (cantidad) => Array.from({ length: cantidad }, (_, i) => ({
  ...EMPRESA, id: `0199e2a0-0000-7000-8000-${String(i).padStart(12, '0')}`, nombre: `Empresa ${String(i + 1).padStart(2, '0')}`,
}));

/**
 * Abre la pantalla de empresas como administrador de la plataforma, con la API simulada.
 *
 * @param {object[]} registros las empresas que tiene la API
 * @returns {{ usuario: import('@testing-library/user-event').UserEvent, simulada: import('../pruebas/crud.js').ApiSimulada }}
 *   el usuario de la prueba y la API simulada
 */
function abrirEmpresas(registros = [EMPRESA, EMPRESA_DESACTIVADA]) {
  conSesion(USUARIOS.ADMIN_PLATAFORMA);
  const simulada = apiSimulada(API_EMPRESAS, registros, OPCIONES_EMPRESAS);
  const { usuario } = abrirApp(RUTA_EMPRESAS);
  return { usuario, simulada };
}

describe('pantalla de registros', () => {
  it('RP-23 · con más de 20 registros, la lista va en páginas fijas de 20, pasa a la siguiente y la pide a la API', async () => {
    const { usuario, simulada } = abrirEmpresas(empresasNumeradas(25));

    const tabla = await tablaDeEmpresas();
    expect(within(tabla).getAllByRole('row')).toHaveLength(21);
    expect(screen.getByText('1–20 de 25')).toBeInTheDocument();
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
    await usuario.click(screen.getByRole('button', { name: 'Ir a la página siguiente' }));

    expect(await screen.findByText('Empresa 21')).toBeInTheDocument();
    expect(simulada.peticiones.map(({ ruta }) => ruta)).toContain(`${API_EMPRESAS}?pagina=1&tamano=20`);
  });

  it('RNF-06 · C4 · la caja de la tabla recibe el foco y lleva el nombre de la lista, para recorrerla con el teclado si no cabe en la ventana', async () => {
    abrirEmpresas();

    const caja = await screen.findByRole('region', { name: 'Empresas registradas' });
    expect(caja).toHaveAttribute('tabindex', '0');
    expect(within(caja).getByRole('table', { name: 'Empresas registradas' })).toBeInTheDocument();
  });

  it('RNF-05 · en el celular, la lista se ve en tarjetas, cada una con sus datos y sus acciones, sin tabla', async () => {
    anchoDePantalla(390);
    abrirEmpresas();

    const lista = await screen.findByRole('list', { name: 'Empresas registradas' });
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
    const tarjetas = within(lista).getAllByRole('listitem');
    expect(tarjetas).toHaveLength(2);
    expect(within(tarjetas[0]).getByText('Cacaos del Huila')).toBeInTheDocument();
    expect(within(tarjetas[0]).getByText('NIT 900123456-8')).toBeInTheDocument();
    expect(within(tarjetas[0]).getByText('Activa')).toBeInTheDocument();
    expect(within(tarjetas[0]).getByRole('button', { name: 'Editar Cacaos del Huila' })).toBeInTheDocument();
    expect(within(tarjetas[1]).getByRole('button', { name: 'Reactivar Comercializadora La Mazorca' })).toBeInTheDocument();
  });

  it('RNF-06 · C3 · al registrar con datos que faltan, el foco va al primer campo con error, también dentro del grupo del administrador', async () => {
    const { usuario } = abrirEmpresas();

    await llenarEmpresa(usuario, { ...DATOS_DE_EMPRESA, administrador: {} });
    await usuario.click(screen.getByRole('button', { name: 'Registrar empresa' }));

    const administrador = within(screen.getByRole('group', { name: 'Primer administrador' }));
    await waitFor(() => expect(administrador.getByLabelText('Nombre')).toHaveFocus());
  });

  it('RP-23 · un error de la API que no es de un campo, como el de una regla sin campo, se ve arriba del formulario, que sigue abierto', async () => {
    const { usuario } = abrirEmpresas();
    servidor.use(http.post(API_EMPRESAS, () => problema(400, 'La empresa no se pudo registrar.', { errores: [] })));

    await llenarEmpresa(usuario);
    await usuario.click(screen.getByRole('button', { name: 'Registrar empresa' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('La empresa no se pudo registrar.');
    expect(screen.getByRole('group', { name: 'Empresa' })).toBeInTheDocument();
  });

  it('RP-23 · «Cancelar» cierra el formulario sin enviar nada a la API', async () => {
    const { usuario, simulada } = abrirEmpresas();

    await llenarEmpresa(usuario);
    await usuario.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(screen.queryByRole('group', { name: 'Empresa' })).not.toBeInTheDocument();
    expect(simulada.peticiones.filter(({ metodo }) => metodo !== 'GET')).toEqual([]);
  });

  it('RP-23 · después de registrar, el formulario se cierra y la lista y su resumen se vuelven a pedir, con el registro nuevo', async () => {
    const { usuario } = abrirEmpresas([EMPRESA]);
    expect(await screen.findByText('1 empresa registrada: 1 activa y 0 desactivadas.')).toBeInTheDocument();

    await llenarEmpresa(usuario);
    await usuario.click(screen.getByRole('button', { name: 'Registrar empresa' }));

    expect(await screen.findByText('Se guardó Cacao Andino.')).toBeInTheDocument();
    expect(screen.queryByRole('group', { name: 'Empresa' })).not.toBeInTheDocument();
    expect(await within(await tablaDeEmpresas()).findByText('Cacao Andino')).toBeInTheDocument();
    expect(await screen.findByText('2 empresas registradas: 2 activas y 0 desactivadas.')).toBeInTheDocument();
  });

  it('RNF-04 · si la API no deja hacer una acción, la pantalla dice por qué y la fila queda como estaba', async () => {
    const { usuario } = abrirEmpresas();
    servidor.use(http.post(`${API_EMPRESAS}/:id/desactivar`, () => problema(403, 'No tienes permiso para esta operación.')));

    await usuario.click(await screen.findByRole('button', { name: 'Desactivar Cacaos del Huila' }));
    await usuario.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Desactivar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('No tienes permiso para esta operación.');
    expect(within(await tablaDeEmpresas()).getByRole('button', { name: 'Desactivar Cacaos del Huila' })).toBeInTheDocument();
  });

  it('RP-23 · las notas del JSON se ven donde van: la del formulario solo al registrar, y la de la lista debajo de ella', async () => {
    const { usuario } = abrirEmpresas();
    const notaDelFormulario = 'La empresa queda con los rangos de recomendación por defecto. Después, su administrador crea los usuarios de la empresa.';

    expect(await screen.findByText(/^Desactivar una empresa no borra nada/)).toBeInTheDocument();
    await usuario.click(screen.getByRole('button', { name: 'Nueva empresa' }));
    expect(screen.getByText(notaDelFormulario)).toBeInTheDocument();
    await usuario.click(screen.getByRole('button', { name: 'Cancelar' }));
    await usuario.click(screen.getByRole('button', { name: 'Editar Cacaos del Huila' }));
    expect(screen.queryByText(notaDelFormulario)).not.toBeInTheDocument();
  });

  it('RNF-06 · C4 · al abrir el formulario desde una fila, el foco va a su título; al cancelar, vuelve al botón que lo abrió', async () => {
    const { usuario } = abrirEmpresas();
    const editar = await screen.findByRole('button', { name: 'Editar Cacaos del Huila' });

    await usuario.click(editar);
    expect(screen.getByRole('heading', { name: 'Editar empresa' })).toHaveFocus();
    await usuario.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(editar).toHaveFocus();
  });

  it('RNF-06 · C4 · después de registrar, el foco vuelve a «Nueva empresa»', async () => {
    const { usuario } = abrirEmpresas();

    await llenarEmpresa(usuario);
    await usuario.click(screen.getByRole('button', { name: 'Registrar empresa' }));

    expect(await screen.findByText('Se guardó Cacao Andino.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Nueva empresa' })).toHaveFocus();
  });

  it('RNF-06 · C4 · después de reactivar, el foco sigue en la fila, en su botón, que ahora desactiva', async () => {
    const { usuario } = abrirEmpresas();

    await usuario.click(await screen.findByRole('button', { name: 'Reactivar Comercializadora La Mazorca' }));

    expect(await screen.findByText('Se reactivó Comercializadora La Mazorca.')).toBeInTheDocument();
    expect(await screen.findByRole('button', { name: 'Desactivar Comercializadora La Mazorca' })).toHaveFocus();
  });

  it('RNF-06 · C4 · después de confirmar que se desactiva, el foco vuelve a la fila, en su botón, que ahora reactiva', async () => {
    const { usuario } = abrirEmpresas();

    await usuario.click(await screen.findByRole('button', { name: 'Desactivar Cacaos del Huila' }));
    await usuario.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Desactivar' }));

    expect(await screen.findByText('Se desactivó Cacaos del Huila.')).toBeInTheDocument();
    const reactivar = await screen.findByRole('button', { name: 'Reactivar Cacaos del Huila' });
    await waitFor(() => expect(reactivar).toHaveFocus());
  });
});
