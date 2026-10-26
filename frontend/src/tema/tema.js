/*
 * Nombre: tema.js
 * Descripción: Tema de Material UI de InspectIA (RP-08): los colores claro y oscuro, la letra y las medidas
 *              del prototipo, todos tomados de tokens.js. Usa variables CSS, así el tema cambia sin volver a
 *              dibujar la página.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { createTheme } from '@mui/material/styles';
import { colores, letra, medidas } from './tokens.js';

/** Severidad de un aviso de Material UI → su grupo de colores en los tokens. */
const TONO_DEL_AVISO = { error: 'error', warning: 'advertencia', info: 'informacion', success: 'exito' };

/**
 * La paleta de Material UI de un tema. Los tokens completos quedan en palette.cacao, para los componentes
 * que necesitan colores propios, como la barra superior.
 *
 * @param {typeof colores.claro} c los colores del tema
 * @returns {object} la paleta
 */
function paleta(c) {
  return {
    primary: { main: c.primario, dark: c.enlaceHover, contrastText: c.sobrePrimario },
    error: { main: c.error.principal },
    warning: { main: c.advertencia.principal },
    info: { main: c.informacion.principal },
    success: { main: c.exito.principal },
    background: { default: c.fondo, paper: c.superficie },
    text: { primary: c.texto, secondary: c.textoSecundario },
    divider: c.bordeTarjeta,
    cacao: c,
  };
}

/** El tema de la app, con los esquemas claro y oscuro. */
export const tema = createTheme({
  cssVariables: { colorSchemeSelector: 'class' },
  colorSchemes: {
    light: { palette: paleta(colores.claro) },
    dark: { palette: paleta(colores.oscuro) },
  },
  shape: { borderRadius: medidas.boton.radio },
  typography: {
    fontFamily: letra.familia,
    h1: {
      fontSize: letra.titulo.pc,
      fontWeight: letra.pesos.media,
      lineHeight: 1.25,
      '@media (max-width:599.95px)': { fontSize: letra.titulo.celular },
    },
    h2: { fontSize: letra.subtitulo, fontWeight: letra.pesos.media, lineHeight: 1.3 },
    body1: { fontSize: letra.texto, lineHeight: 1.55 },
    body2: { fontSize: letra.ayuda, lineHeight: 1.5 },
    button: { fontSize: letra.texto, fontWeight: letra.pesos.media, textTransform: 'none' },
  },
  components: {
    MuiCssBaseline: {
      styleOverrides: (theme) => ({
        // El foco del teclado siempre se ve (RNF-06 C4). Los campos ya lo marcan con su borde, y el contenido al
        // que lleva «Saltar al contenido» (tabindex -1) no lo necesita.
        ':focus-visible:not(input, textarea, [tabindex="-1"])': {
          outline: `${medidas.foco.grosor}px solid ${theme.vars.palette.primary.main}`,
          outlineOffset: medidas.foco.separacion,
        },
      }),
    },
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: { minHeight: medidas.boton.alto, borderRadius: medidas.boton.radio, paddingInline: 24 },
      },
    },
    MuiIconButton: {
      styleOverrides: { root: { width: medidas.zonaTactil, height: medidas.zonaTactil, borderRadius: medidas.boton.radio } },
    },
    MuiLink: {
      defaultProps: { underline: 'always' },
      styleOverrides: {
        root: ({ theme }) => ({
          color: theme.vars.palette.primary.main,
          '&:hover': { color: theme.vars.palette.primary.dark },
        }),
      },
    },
    MuiFormLabel: {
      styleOverrides: {
        root: ({ theme }) => ({
          marginBottom: 6,
          fontSize: letra.ayuda,
          fontWeight: letra.pesos.media,
          color: theme.vars.palette.text.primary,
          '&.Mui-focused, &.Mui-error': { color: theme.vars.palette.text.primary },
        }),
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: ({ theme }) => ({
          borderRadius: medidas.campo.radio,
          backgroundColor: theme.vars.palette.background.paper,
          '&:not(.Mui-error):not(.Mui-focused) .MuiOutlinedInput-notchedOutline': {
            borderColor: theme.vars.palette.cacao.bordeCampo,
          },
          '&:hover:not(.Mui-error):not(.Mui-focused) .MuiOutlinedInput-notchedOutline': {
            borderColor: theme.vars.palette.text.primary,
          },
        }),
        input: { boxSizing: 'border-box', height: medidas.campo.alto, padding: '0 14px', fontSize: letra.texto },
      },
    },
    MuiFormHelperText: {
      styleOverrides: { root: { marginInline: 0, marginTop: 6, fontSize: letra.ayuda, lineHeight: 1.45 } },
    },
    MuiAlert: {
      styleOverrides: {
        root: ({ theme, ownerState }) => {
          const tono = theme.vars.palette.cacao[TONO_DEL_AVISO[ownerState.severity ?? 'success']];
          return {
            borderRadius: medidas.boton.radio,
            border: `1px solid ${tono.principal}`,
            backgroundColor: tono.fondo,
            color: tono.texto,
            fontSize: letra.ayuda,
            '& .MuiAlert-icon': { color: tono.principal },
          };
        },
        message: { display: 'flex', flexDirection: 'column', gap: 4 },
      },
    },
    MuiAlertTitle: {
      styleOverrides: { root: { margin: 0, fontSize: letra.ayuda, fontWeight: letra.pesos.media } },
    },
    MuiToggleButton: {
      styleOverrides: {
        root: ({ theme }) => ({
          minHeight: 40,
          paddingInline: 12,
          border: 0,
          borderRadius: 6,
          fontSize: letra.ayuda,
          color: theme.vars.palette.text.secondary,
          '&.Mui-selected, &.Mui-selected:hover': {
            backgroundColor: theme.vars.palette.cacao.seleccion.fondo,
            color: theme.vars.palette.cacao.seleccion.texto,
            fontWeight: letra.pesos.media,
          },
        }),
      },
    },
    MuiToggleButtonGroup: {
      styleOverrides: {
        root: ({ theme }) => ({
          gap: 4,
          padding: 4,
          border: `1px solid ${theme.vars.palette.divider}`,
          borderRadius: 10,
          backgroundColor: theme.vars.palette.background.paper,
        }),
      },
    },
  },
});
