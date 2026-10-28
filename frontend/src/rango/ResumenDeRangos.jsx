/*
 * Nombre: ResumenDeRangos.jsx
 * Descripción: El resumen «Así quedan los rangos» de la pantalla de rangos (RF-13), como en el prototipo: qué
 *              porcentajes de granos sanos abarca cada categoría con los límites como van quedando, mientras se
 *              escriben. Si los límites de un par no bajan en orden, o uno no es un número, las categorías que
 *              dependen de ellos dicen «revisa el límite». Es lo escrito a mano de la pantalla generada: llega en sus
 *              extensiones (RP-23).
 * Fecha de creación: 28/10/2026
 * Actualización: 28/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { useId } from 'react';
import { useTranslation } from 'react-i18next';
import { textoDelDecimal } from '../crud/decimales.js';
import { idiomaDe } from '../i18n/i18n.js';
import { medidas } from '../tema/tokens.js';
import { intervalosDeLosRangos } from './limites.js';

/** El campo de los límites en las filas de los rangos, como lo nombra la API. */
const LIMITE = 'limiteInferior';

/**
 * El texto de lo que abarca una categoría, como «de 90,0 a 95,9 %».
 *
 * @param {import('./limites.js').Intervalo} intervalo lo que abarca
 * @param {(numero: number) => string} numero el texto de un número, con el separador del idioma
 * @param {import('../crud/registros.js').Traducir} t la función de traducción
 * @returns {string} el texto
 */
function textoDelIntervalo(intervalo, numero, t) {
  switch (intervalo.tipo) {
    case 'desde':
      return t('rangos.resumen.desde', { limite: numero(intervalo.limite) });
    case 'entre':
      return t('rangos.resumen.entre', { desde: numero(intervalo.desde), hasta: numero(intervalo.hasta) });
    case 'menosDe':
      return t('rangos.resumen.menosDe', { limite: numero(intervalo.limite) });
    default:
      return t('rangos.resumen.revisar');
  }
}

/**
 * El resumen de los rangos, antes del formulario.
 *
 * @param {object} props las propiedades
 * @param {object[]} props.filas las filas como van quedando en el formulario, con los límites leídos como números
 * @param {import('../crud/PantallaEnBloque.jsx').DescripcionDelBloque} props.pantalla la descripción de la pantalla:
 *   de ella salen el nombre de cada categoría y la escala de los límites
 * @returns {import('react').ReactElement} el resumen
 */
export function ResumenDeRangos({ filas, pantalla }) {
  const { t, i18n } = useTranslation();
  const idDelTitulo = useId();
  const { atributo, valores } = pantalla.clave;
  const { escala } = pantalla.campos.find(({ nombre }) => nombre === LIMITE);
  const region = idiomaDe(i18n.language).region;
  const intervalos = intervalosDeLosRangos(filas.map((fila) => fila[LIMITE]), escala);
  const numero = (valor) => textoDelDecimal(valor, escala, region);
  const nombreDe = (codigo) => t(valores.find((valor) => valor.codigo === codigo).etiqueta);

  return (
    <Box component="section" aria-labelledby={idDelTitulo} sx={(theme) => ({
      display: 'flex',
      flexDirection: 'column',
      gap: 1.5,
      p: { xs: 2, md: '20px 24px' },
      borderRadius: `${medidas.tarjeta.radio}px`,
      border: `1px solid ${theme.vars.palette.divider}`,
      bgcolor: 'background.paper',
    })}>
      <Typography id={idDelTitulo} variant="h2" sx={{ fontSize: 16, fontWeight: 500 }}>
        {t('rangos.resumen.titulo')}
      </Typography>
      <Box component="ul" role="list" sx={{
        m: 0,
        p: 0,
        listStyle: 'none',
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))',
        gap: 1.25,
      }}>
        {filas.map((fila, posicion) => (
          <Box component="li" key={fila[atributo]}
            sx={{ p: '10px 12px', borderRadius: `${medidas.boton.radio}px`, bgcolor: 'background.default' }}>
            <Box component="strong" sx={{ display: 'block', fontWeight: 500 }}>{nombreDe(fila[atributo])}</Box>
            <Box component="span" sx={{ color: 'text.secondary' }}>{textoDelIntervalo(intervalos[posicion], numero, t)}</Box>
          </Box>
        ))}
      </Box>
    </Box>
  );
}
