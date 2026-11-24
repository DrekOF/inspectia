/*
 * Nombre: ModeloDeIa.jsx
 * Descripción: La pantalla «Modelo de IA» del administrador de empresa (RF-17, D-14): el modelo en uso con su F1-score,
 *              su precisión y su recall macro, medidos en el conjunto de prueba; la confianza promedio de los análisis
 *              de su empresa (C2); la matriz de confusión del modelo en uso, en una tabla accesible (D-48), y la tabla
 *              comparativa de los modelos de D-18, con el modelo en uso marcado (C1). Solo la abre el administrador de
 *              empresa: el menú y las rutas la dejan fuera de los demás roles (C3, RNF-04). El prototipo la dejaba
 *              pendiente: sigue sus tarjetas, colores y medidas.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import Typography from '@mui/material/Typography';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { textoDelError } from '../api/errores.js';
import { metricasDelModelo } from '../api/modelo.js';
import { Aviso } from '../componentes/Aviso.jsx';
import { Tarjeta } from '../componentes/Tarjeta.jsx';
import { TituloDePagina } from '../componentes/TituloDePagina.jsx';
import { useEsPc } from '../componentes/useEsPc.js';
import { textoDelDecimal } from '../crud/decimales.js';
import { idiomaDe } from '../i18n/i18n.js';
import { useSesion } from '../sesion/contexto.js';
import { letra, medidas } from '../tema/tokens.js';
import { ComparacionDeModelos } from './ComparacionDeModelos.jsx';
import { MatrizDeConfusion } from './MatrizDeConfusion.jsx';
import { granosDePrueba, modeloEnUso, textoDeMetrica } from './metricas.js';

/** Las métricas macro del modelo en uso, con la clave de su nombre (D-14). */
const METRICAS_MACRO = [
  ['f1Macro', 'modeloIa.f1Macro'],
  ['precisionMacro', 'modeloIa.precisionMacro'],
  ['recallMacro', 'modeloIa.recallMacro'],
];

/**
 * Una métrica del modelo en uso: su nombre arriba y su valor en porcentaje, en un recuadro como los conteos del
 * resultado.
 *
 * @param {{ nombre: string, valor: string }} props el nombre y el valor
 * @returns {import('react').ReactElement} la métrica
 */
function Metrica({ nombre, valor }) {
  return (
    <Box sx={{ p: '12px', borderRadius: `${medidas.boton.radio}px`, bgcolor: 'background.default', minWidth: 0 }}>
      <Box component="dt" sx={{ fontSize: letra.chip, color: 'text.secondary' }}>{nombre}</Box>
      <Box component="dd" sx={{ m: 0, mt: 0.5, fontSize: 22, fontWeight: letra.pesos.media }}>{valor}</Box>
    </Box>
  );
}

/**
 * El modelo en uso: su nombre, su versión y su tipo, y su F1-score, su precisión y su recall macro, medidos con los
 * granos del conjunto de prueba. Si ningún modelo está en uso, lo dice.
 *
 * @param {{ modelo: import('../api/modelo.js').ModeloMedido|null, region: string }} props el modelo y la región
 * @returns {import('react').ReactElement} la tarjeta
 */
function ModeloEnUso({ modelo, region }) {
  const { t } = useTranslation();
  if (!modelo) {
    return (
      <Tarjeta titulo={t('modeloIa.enUso')}>
        <Aviso tipo="info" titulo={t('modeloIa.sinModeloEnUso')}>{t('modeloIa.sinModeloEnUsoDetalle')}</Aviso>
      </Tarjeta>
    );
  }
  const total = granosDePrueba(modelo.matriz);
  return (
    <Tarjeta titulo={t('modeloIa.enUso')}>
      <Box>
        <Typography sx={{ fontSize: letra.subtitulo, fontWeight: letra.pesos.media }}>{modelo.nombre}</Typography>
        <Typography color="text.secondary">
          {modelo.tipo
            ? t('modeloIa.versionYTipo', { version: modelo.version, tipo: t(`modelo.tipo.${modelo.tipo}`) })
            : t('modeloIa.version', { version: modelo.version })}
        </Typography>
      </Box>
      <Box component="dl" sx={{
        m: 0, display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))', gap: 1.5,
      }}>
        {METRICAS_MACRO.map(([dato, clave]) => (
          <Metrica key={dato} nombre={t(clave)} valor={`${textoDeMetrica(modelo[dato], region)}\u00a0%`} />
        ))}
      </Box>
      {total !== null && (
        <Typography variant="body2" color="text.secondary">{t('modeloIa.medidas', { total })}</Typography>
      )}
    </Tarjeta>
  );
}

/**
 * La confianza promedio de los análisis de la empresa (RF-17 C2): la cifra, de cuántos granos y muestras sale, y qué
 * significa. Sin análisis, lo dice.
 *
 * @param {{ confianza: import('../api/modelo.js').ConfianzaPromedio, region: string, empresa: string }} props la
 *   confianza, la región y el nombre de la empresa
 * @returns {import('react').ReactElement} la tarjeta
 */
function Confianza({ confianza, region, empresa }) {
  const { t } = useTranslation();
  const esPc = useEsPc();
  return (
    <Tarjeta titulo={t('modeloIa.confianza')}>
      {confianza.promedio === null ? (
        <Typography>{t('modeloIa.sinAnalisis')}</Typography>
      ) : (
        <>
          <Typography component="p" sx={{
            fontSize: esPc ? letra.cifra.pc : letra.cifra.celular, lineHeight: 1, fontWeight: letra.pesos.negrita,
            letterSpacing: '-0.01em',
          }}>
            {`${textoDelDecimal(confianza.promedio, 2, region)}\u00a0%`}
          </Typography>
          <Typography>
            {t('modeloIa.confianzaDe', {
              granos: t('modeloIa.granos', { count: confianza.granos }),
              muestras: t('modeloIa.muestras', { count: confianza.muestras }),
              empresa,
            })}
          </Typography>
          <Typography variant="body2" color="text.secondary">{t('modeloIa.confianzaQueEs')}</Typography>
        </>
      )}
    </Tarjeta>
  );
}

/**
 * Modelo de IA.
 *
 * @returns {import('react').ReactElement} la pantalla
 */
export default function ModeloDeIa() {
  const { t, i18n } = useTranslation();
  const { sesion, pedir } = useSesion();
  const esPc = useEsPc();
  const region = idiomaDe(i18n.language).region;
  const empresa = sesion.usuario.empresa?.nombre;
  const [recargas, setRecargas] = useState(0);
  const [respuesta, setRespuesta] = useState({ pedido: null, datos: null, error: null });

  useEffect(() => {
    let vigente = true;
    metricasDelModelo(pedir)
      .then((datos) => vigente && setRespuesta({ pedido: recargas, datos, error: null }))
      .catch((error) => vigente && setRespuesta({ pedido: recargas, datos: null, error }));
    return () => {
      vigente = false;
    };
  }, [pedir, recargas]);

  const cargando = respuesta.pedido !== recargas;
  const { datos, error } = respuesta;
  const enUso = datos ? modeloEnUso(datos.modelos) : null;

  return (
    <>
      <TituloDePagina titulo={t('modelo.etiqueta.singular')} descripcion={t('modeloIa.descripcion', { empresa })} />
      {cargando && (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress aria-label={t('crud.cargando')} />
        </Box>
      )}
      {!cargando && error && (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 1.5 }}>
          <Aviso tipo="error" titulo={textoDelError(error, t)} />
          <Button variant="outlined" onClick={() => setRecargas((veces) => veces + 1)}>{t('crud.reintentar')}</Button>
        </Box>
      )}
      {!cargando && datos && (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' }, gap: 3 }}>
            <ModeloEnUso modelo={enUso} region={region} />
            <Confianza confianza={datos.confianza} region={region} empresa={empresa} />
          </Box>
          {enUso?.matriz && (
            <Tarjeta titulo={t('modeloIa.matriz')}>
              <Typography color="text.secondary">{t('modeloIa.matrizDescripcion')}</Typography>
              <MatrizDeConfusion modelo={enUso} />
            </Tarjeta>
          )}
          <Tarjeta titulo={t('modeloIa.comparacion')}>
            <Typography color="text.secondary">{t('modeloIa.comparacionDescripcion')}</Typography>
            <ComparacionDeModelos modelos={datos.modelos} region={region} esPc={esPc} />
          </Tarjeta>
        </Box>
      )}
    </>
  );
}
