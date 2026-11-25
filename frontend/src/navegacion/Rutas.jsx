/*
 * Nombre: Rutas.jsx
 * Descripción: Las rutas de la app (RNF-04): Iniciar sesión y Acerca de se abren sin sesión; las demás piden
 *              una sesión y van dentro del marco, y cada sección solo la abren los roles que la usan. Las
 *              pantallas generadas reciben aquí sus extensiones (RP-23), y las del análisis, escritas a mano, salen de
 *              PANTALLAS_DEL_ANALISIS (RF-04, RF-14); la de las fotos de la muestra solo la abre el usuario final
 *              (RF-05), y la del resultado de un lote, el usuario final y la administradora (RF-10 a RF-12, RF-14 C3).
 *              Las de las estadísticas, también a mano, salen de PANTALLAS_DE_ESTADISTICA (RF-16), y «Modelo de IA»,
 *              escrita a mano, solo la abre la administradora (RF-17). Las secciones que todavía no existen muestran
 *              «Esta sección estará disponible pronto.».
 * Fecha de creación: 20/10/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.4
 */
import Box from '@mui/material/Box';
import CircularProgress from '@mui/material/CircularProgress';
import { Suspense } from 'react';
import { useTranslation } from 'react-i18next';
import { Navigate, Outlet, Route, Routes } from 'react-router';
import Estadisticas from '../estadistica/Estadisticas.jsx';
import { EXTENSIONES } from '../extensiones.js';
import FotosDeLaMuestra from '../lote/FotosDeLaMuestra.jsx';
import Historial from '../lote/Historial.jsx';
import NuevoLote from '../lote/NuevoLote.jsx';
import ResultadoDelLote from '../lote/ResultadoDelLote.jsx';
import ModeloDeIa from '../modelo/ModeloDeIa.jsx';
import AcercaDe from '../paginas/AcercaDe.jsx';
import CambiarContrasena from '../paginas/CambiarContrasena.jsx';
import IniciarSesion from '../paginas/IniciarSesion.jsx';
import Inicio from '../paginas/Inicio.jsx';
import NoEncontrada, { SinPermiso } from '../paginas/NoEncontrada.jsx';
import Pendiente from '../paginas/Pendiente.jsx';
import { useSesion } from '../sesion/contexto.js';
import { RUTA_INICIAR_SESION } from '../sesion/volver.js';
import { Marco } from './Marco.jsx';
import { ROLES, SECCIONES } from './menu.js';

/**
 * Las pantallas escritas a mano (D-21), según la ruta de su sección del menú: las del análisis y la del modelo de IA
 * (RF-17). Se cargan con la app, como Cambiar contraseña, y no aparte, como las generadas.
 */
const PANTALLAS_DEL_ANALISIS = { '/lotes/nuevo': NuevoLote, '/historial': Historial, '/modelo': ModeloDeIa };

/** Las pantallas de las estadísticas, escritas a mano (D-21), según la ruta de su sección del menú (RF-16). */
const PANTALLAS_DE_ESTADISTICA = { '/estadisticas': Estadisticas };

/**
 * Las pantallas que piden una sesión: sin ella llevan a Iniciar sesión; con ella van dentro del marco.
 *
 * @returns {import('react').ReactElement} la pantalla o la redirección
 */
function ConSesion() {
  const { sesion } = useSesion();
  if (!sesion) {
    return <Navigate to={RUTA_INICIAR_SESION} replace />;
  }
  return <Marco><Outlet /></Marco>;
}

/**
 * Una pantalla que solo abren algunos roles: a los demás les dice que no tienen permiso (RNF-04).
 *
 * @param {{ roles: string[], children: import('react').ReactNode }} props los roles y la pantalla
 * @returns {import('react').ReactElement} la pantalla o el aviso
 */
function ConRol({ roles, children }) {
  const { sesion } = useSesion();
  return roles.includes(sesion.usuario.rol) ? children : <SinPermiso />;
}

/**
 * Lo que se ve mientras llega una pantalla generada, que se carga la primera vez que se abre.
 *
 * @returns {import('react').ReactElement} el indicador de carga
 */
function Cargando() {
  const { t } = useTranslation();
  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
      <CircularProgress aria-label={t('crud.cargando')} />
    </Box>
  );
}

/**
 * Una sección: su pantalla del análisis o de las estadísticas, la generada con sus extensiones, o la página de lo que está pendiente.
 *
 * @param {{ seccion: import('./menu.js').Seccion }} props la sección
 * @returns {import('react').ReactElement} la pantalla
 */
function PantallaDeSeccion({ seccion }) {
  const { Pantalla, entidad, etiqueta, ruta } = seccion;
  const DelAnalisis = PANTALLAS_DEL_ANALISIS[ruta] ?? PANTALLAS_DE_ESTADISTICA[ruta];
  if (DelAnalisis) {
    return <DelAnalisis />;
  }
  if (!Pantalla) {
    return <Pendiente etiqueta={etiqueta} />;
  }
  return <Suspense fallback={<Cargando />}><Pantalla extensiones={EXTENSIONES[entidad]} /></Suspense>;
}

/**
 * Las rutas de la app.
 *
 * @returns {import('react').ReactElement} las rutas
 */
export function Rutas() {
  return (
    <Routes>
      <Route path={RUTA_INICIAR_SESION} element={<IniciarSesion />} />
      <Route path="/acerca-de" element={<AcercaDe />} />
      <Route element={<ConSesion />}>
        <Route index element={<Inicio />} />
        <Route path="/cambiar-contrasena" element={<CambiarContrasena />} />
        <Route path="/lotes/:id/fotos"
          element={<ConRol roles={[ROLES.USUARIO_FINAL]}><FotosDeLaMuestra /></ConRol>} />
        {/* /lotes/nuevo sigue abriendo Nuevo lote: React Router prefiere el segmento fijo al de :id. */}
        <Route path="/lotes/:id"
          element={<ConRol roles={[ROLES.USUARIO_FINAL, ROLES.ADMIN_EMPRESA]}><ResultadoDelLote /></ConRol>} />
        {SECCIONES.filter(({ ruta }) => ruta !== '/').map((seccion) => (
          <Route key={seccion.ruta} path={seccion.ruta}
            element={<ConRol roles={seccion.roles}><PantallaDeSeccion seccion={seccion} /></ConRol>} />
        ))}
        <Route path="*" element={<NoEncontrada />} />
      </Route>
    </Routes>
  );
}
