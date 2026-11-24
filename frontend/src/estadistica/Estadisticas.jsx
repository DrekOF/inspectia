/*
 * Nombre: Estadisticas.jsx
 * Descripción: La pantalla Estadísticas del administrador de empresa (RF-16): el periodo, por la fecha del resultado
 *              y de los últimos 12 meses al abrirla, y los lotes analizados de la empresa en él, en cifras y gráficas:
 *              lotes analizados, promedio de granos sanos, lotes por categoría, evolución del promedio por mes y
 *              promedio por finca o proveedor. Cada gráfica va con su tabla (RNF-06), sin librerías nuevas: las barras
 *              van dentro de las tablas y la evolución por mes es un SVG. Sin lotes en el periodo, un mensaje en lugar
 *              de gráficas vacías (C2). El periodo vive en la dirección, así recargar o volver lo conserva.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import { Form, Formik, useFormikContext } from 'formik';
import { useEffect, useEffectEvent, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router';
import { erroresPorCampo, textoDelError } from '../api/errores.js';
import { CAMPOS_DEL_PERIODO, estadisticasDeLaEmpresa } from '../api/estadisticas.js';
import { Aviso } from '../componentes/Aviso.jsx';
import { CampoDeTexto } from '../componentes/CampoDeTexto.jsx';
import { EnfocarPrimerError } from '../componentes/EnfocarPrimerError.jsx';
import { OCULTO_A_LA_VISTA } from '../componentes/ocultoALaVista.js';
import { Tarjeta } from '../componentes/Tarjeta.jsx';
import { TituloDePagina } from '../componentes/TituloDePagina.jsx';
import { useEsPc } from '../componentes/useEsPc.js';
import { textoDelDecimal } from '../crud/decimales.js';
import { idiomaDe } from '../i18n/i18n.js';
import { hoyEnColombia } from '../lote/fechas.js';
import { useSesion } from '../sesion/contexto.js';
import { letra, medidas } from '../tema/tokens.js';
import { GraficaPorMes } from './GraficaPorMes.jsx';
import { direccionDelPeriodo, esquemaDelPeriodo, leerPeriodo } from './periodo.js';

/**
 * Un porcentaje con 1 decimal y el signo, como «90,4 %».
 *
 * @param {number} valor el porcentaje
 * @param {string} region la región del idioma
 * @returns {string} el texto
 */
function porcentaje(valor, region) {
  return `${textoDelDecimal(valor, 1, region)} %`;
}

/**
 * Un mes (aaaa-mm) completo, como «septiembre de 2026» o «September 2026».
 *
 * @param {string} mes el mes
 * @param {string} region la región del idioma
 * @returns {string} el mes
 */
function mesLargo(mes, region) {
  const [anio, numero] = mes.split('-').map(Number);
  return new Intl.DateTimeFormat(region, { month: 'long', year: 'numeric', timeZone: 'UTC' })
    .format(new Date(Date.UTC(anio, numero - 1, 1)));
}

/**
 * Marca en su campo las fechas que el backend rechazó, con su mensaje, y lleva el foco a la primera (RNF-06 C3 y C4).
 *
 * @param {{ error: Error|null }} props el error de la última petición, si falló
 * @returns {null} no muestra nada
 */
function MarcarErroresDelServidor({ error }) {
  const { setErrors, setTouched } = useFormikContext();
  const marcar = useEffectEvent((deLosCampos) => {
    // Sin validar: la validación de Yup los aceptó y borraría los del backend.
    setTouched(Object.fromEntries(Object.keys(deLosCampos).map((nombre) => [nombre, true])), false);
    setErrors(deLosCampos);
    document.getElementById(CAMPOS_DEL_PERIODO.find((nombre) => deLosCampos[nombre]))?.focus();
  });

  useEffect(() => {
    const deLosCampos = error ? erroresPorCampo(error, CAMPOS_DEL_PERIODO) : null;
    if (deLosCampos) {
      marcar(deLosCampos);
    }
  }, [error]);
  return null;
}

/**
 * El formulario del periodo: «Desde» y «Hasta», con Formik y Yup y las reglas del backend. En el PC va en una fila; en
 * el celular, en columna y con el botón a todo el ancho (RNF-05).
 *
 * @param {object} props las propiedades
 * @param {{ desde: string, hasta: string }} props.periodo el periodo que se ve
 * @param {Error|null} props.error el error de la última petición, si falló
 * @param {(periodo: { desde: string, hasta: string }) => void} props.alVer se llama con el periodo de «Ver estadísticas»
 * @returns {import('react').ReactElement} el formulario
 */
function FormularioDelPeriodo({ periodo, error, alVer }) {
  const { t } = useTranslation();
  const esPc = useEsPc();
  const hoy = hoyEnColombia();
  return (
    <Formik initialValues={periodo} validationSchema={esquemaDelPeriodo(t)} onSubmit={alVer}>
      <Box component={Form} noValidate aria-label={t('estadisticas.periodo')} sx={(theme) => ({
        display: 'flex', flexDirection: esPc ? 'row' : 'column', alignItems: esPc ? 'flex-start' : 'stretch', gap: 2,
        p: esPc ? '20px 24px' : 2, mb: 3, borderRadius: `${medidas.tarjeta.radio}px`,
        border: `1px solid ${theme.vars.palette.divider}`, bgcolor: 'background.paper',
      })}>
        <EnfocarPrimerError campos={CAMPOS_DEL_PERIODO} />
        <MarcarErroresDelServidor error={error} />
        <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: 2, flex: 1 }}>
          <CampoDeTexto nombre="desde" etiqueta={t('estadisticas.desde')} type="date" ayuda={t('estadisticas.ayudaPeriodo')}
            inputProps={{ max: hoy }} />
          <CampoDeTexto nombre="hasta" etiqueta={t('estadisticas.hasta')} type="date" descritoPor="desde-ayuda"
            inputProps={{ max: hoy }} />
        </Box>
        {/* Alineado con los campos, debajo de su etiqueta. */}
        <Button type="submit" variant="contained" sx={esPc ? { mt: '27px' } : undefined}>
          {t('estadisticas.ver')}
        </Button>
      </Box>
    </Formik>
  );
}

/**
 * Las cifras de arriba: lotes analizados y promedio de granos sanos, como nombre y valor (dl).
 *
 * @param {{ datos: import('../api/estadisticas.js').Estadisticas, region: string }} props las estadísticas y la región
 * @returns {import('react').ReactElement} las cifras
 */
function Cifras({ datos, region }) {
  const { t } = useTranslation();
  const esPc = useEsPc();
  const cifras = [
    [t('estadisticas.lotesAnalizados'), String(datos.lotesAnalizados)],
    [t('estadisticas.promedio'), porcentaje(datos.promedioSanos, region)],
  ];
  return (
    <Box component="dl" sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 2, m: 0 }}>
      {cifras.map(([nombre, valor]) => (
        <Box key={nombre} sx={(theme) => ({
          p: esPc ? `${medidas.tarjeta.relleno}px 28px` : 2.5, borderRadius: `${medidas.tarjeta.radio}px`,
          border: `1px solid ${theme.vars.palette.divider}`, bgcolor: 'background.paper',
        })}>
          <Typography component="dt" color="text.secondary">{nombre}</Typography>
          <Typography component="dd" sx={{
            m: 0, mt: 0.5, fontSize: esPc ? letra.titulo.pc + 10 : letra.titulo.celular + 8,
            fontWeight: letra.pesos.negrita, lineHeight: 1.2,
          }}>
            {valor}
          </Typography>
        </Box>
      ))}
    </Box>
  );
}

/**
 * Una barra dentro de una celda, de 0 a 100 %, junto a su valor escrito. Es un dibujo: el valor ya lo dice (RNF-06).
 *
 * @param {{ fraccion: number, texto: string }} props cuánto de la barra se llena, de 0 a 1, y el valor
 * @returns {import('react').ReactElement} la barra con su valor
 */
function BarraConValor({ fraccion, texto }) {
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
      <Box aria-hidden="true" sx={(theme) => ({
        flex: 1, minWidth: 32, maxWidth: 280, height: 10, borderRadius: '5px', bgcolor: theme.vars.palette.cacao.lineaFila,
      })}>
        <Box sx={{ width: `${Math.min(Math.max(fraccion, 0), 1) * 100}%`, height: '100%', borderRadius: '5px', bgcolor: 'primary.main' }} />
      </Box>
      <Box component="span" sx={{ minWidth: '4.5ch', textAlign: 'right', fontVariantNumeric: 'tabular-nums' }}>{texto}</Box>
    </Box>
  );
}

/**
 * Una tabla de las estadísticas, con su nombre. En una ventana angosta, su caja se desplaza a los lados; por eso recibe
 * el foco, con el nombre de la tabla, y se recorre con las flechas del teclado (WCAG 2.1.1), como la del historial.
 *
 * @param {object} props las propiedades
 * @param {string} props.nombre el nombre de la tabla
 * @param {string[]} props.columnas los títulos de las columnas
 * @param {Array<{ clave: string, celdas: import('react').ReactNode[] }>} props.filas las filas
 * @returns {import('react').ReactElement} la tabla
 */
function TablaDeDatos({ nombre, columnas, filas }) {
  const esPc = useEsPc();
  const celda = esPc ? undefined : { px: 1 };
  return (
    <Box role="region" tabIndex={0} aria-label={nombre} sx={{ overflowX: 'auto' }}>
      <Table aria-label={nombre}>
        <TableHead>
          <TableRow>
            {columnas.map((titulo) => <TableCell key={titulo} sx={celda}>{titulo}</TableCell>)}
          </TableRow>
        </TableHead>
        <TableBody>
          {filas.map(({ clave, celdas }) => (
            <TableRow key={clave}>
              {celdas.map((contenido, indice) => (
                <TableCell key={indice} sx={{ ...celda, ...(indice === 0 ? { fontWeight: 500 } : {}) }}>{contenido}</TableCell>
              ))}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Box>
  );
}

/**
 * Las cifras y las gráficas de un periodo con lotes.
 *
 * @param {{ datos: import('../api/estadisticas.js').Estadisticas, region: string }} props las estadísticas y la región
 * @returns {import('react').ReactElement} las secciones
 */
function Indicadores({ datos, region }) {
  const { t } = useTranslation();
  const esPc = useEsPc();
  const total = datos.lotesAnalizados;
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
      <Cifras datos={datos} region={region} />
      <Tarjeta titulo={t('estadisticas.porCategoria')}>
        <TablaDeDatos nombre={t('estadisticas.porCategoria')}
          columnas={[t('lote.categoria.etiqueta'), t('estadisticas.lotes'), t('estadisticas.parteDelTotal')]}
          filas={datos.porCategoria.map(({ categoria, lotes }) => ({
            clave: categoria,
            celdas: [
              t(`rango_recomendacion.categoria.${categoria}`),
              <BarraConValor key="lotes" fraccion={lotes / total} texto={String(lotes)} />,
              porcentaje((lotes / total) * 100, region),
            ],
          }))} />
        <Typography variant="body2" color="text.secondary">{t('estadisticas.notaCategoria')}</Typography>
      </Tarjeta>
      <Tarjeta titulo={t('estadisticas.porMes')}>
        <GraficaPorMes meses={datos.porMes} region={region} horizontal={!esPc} />
        <TablaDeDatos nombre={t('estadisticas.porMes')}
          columnas={[t('estadisticas.mes'), t('estadisticas.lotes'), t('estadisticas.promedio')]}
          filas={datos.porMes.map(({ mes, lotes, promedioSanos }) => ({
            clave: mes,
            celdas: [mesLargo(mes, region), String(lotes),
              promedioSanos === null ? t('estadisticas.sinLotesEnElMes') : porcentaje(promedioSanos, region)],
          }))} />
      </Tarjeta>
      <Tarjeta titulo={t('estadisticas.porOrigen')}>
        <TablaDeDatos nombre={t('estadisticas.porOrigen')}
          columnas={[t('lote.origen.etiqueta'), t('estadisticas.promedio'), t('estadisticas.lotes')]}
          filas={datos.porOrigen.map(({ origen, lotes, promedioSanos }) => ({
            clave: origen,
            celdas: [
              origen,
              <BarraConValor key="promedio" fraccion={promedioSanos / 100} texto={porcentaje(promedioSanos, region)} />,
              String(lotes),
            ],
          }))} />
      </Tarjeta>
    </Box>
  );
}

/**
 * Estadísticas.
 *
 * @returns {import('react').ReactElement} la pantalla
 */
export default function Estadisticas() {
  const { t, i18n } = useTranslation();
  const { sesion, pedir } = useSesion();
  const [parametros, setParametros] = useSearchParams();
  const { desde, hasta } = leerPeriodo(parametros, hoyEnColombia());
  const [recargas, setRecargas] = useState(0);
  const [respuesta, setRespuesta] = useState({ pedido: null, datos: null, error: null });
  const region = idiomaDe(i18n.language).region;

  const pedido = `${desde}|${hasta}|${recargas}`;
  useEffect(() => {
    let vigente = true;
    estadisticasDeLaEmpresa(pedir, { desde, hasta })
      .then((datos) => vigente && setRespuesta({ pedido, datos, error: null }))
      .catch((error) => vigente && setRespuesta({ pedido, datos: null, error }));
    return () => {
      vigente = false;
    };
  }, [pedir, desde, hasta, pedido]);

  const cargando = respuesta.pedido !== pedido;
  const { datos, error } = respuesta;
  // Una fecha que el backend rechazó se marca en su campo: pedirlas otra vez daría el mismo error.
  const errorDelPeriodo = Boolean(error && erroresPorCampo(error, CAMPOS_DEL_PERIODO));

  /**
   * Muestra las estadísticas de otro periodo; si es el mismo, las pide otra vez.
   *
   * @param {{ desde: string, hasta: string }} elegido el periodo del formulario
   */
  function ver(elegido) {
    const siguiente = direccionDelPeriodo(elegido);
    if (siguiente.toString() !== direccionDelPeriodo({ desde, hasta }).toString()) {
      setParametros(siguiente);
      return;
    }
    setRecargas((veces) => veces + 1);
    // La dirección sin periodo, o escrita de otra forma, queda con el que se ve, sin otra entrada en el historial.
    if (siguiente.toString() !== parametros.toString()) {
      setParametros(siguiente, { replace: true });
    }
  }

  return (
    <>
      <TituloDePagina titulo={t('estadisticas.titulo')}
        descripcion={t('estadisticas.subtitulo', { empresa: sesion.usuario.empresa?.nombre })} />
      {/* La clave rehace el formulario cuando el periodo cambia en la dirección, como al volver con «Atrás». */}
      <FormularioDelPeriodo key={`${desde}|${hasta}`} periodo={{ desde, hasta }} error={error} alVer={ver} />
      {/* Cuántos lotes hay en el periodo, para los lectores de pantalla (WCAG 4.1.3): queda vacío mientras llegan, así
          cada respuesta se anuncia, también si no cambió. */}
      <Box role="status" sx={OCULTO_A_LA_VISTA}>
        {datos && !cargando ? t('estadisticas.encontrados', { count: datos.lotesAnalizados }) : ''}
      </Box>
      {cargando && !datos && (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress aria-label={t('crud.cargando')} />
        </Box>
      )}
      {!cargando && error && (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 1.5 }}>
          <Aviso tipo="error" titulo={textoDelError(error, t)} />
          {!errorDelPeriodo && (
            <Button variant="outlined" onClick={() => setRecargas((veces) => veces + 1)}>{t('crud.reintentar')}</Button>
          )}
        </Box>
      )}
      {datos && datos.sinLotes && <Typography>{t('estadisticas.sinLotes')}</Typography>}
      {datos && !datos.sinLotes && <Indicadores datos={datos} region={region} />}
    </>
  );
}
