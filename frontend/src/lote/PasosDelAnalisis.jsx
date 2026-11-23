/*
 * Nombre: PasosDelAnalisis.jsx
 * Descripción: Los tres pasos del análisis de un lote, como en el prototipo: los datos del lote, las fotos de la
 *              muestra y el resultado. En el PC van en fila: los listos en verde, con «(listo)» para los lectores
 *              de pantalla, y el paso en curso marcado. En el celular, «Paso N de 3» con una barra (RF-04, RF-05,
 *              RNF-05, RNF-06).
 * Fecha de creación: 22/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import Check from '@mui/icons-material/Check';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { useTranslation } from 'react-i18next';
import { OCULTO_A_LA_VISTA } from '../componentes/ocultoALaVista.js';
import { useEsPc } from '../componentes/useEsPc.js';
import { letra } from '../tema/tokens.js';

/** @typedef {import('@mui/material/styles').SxProps<import('@mui/material/styles').Theme>} Estilo */

/** Los pasos, en orden. */
const PASOS = ['pasos.datos', 'pasos.fotos', 'pasos.resultado'];

/**
 * En qué va un paso: listo, en curso o por hacer.
 *
 * @param {number} numero el número del paso, desde 1
 * @param {number} actual el número del paso en curso
 * @returns {'listo'|'enCurso'|'porHacer'} su estado
 */
function estadoDelPaso(numero, actual) {
  if (numero < actual) {
    return 'listo';
  }
  return numero === actual ? 'enCurso' : 'porHacer';
}

/**
 * El estilo de un paso en el PC, como en el prototipo: el número en un círculo y el nombre del paso. El listo va
 * en verde y con un chulo; el que va en curso, en el color primario; el que falta, con un borde.
 *
 * @param {'listo'|'enCurso'|'porHacer'} estado el estado del paso
 * @returns {{ paso: Estilo, circulo: Estilo }} el estilo del paso y el de su círculo
 */
function estiloDelPaso(estado) {
  const circulo = { width: 32, height: 32, borderRadius: '50%', display: 'flex', alignItems: 'center',
    justifyContent: 'center', flexShrink: 0 };
  if (estado === 'listo') {
    return {
      paso: (theme) => ({ color: theme.vars.palette.cacao.exito.texto }),
      circulo: (theme) => ({ ...circulo, bgcolor: theme.vars.palette.cacao.exito.fondo }),
    };
  }
  if (estado === 'enCurso') {
    return {
      paso: { color: 'primary.main', fontWeight: letra.pesos.media },
      circulo: { ...circulo, bgcolor: 'primary.main', color: 'primary.contrastText', fontWeight: letra.pesos.negrita },
    };
  }
  return {
    paso: { color: 'text.secondary' },
    circulo: (theme) => ({ ...circulo, width: 30, height: 30, fontWeight: letra.pesos.media,
      border: `1px solid ${theme.vars.palette.cacao.bordeCampo}` }),
  };
}

/**
 * El color de un paso en la barra del celular: verde si está listo, el primario si va en curso y el de los bordes
 * si falta, como en el prototipo.
 *
 * @param {import('@mui/material/styles').Theme} theme el tema
 * @param {'listo'|'enCurso'|'porHacer'} estado el estado del paso
 * @returns {string} el color
 */
function colorEnLaBarra(theme, estado) {
  if (estado === 'listo') {
    return theme.vars.palette.cacao.exito.principal;
  }
  return estado === 'enCurso' ? theme.vars.palette.primary.main : theme.vars.palette.divider;
}

/**
 * Los pasos en el PC: en fila, unidos por una línea, verde si sale de un paso listo.
 *
 * @param {{ actual: number }} props el número del paso en curso
 * @returns {import('react').ReactElement} los pasos
 */
function PasosEnFila({ actual }) {
  const { t } = useTranslation();
  return (
    <Box component="ol" role="list" aria-label={t('pasos.titulo')} sx={{
      listStyle: 'none', m: 0, mb: 3, p: 0, display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '12px 20px',
    }}>
      {PASOS.map((clave, indice) => {
        const estado = estadoDelPaso(indice + 1, actual);
        const estilo = estiloDelPaso(estado);
        const anteriorListo = indice > 0 && estadoDelPaso(indice, actual) === 'listo';
        return (
          <Box component="li" key={clave} aria-current={estado === 'enCurso' ? 'step' : undefined} sx={[{
            display: 'flex', alignItems: 'center', gap: '10px', fontSize: letra.texto,
          }, estilo.paso]}>
            {indice > 0 && (
              <Box aria-hidden sx={(theme) => ({
                width: 40, height: 2, mr: '10px',
                bgcolor: anteriorListo ? theme.vars.palette.cacao.exito.principal : theme.vars.palette.divider,
              })} />
            )}
            <Box aria-hidden sx={estilo.circulo}>{estado === 'listo' ? <Check sx={{ fontSize: 18 }} /> : indice + 1}</Box>
            {t(clave)}
            {estado === 'listo' && <Box component="span" sx={OCULTO_A_LA_VISTA}>{t('pasos.listo')}</Box>}
          </Box>
        );
      })}
    </Box>
  );
}

/**
 * Los pasos en el celular: «Paso N de 3» con el nombre del paso en curso, y una barra con un tramo por paso.
 *
 * @param {{ actual: number }} props el número del paso en curso
 * @returns {import('react').ReactElement} los pasos
 */
function PasosEnBarra({ actual }) {
  const { t } = useTranslation();
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1, mb: 3 }}>
      <Typography variant="body2" color="text.secondary">
        {t('pasos.deTotal', { numero: actual, total: PASOS.length })}
        {' · '}
        <Box component="strong" sx={{ fontWeight: letra.pesos.media, color: 'primary.main' }}>
          {t(PASOS[actual - 1])}
        </Box>
      </Typography>
      <Box aria-hidden sx={{ display: 'grid', gridTemplateColumns: `repeat(${PASOS.length}, minmax(0, 1fr))`, gap: '6px' }}>
        {PASOS.map((clave, indice) => (
          <Box key={clave} sx={(theme) => ({
            height: 6, borderRadius: '3px', bgcolor: colorEnLaBarra(theme, estadoDelPaso(indice + 1, actual)),
          })} />
        ))}
      </Box>
    </Box>
  );
}

/**
 * Los pasos del análisis.
 *
 * @param {{ actual: 1|2|3 }} props el número del paso en curso
 * @returns {import('react').ReactElement} los pasos
 */
export function PasosDelAnalisis({ actual }) {
  return useEsPc() ? <PasosEnFila actual={actual} /> : <PasosEnBarra actual={actual} />;
}
