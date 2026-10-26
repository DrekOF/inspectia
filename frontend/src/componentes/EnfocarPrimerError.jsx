/*
 * Nombre: EnfocarPrimerError.jsx
 * Descripción: Después de enviar un formulario de Formik con errores, lleva el foco al primer campo marcado,
 *              para que quien usa el teclado o un lector de pantalla llegue al error (RNF-06 C3 y C4). También
 *              los campos de un grupo, como administrador.correo.
 * Fecha de creación: 20/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import { getIn, useFormikContext } from 'formik';
import { useEffect, useRef } from 'react';

/**
 * Enfoca el primer campo con error al terminar cada envío, sea por la validación o por el backend.
 *
 * @param {{ campos: string[] }} props los nombres de los campos, en el orden del formulario, como nit o
 *   administrador.correo; cada uno es también el id de su campo
 * @returns {null} no muestra nada
 */
export function EnfocarPrimerError({ campos }) {
  const { submitCount, isSubmitting, isValidating, errors } = useFormikContext();
  const envioRevisado = useRef(0);

  useEffect(() => {
    if (submitCount === envioRevisado.current || isSubmitting || isValidating) {
      return;
    }
    envioRevisado.current = submitCount;
    const primero = campos.find((campo) => getIn(errors, campo));
    if (primero) {
      document.getElementById(primero)?.focus();
    }
  }, [submitCount, isSubmitting, isValidating, errors, campos]);

  return null;
}
