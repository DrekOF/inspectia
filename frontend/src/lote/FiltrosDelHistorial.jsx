/*
 * Nombre: FiltrosDelHistorial.jsx
 * Descripción: Los filtros del Historial de lotes (RF-14 C2), como en el prototipo: el formulario «Filtros del
 *              historial» con la fecha del resultado desde y hasta, el estado, la categoría, el código del lote y
 *              quién lo registró, de la lista que da el backend, también con los desactivados. Usa Formik y Yup, con
 *              las reglas del backend, y marca en su campo los errores de los dos (RNF-06 C3). Los filtros se aplican
 *              con «Aplicar filtros» y «Limpiar» los quita todos; los aplicados viven en la dirección de la página y el
 *              formulario los sigue, también al volver con «Atrás». En el PC se ve siempre; en el celular va detrás
 *              del botón «Filtros», abierto si hay alguno puesto (RNF-05).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import ExpandLess from '@mui/icons-material/ExpandLess';
import ExpandMore from '@mui/icons-material/ExpandMore';
import FilterList from '@mui/icons-material/FilterList';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import FormControl from '@mui/material/FormControl';
import FormLabel from '@mui/material/FormLabel';
import NativeSelect from '@mui/material/NativeSelect';
import OutlinedInput from '@mui/material/OutlinedInput';
import { Form, Formik, useField, useFormikContext } from 'formik';
import { useEffect, useEffectEvent, useId, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { erroresPorCampo } from '../api/errores.js';
import { NOMBRES_DE_LOS_FILTROS, registradoresDeLotes } from '../api/lotes.js';
import { CampoDeTexto } from '../componentes/CampoDeTexto.jsx';
import { EnfocarPrimerError } from '../componentes/EnfocarPrimerError.jsx';
import { idsQueDescriben, MensajesDelCampo } from '../componentes/MensajesDelCampo.jsx';
import { useEsPc } from '../componentes/useEsPc.js';
import { useSesion } from '../sesion/contexto.js';
import { medidas } from '../tema/tokens.js';
import { hoyEnColombia } from './fechas.js';
import {
  CATEGORIAS, direccionDe, ESTADOS_DEL_LOTE, esquemaDeFiltros, filtrosParaAplicar, hayFiltros, SIN_FILTROS,
} from './filtros.js';

/**
 * Una lista de un filtro: nativa del navegador, que funciona igual con el teclado, con el dedo y con los lectores de
 * pantalla (RNF-06). Su primera opción, «Todos» o «Todas», no filtra y siempre está.
 *
 * @param {object} props las propiedades
 * @param {string} props.nombre el nombre del campo en Formik, el mismo del filtro en la API; también es su id
 * @param {string} props.etiqueta su etiqueta
 * @param {string} props.todas el texto de la opción que no filtra, como «Todos»
 * @param {Array<{ valor: string, etiqueta: string }>} props.opciones los valores que se pueden elegir
 * @param {string} [props.ayuda] la ayuda que va debajo
 * @returns {import('react').ReactElement} el campo
 */
function CampoDeFiltro({ nombre, etiqueta, todas, opciones, ayuda }) {
  const [campo, meta] = useField(nombre);
  const error = meta.touched && meta.error ? meta.error : null;
  return (
    <FormControl fullWidth error={Boolean(error)}>
      <FormLabel htmlFor={nombre}>{etiqueta}</FormLabel>
      <NativeSelect {...campo} input={<OutlinedInput />}
        inputProps={{ id: nombre, 'aria-describedby': idsQueDescriben(nombre, { error, ayuda }) }}>
        <option value="">{todas}</option>
        {opciones.map((opcion) => <option key={opcion.valor} value={opcion.valor}>{opcion.etiqueta}</option>)}
      </NativeSelect>
      <MensajesDelCampo nombre={nombre} error={error} ayuda={ayuda} />
    </FormControl>
  );
}

/**
 * Hace que el formulario siga los filtros aplicados: cuando cambian en la dirección, como al aplicarlos, al
 * limpiarlos o al volver con «Atrás», el formulario los muestra, sin errores. Conserva la cuenta de los envíos, así
 * el foco sigue yendo al primer error del siguiente (RNF-06 C4).
 *
 * @param {{ filtros: import('../api/lotes.js').FiltrosDelHistorial }} props los filtros aplicados
 * @returns {null} no muestra nada
 */
function SeguirLosFiltrosAplicados({ filtros }) {
  const { resetForm, submitCount } = useFormikContext();
  const clave = direccionDe(filtros, 0).toString();
  const seguidos = useRef(clave);
  const seguir = useEffectEvent(() => resetForm({ values: filtros, submitCount }));

  useEffect(() => {
    if (seguidos.current !== clave) {
      seguidos.current = clave;
      seguir();
    }
  }, [clave]);
  return null;
}

/**
 * Marca en su campo los filtros que el backend rechazó, con su mensaje, y lleva el foco al primero (RNF-06 C3 y C4).
 * La lista llega después del envío, así que no los marca la validación de Formik.
 *
 * @param {{ error: Error|null }} props el error de la última lista, si falló
 * @returns {null} no muestra nada
 */
function MarcarErroresDelServidor({ error }) {
  const { setErrors, setTouched } = useFormikContext();
  const marcar = useEffectEvent((deLosCampos) => {
    // Sin validar: la validación de Yup los aceptó y borraría los del backend.
    setTouched(Object.fromEntries(Object.keys(deLosCampos).map((nombre) => [nombre, true])), false);
    setErrors(deLosCampos);
    document.getElementById(NOMBRES_DE_LOS_FILTROS.find((nombre) => deLosCampos[nombre]))?.focus();
  });

  useEffect(() => {
    const deLosCampos = error ? erroresPorCampo(error, NOMBRES_DE_LOS_FILTROS) : null;
    if (deLosCampos) {
      marcar(deLosCampos);
    }
  }, [error]);
  return null;
}

/**
 * El formulario de los filtros.
 *
 * @param {object} props las propiedades
 * @param {import('../api/lotes.js').FiltrosDelHistorial} props.filtros los filtros aplicados
 * @param {{ lista: { id: string, nombre: string }[], error: Error|null }} props.registradores quienes registraron
 *   lotes, para la lista «Usuario», o el error si no llegaron
 * @param {Error|null} props.error el error de la última lista, si falló
 * @param {(filtros: import('../api/lotes.js').FiltrosDelHistorial) => void} props.alAplicar se llama con los filtros
 *   de «Aplicar filtros»
 * @param {() => void} props.alLimpiar se llama con «Limpiar»
 * @returns {import('react').ReactElement} el formulario
 */
function FormularioDeFiltros({ filtros, registradores, error, alAplicar, alLimpiar }) {
  const { t } = useTranslation();
  const esPc = useEsPc();
  const hoy = hoyEnColombia();
  const opcionesDe = (codigos, prefijo) => codigos.map((codigo) => ({ valor: codigo, etiqueta: t(`${prefijo}.${codigo}`) }));

  return (
    <Formik initialValues={filtros} validationSchema={esquemaDeFiltros(t)}
      onSubmit={(valores) => alAplicar(filtrosParaAplicar(valores))}>
      {({ resetForm, submitCount }) => (
        <Box component={Form} noValidate aria-label={t('historial.filtros')} sx={(theme) => ({
          display: 'flex', flexDirection: 'column', gap: 2, p: esPc ? '20px 24px' : 2,
          borderRadius: `${medidas.tarjeta.radio}px`, border: `1px solid ${theme.vars.palette.divider}`,
          bgcolor: 'background.paper',
        })}>
          <EnfocarPrimerError campos={NOMBRES_DE_LOS_FILTROS} />
          <SeguirLosFiltrosAplicados filtros={filtros} />
          <MarcarErroresDelServidor error={error} />
          <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: 2, alignItems: 'start' }}>
            {/* Un lote pendiente no tiene fecha del resultado: la ayuda lo dice, y describe las dos fechas. */}
            <CampoDeTexto nombre="desde" etiqueta={t('historial.desde')} type="date" ayuda={t('historial.ayudaFechas')}
              inputProps={{ max: hoy }} />
            <CampoDeTexto nombre="hasta" etiqueta={t('historial.hasta')} type="date" descritoPor="desde-ayuda"
              inputProps={{ max: hoy }} />
            <CampoDeFiltro nombre="estado" etiqueta={t('lote.estado.etiqueta')} todas={t('historial.todos')}
              opciones={opcionesDe(ESTADOS_DEL_LOTE, 'lote.estado')} />
            <CampoDeFiltro nombre="categoria" etiqueta={t('rango_recomendacion.categoria.etiqueta')}
              todas={t('historial.todas')} opciones={opcionesDe(CATEGORIAS, 'rango_recomendacion.categoria')} />
            <CampoDeTexto nombre="codigo" etiqueta={t('lote.codigo.etiqueta')} type="search"
              placeholder={t('historial.ejemploCodigo')} autoComplete="off" />
            <CampoDeFiltro nombre="usuario" etiqueta={t('historial.usuario')} todas={t('historial.todos')}
              opciones={registradores.lista.map(({ id, nombre }) => ({ valor: id, etiqueta: nombre }))}
              ayuda={registradores.error ? t('historial.usuariosSinCargar') : undefined} />
          </Box>
          {/* En el celular, en columna y sin flexWrap: con «wrap», Safari encoge el botón a su texto al tocarlo y el toque
              que cae fuera del texto no lo pulsa. */}
          <Box sx={{ display: 'flex', flexDirection: esPc ? 'row' : 'column', flexWrap: esPc ? 'wrap' : 'nowrap', gap: 1.5 }}>
            <Button type="submit" variant="contained">{t('historial.aplicarFiltros')}</Button>
            {/* No es type="reset": el formulario volvería a los filtros aplicados, y «Limpiar» los quita. */}
            <Button type="button" variant="text" onClick={() => {
              resetForm({ values: SIN_FILTROS, submitCount });
              alLimpiar();
            }}>
              {t('historial.limpiar')}
            </Button>
          </Box>
        </Box>
      )}
    </Formik>
  );
}

/**
 * Los filtros del historial.
 *
 * @param {object} props las propiedades
 * @param {import('../api/lotes.js').FiltrosDelHistorial} props.filtros los filtros aplicados, de la dirección
 * @param {Error|null} props.error el error de la última lista, si falló: los de un filtro se marcan en su campo
 * @param {(filtros: import('../api/lotes.js').FiltrosDelHistorial) => void} props.alAplicar se llama con los filtros
 *   de «Aplicar filtros»
 * @param {() => void} props.alLimpiar se llama con «Limpiar»
 * @returns {import('react').ReactElement} los filtros
 */
export function FiltrosDelHistorial({ filtros, error, alAplicar, alLimpiar }) {
  const { t } = useTranslation();
  const { pedir } = useSesion();
  const esPc = useEsPc();
  const idDelPanel = useId();
  // En el celular, los filtros se abren si la dirección trae alguno, al entrar o al volver con «Atrás», así se ve qué
  // filtra la lista.
  const conFiltros = hayFiltros(filtros);
  const [abiertos, setAbiertos] = useState(conFiltros);
  const [conFiltrosAntes, setConFiltrosAntes] = useState(conFiltros);
  if (conFiltros !== conFiltrosAntes) {
    setConFiltrosAntes(conFiltros);
    if (conFiltros) {
      setAbiertos(true);
    }
  }
  const [registradores, setRegistradores] = useState({ lista: [], error: null });

  // La lista «Usuario» se pide una vez, aunque el formulario se cierre y se abra en el celular.
  useEffect(() => {
    let vigente = true;
    registradoresDeLotes(pedir)
      .then((lista) => vigente && setRegistradores({ lista, error: null }))
      .catch((rechazo) => vigente && setRegistradores({ lista: [], error: rechazo }));
    return () => {
      vigente = false;
    };
  }, [pedir]);

  const formulario = (
    <FormularioDeFiltros filtros={filtros} registradores={registradores} error={error} alAplicar={alAplicar}
      alLimpiar={alLimpiar} />
  );
  if (esPc) {
    return <Box sx={{ mb: 3 }}>{formulario}</Box>;
  }
  return (
    <Box sx={{ mb: 2, display: 'flex', flexDirection: 'column', gap: 1.5 }}>
      <Button variant="outlined" aria-expanded={abiertos} aria-controls={idDelPanel}
        startIcon={<FilterList />} endIcon={abiertos ? <ExpandLess /> : <ExpandMore />}
        onClick={() => setAbiertos((antes) => !antes)}>
        {t('historial.mostrarFiltros')}
      </Button>
      <Box id={idDelPanel}>{abiertos && formulario}</Box>
    </Box>
  );
}
