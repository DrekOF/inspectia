/*
 * Nombre: NoEncontrada.jsx
 * Descripción: Las páginas para una dirección que no existe y para una sección de otro rol (RNF-04): dicen
 *              qué pasó y ofrecen volver al inicio (RNF-06 C3).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { TituloDePagina } from '../componentes/TituloDePagina.jsx';

/**
 * Una página con un título, un texto y el botón «Ir al inicio».
 *
 * @param {{ titulo: string, texto: string }} props el título y el texto
 * @returns {import('react').ReactElement} la pantalla
 */
function IrAlInicio({ titulo, texto }) {
  const { t } = useTranslation();
  return (
    <>
      <TituloDePagina titulo={titulo} />
      <Typography sx={{ mb: 3 }}>{texto}</Typography>
      <Button component={Link} to="/" variant="contained">{t('accion.irAlInicio')}</Button>
    </>
  );
}

/**
 * Una dirección que no existe.
 *
 * @returns {import('react').ReactElement} la pantalla
 */
export default function NoEncontrada() {
  const { t } = useTranslation();
  return <IrAlInicio titulo={t('noEncontrada.titulo')} texto={t('noEncontrada.texto')} />;
}

/**
 * Una sección de otro rol: no se abre aunque se escriba su dirección (RNF-04).
 *
 * @returns {import('react').ReactElement} la pantalla
 */
export function SinPermiso() {
  const { t } = useTranslation();
  return <IrAlInicio titulo={t('sinPermiso.titulo')} texto={t('error.prohibido')} />;
}
