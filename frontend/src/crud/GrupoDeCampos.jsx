/*
 * Nombre: GrupoDeCampos.jsx
 * Descripción: Un grupo de campos de un formulario generado, con su título, como «Empresa» y «Primer
 *              administrador» al registrar una empresa (RP-23). Es un fieldset: los lectores de pantalla
 *              anuncian el grupo de cada campo (RNF-06).
 * Fecha de creación: 26/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';

/**
 * Un grupo de campos con su título.
 *
 * @param {object} props las propiedades
 * @param {string} props.titulo el título del grupo
 * @param {import('react').ReactNode} props.children los campos
 * @returns {import('react').ReactElement} el grupo
 */
export function GrupoDeCampos({ titulo, children }) {
  return (
    <Box component="fieldset" sx={{ border: 0, m: 0, p: 0, minWidth: 0, display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Typography component="legend" variant="subtitle1" sx={{ p: 0, mb: 0.5, fontWeight: 500 }}>{titulo}</Typography>
      {children}
    </Box>
  );
}
