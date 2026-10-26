/*
 * Nombre: useDevolverElFoco.js
 * Descripción: Recuerda el botón que abre un panel en la misma página, como «Editar» en una fila, para devolverle
 *              el foco al cerrarlo: quien usa el teclado sigue donde estaba (RNF-06 C4).
 * Fecha de creación: 26/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { useCallback, useRef } from 'react';

/**
 * Recuerda el botón que abre un panel y le devuelve el foco al cerrarlo.
 *
 * @returns {{ recordar: (boton: HTMLElement) => void, devolver: () => void }} recordar el botón al abrir el panel y
 *   devolverle el foco al cerrarlo
 */
export function useDevolverElFoco() {
  const boton = useRef(null);
  const recordar = useCallback((elemento) => {
    boton.current = elemento;
  }, []);
  const devolver = useCallback(() => {
    boton.current?.focus();
    boton.current = null;
  }, []);
  return { recordar, devolver };
}
