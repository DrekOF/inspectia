/*
 * Nombre: LeyendaDeMarcas.jsx
 * Descripción: La leyenda de las marcas de una foto, como en el prototipo: sano, en verde con un chulo; no sano, en
 *              rojo con una equis, y dudoso, con un signo de pregunta (RF-11 C1 y C2, D-42). Debajo dice cómo ver la
 *              clase de un grano: con el cursor o tocándolo en el PC, tocándolo en el celular, y con las flechas del
 *              teclado, para los lectores de pantalla (RF-11 C4, RNF-06 C4).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Check from '@mui/icons-material/Check';
import Close from '@mui/icons-material/Close';
import QuestionMark from '@mui/icons-material/QuestionMark';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { useTranslation } from 'react-i18next';
import { OCULTO_A_LA_VISTA } from '../componentes/ocultoALaVista.js';
import { useEsPc } from '../componentes/useEsPc.js';
import { letra, marcasDeFoto } from '../tema/tokens.js';

/** Las marcas de la leyenda: su texto, su color, su símbolo y si va en un círculo, como la del grano dudoso. */
const MARCAS = [
  { clave: 'grano.clase.SANO', color: marcasDeFoto.sano, Icono: Check, circulo: false },
  { clave: 'grano.clase.NO_SANO', color: marcasDeFoto.noSano, Icono: Close, circulo: false },
  { clave: 'resultado.dudoso', color: marcasDeFoto.dudoso, Icono: QuestionMark, circulo: true },
];

/**
 * La leyenda de las marcas.
 *
 * @param {{ idDeLaAyuda: string }} props el id del texto que dice cómo ver la clase de un grano, que describe los
 *   granos de la foto
 * @returns {import('react').ReactElement} la leyenda
 */
export function LeyendaDeMarcas({ idDeLaAyuda }) {
  const { t } = useTranslation();
  const esPc = useEsPc();
  const lado = esPc ? 22 : 20;
  return (
    <Box sx={{ display: 'flex', flexDirection: esPc ? 'row' : 'column', flexWrap: 'wrap', gap: esPc ? '12px 24px' : 1.5,
      alignItems: esPc ? 'center' : 'flex-start' }}>
      <Box component="ul" role="list" aria-label={t('resultado.leyenda')} sx={{
        listStyle: 'none', m: 0, p: 0, display: 'flex', flexWrap: 'wrap', alignItems: 'center',
        gap: esPc ? '12px 24px' : '10px 18px', fontSize: letra.ayuda,
      }}>
        {MARCAS.map(({ clave, color, Icono, circulo }) => (
          <Box component="li" key={clave} sx={{ display: 'flex', alignItems: 'center', gap: esPc ? 1 : 0.75 }}>
            <Box aria-hidden sx={{
              width: lado, height: lado, flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'center',
              borderRadius: circulo ? '50%' : '4px', bgcolor: color, color: marcasDeFoto.simbolo,
            }}>
              <Icono sx={{ fontSize: lado - 6 }} />
            </Box>
            {t(clave)}
          </Box>
        ))}
      </Box>
      <Typography id={idDeLaAyuda} variant="body2" color="text.secondary">
        {esPc ? t('resultado.ayudaPc') : t('resultado.ayudaCelular')}
        {' '}
        <Box component="span" sx={OCULTO_A_LA_VISTA}>{t('resultado.ayudaTeclado')}</Box>
      </Typography>
    </Box>
  );
}
