/*
 * Nombre: CampoContrasena.jsx
 * Descripción: Un campo de contraseña con el botón para mostrarla y volver a ocultarla, de 44 px para el
 *              dedo (RNF-05).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Visibility from '@mui/icons-material/Visibility';
import VisibilityOff from '@mui/icons-material/VisibilityOff';
import IconButton from '@mui/material/IconButton';
import InputAdornment from '@mui/material/InputAdornment';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { CampoDeTexto } from './CampoDeTexto.jsx';

/**
 * Un campo de contraseña.
 *
 * @param {object} props las propiedades
 * @param {string} props.nombre el nombre del campo en Formik
 * @param {string} props.etiqueta su etiqueta
 * @param {string} [props.mostrar] el nombre del botón para mostrarla; por defecto, «Mostrar la contraseña»
 * @param {string} [props.ocultar] el nombre del botón para ocultarla; por defecto, «Ocultar la contraseña»
 * @param {string} [props.autoComplete] qué contraseña es para el navegador: current-password o new-password
 * @param {string} [props.descritoPor] el id de otro texto que la describe, como sus reglas
 * @returns {import('react').ReactElement} el campo
 */
export function CampoContrasena({ mostrar, ocultar, ...props }) {
  const { t } = useTranslation();
  const [visible, setVisible] = useState(false);

  return (
    <CampoDeTexto {...props} type={visible ? 'text' : 'password'} alFinal={(
      <InputAdornment position="end">
        <IconButton edge="end" onClick={() => setVisible(!visible)}
          aria-label={visible ? (ocultar ?? t('contrasena.ocultar')) : (mostrar ?? t('contrasena.mostrar'))}>
          {visible ? <VisibilityOff /> : <Visibility />}
        </IconButton>
      </InputAdornment>
    )} />
  );
}
