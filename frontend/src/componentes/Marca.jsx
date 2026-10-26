/*
 * Nombre: Marca.jsx
 * Descripción: La marca de InspectIA: el grano de cacao del prototipo y el nombre. Toma el color del texto
 *              que la rodea.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Box from '@mui/material/Box';
import SvgIcon from '@mui/material/SvgIcon';

/** El nombre de la app: es una marca, no se traduce. */
export const NOMBRE_DE_LA_APP = 'InspectIA';

/**
 * El grano de cacao del logo, dibujado con trazos.
 *
 * @param {object} props las propiedades de SvgIcon, como sx
 * @param {import('@mui/material/styles').SxProps} [props.sx] estilos de Material UI, como el tamaño
 * @returns {import('react').ReactElement} el ícono
 */
export function IconoDeGrano({ sx, ...props }) {
  return (
    <SvgIcon {...props} viewBox="0 0 24 24" aria-hidden
      sx={[{ fill: 'none', stroke: 'currentColor', strokeWidth: 1.8, strokeLinecap: 'round', strokeLinejoin: 'round' },
        ...(Array.isArray(sx) ? sx : [sx])]}>
      <ellipse cx="12" cy="12" rx="6.5" ry="9.5" transform="rotate(30 12 12)" />
      <path d="M8.6 17.4c1.7-1.6 2.4-3.6 2.6-5.6.2-2 .9-3.9 2.9-5.4" />
    </SvgIcon>
  );
}

/**
 * La marca: el grano y el nombre.
 *
 * @param {object} props las propiedades
 * @param {number} [props.tamano] el tamaño de la letra en píxeles; el grano es un poco más grande
 * @param {import('@mui/material/styles').SxProps} [props.sx] estilos de Material UI, como el color
 * @returns {import('react').ReactElement} la marca
 */
export function Marca({ tamano = 20, sx }) {
  return (
    <Box sx={[{ display: 'flex', alignItems: 'center', gap: 1.25, fontSize: tamano, fontWeight: 700 },
      ...(Array.isArray(sx) ? sx : [sx])]}>
      <IconoDeGrano sx={{ fontSize: Math.round(tamano * 1.4) }} />
      <span>{NOMBRE_DE_LA_APP}</span>
    </Box>
  );
}
