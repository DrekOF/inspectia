/*
 * Nombre: ResumenDelResultado.jsx
 * Descripción: El resumen del resultado de un lote, como en el prototipo: la cifra grande del porcentaje corregido de
 *              granos sanos, de 64 px en el PC y 52 en el celular, con su rango probable debajo (D-43); los granos que
 *              contó el modelo, con «87 de 100 marcados como sanos» y la frase que explica la corrección (RF-11 C6,
 *              D-39), y la categoría probable con la recomendación guardada y la nota de la prueba de corte (RF-12
 *              C10). Todas las cifras son las guardadas con el lote: la página solo les da formato (RF-11 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Check from '@mui/icons-material/Check';
import Close from '@mui/icons-material/Close';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { useTranslation } from 'react-i18next';
import { useEsPc } from '../componentes/useEsPc.js';
import { textoDelDecimal } from '../crud/decimales.js';
import { letra, medidas } from '../tema/tokens.js';
import { textosDelRango } from './resultado.js';

/**
 * Una casilla de los conteos, como en el prototipo: el nombre arriba y el número debajo, en los colores de su tono.
 *
 * @param {object} props las propiedades
 * @param {string} props.nombre el nombre, como «Sanos»
 * @param {number} props.valor el número
 * @param {'exito'|'error'} [props.tono] el tono de los tokens; sin él, la casilla va sobre el fondo de la página
 * @param {import('react').ReactElement} [props.icono] el ícono que acompaña al color
 * @returns {import('react').ReactElement} la casilla
 */
function Conteo({ nombre, valor, tono, icono }) {
  const esPc = useEsPc();
  return (
    <Box sx={(theme) => {
      const colores = tono ? theme.vars.palette.cacao[tono] : null;
      return {
        p: esPc ? '12px' : '10px', borderRadius: `${medidas.boton.radio}px`, minWidth: 0,
        bgcolor: colores ? colores.fondo : 'background.default', color: colores ? colores.texto : 'text.primary',
      };
    }}>
      <Box component="dt" sx={{
        display: 'flex', alignItems: 'center', gap: 0.5, fontSize: letra.chip,
        color: tono ? 'inherit' : 'text.secondary', '& svg': { fontSize: esPc ? 14 : 13 },
      }}>
        {icono}
        {nombre}
      </Box>
      <Box component="dd" sx={{ m: 0, mt: 0.5, fontSize: esPc ? 22 : 20, fontWeight: letra.pesos.media }}>{valor}</Box>
    </Box>
  );
}

/**
 * El resumen del resultado de un lote analizado.
 *
 * @param {object} props las propiedades
 * @param {import('../api/analisis.js').LoteConResultado} props.lote el lote, con su resultado guardado
 * @param {string} props.region la región del idioma, como es-CO
 * @returns {import('react').ReactElement} el resumen
 */
export function ResumenDelResultado({ lote, region }) {
  const { t } = useTranslation();
  const esPc = useEsPc();
  const rango = textosDelRango(lote.porcentajeSanos, lote.margen, region);
  return (
    <Box component="section" aria-label={t('resultado.resumen')} sx={(theme) => ({
      display: 'flex', flexDirection: esPc ? 'row' : 'column', flexWrap: 'wrap', gap: esPc ? '28px 40px' : 2,
      p: esPc ? '28px' : '20px', borderRadius: `${medidas.tarjeta.radio}px`, minWidth: 0,
      border: `1px solid ${theme.vars.palette.divider}`, bgcolor: 'background.paper',
    })}>
      <Box sx={{ flex: esPc ? '1 1 280px' : 'none', minWidth: 0, display: 'flex', flexDirection: 'column', gap: esPc ? '14px' : 2 }}>
        <Box component="p" sx={{ m: 0, display: 'flex', flexDirection: 'column', gap: '2px' }}>
          <Box component="span" sx={{
            fontSize: esPc ? letra.cifra.pc : letra.cifra.celular, lineHeight: 1, fontWeight: letra.pesos.negrita,
            letterSpacing: '-0.01em',
          }}>
            {`${textoDelDecimal(lote.porcentajeSanos, 1, region)}\u00a0%`}
          </Box>
          <Box component="span" sx={{ fontSize: esPc ? 17 : 16, color: 'text.secondary' }}>
            {t('resultado.deGranosSanos')}
          </Box>
        </Box>
        <Typography sx={{ fontSize: 15 }}>{t('resultado.rango', rango)}</Typography>
        <Box component="dl" sx={{
          m: 0, display: 'grid', gridTemplateColumns: 'repeat(3, minmax(0, 1fr))', gap: esPc ? '12px' : 1,
        }}>
          <Conteo nombre={t('resultado.granos')} valor={lote.totalGranos} />
          <Conteo nombre={t('resultado.sanos')} valor={lote.granosSanos} tono="exito" icono={<Check aria-hidden />} />
          <Conteo nombre={t('resultado.noSanos')} valor={lote.totalGranos - lote.granosSanos} tono="error"
            icono={<Close aria-hidden />} />
        </Box>
        <Box>
          <Typography variant="body2">
            {t('resultado.marcados', { sanos: lote.granosSanos, total: lote.totalGranos })}
          </Typography>
          <Typography variant="body2" color="text.secondary">{t('resultado.corrige')}</Typography>
        </Box>
      </Box>
      {!esPc && <Box aria-hidden sx={{ height: '1px', bgcolor: 'divider' }} />}
      <Box sx={{ flex: esPc ? '1 1 360px' : 'none', minWidth: 0, display: 'flex', flexDirection: 'column', gap: esPc ? '12px' : '10px' }}>
        <Typography component="p" sx={{ fontSize: letra.ayuda, fontWeight: letra.pesos.media, color: 'text.secondary' }}>
          {t('lote.categoria.etiqueta')}
        </Typography>
        <Box component="p" sx={(theme) => ({
          m: 0, alignSelf: 'flex-start', px: esPc ? 2 : '14px', py: esPc ? 1 : '6px', borderRadius: '24px',
          bgcolor: theme.vars.palette.cacao.seleccion.fondo, color: theme.vars.palette.cacao.seleccion.texto,
          fontSize: esPc ? 22 : 20, fontWeight: letra.pesos.negrita, lineHeight: 1.3,
        })}>
          {t(`rango_recomendacion.categoria.${lote.categoria}`)}
        </Box>
        <Typography sx={{ fontSize: esPc ? 17 : 16, lineHeight: 1.55 }}>{lote.recomendacion}</Typography>
        <Typography variant="body2" color="text.secondary">{t('resultado.nota')}</Typography>
      </Box>
    </Box>
  );
}
