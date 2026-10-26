/*
 * Nombre: MenuLateral.jsx
 * Descripción: El menú principal según el rol (RF-01 C1, RNF-04) y «Mi cuenta», como en el prototipo: la
 *              sección actual se marca con aria-current y cada entrada mide al menos 44 px (RNF-05). Una
 *              pantalla generada con un ícono que el menú no conoce sale con uno genérico (RP-23).
 * Fecha de creación: 20/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import AddCircleOutlineOutlined from '@mui/icons-material/AddCircleOutlineOutlined';
import BusinessOutlined from '@mui/icons-material/BusinessOutlined';
import GroupOutlined from '@mui/icons-material/GroupOutlined';
import HistoryOutlined from '@mui/icons-material/HistoryOutlined';
import HomeOutlined from '@mui/icons-material/HomeOutlined';
import InfoOutlined from '@mui/icons-material/InfoOutlined';
import InsertChartOutlined from '@mui/icons-material/InsertChartOutlined';
import KeyOutlined from '@mui/icons-material/KeyOutlined';
import Logout from '@mui/icons-material/Logout';
import PsychologyOutlined from '@mui/icons-material/PsychologyOutlined';
import TuneOutlined from '@mui/icons-material/TuneOutlined';
import ViewListOutlined from '@mui/icons-material/ViewListOutlined';
import Box from '@mui/material/Box';
import Divider from '@mui/material/Divider';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Typography from '@mui/material/Typography';
import { useTranslation } from 'react-i18next';
import { NavLink, useNavigate } from 'react-router';
import { useSesion } from '../sesion/contexto.js';
import { RUTA_INICIAR_SESION } from '../sesion/volver.js';
import { MENU_DE_CUENTA, menuPrincipal } from './menu.js';

/** El ícono de cada entrada, por el nombre que le da menu.js. */
export const ICONOS = {
  inicio: HomeOutlined,
  nuevoLote: AddCircleOutlineOutlined,
  historial: HistoryOutlined,
  usuarios: GroupOutlined,
  rangos: TuneOutlined,
  estadisticas: InsertChartOutlined,
  modelo: PsychologyOutlined,
  empresas: BusinessOutlined,
  contrasena: KeyOutlined,
  acercaDe: InfoOutlined,
  cerrarSesion: Logout,
};

/** El ícono de una pantalla generada que dice uno que el menú no conoce, como la de otra temática. */
const ICONO_GENERICO = ViewListOutlined;

/**
 * El estilo de cada entrada; la sección actual va resaltada, como en el prototipo.
 *
 * @param {import('@mui/material/styles').Theme} theme el tema
 * @returns {object} el estilo
 */
const estiloDeEntrada = (theme) => ({
  minHeight: 44,
  borderRadius: 1,
  gap: 1.5,
  px: 1.5,
  color: theme.vars.palette.text.primary,
  '&[aria-current="page"]': {
    backgroundColor: theme.vars.palette.cacao.seleccion.fondo,
    color: theme.vars.palette.cacao.seleccion.texto,
    '& .MuiListItemText-primary': { fontWeight: 500 },
  },
  '& .MuiListItemIcon-root': { minWidth: 0, color: 'inherit' },
});

/**
 * Una entrada del menú que lleva a una sección.
 *
 * @param {object} props las propiedades
 * @param {import('./menu.js').Seccion} props.entrada la entrada
 * @param {() => void} [props.alElegir] se llama al elegirla, para cerrar el menú en el celular
 * @returns {import('react').ReactElement} la entrada
 */
function Entrada({ entrada, alElegir }) {
  const { t } = useTranslation();
  const Icono = ICONOS[entrada.icono] ?? ICONO_GENERICO;
  return (
    <ListItem disablePadding>
      <ListItemButton component={NavLink} to={entrada.ruta} end onClick={alElegir} sx={estiloDeEntrada}>
        <ListItemIcon><Icono aria-hidden /></ListItemIcon>
        <ListItemText primary={t(entrada.etiqueta)} />
      </ListItemButton>
    </ListItem>
  );
}

/**
 * El menú de la persona que entró: sus secciones y «Mi cuenta».
 *
 * @param {{ alElegir?: () => void }} props se llama al elegir una entrada, para cerrar el menú en el celular
 * @returns {import('react').ReactElement} el menú
 */
export function MenuLateral({ alElegir }) {
  const { t } = useTranslation();
  const { sesion, cerrarSesion } = useSesion();
  const navegar = useNavigate();
  const [cambiarContrasena, acercaDe, salir] = MENU_DE_CUENTA;
  const IconoSalir = ICONOS[salir.icono];

  return (
    <Box component="nav" aria-label={t('menu.principal')}
      sx={{ flex: 1, p: '16px 12px 20px', display: 'flex', flexDirection: 'column' }}>
      <List disablePadding sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
        {menuPrincipal(sesion.usuario.rol).map((entrada) => (
          <Entrada key={entrada.ruta} entrada={entrada} alElegir={alElegir} />
        ))}
      </List>
      <Box sx={{ flex: 1, minHeight: 24 }} />
      <Divider sx={{ my: 1, mx: 0.5 }} />
      <Typography id="titulo-mi-cuenta" variant="caption" color="text.secondary"
        sx={{ m: '4px 12px', fontSize: 12, fontWeight: 500 }}>
        {t('cuenta.titulo')}
      </Typography>
      <List disablePadding aria-labelledby="titulo-mi-cuenta" sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
        <Entrada entrada={cambiarContrasena} alElegir={alElegir} />
        <Entrada entrada={acercaDe} alElegir={alElegir} />
        <ListItem disablePadding>
          <ListItemButton component="button" sx={estiloDeEntrada} onClick={() => {
            cerrarSesion();
            navegar(RUTA_INICIAR_SESION, { replace: true });
          }}>
            <ListItemIcon><IconoSalir aria-hidden /></ListItemIcon>
            <ListItemText primary={t(salir.etiqueta)} />
          </ListItemButton>
        </ListItem>
      </List>
    </Box>
  );
}
