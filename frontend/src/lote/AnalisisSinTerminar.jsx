/*
 * Nombre: AnalisisSinTerminar.jsx
 * Descripción: El aviso de un análisis que no terminó: la muestra volvió de la cola sin resultado y quedó pendiente
 *              otra vez (RNF-06 C3). Dice por qué, según lo que dejó el servicio de IA (D-44): la foto que hay que
 *              repetir y su motivo (RF-06, RF-07 C2), con los granos dudosos y la repetición que va (RF-09 C1), varias
 *              fotos, menos de 100 granos (RF-07 C3) o que el servicio no respondió (RF-08 C6); sin motivo, el mensaje
 *              general. Al usuario final le dice qué hacer y lo lleva a las fotos; a la administradora, quién puede
 *              enviarla de nuevo. Desde la segunda muestra, el título dice su número (D-25).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { Aviso } from '../componentes/Aviso.jsx';
import { idiomaDe } from '../i18n/i18n.js';
import { listaDeNumeros } from './revision.js';

/**
 * El título y qué hacer, según por qué volvió la muestra.
 *
 * @param {import('./revision.js').Devolucion} devolucion por qué volvió la muestra
 * @param {number} numeroDeMuestra el número de la muestra, 1 o 2
 * @param {(clave: string, opciones?: object) => string} t la función de traducción de i18next
 * @param {string} region la región del idioma, como es-CO
 * @returns {{ titulo: string, queHacer: string }} los textos
 */
function textosDeLaDevolucion(devolucion, numeroDeMuestra, t, region) {
  const { fotosPorRepetir, motivoDevolucion, repeticiones } = devolucion;
  const deLaMuestra = numeroDeMuestra > 1 ? 'DeLaMuestra' : '';
  if (fotosPorRepetir.length === 1) {
    const [{ numero, motivo }] = fotosPorRepetir;
    // El motivo va dentro de la frase, en minúscula: «…de la muestra: foto borrosa.».
    const nombre = t(`foto.motivo.${motivo}`).toLocaleLowerCase(region);
    return {
      titulo: t(`espera.porRepetir${deLaMuestra}`, { numero, muestra: numeroDeMuestra, motivo: nombre }),
      queHacer: t(`espera.queHacer.${motivo}`, { repeticion: Math.max(repeticiones, 1) }),
    };
  }
  if (fotosPorRepetir.length > 1) {
    const numeros = listaDeNumeros(fotosPorRepetir.map((foto) => foto.numero).sort((uno, otro) => uno - otro), region);
    return {
      titulo: t(`espera.porRepetirVarias${deLaMuestra}`, { numeros, muestra: numeroDeMuestra }),
      queHacer: t('espera.falloQueHacer'),
    };
  }
  if (motivoDevolucion) {
    return {
      titulo: t(`espera.devuelta.${motivoDevolucion}${deLaMuestra}`, { muestra: numeroDeMuestra }),
      queHacer: t(`espera.devueltaQueHacer.${motivoDevolucion}`),
    };
  }
  return {
    titulo: numeroDeMuestra > 1 ? t('espera.falloDeLaMuestra', { numero: numeroDeMuestra }) : t('espera.fallo'),
    queHacer: t('espera.falloQueHacer'),
  };
}

/**
 * Un análisis que no terminó.
 *
 * @param {object} props las propiedades
 * @param {import('./revision.js').Devolucion} props.devolucion por qué volvió la muestra
 * @param {number} props.muestra el número de la muestra que volvió, 1 o 2
 * @param {string} props.loteId el lote
 * @param {boolean} props.esUsuarioFinal si la sesión es del usuario final, el único que sube fotos
 * @returns {import('react').ReactElement} el aviso
 */
export function AnalisisSinTerminar({ devolucion, muestra, loteId, esUsuarioFinal }) {
  const { t, i18n } = useTranslation();
  const { titulo, queHacer } = textosDeLaDevolucion(devolucion, muestra, t, idiomaDe(i18n.language).region);
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 1.5 }}>
      <Aviso tipo="error" titulo={titulo}>
        {esUsuarioFinal ? queHacer : t('espera.falloAdmin')}
      </Aviso>
      {esUsuarioFinal && (
        <Button component={Link} to={`/lotes/${loteId}/fotos`} variant="contained">
          {t('espera.volverALasFotos')}
        </Button>
      )}
    </Box>
  );
}
