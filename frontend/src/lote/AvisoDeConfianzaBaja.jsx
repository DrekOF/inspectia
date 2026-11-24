/*
 * Nombre: AvisoDeConfianzaBaja.jsx
 * Descripción: El aviso de confianza baja del resultado, como en el prototipo (RF-09 C4): si tras 2 repeticiones los
 *              granos dudosos siguieron pasando del máximo, «Confianza baja: confirma con la prueba de corte», con el
 *              máximo de dudosos que usó la muestra. Si una de las 2 muestras del lote lo tiene, el lote también (RF-09
 *              C6, D-25): el máximo es el de la primera muestra con el aviso. En el tono de advertencia de los tokens,
 *              con su ícono, y sin interrumpir (role="status", RNF-06).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import WarningAmber from '@mui/icons-material/WarningAmber';
import Box from '@mui/material/Box';
import { useTranslation } from 'react-i18next';
import { useEsPc } from '../componentes/useEsPc.js';
import { letra, medidas } from '../tema/tokens.js';
import { porcentajeDelAviso, textoDelPorcentaje } from './revision.js';

/**
 * El aviso de confianza baja.
 *
 * @param {object} props las propiedades
 * @param {import('../api/analisis.js').MuestraDelLote[]} props.muestras las muestras del lote, con su aviso y su
 *   máximo de dudosos
 * @param {string} props.region la región del idioma, como es-CO
 * @returns {import('react').ReactElement} el aviso
 */
export function AvisoDeConfianzaBaja({ muestras, region }) {
  const { t } = useTranslation();
  const esPc = useEsPc();
  const porcentaje = porcentajeDelAviso(muestras);
  return (
    <Box role="status" sx={(theme) => {
      const advertencia = theme.vars.palette.cacao.advertencia;
      return {
        display: 'flex', alignItems: 'flex-start', gap: 1.5, p: esPc ? '16px 20px' : 2,
        borderRadius: `${medidas.tarjeta.radio}px`, border: `1px solid ${advertencia.principal}`,
        bgcolor: advertencia.fondo, color: advertencia.texto,
      };
    }}>
      <WarningAmber aria-hidden sx={(theme) => ({
        flexShrink: 0, fontSize: 24, color: theme.vars.palette.cacao.advertencia.principal,
      })} />
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5, fontSize: 15, lineHeight: 1.5 }}>
        <Box component="strong" sx={{ fontWeight: letra.pesos.media }}>{t('resultado.confianzaBaja')}</Box>
        {porcentaje !== null && (
          <span>{t('resultado.confianzaBajaDetalle', { porcentaje: textoDelPorcentaje(porcentaje, region) })}</span>
        )}
      </Box>
    </Box>
  );
}
