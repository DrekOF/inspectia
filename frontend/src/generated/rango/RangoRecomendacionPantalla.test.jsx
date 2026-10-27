/*
 * Nombre: RangoRecomendacionPantalla.test.jsx
 * Descripción: Pruebas generadas de la pantalla de rango_recomendacion: las filas, la solo lectura,
 *              guardar, los errores de la API, restaurar y permisos.
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.10)
 * Versión: 1.10
 * Entidades: rango_recomendacion 1.3
 */
import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { abrirApp, conSesion, USUARIOS } from '../../pruebas/ayudas.jsx';
import { apiEnBloqueSimulada } from '../../pruebas/crud.js';
import { problema, servidor } from '../../pruebas/servidor.js';

/** La pantalla en la app y su ruta en la API. */
const RUTA = '/rangos-recomendacion';
const API = '/api/v1/rangos-recomendacion';

/** Las filas como las responde la API: las de los valores por defecto del JSON, en el orden de su lista. */
const FILAS = [
  { categoria: 'PREMIUM', limiteInferior: 96.0, recomendacion: 'Apto para mercado especial o de exportación.' },
  { categoria: 'ESTANDAR', limiteInferior: 90.0, recomendacion: 'Apto para venta estándar. Retirar a mano los granos defectuosos podría subirlo de categoría.' },
  { categoria: 'CORRIENTE', limiteInferior: 82.0, recomendacion: 'Retirar los granos defectuosos antes de vender, y revisar la fermentación y el secado.' },
  { categoria: 'FUERA_DE_NORMA', limiteInferior: 0.0, recomendacion: 'No venderlo como cacao de calidad: hacer una selección completa y revisar todo el proceso después de la cosecha.' },
];

/** Las mismas filas, con un texto propio de la empresa en cada una. */
const CON_TEXTO_PROPIO = [
  { categoria: 'PREMIUM', limiteInferior: 96.0, recomendacion: 'Texto propio de la empresa.' },
  { categoria: 'ESTANDAR', limiteInferior: 90.0, recomendacion: 'Texto propio de la empresa.' },
  { categoria: 'CORRIENTE', limiteInferior: 82.0, recomendacion: 'Texto propio de la empresa.' },
  { categoria: 'FUERA_DE_NORMA', limiteInferior: 0.0, recomendacion: 'Texto propio de la empresa.' },
];

/** Los errores de la API que son de todo el bloque: los de sus reglas, que no tienen campo, y el de las filas. */
const ERRORES_DEL_BLOQUE = [
  { campo: null, mensaje: 'Los límites deben bajar en orden: Premium o Especial > Estándar > Corriente > 0 %.' },
  { campo: 'filas', mensaje: 'Envía una fila por cada categoría, sin repetir: Premium o Especial, Estándar, Corriente y Fuera de norma.' },
];

/**
 * El formulario, cuando terminan de cargar las filas.
 *
 * @returns {Promise<HTMLElement>} el formulario
 */
const formulario = () => screen.findByRole('form', { name: 'Editar los rangos' });

/**
 * La fila de un valor de categoría, por su nombre.
 *
 * @param {string} nombre el nombre del valor
 * @returns {Promise<HTMLElement>} la fila
 */
const filaDe = (nombre) => screen.findByRole('group', { name: nombre });

describe('pantalla de rango_recomendacion (generada)', () => {
  it('RF-13 · la pantalla de rango_recomendacion muestra una fila por cada valor de categoria, en su orden, con sus datos', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    apiEnBloqueSimulada(API, FILAS);
    abrirApp(RUTA);

    const filas = within(await formulario()).getAllByRole('group');
    expect(filas.map((fila) => fila.querySelector('legend').textContent))
      .toEqual(['Premium o Especial', 'Estándar', 'Corriente', 'Fuera de norma']);
    expect(within(filas[0]).getByLabelText('Límite inferior (% de granos sanos)')).toHaveValue('96,0');
    expect(within(filas[0]).getByLabelText('Recomendación')).toHaveValue('Apto para mercado especial o de exportación.');
  });

  it('RF-13 · en la fila de FUERA_DE_NORMA, limiteInferior es de solo lectura, con el mensaje de su regla', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    apiEnBloqueSimulada(API, FILAS);
    abrirApp(RUTA);

    const campo = within(await filaDe('Fuera de norma')).getByLabelText('Límite inferior (% de granos sanos)');
    expect(campo).toHaveAttribute('readonly');
    expect(campo).toHaveAccessibleDescription(expect.stringContaining('El límite de Fuera de norma es fijo: 0 %.'));
  });

  it('RF-13 · guardar envía todas las filas juntas, con los números escritos en el formulario, y muestra el mensaje del JSON', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    const simulada = apiEnBloqueSimulada(API, FILAS);
    const { usuario } = abrirApp(RUTA);

    const primera = await filaDe('Premium o Especial');
    const numero = within(primera).getByLabelText('Límite inferior (% de granos sanos)');
    await usuario.clear(numero);
    await usuario.type(numero, '96,0');
    const texto = within(primera).getByLabelText('Recomendación');
    await usuario.clear(texto);
    await usuario.type(texto, 'Texto propio de la empresa.');
    await usuario.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByText('Se guardaron los rangos.')).toBeInTheDocument();
    expect(simulada.peticiones).toContainEqual({ metodo: 'PUT', ruta: API, cuerpo: { filas: [
      { categoria: 'PREMIUM', limiteInferior: 96.0, recomendacion: 'Texto propio de la empresa.' },
      { categoria: 'ESTANDAR', limiteInferior: 90.0, recomendacion: 'Apto para venta estándar. Retirar a mano los granos defectuosos podría subirlo de categoría.' },
      { categoria: 'CORRIENTE', limiteInferior: 82.0, recomendacion: 'Retirar los granos defectuosos antes de vender, y revisar la fermentación y el secado.' },
      { categoria: 'FUERA_DE_NORMA', limiteInferior: 0.0, recomendacion: 'No venderlo como cacao de calidad: hacer una selección completa y revisar todo el proceso después de la cosecha.' },
    ] } });
  });

  it('RF-13 · un dato que no cumple su validación se marca en su campo y no se envía nada', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    const simulada = apiEnBloqueSimulada(API, FILAS);
    const { usuario } = abrirApp(RUTA);

    const primera = await filaDe('Premium o Especial');
    const texto = within(primera).getByLabelText('Recomendación');
    await usuario.clear(texto);
    await usuario.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await within(primera).findByText('Este dato es obligatorio.')).toBeInTheDocument();
    expect(texto).toHaveAttribute('aria-invalid', 'true');
    expect(simulada.peticiones.filter(({ metodo }) => metodo === 'PUT')).toEqual([]);
  });

  it('RF-13 · un error de la API en el campo de una fila queda en ese campo', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    apiEnBloqueSimulada(API, FILAS);
    servidor.use(http.put(API, () => problema(400, 'Revisa los datos marcados.', {
      errores: [{ campo: 'filas[1].recomendacion', mensaje: 'Puede tener como máximo 300 caracteres.' }],
    })));
    const { usuario } = abrirApp(RUTA);

    await formulario();
    await usuario.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    const segunda = await filaDe('Estándar');
    expect(await within(segunda).findByText('Puede tener como máximo 300 caracteres.')).toBeInTheDocument();
    expect(within(segunda).getByLabelText('Recomendación')).toHaveAttribute('aria-invalid', 'true');
  });

  it.each(ERRORES_DEL_BLOQUE)('RF-13 · un error de la API del bloque, sin campo o en filas, va arriba del formulario con su mensaje: $campo',
    async (error) => {
      conSesion(USUARIOS.ADMIN_EMPRESA);
      apiEnBloqueSimulada(API, FILAS);
      servidor.use(http.put(API, () => problema(400, 'Revisa los datos marcados.', { errores: [error] })));
      const { usuario } = abrirApp(RUTA);

      await formulario();
      await usuario.click(screen.getByRole('button', { name: 'Guardar cambios' }));

      expect(await screen.findByRole('alert')).toHaveTextContent(error.mensaje);
    });

  it('RF-13 · restaurar pide confirmación con el texto del JSON y, al confirmar, vuelve a los valores por defecto', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    const simulada = apiEnBloqueSimulada(API, CON_TEXTO_PROPIO, FILAS);
    const { usuario } = abrirApp(RUTA);

    await formulario();
    await usuario.click(screen.getByRole('button', { name: 'Restaurar valores por defecto' }));
    const dialogo = screen.getByRole('dialog', { name: '¿Restaurar los valores por defecto?' });
    expect(dialogo).toHaveTextContent('Se pierden los límites y los textos propios de la empresa.');
    await usuario.click(within(dialogo).getByRole('button', { name: 'Restaurar' }));

    expect(await screen.findByText('Se restauraron los valores por defecto.')).toBeInTheDocument();
    expect(simulada.peticiones).toContainEqual({ metodo: 'POST', ruta: `${API}/restaurar-por-defecto`, cuerpo: null });
    expect(within(await filaDe('Premium o Especial')).getByLabelText('Recomendación'))
      .toHaveValue('Apto para mercado especial o de exportación.');
  });

  it('RF-13 · al cancelar la confirmación de restaurar no cambia nada', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    const simulada = apiEnBloqueSimulada(API, FILAS);
    const { usuario } = abrirApp(RUTA);

    await formulario();
    await usuario.click(screen.getByRole('button', { name: 'Restaurar valores por defecto' }));
    await usuario.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancelar' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(simulada.peticiones.filter(({ metodo }) => metodo === 'POST')).toEqual([]);
  });

  it.each(['USUARIO_FINAL', 'ADMIN_PLATAFORMA'])(
    'RNF-04 · la pantalla de rango_recomendacion solo la abren los roles de listar: %s ve que no tiene permiso', async (rol) => {
      conSesion(USUARIOS[rol]);
      abrirApp(RUTA);

      expect(await screen.findByRole('heading', { name: 'Sin permiso' })).toBeInTheDocument();
    });

  it('RNF-06 · sin conexión, la pantalla de rango_recomendacion dice qué pasó y qué hacer, y se puede reintentar', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    servidor.use(http.get(API, () => HttpResponse.error()));
    const { usuario } = abrirApp(RUTA);

    expect(await screen.findByText('No se pudo conectar con el servidor. Revisa tu conexión a internet e intenta de nuevo.'))
      .toBeInTheDocument();
    apiEnBloqueSimulada(API, FILAS);
    await usuario.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await formulario()).toBeInTheDocument();
  });
});
