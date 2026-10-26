/*
 * Nombre: BarraSuperior.jsx
 * Descripción: La barra superior del prototipo (64 px): el botón del menú en el celular, la marca, el idioma
 *              y el tema en el PC (RP-08, RP-11) y «Mi cuenta» con el nombre de quien entró y su empresa, o su
 *              rol si no tiene empresa (RF-01 C1).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import DarkModeOutlined from '@mui/icons-material/DarkModeOutlined';
import ExpandMore from '@mui/icons-material/ExpandMore';
import Language from '@mui/icons-material/Language';
import LightModeOutlined from '@mui/icons-material/LightModeOutlined';
import MenuIcon from '@mui/icons-material/Menu';
import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import { useColorScheme } from '@mui/material/styles';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
import { Marca } from '../componentes/Marca.jsx';
import { idiomaDe, IDIOMAS } from '../i18n/i18n.js';
import { useSesion } from '../sesion/contexto.js';
import { RUTA_INICIAR_SESION } from '../sesion/volver.js';
import { MENU_DE_CUENTA } from './menu.js';
import { ICONOS } from './MenuLateral.jsx';

/**
 * Las iniciales de un nombre, para el avatar: las de sus dos primeras palabras.
 *
 * @param {string} nombre el nombre, como «Ana Pérez»
 * @returns {string} las iniciales, como «AP»
 */
function iniciales(nombre) {
  return nombre.split(' ').filter(Boolean).slice(0, 2).map((palabra) => palabra[0]).join('').toUpperCase();
}

/**
 * El botón del idioma, con el menú de los idiomas.
 *
 * @returns {import('react').ReactElement} el botón y su menú
 */
function BotonDeIdioma() {
  const { t, i18n } = useTranslation();
  const [ancla, setAncla] = useState(null);
  const actual = idiomaDe(i18n.language);
  const cerrar = () => setAncla(null);

  return (
    <>
      <Button color="inherit" startIcon={<Language aria-hidden />} onClick={(evento) => setAncla(evento.currentTarget)}
        aria-label={t('idioma.boton', { idioma: actual.nombre })} aria-haspopup="menu"
        aria-expanded={ancla ? 'true' : undefined} aria-controls={ancla ? 'menu-idioma' : undefined}
        sx={{ minHeight: 44, px: 1.25, fontSize: 14 }}>
        {/* El nombre del idioma, no su código: lo que se ve está en el nombre del botón (WCAG 2.5.3). */}
        {actual.nombre}
      </Button>
      <Menu id="menu-idioma" anchorEl={ancla} open={Boolean(ancla)} onClose={cerrar}>
        {IDIOMAS.map(({ codigo, nombre }) => (
          <MenuItem key={codigo} role="menuitemradio" aria-checked={codigo === actual.codigo} lang={codigo}
            selected={codigo === actual.codigo} onClick={() => {
              i18n.changeLanguage(codigo);
              cerrar();
            }}>
            {nombre}
          </MenuItem>
        ))}
      </Menu>
    </>
  );
}

/**
 * El botón que cambia entre el tema claro y el oscuro.
 *
 * @returns {import('react').ReactElement} el botón
 */
function BotonDeTema() {
  const { t } = useTranslation();
  const { mode, systemMode, setMode } = useColorScheme();
  const oscuro = (mode === 'system' ? systemMode : mode) === 'dark';
  return (
    <IconButton color="inherit" onClick={() => setMode(oscuro ? 'light' : 'dark')}
      aria-label={t(oscuro ? 'tema.aClaro' : 'tema.aOscuro')}>
      {oscuro ? <LightModeOutlined /> : <DarkModeOutlined />}
    </IconButton>
  );
}

/**
 * El botón «Mi cuenta», con su menú: Cambiar contraseña, Acerca de y Cerrar sesión.
 *
 * @param {{ esPc: boolean }} props si se ve en tamaño de PC, con el nombre y la empresa junto al avatar
 * @returns {import('react').ReactElement} el botón y su menú
 */
function BotonDeCuenta({ esPc }) {
  const { t } = useTranslation();
  const { sesion, cerrarSesion } = useSesion();
  const navegar = useNavigate();
  const [ancla, setAncla] = useState(null);
  const { nombre, empresa, rol } = sesion.usuario;
  const detalle = empresa?.nombre ?? t(`usuario.rol.${rol}`);
  const cerrar = () => setAncla(null);

  return (
    <>
      <Button color="inherit" onClick={(evento) => setAncla(evento.currentTarget)}
        aria-label={t('cuenta.boton', { nombre, detalle })} aria-haspopup="menu"
        aria-expanded={ancla ? 'true' : undefined} aria-controls={ancla ? 'menu-cuenta' : undefined}
        sx={{ minHeight: 48, borderRadius: 6, pl: 0.5, pr: 1, gap: 1.25, textAlign: 'left' }}>
        {/* Las iniciales son una imagen decorativa, como una foto de perfil: se dibujan con CSS para que no
            cuenten como el texto visible del botón, cuyo nombre es «Mi cuenta: …» (WCAG 2.5.3). */}
        <Box component="span" aria-hidden data-iniciales={iniciales(nombre)} sx={(theme) => ({
          width: 36,
          height: 36,
          flex: 'none',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          borderRadius: '50%',
          fontSize: 14,
          fontWeight: 700,
          bgcolor: theme.vars.palette.cacao.avatar.fondo,
          color: theme.vars.palette.cacao.avatar.texto,
          '&::before': { content: 'attr(data-iniciales)' },
        })} />
        {esPc && (
          <>
            <Box component="span" sx={{ display: 'flex', flexDirection: 'column', lineHeight: 1.25 }}>
              {/* El espacio entre el nombre y la empresa separa sus palabras para quien lee el texto del botón. */}
              <Box component="span" sx={{ fontSize: 14, fontWeight: 500 }}>{nombre}</Box>{' '}
              <Box component="span" sx={(theme) => ({ fontSize: 12, fontWeight: 400,
                color: theme.vars.palette.cacao.barra.subtitulo })}>
                {detalle}
              </Box>
            </Box>
            <ExpandMore aria-hidden />
          </>
        )}
      </Button>
      <Menu id="menu-cuenta" anchorEl={ancla} open={Boolean(ancla)} onClose={cerrar}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }} transformOrigin={{ vertical: 'top', horizontal: 'right' }}>
        {MENU_DE_CUENTA.map(({ etiqueta, ruta, icono }) => {
          const Icono = ICONOS[icono];
          const alElegir = ruta ? cerrar : () => {
            cerrar();
            cerrarSesion();
            navegar(RUTA_INICIAR_SESION, { replace: true });
          };
          return (
            <MenuItem key={etiqueta} onClick={alElegir} {...(ruta ? { component: Link, to: ruta } : {})}>
              <ListItemIcon><Icono aria-hidden fontSize="small" /></ListItemIcon>
              {t(etiqueta)}
            </MenuItem>
          );
        })}
      </Menu>
    </>
  );
}

/**
 * La barra superior.
 *
 * @param {object} props las propiedades
 * @param {boolean} props.esPc si se ve en tamaño de PC
 * @param {boolean} props.menuAbierto si el menú del celular está abierto
 * @param {() => void} props.alAbrirMenu abre el menú del celular
 * @returns {import('react').ReactElement} la barra
 */
export function BarraSuperior({ esPc, menuAbierto, alAbrirMenu }) {
  const { t } = useTranslation();
  return (
    <AppBar position="static" elevation={0} sx={(theme) => ({
      height: 64,
      justifyContent: 'center',
      backgroundColor: theme.vars.palette.cacao.barra.fondo,
      color: theme.vars.palette.cacao.barra.texto,
      borderBottom: `1px solid ${theme.vars.palette.cacao.barra.borde}`,
    })}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, pl: 2, pr: 1.5 }}>
        {!esPc && (
          <IconButton color="inherit" edge="start" onClick={alAbrirMenu} aria-label={t('menu.abrir')}
            aria-expanded={menuAbierto} aria-controls="menu-del-celular">
            <MenuIcon />
          </IconButton>
        )}
        <Marca sx={(theme) => ({ px: 0.5, color: theme.vars.palette.cacao.barra.marca })} />
        <Box sx={{ flex: 1 }} />
        {esPc && <BotonDeIdioma />}
        {esPc && <BotonDeTema />}
        <BotonDeCuenta esPc={esPc} />
      </Box>
    </AppBar>
  );
}
