/*
 * Nombre: FotoMarcada.jsx
 * Descripción: Una foto de la muestra con sus granos marcados (RF-11): la foto entera, sin recortar, y encima una marca
 *              por grano, con el borde verde y un chulo si es sano, rojo y una equis si no lo es (C1 y C2), y además un
 *              signo de pregunta si es dudoso (D-42). Las marcas son botones de la página, ubicados en porcentaje del
 *              tamaño original de la foto, así siguen al grano en el PC y en el celular (C5). Al pasar el cursor, al
 *              tocar un grano o al llegarle con el teclado, un globo dice su clase, sin la confianza (C4), y Escape lo
 *              cierra. La foto es una sola parada de Tab: las flechas pasan de un grano a otro en orden de lectura, e
 *              Inicio y Fin van al primero y al último (RNF-06 C4). Si la imagen no carga, avisa una vez.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Check from '@mui/icons-material/Check';
import Close from '@mui/icons-material/Close';
import QuestionMark from '@mui/icons-material/QuestionMark';
import Box from '@mui/material/Box';
import ClickAwayListener from '@mui/material/ClickAwayListener';
import Tooltip from '@mui/material/Tooltip';
import { useId, useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useEsPc } from '../componentes/useEsPc.js';
import { letra, marcasDeFoto } from '../tema/tokens.js';
import { enOrdenDeLectura, indiceSiguiente, posicionEnPorcentaje } from './granos.js';
import { LeyendaDeMarcas } from './LeyendaDeMarcas.jsx';

/** El color y el símbolo de cada clase (RF-11 C1 y C2). */
const MARCAS = {
  SANO: { color: marcasDeFoto.sano, Icono: Check },
  NO_SANO: { color: marcasDeFoto.noSano, Icono: Close },
};

/** @typedef {import('@mui/material/styles').SxProps<import('@mui/material/styles').Theme>} Estilo */

/**
 * El estilo de una insignia de la marca: un círculo con el símbolo en blanco, en una esquina de arriba del grano.
 *
 * @param {string} color el color del círculo
 * @param {'izquierda'|'derecha'} esquina en qué esquina va: la clase a la izquierda y el «?» a la derecha
 * @param {boolean} esPc si se ve en el PC; en el celular, las insignias son más pequeñas para no tapar el grano
 * @returns {Estilo} el estilo
 */
function estiloDeLaInsignia(color, esquina, esPc) {
  const lado = esPc ? 18 : 14;
  return {
    position: 'absolute', top: 0, [esquina === 'izquierda' ? 'left' : 'right']: 0, width: lado, height: lado,
    transform: `translate(${esquina === 'izquierda' ? '-50%' : '50%'}, -50%)`, borderRadius: '50%',
    display: 'flex', alignItems: 'center', justifyContent: 'center', bgcolor: color, color: marcasDeFoto.simbolo,
    pointerEvents: 'none', '& svg': { fontSize: lado - 4 },
  };
}

/**
 * El estilo de la marca de un grano: un recuadro con el borde del color de su clase, en su lugar sobre la foto. El
 * foco del teclado es un anillo doble, claro y oscuro, que se ve sobre cualquier grano.
 *
 * @param {{ left: number, top: number, width: number, height: number }} posicion dónde va, en porcentaje de la foto
 * @param {string} color el color de su clase
 * @returns {Estilo} el estilo
 */
function estiloDeLaMarca(posicion, color) {
  return {
    position: 'absolute', left: `${posicion.left}%`, top: `${posicion.top}%`, width: `${posicion.width}%`,
    height: `${posicion.height}%`, boxSizing: 'border-box', m: 0, p: 0, border: `2px solid ${color}`,
    borderRadius: '4px', bgcolor: 'transparent', cursor: 'pointer',
    '&:focus-visible': {
      outline: `2px solid ${marcasDeFoto.foco.oscuro}`, outlineOffset: '2px',
      boxShadow: `0 0 0 2px ${marcasDeFoto.foco.claro}`,
    },
  };
}

/**
 * El estilo del globo con la clase de un grano, como en el prototipo: el color del texto de fondo y el de la
 * superficie en las letras, así se lee en los dos temas.
 *
 * @param {boolean} esPc si se ve en el PC
 * @returns {{ tooltip: { sx: Estilo }, arrow: { sx: Estilo } }} los estilos del globo y de su flecha
 */
function estiloDelGlobo(esPc) {
  return {
    tooltip: {
      sx: (theme) => ({
        bgcolor: theme.vars.palette.text.primary, color: theme.vars.palette.background.paper,
        fontSize: esPc ? letra.ayuda : letra.chip, fontWeight: letra.pesos.regular, lineHeight: 1.35,
        px: esPc ? 1.5 : 1.25, py: esPc ? 1 : 0.75, borderRadius: '8px',
      }),
    },
    arrow: { sx: (theme) => ({ color: theme.vars.palette.text.primary }) },
  };
}

/**
 * Una foto con sus granos marcados y su leyenda.
 *
 * @param {object} props las propiedades
 * @param {import('../api/analisis.js').FotoMarcada} props.foto la foto, con su enlace de vista y sus granos
 * @param {() => void} props.alFallarLaVista se llama si la imagen no carga, como cuando su enlace venció
 * @param {() => void} [props.alCargarLaVista] se llama cuando la imagen carga
 * @returns {import('react').ReactElement} la foto
 */
export function FotoMarcada({ foto, alFallarLaVista, alCargarLaVista }) {
  const { t } = useTranslation();
  const esPc = useEsPc();
  const idDeLaAyuda = useId();
  // El tamaño original de la foto, que se sabe cuando carga: las marcas van en porcentaje de él.
  const [tamano, setTamano] = useState(null);
  // El grano que recibe el foco con Tab (tabindex móvil) y el que muestra su globo.
  const [activo, setActivo] = useState(0);
  const [abierto, setAbierto] = useState(null);
  const botones = useRef([]);
  const granos = useMemo(() => enOrdenDeLectura(foto.granos), [foto.granos]);
  const conFoco = Math.min(activo, granos.length - 1);

  /**
   * Guarda el tamaño original de la foto cuando carga.
   *
   * @param {import('react').SyntheticEvent<HTMLImageElement>} evento la carga de la imagen
   */
  function alCargar(evento) {
    const { naturalWidth: ancho, naturalHeight: alto } = evento.currentTarget;
    if (ancho > 0 && alto > 0) {
      setTamano({ ancho, alto });
      alCargarLaVista?.();
    }
  }

  /**
   * Muestra la clase de un grano y lo deja como el de la parada de Tab.
   *
   * @param {number} indice el grano, en orden de lectura, desde 0
   */
  function mostrar(indice) {
    setActivo(indice);
    setAbierto(indice);
  }

  /**
   * Las teclas de la foto: las flechas, Inicio y Fin llevan el foco a otro grano, y Escape cierra el globo.
   *
   * @param {import('react').KeyboardEvent<HTMLButtonElement>} evento la tecla
   */
  function alPulsarTecla(evento) {
    if (evento.key === 'Escape') {
      setAbierto(null);
      return;
    }
    const siguiente = indiceSiguiente(conFoco, evento.key, granos.length);
    if (siguiente === null) {
      return;
    }
    evento.preventDefault();
    mostrar(siguiente);
    botones.current[siguiente]?.focus();
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: esPc ? 2 : 1.5 }}>
      <ClickAwayListener onClickAway={() => setAbierto(null)}>
        <Box sx={{ position: 'relative' }}>
          <Box component="img" src={foto.vista.url} alt={t('resultado.imagen', { numero: foto.numero })}
            onLoad={alCargar} onError={alFallarLaVista} decoding="async" sx={{
              display: 'block', width: '100%', height: 'auto', borderRadius: esPc ? '8px' : '6px', bgcolor: 'action.hover',
            }} />
          {tamano && granos.length > 0 && (
            <Box role="group" aria-label={t('resultado.granosDeLaFoto', { numero: foto.numero })}
              aria-describedby={idDeLaAyuda} sx={{ position: 'absolute', inset: 0 }}>
              {granos.map((grano, indice) => {
                const { color, Icono } = MARCAS[grano.clase];
                const clase = t(`grano.clase.${grano.clase}`);
                const nombre = t(`resultado.grano.${grano.clase}`, { numero: indice + 1 });
                return (
                  // Los granos no cambian de lugar en la lista: su número en el orden de lectura es su identidad.
                  <Tooltip key={indice} title={grano.dudoso ? t('resultado.claseDudosa', { clase }) : clase}
                    /* El cursor solo abre el globo: el grano de la parada de Tab lo eligen el foco, el toque y
                       las flechas, así un cursor que pasa no cambia desde dónde se mueven las flechas. */
                    open={abierto === indice} onOpen={() => setAbierto(indice)}
                    onClose={() => setAbierto((antes) => (antes === indice ? null : antes))}
                    disableTouchListener arrow placement="top" slotProps={estiloDelGlobo(esPc)}>
                    <Box component="button" type="button" ref={(boton) => { botones.current[indice] = boton; }}
                      tabIndex={indice === conFoco ? 0 : -1}
                      aria-label={grano.dudoso ? t('resultado.granoDudoso', { grano: nombre }) : nombre}
                      onFocus={() => mostrar(indice)} onClick={() => mostrar(indice)} onKeyDown={alPulsarTecla}
                      sx={estiloDeLaMarca(posicionEnPorcentaje(grano, tamano), color)}>
                      <Box component="span" aria-hidden sx={estiloDeLaInsignia(color, 'izquierda', esPc)}>
                        <Icono />
                      </Box>
                      {grano.dudoso && (
                        <Box component="span" aria-hidden sx={estiloDeLaInsignia(marcasDeFoto.dudoso, 'derecha', esPc)}>
                          <QuestionMark />
                        </Box>
                      )}
                    </Box>
                  </Tooltip>
                );
              })}
            </Box>
          )}
        </Box>
      </ClickAwayListener>
      <LeyendaDeMarcas idDeLaAyuda={idDeLaAyuda} />
    </Box>
  );
}
