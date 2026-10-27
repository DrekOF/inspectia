/*
 * Nombre: MensajeDeLaFilaPropia.jsx
 * Descripción: El mensaje de una regla de la fila propia en el formulario de una pantalla generada (RP-23), en
 *              lugar del campo que esa regla no deja cambiar ahí, como la contraseña de quien tiene la sesión:
 *              «Para cambiar tu propia contraseña, usa «Cambiar contraseña».», con el nombre de esa página como
 *              enlace (RF-03 C11): el texto del enlace dice a dónde lleva.
 * Fecha de creación: 27/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Link from '@mui/material/Link';
import Typography from '@mui/material/Typography';
import { useTranslation } from 'react-i18next';
import { Link as EnlaceDeRuta } from 'react-router';

/** La ruta de Cambiar contraseña (RF-18), en «Mi cuenta». */
const RUTA_CAMBIAR_CONTRASENA = '/cambiar-contrasena';

/**
 * El mensaje de una regla de la fila propia. Si lleva a Cambiar contraseña, el nombre de esa opción de «Mi cuenta»
 * dentro del mensaje es un enlace a ella, en el idioma de la app: «Cambiar contraseña» o “Change password”.
 *
 * @param {object} props las propiedades
 * @param {string} props.mensaje el mensaje de la regla, ya traducido
 * @param {boolean} [props.cambiarContrasena] si el mensaje lleva a Cambiar contraseña
 * @returns {import('react').ReactElement} el mensaje
 */
export function MensajeDeLaFilaPropia({ mensaje, cambiarContrasena = false }) {
  const { t } = useTranslation();
  const pagina = t('cuenta.cambiarContrasena');
  const posicion = cambiarContrasena ? mensaje.indexOf(pagina) : -1;
  if (posicion < 0) {
    return <Typography variant="body2" color="text.secondary">{mensaje}</Typography>;
  }
  return (
    <Typography variant="body2" color="text.secondary">
      {mensaje.slice(0, posicion)}
      <Link component={EnlaceDeRuta} to={RUTA_CAMBIAR_CONTRASENA}>{pagina}</Link>
      {mensaje.slice(posicion + pagina.length)}
    </Typography>
  );
}
