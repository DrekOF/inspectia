/*
 * Nombre: CampoDeTexto.jsx
 * Descripción: Un campo de un formulario de Formik como el del prototipo: la etiqueta arriba, 48 px de alto
 *              y, debajo, la ayuda o el error con su ícono. El error marca el campo con aria-invalid y queda
 *              unido a él con aria-describedby, para los lectores de pantalla (RNF-06 C3 y C4).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import ErrorOutlineOutlined from '@mui/icons-material/ErrorOutlineOutlined';
import Box from '@mui/material/Box';
import FormControl from '@mui/material/FormControl';
import FormHelperText from '@mui/material/FormHelperText';
import FormLabel from '@mui/material/FormLabel';
import OutlinedInput from '@mui/material/OutlinedInput';
import { useField } from 'formik';

/**
 * Un campo de texto.
 *
 * @param {object} props las propiedades
 * @param {string} props.nombre el nombre del campo en Formik; también es su id
 * @param {string} props.etiqueta su etiqueta
 * @param {string} [props.descritoPor] el id de otro texto que lo describe, como las reglas de una contraseña
 * @param {import('react').ReactNode} [props.alFinal] lo que va al final del campo, como un botón
 * @param {string} [props.type] el tipo del campo, como email o password
 * @param {string} [props.autoComplete] qué dato es para el navegador, como username
 * @returns {import('react').ReactElement} el campo
 */
export function CampoDeTexto({ nombre, etiqueta, descritoPor, alFinal, ...props }) {
  const [campo, meta] = useField(nombre);
  const error = meta.touched && meta.error ? meta.error : null;
  const idDelError = `${nombre}-error`;
  const descripcion = [error && idDelError, descritoPor].filter(Boolean).join(' ') || undefined;

  return (
    <FormControl fullWidth error={Boolean(error)}>
      <FormLabel htmlFor={nombre}>{etiqueta}</FormLabel>
      <OutlinedInput id={nombre} {...campo} {...props} endAdornment={alFinal}
        inputProps={{ 'aria-describedby': descripcion }} />
      {error && (
        <FormHelperText id={idDelError} component="div">
          <Box component="span" sx={{ display: 'flex', gap: 0.75, alignItems: 'flex-start' }}>
            <ErrorOutlineOutlined aria-hidden sx={{ fontSize: 18, mt: '1px' }} />
            <span>{error}</span>
          </Box>
        </FormHelperText>
      )}
    </FormControl>
  );
}
