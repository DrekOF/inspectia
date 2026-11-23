/*
 * Nombre: GuiaDeCaptura.jsx
 * Descripción: La guía de captura de las fotos de la muestra, como en el prototipo: una foto de ejemplo, dibujada
 *              mientras no haya fotos reales (P-12), y debajo las reglas de D-08, D-23 y RF-05 (RF-05 C4). En el PC
 *              va en la columna izquierda, junto a las fotos, y en el celular, encima de ellas.
 * Fecha de creación: 22/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { useId } from 'react';
import { useTranslation } from 'react-i18next';
import { fotoDeEjemplo, medidas } from '../tema/tokens.js';

/** Las reglas de la guía, en el orden del prototipo. */
const REGLAS = ['guia.granos', 'guia.papel', 'guia.luz', 'guia.arriba', 'guia.formato'];

/** Los granos del dibujo: centro, radios y giro, separados entre sí como pide la guía. */
const GRANOS = [
  [34, 30, 0], [78, 26, 35], [122, 32, -20], [166, 28, 60], [210, 30, 10],
  [46, 72, -45], [92, 70, 15], [140, 74, 80], [186, 70, -30], [228, 76, 45],
  [30, 114, 20], [76, 116, -60], [124, 112, 5], [170, 118, 40], [216, 114, -15],
];

/**
 * La foto de ejemplo: granos enteros y separados sobre papel blanco, vistos desde arriba.
 *
 * @param {{ descripcion: string }} props lo que muestra, para los lectores de pantalla
 * @returns {import('react').ReactElement} el dibujo
 */
function FotoDeEjemplo({ descripcion }) {
  return (
    <Box component="svg" role="img" aria-label={descripcion} viewBox="0 0 256 144"
      sx={{ display: 'block', width: '100%', maxWidth: 360, height: 'auto' }}>
      <rect x="1" y="1" width="254" height="142" rx="8" fill={fotoDeEjemplo.papel} stroke={fotoDeEjemplo.borde} />
      {GRANOS.map(([x, y, giro]) => (
        <g key={`${x}-${y}`} transform={`rotate(${giro} ${x} ${y})`}>
          <ellipse cx={x} cy={y} rx="14" ry="9" fill={fotoDeEjemplo.grano} />
          <ellipse cx={x - 3} cy={y - 3} rx="5" ry="2.5" fill={fotoDeEjemplo.brillo} />
        </g>
      ))}
    </Box>
  );
}

/**
 * La guía de captura, en una región con su título. Ocupa la parte angosta de la fila que comparte con las fotos.
 *
 * @returns {import('react').ReactElement} la guía
 */
export function GuiaDeCaptura() {
  const { t } = useTranslation();
  const idDelTitulo = useId();
  return (
    <Box component="section" aria-labelledby={idDelTitulo} sx={(theme) => ({
      flex: '1 1 300px', minWidth: 0, display: 'flex', flexDirection: 'column', gap: { xs: 1.5, md: '14px' },
      p: { xs: 2, md: '22px' }, borderRadius: `${medidas.tarjeta.radio}px`,
      border: `1px solid ${theme.vars.palette.divider}`, bgcolor: 'background.paper',
    })}>
      <Typography variant="h2" id={idDelTitulo}>{t('guia.titulo')}</Typography>
      <Box component="figure" sx={{ m: 0, display: 'flex', flexDirection: 'column', gap: 1 }}>
        <FotoDeEjemplo descripcion={t('guia.ejemplo')} />
        <Typography component="figcaption" variant="body2" color="text.secondary">{t('guia.ejemploTitulo')}</Typography>
      </Box>
      <Box component="ul" sx={{ m: 0, pl: 2.5, display: 'flex', flexDirection: 'column', gap: 1 }}>
        {REGLAS.map((regla) => <Typography component="li" key={regla}>{t(regla)}</Typography>)}
      </Box>
    </Box>
  );
}
