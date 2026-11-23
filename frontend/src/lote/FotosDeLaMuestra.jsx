/*
 * Nombre: FotosDeLaMuestra.jsx
 * Descripción: La pantalla Fotos de la muestra, el paso 2 del análisis, como en el prototipo. Nombra el lote y
 *              muestra la guía de captura (RF-04 C1, RF-05 C4); las fotos se suben desde el PR de RF-05. Si el
 *              lote no existe o es de otra empresa, lo dice con el mensaje del backend (RNF-03).
 * Fecha de creación: 22/11/2026
 * Actualización: 22/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Box from '@mui/material/Box';
import CircularProgress from '@mui/material/CircularProgress';
import Typography from '@mui/material/Typography';
import { useEffect, useId, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router';
import { textoDelError } from '../api/errores.js';
import { verLote } from '../api/lotes.js';
import { Aviso } from '../componentes/Aviso.jsx';
import { TituloDePagina } from '../componentes/TituloDePagina.jsx';
import { useSesion } from '../sesion/contexto.js';
import { GuiaDeCaptura } from './GuiaDeCaptura.jsx';
import { PasosDelAnalisis } from './PasosDelAnalisis.jsx';

/**
 * Fotos de la muestra.
 *
 * @returns {import('react').ReactElement} la pantalla
 */
export default function FotosDeLaMuestra() {
  const { t } = useTranslation();
  const { pedir } = useSesion();
  const { id } = useParams();
  const idDeTusFotos = useId();
  const [respuesta, setRespuesta] = useState({ id: null, lote: null, error: null });

  useEffect(() => {
    let vigente = true;
    verLote(pedir, id)
      .then((lote) => vigente && setRespuesta({ id, lote, error: null }))
      .catch((error) => vigente && setRespuesta({ id, lote: null, error }));
    return () => {
      vigente = false;
    };
  }, [pedir, id]);

  const cargando = respuesta.id !== id;
  const { lote, error } = cargando ? { lote: null, error: null } : respuesta;

  return (
    <>
      <PasosDelAnalisis actual={2} />
      <TituloDePagina titulo={t('fotos.titulo')}
        descripcion={lote ? t('fotos.descripcion', { codigo: lote.codigo, origen: lote.origen }) : undefined} />
      {cargando && (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress aria-label={t('crud.cargando')} />
        </Box>
      )}
      {error && <Aviso tipo="error" titulo={textoDelError(error, t)} />}
      {lote && (
        <>
          <GuiaDeCaptura />
          <Box component="section" aria-labelledby={idDeTusFotos}
            sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            <Typography variant="h2" id={idDeTusFotos}>{t('fotos.tusFotos')}</Typography>
            <Typography color="text.secondary">{t('fotos.pronto')}</Typography>
          </Box>
        </>
      )}
    </>
  );
}
