/*
 * Nombre: PanelAdministradores.jsx
 * Descripción: El panel de administradores de una empresa, en la pantalla de empresas (RF-02 C12): el
 *              administrador de la plataforma ve los administradores de la empresa y restablece la contraseña
 *              de uno, con las mismas validaciones de la contraseña inicial y el aviso de que se cierran sus
 *              sesiones y se le quita el bloqueo (D-30). Es una extensión de la pantalla generada. Al abrir el
 *              panel y su formulario, el foco va a su título y, al cerrarlos, vuelve al botón que los abrió
 *              (RNF-06 C4).
 * Fecha de creación: 26/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Close from '@mui/icons-material/Close';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import CircularProgress from '@mui/material/CircularProgress';
import IconButton from '@mui/material/IconButton';
import Typography from '@mui/material/Typography';
import { Form, Formik } from 'formik';
import { useEffect, useId, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { object, reach } from 'yup';
import { erroresPorCampo, textoDelError } from '../api/errores.js';
import { Aviso } from '../componentes/Aviso.jsx';
import { CampoContrasena } from '../componentes/CampoContrasena.jsx';
import { EnfocarPrimerError } from '../componentes/EnfocarPrimerError.jsx';
import { useDevolverElFoco } from '../componentes/useDevolverElFoco.js';
import { useEnfocarAlMostrar } from '../componentes/useEnfocarAlMostrar.js';
import { esquemaParaCrear } from '../generated/empresa/empresaEsquemas.js';
import { useSesion } from '../sesion/contexto.js';
import { medidas } from '../tema/tokens.js';
import { listarAdministradores, restablecerContrasena } from './administradoresApi.js';

const CAMPOS = ['contrasena'];

/**
 * Las validaciones de la contraseña nueva: las mismas de la contraseña inicial del primer administrador, que salen
 * del JSON de usuario (RF-02 C12).
 *
 * @param {import('../crud/registros.js').Traducir} t la función de traducción
 * @returns {import('yup').ObjectSchema} el esquema
 */
function esquemaDeLaContrasena(t) {
  return object({ contrasena: reach(esquemaParaCrear(t), 'administrador.contrasena') });
}

/**
 * El formulario para restablecer la contraseña de un administrador.
 *
 * @param {object} props las propiedades
 * @param {string} props.empresaId la empresa
 * @param {import('./administradoresApi.js').Administrador} props.administrador el administrador
 * @param {() => void} props.alRestablecer se llama cuando la API restableció la contraseña
 * @param {() => void} props.alCancelar se llama al cancelar
 * @returns {import('react').ReactElement} el formulario
 */
function FormularioDeContrasena({ empresaId, administrador, alRestablecer, alCancelar }) {
  const { t } = useTranslation();
  const { pedir } = useSesion();
  const [error, setError] = useState(null);
  const idDelTitulo = useId();
  const titulo = useEnfocarAlMostrar();

  const restablecer = async ({ contrasena }, { setErrors }) => {
    setError(null);
    try {
      await restablecerContrasena(pedir, empresaId, administrador.id, contrasena);
      alRestablecer();
    } catch (falla) {
      const deLosCampos = erroresPorCampo(falla, CAMPOS);
      if (deLosCampos) {
        setErrors(deLosCampos);
      } else {
        setError(falla);
      }
    }
  };

  return (
    <Formik initialValues={{ contrasena: '' }} validationSchema={esquemaDeLaContrasena(t)} onSubmit={restablecer}>
      {({ isSubmitting }) => (
        <Box component={Form} noValidate aria-labelledby={idDelTitulo}
          sx={(theme) => ({ display: 'flex', flexDirection: 'column', gap: 2, pt: 2.5, borderTop: `1px solid ${theme.vars.palette.divider}` })}>
          <Typography id={idDelTitulo} ref={titulo} tabIndex={-1} variant="subtitle1" component="h3"
            sx={{ fontWeight: 500, outline: 'none' }}>
            {t('administradores.contrasenaDe', { nombre: administrador.nombre })}
          </Typography>
          <EnfocarPrimerError campos={CAMPOS} />
          {error && <Aviso tipo="error" titulo={textoDelError(error, t)} />}
          <CampoContrasena nombre="contrasena" etiqueta={t('administradores.contrasenaNueva')}
            ayuda={t('administradores.ayuda')} autoComplete="new-password"
            mostrar={t('cambiarContrasena.mostrarNueva')} ocultar={t('cambiarContrasena.ocultarNueva')} />
          <Typography variant="body2" color="text.secondary">
            {t('administradores.aviso', { nombre: administrador.nombre.split(' ')[0] })}
          </Typography>
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1.5 }}>
            <Button type="submit" variant="contained" disabled={isSubmitting}>{t('administradores.restablecer')}</Button>
            <Button variant="outlined" onClick={alCancelar}>{t('accion.cancelar')}</Button>
          </Box>
        </Box>
      )}
    </Formik>
  );
}

/**
 * El panel de administradores de una empresa.
 *
 * @param {object} props las propiedades
 * @param {{ id: string, nombre: string }} props.fila la empresa, como la responde la API
 * @param {() => void} props.alCerrar se llama al cerrar el panel
 * @returns {import('react').ReactElement} el panel
 */
export function PanelAdministradores({ fila, alCerrar }) {
  const { t } = useTranslation();
  const { pedir } = useSesion();
  const [respuesta, setRespuesta] = useState({ administradores: null, error: null });
  const [elegido, setElegido] = useState(null);
  const [listo, setListo] = useState(null);
  const idDelTitulo = useId();
  const titulo = useEnfocarAlMostrar();
  const foco = useDevolverElFoco();

  useEffect(() => {
    let vigente = true;
    listarAdministradores(pedir, fila.id)
      .then((administradores) => vigente && setRespuesta({ administradores, error: null }))
      .catch((error) => vigente && setRespuesta({ administradores: null, error }));
    return () => {
      vigente = false;
    };
  }, [pedir, fila.id]);

  const elegir = (administrador, boton) => {
    setListo(null);
    foco.recordar(boton);
    setElegido(administrador);
  };

  const cerrarFormulario = () => {
    setElegido(null);
    foco.devolver();
  };

  const restablecida = () => {
    setListo(t('administradores.listo', { nombre: elegido.nombre }));
    cerrarFormulario();
  };

  return (
    <Box component="section" aria-labelledby={idDelTitulo} sx={(theme) => ({
      borderRadius: `${medidas.tarjeta.radio}px`, border: `1px solid ${theme.vars.palette.divider}`,
      bgcolor: 'background.paper', p: { xs: 2.5, md: `${medidas.tarjeta.relleno}px` },
      display: 'flex', flexDirection: 'column', gap: 2,
    })}>
      <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 2 }}>
        <Box>
          <Typography id={idDelTitulo} ref={titulo} tabIndex={-1} variant="h2" sx={{ outline: 'none' }}>
            {t('administradores.titulo', { empresa: fila.nombre })}
          </Typography>
          <Typography color="text.secondary">{t('administradores.elige')}</Typography>
        </Box>
        <IconButton aria-label={t('crud.cerrar')} onClick={alCerrar}><Close aria-hidden /></IconButton>
      </Box>
      {listo && <Aviso tipo="success">{listo}</Aviso>}
      {respuesta.error && <Aviso tipo="error" titulo={textoDelError(respuesta.error, t)} />}
      {!respuesta.administradores && !respuesta.error && <CircularProgress aria-label={t('crud.cargando')} />}
      {respuesta.administradores && (
        <Box component="ul" role="list" sx={{ listStyle: 'none', m: 0, p: 0, display: 'flex', flexDirection: 'column', gap: 1 }}>
          {respuesta.administradores.map((administrador) => (
            <Box component="li" key={administrador.id} sx={{
              display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: 1.5,
            }}>
              <Box>
                <Typography sx={{ fontWeight: 500 }}>{administrador.nombre}</Typography>
                <Typography variant="body2" color="text.secondary">{administrador.correo}</Typography>
              </Box>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 1.5 }}>
                <Chip size="small" label={t(administrador.activo ? 'administradores.activo' : 'administradores.inactivo')}
                  sx={(theme) => ({
                    borderRadius: `${medidas.chip.radio}px`, fontWeight: 500,
                    bgcolor: administrador.activo ? theme.vars.palette.cacao.exito.fondo : theme.vars.palette.cacao.seleccion.fondo,
                    color: administrador.activo ? theme.vars.palette.cacao.exito.texto : theme.vars.palette.text.secondary,
                  })} />
                <Button size="small" variant="outlined" onClick={(evento) => elegir(administrador, evento.currentTarget)}
                  aria-label={t('administradores.restablecerDe', { nombre: administrador.nombre })}
                  sx={{ minHeight: medidas.boton.altoEnTabla }}>
                  {t('administradores.restablecer')}
                </Button>
              </Box>
            </Box>
          ))}
        </Box>
      )}
      {elegido && (
        <FormularioDeContrasena key={elegido.id} empresaId={fila.id} administrador={elegido}
          alRestablecer={restablecida} alCancelar={cerrarFormulario} />
      )}
    </Box>
  );
}
