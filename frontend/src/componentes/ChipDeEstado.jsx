/*
 * Nombre: ChipDeEstado.jsx
 * Descripción: El estado de algo en un chip, como en el prototipo: el texto con un ícono, en los colores de un tono de
 *              los tokens (RP-08). El ícono acompaña al color, así el color no es la única señal (RNF-06). Lo usan el
 *              estado de un lote en el historial y el de una foto de la muestra.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Chip from '@mui/material/Chip';
import { letra, medidas } from '../tema/tokens.js';

/**
 * Un chip de estado.
 *
 * @param {object} props las propiedades
 * @param {string} props.texto el estado, como «Pendiente de análisis»
 * @param {'exito'|'advertencia'|'error'|'informacion'} props.tono el tono de los tokens
 * @param {import('react').ReactElement} props.icono el ícono, que va antes del texto
 * @returns {import('react').ReactElement} el chip
 */
export function ChipDeEstado({ texto, tono, icono }) {
  return (
    <Chip size="small" label={texto} icon={icono} sx={(theme) => {
      const colores = theme.vars.palette.cacao[tono];
      return {
        height: 'auto', gap: '6px', px: '10px', py: '4px', borderRadius: `${medidas.chip.radio}px`,
        bgcolor: colores.fondo, color: colores.texto, fontSize: letra.chip, fontWeight: letra.pesos.media,
        '& .MuiChip-icon': { m: 0, fontSize: 14, color: 'inherit' },
        '& .MuiChip-label': { p: 0 },
      };
    }} />
  );
}
