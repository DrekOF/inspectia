/*
 * Nombre: FotosDeLaMuestra.jsx
 * Descripción: La pantalla Fotos de la muestra, el paso 2 del análisis, como en el prototipo. Nombra el lote y pone la
 *              guía de captura (RF-05 C4) junto a las 3 casillas de las fotos, que se suben, se ven y se reemplazan
 *              (RF-05). Al retomar un lote pendiente muestra las que ya se subieron (RF-14 C5). Pide el lote y las
 *              fotos a la vez. Si el lote no existe o es de otra empresa, lo dice con el mensaje del backend (RNF-03);
 *              si no llegó por otra causa, ofrece «Reintentar»; y si ya no está pendiente, dice dónde ver su resultado.
 *              Si su muestra ya se envió y está en espera o en proceso, lleva al resultado del lote, donde se ve la
 *              espera (RNF-02 C3).
 * Fecha de creación: 22/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, Navigate, useParams } from 'react-router';
import { textoDelError } from '../api/errores.js';
import { verLote } from '../api/lotes.js';
import { Aviso } from '../componentes/Aviso.jsx';
import { TituloDePagina } from '../componentes/TituloDePagina.jsx';
import { useSesion } from '../sesion/contexto.js';
import { GuiaDeCaptura } from './GuiaDeCaptura.jsx';
import { PasosDelAnalisis } from './PasosDelAnalisis.jsx';
import { EN_CURSO } from './resultado.js';
import { TusFotos } from './TusFotos.jsx';
import { useFotosDelLote } from './useFotosDelLote.js';

/**
 * Las fotos de la muestra de un lote.
 *
 * @param {{ loteId: string }} props el lote
 * @returns {import('react').ReactElement} la pantalla
 */
function FotosDelLote({ loteId }) {
  const { t } = useTranslation();
  const { pedir } = useSesion();
  const fotos = useFotosDelLote(pedir, loteId);
  const titulo = useRef(null);
  const [recargas, setRecargas] = useState(0);
  const [respuesta, setRespuesta] = useState({ recarga: -1, lote: null, error: null });

  useEffect(() => {
    let vigente = true;
    verLote(pedir, loteId)
      .then((lote) => vigente && setRespuesta({ recarga: recargas, lote, error: null }))
      .catch((error) => vigente && setRespuesta({ recarga: recargas, lote: null, error }));
    return () => {
      vigente = false;
    };
  }, [pedir, loteId, recargas]);

  /** Pide otra vez el lote. Antes lleva el foco al título: «Reintentar» desaparece mientras carga (RNF-06 C4). */
  function reintentar() {
    titulo.current?.focus();
    setRecargas((veces) => veces + 1);
  }

  // Una muestra que ya se envió no recibe fotos: su espera se ve en el resultado del lote.
  if (EN_CURSO.includes(fotos.estado)) {
    return <Navigate to={`/lotes/${loteId}`} replace />;
  }

  const cargando = respuesta.recarga !== recargas;
  const { lote, error } = cargando ? { lote: null, error: null } : respuesta;
  const pendiente = lote?.estado === 'PENDIENTE';
  let descripcion;
  if (lote) {
    descripcion = t(pendiente ? 'fotos.descripcion' : 'fotos.descripcionSinSubir',
      { codigo: lote.codigo, origen: lote.origen });
  }
  return (
    <>
      {(!lote || pendiente) && <PasosDelAnalisis actual={2} />}
      <TituloDePagina titulo={t('fotos.titulo')} descripcion={descripcion} refDelTitulo={titulo} />
      {cargando && (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress aria-label={t('crud.cargando')} />
        </Box>
      )}
      {error && (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 1.5 }}>
          <Aviso tipo="error" titulo={textoDelError(error, t)} />
          {/* Un lote que no existe o es de otra empresa no aparece reintentando (RNF-03). */}
          {error.estado !== 404 && <Button variant="outlined" onClick={reintentar}>{t('crud.reintentar')}</Button>}
        </Box>
      )}
      {/* Un lote que ya no está pendiente no recibe fotos: el backend las rechazaría. */}
      {lote && !pendiente && (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 1.5 }}>
          <Aviso tipo="info" titulo={t('foto.lote_no_pendiente')} />
          <Button component={Link} to={`/lotes/${lote.id}`} variant="outlined">{t('fotos.verElResultado')}</Button>
        </Box>
      )}
      {pendiente && (
        <Box sx={{ display: 'flex', flexWrap: 'wrap', alignItems: 'flex-start', gap: 3 }}>
          <GuiaDeCaptura />
          <TusFotos loteId={lote.id} fotos={fotos} />
        </Box>
      )}
    </>
  );
}

/**
 * Fotos de la muestra.
 *
 * @returns {import('react').ReactElement} la pantalla
 */
export default function FotosDeLaMuestra() {
  const { id } = useParams();
  // Otra dirección es otro lote: la pantalla empieza de cero, sin las fotos ni las subidas del anterior.
  return <FotosDelLote key={id} loteId={id} />;
}
