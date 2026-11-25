/*
 * Nombre: CasillaDeFoto.jsx
 * Descripción: La casilla de una foto de la muestra, como en el prototipo (RF-05): vacía, con «Elegir archivo» en el
 *              PC o «Tomar foto», que abre la cámara trasera, en el celular (C1); o con la foto ya subida, su estado y
 *              «Reemplazar». Revisa el archivo antes de pedir el enlace (C3), lo sube directo al enlace firmado
 *              (D-41), lo confirma y dice cómo terminó; si falló, ofrece «Reintentar». El enlace se pide para la
 *              muestra que la página espera que reciba la foto: si el lote cambió mientras la página estaba abierta,
 *              el backend responde 409 y la casilla muestra su mensaje, sin «Reintentar» (D-25). Avisa cuándo empieza
 *              y cuándo termina su subida, para que «Analizar la muestra» espere a que termine (RF-05 C2). Con la
 *              revisión del servicio de IA (D-46), el chip dice si la foto quedó «Aprobada», con los granos que
 *              encontró, o «Por repetir», con su motivo y qué hacer (RF-06 C1 a C3, RNF-06 C3) y «Repetir la foto»;
 *              con granos pegados o dudosos, la foto se ve entera con sus marcas (RF-07 C2, RF-09 C1). Sin revisión,
 *              como con el clasificador falso, queda «Cargada», como antes.
 * Fecha de creación: 23/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.3
 */
import Check from '@mui/icons-material/Check';
import Close from '@mui/icons-material/Close';
import CloudDone from '@mui/icons-material/CloudDone';
import ErrorOutline from '@mui/icons-material/ErrorOutlineOutlined';
import FileUpload from '@mui/icons-material/FileUpload';
import PhotoCamera from '@mui/icons-material/PhotoCamera';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import LinearProgress from '@mui/material/LinearProgress';
import Typography from '@mui/material/Typography';
import { useEffect, useId, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { textoDelError } from '../api/errores.js';
import { confirmarFoto, pedirSubida, subirArchivo } from '../api/fotos.js';
import { ChipDeEstado } from '../componentes/ChipDeEstado.jsx';
import { OCULTO_A_LA_VISTA } from '../componentes/ocultoALaVista.js';
import { useEsPc } from '../componentes/useEsPc.js';
import { idiomaDe } from '../i18n/i18n.js';
import { useSesion } from '../sesion/contexto.js';
import { letra, medidas } from '../tema/tokens.js';
import { ACEPTA, revisarArchivo } from './archivosDeFoto.js';
import { FotoRevisada } from './FotoRevisada.jsx';
import { MAXIMO_DE_REPETICIONES, textoDelPorcentaje } from './revision.js';

/** @typedef {import('@mui/material/styles').SxProps<import('@mui/material/styles').Theme>} Estilo */

/** Los datos de la foto que el backend puede rechazar: reintentar no sirve, hay que elegir otra foto. */
const DATOS_DE_LA_FOTO = ['tipo', 'tamanoBytes', 'numero'];

/** Sin una subida en curso ni un error. */
const QUIETA = { fase: 'quieta' };

/** El tono y el ícono del chip de cada estado de la foto, como en el prototipo: el ícono acompaña al color (RNF-06). */
const CHIPS = {
  CARGADA: { tono: 'informacion', Icono: CloudDone },
  APROBADA: { tono: 'exito', Icono: Check },
  POR_REPETIR: { tono: 'error', Icono: Close },
};

/** Una muestra sin repeticiones por granos dudosos ni máximo evaluado. */
const SIN_REPETICIONES = { repeticiones: 0, porcentajeMaxDudosos: null };

/** Lo que se anuncia al confirmar la foto, según cómo quedó (D-46). */
const ANUNCIOS = { CARGADA: 'fotos.cargada', APROBADA: 'fotos.aprobada', POR_REPETIR: 'fotos.porRepetir' };

/**
 * @typedef {object} DeLaMuestra lo que la casilla necesita de su muestra para decir qué hacer con una foto por repetir
 * @property {number} repeticiones cuántas fotos pidió repetir RF-09 por granos dudosos en la muestra, de 0 a 2
 * @property {number|null} porcentajeMaxDudosos el máximo de dudosos que usó la muestra (P-15), si ya lo evaluó
 */

/**
 * Qué pasó con una foto por repetir y qué hacer (RNF-06 C3): con granos dudosos, el máximo y la repetición que va, y
 * en la segunda, que el resultado saldrá con el aviso de confianza baja (RF-09 C1 y C4).
 *
 * @param {import('../api/fotos.js').Foto} foto la foto por repetir
 * @param {DeLaMuestra} muestra las repeticiones y el máximo de dudosos de su muestra
 * @param {(clave: string, opciones?: object) => string} t la función de traducción de i18next
 * @param {string} region la región del idioma, como es-CO
 * @returns {string} el mensaje
 */
function queHacer(foto, muestra, t, region) {
  if (foto.motivo !== 'GRANOS_DUDOSOS') {
    return t(`fotos.queHacer.${foto.motivo}`);
  }
  const porcentaje = muestra.porcentajeMaxDudosos === null || muestra.porcentajeMaxDudosos === undefined
    ? '' : textoDelPorcentaje(muestra.porcentajeMaxDudosos, region);
  const ultima = muestra.repeticiones >= MAXIMO_DE_REPETICIONES;
  return t(ultima ? 'fotos.queHacer.GRANOS_DUDOSOS_ultima' : 'fotos.queHacer.GRANOS_DUDOSOS',
    { porcentaje, repeticion: Math.max(muestra.repeticiones, 1) });
}

/**
 * Lo que dicen las marcas de una foto por repetir: cuántas zonas de granos pegados y cuántos granos dudosos.
 *
 * @param {import('../api/fotos.js').Foto} foto la foto
 * @param {(clave: string, opciones?: object) => string} t la función de traducción de i18next
 * @returns {string|null} el texto, o null si la foto no tiene marcas
 */
function textoDeLasMarcas(foto, t) {
  const textos = [];
  if (foto.regionesPegadas?.length > 0) {
    textos.push(t('fotos.regionesPegadas', { count: foto.regionesPegadas.length }));
  }
  if (foto.granosDudosos?.length > 0) {
    textos.push(t('fotos.granosDudosos', { count: foto.granosDudosos.length }));
  }
  return textos.length > 0 ? textos.join(' ') : null;
}

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
 * no existe, ya no está pendiente o cambió la muestra que recibe las fotos (409), o si el backend rechazó la foto.
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
 * @param {number} props.muestra el número de la muestra que la página espera que reciba la foto, 1 o 2 (D-25)
 * @param {number} props.numero el número de la foto, de 1 a 3; la 3 es opcional
 * @param {import('../api/fotos.js').Foto} [props.foto] la foto ya subida, si la hay
 * @param {DeLaMuestra} [props.deLaMuestra] las repeticiones y el máximo de dudosos de la muestra (RF-09)
 * @param {(foto: import('../api/fotos.js').Foto) => void} props.alConfirmar se llama con la foto que se confirmó
 * @param {() => void} props.alFallarLaVista se llama si la imagen no carga, como cuando su enlace venció
 * @param {(numero: number, enCurso: boolean) => void} [props.alCambiarLaSubida] se llama cuando su subida empieza y
 *   cuando termina, bien o mal
 * @returns {import('react').ReactElement} la casilla
 */
export function CasillaDeFoto({
  loteId, muestra, numero, foto, deLaMuestra = SIN_REPETICIONES, alConfirmar, alFallarLaVista, alCambiarLaSubida,
}) {
  const { t, i18n } = useTranslation();
  const { pedir } = useSesion();
  const esPc = useEsPc();
  const entrada = useRef(null);
  const botonDeLaFoto = useRef(null);
  // Si hay una subida en curso: el estado tarda un render en cambiar, y dos elecciones seguidas no deben subir dos.
  const enCurso = useRef(false);
  const idDelTitulo = useId();
  const idDeLasMarcas = useId();
  const [subida, setSubida] = useState(QUIETA);
  const [anuncio, setAnuncio] = useState('');
  const vacia = !foto;
  const subiendo = subida.fase === 'subiendo';
  const fallida = subida.fase === 'fallida';
  const porRepetir = foto?.estado === 'POR_REPETIR';
  const marcas = porRepetir ? textoDeLasMarcas(foto, t) : null;
  const chip = CHIPS[foto?.estado] ?? CHIPS.CARGADA;
  const conteo = foto?.estado === 'APROBADA' && Number.isInteger(foto.granosEncontrados) ? foto.granosEncontrados : null;

  useEffect(() => {
    alCambiarLaSubida?.(numero, subiendo);
  }, [alCambiarLaSubida, numero, subiendo]);

  /**
   * Revisa la foto, pide el enlace, la sube y la confirma. La casilla queda ocupada desde que empieza, también
   * mientras lee los primeros bytes, que con un archivo en la nube puede tardar. Si algo falla, guarda el archivo para
   * reintentar y, si ya llegó al almacén, su llave: reintentar entonces solo lo confirma, sin subirlo de nuevo.
   *
   * @param {File} archivo la foto elegida
   * @param {string|null} [llaveSubida] la llave del archivo, si ya llegó y solo falta confirmarlo
   */
  async function subir(archivo, llaveSubida = null) {
    if (enCurso.current) {
      return;
    }
    enCurso.current = true;
    setSubida({ fase: 'subiendo' });
    setAnuncio(t('fotos.subiendo', { numero }));
    let llave = llaveSubida;
    try {
      if (!llave) {
        const revision = await revisarArchivo(archivo);
        if (revision.error) {
          setSubida({ fase: 'fallida', mensaje: t(revision.error), reintentable: false });
          setAnuncio('');
          return;
        }
        const enlace = await pedirSubida(pedir, loteId, numero,
          { tipo: revision.tipo, tamanoBytes: archivo.size, muestra });
        await subirArchivo(enlace, archivo, i18n.language);
        llave = enlace.llave;
      }
      const confirmada = await confirmarFoto(pedir, loteId, numero, llave);
      alConfirmar(confirmada);
      setSubida(QUIETA);
      setAnuncio(t(ANUNCIOS[confirmada.estado] ?? ANUNCIOS.CARGADA, { numero }));
    } catch (error) {
      // Si el backend dice que el archivo no está, reintentar lo sube de nuevo.
      const sinElArchivo = (error.errores ?? []).some(({ campo }) => campo === 'llave');
      setSubida({
        fase: 'fallida', mensaje: mensajeDelError(error, t), reintentable: sePuedeReintentar(error), archivo,
        llave: sinElArchivo ? null : llave,
      });
      setAnuncio('');
    } finally {
      enCurso.current = false;
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
    if (elegida) {
      subir(elegida);
    }
  }

  /** Abre el selector de archivos, o la cámara en el celular, dentro del mismo clic: iOS no lo abre después. */
  function abrirSelector() {
    if (!enCurso.current) {
      entrada.current?.click();
    }
  }

  /**
   * Reintenta la subida. Antes lleva el foco al botón de la foto, que sigue en la casilla: «Reintentar» desaparece
   * mientras sube, y el foco se perdería con él (RNF-06 C4).
   */
  function reintentar() {
    botonDeLaFoto.current?.focus();
    subir(subida.archivo, subida.llave);
  }

  let boton = { texto: t('fotos.reemplazar'), variante: 'outlined', icono: undefined, alto: medidas.zonaTactil };
  if (porRepetir) {
    // Como en el prototipo: repetir la foto es lo que sigue, así que el botón va relleno.
    boton = { texto: t('fotos.repetir'), variante: 'contained', icono: undefined, alto: medidas.zonaTactil };
  } else if (vacia) {
    boton = esPc
      ? { texto: t('fotos.elegirArchivo'), variante: 'outlined', icono: undefined, alto: medidas.zonaTactil }
      : { texto: t('fotos.tomarFoto'), variante: 'contained', icono: <PhotoCamera />, alto: medidas.boton.alto };
  }

  return (
    <Box role="group" aria-labelledby={idDelTitulo}
      sx={estiloDeLaCasilla({ vacia, esPc, conError: fallida || porRepetir })}>
      {vacia && esPc && <FileUpload aria-hidden sx={{ fontSize: 36, color: 'text.secondary' }} />}
      {/* Con marcas, la foto va entera: recortada, las marcas no caerían sobre sus granos. */}
      {!vacia && marcas && (
        <FotoRevisada foto={foto} alt={t('fotos.imagen', { numero })} idDeLaDescripcion={idDeLasMarcas}
          alFallarLaVista={alFallarLaVista} esPc={esPc} />
      )}
      {!vacia && !marcas && (
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
        {!vacia && <ChipDeEstado texto={t(`foto.estado.${foto.estado}`)} tono={chip.tono} icono={<chip.Icono />} />}
      </Box>
      {conteo !== null && (
        <Typography variant="body2" color="text.secondary">{t('fotos.granosEncontrados', { count: conteo })}</Typography>
      )}
      {porRepetir && (
        <Typography role="alert" variant="body2" sx={(theme) => ({
          display: 'flex', gap: 0.75, alignItems: 'flex-start', lineHeight: 1.45, overflowWrap: 'anywhere',
          color: theme.vars.palette.cacao.error.texto,
        })}>
          <ErrorOutline aria-hidden sx={{ fontSize: 18, mt: '1px', flexShrink: 0 }} />
          {queHacer(foto, deLaMuestra, t, idiomaDe(i18n.language).region)}
        </Typography>
      )}
      {marcas && (
        <Typography id={idDeLasMarcas} variant="body2" color="text.secondary" sx={{ lineHeight: 1.45 }}>
          {marcas}
        </Typography>
      )}
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
            <Button variant="outlined" onClick={reintentar} sx={{ minHeight: medidas.zonaTactil }}>
              {t('crud.reintentar')}
            </Button>
          )}
        </Box>
      )}
      {/* El mismo botón antes y después de subir la foto: así el foco sigue en él (RNF-06 C4). Mientras sube, no
          se deshabilita, porque perdería el foco: queda marcado con aria-disabled y no abre el selector. */}
      <Button ref={botonDeLaFoto} variant={boton.variante} startIcon={boton.icono} onClick={abrirSelector}
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
