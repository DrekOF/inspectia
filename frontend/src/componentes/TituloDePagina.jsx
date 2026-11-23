/*
 * Nombre: TituloDePagina.jsx
 * Descripción: El título de una pantalla (h1) con su descripción debajo, como en el prototipo. También pone
 *              el título de la pestaña del navegador. Con una referencia, el título puede recibir el foco, como cuando
 *              desaparece el botón que lo tenía.
 * Fecha de creación: 20/10/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
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
 * @param {import('react').RefObject<HTMLElement|null>} [props.refDelTitulo] la referencia del título, para llevarle
 *   el foco; con ella, el título lo puede recibir (tabIndex -1)
 * @returns {import('react').ReactElement} el título
 */
export function TituloDePagina({ titulo, descripcion, tituloDePestana, refDelTitulo }) {
  useTitulo(tituloDePestana ?? titulo);
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.75, mb: 3 }}>
      <Typography variant="h1" ref={refDelTitulo} tabIndex={refDelTitulo ? -1 : undefined}
        sx={refDelTitulo ? { outline: 'none' } : undefined}>
        {titulo}
      </Typography>
      {descripcion && <Typography color="text.secondary">{descripcion}</Typography>}
    </Box>
  );
}
