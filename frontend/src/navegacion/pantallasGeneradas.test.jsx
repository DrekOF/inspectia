/*
 * Nombre: pantallasGeneradas.test.jsx
 * Descripción: Pruebas del menú y de las rutas de las pantallas generadas (RP-23, RNF-04): salen de
 *              src/generated/pantallas.js, con los roles de listar de cada JSON. Con una lista simulada que trae
 *              además una pantalla de producto, la otra temática, que menu.js no nombra.
 * Fecha de creación: 26/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { screen, within } from '@testing-library/react';
import { beforeAll, describe, expect, it, vi } from 'vitest';
import i18n from '../i18n/i18n.js';
import { abrirApp, conSesion, USUARIOS } from '../pruebas/ayudas.jsx';
import { menuPrincipal } from './menu.js';

vi.mock('../generated/pantallas.js', () => ({
  PANTALLAS: [
    {
      entidad: 'empresa', ruta: '/empresas', etiqueta: 'empresa.etiqueta.plural', icono: 'empresas',
      roles: ['ADMIN_PLATAFORMA'], Pantalla: () => 'Pantalla de empresas de prueba',
    },
    {
      entidad: 'producto', ruta: '/productos', etiqueta: 'producto.etiqueta.plural', icono: 'productos',
      roles: ['ADMIN_EMPRESA'], Pantalla: () => 'Pantalla de productos de prueba',
    },
  ],
}));

beforeAll(() => {
  i18n.addResource('es', 'translation', 'producto.etiqueta.plural', 'Productos');
});

describe('pantallas generadas', () => {
  it('RP-23 · una pantalla generada que menu.js no nombra entra al final del menú de los roles de su listar', () => {
    expect(menuPrincipal('ADMIN_EMPRESA').map(({ ruta }) => ruta).at(-1)).toBe('/productos');
    expect(menuPrincipal('ADMIN_PLATAFORMA').map(({ ruta }) => ruta)).toEqual(['/', '/empresas']);
  });

  it('RNF-04 · la ruta de una pantalla generada la abren los roles de listar de su JSON; los demás ven que no tienen permiso', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    abrirApp('/productos');
    expect(await screen.findByText('Pantalla de productos de prueba')).toBeInTheDocument();
  });

  it('RNF-04 · un rol que no está en listar no abre la pantalla generada aunque escriba su dirección', async () => {
    conSesion(USUARIOS.USUARIO_FINAL);
    abrirApp('/productos');
    expect(await screen.findByRole('heading', { name: 'Sin permiso' })).toBeInTheDocument();
  });

  it('RP-23 · una pantalla generada con un ícono que el menú no conoce sale con uno genérico', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    abrirApp('/');

    const menu = await screen.findByRole('navigation', { name: 'Menú principal' });
    expect(within(menu).getByRole('link', { name: 'Productos' })).toHaveAttribute('href', '/productos');
  });
});
