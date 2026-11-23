/*
 * Nombre: PasosDelAnalisis.jsx
 * Descripción: Los tres pasos del análisis de un lote, como en el prototipo: los datos del lote, las fotos de la
 *              muestra y el resultado. El paso en curso va marcado para los lectores de pantalla y los anteriores
 *              dicen «(listo)» (RF-04, RF-05, RNF-06).
 * Fecha de creación: 22/11/2026
 * Actualización: 22/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Check from '@mui/icons-material/Check';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { useTranslation } from 'react-i18next';

/** Los pasos, en orden. */
const PASOS = ['pasos.datos', 'pasos.fotos', 'pasos.resultado'];

/**
 * Los pasos del análisis.
 *
 * @param {{ actual: 1|2|3 }} props el número del paso en curso
 * @returns {import('react').ReactElement} los pasos
 */
export function PasosDelAnalisis({ actual }) {
  const { t } = useTranslation();
  return (
    <Box component="ol" role="list" aria-label={t('pasos.titulo')}
      sx={{ listStyle: 'none', m: 0, mb: 3, p: 0, display: 'flex', flexWrap: 'wrap', gap: { xs: 1.5, md: 3 } }}>
      {PASOS.map((clave, indice) => {
        const numero = indice + 1;
        const listo = numero < actual;
        const enCurso = numero === actual;
        return (
          <Box component="li" key={clave} aria-current={enCurso ? 'step' : undefined}
            sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Box aria-hidden sx={(theme) => ({
              width: 28, height: 28, borderRadius: '50%', display: 'flex', alignItems: 'center',
              justifyContent: 'center', fontSize: 14, fontWeight: 500, flexShrink: 0,
              ...(listo || enCurso
                ? { bgcolor: 'primary.main', color: 'primary.contrastText' }
                : { border: `1px solid ${theme.vars.palette.divider}`, color: 'text.secondary' }),
            })}>
              {listo ? <Check sx={{ fontSize: 18 }} /> : numero}
            </Box>
            <Typography variant="body2" sx={{ fontWeight: enCurso ? 500 : 400 }}
              color={enCurso || listo ? 'text.primary' : 'text.secondary'}>
              {t(clave)}
            </Typography>
            {listo && <Typography variant="body2" color="text.secondary">{t('pasos.listo')}</Typography>}
          </Box>
        );
      })}
    </Box>
  );
}
