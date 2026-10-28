/*
 * Nombre: extensiones.js
 * Descripción: Lo escrito a mano para cada pantalla generada, por entidad: lo que su JSON no puede declarar,
 *              como el panel de administradores de una empresa (RF-02 C12) o, en los rangos, el resumen «Así quedan
 *              los rangos» y la regla del orden de sus límites (RF-13). Rutas.jsx se lo pasa a la pantalla; así el
 *              código generado no importa código escrito a mano (RP-23).
 * Fecha de creación: 26/10/2026
 * Actualización: 28/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import { PanelAdministradores } from './empresa/PanelAdministradores.jsx';
import { ROLES } from './navegacion/menu.js';
import { reglaDelOrden } from './rango/limites.js';
import { ResumenDeRangos } from './rango/ResumenDeRangos.jsx';

/**
 * Las extensiones de cada pantalla generada, por entidad: las de una lista con formulario o las de una que se edita
 * en bloque.
 *
 * @type {Record<string, import('./crud/PantallaDeRegistros.jsx').Extensiones
 *   | import('./crud/PantallaEnBloque.jsx').ExtensionesDelBloque>}
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
  rango_recomendacion: {
    Resumen: ResumenDeRangos,
    // La misma regla de ReglasRangoRecomendacionImpl en el backend: los límites bajan estrictamente (RF-13 C1).
    reglaDelBloque: reglaDelOrden,
  },
};
