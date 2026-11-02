/*
 * Nombre: en.js
 * Descripción: Textos propios del frontend en inglés (RP-11). Las etiquetas de los datos y los mensajes que
 *              comparte con el backend no van aquí: salen de sus archivos .properties
 *              (herramientas/textos-del-backend.js). Los marcadores van entre llaves, como en el backend.
 * Fecha de creación: 20/10/2026
 * Actualización: 02/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.3
 */

/** Textos en inglés, por clave. Tiene las mismas claves que es.js (i18n.test.js lo revisa). */
export default {
  // Iniciar sesión (RF-01)
  'iniciarSesion.titulo': 'Sign in',
  'iniciarSesion.subtitulo': 'Sign in with your email and the password your administrator gave you.',
  'iniciarSesion.boton': 'Sign in',
  'iniciarSesion.olvido': 'Forgot your password? Ask your company administrator to reset it.',
  'iniciarSesion.lema': 'Assess your cocoa lots with photos of a sample',
  'iniciarSesion.descripcion': 'Photograph 100 to 150 beans on white paper and get the percentage of healthy beans, the probable category and a recommendation.',
  'iniciarSesion.universidad': 'Universidad Surcolombiana · Artificial Intelligence final project',
  'iniciarSesion.acercaDe': 'About InspectIA',
  'contrasena.mostrar': 'Show the password',
  'contrasena.ocultar': 'Hide the password',

  // Cambiar contraseña (RF-18)
  'cambiarContrasena.titulo': 'Change password',
  'cambiarContrasena.subtitulo': 'Enter your current password and the new one. When you save, your other sessions are closed; this one stays open.',
  'cambiarContrasena.actual': 'Current password',
  'cambiarContrasena.nueva': 'New password',
  'cambiarContrasena.repetida': 'Repeat the new password',
  'cambiarContrasena.mostrarActual': 'Show the current password',
  'cambiarContrasena.ocultarActual': 'Hide the current password',
  'cambiarContrasena.mostrarNueva': 'Show the new password',
  'cambiarContrasena.ocultarNueva': 'Hide the new password',
  'cambiarContrasena.mostrarRepetida': 'Show the repeated password',
  'cambiarContrasena.ocultarRepetida': 'Hide the repeated password',
  'cambiarContrasena.reglas': 'The new password must have:',
  'cambiarContrasena.reglaLargo': 'From 8 to 64 characters.',
  'cambiarContrasena.reglaBytes': 'At most 72 bytes: each accented letter and each ñ take 2.',
  'cambiarContrasena.reglaEspacios': 'Something other than spaces.',
  'cambiarContrasena.cumple': 'Met',
  'cambiarContrasena.noCumple': 'Not met',
  'cambiarContrasena.noCoincide': 'It does not match the new password.',
  'cambiarContrasena.listo': 'Your password changed. Your other sessions were closed; this one stays open.',
  'cambiarContrasena.bloqueoHasta': 'You can try again at {hora}.',

  // Acciones y errores comunes (RNF-06 C3)
  'accion.guardar': 'Save',
  'accion.cancelar': 'Cancel',
  'accion.irAlInicio': 'Go to the home page',
  'conexion.sinConexion': 'Could not connect to the server. Check your internet connection and try again.',

  // Marco: barra, menú y «Mi cuenta» (RF-01 C1, RNF-05, RNF-06 C4)
  'marco.saltar': 'Skip to content',
  'menu.principal': 'Main menu',
  'menu.abrir': 'Open the menu',
  'menu.inicio': 'Home',
  'menu.nuevoLote': 'New lot',
  'menu.historial': 'History',
  'menu.estadisticas': 'Statistics',
  'cuenta.titulo': 'My account',
  'cuenta.boton': 'My account: {nombre}, {detalle}',
  'cuenta.cambiarContrasena': 'Change password',
  'cuenta.acercaDe': 'About',
  'cuenta.cerrarSesion': 'Sign out',

  // Idioma y tema (RP-08, RP-11)
  'idioma.titulo': 'Language',
  'idioma.boton': 'Language: {idioma}',
  'tema.titulo': 'Theme',
  'tema.claro': 'Light',
  'tema.oscuro': 'Dark',
  'tema.sistema': 'System',
  'tema.aOscuro': 'Switch to dark theme',
  'tema.aClaro': 'Switch to light theme',

  // Inicio de cada rol
  'inicio.saludo': 'Hello, {nombre}',
  'inicio.USUARIO_FINAL': '{empresa} · Choose what you want to do.',
  'inicio.ADMIN_EMPRESA': 'You manage {empresa}.',
  'inicio.ADMIN_PLATAFORMA': 'You manage the companies that use InspectIA.',

  // Secciones pendientes, sin permiso y direcciones que no existen
  'pendiente.texto': 'This section will be available soon.',
  'sinPermiso.titulo': 'No permission',
  'noEncontrada.titulo': 'This page does not exist',
  'noEncontrada.texto': 'Check the address or go back to the home page.',

  // Pantallas generadas: lo común a todas (RP-23); lo propio de cada una sale de pantalla.textos de su JSON
  'crud.editar': 'Edit',
  'crud.desactivar': 'Deactivate',
  'crud.reactivar': 'Reactivate',
  'crud.guardarCambios': 'Save changes',
  'crud.estado': 'Status',
  'crud.acciones': 'Actions',
  'crud.cargando': 'Loading…',
  'crud.reintentar': 'Try again',
  'crud.cerrar': 'Close',
  'crud.rango': '{desde}–{hasta} of {total}',
  'crud.paginaSiguiente': 'Go to the next page',
  'crud.paginaAnterior': 'Go to the previous page',
  'crud.restaurarPorDefecto': 'Restore default values',
  'crud.confirmarRestaurar': 'Restore the default values?',
  'crud.restaurar': 'Restore',
  'crud.eligeUnaOpcion': 'Choose an option',

  // Administradores de una empresa, en la pantalla de empresas (RF-02 C12, D-30)
  'administradores.accion': 'Administrators',
  'administradores.accionDe': 'Administrators of {nombre}',
  'administradores.titulo': 'Administrators of {empresa}',
  'administradores.elige': 'Choose whose password to reset.',
  'administradores.activo': 'Active',
  'administradores.inactivo': 'Deactivated',
  'administradores.restablecer': 'Reset password',
  'administradores.restablecerDe': 'Reset the password of {nombre}',
  'administradores.contrasenaDe': 'New password for {nombre}',
  'administradores.contrasenaNueva': 'New password',
  'administradores.ayuda': '8 to 64 characters; each accented letter or ñ takes 2 of the 72 bytes.',
  'administradores.aviso': 'When you reset it, their open sessions are closed and the lock for failed attempts is removed, if they had one. Share it with {nombre}: they can change it in “Change password”.',
  'administradores.listo': 'The password of {nombre} was reset.',

  // Resumen de los rangos de recomendación: qué porcentajes abarca cada categoría (RF-13)
  'rangos.resumen.titulo': 'How the ranges look',
  'rangos.resumen.desde': '{limite}% or more',
  'rangos.resumen.entre': '{desde} to {hasta}%',
  'rangos.resumen.menosDe': 'less than {limite}%',
  'rangos.resumen.revisar': 'check the limit',

  // Acerca de, con la cita del dataset (RNF-08 C1)
  'acercaDe.titulo': 'About InspectIA',
  'acercaDe.descripcion': 'InspectIA assesses lots of cocoa beans from photos of a sample: it separates each bean, classifies it as healthy or not healthy with an image recognition model and calculates the percentage of healthy beans in the lot.',
  'acercaDe.modeloTitulo': 'What the model measures',
  'acercaDe.modeloTexto': 'The model looks at each bean from the outside: it does not measure internal fermentation. That is why the category the app gives is probable; the official one is confirmed with the cut test (NTC 1252:2021).',
  'acercaDe.datosTitulo': 'Data it was trained with',
  'acercaDe.dataset': 'Dataset',
  'acercaDe.datasetValor': 'Ecuadorian cacao beans for classification (cacao-beans-for-classification), version 1, published on 07/29/2022.',
  'acercaDe.autor': 'Author',
  'acercaDe.autorValor': 'Luis Fernando Zhinin Vera, on Kaggle.',
  'acercaDe.articulo': 'Article',
  'acercaDe.articuloValor': 'Zhinin-Vera, L. et al. (2023). IWANN 2023, LNCS 14135.',
  'acercaDe.licencia': 'License',
  'acercaDe.licenciaValor': 'CC BY-NC 4.0: it allows use with credit to the authors, but not commercial use.',
  'acercaDe.kaggle': 'See the dataset on Kaggle',
  'acercaDe.nota': 'Because of that license, InspectIA is free of charge while it uses models trained with this data.',
  'acercaDe.proyectoTitulo': 'The project',
  'acercaDe.proyectoTexto': 'Final project of the Artificial Intelligence course, Software Engineering Program, Universidad Surcolombiana. Team: Cristhian Eduardo Sánchez Mosquera, Julián Andrés Silva Morales and Jhon Jairo Perdomo Perdomo. Professor: Juan Antonio Castro Silva.',
  'acercaDe.version': 'Version {version}',
  'acercaDe.volver': 'Back to Sign in',
};
