/*
 * Nombre: main.jsx
 * Descripción: Punto de entrada del frontend: carga la letra Roboto desde la app (400, 500 y 700), pone la clase
 *              del tema claro u oscuro y dibuja la app en la página.
 * Fecha de creación: 20/10/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import '@fontsource/roboto/400.css';
import '@fontsource/roboto/500.css';
import '@fontsource/roboto/700.css';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App.jsx';
import { ponerClaseInicial } from './tema/claseInicial.js';

// La clase del tema va antes de dibujar: así nada se pinta primero con el tema claro (tema/claseInicial.js).
ponerClaseInicial();

createRoot(document.getElementById('app')).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
