/*
 * Nombre: Pendiente.jsx
 * Descripción: La página de una sección que todavía no existe: su nombre y «Esta sección estará disponible
 *              pronto.», sin códigos de requisitos a la vista.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Typography from '@mui/material/Typography';
import { useTranslation } from 'react-i18next';
import { TituloDePagina } from '../componentes/TituloDePagina.jsx';

/**
 * Una sección pendiente.
 *
 * @param {{ etiqueta: string }} props la clave del nombre de la sección, como menu.estadisticas
 * @returns {import('react').ReactElement} la pantalla
 */
export default function Pendiente({ etiqueta }) {
  const { t } = useTranslation();
  return (
    <>
      <TituloDePagina titulo={t(etiqueta)} />
      <Typography>{t('pendiente.texto')}</Typography>
    </>
  );
}
