/*
 * Nombre: ProveedorDeSesion.jsx
 * Descripción: La sesión de la app (RF-01, D-28, D-31): la abre con la respuesta del backend, la guarda en el
 *              navegador, pide al abrir la app el rol que tiene ahora el usuario, la cierra al vencer (RNF-04)
 *              o con un 401, y sigue a las otras pestañas del mismo navegador.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { useCallback, useEffect, useMemo, useState } from 'react';
import { crearCliente } from '../api/cliente.js';
import { sesionActual } from '../api/sesion.js';
import i18n from '../i18n/i18n.js';
import { borrarSesion, CLAVE_SESION, guardarSesion, leerSesion } from './almacen.js';
import { ContextoDeSesion } from './contexto.js';

/** setTimeout no espera más de unos 24 días: una espera más larga se parte en varias. */
const ESPERA_MAXIMA = 2 ** 31 - 1;

/**
 * Da la sesión a toda la app. Va dentro del enrutador, porque guarda la ruta para volver tras un 401.
 *
 * @param {{ children: import('react').ReactNode }} props las pantallas de la app
 * @returns {import('react').ReactElement} el proveedor
 */
export function ProveedorDeSesion({ children }) {
  const [estado, setEstado] = useState(() => ({ sesion: leerSesion(), cierre: null }));

  /*
   * Cierra la sesión. Si se cerró sin que la persona lo pidiera (un 401 o el vencimiento), recuerda dónde
   * estaba y de quién era, para volver ahí si entra el mismo usuario.
   */
  const cerrar = useCallback(({ mensaje = null, volver = false } = {}) => {
    borrarSesion();
    const { pathname, search, hash } = window.location;
    setEstado((actual) => ({
      sesion: null,
      cierre: {
        mensaje,
        regreso: volver && actual.sesion ? { ruta: pathname + search + hash, usuarioId: actual.sesion.usuario.id } : null,
      },
    }));
  }, []);

  const iniciar = useCallback(({ token, vence, usuario }) => {
    const sesion = { token, vence, usuario };
    guardarSesion(sesion);
    setEstado({ sesion, cierre: null });
  }, []);

  const cerrarSesion = useCallback(() => cerrar(), [cerrar]);

  const [pedir] = useState(() => crearCliente({
    token: () => leerSesion()?.token ?? null,
    idioma: () => i18n.language,
    alCerrarSesion: (mensaje) => cerrar({ mensaje, volver: true }),
  }));

  // Al abrir la app con una sesión guardada, el backend dice el rol que tiene ahora el usuario (D-28). Un 401
  // la cierra desde el cliente; sin conexión, sigue la sesión guardada.
  useEffect(() => {
    const inicial = leerSesion();
    if (!inicial) {
      return undefined;
    }
    let vigente = true;
    sesionActual(pedir).then(({ vence, usuario }) => {
      const actual = leerSesion();
      if (vigente && actual?.token === inicial.token) {
        const sesion = { ...actual, vence, usuario };
        guardarSesion(sesion);
        setEstado((anterior) => ({ ...anterior, sesion }));
      }
    }, () => {});
    return () => {
      vigente = false;
    };
  }, [pedir]);

  // La sesión vence a las 8 horas: a esa hora vuelve a Iniciar sesión (RF-01 C6).
  const vence = estado.sesion?.vence;
  useEffect(() => {
    if (!vence) {
      return undefined;
    }
    const limite = new Date(vence).getTime();
    let espera;
    const revisar = () => {
      const falta = limite - Date.now();
      if (falta > 0) {
        espera = setTimeout(revisar, Math.min(falta, ESPERA_MAXIMA));
      } else {
        cerrar({ mensaje: i18n.t('error.sesion_vencida'), volver: true });
      }
    };
    espera = setTimeout(revisar, Math.min(Math.max(limite - Date.now(), 0), ESPERA_MAXIMA));
    return () => clearTimeout(espera);
  }, [vence, cerrar]);

  // Otra pestaña cerró o abrió sesión: esta la sigue (D-31).
  useEffect(() => {
    const alCambiar = (evento) => {
      if (evento.key !== CLAVE_SESION && evento.key !== null) {
        return;
      }
      const guardada = leerSesion();
      setEstado((actual) => {
        if (!guardada) {
          return actual.sesion ? { sesion: null, cierre: null } : actual;
        }
        return actual.sesion?.token === guardada.token ? actual : { sesion: guardada, cierre: null };
      });
    };
    window.addEventListener('storage', alCambiar);
    return () => window.removeEventListener('storage', alCambiar);
  }, []);

  const valor = useMemo(
    () => ({ sesion: estado.sesion, cierre: estado.cierre, pedir, iniciar, cerrarSesion }),
    [estado, pedir, iniciar, cerrarSesion],
  );
  return <ContextoDeSesion.Provider value={valor}>{children}</ContextoDeSesion.Provider>;
}
