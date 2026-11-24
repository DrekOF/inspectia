/*
 * Nombre: DescargarReporte.jsx
 * Descripción: El botón «Descargar PDF» del resultado del lote, como en el prototipo (RF-15): baja el reporte con la
 *              sesión, en el idioma de la página, y lo guarda como archivo. Mientras baja, el botón lo dice y no se
 *              puede pulsar otra vez; al terminar, lo anuncia a los lectores de pantalla, y si falla, muestra el
 *              motivo. La descarga misma (bajarReporte) la usa también el historial (RF-15).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import FileDownloadOutlined from '@mui/icons-material/FileDownloadOutlined';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import { useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { reporteDelLote } from '../api/analisis.js';
import { textoDelError } from '../api/errores.js';
import { Aviso } from '../componentes/Aviso.jsx';
import { OCULTO_A_LA_VISTA } from '../componentes/ocultoALaVista.js';
import { idiomaDe } from '../i18n/i18n.js';
import { useSesion } from '../sesion/contexto.js';
import { medidas } from '../tema/tokens.js';

/**
 * Guarda un archivo en el equipo: un enlace temporal al contenido, con su nombre, que se pulsa y se quita. El enlace
 * del contenido se suelta después, cuando el navegador ya empezó a guardarlo.
 *
 * @param {Blob} archivo el contenido
 * @param {string} nombre el nombre del archivo
 */
export function guardarArchivo(archivo, nombre) {
  const url = URL.createObjectURL(archivo);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombre;
  enlace.style.display = 'none';
  document.body.append(enlace);
  enlace.click();
  enlace.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

/**
 * Baja el reporte PDF de un lote con la sesión, en un idioma, y lo guarda en el equipo con el nombre que manda el
 * servidor, o con InspectIA-<código>-<idioma>.pdf si no lo manda (RF-15). Lo usan el resultado y el historial.
 *
 * @param {import('../api/cliente.js').Pedir} pedir la función del cliente de la API
 * @param {string} loteId el lote
 * @param {string} codigo su código, para el nombre del archivo si el servidor no lo manda
 * @param {'es'|'en'} idioma el idioma del reporte, el de la página
 * @returns {Promise<void>} se cumple cuando el archivo ya se entregó al navegador
 */
export async function bajarReporte(pedir, loteId, codigo, idioma) {
  const { archivo, nombre } = await reporteDelLote(pedir, loteId, idioma);
  guardarArchivo(archivo, nombre ?? `InspectIA-${codigo}-${idioma}.pdf`);
}

/**
 * «Descargar PDF» del resultado de un lote analizado (RF-15), para el usuario final y la administradora.
 *
 * @param {object} props las propiedades
 * @param {string} props.loteId el lote
 * @param {string} props.codigo su código, para el nombre del archivo si el servidor no lo manda
 * @param {boolean} [props.aLoAncho] si va a todo el ancho, como en el celular
 * @returns {import('react').ReactElement} el botón, su anuncio y su error
 */
export function DescargarReporte({ loteId, codigo, aLoAncho = false }) {
  const { t, i18n } = useTranslation();
  const { pedir } = useSesion();
  const [estado, setEstado] = useState({ descargando: false, listo: false, error: null });
  // Una descarga a la vez: el botón sigue enfocable mientras baja, pero no pide otra.
  const enCurso = useRef(false);
  const idioma = idiomaDe(i18n.language).codigo;

  async function descargar() {
    if (enCurso.current) {
      return;
    }
    enCurso.current = true;
    setEstado({ descargando: true, listo: false, error: null });
    try {
      await bajarReporte(pedir, loteId, codigo, idioma);
      setEstado({ descargando: false, listo: true, error: null });
    } catch (error) {
      setEstado({ descargando: false, listo: false, error });
    } finally {
      enCurso.current = false;
    }
  }

  let anuncio = '';
  if (estado.descargando) {
    anuncio = t('reporte.descargando');
  } else if (estado.listo) {
    anuncio = t('reporte.descargado', { codigo });
  }

  return (
    <Box sx={{
      display: 'flex', flexDirection: 'column', gap: 1, alignItems: aLoAncho ? 'stretch' : 'flex-end',
      maxWidth: aLoAncho ? 'none' : 420,
    }}>
      <Button variant="outlined" onClick={descargar} aria-disabled={estado.descargando || undefined}
        aria-busy={estado.descargando || undefined}
        startIcon={estado.descargando ? <CircularProgress size={18} color="inherit" aria-hidden />
          : <FileDownloadOutlined aria-hidden />}
        sx={{ minHeight: medidas.boton.alto, alignSelf: aLoAncho ? 'stretch' : 'auto' }}>
        {estado.descargando ? t('reporte.descargando') : t('reporte.descargar')}
      </Button>
      <Box role="status" sx={OCULTO_A_LA_VISTA}>{anuncio}</Box>
      {estado.error && (
        <Aviso tipo="error" titulo={t('reporte.error')}>{textoDelError(estado.error, t)}</Aviso>
      )}
    </Box>
  );
}
