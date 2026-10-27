/*
 * Nombre: Marco.test.jsx
 * Descripción: Pruebas del marco de la app con sesión (RF-01, RNF-04, RNF-05, RNF-06): el enlace «Saltar al
 *              contenido», la barra con el usuario, las secciones pendientes, las rutas de otro rol, las que
 *              no existen, el menú en el celular, y el idioma y el tema desde la barra (RP-08, RP-11).
 * Fecha de creación: 20/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import { screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { abrirApp, conSesion, USUARIOS } from '../pruebas/ayudas.jsx';
import { anchoDePantalla } from '../pruebas/preparacion.js';

const menuPrincipal = () => screen.findByRole('navigation', { name: 'Menú principal' });

describe('marco de la app', () => {
  it('RNF-06 · C4 · cada pantalla con menú empieza con el enlace «Saltar al contenido», que lleva al contenido', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    abrirApp('/');
    await menuPrincipal();

    const saltar = screen.getByRole('link', { name: 'Saltar al contenido' });
    expect(saltar).toHaveAttribute('href', '#contenido');
    expect(document.getElementById('contenido')).toBe(screen.getByRole('main'));
    expect(screen.getAllByRole('link')[0]).toBe(saltar);
  });

  it.each([
    ['ADMIN_EMPRESA', 'Mi cuenta: Ana Pérez, Cacaos del Huila'],
    ['USUARIO_FINAL', 'Mi cuenta: Carlos Ramírez, Cacaos del Huila'],
    ['ADMIN_PLATAFORMA', 'Mi cuenta: Andrés Gómez, Administrador de la plataforma'],
  ])('RF-01 · C1 · la barra muestra quién entró: su nombre y su empresa, o su rol si no tiene empresa: %s', async (rol, nombre) => {
    conSesion(USUARIOS[rol]);
    const { usuario } = abrirApp('/');

    await usuario.click(await screen.findByRole('button', { name: nombre }));

    const cuenta = screen.getByRole('menu');
    expect(within(cuenta).getAllByRole('menuitem').map((opcion) => opcion.textContent))
      .toEqual(['Cambiar contraseña', 'Acerca de', 'Cerrar sesión']);
  });

  it.each([['Estadísticas'], ['Modelo de IA'], ['Historial']])(
    'RF-01 · las secciones que aún no existen dicen «Esta sección estará disponible pronto.», sin códigos de requisitos: %s',
    async (seccion) => {
      conSesion(USUARIOS.ADMIN_EMPRESA);
      const { usuario } = abrirApp('/');

      await usuario.click(within(await menuPrincipal()).getByRole('link', { name: seccion }));

      expect(await screen.findByRole('heading', { name: seccion })).toBeInTheDocument();
      const contenido = screen.getByRole('main');
      expect(contenido).toHaveTextContent('Esta sección estará disponible pronto.');
      expect(contenido.textContent).not.toMatch(/\b(RF|RNF|RP|D|P)-\d{2}\b/);
      expect(within(await menuPrincipal()).getByRole('link', { name: seccion })).toHaveAttribute('aria-current', 'page');
    },
  );

  it('RNF-04 · una pantalla de otro rol no se abre aunque se escriba su dirección: avisa que no tiene permiso', async () => {
    conSesion(USUARIOS.USUARIO_FINAL);

    abrirApp('/usuarios');

    expect(await screen.findByText('No tienes permiso para esta operación.')).toBeInTheDocument();
  });

  it('RF-01 · una dirección que no existe lo dice y ofrece volver al inicio', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    const { usuario } = abrirApp('/no-existe');

    expect(await screen.findByRole('heading', { name: 'Esta página no existe' })).toBeInTheDocument();
    await usuario.click(screen.getByRole('link', { name: 'Ir al inicio' }));
    expect(await screen.findByRole('heading', { name: 'Hola, Ana' })).toBeInTheDocument();
  });

  it('RNF-05 · en el celular el menú se esconde detrás del botón «Abrir el menú»', async () => {
    anchoDePantalla(390);
    conSesion(USUARIOS.USUARIO_FINAL);
    const { usuario } = abrirApp('/');
    await screen.findByRole('heading', { name: 'Hola, Carlos' });

    expect(screen.queryByRole('navigation', { name: 'Menú principal' })).not.toBeInTheDocument();
    await usuario.click(screen.getByRole('button', { name: 'Abrir el menú' }));
    expect(within(await menuPrincipal()).getByRole('link', { name: 'Nuevo lote' })).toBeInTheDocument();
  });

  it('RP-11 · el idioma se cambia desde la barra y el menú queda en inglés', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    const { usuario } = abrirApp('/');

    await usuario.click(await screen.findByRole('button', { name: 'Idioma: Español' }));
    await usuario.click(screen.getByRole('menuitemradio', { name: 'English' }));

    const menu = await screen.findByRole('navigation', { name: 'Main menu' });
    expect(within(menu).getByRole('link', { name: 'Users' })).toBeInTheDocument();
    expect(document.documentElement).toHaveAttribute('lang', 'en');
  });

  it('RP-08 · el tema se cambia desde la barra', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    const { usuario } = abrirApp('/');

    await usuario.click(await screen.findByRole('button', { name: 'Cambiar a tema oscuro' }));

    expect(screen.getByRole('button', { name: 'Cambiar a tema claro' })).toBeInTheDocument();
  });
});
