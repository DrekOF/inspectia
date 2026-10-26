/*
 * Nombre: Inicio.jsx
 * Descripción: El inicio de cada rol (RF-01 C1): el saludo con el nombre y qué administra o de qué empresa
 *              es, como en el prototipo. Las tarjetas de cada rol llegan con las funciones a las que llevan.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { useTranslation } from 'react-i18next';
import { TituloDePagina } from '../componentes/TituloDePagina.jsx';
import { useSesion } from '../sesion/contexto.js';

/**
 * El inicio.
 *
 * @returns {import('react').ReactElement} la pantalla
 */
export default function Inicio() {
  const { t } = useTranslation();
  const { usuario } = useSesion().sesion;
  const nombre = usuario.nombre.split(' ')[0];
  return (
    <TituloDePagina titulo={t('inicio.saludo', { nombre })} tituloDePestana={t('menu.inicio')}
      descripcion={t(`inicio.${usuario.rol}`, { empresa: usuario.empresa?.nombre })} />
  );
}
