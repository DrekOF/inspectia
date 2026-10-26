/*
 * Nombre: useTitulo.js
 * Descripción: Pone el título de la pestaña del navegador según la pantalla, como «Cambiar contraseña ·
 *              InspectIA», para que cada página tenga un título que la describa (RNF-06 C4).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { useEffect } from 'react';
import { NOMBRE_DE_LA_APP } from './Marca.jsx';

/**
 * Pone el título de la pestaña.
 *
 * @param {string} titulo el nombre de la pantalla
 */
export function useTitulo(titulo) {
  useEffect(() => {
    document.title = `${titulo} · ${NOMBRE_DE_LA_APP}`;
  }, [titulo]);
}
