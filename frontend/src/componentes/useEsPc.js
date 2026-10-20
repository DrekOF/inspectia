/*
 * Nombre: useEsPc.js
 * Descripción: Si la pantalla tiene el ancho de un PC (desde 900 px) o de un celular, para elegir el diseño
 *              de cada uno (RNF-05).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { useTheme } from '@mui/material/styles';
import useMediaQuery from '@mui/material/useMediaQuery';

/**
 * Si la pantalla tiene el ancho de un PC: desde 900 px el menú queda a la vista.
 *
 * @returns {boolean} si es de PC
 */
export function useEsPc() {
  const tema = useTheme();
  return useMediaQuery(tema.breakpoints.up('md'), { noSsr: true });
}
