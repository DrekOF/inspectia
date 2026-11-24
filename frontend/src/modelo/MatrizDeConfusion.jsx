/*
 * Nombre: MatrizDeConfusion.jsx
 * Descripción: La matriz de confusión del modelo en uso en una tabla accesible (RF-17, RNF-07 C2, D-48): la clase real
 *              en las filas y la que dio el modelo en las columnas, con los aciertos en verde y un chulo y los errores
 *              en rojo y una equis. El texto «Acierto» o «Error» acompaña al color (RNF-06).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import Check from '@mui/icons-material/Check';
import Close from '@mui/icons-material/Close';
import Box from '@mui/material/Box';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import { useId } from 'react';
import { useTranslation } from 'react-i18next';
import { OCULTO_A_LA_VISTA } from '../componentes/ocultoALaVista.js';
import { letra, medidas } from '../tema/tokens.js';

/**
 * Una celda de la matriz: cuántos granos y si son aciertos o errores del modelo, en los colores de los tokens.
 *
 * @param {{ granos: number, acierto: boolean }} props los granos y si la celda es de la diagonal (aciertos)
 * @returns {import('react').ReactElement} la celda
 */
function Celda({ granos, acierto }) {
  const { t } = useTranslation();
  const Icono = acierto ? Check : Close;
  return (
    <TableCell align="center" sx={(theme) => {
      const colores = theme.vars.palette.cacao[acierto ? 'exito' : 'error'];
      return { bgcolor: colores.fondo, color: colores.texto, borderBottom: 'none', py: 1.5, px: 1 };
    }}>
      <Box component="span" sx={{ display: 'block', fontSize: 22, fontWeight: letra.pesos.media, lineHeight: 1.3 }}>
        {granos}
      </Box>
      <Box component="span" sx={{
        display: 'inline-flex', alignItems: 'center', gap: 0.5, fontSize: letra.chip, fontWeight: letra.pesos.media,
        '& svg': { fontSize: 14 },
      }}>
        <Icono aria-hidden />
        {t(acierto ? 'modeloIa.acierto' : 'modeloIa.error')}
      </Box>
    </TableCell>
  );
}

/**
 * La matriz de confusión de un modelo, como tabla: su título lo nombra para los lectores de pantalla, y cada fila y
 * cada columna tienen su encabezado, así cada cifra se lee con su clase real y la que dio el modelo.
 *
 * @param {{ modelo: import('../api/modelo.js').ModeloMedido }} props el modelo, con su matriz
 * @returns {import('react').ReactElement} la tabla
 */
export function MatrizDeConfusion({ modelo }) {
  const { t } = useTranslation();
  const idDelTitulo = useId();
  const { matriz } = modelo;
  const filas = [
    ['modeloIa.sanos', [['comoSanos', matriz.sanosComoSanos, true], ['comoNoSanos', matriz.sanosComoNoSanos, false]]],
    ['modeloIa.noSanos', [['comoSanos', matriz.noSanosComoSanos, false],
      ['comoNoSanos', matriz.noSanosComoNoSanos, true]]],
  ];
  return (
    <Box sx={{ maxWidth: 560 }}>
      <Table aria-labelledby={idDelTitulo} sx={{
        tableLayout: 'fixed', borderCollapse: 'separate', borderSpacing: '6px',
        '& th': { fontWeight: letra.pesos.media, fontSize: letra.ayuda, borderBottom: 'none', px: 1, py: 0.5 },
        '& td': { borderRadius: `${medidas.boton.radio}px` },
      }}>
        <Box component="caption" id={idDelTitulo} sx={OCULTO_A_LA_VISTA}>
          {t('modeloIa.matrizTabla', { nombre: modelo.nombre })}
        </Box>
        <TableHead>
          <TableRow>
            <TableCell sx={{ color: 'text.secondary' }}>{t('modeloIa.claseReal')}</TableCell>
            <TableCell align="center">{t('modeloIa.comoSanos')}</TableCell>
            <TableCell align="center">{t('modeloIa.comoNoSanos')}</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {filas.map(([clave, celdas]) => (
            <TableRow key={clave}>
              <TableCell component="th" scope="row">{t(clave)}</TableCell>
              {celdas.map(([columna, granos, acierto]) => (
                <Celda key={columna} granos={granos} acierto={acierto} />
              ))}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Box>
  );
}
