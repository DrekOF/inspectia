/*
 * Nombre: ResultadoDelLote.test.jsx
 * Descripción: Pruebas de la pantalla Resultado del lote (RF-10, RF-11, RF-12, RF-14 C3): la cifra guardada con su
 *              rango probable, recortado en los dos extremos, en español y en inglés (D-43); los granos que contó el
 *              modelo con la frase de la corrección; la categoría probable; la sugerencia de otra muestra; las fotos
 *              marcadas, con el color y el símbolo de cada clase, la marca de los dudosos, el globo con la clase al
 *              pasar el cursor, al tocar y con el teclado, sin la confianza; las pestañas; la vista de la
 *              administradora; el celular; la espera del análisis, su avance y su final (RNF-01 C4, RNF-02 C3), y los
 *              errores (RNF-03, RNF-04, RNF-06).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { act, fireEvent, screen, waitFor, within } from '@testing-library/react';
import { http } from 'msw';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import i18n from '../i18n/i18n.js';
import { abrirApp, conSesion, USUARIOS } from '../pruebas/ayudas.jsx';
import { LOTE_PENDIENTE } from '../pruebas/lotes.js';
import { anchoDePantalla } from '../pruebas/preparacion.js';
import {
  avance, cargarImagen, conAvance, conResultado, granosDeFoto, fotoMarcada, RECOMENDACION_ESTANDAR, resultadoAnalizado,
  resultadoPendiente,
} from '../pruebas/resultados.js';
import { problema, servidor } from '../pruebas/servidor.js';
import { marcasDeFoto } from '../tema/tokens.js';
import { TARDA_MAS_DE } from './EsperaDelAnalisis.jsx';
import { FALLOS_ANTES_DE_PARAR, INTERVALO_DE_CONSULTA } from './useAnalisisEnCurso.js';

const LOTE = LOTE_PENDIENTE.id;
const RUTA = `/lotes/${LOTE}`;
const EN_ESPERA = 'Tu análisis está en espera: empieza apenas termine el que va antes.';
const EN_PROCESO = 'Analizando la muestra…';
const LISTO = 'Listo: este es el resultado del lote L-0143.';

/**
 * La imagen de una foto marcada, cargada como si el navegador la hubiera traído, con su tamaño original.
 *
 * @param {number} [numero] el número de la foto
 * @returns {Promise<HTMLImageElement>} la imagen
 */
async function imagenCargada(numero = 1) {
  const imagen = await screen.findByRole('img', { name: `Foto ${numero} de la muestra, con sus granos marcados` });
  cargarImagen(imagen);
  return imagen;
}

/**
 * Los granos de una foto: el grupo de sus marcas, cuando aparece.
 *
 * @param {number} [numero] el número de la foto
 * @returns {Promise<HTMLElement>} el grupo
 */
function granosDeLaFoto(numero = 1) {
  return screen.findByRole('group', { name: `Granos de la foto ${numero}` });
}

/**
 * El aviso de la página para los lectores de pantalla: el primer role="status", que va antes del contenido y no cambia
 * con cada consulta del avance.
 *
 * @returns {HTMLElement} la región
 */
function anuncioDeLaPagina() {
  return screen.getAllByRole('status')[0];
}

/**
 * Hace como si la pestaña del navegador se ocultara o se volviera a ver.
 *
 * @param {boolean} oculta si queda oculta
 */
function pestanaOculta(oculta) {
  Object.defineProperty(document, 'hidden', { configurable: true, get: () => oculta });
  document.dispatchEvent(new Event('visibilitychange'));
}

/**
 * Deja pasar el tiempo de los temporizadores falsos y lo que sus llamadas dejaron pendiente.
 *
 * @param {number} milisegundos cuánto tiempo
 */
async function avanzar(milisegundos) {
  await act(() => vi.advanceTimersByTimeAsync(milisegundos));
}

/**
 * Espera a que termine la consulta del avance en vuelo, deja pasar el intervalo y espera a que llegue la siguiente.
 *
 * @param {{ veces: number, enVuelo: number }} consultas lo que anotó la API simulada del avance
 * @param {number} vez el número de la consulta que se espera, desde 1
 */
async function siguienteConsulta(consultas, vez) {
  await waitFor(() => expect(consultas.enVuelo).toBe(0));
  await avanzar(INTERVALO_DE_CONSULTA);
  await waitFor(() => expect(consultas.veces).toBe(vez));
}

describe('Resultado del lote', () => {
  afterEach(() => {
    vi.useRealTimers();
    anchoDePantalla(1280);
    delete document.hidden;
  });

  describe('lote analizado', () => {
    beforeEach(() => conSesion(USUARIOS.USUARIO_FINAL));

    it('RF-11 · C3 · el resultado muestra las cifras guardadas del lote, sin recalcularlas: la cifra corregida, su rango probable y los granos que contó el modelo', async () => {
      conResultado(LOTE, resultadoAnalizado());
      abrirApp(RUTA);

      expect(await screen.findByRole('heading', { level: 1, name: 'Resultado del lote L-0143' })).toBeInTheDocument();
      expect(screen.getByText('La Esperanza · Recibido el 20/10/2026 · Analizado el 23/11/2026 a las 11:42 a. m.'))
        .toBeInTheDocument();
      const resumen = screen.getByRole('region', { name: 'Resumen del resultado' });
      expect(within(resumen).getByText('90,6 %')).toBeInTheDocument();
      expect(within(resumen).getByText('de granos sanos')).toBeInTheDocument();
      expect(within(resumen).getByText('Rango probable: entre 83,1 y 98,1 %')).toBeInTheDocument();
      expect(within(resumen).getAllByRole('term').map((nombre) => nombre.textContent)).toEqual(['Granos', 'Sanos', 'No sanos']);
      expect(within(resumen).getAllByRole('definition').map((valor) => valor.textContent)).toEqual(['100', '87', '13']);
      expect(screen.queryByText('87,0 %')).not.toBeInTheDocument();
      expect(screen.queryByText(/±/)).not.toBeInTheDocument();
    });

    it('RF-10 · con 100,0 % y margen de 5,7 puntos, el rango probable se recorta arriba y su extremo va sin decimales: «entre 94,3 y 100 %» (prueba de límite)', async () => {
      conResultado(LOTE, resultadoAnalizado({
        porcentajeSanos: 100.0, margen: 5.7, granosSanos: 93, categoria: 'PREMIUM', sugerirOtraMuestra: false,
      }));
      abrirApp(RUTA);

      const resumen = await screen.findByRole('region', { name: 'Resumen del resultado' });
      expect(within(resumen).getByText('100,0 %')).toBeInTheDocument();
      expect(within(resumen).getByText('Rango probable: entre 94,3 y 100 %')).toBeInTheDocument();
      expect(within(resumen).getByText('Premium o Especial')).toBeInTheDocument();
    });

    it('RF-10 · con 0,0 % y margen de 11,0 puntos, el rango probable se recorta abajo: «entre 0 y 11,0 %» (prueba de límite)', async () => {
      conResultado(LOTE, resultadoAnalizado({
        porcentajeSanos: 0.0, margen: 11.0, granosSanos: 40, categoria: 'FUERA_DE_NORMA', sugerirOtraMuestra: false,
      }));
      abrirApp(RUTA);

      const resumen = await screen.findByRole('region', { name: 'Resumen del resultado' });
      expect(within(resumen).getByText('0,0 %')).toBeInTheDocument();
      expect(within(resumen).getByText('Rango probable: entre 0 y 11,0 %')).toBeInTheDocument();
      expect(within(resumen).getByText('40 de 100 marcados como sanos')).toBeInTheDocument();
    });

    it('RF-10 · en inglés, la cifra y el rango llevan punto decimal, y las fechas y la hora van en el formato del inglés', async () => {
      await i18n.changeLanguage('en');
      conResultado(LOTE, resultadoAnalizado());
      abrirApp(RUTA);

      expect(await screen.findByRole('heading', { level: 1, name: 'Result of lot L-0143' })).toBeInTheDocument();
      expect(screen.getByText('La Esperanza · Received on 10/20/2026 · Analyzed on 11/23/2026 at 11:42 AM')).toBeInTheDocument();
      const resumen = screen.getByRole('region', { name: 'Result summary' });
      expect(within(resumen).getByText('90.6 %')).toBeInTheDocument();
      expect(within(resumen).getByText('Probable range: between 83.1 and 98.1 %')).toBeInTheDocument();
      expect(within(resumen).getByText('Standard')).toBeInTheDocument();
      expect(within(resumen).getByText('87 of 100 marked as healthy')).toBeInTheDocument();
    });

    it('RF-11 · C6 · junto a las fotos se ve lo que contó el modelo, «87 de 100 marcados como sanos», y la frase que explica la corrección', async () => {
      conResultado(LOTE, resultadoAnalizado());
      abrirApp(RUTA);

      const resumen = await screen.findByRole('region', { name: 'Resumen del resultado' });
      expect(within(resumen).getByText('87 de 100 marcados como sanos')).toBeInTheDocument();
      expect(within(resumen).getByText('El porcentaje corrige los errores conocidos del modelo.')).toBeInTheDocument();
    });

    it('RF-12 · C10 · la categoría se muestra como probable, con la recomendación que se guardó con el lote y la nota de la prueba de corte', async () => {
      conResultado(LOTE, resultadoAnalizado());
      abrirApp(RUTA);

      const resumen = await screen.findByRole('region', { name: 'Resumen del resultado' });
      expect(within(resumen).getByText('Categoría probable')).toBeInTheDocument();
      expect(within(resumen).getByText('Estándar')).toBeInTheDocument();
      expect(within(resumen).getByText(RECOMENDACION_ESTANDAR)).toBeInTheDocument();
      expect(within(resumen).getByText('Es una categoría probable: la oficial se confirma con la prueba de corte.'))
        .toBeInTheDocument();
    });

    it('RF-12 · si el lote sugiere otra muestra, un aviso informativo que no interrumpe lo dice, todavía sin el botón para analizarla', async () => {
      conResultado(LOTE, resultadoAnalizado());
      abrirApp(RUTA);

      await screen.findByRole('region', { name: 'Resumen del resultado' });
      const aviso = screen.getAllByRole('status').find((uno) => uno.textContent.includes('menos de 1 punto'));
      expect(aviso).toHaveTextContent('El resultado quedó a menos de 1 punto del límite de una categoría.');
      expect(aviso).toHaveTextContent('el resultado sumará los granos de las dos muestras.');
      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
      expect(screen.queryByRole('link', { name: /otra muestra/ })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /otra muestra/ })).not.toBeInTheDocument();
    });

    it('RF-12 · un lote que no sugiere otra muestra no tiene el aviso', async () => {
      conResultado(LOTE, resultadoAnalizado({ porcentajeSanos: 93.0, sugerirOtraMuestra: false }));
      abrirApp(RUTA);

      await screen.findByRole('region', { name: 'Resumen del resultado' });
      expect(screen.queryByText(/menos de 1 punto/)).not.toBeInTheDocument();
    });

    it('RF-11 · C1 · cada grano de la foto aparece marcado en su lugar: el sano con el borde verde y el no sano con el rojo', async () => {
      conResultado(LOTE, resultadoAnalizado());
      abrirApp(RUTA);

      await imagenCargada(1);
      const granos = within(await granosDeLaFoto(1)).getAllByRole('button');
      expect(granos).toHaveLength(50);
      expect(granos[0]).toHaveAccessibleName('Grano 1: no sano, dudoso');
      expect(granos[0]).toHaveStyle({ left: '2%', top: '4%', width: '6%', height: '12%' });
      expect(granos[0]).toHaveStyle({ borderColor: marcasDeFoto.noSano });
      expect(granos[0]).not.toHaveStyle({ borderColor: marcasDeFoto.sano });
      expect(granos[6]).toHaveAccessibleName('Grano 7: sano');
      expect(granos[6]).toHaveStyle({ left: '62%', top: '4%' });
      expect(granos[6]).toHaveStyle({ borderColor: marcasDeFoto.sano });
      expect(granos[49]).toHaveAccessibleName('Grano 50: sano');
      expect(granos[49]).toHaveStyle({ left: '92%', top: '84%' });
    });

    it('RF-11 · C1 · las marcas aparecen cuando la foto carga: antes no se sabe su tamaño', async () => {
      conResultado(LOTE, resultadoAnalizado());
      abrirApp(RUTA);

      const imagen = await screen.findByRole('img', { name: 'Foto 1 de la muestra, con sus granos marcados' });
      expect(imagen).toHaveAttribute('src', expect.stringContaining('foto-1.jpg'));
      expect(screen.queryByRole('group', { name: 'Granos de la foto 1' })).not.toBeInTheDocument();
      cargarImagen(imagen);
      expect(await granosDeLaFoto(1)).toBeInTheDocument();
    });

    it('RF-11 · C2 · además del color, los sanos llevan un chulo y los no sanos una equis; los dudosos, también un signo de pregunta (D-42)', async () => {
      conResultado(LOTE, resultadoAnalizado());
      abrirApp(RUTA);

      await imagenCargada(1);
      const grupo = await granosDeLaFoto(1);
      expect(within(grupo).getAllByTestId('CheckIcon')).toHaveLength(44);
      expect(within(grupo).getAllByTestId('CloseIcon')).toHaveLength(6);
      expect(within(grupo).getAllByTestId('QuestionMarkIcon')).toHaveLength(6);
      const [primero] = within(grupo).getAllByRole('button');
      expect(within(primero).getByTestId('CloseIcon')).toBeInTheDocument();
      expect(within(primero).getByTestId('QuestionMarkIcon')).toBeInTheDocument();
      const leyenda = screen.getByRole('list', { name: 'Leyenda de las marcas' });
      expect(within(leyenda).getAllByRole('listitem').map((marca) => marca.textContent)).toEqual(['Sano', 'No sano', 'Dudoso']);
    });

    it('RF-11 · C4 · al pasar el cursor sobre un grano se ve su clase, sin la confianza, y Escape cierra el globo', async () => {
      conResultado(LOTE, resultadoAnalizado());
      const { usuario } = abrirApp(RUTA);

      await imagenCargada(1);
      const granos = within(await granosDeLaFoto(1)).getAllByRole('button');
      await usuario.hover(granos[0]);
      const globo = await screen.findByRole('tooltip');
      expect(globo).toHaveTextContent('No sano · dudoso');
      expect(globo.textContent).not.toMatch(/\d/);
      await usuario.keyboard('{Escape}');
      await waitFor(() => expect(screen.queryByRole('tooltip')).not.toBeInTheDocument());

      await usuario.hover(granos[6]);
      expect(await screen.findByRole('tooltip')).toHaveTextContent(/^Sano$/);
      expect(screen.getByText('Pasa el cursor sobre un grano o tócalo para ver su clase.')).toBeInTheDocument();
    });

    it('RF-11 · C4 · con el teclado, la foto es una sola parada de Tab: las flechas pasan de grano en grano en orden de lectura y muestran su clase', async () => {
      conResultado(LOTE, resultadoAnalizado());
      const { usuario } = abrirApp(RUTA);

      await imagenCargada(1);
      const grupo = await granosDeLaFoto(1);
      const granos = within(grupo).getAllByRole('button');
      expect(granos.filter((grano) => grano.tabIndex === 0)).toEqual([granos[0]]);
      expect(grupo).toHaveAccessibleDescription(
        'Pasa el cursor sobre un grano o tócalo para ver su clase. Con el teclado, entra a la foto con Tab y pasa de un grano a otro con las flechas.',
      );
      screen.getByRole('tab', { name: 'Foto 1 · 50 granos' }).focus();

      await usuario.tab();
      expect(granos[0]).toHaveFocus();
      expect(await screen.findByRole('tooltip')).toHaveTextContent('No sano · dudoso');
      await usuario.keyboard('{End}');
      expect(granos[49]).toHaveFocus();
      await waitFor(() => expect(screen.getByRole('tooltip')).toHaveTextContent(/^Sano$/));
      await usuario.keyboard('{ArrowLeft}{ArrowLeft}');
      expect(granos[47]).toHaveFocus();
      await usuario.keyboard('{Home}{ArrowRight}');
      expect(granos[1]).toHaveFocus();
      expect(granos.filter((grano) => grano.tabIndex === 0)).toEqual([granos[1]]);
      await usuario.keyboard('{Escape}');
      await waitFor(() => expect(screen.queryByRole('tooltip')).not.toBeInTheDocument());

      await usuario.tab();
      expect(grupo).not.toContainElement(document.activeElement);
      await usuario.tab({ shift: true });
      expect(granos[1]).toHaveFocus();
    });

    it('RNF-06 · C4 · pasar el cursor por otro grano solo abre su globo: las flechas siguen desde el grano que tiene el foco', async () => {
      conResultado(LOTE, resultadoAnalizado());
      const { usuario } = abrirApp(RUTA);

      await imagenCargada(1);
      const granos = within(await granosDeLaFoto(1)).getAllByRole('button');
      screen.getByRole('tab', { name: 'Foto 1 · 50 granos' }).focus();
      await usuario.tab();
      await usuario.keyboard('{ArrowRight}{ArrowRight}{ArrowRight}{ArrowRight}');
      expect(granos[4]).toHaveFocus();
      await usuario.hover(granos[19]);
      await waitFor(() => expect(screen.getByRole('tooltip')).toHaveTextContent(/^Sano$/));

      await usuario.keyboard('{ArrowRight}');

      expect(granos[5]).toHaveFocus();
      expect(granos.filter((grano) => grano.tabIndex === 0)).toEqual([granos[5]]);
    });

    it('RF-11 · C4 · en el celular, al tocar un grano se ve su clase, y al tocar fuera de la foto el globo se cierra', async () => {
      anchoDePantalla(390);
      conResultado(LOTE, resultadoAnalizado());
      const { usuario } = abrirApp(RUTA);

      await imagenCargada(1);
      const granos = within(await granosDeLaFoto(1)).getAllByRole('button');
      expect(screen.getByText('Toca un grano para ver su clase.')).toBeInTheDocument();
      await usuario.click(granos[10]);
      expect(await screen.findByRole('tooltip')).toHaveTextContent(/^Sano$/);
      await usuario.click(screen.getByRole('heading', { level: 1 }));

      await waitFor(() => expect(screen.queryByRole('tooltip')).not.toBeInTheDocument());
    });

    it('RF-11 · una pestaña por foto, «Foto 1 · 50 granos», y solo se carga la foto de la pestaña elegida', async () => {
      conResultado(LOTE, resultadoAnalizado());
      const { usuario } = abrirApp(RUTA);

      const pestanas = await screen.findByRole('tablist', { name: 'Fotos de la muestra 1' });
      expect(within(pestanas).getAllByRole('tab').map((pestana) => pestana.textContent))
        .toEqual(['Foto 1 · 50 granos', 'Foto 2 · 50 granos']);
      expect(screen.getAllByRole('img', { name: /de la muestra, con sus granos marcados$/ })).toHaveLength(1);
      await usuario.click(within(pestanas).getByRole('tab', { name: 'Foto 2 · 50 granos' }));

      expect(within(pestanas).getByRole('tab', { name: 'Foto 2 · 50 granos' })).toHaveAttribute('aria-selected', 'true');
      expect(screen.getByRole('tabpanel')).toHaveAccessibleName('Foto 2 · 50 granos');
      expect(await imagenCargada(2)).toHaveAttribute('src', expect.stringContaining('foto-2.jpg'));
      expect(screen.queryByRole('img', { name: 'Foto 1 de la muestra, con sus granos marcados' })).not.toBeInTheDocument();
      expect(within(await granosDeLaFoto(2)).getAllByTestId('CloseIcon')).toHaveLength(7);
    });

    it('RF-11 · si el enlace de una foto venció y la imagen no carga, la página pide otra vez el resultado, una sola vez', async () => {
      const pedidas = conResultado(LOTE, (vez) => resultadoAnalizado({}, { vence: vez }));
      abrirApp(RUTA);

      fireEvent.error(await screen.findByRole('img', { name: 'Foto 1 de la muestra, con sus granos marcados' }));
      await waitFor(() => expect(screen.getByRole('img', { name: 'Foto 1 de la muestra, con sus granos marcados' }))
        .toHaveAttribute('src', expect.stringContaining('vence=2')));
      fireEvent.error(screen.getByRole('img', { name: 'Foto 1 de la muestra, con sus granos marcados' }));

      await new Promise((resolver) => { setTimeout(resolver, 50); });
      expect(pedidas.veces).toBe(2);
      expect(screen.getByRole('region', { name: 'Resumen del resultado' })).toBeInTheDocument();
    });

    it('RF-11 · después de que una foto carga, un enlace que vence otra vez más tarde también se recupera: la página vuelve a pedir el resultado', async () => {
      const pedidas = conResultado(LOTE, (vez) => resultadoAnalizado({}, { vence: vez }));
      abrirApp(RUTA);
      const nombre = 'Foto 1 de la muestra, con sus granos marcados';

      fireEvent.error(await screen.findByRole('img', { name: nombre }));
      await waitFor(() => expect(screen.getByRole('img', { name: nombre })).toHaveAttribute('src', expect.stringContaining('vence=2')));
      cargarImagen(screen.getByRole('img', { name: nombre }));
      fireEvent.error(screen.getByRole('img', { name: nombre }));

      await waitFor(() => expect(screen.getByRole('img', { name: nombre })).toHaveAttribute('src', expect.stringContaining('vence=3')));
      expect(pedidas.veces).toBe(3);
    });

    it('RF-11 · C5 · en el celular, la cifra mide 52 px, las pestañas ocupan todo el ancho y «Nuevo lote» va al final, a todo el ancho (RNF-05)', async () => {
      anchoDePantalla(390);
      conResultado(LOTE, resultadoAnalizado({}, {
        fotos: [1, 2, 3].map((numero) => fotoMarcada(numero, { granos: granosDeFoto({ noSanos: numero }) })),
      }));
      abrirApp(RUTA);

      const resumen = await screen.findByRole('region', { name: 'Resumen del resultado' });
      expect(within(resumen).getByText('90,6 %')).toHaveStyle({ fontSize: '52px' });
      expect(within(resumen).getByText('90,6 %')).not.toHaveStyle({ fontSize: '64px' });
      const pestanas = screen.getAllByRole('tab');
      expect(pestanas.map((pestana) => pestana.textContent)).toEqual(['Foto 150 granos', 'Foto 250 granos', 'Foto 350 granos']);
      expect(pestanas[0]).toHaveClass('MuiTab-fullWidth');
      const nuevoLote = within(screen.getByRole('main')).getByRole('link', { name: 'Nuevo lote' });
      expect(nuevoLote).toHaveStyle({ alignSelf: 'stretch' });
      expect(screen.getByRole('region', { name: 'Fotos marcadas' }).compareDocumentPosition(nuevoLote))
        .toBe(Node.DOCUMENT_POSITION_FOLLOWING);
    });

    it('RF-11 · C5 · en el PC, la cifra mide 64 px y «Nuevo lote» va junto al título, como en el prototipo', async () => {
      conResultado(LOTE, resultadoAnalizado());
      abrirApp(RUTA);

      const resumen = await screen.findByRole('region', { name: 'Resumen del resultado' });
      expect(within(resumen).getByText('90,6 %')).toHaveStyle({ fontSize: '64px' });
      const nuevoLote = within(screen.getByRole('main')).getByRole('link', { name: 'Nuevo lote' });
      expect(nuevoLote).toHaveAttribute('href', '/lotes/nuevo');
      expect(resumen.compareDocumentPosition(nuevoLote)).toBe(Node.DOCUMENT_POSITION_PRECEDING);
    });
  });

  it('RF-14 · C3 · la administradora ve el detalle con las mismas cifras, quién envió la muestra, el modelo y las fotos, sin pasos ni acciones', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    conResultado(LOTE, resultadoAnalizado());
    abrirApp(RUTA);

    const resumen = await screen.findByRole('region', { name: 'Resumen del resultado' });
    expect(within(resumen).getByText('90,6 %')).toBeInTheDocument();
    expect(within(resumen).getByText('Rango probable: entre 83,1 y 98,1 %')).toBeInTheDocument();
    expect(within(resumen).getByText('Estándar')).toBeInTheDocument();
    const fotos = screen.getByRole('region', { name: 'Fotos marcadas' });
    expect(within(fotos).getByText('Analizada por Carlos Ramírez · Modelo de IA: clasificador-falso 1.0')).toBeInTheDocument();
    expect(within(fotos).getAllByRole('tab')).toHaveLength(2);
    expect(screen.queryByRole('list', { name: 'Pasos del análisis' })).not.toBeInTheDocument();
    expect(within(screen.getByRole('main')).queryByRole('link')).not.toBeInTheDocument();
    expect(within(screen.getByRole('main')).queryByRole('button', { name: /otra muestra|Analizar/ })).not.toBeInTheDocument();
  });

  it('RNF-03 · si el lote no existe o es de otra empresa, la página lo dice con el mensaje del backend, sin «Reintentar»', async () => {
    conSesion(USUARIOS.USUARIO_FINAL);
    servidor.use(http.get(`/api/v1/lotes/${LOTE}/resultado`, () => problema(404, 'No existe un registro con ese identificador.')));
    abrirApp(RUTA);

    expect(await screen.findByRole('alert')).toHaveTextContent('No existe un registro con ese identificador.');
    expect(screen.getByRole('heading', { level: 1, name: 'Resultado del lote' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Reintentar' })).not.toBeInTheDocument();
    expect(screen.queryByRole('region', { name: 'Resumen del resultado' })).not.toBeInTheDocument();
  });

  it('RNF-06 · C4 · si el resultado no llega por otra causa, «Reintentar» deja el foco en el título y lo pide otra vez', async () => {
    conSesion(USUARIOS.USUARIO_FINAL);
    conResultado(LOTE, resultadoAnalizado());
    servidor.use(http.get(`/api/v1/lotes/${LOTE}/resultado`, () => problema(500, 'Error interno.'), { once: true }));
    const { usuario } = abrirApp(RUTA);

    expect(await screen.findByRole('alert')).toHaveTextContent('Error interno.');
    await usuario.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(screen.getByRole('heading', { level: 1 })).toHaveFocus();
    expect(await screen.findByRole('heading', { level: 1, name: 'Resultado del lote L-0143' })).toHaveFocus();
    expect(screen.getByRole('region', { name: 'Resumen del resultado' })).toBeInTheDocument();
  });

  it('RNF-04 · C2 · el administrador de la plataforma que abre el resultado de un lote ve «Sin permiso»', async () => {
    conSesion(USUARIOS.ADMIN_PLATAFORMA);
    abrirApp(RUTA);

    expect(await screen.findByRole('heading', { name: 'Sin permiso' })).toBeInTheDocument();
    expect(screen.queryByRole('region', { name: 'Resumen del resultado' })).not.toBeInTheDocument();
  });

  describe('lote pendiente', () => {
    beforeEach(() => conSesion(USUARIOS.USUARIO_FINAL));

    it('RNF-02 · C3 · con la muestra en la cola, el usuario ve que su análisis está en espera, con la barra de avance, en el paso 3 y con el foco en el panel', async () => {
      conResultado(LOTE, resultadoPendiente('EN_ESPERA'));
      conAvance(LOTE, [avance('EN_ESPERA')]);
      abrirApp(RUTA);

      const espera = await screen.findByRole('region', { name: 'Análisis de la muestra' });
      expect(within(espera).getByText('En espera')).toBeInTheDocument();
      expect(within(espera).getByRole('progressbar', { name: 'Avance del análisis' })).toBeInTheDocument();
      expect(within(espera).getByText(EN_ESPERA)).toBeInTheDocument();
      // El foco llega con el efecto del panel, después de dibujarlo.
      await waitFor(() => expect(within(espera).getByRole('heading', { name: 'Análisis de la muestra' })).toHaveFocus());
      expect(screen.getByRole('heading', { level: 1, name: 'Resultado del lote L-0143' })).toBeInTheDocument();
      expect(screen.getByText('La Esperanza · Recibido el 20/10/2026')).toBeInTheDocument();
      const pasos = screen.getByRole('list', { name: 'Pasos del análisis' });
      expect(within(pasos).getByText('Resultado').closest('li')).toHaveAttribute('aria-current', 'step');
      expect(screen.queryByRole('region', { name: 'Resumen del resultado' })).not.toBeInTheDocument();
    });

    it('RNF-05 · en el celular, la espera dice «Paso 3 de 3 · Resultado», como en el prototipo, y su barra de avance ocupa el ancho', async () => {
      anchoDePantalla(390);
      conResultado(LOTE, resultadoPendiente('EN_PROCESO'));
      conAvance(LOTE, [avance('EN_PROCESO')]);
      abrirApp(RUTA);

      const espera = await screen.findByRole('region', { name: 'Análisis de la muestra' });
      expect(screen.getByText('Paso 3 de 3 ·')).toHaveTextContent('Paso 3 de 3 · Resultado');
      expect(screen.queryByRole('list', { name: 'Pasos del análisis' })).not.toBeInTheDocument();
      expect(within(espera).getByText(EN_PROCESO)).toBeInTheDocument();
      expect(within(espera).getByRole('progressbar', { name: 'Avance del análisis' })).toHaveStyle({ alignSelf: 'stretch' });
    });

    it('RNF-01 · C4 · mientras se procesa se ve el avance; el aviso para lectores cambia una vez por estado y, al terminar, la página muestra el resultado con el foco en el título', async () => {
      vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'], shouldAdvanceTime: true });
      const resultado = conResultado(LOTE, (vez) => (vez === 1 ? resultadoPendiente('EN_ESPERA') : resultadoAnalizado()));
      const consultas = conAvance(LOTE, [avance('EN_ESPERA'), avance('EN_PROCESO'), avance('EN_PROCESO'), avance('TERMINADA')]);
      abrirApp(RUTA);
      const espera = await screen.findByRole('region', { name: 'Análisis de la muestra' });
      const anuncio = anuncioDeLaPagina();
      expect(anuncio).toHaveTextContent(EN_ESPERA);
      const anunciados = [];
      const observador = new MutationObserver(() => anunciados.push(anuncio.textContent));
      observador.observe(anuncio, { childList: true, characterData: true, subtree: true });

      await siguienteConsulta(consultas, 1);
      await siguienteConsulta(consultas, 2);
      expect(await within(espera).findByText(EN_PROCESO)).toBeInTheDocument();
      expect(within(espera).getByText('En proceso')).toBeInTheDocument();
      expect(within(espera).getByRole('progressbar', { name: 'Avance del análisis' })).toBeInTheDocument();
      await siguienteConsulta(consultas, 3);
      await siguienteConsulta(consultas, 4);

      expect(await screen.findByRole('region', { name: 'Resumen del resultado' })).toBeInTheDocument();
      await waitFor(() => expect(screen.getByRole('heading', { level: 1, name: 'Resultado del lote L-0143' })).toHaveFocus());
      expect(anuncio).toHaveTextContent(LISTO);
      expect(anunciados.filter(Boolean)).toEqual([EN_PROCESO, LISTO]);
      expect(screen.getByRole('list', { name: 'Pasos del análisis' })).toBeInTheDocument();
      expect(resultado.veces).toBe(2);
      await avanzar(3000);
      expect(consultas.veces).toBe(4);
      observador.disconnect();
    });

    it('RNF-01 · C4 · si el análisis tarda más de 15 segundos, la página lo dice y ofrece volver al Historial, donde el resultado queda guardado', async () => {
      vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'], shouldAdvanceTime: true });
      conResultado(LOTE, resultadoPendiente('EN_ESPERA'));
      conAvance(LOTE, [avance('EN_ESPERA')]);
      abrirApp(RUTA);
      const espera = await screen.findByRole('region', { name: 'Análisis de la muestra' });
      const tarda = 'Está tardando más de lo normal. Puedes esperar aquí o volver al Historial: el resultado queda guardado.';

      await avanzar(TARDA_MAS_DE - 1500);
      expect(within(espera).queryByText(tarda)).not.toBeInTheDocument();
      await avanzar(1600);

      expect(await within(espera).findByText(tarda)).toBeInTheDocument();
      expect(within(espera).getByText(tarda).closest('[role="status"]')).toBeInTheDocument();
      expect(within(espera).getByRole('link', { name: 'Ir al Historial' })).toHaveAttribute('href', '/historial');
      expect(within(espera).getByRole('progressbar', { name: 'Avance del análisis' })).toBeInTheDocument();
    });

    it('RNF-06 · C3 · si el análisis no terminó y la muestra volvió a quedar pendiente, la página lo dice y lleva a las fotos', async () => {
      vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'], shouldAdvanceTime: true });
      conResultado(LOTE, resultadoPendiente('EN_PROCESO'));
      const consultas = conAvance(LOTE, [avance('PENDIENTE', { fallo: true })]);
      abrirApp(RUTA);
      await screen.findByRole('region', { name: 'Análisis de la muestra' });

      await siguienteConsulta(consultas, 1);

      const aviso = await screen.findByRole('alert');
      expect(aviso).toHaveTextContent('El análisis de la muestra no terminó: la muestra volvió a quedar pendiente.');
      expect(aviso).toHaveTextContent('Revisa las fotos y pulsa otra vez «Analizar la muestra».');
      expect(screen.getByRole('link', { name: 'Volver a las fotos' })).toHaveAttribute('href', `/lotes/${LOTE}/fotos`);
      expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
      await avanzar(3000);
      expect(consultas.veces).toBe(1);
    });

    it('RNF-06 · C3 · a la administradora, un análisis que no terminó se lo dice sin llevarla a las fotos', async () => {
      conSesion(USUARIOS.ADMIN_EMPRESA);
      conResultado(LOTE, resultadoPendiente('PENDIENTE', { fechaEnvio: '2026-11-23T16:42:00Z' }));
      abrirApp(RUTA);

      expect(await screen.findByRole('alert'))
        .toHaveTextContent('Un usuario final de tu empresa puede revisar las fotos y enviarla de nuevo.');
      expect(screen.queryByRole('link', { name: 'Volver a las fotos' })).not.toBeInTheDocument();
    });

    it('RNF-02 · si el resultado llega con la muestra ya terminada y el lote todavía pendiente, porque se leyó mientras se guardaba, la página consulta el avance y vuelve a pedir el resultado', async () => {
      vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'], shouldAdvanceTime: true });
      const resultado = conResultado(LOTE, (vez) => (vez === 1 ? resultadoPendiente('TERMINADA') : resultadoAnalizado()));
      const consultas = conAvance(LOTE, [avance('TERMINADA')]);
      abrirApp(RUTA);
      await waitFor(() => expect(resultado.veces).toBe(1));

      await siguienteConsulta(consultas, 1);

      expect(await screen.findByRole('region', { name: 'Resumen del resultado' })).toBeInTheDocument();
      expect(resultado.veces).toBe(2);
      await avanzar(3000);
      expect(consultas.veces).toBe(1);
    });

    it('RNF-02 · la consulta del avance se pausa con la pestaña oculta y, al volver a verse, consulta enseguida', async () => {
      vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'], shouldAdvanceTime: true });
      conResultado(LOTE, resultadoPendiente('EN_ESPERA'));
      const consultas = conAvance(LOTE, [avance('EN_ESPERA')]);
      abrirApp(RUTA);
      await screen.findByRole('region', { name: 'Análisis de la muestra' });

      pestanaOculta(true);
      await avanzar(5000);
      expect(consultas.veces).toBe(0);
      pestanaOculta(false);

      await waitFor(() => expect(consultas.veces).toBe(1));
      await siguienteConsulta(consultas, 2);
    });

    it('RNF-02 · nunca hay dos consultas del avance a la vez: la siguiente sale cuando llega la respuesta de la anterior', async () => {
      vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'], shouldAdvanceTime: true });
      conResultado(LOTE, resultadoPendiente('EN_ESPERA'));
      let soltar = () => {};
      const consultas = conAvance(LOTE, [avance('EN_ESPERA')], {
        espera: () => new Promise((resolver) => { soltar = resolver; }),
      });
      abrirApp(RUTA);
      await screen.findByRole('region', { name: 'Análisis de la muestra' });

      await avanzar(INTERVALO_DE_CONSULTA);
      await waitFor(() => expect(consultas.veces).toBe(1));
      pestanaOculta(true);
      pestanaOculta(false);
      await avanzar(5 * INTERVALO_DE_CONSULTA);
      expect(consultas.veces).toBe(1);
      soltar();
      await siguienteConsulta(consultas, 2);
      soltar();

      expect(consultas.maximoEnVuelo).toBe(1);
    });

    it('RNF-06 · C3 · si la consulta del avance falla varias veces seguidas, la página lo dice con «Reintentar», que vuelve a consultar', async () => {
      vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'], shouldAdvanceTime: true });
      conResultado(LOTE, resultadoPendiente('EN_ESPERA'));
      const error = () => problema(500, 'Error interno.');
      const consultas = conAvance(LOTE, [error, error, error, avance('EN_PROCESO')]);
      const { usuario } = abrirApp(RUTA);
      const espera = await screen.findByRole('region', { name: 'Análisis de la muestra' });

      for (let vez = 1; vez <= FALLOS_ANTES_DE_PARAR; vez += 1) {
        await siguienteConsulta(consultas, vez);
      }
      expect(await within(espera).findByRole('alert')).toHaveTextContent('Error interno.');
      await usuario.click(within(espera).getByRole('button', { name: 'Reintentar' }));

      expect(within(espera).getByRole('heading', { name: 'Análisis de la muestra' })).toHaveFocus();
      expect(await within(espera).findByText(EN_PROCESO)).toBeInTheDocument();
      expect(consultas.veces).toBe(4);
    });

    it('RF-14 · C5 · un lote pendiente que no se ha enviado dice que todavía no tiene resultado y ofrece «Continuar» al usuario final', async () => {
      conResultado(LOTE, resultadoPendiente('PENDIENTE'));
      abrirApp(RUTA);

      expect(await screen.findByText('Este lote todavía no tiene resultado.')).toBeInTheDocument();
      expect(screen.getByRole('link', { name: 'Continuar el lote L-0143' })).toHaveAttribute('href', `/lotes/${LOTE}/fotos`);
      expect(screen.queryByRole('list', { name: 'Pasos del análisis' })).not.toBeInTheDocument();
      expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
    });

    it('RF-14 · un lote sin muestras tampoco tiene resultado, y la administradora no recibe «Continuar»', async () => {
      conSesion(USUARIOS.ADMIN_EMPRESA);
      conResultado(LOTE, resultadoPendiente(null));
      abrirApp(RUTA);

      expect(await screen.findByText('Este lote todavía no tiene resultado.')).toBeInTheDocument();
      expect(screen.queryByRole('link', { name: /Continuar/ })).not.toBeInTheDocument();
    });
  });
});
