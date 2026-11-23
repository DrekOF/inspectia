/*
 * Nombre: CasillaDeFoto.jsx
 * Descripción: La casilla de una foto de la muestra, como en el prototipo (RF-05): vacía, con «Elegir archivo» en el
 *              PC o «Tomar foto», que abre la cámara trasera, en el celular (C1); o con la foto ya subida, su estado y
 *              «Reemplazar». Revisa el archivo antes de pedir el enlace (C3), lo sube directo al enlace firmado
 *              (D-41), lo confirma y dice cómo terminó; si falló, ofrece «Reintentar».
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import CloudDone from '@mui/icons-material/CloudDone';
import ErrorOutline from '@mui/icons-material/ErrorOutlineOutlined';
import FileUpload from '@mui/icons-material/FileUpload';
import PhotoCamera from '@mui/icons-material/PhotoCamera';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import LinearProgress from '@mui/material/LinearProgress';
import Typography from '@mui/material/Typography';
import { useId, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { textoDelError } from '../api/errores.js';
import { confirmarFoto, pedirSubida, subirArchivo } from '../api/fotos.js';
import { ChipDeEstado } from '../componentes/ChipDeEstado.jsx';
import { OCULTO_A_LA_VISTA } from '../componentes/ocultoALaVista.js';
import { useEsPc } from '../componentes/useEsPc.js';
import { useSesion } from '../sesion/contexto.js';
import { letra, medidas } from '../tema/tokens.js';
import { ACEPTA, revisarArchivo } from './archivosDeFoto.js';

/** @typedef {import('@mui/material/styles').SxProps<import('@mui/material/styles').Theme>} Estilo */

/** Los datos de la foto que el backend puede rechazar: reintentar no sirve, hay que elegir otra foto. */
const DATOS_DE_LA_FOTO = ['tipo', 'tamanoBytes', 'numero'];

/** Sin una subida en curso ni un error. */
const QUIETA = { fase: 'quieta' };

/**
 * El mensaje de un error de la subida: el del dato que el backend rechazó, que dice qué hacer, o el del error.
 *
 * @param {Error} error el error, normalmente un ErrorDeApi
 * @param {(clave: string) => string} t la función de traducción de i18next
 * @returns {string} el mensaje
 */
function mensajeDelError(error, t) {
  const deUnDato = (error.errores ?? []).find(({ campo }) => campo);
  return deUnDato ? deUnDato.mensaje : textoDelError(error, t);
}

/**
 * Si vale la pena reintentar una subida que falló: sí, si falló la conexión, el servidor o el enlace; no, si el lote
 * no existe o ya no está pendiente, o si el backend rechazó la foto.
 *
 * @param {Error} error el error, normalmente un ErrorDeApi
 * @returns {boolean} si se ofrece «Reintentar»
 */
function sePuedeReintentar(error) {
  if (error.estado === 404 || error.estado === 409) {
    return false;
  }
  return !(error.errores ?? []).some(({ campo }) => DATOS_DE_LA_FOTO.includes(campo));
}

/**
 * El estilo de la casilla, como en el prototipo: vacía, con el borde punteado y, en el PC, el contenido al centro;
 * con la foto, una tarjeta. Con un error, el borde va en el color de los errores.
 *
 * @param {{ vacia: boolean, esPc: boolean, conError: boolean }} opciones cómo está la casilla y dónde se ve
 * @returns {Estilo} el estilo
 */
function estiloDeLaCasilla({ vacia, esPc, conError }) {
  return (theme) => {
    const { cacao } = theme.vars.palette;
    const comun = { display: 'flex', flexDirection: 'column', gap: esPc ? '10px' : 1, minWidth: 0,
      borderRadius: `${medidas.tarjeta.radio}px` };
    if (vacia) {
      return {
        ...comun, p: esPc ? 2 : 1.5, minHeight: esPc ? 300 : undefined,
        border: `2px dashed ${conError ? cacao.error.principal : cacao.bordeCampo}`,
        justifyContent: esPc ? 'center' : 'flex-start', alignItems: esPc ? 'center' : 'stretch',
        textAlign: esPc ? 'center' : 'left',
      };
    }
    return {
      ...comun, p: esPc ? 1.5 : 1, bgcolor: 'background.paper',
      border: `1px solid ${conError ? cacao.error.principal : theme.vars.palette.divider}`,
    };
  };
}

/**
 * La casilla de una foto.
 *
 * @param {object} props las propiedades
 * @param {string} props.loteId el lote
 * @param {number} props.numero el número de la foto, de 1 a 3; la 3 es opcional
 * @param {import('../api/fotos.js').Foto} [props.foto] la foto ya subida, si la hay
 * @param {(foto: import('../api/fotos.js').Foto) => void} props.alConfirmar se llama con la foto que se confirmó
 * @param {() => void} props.alFallarLaVista se llama si la imagen no carga, como cuando su enlace venció
 * @returns {import('react').ReactElement} la casilla
 */
export function CasillaDeFoto({ loteId, numero, foto, alConfirmar, alFallarLaVista }) {
  const { t } = useTranslation();
  const { pedir } = useSesion();
  const esPc = useEsPc();
  const entrada = useRef(null);
  const idDelTitulo = useId();
  const [subida, setSubida] = useState(QUIETA);
  const [anuncio, setAnuncio] = useState('');
  const vacia = !foto;
  const subiendo = subida.fase === 'subiendo';
  const fallida = subida.fase === 'fallida';

  /**
   * Revisa la foto, pide el enlace, la sube y la confirma. Si algo falla, guarda el archivo para reintentar.
   *
   * @param {File} archivo la foto elegida
   */
  async function subir(archivo) {
    setAnuncio('');
    try {
      const revision = await revisarArchivo(archivo);
      if (revision.error) {
        setSubida({ fase: 'fallida', mensaje: t(revision.error), reintentable: false });
        return;
      }
      setSubida({ fase: 'subiendo' });
      setAnuncio(t('fotos.subiendo', { numero }));
      const enlace = await pedirSubida(pedir, loteId, numero, { tipo: revision.tipo, tamanoBytes: archivo.size });
      await subirArchivo(enlace, archivo);
      alConfirmar(await confirmarFoto(pedir, loteId, numero, enlace.llave));
      setSubida(QUIETA);
      setAnuncio(t('fotos.cargada', { numero }));
    } catch (error) {
      setSubida({ fase: 'fallida', mensaje: mensajeDelError(error, t), reintentable: sePuedeReintentar(error), archivo });
      setAnuncio('');
    }
  }

  /**
   * Sube la foto elegida. Vacía la elección, para que elegir otra vez el mismo archivo también cuente.
   *
   * @param {import('react').ChangeEvent<HTMLInputElement>} evento el cambio del selector
   */
  function alElegir(evento) {
    const [elegida] = evento.target.files ?? [];
    evento.target.value = '';
    if (elegida && !subiendo) {
      subir(elegida);
    }
  }

  /** Abre el selector de archivos, o la cámara en el celular, dentro del mismo clic: iOS no lo abre después. */
  function abrirSelector() {
    if (!subiendo) {
      entrada.current?.click();
    }
  }

  let boton = { texto: t('fotos.reemplazar'), variante: 'outlined', icono: undefined, alto: medidas.zonaTactil };
  if (vacia) {
    boton = esPc
      ? { texto: t('fotos.elegirArchivo'), variante: 'outlined', icono: undefined, alto: medidas.zonaTactil }
      : { texto: t('fotos.tomarFoto'), variante: 'contained', icono: <PhotoCamera />, alto: medidas.boton.alto };
  }

  return (
    <Box role="group" aria-labelledby={idDelTitulo} sx={estiloDeLaCasilla({ vacia, esPc, conError: fallida })}>
      {vacia
        ? esPc && <FileUpload aria-hidden sx={{ fontSize: 36, color: 'text.secondary' }} />
        : (
          <Box component="img" src={foto.vista.url} alt={t('fotos.imagen', { numero })} onError={alFallarLaVista}
            decoding="async" sx={{
              display: 'block', width: '100%', aspectRatio: '4 / 3', objectFit: 'cover', bgcolor: 'action.hover',
              borderRadius: esPc ? '8px' : '6px',
            }} />
        )}
      <Box sx={{
        display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 1,
        justifyContent: vacia && esPc ? 'center' : 'space-between',
      }}>
        <Typography id={idDelTitulo} sx={{ fontSize: 15, fontWeight: letra.pesos.media, color: 'text.primary' }}>
          {numero === 3 ? t('fotos.fotoOpcional', { numero }) : t('fotos.foto', { numero })}
        </Typography>
        {!vacia && <ChipDeEstado texto={t(`foto.estado.${foto.estado}`)} tono="informacion" icono={<CloudDone />} />}
      </Box>
      {vacia && numero === 3 && (
        <Typography variant="body2" color="text.secondary">{t('fotos.ayudaOpcional')}</Typography>
      )}
      {subiendo && <LinearProgress aria-label={t('fotos.subiendo', { numero })} />}
      {fallida && (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: esPc && vacia ? 'center' : 'flex-start', gap: 1 }}>
          <Typography role="alert" variant="body2" sx={(theme) => ({
            display: 'flex', gap: 0.75, alignItems: 'flex-start', textAlign: 'left', overflowWrap: 'anywhere',
            color: theme.vars.palette.cacao.error.texto,
          })}>
            <ErrorOutline aria-hidden sx={{ fontSize: 18, mt: '1px', flexShrink: 0 }} />
            {subida.mensaje}
          </Typography>
          {subida.reintentable && (
            <Button variant="outlined" onClick={() => subir(subida.archivo)} sx={{ minHeight: medidas.zonaTactil }}>
              {t('crud.reintentar')}
            </Button>
          )}
        </Box>
      )}
      {/* El mismo botón antes y después de subir la foto: así el foco sigue en él (RNF-06 C4). Mientras sube, no
          se deshabilita, porque perdería el foco: queda marcado con aria-disabled y no abre el selector. */}
      <Button variant={boton.variante} startIcon={boton.icono} onClick={abrirSelector}
        aria-disabled={subiendo || undefined} sx={{
          minHeight: boton.alto, px: '18px', alignSelf: esPc && vacia ? 'center' : 'stretch',
          '& .MuiButton-startIcon > *:nth-of-type(1)': { fontSize: 20 },
          '&[aria-disabled="true"]': { opacity: 0.6, cursor: 'progress' },
        }}>
        {boton.texto}
      </Button>
      <input ref={entrada} type="file" hidden accept={ACEPTA} capture={esPc ? undefined : 'environment'}
        aria-label={t('fotos.archivo', { numero })} onChange={alElegir} />
      <Box role="status" sx={OCULTO_A_LA_VISTA}>{anuncio}</Box>
    </Box>
  );
}
