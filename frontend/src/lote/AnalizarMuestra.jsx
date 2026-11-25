/*
 * Nombre: AnalizarMuestra.jsx
 * Descripción: «Analizar la muestra», al pie de «Tus fotos», como en el prototipo: envía la muestra a la cola del
 *              análisis y lleva al resultado del lote, donde se ve la espera (RNF-02 C3). Pide al menos 2 fotos y
 *              ninguna subida en curso (RF-05 C2). Mientras falta algo, el botón no se deshabilita, porque perdería el
 *              foco: queda marcado con aria-disabled y unido a un texto que dice qué falta (RNF-06 C3 y C4). Un doble
 *              clic envía la muestra una sola vez. Si el backend la rechaza, muestra su mensaje. Con una foto que la
 *              revisión del servicio de IA marcó «Por repetir», espera a que se repita y dice cuál (RF-06, RNF-06 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';
import { useId, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { enviarMuestra } from '../api/analisis.js';
import { textoDelError } from '../api/errores.js';
import { Aviso } from '../componentes/Aviso.jsx';
import { useEsPc } from '../componentes/useEsPc.js';
import { idiomaDe } from '../i18n/i18n.js';
import { useSesion } from '../sesion/contexto.js';
import { medidas } from '../tema/tokens.js';
import { listaDeNumeros, numerosPorRepetir } from './revision.js';

/** Las fotos que pide una muestra para analizarla; la tercera es opcional (RF-05 C2, D-23). */
export const MINIMO_DE_FOTOS = 2;

/**
 * Qué falta para poder analizar la muestra: que terminen las subidas en curso, que se repitan las fotos que la
 * revisión marcó «Por repetir» o que haya al menos 2 fotos.
 *
 * @param {import('../api/fotos.js').Foto[]} fotos las fotos ya subidas
 * @param {number[]} subiendo los números de las fotos que se están subiendo
 * @param {(clave: string, opciones?: object) => string} t la función de traducción de i18next
 * @param {string} region la región del idioma, como es-CO, para juntar los números de las fotos
 * @returns {string|null} el motivo, o null si ya se puede analizar
 */
function motivoParaEsperar(fotos, subiendo, t, region) {
  if (subiendo.length === 1) {
    return t('analizar.esperaSubida', { numero: subiendo[0] });
  }
  if (subiendo.length > 1) {
    return t('analizar.esperaSubidas');
  }
  const porRepetir = numerosPorRepetir(fotos);
  if (porRepetir.length === 1) {
    return t('analizar.repiteLaFoto', { numero: porRepetir[0] });
  }
  if (porRepetir.length > 1) {
    return t('analizar.repiteLasFotos', { numeros: listaDeNumeros(porRepetir, region) });
  }
  return fotos.length < MINIMO_DE_FOTOS ? t('analisis.fotos.minimo') : null;
}

/**
 * El mensaje de un rechazo: el del dato que el backend rechazó, como «Sube al menos 2 fotos…», o el del error.
 *
 * @param {Error} error el error, normalmente un ErrorDeApi
 * @param {(clave: string) => string} t la función de traducción de i18next
 * @returns {string} el mensaje
 */
function mensajeDelRechazo(error, t) {
  const deUnDato = (error.errores ?? []).find(({ campo }) => campo);
  return deUnDato ? deUnDato.mensaje : textoDelError(error, t);
}

/**
 * «Analizar la muestra».
 *
 * @param {object} props las propiedades
 * @param {string} props.loteId el lote
 * @param {ReturnType<typeof import('./useFotosDelLote.js').useFotosDelLote>} props.fotos las fotos de la muestra
 * @param {number[]} props.subiendo los números de las fotos que se están subiendo
 * @returns {import('react').ReactElement} el botón, con lo que falta o el error
 */
export function AnalizarMuestra({ loteId, fotos, subiendo }) {
  const { t, i18n } = useTranslation();
  const { pedir } = useSesion();
  const navegar = useNavigate();
  const esPc = useEsPc();
  const idDelMotivo = useId();
  // Si ya se está enviando: el estado tarda un render en cambiar, y un doble clic no debe enviar dos veces.
  const enCurso = useRef(false);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState(null);
  const motivo = motivoParaEsperar(Object.values(fotos.fotos), subiendo, t, idiomaDe(i18n.language).region);

  /** Envía la muestra y lleva al resultado del lote, donde se ve la espera. */
  async function analizar() {
    if (motivo || enCurso.current) {
      return;
    }
    enCurso.current = true;
    setEnviando(true);
    setError(null);
    try {
      await enviarMuestra(pedir, loteId);
      navegar(`/lotes/${loteId}`, { state: { desdeElAnalisis: true } });
      return;
    } catch (rechazo) {
      if (rechazo.estado === 409) {
        // El lote ya tiene su resultado: la página del resultado lo muestra.
        navegar(`/lotes/${loteId}`);
        return;
      }
      setError(rechazo);
      // Con menos de 2 fotos, la lista de la página no estaba al día: se pide otra vez.
      if (rechazo.estado === 400) {
        fotos.recargar();
      }
    }
    enCurso.current = false;
    setEnviando(false);
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, mt: 1 }}>
      <Box sx={{
        display: 'flex', flexDirection: esPc ? 'row' : 'column', flexWrap: 'wrap',
        alignItems: esPc ? 'center' : 'stretch', gap: esPc ? '12px 16px' : '10px',
      }}>
        <Button variant="contained" onClick={analizar} aria-disabled={motivo || enviando ? true : undefined}
          aria-describedby={motivo ? idDelMotivo : undefined}
          sx={(theme) => ({
            minHeight: medidas.boton.alto, px: '28px',
            '&[aria-disabled="true"], &[aria-disabled="true"]:hover': {
              bgcolor: theme.vars.palette.divider, color: theme.vars.palette.text.secondary,
              cursor: enviando ? 'progress' : 'not-allowed',
            },
          })}>
          {t('analizar.boton')}
        </Button>
        {motivo && (
          <Typography id={idDelMotivo} variant="body2" color="text.secondary" sx={{ lineHeight: 1.45 }}>
            {motivo}
          </Typography>
        )}
      </Box>
      {error && <Aviso tipo="error" titulo={mensajeDelRechazo(error, t)} />}
    </Box>
  );
}
