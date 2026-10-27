/*
 * Nombre: Aviso.jsx
 * Descripción: Un aviso en un recuadro con ícono, con los colores de los tokens (RP-08). Los errores y las
 *              advertencias usan role="alert" y los demás role="status", como dice el prototipo (RNF-06); una
 *              advertencia que no interrumpe, como la del máximo de usuarios activos, también usa role="status".
 * Fecha de creación: 20/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import Alert from '@mui/material/Alert';
import AlertTitle from '@mui/material/AlertTitle';

/**
 * Un aviso.
 *
 * @param {object} props las propiedades
 * @param {'error'|'warning'|'info'|'success'} props.tipo el tipo de aviso
 * @param {string} [props.titulo] lo que pasó, en negrilla
 * @param {boolean} [props.urgente] si interrumpe al lector de pantalla (role="alert"); por defecto, los errores y
 *   las advertencias
 * @param {string} [props.id] su id, para que otro elemento lo tome como su descripción
 * @param {import('react').ReactNode} [props.children] el detalle, como qué hacer
 * @returns {import('react').ReactElement} el aviso
 */
export function Aviso({ tipo, titulo, urgente = tipo === 'error' || tipo === 'warning', id, children }) {
  return (
    <Alert id={id} severity={tipo} role={urgente ? 'alert' : 'status'}>
      {titulo && <AlertTitle component="strong">{titulo}</AlertTitle>}
      {children && <span>{children}</span>}
    </Alert>
  );
}
