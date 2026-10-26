/*
 * Nombre: Preferencias.jsx
 * Descripción: Los selectores de idioma (RP-11) y de tema (RP-08) con botones, como en Iniciar sesión del
 *              prototipo. El idioma y el tema elegidos quedan guardados en el navegador.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Box from '@mui/material/Box';
import { useColorScheme } from '@mui/material/styles';
import ToggleButton from '@mui/material/ToggleButton';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import { useTranslation } from 'react-i18next';
import { IDIOMAS } from '../i18n/i18n.js';

/** Los modos del tema: claro, oscuro o el del sistema. */
const MODOS = [['light', 'tema.claro'], ['dark', 'tema.oscuro'], ['system', 'tema.sistema']];

/**
 * El selector de idioma: un botón por idioma, con su nombre en su propio idioma.
 *
 * @param {{ ancho?: boolean }} props si los botones ocupan todo el ancho, como en el celular
 * @returns {import('react').ReactElement} el selector
 */
export function SelectorDeIdioma({ ancho = false }) {
  const { t, i18n } = useTranslation();
  return (
    <ToggleButtonGroup exclusive value={i18n.language} aria-label={t('idioma.titulo')} fullWidth={ancho}
      onChange={(_, codigo) => codigo && i18n.changeLanguage(codigo)}>
      {IDIOMAS.map(({ codigo, nombre }) => (
        <ToggleButton key={codigo} value={codigo} lang={codigo}>{nombre}</ToggleButton>
      ))}
    </ToggleButtonGroup>
  );
}

/**
 * El selector de tema: claro, oscuro o el del sistema.
 *
 * @param {{ ancho?: boolean }} props si los botones ocupan todo el ancho, como en el celular
 * @returns {import('react').ReactElement} el selector
 */
export function SelectorDeTema({ ancho = false }) {
  const { t } = useTranslation();
  const { mode, setMode } = useColorScheme();
  return (
    <ToggleButtonGroup exclusive value={mode ?? 'system'} aria-label={t('tema.titulo')} fullWidth={ancho}
      onChange={(_, modo) => modo && setMode(modo)}>
      {MODOS.map(([modo, etiqueta]) => <ToggleButton key={modo} value={modo}>{t(etiqueta)}</ToggleButton>)}
    </ToggleButtonGroup>
  );
}

/**
 * Los dos selectores juntos.
 *
 * @param {{ ancho?: boolean }} props si van uno debajo del otro y a todo el ancho, como en el celular
 * @returns {import('react').ReactElement} los selectores
 */
export function Preferencias({ ancho = false }) {
  return (
    <Box sx={{ display: 'flex', flexDirection: ancho ? 'column' : 'row', flexWrap: 'wrap', gap: 1.5,
      justifyContent: 'space-between' }}>
      <SelectorDeIdioma ancho={ancho} />
      <SelectorDeTema ancho={ancho} />
    </Box>
  );
}
