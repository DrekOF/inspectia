/*
 * Nombre: NuevoLote.test.jsx
 * Descripción: Pruebas de la pantalla Nuevo lote (RF-04): registrar con datos válidos y pasar a las fotos, el
 *              código repetido, la fecha posterior a hoy, los datos obligatorios, las sugerencias de finca o
 *              proveedor y Cancelar.
 * Fecha de creación: 10/11/2026
 * Actualización: 10/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { fireEvent, screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import { abrirApp, conSesion, USUARIOS } from '../pruebas/ayudas.jsx';
import { hoyEnColombia, LOTE_PENDIENTE } from '../pruebas/lotes.js';
import { problema, servidor } from '../pruebas/servidor.js';

const CARLOS = USUARIOS.USUARIO_FINAL;
const CREADO = { ...LOTE_PENDIENTE, id: '0199f0a0-0000-7000-8000-000000000201', codigo: 'L-0200',
  fechaRecepcion: '2026-11-02', origen: 'Finca La Esperanza', observaciones: 'Secado al sol.' };

/**
 * Escribe los datos del lote y pulsa «Continuar a las fotos». La fecha se escribe con un evento de cambio, como
 * la deja el selector de fecha del navegador.
 *
 * @param {import('@testing-library/user-event').UserEvent} usuario quien usa la pantalla
 * @param {{ codigo?: string, fecha?: string, origen?: string, observaciones?: string }} [datos] lo que escribe
 */
async function registrar(usuario, { codigo = 'L-0200', fecha = '2026-11-02', origen = 'Finca La Esperanza',
  observaciones = 'Secado al sol.' } = {}) {
  if (codigo) await usuario.type(screen.getByLabelText('Código del lote'), codigo);
  if (fecha) fireEvent.change(screen.getByLabelText('Fecha de recepción'), { target: { value: fecha } });
  if (origen) await usuario.type(screen.getByLabelText('Finca o proveedor'), origen);
  if (observaciones) await usuario.type(screen.getByLabelText(/^Observaciones/), observaciones);
  await usuario.click(screen.getByRole('button', { name: 'Continuar a las fotos' }));
}

/**
 * Responde las sugerencias de finca o proveedor que contienen el texto, sin distinguir mayúsculas.
 *
 * @param {string[]} origenes las fincas y los proveedores que ya usó la empresa
 */
function conSugerencias(origenes) {
  servidor.use(http.get('/api/v1/lotes/origenes', ({ request }) => {
    const texto = new URL(request.url).searchParams.get('texto') ?? '';
    return HttpResponse.json(origenes.filter((origen) => origen.toLowerCase().includes(texto.toLowerCase())));
  }));
}

describe('Nuevo lote', () => {
  beforeEach(() => {
    conSesion(CARLOS);
    conSugerencias([]);
  });

  it('RF-04 · C1 · con datos válidos registra el lote y pasa a las fotos de la muestra', async () => {
    let enviado;
    servidor.use(
      http.post('/api/v1/lotes', async ({ request }) => {
        enviado = await request.json();
        return HttpResponse.json(CREADO, { status: 201 });
      }),
      http.get(`/api/v1/lotes/${CREADO.id}`, () => HttpResponse.json(CREADO)),
    );
    const { usuario } = abrirApp('/lotes/nuevo');

    await registrar(usuario);

    expect(await screen.findByRole('heading', { name: 'Fotos de la muestra' })).toBeInTheDocument();
    expect(screen.getByText(/^Lote L-0200 · Finca La Esperanza\./)).toBeInTheDocument();
    expect(window.location.pathname).toBe(`/lotes/${CREADO.id}/fotos`);
    expect(enviado).toEqual({ codigo: 'L-0200', fechaRecepcion: '2026-11-02', origen: 'Finca La Esperanza',
      observaciones: 'Secado al sol.' });
  });

  it('RF-04 · C2 · si el código ya existe en la empresa, lo marca con el aviso del prototipo y no pasa a las fotos', async () => {
    servidor.use(http.post('/api/v1/lotes', () => problema(409, 'Ya existe un registro con esos datos.', {
      errores: [{ campo: 'codigo', mensaje: 'Ya existe un lote con este código en la empresa.' }],
    })));
    const { usuario } = abrirApp('/lotes/nuevo');

    await registrar(usuario, { codigo: 'L-0142' });

    expect(await screen.findByText('Ya existe un lote con el código L-0142 en tu empresa. Usa otro código.'))
      .toBeInTheDocument();
    expect(screen.getByLabelText('Código del lote')).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByRole('heading', { name: 'Nuevo lote' })).toBeInTheDocument();
  });

  it('RF-04 · C4 · una fecha de recepción posterior a hoy se marca antes de enviar, y el campo no ofrece fechas después de hoy', async () => {
    let enviados = 0;
    servidor.use(http.post('/api/v1/lotes', () => {
      enviados += 1;
      return HttpResponse.json(CREADO, { status: 201 });
    }));
    const { usuario } = abrirApp('/lotes/nuevo');

    expect(screen.getByLabelText('Fecha de recepción')).toHaveAttribute('max', hoyEnColombia());
    await registrar(usuario, { fecha: hoyEnColombia(1) });

    expect(await screen.findByText('La fecha de recepción no puede ser posterior a hoy.')).toBeInTheDocument();
    expect(screen.getByLabelText('Fecha de recepción')).toHaveAttribute('aria-invalid', 'true');
    expect(enviados).toBe(0);
  });

  it('RF-04 · C4 · la fecha de hoy en Colombia sí se acepta (prueba de límite)', async () => {
    let enviado;
    servidor.use(
      http.post('/api/v1/lotes', async ({ request }) => {
        enviado = await request.json();
        return HttpResponse.json({ ...CREADO, fechaRecepcion: enviado.fechaRecepcion }, { status: 201 });
      }),
      http.get(`/api/v1/lotes/${CREADO.id}`, () => HttpResponse.json(CREADO)),
    );
    const { usuario } = abrirApp('/lotes/nuevo');

    await registrar(usuario, { fecha: hoyEnColombia() });

    expect(await screen.findByRole('heading', { name: 'Fotos de la muestra' })).toBeInTheDocument();
    expect(enviado.fechaRecepcion).toBe(hoyEnColombia());
  });

  it('RF-04 · el código, la fecha y la finca o el proveedor son obligatorios; las observaciones no', async () => {
    let enviados = 0;
    servidor.use(http.post('/api/v1/lotes', () => {
      enviados += 1;
      return HttpResponse.json(CREADO, { status: 201 });
    }));
    const { usuario } = abrirApp('/lotes/nuevo');

    await usuario.click(screen.getByRole('button', { name: 'Continuar a las fotos' }));

    expect(await screen.findAllByText('Este dato es obligatorio.')).toHaveLength(3);
    for (const campo of ['Código del lote', 'Fecha de recepción', 'Finca o proveedor']) {
      expect(screen.getByLabelText(campo)).toHaveAttribute('aria-invalid', 'true');
    }
    expect(screen.getByLabelText(/^Observaciones/)).not.toHaveAttribute('aria-invalid', 'true');
    expect(enviados).toBe(0);
  });

  it('RF-04 · el código tiene como máximo 30 caracteres', async () => {
    const { usuario } = abrirApp('/lotes/nuevo');

    await registrar(usuario, { codigo: 'L'.repeat(31) });

    expect(await screen.findByText('Puede tener como máximo 30 caracteres.')).toBeInTheDocument();
  });

  it('RF-04 · al escribir la finca o el proveedor sugiere los que ya usó la empresa, y al elegir uno lo escribe', async () => {
    conSugerencias(['Finca La Esperanza', 'Finca La Estrella', 'Cooperativa Agrosur']);
    const { usuario } = abrirApp('/lotes/nuevo');

    await usuario.type(screen.getByLabelText('Finca o proveedor'), 'La Es');

    const lista = await screen.findByRole('listbox', { name: 'Fincas y proveedores que ya usó tu empresa' });
    expect(within(lista).getAllByRole('option').map((opcion) => opcion.textContent))
      .toEqual(['Finca La Esperanza', 'Finca La Estrella']);
    await usuario.click(within(lista).getByRole('option', { name: 'Finca La Esperanza' }));
    expect(screen.getByLabelText('Finca o proveedor')).toHaveValue('Finca La Esperanza');
    expect(screen.getByText('Te sugerimos las que ya usó tu empresa, para que las estadísticas las agrupen bien.'))
      .toBeInTheDocument();
  });

  it('RF-04 · los pasos del análisis muestran que el lote va en el primero', () => {
    abrirApp('/lotes/nuevo');

    const pasos = screen.getByRole('list', { name: 'Pasos del análisis' });
    expect(within(pasos).getAllByRole('listitem').map((paso) => paso.textContent))
      .toEqual([expect.stringContaining('Datos del lote'), expect.stringContaining('Fotos de la muestra'),
        expect.stringContaining('Resultado')]);
    expect(within(pasos).getByText('Datos del lote').closest('li')).toHaveAttribute('aria-current', 'step');
  });

  it('RF-04 · Cancelar vuelve al inicio sin registrar nada', async () => {
    let enviados = 0;
    servidor.use(http.post('/api/v1/lotes', () => {
      enviados += 1;
      return HttpResponse.json(CREADO, { status: 201 });
    }));
    const { usuario } = abrirApp('/lotes/nuevo');

    await usuario.click(screen.getByRole('link', { name: 'Cancelar' }));

    expect(await screen.findByRole('heading', { name: 'Hola, Carlos' })).toBeInTheDocument();
    expect(enviados).toBe(0);
  });
});
