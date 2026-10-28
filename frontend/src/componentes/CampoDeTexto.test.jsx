/*
 * Nombre: CampoDeTexto.test.jsx
 * Descripción: Pruebas del campo de texto de los formularios: su ayuda, como la del NIT, queda debajo y unida al
 *              campo para los lectores de pantalla, también cuando el campo tiene un error (RNF-06); y, con
 *              variasLineas, va en varias líneas, como la recomendación de un rango (RF-13).
 * Fecha de creación: 26/10/2026
 * Actualización: 28/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import { render, screen } from '@testing-library/react';
import { Formik } from 'formik';
import { describe, expect, it } from 'vitest';
import { CampoDeTexto } from './CampoDeTexto.jsx';

const AYUDA = 'Sin puntos y con el dígito de verificación: 900123456-8.';

/**
 * Dibuja el campo del NIT dentro de un formulario de Formik.
 *
 * @param {object} [estado] el estado inicial del formulario: sus errores y los campos tocados
 */
function dibujarNit(estado = {}) {
  render(
    <Formik initialValues={{ nit: '' }} onSubmit={() => {}} {...estado}>
      <CampoDeTexto nombre="nit" etiqueta="NIT" ayuda={AYUDA} />
    </Formik>,
  );
}

describe('campo de texto', () => {
  it('RNF-06 · la ayuda de un campo se ve debajo de él y queda unida al campo para los lectores de pantalla', () => {
    dibujarNit();

    expect(screen.getByText(AYUDA)).toBeVisible();
    expect(screen.getByLabelText('NIT')).toHaveAccessibleDescription(AYUDA);
  });

  it('RNF-06 · C3 · con un error, el campo dice qué pasó y conserva su ayuda: los dos quedan unidos al campo', () => {
    const error = 'El dígito de verificación del NIT no corresponde.';

    dibujarNit({ initialErrors: { nit: error }, initialTouched: { nit: true } });

    const campo = screen.getByLabelText('NIT');
    expect(campo).toHaveAttribute('aria-invalid', 'true');
    expect(campo).toHaveAccessibleDescription(`${error} ${AYUDA}`);
  });

  it('RNF-06 · un campo sin ayuda no la anuncia', () => {
    render(
      <Formik initialValues={{ ciudad: '' }} onSubmit={() => {}}>
        <CampoDeTexto nombre="ciudad" etiqueta="Ciudad" />
      </Formik>,
    );

    expect(screen.getByLabelText('Ciudad')).not.toHaveAttribute('aria-describedby');
  });

  it('RP-23 · con variasLineas, el campo de texto va en varias líneas, como la recomendación de un rango', () => {
    render(
      <Formik initialValues={{ recomendacion: 'Apto para venta estándar.' }} onSubmit={() => {}}>
        <CampoDeTexto nombre="recomendacion" etiqueta="Recomendación" variasLineas />
      </Formik>,
    );

    const campo = screen.getByLabelText('Recomendación');
    expect(campo.tagName).toBe('TEXTAREA');
    expect(campo).toHaveValue('Apto para venta estándar.');
  });
});
