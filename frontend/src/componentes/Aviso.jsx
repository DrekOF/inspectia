/*
 * Nombre: Aviso.jsx
 * Descripción: Un aviso en un recuadro con ícono, con los colores de los tokens (RP-08). Los errores y las
 *              advertencias usan role="alert" y los demás role="status", como dice el prototipo (RNF-06).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Alert from '@mui/material/Alert';
import AlertTitle from '@mui/material/AlertTitle';

/**
 * Un aviso.
 *
 * @param {object} props las propiedades
 * @param {'error'|'warning'|'info'|'success'} props.tipo el tipo de aviso
 * @param {string} [props.titulo] lo que pasó, en negrilla
 * @param {import('react').ReactNode} [props.children] el detalle, como qué hacer
 * @returns {import('react').ReactElement} el aviso
 */
export function Aviso({ tipo, titulo, children }) {
  const urgente = tipo === 'error' || tipo === 'warning';
  return (
    <Alert severity={tipo} role={urgente ? 'alert' : 'status'}>
      {titulo && <AlertTitle component="strong">{titulo}</AlertTitle>}
      {children && <span>{children}</span>}
    </Alert>
  );
}
