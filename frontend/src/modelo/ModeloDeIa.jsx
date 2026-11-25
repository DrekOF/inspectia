/*
 * Nombre: ModeloDeIa.jsx
 * Descripción: La pantalla «Modelo de IA» del administrador de empresa (RF-17, D-14): el modelo en uso con su F1-score,
 *              su precisión y su recall macro, medidos en el conjunto de prueba; la confianza promedio de los análisis
 *              de su empresa (C2); la matriz de confusión del modelo en uso, en una tabla accesible (D-48), y la tabla
 *              comparativa de los modelos de D-18, con el modelo en uso marcado (C1). Solo la abre el administrador de
 *              empresa: el menú y las rutas la dejan fuera de los demás roles (C3, RNF-04). El prototipo la dejaba
 *              pendiente: sigue sus tarjetas, colores y medidas. El periodo de la confianza promedio es opcional:
 *              «Desde» y «Hasta», en días de Colombia como el historial, viven en la dirección; sin ellos, la confianza
 *              es de todos los análisis.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import Typography from '@mui/material/Typography';
import { Form, Formik } from 'formik';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router';
import { erroresPorCampo, textoDelError } from '../api/errores.js';
import { CAMPOS_DEL_PERIODO } from '../api/estadisticas.js';
import { metricasDelModelo } from '../api/modelo.js';
import { Aviso } from '../componentes/Aviso.jsx';
import { CampoDeTexto } from '../componentes/CampoDeTexto.jsx';
import { EnfocarPrimerError } from '../componentes/EnfocarPrimerError.jsx';
import { Tarjeta } from '../componentes/Tarjeta.jsx';
import { TituloDePagina } from '../componentes/TituloDePagina.jsx';
import { useEsPc } from '../componentes/useEsPc.js';
import { textoDelDecimal } from '../crud/decimales.js';
import { MarcarErroresDelPeriodo } from '../estadistica/MarcarErroresDelPeriodo.jsx';
import { direccionDelPeriodoOpcional, esquemaDelPeriodoOpcional, leerPeriodoOpcional } from '../estadistica/periodo.js';
import { idiomaDe } from '../i18n/i18n.js';
import { diaDelCalendario, hoyEnColombia } from '../lote/fechas.js';
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
 * El texto del periodo de la confianza, como «De los análisis terminados del 01/11/2026 al 30/11/2026.», o nada si no
 * tiene fechas.
 *
 * @param {{ desde: string, hasta: string }} periodo el periodo
 * @param {(clave: string, valores?: object) => string} t la función de traducción de i18next
 * @param {string} region la región del idioma
 * @returns {string|null} el texto
 */
function textoDelPeriodo({ desde, hasta }, t, region) {
  const dias = { desde: desde && diaDelCalendario(desde, region), hasta: hasta && diaDelCalendario(hasta, region) };
  if (desde && hasta) {
    return t('modeloIa.periodoEntre', dias);
  }
  if (desde) {
    return t('modeloIa.periodoDesde', dias);
  }
  return hasta ? t('modeloIa.periodoHasta', dias) : null;
}

/**
 * El formulario del periodo de la confianza promedio (RF-17): «Desde» y «Hasta», opcionales, con Formik y Yup y las
 * reglas del backend, «Ver confianza» y, si hay un periodo, «Todos los análisis», que lo quita. En el PC va en una
 * fila; en el celular, en columna y con los botones a todo el ancho (RNF-05).
 *
 * @param {object} props las propiedades
 * @param {{ desde: string, hasta: string }} props.periodo el periodo que se ve
 * @param {Error|null} props.error el error de la última petición, si falló
 * @param {(periodo: { desde: string, hasta: string }) => void} props.alVer se llama con el periodo de «Ver confianza»
 * @param {() => void} props.alQuitar se llama con «Todos los análisis»
 * @returns {import('react').ReactElement} el formulario
 */
function FormularioDelPeriodo({ periodo, error, alVer, alQuitar }) {
  const { t } = useTranslation();
  const esPc = useEsPc();
  const hoy = hoyEnColombia();
  const conPeriodo = Boolean(periodo.desde || periodo.hasta);
  return (
    <Formik initialValues={periodo} validationSchema={esquemaDelPeriodoOpcional(t)} onSubmit={alVer}>
      <Box component={Form} noValidate aria-label={t('modeloIa.periodo')} sx={(theme) => ({
        display: 'flex', flexDirection: esPc ? 'row' : 'column', alignItems: esPc ? 'flex-start' : 'stretch', gap: 2,
        p: esPc ? '20px 24px' : 2, mb: 3, borderRadius: `${medidas.tarjeta.radio}px`,
        border: `1px solid ${theme.vars.palette.divider}`, bgcolor: 'background.paper',
      })}>
        <EnfocarPrimerError campos={CAMPOS_DEL_PERIODO} />
        <MarcarErroresDelPeriodo error={error} />
        <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: 2, flex: 1 }}>
          <CampoDeTexto nombre="desde" etiqueta={t('estadisticas.desde')} type="date" ayuda={t('modeloIa.ayudaPeriodo')}
            inputProps={{ max: hoy }} />
          <CampoDeTexto nombre="hasta" etiqueta={t('estadisticas.hasta')} type="date" descritoPor="desde-ayuda"
            inputProps={{ max: hoy }} />
        </Box>
        {/* Alineados con los campos, debajo de su etiqueta. */}
        <Box sx={{ display: 'flex', flexDirection: esPc ? 'row' : 'column', gap: 1, mt: esPc ? '27px' : 0 }}>
          <Button type="submit" variant="contained">{t('modeloIa.verConfianza')}</Button>
          {conPeriodo && <Button variant="outlined" onClick={alQuitar}>{t('modeloIa.todosLosAnalisis')}</Button>}
        </Box>
      </Box>
    </Formik>
  );
}

/**
 * La confianza promedio de los análisis de la empresa (RF-17 C2): la cifra, de cuántos granos y muestras sale, qué
 * significa y, si se eligió, el periodo. Sin análisis, lo dice.
 *
 * @param {{ confianza: import('../api/modelo.js').ConfianzaPromedio, region: string, empresa: string,
 *   periodo: { desde: string, hasta: string } }} props la confianza, la región, el nombre de la empresa y el periodo
 * @returns {import('react').ReactElement} la tarjeta
 */
function Confianza({ confianza, region, empresa, periodo }) {
  const { t } = useTranslation();
  const esPc = useEsPc();
  const delPeriodo = textoDelPeriodo(periodo, t, region);
  return (
    <Tarjeta titulo={t('modeloIa.confianza')}>
      {delPeriodo && <Typography variant="body2" color="text.secondary">{delPeriodo}</Typography>}
      {confianza.promedio === null ? (
        <Typography>{t(delPeriodo ? 'modeloIa.sinAnalisisEnElPeriodo' : 'modeloIa.sinAnalisis')}</Typography>
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
  // El periodo de la confianza vive en la dirección: recargar o volver con «Atrás» lo conserva.
  const [parametros, setParametros] = useSearchParams();
  const { desde, hasta } = leerPeriodoOpcional(parametros);
  const [recargas, setRecargas] = useState(0);
  const [respuesta, setRespuesta] = useState({ pedido: null, datos: null, error: null });

  const pedido = `${desde}|${hasta}|${recargas}`;
  useEffect(() => {
    let vigente = true;
    metricasDelModelo(pedir, { desde, hasta })
      .then((datos) => vigente && setRespuesta({ pedido, datos, error: null }))
      // Si el backend rechazó una fecha, lo que se veía sigue a la vista y el error se marca en su campo.
      .catch((error) => vigente && setRespuesta((anterior) => ({
        pedido, datos: erroresPorCampo(error, CAMPOS_DEL_PERIODO) ? anterior.datos : null, error,
      })));
    return () => {
      vigente = false;
    };
  }, [pedir, desde, hasta, pedido]);

  // Mientras llega otro periodo, sigue el anterior a la vista.
  const cargando = respuesta.pedido !== pedido;
  const { datos, error } = respuesta;
  // Una fecha que el backend rechazó se marca en su campo: pedirla otra vez daría el mismo error.
  const errorDelPeriodo = Boolean(error && erroresPorCampo(error, CAMPOS_DEL_PERIODO));
  const enUso = datos ? modeloEnUso(datos.modelos) : null;

  /**
   * Muestra la confianza de otro periodo; si es el mismo, la pide otra vez.
   *
   * @param {{ desde: string, hasta: string }} elegido el periodo del formulario
   */
  function ver(elegido) {
    const siguiente = direccionDelPeriodoOpcional(elegido);
    if (siguiente.toString() !== direccionDelPeriodoOpcional({ desde, hasta }).toString()) {
      setParametros(siguiente);
      return;
    }
    setRecargas((veces) => veces + 1);
    if (siguiente.toString() !== parametros.toString()) {
      setParametros(siguiente, { replace: true });
    }
  }

  /** «Todos los análisis»: quita el periodo de la dirección. */
  function quitarPeriodo() {
    setParametros(new URLSearchParams());
  }

  return (
    <>
      <TituloDePagina titulo={t('modelo.etiqueta.singular')} descripcion={t('modeloIa.descripcion', { empresa })} />
      {/* La clave rehace el formulario cuando el periodo cambia en la dirección, como al volver con «Atrás». */}
      <FormularioDelPeriodo key={`${desde}|${hasta}`} periodo={{ desde, hasta }} error={error} alVer={ver}
        alQuitar={quitarPeriodo} />
      {cargando && !datos && (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress aria-label={t('crud.cargando')} />
        </Box>
      )}
      {!cargando && error && !errorDelPeriodo && (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 1.5 }}>
          <Aviso tipo="error" titulo={textoDelError(error, t)} />
          <Button variant="outlined" onClick={() => setRecargas((veces) => veces + 1)}>{t('crud.reintentar')}</Button>
        </Box>
      )}
      {datos && (!error || errorDelPeriodo) && (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' }, gap: 3 }}>
            <ModeloEnUso modelo={enUso} region={region} />
            <Confianza confianza={datos.confianza} region={region} empresa={empresa} periodo={{ desde, hasta }} />
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
