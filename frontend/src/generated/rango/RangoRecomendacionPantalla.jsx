/*
 * Nombre: RangoRecomendacionPantalla.jsx
 * Descripción: Pantalla de rango_recomendacion (RF-13): una fila por cada valor de categoría, editadas
 *              juntas.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.10)
 * Versión: 1.10
 * Entidades: rango_recomendacion 1.3
 */
import { PantallaEnBloque } from '../../crud/PantallaEnBloque.jsx';
import * as api from './rangoRecomendacionApi.js';
import { esquemaDelBloque } from './rangoRecomendacionEsquemas.js';

/**
 * Lo que dice de la pantalla specs/entidades/rango_recomendacion.json: los roles de cada acción, la lista que identifica
 * cada fila, con sus valores en orden, los campos de cada fila y las claves de sus textos, o null los que el JSON no
 * trae.
 *
 * @type {import('../../crud/PantallaEnBloque.jsx').DescripcionDelBloque}
 */
const PANTALLA = {
  entidad: 'rango_recomendacion',
  titulo: 'rango_recomendacion.etiqueta.plural',
  operaciones: { editar: ['ADMIN_EMPRESA'], restaurarPorDefecto: ['ADMIN_EMPRESA'] },
  clave: {
    atributo: 'categoria',
    valores: [
      { codigo: 'PREMIUM', etiqueta: 'rango_recomendacion.categoria.PREMIUM' },
      { codigo: 'ESTANDAR', etiqueta: 'rango_recomendacion.categoria.ESTANDAR' },
      { codigo: 'CORRIENTE', etiqueta: 'rango_recomendacion.categoria.CORRIENTE' },
      { codigo: 'FUERA_DE_NORMA', etiqueta: 'rango_recomendacion.categoria.FUERA_DE_NORMA' },
    ],
  },
  campos: [
    { nombre: 'limiteInferior', tipo: 'decimal', etiqueta: 'rango_recomendacion.limite_inferior.etiqueta', escala: 1, soloLecturaSi: { codigo: 'FUERA_DE_NORMA', mensaje: 'rango_recomendacion.limite_inferior.solo_lectura_si' } },
    { nombre: 'recomendacion', tipo: 'texto', etiqueta: 'rango_recomendacion.recomendacion.etiqueta', variasLineas: true },
  ],
  textos: {
    descripcion: 'rango_recomendacion.pantalla.descripcion',
    tituloEditar: 'rango_recomendacion.pantalla.titulo_editar',
    notaFormulario: 'rango_recomendacion.pantalla.nota_formulario',
    alGuardar: 'rango_recomendacion.pantalla.al_guardar',
    notaRestaurar: 'rango_recomendacion.pantalla.nota_restaurar',
    confirmarRestaurarDetalle: 'rango_recomendacion.pantalla.confirmar_restaurar_detalle',
    alRestaurar: 'rango_recomendacion.pantalla.al_restaurar',
  },
};

/**
 * La pantalla de rangos de recomendación (RF-13): una fila por cada categoría, todas en un solo formulario. Lo
 * que su JSON no puede declarar llega en extensiones, desde Rutas.jsx.
 *
 * @param {object} props las propiedades
 * @param {import('../../crud/PantallaEnBloque.jsx').ExtensionesDelBloque} [props.extensiones] lo escrito a mano
 *   para esta pantalla
 * @returns {import('react').ReactElement} la pantalla
 */
export default function RangoRecomendacionPantalla({ extensiones }) {
  return <PantallaEnBloque pantalla={PANTALLA} api={api} esquema={esquemaDelBloque} extensiones={extensiones} />;
}
