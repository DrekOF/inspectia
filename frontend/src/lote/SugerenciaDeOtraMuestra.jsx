/*
 * Nombre: SugerenciaDeOtraMuestra.jsx
 * Descripción: El aviso de la sugerencia de otra muestra en el resultado de un lote, como en el prototipo (D-24,
 *              D-47): una sección «Sugerencia» en el tono de información, que dice a cuántos puntos quedó el resultado
 *              y de qué límite, guardados con el lote, o que quedó justo en él, y que conviene analizar una segunda
 *              muestra. Al usuario final le ofrece «Analizar otra muestra», que lleva a las fotos de la muestra 2, o
 *              «Continuar la muestra 2» si ya la empezó (RF-14 C6, D-25); a la administradora, que no analiza
 *              muestras, le dice quién puede hacerlo (RF-14, restricción 4). En el celular, el botón va debajo, a todo
 *              el ancho (RNF-05).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import InfoOutlined from '@mui/icons-material/InfoOutlined';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { useEsPc } from '../componentes/useEsPc.js';
import { letra, medidas } from '../tema/tokens.js';
import { textosDeLaSugerencia } from './resultado.js';

/**
 * El título del aviso: a cuántos puntos quedó el resultado y de qué límite, o que quedó justo en él.
 *
 * @param {import('../api/analisis.js').LoteConResultado} lote el lote, con su límite guardado
 * @param {(clave: string, valores?: object) => string} t la función de traducción de i18next
 * @param {string} region la región del idioma, como es-CO
 * @returns {string|null} el título, o null si el lote no trae su límite
 */
function tituloDeLaSugerencia(lote, t, region) {
  if (lote.limiteSugerencia === null || lote.limiteSugerencia === undefined) {
    return null;
  }
  const { puntos, limite, enElLimite } = textosDeLaSugerencia(lote.porcentajeSanos, lote.limiteSugerencia, region);
  const categoria = t(`rango_recomendacion.categoria.${lote.categoriaDelLimite}`);
  return enElLimite
    ? t('resultado.sugerenciaEnElLimite', { categoria, limite })
    : t('resultado.sugerencia', { puntos, categoria, limite });
}

/**
 * La sugerencia de otra muestra.
 *
 * @param {object} props las propiedades
 * @param {import('../api/analisis.js').LoteConResultado} props.lote el lote, con su resultado y su límite guardados
 * @param {string} props.region la región del idioma, como es-CO
 * @param {boolean} props.esUsuarioFinal si la sesión es del usuario final, el único que analiza muestras
 * @param {number|null} props.muestraEmpezada el número de la otra muestra, si ya se empezó y no se ha enviado
 * @returns {import('react').ReactElement} el aviso
 */
export function SugerenciaDeOtraMuestra({ lote, region, esUsuarioFinal, muestraEmpezada }) {
  const { t } = useTranslation();
  const esPc = useEsPc();
  const titulo = tituloDeLaSugerencia(lote, t, region);
  return (
    <Box component="section" aria-label={t('resultado.sugerenciaNombre')} sx={(theme) => {
      const informacion = theme.vars.palette.cacao.informacion;
      // En el celular, en columna y sin flexWrap: con «wrap», Safari encoge el botón a su texto al tocarlo y el toque
      // que cae fuera del texto no lo pulsa.
      return {
        display: 'flex', flexDirection: esPc ? 'row' : 'column', flexWrap: esPc ? 'wrap' : 'nowrap',
        alignItems: esPc ? 'center' : 'stretch',
        gap: esPc ? 2 : 1.5, p: esPc ? '18px 20px' : 2, borderRadius: `${medidas.tarjeta.radio}px`,
        border: `1px solid ${informacion.principal}`, bgcolor: informacion.fondo, color: informacion.texto,
      };
    }}>
      <Box sx={{ flex: esPc ? '1 1 420px' : 'none', minWidth: 0, display: 'flex', alignItems: 'flex-start', gap: 1.5 }}>
        <InfoOutlined aria-hidden sx={(theme) => ({
          flexShrink: 0, fontSize: esPc ? 24 : 22, color: theme.vars.palette.cacao.informacion.principal,
        })} />
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5, fontSize: 15, lineHeight: 1.5 }}>
          {titulo && <Box component="strong" sx={{ fontWeight: letra.pesos.media }}>{titulo}</Box>}
          <span>{t(esUsuarioFinal ? 'resultado.sugerenciaDetalle' : 'resultado.sugerenciaDetalleAdmin')}</span>
        </Box>
      </Box>
      {/* Un enlace con forma de botón, como en el prototipo: lleva a otra página. */}
      {esUsuarioFinal && (
        <Button component={Link} to={`/lotes/${lote.id}/fotos`} variant="contained" sx={(theme) => {
          const informacion = theme.vars.palette.cacao.informacion;
          return {
            minHeight: esPc ? medidas.zonaTactil : medidas.boton.alto, px: '18px', fontSize: 15,
            whiteSpace: esPc ? 'nowrap' : 'normal', bgcolor: informacion.principal,
            // La superficie da más de 7:1 sobre el azul de información en los dos temas, como el blanco del prototipo.
            color: theme.vars.palette.background.paper,
            '&:hover': { bgcolor: informacion.texto },
          };
        }}>
          {muestraEmpezada
            ? t('resultado.continuarLaMuestra', { numero: muestraEmpezada })
            : t('resultado.analizarOtraMuestra')}
        </Button>
      )}
    </Box>
  );
}
