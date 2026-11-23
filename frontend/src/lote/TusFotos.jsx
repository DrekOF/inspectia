/*
 * Nombre: TusFotos.jsx
 * Descripción: La sección «Tus fotos» de la página de las fotos, como en el prototipo: las 3 casillas de la muestra,
 *              la tercera opcional (RF-05 C2), con las fotos que ya se subieron al retomar el lote (RF-14 C5). En el
 *              PC van en una rejilla y en el celular, en 2 columnas, con la tercera debajo a todo el ancho (RNF-05).
 *              Si la lista de las fotos no llega, lo dice con «Reintentar».
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import Typography from '@mui/material/Typography';
import { useId, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { textoDelError } from '../api/errores.js';
import { Aviso } from '../componentes/Aviso.jsx';
import { useEsPc } from '../componentes/useEsPc.js';
import { CasillaDeFoto } from './CasillaDeFoto.jsx';

/** Los números de las fotos de una muestra: 2 o 3, la tercera opcional (RF-05 C2, D-23). */
const NUMEROS = [1, 2, 3];

/**
 * Lo que va debajo del título: las casillas, o lo que pasa con la lista de las fotos.
 *
 * @param {object} props las propiedades
 * @param {string} props.loteId el lote
 * @param {ReturnType<typeof import('./useFotosDelLote.js').useFotosDelLote>} props.fotos las fotos de la muestra
 * @param {() => void} props.alReintentar se llama con «Reintentar», si la lista no llegó
 * @returns {import('react').ReactElement} el contenido
 */
function Contenido({ loteId, fotos, alReintentar }) {
  const { t } = useTranslation();
  const esPc = useEsPc();
  if (!fotos.cargada && fotos.cargando) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
        <CircularProgress aria-label={t('crud.cargando')} />
      </Box>
    );
  }
  if (!fotos.cargada) {
    return (
      <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 1.5 }}>
        <Aviso tipo="error" titulo={textoDelError(fotos.error, t)} />
        <Button variant="outlined" onClick={alReintentar}>{t('crud.reintentar')}</Button>
      </Box>
    );
  }
  return (
    <Box sx={esPc
      ? { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 2 }
      : { display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: '10px',
        '& > :nth-of-type(3)': { gridColumn: '1 / -1' } }}>
      {NUMEROS.map((numero) => (
        <CasillaDeFoto key={numero} loteId={loteId} numero={numero} foto={fotos.fotos[numero]}
          alConfirmar={fotos.confirmada} alFallarLaVista={fotos.vistaFallida} />
      ))}
    </Box>
  );
}

/**
 * «Tus fotos», de un lote pendiente de análisis.
 *
 * @param {object} props las propiedades
 * @param {string} props.loteId el lote
 * @param {ReturnType<typeof import('./useFotosDelLote.js').useFotosDelLote>} props.fotos las fotos de la muestra
 * @returns {import('react').ReactElement} la sección
 */
export function TusFotos({ loteId, fotos }) {
  const { t } = useTranslation();
  const idDelTitulo = useId();
  const titulo = useRef(null);

  /** Pide otra vez la lista. Antes lleva el foco al título: «Reintentar» desaparece mientras carga (RNF-06 C4). */
  function reintentar() {
    titulo.current?.focus();
    fotos.recargar();
  }

  return (
    <Box component="section" aria-labelledby={idDelTitulo}
      sx={{ flex: '2 1 520px', minWidth: 0, display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Typography variant="h2" id={idDelTitulo} ref={titulo} tabIndex={-1} sx={{ outline: 'none' }}>
        {t('fotos.tusFotos')}
      </Typography>
      <Contenido loteId={loteId} fotos={fotos} alReintentar={reintentar} />
    </Box>
  );
}
