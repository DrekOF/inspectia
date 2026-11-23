/*
 * Nombre: CampoDeOrigen.jsx
 * Descripción: El campo «Finca o proveedor» de Nuevo lote, como en el prototipo: al escribir, sugiere las fincas
 *              y los proveedores que ya usó la empresa, para reutilizar el nombre que ya existe y que las
 *              estadísticas por finca no se partan (RF-04, restricción 3; RF-16). Se puede escribir uno nuevo.
 *              Tiene la etiqueta, el error, la ayuda y el alto de los demás campos (RNF-06), y no tiene botón para
 *              borrar: el texto se borra con el teclado, como en el prototipo.
 * Fecha de creación: 22/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import Autocomplete from '@mui/material/Autocomplete';
import FormControl from '@mui/material/FormControl';
import FormLabel from '@mui/material/FormLabel';
import OutlinedInput from '@mui/material/OutlinedInput';
import { useField } from 'formik';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { origenesDeLotes } from '../api/lotes.js';
import { idsQueDescriben, MensajesDelCampo } from '../componentes/MensajesDelCampo.jsx';
import { useSesion } from '../sesion/contexto.js';

/** Cuánto espera, después de la última tecla, para pedir las sugerencias: así no pide una por cada letra. */
const ESPERA_EN_MS = 250;

/**
 * Las sugerencias para lo que lleva escrito: ninguna si está en blanco, o si no se pudieron pedir.
 *
 * @param {string} texto lo que lleva escrito
 * @returns {string[]} las sugerencias
 */
function useSugerencias(texto) {
  const { pedir } = useSesion();
  const buscado = texto.trim();
  const [respuesta, setRespuesta] = useState({ buscado: '', sugerencias: [] });

  useEffect(() => {
    if (!buscado) {
      return undefined;
    }
    let vigente = true;
    const espera = setTimeout(() => {
      origenesDeLotes(pedir, buscado)
        .then((sugerencias) => vigente && setRespuesta({ buscado, sugerencias }))
        .catch(() => vigente && setRespuesta({ buscado, sugerencias: [] }));
    }, ESPERA_EN_MS);
    return () => {
      vigente = false;
      clearTimeout(espera);
    };
  }, [pedir, buscado]);

  return buscado && respuesta.buscado === buscado ? respuesta.sugerencias : [];
}

/**
 * El campo de la finca o el proveedor.
 *
 * @param {object} props las propiedades
 * @param {string} props.nombre el nombre del campo en Formik; también es su id
 * @param {string} props.etiqueta su etiqueta
 * @param {string} [props.ayuda] la ayuda que va debajo
 * @returns {import('react').ReactElement} el campo
 */
export function CampoDeOrigen({ nombre, etiqueta, ayuda }) {
  const { t } = useTranslation();
  const [campo, meta, ayudas] = useField(nombre);
  const sugerencias = useSugerencias(campo.value);
  const error = meta.touched && meta.error ? meta.error : null;

  return (
    <FormControl fullWidth error={Boolean(error)}>
      <FormLabel htmlFor={nombre}>{etiqueta}</FormLabel>
      {/* Sin el relleno que Material UI le pone al campo de un Autocomplete, mide 48 px como los demás. */}
      <Autocomplete id={nombre} freeSolo disableClearable options={sugerencias} filterOptions={(opciones) => opciones}
        inputValue={campo.value} onInputChange={(_, valor) => ayudas.setValue(valor)}
        onChange={(_, valor) => ayudas.setValue(valor ?? '')}
        slotProps={{ listbox: { 'aria-label': t('nuevoLote.sugerencias') } }}
        sx={{ '& .MuiOutlinedInput-root': { p: 0, '& .MuiAutocomplete-input': { p: '0 14px' } } }}
        renderInput={({ slotProps: { input, htmlInput } }) => (
          <OutlinedInput ref={input.ref} className={input.className} startAdornment={input.startAdornment}
            endAdornment={input.endAdornment} onMouseDown={input.onMouseDown} fullWidth autoComplete="off"
            inputProps={{
              ...htmlInput,
              name: nombre,
              onBlur: (evento) => {
                htmlInput.onBlur?.(evento);
                ayudas.setTouched(true);
              },
              'aria-describedby': idsQueDescriben(nombre, { error, ayuda }),
            }} />
        )} />
      <MensajesDelCampo nombre={nombre} error={error} ayuda={ayuda} />
    </FormControl>
  );
}
