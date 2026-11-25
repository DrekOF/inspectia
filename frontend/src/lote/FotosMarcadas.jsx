/*
 * Nombre: FotosMarcadas.jsx
 * Descripción: La sección «Fotos marcadas» del resultado de un lote, como en el prototipo: de cada muestra terminada,
 *              quién la envió y qué modelo la analizó (RF-14 C3), y una pestaña por foto, «Foto 1 · 50 granos», con la
 *              foto elegida y sus granos marcados (RF-11). Solo se carga la imagen de la pestaña elegida. En el
 *              celular, las pestañas ocupan todo el ancho y su texto va en dos líneas, así caben desde 360 px (RNF-05).
 *              Si el lote tiene dos muestras (D-25), cada una va con su título, «Muestra 1», sus cifras, «87 de 100
 *              marcados como sanos» (RF-14 C3), y los nombres de sus fotos dicen de qué muestra son (RNF-06); también
 *              mientras la segunda se toma o se analiza, así se sabe que las fotos son de la primera.
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import Box from '@mui/material/Box';
import Tab from '@mui/material/Tab';
import Tabs from '@mui/material/Tabs';
import Typography from '@mui/material/Typography';
import { useId, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useEsPc } from '../componentes/useEsPc.js';
import { letra, medidas } from '../tema/tokens.js';
import { FotoMarcada } from './FotoMarcada.jsx';

/**
 * El estilo de las pestañas, como en el prototipo: un riel sobre el fondo de la página y la pestaña elegida como una
 * tarjeta, sin la raya de Material UI.
 *
 * @param {boolean} esPc si se ven en el PC
 * @returns {import('@mui/material/styles').SxProps<import('@mui/material/styles').Theme>} el estilo
 */
function estiloDeLasPestanas(esPc) {
  return (theme) => ({
    minHeight: 0, p: 0.5, borderRadius: '10px', bgcolor: 'background.default',
    alignSelf: esPc ? 'flex-start' : 'stretch', maxWidth: '100%',
    '& .MuiTabs-list': { gap: 0.5 },
    '& .MuiTab-root': {
      minHeight: esPc ? medidas.boton.altoEnTabla : medidas.zonaTactil, minWidth: 0, px: esPc ? 1.75 : 1, py: 0.5,
      borderRadius: `${medidas.boton.radio}px`, fontSize: letra.ayuda, lineHeight: 1.3, color: 'text.secondary',
    },
    '& .MuiTab-root.Mui-selected': {
      bgcolor: 'background.paper', color: 'primary.main', fontWeight: letra.pesos.media,
      boxShadow: `inset 0 0 0 1px ${theme.vars.palette.divider}`,
    },
  });
}

/**
 * Las fotos de una muestra, en pestañas.
 *
 * @param {object} props las propiedades
 * @param {import('../api/analisis.js').MuestraDelLote} props.muestra la muestra terminada
 * @param {boolean} props.conTitulo si va con su título, «Muestra 1», sus cifras y su número en los nombres de las
 *   fotos, porque el lote tiene más de una
 * @param {() => void} props.alFallarLaVista se llama si una imagen no carga
 * @param {() => void} [props.alCargarLaVista] se llama cuando una imagen carga
 * @returns {import('react').ReactElement} las fotos
 */
function FotosDeUnaMuestra({ muestra, conTitulo, alFallarLaVista, alCargarLaVista }) {
  const { t } = useTranslation();
  const esPc = useEsPc();
  const id = useId();
  const [elegida, setElegida] = useState(0);
  const foto = muestra.fotos[elegida] ?? muestra.fotos[0];
  const quien = [
    muestra.analizadaPor && t('resultado.analizadaPor', { nombre: muestra.analizadaPor.nombre }),
    muestra.modelo && t('resultado.modelo', muestra.modelo),
  ].filter(Boolean).join(' · ');

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: esPc ? 2 : 1.5, minWidth: 0 }}>
      {conTitulo && (
        <Box>
          <Typography variant="h3" sx={{ fontSize: 17, fontWeight: letra.pesos.media }}>
            {t('resultado.muestra', { numero: muestra.numero })}
          </Typography>
          {/* Las cifras de esta muestra; las del lote, que las suman, están en el resumen (RF-14 C3). */}
          <Typography variant="body2" sx={{ mt: 0.5 }}>
            {t('resultado.marcados', { sanos: muestra.granosSanos, total: muestra.totalGranos })}
          </Typography>
        </Box>
      )}
      {quien && <Typography variant="body2" color="text.secondary">{quien}</Typography>}
      <Tabs value={elegida} onChange={(_, valor) => setElegida(valor)} variant={esPc ? 'standard' : 'fullWidth'}
        aria-label={t('resultado.pestanas', { numero: muestra.numero })} slotProps={{ indicator: { sx: { display: 'none' } } }}
        sx={estiloDeLasPestanas(esPc)}>
        {muestra.fotos.map((una, indice) => {
          const nombre = t('resultado.pestana', { numero: una.numero });
          const granos = t('resultado.granosDeLaPestana', { count: una.granos.length });
          return (
            <Tab key={una.numero} value={indice} id={`${id}-pestana-${indice}`} aria-controls={`${id}-panel`}
              label={esPc ? `${nombre} · ${granos}` : (
                <Box component="span" sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                  <span>{nombre}</span>
                  <Box component="span" sx={{ fontSize: 12 }}>{granos}</Box>
                </Box>
              )} />
          );
        })}
      </Tabs>
      {foto && (
        <Box role="tabpanel" id={`${id}-panel`} aria-labelledby={`${id}-pestana-${elegida}`}>
          <FotoMarcada key={foto.numero} foto={foto} alFallarLaVista={alFallarLaVista}
            alCargarLaVista={alCargarLaVista} muestra={conTitulo ? muestra.numero : null} />
        </Box>
      )}
    </Box>
  );
}

/**
 * «Fotos marcadas».
 *
 * @param {object} props las propiedades
 * @param {import('../api/analisis.js').MuestraDelLote[]} props.muestras las muestras del lote
 * @param {() => void} props.alFallarLaVista se llama si una imagen no carga, como cuando su enlace venció
 * @param {() => void} [props.alCargarLaVista] se llama cuando una imagen carga
 * @returns {import('react').ReactElement} la sección
 */
export function FotosMarcadas({ muestras, alFallarLaVista, alCargarLaVista }) {
  const { t } = useTranslation();
  const esPc = useEsPc();
  const idDelTitulo = useId();
  const terminadas = muestras.filter(({ estado }) => estado === 'TERMINADA');
  // Con la segunda muestra, también la que todavía no termina, cada muestra se nombra (D-25).
  const variasMuestras = muestras.length > 1;
  return (
    <Box component="section" aria-labelledby={idDelTitulo} sx={(theme) => ({
      display: 'flex', flexDirection: 'column', gap: esPc ? 2 : 1.5, p: esPc ? 3 : 2, minWidth: 0,
      borderRadius: `${medidas.tarjeta.radio}px`, border: `1px solid ${theme.vars.palette.divider}`,
      bgcolor: 'background.paper',
    })}>
      <Typography variant="h2" id={idDelTitulo} sx={{ fontSize: esPc ? letra.subtitulo : 18 }}>
        {t('resultado.fotosMarcadas')}
      </Typography>
      {terminadas.map((muestra) => (
        <FotosDeUnaMuestra key={muestra.numero} muestra={muestra} conTitulo={variasMuestras}
          alFallarLaVista={alFallarLaVista} alCargarLaVista={alCargarLaVista} />
      ))}
    </Box>
  );
}
