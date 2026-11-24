/*
 * Nombre: Historial.jsx
 * Descripción: La pantalla Historial de lotes (RF-14), como en el prototipo: los lotes de la empresa, del último
 *              movimiento al más antiguo, con la fecha del resultado, el código, la finca o el proveedor, el
 *              estado, el porcentaje de granos sanos, la categoría probable y quién lo registró. En el PC van en
 *              una tabla y en el celular, en tarjetas (RNF-05), por páginas de 20. Los dos roles abren el detalle de
 *              un lote analizado con «Ver» (RF-14 C3); el usuario final retoma un lote pendiente con «Continuar»
 *              (RF-14 C5), y la administradora no. Un lote que sugiere otra muestra lleva el chip «Sugiere otra
 *              muestra» junto a su estado (D-25). Los filtros (RF-14 C2) y la página van en la dirección, así se
 *              conservan al volver con «Atrás» o al recargar; el total se anuncia a los lectores de pantalla, y si
 *              ningún lote cumple los filtros, lo dice y ofrece quitarlos.
 * Fecha de creación: 22/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.4
 */
import Add from '@mui/icons-material/Add';
import Check from '@mui/icons-material/Check';
import Schedule from '@mui/icons-material/Schedule';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import PaginationItem from '@mui/material/PaginationItem';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import usePagination from '@mui/material/usePagination';
import { useEffect, useId, useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useSearchParams } from 'react-router';
import { erroresPorCampo, textoDelError } from '../api/errores.js';
import { historialDeLotes, NOMBRES_DE_LOS_FILTROS } from '../api/lotes.js';
import { Aviso } from '../componentes/Aviso.jsx';
import { ChipDeEstado } from '../componentes/ChipDeEstado.jsx';
import { OCULTO_A_LA_VISTA } from '../componentes/ocultoALaVista.js';
import { TituloDePagina } from '../componentes/TituloDePagina.jsx';
import { useEsPc } from '../componentes/useEsPc.js';
import { textoDelDecimal } from '../crud/decimales.js';
import { idiomaDe } from '../i18n/i18n.js';
import { ROLES } from '../navegacion/menu.js';
import { useSesion } from '../sesion/contexto.js';
import { letra, medidas } from '../tema/tokens.js';
import { diaEnColombia } from './fechas.js';
import { FiltrosDelHistorial } from './FiltrosDelHistorial.jsx';
import { direccionDe, hayFiltros, leerDireccion } from './filtros.js';

/** Lo que muestra una celda sin dato, como el resultado de un lote pendiente. */
const SIN_DATO = '—';

/**
 * El estilo de un recuadro, como el de la tabla o el de una tarjeta.
 *
 * @param {import('@mui/material/styles').Theme} theme el tema
 * @returns {object} el estilo
 */
const recuadro = (theme) => ({
  borderRadius: `${medidas.tarjeta.radio}px`,
  border: `1px solid ${theme.vars.palette.divider}`,
  bgcolor: 'background.paper',
});

/**
 * Los textos de cada dato de un lote, en el orden de las columnas del prototipo.
 *
 * @param {import('../api/lotes.js').Lote} lote el lote
 * @param {(clave: string) => string} t la función de traducción de i18next
 * @param {string} region la región del idioma, como es-CO
 * @returns {{ fecha: string, codigo: string, origen: string, estado: string, porcentaje: string,
 *   categoria: string, usuario: string }} los textos
 */
function datosDelLote(lote, t, region) {
  return {
    fecha: lote.fechaResultado ? diaEnColombia(lote.fechaResultado, region) : SIN_DATO,
    codigo: lote.codigo,
    origen: lote.origen,
    estado: t(`lote.estado.${lote.estado}`),
    porcentaje: lote.porcentajeSanos === null ? SIN_DATO : `${textoDelDecimal(lote.porcentajeSanos, 1, region)} %`,
    categoria: lote.categoria ? t(`rango_recomendacion.categoria.${lote.categoria}`) : SIN_DATO,
    usuario: lote.usuario.nombre,
  };
}

/** Las columnas de la tabla, con la clave de su título. */
const COLUMNAS = [
  ['fecha', 'lote.fecha_resultado.etiqueta'],
  ['codigo', 'historial.codigo'],
  ['origen', 'lote.origen.etiqueta'],
  ['estado', 'lote.estado.etiqueta'],
  ['porcentaje', 'lote.porcentaje_sanos.etiqueta'],
  ['categoria', 'lote.categoria.etiqueta'],
  ['usuario', 'lote.usuario_id.etiqueta'],
];

/**
 * El estado de un lote, como en el prototipo: pendiente, en ámbar y con un reloj; analizado, en verde y con un
 * chulo. El ícono acompaña al color, así el color no es la única señal (RNF-06). Si el lote sugiere otra muestra,
 * lleva al lado el chip «Sugiere otra muestra», en el tono de información y sin ícono, como en el prototipo: su texto
 * ya lo dice (D-25). Si no caben, los chips bajan a otra línea.
 *
 * @param {{ lote: import('../api/lotes.js').Lote, texto: string }} props el lote y el texto de su estado
 * @returns {import('react').ReactElement} el estado
 */
function Estado({ lote, texto }) {
  const { t } = useTranslation();
  const analizado = lote.estado === 'ANALIZADO';
  const Icono = analizado ? Check : Schedule;
  return (
    <Box sx={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '6px' }}>
      <ChipDeEstado texto={texto} tono={analizado ? 'exito' : 'advertencia'} icono={<Icono />} />
      {lote.sugerirOtraMuestra === true && (
        <ChipDeEstado texto={t('lote.sugerir_otra_muestra.etiqueta')} tono="informacion" />
      )}
    </Box>
  );
}

/**
 * La acción de un lote: «Ver», que abre el detalle de un lote analizado, para los dos roles (RF-14 C3), o
 * «Continuar», que lleva a las fotos de un lote pendiente, solo para el usuario final (RF-14 C5). Su nombre dice el
 * lote y empieza por el texto que se ve (D-32). En la tabla es un botón de texto de 40 px, como en el prototipo; en la
 * tarjeta, un botón con borde a todo el ancho, de 44 px (RNF-05).
 *
 * @param {{ lote: import('../api/lotes.js').Lote, registra: boolean, enTarjeta?: boolean }} props el lote, si la
 *   sesión es del usuario final y dónde va
 * @returns {import('react').ReactElement|null} el enlace, o nada si el lote no tiene acción para ese rol
 */
function AccionDelLote({ lote, registra, enTarjeta = false }) {
  const { t } = useTranslation();
  let accion = null;
  if (lote.estado === 'ANALIZADO') {
    accion = { ruta: `/lotes/${lote.id}`, texto: t('historial.ver'), nombre: t('historial.verLote', { codigo: lote.codigo }) };
  } else if (registra) {
    accion = {
      ruta: `/lotes/${lote.id}/fotos`, texto: t('historial.continuar'),
      nombre: t('historial.continuarLote', { codigo: lote.codigo }),
    };
  }
  if (!accion) {
    return null;
  }
  return (
    <Button component={Link} to={accion.ruta} variant={enTarjeta ? 'outlined' : 'text'} aria-label={accion.nombre}
      sx={enTarjeta
        ? { minHeight: medidas.zonaTactil, mt: 0.75 }
        : { minHeight: medidas.boton.altoEnTabla, px: '10px', textDecoration: 'underline' }}>
      {accion.texto}
    </Button>
  );
}

/**
 * Los lotes en el PC: una tabla con su título para los lectores de pantalla. En una ventana angosta, la caja de la
 * tabla se desplaza a los lados; por eso recibe el foco, con el nombre de la tabla, y se recorre con las flechas
 * del teclado (WCAG 2.1.1). La última columna tiene las acciones de cada lote.
 *
 * @param {{ lotes: import('../api/lotes.js').Lote[], region: string, registra: boolean }} props los lotes de la
 *   página, la región y si la sesión es del usuario final, que registra y retoma lotes
 * @returns {import('react').ReactElement} la tabla
 */
function Tabla({ lotes, region, registra }) {
  const { t } = useTranslation();
  const idDelTitulo = useId();
  return (
    <Box role="region" tabIndex={0} aria-labelledby={idDelTitulo}
      sx={(theme) => ({ ...recuadro(theme), overflowX: 'auto' })}>
      <Table>
        <Box component="caption" id={idDelTitulo} sx={OCULTO_A_LA_VISTA}>{t('historial.lista')}</Box>
        <TableHead>
          <TableRow>
            {COLUMNAS.map(([dato, clave]) => <TableCell key={dato}>{t(clave)}</TableCell>)}
            <TableCell align="right">{t('crud.acciones')}</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {lotes.map((lote) => {
            const datos = datosDelLote(lote, t, region);
            return (
              <TableRow key={lote.id}>
                {COLUMNAS.map(([dato]) => (
                  <TableCell key={dato} sx={dato === 'codigo' ? { fontWeight: 500 } : undefined}>
                    {dato === 'estado' ? <Estado lote={lote} texto={datos.estado} /> : datos[dato]}
                  </TableCell>
                ))}
                <TableCell align="right" sx={{ py: 1 }}>
                  <AccionDelLote lote={lote} registra={registra} />
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </Box>
  );
}

/**
 * Los lotes en el celular: una tarjeta por lote, con su código y su estado arriba (RNF-05). La de un lote analizado
 * lleva «Ver» al pie y, para el usuario final, la de un lote pendiente lleva «Continuar».
 *
 * @param {{ lotes: import('../api/lotes.js').Lote[], region: string, registra: boolean }} props los lotes de la
 *   página, la región y si la sesión es del usuario final, que registra y retoma lotes
 * @returns {import('react').ReactElement} las tarjetas
 */
function Tarjetas({ lotes, region, registra }) {
  const { t } = useTranslation();
  return (
    <Box component="ul" role="list" aria-label={t('historial.lista')}
      sx={{ listStyle: 'none', m: 0, p: 0, display: 'flex', flexDirection: 'column', gap: 1.5 }}>
      {lotes.map((lote) => {
        const datos = datosDelLote(lote, t, region);
        return (
          <Box component="li" key={lote.id} sx={(theme) => ({
            ...recuadro(theme), p: 2, display: 'flex', flexDirection: 'column', gap: 0.75,
          })}>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'center', gap: 1 }}>
              <Typography sx={{ fontWeight: 500 }}>{datos.codigo}</Typography>
              <Estado lote={lote} texto={datos.estado} />
            </Box>
            {COLUMNAS.filter(([dato]) => dato !== 'codigo' && dato !== 'estado').map(([dato, clave]) => (
              <Typography key={dato} variant="body2">
                <Box component="span" sx={{ color: 'text.secondary' }}>{t(clave)}: </Box>
                {datos[dato]}
              </Typography>
            ))}
            <AccionDelLote lote={lote} registra={registra} enTarjeta />
          </Box>
        );
      })}
    </Box>
  );
}

/**
 * El estilo de un botón de las páginas, como en el prototipo: de 44 px, con borde, y la página que se ve en el
 * color primario, que se distingue de las demás con más de 3:1 (WCAG 1.4.11). Los puntos suspensivos no son
 * botones: van sin borde y más angostos.
 *
 * @param {string} tipo el tipo del botón: page, previous, next, start-ellipsis o end-ellipsis
 * @returns {import('@mui/material/styles').SxProps<import('@mui/material/styles').Theme>} el estilo
 */
function estiloDePagina(tipo) {
  if (tipo.endsWith('ellipsis')) {
    return { minWidth: 24, m: 0 };
  }
  return (theme) => ({
    minWidth: medidas.zonaTactil, height: medidas.zonaTactil, m: 0, px: 1, borderRadius: `${medidas.boton.radio}px`,
    border: `1px solid ${theme.vars.palette.divider}`, bgcolor: 'background.paper', fontSize: letra.texto,
    ...(tipo === 'page' ? {} : { color: 'primary.main' }),
    '&.Mui-selected, &.Mui-selected:hover, &.Mui-selected.Mui-focusVisible': {
      bgcolor: 'primary.main', borderColor: 'primary.main', color: 'primary.contrastText',
      fontWeight: letra.pesos.media,
    },
  });
}

/**
 * Las páginas del historial, como en el prototipo: cuántos lotes hay y los botones de cada página. En el celular
 * muestra menos páginas a cada lado de la que se ve, y la fila de botones baja a otra línea si no cabe: ninguna
 * pantalla se desplaza a los lados desde 360 px (RNF-05 C1).
 *
 * @param {object} props las propiedades
 * @param {number} props.total cuántos lotes hay
 * @param {number} props.paginas cuántas páginas hay
 * @param {number} props.pagina la página que se ve, desde 0
 * @param {(pagina: number) => void} props.alCambiar se llama con la página elegida, desde 0
 * @returns {import('react').ReactElement} las páginas
 */
function Paginas({ total, paginas, pagina, alCambiar }) {
  const { t } = useTranslation();
  const esPc = useEsPc();
  const { items } = usePagination({
    count: Math.max(paginas, 1), page: pagina + 1, siblingCount: esPc ? 1 : 0,
    onChange: (_, elegida) => alCambiar(elegida - 1),
  });
  const nombre = { previous: t('historial.paginaAnterior'), next: t('historial.paginaSiguiente') };
  return (
    <Box component="nav" aria-label={t('historial.paginas')} sx={{
      display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: 1.5, mt: 2,
    }}>
      <Typography variant="body2" color="text.secondary">{t('historial.cuenta', { count: total })}</Typography>
      <Box component="ul" sx={{
        listStyle: 'none', m: 0, p: 0, display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '6px',
      }}>
        {items.map(({ page: numero, type: tipo, selected: seleccionada, ...item }) => (
          <li key={`${tipo}-${numero}`}>
            <PaginationItem {...item} type={tipo} page={numero} selected={seleccionada} sx={estiloDePagina(tipo)}
              aria-current={seleccionada ? 'page' : undefined}
              aria-label={nombre[tipo] ?? (tipo === 'page' ? t('historial.pagina', { pagina: numero }) : undefined)} />
          </li>
        ))}
      </Box>
    </Box>
  );
}

/**
 * Historial de lotes.
 *
 * @returns {import('react').ReactElement} la pantalla
 */
export default function Historial() {
  const { t, i18n } = useTranslation();
  const { sesion, pedir } = useSesion();
  const esPc = useEsPc();
  const titulo = useRef(null);
  // Los filtros y la página viven en la dirección: «Atrás» desde «Ver», recargar o volver a entrar los conservan.
  const [parametros, setParametros] = useSearchParams();
  const { filtros, pagina } = useMemo(() => leerDireccion(parametros), [parametros]);
  const [recargas, setRecargas] = useState(0);
  const [respuesta, setRespuesta] = useState({ pedido: null, datos: null, error: null });
  const region = idiomaDe(i18n.language).region;

  const pedido = `${direccionDe(filtros, pagina)}|${recargas}`;
  useEffect(() => {
    let vigente = true;
    historialDeLotes(pedir, pagina, filtros)
      .then((datos) => vigente && setRespuesta({ pedido, datos, error: null }))
      .catch((error) => vigente && setRespuesta({ pedido, datos: null, error }));
    return () => {
      vigente = false;
    };
  }, [pedir, pagina, filtros, pedido]);

  // Mientras llega otra página, sigue la anterior a la vista.
  const cargando = respuesta.pedido !== pedido;
  const { datos, error } = respuesta;
  const total = datos?.totalElementos ?? 0;
  const conFiltros = hayFiltros(filtros);
  // Un filtro que el backend rechazó se marca en su campo: pedir la lista otra vez daría el mismo error.
  const errorDeLosFiltros = Boolean(error && erroresPorCampo(error, NOMBRES_DE_LOS_FILTROS));
  // Solo el usuario final registra lotes (RF-04) y los retoma (RF-14 C5); la administradora solo consulta.
  const registra = sesion.usuario.rol === ROLES.USUARIO_FINAL;

  /**
   * Aplica unos filtros desde la primera página. Si son los mismos de la dirección, pide la lista otra vez.
   *
   * @param {import('../api/lotes.js').FiltrosDelHistorial} nuevos los filtros
   */
  function aplicar(nuevos) {
    const siguiente = direccionDe(nuevos, 0);
    if (siguiente.toString() === parametros.toString()) {
      setRecargas((veces) => veces + 1);
    } else {
      setParametros(siguiente);
    }
  }

  /** Quita todos los filtros y vuelve a la primera página. */
  function limpiar() {
    if (parametros.toString() !== '') {
      setParametros(new URLSearchParams());
    }
  }

  /** «Limpiar filtros», de la lista sin coincidencias: el botón desaparece, así que el foco va al título (RNF-06 C4). */
  function limpiarDesdeLaLista() {
    titulo.current?.focus();
    limpiar();
  }

  return (
    <>
      <TituloDePagina refDelTitulo={titulo} titulo={t('historial.titulo')}
        descripcion={t('historial.subtitulo', { empresa: sesion.usuario.empresa?.nombre })} />
      <FiltrosDelHistorial filtros={filtros} error={error} alAplicar={aplicar} alLimpiar={limpiar} />
      {/* El total, para los lectores de pantalla: al aplicar un filtro dice cuántos lotes quedan (WCAG 4.1.3). */}
      <Box role="status" sx={OCULTO_A_LA_VISTA}>{datos ? t('historial.encontrados', { count: total }) : ''}</Box>
      {cargando && !datos && (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress aria-label={t('crud.cargando')} />
        </Box>
      )}
      {!cargando && error && (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 1.5 }}>
          <Aviso tipo="error" titulo={textoDelError(error, t)} />
          {!errorDeLosFiltros && (
            <Button variant="outlined" onClick={() => setRecargas((veces) => veces + 1)}>{t('crud.reintentar')}</Button>
          )}
        </Box>
      )}
      {datos && total === 0 && (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 2 }}>
          <Typography>{t(conFiltros ? 'historial.sinCoincidencias' : 'historial.vacia')}</Typography>
          {conFiltros && (
            <Button variant="outlined" onClick={limpiarDesdeLaLista}>{t('historial.limpiarFiltros')}</Button>
          )}
          {!conFiltros && registra && (
            <Button component={Link} to="/lotes/nuevo" variant="contained" startIcon={<Add />}>
              {t('menu.nuevoLote')}
            </Button>
          )}
        </Box>
      )}
      {datos && total > 0 && (
        <>
          {esPc
            ? <Tabla lotes={datos.contenido} region={region} registra={registra} />
            : <Tarjetas lotes={datos.contenido} region={region} registra={registra} />}
          <Paginas total={total} paginas={datos.totalPaginas} pagina={pagina}
            alCambiar={(otra) => setParametros(direccionDe(filtros, otra))} />
        </>
      )}
    </>
  );
}
