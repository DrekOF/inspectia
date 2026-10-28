/*
 * Nombre: CampoDeTexto.jsx
 * Descripción: Un campo de un formulario de Formik como el del prototipo: la etiqueta arriba, 48 px de alto
 *              y, debajo, el error con su ícono y la ayuda. El error marca el campo con aria-invalid, y el error
 *              y la ayuda quedan unidos a él con aria-describedby, para los lectores de pantalla (RNF-06 C3 y
 *              C4). Un texto largo, como la recomendación de un rango, va en varias líneas, y una regla de todo el
 *              formulario puede marcar el campo con su aviso (RF-13).
 * Fecha de creación: 20/10/2026
 * Actualización: 28/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
import FormControl from '@mui/material/FormControl';
import FormLabel from '@mui/material/FormLabel';
import OutlinedInput from '@mui/material/OutlinedInput';
import { useField } from 'formik';
import { idsQueDescriben, MensajesDelCampo } from './MensajesDelCampo.jsx';

/**
 * Un campo en varias líneas: el alto lo dan las líneas, no los 48 px de los demás campos, y deja el mismo margen
 * alrededor del texto.
 */
const VARIAS_LINEAS = { padding: '12px 14px', '& .MuiOutlinedInput-input': { height: 'auto', padding: 0, lineHeight: 1.5 } };

/**
 * Un campo de texto.
 *
 * @param {object} props las propiedades
 * @param {string} props.nombre el nombre del campo en Formik, como nit o administrador.correo; también es su id
 * @param {string} props.etiqueta su etiqueta
 * @param {string} [props.ayuda] la ayuda que va debajo, como el formato del NIT
 * @param {string} [props.descritoPor] el id de otro texto que lo describe, como las reglas de una contraseña
 * @param {string} [props.marcadoPor] el id del aviso de una regla de todo el formulario que no se cumple, como el
 *   orden de los límites de los rangos: el campo se marca como uno con error, y el aviso lo describe
 * @param {boolean} [props.variasLineas] si el texto va en varias líneas, como la recomendación de un rango: el campo
 *   empieza con dos y crece con el texto
 * @param {import('react').ReactNode} [props.alFinal] lo que va al final del campo, como un botón
 * @param {string} [props.type] el tipo del campo, como email o password
 * @param {string} [props.autoComplete] qué dato es para el navegador, como username
 * @returns {import('react').ReactElement} el campo
 */
export function CampoDeTexto({ nombre, etiqueta, ayuda, descritoPor, marcadoPor, variasLineas = false, alFinal, ...props }) {
  const [campo, meta] = useField(nombre);
  const error = meta.touched && meta.error ? meta.error : null;

  return (
    <FormControl fullWidth error={Boolean(error || marcadoPor)}>
      <FormLabel htmlFor={nombre}>{etiqueta}</FormLabel>
      <OutlinedInput id={nombre} {...campo} multiline={variasLineas} minRows={variasLineas ? 2 : undefined}
        sx={variasLineas ? VARIAS_LINEAS : undefined} {...props} endAdornment={alFinal}
        inputProps={{ 'aria-describedby': idsQueDescriben(nombre, { marcadoPor, error, ayuda, descritoPor }) }} />
      <MensajesDelCampo nombre={nombre} error={error} ayuda={ayuda} />
    </FormControl>
  );
}
