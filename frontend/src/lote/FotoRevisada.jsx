/*
 * Nombre: FotoRevisada.jsx
 * Descripción: La foto de una casilla que la revisión del servicio de IA marcó «Por repetir» con algo que mostrar
 *              sobre ella (D-46): las zonas de granos pegados, con un recuadro punteado rojo (RF-07 C2, D-45), o los
 *              granos dudosos que pidió repetir RF-09, con un recuadro ámbar y un «?», sin su clase, porque todavía no
 *              hay resultado (RF-09 C1, RF-11 C4). La foto va entera, sin recortar, para que las marcas caigan en su
 *              lugar; las marcas van en porcentaje del tamaño original de la foto, como en FotoMarcada.jsx, así siguen
 *              al grano en el PC y en el celular (RNF-05). Son decorativas: el texto de la casilla las dice, y la foto
 *              lo toma como su descripción (RNF-06).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Box from '@mui/material/Box';
import { useState } from 'react';
import { marcasDeFoto } from '../tema/tokens.js';
import { posicionEnPorcentaje } from './granos.js';

/** @typedef {{ x: number, y: number, ancho: number, alto: number }} Rectangulo un rectángulo en píxeles de la foto */

/**
 * El estilo de una marca: un recuadro en su lugar sobre la foto.
 *
 * @param {{ left: number, top: number, width: number, height: number }} posicion dónde va, en porcentaje de la foto
 * @param {string} color el color del borde
 * @param {'dashed'|'solid'} trazo el trazo del borde: punteado para las zonas de granos pegados
 * @returns {object} el estilo
 */
function estiloDeLaMarca(posicion, color, trazo) {
  return {
    position: 'absolute', left: `${posicion.left}%`, top: `${posicion.top}%`, width: `${posicion.width}%`,
    height: `${posicion.height}%`, boxSizing: 'border-box', borderWidth: '2px', borderStyle: trazo, borderColor: color,
    borderRadius: '4px',
  };
}

/**
 * La foto entera con las marcas de su revisión.
 *
 * @param {object} props las propiedades
 * @param {import('../api/fotos.js').Foto} props.foto la foto, con su enlace de vista, sus zonas de granos pegados y
 *   sus granos dudosos
 * @param {string} props.alt el nombre de la foto, como «Foto 2 de la muestra»
 * @param {string} props.idDeLaDescripcion el id del texto que dice qué marcan las marcas
 * @param {() => void} props.alFallarLaVista se llama si la imagen no carga, como cuando su enlace venció
 * @param {boolean} props.esPc si se ve en el PC
 * @returns {import('react').ReactElement} la foto
 */
export function FotoRevisada({ foto, alt, idDeLaDescripcion, alFallarLaVista, esPc }) {
  // El tamaño original de la foto, que se sabe cuando carga: las marcas van en porcentaje de él.
  const [tamano, setTamano] = useState(null);
  const regiones = foto.regionesPegadas ?? [];
  const dudosos = foto.granosDudosos ?? [];

  /**
   * Guarda el tamaño original de la foto cuando carga.
   *
   * @param {import('react').SyntheticEvent<HTMLImageElement>} evento la carga de la imagen
   */
  function alCargar(evento) {
    const { naturalWidth: ancho, naturalHeight: alto } = evento.currentTarget;
    if (ancho > 0 && alto > 0) {
      setTamano({ ancho, alto });
    }
  }

  const lado = esPc ? 18 : 14;
  return (
    <Box sx={{ position: 'relative', bgcolor: 'action.hover', borderRadius: esPc ? '8px' : '6px' }}>
      <Box component="img" src={foto.vista.url} alt={alt} aria-describedby={idDeLaDescripcion} onLoad={alCargar}
        onError={alFallarLaVista} decoding="async" sx={{
          display: 'block', width: '100%', height: 'auto', objectFit: 'contain', borderRadius: esPc ? '8px' : '6px',
        }} />
      {tamano && (
        <Box aria-hidden sx={{ position: 'absolute', inset: 0, pointerEvents: 'none' }}>
          {regiones.map((region, indice) => (
            // Las zonas no cambian de lugar en la lista: su posición es su identidad.
            <Box key={`pegados-${indice}`} data-marca="pegados"
              sx={estiloDeLaMarca(posicionEnPorcentaje(region, tamano), marcasDeFoto.noSano, 'dashed')} />
          ))}
          {dudosos.map((grano, indice) => (
            <Box key={`dudoso-${indice}`} data-marca="dudoso"
              sx={estiloDeLaMarca(posicionEnPorcentaje(grano, tamano), marcasDeFoto.dudoso, 'solid')}>
              <Box component="span" sx={{
                position: 'absolute', top: 0, right: 0, width: lado, height: lado, transform: 'translate(50%, -50%)',
                borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center',
                bgcolor: marcasDeFoto.dudoso, color: marcasDeFoto.simbolo, fontSize: lado - 5, fontWeight: 700,
                lineHeight: 1,
              }}>
                ?
              </Box>
            </Box>
          ))}
        </Box>
      )}
    </Box>
  );
}
