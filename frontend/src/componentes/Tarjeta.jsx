/*
 * Nombre: Tarjeta.jsx
 * Descripción: Una tarjeta del prototipo: superficie con borde de 1 px, radio de 12 px y 24 px de relleno,
 *              con su título (h2) como nombre de la sección.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { useId } from 'react';
import { medidas } from '../tema/tokens.js';

/**
 * Una tarjeta con título.
 *
 * @param {object} props las propiedades
 * @param {string} props.titulo el título de la tarjeta
 * @param {import('react').ReactNode} props.children el contenido
 * @returns {import('react').ReactElement} la tarjeta
 */
export function Tarjeta({ titulo, children }) {
  const id = useId();
  return (
    <Box component="section" aria-labelledby={id} sx={(theme) => ({
      display: 'flex',
      flexDirection: 'column',
      gap: 1.5,
      p: { xs: 2.5, md: `${medidas.tarjeta.relleno}px 28px` },
      borderRadius: `${medidas.tarjeta.radio}px`,
      border: `1px solid ${theme.vars.palette.divider}`,
      bgcolor: 'background.paper',
    })}>
      <Typography id={id} variant="h2">{titulo}</Typography>
      {children}
    </Box>
  );
}
