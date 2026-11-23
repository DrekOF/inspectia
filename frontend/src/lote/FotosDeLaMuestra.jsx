/*
 * Nombre: FotosDeLaMuestra.jsx
 * Descripción: La pantalla Fotos de la muestra, el paso 2 del análisis, como en el prototipo. Nombra el lote y pone la
 *              guía de captura (RF-05 C4) junto a las 3 casillas de las fotos, que se suben, se ven y se reemplazan
 *              (RF-05). Al retomar un lote pendiente muestra las que ya se subieron (RF-14 C5). Pide el lote y las
 *              fotos a la vez. Si el lote no existe o es de otra empresa, lo dice con el mensaje del backend (RNF-03).
 * Fecha de creación: 22/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import Box from '@mui/material/Box';
import CircularProgress from '@mui/material/CircularProgress';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router';
import { textoDelError } from '../api/errores.js';
import { verLote } from '../api/lotes.js';
import { Aviso } from '../componentes/Aviso.jsx';
import { TituloDePagina } from '../componentes/TituloDePagina.jsx';
import { useSesion } from '../sesion/contexto.js';
import { GuiaDeCaptura } from './GuiaDeCaptura.jsx';
import { PasosDelAnalisis } from './PasosDelAnalisis.jsx';
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
  const [respuesta, setRespuesta] = useState({ lote: null, error: null });

  useEffect(() => {
    let vigente = true;
    verLote(pedir, loteId)
      .then((lote) => vigente && setRespuesta({ lote, error: null }))
      .catch((error) => vigente && setRespuesta({ lote: null, error }));
    return () => {
      vigente = false;
    };
  }, [pedir, loteId]);

  const { lote, error } = respuesta;
  return (
    <>
      <PasosDelAnalisis actual={2} />
      <TituloDePagina titulo={t('fotos.titulo')}
        descripcion={lote ? t('fotos.descripcion', { codigo: lote.codigo, origen: lote.origen }) : undefined} />
      {!lote && !error && (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress aria-label={t('crud.cargando')} />
        </Box>
      )}
      {error && <Aviso tipo="error" titulo={textoDelError(error, t)} />}
      {lote && (
        <Box sx={{ display: 'flex', flexWrap: 'wrap', alignItems: 'flex-start', gap: 3 }}>
          <GuiaDeCaptura />
          <TusFotos lote={lote} fotos={fotos} />
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
