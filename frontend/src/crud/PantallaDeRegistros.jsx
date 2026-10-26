/*
 * Nombre: PantallaDeRegistros.jsx
 * Descripción: La pantalla de una entidad generada (RP-23): su lista por páginas y con su resumen (tabla en el PC
 *              y tarjetas en el celular, RNF-05), el formulario para registrar o editar y las acciones de cada
 *              fila, con lo que dice su JSON: los roles de cada acción (RNF-04), las columnas, los campos y los
 *              textos. Lo escrito a mano para una entidad, como el panel de administradores de una empresa,
 *              llega en sus extensiones. Al abrir un panel, el foco va a su título y, al cerrarlo, vuelve al botón
 *              que lo abrió (RNF-06 C4).
 * Fecha de creación: 26/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Add from '@mui/icons-material/Add';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import CircularProgress from '@mui/material/CircularProgress';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TablePagination from '@mui/material/TablePagination';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import { Form, Formik } from 'formik';
import { useEffect, useId, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { erroresPorCampo, textoDelError } from '../api/errores.js';
import { Aviso } from '../componentes/Aviso.jsx';
import { EnfocarPrimerError } from '../componentes/EnfocarPrimerError.jsx';
import { TituloDePagina } from '../componentes/TituloDePagina.jsx';
import { useDevolverElFoco } from '../componentes/useDevolverElFoco.js';
import { useEnfocarAlMostrar } from '../componentes/useEnfocarAlMostrar.js';
import { useEsPc } from '../componentes/useEsPc.js';
import { useSesion } from '../sesion/contexto.js';
import { medidas } from '../tema/tokens.js';
import { erroresAnidados, puede, resumenDeRegistros, valoresDeFila } from './registros.js';

/**
 * @typedef {object} Columna una columna de la lista
 * @property {string} atributo el atributo que muestra, como lo nombra la API
 * @property {string} etiqueta la clave del título de la columna
 * @property {{ atributo: string, texto: string|null }} [debajo] el atributo que va debajo, como el NIT bajo el nombre
 *   de la empresa, y la clave del texto que lo muestra, o null para mostrar solo su valor
 * @property {string} [valores] si el atributo es una lista, la clave de los textos de sus valores, como
 *   producto.estado
 */

/**
 * @typedef {object} DescripcionDePantalla lo que dice de su pantalla el JSON de una entidad
 * @property {string} entidad la entidad, como empresa
 * @property {string} titulo la clave del título de la pantalla
 * @property {Record<string, string[]>} operaciones los roles de crear, editar, desactivar y reactivar
 * @property {string|null} atributoActivo el atributo que cambian desactivar y reactivar, o null si no se desactiva
 * @property {string} atributoDelNombre el atributo que nombra una fila en sus textos, como «Editar {nombre}»
 * @property {Columna[]} columnas las columnas de la lista, en orden
 * @property {object} textos las claves de los textos de la pantalla (pantalla.textos del JSON), o null las que no
 *   trae; el resumen trae las claves de su frase y de lo que cuenta
 * @property {{ titulo: string, detalle: string }|null} confirmarDesactivar las claves del diálogo que confirma al
 *   desactivar, o null si desactivar no pide confirmación
 */

/**
 * @typedef {object} Formulario el formulario de una pantalla generada
 * @property {import('react').ComponentType<{ paraCrear: boolean }>} Campos los campos, en el orden del JSON
 * @property {string[]} [camposParaCrear] los campos para registrar, en orden, como los nombra la API
 * @property {() => object} [valoresParaCrear] los valores de un registro nuevo
 * @property {(t: import('./registros.js').Traducir) => import('yup').ObjectSchema} [esquemaParaCrear] las validaciones
 *   para registrar
 * @property {string[]} [camposParaEditar] los campos para editar, en orden
 * @property {(fila: object) => object} [valoresParaEditar] los valores de una fila para editarla
 * @property {(t: import('./registros.js').Traducir) => import('yup').ObjectSchema} [esquemaParaEditar] las validaciones
 *   para editar
 */

/**
 * @typedef {object} AccionDeFila una acción escrita a mano para cada fila, que abre su propio panel
 * @property {string} clave su nombre, como administradores
 * @property {string} texto la clave del texto de su botón
 * @property {string} nombreAccesible la clave del nombre de su botón para los lectores de pantalla, con los valores
 *   de la fila, como «Administradores de {nombre}»
 * @property {string[]} [roles] los roles que la ven; sin ellos, todos los que abren la pantalla
 * @property {import('react').ComponentType<{ fila: object, alCerrar: () => void }>} Panel el panel que abre; como el
 *   formulario, lleva el foco a su título al abrirse (useEnfocarAlMostrar)
 */

/**
 * @typedef {object} Eleccion lo que se eligió en una fila o arriba de la lista
 * @property {'crear'|'editar'|'desactivar'|'reactivar'|'extension'} tipo la acción
 * @property {object|null} fila la fila, o null al registrar
 * @property {AccionDeFila} [accion] la acción escrita a mano, si el tipo es extension
 * @property {HTMLElement} [boton] el botón que abre el panel, para devolverle el foco al cerrarlo
 */

/**
 * @typedef {object} Extensiones lo escrito a mano para una pantalla generada: lo que su JSON no puede declarar
 * @property {AccionDeFila[]} [accionesDeFila] acciones extra de cada fila
 */

/** Las filas de cada página, siempre 20, como en el prototipo («20 por página») y en RF-14. */
const FILAS_POR_PAGINA = 20;

/** Oculto a la vista, pero no para los lectores de pantalla, como el título de la tabla del prototipo. */
const OCULTO_A_LA_VISTA = {
  border: 0, clip: 'rect(0 0 0 0)', height: '1px', margin: '-1px', overflow: 'hidden', padding: 0,
  position: 'absolute', whiteSpace: 'nowrap', width: '1px',
};

/**
 * El estilo de un recuadro de la pantalla, como el del formulario o el de una tarjeta.
 *
 * @param {import('@mui/material/styles').Theme} theme el tema
 * @returns {object} el estilo
 */
const recuadro = (theme) => ({
  borderRadius: `${medidas.tarjeta.radio}px`,
  border: `1px solid ${theme.vars.palette.divider}`,
  bgcolor: 'background.paper',
});

/**
 * El texto de una celda: el valor del atributo o, si es una lista, la etiqueta de su valor.
 *
 * @param {Columna} columna la columna
 * @param {object} fila la fila
 * @param {import('./registros.js').Traducir} t la función de traducción
 * @returns {string} el texto
 */
function textoDeCelda(columna, fila, t) {
  const valor = fila[columna.atributo];
  if (valor === null || valor === undefined || valor === '') {
    return '';
  }
  return columna.valores ? t(`${columna.valores}.${valor}`) : String(valor);
}

/**
 * Lo que va debajo del nombre en la primera columna, como «NIT 900123456-8».
 *
 * @param {Columna} columna la primera columna
 * @param {object} fila la fila
 * @param {import('./registros.js').Traducir} t la función de traducción
 * @returns {string|null} el texto, o null si la columna no lleva nada debajo
 */
function textoDeDebajo(columna, fila, t) {
  if (!columna.debajo) {
    return null;
  }
  return columna.debajo.texto ? t(columna.debajo.texto, valoresDeFila(fila)) : String(fila[columna.debajo.atributo] ?? '');
}

/**
 * El estado de una fila: activa o desactivada, con los textos de su JSON.
 *
 * @param {{ activo: boolean, textos: object }} props si está activa y las claves de los textos de la pantalla
 * @returns {import('react').ReactElement} el estado
 */
function Estado({ activo, textos }) {
  const { t } = useTranslation();
  return (
    <Chip size="small" label={t(activo ? textos.activo : textos.inactivo)} sx={(theme) => ({
      borderRadius: `${medidas.chip.radio}px`,
      fontWeight: 500,
      bgcolor: activo ? theme.vars.palette.cacao.exito.fondo : theme.vars.palette.cacao.seleccion.fondo,
      color: activo ? theme.vars.palette.cacao.exito.texto : theme.vars.palette.text.secondary,
    })} />
  );
}

/** El estilo de los botones de una fila: más bajos que los demás, pero con el alto mínimo para tocarlos (RNF-05). */
const BOTON_DE_FILA = { minHeight: medidas.boton.altoEnTabla, paddingInline: 1.5 };

/**
 * Los botones de las acciones de una fila que el rol puede usar: editar, las escritas a mano, como
 * Administradores, y desactivar o reactivar. Desactivar y reactivar son el mismo botón, que cambia con la fila:
 * así el foco sigue en él después de usarlo (RNF-06 C4).
 *
 * @param {object} props las propiedades
 * @param {DescripcionDePantalla} props.pantalla la descripción de la pantalla
 * @param {object} props.fila la fila
 * @param {{ editar: boolean, desactivar: boolean, reactivar: boolean }} props.permisos lo que el rol puede hacer
 * @param {AccionDeFila[]} props.extras las acciones escritas a mano que el rol puede usar
 * @param {(eleccion: Eleccion) => void} props.alElegir se llama con la acción elegida
 * @returns {import('react').ReactElement} los botones
 */
function Acciones({ pantalla, fila, permisos, extras, alElegir }) {
  const { t } = useTranslation();
  const { textos } = pantalla;
  const valores = valoresDeFila(fila);
  const activo = pantalla.atributoActivo ? fila[pantalla.atributoActivo] : null;
  const alternar = (activo === true && permisos.desactivar) || (activo === false && permisos.reactivar);
  return (
    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
      {permisos.editar && (
        <Button size="small" aria-label={t(textos.editarFila, valores)} sx={BOTON_DE_FILA}
          onClick={(evento) => alElegir({ tipo: 'editar', fila, boton: evento.currentTarget })}>
          {t('crud.editar')}
        </Button>
      )}
      {extras.map((accion) => (
        <Button key={accion.clave} size="small" aria-label={t(accion.nombreAccesible, valores)} sx={BOTON_DE_FILA}
          onClick={(evento) => alElegir({ tipo: 'extension', fila, accion, boton: evento.currentTarget })}>
          {t(accion.texto)}
        </Button>
      ))}
      {alternar && (
        <Button size="small" color={activo ? 'error' : 'primary'} sx={BOTON_DE_FILA}
          aria-label={t(activo ? textos.desactivarFila : textos.reactivarFila, valores)}
          onClick={() => alElegir({ tipo: activo ? 'desactivar' : 'reactivar', fila })}>
          {t(activo ? 'crud.desactivar' : 'crud.reactivar')}
        </Button>
      )}
    </Box>
  );
}

/**
 * La lista en el PC: una tabla con su título para los lectores de pantalla.
 *
 * @param {object} props las propiedades
 * @param {DescripcionDePantalla} props.pantalla la descripción de la pantalla
 * @param {object[]} props.filas las filas de la página
 * @param {boolean} props.conAcciones si el rol tiene alguna acción
 * @param {(fila: object) => import('react').ReactNode} props.acciones los botones de una fila
 * @returns {import('react').ReactElement} la tabla
 */
function Tabla({ pantalla, filas, conAcciones, acciones }) {
  const { t } = useTranslation();
  const [primera, ...demas] = pantalla.columnas;
  return (
    <Box sx={(theme) => ({ ...recuadro(theme), overflowX: 'auto' })}>
      <Table>
        <Box component="caption" sx={OCULTO_A_LA_VISTA}>{t(pantalla.textos.tituloLista)}</Box>
        <TableHead>
          <TableRow>
            {pantalla.columnas.map((columna) => <TableCell key={columna.atributo}>{t(columna.etiqueta)}</TableCell>)}
            {pantalla.atributoActivo && <TableCell>{t('crud.estado')}</TableCell>}
            {conAcciones && <TableCell>{t('crud.acciones')}</TableCell>}
          </TableRow>
        </TableHead>
        <TableBody>
          {filas.map((fila) => (
            <TableRow key={fila.id}>
              <TableCell>
                <Typography variant="body2" sx={{ fontWeight: 500 }}>{textoDeCelda(primera, fila, t)}</Typography>
                {primera.debajo && (
                  <Typography variant="body2" color="text.secondary">{textoDeDebajo(primera, fila, t)}</Typography>
                )}
              </TableCell>
              {demas.map((columna) => <TableCell key={columna.atributo}>{textoDeCelda(columna, fila, t)}</TableCell>)}
              {pantalla.atributoActivo && (
                <TableCell><Estado activo={fila[pantalla.atributoActivo]} textos={pantalla.textos} /></TableCell>
              )}
              {conAcciones && <TableCell>{acciones(fila)}</TableCell>}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Box>
  );
}

/**
 * La lista en el celular: una tarjeta por fila, con sus datos y sus acciones (RNF-05).
 *
 * @param {object} props las propiedades
 * @param {DescripcionDePantalla} props.pantalla la descripción de la pantalla
 * @param {object[]} props.filas las filas de la página
 * @param {(fila: object) => import('react').ReactNode} props.acciones los botones de una fila
 * @returns {import('react').ReactElement} las tarjetas
 */
function Tarjetas({ pantalla, filas, acciones }) {
  const { t } = useTranslation();
  const [primera, ...demas] = pantalla.columnas;
  return (
    <Box component="ul" role="list" aria-label={t(pantalla.textos.tituloLista)}
      sx={{ listStyle: 'none', m: 0, p: 0, display: 'flex', flexDirection: 'column', gap: 1.5 }}>
      {filas.map((fila) => (
        <Box component="li" key={fila.id} sx={(theme) => ({
          ...recuadro(theme), p: 2, display: 'flex', flexDirection: 'column', gap: 0.75,
        })}>
          <Typography sx={{ fontWeight: 500 }}>{textoDeCelda(primera, fila, t)}</Typography>
          {primera.debajo && <Typography variant="body2" color="text.secondary">{textoDeDebajo(primera, fila, t)}</Typography>}
          {demas.map((columna) => (
            <Typography key={columna.atributo} variant="body2">
              <Box component="span" sx={{ color: 'text.secondary' }}>{t(columna.etiqueta)}: </Box>
              {textoDeCelda(columna, fila, t)}
            </Typography>
          ))}
          {pantalla.atributoActivo && (
            <Box><Estado activo={fila[pantalla.atributoActivo]} textos={pantalla.textos} /></Box>
          )}
          {acciones(fila)}
        </Box>
      ))}
    </Box>
  );
}

/**
 * El formulario para registrar o editar un registro, con las validaciones de su JSON. Los errores de la API van
 * a su campo y, si no son de un campo, arriba del formulario.
 *
 * @param {object} props las propiedades
 * @param {DescripcionDePantalla} props.pantalla la descripción de la pantalla
 * @param {object} props.api el cliente de la API de la entidad
 * @param {Formulario} props.formulario el formulario
 * @param {object|null} props.fila la fila que se edita, o null para registrar
 * @param {(registro: object) => void} props.alGuardar se llama con el registro como quedó en la API
 * @param {() => void} props.alCerrar se llama al cancelar
 * @returns {import('react').ReactElement} el formulario
 */
function PanelDelFormulario({ pantalla, api, formulario, fila, alGuardar, alCerrar }) {
  const { t } = useTranslation();
  const { pedir } = useSesion();
  const [error, setError] = useState(null);
  const idDelTitulo = useId();
  const titulo = useEnfocarAlMostrar();
  const { textos } = pantalla;
  const paraCrear = fila === null;
  const campos = paraCrear ? formulario.camposParaCrear : formulario.camposParaEditar;
  const { Campos } = formulario;

  const guardar = async (valores, { setErrors }) => {
    setError(null);
    try {
      alGuardar(paraCrear ? await api.crear(pedir, valores) : await api.editar(pedir, fila.id, valores));
    } catch (falla) {
      const deLosCampos = erroresPorCampo(falla, campos);
      if (deLosCampos) {
        setErrors(erroresAnidados(deLosCampos));
      } else {
        setError(falla);
      }
    }
  };

  return (
    <Box sx={(theme) => ({ ...recuadro(theme), p: { xs: 2.5, md: `${medidas.tarjeta.relleno}px` }, mb: 3 })}>
      <Typography id={idDelTitulo} ref={titulo} tabIndex={-1} variant="h2" sx={{ mb: 2.5, outline: 'none' }}>
        {t(paraCrear ? textos.nuevo : textos.tituloEditar)}
      </Typography>
      <Formik initialValues={paraCrear ? formulario.valoresParaCrear() : formulario.valoresParaEditar(fila)}
        validationSchema={(paraCrear ? formulario.esquemaParaCrear : formulario.esquemaParaEditar)(t)} onSubmit={guardar}>
        {({ isSubmitting }) => (
          <Box component={Form} noValidate aria-labelledby={idDelTitulo}
            sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}>
            <EnfocarPrimerError campos={campos} />
            {error && <Aviso tipo="error" titulo={textoDelError(error, t)}>{error.ayuda}</Aviso>}
            <Box sx={{ display: 'grid', gap: 3, gridTemplateColumns: { xs: '1fr', md: 'repeat(auto-fit, minmax(280px, 1fr))' } }}>
              <Campos paraCrear={paraCrear} />
            </Box>
            {paraCrear && textos.notaFormulario && (
              <Typography variant="body2" color="text.secondary">{t(textos.notaFormulario)}</Typography>
            )}
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1.5 }}>
              <Button type="submit" variant="contained" disabled={isSubmitting}>
                {paraCrear ? t(textos.crear) : t('crud.guardarCambios')}
              </Button>
              <Button variant="outlined" onClick={alCerrar}>{t('accion.cancelar')}</Button>
            </Box>
          </Box>
        )}
      </Formik>
    </Box>
  );
}

/**
 * El diálogo que confirma desactivar un registro, con los textos de su JSON, como «¿Desactivar Cacaos del Huila?».
 *
 * @param {object} props las propiedades
 * @param {{ titulo: string, detalle: string }} props.textos las claves de su título y de su detalle
 * @param {boolean} props.abierto si el diálogo está abierto
 * @param {object|null} props.fila la fila que se va a desactivar; queda mientras el diálogo se cierra
 * @param {() => void} props.alConfirmar se llama al confirmar
 * @param {() => void} props.alCancelar se llama al cancelar o al cerrar el diálogo
 * @returns {import('react').ReactElement} el diálogo
 */
function ConfirmarDesactivar({ textos, abierto, fila, alConfirmar, alCancelar }) {
  const { t } = useTranslation();
  const idDelTitulo = useId();
  const idDelDetalle = useId();
  const valores = valoresDeFila(fila);
  return (
    <Dialog open={abierto} onClose={alCancelar} aria-labelledby={idDelTitulo} aria-describedby={idDelDetalle}>
      <DialogTitle id={idDelTitulo}>{t(textos.titulo, valores)}</DialogTitle>
      <DialogContent>
        <DialogContentText id={idDelDetalle}>{t(textos.detalle, valores)}</DialogContentText>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2.5, flexWrap: 'wrap', gap: 1 }}>
        <Button variant="outlined" onClick={alCancelar}>{t('accion.cancelar')}</Button>
        <Button variant="contained" color="error" onClick={alConfirmar}>{t('crud.desactivar')}</Button>
      </DialogActions>
    </Dialog>
  );
}

/**
 * La pantalla de una entidad generada: la lista, el formulario y las acciones de cada fila.
 *
 * @param {object} props las propiedades
 * @param {DescripcionDePantalla} props.pantalla lo que dice su JSON
 * @param {object} props.api el cliente de su API, generado: listar y, si las tiene, crear, editar, desactivar y
 *   reactivar
 * @param {Formulario} [props.formulario] su formulario, si se puede registrar o editar
 * @param {Extensiones} [props.extensiones] lo escrito a mano para ella
 * @returns {import('react').ReactElement} la pantalla
 */
export function PantallaDeRegistros({ pantalla, api, formulario, extensiones }) {
  const { t } = useTranslation();
  const { pedir, sesion } = useSesion();
  const esPc = useEsPc();
  const rol = sesion.usuario.rol;
  const { textos, operaciones } = pantalla;
  const [pagina, setPagina] = useState(0);
  const [recargas, setRecargas] = useState(0);
  const [respuesta, setRespuesta] = useState({ pedido: null, pagina: null, activos: null, error: null });
  const [panel, setPanel] = useState(null);
  const [resultado, setResultado] = useState(null);
  const [porDesactivar, setPorDesactivar] = useState({ abierto: false, fila: null });
  const foco = useDevolverElFoco();

  // El resumen cuenta los activos con el filtro del listado (D-33): pide una sola fila y lee el total.
  const contarActivos = Boolean(pantalla.atributoActivo && textos.resumen?.activos);
  const pedido = `${pagina}|${recargas}`;
  useEffect(() => {
    let vigente = true;
    Promise.all([
      api.listar(pedir, { pagina, tamano: FILAS_POR_PAGINA }),
      contarActivos ? api.listar(pedir, { tamano: 1, activo: true }) : null,
    ]).then(([datos, activos]) => {
      if (vigente) {
        setRespuesta({ pedido, pagina: datos, activos: activos?.totalElementos ?? null, error: null });
      }
    }).catch((error) => {
      if (vigente) {
        setRespuesta({ pedido, pagina: null, activos: null, error });
      }
    });
    return () => {
      vigente = false;
    };
  }, [api, pedir, pagina, contarActivos, pedido]);

  const cargando = respuesta.pedido !== pedido;
  const datos = respuesta.pagina;
  const total = datos?.totalElementos ?? 0;
  const permisos = {
    crear: Boolean(formulario?.esquemaParaCrear) && puede(operaciones, rol, 'crear'),
    editar: Boolean(formulario?.esquemaParaEditar) && puede(operaciones, rol, 'editar'),
    desactivar: puede(operaciones, rol, 'desactivar'),
    reactivar: puede(operaciones, rol, 'reactivar'),
  };
  const extras = (extensiones?.accionesDeFila ?? []).filter((accion) => !accion.roles || accion.roles.includes(rol));
  const conAcciones = permisos.editar || extras.length > 0
    || (Boolean(pantalla.atributoActivo) && (permisos.desactivar || permisos.reactivar));
  const resumen = datos && total > 0 ? resumenDeRegistros(t, textos.resumen, { total, activos: respuesta.activos }) : null;

  const recargar = () => setRecargas((veces) => veces + 1);

  const cambiarActivo = async (fila, operacion) => {
    try {
      const registro = await api[operacion](pedir, fila.id);
      const texto = operacion === 'desactivar' ? textos.alDesactivar : textos.alReactivar;
      setResultado({ mensaje: t(texto, valoresDeFila(registro ?? fila)) });
      recargar();
    } catch (error) {
      setResultado({ error });
    }
  };

  /** @param {Eleccion} eleccion lo que se eligió */
  const elegir = ({ tipo, fila, accion, boton }) => {
    setResultado(null);
    if (tipo === 'desactivar' && pantalla.confirmarDesactivar) {
      setPorDesactivar({ abierto: true, fila });
    } else if (tipo === 'desactivar' || tipo === 'reactivar') {
      cambiarActivo(fila, tipo);
    } else {
      foco.recordar(boton);
      setPanel({ tipo, fila, accion });
    }
  };

  const cerrarPanel = () => {
    setPanel(null);
    foco.devolver();
  };

  const guardado = (registro) => {
    cerrarPanel();
    setResultado({ mensaje: t(textos.alGuardar, valoresDeFila(registro)) });
    recargar();
  };

  const confirmarDesactivar = () => {
    setPorDesactivar({ abierto: false, fila: porDesactivar.fila });
    cambiarActivo(porDesactivar.fila, 'desactivar');
  };

  const acciones = (fila) => <Acciones pantalla={pantalla} fila={fila} permisos={permisos} extras={extras} alElegir={elegir} />;
  const PanelExtra = panel?.tipo === 'extension' ? panel.accion.Panel : null;

  return (
    <>
      <TituloDePagina titulo={t(pantalla.titulo)} descripcion={textos.descripcion ? t(textos.descripcion) : undefined} />
      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, alignItems: 'center', justifyContent: 'space-between', mb: 2.5 }}>
        <Typography color="text.secondary">{resumen}</Typography>
        {permisos.crear && (
          <Button variant="contained" startIcon={<Add aria-hidden />}
            onClick={(evento) => elegir({ tipo: 'crear', fila: null, boton: evento.currentTarget })}>
            {t(textos.nuevo)}
          </Button>
        )}
      </Box>
      {resultado?.mensaje && <Box sx={{ mb: 2.5 }}><Aviso tipo="success">{resultado.mensaje}</Aviso></Box>}
      {resultado?.error && <Box sx={{ mb: 2.5 }}><Aviso tipo="error" titulo={textoDelError(resultado.error, t)} /></Box>}
      {(panel?.tipo === 'crear' || panel?.tipo === 'editar') && (
        <PanelDelFormulario key={`${panel.tipo}-${panel.fila?.id ?? ''}`} pantalla={pantalla} api={api}
          formulario={formulario} fila={panel.fila} alGuardar={guardado} alCerrar={cerrarPanel} />
      )}
      {PanelExtra && (
        <Box sx={{ mb: 3 }}>
          <PanelExtra key={panel.fila.id} fila={panel.fila} alCerrar={cerrarPanel} />
        </Box>
      )}
      {cargando && !datos && (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress aria-label={t('crud.cargando')} />
        </Box>
      )}
      {!cargando && respuesta.error && (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 1.5 }}>
          <Aviso tipo="error" titulo={textoDelError(respuesta.error, t)} />
          <Button variant="outlined" onClick={recargar}>{t('crud.reintentar')}</Button>
        </Box>
      )}
      {datos && total === 0 && <Typography>{t(textos.vacia)}</Typography>}
      {datos && total > 0 && (esPc
        ? <Tabla pantalla={pantalla} filas={datos.contenido} conAcciones={conAcciones} acciones={acciones} />
        : <Tarjetas pantalla={pantalla} filas={datos.contenido} acciones={acciones} />)}
      {/* Sin opciones, MUI no muestra el selector de filas por página: son siempre 20. */}
      {datos && datos.totalPaginas > 1 && (
        <TablePagination component="div" count={total} page={pagina} rowsPerPage={FILAS_POR_PAGINA}
          rowsPerPageOptions={[]} onPageChange={(_, nueva) => setPagina(nueva)}
          labelDisplayedRows={({ from, to, count }) => t('crud.rango', { desde: from, hasta: to, total: count })}
          getItemAriaLabel={(tipo) => t(tipo === 'next' ? 'crud.paginaSiguiente' : 'crud.paginaAnterior')} />
      )}
      {textos.notaLista && <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>{t(textos.notaLista)}</Typography>}
      {pantalla.confirmarDesactivar && (
        <ConfirmarDesactivar textos={pantalla.confirmarDesactivar} abierto={porDesactivar.abierto}
          fila={porDesactivar.fila} alConfirmar={confirmarDesactivar}
          alCancelar={() => setPorDesactivar({ abierto: false, fila: porDesactivar.fila })} />
      )}
    </>
  );
}
