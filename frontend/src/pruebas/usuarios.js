/*
 * Nombre: usuarios.js
 * Descripción: Datos y pasos que comparten las pruebas de la pantalla de usuarios (RF-03): usuarios como los
 *              responde la API, entre ellos el de la sesión; las respuestas de la API cuando una regla no deja
 *              guardar; abrir la pantalla con la API simulada y llenar el formulario. Los datos son ficticios, como
 *              los del prototipo.
 * Fecha de creación: 27/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { screen } from '@testing-library/react';
import { abrirApp, conSesion, USUARIOS } from './ayudas.jsx';
import { apiSimulada } from './crud.js';
import { problema } from './servidor.js';

/** La ruta de la pantalla y la de la API de usuarios. */
export const RUTA_USUARIOS = '/usuarios';
export const API_USUARIOS = '/api/v1/usuarios';

/** Lo que necesita saber la API simulada de usuarios. */
export const OPCIONES_USUARIOS = { atributoActivo: 'activo' };

/** El administrador de la sesión (USUARIOS.ADMIN_EMPRESA), como lo responde la API: su fila es la propia. */
export const YO = {
  id: USUARIOS.ADMIN_EMPRESA.id,
  nombre: 'Ana Pérez',
  correo: 'ana.perez@cacaosdelhuila.co',
  rol: 'ADMIN_EMPRESA',
  activo: true,
};

/** Un usuario final activo de la misma empresa. */
export const CARLOS = {
  id: USUARIOS.USUARIO_FINAL.id,
  nombre: 'Carlos Ramírez',
  correo: 'carlos.ramirez@cacaosdelhuila.co',
  rol: 'USUARIO_FINAL',
  activo: true,
};

/** Un usuario desactivado. */
export const LUISA = {
  id: '0199e2a0-0000-7000-8000-0000000000c3',
  nombre: 'Luisa Cárdenas',
  correo: 'luisa.cardenas@cacaosdelhuila.co',
  rol: 'USUARIO_FINAL',
  activo: false,
};

/** Datos válidos para crear un usuario, como el del formulario del prototipo. */
export const DATOS_DE_USUARIO = {
  nombre: 'Pedro Salas',
  correo: 'pedro.salas@cacaosdelhuila.co',
  contrasena: 'Clave-Segura-2026',
  rol: 'USUARIO_FINAL',
};

/** Los mensajes de la API, en español, cuando una regla de usuario no deja guardar. */
export const MENSAJES = {
  maximo: 'La empresa ya tiene 25 usuarios activos: desactiva uno antes de crear o reactivar otro.',
  desactivarse: 'No puedes desactivar tu propio usuario.',
  rolPropio: 'No puedes cambiar tu propio rol.',
  contrasenaPropia: 'Para cambiar tu propia contraseña, usa «Cambiar contraseña».',
  rolNoPermitido: 'Solo puedes asignar los roles Usuario final o Administrador de empresa.',
  correoRepetido: 'Ya existe un usuario con este correo.',
};

/**
 * Los usuarios activos de una empresa, contando la fila propia; los demás van numerados, como «Usuario 01». Con 24,
 * la empresa todavía puede crear o reactivar uno; con 25, no (RF-03 C5).
 *
 * @param {number} cantidad cuántos activos, con la fila propia
 * @returns {object[]} los usuarios, como los responde la API
 */
export function usuariosActivos(cantidad) {
  return [YO, ...Array.from({ length: cantidad - 1 }, (_, i) => {
    const numero = String(i + 1).padStart(2, '0');
    return {
      id: `0199e2a0-0000-7000-8000-${String(200 + i).padStart(12, '0')}`,
      nombre: `Usuario ${numero}`,
      correo: `usuario${numero}@cacaosdelhuila.co`,
      rol: 'USUARIO_FINAL',
      activo: true,
    };
  })];
}

/**
 * La respuesta de la API cuando una regla no deja guardar: 400 «Revisa los datos marcados.», con el mensaje de la
 * regla en su campo, o sin campo si la regla no es de un dato, como el máximo de 25 activos.
 *
 * @param {string} mensaje el mensaje de la regla
 * @param {string|null} [campo] el campo de la regla, o null si no tiene
 * @returns {Response} la respuesta
 */
export function reglaIncumplida(mensaje, campo = null) {
  return problema(400, 'Revisa los datos marcados.', { errores: [{ campo, mensaje }] });
}

/**
 * Abre la pantalla de usuarios como el administrador de la empresa, con la API simulada.
 *
 * @param {object[]} [registros] los usuarios que tiene la API
 * @returns {{ usuario: import('@testing-library/user-event').UserEvent, simulada: import('./crud.js').ApiSimulada }}
 *   el usuario de la prueba y la API simulada
 */
export function abrirUsuarios(registros = [YO, CARLOS, LUISA]) {
  conSesion(USUARIOS.ADMIN_EMPRESA);
  const simulada = apiSimulada(API_USUARIOS, registros, OPCIONES_USUARIOS);
  const { usuario } = abrirApp(RUTA_USUARIOS);
  return { usuario, simulada };
}

/**
 * Abre el formulario «Nuevo usuario» y escribe los datos que se le den; los que falten quedan vacíos, y el rol solo
 * se elige si no es el inicial, Usuario final.
 *
 * @param {import('@testing-library/user-event').UserEvent} usuario el usuario de la prueba
 * @param {object} [datos] los datos, con los nombres de la API; por defecto, DATOS_DE_USUARIO
 */
export async function llenarUsuario(usuario, datos = DATOS_DE_USUARIO) {
  await usuario.click(await screen.findByRole('button', { name: 'Nuevo usuario' }));
  for (const [campo, etiqueta] of [['nombre', 'Nombre'], ['correo', 'Correo'], ['contrasena', 'Contraseña inicial']]) {
    if (datos[campo]) {
      await usuario.type(screen.getByLabelText(etiqueta), datos[campo]);
    }
  }
  if (datos.rol && datos.rol !== 'USUARIO_FINAL') {
    await usuario.selectOptions(screen.getByLabelText('Rol'), datos.rol);
  }
}
