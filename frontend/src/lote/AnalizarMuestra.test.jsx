/*
 * Nombre: AnalizarMuestra.test.jsx
 * Descripción: Pruebas de «Analizar la muestra», al pie de la página de las fotos (RF-05 C2): con 0 y 1 foto queda
 *              deshabilitado, con su motivo visible y sin perder el foco; con 2 y 3 fotos envía la muestra y lleva a
 *              la espera del resultado, en el paso 3; mientras sube una foto, espera; un doble clic la envía una sola
 *              vez; un rechazo del backend muestra su mensaje, y una muestra que ya está en la cola lleva a su espera
 *              (RNF-02 C3, RNF-06 C3 y C4).
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { abrirApp, conSesion, USUARIOS } from '../pruebas/ayudas.jsx';
import { archivo, conFotos, conSubidas, fotoCargada } from '../pruebas/fotos.js';
import { LOTE_PENDIENTE } from '../pruebas/lotes.js';
import { anchoDePantalla } from '../pruebas/preparacion.js';
import { avance, conAvance, conEnvio, conResultado, resultadoAnalizado, resultadoPendiente } from '../pruebas/resultados.js';
import { problema, servidor } from '../pruebas/servidor.js';

const LOTE = LOTE_PENDIENTE.id;
const RUTA = `/lotes/${LOTE}/fotos`;
const MINIMO = 'Sube al menos 2 fotos para analizar la muestra.';

/**
 * «Analizar la muestra», cuando aparece.
 *
 * @returns {Promise<HTMLElement>} el botón
 */
function botonAnalizar() {
  return screen.findByRole('button', { name: 'Analizar la muestra' });
}

describe('Analizar la muestra', () => {
  afterEach(() => anchoDePantalla(1280));

  beforeEach(() => {
    conSesion(USUARIOS.USUARIO_FINAL);
    servidor.use(http.get(`/api/v1/lotes/${LOTE}`, () => HttpResponse.json(LOTE_PENDIENTE)));
    conResultado(LOTE, resultadoPendiente('EN_ESPERA'));
    conAvance(LOTE, [avance('EN_ESPERA')]);
  });

  it('RF-05 · C2 · sin fotos, «Analizar la muestra» queda deshabilitado con su motivo a la vista, sigue recibiendo el foco y no envía nada', async () => {
    conFotos(LOTE, []);
    const envio = conEnvio(LOTE);
    const { usuario } = abrirApp(RUTA);

    const boton = await botonAnalizar();
    expect(boton).toHaveAttribute('aria-disabled', 'true');
    expect(boton).not.toBeDisabled();
    expect(boton).toHaveAccessibleDescription(MINIMO);
    expect(screen.getByText(MINIMO)).toBeVisible();
    boton.focus();
    expect(boton).toHaveFocus();
    await usuario.click(boton);

    expect(envio.veces).toBe(0);
    expect(window.location.pathname).toBe(RUTA);
  });

  it('RF-05 · C2 · con 1 foto sigue deshabilitado: la muestra pide al menos 2 (prueba de límite)', async () => {
    conFotos(LOTE, [fotoCargada(1)]);
    const envio = conEnvio(LOTE);
    const { usuario } = abrirApp(RUTA);

    await screen.findByRole('img', { name: 'Foto 1 de la muestra' });
    const boton = await botonAnalizar();
    expect(boton).toHaveAttribute('aria-disabled', 'true');
    expect(boton).toHaveAccessibleDescription(MINIMO);
    await usuario.click(boton);

    expect(envio.veces).toBe(0);
  });

  it('RF-05 · C2 · con 2 fotos se habilita: envía la muestra y lleva a la espera del resultado, en el paso 3 (prueba de límite)', async () => {
    conFotos(LOTE, [fotoCargada(1), fotoCargada(2)]);
    const envio = conEnvio(LOTE);
    const { usuario } = abrirApp(RUTA);

    await screen.findByRole('img', { name: 'Foto 2 de la muestra' });
    const boton = await botonAnalizar();
    expect(boton).not.toHaveAttribute('aria-disabled');
    expect(boton).not.toHaveAccessibleDescription();
    expect(screen.queryByText(MINIMO)).not.toBeInTheDocument();
    await usuario.click(boton);

    expect(await screen.findByRole('heading', { level: 1, name: 'Resultado del lote L-0143' })).toBeInTheDocument();
    expect(window.location.pathname).toBe(`/lotes/${LOTE}`);
    expect(envio.veces).toBe(1);
    const pasos = screen.getByRole('list', { name: 'Pasos del análisis' });
    expect(within(pasos).getByText('Resultado').closest('li')).toHaveAttribute('aria-current', 'step');
    expect(await screen.findByRole('progressbar', { name: 'Avance del análisis' })).toBeInTheDocument();
  });

  it('RF-05 · C2 · con 3 fotos, el máximo, también se habilita y envía la muestra', async () => {
    conFotos(LOTE, [fotoCargada(1), fotoCargada(2), fotoCargada(3)]);
    const envio = conEnvio(LOTE);
    const { usuario } = abrirApp(RUTA);

    await screen.findByRole('img', { name: 'Foto 3 de la muestra' });
    await usuario.click(await botonAnalizar());

    expect(await screen.findByRole('heading', { level: 1, name: 'Resultado del lote L-0143' })).toBeInTheDocument();
    expect(envio.veces).toBe(1);
  });

  it('RF-05 · C2 · mientras sube una foto, el botón espera: queda deshabilitado con «Espera a que termine de subir la foto 3.» y se habilita al terminar', async () => {
    conFotos(LOTE, [fotoCargada(1), fotoCargada(2)]);
    let soltar;
    conSubidas(LOTE, { espera: new Promise((resolver) => { soltar = resolver; }) });
    const envio = conEnvio(LOTE);
    const { usuario } = abrirApp(RUTA);
    await screen.findByRole('img', { name: 'Foto 2 de la muestra' });
    expect(await botonAnalizar()).not.toHaveAttribute('aria-disabled');

    fireEvent.change(screen.getByLabelText('Archivo de la foto 3'), { target: { files: [archivo('tercera.jpg', 'image/jpeg')] } });

    const boton = await botonAnalizar();
    await waitFor(() => expect(boton).toHaveAttribute('aria-disabled', 'true'));
    expect(boton).toHaveAccessibleDescription('Espera a que termine de subir la foto 3.');
    await usuario.click(boton);
    expect(envio.veces).toBe(0);
    soltar();
    await screen.findByRole('img', { name: 'Foto 3 de la muestra' });
    await waitFor(() => expect(boton).not.toHaveAttribute('aria-disabled'));
  });

  it('RF-05 · C2 · un doble clic envía la muestra una sola vez', async () => {
    conFotos(LOTE, [fotoCargada(1), fotoCargada(2)]);
    let soltar;
    const envio = conEnvio(LOTE, { espera: new Promise((resolver) => { soltar = resolver; }) });
    const { usuario } = abrirApp(RUTA);
    await screen.findByRole('img', { name: 'Foto 2 de la muestra' });

    await usuario.dblClick(await botonAnalizar());
    await usuario.click(await botonAnalizar());
    soltar();

    expect(await screen.findByRole('heading', { level: 1, name: 'Resultado del lote L-0143' })).toBeInTheDocument();
    expect(envio.veces).toBe(1);
  });

  it('RF-05 · C2 · si el backend rechaza la muestra por tener menos de 2 fotos, la página muestra su mensaje, que dice qué hacer, y pide otra vez las fotos', async () => {
    const lista = conFotos(LOTE, [fotoCargada(1), fotoCargada(2)]);
    conEnvio(LOTE, { respuesta: () => problema(400, 'Revisa los datos marcados.', { errores: [{ campo: 'fotos', mensaje: MINIMO }] }) });
    const { usuario } = abrirApp(RUTA);
    await screen.findByRole('img', { name: 'Foto 2 de la muestra' });

    await usuario.click(await botonAnalizar());

    expect(await screen.findByRole('alert')).toHaveTextContent(MINIMO);
    await waitFor(() => expect(lista.veces).toBe(2));
    expect(window.location.pathname).toBe(RUTA);
  });

  it('RNF-06 · C3 · si el envío no llega al servidor, la página lo dice y el botón deja intentarlo de nuevo', async () => {
    conFotos(LOTE, [fotoCargada(1), fotoCargada(2)]);
    const envio = conEnvio(LOTE);
    servidor.use(http.post(`/api/v1/lotes/${LOTE}/analisis`, () => HttpResponse.error(), { once: true }));
    const { usuario } = abrirApp(RUTA);
    await screen.findByRole('img', { name: 'Foto 2 de la muestra' });

    await usuario.click(await botonAnalizar());
    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo conectar con el servidor.');
    expect(await botonAnalizar()).not.toHaveAttribute('aria-disabled');
    await usuario.click(await botonAnalizar());

    expect(await screen.findByRole('heading', { level: 1, name: 'Resultado del lote L-0143' })).toBeInTheDocument();
    expect(envio.veces).toBe(1);
  });

  it('RF-05 · si el lote ya tiene su resultado, «Analizar la muestra» lleva a él', async () => {
    conFotos(LOTE, [fotoCargada(1), fotoCargada(2)]);
    conEnvio(LOTE, { respuesta: () => problema(409, 'Este lote ya tiene su resultado: no se puede volver a analizar.') });
    conResultado(LOTE, resultadoAnalizado());
    const { usuario } = abrirApp(RUTA);
    await screen.findByRole('img', { name: 'Foto 2 de la muestra' });

    await usuario.click(await botonAnalizar());

    expect(await screen.findByText('90,6 %')).toBeInTheDocument();
    expect(window.location.pathname).toBe(`/lotes/${LOTE}`);
  });

  it('RNF-02 · C3 · si la muestra ya está en espera o en proceso, la página de las fotos lleva al resultado del lote, donde se ve que está en espera', async () => {
    servidor.use(http.get(`/api/v1/lotes/${LOTE}/fotos`, () => HttpResponse.json({
      muestra: 1, estado: 'EN_ESPERA', fotos: [fotoCargada(1), fotoCargada(2)],
    })));
    abrirApp(RUTA);

    expect(await screen.findByRole('heading', { level: 1, name: 'Resultado del lote L-0143' })).toBeInTheDocument();
    expect(window.location.pathname).toBe(`/lotes/${LOTE}`);
    const espera = await screen.findByRole('region', { name: 'Análisis de la muestra' });
    expect(within(espera).getByText('Tu análisis está en espera: empieza apenas termine el que va antes.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Analizar la muestra' })).not.toBeInTheDocument();
  });

  it('RNF-05 · en el celular, «Analizar la muestra» va a todo el ancho, al pie de las fotos, con su motivo debajo', async () => {
    anchoDePantalla(390);
    conFotos(LOTE, [fotoCargada(1)]);
    abrirApp(RUTA);

    const boton = await botonAnalizar();
    expect(boton.parentElement).toHaveStyle({ flexDirection: 'column', alignItems: 'stretch' });
    expect(boton.nextElementSibling).toHaveTextContent(MINIMO);
    const tusFotos = screen.getByRole('region', { name: 'Tus fotos' });
    expect(tusFotos).toContainElement(boton);
    expect(within(tusFotos).getByRole('group', { name: 'Foto 3 (opcional)' }).compareDocumentPosition(boton))
      .toBe(Node.DOCUMENT_POSITION_FOLLOWING);
  });
});
