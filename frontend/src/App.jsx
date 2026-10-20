/*
 * Nombre: App.jsx
 * Descripción: La app de InspectIA: el tema claro, oscuro o del sistema (RP-08), los idiomas (RP-11), las
 *              rutas del navegador y la sesión.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import CssBaseline from '@mui/material/CssBaseline';
import { ThemeProvider } from '@mui/material/styles';
import { BrowserRouter } from 'react-router';
import './i18n/i18n.js';
import { Rutas } from './navegacion/Rutas.jsx';
import { ProveedorDeSesion } from './sesion/ProveedorDeSesion.jsx';
import { tema } from './tema/tema.js';

/** Claves del navegador donde queda el tema elegido. */
const CLAVES_DEL_TEMA = { modo: 'inspectia.tema', esquema: 'inspectia.esquema' };

/**
 * La app. Usa el tema del sistema hasta que la persona elija otro.
 *
 * @returns {import('react').ReactElement} la app
 */
export default function App() {
  return (
    <ThemeProvider theme={tema} defaultMode="system" modeStorageKey={CLAVES_DEL_TEMA.modo}
      colorSchemeStorageKey={CLAVES_DEL_TEMA.esquema} noSsr disableTransitionOnChange>
      <CssBaseline />
      <BrowserRouter>
        <ProveedorDeSesion>
          <Rutas />
        </ProveedorDeSesion>
      </BrowserRouter>
    </ThemeProvider>
  );
}
