/*
 * Nombre: empresas.js
 * Descripción: Datos y pasos que comparten las pruebas de la pantalla de empresas (RF-02): empresas como las
 *              responde la API, datos válidos para registrar una y cómo llenar su formulario. Los datos son
 *              ficticios, como los del prototipo.
 * Fecha de creación: 26/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { screen, within } from '@testing-library/react';

/** La ruta de la pantalla y la de la API de empresas. */
export const RUTA_EMPRESAS = '/empresas';
export const API_EMPRESAS = '/api/v1/empresas';

/** Lo que necesita saber la API simulada de empresas. */
export const OPCIONES_EMPRESAS = { atributoActivo: 'activa' };

/** Una empresa activa, como la responde la API. */
export const EMPRESA = {
  id: '0199e2a0-0000-7000-8000-0000000000b1',
  nombre: 'Cacaos del Huila',
  nit: '900123456-8',
  ciudad: 'Neiva',
  activa: true,
};

/** Una empresa desactivada. */
export const EMPRESA_DESACTIVADA = {
  id: '0199e2a0-0000-7000-8000-0000000000b2',
  nombre: 'Comercializadora La Mazorca',
  nit: '800215478-2',
  ciudad: 'Chaparral',
  activa: false,
};

/** Datos válidos para registrar una empresa con su primer administrador. */
export const DATOS_DE_EMPRESA = {
  nombre: 'Cacao Andino',
  nit: '901245780-7',
  ciudad: 'Bucaramanga',
  administrador: { nombre: 'Laura Gómez', correo: 'laura.gomez@cacaoandino.co', contrasena: 'Clave-Segura-2026' },
};

/**
 * Abre el formulario «Nueva empresa» y escribe los datos que se le den; los que falten quedan vacíos.
 *
 * @param {import('@testing-library/user-event').UserEvent} usuario el usuario de la prueba
 * @param {object} [datos] los datos, con los nombres de la API; por defecto, DATOS_DE_EMPRESA
 */
export async function llenarEmpresa(usuario, datos = DATOS_DE_EMPRESA) {
  await usuario.click(await screen.findByRole('button', { name: 'Nueva empresa' }));
  const empresa = within(screen.getByRole('group', { name: 'Empresa' }));
  const administrador = within(screen.getByRole('group', { name: 'Primer administrador' }));
  for (const [campo, etiqueta] of [['nombre', 'Nombre'], ['nit', 'NIT'], ['ciudad', 'Ciudad']]) {
    if (datos[campo]) {
      await usuario.type(empresa.getByLabelText(etiqueta), datos[campo]);
    }
  }
  for (const [campo, etiqueta] of [['nombre', 'Nombre'], ['correo', 'Correo'], ['contrasena', 'Contraseña inicial']]) {
    if (datos.administrador?.[campo]) {
      await usuario.type(administrador.getByLabelText(etiqueta), datos.administrador[campo]);
    }
  }
}
