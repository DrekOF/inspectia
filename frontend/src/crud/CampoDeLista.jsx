/*
 * Nombre: CampoDeLista.jsx
 * Descripción: El campo de un formulario generado para un atributo de tipo lista: muestra sus valores con su
 *              etiqueta y guarda el código del elegido (RP-23). Es una lista nativa del navegador, que funciona
 *              igual con el teclado, con el dedo y con los lectores de pantalla (RNF-06).
 * Fecha de creación: 26/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import FormControl from '@mui/material/FormControl';
import FormLabel from '@mui/material/FormLabel';
import NativeSelect from '@mui/material/NativeSelect';
import OutlinedInput from '@mui/material/OutlinedInput';
import { useField } from 'formik';
import { idsQueDescriben, MensajesDelCampo } from '../componentes/MensajesDelCampo.jsx';

/**
 * Un campo de lista.
 *
 * @param {object} props las propiedades
 * @param {string} props.nombre el nombre del campo en Formik; también es su id
 * @param {string} props.etiqueta su etiqueta
 * @param {string} [props.ayuda] la ayuda que va debajo
 * @param {Array<{ valor: string, etiqueta: string }>} props.opciones los valores que se pueden elegir: el código
 *   y su etiqueta
 * @returns {import('react').ReactElement} el campo
 */
export function CampoDeLista({ nombre, etiqueta, ayuda, opciones }) {
  const [campo, meta] = useField(nombre);
  const error = meta.touched && meta.error ? meta.error : null;

  return (
    <FormControl fullWidth error={Boolean(error)}>
      <FormLabel htmlFor={nombre}>{etiqueta}</FormLabel>
      <NativeSelect {...campo} input={<OutlinedInput />}
        inputProps={{ id: nombre, 'aria-describedby': idsQueDescriben(nombre, { error, ayuda }) }}>
        {opciones.map((opcion) => <option key={opcion.valor} value={opcion.valor}>{opcion.etiqueta}</option>)}
      </NativeSelect>
      <MensajesDelCampo nombre={nombre} error={error} ayuda={ayuda} />
    </FormControl>
  );
}
