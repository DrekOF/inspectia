/*
 * Nombre: pantallas.js
 * Descripción: Las pantallas generadas: la ruta, la entrada del menú y los roles de cada una (RP-23,
 *              RNF-04).
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 27/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.9)
 * Versión: 1.9
 * Entidades: empresa 1.3, foto 1.0, grano 1.0, lote 1.0, modelo 1.0, muestra 1.0, rango_recomendacion 1.2, usuario 1.8
 */
import { lazy } from 'react';

/**
 * @typedef {object} PantallaGenerada una pantalla que sale de un JSON de specs/entidades
 * @property {string} entidad la entidad, como empresa: con ella Rutas.jsx le pasa lo escrito a mano para ella
 * @property {string} ruta su ruta en la app
 * @property {string} etiqueta la clave del nombre de su entrada en el menú
 * @property {string} icono el nombre de su ícono en MenuLateral.jsx
 * @property {string[]} roles los roles que la abren: los de listar
 * @property {import('react').ComponentType<object>} Pantalla la pantalla, que se carga la primera vez que se abre
 */

/**
 * Las pantallas generadas, en el orden de alcance.json.
 *
 * @type {PantallaGenerada[]}
 */
export const PANTALLAS = [
  {
    entidad: 'empresa',
    ruta: '/empresas',
    etiqueta: 'empresa.etiqueta.plural',
    icono: 'empresas',
    roles: ['ADMIN_PLATAFORMA'],
    Pantalla: lazy(() => import('./empresa/EmpresaPantalla.jsx')),
  },
  {
    entidad: 'usuario',
    ruta: '/usuarios',
    etiqueta: 'usuario.etiqueta.plural',
    icono: 'usuarios',
    roles: ['ADMIN_EMPRESA'],
    Pantalla: lazy(() => import('./usuario/UsuarioPantalla.jsx')),
  },
];
