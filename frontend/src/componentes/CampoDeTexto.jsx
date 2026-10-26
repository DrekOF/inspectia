/*
 * Nombre: CampoDeTexto.jsx
 * Descripción: Un campo de un formulario de Formik como el del prototipo: la etiqueta arriba, 48 px de alto
 *              y, debajo, el error con su ícono y la ayuda. El error marca el campo con aria-invalid, y el error
 *              y la ayuda quedan unidos a él con aria-describedby, para los lectores de pantalla (RNF-06 C3 y
 *              C4).
 * Fecha de creación: 20/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import FormControl from '@mui/material/FormControl';
import FormLabel from '@mui/material/FormLabel';
import OutlinedInput from '@mui/material/OutlinedInput';
import { useField } from 'formik';
import { idsQueDescriben, MensajesDelCampo } from './MensajesDelCampo.jsx';

/**
 * Un campo de texto.
 *
 * @param {object} props las propiedades
 * @param {string} props.nombre el nombre del campo en Formik, como nit o administrador.correo; también es su id
 * @param {string} props.etiqueta su etiqueta
 * @param {string} [props.ayuda] la ayuda que va debajo, como el formato del NIT
 * @param {string} [props.descritoPor] el id de otro texto que lo describe, como las reglas de una contraseña
 * @param {import('react').ReactNode} [props.alFinal] lo que va al final del campo, como un botón
 * @param {string} [props.type] el tipo del campo, como email o password
 * @param {string} [props.autoComplete] qué dato es para el navegador, como username
 * @returns {import('react').ReactElement} el campo
 */
export function CampoDeTexto({ nombre, etiqueta, ayuda, descritoPor, alFinal, ...props }) {
  const [campo, meta] = useField(nombre);
  const error = meta.touched && meta.error ? meta.error : null;

  return (
    <FormControl fullWidth error={Boolean(error)}>
      <FormLabel htmlFor={nombre}>{etiqueta}</FormLabel>
      <OutlinedInput id={nombre} {...campo} {...props} endAdornment={alFinal}
        inputProps={{ 'aria-describedby': idsQueDescriben(nombre, { error, ayuda, descritoPor }) }} />
      <MensajesDelCampo nombre={nombre} error={error} ayuda={ayuda} />
    </FormControl>
  );
}
