/*
 * Nombre: GraficaPorMes.jsx
 * Descripción: La gráfica de la evolución del promedio de granos sanos por mes (RF-16), en SVG y sin librerías: una
 *              columna por mes en el PC y una barra por mes en el celular, en la escala de 0 a 100 %, con el valor
 *              escrito en cada una y los colores del tema. Es un dibujo: su tabla, junto a ella, dice lo mismo a los
 *              lectores de pantalla (RNF-06).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Box from '@mui/material/Box';
import { textoDelDecimal } from '../crud/decimales.js';

/** El tamaño de la letra del dibujo, en píxeles: el de las ayudas del prototipo menos uno, para que quepan 12 meses. */
const LETRA = 12;

/** El radio de la punta de cada columna o barra, como las del resto de la app. */
const RADIO = 4;

/**
 * Un mes (aaaa-mm) en corto, como «sept 26» o «Sep 26».
 *
 * @param {string} mes el mes
 * @param {string} region la región del idioma, como es-CO
 * @returns {string} el mes
 */
export function mesCorto(mes, region) {
  const [anio, numero] = mes.split('-').map(Number);
  return new Intl.DateTimeFormat(region, { month: 'short', year: '2-digit', timeZone: 'UTC' })
    .format(new Date(Date.UTC(anio, numero - 1, 1)));
}

/**
 * El camino de un rectángulo con las dos esquinas de la punta redondeadas: arriba en una columna y a la derecha en
 * una barra. La base queda recta, sobre el eje.
 *
 * @param {number} x la izquierda
 * @param {number} y arriba
 * @param {number} ancho el ancho
 * @param {number} alto el alto
 * @param {boolean} horizontal si es una barra que crece a la derecha
 * @returns {string} el camino del SVG
 */
function rectanguloConPunta(x, y, ancho, alto, horizontal) {
  const r = Math.min(RADIO, ancho / 2, alto / 2);
  if (horizontal) {
    return `M${x},${y}H${x + ancho - r}A${r},${r} 0 0 1 ${x + ancho},${y + r}V${y + alto - r}`
      + `A${r},${r} 0 0 1 ${x + ancho - r},${y + alto}H${x}Z`;
  }
  return `M${x},${y + alto}V${y + r}A${r},${r} 0 0 1 ${x + r},${y}H${x + ancho - r}`
    + `A${r},${r} 0 0 1 ${x + ancho},${y + r}V${y + alto}Z`;
}

/**
 * Las columnas, una por mes, sobre las líneas de 0, 50 y 100 %.
 *
 * @param {{ meses: import('../api/estadisticas.js').Estadisticas['porMes'], region: string }} props los meses y la
 *   región del idioma
 * @returns {import('react').ReactElement} el dibujo
 */
function Columnas({ meses, region }) {
  const izquierda = 44;
  const paso = 56;
  const arriba = 24;
  const altoUtil = 160;
  const ancho = izquierda + meses.length * paso + 8;
  const alto = arriba + altoUtil + 28;
  const y = (porcentaje) => arriba + altoUtil * (1 - porcentaje / 100);
  return (
    <svg viewBox={`0 0 ${ancho} ${alto}`} width={ancho} height={alto} style={{ maxWidth: '100%', height: 'auto' }}>
      {[0, 50, 100].map((linea) => (
        <g key={linea}>
          <line x1={izquierda} x2={ancho} y1={y(linea)} y2={y(linea)} className="linea" />
          <text x={izquierda - 6} y={y(linea) + 4} textAnchor="end" className="eje">{`${linea} %`}</text>
        </g>
      ))}
      {meses.map(({ mes, promedioSanos }, indice) => {
        const x = izquierda + indice * paso + 10;
        return (
          <g key={mes}>
            {promedioSanos !== null && (
              <>
                <path data-columna={mes} className="marca"
                  d={rectanguloConPunta(x, y(promedioSanos), paso - 20, altoUtil * (promedioSanos / 100), false)} />
                <text x={x + (paso - 20) / 2} y={y(promedioSanos) - 6} textAnchor="middle" className="valor">
                  {textoDelDecimal(promedioSanos, 1, region)}
                </text>
              </>
            )}
            <text x={x + (paso - 20) / 2} y={alto - 8} textAnchor="middle" className="eje">{mesCorto(mes, region)}</text>
          </g>
        );
      })}
    </svg>
  );
}

/**
 * Las barras, una por mes, de 0 a 100 %: en el celular caben mejor que las columnas, con el mes a la izquierda.
 *
 * @param {{ meses: import('../api/estadisticas.js').Estadisticas['porMes'], region: string }} props los meses y la
 *   región del idioma
 * @returns {import('react').ReactElement} el dibujo
 */
function Barras({ meses, region }) {
  // 284 de ancho: cabe sin achicarse en un celular de 360 px, con los márgenes y el relleno de la tarjeta.
  const izquierda = 60;
  const anchoUtil = 180;
  const fila = 30;
  const ancho = izquierda + anchoUtil + 44;
  const alto = meses.length * fila + 4;
  return (
    <svg viewBox={`0 0 ${ancho} ${alto}`} width={ancho} height={alto} style={{ maxWidth: '100%', height: 'auto' }}>
      <line x1={izquierda} x2={izquierda} y1={0} y2={alto} className="linea" />
      {meses.map(({ mes, promedioSanos }, indice) => {
        const y = indice * fila + 4;
        const largo = promedioSanos === null ? 0 : anchoUtil * (promedioSanos / 100);
        return (
          <g key={mes}>
            <text x={izquierda - 8} y={y + 15} textAnchor="end" className="eje">{mesCorto(mes, region)}</text>
            {promedioSanos !== null && (
              <>
                <path data-columna={mes} className="marca" d={rectanguloConPunta(izquierda, y + 4, largo, 14, true)} />
                <text x={izquierda + largo + 6} y={y + 15} className="valor">{textoDelDecimal(promedioSanos, 1, region)}</text>
              </>
            )}
          </g>
        );
      })}
    </svg>
  );
}

/**
 * La gráfica por mes. Los meses sin lotes quedan sin columna: no se dibuja un promedio que no existe (RF-16).
 *
 * @param {object} props las propiedades
 * @param {import('../api/estadisticas.js').Estadisticas['porMes']} props.meses los meses, en orden
 * @param {string} props.region la región del idioma, como es-CO
 * @param {boolean} props.horizontal si va en barras, como en el celular
 * @returns {import('react').ReactElement} la gráfica, oculta a los lectores de pantalla
 */
export function GraficaPorMes({ meses, region, horizontal }) {
  const Dibujo = horizontal ? Barras : Columnas;
  return (
    <Box aria-hidden="true" data-testid="grafica-por-mes" sx={(theme) => ({
      display: 'flex', justifyContent: horizontal ? 'flex-start' : 'center',
      '& svg': { display: 'block', fontFamily: 'inherit', fontSize: LETRA },
      '& .marca': { fill: theme.vars.palette.primary.main },
      '& .linea': { stroke: theme.vars.palette.divider, strokeWidth: 1 },
      '& .eje': { fill: theme.vars.palette.text.secondary },
      '& .valor': { fill: theme.vars.palette.text.primary, fontWeight: 500 },
    })}>
      <Dibujo meses={meses} region={region} />
    </Box>
  );
}
