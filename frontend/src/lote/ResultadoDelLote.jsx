/*
 * Nombre: ResultadoDelLote.jsx
 * Descripción: La pantalla Resultado del lote, /lotes/:id: el paso 3 del análisis y el detalle del lote desde el
 *              historial, para el usuario final y la administradora (RF-14 C3). Con el lote analizado muestra su
 *              resultado guardado: la cifra con su rango probable (RF-10, D-43), los granos que contó el modelo, la
 *              categoría probable con su recomendación (RF-12), la sugerencia de otra muestra, si la hay (D-24), y las
 *              fotos marcadas (RF-11). Con la muestra en la cola o en proceso muestra la espera, consulta su avance y,
 *              al terminar, el resultado, con el foco en el título (RNF-01 C4, RNF-02 C3). Si el análisis no terminó,
 *              lo dice y lleva a las fotos; si el lote no tiene resultado, lo dice. Los pasos del análisis solo se ven
 *              al llegar desde «Analizar la muestra» o mientras espera. Si el lote no existe o es de otra empresa, lo
 *              dice sin «Reintentar» (RNF-03); si no llegó por otra causa, ofrece «Reintentar».
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Add from '@mui/icons-material/Add';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import Typography from '@mui/material/Typography';
import { useCallback, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useLocation, useParams } from 'react-router';
import { resultadoDelLote } from '../api/analisis.js';
import { textoDelError } from '../api/errores.js';
import { Aviso } from '../componentes/Aviso.jsx';
import { OCULTO_A_LA_VISTA } from '../componentes/ocultoALaVista.js';
import { TituloDePagina } from '../componentes/TituloDePagina.jsx';
import { useEsPc } from '../componentes/useEsPc.js';
import { idiomaDe } from '../i18n/i18n.js';
import { ROLES } from '../navegacion/menu.js';
import { useSesion } from '../sesion/contexto.js';
import { medidas } from '../tema/tokens.js';
import { EsperaDelAnalisis } from './EsperaDelAnalisis.jsx';
import { diaDelCalendario, diaEnColombia, horaEnColombia } from './fechas.js';
import { FotosMarcadas } from './FotosMarcadas.jsx';
import { PasosDelAnalisis } from './PasosDelAnalisis.jsx';
import { EN_CURSO, muestraEnCurso, vistaDelLote } from './resultado.js';
import { ResumenDelResultado } from './ResumenDelResultado.jsx';
import { useAnalisisEnCurso } from './useAnalisisEnCurso.js';

/** Los errores que dicen que el lote no está: no existe, es de otra empresa o su id no es válido (RNF-03). */
const SIN_EL_LOTE = [400, 404];

/**
 * La descripción debajo del título: la finca o el proveedor, la fecha de recepción y, si ya se analizó, la fecha y la
 * hora del resultado en Colombia.
 *
 * @param {import('../api/analisis.js').LoteConResultado} lote el lote
 * @param {(clave: string, opciones?: object) => string} t la función de traducción de i18next
 * @param {string} region la región del idioma, como es-CO
 * @returns {string} la descripción
 */
function descripcionDel(lote, t, region) {
  const datos = { origen: lote.origen, recepcion: diaDelCalendario(lote.fechaRecepcion, region) };
  if (!lote.fechaResultado) {
    return t('resultado.datos', datos);
  }
  return t('resultado.datosAnalizado', {
    ...datos, dia: diaEnColombia(lote.fechaResultado, region), hora: horaEnColombia(lote.fechaResultado, region),
  });
}

/**
 * «Nuevo lote», para el usuario final, como en el prototipo.
 *
 * @param {{ esPc: boolean }} props si se ve en el PC; en el celular, va a todo el ancho
 * @returns {import('react').ReactElement} el botón
 */
function NuevoLote({ esPc }) {
  const { t } = useTranslation();
  return (
    <Button component={Link} to="/lotes/nuevo" variant="contained" startIcon={<Add />}
      sx={{ minHeight: medidas.boton.alto, alignSelf: esPc ? 'auto' : 'stretch' }}>
      {t('menu.nuevoLote')}
    </Button>
  );
}

/**
 * El resultado de un lote.
 *
 * @param {{ loteId: string, desdeElAnalisis: boolean }} props el lote y si se llegó desde «Analizar la muestra»
 * @returns {import('react').ReactElement} la pantalla
 */
function ResultadoDe({ loteId, desdeElAnalisis }) {
  const { t, i18n } = useTranslation();
  const { sesion, pedir } = useSesion();
  const esPc = useEsPc();
  const region = idiomaDe(i18n.language).region;
  // Solo el usuario final registra lotes, sube fotos y envía muestras (RF-04, RF-05, RF-14 restricción 4).
  const esUsuarioFinal = sesion.usuario.rol === ROLES.USUARIO_FINAL;
  const titulo = useRef(null);
  const recargoPorLaVista = useRef(false);
  const [recargas, setRecargas] = useState(0);
  const [respuesta, setRespuesta] = useState({ recarga: -1, datos: null, error: null });
  // Si el análisis terminó mientras se esperaba en esta página.
  const [termino, setTermino] = useState(false);

  useEffect(() => {
    let vigente = true;
    resultadoDelLote(pedir, loteId)
      .then((datos) => vigente && setRespuesta({ recarga: recargas, datos, error: null }))
      .catch((error) => vigente && setRespuesta({ recarga: recargas, datos: null, error }));
    return () => {
      vigente = false;
    };
  }, [pedir, loteId, recargas]);

  // Mientras llega otra vez el resultado, sigue a la vista el anterior.
  const cargando = respuesta.recarga !== recargas;
  const { datos, error } = respuesta;
  const enCurso = datos?.lote.estado === 'PENDIENTE' && EN_CURSO.includes(muestraEnCurso(datos.muestras)?.estado);
  const analisis = useAnalisisEnCurso(pedir, loteId, {
    activo: Boolean(enCurso),
    alTerminar: () => {
      setTermino(true);
      setRecargas((veces) => veces + 1);
    },
  });
  const vista = datos ? vistaDelLote(datos, analisis.avance) : null;
  const lote = datos?.lote;

  // Al llegar el resultado después de esperarlo, o desde «Analizar la muestra», el foco va al título: el botón que lo
  // tenía ya no está (RNF-06 C4).
  useEffect(() => {
    if (vista === 'resultado' && (termino || desdeElAnalisis)) {
      titulo.current?.focus();
    }
  }, [vista, termino, desdeElAnalisis]);

  // Si una imagen no carga, lo normal es que su enlace venció: el resultado trae enlaces nuevos. Una sola vez, para
  // no pedirlo sin fin si la foto no está.
  const vistaFallida = useCallback(() => {
    if (!recargoPorLaVista.current) {
      recargoPorLaVista.current = true;
      setRecargas((veces) => veces + 1);
    }
  }, []);

  /** Pide otra vez el resultado. Antes lleva el foco al título: «Reintentar» desaparece mientras carga (RNF-06 C4). */
  function reintentar() {
    titulo.current?.focus();
    setRecargas((veces) => veces + 1);
  }

  // Lo que se anuncia a los lectores de pantalla: solo cambia cuando cambia el estado, no con cada consulta.
  let anuncio = '';
  if (vista === 'espera') {
    anuncio = t(`espera.${analisis.avance?.estado ?? muestraEnCurso(datos.muestras).estado}`);
  } else if (vista === 'resultado' && termino) {
    anuncio = t('resultado.listo', { codigo: lote.codigo });
  }
  const conPasos = desdeElAnalisis || termino || vista === 'espera';
  const conAcciones = esUsuarioFinal && vista === 'resultado';

  return (
    <>
      {conPasos && <PasosDelAnalisis actual={3} />}
      <Box sx={{ display: 'flex', flexWrap: 'wrap', alignItems: 'flex-end', justifyContent: 'space-between', columnGap: 2 }}>
        <Box sx={{ flex: '1 1 320px', minWidth: 0 }}>
          <TituloDePagina refDelTitulo={titulo}
            titulo={lote ? t('resultado.titulo', { codigo: lote.codigo }) : t('resultado.tituloSinLote')}
            descripcion={lote ? descripcionDel(lote, t, region) : undefined} />
        </Box>
        {conAcciones && esPc && <Box sx={{ mb: 3 }}><NuevoLote esPc /></Box>}
      </Box>
      <Box role="status" sx={OCULTO_A_LA_VISTA}>{anuncio}</Box>
      {((cargando && !datos) || vista === 'cargando') && (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress aria-label={t('crud.cargando')} />
        </Box>
      )}
      {error && !cargando && (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 1.5 }}>
          <Aviso tipo="error" titulo={textoDelError(error, t)} />
          {!SIN_EL_LOTE.includes(error.estado) && (
            <Button variant="outlined" onClick={reintentar}>{t('crud.reintentar')}</Button>
          )}
        </Box>
      )}
      {vista === 'espera' && (
        <EsperaDelAnalisis estado={analisis.avance?.estado ?? muestraEnCurso(datos.muestras).estado}
          error={analisis.error} alReintentar={analisis.reintentar} />
      )}
      {vista === 'fallo' && (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 1.5 }}>
          <Aviso tipo="error" titulo={t('espera.fallo')}>
            {esUsuarioFinal ? t('espera.falloQueHacer') : t('espera.falloAdmin')}
          </Aviso>
          {esUsuarioFinal && (
            <Button component={Link} to={`/lotes/${loteId}/fotos`} variant="contained">
              {t('espera.volverALasFotos')}
            </Button>
          )}
        </Box>
      )}
      {vista === 'sinResultado' && (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 2 }}>
          <Typography>{t('resultado.sinResultado')}</Typography>
          {esUsuarioFinal && (
            <Button component={Link} to={`/lotes/${loteId}/fotos`} variant="contained"
              aria-label={t('historial.continuarLote', { codigo: lote.codigo })}>
              {t('historial.continuar')}
            </Button>
          )}
        </Box>
      )}
      {vista === 'resultado' && (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: esPc ? 3 : 2 }}>
          <ResumenDelResultado lote={lote} region={region} />
          {lote.sugerirOtraMuestra && (
            <Aviso tipo="info" urgente={false} titulo={t('resultado.sugerencia')}>{t('resultado.sugerenciaDetalle')}</Aviso>
          )}
          <FotosMarcadas muestras={datos.muestras} alFallarLaVista={vistaFallida} />
          {conAcciones && !esPc && <NuevoLote esPc={false} />}
        </Box>
      )}
    </>
  );
}

/**
 * Resultado del lote.
 *
 * @returns {import('react').ReactElement} la pantalla
 */
export default function ResultadoDelLote() {
  const { id } = useParams();
  const { state } = useLocation();
  // Otra dirección es otro lote: la pantalla empieza de cero, sin la espera ni el resultado del anterior.
  return <ResultadoDe key={id} loteId={id} desdeElAnalisis={Boolean(state?.desdeElAnalisis)} />;
}
