/*
 * Nombre: EsperaDelAnalisis.jsx
 * Descripción: El panel de la espera del análisis, en la página del resultado (paso 3), para no sumar pantallas al
 *              flujo (RNF-06 C2): dice si la muestra está en espera, en la cola, o en proceso (RNF-02 C3), con una
 *              barra de avance con su nombre (RNF-01 C4). No dice la posición en la cola, que es de todas las empresas
 *              (RNF-03). A los 15 segundos, el máximo de RNF-01 C2, avisa que tarda más de lo normal y que el
 *              resultado queda guardado. Al mostrarse lleva el foco a su título (RNF-06 C4). Si la consulta del avance
 *              falla varias veces, lo dice con «Reintentar». La de la segunda muestra lleva su número en el título
 *              (D-25).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import Autorenew from '@mui/icons-material/Autorenew';
import Schedule from '@mui/icons-material/Schedule';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import LinearProgress from '@mui/material/LinearProgress';
import Typography from '@mui/material/Typography';
import { useEffect, useId, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { textoDelError } from '../api/errores.js';
import { Aviso } from '../componentes/Aviso.jsx';
import { ChipDeEstado } from '../componentes/ChipDeEstado.jsx';
import { useEnfocarAlMostrar } from '../componentes/useEnfocarAlMostrar.js';
import { useEsPc } from '../componentes/useEsPc.js';
import { medidas } from '../tema/tokens.js';

/** Desde cuándo la espera tarda más de lo normal, en milisegundos: ningún análisis debe pasar de 15 s (RNF-01 C2). */
export const TARDA_MAS_DE = 15_000;

/**
 * La espera del análisis.
 *
 * @param {object} props las propiedades
 * @param {'EN_ESPERA'|'EN_PROCESO'} props.estado el estado de la muestra
 * @param {Error|null} props.error el error con el que se dejó de consultar el avance, si se dejó
 * @param {() => void} props.alReintentar se llama con «Reintentar», para consultar otra vez
 * @param {number} [props.muestra] el número de la muestra; desde la segunda, el título lo dice
 * @returns {import('react').ReactElement} el panel
 */
export function EsperaDelAnalisis({ estado, error, alReintentar, muestra = 1 }) {
  const { t } = useTranslation();
  const esPc = useEsPc();
  const idDelTitulo = useId();
  const titulo = useEnfocarAlMostrar();
  const [tarda, setTarda] = useState(false);

  useEffect(() => {
    const temporizador = setTimeout(() => setTarda(true), TARDA_MAS_DE);
    return () => clearTimeout(temporizador);
  }, []);

  /** Consulta otra vez. Antes lleva el foco al título: «Reintentar» desaparece al pulsarlo (RNF-06 C4). */
  function reintentar() {
    titulo.current?.focus();
    alReintentar();
  }

  const enProceso = estado === 'EN_PROCESO';
  const Icono = enProceso ? Autorenew : Schedule;
  return (
    <Box component="section" aria-labelledby={idDelTitulo} sx={(theme) => ({
      display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 2, p: esPc ? '28px' : '20px',
      borderRadius: `${medidas.tarjeta.radio}px`, border: `1px solid ${theme.vars.palette.divider}`,
      bgcolor: 'background.paper',
    })}>
      <Typography variant="h2" id={idDelTitulo} ref={titulo} tabIndex={-1} sx={{ outline: 'none' }}>
        {muestra > 1 ? t('espera.tituloDeLaMuestra', { numero: muestra }) : t('espera.titulo')}
      </Typography>
      {error ? (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 1.5, alignSelf: 'stretch' }}>
          <Aviso tipo="error" titulo={textoDelError(error, t)} />
          {/* Un lote que ya no existe no aparece reintentando (RNF-03). */}
          {error.estado !== 404 && <Button variant="outlined" onClick={reintentar}>{t('crud.reintentar')}</Button>}
        </Box>
      ) : (
        <>
          <ChipDeEstado texto={t(`muestra.estado.${estado}`)} tono={enProceso ? 'informacion' : 'advertencia'}
            icono={<Icono />} />
          <LinearProgress aria-label={t('espera.avance')} sx={{ alignSelf: 'stretch', height: 6, borderRadius: '3px' }} />
          <Typography>{t(`espera.${estado}`)}</Typography>
        </>
      )}
      {/* El aviso de que tarda llega después: va en un role="status" que ya estaba, así se anuncia. */}
      <Box role="status" sx={{ alignSelf: 'stretch' }}>
        {tarda && !error && <Typography color="text.secondary">{t('espera.tarda')}</Typography>}
      </Box>
      {tarda && !error && (
        <Button component={Link} to="/historial" variant="outlined"
          sx={{ alignSelf: esPc ? 'flex-start' : 'stretch', minHeight: medidas.zonaTactil }}>
          {t('espera.irAlHistorial')}
        </Button>
      )}
    </Box>
  );
}
