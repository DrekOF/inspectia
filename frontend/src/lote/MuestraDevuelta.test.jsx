/*
 * Nombre: MuestraDevuelta.test.jsx
 * Descripción: Pruebas de la página del resultado cuando el servicio de IA devuelve la muestra sin resultado (D-44):
 *              el aviso dice por qué, una foto por repetir con su motivo (RF-06, RF-07 C2), muchos granos dudosos con
 *              la repetición que va (RF-09 C1), varias fotos, menos de 100 granos (RF-07 C3) o sin respuesta del
 *              servicio (RF-08 C6), y qué hacer (RNF-06 C3), al esperar y al recargar, en la primera y en la segunda
 *              muestra, para el usuario final y la administradora; y el aviso de confianza baja del resultado, con el
 *              máximo de dudosos que usó la muestra (RF-09 C2, C4 y C6), en español y en inglés.
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { act, screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import i18n from '../i18n/i18n.js';
import { abrirApp, conSesion, USUARIOS } from '../pruebas/ayudas.jsx';
import { LOTE_PENDIENTE } from '../pruebas/lotes.js';
import {
  avance, conAvance, conResultado, resultadoAnalizado, resultadoConDosMuestras, resultadoConSegundaMuestra,
  resultadoPendiente,
} from '../pruebas/resultados.js';

const LOTE = LOTE_PENDIENTE.id;
const RUTA = `/lotes/${LOTE}`;
const ENVIADA = '2026-11-23T16:42:00Z';
const CONFIANZA_BAJA = 'Confianza baja: confirma con la prueba de corte.';

/**
 * El resultado de un lote pendiente cuya muestra volvió de la cola sin resultado, con lo que dijo el servicio de IA.
 *
 * @param {object} devolucion el motivo, las fotos por repetir y las repeticiones (D-44)
 * @returns {object} el resultado
 */
function devuelta(devolucion) {
  return resultadoPendiente('PENDIENTE', { fechaEnvio: ENVIADA, devolucion });
}

/**
 * Un resultado con el aviso de confianza baja en sus muestras, como lo responde el backend (RF-09 C4 y C6).
 *
 * @param {object} resultado el resultado, con sus muestras terminadas
 * @param {Array<{ avisoConfianzaBaja: boolean, porcentajeMaxDudosos: number }>} porMuestra el aviso y el máximo de
 *   dudosos de cada muestra, en orden
 * @returns {object} el resultado con el aviso
 */
function conAviso(resultado, porMuestra) {
  return {
    lote: { ...resultado.lote, avisoConfianzaBaja: porMuestra.some((muestra) => muestra.avisoConfianzaBaja) },
    muestras: resultado.muestras.map((muestra, indice) => ({ ...muestra, ...porMuestra[indice] })),
  };
}

/**
 * El aviso de error de la muestra devuelta, cuando aparece.
 *
 * @returns {Promise<HTMLElement>} el aviso
 */
function avisoDeError() {
  return screen.findByRole('alert');
}

describe('Muestra devuelta por el servicio de IA', () => {
  beforeEach(() => conSesion(USUARIOS.USUARIO_FINAL));
  afterEach(async () => {
    await act(() => i18n.changeLanguage('es'));
  });

  it('RF-06 · C1 · si la cola devolvió la muestra por una foto borrosa, mientras se espera el aviso nombra la foto, el motivo y qué hacer', async () => {
    conResultado(LOTE, resultadoPendiente('EN_PROCESO'));
    conAvance(LOTE, [avance('PENDIENTE', {
      fallo: true, motivoDevolucion: null, repeticiones: 0, fotosPorRepetir: [{ numero: 2, motivo: 'BORROSA' }],
    })]);
    abrirApp(RUTA);

    const aviso = await avisoDeError();
    expect(aviso).toHaveTextContent('Hay que repetir la foto 2 de la muestra: foto borrosa.');
    expect(aviso).toHaveTextContent('Tómala de nuevo sin mover el celular.');
    expect(screen.getByRole('link', { name: 'Volver a las fotos' })).toHaveAttribute('href', `${RUTA}/fotos`);
  });

  it('RF-07 · C2 · con granos pegados, el aviso pide separarlos donde está marcado', async () => {
    conResultado(LOTE, devuelta({ fotosPorRepetir: [{ numero: 1, motivo: 'GRANOS_PEGADOS' }] }));
    abrirApp(RUTA);

    const aviso = await avisoDeError();
    expect(aviso).toHaveTextContent('Hay que repetir la foto 1 de la muestra: granos pegados.');
    expect(aviso).toHaveTextContent('Separa los granos donde está marcado y toma la foto de nuevo.');
  });

  it('RF-09 · C1 · con muchos granos dudosos, el aviso dice que están marcados en la foto y qué repetición va', async () => {
    conResultado(LOTE, devuelta({ repeticiones: 1, fotosPorRepetir: [{ numero: 2, motivo: 'GRANOS_DUDOSOS' }] }));
    abrirApp(RUTA);

    const aviso = await avisoDeError();
    expect(aviso).toHaveTextContent('Hay que repetir la foto 2 de la muestra: muchos granos dudosos.');
    expect(aviso).toHaveTextContent('Los granos dudosos están marcados en la foto. Repetición 1 de 2.');
  });

  it('RNF-06 · C3 · con varias fotos por repetir, el aviso las nombra y pide revisarlas', async () => {
    conResultado(LOTE, devuelta({
      fotosPorRepetir: [{ numero: 1, motivo: 'POCA_LUZ' }, { numero: 3, motivo: 'BORROSA' }],
    }));
    abrirApp(RUTA);

    const aviso = await avisoDeError();
    expect(aviso).toHaveTextContent('Hay que repetir las fotos 1 y 3 de la muestra.');
    expect(aviso).toHaveTextContent('Revisa las fotos y pulsa otra vez «Analizar la muestra».');
  });

  it('RF-07 · C3 · con menos de 100 granos, el aviso dice que no se calculó el resultado y pide completar la muestra', async () => {
    conResultado(LOTE, devuelta({ motivoDevolucion: 'POCOS_GRANOS', fotosPorRepetir: [] }));
    abrirApp(RUTA);

    const aviso = await avisoDeError();
    expect(aviso).toHaveTextContent('La muestra tiene menos de 100 granos: no se calculó el resultado.');
    expect(aviso).toHaveTextContent('Agrega la foto 3 o repite una foto con más granos, y pulsa otra vez «Analizar la '
      + 'muestra».');
    expect(screen.getByRole('link', { name: 'Volver a las fotos' })).toBeInTheDocument();
  });

  it('RF-08 · C6 · si el servicio de IA no respondió, el aviso lo dice, sin resultado, y pide intentarlo en unos minutos', async () => {
    conResultado(LOTE, resultadoPendiente('EN_ESPERA'));
    conAvance(LOTE, [avance('PENDIENTE', { fallo: true, motivoDevolucion: 'SIN_RESPUESTA_DE_LA_IA', fotosPorRepetir: [] })]);
    abrirApp(RUTA);

    const aviso = await avisoDeError();
    expect(aviso).toHaveTextContent('El servicio de IA no respondió: la muestra volvió a quedar pendiente, sin resultado.');
    expect(aviso).toHaveTextContent('Pulsa otra vez «Analizar la muestra» en unos minutos.');
    expect(screen.queryByRole('region', { name: 'Resumen del resultado' })).not.toBeInTheDocument();
  });

  it('RNF-06 · C3 · sin motivo, por otra falla, el aviso sigue siendo el general de hoy', async () => {
    conResultado(LOTE, devuelta({ motivoDevolucion: null, fotosPorRepetir: [], repeticiones: 0 }));
    abrirApp(RUTA);

    const aviso = await avisoDeError();
    expect(aviso).toHaveTextContent('El análisis de la muestra no terminó: la muestra volvió a quedar pendiente.');
    expect(aviso).toHaveTextContent('Revisa las fotos y pulsa otra vez «Analizar la muestra».');
  });

  it('RNF-06 · C3 · a la administradora, el aviso dice el motivo y quién puede enviarla de nuevo, sin llevarla a las fotos', async () => {
    conSesion(USUARIOS.ADMIN_EMPRESA);
    conResultado(LOTE, devuelta({ motivoDevolucion: 'POCOS_GRANOS', fotosPorRepetir: [] }));
    abrirApp(RUTA);

    const aviso = await avisoDeError();
    expect(aviso).toHaveTextContent('La muestra tiene menos de 100 granos: no se calculó el resultado.');
    expect(aviso).toHaveTextContent('Un usuario final de tu empresa puede revisar las fotos y enviarla de nuevo.');
    expect(screen.queryByRole('link', { name: 'Volver a las fotos' })).not.toBeInTheDocument();
  });

  it('D-25 · si volvió la muestra 2, el aviso dice su número, junto al resultado de la primera', async () => {
    conResultado(LOTE, resultadoConSegundaMuestra('PENDIENTE', {
      fechaEnvio: '2026-11-23T17:10:00Z', devolucion: { fotosPorRepetir: [{ numero: 1, motivo: 'BORROSA' }] },
    }));
    abrirApp(RUTA);

    expect(await avisoDeError()).toHaveTextContent('Hay que repetir la foto 1 de la muestra 2: foto borrosa.');
    expect(screen.getByRole('region', { name: 'Resumen del resultado' })).toBeInTheDocument();
  });

  it('D-25 · la muestra 2 sin respuesta del servicio también dice su número', async () => {
    conResultado(LOTE, resultadoConSegundaMuestra('PENDIENTE', {
      fechaEnvio: '2026-11-23T17:10:00Z', devolucion: { motivoDevolucion: 'SIN_RESPUESTA_DE_LA_IA' },
    }));
    abrirApp(RUTA);

    expect(await avisoDeError()).toHaveTextContent('El servicio de IA no respondió: la muestra 2 volvió a quedar '
      + 'pendiente, sin resultado.');
  });

  it('D-25 · la muestra 2 con menos de 100 granos también dice su número', async () => {
    conResultado(LOTE, resultadoConSegundaMuestra('PENDIENTE', {
      fechaEnvio: '2026-11-23T17:10:00Z', devolucion: { motivoDevolucion: 'POCOS_GRANOS' },
    }));
    abrirApp(RUTA);

    expect(await avisoDeError()).toHaveTextContent('La muestra 2 tiene menos de 100 granos: no se calculó el resultado.');
  });

  it('RNF-06 · C3 · en inglés, el aviso dice el motivo en inglés, en minúscula dentro de la frase', async () => {
    await act(() => i18n.changeLanguage('en'));
    conResultado(LOTE, devuelta({ fotosPorRepetir: [{ numero: 2, motivo: 'POCA_LUZ' }] }));
    abrirApp(RUTA);

    const aviso = await avisoDeError();
    expect(aviso).toHaveTextContent('Photo 2 of the sample needs a retake: too dark.');
    expect(aviso).toHaveTextContent('Take it again with more light and no harsh shadows.');
  });
});

describe('Aviso de confianza baja', () => {
  beforeEach(() => conSesion(USUARIOS.USUARIO_FINAL));
  afterEach(async () => {
    await act(() => i18n.changeLanguage('es'));
  });

  it('RF-09 · C4 · tras 2 repeticiones con más del máximo de dudosos, el resultado sale con «Confianza baja: confirma con la prueba de corte», sin interrumpir', async () => {
    conResultado(LOTE, conAviso(resultadoAnalizado(), [{ avisoConfianzaBaja: true, porcentajeMaxDudosos: 10.0 }]));
    abrirApp(RUTA);

    const titulo = await screen.findByText(CONFIANZA_BAJA);
    const aviso = titulo.closest('[role="status"]');
    expect(aviso).not.toBeNull();
    expect(aviso).toHaveTextContent('Más del 10 % de los granos quedaron dudosos, aun después de repetir las fotos 2 veces.');
    // Debajo del resumen y de la sugerencia, como en el prototipo.
    const resumen = screen.getByRole('region', { name: 'Resumen del resultado' });
    const sugerencia = screen.getByRole('region', { name: 'Sugerencia' });
    expect(resumen.compareDocumentPosition(aviso) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(sugerencia.compareDocumentPosition(aviso) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('RF-09 · C2 · sin el aviso, el resultado no habla de confianza baja', async () => {
    conResultado(LOTE, conAviso(resultadoAnalizado(), [{ avisoConfianzaBaja: false, porcentajeMaxDudosos: 10.0 }]));
    abrirApp(RUTA);

    await screen.findByRole('region', { name: 'Resumen del resultado' });
    expect(screen.queryByText(CONFIANZA_BAJA)).not.toBeInTheDocument();
  });

  it('RF-09 · C6 · si la muestra 2 salió con el aviso, el resultado del lote también lo tiene, con el máximo de esa muestra', async () => {
    conResultado(LOTE, conAviso(resultadoConDosMuestras(), [
      { avisoConfianzaBaja: false, porcentajeMaxDudosos: 10.0 }, { avisoConfianzaBaja: true, porcentajeMaxDudosos: 12.5 },
    ]));
    abrirApp(RUTA);

    const aviso = (await screen.findByText(CONFIANZA_BAJA)).closest('[role="status"]');
    expect(aviso).toHaveTextContent('Más del 12,5 % de los granos quedaron dudosos');
  });

  it('RF-09 · C4 · en inglés, el aviso dice lo mismo, con el punto decimal', async () => {
    await act(() => i18n.changeLanguage('en'));
    conResultado(LOTE, conAviso(resultadoAnalizado(), [{ avisoConfianzaBaja: true, porcentajeMaxDudosos: 10.5 }]));
    abrirApp(RUTA);

    const aviso = (await screen.findByText('Low confidence: confirm with the cut test.')).closest('[role="status"]');
    expect(aviso).toHaveTextContent('More than 10.5 % of the beans were uncertain, even after retaking the photos twice.');
  });

  it('RF-09 · C4 · si el lote trae el aviso y la muestra no trae su máximo, el aviso sale sin el detalle', async () => {
    const resultado = resultadoAnalizado({ avisoConfianzaBaja: true });
    conResultado(LOTE, resultado);
    abrirApp(RUTA);

    const aviso = (await screen.findByText(CONFIANZA_BAJA)).closest('[role="status"]');
    await waitFor(() => expect(aviso).not.toHaveTextContent('dudosos'));
    expect(within(aviso).getByText(CONFIANZA_BAJA)).toBeInTheDocument();
  });
});
