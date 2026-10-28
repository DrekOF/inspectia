/*
 * Nombre: PantallaEnBloque.jsx
 * Descripción: La pantalla de una entidad generada que se edita en bloque (RP-23), como los rangos de recomendación
 *              (RF-13): una fila por cada valor de su lista, todas en un solo formulario, con lo que dice su JSON: los
 *              roles de cada acción (RNF-04), los campos de cada fila por su tipo, los de solo lectura con su motivo,
 *              los textos y restaurar los valores por defecto, que pide confirmación (D-34). Los números se ven con
 *              el separador del idioma y se escriben con coma o con punto. Los errores de la API van al campo de su
 *              fila o, si son de todo el bloque, arriba del formulario. Lo escrito a mano para una entidad, como el
 *              resumen de los rangos y la regla del orden de sus límites, llega en sus extensiones.
 * Fecha de creación: 28/10/2026
 * Actualización: 28/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import CachedOutlined from '@mui/icons-material/CachedOutlined';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import Typography from '@mui/material/Typography';
import { Formik } from 'formik';
import { useEffect, useId, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { erroresPorCampo, textoDelError } from '../api/errores.js';
import { Aviso } from '../componentes/Aviso.jsx';
import { CampoDeNumero } from '../componentes/CampoDeNumero.jsx';
import { CampoDeTexto } from '../componentes/CampoDeTexto.jsx';
import { EnfocarPrimerError } from '../componentes/EnfocarPrimerError.jsx';
import { TituloDePagina } from '../componentes/TituloDePagina.jsx';
import { idiomaDe } from '../i18n/i18n.js';
import { useSesion } from '../sesion/contexto.js';
import { medidas } from '../tema/tokens.js';
import { textoDelDecimal } from './decimales.js';
import { erroresAnidados, puede } from './registros.js';

/**
 * @typedef {object} CampoDelBloque un campo de cada fila
 * @property {string} nombre su nombre, como lo nombra la API, como limiteInferior
 * @property {'texto'|'decimal'} tipo su tipo: un texto o un número con decimales
 * @property {string} etiqueta la clave de su etiqueta
 * @property {number} [escala] los decimales de un número
 * @property {boolean} [variasLineas] si un texto va en varias líneas
 * @property {{ codigo: string, mensaje: string }} [soloLecturaSi] el valor de la lista de la fila en la que no se
 *   puede cambiar y la clave del motivo (solo_lectura_si del JSON)
 */

/**
 * @typedef {object} DescripcionDelBloque lo que dice de su pantalla el JSON de una entidad que se edita en bloque
 * @property {string} entidad la entidad, como rango_recomendacion
 * @property {string} titulo la clave del título de la pantalla
 * @property {Record<string, string[]>} operaciones los roles de editar y de restaurar los valores por defecto
 * @property {{ atributo: string, valores: Array<{ codigo: string, etiqueta: string }> }} clave la lista que identifica
 *   cada fila: su atributo y sus valores, en orden, con la clave de la etiqueta de cada uno
 * @property {CampoDelBloque[]} campos los campos de cada fila, en orden
 * @property {object} textos las claves de los textos de la pantalla (pantalla.textos del JSON), o null las que no trae
 */

/**
 * @typedef {object} ReglaIncumplida una regla de todas las filas juntas que no se cumple
 * @property {string} mensaje la clave de su mensaje, la misma de la API
 * @property {string[]} campos los campos que marca, como filas[0].limiteInferior
 */

/**
 * @typedef {object} ExtensionesDelBloque lo escrito a mano para una pantalla en bloque: lo que su JSON no puede declarar
 * @property {import('react').ComponentType<{ filas: object[], pantalla: DescripcionDelBloque }>} [Resumen] lo que va
 *   antes del formulario, como «Así quedan los rangos»: recibe las filas como van quedando, con los números leídos
 * @property {(filas: object[]) => ReglaIncumplida|null} [reglaDelBloque] las reglas de todas las filas juntas, como el
 *   orden de los límites, que se revisan antes de enviar: recibe las filas con los números leídos y devuelve la que no
 *   se cumple, o null. La API las revisa igual
 */

/**
 * @typedef {object} AvisoDelBloque lo que dice el aviso de error de todo el bloque, arriba del formulario
 * @property {string} titulo lo que pasó
 * @property {string} [ayuda] qué hacer, si la API lo dice
 * @property {string[]} marcados los campos que marca una regla del bloque, que el aviso describe
 */

/**
 * El estilo de un recuadro de la pantalla, como el del formulario.
 *
 * @param {import('@mui/material/styles').Theme} theme el tema
 * @returns {object} el estilo
 */
const recuadro = (theme) => ({
  borderRadius: `${medidas.tarjeta.radio}px`,
  border: `1px solid ${theme.vars.palette.divider}`,
  bgcolor: 'background.paper',
});

/** En el celular, cada parte de una fila y cada botón ocupan todo el ancho; en el PC, las columnas del prototipo. */
const A_TODO_EL_ANCHO = '1 1 100%';
const COLUMNAS = {
  nombre: { xs: A_TODO_EL_ANCHO, md: '0 0 200px' },
  decimal: { xs: A_TODO_EL_ANCHO, md: '0 0 260px' },
  texto: { xs: A_TODO_EL_ANCHO, md: '1 1 320px' },
};

/**
 * El nombre de un campo de una fila en Formik, que es también su id y el nombre que le da la API en sus errores, como
 * filas[1].recomendacion.
 *
 * @param {number} posicion la posición de la fila
 * @param {CampoDelBloque} campo el campo
 * @returns {string} el nombre
 */
const nombreDelCampo = (posicion, campo) => `filas[${posicion}].${campo.nombre}`;

/**
 * Si un campo no se puede cambiar en una fila, por el valor de su lista (solo_lectura_si del JSON).
 *
 * @param {DescripcionDelBloque} pantalla la descripción de la pantalla
 * @param {object} fila la fila
 * @param {CampoDelBloque} campo el campo
 * @returns {boolean} si es de solo lectura
 */
const deSoloLectura = (pantalla, fila, campo) => Boolean(campo.soloLecturaSi)
  && fila[pantalla.clave.atributo] === campo.soloLecturaSi.codigo;

/**
 * Los valores del formulario para unas filas como las responde la API: los números, con la escala de su columna y el
 * separador del idioma.
 *
 * @param {DescripcionDelBloque} pantalla la descripción de la pantalla
 * @param {object[]} filas las filas
 * @param {string} region la región del idioma de la app, como es-CO
 * @returns {{ filas: object[] }} los valores
 */
function valoresDelFormulario(pantalla, filas, region) {
  const { atributo } = pantalla.clave;
  return {
    filas: filas.map((fila) => ({
      [atributo]: fila[atributo],
      ...Object.fromEntries(pantalla.campos.map((campo) => [campo.nombre, campo.tipo === 'decimal'
        ? textoDelDecimal(fila[campo.nombre], campo.escala, region) : (fila[campo.nombre] ?? '')])),
    })),
  };
}

/**
 * Las filas para la API: el valor de su lista y sus campos, con los textos recortados y los números leídos.
 *
 * @param {DescripcionDelBloque} pantalla la descripción de la pantalla
 * @param {object[]} leidas las filas, como las deja la validación
 * @returns {object[]} las filas
 */
function filasParaLaApi(pantalla, leidas) {
  const { atributo } = pantalla.clave;
  return leidas.map((fila) => ({
    [atributo]: fila[atributo],
    ...Object.fromEntries(pantalla.campos.map(({ nombre }) => [nombre, fila[nombre]])),
  }));
}

/**
 * Los mensajes de los errores de la API que son de todo el bloque y no de un campo del formulario: los de sus reglas,
 * que no tienen campo, y el de las filas, como cuando no llega una por cada valor de la lista.
 *
 * @param {Error} error el error, normalmente un ErrorDeApi
 * @param {string[]} campos los campos del formulario
 * @returns {string|null} los mensajes, o null si no hay ninguno
 */
function mensajesDelBloque(error, campos) {
  const mensajes = (error.errores ?? []).filter(({ campo }) => !campos.includes(campo)).map(({ mensaje }) => mensaje);
  return mensajes.length > 0 ? mensajes.join(' ') : null;
}

/**
 * Una fila del bloque: un grupo con el nombre de su valor de la lista y sus campos, cada uno por su tipo. En el PC, el
 * nombre va a la izquierda, como en el prototipo: la leyenda flota para ser una columna más del grupo.
 *
 * @param {object} props las propiedades
 * @param {DescripcionDelBloque} props.pantalla la descripción de la pantalla
 * @param {object} props.fila la fila, como está en el formulario
 * @param {number} props.posicion su posición
 * @param {boolean} props.ultima si es la última, que no lleva la línea de abajo
 * @param {AvisoDelBloque|null} props.aviso el aviso del formulario, si marca campos
 * @param {string} props.idDelAviso el id del aviso, que describe los campos que marca
 * @returns {import('react').ReactElement} la fila
 */
function Fila({ pantalla, fila, posicion, ultima, aviso, idDelAviso }) {
  const { t } = useTranslation();
  const valor = pantalla.clave.valores.find(({ codigo }) => codigo === fila[pantalla.clave.atributo]);
  return (
    <Box component="fieldset" sx={(theme) => ({
      m: 0, p: 0, border: 0, minWidth: 0, display: 'flex', flexWrap: 'wrap', gap: '12px 20px', alignItems: 'flex-start',
      ...(ultima ? {} : { pb: '14px', borderBottom: `1px solid ${theme.vars.palette.cacao.lineaFila}` }),
    })}>
      <Typography component="legend" sx={{ float: 'left', flex: COLUMNAS.nombre, p: 0, fontWeight: 500 }}>
        {t(valor.etiqueta)}
      </Typography>
      {pantalla.campos.map((campo) => {
        const nombre = nombreDelCampo(posicion, campo);
        const motivo = deSoloLectura(pantalla, fila, campo) ? t(campo.soloLecturaSi.mensaje) : null;
        const comunes = {
          nombre, etiqueta: t(campo.etiqueta), marcadoPor: aviso?.marcados.includes(nombre) ? idDelAviso : undefined,
        };
        return (
          <Box key={campo.nombre} sx={{ flex: COLUMNAS[campo.tipo], minWidth: 0 }}>
            {campo.tipo === 'decimal'
              ? <CampoDeNumero {...comunes} soloLectura={motivo} />
              : <CampoDeTexto {...comunes} variasLineas={campo.variasLineas} readOnly={Boolean(motivo)}
                ayuda={motivo ?? undefined} />}
          </Box>
        );
      })}
    </Box>
  );
}

/**
 * El diálogo que confirma restaurar los valores por defecto, con lo que se pierde (D-34).
 *
 * @param {object} props las propiedades
 * @param {string} props.detalle lo que se pierde
 * @param {boolean} props.abierto si el diálogo está abierto
 * @param {() => void} props.alConfirmar se llama al confirmar
 * @param {() => void} props.alCancelar se llama al cancelar o al cerrar el diálogo
 * @returns {import('react').ReactElement} el diálogo
 */
function ConfirmarRestaurar({ detalle, abierto, alConfirmar, alCancelar }) {
  const { t } = useTranslation();
  const idDelTitulo = useId();
  const idDelDetalle = useId();
  return (
    <Dialog open={abierto} onClose={alCancelar} aria-labelledby={idDelTitulo} aria-describedby={idDelDetalle}>
      <DialogTitle id={idDelTitulo}>{t('crud.confirmarRestaurar')}</DialogTitle>
      <DialogContent>
        <DialogContentText id={idDelDetalle}>{detalle}</DialogContentText>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2.5, flexWrap: 'wrap', gap: 1 }}>
        <Button variant="outlined" onClick={alCancelar}>{t('accion.cancelar')}</Button>
        <Button variant="contained" color="error" onClick={alConfirmar}>{t('crud.restaurar')}</Button>
      </DialogActions>
    </Dialog>
  );
}

/**
 * El formulario con todas las filas, el resumen escrito a mano, si lo hay, y sus acciones: guardar y restaurar.
 *
 * Los errores de todo el bloque van arriba del formulario, como en el prototipo. Las reglas del bloque se revisan
 * después de las validaciones de cada campo, como en la API, y marcan sus campos sin repetir el mensaje en cada uno:
 * el aviso los describe, y el foco va al primero. El aviso de que se guardó o se restauró va junto a los botones,
 * donde se ve después de usarlos, también en el celular.
 *
 * @param {object} props las propiedades
 * @param {DescripcionDelBloque} props.pantalla la descripción de la pantalla
 * @param {object} props.api el cliente de su API, generado: listar, editar y, si la tiene, restaurarPorDefecto
 * @param {(t: import('./registros.js').Traducir) => import('yup').ObjectSchema} props.esquema sus validaciones
 * @param {ExtensionesDelBloque} [props.extensiones] lo escrito a mano para ella
 * @param {object[]} props.filas las filas, como las respondió la API
 * @param {{ editar: boolean, restaurar: boolean }} props.permisos lo que puede hacer el rol
 * @returns {import('react').ReactElement} el formulario
 */
function FormularioDelBloque({ pantalla, api, esquema, extensiones, filas, permisos }) {
  const { t, i18n } = useTranslation();
  const { pedir } = useSesion();
  const [aviso, setAviso] = useState(null);
  const [hecho, setHecho] = useState(null);
  const [confirmando, setConfirmando] = useState(false);
  const idDelAviso = useId();
  const { textos } = pantalla;
  const region = idiomaDe(i18n.language).region;
  const validaciones = esquema(t);
  const Resumen = extensiones?.Resumen;
  const campos = filas.flatMap((fila, posicion) => pantalla.campos.map((campo) => ({
    nombre: nombreDelCampo(posicion, campo), editable: !deSoloLectura(pantalla, fila, campo),
  })));
  const nombres = campos.map(({ nombre }) => nombre);

  /**
   * Las filas con los números leídos y los textos recortados, como las valida Yup.
   *
   * @param {{ filas: object[] }} valores los valores del formulario
   * @returns {object[]} las filas
   */
  const leer = (valores) => validaciones.cast(valores, { assert: false }).filas;

  const guardar = async (valores, { setErrors, resetForm }) => {
    const leidas = leer(valores);
    const regla = extensiones?.reglaDelBloque?.(leidas) ?? null;
    if (regla) {
      const marcados = regla.campos.filter((nombre) => campos.some((campo) => campo.nombre === nombre && campo.editable));
      setAviso({ titulo: t(regla.mensaje), marcados });
      document.getElementById(marcados[0])?.focus();
      return;
    }
    try {
      const guardadas = await api.editar(pedir, filasParaLaApi(pantalla, leidas));
      resetForm({ values: valoresDelFormulario(pantalla, guardadas, region) });
      setHecho(t(textos.alGuardar));
    } catch (falla) {
      const deLosCampos = erroresPorCampo(falla, nombres);
      if (deLosCampos) {
        setErrors(erroresAnidados(deLosCampos));
      }
      const delBloque = mensajesDelBloque(falla, nombres);
      if (delBloque || !deLosCampos) {
        setAviso({ titulo: delBloque ?? textoDelError(falla, t), ayuda: delBloque ? undefined : falla.ayuda, marcados: [] });
      }
    }
  };

  const restaurar = async (resetForm) => {
    setConfirmando(false);
    setAviso(null);
    setHecho(null);
    try {
      resetForm({ values: valoresDelFormulario(pantalla, await api.restaurarPorDefecto(pedir), region) });
      setHecho(t(textos.alRestaurar));
    } catch (falla) {
      setAviso({ titulo: textoDelError(falla, t), ayuda: falla.ayuda, marcados: [] });
    }
  };

  return (
    <Formik initialValues={valoresDelFormulario(pantalla, filas, region)} validationSchema={validaciones}
      onSubmit={guardar}>
      {({ values, isSubmitting, handleSubmit, resetForm }) => (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
          {Resumen && <Resumen filas={leer(values)} pantalla={pantalla} />}
          {/* Cada intento de guardar empieza sin el aviso anterior, aunque una validación no deje enviar. */}
          <Box component="form" noValidate aria-label={t(textos.tituloEditar)} onSubmit={(evento) => {
            setAviso(null);
            setHecho(null);
            handleSubmit(evento);
          }} sx={(theme) => ({
            ...recuadro(theme), p: { xs: 2.5, md: `${medidas.tarjeta.relleno}px` },
            display: 'flex', flexDirection: 'column', gap: 2.25,
          })}>
            <EnfocarPrimerError campos={nombres} />
            {aviso && <Aviso tipo="error" id={idDelAviso} titulo={aviso.titulo}>{aviso.ayuda}</Aviso>}
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              {values.filas.map((fila, posicion) => (
                <Fila key={fila[pantalla.clave.atributo]} pantalla={pantalla} fila={fila} posicion={posicion}
                  ultima={posicion === values.filas.length - 1} aviso={aviso} idDelAviso={idDelAviso} />
              ))}
            </Box>
            {textos.notaFormulario && (
              <Typography variant="body2" color="text.secondary">{t(textos.notaFormulario)}</Typography>
            )}
            {hecho && <Aviso tipo="success">{hecho}</Aviso>}
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1.5 }}>
              {permisos.editar && (
                <Button type="submit" variant="contained" disabled={isSubmitting} sx={{ flex: { xs: A_TODO_EL_ANCHO, md: 'none' } }}>
                  {t('crud.guardarCambios')}
                </Button>
              )}
              {permisos.restaurar && (
                <Button variant="outlined" startIcon={<CachedOutlined aria-hidden />} onClick={() => setConfirmando(true)}
                  sx={{ flex: { xs: A_TODO_EL_ANCHO, md: 'none' } }}>
                  {t('crud.restaurarPorDefecto')}
                </Button>
              )}
            </Box>
            {permisos.restaurar && textos.notaRestaurar && (
              <Typography variant="body2" color="text.secondary" sx={{ fontSize: 13 }}>{t(textos.notaRestaurar)}</Typography>
            )}
          </Box>
          {permisos.restaurar && (
            <ConfirmarRestaurar detalle={t(textos.confirmarRestaurarDetalle)} abierto={confirmando}
              alConfirmar={() => restaurar(resetForm)} alCancelar={() => setConfirmando(false)} />
          )}
        </Box>
      )}
    </Formik>
  );
}

/**
 * La pantalla de una entidad generada que se edita en bloque: carga sus filas y muestra el formulario.
 *
 * @param {object} props las propiedades
 * @param {DescripcionDelBloque} props.pantalla lo que dice su JSON
 * @param {object} props.api el cliente de su API, generado: listar, editar y, si la tiene, restaurarPorDefecto
 * @param {(t: import('./registros.js').Traducir) => import('yup').ObjectSchema} props.esquema las validaciones de sus
 *   filas, generadas
 * @param {ExtensionesDelBloque} [props.extensiones] lo escrito a mano para ella
 * @returns {import('react').ReactElement} la pantalla
 */
export function PantallaEnBloque({ pantalla, api, esquema, extensiones }) {
  const { t } = useTranslation();
  const { pedir, sesion } = useSesion();
  const rol = sesion.usuario.rol;
  const { textos, operaciones } = pantalla;
  const [recargas, setRecargas] = useState(0);
  const [respuesta, setRespuesta] = useState({ pedido: null, filas: null, error: null });

  useEffect(() => {
    let vigente = true;
    api.listar(pedir).then((filas) => {
      if (vigente) {
        setRespuesta({ pedido: recargas, filas, error: null });
      }
    }).catch((error) => {
      if (vigente) {
        setRespuesta({ pedido: recargas, filas: null, error });
      }
    });
    return () => {
      vigente = false;
    };
  }, [api, pedir, recargas]);

  const cargando = respuesta.pedido !== recargas;
  const permisos = { editar: puede(operaciones, rol, 'editar'), restaurar: puede(operaciones, rol, 'restaurarPorDefecto') };

  return (
    <>
      <TituloDePagina titulo={t(pantalla.titulo)} descripcion={textos.descripcion ? t(textos.descripcion) : undefined} />
      {cargando && (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress aria-label={t('crud.cargando')} />
        </Box>
      )}
      {!cargando && respuesta.error && (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 1.5 }}>
          <Aviso tipo="error" titulo={textoDelError(respuesta.error, t)} />
          <Button variant="outlined" onClick={() => setRecargas((veces) => veces + 1)}>{t('crud.reintentar')}</Button>
        </Box>
      )}
      {!cargando && respuesta.filas && (
        <FormularioDelBloque pantalla={pantalla} api={api} esquema={esquema} extensiones={extensiones}
          filas={respuesta.filas} permisos={permisos} />
      )}
    </>
  );
}
