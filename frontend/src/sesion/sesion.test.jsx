/*
 * Nombre: sesion.test.jsx
 * Descripción: Pruebas de la sesión en la app (RF-01, RF-18, RNF-04, D-28, D-31): las pantallas internas sin
 *              sesión, el rol que responde el backend al abrir, el vencimiento a las 8 horas, el 401 de una
 *              sesión que dejó de servir, volver tras un 401, cerrar sesión y las pestañas.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { act, screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { abrirApp, conSesion, enHoras, respuestaDeSesion, USUARIOS } from '../pruebas/ayudas.jsx';
import { problema, servidor } from '../pruebas/servidor.js';
import { CLAVE_SESION, leerSesion } from './almacen.js';

const ANA = USUARIOS.ADMIN_EMPRESA;
const CARLOS = USUARIOS.USUARIO_FINAL;
const CAMBIADA = 'Tu contraseña cambió: inicia sesión de nuevo.';

/**
 * Inicia sesión desde la pantalla Iniciar sesión, con el backend respondiendo por la persona dada.
 *
 * @param {import('@testing-library/user-event').UserEvent} usuario quien usa la pantalla
 * @param {object} persona el usuario que entra, de USUARIOS
 */
async function entrarComo(usuario, persona) {
  servidor.use(http.post('/api/v1/sesion', () => HttpResponse.json(respuestaDeSesion(persona))));
  await usuario.type(screen.getByLabelText('Correo'), persona.correo);
  await usuario.type(screen.getByLabelText('Contraseña'), 'Clave-Segura-2026');
  await usuario.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
}

afterEach(() => {
  vi.useRealTimers();
});

describe('sesión', () => {
  it('RNF-04 · sin sesión, una pantalla interna lleva a Iniciar sesión', () => {
    abrirApp('/cambiar-contrasena');

    expect(screen.getByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument();
  });

  it('RF-01 · C1 · al abrir la app con la sesión guardada entra sin pedir los datos, con el rol que responde el backend: un cambio de rol se ve al volver a abrirla (D-28)', async () => {
    conSesion(ANA);
    servidor.use(http.get('/api/v1/sesion', () => HttpResponse.json({ vence: enHoras(6), usuario: { ...ANA, rol: 'USUARIO_FINAL' } })));

    abrirApp('/');

    expect(await screen.findByRole('link', { name: 'Nuevo lote' })).toBeInTheDocument();
    const menu = screen.getByRole('navigation', { name: 'Menú principal' });
    expect(within(menu).queryByRole('link', { name: 'Usuarios' })).not.toBeInTheDocument();
  });

  it('RF-01 · C6 · la sesión vence a las 8 horas: la app vuelve a Iniciar sesión y pide entrar de nuevo', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    conSesion(ANA, { vence: enHoras(8) });
    abrirApp('/');
    await screen.findByRole('heading', { name: 'Hola, Ana' });

    await act(async () => {
      vi.advanceTimersByTime(8 * 3_600_000);
    });

    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument();
    expect(screen.getByText('Tu sesión venció: inicia sesión de nuevo.')).toBeInTheDocument();
    expect(leerSesion()).toBeNull();
  });

  it('RF-18 · C5 · si la contraseña cambió en otra sesión, esta deja de servir: la siguiente petición lleva a Iniciar sesión con «Tu contraseña cambió»', async () => {
    conSesion(ANA);
    servidor.use(http.get('/api/v1/sesion', () => problema(401, CAMBIADA)));

    abrirApp('/');

    expect(await screen.findByText(CAMBIADA)).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument();
    expect(leerSesion()).toBeNull();
  });

  it('RF-01 · después de un 401, si entra el mismo usuario vuelve a la pantalla donde estaba', async () => {
    conSesion(ANA);
    servidor.use(http.get('/api/v1/sesion', () => problema(401, CAMBIADA)));
    const { usuario } = abrirApp('/cambiar-contrasena');
    await screen.findByText(CAMBIADA);

    await entrarComo(usuario, ANA);

    expect(await screen.findByRole('heading', { name: 'Cambiar contraseña' })).toBeInTheDocument();
  });

  it('RF-01 · después de un 401, si entra otro usuario va al inicio de su rol', async () => {
    conSesion(ANA);
    servidor.use(http.get('/api/v1/sesion', () => problema(401, CAMBIADA)));
    const { usuario } = abrirApp('/cambiar-contrasena');
    await screen.findByText(CAMBIADA);

    await entrarComo(usuario, CARLOS);

    expect(await screen.findByRole('heading', { name: 'Hola, Carlos' })).toBeInTheDocument();
  });

  it('RF-01 · Cerrar sesión borra el token y vuelve a Iniciar sesión; al entrar de nuevo va al inicio (D-28)', async () => {
    conSesion(ANA);
    const { usuario } = abrirApp('/cambiar-contrasena');

    await usuario.click(within(await screen.findByRole('navigation', { name: 'Menú principal' }))
      .getByRole('button', { name: 'Cerrar sesión' }));

    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument();
    expect(leerSesion()).toBeNull();
    await entrarComo(usuario, ANA);
    expect(await screen.findByRole('heading', { name: 'Hola, Ana' })).toBeInTheDocument();
  });

  it('RF-01 · cerrar sesión en otra pestaña la cierra también en esta (D-31)', async () => {
    conSesion(ANA);
    abrirApp('/');
    await screen.findByRole('heading', { name: 'Hola, Ana' });

    act(() => {
      localStorage.removeItem(CLAVE_SESION);
      window.dispatchEvent(new StorageEvent('storage', { key: CLAVE_SESION, newValue: null }));
    });

    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument();
  });
});
