/*
 * Nombre: Marco.jsx
 * Descripción: El marco de las pantallas con sesión, como en el prototipo: el enlace «Saltar al contenido»
 *              (RNF-06 C4), la barra superior y el menú lateral de 240 px en el PC, o detrás del botón del
 *              menú en el celular, con el idioma y el tema (RNF-05). Tiene también el marco sencillo de las
 *              pantallas que se abren sin sesión.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Box from '@mui/material/Box';
import Divider from '@mui/material/Divider';
import Drawer from '@mui/material/Drawer';
import Link from '@mui/material/Link';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Marca } from '../componentes/Marca.jsx';
import { Preferencias } from '../componentes/Preferencias.jsx';
import { useEsPc } from '../componentes/useEsPc.js';
import { medidas } from '../tema/tokens.js';
import { BarraSuperior } from './BarraSuperior.jsx';
import { MenuLateral } from './MenuLateral.jsx';

/** El id del contenido de cada pantalla, al que lleva «Saltar al contenido». */
const CONTENIDO = 'contenido';

/**
 * El enlace «Saltar al contenido»: es lo primero que se alcanza con el teclado y solo se ve con el foco.
 *
 * @returns {import('react').ReactElement} el enlace
 */
function SaltarAlContenido() {
  const { t } = useTranslation();
  return (
    <Link href={`#${CONTENIDO}`} sx={(theme) => ({
      position: 'absolute',
      left: -9999,
      top: 8,
      zIndex: theme.zIndex.tooltip,
      p: '10px 14px',
      borderRadius: 1,
      backgroundColor: theme.vars.palette.background.paper,
      '&:focus': { left: 16 },
    })}>
      {t('marco.saltar')}
    </Link>
  );
}

/**
 * El contenido de una pantalla, al que lleva «Saltar al contenido», con los márgenes del prototipo.
 *
 * @param {{ children: import('react').ReactNode }} props el contenido
 * @returns {import('react').ReactElement} el contenido
 */
function Contenido({ children }) {
  return (
    <Box component="main" id={CONTENIDO} tabIndex={-1} sx={{
      flex: 1,
      minWidth: 0,
      outline: 'none',
      px: { xs: `${medidas.contenido.ladosCelular}px`, md: `${medidas.contenido.lados}px` },
      pt: { xs: 3, md: `${medidas.contenido.arriba}px` },
      pb: 6,
    }}>
      <Box sx={{ maxWidth: medidas.contenido.anchoMaximo, mx: 'auto' }}>{children}</Box>
    </Box>
  );
}

/**
 * El marco de una pantalla que se abre sin sesión, como Acerca de: la barra con la marca y el contenido.
 *
 * @param {{ children: import('react').ReactNode }} props el contenido de la pantalla
 * @returns {import('react').ReactElement} el marco
 */
export function MarcoSinSesion({ children }) {
  return (
    <Box sx={{ minHeight: '100dvh', display: 'flex', flexDirection: 'column', bgcolor: 'background.default' }}>
      <Box component="header" sx={(theme) => ({
        height: 64,
        display: 'flex',
        alignItems: 'center',
        px: 2,
        backgroundColor: theme.vars.palette.cacao.barra.fondo,
        borderBottom: `1px solid ${theme.vars.palette.cacao.barra.borde}`,
      })}>
        <Marca sx={(theme) => ({ px: 0.5, color: theme.vars.palette.cacao.barra.marca })} />
      </Box>
      <Contenido>{children}</Contenido>
    </Box>
  );
}

/**
 * El marco de una pantalla con sesión.
 *
 * @param {{ children: import('react').ReactNode }} props el contenido de la pantalla
 * @returns {import('react').ReactElement} el marco
 */
export function Marco({ children }) {
  const esPc = useEsPc();
  const [menuAbierto, setMenuAbierto] = useState(false);
  const cerrarMenu = () => setMenuAbierto(false);

  return (
    <Box sx={{ minHeight: '100dvh', display: 'flex', flexDirection: 'column', bgcolor: 'background.default' }}>
      <SaltarAlContenido />
      <BarraSuperior esPc={esPc} menuAbierto={menuAbierto} alAbrirMenu={() => setMenuAbierto(true)} />
      <Box sx={{ flex: 1, display: 'flex' }}>
        {esPc ? (
          <Box sx={(theme) => ({ width: medidas.menu, flexShrink: 0, display: 'flex', flexDirection: 'column',
            bgcolor: 'background.paper', borderRight: `1px solid ${theme.vars.palette.divider}` })}>
            <MenuLateral />
          </Box>
        ) : (
          <Drawer id="menu-del-celular" open={menuAbierto} onClose={cerrarMenu}
            slotProps={{ paper: { sx: { width: 280, maxWidth: '85vw' } } }}>
            <MenuLateral alElegir={cerrarMenu} />
            <Divider />
            <Box sx={{ p: 2 }}>
              <Preferencias ancho />
            </Box>
          </Drawer>
        )}
        <Contenido>{children}</Contenido>
      </Box>
    </Box>
  );
}
