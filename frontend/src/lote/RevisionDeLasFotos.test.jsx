/*
 * Nombre: RevisionDeLasFotos.test.jsx
 * Descripción: Pruebas de lo que la página de las fotos muestra de la revisión del servicio de IA (D-46): cada casilla
 *              «Aprobada» con sus granos o «Por repetir» con su motivo y qué hacer (RF-06 C1 a C3, RNF-06 C3), las
 *              zonas de granos pegados dibujadas sobre la foto entera (RF-07 C2, D-45), los granos dudosos marcados con
 *              «?» y la repetición que va (RF-09 C1), y «Cargada» como hoy cuando no hubo revisión; debajo, el total de
 *              granos y el aviso si no llegan a 100, sin bloquear el envío (RF-07 C3); «Analizar la muestra» espera a
 *              que se repitan las fotos marcadas; y el aviso de la muestra devuelta con su motivo (RF-07 C3, RF-08 C6,
 *              D-44), en español y en inglés, en el PC y en el celular (RNF-05).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { act, fireEvent, screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import i18n from '../i18n/i18n.js';
import { abrirApp, conSesion, USUARIOS } from '../pruebas/ayudas.jsx';
import { archivo, conFotos, conSubidas, fotoCargada, fotoRevisada } from '../pruebas/fotos.js';
import { LOTE_PENDIENTE } from '../pruebas/lotes.js';
import { anchoDePantalla } from '../pruebas/preparacion.js';
import { cargarImagen, conEnvio } from '../pruebas/resultados.js';
import { problema, servidor } from '../pruebas/servidor.js';
import { marcasDeFoto } from '../tema/tokens.js';

const LOTE = LOTE_PENDIENTE.id;
const RUTA = `/lotes/${LOTE}/fotos`;
const BORROSA = 'Foto borrosa: tómala de nuevo sin mover el celular.';
const POCA_LUZ = 'Poca luz: tómala de nuevo con más luz, sin sombras fuertes.';
const PEGADOS = 'Granos pegados: sepáralos donde está marcado y toma la foto de nuevo.';
const POR_REPETIR_DEL_BACKEND = 'Repite las fotos marcadas «Por repetir» antes de analizar la muestra.';
/** Dos zonas de granos pegados, en píxeles de una foto de 2000 × 1500 (D-45). */
const REGIONES = [{ x: 1000, y: 750, ancho: 200, alto: 150 }, { x: 0, y: 0, ancho: 100, alto: 300 }];

/**
 * Elige un archivo para una foto, como lo deja el selector del navegador.
 *
 * @param {number} numero el número de la foto
 * @param {File} elegido el archivo
 */
async function elegir(numero, elegido) {
  fireEvent.change(await screen.findByLabelText(`Archivo de la foto ${numero}`), { target: { files: [elegido] } });
}

/**
 * La casilla de una foto, cuando aparece.
 *
 * @param {number} numero el número de la foto
 * @returns {Promise<HTMLElement>} la casilla
 */
function casilla(numero) {
  return screen.findByRole('group', { name: numero === 3 ? 'Foto 3 (opcional)' : `Foto ${numero}` });
}

/**
 * Las marcas dibujadas sobre la foto de una casilla, de un tipo.
 *
 * @param {HTMLElement} contenedor la casilla
 * @param {'pegados'|'dudoso'} tipo qué marcan
 * @returns {HTMLElement[]} las marcas
 */
function marcas(contenedor, tipo) {
  return [...contenedor.querySelectorAll(`[data-marca="${tipo}"]`)];
}

describe('Revisión de las fotos', () => {
  beforeEach(() => {
    conSesion(USUARIOS.USUARIO_FINAL);
    servidor.use(http.get(`/api/v1/lotes/${LOTE}`, () => HttpResponse.json(LOTE_PENDIENTE)));
    conFotos(LOTE, []);
  });
  afterEach(async () => {
    anchoDePantalla(1280);
    await act(() => i18n.changeLanguage('es'));
  });

  describe('la casilla de cada foto', () => {
    it('RF-06 · C3 · la foto que cumple la guía queda «Aprobada», sin avisos, con los granos que encontró, y se anuncia', async () => {
      conSubidas(LOTE, { revision: () => ({ estado: 'APROBADA', granosEncontrados: 52 }) });
      abrirApp(RUTA);

      await elegir(1, archivo('granos.jpg', 'image/jpeg', 2048));

      const primera = await casilla(1);
      expect(await within(primera).findByText('Aprobada')).toBeInTheDocument();
      expect(within(primera).getByText('52 granos encontrados')).toBeInTheDocument();
      expect(within(primera).queryByRole('alert')).not.toBeInTheDocument();
      expect(within(primera).getByRole('button', { name: 'Reemplazar' })).toBeInTheDocument();
      expect(within(primera).getByRole('status')).toHaveTextContent('Foto 1 aprobada.');
    });

    it('RF-06 · C1 · una foto borrosa queda «Por repetir», con el mensaje que pide tomarla sin mover el celular y «Repetir la foto»', async () => {
      conSubidas(LOTE, { revision: () => ({ estado: 'POR_REPETIR', motivo: 'BORROSA', granosEncontrados: 0 }) });
      abrirApp(RUTA);

      await elegir(2, archivo('granos.jpg', 'image/jpeg', 2048));

      const segunda = await casilla(2);
      expect(await within(segunda).findByRole('alert')).toHaveTextContent(BORROSA);
      expect(within(segunda).getByText('Por repetir')).toBeInTheDocument();
      expect(within(segunda).getByRole('button', { name: 'Repetir la foto' })).toBeInTheDocument();
      expect(within(segunda).queryByRole('button', { name: 'Reemplazar' })).not.toBeInTheDocument();
      expect(within(segunda).queryByText(/granos encontrados/)).not.toBeInTheDocument();
      expect(within(segunda).getByRole('status')).toHaveTextContent('Foto 2 por repetir.');
    });

    it('RF-06 · C2 · al retomar el lote, una foto oscura sigue «Por repetir» con el mensaje sobre la luz', async () => {
      conFotos(LOTE, [fotoRevisada(1), fotoRevisada(2, { estado: 'POR_REPETIR', motivo: 'POCA_LUZ', granosEncontrados: 12 })]);
      abrirApp(RUTA);

      const segunda = await casilla(2);
      expect(await within(segunda).findByRole('alert')).toHaveTextContent(POCA_LUZ);
      expect(within(await casilla(1)).getByText('Aprobada')).toBeInTheDocument();
    });

    it('RF-07 · C2 · una foto con granos pegados se ve entera, con cada zona marcada en su lugar y el texto que dice cuántas son', async () => {
      conFotos(LOTE, [fotoRevisada(1, {
        estado: 'POR_REPETIR', motivo: 'GRANOS_PEGADOS', granosEncontrados: 47, regionesPegadas: REGIONES,
      })]);
      abrirApp(RUTA);

      const primera = await casilla(1);
      expect(await within(primera).findByRole('alert')).toHaveTextContent(PEGADOS);
      const imagen = within(primera).getByRole('img', { name: 'Foto 1 de la muestra' });
      expect(imagen).toHaveAccessibleDescription('2 zonas con granos pegados, marcadas en la foto.');
      expect(imagen).toHaveStyle({ objectFit: 'contain' });
      expect(marcas(primera, 'pegados')).toHaveLength(0);

      cargarImagen(imagen, { ancho: 2000, alto: 1500 });

      const [zona, otra] = marcas(primera, 'pegados');
      expect(zona).toHaveStyle({ left: '50%', top: '50%', width: '10%', height: '10%' });
      expect(zona).toHaveStyle({ borderStyle: 'dashed', borderColor: marcasDeFoto.noSano });
      expect(otra).toHaveStyle({ left: '0%', top: '0%', width: '5%', height: '20%' });
      expect(zona.closest('[aria-hidden="true"]')).not.toBeNull();
    });

    it('RF-07 · C2 · con una sola zona, el texto la dice en singular', async () => {
      conFotos(LOTE, [fotoRevisada(1, {
        estado: 'POR_REPETIR', motivo: 'GRANOS_PEGADOS', granosEncontrados: 47, regionesPegadas: [REGIONES[0]],
      })]);
      abrirApp(RUTA);

      expect(await within(await casilla(1)).findByText('1 zona con granos pegados, marcada en la foto.')).toBeInTheDocument();
    });

    it('RF-09 · C1 · la foto con muchos granos dudosos dice el máximo, la repetición que va y cuáles son, marcados con «?» y sin su clase', async () => {
      conFotos(LOTE, [fotoRevisada(1), fotoRevisada(2, {
        estado: 'POR_REPETIR', motivo: 'GRANOS_DUDOSOS', granosEncontrados: 50,
        granosDudosos: [{ x: 340, y: 512, ancho: 118, alto: 74 }, { x: 902, y: 1220, ancho: 121, alto: 70 }],
      })], { repeticiones: 1, porcentajeMaxDudosos: 10.0 });
      abrirApp(RUTA);

      const segunda = await casilla(2);
      expect(await within(segunda).findByRole('alert')).toHaveTextContent('Más del 10 % de los granos quedaron dudosos: '
        + 'tómala de nuevo con buena luz y el celular quieto. Repetición 1 de 2.');
      const imagen = within(segunda).getByRole('img', { name: 'Foto 2 de la muestra' });
      expect(imagen).toHaveAccessibleDescription('2 granos dudosos, marcados con «?» en la foto.');
      cargarImagen(imagen, { ancho: 2000, alto: 1500 });
      const dudosos = marcas(segunda, 'dudoso');
      expect(dudosos).toHaveLength(2);
      expect(dudosos[0]).toHaveStyle({ left: '17%', borderColor: marcasDeFoto.dudoso });
      expect(dudosos[0]).toHaveTextContent('?');
      expect(within(segunda).queryByText(/sano/i)).not.toBeInTheDocument();
    });

    it('RF-09 · C4 · en la segunda repetición, el mensaje avisa que el resultado saldrá con confianza baja si siguen pasando del máximo', async () => {
      conFotos(LOTE, [fotoRevisada(1, {
        estado: 'POR_REPETIR', motivo: 'GRANOS_DUDOSOS', granosEncontrados: 50,
        granosDudosos: [{ x: 340, y: 512, ancho: 118, alto: 74 }],
      }), fotoRevisada(2)], { repeticiones: 2, porcentajeMaxDudosos: 10.5 });
      abrirApp(RUTA);

      const primera = await casilla(1);
      expect(await within(primera).findByRole('alert')).toHaveTextContent('Más del 10,5 % de los granos quedaron dudosos: '
        + 'tómala de nuevo con buena luz y el celular quieto. Repetición 2 de 2: si siguen pasando del 10,5 %, el '
        + 'resultado saldrá con un aviso de confianza baja.');
      expect(within(primera).getByText('1 grano dudoso, marcado con «?» en la foto.')).toBeInTheDocument();
    });

    it('RF-09 · C3 · «Repetir la foto» sube la nueva con el mismo número, que pasa por la misma revisión y queda «Aprobada»', async () => {
      conFotos(LOTE, [fotoRevisada(1, { estado: 'POR_REPETIR', motivo: 'BORROSA', granosEncontrados: 3 })]);
      const anotado = conSubidas(LOTE, { revision: () => ({ estado: 'APROBADA', granosEncontrados: 55 }) });
      abrirApp(RUTA);
      const primera = await casilla(1);
      expect(await within(primera).findByRole('button', { name: 'Repetir la foto' })).toBeInTheDocument();

      await elegir(1, archivo('otra.jpg', 'image/jpeg', 2048));

      expect(await within(primera).findByText('Aprobada')).toBeInTheDocument();
      expect(within(primera).getByText('55 granos encontrados')).toBeInTheDocument();
      expect(within(primera).queryByRole('alert')).not.toBeInTheDocument();
      expect(anotado.confirmadas).toEqual([{ numero: 1, llave: expect.stringMatching(/foto-1-1\.jpg$/) }]);
    });

    it('D-46 · sin revisión, como con el clasificador falso, la foto queda «Cargada» como hoy: sin conteo, sin avisos y con «Reemplazar»', async () => {
      conSubidas(LOTE);
      abrirApp(RUTA);

      await elegir(1, archivo('granos.jpg', 'image/jpeg', 2048));

      const primera = await casilla(1);
      expect(await within(primera).findByText('Cargada')).toBeInTheDocument();
      expect(within(primera).queryByText(/granos encontrados/)).not.toBeInTheDocument();
      expect(within(primera).queryByRole('alert')).not.toBeInTheDocument();
      expect(within(primera).getByRole('status')).toHaveTextContent('Foto 1 cargada.');
      expect(screen.queryByText(/Entre todas las fotos/)).not.toBeInTheDocument();
    });

    it('RNF-06 · C3 · en inglés, la foto por repetir dice qué pasó y qué hacer, con «Retake needed» y «Retake photo»', async () => {
      await act(() => i18n.changeLanguage('en'));
      conFotos(LOTE, [fotoRevisada(1, { estado: 'POR_REPETIR', motivo: 'BORROSA', granosEncontrados: 0 }), fotoRevisada(2)]);
      abrirApp(RUTA);

      const primera = await screen.findByRole('group', { name: 'Photo 1' });
      expect(await within(primera).findByRole('alert')).toHaveTextContent('Blurry photo: take it again holding the phone still.');
      expect(within(primera).getByText('Retake needed')).toBeInTheDocument();
      expect(within(primera).getByRole('button', { name: 'Retake photo' })).toBeInTheDocument();
      expect(within(screen.getByRole('group', { name: 'Photo 2' })).getByText('52 beans found')).toBeInTheDocument();
    });

    it('RNF-05 · en el celular, la foto por repetir muestra su mensaje y «Repetir la foto» a todo el ancho de la casilla', async () => {
      anchoDePantalla(390);
      conFotos(LOTE, [fotoRevisada(1, { estado: 'POR_REPETIR', motivo: 'BORROSA', granosEncontrados: 0 })]);
      abrirApp(RUTA);

      const primera = await casilla(1);
      expect(await within(primera).findByRole('alert')).toHaveTextContent(BORROSA);
      expect(within(primera).getByRole('button', { name: 'Repetir la foto' })).toHaveStyle({ alignSelf: 'stretch' });
    });
  });

  describe('el total de granos', () => {
    it('RF-07 · C3 · con todas las fotos revisadas, debajo se ve cuántos granos suman', async () => {
      conFotos(LOTE, [fotoRevisada(1, { granosEncontrados: 52 }), fotoRevisada(2, { granosEncontrados: 55 })]);
      abrirApp(RUTA);

      expect(await screen.findByText('Entre todas las fotos: 107 granos.')).toBeInTheDocument();
      expect(screen.queryByText(/hacen falta al menos 100/)).not.toBeInTheDocument();
    });

    it('RF-07 · C3 · con 99 granos en 2 fotos, avisa que faltan y pide agregar la foto 3, sin bloquear el envío (prueba de límite)', async () => {
      conFotos(LOTE, [fotoRevisada(1, { granosEncontrados: 50 }), fotoRevisada(2, { granosEncontrados: 49 })]);
      const envio = conEnvio(LOTE);
      const { usuario } = abrirApp(RUTA);

      expect(await screen.findByText('Las fotos suman 99 granos y hacen falta al menos 100: agrega la foto 3 o repite '
        + 'una con más granos.')).toBeInTheDocument();
      const boton = screen.getByRole('button', { name: 'Analizar la muestra' });
      expect(boton).not.toHaveAttribute('aria-disabled');
      await usuario.click(boton);
      await waitFor(() => expect(envio.veces).toBe(1));
    });

    it('RF-07 · C3 · con 100 granos no hay aviso: alcanzan (prueba de límite)', async () => {
      conFotos(LOTE, [fotoRevisada(1, { granosEncontrados: 50 }), fotoRevisada(2, { granosEncontrados: 50 })]);
      abrirApp(RUTA);

      expect(await screen.findByText('Entre todas las fotos: 100 granos.')).toBeInTheDocument();
      expect(screen.queryByText(/hacen falta al menos 100/)).not.toBeInTheDocument();
    });

    it('RF-07 · C3 · con 3 fotos que suman menos de 100, el aviso pide repetir una foto con más granos', async () => {
      conFotos(LOTE, [fotoRevisada(1, { granosEncontrados: 30 }), fotoRevisada(2, { granosEncontrados: 30 }),
        fotoRevisada(3, { granosEncontrados: 30 })]);
      abrirApp(RUTA);

      expect(await screen.findByText('Las fotos suman 90 granos y hacen falta al menos 100: repite una foto con más '
        + 'granos.')).toBeInTheDocument();
    });

    it('RF-07 · C3 · si una foto no tiene conteo, como una «Cargada», no se muestra el total', async () => {
      conFotos(LOTE, [fotoRevisada(1), fotoCargada(2)]);
      abrirApp(RUTA);

      await screen.findByRole('img', { name: 'Foto 2 de la muestra' });
      expect(screen.queryByText(/Entre todas las fotos|Las fotos suman/)).not.toBeInTheDocument();
    });
  });

  describe('«Analizar la muestra» con fotos por repetir', () => {
    it('RNF-06 · C3 · con una foto «Por repetir», el botón espera y dice cuál repetir; no envía la muestra', async () => {
      conFotos(LOTE, [fotoRevisada(1), fotoRevisada(2, { estado: 'POR_REPETIR', motivo: 'BORROSA', granosEncontrados: 0 })]);
      const envio = conEnvio(LOTE);
      const { usuario } = abrirApp(RUTA);

      const boton = await screen.findByRole('button', { name: 'Analizar la muestra' });
      await waitFor(() => expect(boton).toHaveAttribute('aria-disabled', 'true'));
      expect(boton).toHaveAccessibleDescription('Repite la foto 2 para continuar.');
      await usuario.click(boton);
      expect(envio.veces).toBe(0);
    });

    it('RNF-06 · C3 · con dos fotos «Por repetir», el motivo las nombra a las dos', async () => {
      conFotos(LOTE, [fotoRevisada(1, { estado: 'POR_REPETIR', motivo: 'POCA_LUZ', granosEncontrados: 0 }),
        fotoRevisada(2, { estado: 'POR_REPETIR', motivo: 'BORROSA', granosEncontrados: 0 })]);
      abrirApp(RUTA);

      expect(await screen.findByRole('button', { name: 'Analizar la muestra' }))
        .toHaveAccessibleDescription('Repite las fotos 1 y 2 para continuar.');
    });

    it('RNF-06 · C3 · si el backend igual rechaza la muestra por una foto por repetir, la página muestra su mensaje y pide otra vez las fotos', async () => {
      const pedidas = conFotos(LOTE, (vez) => (vez === 1 ? [fotoRevisada(1), fotoCargada(2)]
        : [fotoRevisada(1), fotoRevisada(2, { estado: 'POR_REPETIR', motivo: 'BORROSA', granosEncontrados: 0 })]));
      conEnvio(LOTE, {
        respuesta: () => problema(400, 'Revisa los datos marcados.', {
          errores: [{ campo: 'fotos', mensaje: POR_REPETIR_DEL_BACKEND }],
        }),
      });
      const { usuario } = abrirApp(RUTA);

      await usuario.click(await screen.findByRole('button', { name: 'Analizar la muestra' }));

      expect(await screen.findByText(POR_REPETIR_DEL_BACKEND)).toBeInTheDocument();
      await waitFor(() => expect(pedidas.veces).toBe(2));
      expect(await within(await casilla(2)).findByRole('alert')).toHaveTextContent(BORROSA);
    });
  });

  describe('la muestra devuelta', () => {
    it('RF-07 · C3 · si volvió por tener menos de 100 granos, arriba de las fotos lo dice y pide agregar la foto 3 o repetir una', async () => {
      conFotos(LOTE, [fotoRevisada(1, { granosEncontrados: 45 }), fotoRevisada(2, { granosEncontrados: 40 })],
        { motivoDevolucion: 'POCOS_GRANOS' });
      abrirApp(RUTA);

      const aviso = await screen.findByText(/La muestra volvió sin resultado/);
      expect(aviso).toHaveTextContent('La muestra volvió sin resultado: entre todas las fotos hay menos de 100 granos. '
        + 'Agrega la foto 3 o repite una foto con más granos.');
      expect(aviso.closest('[role="alert"]')).not.toBeNull();
    });

    it('RF-07 · C3 · con 3 fotos, el aviso de la muestra devuelta solo pide repetir una foto con más granos', async () => {
      conFotos(LOTE, [fotoRevisada(1, { granosEncontrados: 30 }), fotoRevisada(2, { granosEncontrados: 30 }),
        fotoRevisada(3, { granosEncontrados: 30 })], { motivoDevolucion: 'POCOS_GRANOS' });
      abrirApp(RUTA);

      expect(await screen.findByText('La muestra volvió sin resultado: entre todas las fotos hay menos de 100 granos. '
        + 'Repite una foto con más granos.')).toBeInTheDocument();
    });

    it('RF-08 · C6 · si el servicio de IA no respondió, arriba de las fotos lo dice sin interrumpir y pide pulsar otra vez «Analizar la muestra»', async () => {
      conFotos(LOTE, [fotoCargada(1), fotoCargada(2)], { motivoDevolucion: 'SIN_RESPUESTA_DE_LA_IA' });
      abrirApp(RUTA);

      const aviso = await screen.findByText(/El servicio de IA no respondió/);
      expect(aviso).toHaveTextContent('El servicio de IA no respondió la última vez. Pulsa otra vez «Analizar la muestra».');
      expect(aviso.closest('[role="status"]')).not.toBeNull();
      expect(screen.getByRole('button', { name: 'Analizar la muestra' })).not.toHaveAttribute('aria-disabled');
    });

    it('RF-08 · C6 · en inglés, el aviso dice lo mismo', async () => {
      await act(() => i18n.changeLanguage('en'));
      conFotos(LOTE, [fotoCargada(1), fotoCargada(2)], { motivoDevolucion: 'SIN_RESPUESTA_DE_LA_IA' });
      abrirApp(RUTA);

      expect(await screen.findByText('The AI service did not respond last time. Press “Analyze the sample” again.'))
        .toBeInTheDocument();
    });
  });
});
