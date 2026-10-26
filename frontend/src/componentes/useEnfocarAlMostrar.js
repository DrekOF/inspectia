/*
 * Nombre: useEnfocarAlMostrar.js
 * Descripción: Lleva el foco al título de un panel que se abre en la misma página, como el formulario de una
 *              pantalla de registros: el título queda a la vista aunque el botón que lo abrió esté más abajo, y
 *              los lectores de pantalla lo leen (RNF-06 C4).
 * Fecha de creación: 26/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { useEffect, useRef } from 'react';

/**
 * Enfoca un elemento al mostrarse. El elemento necesita tabIndex={-1}, porque un título no recibe el foco por sí
 * solo.
 *
 * @returns {import('react').RefObject<HTMLElement|null>} la referencia para el elemento
 */
export function useEnfocarAlMostrar() {
  const elemento = useRef(null);
  useEffect(() => {
    elemento.current?.focus();
  }, []);
  return elemento;
}
