/*
 * Nombre: CampoDeNumero.test.jsx
 * Descripción: Pruebas del campo de un número de los formularios, como el límite de un rango (RF-13): abre el
 *              teclado decimal del celular y guarda lo que se escribe, con coma o con punto; y, de solo lectura, no
 *              se puede cambiar y dice por qué, unido al campo para los lectores de pantalla.
 * Fecha de creación: 28/10/2026
 * Actualización: 28/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Formik } from 'formik';
import { describe, expect, it } from 'vitest';
import { CampoDeNumero } from './CampoDeNumero.jsx';

/** Por qué no se puede cambiar el límite de Fuera de norma. */
const FIJO = 'El límite de Fuera de norma es fijo: 0 %.';

/**
 * Dibuja un campo de número dentro de un formulario de Formik.
 *
 * @param {string} valor el valor inicial, como lo muestra el formulario
 * @param {object} [props] las demás propiedades del campo, como soloLectura
 */
function dibujarLimite(valor, props = {}) {
  render(
    <Formik initialValues={{ limite: valor }} onSubmit={() => {}}>
      <CampoDeNumero nombre="limite" etiqueta="Límite inferior" {...props} />
    </Formik>,
  );
}

describe('campo de número', () => {
  it('RNF-05 · el campo de un número abre el teclado decimal del celular y guarda lo que se escribe, con coma o con punto', async () => {
    const usuario = userEvent.setup();
    dibujarLimite('');

    const campo = screen.getByLabelText('Límite inferior');
    expect(campo).toHaveAttribute('inputmode', 'decimal');
    expect(campo).toHaveAttribute('type', 'text');
    await usuario.type(campo, '90,5');
    expect(campo).toHaveValue('90,5');
    await usuario.clear(campo);
    await usuario.type(campo, '90.5');
    expect(campo).toHaveValue('90.5');
  });

  it('RNF-06 · un campo de número de solo lectura no se puede cambiar y dice por qué, unido al campo para los lectores de pantalla', async () => {
    const usuario = userEvent.setup();
    dibujarLimite('0,0', { soloLectura: FIJO });

    const campo = screen.getByLabelText('Límite inferior');
    await usuario.type(campo, '5');

    expect(campo).toHaveValue('0,0');
    expect(campo).toHaveAttribute('readonly');
    expect(campo).toHaveAccessibleDescription(FIJO);
  });
});
