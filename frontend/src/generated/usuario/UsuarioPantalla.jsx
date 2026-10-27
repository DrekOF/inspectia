/*
 * Nombre: UsuarioPantalla.jsx
 * Descripción: Pantalla de usuario (RF-03): su lista, su formulario y sus acciones.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.9)
 * Versión: 1.9
 * Entidades: usuario 1.8
 */
import { useTranslation } from 'react-i18next';
import { CampoContrasena } from '../../componentes/CampoContrasena.jsx';
import { CampoDeTexto } from '../../componentes/CampoDeTexto.jsx';
import { CampoDeLista } from '../../crud/CampoDeLista.jsx';
import { MensajeDeLaFilaPropia } from '../../crud/MensajeDeLaFilaPropia.jsx';
import { PantallaDeRegistros } from '../../crud/PantallaDeRegistros.jsx';
import * as api from './usuarioApi.js';
import { esquemaParaCrear, esquemaParaEditar } from './usuarioEsquemas.js';

/**
 * Lo que dice de la pantalla specs/entidades/usuario.json: los roles de cada acción, las columnas de la lista,
 * las claves de sus textos, dónde va el formulario, el límite de activos y la fila de quien tiene la sesión, o null
 * lo que el JSON no trae.
 *
 * @type {import('../../crud/PantallaDeRegistros.jsx').DescripcionDePantalla}
 */
const PANTALLA = {
  entidad: 'usuario',
  titulo: 'usuario.etiqueta.plural',
  operaciones: { crear: ['ADMIN_EMPRESA'], editar: ['ADMIN_EMPRESA'], desactivar: ['ADMIN_EMPRESA'], reactivar: ['ADMIN_EMPRESA'] },
  atributoActivo: 'activo',
  atributoDelNombre: 'nombre',
  columnas: [
    { atributo: 'nombre', etiqueta: 'usuario.nombre.etiqueta', debajo: { atributo: 'correo', texto: null } },
    { atributo: 'rol', etiqueta: 'usuario.rol.etiqueta', valores: 'usuario.rol' },
  ],
  textos: {
    descripcion: null,
    vacia: 'usuario.pantalla.vacia',
    nuevo: 'usuario.pantalla.nuevo',
    crear: 'usuario.pantalla.crear',
    tituloEditar: 'usuario.pantalla.titulo_editar',
    notaFormulario: null,
    notaContrasenaDeOtro: 'usuario.pantalla.nota_contrasena_de_otro',
    tituloLista: 'usuario.pantalla.titulo_lista',
    activo: 'usuario.pantalla.activo',
    inactivo: 'usuario.pantalla.inactivo',
    editarFila: 'usuario.pantalla.editar_fila',
    desactivarFila: 'usuario.pantalla.desactivar_fila',
    reactivarFila: 'usuario.pantalla.reactivar_fila',
    alGuardar: 'usuario.pantalla.al_guardar',
    alDesactivar: 'usuario.pantalla.al_desactivar',
    alReactivar: 'usuario.pantalla.al_reactivar',
    notaLista: 'usuario.pantalla.nota_lista',
    resumen: { frase: 'usuario.pantalla.resumen', activos: 'usuario.pantalla.resumen_activos' },
  },
  confirmarDesactivar: null,
  formulario: 'al_lado',
  limiteDeActivos: { maximo: 25, mensaje: 'usuario.reglas.maximo_usuarios_activos' },
  filaPropia: { marca: 'usuario.pantalla.fila_propia', sinAcciones: ['desactivar'] },
};

/** Los campos del formulario para registrar, en orden: se llaman como en la API, también en sus errores. */
const CAMPOS_PARA_CREAR = ['nombre', 'correo', 'contrasena', 'rol'];

/** Los campos del formulario para editar, en orden. */
const CAMPOS_PARA_EDITAR = ['nombre', 'correo', 'contrasena', 'rol'];

/**
 * Los valores de un registro nuevo: vacíos, el valor por defecto del JSON o el de pantalla.inicial.
 *
 * @returns {object} los valores del formulario
 */
function valoresParaCrear() {
  return { nombre: '', correo: '', contrasena: '', rol: 'USUARIO_FINAL' };
}

/**
 * Los valores de un registro para editarlo: los de su fila.
 *
 * @param {object} fila el registro, como lo responde la API
 * @returns {object} los valores del formulario
 */
function valoresParaEditar(fila) {
  return {
    nombre: fila.nombre ?? '',
    correo: fila.correo ?? '',
    contrasena: fila.contrasena ?? '',
    rol: fila.rol ?? '',
  };
}

/**
 * Los datos para la API al editar: un campo opcional vacío va como null, así queda el valor que había, como una
 * contraseña que no se cambia.
 *
 * @param {object} valores los valores del formulario
 * @returns {object} los datos
 */
function datosParaEditar(valores) {
  return { ...valores, contrasena: valores.contrasena === '' ? null : valores.contrasena };
}

/**
 * Los campos del formulario, en el orden del JSON.
 *
 * @param {object} props las propiedades
 * @param {boolean} props.paraCrear si es el formulario para registrar
 * @param {boolean} props.propia si se edita la fila de quien tiene la sesión: lo que impiden sus reglas no se cambia
 * @returns {import('react').ReactElement} los campos
 */
function Campos({ paraCrear, propia }) {
  const { t } = useTranslation();
  return (
    <>
      <CampoDeTexto nombre="nombre" etiqueta={t('usuario.nombre.etiqueta')} autoComplete="off" />
      <CampoDeTexto nombre="correo" etiqueta={t('usuario.correo.etiqueta')} ayuda={t('usuario.correo.ayuda')} type="email" autoComplete="off" />
      {paraCrear && (
        <CampoContrasena nombre="contrasena" etiqueta={t('usuario.pantalla.contrasena_al_crear')} ayuda={t('usuario.pantalla.ayuda_contrasena_al_crear')} autoComplete="new-password" />
      )}
      {!paraCrear && !propia && (
        <CampoContrasena nombre="contrasena" etiqueta={t('usuario.pantalla.contrasena_al_editar')} ayuda={t('usuario.pantalla.ayuda_contrasena_al_editar')} autoComplete="new-password" />
      )}
      {propia && <MensajeDeLaFilaPropia mensaje={t('usuario.reglas.no_cambiar_su_propia_contrasena')} cambiarContrasena />}
      <CampoDeLista nombre="rol" etiqueta={t('usuario.rol.etiqueta')} opciones={[{ valor: 'USUARIO_FINAL', etiqueta: t('usuario.rol.USUARIO_FINAL') }, { valor: 'ADMIN_EMPRESA', etiqueta: t('usuario.rol.ADMIN_EMPRESA') }]} soloLectura={propia ? t('usuario.reglas.no_cambiar_su_propio_rol') : null} />
    </>
  );
}

/** El formulario: sus campos, sus valores y sus validaciones. */
const FORMULARIO = {
  Campos,
  camposParaCrear: CAMPOS_PARA_CREAR,
  valoresParaCrear,
  esquemaParaCrear,
  camposParaEditar: CAMPOS_PARA_EDITAR,
  valoresParaEditar,
  esquemaParaEditar,
  datosParaEditar,
  contrasenaAlEditar: 'contrasena',
};

/**
 * La pantalla de usuarios (RF-03): la lista, el formulario y las acciones de cada fila. Lo que su JSON
 * no puede declarar llega en extensiones, desde Rutas.jsx.
 *
 * @param {object} props las propiedades
 * @param {import('../../crud/PantallaDeRegistros.jsx').Extensiones} [props.extensiones] lo escrito a mano para
 *   esta pantalla
 * @returns {import('react').ReactElement} la pantalla
 */
export default function UsuarioPantalla({ extensiones }) {
  return (
    <PantallaDeRegistros pantalla={PANTALLA} api={api} formulario={FORMULARIO}
      extensiones={extensiones} />
  );
}
