/*
 * Nombre: EmpresaPantalla.jsx
 * Descripción: Pantalla de empresa (RF-02): su lista, su formulario y sus acciones.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.7)
 * Versión: 1.7
 * Entidades: empresa 1.3, usuario 1.6
 */
import { useTranslation } from 'react-i18next';
import { CampoContrasena } from '../../componentes/CampoContrasena.jsx';
import { CampoDeTexto } from '../../componentes/CampoDeTexto.jsx';
import { GrupoDeCampos } from '../../crud/GrupoDeCampos.jsx';
import { PantallaDeRegistros } from '../../crud/PantallaDeRegistros.jsx';
import * as api from './empresaApi.js';
import { esquemaParaCrear, esquemaParaEditar } from './empresaEsquemas.js';

/**
 * Lo que dice de la pantalla specs/entidades/empresa.json: los roles de cada acción, las columnas de la lista
 * y las claves de sus textos, o null los que el JSON no trae.
 *
 * @type {import('../../crud/PantallaDeRegistros.jsx').DescripcionDePantalla}
 */
const PANTALLA = {
  entidad: 'empresa',
  titulo: 'empresa.etiqueta.plural',
  operaciones: { crear: ['ADMIN_PLATAFORMA'], editar: ['ADMIN_PLATAFORMA'], desactivar: ['ADMIN_PLATAFORMA'], reactivar: ['ADMIN_PLATAFORMA'] },
  atributoActivo: 'activa',
  atributoDelNombre: 'nombre',
  columnas: [
    { atributo: 'nombre', etiqueta: 'empresa.pantalla.columna_nombre', debajo: { atributo: 'nit', texto: 'empresa.pantalla.debajo_del_nombre' } },
    { atributo: 'ciudad', etiqueta: 'empresa.ciudad.etiqueta' },
  ],
  textos: {
    descripcion: null,
    vacia: 'empresa.pantalla.vacia',
    nuevo: 'empresa.pantalla.nuevo',
    crear: 'empresa.pantalla.crear',
    tituloEditar: 'empresa.pantalla.titulo_editar',
    notaFormulario: 'empresa.pantalla.nota_formulario',
    tituloLista: 'empresa.pantalla.titulo_lista',
    activo: 'empresa.pantalla.activo',
    inactivo: 'empresa.pantalla.inactivo',
    editarFila: 'empresa.pantalla.editar_fila',
    desactivarFila: 'empresa.pantalla.desactivar_fila',
    reactivarFila: 'empresa.pantalla.reactivar_fila',
    notaLista: 'empresa.pantalla.nota_lista',
    resumen: { frase: 'empresa.pantalla.resumen', total: 'empresa.pantalla.resumen_total', activos: 'empresa.pantalla.resumen_activos', inactivos: 'empresa.pantalla.resumen_inactivos' },
  },
  confirmarDesactivar: { titulo: 'empresa.pantalla.confirmar_desactivar', detalle: 'empresa.pantalla.confirmar_desactivar_detalle' },
};

/** Los campos del formulario para registrar, en orden: se llaman como en la API, también en sus errores. */
const CAMPOS_PARA_CREAR = ['nombre', 'nit', 'ciudad', 'administrador.nombre', 'administrador.correo', 'administrador.contrasena'];

/** Los campos del formulario para editar, en orden. */
const CAMPOS_PARA_EDITAR = ['nombre', 'nit', 'ciudad'];

/**
 * Los valores de un registro nuevo: vacíos, o el valor por defecto del JSON.
 *
 * @returns {object} los valores del formulario
 */
function valoresParaCrear() {
  return { nombre: '', nit: '', ciudad: '', administrador: { nombre: '', correo: '', contrasena: '' } };
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
    nit: fila.nit ?? '',
    ciudad: fila.ciudad ?? '',
  };
}

/**
 * Los campos del formulario, en el orden del JSON.
 *
 * @param {object} props las propiedades
 * @param {boolean} props.paraCrear si es el formulario para registrar, que lleva también el grupo «Primer administrador»
 * @returns {import('react').ReactElement} los campos
 */
function Campos({ paraCrear }) {
  const { t } = useTranslation();
  return (
    <>
      <GrupoDeCampos titulo={t('empresa.etiqueta.singular')}>
        <CampoDeTexto nombre="nombre" etiqueta={t('empresa.nombre.etiqueta')} autoComplete="off" />
        <CampoDeTexto nombre="nit" etiqueta={t('empresa.nit.etiqueta')} ayuda={t('empresa.nit.ayuda')} autoComplete="off" />
        <CampoDeTexto nombre="ciudad" etiqueta={t('empresa.ciudad.etiqueta')} autoComplete="off" />
      </GrupoDeCampos>
      {paraCrear && (
        <GrupoDeCampos titulo={t('empresa.administrador.etiqueta')}>
          <CampoDeTexto nombre="administrador.nombre" etiqueta={t('usuario.nombre.etiqueta')} autoComplete="off" />
          <CampoDeTexto nombre="administrador.correo" etiqueta={t('usuario.correo.etiqueta')} ayuda={t('usuario.correo.ayuda')} type="email" autoComplete="off" />
          <CampoContrasena nombre="administrador.contrasena" etiqueta={t('empresa.pantalla.contrasena_al_crear')} ayuda={t('empresa.pantalla.ayuda_contrasena_al_crear')} autoComplete="new-password" />
        </GrupoDeCampos>
      )}
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
};

/**
 * La pantalla de empresas (RF-02): la lista, el formulario y las acciones de cada fila. Lo que su JSON
 * no puede declarar llega en extensiones, desde Rutas.jsx.
 *
 * @param {object} props las propiedades
 * @param {import('../../crud/PantallaDeRegistros.jsx').Extensiones} [props.extensiones] lo escrito a mano para
 *   esta pantalla
 * @returns {import('react').ReactElement} la pantalla
 */
export default function EmpresaPantalla({ extensiones }) {
  return (
    <PantallaDeRegistros pantalla={PANTALLA} api={api} formulario={FORMULARIO}
      extensiones={extensiones} />
  );
}
