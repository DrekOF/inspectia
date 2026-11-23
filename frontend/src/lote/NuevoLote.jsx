/*
 * Nombre: NuevoLote.jsx
 * Descripción: La pantalla Nuevo lote, el paso 1 del análisis (RF-04), como en el prototipo: el código, la fecha
 *              de recepción, la finca o el proveedor, con las sugerencias de la empresa, y las observaciones. Con
 *              datos válidos registra el lote y pasa a las fotos de la muestra; si el código ya existe en la
 *              empresa, lo marca con el aviso del prototipo.
 * Fecha de creación: 22/11/2026
 * Actualización: 22/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import { Form, Formik } from 'formik';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
import { erroresPorCampo, textoDelError } from '../api/errores.js';
import { registrarLote } from '../api/lotes.js';
import { Aviso } from '../componentes/Aviso.jsx';
import { CampoDeTexto } from '../componentes/CampoDeTexto.jsx';
import { EnfocarPrimerError } from '../componentes/EnfocarPrimerError.jsx';
import { TituloDePagina } from '../componentes/TituloDePagina.jsx';
import { useSesion } from '../sesion/contexto.js';
import { medidas } from '../tema/tokens.js';
import { CampoDeOrigen } from './CampoDeOrigen.jsx';
import { esquemaDeLote } from './esquemaDeLote.js';
import { hoyEnColombia } from './fechas.js';
import { PasosDelAnalisis } from './PasosDelAnalisis.jsx';

const CAMPOS = ['codigo', 'fechaRecepcion', 'origen', 'observaciones'];
const VALORES_INICIALES = { codigo: '', fechaRecepcion: '', origen: '', observaciones: '' };

/**
 * Nuevo lote.
 *
 * @returns {import('react').ReactElement} la pantalla
 */
export default function NuevoLote() {
  const { t } = useTranslation();
  const { pedir } = useSesion();
  const navegar = useNavigate();
  const [error, setError] = useState(null);
  const hoy = hoyEnColombia();

  const registrar = async ({ codigo, fechaRecepcion, origen, observaciones }, { setErrors }) => {
    setError(null);
    try {
      const lote = await registrarLote(pedir, {
        codigo, fechaRecepcion, origen, observaciones: observaciones.trim() ? observaciones : null,
      });
      navegar(`/lotes/${lote.id}/fotos`);
    } catch (problema) {
      const deLosCampos = erroresPorCampo(problema, CAMPOS);
      if (!deLosCampos) {
        setError(problema);
        return;
      }
      // Un código repetido se dice como en el prototipo, con el código que se escribió (RF-04 C2 y C3).
      if (problema.estado === 409 && deLosCampos.codigo) {
        deLosCampos.codigo = t('nuevoLote.codigoRepetido', { codigo: codigo.trim() });
      }
      setErrors(deLosCampos);
    }
  };

  return (
    <>
      <PasosDelAnalisis actual={1} />
      <TituloDePagina titulo={t('nuevoLote.titulo')} descripcion={t('nuevoLote.subtitulo')} />
      <Formik initialValues={VALORES_INICIALES} validationSchema={esquemaDeLote(t, hoy)} onSubmit={registrar}>
        {({ isSubmitting }) => (
          <Box component={Form} noValidate aria-label={t('nuevoLote.formulario')} sx={(theme) => ({
            maxWidth: 560, display: 'flex', flexDirection: 'column', gap: 2.5, p: { xs: 2.5, md: '28px' },
            borderRadius: `${medidas.tarjeta.radio}px`, border: `1px solid ${theme.vars.palette.divider}`,
            bgcolor: 'background.paper',
          })}>
            <EnfocarPrimerError campos={CAMPOS} />
            {error && <Aviso tipo="error" titulo={textoDelError(error, t)} />}
            <CampoDeTexto nombre="codigo" etiqueta={t('lote.codigo.etiqueta')} ayuda={t('nuevoLote.ayudaCodigo')}
              autoComplete="off" />
            <CampoDeTexto nombre="fechaRecepcion" etiqueta={t('lote.fecha_recepcion.etiqueta')}
              ayuda={t('nuevoLote.ayudaFecha')} type="date" inputProps={{ max: hoy }} />
            <CampoDeOrigen nombre="origen" etiqueta={t('lote.origen.etiqueta')} ayuda={t('nuevoLote.ayudaOrigen')} />
            <CampoDeTexto nombre="observaciones" etiqueta={`${t('lote.observaciones.etiqueta')} ${t('nuevoLote.opcional')}`}
              placeholder={t('nuevoLote.ejemploObservaciones')} variasLineas />
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1.5 }}>
              <Button type="submit" variant="contained" disabled={isSubmitting}>{t('nuevoLote.continuar')}</Button>
              <Button component={Link} to="/" variant="outlined">{t('accion.cancelar')}</Button>
            </Box>
          </Box>
        )}
      </Formik>
    </>
  );
}
