/*
 * Nombre: MarcarErroresDelPeriodo.jsx
 * Descripción: Marca en «Desde» y «Hasta» las fechas que el backend rechazó, con su mensaje, y lleva el foco a la
 *              primera (RNF-06 C3 y C4). Lo usan el periodo de las estadísticas (RF-16) y el de la confianza promedio
 *              del modelo (RF-17).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { useFormikContext } from 'formik';
import { useEffect, useEffectEvent } from 'react';
import { erroresPorCampo } from '../api/errores.js';
import { CAMPOS_DEL_PERIODO } from '../api/estadisticas.js';

/**
 * Marca en su campo las fechas que el backend rechazó, con su mensaje, y lleva el foco a la primera (RNF-06 C3 y C4).
 * Va dentro del formulario de Formik del periodo.
 *
 * @param {{ error: Error|null }} props el error de la última petición, si falló
 * @returns {null} no muestra nada
 */
export function MarcarErroresDelPeriodo({ error }) {
  const { setErrors, setTouched } = useFormikContext();
  const marcar = useEffectEvent((deLosCampos) => {
    // Sin validar: la validación de Yup los aceptó y borraría los del backend.
    setTouched(Object.fromEntries(Object.keys(deLosCampos).map((nombre) => [nombre, true])), false);
    setErrors(deLosCampos);
    document.getElementById(CAMPOS_DEL_PERIODO.find((nombre) => deLosCampos[nombre]))?.focus();
  });

  useEffect(() => {
    const deLosCampos = error ? erroresPorCampo(error, CAMPOS_DEL_PERIODO) : null;
    if (deLosCampos) {
      marcar(deLosCampos);
    }
  }, [error]);
  return null;
}
