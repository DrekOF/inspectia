/*
 * Nombre: TituloDePagina.jsx
 * Descripción: El título de una pantalla (h1) con su descripción debajo, como en el prototipo. También pone
 *              el título de la pestaña del navegador.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { useTitulo } from './useTitulo.js';

/**
 * El título de una pantalla.
 *
 * @param {object} props las propiedades
 * @param {string} props.titulo el título
 * @param {string} [props.descripcion] la descripción, debajo del título
 * @param {string} [props.tituloDePestana] el título de la pestaña, si no es el mismo de la pantalla
 * @returns {import('react').ReactElement} el título
 */
export function TituloDePagina({ titulo, descripcion, tituloDePestana }) {
  useTitulo(tituloDePestana ?? titulo);
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.75, mb: 3 }}>
      <Typography variant="h1">{titulo}</Typography>
      {descripcion && <Typography color="text.secondary">{descripcion}</Typography>}
    </Box>
  );
}
