/*
 * Nombre: IniciarSesion.jsx
 * Descripción: La pantalla Iniciar sesión (RF-01), como Main y LoginMovil del prototipo: el correo y la
 *              contraseña, los mensajes del backend tal cual, el aviso de por qué se cerró la sesión anterior,
 *              el idioma y el tema, y el enlace a Acerca de (RNF-08). La contraseña solo se revisa que no esté
 *              vacía: el largo y los bytes los revisa el backend (RF-01 C8).
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Link from '@mui/material/Link';
import Typography from '@mui/material/Typography';
import { Form, Formik } from 'formik';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Navigate, Link as EnlaceDeRuta } from 'react-router';
import { textoDelError } from '../api/errores.js';
import { iniciarSesion } from '../api/sesion.js';
import { Aviso } from '../componentes/Aviso.jsx';
import { CampoContrasena } from '../componentes/CampoContrasena.jsx';
import { CampoDeTexto } from '../componentes/CampoDeTexto.jsx';
import { EnfocarPrimerError } from '../componentes/EnfocarPrimerError.jsx';
import { Marca, NOMBRE_DE_LA_APP } from '../componentes/Marca.jsx';
import { Preferencias } from '../componentes/Preferencias.jsx';
import { useEsPc } from '../componentes/useEsPc.js';
import { useTitulo } from '../componentes/useTitulo.js';
import { useSesion } from '../sesion/contexto.js';
import { destinoTrasIniciarSesion } from '../sesion/volver.js';
import { esquemaInicioDeSesion } from '../validacion/esquemas.js';

const CAMPOS = ['correo', 'contrasena'];
const VALORES_INICIALES = { correo: '', contrasena: '' };

/**
 * La presentación de InspectIA, sobre el color de la barra: en el PC ocupa la mitad izquierda y en el
 * celular es el encabezado.
 *
 * @param {{ esPc: boolean }} props si se ve en tamaño de PC
 * @returns {import('react').ReactElement} la presentación
 */
function Presentacion({ esPc }) {
  const { t } = useTranslation();
  const colorDeApoyo = (theme) => ({ color: theme.vars.palette.cacao.barra.subtitulo });
  return (
    <Box component={esPc ? 'section' : 'header'} aria-label={esPc ? NOMBRE_DE_LA_APP : undefined}
      sx={(theme) => ({
        flex: esPc ? '1 1 420px' : 'none',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: esPc ? 'space-between' : 'flex-start',
        gap: esPc ? 6 : 1.5,
        p: esPc ? '56px 56px 48px' : '28px 16px 24px',
        bgcolor: theme.vars.palette.cacao.barra.fondo,
        color: theme.vars.palette.cacao.barra.texto,
      })}>
      <Marca tamano={esPc ? 26 : 22} sx={(theme) => ({ color: theme.vars.palette.cacao.barra.marca })} />
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2.5, maxWidth: 480 }}>
        <Typography component="h1" sx={{ fontSize: esPc ? 42 : 21, fontWeight: esPc ? 700 : 500, lineHeight: esPc ? 1.15 : 1.3 }}>
          {t('iniciarSesion.lema')}
        </Typography>
        {esPc && <Typography sx={[{ fontSize: 18 }, colorDeApoyo]}>{t('iniciarSesion.descripcion')}</Typography>}
      </Box>
      {esPc && <Typography variant="body2" sx={colorDeApoyo}>{t('iniciarSesion.universidad')}</Typography>}
    </Box>
  );
}

/**
 * Iniciar sesión. Con una sesión abierta lleva al inicio del rol, o a la pantalla donde estaba si la sesión
 * anterior se cerró sin pedirlo y entra el mismo usuario.
 *
 * @returns {import('react').ReactElement} la pantalla
 */
export default function IniciarSesion() {
  const { t } = useTranslation();
  const { sesion, cierre, pedir, iniciar } = useSesion();
  const esPc = useEsPc();
  const [regreso] = useState(() => cierre?.regreso ?? null);
  const [avisoDeCierre, setAvisoDeCierre] = useState(() => cierre?.mensaje ?? null);
  const [error, setError] = useState(null);
  useTitulo(t('iniciarSesion.titulo'));

  if (sesion) {
    return <Navigate to={destinoTrasIniciarSesion(sesion.usuario, regreso)} replace />;
  }

  const entrar = async (datos) => {
    setAvisoDeCierre(null);
    setError(null);
    try {
      iniciar(await iniciarSesion(pedir, datos));
    } catch (falla) {
      setError(falla);
    }
  };

  return (
    <Box sx={{ minHeight: '100dvh', display: 'flex', flexDirection: esPc ? 'row' : 'column', flexWrap: 'wrap',
      bgcolor: 'background.default' }}>
      <Presentacion esPc={esPc} />
      <Box component="main" sx={{ flex: esPc ? '999 1 560px' : 1, minWidth: 0, display: 'flex', alignItems: 'center',
        justifyContent: 'center', p: esPc ? '48px 24px' : '20px 16px 28px' }}>
        <Box sx={{ width: '100%', maxWidth: 440, display: 'flex', flexDirection: 'column', gap: esPc ? 3 : 2.5 }}>
          <Formik initialValues={VALORES_INICIALES} validationSchema={esquemaInicioDeSesion(t)} onSubmit={entrar}>
            {({ isSubmitting }) => (
              <Box component={Form} noValidate aria-labelledby="titulo-ingreso" sx={(theme) => ({
                display: 'flex',
                flexDirection: 'column',
                gap: 2.5,
                p: esPc ? '40px 36px' : '24px 20px',
                borderRadius: 2,
                border: `1px solid ${theme.vars.palette.divider}`,
                bgcolor: 'background.paper',
              })}>
                <EnfocarPrimerError campos={CAMPOS} />
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.75 }}>
                  <Typography id="titulo-ingreso" variant="h2" sx={{ fontSize: esPc ? 28 : 24 }}>
                    {t('iniciarSesion.titulo')}
                  </Typography>
                  <Typography color="text.secondary" sx={{ fontSize: 15 }}>{t('iniciarSesion.subtitulo')}</Typography>
                </Box>
                {avisoDeCierre && <Aviso tipo="info">{avisoDeCierre}</Aviso>}
                {error && <Aviso tipo="error" titulo={textoDelError(error, t)}>{error.ayuda}</Aviso>}
                <CampoDeTexto nombre="correo" etiqueta={t('usuario.correo.etiqueta')} type="email"
                  autoComplete="username" />
                <CampoContrasena nombre="contrasena" etiqueta={t('usuario.contrasena_hash.etiqueta')}
                  autoComplete="current-password" />
                <Button type="submit" variant="contained" fullWidth disabled={isSubmitting}>
                  {t('iniciarSesion.boton')}
                </Button>
                <Typography variant="body2" color="text.secondary">{t('iniciarSesion.olvido')}</Typography>
              </Box>
            )}
          </Formik>
          <Preferencias ancho={!esPc} />
          <Link component={EnlaceDeRuta} to="/acerca-de"
            sx={{ alignSelf: 'center', minHeight: 44, display: 'flex', alignItems: 'center', fontSize: 15 }}>
            {t('iniciarSesion.acercaDe')}
          </Link>
        </Box>
      </Box>
    </Box>
  );
}
