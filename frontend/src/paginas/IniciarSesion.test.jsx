/*
 * Nombre: IniciarSesion.test.jsx
 * Descripción: Pruebas de la pantalla Iniciar sesión (RF-01): entrar con cada rol, los mensajes del backend
 *              con datos errados o con una cuenta desactivada, la contraseña de más de 72 bytes, los campos
 *              obligatorios, los errores sin conexión (RNF-06), Acerca de sin sesión (RNF-08) y el idioma y
 *              el tema (RP-08, RP-11).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { cleanup, screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { abrirApp, conSesion, respuestaDeSesion, USUARIOS } from '../pruebas/ayudas.jsx';
import { problema, servidor } from '../pruebas/servidor.js';
import { leerSesion } from '../sesion/almacen.js';

const AYUDA = 'Si fallas 5 veces seguidas, la cuenta se bloquea 15 minutos.';

/**
 * Escribe el correo y la contraseña y pulsa «Iniciar sesión».
 *
 * @param {import('@testing-library/user-event').UserEvent} usuario quien usa la pantalla
 * @param {string} correo el correo
 * @param {string} [contrasena] la contraseña
 */
async function entrar(usuario, correo, contrasena = 'Clave-Segura-2026') {
  await usuario.type(screen.getByLabelText('Correo'), correo);
  await usuario.type(screen.getByLabelText('Contraseña'), contrasena);
  await usuario.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
}

describe('Iniciar sesión', () => {
  it('RF-01 · sin sesión, la app abre Iniciar sesión, con el correo y la contraseña listos para el navegador', () => {
    abrirApp('/');

    expect(screen.getByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument();
    expect(screen.getByLabelText('Correo')).toHaveAttribute('autocomplete', 'username');
    expect(screen.getByLabelText('Contraseña')).toHaveAttribute('autocomplete', 'current-password');
  });

  it.each([
    ['USUARIO_FINAL', 'Hola, Carlos', ['Inicio', 'Nuevo lote', 'Historial']],
    ['ADMIN_EMPRESA', 'Hola, Ana', ['Inicio', 'Historial', 'Usuarios', 'Rangos de recomendación', 'Estadísticas', 'Modelo de IA']],
    ['ADMIN_PLATAFORMA', 'Hola, Andrés', ['Inicio', 'Empresas']],
  ])('RF-01 · C1 · con datos correctos entra al inicio de su rol y ve el menú de su rol: %s', async (rol, saludo, entradas) => {
    const persona = USUARIOS[rol];
    let enviado;
    servidor.use(http.post('/api/v1/sesion', async ({ request }) => {
      enviado = await request.json();
      return HttpResponse.json(respuestaDeSesion(persona));
    }));
    const { usuario } = abrirApp('/iniciar-sesion');

    await entrar(usuario, persona.correo);

    expect(await screen.findByRole('heading', { name: saludo })).toBeInTheDocument();
    const menu = screen.getByRole('navigation', { name: 'Menú principal' });
    const enlaces = within(menu).getAllByRole('link').map((enlace) => enlace.textContent);
    expect(enlaces).toEqual([...entradas, 'Cambiar contraseña', 'Acerca de']);
    expect(within(menu).getByRole('button', { name: 'Cerrar sesión' })).toBeInTheDocument();
    expect(enviado).toEqual({ correo: persona.correo, contrasena: 'Clave-Segura-2026' });
    expect(leerSesion()?.usuario).toEqual(persona);
  });

  it('RF-01 · C2 · con datos errados muestra «Correo o contraseña incorrectos» y la regla del bloqueo, sin marcar cuál de los dos falló', async () => {
    servidor.use(http.post('/api/v1/sesion', () => problema(401, 'Correo o contraseña incorrectos', { ayuda: AYUDA })));
    const { usuario } = abrirApp('/iniciar-sesion');

    await entrar(usuario, 'ana.perez@cacaosdelhuila.co', 'no-es-la-clave');

    const aviso = await screen.findByRole('alert');
    expect(aviso).toHaveTextContent('Correo o contraseña incorrectos');
    expect(aviso).toHaveTextContent(AYUDA);
    expect(screen.getByLabelText('Correo')).not.toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByLabelText('Contraseña')).not.toHaveAttribute('aria-invalid', 'true');
    expect(leerSesion()).toBeNull();
  });

  /**
   * Intenta entrar con una cuenta que el backend rechaza por un motivo y revisa que se vea y no entre.
   *
   * @param {string} motivo el mensaje del backend
   */
  async function rechazadoPor(motivo) {
    servidor.use(http.post('/api/v1/sesion', () => problema(401, motivo)));
    const { usuario } = abrirApp('/iniciar-sesion');

    await entrar(usuario, 'luis.gomez@cacaosdelhuila.co');

    expect(await screen.findByRole('alert')).toHaveTextContent(motivo);
    expect(screen.getByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument();
    expect(leerSesion()).toBeNull();
  }

  it('RF-01 · C3 · un usuario desactivado no entra: ve el motivo que da el backend', async () => {
    await rechazadoPor('Tu usuario está desactivado: pide al administrador de tu empresa que lo reactive.');
  });

  it('RF-01 · C4 · un usuario de una empresa desactivada no entra: ve el motivo que da el backend', async () => {
    await rechazadoPor('Tu empresa está desactivada: comunícate con el equipo de InspectIA.');
  });

  it('RF-01 · C8 · una contraseña de más de 64 caracteres y 72 bytes llega completa al backend y recibe la misma respuesta que cualquier fallo', async () => {
    // 68 caracteres y 118 bytes: Iniciar sesión no revisa el largo ni los bytes, solo que no esté vacía.
    const larga = `Clave-Segura-2026-${'ñ'.repeat(50)}`;
    let enviada;
    servidor.use(http.post('/api/v1/sesion', async ({ request }) => {
      enviada = (await request.json()).contrasena;
      return problema(401, 'Correo o contraseña incorrectos', { ayuda: AYUDA });
    }));
    const { usuario } = abrirApp('/iniciar-sesion');

    await entrar(usuario, 'ana.perez@cacaosdelhuila.co', larga);

    const aviso = await screen.findByRole('alert');
    expect(aviso).toHaveTextContent('Correo o contraseña incorrectos');
    expect(aviso).toHaveTextContent(AYUDA);
    expect(enviada).toBe(larga);
    expect(screen.getByLabelText('Contraseña')).not.toHaveAttribute('aria-invalid', 'true');
    expect(leerSesion()).toBeNull();
  });

  it('RNF-06 · C3 · sin conexión con el servidor, Iniciar sesión dice qué pasó y qué hacer', async () => {
    servidor.use(http.post('/api/v1/sesion', () => HttpResponse.error()));
    const { usuario } = abrirApp('/iniciar-sesion');

    await entrar(usuario, 'ana.perez@cacaosdelhuila.co');

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudo conectar con el servidor. Revisa tu conexión a internet e intenta de nuevo.');
    expect(screen.getByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument();
    expect(leerSesion()).toBeNull();
  });

  it('RNF-06 · C3 · si el servidor falla sin dar un mensaje, Iniciar sesión dice qué pasó y qué hacer', async () => {
    servidor.use(http.post('/api/v1/sesion', () => new HttpResponse('<html>Bad Gateway</html>', { status: 502 })));
    const { usuario } = abrirApp('/iniciar-sesion');

    await entrar(usuario, 'ana.perez@cacaosdelhuila.co');

    expect(await screen.findByRole('alert')).toHaveTextContent('Ocurrió un error inesperado. Intenta de nuevo más tarde.');
  });

  it('RF-01 · sin datos no envía nada y marca los dos campos como obligatorios', async () => {
    let enviados = 0;
    servidor.use(http.post('/api/v1/sesion', () => {
      enviados += 1;
      return HttpResponse.json({});
    }));
    const { usuario } = abrirApp('/iniciar-sesion');

    await usuario.click(screen.getByRole('button', { name: 'Iniciar sesión' }));

    expect(await screen.findAllByText('Este dato es obligatorio.')).toHaveLength(2);
    expect(screen.getByLabelText('Correo')).toHaveAttribute('aria-invalid', 'true');
    expect(enviados).toBe(0);
  });

  it('RF-01 · la contraseña se puede mostrar y volver a ocultar', async () => {
    const { usuario } = abrirApp('/iniciar-sesion');
    const campo = screen.getByLabelText('Contraseña');

    await usuario.click(screen.getByRole('button', { name: 'Mostrar la contraseña' }));
    expect(campo).toHaveAttribute('type', 'text');

    await usuario.click(screen.getByRole('button', { name: 'Ocultar la contraseña' }));
    expect(campo).toHaveAttribute('type', 'password');
  });

  it('RF-01 · con una sesión abierta, Iniciar sesión lleva al inicio de su rol', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);

    abrirApp('/iniciar-sesion');

    expect(await screen.findByRole('heading', { name: 'Hola, Ana' })).toBeInTheDocument();
  });

  it('RNF-08 · C1 · sin sesión, desde Iniciar sesión se abre Acerca de con la cita del dataset', async () => {
    const { usuario } = abrirApp('/iniciar-sesion');

    await usuario.click(screen.getByRole('link', { name: 'Acerca de InspectIA' }));

    expect(await screen.findByRole('heading', { name: 'Acerca de InspectIA' })).toBeInTheDocument();
    expect(screen.getByText(/CC BY-NC 4\.0/)).toBeInTheDocument();
  });

  it('RP-11 · Iniciar sesión cambia a inglés, y pide al backend los mensajes en inglés', async () => {
    let idioma;
    servidor.use(http.post('/api/v1/sesion', ({ request }) => {
      idioma = request.headers.get('Accept-Language');
      return problema(401, 'Incorrect email or password',
        { ayuda: 'If you fail 5 times in a row, the account is locked for 15 minutes.' });
    }));
    const { usuario } = abrirApp('/iniciar-sesion');

    await usuario.click(within(screen.getByRole('group', { name: 'Idioma' })).getByRole('button', { name: 'English' }));
    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
    await usuario.type(screen.getByLabelText('Email'), 'ana.perez@cacaosdelhuila.co');
    await usuario.type(screen.getByLabelText('Password'), 'no-es-la-clave');
    await usuario.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Incorrect email or password');
    expect(idioma).toBe('en');
    expect(document.documentElement).toHaveAttribute('lang', 'en');
  });

  it('RP-08 · el tema se cambia a oscuro desde Iniciar sesión y queda guardado al volver a abrir la app', async () => {
    const { usuario } = abrirApp('/iniciar-sesion');
    const tema = () => within(screen.getByRole('group', { name: 'Tema' }));

    await usuario.click(tema().getByRole('button', { name: 'Oscuro' }));
    expect(tema().getByRole('button', { name: 'Oscuro' })).toHaveAttribute('aria-pressed', 'true');

    cleanup();
    abrirApp('/iniciar-sesion');
    expect(tema().getByRole('button', { name: 'Oscuro' })).toHaveAttribute('aria-pressed', 'true');
    expect(tema().getByRole('button', { name: 'Claro' })).toHaveAttribute('aria-pressed', 'false');
  });
});
