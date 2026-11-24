/*
 * Nombre: ComparacionDeModelos.jsx
 * Descripción: La tabla comparativa de los modelos de IA (RF-17 C1, D-18, D-48): cada modelo con su tipo y sus métricas
 *              del conjunto de prueba, y el modelo en uso marcado con el chip «En uso» y el fondo de la selección. En el
 *              PC va en una tabla que se desplaza a los lados si no cabe; en el celular, en una tarjeta por modelo
 *              (RNF-05).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Check from '@mui/icons-material/Check';
import Box from '@mui/material/Box';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import { useId } from 'react';
import { useTranslation } from 'react-i18next';
import { ChipDeEstado } from '../componentes/ChipDeEstado.jsx';
import { OCULTO_A_LA_VISTA } from '../componentes/ocultoALaVista.js';
import { medidas } from '../tema/tokens.js';
import { METRICAS, SIN_DATO, textoDeMetrica } from './metricas.js';

/**
 * El estilo de un recuadro, como el de la tabla o el de una tarjeta.
 *
 * @param {import('@mui/material/styles').Theme} theme el tema
 * @returns {object} el estilo
 */
const recuadro = (theme) => ({
  borderRadius: `${medidas.tarjeta.radio}px`,
  border: `1px solid ${theme.vars.palette.divider}`,
  bgcolor: 'background.paper',
});

/**
 * El tipo de un modelo, como «Red convolucional (CNN)», o «—» si no lo tiene.
 *
 * @param {import('../api/modelo.js').ModeloMedido} modelo el modelo
 * @param {(clave: string) => string} t la función de traducción de i18next
 * @returns {string} el texto
 */
function tipoDe(modelo, t) {
  return modelo.tipo ? t(`modelo.tipo.${modelo.tipo}`) : SIN_DATO;
}

/**
 * El chip «En uso», en el tono de éxito y con un chulo: el ícono acompaña al color (RNF-06).
 *
 * @returns {import('react').ReactElement} el chip
 */
function ChipEnUso() {
  const { t } = useTranslation();
  return <ChipDeEstado texto={t('modelo.en_uso.etiqueta')} tono="exito" icono={<Check />} />;
}

/**
 * El nombre de un modelo, su versión debajo y, si está en uso, el chip «En uso».
 *
 * @param {{ modelo: import('../api/modelo.js').ModeloMedido }} props el modelo
 * @returns {import('react').ReactElement} el nombre
 */
function NombreDelModelo({ modelo }) {
  const { t } = useTranslation();
  return (
    <Box sx={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '4px 10px' }}>
      <Box>
        <Box component="span" sx={{ display: 'block', fontWeight: 500 }}>{modelo.nombre}</Box>
        <Box component="span" sx={{ display: 'block', color: 'text.secondary', fontSize: 14 }}>
          {t('modeloIa.version', { version: modelo.version })}
        </Box>
      </Box>
      {modelo.enUso && <ChipEnUso />}
    </Box>
  );
}

/**
 * Los modelos en el PC: una tabla con su título para los lectores de pantalla, el modelo como encabezado de cada fila y
 * una columna por métrica. En una ventana angosta, la caja de la tabla se desplaza a los lados; por eso recibe el foco,
 * con el nombre de la tabla, y se recorre con las flechas del teclado (WCAG 2.1.1).
 *
 * @param {{ modelos: import('../api/modelo.js').ModeloMedido[], region: string }} props los modelos y la región
 * @returns {import('react').ReactElement} la tabla
 */
function Tabla({ modelos, region }) {
  const { t } = useTranslation();
  const idDelTitulo = useId();
  return (
    <Box role="region" tabIndex={0} aria-labelledby={idDelTitulo}
      sx={(theme) => ({ ...recuadro(theme), overflowX: 'auto' })}>
      <Table>
        <Box component="caption" id={idDelTitulo} sx={OCULTO_A_LA_VISTA}>{t('modeloIa.tabla')}</Box>
        <TableHead>
          <TableRow>
            <TableCell>{t('modeloIa.columnaModelo')}</TableCell>
            <TableCell>{t('modelo.tipo.etiqueta')}</TableCell>
            {METRICAS.map(([dato, clave]) => <TableCell key={dato} align="right">{t(clave)}</TableCell>)}
          </TableRow>
        </TableHead>
        <TableBody>
          {modelos.map((modelo) => (
            <TableRow key={modelo.id} sx={modelo.enUso
              ? (theme) => ({ bgcolor: theme.vars.palette.cacao.seleccion.fondo })
              : undefined}>
              <TableCell component="th" scope="row" sx={{ minWidth: 180 }}>
                <NombreDelModelo modelo={modelo} />
              </TableCell>
              <TableCell>{tipoDe(modelo, t)}</TableCell>
              {METRICAS.map(([dato]) => (
                <TableCell key={dato} align="right">{textoDeMetrica(modelo[dato], region)}</TableCell>
              ))}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Box>
  );
}

/**
 * Los modelos en el celular: una tarjeta por modelo, con su nombre, su versión y el chip «En uso» arriba, y cada
 * métrica con su nombre (RNF-05).
 *
 * @param {{ modelos: import('../api/modelo.js').ModeloMedido[], region: string }} props los modelos y la región
 * @returns {import('react').ReactElement} las tarjetas
 */
function Tarjetas({ modelos, region }) {
  const { t } = useTranslation();
  return (
    <Box component="ul" role="list" aria-label={t('modeloIa.tabla')}
      sx={{ listStyle: 'none', m: 0, p: 0, display: 'flex', flexDirection: 'column', gap: 1.5 }}>
      {modelos.map((modelo) => {
        const datos = [
          ['tipo', t('modelo.tipo.etiqueta'), tipoDe(modelo, t)],
          ...METRICAS.map(([dato, clave]) => [dato, t(clave), textoDeMetrica(modelo[dato], region)]),
        ];
        return (
          <Box component="li" key={modelo.id} sx={(theme) => ({
            ...recuadro(theme), p: 2, display: 'flex', flexDirection: 'column', gap: 1,
            ...(modelo.enUso ? { bgcolor: theme.vars.palette.cacao.seleccion.fondo } : {}),
          })}>
            <NombreDelModelo modelo={modelo} />
            <Box component="dl" sx={{ m: 0, display: 'flex', flexDirection: 'column', gap: 0.5 }}>
              {datos.map(([dato, nombre, valor]) => (
                <Box key={dato} sx={{ display: 'flex', justifyContent: 'space-between', gap: 2 }}>
                  <Typography component="dt" variant="body2" color="text.secondary">{nombre}</Typography>
                  <Typography component="dd" variant="body2" sx={{ m: 0, textAlign: 'right' }}>{valor}</Typography>
                </Box>
              ))}
            </Box>
          </Box>
        );
      })}
    </Box>
  );
}

/**
 * La comparación de los modelos: tabla en el PC y tarjetas en el celular.
 *
 * @param {{ modelos: import('../api/modelo.js').ModeloMedido[], region: string, esPc: boolean }} props los modelos,
 *   del mayor F1 macro al menor, la región del idioma y si se ve en el PC
 * @returns {import('react').ReactElement} la comparación
 */
export function ComparacionDeModelos({ modelos, region, esPc }) {
  return esPc ? <Tabla modelos={modelos} region={region} /> : <Tarjetas modelos={modelos} region={region} />;
}
