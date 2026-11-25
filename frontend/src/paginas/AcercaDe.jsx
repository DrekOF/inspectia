/*
 * Nombre: AcercaDe.jsx
 * Descripción: La página Acerca de (RNF-08 C1 y C2), como en el prototipo: qué hace InspectIA, lo que mide el
 *              modelo, la cita del dataset (nombre, autor, artículo con su título, licencia CC BY-NC 4.0 y
 *              enlaces a Kaggle y al DOI del artículo) con la nota de que InspectIA no se cobra, el proyecto y la
 *              versión. Se abre con sesión, desde «Mi cuenta», y sin sesión, desde Iniciar sesión.
 * Fecha de creación: 20/10/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import ArrowBack from '@mui/icons-material/ArrowBack';
import Box from '@mui/material/Box';
import Link from '@mui/material/Link';
import Typography from '@mui/material/Typography';
import { useTranslation } from 'react-i18next';
import { Link as EnlaceDeRuta } from 'react-router';
import { version } from '../../package.json';
import { Tarjeta } from '../componentes/Tarjeta.jsx';
import { TituloDePagina } from '../componentes/TituloDePagina.jsx';
import { Marco, MarcoSinSesion } from '../navegacion/Marco.jsx';
import { useSesion } from '../sesion/contexto.js';
import { RUTA_INICIAR_SESION } from '../sesion/volver.js';

/** La página del dataset en Kaggle (RNF-08). */
const KAGGLE = 'https://www.kaggle.com/datasets/luiszhinin979/ecuadorian-cocoa-beans-for-classification';

/** El artículo de los autores del dataset, por su DOI (RNF-08 C2). */
const ARTICULO = 'https://doi.org/10.1007/978-3-031-43078-7_18';

/** Los datos de la cita del dataset: el nombre de cada dato y su valor. */
const CITA = [
  ['acercaDe.dataset', 'acercaDe.datasetValor'],
  ['acercaDe.autor', 'acercaDe.autorValor'],
  ['acercaDe.articulo', 'acercaDe.articuloValor'],
  ['acercaDe.licencia', 'acercaDe.licenciaValor'],
];

/**
 * El contenido de Acerca de.
 *
 * @returns {import('react').ReactElement} el contenido
 */
function Contenido() {
  const { t } = useTranslation();
  return (
    <Box sx={{ maxWidth: 860, mx: 'auto' }}>
      <TituloDePagina titulo={t('acercaDe.titulo')} descripcion={t('acercaDe.descripcion')}
        tituloDePestana={t('cuenta.acercaDe')} />
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
        <Tarjeta titulo={t('acercaDe.modeloTitulo')}>
          <Typography sx={{ fontSize: 15 }}>{t('acercaDe.modeloTexto')}</Typography>
        </Tarjeta>
        <Tarjeta titulo={t('acercaDe.datosTitulo')}>
          <Box component="dl" sx={{ m: 0, display: 'flex', flexDirection: 'column', gap: 1.5, fontSize: 15 }}>
            {CITA.map(([nombre, valor]) => (
              <Box key={nombre} sx={{ display: 'flex', flexWrap: 'wrap', gap: '4px 16px' }}>
                <Box component="dt" sx={{ flex: '0 0 140px', fontWeight: 500, color: 'text.secondary' }}>{t(nombre)}</Box>
                <Box component="dd" sx={{ flex: '1 1 300px', m: 0 }}>{t(valor)}</Box>
              </Box>
            ))}
          </Box>
          <Link href={KAGGLE} sx={{ fontSize: 15, alignSelf: 'flex-start' }}>{t('acercaDe.kaggle')}</Link>
          <Link href={ARTICULO} sx={{ fontSize: 15, alignSelf: 'flex-start' }}>{t('acercaDe.doi')}</Link>
          <Typography variant="body2" color="text.secondary">{t('acercaDe.nota')}</Typography>
        </Tarjeta>
        <Tarjeta titulo={t('acercaDe.proyectoTitulo')}>
          <Typography sx={{ fontSize: 15 }}>{t('acercaDe.proyectoTexto')}</Typography>
          <Typography variant="body2" color="text.secondary">{t('acercaDe.version', { version })}</Typography>
        </Tarjeta>
      </Box>
    </Box>
  );
}

/**
 * Acerca de: dentro del marco con sesión; sin sesión, con un enlace para volver a Iniciar sesión.
 *
 * @returns {import('react').ReactElement} la pantalla
 */
export default function AcercaDe() {
  const { t } = useTranslation();
  const { sesion } = useSesion();
  if (sesion) {
    return <Marco><Contenido /></Marco>;
  }
  return (
    <MarcoSinSesion>
      <Box sx={{ maxWidth: 860, mx: 'auto', mb: 2 }}>
        <Link component={EnlaceDeRuta} to={RUTA_INICIAR_SESION}
          sx={{ display: 'inline-flex', alignItems: 'center', gap: 1, minHeight: 44, fontSize: 15 }}>
          <ArrowBack aria-hidden fontSize="small" />
          {t('acercaDe.volver')}
        </Link>
      </Box>
      <Contenido />
    </MarcoSinSesion>
  );
}
