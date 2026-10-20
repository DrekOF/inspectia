/*
 * Nombre: CambiarContrasena.jsx
 * Descripción: La pantalla Cambiar contraseña (RF-18), como en el prototipo: la contraseña actual, la nueva
 *              con sus reglas a la vista mientras se escribe, y la nueva repetida. Al guardar, esta sesión
 *              sigue con el token nuevo; si la actual está errada la marca, y si la cuenta está bloqueada dice
 *              a qué hora termina el bloqueo.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import CancelOutlined from '@mui/icons-material/CancelOutlined';
import CheckCircleOutlineOutlined from '@mui/icons-material/CheckCircleOutlineOutlined';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';
import { Form, Formik } from 'formik';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { erroresPorCampo, textoDelError } from '../api/errores.js';
import { cambiarContrasena } from '../api/sesion.js';
import { Aviso } from '../componentes/Aviso.jsx';
import { CampoContrasena } from '../componentes/CampoContrasena.jsx';
import { EnfocarPrimerError } from '../componentes/EnfocarPrimerError.jsx';
import { TituloDePagina } from '../componentes/TituloDePagina.jsx';
import { idiomaDe } from '../i18n/i18n.js';
import { useSesion } from '../sesion/contexto.js';
import { medidas } from '../tema/tokens.js';
import { esquemaCambioDeContrasena } from '../validacion/esquemas.js';
import { reglasDeContrasena } from '../validacion/reglas.js';

const CAMPOS = ['contrasenaActual', 'contrasenaNueva', 'contrasenaRepetida'];
const VALORES_INICIALES = { contrasenaActual: '', contrasenaNueva: '', contrasenaRepetida: '' };
const ID_REGLAS = 'reglas-contrasena-nueva';

/** Las reglas de la contraseña nueva, con su texto. */
const REGLAS = [
  ['largo', 'cambiarContrasena.reglaLargo'],
  ['bytes', 'cambiarContrasena.reglaBytes'],
  ['espacios', 'cambiarContrasena.reglaEspacios'],
];

/**
 * Las reglas de la contraseña nueva y si cada una se cumple con lo que lleva escrito (RF-18 C3 y C4).
 *
 * @param {{ contrasena: string }} props la contraseña nueva
 * @returns {import('react').ReactElement} las reglas
 */
function Reglas({ contrasena }) {
  const { t } = useTranslation();
  const cumple = reglasDeContrasena(contrasena);
  return (
    <Box id={ID_REGLAS} sx={{ display: 'flex', flexDirection: 'column', gap: 1, p: '14px 16px', borderRadius: 1,
      bgcolor: 'background.default' }}>
      <Typography variant="body2" sx={{ fontWeight: 500 }}>{t('cambiarContrasena.reglas')}</Typography>
      <Box component="ul" sx={{ m: 0, p: 0, listStyle: 'none', display: 'flex', flexDirection: 'column', gap: 0.75 }}>
        {REGLAS.map(([regla, texto]) => (
          <Box component="li" key={regla} sx={{ display: 'flex', gap: 1, alignItems: 'flex-start', fontSize: 14 }}>
            {cumple[regla]
              ? <CheckCircleOutlineOutlined aria-hidden sx={(theme) => ({ fontSize: 20, color: theme.vars.palette.cacao.exito.principal })} />
              : <CancelOutlined aria-hidden sx={{ fontSize: 20, color: 'text.secondary' }} />}
            <span>
              <span>{t(texto)}</span>{' '}
              <Box component="span" sx={(theme) => ({ fontWeight: 500,
                color: cumple[regla] ? theme.vars.palette.cacao.exito.texto : theme.vars.palette.text.secondary })}>
                {t(cumple[regla] ? 'cambiarContrasena.cumple' : 'cambiarContrasena.noCumple')}
              </Box>
            </span>
          </Box>
        ))}
      </Box>
    </Box>
  );
}

/**
 * El aviso de un error que no es de un campo: el bloqueo de la cuenta con la hora en que termina, o
 * cualquier otro con el mensaje del backend.
 *
 * @param {{ error: Error }} props el error
 * @returns {import('react').ReactElement} el aviso
 */
function AvisoDeError({ error }) {
  const { t, i18n } = useTranslation();
  if (error.bloqueadoHasta) {
    const hora = new Intl.DateTimeFormat(idiomaDe(i18n.language).region, { hour: 'numeric', minute: '2-digit' })
      .format(new Date(error.bloqueadoHasta));
    return <Aviso tipo="warning" titulo={textoDelError(error, t)}>{t('cambiarContrasena.bloqueoHasta', { hora })}</Aviso>;
  }
  return <Aviso tipo="error" titulo={textoDelError(error, t)}>{error.ayuda}</Aviso>;
}

/**
 * Cambiar contraseña.
 *
 * @returns {import('react').ReactElement} la pantalla
 */
export default function CambiarContrasena() {
  const { t } = useTranslation();
  const { pedir, iniciar } = useSesion();
  const [resultado, setResultado] = useState(null);

  const guardar = async ({ contrasenaActual, contrasenaNueva }, { setErrors, resetForm }) => {
    setResultado(null);
    try {
      iniciar(await cambiarContrasena(pedir, { contrasenaActual, contrasenaNueva }));
      resetForm();
      setResultado({ cambiada: true });
    } catch (error) {
      const deLosCampos = erroresPorCampo(error, CAMPOS);
      if (deLosCampos) {
        setErrors(deLosCampos);
      } else {
        setResultado({ error });
      }
    }
  };

  return (
    <>
      <TituloDePagina titulo={t('cambiarContrasena.titulo')} descripcion={t('cambiarContrasena.subtitulo')} />
      <Formik initialValues={VALORES_INICIALES} validationSchema={esquemaCambioDeContrasena(t)} onSubmit={guardar}>
        {({ values, isSubmitting }) => (
          <Box component={Form} noValidate aria-label={t('cambiarContrasena.titulo')} sx={(theme) => ({
            maxWidth: 560,
            display: 'flex',
            flexDirection: 'column',
            gap: 2.5,
            p: { xs: 2.5, md: '28px' },
            borderRadius: `${medidas.tarjeta.radio}px`,
            border: `1px solid ${theme.vars.palette.divider}`,
            bgcolor: 'background.paper',
          })}>
            <EnfocarPrimerError campos={CAMPOS} />
            {resultado?.cambiada && <Aviso tipo="success">{t('cambiarContrasena.listo')}</Aviso>}
            {resultado?.error && <AvisoDeError error={resultado.error} />}
            <CampoContrasena nombre="contrasenaActual" etiqueta={t('cambiarContrasena.actual')}
              autoComplete="current-password" mostrar={t('cambiarContrasena.mostrarActual')}
              ocultar={t('cambiarContrasena.ocultarActual')} />
            <CampoContrasena nombre="contrasenaNueva" etiqueta={t('cambiarContrasena.nueva')}
              autoComplete="new-password" descritoPor={ID_REGLAS} mostrar={t('cambiarContrasena.mostrarNueva')}
              ocultar={t('cambiarContrasena.ocultarNueva')} />
            <CampoContrasena nombre="contrasenaRepetida" etiqueta={t('cambiarContrasena.repetida')}
              autoComplete="new-password" mostrar={t('cambiarContrasena.mostrarRepetida')}
              ocultar={t('cambiarContrasena.ocultarRepetida')} />
            <Reglas contrasena={values.contrasenaNueva} />
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1.5 }}>
              <Button type="submit" variant="contained" disabled={isSubmitting}>{t('accion.guardar')}</Button>
              <Button component={Link} to="/" variant="outlined">{t('accion.cancelar')}</Button>
            </Box>
          </Box>
        )}
      </Formik>
    </>
  );
}
