/*
 * Nombre: ayudas.jsx
 * Descripción: Ayudas de las pruebas de componentes: usuarios de ejemplo de cada rol, respuestas de la
 *              sesión como las del backend y abrir la app en una ruta, con sesión o sin ella.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import App from '../App.jsx';
import { guardarSesion } from '../sesion/almacen.js';
import { servidor } from './servidor.js';

/** Usuarios de ejemplo, uno por rol. Los datos son ficticios, como los del prototipo. */
export const USUARIOS = {
  USUARIO_FINAL: {
    id: '0199e2a0-0000-7000-8000-000000000011',
    nombre: 'Carlos Ramírez',
    correo: 'carlos.ramirez@cacaosdelhuila.co',
    rol: 'USUARIO_FINAL',
    empresa: { id: '0199e2a0-0000-7000-8000-000000000001', nombre: 'Cacaos del Huila' },
  },
  ADMIN_EMPRESA: {
    id: '0199e2a0-0000-7000-8000-000000000012',
    nombre: 'Ana Pérez',
    correo: 'ana.perez@cacaosdelhuila.co',
    rol: 'ADMIN_EMPRESA',
    empresa: { id: '0199e2a0-0000-7000-8000-000000000001', nombre: 'Cacaos del Huila' },
  },
  ADMIN_PLATAFORMA: {
    id: '0199e2a0-0000-7000-8000-000000000013',
    nombre: 'Andrés Gómez',
    correo: 'andres.gomez@inspectia.co',
    rol: 'ADMIN_PLATAFORMA',
    empresa: null,
  },
};

/**
 * Una fecha a unas horas de ahora, como la escribe el backend (ISO 8601).
 *
 * @param {number} horas las horas, negativas para el pasado
 * @returns {string} la fecha
 */
export function enHoras(horas) {
  return new Date(Date.now() + horas * 3_600_000).toISOString();
}

/**
 * La respuesta del inicio de sesión (POST /api/v1/sesion) o del cambio de contraseña.
 *
 * @param {object} usuario el usuario, de USUARIOS
 * @param {{ token?: string, vence?: string }} [opciones] el token y cuándo vence; por defecto, en 8 horas
 * @returns {object} la respuesta
 */
export function respuestaDeSesion(usuario, { token = 'token-de-prueba', vence = enHoras(8) } = {}) {
  return { token, tipo: 'Bearer', vence, usuario };
}

/**
 * Deja guardada una sesión, como si el usuario hubiera entrado antes, y hace que el backend la reconozca
 * en GET /api/v1/sesion.
 *
 * @param {object} usuario el usuario, de USUARIOS
 * @param {{ token?: string, vence?: string }} [opciones] el token y cuándo vence
 * @returns {object} la sesión guardada
 */
export function conSesion(usuario, opciones = {}) {
  const sesion = respuestaDeSesion(usuario, opciones);
  guardarSesion({ token: sesion.token, vence: sesion.vence, usuario });
  servidor.use(http.get('/api/v1/sesion', () => HttpResponse.json({ vence: sesion.vence, usuario })));
  return sesion;
}

/**
 * Abre la app en una ruta, como si la persona la escribiera en el navegador.
 *
 * @param {string} [ruta] la ruta, como /cambiar-contrasena
 * @returns {{ usuario: import('@testing-library/user-event').UserEvent }} el usuario para interactuar
 */
export function abrirApp(ruta = '/') {
  window.history.replaceState({}, '', ruta);
  const usuario = userEvent.setup();
  render(<App />);
  return { usuario };
}
