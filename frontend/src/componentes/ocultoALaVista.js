/*
 * Nombre: ocultoALaVista.js
 * Descripción: El estilo de lo que se oculta a la vista pero no a los lectores de pantalla, como el título de la
 *              tabla del historial o el «(listo)» de un paso del análisis, como en el prototipo (RNF-06).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */

/** Oculto a la vista, pero no para los lectores de pantalla. */
export const OCULTO_A_LA_VISTA = {
  border: 0, clip: 'rect(0 0 0 0)', height: '1px', margin: '-1px', overflow: 'hidden', padding: 0,
  position: 'absolute', whiteSpace: 'nowrap', width: '1px',
};
