/*
 * Nombre: extensiones.js
 * Descripción: Lo escrito a mano para cada pantalla generada, por entidad: lo que su JSON no puede declarar,
 *              como el panel de administradores de una empresa (RF-02 C12). Rutas.jsx se lo pasa a la pantalla;
 *              así el código generado no importa código escrito a mano (RP-23).
 * Fecha de creación: 26/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { PanelAdministradores } from './empresa/PanelAdministradores.jsx';
import { ROLES } from './navegacion/menu.js';

/**
 * Las extensiones de cada pantalla generada, por entidad.
 *
 * @type {Record<string, import('./crud/PantallaDeRegistros.jsx').Extensiones>}
 */
export const EXTENSIONES = {
  empresa: {
    accionesDeFila: [{
      clave: 'administradores',
      texto: 'administradores.accion',
      nombreAccesible: 'administradores.accionDe',
      // Como en la API: solo el administrador de la plataforma restablece contraseñas de administradores.
      roles: [ROLES.ADMIN_PLATAFORMA],
      Panel: PanelAdministradores,
    }],
  },
};
