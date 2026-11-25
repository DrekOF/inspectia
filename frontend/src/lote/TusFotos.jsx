/*
 * Nombre: TusFotos.jsx
 * Descripción: La sección «Tus fotos» de la página de las fotos, como en el prototipo: las 3 casillas de la muestra,
 *              la tercera opcional (RF-05 C2), con las fotos que ya se subieron al retomar el lote (RF-14 C5). En el
 *              PC van en una rejilla y en el celular, en 2 columnas, con la tercera debajo a todo el ancho (RNF-05).
 *              Al pie, «Analizar la muestra», que sabe qué fotos se están subiendo (RF-05 C2). Si la lista de las
 *              fotos no llega, lo dice con «Reintentar». Cada casilla pide su enlace para la muestra que la página
 *              espera que reciba las fotos (D-25). Con la revisión del servicio de IA (D-46): arriba, si la muestra
 *              volvió sin resultado, por qué y qué hacer (RF-07 C3, RF-08 C6, D-44); debajo de las casillas, cuántos
 *              granos suman las fotos y, si no llegan a 100, qué hacer, sin bloquear el envío (RF-07 C3).
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.3
 */
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import Typography from '@mui/material/Typography';
import { useCallback, useId, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { textoDelError } from '../api/errores.js';
import { Aviso } from '../componentes/Aviso.jsx';
import { useEsPc } from '../componentes/useEsPc.js';
import { AnalizarMuestra } from './AnalizarMuestra.jsx';
import { CasillaDeFoto } from './CasillaDeFoto.jsx';
import { MINIMO_DE_GRANOS, totalDeGranos } from './revision.js';

/** Los números de las fotos de una muestra: 2 o 3, la tercera opcional (RF-05 C2, D-23). */
const NUMEROS = [1, 2, 3];

/** El número de fotos de una muestra completa: con 3 ya no se puede agregar otra (RF-05 C2). */
const MAXIMO_DE_FOTOS = NUMEROS.length;

/**
 * Por qué volvió la muestra sin resultado, arriba de las casillas (D-44): con menos de 100 granos, una advertencia que
 * pide agregar la foto 3 o repetir una (RF-07 C3); sin respuesta del servicio de IA, un aviso que no interrumpe y pide
 * enviarla otra vez (RF-08 C6). Si la devolvió una foto, lo dice su casilla.
 *
 * @param {{ motivo: string|null, cuantas: number }} props el motivo de la devolución y cuántas fotos tiene la muestra
 * @returns {import('react').ReactElement|null} el aviso, o nada
 */
function MuestraDevuelta({ motivo, cuantas }) {
  const { t } = useTranslation();
  if (motivo === 'POCOS_GRANOS') {
    return <Aviso tipo="warning" titulo={t(cuantas >= MAXIMO_DE_FOTOS ? 'fotos.devuelta.POCOS_GRANOSConTres'
      : 'fotos.devuelta.POCOS_GRANOS')} />;
  }
  if (motivo === 'SIN_RESPUESTA_DE_LA_IA') {
    return <Aviso tipo="info" titulo={t('fotos.devuelta.SIN_RESPUESTA_DE_LA_IA')} />;
  }
  return null;
}

/**
 * Cuántos granos suman las fotos revisadas y, si no llegan a 100, qué hacer (RF-07 C3). Es una ayuda, no un bloqueo:
 * el mínimo lo decide la cola (D-44). Si alguna foto no tiene conteo, como una «Cargada», no dice nada.
 *
 * @param {{ fotos: import('../api/fotos.js').Foto[] }} props las fotos de la muestra
 * @returns {import('react').ReactElement|null} el total, o nada
 */
function TotalDeGranos({ fotos }) {
  const { t } = useTranslation();
  const total = totalDeGranos(fotos);
  if (total === null) {
    return null;
  }
  if (total >= MINIMO_DE_GRANOS) {
    return <Typography variant="body2" color="text.secondary">{t('fotos.totalDeGranos', { total })}</Typography>;
  }
  return (
    <Aviso tipo="warning" urgente={false}
      titulo={t(fotos.length >= MAXIMO_DE_FOTOS ? 'fotos.faltanGranosConTres' : 'fotos.faltanGranos', { total })} />
  );
}

/**
 * Lo que va debajo del título: las casillas, o lo que pasa con la lista de las fotos.
 *
 * @param {object} props las propiedades
 * @param {string} props.loteId el lote
 * @param {number} props.muestra el número de la muestra que la página espera que reciba las fotos, 1 o 2
 * @param {ReturnType<typeof import('./useFotosDelLote.js').useFotosDelLote>} props.fotos las fotos de la muestra
 * @param {() => void} props.alReintentar se llama con «Reintentar», si la lista no llegó
 * @param {(numero: number, enCurso: boolean) => void} props.alCambiarLaSubida se llama cuando la subida de una foto
 *   empieza y cuando termina
 * @returns {import('react').ReactElement} el contenido
 */
function Contenido({ loteId, muestra, fotos, alReintentar, alCambiarLaSubida }) {
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
  const subidas = Object.values(fotos.fotos);
  const deLaMuestra = { repeticiones: fotos.repeticiones, porcentajeMaxDudosos: fotos.porcentajeMaxDudosos };
  return (
    <>
      <MuestraDevuelta motivo={fotos.motivoDevolucion} cuantas={subidas.length} />
      <Box sx={esPc
        ? { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 2 }
        : { display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: '10px',
          '& > :nth-of-type(3)': { gridColumn: '1 / -1' } }}>
        {NUMEROS.map((numero) => (
          <CasillaDeFoto key={numero} loteId={loteId} muestra={muestra} numero={numero} foto={fotos.fotos[numero]}
            deLaMuestra={deLaMuestra} alConfirmar={fotos.confirmada} alFallarLaVista={fotos.vistaFallida}
            alCambiarLaSubida={alCambiarLaSubida} />
        ))}
      </Box>
      <TotalDeGranos fotos={subidas} />
    </>
  );
}

/**
 * «Tus fotos», de un lote pendiente de análisis o de la muestra 2 de uno que la sugiere.
 *
 * @param {object} props las propiedades
 * @param {string} props.loteId el lote
 * @param {number} props.muestra el número de la muestra que la página espera que reciba las fotos, 1 o 2 (D-25)
 * @param {ReturnType<typeof import('./useFotosDelLote.js').useFotosDelLote>} props.fotos las fotos de la muestra
 * @returns {import('react').ReactElement} la sección
 */
export function TusFotos({ loteId, muestra, fotos }) {
  const { t } = useTranslation();
  const idDelTitulo = useId();
  const titulo = useRef(null);
  // Los números de las fotos que se están subiendo, para que «Analizar la muestra» espere a que terminen.
  const [subiendo, setSubiendo] = useState([]);
  const alCambiarLaSubida = useCallback((numero, enCurso) => setSubiendo((antes) => {
    if (antes.includes(numero) === enCurso) {
      return antes;
    }
    return enCurso ? [...antes, numero].sort((uno, otro) => uno - otro) : antes.filter((otro) => otro !== numero);
  }), []);

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
      <Contenido loteId={loteId} muestra={muestra} fotos={fotos} alReintentar={reintentar}
        alCambiarLaSubida={alCambiarLaSubida} />
      {fotos.cargada && <AnalizarMuestra loteId={loteId} fotos={fotos} subiendo={subiendo} />}
    </Box>
  );
}
