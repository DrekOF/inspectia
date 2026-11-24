/*
 * Nombre: ResultadoDelLote.jsx
 * Descripción: La pantalla Resultado del lote, /lotes/:id: el paso 3 del análisis y el detalle del lote desde el
 *              historial, para el usuario final y la administradora (RF-14 C3). Con el lote analizado muestra su
 *              resultado guardado: la cifra con su rango probable (RF-10, D-43), los granos que contó el modelo, la
 *              categoría probable con su recomendación (RF-12), la sugerencia de otra muestra, si la hay, con el límite
 *              guardado y «Analizar otra muestra» para el usuario final (D-24, D-47, RF-14 C6), y las fotos marcadas
 *              (RF-11), con las cifras de cada muestra si son dos. Con la muestra en la cola o en proceso muestra la
 *              espera, consulta su avance y, al terminar, el resultado, con el foco en el título (RNF-01 C4, RNF-02
 *              C3); también si el resultado llegó con la muestra ya terminada y el lote todavía pendiente. Mientras la
 *              segunda muestra espera o se analiza, la espera va arriba y debajo el resultado de la primera, con el
 *              aviso de que cambiará (D-25). Si el análisis no terminó, lo dice y lleva a las fotos; si fue el de la
 *              segunda muestra, junto al resultado de la primera; si el lote no tiene resultado, lo dice. Los pasos
 *              del análisis solo se ven al llegar desde «Analizar la muestra» o mientras espera. Si el lote no existe
 *              o es de otra empresa, lo dice sin «Reintentar» (RNF-03); si no llegó por otra causa, ofrece
 *              «Reintentar».
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import Add from '@mui/icons-material/Add';
import InfoOutlined from '@mui/icons-material/InfoOutlined';
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
import { SugerenciaDeOtraMuestra } from './SugerenciaDeOtraMuestra.jsx';
import { useAnalisisEnCurso } from './useAnalisisEnCurso.js';

/** Los errores que dicen que el lote no está: no existe, es de otra empresa o su id no es válido (RNF-03). */
const SIN_EL_LOTE = [400, 404];

/** Las vistas que muestran la espera del análisis y consultan su avance. */
const ESPERAS = ['espera', 'esperaConResultado'];

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
 * Un análisis que no terminó: la muestra volvió de la cola sin resultado y quedó pendiente otra vez (RNF-06 C3). Al
 * usuario final lo lleva a las fotos, para revisarlas y enviarla de nuevo; a la administradora le dice quién puede.
 *
 * @param {object} props las propiedades
 * @param {string} props.titulo lo que pasó
 * @param {string} props.loteId el lote
 * @param {boolean} props.esUsuarioFinal si la sesión es del usuario final, el único que sube fotos
 * @returns {import('react').ReactElement} el aviso
 */
function AnalisisSinTerminar({ titulo, loteId, esUsuarioFinal }) {
  const { t } = useTranslation();
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 1.5 }}>
      <Aviso tipo="error" titulo={titulo}>
        {esUsuarioFinal ? t('espera.falloQueHacer') : t('espera.falloAdmin')}
      </Aviso>
      {esUsuarioFinal && (
        <Button component={Link} to={`/lotes/${loteId}/fotos`} variant="contained">
          {t('espera.volverALasFotos')}
        </Button>
      )}
    </Box>
  );
}

/**
 * El aviso, debajo de la espera de la segunda muestra, de que el resultado que se ve es el de la primera y cambiará
 * (D-25). Es una nota que se lee en su lugar, no un anuncio: lo que se anuncia es el estado de la espera.
 *
 * @param {{ numero: number }} props el número de la muestra que se espera
 * @returns {import('react').ReactElement} la nota
 */
function ResultadoQueCambiara({ numero }) {
  const { t } = useTranslation();
  return (
    <Box role="note" sx={(theme) => ({
      display: 'flex', alignItems: 'flex-start', gap: 1, px: 2, py: 1.5, borderRadius: `${medidas.boton.radio}px`,
      border: `1px solid ${theme.vars.palette.cacao.informacion.principal}`,
      bgcolor: theme.vars.palette.cacao.informacion.fondo, color: theme.vars.palette.cacao.informacion.texto,
    })}>
      <InfoOutlined aria-hidden sx={(theme) => ({ fontSize: 20, mt: '2px', color: theme.vars.palette.cacao.informacion.principal })} />
      <Typography variant="body2" sx={{ fontSize: 15, color: 'inherit' }}>{t('resultado.cambiara', { numero })}</Typography>
    </Box>
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
  const muestra = datos ? muestraEnCurso(datos.muestras) : null;
  // La muestra en curso se sigue mientras espera o se analiza, también la segunda con el lote ya analizado (D-25). Con
  // la muestra terminada y el lote pendiente, el resultado se leyó mientras se guardaba: el avance dirá TERMINADA y el
  // resultado se pide otra vez, en lugar de quedarse cargando.
  const enCurso = EN_CURSO.includes(muestra?.estado)
    || (datos?.lote.estado === 'PENDIENTE' && muestra?.estado === 'TERMINADA');
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

  // Si una imagen no carga, lo normal es que su enlace venció: el resultado trae enlaces nuevos. Una vez por cada
  // imagen que falla después de que otra cargó, para no pedirlo sin fin si la foto no está.
  const vistaFallida = useCallback(() => {
    if (!recargoPorLaVista.current) {
      recargoPorLaVista.current = true;
      setRecargas((veces) => veces + 1);
    }
  }, []);
  // Una imagen que carga dice que los enlaces sirven: si otro vence más tarde, se vuelve a pedir el resultado.
  const vistaCargada = useCallback(() => {
    recargoPorLaVista.current = false;
  }, []);

  /** Pide otra vez el resultado. Antes lleva el foco al título: «Reintentar» desaparece mientras carga (RNF-06 C4). */
  function reintentar() {
    titulo.current?.focus();
    setRecargas((veces) => veces + 1);
  }

  const estadoDeLaEspera = analisis.avance?.estado ?? muestra?.estado;
  // Lo que se anuncia a los lectores de pantalla: solo cambia cuando cambia el estado, no con cada consulta.
  let anuncio = '';
  if (ESPERAS.includes(vista)) {
    anuncio = t(`espera.${estadoDeLaEspera}`);
  } else if (vista === 'resultado' && termino) {
    anuncio = t('resultado.listo', { codigo: lote.codigo });
  }
  const conPasos = desdeElAnalisis || termino || ESPERAS.includes(vista);
  const conAcciones = esUsuarioFinal && vista === 'resultado';
  // La segunda muestra empezada y sin enviar se retoma desde el aviso de la sugerencia (RF-14 C6).
  const muestraEmpezada = muestra?.numero > 1 && muestra.estado === 'PENDIENTE' ? muestra.numero : null;
  const espera = (
    <EsperaDelAnalisis estado={estadoDeLaEspera} muestra={muestra?.numero} error={analisis.error}
      alReintentar={analisis.reintentar} />
  );
  const fotosMarcadas = datos && (
    <FotosMarcadas muestras={datos.muestras} alFallarLaVista={vistaFallida} alCargarLaVista={vistaCargada} />
  );

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
      {vista === 'espera' && espera}
      {vista === 'fallo' && (
        <AnalisisSinTerminar titulo={t('espera.fallo')} loteId={loteId} esUsuarioFinal={esUsuarioFinal} />
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
            <SugerenciaDeOtraMuestra lote={lote} region={region} esUsuarioFinal={esUsuarioFinal}
              muestraEmpezada={muestraEmpezada} />
          )}
          {fotosMarcadas}
          {conAcciones && !esPc && <NuevoLote esPc={false} />}
        </Box>
      )}
      {/* La segunda muestra en la cola o en proceso: su espera arriba y, debajo, el resultado de la primera (D-25). */}
      {vista === 'esperaConResultado' && (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: esPc ? 3 : 2 }}>
          {espera}
          <ResultadoQueCambiara numero={muestra.numero} />
          <ResumenDelResultado lote={lote} region={region} />
          {fotosMarcadas}
        </Box>
      )}
      {/* La segunda muestra volvió sin resultado: el aviso arriba y el resultado de la primera debajo. */}
      {vista === 'resultadoConFallo' && (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: esPc ? 3 : 2 }}>
          <AnalisisSinTerminar titulo={t('espera.falloDeLaMuestra', { numero: muestra.numero })} loteId={loteId}
            esUsuarioFinal={esUsuarioFinal} />
          <ResumenDelResultado lote={lote} region={region} />
          {fotosMarcadas}
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
