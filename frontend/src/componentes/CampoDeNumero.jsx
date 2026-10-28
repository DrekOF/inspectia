/*
 * Nombre: CampoDeNumero.jsx
 * Descripción: El campo de un número de un formulario de Formik, como el límite de un rango (RF-13): un campo de
 *              texto que abre el teclado decimal del celular y guarda lo que se escribe, con coma o con punto; lo lee
 *              su validación. Como los demás campos, el error lo marca con aria-invalid y queda unido a él (RNF-06
 *              C3). Si una regla no deja cambiarlo, como el límite de Fuera de norma, es de solo lectura, con un
 *              candado, el borde punteado del prototipo y el motivo debajo, unido al campo para los lectores de
 *              pantalla.
 * Fecha de creación: 28/10/2026
 * Actualización: 28/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import LockOutlined from '@mui/icons-material/LockOutlined';
import FormControl from '@mui/material/FormControl';
import FormLabel from '@mui/material/FormLabel';
import InputAdornment from '@mui/material/InputAdornment';
import OutlinedInput from '@mui/material/OutlinedInput';
import { useField } from 'formik';
import { idsQueDescriben, MensajesDelCampo } from './MensajesDelCampo.jsx';

/** Un campo de solo lectura, como en el prototipo: con el fondo de la página y el borde punteado. */
const SOLO_LECTURA = { bgcolor: 'background.default', '& .MuiOutlinedInput-notchedOutline': { borderStyle: 'dashed' } };

/**
 * Un campo de número.
 *
 * @param {object} props las propiedades
 * @param {string} props.nombre el nombre del campo en Formik, como filas[0].limiteInferior; también es su id
 * @param {string} props.etiqueta su etiqueta
 * @param {string|null} [props.soloLectura] por qué no se puede cambiar, como «El límite de Fuera de norma es fijo:
 *   0 %.»: el campo queda de solo lectura, con su valor, y el motivo va debajo
 * @param {string} [props.marcadoPor] el id del aviso de una regla de todo el formulario que no se cumple, como el
 *   orden de los límites: el campo se marca como uno con error, y el aviso lo describe
 * @returns {import('react').ReactElement} el campo
 */
export function CampoDeNumero({ nombre, etiqueta, soloLectura = null, marcadoPor }) {
  const [campo, meta] = useField(nombre);
  const error = meta.touched && meta.error ? meta.error : null;

  return (
    <FormControl fullWidth error={Boolean(error || marcadoPor)}>
      <FormLabel htmlFor={nombre}>{etiqueta}</FormLabel>
      <OutlinedInput id={nombre} {...campo} readOnly={Boolean(soloLectura)} sx={soloLectura ? SOLO_LECTURA : undefined}
        endAdornment={soloLectura && (
          <InputAdornment position="end">
            <LockOutlined aria-hidden sx={{ fontSize: 18, color: 'text.secondary' }} />
          </InputAdornment>
        )}
        inputProps={{
          inputMode: 'decimal',
          'aria-describedby': idsQueDescriben(nombre, { marcadoPor, error, ayuda: soloLectura }),
        }} />
      <MensajesDelCampo nombre={nombre} error={error} ayuda={soloLectura} />
    </FormControl>
  );
}
