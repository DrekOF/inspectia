/*
 * Nombre: CampoDeLista.test.jsx
 * Descripción: Pruebas del campo de lista de los formularios generados: muestra los valores de la lista del JSON
 *              con su etiqueta, guarda su código y dice qué pasó si hay un error (RP-23, RNF-06).
 * Fecha de creación: 26/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Form, Formik } from 'formik';
import { describe, expect, it, vi } from 'vitest';
import { CampoDeLista } from './CampoDeLista.jsx';

const OPCIONES = [{ valor: 'ACTIVO', etiqueta: 'Activo' }, { valor: 'INACTIVO', etiqueta: 'Inactivo' }];

describe('campo de lista', () => {
  it('RP-23 · el campo de lista muestra los valores de la lista del JSON con su etiqueta y guarda el código del elegido', async () => {
    const enviar = vi.fn();
    const usuario = userEvent.setup();
    render(
      <Formik initialValues={{ estado: 'ACTIVO' }} onSubmit={enviar}>
        <Form>
          <CampoDeLista nombre="estado" etiqueta="Estado" opciones={OPCIONES} />
          <button type="submit">Guardar</button>
        </Form>
      </Formik>,
    );

    const lista = screen.getByLabelText('Estado');
    expect(within(lista).getAllByRole('option').map((opcion) => opcion.textContent)).toEqual(['Activo', 'Inactivo']);
    expect(lista).toHaveValue('ACTIVO');
    await usuario.selectOptions(lista, 'Inactivo');
    await usuario.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(enviar).toHaveBeenCalledWith({ estado: 'INACTIVO' }, expect.anything());
  });

  it('RNF-06 · C3 · con un error, el campo de lista dice qué pasó y queda unido al campo', () => {
    render(
      <Formik initialValues={{ estado: 'ACTIVO' }} initialErrors={{ estado: 'El valor no es válido.' }}
        initialTouched={{ estado: true }} onSubmit={() => {}}>
        <CampoDeLista nombre="estado" etiqueta="Estado" opciones={OPCIONES} ayuda="Cómo queda el producto." />
      </Formik>,
    );

    const lista = screen.getByLabelText('Estado');
    expect(lista).toHaveAttribute('aria-invalid', 'true');
    expect(lista).toHaveAccessibleDescription('El valor no es válido. Cómo queda el producto.');
  });
});
