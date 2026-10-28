/*
 * Nombre: MensajesDelCampo.jsx
 * Descripción: El error y la ayuda que van debajo de un campo de un formulario, unidos a él para los lectores
 *              de pantalla: primero lo que pasó y después la ayuda (RNF-06 C3). Lo que pasó también puede estar
 *              en un aviso de todo el formulario, como el de una regla de varias filas (RF-13).
 * Fecha de creación: 26/10/2026
 * Actualización: 28/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import ErrorOutlineOutlined from '@mui/icons-material/ErrorOutlineOutlined';
import Box from '@mui/material/Box';
import FormHelperText from '@mui/material/FormHelperText';

/**
 * Los ids de los textos que describen un campo, en orden: el aviso que lo marca, su error, su ayuda y otro texto
 * que lo describa.
 *
 * @param {string} nombre el nombre del campo, que es también su id
 * @param {object} mensajes lo que lo describe
 * @param {string} [mensajes.marcadoPor] el id del aviso de una regla de todo el formulario que lo marca, como el
 *   orden de los límites de los rangos
 * @param {string|null} [mensajes.error] el error, si lo tiene
 * @param {string} [mensajes.ayuda] la ayuda
 * @param {string} [mensajes.descritoPor] el id de otro texto que lo describe, como las reglas de una contraseña
 * @returns {string|undefined} los ids, separados por espacios, o undefined si nada lo describe
 */
export function idsQueDescriben(nombre, { marcadoPor, error, ayuda, descritoPor }) {
  return [marcadoPor, error && `${nombre}-error`, ayuda && `${nombre}-ayuda`, descritoPor].filter(Boolean).join(' ')
    || undefined;
}

/**
 * El error y la ayuda de un campo, debajo de él.
 *
 * @param {object} props las propiedades
 * @param {string} props.nombre el nombre del campo, que es también su id
 * @param {string|null} [props.error] el error, si lo tiene
 * @param {string} [props.ayuda] la ayuda
 * @returns {import('react').ReactElement} los mensajes
 */
export function MensajesDelCampo({ nombre, error, ayuda }) {
  return (
    <>
      {error && (
        <FormHelperText id={`${nombre}-error`} component="div">
          <Box component="span" sx={{ display: 'flex', gap: 0.75, alignItems: 'flex-start' }}>
            <ErrorOutlineOutlined aria-hidden sx={{ fontSize: 18, mt: '1px' }} />
            <span>{error}</span>
          </Box>
        </FormHelperText>
      )}
      {ayuda && <FormHelperText id={`${nombre}-ayuda`} error={false}>{ayuda}</FormHelperText>}
    </>
  );
}
