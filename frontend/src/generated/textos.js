/*
 * Nombre: textos.js
 * Descripción: Los textos de las pantallas generadas, en español y en inglés: salen de pantalla.textos
 *              de cada JSON (RP-11).
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.7)
 * Versión: 1.7
 * Entidades: empresa 1.3, foto 1.0, grano 1.0, lote 1.0, modelo 1.0, muestra 1.0, rango_recomendacion 1.2, usuario 1.6
 */

/**
 * Los textos de las pantallas generadas, por idioma: salen de pantalla.textos de cada JSON (RP-11). Los que se
 * cuentan van en singular y en plural. i18n.js los junta con los textos del backend y con los propios del
 * frontend.
 */
export default {
  es: {
    'empresa.pantalla.resumen': '{total}: {activos} y {inactivos}.',
    'empresa.pantalla.resumen_total.singular': '{cantidad} empresa registrada',
    'empresa.pantalla.resumen_total.plural': '{cantidad} empresas registradas',
    'empresa.pantalla.resumen_activos.singular': '{cantidad} activa',
    'empresa.pantalla.resumen_activos.plural': '{cantidad} activas',
    'empresa.pantalla.resumen_inactivos.singular': '{cantidad} desactivada',
    'empresa.pantalla.resumen_inactivos.plural': '{cantidad} desactivadas',
    'empresa.pantalla.vacia': 'Todavía no hay empresas registradas.',
    'empresa.pantalla.nuevo': 'Nueva empresa',
    'empresa.pantalla.crear': 'Registrar empresa',
    'empresa.pantalla.titulo_editar': 'Editar empresa',
    'empresa.pantalla.nota_formulario': 'La empresa queda con los rangos de recomendación por defecto. Después, su administrador crea los usuarios de la empresa.',
    'empresa.pantalla.contrasena_al_crear': 'Contraseña inicial',
    'empresa.pantalla.ayuda_contrasena_al_crear': 'De 8 a 64 caracteres; cada tilde o eñe ocupa 2 de los 72 bytes. Compártela con la persona: podrá cambiarla en «Cambiar contraseña».',
    'empresa.pantalla.titulo_lista': 'Empresas registradas',
    'empresa.pantalla.columna_nombre': 'Empresa',
    'empresa.pantalla.debajo_del_nombre': 'NIT {nit}',
    'empresa.pantalla.activo': 'Activa',
    'empresa.pantalla.inactivo': 'Desactivada',
    'empresa.pantalla.editar_fila': 'Editar {nombre}',
    'empresa.pantalla.desactivar_fila': 'Desactivar {nombre}',
    'empresa.pantalla.reactivar_fila': 'Reactivar {nombre}',
    'empresa.pantalla.confirmar_desactivar': '¿Desactivar {nombre}?',
    'empresa.pantalla.confirmar_desactivar_detalle': 'Sus usuarios no podrán iniciar sesión hasta que la reactives. No se borra ningún dato.',
    'empresa.pantalla.nota_lista': 'Desactivar una empresa no borra nada: sus usuarios no pueden iniciar sesión hasta que la reactives, y sus lotes y resultados se conservan.',
  },
  en: {
    'empresa.pantalla.resumen': '{total}: {activos} and {inactivos}.',
    'empresa.pantalla.resumen_total.singular': '{cantidad} registered company',
    'empresa.pantalla.resumen_total.plural': '{cantidad} registered companies',
    'empresa.pantalla.resumen_activos.singular': '{cantidad} active',
    'empresa.pantalla.resumen_activos.plural': '{cantidad} active',
    'empresa.pantalla.resumen_inactivos.singular': '{cantidad} deactivated',
    'empresa.pantalla.resumen_inactivos.plural': '{cantidad} deactivated',
    'empresa.pantalla.vacia': 'There are no registered companies yet.',
    'empresa.pantalla.nuevo': 'New company',
    'empresa.pantalla.crear': 'Register company',
    'empresa.pantalla.titulo_editar': 'Edit company',
    'empresa.pantalla.nota_formulario': 'The company gets the default recommendation ranges. Then its administrator creates the company\'s users.',
    'empresa.pantalla.contrasena_al_crear': 'Initial password',
    'empresa.pantalla.ayuda_contrasena_al_crear': '8 to 64 characters; each accented letter or ñ takes 2 of the 72 bytes. Share it with the person: they can change it in “Change password”.',
    'empresa.pantalla.titulo_lista': 'Registered companies',
    'empresa.pantalla.columna_nombre': 'Company',
    'empresa.pantalla.debajo_del_nombre': 'NIT {nit}',
    'empresa.pantalla.activo': 'Active',
    'empresa.pantalla.inactivo': 'Deactivated',
    'empresa.pantalla.editar_fila': 'Edit {nombre}',
    'empresa.pantalla.desactivar_fila': 'Deactivate {nombre}',
    'empresa.pantalla.reactivar_fila': 'Reactivate {nombre}',
    'empresa.pantalla.confirmar_desactivar': 'Deactivate {nombre}?',
    'empresa.pantalla.confirmar_desactivar_detalle': 'Its users won\'t be able to sign in until you reactivate it. No data is deleted.',
    'empresa.pantalla.nota_lista': 'Deactivating a company deletes nothing: its users can\'t sign in until you reactivate it, and its lots and results are kept.',
  },
};
