/*
 * Nombre: CambiarContrasena.test.jsx
 * Descripción: Pruebas de la pantalla Cambiar contraseña (RF-18): el cambio con el token nuevo, la
 *              contraseña actual errada, el bloqueo con su hora, las reglas de la contraseña nueva y que esta
 *              sesión siga abierta.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import { abrirApp, conSesion, enHoras, respuestaDeSesion, USUARIOS } from '../pruebas/ayudas.jsx';
import { problema, servidor } from '../pruebas/servidor.js';
import { leerSesion } from '../sesion/almacen.js';

const ANA = USUARIOS.ADMIN_EMPRESA;
const ACTUAL = 'Clave-Actual-2026';
const NUEVA = 'Granos del Huila';
const AYUDA = 'Si fallas 5 veces seguidas, la cuenta se bloquea 15 minutos.';
let vence;

/**
 * Escribe la contraseña actual, la nueva y la repetida y pulsa «Guardar».
 *
 * @param {import('@testing-library/user-event').UserEvent} usuario quien usa la pantalla
 * @param {{ actual?: string, nueva?: string, repetida?: string }} [datos] lo que escribe
 */
async function guardar(usuario, { actual = ACTUAL, nueva = NUEVA, repetida = nueva } = {}) {
  if (actual) await usuario.type(screen.getByLabelText('Contraseña actual'), actual);
  if (nueva) await usuario.type(screen.getByLabelText('Contraseña nueva'), nueva);
  if (repetida) await usuario.type(screen.getByLabelText('Repite la contraseña nueva'), repetida);
  await usuario.click(screen.getByRole('button', { name: 'Guardar' }));
}

/**
 * El estado que muestra una regla de la contraseña nueva: «Cumple» o «No cumple».
 *
 * @param {string} regla el texto de la regla
 * @returns {string} el estado
 */
function estadoDe(regla) {
  return within(screen.getByText(regla).closest('li')).getByText(/^(Cumple|No cumple)$/).textContent;
}

describe('Cambiar contraseña', () => {
  beforeEach(() => {
    vence = enHoras(6);
    conSesion(ANA, { token: 'token-viejo', vence });
  });

  it('RF-18 · C1 · con la contraseña actual correcta y una nueva válida la cambia y avisa que las otras sesiones se cerraron', async () => {
    let peticion;
    servidor.use(http.put('/api/v1/sesion/contrasena', async ({ request }) => {
      peticion = { autorizacion: request.headers.get('Authorization'), cuerpo: await request.json() };
      return HttpResponse.json(respuestaDeSesion(ANA, { token: 'token-nuevo', vence }));
    }));
    const { usuario } = abrirApp('/cambiar-contrasena');

    await guardar(usuario);

    expect(await screen.findByRole('status')).toHaveTextContent(
      'Tu contraseña cambió. Tus otras sesiones se cerraron; esta sigue abierta.');
    expect(peticion).toEqual({ autorizacion: 'Bearer token-viejo',
      cuerpo: { contrasenaActual: ACTUAL, contrasenaNueva: NUEVA } });
  });

  it('RF-18 · C5 · al cambiarla, esta sesión sigue abierta con el token nuevo, que vence a la misma hora', async () => {
    servidor.use(http.put('/api/v1/sesion/contrasena',
      () => HttpResponse.json(respuestaDeSesion(ANA, { token: 'token-nuevo', vence }))));
    const { usuario } = abrirApp('/cambiar-contrasena');

    await guardar(usuario);

    await screen.findByRole('status');
    expect(leerSesion()).toEqual({ token: 'token-nuevo', vence, usuario: ANA });
    expect(screen.getByRole('heading', { name: 'Cambiar contraseña' })).toBeInTheDocument();
  });

  it('RF-18 · C2 · con la contraseña actual errada marca ese campo con el mensaje y la regla del bloqueo, y no cambia nada', async () => {
    servidor.use(http.put('/api/v1/sesion/contrasena', () => problema(400, 'Revisa los datos marcados.', {
      errores: [{ campo: 'contrasenaActual', mensaje: 'La contraseña actual no es correcta.' }], ayuda: AYUDA,
    })));
    const { usuario } = abrirApp('/cambiar-contrasena');

    await guardar(usuario, { actual: 'no-es-la-actual' });

    expect(await screen.findByText(`La contraseña actual no es correcta. ${AYUDA}`)).toBeInTheDocument();
    expect(screen.getByLabelText('Contraseña actual')).toHaveAttribute('aria-invalid', 'true');
    expect(leerSesion().token).toBe('token-viejo');
  });

  it('RF-18 · C2 · con la cuenta bloqueada avisa que no se puede cambiar y a qué hora termina el bloqueo', async () => {
    const hasta = '2026-10-20T20:45:00Z';
    servidor.use(http.put('/api/v1/sesion/contrasena', () => problema(429,
      'Tu cuenta está bloqueada por intentos fallidos: podrás cambiar la contraseña cuando termine el bloqueo.',
      { bloqueadoHasta: hasta }, { 'Retry-After': '540' })));
    // Intl separa «p. m.» con espacios que no se cortan, y toHaveTextContent los cambia por espacios comunes.
    const hora = new Intl.DateTimeFormat('es-CO', { hour: 'numeric', minute: '2-digit' }).format(new Date(hasta))
      .replace(/\s/g, ' ');
    const { usuario } = abrirApp('/cambiar-contrasena');

    await guardar(usuario);

    const aviso = await screen.findByRole('alert');
    expect(aviso).toHaveTextContent('Tu cuenta está bloqueada por intentos fallidos');
    expect(aviso).toHaveTextContent(`Podrás intentarlo a las ${hora}.`);
  });

  it('RF-18 · C3 · las reglas de la contraseña nueva se ven antes de guardar y dicen si el largo se cumple: 7 no, 8 sí', async () => {
    const { usuario } = abrirApp('/cambiar-contrasena');
    const nueva = screen.getByLabelText('Contraseña nueva');

    await usuario.type(nueva, 'a'.repeat(7));
    expect(estadoDe('De 8 a 64 caracteres.')).toBe('No cumple');

    await usuario.type(nueva, 'a');
    expect(estadoDe('De 8 a 64 caracteres.')).toBe('Cumple');
  });

  it('RF-18 · C4 · la regla de los 72 bytes cuenta 2 por cada eñe: con 9 eñes en 64 caracteres no se cumple', async () => {
    const { usuario } = abrirApp('/cambiar-contrasena');

    await usuario.type(screen.getByLabelText('Contraseña nueva'), 'ñ'.repeat(9) + 'a'.repeat(55));

    expect(estadoDe('Como máximo 72 bytes: cada letra con tilde y cada eñe ocupan 2.')).toBe('No cumple');
    expect(estadoDe('De 8 a 64 caracteres.')).toBe('Cumple');
  });

  it('RF-18 · si la contraseña repetida no coincide, no envía nada y lo marca en ese campo', async () => {
    let enviados = 0;
    servidor.use(http.put('/api/v1/sesion/contrasena', () => {
      enviados += 1;
      return HttpResponse.json({});
    }));
    const { usuario } = abrirApp('/cambiar-contrasena');

    await guardar(usuario, { repetida: 'Granos del Cauca' });

    expect(await screen.findByText('No coincide con la contraseña nueva.')).toBeInTheDocument();
    expect(enviados).toBe(0);
  });

  it('RF-18 · Cancelar vuelve al inicio sin cambiar nada', async () => {
    const { usuario } = abrirApp('/cambiar-contrasena');

    await usuario.click(screen.getByRole('link', { name: 'Cancelar' }));

    expect(await screen.findByRole('heading', { name: 'Hola, Ana' })).toBeInTheDocument();
    expect(leerSesion().token).toBe('token-viejo');
  });
});
