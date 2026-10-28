/*
 * Nombre: rangos.js
 * Descripción: Datos y pasos que comparten las pruebas de la pantalla de rangos (RF-13): las filas como las responde
 *              la API, las de D-10 y unas propias de una empresa; el mensaje de la regla del orden; abrir la pantalla
 *              con la API simulada; y encontrar, escribir y guardar los campos de cada categoría.
 * Fecha de creación: 28/10/2026
 * Actualización: 28/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { screen, within } from '@testing-library/react';
import { abrirApp, conSesion, USUARIOS } from './ayudas.jsx';
import { apiEnBloqueSimulada } from './crud.js';

/** La ruta de la pantalla y la de la API de los rangos. */
export const RUTA_RANGOS = '/rangos-recomendacion';
export const API_RANGOS = '/api/v1/rangos-recomendacion';

/** Los rangos por defecto (D-10, anexo A.3), como los responde la API. */
export const POR_DEFECTO = [
  { categoria: 'PREMIUM', limiteInferior: 96.0, recomendacion: 'Apto para mercado especial o de exportación.' },
  {
    categoria: 'ESTANDAR', limiteInferior: 90.0,
    recomendacion: 'Apto para venta estándar. Retirar a mano los granos defectuosos podría subirlo de categoría.',
  },
  {
    categoria: 'CORRIENTE', limiteInferior: 82.0,
    recomendacion: 'Retirar los granos defectuosos antes de vender, y revisar la fermentación y el secado.',
  },
  {
    categoria: 'FUERA_DE_NORMA', limiteInferior: 0.0,
    recomendacion: 'No venderlo como cacao de calidad: hacer una selección completa y revisar todo el proceso después '
      + 'de la cosecha.',
  },
];

/** Unos rangos propios de una empresa, distintos de los de D-10. */
export const PROPIOS = [
  { categoria: 'PREMIUM', limiteInferior: 97.5, recomendacion: 'Para el comprador de exportación.' },
  { categoria: 'ESTANDAR', limiteInferior: 91.0, recomendacion: 'Para el comprador local.' },
  { categoria: 'CORRIENTE', limiteInferior: 80.0, recomendacion: 'Seleccionar a mano antes de vender.' },
  { categoria: 'FUERA_DE_NORMA', limiteInferior: 0.0, recomendacion: 'No vender este lote.' },
];

/** Las categorías, en el orden de su lista: de la mejor a la peor. */
export const CATEGORIAS = ['Premium o Especial', 'Estándar', 'Corriente', 'Fuera de norma'];

/** Las etiquetas de los campos de cada fila. */
export const LIMITE = 'Límite inferior (% de granos sanos)';
export const RECOMENDACION = 'Recomendación';

/** Los mensajes que ve el administrador. */
export const MENSAJES = {
  orden: 'Los límites deben bajar en orden: Premium o Especial > Estándar > Corriente > 0 %.',
  fijo: 'El límite de Fuera de norma es fijo: 0 %.',
  numero: 'Escribe un número, como 90,5.',
  guardados: 'Se guardaron los rangos.',
  restaurados: 'Se restauraron los valores por defecto.',
  seQuitan: 'Se pierden los límites y los textos propios de la empresa.',
};

/**
 * Abre la pantalla de rangos como el administrador de la empresa, con la API simulada.
 *
 * @param {object[]} [filas] las filas que tiene la API
 * @param {object[]} [porDefecto] las filas a las que vuelve al restaurar
 * @returns {{ usuario: import('@testing-library/user-event').UserEvent,
 *   simulada: import('./crud.js').ApiEnBloqueSimulada }} el usuario de la prueba y la API simulada
 */
export function abrirRangos(filas = POR_DEFECTO, porDefecto = POR_DEFECTO) {
  conSesion(USUARIOS.ADMIN_EMPRESA);
  const simulada = apiEnBloqueSimulada(API_RANGOS, filas, porDefecto);
  const { usuario } = abrirApp(RUTA_RANGOS);
  return { usuario, simulada };
}

/**
 * Un campo de una categoría, cuando terminan de cargar las filas.
 *
 * @param {string} categoria el nombre de la categoría, como «Estándar»
 * @param {string} [etiqueta] la etiqueta del campo; por defecto, el límite inferior
 * @returns {Promise<HTMLElement>} el campo
 */
export async function campoDe(categoria, etiqueta = LIMITE) {
  return within(await screen.findByRole('group', { name: categoria })).getByLabelText(etiqueta);
}

/**
 * Escribe un límite en el campo de una categoría, en lugar del que tenía.
 *
 * @param {import('@testing-library/user-event').UserEvent} usuario el usuario de la prueba
 * @param {string} categoria el nombre de la categoría
 * @param {string} texto lo que se escribe, como «95,9»
 */
export async function escribirLimite(usuario, categoria, texto) {
  const campo = await campoDe(categoria);
  await usuario.clear(campo);
  await usuario.type(campo, texto);
}

/**
 * Pulsa «Guardar cambios».
 *
 * @param {import('@testing-library/user-event').UserEvent} usuario el usuario de la prueba
 */
export async function guardar(usuario) {
  await usuario.click(screen.getByRole('button', { name: 'Guardar cambios' }));
}

/**
 * Las filas que se enviaron a la API cada vez que se guardó.
 *
 * @param {import('./crud.js').ApiEnBloqueSimulada} simulada la API simulada
 * @returns {object[][]} las filas de cada PUT, en orden
 */
export function enviadas(simulada) {
  return simulada.peticiones.filter(({ metodo }) => metodo === 'PUT').map(({ cuerpo }) => cuerpo.filas);
}

/**
 * Lo que dice el resumen «Así quedan los rangos» de una categoría, sin su nombre.
 *
 * @param {string} categoria el nombre de la categoría
 * @param {string} [titulo] el título del resumen, en el idioma de la app
 * @returns {Promise<string>} lo que abarca, como «de 90,0 a 95,9 %», o «revisa el límite»
 */
export async function enElResumen(categoria, titulo = 'Así quedan los rangos') {
  const resumen = await screen.findByRole('region', { name: titulo });
  const item = within(resumen).getByText(categoria).closest('li');
  return item.textContent.replace(categoria, '').trim();
}
