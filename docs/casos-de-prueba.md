<!--
Nombre: casos-de-prueba.md
Descripción: Casos de prueba de InspectIA por ficha y criterio de aceptación, y los criterios que aún no
             tienen prueba (RP-13). Archivo generado por generador/casos_de_prueba.py: no se edita a mano;
             sale de specs/fichas.json y de las marcas de las pruebas.
Fecha de creación: 12/10/2026
Actualización: 23/11/2026
Autor: Equipo InspectIA (generado por generador/casos_de_prueba.py v1.2)
Versión: 1.2
-->

# Casos de prueba

Cada criterio de aceptación de `specs/fichas.json` con las pruebas que lo cubren, y los que todavía no tienen ninguna (RP-13). Cada prueba dice qué cubre con su marca: `@DisplayName` en Java, `@pytest.mark.caso` en Python y el título de `test` o `it` en JavaScript, con la forma «<código> · C<n> · …» si cubre un criterio o «<código> · …» si no (ver `CLAUDE.md`).

Este documento no se edita a mano. Después de agregar o cambiar pruebas, o de cambiar las fichas, se vuelve a armar:

```bash
generador/.venv/Scripts/python generador/casos_de_prueba.py generar
```

## Resumen

- **154 criterios de aceptación** en 26 fichas: 62 con prueba y 90 pendientes (2 eliminados).
- **810 pruebas:** 277 de Java (33 generadas), 412 de JavaScript (73 generadas) y 121 de Python. Una prueba parametrizada cuenta una vez.

| Ficha | Criterios | Con prueba | Pendientes |
|---|---:|---:|---:|
| RF-01 · Iniciar sesión según el rol | 8 | 8 | 0 |
| RF-02 · Registrar empresas y su primer administrador | 12 | 12 | 0 |
| RF-03 · Gestionar los usuarios de la empresa | 11 | 11 | 0 |
| RF-04 · Registrar un lote | 4 | 4 | 0 |
| RF-05 · Tomar o subir las fotos de la muestra del lote, con guía de captura | 5 | 1 | 4 |
| RF-06 · Validar la calidad de cada foto (nitidez y luz) y pedir repetirla si no sirve | 3 | 0 | 3 |
| RF-07 · Separar y recortar los granos de cada foto, con control de granos pegados | 5 | 0 | 5 |
| RF-08 · Clasificar cada grano como sano o no sano con el modelo de IA | 6 | 0 | 6 |
| RF-09 · Pedir una foto más clara o más fotos cuando la confianza del modelo es baja | 6 | 0 | 6 |
| RF-10 · Calcular el porcentaje de granos sanos del lote | 14 | 0 | 14 |
| RF-11 · Mostrar el resultado con la foto marcada (cada grano sano o no sano) | 6 | 0 | 6 |
| RF-12 · Generar la recomendación según los rangos de la empresa | 12 | 0 | 10 |
| RF-13 · Configurar los rangos de recomendación de la empresa | 5 | 4 | 1 |
| RF-14 · Consultar el historial de lotes evaluados | 6 | 2 | 4 |
| RF-15 · Descargar el reporte del lote en PDF | 5 | 0 | 5 |
| RF-16 · Consultar las estadísticas de la empresa | 4 | 0 | 4 |
| RF-17 · Consultar las métricas del modelo | 3 | 0 | 3 |
| RF-18 · Cambiar la propia contraseña | 5 | 5 | 0 |
| RNF-01 · Entregar el resultado del análisis de un lote en 5 a 8 segundos | 4 | 0 | 4 |
| RNF-02 · Soportar hasta 30 empresas con 25 usuarios cada una | 4 | 0 | 4 |
| RNF-03 · Mantener separados los datos de cada empresa | 5 | 4 | 1 |
| RNF-04 · Exigir autenticación y restringir cada función según el rol | 6 | 6 | 0 |
| RNF-05 · Funcionar en PC y en celular desde el navegador | 3 | 2 | 1 |
| RNF-06 · Ser sencilla de usar para el usuario final | 4 | 2 | 2 |
| RNF-07 · Alcanzar el F1-score mínimo del modelo de IA | 5 | 0 | 5 |
| RNF-08 · Citar a los autores del dataset y no usarlo con fines comerciales | 3 | 1 | 2 |

## RF-01 · Iniciar sesión según el rol

| Criterio | Pruebas |
|---|---|
| C1. Con datos correctos, el sistema muestra el menú del rol del usuario. | [`InicioSesionIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/InicioSesionIT.java) · con datos correctos, cada rol inicia sesión y recibe su rol, su nombre y su empresa para armar su menú (parametrizada)<br>[`SesionServicioTest`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionServicioTest.java) · con la contraseña correcta, los intentos vuelven a cero y entrega el token, con la versión de sesión de la base, y el usuario con su rol y su empresa<br>[`iniciar-sesion.spec.js`](../frontend/e2e/iniciar-sesion.spec.js) · el administrador de empresa entra y ve el menú de su rol<br>[`iniciar-sesion.spec.js`](../frontend/e2e/iniciar-sesion.spec.js) · el administrador de la plataforma entra y ve el menú de su rol<br>[`iniciar-sesion.spec.js`](../frontend/e2e/iniciar-sesion.spec.js) · el usuario final entra y ve el menú de su rol<br>[`flujos.test.js`](../frontend/selenium/flujos.test.js) · el administrador de empresa entra y ve el menú de su rol (Selenium)<br>[`Marco.test.jsx`](../frontend/src/navegacion/Marco.test.jsx) · la barra muestra quién entró: su nombre y su empresa, o su rol si no tiene empresa: %s (parametrizada)<br>[`menu.test.js`](../frontend/src/navegacion/menu.test.js) · cada rol ve su menú, sin las funciones de los otros roles: %s (parametrizada)<br>[`IniciarSesion.test.jsx`](../frontend/src/paginas/IniciarSesion.test.jsx) · con datos correctos entra al inicio de su rol y ve el menú de su rol: %s (parametrizada)<br>[`sesion.test.jsx`](../frontend/src/sesion/sesion.test.jsx) · al abrir la app con la sesión guardada entra sin pedir los datos, con el rol que responde el backend: un cambio de rol se ve al volver a abrirla (D-28) |
| C2. Con datos incorrectos, muestra «Correo o contraseña incorrectos», sin decir cuál de los dos falló. | [`InicioSesionIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/InicioSesionIT.java) · con datos incorrectos responde «Correo o contraseña incorrectos», igual si falló el correo o la contraseña<br>[`SesionServicioTest`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionServicioTest.java) · si el correo no existe, la contraseña igual se compara con BCrypt, para que la respuesta tarde lo mismo<br>[`iniciar-sesion.spec.js`](../frontend/e2e/iniciar-sesion.spec.js) · con datos errados ve «Correo o contraseña incorrectos» y la regla del bloqueo, igual con un correo que no existe<br>[`flujos.test.js`](../frontend/selenium/flujos.test.js) · con datos errados se ve «Correo o contraseña incorrectos» (Selenium)<br>[`cliente.test.js`](../frontend/src/api/cliente.test.js) · un 401 sin sesión, como el de datos errados al iniciar sesión, no cierra nada: solo responde el error<br>[`IniciarSesion.test.jsx`](../frontend/src/paginas/IniciarSesion.test.jsx) · con datos errados muestra «Correo o contraseña incorrectos» y la regla del bloqueo, sin marcar cuál de los dos falló |
| C3. Un usuario desactivado no puede iniciar sesión. | [`InicioSesionIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/InicioSesionIT.java) · un usuario desactivado no puede iniciar sesión<br>[`SesionAbiertaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionAbiertaIT.java) · al desactivar un usuario, su sesión abierta deja de servir en la siguiente petición<br>[`iniciar-sesion.spec.js`](../frontend/e2e/iniciar-sesion.spec.js) · un usuario desactivado no entra y ve el motivo<br>[`IniciarSesion.test.jsx`](../frontend/src/paginas/IniciarSesion.test.jsx) · un usuario desactivado no entra: ve el motivo que da el backend |
| C4. Un usuario de una empresa desactivada no puede iniciar sesión (D-27). | [`InicioSesionIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/InicioSesionIT.java) · un usuario de una empresa desactivada no puede iniciar sesión<br>[`SesionAbiertaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionAbiertaIT.java) · al desactivar una empresa, las sesiones abiertas de sus usuarios dejan de servir<br>[`iniciar-sesion.spec.js`](../frontend/e2e/iniciar-sesion.spec.js) · un usuario de una empresa desactivada no entra y ve el motivo<br>[`IniciarSesion.test.jsx`](../frontend/src/paginas/IniciarSesion.test.jsx) · un usuario de una empresa desactivada no entra: ve el motivo que da el backend |
| C5. Después de 5 intentos fallidos seguidos, la cuenta se bloquea 15 minutos. | [`InicioSesionIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/InicioSesionIT.java) · después de 5 intentos fallidos seguidos la cuenta se bloquea: ni la contraseña correcta entra hasta que pasen 15 minutos<br>[`InicioSesionIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/InicioSesionIT.java) · los intentos fallidos deben ser seguidos: iniciar sesión vuelve a contar desde cero<br>[`SesionServicioTest`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionServicioTest.java) · durante el bloqueo, ni la contraseña correcta entra; el intento no cuenta y también se compara con BCrypt<br>[`SesionServicioTest`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionServicioTest.java) · el bloqueo dura hasta su hora exacta: un segundo antes no entra y en ese segundo ya entra (prueba de límite) (parametrizada)<br>[`SesionServicioTest`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionServicioTest.java) · un fallo se registra con la hora del servidor y, si es el quinto seguido, bloquea la cuenta 15 minutos desde ese momento<br>[`iniciar-sesion.spec.js`](../frontend/e2e/iniciar-sesion.spec.js) · después de 5 intentos fallidos seguidos la cuenta se bloquea: el sexto no entra aunque la contraseña sea la correcta |
| C6. La sesión vence a las 8 horas y el sistema pide iniciar sesión de nuevo. | [`ValidadorDeVigenciaTest`](../backend/src/test/java/co/edu/usco/inspectia/comun/seguridad/ValidadorDeVigenciaTest.java) · el token sirve hasta un segundo antes de vencer; desde el segundo en que vence, ya no (prueba de límite) (parametrizada)<br>[`SesionAbiertaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionAbiertaIT.java) · con la sesión vencida se puede volver a iniciar sesión, aunque el frontend todavía envíe el token viejo<br>[`SesionAbiertaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionAbiertaIT.java) · la sesión vence a las 8 horas y pide iniciar sesión de nuevo (prueba de límite) (parametrizada)<br>[`iniciar-sesion.spec.js`](../frontend/e2e/iniciar-sesion.spec.js) · la sesión vence a las 8 horas y la app pide iniciar sesión de nuevo<br>[`almacen.test.js`](../frontend/src/sesion/almacen.test.js) · una sesión vencida no se usa: al leerla se borra y hay que iniciar sesión de nuevo: %s (parametrizada)<br>[`sesion.test.jsx`](../frontend/src/sesion/sesion.test.jsx) · la sesión vence a las 8 horas: la app vuelve a Iniciar sesión y pide entrar de nuevo |
| C7. Al terminar el bloqueo, la cuenta vuelve a tener 5 intentos: un fallo más no la bloquea. | [`InicioSesionIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/InicioSesionIT.java) · al terminar el bloqueo, la cuenta vuelve a tener 5 intentos: un fallo más no la bloquea |
| C8. Una contraseña de más de 72 bytes nunca inicia sesión, aunque sus primeros 72 bytes coincidan con la guardada. Recibe la misma respuesta y tarda lo mismo que cualquier intento fallido, y cuenta como uno. | [`InicioSesionIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/InicioSesionIT.java) · una contraseña de más de 72 bytes nunca inicia sesión, aunque sus primeros 72 bytes coincidan con la guardada, y cuenta como intento fallido<br>[`SesionServicioTest`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionServicioTest.java) · una contraseña de más de 72 bytes no entra aunque BCrypt diga que coincide, cuenta como fallo y también se compara con BCrypt<br>[`iniciar-sesion.spec.js`](../frontend/e2e/iniciar-sesion.spec.js) · una contraseña de más de 72 bytes no entra aunque sus primeros 72 coincidan con la guardada: ve lo mismo que con cualquier fallo<br>[`IniciarSesion.test.jsx`](../frontend/src/paginas/IniciarSesion.test.jsx) · una contraseña de más de 64 caracteres y 72 bytes llega completa al backend y recibe la misma respuesta que cualquier fallo<br>[`esquemas.test.js`](../frontend/src/validacion/esquemas.test.js) · la contraseña para iniciar sesión no tiene límite de largo ni de bytes: la que no sirve la rechaza el backend |

Otras pruebas de RF-01, que no cubren un criterio:

- [`CrearAdminPlataformaIT`](../backend/src/test/java/co/edu/usco/inspectia/CrearAdminPlataformaIT.java) · el script crea el administrador de la plataforma sin empresa y con un hash que Spring Security acepta
- [`CrearAdminPlataformaIT`](../backend/src/test/java/co/edu/usco/inspectia/CrearAdminPlataformaIT.java) · el script no cambia nada si el administrador ya existe, así que se puede correr varias veces
- [`CrearAdminPlataformaIT`](../backend/src/test/java/co/edu/usco/inspectia/CrearAdminPlataformaIT.java) · el script no crea nada si falta un dato o si el nombre o el correo no son válidos (parametrizada)
- [`CrearAdminPlataformaIT`](../backend/src/test/java/co/edu/usco/inspectia/CrearAdminPlataformaIT.java) · el script no toca un usuario de una empresa que tenga el mismo correo
- [`CrearAdminPlataformaIT`](../backend/src/test/java/co/edu/usco/inspectia/CrearAdminPlataformaIT.java) · la contraseña del administrador acepta de 8 a 64 caracteres (prueba de límite) (parametrizada)
- [`CrearAdminPlataformaIT`](../backend/src/test/java/co/edu/usco/inspectia/CrearAdminPlataformaIT.java) · la contraseña del administrador ocupa como máximo 72 bytes, con el mensaje de RF-02 (prueba de límite) (parametrizada)
- [`CrearAdminPlataformaIT`](../backend/src/test/java/co/edu/usco/inspectia/CrearAdminPlataformaIT.java) · la contraseña no sale en pantalla ni en el registro de PostgreSQL, aunque el servidor registre todas las sentencias
- [`CrearAdminPlataformaIT`](../backend/src/test/java/co/edu/usco/inspectia/CrearAdminPlataformaIT.java) · una contraseña de solo espacios cuenta como si faltara, como en la API; con una letra se acepta (prueba de límite) (parametrizada)
- [`InicioSesionIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/InicioSesionIT.java) · el correo se compara sin distinguir mayúsculas ni espacios al inicio o al final
- [`InicioSesionIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/InicioSesionIT.java) · si falta el correo o la contraseña, responde 400 con cada campo que falta
- [`cliente.test.js`](../frontend/src/api/cliente.test.js) · cada petición lleva el token en Authorization: Bearer y el idioma de la app en Accept-Language
- [`cliente.test.js`](../frontend/src/api/cliente.test.js) · sin sesión no envía Authorization y envía los datos en JSON
- [`Marco.test.jsx`](../frontend/src/navegacion/Marco.test.jsx) · las secciones que aún no existen dicen «Esta sección estará disponible pronto.», sin códigos de requisitos: %s (parametrizada)
- [`Marco.test.jsx`](../frontend/src/navegacion/Marco.test.jsx) · una dirección que no existe lo dice y ofrece volver al inicio
- [`menu.test.js`](../frontend/src/navegacion/menu.test.js) · un rol que la app no conoce no ve funciones, solo «Mi cuenta»
- [`menu.test.js`](../frontend/src/navegacion/menu.test.js) · «Mi cuenta» tiene Cambiar contraseña, Acerca de y Cerrar sesión, para todos los roles
- [`IniciarSesion.test.jsx`](../frontend/src/paginas/IniciarSesion.test.jsx) · con una sesión abierta, Iniciar sesión lleva al inicio de su rol
- [`IniciarSesion.test.jsx`](../frontend/src/paginas/IniciarSesion.test.jsx) · la contraseña se puede mostrar y volver a ocultar
- [`IniciarSesion.test.jsx`](../frontend/src/paginas/IniciarSesion.test.jsx) · sin datos no envía nada y marca los dos campos como obligatorios
- [`IniciarSesion.test.jsx`](../frontend/src/paginas/IniciarSesion.test.jsx) · sin sesión, la app abre Iniciar sesión, con el correo y la contraseña listos para el navegador
- [`almacen.test.js`](../frontend/src/sesion/almacen.test.js) · cerrar sesión borra el token del navegador (D-28)
- [`almacen.test.js`](../frontend/src/sesion/almacen.test.js) · la sesión queda guardada en el navegador y sobrevive a recargar la página (D-31)
- [`almacen.test.js`](../frontend/src/sesion/almacen.test.js) · sin sesión guardada no hay sesión
- [`almacen.test.js`](../frontend/src/sesion/almacen.test.js) · una sesión guardada incompleta no se usa y se borra: %s (parametrizada)
- [`almacen.test.js`](../frontend/src/sesion/almacen.test.js) · una sesión guardada que no se puede leer se borra
- [`sesion.test.jsx`](../frontend/src/sesion/sesion.test.jsx) · Cerrar sesión borra el token y vuelve a Iniciar sesión; al entrar de nuevo va al inicio (D-28)
- [`sesion.test.jsx`](../frontend/src/sesion/sesion.test.jsx) · cerrar sesión en otra pestaña la cierra también en esta (D-31)
- [`sesion.test.jsx`](../frontend/src/sesion/sesion.test.jsx) · después de un 401, si entra el mismo usuario vuelve a la pantalla donde estaba
- [`sesion.test.jsx`](../frontend/src/sesion/sesion.test.jsx) · después de un 401, si entra otro usuario va al inicio de su rol
- [`volver.test.js`](../frontend/src/sesion/volver.test.js) · con una ruta externa, o la misma de Iniciar sesión, va al inicio de su rol: %s (parametrizada)
- [`volver.test.js`](../frontend/src/sesion/volver.test.js) · si entra el mismo usuario, vuelve a la pantalla donde estaba
- [`volver.test.js`](../frontend/src/sesion/volver.test.js) · si entra otro usuario, va al inicio de su rol, no a la pantalla del anterior
- [`volver.test.js`](../frontend/src/sesion/volver.test.js) · sin pantalla anterior, como al abrir la app, va al inicio de su rol
- [`volver.test.js`](../frontend/src/sesion/volver.test.js) · solo se vuelve a una ruta interna: empieza por «/» y no por «//» ni «/\»: %s (parametrizada)
- [`esquemas.test.js`](../frontend/src/validacion/esquemas.test.js) · el correo y la contraseña son obligatorios: %s (parametrizada)

## RF-02 · Registrar empresas y su primer administrador

| Criterio | Pruebas |
|---|---|
| C1. Con datos válidos, la empresa y su administrador quedan registrados. | [`RegistroEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/RegistroEmpresaIT.java) · con datos válidos, la empresa y su administrador quedan registrados<br>[`flujos.test.js`](../frontend/selenium/flujos.test.js) · el administrador de la plataforma registra una empresa y su primer administrador en la pantalla (Selenium)<br>[`RegistrarEmpresa.test.jsx`](../frontend/src/empresa/RegistrarEmpresa.test.jsx) · con datos válidos, la pantalla registra la empresa y su primer administrador, y la muestra en la lista |
| C2. Si el NIT o el correo del administrador ya existen, el sistema no registra la empresa y lo avisa. | [`RegistroEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/RegistroEmpresaIT.java) · si el NIT ya existe, no registra la empresa y lo avisa<br>[`RegistroEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/RegistroEmpresaIT.java) · si el correo del administrador ya existe, aunque cambien las mayúsculas, no registra la empresa y lo avisa<br>[`RegistrarEmpresa.test.jsx`](../frontend/src/empresa/RegistrarEmpresa.test.jsx) · si el NIT o el correo del administrador ya existen, no la registra y lo avisa en cada campo |
| C3. Si faltan datos obligatorios, el formulario marca los campos que faltan. | [`RegistroEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/RegistroEmpresaIT.java) · si falta el administrador, lo marca y no guarda nada<br>[`RegistroEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/RegistroEmpresaIT.java) · si faltan datos obligatorios, responde un error por cada campo y no guarda nada<br>[`RegistrarEmpresa.test.jsx`](../frontend/src/empresa/RegistrarEmpresa.test.jsx) · si faltan datos obligatorios, el formulario marca los campos que faltan y no envía nada |
| C4. Si el dígito de verificación del NIT no corresponde, el formulario lo marca y no registra la empresa. | [`RegistroEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/RegistroEmpresaIT.java) · si el dígito de verificación del NIT no corresponde, lo marca y no registra la empresa<br>[`DigitoVerificacionNitTest`](../backend/src/test/java/co/edu/usco/inspectia/empresa/reglas/DigitoVerificacionNitTest.java) · acepta un NIT con su dígito de verificación correcto (parametrizada)<br>[`DigitoVerificacionNitTest`](../backend/src/test/java/co/edu/usco/inspectia/empresa/reglas/DigitoVerificacionNitTest.java) · calcula el dígito de verificación con el algoritmo de la DIAN (parametrizada)<br>[`DigitoVerificacionNitTest`](../backend/src/test/java/co/edu/usco/inspectia/empresa/reglas/DigitoVerificacionNitTest.java) · rechaza un NIT cuyo dígito de verificación no corresponde (parametrizada)<br>[`DigitoVerificacionNitTest`](../backend/src/test/java/co/edu/usco/inspectia/empresa/reglas/DigitoVerificacionNitTest.java) · rechaza un NIT sin dígito de verificación o con otro formato (parametrizada)<br>[`RegistrarEmpresa.test.jsx`](../frontend/src/empresa/RegistrarEmpresa.test.jsx) · si el dígito de verificación del NIT no corresponde, el formulario lo marca y no registra la empresa |
| C5. La contraseña inicial acepta de 8 a 64 caracteres: con 7 o con 65 se rechaza (prueba de límite). | [`RegistroEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/RegistroEmpresaIT.java) · la contraseña inicial acepta de 8 a 64 caracteres (prueba de límite) (parametrizada)<br>[`RegistrarEmpresa.test.jsx`](../frontend/src/empresa/RegistrarEmpresa.test.jsx) · la contraseña inicial acepta de 8 a 64 caracteres: con 7 o con 65 se rechaza (prueba de límite): %s (parametrizada) |
| C6. Una contraseña inicial de 64 caracteres con 8 eñes (72 bytes) se acepta; con 9 eñes (73 bytes) se rechaza con el mensaje «La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes» (prueba de límite). | [`RegistroEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/RegistroEmpresaIT.java) · la contraseña inicial ocupa como máximo 72 bytes: con 8 eñes se acepta y con 9 se rechaza con el mensaje de la ficha (prueba de límite) (parametrizada)<br>[`RegistrarEmpresa.test.jsx`](../frontend/src/empresa/RegistrarEmpresa.test.jsx) · una contraseña inicial de 64 caracteres con 8 eñes se acepta; con 9 eñes se rechaza con su mensaje (prueba de límite): %s (parametrizada) |
| C7. La empresa nueva queda con los rangos por defecto de D-10. | [`RegistroEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/RegistroEmpresaIT.java) · la empresa nueva queda con los rangos por defecto de D-10 |
| C8. El administrador creado puede iniciar sesión con la contraseña inicial. | [`RegistroEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/RegistroEmpresaIT.java) · el administrador queda activo, en una empresa activa y con la contraseña inicial (el inicio de sesión se prueba en RF-01)<br>[`InicioSesionIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/InicioSesionIT.java) · el administrador creado con la empresa inicia sesión con su contraseña inicial<br>[`empresas.spec.js`](../frontend/e2e/empresas.spec.js) · la empresa registrada en la pantalla queda con su administrador, que entra con la contraseña inicial (de punta a punta) |
| C9. Al desactivar una empresa, sus usuarios no pueden iniciar sesión y sus lotes y resultados se conservan (D-27). | [`EdicionEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/EdicionEmpresaIT.java) · al desactivar una empresa no se borra nada: sus usuarios, rangos y lotes se conservan<br>[`InicioSesionIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/InicioSesionIT.java) · al desactivar una empresa, sus usuarios no pueden iniciar sesión<br>[`empresas.spec.js`](../frontend/e2e/empresas.spec.js) · al desactivar una empresa en la pantalla, después de confirmarlo, su administrador ya no puede iniciar sesión (de punta a punta) |
| C10. Al reactivar una empresa, sus usuarios activos pueden volver a iniciar sesión (D-27). | [`EdicionEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/EdicionEmpresaIT.java) · al reactivar una empresa queda activa otra vez y cada usuario conserva su estado<br>[`InicioSesionIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/InicioSesionIT.java) · al reactivar una empresa, sus usuarios activos vuelven a iniciar sesión y los desactivados no<br>[`empresas.spec.js`](../frontend/e2e/empresas.spec.js) · al reactivar una empresa en la pantalla, su administrador vuelve a iniciar sesión (de punta a punta) |
| C11. Al editar una empresa, el NIT pasa por las mismas validaciones del registro: único y con dígito de verificación válido (D-27). | [`EdicionEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/EdicionEmpresaIT.java) · al editar, la empresa puede conservar su propio NIT y guardar los demás cambios<br>[`EdicionEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/EdicionEmpresaIT.java) · al editar, un NIT con dígito de verificación inválido responde 400 y no cambia nada<br>[`EdicionEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/EdicionEmpresaIT.java) · al editar, un NIT de otra empresa responde 409 y no cambia nada<br>[`RegistrarEmpresa.test.jsx`](../frontend/src/empresa/RegistrarEmpresa.test.jsx) · al editar una empresa, el NIT pasa por las mismas validaciones del registro: su formato, su dígito y que no se repita |
| C12. El administrador de la plataforma puede restablecer la contraseña de un administrador de empresa; la nueva contraseña pasa por las mismas validaciones de la contraseña inicial. | [`RestablecerContrasenaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/administradores/RestablecerContrasenaIT.java) · el administrador de la plataforma restablece la contraseña de un administrador de empresa: entra con la nueva y no con la anterior<br>[`RestablecerContrasenaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/administradores/RestablecerContrasenaIT.java) · la contraseña nueva pasa por las mismas validaciones de la inicial (prueba de límite) (parametrizada)<br>[`RestablecerContrasenaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/administradores/RestablecerContrasenaIT.java) · una contraseña nueva de solo espacios se rechaza como si faltara, igual que la inicial; con una letra se acepta (prueba de límite) (parametrizada)<br>[`empresas.spec.js`](../frontend/e2e/empresas.spec.js) · el administrador de la plataforma restablece en la pantalla la contraseña del administrador de una empresa: la nueva entra y la anterior ya no (de punta a punta)<br>[`PanelAdministradores.test.jsx`](../frontend/src/empresa/PanelAdministradores.test.jsx) · antes de restablecer, el panel dice que se cierran sus sesiones abiertas y se le quita el bloqueo (D-30)<br>[`PanelAdministradores.test.jsx`](../frontend/src/empresa/PanelAdministradores.test.jsx) · el administrador de la plataforma ve los administradores de una empresa y restablece la contraseña de uno<br>[`PanelAdministradores.test.jsx`](../frontend/src/empresa/PanelAdministradores.test.jsx) · la contraseña nueva pasa por las mismas validaciones de la contraseña inicial (prueba de límite): %s (parametrizada) |

Otras pruebas de RF-02, que no cubren un criterio:

- [`EmpresaApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/empresa/EmpresaApiGeneradaIT.java) · cada validación del JSON de empresa acepta su valor del borde al crear (parametrizada, generada)
- [`EmpresaApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/empresa/EmpresaApiGeneradaIT.java) · cada validación del JSON de empresa acepta su valor del borde al editar (parametrizada, generada)
- [`EmpresaApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/empresa/EmpresaApiGeneradaIT.java) · cada validación del JSON de empresa responde 400 con su mensaje al crear (parametrizada, generada)
- [`EmpresaApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/empresa/EmpresaApiGeneradaIT.java) · cada validación del JSON de empresa responde 400 con su mensaje al editar (parametrizada, generada)
- [`EmpresaApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/empresa/EmpresaApiGeneradaIT.java) · el listado de empresa filtra por activo: con true trae solo los activos y con false, los desactivados (generada)
- [`EmpresaApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/empresa/EmpresaApiGeneradaIT.java) · empresa guarda sin espacios al inicio ni al final los textos que su JSON pide recortar (generada)
- [`EmpresaApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/empresa/EmpresaApiGeneradaIT.java) · los datos únicos del JSON de empresa no se repiten: responde 409 (generada)
- [`EdicionEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/EdicionEmpresaIT.java) · ver, editar, desactivar o reactivar una empresa que no existe responde 404 (parametrizada)
- [`EmpresaServicioTest`](../backend/src/test/java/co/edu/usco/inspectia/empresa/EmpresaServicioTest.java) · al crear, guarda la empresa, su administrador con la contraseña codificada y los rangos, en ese orden
- [`EmpresaServicioTest`](../backend/src/test/java/co/edu/usco/inspectia/empresa/EmpresaServicioTest.java) · al crear, si la regla del NIT no se cumple, el servicio no guarda nada
- [`EmpresaServicioTest`](../backend/src/test/java/co/edu/usco/inspectia/empresa/EmpresaServicioTest.java) · al editar, revisa la regla con los datos nuevos y, si no se cumple, no cambia la empresa
- [`EmpresaServicioTest`](../backend/src/test/java/co/edu/usco/inspectia/empresa/EmpresaServicioTest.java) · crear guarda la empresa, su administrador y sus rangos en una sola transacción
- [`RegistroEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/RegistroEmpresaIT.java) · la empresa registrada aparece en el listado
- [`RestablecerContrasenaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/administradores/RestablecerContrasenaIT.java) · el administrador de la plataforma ve los administradores de una empresa, por nombre y sin sus contraseñas
- [`RestablecerContrasenaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/administradores/RestablecerContrasenaIT.java) · restablecer la contraseña de un administrador de empresa desbloquea su cuenta
- [`RestablecerContrasenaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/administradores/RestablecerContrasenaIT.java) · si el usuario no es administrador de esa empresa, la API responde que no existe y no cambia nada
- [`RestablecerContrasenaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/administradores/RestablecerContrasenaIT.java) · también restablece la contraseña de un administrador desactivado o de una empresa desactivada, sin reactivarlos
- [`PanelAdministradores.test.jsx`](../frontend/src/empresa/PanelAdministradores.test.jsx) · un error de la API al restablecer se ve en el campo, o arriba del formulario si no es de un campo
- [`PanelAdministradores.test.jsx`](../frontend/src/empresa/PanelAdministradores.test.jsx) · «Cerrar» cierra el panel de administradores y la lista de empresas sigue a la vista
- [`EmpresaPantalla.test.jsx`](../frontend/src/generated/empresa/EmpresaPantalla.test.jsx) · desactivar empresa pide confirmación, y reactivarla no (generada)
- [`EmpresaPantalla.test.jsx`](../frontend/src/generated/empresa/EmpresaPantalla.test.jsx) · la pantalla de empresa edita con los datos del formulario (generada)
- [`EmpresaPantalla.test.jsx`](../frontend/src/generated/empresa/EmpresaPantalla.test.jsx) · la pantalla de empresa muestra sus registros y su resumen (generada)
- [`EmpresaPantalla.test.jsx`](../frontend/src/generated/empresa/EmpresaPantalla.test.jsx) · la pantalla de empresa registra con los datos del formulario (generada)
- [`EmpresaPantalla.test.jsx`](../frontend/src/generated/empresa/EmpresaPantalla.test.jsx) · lo que solo revisa el backend de empresa llega a su campo al guardar (generada)
- [`EmpresaPantalla.test.jsx`](../frontend/src/generated/empresa/EmpresaPantalla.test.jsx) · sin registros, la pantalla de empresa lo dice (generada)
- [`EmpresaPantalla.test.jsx`](../frontend/src/generated/empresa/EmpresaPantalla.test.jsx) · un dato con HTML se ve como texto en la pantalla de empresa: no se interpreta (generada)
- [`EmpresaPantalla.test.jsx`](../frontend/src/generated/empresa/EmpresaPantalla.test.jsx) · un dato repetido de empresa se marca en su campo (409) (generada)
- [`empresaApi.test.js`](../frontend/src/generated/empresa/empresaApi.test.js) · el cliente de la API de empresa usa la ruta y el método de cada operación del JSON (generada)
- [`empresaApi.test.js`](../frontend/src/generated/empresa/empresaApi.test.js) · el listado de empresa pide la página, el tamaño y, si se pide, el filtro activo (D-33) (generada)
- [`empresaEsquemas.test.js`](../frontend/src/generated/empresa/empresaEsquemas.test.js) · cada validación del JSON de empresa acepta al editar su valor del borde, como la API: %s (parametrizada, generada)
- [`empresaEsquemas.test.js`](../frontend/src/generated/empresa/empresaEsquemas.test.js) · cada validación del JSON de empresa acepta al registrar su valor del borde, como la API: %s (parametrizada, generada)
- [`empresaEsquemas.test.js`](../frontend/src/generated/empresa/empresaEsquemas.test.js) · cada validación del JSON de empresa rechaza al editar lo mismo que la API, con su mensaje: %s (parametrizada, generada)
- [`empresaEsquemas.test.js`](../frontend/src/generated/empresa/empresaEsquemas.test.js) · cada validación del JSON de empresa rechaza al registrar lo mismo que la API, con su mensaje: %s (parametrizada, generada)
- [`empresaEsquemas.test.js`](../frontend/src/generated/empresa/empresaEsquemas.test.js) · los ejemplos del JSON de empresa cumplen las validaciones del formulario (generada)
- [`empresaEsquemas.test.js`](../frontend/src/generated/empresa/empresaEsquemas.test.js) · los mensajes de las validaciones de empresa salen en el idioma de la app (generada)

## RF-03 · Gestionar los usuarios de la empresa

| Criterio | Pruebas |
|---|---|
| C1. Un usuario creado con datos válidos puede iniciar sesión. | [`GestionUsuariosIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/GestionUsuariosIT.java) · un usuario creado con datos válidos puede iniciar sesión, con su rol y la empresa del administrador<br>[`usuarios.spec.js`](../frontend/e2e/usuarios.spec.js) · el usuario creado en la pantalla queda activo, con el rol elegido, e inicia sesión con su contraseña inicial (de punta a punta)<br>[`flujos.test.js`](../frontend/selenium/flujos.test.js) · el administrador de empresa crea un usuario en la pantalla y ese usuario inicia sesión (Selenium)<br>[`GestionarUsuarios.test.jsx`](../frontend/src/usuario/GestionarUsuarios.test.jsx) · con datos válidos, la pantalla crea el usuario con el rol elegido y lo muestra activo en la lista |
| C2. No se puede crear un usuario con un correo que ya existe en la plataforma. | [`GestionUsuariosIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/GestionUsuariosIT.java) · no se puede crear un usuario con un correo que ya existe en la plataforma, aunque sea de otra empresa o cambie en mayúsculas<br>[`usuarios.spec.js`](../frontend/e2e/usuarios.spec.js) · la pantalla no crea un usuario con el correo de alguien de otra empresa: lo avisa en el campo Correo (de punta a punta)<br>[`GestionarUsuarios.test.jsx`](../frontend/src/usuario/GestionarUsuarios.test.jsx) · si el correo ya existe en la plataforma, no crea el usuario y lo avisa en el campo Correo |
| C3. La contraseña acepta de 8 a 64 caracteres: con 7 o con 65 se rechaza (prueba de límite). | [`GestionUsuariosIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/GestionUsuariosIT.java) · la contraseña acepta de 8 a 64 caracteres: con 7 o con 65 se rechaza (prueba de límite) (parametrizada)<br>[`usuarios.spec.js`](../frontend/e2e/usuarios.spec.js) · la contraseña inicial con 7 o con 65 caracteres no se guarda; con 64 sí, y el usuario entra con ella (prueba de límite, de punta a punta)<br>[`GestionarUsuarios.test.jsx`](../frontend/src/usuario/GestionarUsuarios.test.jsx) · al restablecer la contraseña de otra persona, la nueva acepta de 8 a 64 caracteres: con 7 o con 65 se rechaza (prueba de límite): %s (parametrizada)<br>[`GestionarUsuarios.test.jsx`](../frontend/src/usuario/GestionarUsuarios.test.jsx) · la contraseña inicial acepta de 8 a 64 caracteres: con 7 o con 65 se rechaza (prueba de límite): %s (parametrizada) |
| C4. Una contraseña de 64 caracteres con 8 eñes (72 bytes) se acepta; con 9 eñes (73 bytes) se rechaza con el mensaje «La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes» (prueba de límite). | [`GestionUsuariosIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/GestionUsuariosIT.java) · una contraseña de 64 caracteres con 8 eñes (72 bytes) se acepta y con 9 (73 bytes) se rechaza con su mensaje (prueba de límite) (parametrizada)<br>[`usuarios.spec.js`](../frontend/e2e/usuarios.spec.js) · al restablecer en la pantalla la contraseña de otra persona, con 9 eñes no se guarda; con 8 eñes (72 bytes) sí, y esa persona entra con la nueva y ya no con la anterior (prueba de límite, de punta a punta)<br>[`usuarios.spec.js`](../frontend/e2e/usuarios.spec.js) · una contraseña inicial de 64 caracteres con 9 eñes no se guarda; con 8 eñes (72 bytes) sí, y el usuario entra con ella (prueba de límite, de punta a punta)<br>[`GestionarUsuarios.test.jsx`](../frontend/src/usuario/GestionarUsuarios.test.jsx) · al restablecer la contraseña de otra persona, con 8 eñes en 64 caracteres se acepta; con 9 eñes se rechaza con su mensaje (prueba de límite): %s (parametrizada)<br>[`GestionarUsuarios.test.jsx`](../frontend/src/usuario/GestionarUsuarios.test.jsx) · una contraseña inicial de 64 caracteres con 8 eñes se acepta; con 9 eñes se rechaza con su mensaje (prueba de límite): %s (parametrizada) |
| C5. Con 25 usuarios activos, el sistema no deja crear ni reactivar otro, y explica por qué. | [`GestionUsuariosIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/GestionUsuariosIT.java) · con 24 activos, si dos administradores crean o reactivan un usuario al mismo tiempo, solo uno lo logra y la empresa queda con 25 (prueba de límite) (parametrizada)<br>[`GestionUsuariosIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/GestionUsuariosIT.java) · con 25 usuarios activos no deja crear ni reactivar otro, y explica por qué (prueba de límite)<br>[`ReglasUsuarioImplTest`](../backend/src/test/java/co/edu/usco/inspectia/usuario/reglas/ReglasUsuarioImplTest.java) · antes de contar los activos bloquea la fila de la empresa, para que dos altas a la vez no pasen de 25<br>[`ReglasUsuarioImplTest`](../backend/src/test/java/co/edu/usco/inspectia/usuario/reglas/ReglasUsuarioImplTest.java) · con 24 activos en la empresa se puede crear o reactivar otro; con 25, no (prueba de límite) (parametrizada)<br>[`usuarios.spec.js`](../frontend/e2e/usuarios.spec.js) · al llegar a 25 usuarios activos, la pantalla no deja crear ni reactivar otro, y explica por qué (prueba de límite, de punta a punta)<br>[`GestionarUsuarios.test.jsx`](../frontend/src/usuario/GestionarUsuarios.test.jsx) · al crear el usuario 25, la lista se vuelve a pedir y ya no deja crear otro<br>[`GestionarUsuarios.test.jsx`](../frontend/src/usuario/GestionarUsuarios.test.jsx) · con 24 usuarios activos todavía se puede crear o reactivar uno, sin aviso (prueba de límite)<br>[`GestionarUsuarios.test.jsx`](../frontend/src/usuario/GestionarUsuarios.test.jsx) · con 25 usuarios activos no deja crear ni reactivar otro, y un aviso que no interrumpe explica por qué (prueba de límite)<br>[`GestionarUsuarios.test.jsx`](../frontend/src/usuario/GestionarUsuarios.test.jsx) · si al crear la API responde que la empresa ya tiene 25 activos, el formulario muestra el mensaje de la regla y sigue abierto<br>[`GestionarUsuarios.test.jsx`](../frontend/src/usuario/GestionarUsuarios.test.jsx) · si al reactivar la API responde que la empresa ya tiene 25 activos, la pantalla muestra el mensaje de la regla y la fila sigue desactivada |
| C6. Un usuario desactivado no puede iniciar sesión y sus análisis siguen en el historial. | [`GestionUsuariosIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/GestionUsuariosIT.java) · un usuario desactivado no puede iniciar sesión<br>[`usuarios.spec.js`](../frontend/e2e/usuarios.spec.js) · al desactivar un usuario en la pantalla, ya no puede iniciar sesión (de punta a punta) |
| C7. El administrador no puede desactivarse a sí mismo. | [`GestionUsuariosIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/GestionUsuariosIT.java) · el administrador no puede desactivarse a sí mismo<br>[`ReglasUsuarioImplTest`](../backend/src/test/java/co/edu/usco/inspectia/usuario/reglas/ReglasUsuarioImplTest.java) · el administrador no puede desactivarse a sí mismo; a otro usuario sí<br>[`usuarios.spec.js`](../frontend/e2e/usuarios.spec.js) · la fila del administrador lleva «Tú» y no tiene «Desactivar»; la de otro usuario activo sí lo tiene (de punta a punta)<br>[`GestionarUsuarios.test.jsx`](../frontend/src/usuario/GestionarUsuarios.test.jsx) · en el celular, la tarjeta propia lleva «Tú» y se puede editar, pero no tiene «Desactivar»; las demás activas sí<br>[`GestionarUsuarios.test.jsx`](../frontend/src/usuario/GestionarUsuarios.test.jsx) · la API también revisa que nadie se desactive a sí mismo: si responde con esa regla, la pantalla muestra su mensaje y la fila sigue activa |
| C8. El administrador no puede cambiar su propio rol: si lo intenta, el sistema no guarda el cambio y explica por qué. | [`GestionUsuariosIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/GestionUsuariosIT.java) · el administrador no puede cambiar su propio rol: no guarda el cambio y explica por qué<br>[`ReglasUsuarioImplTest`](../backend/src/test/java/co/edu/usco/inspectia/usuario/reglas/ReglasUsuarioImplTest.java) · el administrador no puede cambiar su propio rol; sí dejarlo igual o cambiar el de otro<br>[`usuarios.spec.js`](../frontend/e2e/usuarios.spec.js) · al editar su propia fila, el rol está deshabilitado y explica por qué, y al guardar sigue igual (de punta a punta)<br>[`GestionarUsuarios.test.jsx`](../frontend/src/usuario/GestionarUsuarios.test.jsx) · al editar su propia fila, el rol se ve deshabilitado, con su rol y el mensaje de la regla, y se guarda sin cambios<br>[`GestionarUsuarios.test.jsx`](../frontend/src/usuario/GestionarUsuarios.test.jsx) · si la API rechaza el rol al guardar la fila propia, el mensaje de la regla queda en el campo Rol y no se guarda |
| C9. No puede ver ni editar usuarios de otra empresa. | [`UsuariosPorEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/UsuariosPorEmpresaIT.java) · el administrador no ve ni modifica usuarios de otra empresa: la API responde que no existen<br>[`usuarios.spec.js`](../frontend/e2e/usuarios.spec.js) · la lista solo tiene los usuarios de su empresa: no aparecen los de otra (de punta a punta) |
| C10. Si un administrador de empresa envía a la API un usuario con rol ADMIN_PLATAFORMA, la API responde «prohibido» (403) y no guarda nada (prueba de seguridad, RP-12). | [`GestionUsuariosIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/GestionUsuariosIT.java) · el 403 del rol va antes que los demás errores de los datos<br>[`GestionUsuariosIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/GestionUsuariosIT.java) · si el administrador envía un usuario con rol ADMIN_PLATAFORMA, la API responde 403 y no guarda nada (prueba de seguridad)<br>[`usuarios.spec.js`](../frontend/e2e/usuarios.spec.js) · el formulario solo ofrece Usuario final y Administrador de empresa, y la API responde «prohibido» (403) al rol ADMIN_PLATAFORMA sin guardar nada (prueba de seguridad, de punta a punta)<br>[`GestionarUsuarios.test.jsx`](../frontend/src/usuario/GestionarUsuarios.test.jsx) · el formulario solo ofrece los roles Usuario final y Administrador de empresa, al crear y al editar<br>[`GestionarUsuarios.test.jsx`](../frontend/src/usuario/GestionarUsuarios.test.jsx) · si la API responde «prohibido» (403) por el rol, su mensaje queda en el campo Rol y no se crea el usuario |
| C11. El administrador no puede cambiar su propia contraseña desde la gestión de usuarios: para eso está «Cambiar contraseña» (RF-18). | [`GestionUsuariosIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/GestionUsuariosIT.java) · el administrador no puede cambiar su propia contraseña desde la gestión de usuarios: para eso está «Cambiar contraseña»<br>[`ReglasUsuarioImplTest`](../backend/src/test/java/co/edu/usco/inspectia/usuario/reglas/ReglasUsuarioImplTest.java) · el administrador no puede cambiar su propia contraseña desde la gestión de usuarios; la de otro sí<br>[`usuarios.spec.js`](../frontend/e2e/usuarios.spec.js) · al editar su propia fila no aparece la contraseña: el mensaje de la regla lleva a «Cambiar contraseña» y su enlace la abre (de punta a punta)<br>[`GestionarUsuarios.test.jsx`](../frontend/src/usuario/GestionarUsuarios.test.jsx) · al editar su propia fila no aparece la contraseña: el mensaje de la regla lleva a «Cambiar contraseña», con el enlace en su nombre<br>[`GestionarUsuarios.test.jsx`](../frontend/src/usuario/GestionarUsuarios.test.jsx) · en inglés, el enlace del mensaje también va en el nombre de la página, «Change password» |

Otras pruebas de RF-03, que no cubren un criterio:

- [`UsuarioApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/usuario/UsuarioApiGeneradaIT.java) · cada validación del JSON de usuario acepta su valor del borde al crear (parametrizada, generada)
- [`UsuarioApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/usuario/UsuarioApiGeneradaIT.java) · cada validación del JSON de usuario acepta su valor del borde al editar (parametrizada, generada)
- [`UsuarioApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/usuario/UsuarioApiGeneradaIT.java) · cada validación del JSON de usuario responde 400 con su mensaje al crear (parametrizada, generada)
- [`UsuarioApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/usuario/UsuarioApiGeneradaIT.java) · cada validación del JSON de usuario responde 400 con su mensaje al editar (parametrizada, generada)
- [`UsuarioApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/usuario/UsuarioApiGeneradaIT.java) · el listado de usuario filtra por activo: con true trae solo los activos y con false, los desactivados (generada)
- [`UsuarioApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/usuario/UsuarioApiGeneradaIT.java) · los datos únicos del JSON de usuario no se repiten: responde 409 (generada)
- [`UsuarioApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/usuario/UsuarioApiGeneradaIT.java) · usuario guarda sin espacios al inicio ni al final los textos que su JSON pide recortar (generada)
- [`NoEnBlancoValidadorTest`](../backend/src/test/java/co/edu/usco/inspectia/comun/validacion/NoEnBlancoValidadorTest.java) · @NoEnBlanco deja no enviar el dato, pero si llega no puede estar vacío ni tener solo espacios (prueba de límite) (parametrizada)
- [`NoEnBlancoValidadorTest`](../backend/src/test/java/co/edu/usco/inspectia/comun/validacion/NoEnBlancoValidadorTest.java) · @NoEnBlanco también rechaza un texto con solo tabuladores o saltos de línea, con el mensaje de un dato obligatorio (parametrizada)
- [`ValorPermitidoValidadorTest`](../backend/src/test/java/co/edu/usco/inspectia/comun/validacion/ValorPermitidoValidadorTest.java) · @ValorPermitido solo deja los valores de valores_en_formulario; sin valor no opina (parametrizada)
- [`CambiosALaVezIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/CambiosALaVezIT.java) · desactivar a un usuario mientras cambia su contraseña (RF-18) no lo reactiva: la contraseña cambia y el usuario sigue desactivado
- [`CambiosALaVezIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/CambiosALaVezIT.java) · editar el nombre de un administrador mientras se restablece su contraseña (RF-02 C12) no la deshace: quedan la contraseña nueva y el nombre nuevo, y su sesión de antes sigue cortada
- [`GestionUsuariosIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/GestionUsuariosIT.java) · al reactivar un usuario, vuelve a iniciar sesión
- [`GestionUsuariosIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/GestionUsuariosIT.java) · restablecer la contraseña de un usuario desbloquea su cuenta
- [`GestionUsuariosIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/GestionUsuariosIT.java) · un rol que no existe responde 400 con el campo rol
- [`GestionUsuariosIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/GestionUsuariosIT.java) · una contraseña de solo espacios se rechaza al crear y al restablecer, con el mismo mensaje que si faltara; con una letra se acepta (prueba de límite) (parametrizada)
- [`ReglasUsuarioImplTest`](../backend/src/test/java/co/edu/usco/inspectia/usuario/reglas/ReglasUsuarioImplTest.java) · reactivar a un usuario que ya está activo no suma otro: cumple sin contar
- [`usuarios.spec.js`](../frontend/e2e/usuarios.spec.js) · al reactivar un usuario en la pantalla, vuelve a iniciar sesión (de punta a punta)
- [`usuarios.spec.js`](../frontend/e2e/usuarios.spec.js) · el formulario va al lado de la lista en el PC y encima de ella en el celular (de punta a punta)
- [`usuarios.spec.js`](../frontend/e2e/usuarios.spec.js) · en una ventana de PC de 1250 px de ancho, el formulario también va al lado de la lista (de punta a punta)
- [`UsuarioPantalla.test.jsx`](../frontend/src/generated/usuario/UsuarioPantalla.test.jsx) · al editar a otra persona, la nota de la contraseña aparece al escribir una nueva (generada)
- [`UsuarioPantalla.test.jsx`](../frontend/src/generated/usuario/UsuarioPantalla.test.jsx) · al editar la fila propia, los campos que impiden sus reglas no se cambian y muestran el mensaje de la regla (generada)
- [`UsuarioPantalla.test.jsx`](../frontend/src/generated/usuario/UsuarioPantalla.test.jsx) · al editar sin escribir una contraseña, no la envía y queda la que había (generada)
- [`UsuarioPantalla.test.jsx`](../frontend/src/generated/usuario/UsuarioPantalla.test.jsx) · al llegar al máximo de activos de su regla, no deja registrar ni reactivar y muestra el mensaje de la regla (generada)
- [`UsuarioPantalla.test.jsx`](../frontend/src/generated/usuario/UsuarioPantalla.test.jsx) · desactivar usuario no pide confirmación, y reactivarla no (generada)
- [`UsuarioPantalla.test.jsx`](../frontend/src/generated/usuario/UsuarioPantalla.test.jsx) · el formulario para registrar empieza con los valores iniciales del JSON (generada)
- [`UsuarioPantalla.test.jsx`](../frontend/src/generated/usuario/UsuarioPantalla.test.jsx) · la fila de quien tiene la sesión lleva su marca y no tiene las acciones que impiden sus reglas (generada)
- [`UsuarioPantalla.test.jsx`](../frontend/src/generated/usuario/UsuarioPantalla.test.jsx) · la pantalla de usuario edita con los datos del formulario (generada)
- [`UsuarioPantalla.test.jsx`](../frontend/src/generated/usuario/UsuarioPantalla.test.jsx) · la pantalla de usuario muestra sus registros y su resumen (generada)
- [`UsuarioPantalla.test.jsx`](../frontend/src/generated/usuario/UsuarioPantalla.test.jsx) · la pantalla de usuario registra con los datos del formulario (generada)
- [`UsuarioPantalla.test.jsx`](../frontend/src/generated/usuario/UsuarioPantalla.test.jsx) · lo que solo revisa el backend de usuario llega a su campo al guardar (generada)
- [`UsuarioPantalla.test.jsx`](../frontend/src/generated/usuario/UsuarioPantalla.test.jsx) · sin registros, la pantalla de usuario lo dice (generada)
- [`UsuarioPantalla.test.jsx`](../frontend/src/generated/usuario/UsuarioPantalla.test.jsx) · un dato con HTML se ve como texto en la pantalla de usuario: no se interpreta (generada)
- [`UsuarioPantalla.test.jsx`](../frontend/src/generated/usuario/UsuarioPantalla.test.jsx) · un dato repetido de usuario se marca en su campo (409) (generada)
- [`usuarioApi.test.js`](../frontend/src/generated/usuario/usuarioApi.test.js) · el cliente de la API de usuario usa la ruta y el método de cada operación del JSON (generada)
- [`usuarioApi.test.js`](../frontend/src/generated/usuario/usuarioApi.test.js) · el listado de usuario pide la página, el tamaño y, si se pide, el filtro activo (D-33) (generada)
- [`usuarioEsquemas.test.js`](../frontend/src/generated/usuario/usuarioEsquemas.test.js) · cada validación del JSON de usuario acepta al editar su valor del borde, como la API: %s (parametrizada, generada)
- [`usuarioEsquemas.test.js`](../frontend/src/generated/usuario/usuarioEsquemas.test.js) · cada validación del JSON de usuario acepta al registrar su valor del borde, como la API: %s (parametrizada, generada)
- [`usuarioEsquemas.test.js`](../frontend/src/generated/usuario/usuarioEsquemas.test.js) · cada validación del JSON de usuario rechaza al editar lo mismo que la API, con su mensaje: %s (parametrizada, generada)
- [`usuarioEsquemas.test.js`](../frontend/src/generated/usuario/usuarioEsquemas.test.js) · cada validación del JSON de usuario rechaza al registrar lo mismo que la API, con su mensaje: %s (parametrizada, generada)
- [`usuarioEsquemas.test.js`](../frontend/src/generated/usuario/usuarioEsquemas.test.js) · los ejemplos del JSON de usuario cumplen las validaciones del formulario (generada)
- [`usuarioEsquemas.test.js`](../frontend/src/generated/usuario/usuarioEsquemas.test.js) · los mensajes de las validaciones de usuario salen en el idioma de la app (generada)
- [`GestionarUsuarios.test.jsx`](../frontend/src/usuario/GestionarUsuarios.test.jsx) · al restablecer la contraseña de otra persona, la nota de D-30 aparece al escribirla, debajo de los campos, y se va si se borra
- [`GestionarUsuarios.test.jsx`](../frontend/src/usuario/GestionarUsuarios.test.jsx) · el administrador de la empresa abre «Usuarios» desde el menú y ve los usuarios de su empresa, con los activos contra el máximo
- [`GestionarUsuarios.test.jsx`](../frontend/src/usuario/GestionarUsuarios.test.jsx) · la nota de D-30 no aparece al crear un usuario, que no tiene sesiones, ni en la fila propia, que no cambia su contraseña aquí

## RF-04 · Registrar un lote

| Criterio | Pruebas |
|---|---|
| C1. Con datos válidos, el lote queda registrado y el sistema pasa a la captura de fotos. | [`RegistroDeLotesIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/RegistroDeLotesIT.java) · con datos válidos, el lote queda registrado como pendiente de análisis, con la empresa y el usuario de la sesión<br>[`LoteTest`](../backend/src/test/java/co/edu/usco/inspectia/analisis/dominio/LoteTest.java) · un lote recién registrado queda pendiente de análisis, con la empresa y el usuario que lo registra<br>[`lotes.spec.js`](../frontend/e2e/lotes.spec.js) · con datos válidos el lote queda registrado, la pantalla pasa a las fotos y el historial lo muestra pendiente (de punta a punta)<br>[`flujos.test.js`](../frontend/selenium/flujos.test.js) · el usuario final registra un lote en la pantalla, pasa a las fotos y lo ve pendiente en el historial (Selenium)<br>[`NuevoLote.test.jsx`](../frontend/src/lote/NuevoLote.test.jsx) · con datos válidos registra el lote y pasa a las fotos de la muestra |
| C2. Si el código ya existe en la empresa, el sistema lo avisa y no registra el lote. | [`RegistroDeLotesIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/RegistroDeLotesIT.java) · si el código ya existe en la empresa, el sistema lo avisa y no registra el lote, aunque lo registre otro usuario<br>[`lotes.spec.js`](../frontend/e2e/lotes.spec.js) · si el código ya existe en la empresa, la pantalla lo avisa y no registra el lote (de punta a punta)<br>[`NuevoLote.test.jsx`](../frontend/src/lote/NuevoLote.test.jsx) · si el código ya existe en la empresa, lo marca con el aviso del prototipo y no pasa a las fotos |
| C3. El código «l-001 » cuenta como repetido si ya existe «L-001». | [`EsquemaIT`](../backend/src/test/java/co/edu/usco/inspectia/EsquemaIT.java) · la base trata «l-001 » y «L-001» como el mismo código de la empresa<br>[`RegistroDeLotesIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/RegistroDeLotesIT.java) · el código «l-001 » cuenta como repetido si ya existe «L-001»<br>[`LoteTest`](../backend/src/test/java/co/edu/usco/inspectia/analisis/dominio/LoteTest.java) · «l-001 » y «L-001» tienen la misma clave de código; «L-0001» tiene otra<br>[`lotes.spec.js`](../frontend/e2e/lotes.spec.js) · el código en minúsculas y con un espacio al final cuenta como repetido (de punta a punta) |
| C4. La fecha de recepción no puede ser posterior a hoy. | [`FechaDeRecepcionIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/FechaDeRecepcionIT.java) · la fecha de recepción no puede ser posterior a hoy: con hoy en Colombia, el día siguiente se rechaza y hoy pasa (prueba de límite)<br>[`FechaDeRecepcionIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/FechaDeRecepcionIT.java) · una fecha de recepción de varias semanas después también se rechaza<br>[`LoteTest`](../backend/src/test/java/co/edu/usco/inspectia/analisis/dominio/LoteTest.java) · el dominio rechaza una fecha de recepción posterior a hoy y acepta la de hoy (prueba de límite)<br>[`lotes.spec.js`](../frontend/e2e/lotes.spec.js) · una fecha de recepción posterior a hoy en Colombia se rechaza en la pantalla, y la de hoy pasa (prueba de límite, de punta a punta)<br>[`NuevoLote.test.jsx`](../frontend/src/lote/NuevoLote.test.jsx) · la fecha de hoy en Colombia sí se acepta, aunque en UTC ya sea el día siguiente (prueba de límite)<br>[`NuevoLote.test.jsx`](../frontend/src/lote/NuevoLote.test.jsx) · una fecha de recepción posterior a hoy se marca antes de enviar, y el campo no ofrece fechas después de hoy<br>[`fechas.test.js`](../frontend/src/lote/fechas.test.js) · a las 10 de la noche del 9 de noviembre en Colombia hoy todavía es el 9, aunque en UTC ya sea el 10 (prueba de límite)<br>[`fechas.test.js`](../frontend/src/lote/fechas.test.js) · el día cambia a la medianoche de Colombia, a las 5 de la mañana en UTC (prueba de límite) |

Otras pruebas de RF-04, que no cubren un criterio:

- [`FechaDeRecepcionIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/FechaDeRecepcionIT.java) · la fecha de recepción puede ser de hace exactamente un año en Colombia, pero no del día anterior (prueba de límite)
- [`FechaDeRecepcionIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/FechaDeRecepcionIT.java) · una fecha de recepción de hace miles de años responde 400 con su mensaje y no llega a la base, que no la admite (antes respondía 500)
- [`RegistroDeLotesIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/RegistroDeLotesIT.java) · al escribir la finca o el proveedor, el sistema sugiere los que ya usó la empresa, sin distinguir mayúsculas y sin repetirlos
- [`RegistroDeLotesIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/RegistroDeLotesIT.java) · con el código de 30 caracteres, la finca o el proveedor de 120 y las observaciones de 500, el lote se registra (prueba de límite)
- [`RegistroDeLotesIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/RegistroDeLotesIT.java) · el código, la finca o el proveedor y las observaciones se guardan sin los espacios del inicio y del final; unas observaciones en blanco quedan vacías
- [`RegistroDeLotesIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/RegistroDeLotesIT.java) · el mismo código se puede usar en otra empresa
- [`RegistroDeLotesIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/RegistroDeLotesIT.java) · las sugerencias de finca o proveedor tampoco distinguen tildes: «paraiso» sugiere «Finca El Paraíso» y «unión», «Finca La Union»
- [`RegistroDeLotesIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/RegistroDeLotesIT.java) · los datos obligatorios y sus longitudes máximas: código de 30 caracteres, finca o proveedor de 120 y observaciones de 500 (parametrizada)
- [`RegistroDeLotesIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/RegistroDeLotesIT.java) · se sugiere la forma más usada de cada finca, aunque otra sea más reciente: «Finca El Paraíso», escrita bien 3 veces, gana a «finca el paraiso», escrita después una vez
- [`RegistroDeLotesIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/RegistroDeLotesIT.java) · se sugieren como máximo 10 fincas o proveedores
- [`RegistroDeLotesIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/RegistroDeLotesIT.java) · si la misma finca se escribió con tilde y sin ella el mismo número de veces, se sugiere una sola vez, con la forma más reciente
- [`RegistroDeLotesIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/RegistroDeLotesIT.java) · una fecha de recepción que no tiene el formato aaaa-mm-dd se rechaza en su campo
- [`LoteTest`](../backend/src/test/java/co/edu/usco/inspectia/analisis/dominio/LoteTest.java) · el dominio acepta una fecha de recepción de hace exactamente un año y rechaza la del día anterior (prueba de límite)
- [`LoteTest`](../backend/src/test/java/co/edu/usco/inspectia/analisis/dominio/LoteTest.java) · si hoy es 29 de febrero, hace un año es el 28 de febrero del año anterior (prueba de límite)
- [`TextosTest`](../backend/src/test/java/co/edu/usco/inspectia/comun/texto/TextosTest.java) · la clave de búsqueda no distingue mayúsculas, tildes ni los espacios del inicio y del final
- [`TextosTest`](../backend/src/test/java/co/edu/usco/inspectia/comun/texto/TextosTest.java) · sin tildes y la clave de búsqueda dejan un texto vacío como llega
- [`TextosTest`](../backend/src/test/java/co/edu/usco/inspectia/comun/texto/TextosTest.java) · sin tildes: «Paraíso» queda «Paraiso», «Peña» queda «Pena» y «Güiza» queda «Guiza»
- [`lotes.spec.js`](../frontend/e2e/lotes.spec.js) · al escribir la finca sin tilde, la pantalla sugiere la que ya usó la empresa con tilde, y al elegirla la escribe (de punta a punta)
- [`lotes.spec.js`](../frontend/e2e/lotes.spec.js) · el campo «Finca o proveedor» mide lo mismo que los demás campos, como en el prototipo, y no tiene el botón «Clear» sin traducir (de punta a punta)
- [`lotes.spec.js`](../frontend/e2e/lotes.spec.js) · una fecha de recepción de hace más de un año en Colombia se rechaza en la pantalla, y la de hace exactamente un año pasa (prueba de límite, de punta a punta)
- [`FotosDeLaMuestra.test.jsx`](../frontend/src/lote/FotosDeLaMuestra.test.jsx) · la página de las fotos nombra el lote, con su código y su finca o proveedor, y dice que quedó pendiente de análisis
- [`FotosDeLaMuestra.test.jsx`](../frontend/src/lote/FotosDeLaMuestra.test.jsx) · los pasos del análisis muestran el primero listo y el segundo en curso
- [`NuevoLote.test.jsx`](../frontend/src/lote/NuevoLote.test.jsx) · Cancelar vuelve al inicio sin registrar nada
- [`NuevoLote.test.jsx`](../frontend/src/lote/NuevoLote.test.jsx) · al escribir la finca o el proveedor sugiere los que ya usó la empresa, y al elegir uno lo escribe
- [`NuevoLote.test.jsx`](../frontend/src/lote/NuevoLote.test.jsx) · el código tiene como máximo 30 caracteres
- [`NuevoLote.test.jsx`](../frontend/src/lote/NuevoLote.test.jsx) · el código, la fecha y la finca o el proveedor son obligatorios; las observaciones no
- [`NuevoLote.test.jsx`](../frontend/src/lote/NuevoLote.test.jsx) · la fecha de recepción de hace exactamente un año en Colombia se acepta, y la del día anterior se marca antes de enviar (prueba de límite)
- [`NuevoLote.test.jsx`](../frontend/src/lote/NuevoLote.test.jsx) · los pasos del análisis muestran que el lote va en el primero
- [`fechas.test.js`](../frontend/src/lote/fechas.test.js) · hace un año es el mismo día del año anterior: la fecha de recepción más antigua que se acepta (prueba de límite)
- [`fechas.test.js`](../frontend/src/lote/fechas.test.js) · si hoy es 29 de febrero, hace un año es el 28 de febrero, como en el backend (prueba de límite)

## RF-05 · Tomar o subir las fotos de la muestra del lote, con guía de captura

| Criterio | Pruebas |
|---|---|
| C1. En el celular, el botón abre la cámara; en el PC, permite elegir archivos. | **Pendiente** |
| C2. El sistema exige al menos 2 fotos y acepta como máximo 3. | **Pendiente** |
| C3. Rechaza archivos que no sean JPG o PNG o que pasen de 10 MB, con un mensaje claro. | **Pendiente** |
| C4. La guía de captura se puede consultar antes de cada foto. | [`FotosDeLaMuestra.test.jsx`](../frontend/src/lote/FotosDeLaMuestra.test.jsx) · la guía de captura se ve en la página de las fotos, con la foto de ejemplo y sus reglas |
| C5. Cuando una foto se reemplaza, la anterior se borra de S3 y de la base de datos. | **Pendiente** |

## RF-06 · Validar la calidad de cada foto (nitidez y luz) y pedir repetirla si no sirve

| Criterio | Pruebas |
|---|---|
| C1. Una foto borrosa se rechaza con un mensaje que pide tomarla de nuevo sin mover el celular. | **Pendiente** |
| C2. Una foto oscura se rechaza con un mensaje sobre la luz. | **Pendiente** |
| C3. Una foto que cumple la guía pasa sin avisos. | **Pendiente** |

## RF-07 · Separar y recortar los granos de cada foto, con control de granos pegados

| Criterio | Pruebas |
|---|---|
| C1. En una foto con 50 granos separados, encuentra los 50. | **Pendiente** |
| C2. Una foto con granos pegados se rechaza y el sistema muestra dónde están. | **Pendiente** |
| C3. Si entre todas las fotos hay menos de 100 granos válidos, el sistema no calcula el resultado y pide completar la muestra. | **Pendiente** |
| C4. Los granos cortados por el borde de la foto no se cuentan. | **Pendiente** |
| C5. Cada recorte guarda su posición en la foto original. | **Pendiente** |

## RF-08 · Clasificar cada grano como sano o no sano con el modelo de IA

| Criterio | Pruebas |
|---|---|
| C1. Cada grano detectado recibe una sola clase: sano o no sano. | **Pendiente** |
| C2. Cada clasificación trae su confianza. | **Pendiente** |
| C3. El análisis guarda qué modelo y qué versión lo hicieron. | **Pendiente** |
| C4. El modelo cumple el F1-score mínimo de RNF-07 en el conjunto de prueba (334 granos). | **Pendiente** |
| C5. La clasificación de toda la muestra cabe dentro del tiempo total del análisis, de 5 a 8 segundos (RNF-01). | **Pendiente** |
| C6. Si el servicio de IA no responde, el sistema avisa al usuario y no guarda un resultado incompleto. | **Pendiente** |

## RF-09 · Pedir una foto más clara o más fotos cuando la confianza del modelo es baja

| Criterio | Pruebas |
|---|---|
| C1. Con más del 10 % de granos dudosos, el sistema pide repetir la foto con más granos dudosos y muestra cuáles son. | **Pendiente** |
| C2. Con 10 % o menos, el análisis sigue sin pedir nada. | **Pendiente** |
| C3. La foto repetida reemplaza a la anterior y pasa por la misma validación que las primeras. | **Pendiente** |
| C4. Si tras 2 repeticiones siguen pasando del 10 %, el resultado sale con el aviso «Confianza baja: confirma con la prueba de corte». | **Pendiente** |
| C5. Si el equipo cambia el umbral, los granos dudosos de las muestras ya analizadas no cambian. | **Pendiente** |
| C6. Si una de las 2 muestras del lote salió con aviso de confianza baja, el resultado del lote también lo tiene (D-25). | **Pendiente** |

## RF-10 · Calcular el porcentaje de granos sanos del lote

| Criterio | Pruebas |
|---|---|
| C1. Con 93 granos sanos de 100, el porcentaje contado es 93,0 %. | **Pendiente** |
| C2. Con 141 granos sanos de 150, el porcentaje contado es 94,0 %. | **Pendiente** |
| C3. Con 100 granos sanos de 107, el porcentaje contado es 93,5 % (redondeo a un decimal). | **Pendiente** |
| C4. Con 2 muestras, 107 sanos de 120 y 113 de 125 dan 220 de 245 = 89,8 % contado (D-25). | **Pendiente** |
| C5. Con 2 muestras de distinto tamaño se suman los granos: 109 sanos de 120 (90,8 %) y 120 de 150 (80,0 %) dan 229 de 270 = 84,8 % contado, no el promedio de los porcentajes (85,4 %). | **Pendiente** |
| C6. El porcentaje corregido se guarda con el lote, y cada muestra guarda sus conteos y el modelo que la analizó (D-18, D-25). | **Pendiente** |
| C7. El cálculo tiene pruebas unitarias y de mutación (RP-12) que no dependen del servicio de IA (D-21). | **Pendiente** |
| C8. Con EfficientNet-B0 (recall sana 0,912 y mala 0,534), 87 sanos de 100 (87,0 % contado) dan 90,6 % corregido (D-39). | **Pendiente** |
| C9. Con EfficientNet-B0, 93 sanos de 100 dan 100,0 % corregido: el porcentaje se limita a 100 % (sin el límite serían 104,0 %). | **Pendiente** |
| C10. Con EfficientNet-B0, 40 sanos de 100 dan 0,0 % corregido: el porcentaje se limita a 0 % (sin el límite serían −14,8 %). | **Pendiente** |
| C11. Con EfficientNet-B0 y 2 muestras, 220 sanos de 245 (89,8 % contado) dan 96,9 % corregido (D-25). | **Pendiente** |
| C12. Con EfficientNet-B0, 84 sanos de 101 (83,2 % contado) dan 82,0 % corregido: se corrige con los conteos y se redondea al final; corregir el 83,2 % ya redondeado daría 82,1 %. | **Pendiente** |
| C13. La corrección usa los recall del modelo que analizó la muestra: si cambia el modelo en uso, los lotes ya analizados conservan su porcentaje. | **Pendiente** |
| C14. Con 2 muestras de modelos distintos, 100 sanos de 120 con EfficientNet-B0 y 105 de 125 con ResNet-50 (recall sana 0,874 y mala 0,521) dan 86,7 % corregido; con los recall de EfficientNet-B0 en las dos darían 83,1 %. | **Pendiente** |

## RF-11 · Mostrar el resultado con la foto marcada (cada grano sano o no sano)

| Criterio | Pruebas |
|---|---|
| C1. Cada grano clasificado aparece marcado en la foto: verde si es sano y rojo si no lo es. | **Pendiente** |
| C2. Los granos sanos y no sanos se distinguen también por un símbolo, no solo por el color. | **Pendiente** |
| C3. Las cifras de la pantalla coinciden con el cálculo de RF-10. | **Pendiente** |
| C4. Al tocar un grano, o al pasar el cursor sobre él, se ve su clase; el valor de la confianza no se muestra al usuario final (sección 1). | **Pendiente** |
| C5. La pantalla se ve bien en celular y en PC (RNF-05). | **Pendiente** |
| C6. Junto a las fotos se ve lo que contó el modelo, por ejemplo «93 de 100 marcados como sanos», y la frase «El porcentaje corrige los errores conocidos del modelo» (D-39). | **Pendiente** |

## RF-12 · Generar la recomendación según los rangos de la empresa

| Criterio | Pruebas |
|---|---|
| C1. Con 99 %, la categoría es Premium o Especial, sin sugerencia (está a 3 puntos de 96). | **Pendiente** |
| C2. Con 93 %, la categoría es Estándar, sin sugerencia (está a 3 puntos de 90 y de 96). | **Pendiente** |
| C3. Con 90,9 %, la categoría es Estándar y sugiere analizar otra muestra (está a 0,9 puntos de 90); con 91,0 % no la sugiere (está a 1 punto). Es la prueba de límite del margen (D-24). | **Pendiente** |
| C4. Con 90,0 % la categoría es Estándar y con 89,9 % es Corriente; ambos sugieren otra muestra (prueba de límite). | **Pendiente** |
| C5. Con 75 %, la categoría es Fuera de norma, sin sugerencia. | **Pendiente** |
| C6. Eliminado (D-39): «Con 143 granos sanos de 149 (95,97 %, redondeado a 96,0 %), la categoría es Premium o Especial.» Con la corrección, esos conteos dan 100,0 %; el límite del redondeo pasa al criterio 11. | Eliminado |
| C7. Eliminado (D-39): «Con 2 muestras, la categoría sale de la suma de sus granos: 220 sanos de 245 (89,8 %) es Corriente, y no se sugiere otra muestra aunque quede a 0,2 puntos de 90 (D-25).» Con la corrección, esos conteos dan 96,9 %; lo reemplaza el criterio 12. | Eliminado |
| C8. Un lote sin sugerencia, o que ya tiene 2 muestras, no permite agregar otra (D-25). | **Pendiente** |
| C9. Si la empresa cambió sus rangos (RF-13), se usan los de la empresa. | **Pendiente** |
| C10. La categoría siempre se muestra como probable. | **Pendiente** |
| C11. Con EfficientNet-B0, 118 granos sanos de 132 (89,4 % contado) dan 95,95 % corregido, que se redondea a 96,0 %: la categoría es Premium o Especial (prueba de límite del redondeo, D-39). | **Pendiente** |
| C12. Con 2 muestras, la categoría sale de la suma de sus granos: con EfficientNet-B0, 220 sanos de 245 (96,9 % corregido) es Premium o Especial, y no se sugiere otra muestra aunque quede a 0,9 puntos de 96 (D-25). | **Pendiente** |

## RF-13 · Configurar los rangos de recomendación de la empresa

| Criterio | Pruebas |
|---|---|
| C1. Si los límites no bajan en orden (por ejemplo, Estándar 97 % y Premium 96 %), el sistema no guarda y explica el error. | [`RangosRecomendacionIT`](../backend/src/test/java/co/edu/usco/inspectia/rango/RangosRecomendacionIT.java) · los límites deben bajar estrictamente: un empate o Corriente en 0 % tampoco se guardan (parametrizada)<br>[`RangosRecomendacionIT`](../backend/src/test/java/co/edu/usco/inspectia/rango/RangosRecomendacionIT.java) · si los límites no bajan en orden (Estándar 97 % y Premium 96 %), el sistema no guarda y explica el error<br>[`ReglasRangoRecomendacionImplTest`](../backend/src/test/java/co/edu/usco/inspectia/rango/reglas/ReglasRangoRecomendacionImplTest.java) · los límites deben bajar estrictamente de Premium o Especial a Fuera de norma: un empate o un límite mayor que el anterior no cumplen (prueba de límite) (parametrizada)<br>[`rangos.spec.js`](../frontend/e2e/rangos.spec.js) · si los límites no bajan en orden (Estándar 97 % y Premium 96 %), no se guarda: el aviso lo explica y, al recargar, Estándar sigue en 90,0 (de punta a punta)<br>[`GestionarRangos.test.jsx`](../frontend/src/rango/GestionarRangos.test.jsx) · %s no baja en orden: no guarda (prueba de límite) (parametrizada)<br>[`GestionarRangos.test.jsx`](../frontend/src/rango/GestionarRangos.test.jsx) · %s sí baja en orden: guarda (prueba de límite) (parametrizada)<br>[`GestionarRangos.test.jsx`](../frontend/src/rango/GestionarRangos.test.jsx) · al no guardar por el orden, el foco va al primer límite marcado<br>[`GestionarRangos.test.jsx`](../frontend/src/rango/GestionarRangos.test.jsx) · si la API responde la regla del orden, su mensaje se ve arriba del formulario, en lugar del mensaje general<br>[`GestionarRangos.test.jsx`](../frontend/src/rango/GestionarRangos.test.jsx) · si los límites no bajan en orden (Estándar 97 % y Premium 96 %), no guarda, explica el error y marca los dos límites<br>[`limites.test.js`](../frontend/src/rango/limites.test.js) · con los límites en orden, la regla deja guardar<br>[`limites.test.js`](../frontend/src/rango/limites.test.js) · la regla del orden da el mensaje de la regla del JSON y los límites de cada par que no baja<br>[`limites.test.js`](../frontend/src/rango/limites.test.js) · los límites bajan estrictamente, como los revisa el backend: %s (parametrizada) |
| C2. Un análisis hecho después del cambio usa los rangos nuevos. | **Pendiente** |
| C3. Los análisis anteriores no cambian. | [`RangosRecomendacionIT`](../backend/src/test/java/co/edu/usco/inspectia/rango/RangosRecomendacionIT.java) · los análisis anteriores no cambian: un lote ya analizado conserva su categoría y su recomendación al editar y al restaurar los rangos |
| C4. «Restaurar valores por defecto» vuelve a los rangos de D-10. | [`RangosRecomendacionIT`](../backend/src/test/java/co/edu/usco/inspectia/rango/RangosRecomendacionIT.java) · restaurar deja lo mismo que recibe una empresa nueva (RF-02): los valores por defecto salen de una sola fuente<br>[`RangosRecomendacionIT`](../backend/src/test/java/co/edu/usco/inspectia/rango/RangosRecomendacionIT.java) · «Restaurar valores por defecto» vuelve a los rangos de D-10, también si se repite<br>[`rangos.spec.js`](../frontend/e2e/rangos.spec.js) · después de guardar unos rangos propios, «Restaurar valores por defecto» vuelve a los de D-10, también al recargar (de punta a punta)<br>[`flujos.test.js`](../frontend/selenium/flujos.test.js) · el administrador de empresa guarda unos rangos propios y «Restaurar valores por defecto» los devuelve a los de D-10 (Selenium)<br>[`GestionarRangos.test.jsx`](../frontend/src/rango/GestionarRangos.test.jsx) · al cancelar la confirmación, los rangos propios de la empresa quedan como estaban<br>[`GestionarRangos.test.jsx`](../frontend/src/rango/GestionarRangos.test.jsx) · «Restaurar valores por defecto» pide confirmación, dice qué se pierde y vuelve a los rangos de D-10 |
| C5. Un usuario final no puede entrar a esta pantalla (RNF-04). | [`RangosRecomendacionIT`](../backend/src/test/java/co/edu/usco/inspectia/rango/RangosRecomendacionIT.java) · un usuario final no puede entrar a los rangos: listar, editar y restaurar responden 403 y no cambian nada (RNF-04)<br>[`GestionarRangos.test.jsx`](../frontend/src/rango/GestionarRangos.test.jsx) · un usuario final no ve «Rangos de recomendación» en el menú<br>[`GestionarRangos.test.jsx`](../frontend/src/rango/GestionarRangos.test.jsx) · un usuario final que escribe la dirección de los rangos ve que no tiene permiso, y la pantalla no pide nada a la API |

Otras pruebas de RF-13, que no cubren un criterio:

- [`RangoRecomendacionApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/rango/RangoRecomendacionApiGeneradaIT.java) · cada validación del JSON de rango_recomendacion responde 400 con su mensaje en el campo de la fila y no guarda nada (parametrizada, generada)
- [`RangoRecomendacionApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/rango/RangoRecomendacionApiGeneradaIT.java) · limiteInferior no cambia en la fila de FUERA_DE_NORMA: otro valor responde 400 en su campo y no guarda nada (generada)
- [`RangoRecomendacionApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/rango/RangoRecomendacionApiGeneradaIT.java) · rango_recomendacion guarda sin espacios al inicio ni al final los textos que su JSON pide recortar (generada)
- [`RangoRecomendacionApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/rango/RangoRecomendacionApiGeneradaIT.java) · rango_recomendacion se edita con una fila por cada categoría: con una de menos, una de más o una repetida responde 400 en filas y no guarda nada (parametrizada, generada)
- [`RangoRecomendacionApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/rango/RangoRecomendacionApiGeneradaIT.java) · restaurar deja a la empresa una fila por cada categoría, con los valores por defecto del JSON (generada)
- [`DecimalesTest`](../backend/src/test/java/co/edu/usco/inspectia/comun/numero/DecimalesTest.java) · no redondea: un número con más decimales de los de su escala es un error, porque la validación ya debió rechazarlo
- [`DecimalesTest`](../backend/src/test/java/co/edu/usco/inspectia/comun/numero/DecimalesTest.java) · un número queda con la escala de su columna, como lo guarda la base: 90.50 como 90.5 y 100 como 100.0 (parametrizada)
- [`MaximoDecimalesValidadorTest`](../backend/src/test/java/co/edu/usco/inspectia/comun/validacion/MaximoDecimalesValidadorTest.java) · @MaximoDecimales cuenta los decimales sin los ceros del final: 90,50 vale 90,5 (prueba de límite) (parametrizada)
- [`MaximoDecimalesValidadorTest`](../backend/src/test/java/co/edu/usco/inspectia/comun/validacion/MaximoDecimalesValidadorTest.java) · @MaximoDecimales deja pasar un dato que no llegó: si es obligatorio, lo revisa @NotNull
- [`RangosRecomendacionIT`](../backend/src/test/java/co/edu/usco/inspectia/rango/RangosRecomendacionIT.java) · editar responde los límites como quedan guardados, igual que el listado: 100 como 100.0, 90.50 como 90.5 y 1E+1 como 10.0
- [`RangosRecomendacionIT`](../backend/src/test/java/co/edu/usco/inspectia/rango/RangosRecomendacionIT.java) · el administrador cambia los límites y los textos de las 4 categorías: se guardan sin espacios al inicio ni al final, que son los que leerá el análisis (RF-12)
- [`RangosRecomendacionIT`](../backend/src/test/java/co/edu/usco/inspectia/rango/RangosRecomendacionIT.java) · el administrador ve los 4 rangos de su empresa, en el orden de las categorías, con los valores por defecto de una empresa nueva
- [`RangosRecomendacionIT`](../backend/src/test/java/co/edu/usco/inspectia/rango/RangosRecomendacionIT.java) · el límite de Fuera de norma es fijo en 0 %: con 0 se acepta y con otro valor no se guarda nada (prueba de límite) (parametrizada)
- [`RangosRecomendacionIT`](../backend/src/test/java/co/edu/usco/inspectia/rango/RangosRecomendacionIT.java) · la recomendación es obligatoria y tiene como máximo 300 caracteres: vacía, de solo espacios o de 301 se rechaza (prueba de límite) (parametrizada)
- [`RangosRecomendacionIT`](../backend/src/test/java/co/edu/usco/inspectia/rango/RangosRecomendacionIT.java) · los límites van de 0 a 100 % con un decimal como máximo, contado sin los ceros del final: 90,50 vale 90,5 (prueba de límite) (parametrizada)
- [`RangosRecomendacionIT`](../backend/src/test/java/co/edu/usco/inspectia/rango/RangosRecomendacionIT.java) · restaurar repone una categoría que falte: la empresa siempre queda con las 4
- [`RangosRecomendacionIT`](../backend/src/test/java/co/edu/usco/inspectia/rango/RangosRecomendacionIT.java) · se editan las 4 categorías juntas: con menos filas, con más o con una categoría repetida no se guarda nada (parametrizada)
- [`RangosRecomendacionIT`](../backend/src/test/java/co/edu/usco/inspectia/rango/RangosRecomendacionIT.java) · si dos administradores cambian los rangos a la vez, el segundo espera a que el primero guarde y deja sus 4 filas como las envió: los límites no quedan fuera de orden
- [`RangosRecomendacionIT`](../backend/src/test/java/co/edu/usco/inspectia/rango/RangosRecomendacionIT.java) · sin filas responde 400 en filas y no guarda nada
- [`RangosRecomendacionIT`](../backend/src/test/java/co/edu/usco/inspectia/rango/RangosRecomendacionIT.java) · una categoría que no existe responde 400 en el campo de su fila
- [`ReglasRangoRecomendacionImplTest`](../backend/src/test/java/co/edu/usco/inspectia/rango/reglas/ReglasRangoRecomendacionImplTest.java) · la regla compara los límites en el orden de las categorías, no en el que lleguen las filas
- [`ReglasRangoRecomendacionImplTest`](../backend/src/test/java/co/edu/usco/inspectia/rango/reglas/ReglasRangoRecomendacionImplTest.java) · los límites se comparan por su valor: 90 y 90.0 son un empate
- [`rangos.spec.js`](../frontend/e2e/rangos.spec.js) · un límite escrito con dos decimales, como 90,50, se guarda y se ve como lo guarda la API, 90,5, también al recargar (de punta a punta)
- [`RangoRecomendacionPantalla.test.jsx`](../frontend/src/generated/rango/RangoRecomendacionPantalla.test.jsx) · al cancelar la confirmación de restaurar no cambia nada (generada)
- [`RangoRecomendacionPantalla.test.jsx`](../frontend/src/generated/rango/RangoRecomendacionPantalla.test.jsx) · en la fila de FUERA_DE_NORMA, limiteInferior es de solo lectura, con el mensaje de su regla (generada)
- [`RangoRecomendacionPantalla.test.jsx`](../frontend/src/generated/rango/RangoRecomendacionPantalla.test.jsx) · guardar envía todas las filas juntas, con los números escritos en el formulario, y muestra el mensaje del JSON (generada)
- [`RangoRecomendacionPantalla.test.jsx`](../frontend/src/generated/rango/RangoRecomendacionPantalla.test.jsx) · la pantalla de rango_recomendacion muestra una fila por cada valor de categoria, en su orden, con sus datos (generada)
- [`RangoRecomendacionPantalla.test.jsx`](../frontend/src/generated/rango/RangoRecomendacionPantalla.test.jsx) · restaurar pide confirmación con el texto del JSON y, al confirmar, vuelve a los valores por defecto (generada)
- [`RangoRecomendacionPantalla.test.jsx`](../frontend/src/generated/rango/RangoRecomendacionPantalla.test.jsx) · un dato que no cumple su validación se marca en su campo y no se envía nada (generada)
- [`RangoRecomendacionPantalla.test.jsx`](../frontend/src/generated/rango/RangoRecomendacionPantalla.test.jsx) · un error de la API del bloque, sin campo o en filas, va arriba del formulario con su mensaje: $campo (parametrizada, generada)
- [`RangoRecomendacionPantalla.test.jsx`](../frontend/src/generated/rango/RangoRecomendacionPantalla.test.jsx) · un error de la API en el campo de una fila queda en ese campo (generada)
- [`rangoRecomendacionApi.test.js`](../frontend/src/generated/rango/rangoRecomendacionApi.test.js) · el cliente de la API de rango_recomendacion usa la ruta y el método de cada operación del JSON: las filas van todas juntas (generada)
- [`rangoRecomendacionEsquemas.test.js`](../frontend/src/generated/rango/rangoRecomendacionEsquemas.test.js) · cada validación del JSON de rango_recomendacion acepta en una fila su valor del borde: %s (parametrizada, generada)
- [`rangoRecomendacionEsquemas.test.js`](../frontend/src/generated/rango/rangoRecomendacionEsquemas.test.js) · cada validación del JSON de rango_recomendacion rechaza en una fila lo mismo que la API, con su mensaje: %s (parametrizada, generada)
- [`rangoRecomendacionEsquemas.test.js`](../frontend/src/generated/rango/rangoRecomendacionEsquemas.test.js) · limiteInferior se escribe con coma o con punto, y lo que no es un número tiene su mensaje (generada)
- [`rangoRecomendacionEsquemas.test.js`](../frontend/src/generated/rango/rangoRecomendacionEsquemas.test.js) · los mensajes de las validaciones de rango_recomendacion salen en el idioma de la app (generada)
- [`rangoRecomendacionEsquemas.test.js`](../frontend/src/generated/rango/rangoRecomendacionEsquemas.test.js) · los valores por defecto de rango_recomendacion, como los muestra el formulario, cumplen las validaciones (generada)
- [`GestionarRangos.test.jsx`](../frontend/src/rango/GestionarRangos.test.jsx) · el administrador de la empresa abre «Rangos de recomendación» desde el menú y ve las 4 categorías, con sus límites, sus recomendaciones y las notas
- [`GestionarRangos.test.jsx`](../frontend/src/rango/GestionarRangos.test.jsx) · el límite de Fuera de norma no se puede cambiar y dice por qué
- [`GestionarRangos.test.jsx`](../frontend/src/rango/GestionarRangos.test.jsx) · el resumen «Así quedan los rangos» dice qué porcentajes abarca cada categoría y cambia mientras se escriben los límites
- [`GestionarRangos.test.jsx`](../frontend/src/rango/GestionarRangos.test.jsx) · en español los límites se ven con coma, se escriben con coma o con punto y van a la API como números; al guardar quedan como los responde
- [`GestionarRangos.test.jsx`](../frontend/src/rango/GestionarRangos.test.jsx) · en inglés, los límites y el resumen van con punto
- [`GestionarRangos.test.jsx`](../frontend/src/rango/GestionarRangos.test.jsx) · un límite que no es un número dice cómo escribirlo, y no se envía nada
- [`limites.test.js`](../frontend/src/rango/limites.test.js) · con los límites de D-10, cada categoría abarca desde su límite hasta un decimal antes del de la categoría de arriba
- [`limites.test.js`](../frontend/src/rango/limites.test.js) · el final de cada categoría queda un paso de la escala antes del límite de arriba, sin errores de redondeo
- [`limites.test.js`](../frontend/src/rango/limites.test.js) · las categorías de un par que no baja, o las que dependen de un límite que no es un número, dicen que hay que revisar el límite

## RF-14 · Consultar el historial de lotes evaluados

| Criterio | Pruebas |
|---|---|
| C1. La lista muestra solo lotes de la empresa del usuario. | [`HistorialDeLotesIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/HistorialDeLotesIT.java) · la lista muestra solo lotes de la empresa del usuario, también los que registraron sus compañeros<br>[`lotes.spec.js`](../frontend/e2e/lotes.spec.js) · el historial solo muestra los lotes de la empresa del usuario: los de otra empresa no aparecen (de punta a punta) |
| C2. Los filtros por fecha del resultado, estado, categoría, código y usuario funcionan solos o combinados. | **Pendiente** |
| C3. El detalle muestra las mismas cifras del resultado guardado y las de cada muestra. | **Pendiente** |
| C4. Con más de 20 lotes, la lista se divide en páginas. | [`HistorialDeLotesIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/HistorialDeLotesIT.java) · con más de 20 lotes, la lista se divide en páginas de 20, del lote más reciente al más antiguo<br>[`lotes.spec.js`](../frontend/e2e/lotes.spec.js) · con más de 20 lotes, el historial se divide en páginas de 20, del más reciente al más antiguo (de punta a punta)<br>[`Historial.test.jsx`](../frontend/src/lote/Historial.test.jsx) · con más de 20 lotes, la lista se divide en páginas de 20 y se pasa a la siguiente |
| C5. Un lote pendiente se puede retomar desde la captura de fotos. | **Pendiente** |
| C6. Un lote con otra muestra sugerida permite analizarla desde el detalle (D-25). | **Pendiente** |

Otras pruebas de RF-14, que no cubren un criterio:

- [`HistorialDeLotesIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/HistorialDeLotesIT.java) · cada lote de la lista trae su fecha de recepción, código, estado, quién lo registró, porcentaje y categoría; los de un lote pendiente van vacíos
- [`HistorialDeLotesIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/HistorialDeLotesIT.java) · el detalle de un lote de la empresa se abre por su identificador
- [`HistorialDeLotesIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/HistorialDeLotesIT.java) · la administradora de la empresa también ve el historial de su empresa
- [`HistorialDeLotesIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/HistorialDeLotesIT.java) · la lista va del último movimiento al más antiguo: la fecha del resultado si el lote se analizó, o la de registro si está pendiente, y trae la fecha del resultado
- [`HistorialDeLotesIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/HistorialDeLotesIT.java) · las páginas van de 0 a la más alta que la consulta puede pedir y traen de 1 a 100 lotes; 
- [`Historial.test.jsx`](../frontend/src/lote/Historial.test.jsx) · el estado de cada lote va con su ícono, como en el prototipo: un chulo si está analizado y un reloj si está pendiente
- [`Historial.test.jsx`](../frontend/src/lote/Historial.test.jsx) · la administradora de la empresa también ve el historial
- [`Historial.test.jsx`](../frontend/src/lote/Historial.test.jsx) · la lista trae las columnas del prototipo: un lote analizado con su resultado y uno pendiente sin él
- [`Historial.test.jsx`](../frontend/src/lote/Historial.test.jsx) · sin lotes, al usuario final la lista le dice que no hay y le ofrece «Nuevo lote» para registrar el primero
- [`Historial.test.jsx`](../frontend/src/lote/Historial.test.jsx) · sin lotes, la administradora solo ve el texto: no registra lotes (RF-04)
- [`fechas.test.js`](../frontend/src/lote/fechas.test.js) · la fecha del resultado se muestra como día de Colombia, en el formato del idioma

## RF-15 · Descargar el reporte del lote en PDF

| Criterio | Pruebas |
|---|---|
| C1. El PDF trae todos los datos de la salida. | **Pendiente** |
| C2. Las cifras del PDF coinciden con las de la pantalla. | **Pendiente** |
| C3. El PDF se abre bien en celular y en PC. | **Pendiente** |
| C4. Al descargarlo de nuevo se obtiene el mismo reporte, aunque la empresa haya cambiado sus rangos. | **Pendiente** |
| C5. Después de la segunda muestra, el PDF trae el resultado de la suma y las dos muestras (D-25). | **Pendiente** |

## RF-16 · Consultar las estadísticas de la empresa

| Criterio | Pruebas |
|---|---|
| C1. Las cifras coinciden con los lotes del historial en el mismo periodo. | **Pendiente** |
| C2. Sin lotes en el periodo, muestra un mensaje en lugar de gráficas vacías. | **Pendiente** |
| C3. Un usuario final no puede entrar a esta pantalla (RNF-04). | **Pendiente** |
| C4. «La Esperanza» y «la esperanza » cuentan como la misma finca. | **Pendiente** |

## RF-17 · Consultar las métricas del modelo

| Criterio | Pruebas |
|---|---|
| C1. Las métricas coinciden con la tabla comparativa de los modelos (D-18). | **Pendiente** |
| C2. La confianza promedio usa solo análisis de la empresa. | **Pendiente** |
| C3. Un usuario final no puede entrar a esta pantalla (RNF-04). | **Pendiente** |

## RF-18 · Cambiar la propia contraseña

| Criterio | Pruebas |
|---|---|
| C1. Con la contraseña actual correcta y una nueva válida, la contraseña cambia: la nueva inicia sesión y la anterior ya no. | [`CambioDeContrasenaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/CambioDeContrasenaIT.java) · con la contraseña actual correcta y una nueva válida, la contraseña cambia: la nueva inicia sesión y la anterior ya no<br>[`cambiar-contrasena.spec.js`](../frontend/e2e/cambiar-contrasena.spec.js) · cambia la contraseña: la nueva inicia sesión y la anterior ya no<br>[`flujos.test.js`](../frontend/selenium/flujos.test.js) · se cambia la contraseña y la nueva inicia sesión (Selenium)<br>[`CambiarContrasena.test.jsx`](../frontend/src/paginas/CambiarContrasena.test.jsx) · con la contraseña actual correcta y una nueva válida la cambia y avisa que las otras sesiones se cerraron |
| C2. Con la contraseña actual errada no cambia nada, y el intento cuenta para el bloqueo de RF-01: después de 5 fallos seguidos, la cuenta se bloquea 15 minutos y tampoco se puede cambiar la contraseña hasta que termine. | [`CambioDeContrasenaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/CambioDeContrasenaIT.java) · al terminar el bloqueo se puede cambiar la contraseña otra vez<br>[`CambioDeContrasenaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/CambioDeContrasenaIT.java) · con la contraseña actual errada no cambia nada, responde con la regla del bloqueo y el intento cuenta<br>[`CambioDeContrasenaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/CambioDeContrasenaIT.java) · después de 5 fallos seguidos la cuenta se bloquea 15 minutos: el quinto ya responde 429 y durante el bloqueo tampoco se cambia con la contraseña correcta<br>[`CambioDeContrasenaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/CambioDeContrasenaIT.java) · los fallos del inicio de sesión y los del cambio de contraseña cuentan juntos para el bloqueo de RF-01<br>[`CambioDeContrasenaServicioTest`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/CambioDeContrasenaServicioTest.java) · con la contraseña actual errada registra el fallo con la hora del servidor y responde en el campo, con la regla del bloqueo<br>[`CambioDeContrasenaServicioTest`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/CambioDeContrasenaServicioTest.java) · durante el bloqueo no compara la contraseña ni cuenta el intento: responde que la cuenta está bloqueada y cuántos segundos faltan, redondeados hacia arriba<br>[`CambioDeContrasenaServicioTest`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/CambioDeContrasenaServicioTest.java) · si ese fallo es el quinto seguido y bloquea la cuenta, ya responde que está bloqueada por 15 minutos<br>[`cambiar-contrasena.spec.js`](../frontend/e2e/cambiar-contrasena.spec.js) · con la contraseña actual errada no cambia nada, y después de 5 fallos seguidos avisa el bloqueo con la hora en que termina<br>[`cliente.test.js`](../frontend/src/api/cliente.test.js) · el 429 del bloqueo trae la hora en que termina y los segundos que faltan<br>[`errores.test.js`](../frontend/src/api/errores.test.js) · el error de un campo del formulario va en ese campo, con la ayuda del backend<br>[`CambiarContrasena.test.jsx`](../frontend/src/paginas/CambiarContrasena.test.jsx) · con la contraseña actual errada marca ese campo con el mensaje y la regla del bloqueo, y no cambia nada<br>[`CambiarContrasena.test.jsx`](../frontend/src/paginas/CambiarContrasena.test.jsx) · con la cuenta bloqueada avisa que no se puede cambiar y a qué hora termina el bloqueo |
| C3. La contraseña nueva acepta de 8 a 64 caracteres: con 7 o con 65 se rechaza (prueba de límite). | [`CambioDeContrasenaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/CambioDeContrasenaIT.java) · la contraseña nueva acepta de 8 a 64 caracteres: con 7 o con 65 se rechaza (prueba de límite) (parametrizada)<br>[`cambiar-contrasena.spec.js`](../frontend/e2e/cambiar-contrasena.spec.js) · la contraseña nueva acepta de 8 a 64 caracteres: con 7 o con 65 se rechaza (prueba de límite)<br>[`CambiarContrasena.test.jsx`](../frontend/src/paginas/CambiarContrasena.test.jsx) · las reglas de la contraseña nueva se ven antes de guardar y dicen si el largo se cumple: 7 no, 8 sí<br>[`esquemas.test.js`](../frontend/src/validacion/esquemas.test.js) · la contraseña nueva acepta de 8 a 64 caracteres: con 7 o con 65 se rechaza (prueba de límite): %s (parametrizada) |
| C4. Una contraseña nueva de 64 caracteres con 8 eñes (72 bytes) se acepta; con 9 eñes (73 bytes) se rechaza con el mensaje «La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes» (prueba de límite). | [`CambioDeContrasenaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/CambioDeContrasenaIT.java) · una contraseña nueva de 64 caracteres con 8 eñes (72 bytes) se acepta y con 9 (73 bytes) se rechaza con su mensaje (prueba de límite) (parametrizada)<br>[`cambiar-contrasena.spec.js`](../frontend/e2e/cambiar-contrasena.spec.js) · una contraseña nueva de 64 caracteres con 8 eñes (72 bytes) se acepta; con 9 eñes (73 bytes) se rechaza (prueba de límite)<br>[`CambiarContrasena.test.jsx`](../frontend/src/paginas/CambiarContrasena.test.jsx) · la regla de los 72 bytes cuenta 2 por cada eñe: con 9 eñes en 64 caracteres no se cumple<br>[`esquemas.test.js`](../frontend/src/validacion/esquemas.test.js) · con 65 eñes, que se pasan del largo y de los bytes, la contraseña nueva da el error de los bytes, como la API<br>[`esquemas.test.js`](../frontend/src/validacion/esquemas.test.js) · la contraseña nueva ocupa como máximo 72 bytes, contando 2 por cada eñe (prueba de límite): %s (parametrizada) |
| C5. Al cambiarla, la sesión desde la que se cambió sigue abierta y las demás dejan de servir. | [`EmisorDeTokensTest`](../backend/src/test/java/co/edu/usco/inspectia/comun/seguridad/EmisorDeTokensTest.java) · el token con el que sigue una sesión lleva la hora de emisión y el vencimiento dados, en segundos enteros, sin alargar la sesión, y su versión de sesión<br>[`CambioDeContrasenaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/CambioDeContrasenaIT.java) · al cambiarla, la sesión desde la que se cambió sigue abierta con un token nuevo, que vence cuando vencía el anterior, y las demás dejan de servir<br>[`CambioDeContrasenaServicioTest`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/CambioDeContrasenaServicioTest.java) · al cambiarla guarda el hash nuevo, aplica la regla que corta las sesiones y entrega un token con la versión de sesión que dejó la regla, que vence cuando vencía el anterior<br>[`cambiar-contrasena.spec.js`](../frontend/e2e/cambiar-contrasena.spec.js) · al cambiarla, la sesión desde la que se cambió sigue abierta y la de otro navegador deja de servir<br>[`CambiarContrasena.test.jsx`](../frontend/src/paginas/CambiarContrasena.test.jsx) · al cambiarla, esta sesión sigue abierta con el token nuevo, que vence a la misma hora<br>[`sesion.test.jsx`](../frontend/src/sesion/sesion.test.jsx) · si la contraseña cambió en otra sesión, esta deja de servir: la siguiente petición lleva a Iniciar sesión con «Tu contraseña cambió» |

Otras pruebas de RF-18, que no cubren un criterio:

- [`InspectiaAplicacionIT`](../backend/src/test/java/co/edu/usco/inspectia/InspectiaAplicacionIT.java) · el reloj del servidor da la hora al microsegundo, la precisión de PostgreSQL, para que una hora guardada valga lo mismo al leerla: en 1.000 lecturas seguidas, una cada 50 µs, ninguna trae fracciones de microsegundo
- [`CambioDeContrasenaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/CambioDeContrasenaIT.java) · cada usuario solo cambia la suya: un identificador en el cuerpo se ignora y la contraseña y las sesiones de los demás no cambian (prueba de seguridad)
- [`CambioDeContrasenaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/CambioDeContrasenaIT.java) · la contraseña nueva puede ser igual a la actual: el cambio igual corta las demás sesiones
- [`CambioDeContrasenaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/CambioDeContrasenaIT.java) · sin la contraseña actual ni la nueva responde 400 en los dos campos y no cuenta como intento
- [`CambioDeContrasenaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/CambioDeContrasenaIT.java) · una contraseña actual de más de 72 bytes cuenta como errada aunque sus primeros 72 coincidan, como al iniciar sesión (RF-01 C8)
- [`CambioDeContrasenaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/CambioDeContrasenaIT.java) · una contraseña nueva de solo espacios se rechaza con el mismo mensaje que si faltara; con una letra se acepta (prueba de límite) (parametrizada)
- [`CambioDeContrasenaServicioTest`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/CambioDeContrasenaServicioTest.java) · una contraseña actual de más de 72 bytes no coincide aunque BCrypt diga que sí, y cuenta como fallo (RF-01 C8)
- [`cliente.test.js`](../frontend/src/api/cliente.test.js) · un 400 del backend llega con su mensaje, el error de cada campo y la ayuda
- [`cliente.test.js`](../frontend/src/api/cliente.test.js) · un 401 de una petición hecha con un token que ya se cambió no cierra la sesión nueva
- [`errores.test.js`](../frontend/src/api/errores.test.js) · %s no marca ningún campo: se muestra como aviso (parametrizada)
- [`errores.test.js`](../frontend/src/api/errores.test.js) · con varios campos marcados, la ayuda va solo con el primero del formulario
- [`errores.test.js`](../frontend/src/api/errores.test.js) · sin ayuda del backend, cada campo lleva solo su mensaje
- [`CambiarContrasena.test.jsx`](../frontend/src/paginas/CambiarContrasena.test.jsx) · Cancelar vuelve al inicio sin cambiar nada
- [`CambiarContrasena.test.jsx`](../frontend/src/paginas/CambiarContrasena.test.jsx) · si la contraseña repetida no coincide, no envía nada y lo marca en ese campo
- [`esquemas.test.js`](../frontend/src/validacion/esquemas.test.js) · cada letra con tilde y cada eñe ocupan 2 bytes, como los cuenta BCrypt: «%s» ocupa %i (parametrizada)
- [`esquemas.test.js`](../frontend/src/validacion/esquemas.test.js) · la contraseña actual es obligatoria
- [`esquemas.test.js`](../frontend/src/validacion/esquemas.test.js) · la contraseña repetida tiene que ser igual a la nueva
- [`esquemas.test.js`](../frontend/src/validacion/esquemas.test.js) · las reglas que se ven mientras se escribe dicen cuáles se cumplen: %s (parametrizada)
- [`esquemas.test.js`](../frontend/src/validacion/esquemas.test.js) · los mensajes salen en el idioma de la app
- [`esquemas.test.js`](../frontend/src/validacion/esquemas.test.js) · un dato está en blanco con las mismas reglas de @NotBlank en el backend: «%s» → %s (parametrizada)
- [`esquemas.test.js`](../frontend/src/validacion/esquemas.test.js) · una contraseña nueva de solo espacios se rechaza con «Este dato es obligatorio.», como en el backend

## RNF-01 · Entregar el resultado del análisis de un lote en 5 a 8 segundos

| Criterio | Pruebas |
|---|---|
| C1. En una prueba de 20 análisis, uno a la vez, con muestras de 100 a 150 granos, al menos 18 terminan en 8 segundos o menos. | **Pendiente** |
| C2. Ningún análisis tarda más de 15 segundos. | **Pendiente** |
| C3. El tiempo se mide en el servidor real, no en el PC de desarrollo. | **Pendiente** |
| C4. Mientras se procesa, el usuario ve un indicador de avance. | **Pendiente** |

## RNF-02 · Soportar hasta 30 empresas con 25 usuarios cada una

| Criterio | Pruebas |
|---|---|
| C1. Con 30 empresas y 750 usuarios registrados, las consultas responden en menos de 2 segundos. | **Pendiente** |
| C2. En una prueba de estrés con 50 usuarios simultáneos (RP-12), las consultas responden en menos de 2 segundos y sin errores. | **Pendiente** |
| C3. Si llegan varios análisis al mismo tiempo, se atienden en orden y ninguno se pierde; el usuario ve que su análisis está en espera. | **Pendiente** |
| C4. Con 5 análisis en cola, el último termina en menos de 45 segundos. | **Pendiente** |

## RNF-03 · Mantener separados los datos de cada empresa

| Criterio | Pruebas |
|---|---|
| C1. Si un usuario pide un lote de otra empresa por su identificador, el sistema responde que no existe. | [`LotesPorEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/LotesPorEmpresaIT.java) · si un usuario pide un lote de otra empresa por su identificador, el sistema responde que no existe |
| C2. Las listas y las estadísticas nunca mezclan datos de empresas distintas. | [`RangoRecomendacionApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/rango/RangoRecomendacionApiGeneradaIT.java) · el listado de rango_recomendacion solo trae las filas de la empresa de la sesión (generada)<br>[`UsuarioApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/usuario/UsuarioApiGeneradaIT.java) · el listado de usuario solo trae los registros de la empresa de la sesión (generada)<br>[`LotesPorEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/LotesPorEmpresaIT.java) · el historial de lotes nunca mezcla empresas: el administrador de Cacao del Sur solo ve los de su empresa<br>[`RangosRecomendacionIT`](../backend/src/test/java/co/edu/usco/inspectia/rango/RangosRecomendacionIT.java) · cada empresa lista solo sus 4 rangos<br>[`UsuariosPorEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/UsuariosPorEmpresaIT.java) · el listado de usuarios solo trae los de la empresa del administrador |
| C3. Las fotos y los reportes en S3 solo se abren con enlaces temporales generados para la empresa dueña. | **Pendiente** |
| C4. Solo el administrador de la plataforma puede no tener empresa: la base de datos rechaza un usuario de otro rol sin empresa. | [`EsquemaIT`](../backend/src/test/java/co/edu/usco/inspectia/EsquemaIT.java) · la base rechaza un usuario sin empresa que no sea ADMIN_PLATAFORMA |
| C5. Hay pruebas de seguridad que lo verifican (RP-12). | [`LotesPorEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/LotesPorEmpresaIT.java) · la empresa y el usuario del lote salen de la sesión: si la petición trae otros, la API los ignora (prueba de seguridad)<br>[`RangosRecomendacionIT`](../backend/src/test/java/co/edu/usco/inspectia/rango/RangosRecomendacionIT.java) · editar y restaurar los rangos de una empresa no toca los de otra, aunque el cuerpo traiga sus identificadores y su empresa (prueba de seguridad)<br>[`UsuariosPorEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/UsuariosPorEmpresaIT.java) · la empresa del usuario nuevo sale de la sesión: si la petición trae otra, la API la ignora (prueba de seguridad) |

Otras pruebas de RNF-03, que no cubren un criterio:

- [`RangoRecomendacionApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/rango/RangoRecomendacionApiGeneradaIT.java) · editar y restaurar las filas de rango_recomendacion no toca las de otra empresa (generada)
- [`UsuarioApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/usuario/UsuarioApiGeneradaIT.java) · el registro nuevo de usuario queda en la empresa de la sesión (generada)
- [`UsuarioApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/usuario/UsuarioApiGeneradaIT.java) · un registro de usuario de otra empresa responde 404 en cada operación que lo usa (parametrizada, generada)
- [`EsquemaIT`](../backend/src/test/java/co/edu/usco/inspectia/EsquemaIT.java) · un lote no puede apuntar a un usuario de otra empresa
- [`LotesPorEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/LotesPorEmpresaIT.java) · las sugerencias de finca o proveedor no traen las de otra empresa
- [`FotosDeLaMuestra.test.jsx`](../frontend/src/lote/FotosDeLaMuestra.test.jsx) · si el lote no existe o es de otra empresa, lo dice con el mensaje del backend

## RNF-04 · Exigir autenticación y restringir cada función según el rol

| Criterio | Pruebas |
|---|---|
| C1. Sin sesión, la API responde «no autorizado» (401). | [`EmpresaApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/empresa/EmpresaApiGeneradaIT.java) · sin sesión, cada operación de empresa responde 401 (parametrizada, generada)<br>[`RangoRecomendacionApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/rango/RangoRecomendacionApiGeneradaIT.java) · sin sesión, cada operación de rango_recomendacion responde 401 (parametrizada, generada)<br>[`UsuarioApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/usuario/UsuarioApiGeneradaIT.java) · sin sesión, cada operación de usuario responde 401 (parametrizada, generada)<br>[`SeguridadLotesIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/SeguridadLotesIT.java) · sin sesión, cada operación de la API de lotes responde 401 en JSON (parametrizada)<br>[`SeguridadEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/SeguridadEmpresaIT.java) · sin sesión, cada operación de la API de empresas responde 401 en JSON (parametrizada)<br>[`RangosRecomendacionIT`](../backend/src/test/java/co/edu/usco/inspectia/rango/RangosRecomendacionIT.java) · sin sesión, las rutas de los rangos responden 401<br>[`CambioDeContrasenaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/CambioDeContrasenaIT.java) · sin sesión, cambiar la contraseña responde 401 y no cambia nada<br>[`SesionAbiertaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionAbiertaIT.java) · sin token, la sesión actual responde 401 |
| C2. Con un rol sin permiso, responde «prohibido» (403); por ejemplo, cuando un usuario final intenta gestionar usuarios. | [`EmpresaApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/empresa/EmpresaApiGeneradaIT.java) · cada operación de empresa solo la usan los roles de su JSON (parametrizada, generada)<br>[`RangoRecomendacionApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/rango/RangoRecomendacionApiGeneradaIT.java) · cada operación de rango_recomendacion solo la usan los roles de su JSON (parametrizada, generada)<br>[`UsuarioApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/usuario/UsuarioApiGeneradaIT.java) · cada operación de usuario solo la usan los roles de su JSON (parametrizada, generada)<br>[`SeguridadLotesIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/SeguridadLotesIT.java) · el administrador de empresa no registra lotes: recibe 403 y no se guarda nada<br>[`SeguridadLotesIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/SeguridadLotesIT.java) · el administrador de la plataforma recibe 403 en cada operación de la API de lotes (parametrizada)<br>[`SeguridadLotesIT`](../backend/src/test/java/co/edu/usco/inspectia/analisis/SeguridadLotesIT.java) · el permiso se revisa antes que los datos: el administrador de empresa recibe 403 aunque envíe datos inválidos<br>[`SeguridadEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/SeguridadEmpresaIT.java) · el permiso se revisa antes que los datos: un usuario final recibe 403 aunque envíe datos inválidos<br>[`SeguridadEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/SeguridadEmpresaIT.java) · un administrador de empresa o un usuario final recibe 403 en cada operación de la API de empresas (parametrizada)<br>[`RangosRecomendacionIT`](../backend/src/test/java/co/edu/usco/inspectia/rango/RangosRecomendacionIT.java) · el administrador de la plataforma tampoco gestiona los rangos de una empresa: responde 403<br>[`RestablecerContrasenaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/administradores/RestablecerContrasenaIT.java) · solo el administrador de la plataforma lista y restablece a los administradores de empresa: los demás roles reciben 403<br>[`SesionAbiertaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionAbiertaIT.java) · con el token de un administrador de empresa, la API de empresas responde 403; con el de la plataforma, 200 |
| C3. Las contraseñas se guardan con hash (BCrypt), nunca en texto plano. | [`RegistroEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/RegistroEmpresaIT.java) · la contraseña del administrador se guarda con hash BCrypt y no sale en la respuesta |
| C4. La sesión vence a las 8 horas. | [`EmisorDeTokensTest`](../backend/src/test/java/co/edu/usco/inspectia/comun/seguridad/EmisorDeTokensTest.java) · el token vence 8 horas después de emitido, en segundos enteros, y solo lleva el emisor, el usuario, las fechas y la versión de sesión<br>[`SesionAbiertaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionAbiertaIT.java) · el token del inicio de sesión dura exactamente 8 horas y solo lleva el emisor, el usuario, las fechas y la versión de sesión, que empieza en 0 |
| C5. Después de 5 intentos fallidos seguidos, la cuenta se bloquea 15 minutos. | [`InicioSesionIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/InicioSesionIT.java) · el bloqueo dura 15 minutos desde el quinto fallo y los intentos durante el bloqueo no lo alargan |
| C6. Al cambiar o restablecer una contraseña, las sesiones abiertas antes del cambio dejan de servir. | [`ValidadorDeVigenciaTest`](../backend/src/test/java/co/edu/usco/inspectia/comun/seguridad/ValidadorDeVigenciaTest.java) · la versión de sesión del token solo sirve si es un entero de 0 a 2147483647, el rango de la columna: -1, 0.5, 2147483648 o un texto no sirven (prueba de límite) (parametrizada)<br>[`ValidadorDeVigenciaTest`](../backend/src/test/java/co/edu/usco/inspectia/comun/seguridad/ValidadorDeVigenciaTest.java) · un token sin versión de sesión no sirve: sin ella no se sabe si es de antes de cambiar la contraseña<br>[`CambiosALaVezIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/CambiosALaVezIT.java) · dos restablecimientos a la vez suben el contador de sesión dos veces: una sesión abierta entre los dos también deja de servir<br>[`CorteDeSesionesIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/CorteDeSesionesIT.java) · si el reloj del servidor se atrasa después del cambio de contraseña, un inicio de sesión nuevo sirve<br>[`CorteDeSesionesIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/CorteDeSesionesIT.java) · si el reloj del servidor se atrasa entre un inicio de sesión y el cambio de contraseña, esa sesión deja de servir, y una nueva sí sirve<br>[`CorteDeSesionesIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/CorteDeSesionesIT.java) · una sesión abierta en el mismo segundo del cambio de contraseña, pero antes de él, deja de servir, en los tres caminos que la cambian (parametrizada)<br>[`GestionUsuariosIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/GestionUsuariosIT.java) · al restablecer la contraseña de un usuario desde la gestión de usuarios, sus sesiones abiertas dejan de servir<br>[`RestablecerContrasenaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/administradores/RestablecerContrasenaIT.java) · al restablecer la contraseña de un administrador de empresa, sus sesiones abiertas dejan de servir<br>[`ReglasUsuarioImplTest`](../backend/src/test/java/co/edu/usco/inspectia/usuario/reglas/ReglasUsuarioImplTest.java) · al cambiar una contraseña, la versión de sesión sube y la cuenta se desbloquea en una sola sentencia de la base, y el usuario queda igual a la base: con la versión que devolvió, no con la que había leído<br>[`SesionServicioTest`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionServicioTest.java) · un token con otra versión de sesión que la de la base ya no sirve, sea menor o mayor; con la misma, sí (prueba de límite) (parametrizada)<br>[`cliente.test.js`](../frontend/src/api/cliente.test.js) · un 401 con sesión la cierra con el mensaje del backend, como «Tu contraseña cambió» |

Otras pruebas de RNF-04, que no cubren un criterio:

- [`UsuarioApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/usuario/UsuarioApiGeneradaIT.java) · un valor de una lista de usuario que existe pero no se puede asignar responde 403 y no guarda nada (parametrizada, generada)
- [`ConfiguracionJwtTest`](../backend/src/test/java/co/edu/usco/inspectia/comun/seguridad/ConfiguracionJwtTest.java) · sin INSPECTIA_JWT_CLAVE, o si no está en Base64 o tiene menos de 32 bytes, el servidor no arranca (parametrizada)
- [`ConfiguracionJwtTest`](../backend/src/test/java/co/edu/usco/inspectia/comun/seguridad/ConfiguracionJwtTest.java) · una clave de 32 bytes en Base64 sirve para firmar con HS256 (prueba de límite)
- [`ValidadorDeVigenciaTest`](../backend/src/test/java/co/edu/usco/inspectia/comun/seguridad/ValidadorDeVigenciaTest.java) · un token sin fecha de emisión no sirve: todos los del servidor la traen (D-28)
- [`ValidadorDeVigenciaTest`](../backend/src/test/java/co/edu/usco/inspectia/comun/seguridad/ValidadorDeVigenciaTest.java) · un token sin fecha de vencimiento no sirve, y no se informa como vencido
- [`GestionUsuariosIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/GestionUsuariosIT.java) · editar un usuario sin cambiar su contraseña no corta sus sesiones
- [`CambioDeContrasenaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/CambioDeContrasenaIT.java) · todos los roles cambian su propia contraseña: usuario final, administrador de empresa y administrador de la plataforma (parametrizada)
- [`SesionAbiertaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionAbiertaIT.java) · el rol sale de la base en cada petición: si cambia, la sesión abierta lo refleja sin otro token
- [`SesionAbiertaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionAbiertaIT.java) · el token de un usuario que no existe no sirve
- [`SesionAbiertaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionAbiertaIT.java) · un encabezado Authorization mal formado no sirve (parametrizada)
- [`SesionAbiertaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionAbiertaIT.java) · un token alterado no sirve: cambiar el usuario sin volver a firmar se rechaza
- [`SesionAbiertaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionAbiertaIT.java) · un token de otro emisor no sirve, aunque esté firmado con la clave correcta
- [`SesionAbiertaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionAbiertaIT.java) · un token firmado con otra clave no sirve
- [`SesionAbiertaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionAbiertaIT.java) · un token sin firma (alg none) no sirve
- [`empresa.spec.js`](../frontend/e2e/generated/empresa.spec.js) · la pantalla de empresa la abre ADMIN_PLATAFORMA, uno de los roles de listar (de punta a punta) (generada)
- [`empresa.spec.js`](../frontend/e2e/generated/empresa.spec.js) · la pantalla de empresa no la abre ADMIN_EMPRESA, que no está en los roles de listar (de punta a punta) (generada)
- [`empresa.spec.js`](../frontend/e2e/generated/empresa.spec.js) · la pantalla de empresa no la abre USUARIO_FINAL, que no está en los roles de listar (de punta a punta) (generada)
- [`rango_recomendacion.spec.js`](../frontend/e2e/generated/rango_recomendacion.spec.js) · la pantalla de rango_recomendacion la abre ADMIN_EMPRESA, uno de los roles de listar (de punta a punta) (generada)
- [`rango_recomendacion.spec.js`](../frontend/e2e/generated/rango_recomendacion.spec.js) · la pantalla de rango_recomendacion no la abre ADMIN_PLATAFORMA, que no está en los roles de listar (de punta a punta) (generada)
- [`rango_recomendacion.spec.js`](../frontend/e2e/generated/rango_recomendacion.spec.js) · la pantalla de rango_recomendacion no la abre USUARIO_FINAL, que no está en los roles de listar (de punta a punta) (generada)
- [`usuario.spec.js`](../frontend/e2e/generated/usuario.spec.js) · la pantalla de usuario la abre ADMIN_EMPRESA, uno de los roles de listar (de punta a punta) (generada)
- [`usuario.spec.js`](../frontend/e2e/generated/usuario.spec.js) · la pantalla de usuario no la abre ADMIN_PLATAFORMA, que no está en los roles de listar (de punta a punta) (generada)
- [`usuario.spec.js`](../frontend/e2e/generated/usuario.spec.js) · la pantalla de usuario no la abre USUARIO_FINAL, que no está en los roles de listar (de punta a punta) (generada)
- [`iniciar-sesion.spec.js`](../frontend/e2e/iniciar-sesion.spec.js) · sin sesión, una pantalla interna lleva a Iniciar sesión
- [`PantallaDeRegistros.test.jsx`](../frontend/src/crud/PantallaDeRegistros.test.jsx) · si la API no deja hacer una acción, la pantalla dice por qué y la fila queda como estaba
- [`registros.test.js`](../frontend/src/crud/registros.test.js) · cada acción de la pantalla aparece solo para los roles de su operación en el JSON: %s %s (parametrizada)
- [`EmpresaPantalla.test.jsx`](../frontend/src/generated/empresa/EmpresaPantalla.test.jsx) · la pantalla de empresa solo la abren los roles de listar: %s ve que no tiene permiso (parametrizada, generada)
- [`RangoRecomendacionPantalla.test.jsx`](../frontend/src/generated/rango/RangoRecomendacionPantalla.test.jsx) · la pantalla de rango_recomendacion solo la abren los roles de listar: %s ve que no tiene permiso (parametrizada, generada)
- [`UsuarioPantalla.test.jsx`](../frontend/src/generated/usuario/UsuarioPantalla.test.jsx) · la pantalla de usuario solo la abren los roles de listar: %s ve que no tiene permiso (parametrizada, generada)
- [`Marco.test.jsx`](../frontend/src/navegacion/Marco.test.jsx) · una pantalla de otro rol no se abre aunque se escriba su dirección: avisa que no tiene permiso
- [`pantallasGeneradas.test.jsx`](../frontend/src/navegacion/pantallasGeneradas.test.jsx) · la ruta de una pantalla generada la abren los roles de listar de su JSON; los demás ven que no tienen permiso
- [`pantallasGeneradas.test.jsx`](../frontend/src/navegacion/pantallasGeneradas.test.jsx) · un rol que no está en listar no abre la pantalla generada aunque escriba su dirección
- [`sesion.test.jsx`](../frontend/src/sesion/sesion.test.jsx) · sin sesión, una pantalla interna lleva a Iniciar sesión
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · la ruta, la entrada del menú y los roles de cada pantalla salen del JSON: la abren los roles de listar
- [`test_java.py`](../generador/pruebas/test_java.py) · las reglas de acceso por ruta y @PreAuthorize salen de operaciones del JSON

## RNF-05 · Funcionar en PC y en celular desde el navegador

| Criterio | Pruebas |
|---|---|
| C1. Todas las pantallas se usan sin desplazamiento horizontal desde 360 px de ancho. | [`empresa.spec.js`](../frontend/e2e/generated/empresa.spec.js) · la pantalla de empresa no se desplaza a los lados desde 360 px de ancho (generada)<br>[`rango_recomendacion.spec.js`](../frontend/e2e/generated/rango_recomendacion.spec.js) · la pantalla de rango_recomendacion no se desplaza a los lados desde 360 px de ancho (generada)<br>[`usuario.spec.js`](../frontend/e2e/generated/usuario.spec.js) · la pantalla de usuario no se desplaza a los lados desde 360 px de ancho (generada)<br>[`responsive.spec.js`](../frontend/e2e/responsive.spec.js) · las pantallas de los lotes no se desplazan a los lados desde 360 px de ancho, tampoco el historial con 7 páginas<br>[`responsive.spec.js`](../frontend/e2e/responsive.spec.js) · ninguna pantalla se desplaza a los lados desde 360 px de ancho<br>[`Historial.test.jsx`](../frontend/src/lote/Historial.test.jsx) · en el celular, con 8 páginas, solo se ven la primera, la última y las vecinas de la que se ve, para que quepan desde 360 px |
| C2. En el celular, la captura abre la cámara, tanto en Android con Chrome como en iPhone con Safari. | **Pendiente** |
| C3. Las pruebas del front-end (RP-14) se ejecutan en tamaño de PC y de celular. | [`responsive.spec.js`](../frontend/e2e/responsive.spec.js) · las pruebas corren en tamaño de PC y de celular: en PC el menú está a la vista y en el celular, detrás del botón |

Otras pruebas de RNF-05, que no cubren un criterio:

- [`CampoDeNumero.test.jsx`](../frontend/src/componentes/CampoDeNumero.test.jsx) · el campo de un número abre el teclado decimal del celular y guarda lo que se escribe, con coma o con punto
- [`PantallaDeRegistros.test.jsx`](../frontend/src/crud/PantallaDeRegistros.test.jsx) · en el celular, la lista se ve en tarjetas, cada una con sus datos y sus acciones, sin tabla
- [`FotosDeLaMuestra.test.jsx`](../frontend/src/lote/FotosDeLaMuestra.test.jsx) · en el celular los pasos dicen «Paso 2 de 3» con el nombre del paso en curso, como en el prototipo
- [`Historial.test.jsx`](../frontend/src/lote/Historial.test.jsx) · en el celular cada lote va en una tarjeta, con su código, su estado y su resultado
- [`Marco.test.jsx`](../frontend/src/navegacion/Marco.test.jsx) · en el celular el menú se esconde detrás del botón «Abrir el menú»

## RNF-06 · Ser sencilla de usar para el usuario final

| Criterio | Pruebas |
|---|---|
| C1. En una prueba con 3 personas que no conocen la app, cada una completa un análisis en menos de 5 minutos, sin ayuda. | **Pendiente** |
| C2. Registrar un lote, subir las fotos y ver el resultado toma como máximo 3 pantallas. | **Pendiente** |
| C3. Cada mensaje de error dice qué pasó y qué hacer. | [`accesibilidad.spec.js`](../frontend/e2e/accesibilidad.spec.js) · un error de un campo dice qué pasó y queda unido al campo para los lectores de pantalla<br>[`cliente.test.js`](../frontend/src/api/cliente.test.js) · si el servidor no responde, el error queda marcado como falta de conexión (estado 0) y no cierra la sesión<br>[`errores.test.js`](../frontend/src/api/errores.test.js) · con errores de campos y de reglas sin campo, el texto tiene solo los de las reglas: los de los campos van en sus campos<br>[`errores.test.js`](../frontend/src/api/errores.test.js) · el mensaje del backend se muestra tal cual, porque ya dice qué pasó y qué hacer<br>[`errores.test.js`](../frontend/src/api/errores.test.js) · si no dejan guardar varias reglas sin campo, el texto tiene sus mensajes, en el orden de la API<br>[`errores.test.js`](../frontend/src/api/errores.test.js) · si una regla sin campo no deja guardar, el texto es su mensaje, que dice por qué y qué hacer, y no el general<br>[`errores.test.js`](../frontend/src/api/errores.test.js) · sin conexión dice que no se pudo conectar con el servidor y que revise la conexión<br>[`errores.test.js`](../frontend/src/api/errores.test.js) · un error que no viene de la API también dice qué hacer<br>[`errores.test.js`](../frontend/src/api/errores.test.js) · una falla sin mensaje del backend dice que fue inesperada y que intente más tarde<br>[`CampoDeTexto.test.jsx`](../frontend/src/componentes/CampoDeTexto.test.jsx) · con un error, el campo dice qué pasó y conserva su ayuda: los dos quedan unidos al campo<br>[`CampoDeLista.test.jsx`](../frontend/src/crud/CampoDeLista.test.jsx) · con un error, el campo de lista dice qué pasó y queda unido al campo<br>[`PantallaDeRegistros.test.jsx`](../frontend/src/crud/PantallaDeRegistros.test.jsx) · al registrar con datos que faltan, el foco va al primer campo con error, también dentro del grupo del administrador<br>[`IniciarSesion.test.jsx`](../frontend/src/paginas/IniciarSesion.test.jsx) · si el servidor falla sin dar un mensaje, Iniciar sesión dice qué pasó y qué hacer<br>[`IniciarSesion.test.jsx`](../frontend/src/paginas/IniciarSesion.test.jsx) · sin conexión con el servidor, Iniciar sesión dice qué pasó y qué hacer |
| C4. Todas las funciones se pueden usar con el teclado y los textos tienen buen contraste. | [`accesibilidad.spec.js`](../frontend/e2e/accesibilidad.spec.js) · las pantallas de esta versión no tienen fallas de accesibilidad WCAG AA en el tema claro<br>[`accesibilidad.spec.js`](../frontend/e2e/accesibilidad.spec.js) · las pantallas de esta versión no tienen fallas de accesibilidad WCAG AA en el tema oscuro<br>[`accesibilidad.spec.js`](../frontend/e2e/accesibilidad.spec.js) · las pantallas de los lotes no tienen fallas de accesibilidad WCAG AA en los dos temas, tampoco con los errores del formulario a la vista<br>[`accesibilidad.spec.js`](../frontend/e2e/accesibilidad.spec.js) · se inicia sesión solo con el teclado<br>[`empresa.spec.js`](../frontend/e2e/generated/empresa.spec.js) · la pantalla de empresa no tiene fallas de accesibilidad WCAG AA, ni en el tema claro ni en el oscuro (generada)<br>[`rango_recomendacion.spec.js`](../frontend/e2e/generated/rango_recomendacion.spec.js) · la pantalla de rango_recomendacion no tiene fallas de accesibilidad WCAG AA, ni en el tema claro ni en el oscuro (generada)<br>[`usuario.spec.js`](../frontend/e2e/generated/usuario.spec.js) · la pantalla de usuario no tiene fallas de accesibilidad WCAG AA, ni en el tema claro ni en el oscuro (generada)<br>[`PantallaDeRegistros.test.jsx`](../frontend/src/crud/PantallaDeRegistros.test.jsx) · al abrir el formulario desde una fila, el foco va a su título; al cancelar, vuelve al botón que lo abrió<br>[`PantallaDeRegistros.test.jsx`](../frontend/src/crud/PantallaDeRegistros.test.jsx) · después de confirmar que se desactiva, el foco vuelve a la fila, en su botón, que ahora reactiva<br>[`PantallaDeRegistros.test.jsx`](../frontend/src/crud/PantallaDeRegistros.test.jsx) · después de reactivar, el foco sigue en la fila, en su botón, que ahora desactiva<br>[`PantallaDeRegistros.test.jsx`](../frontend/src/crud/PantallaDeRegistros.test.jsx) · después de registrar, el foco vuelve a «Nueva empresa»<br>[`PanelAdministradores.test.jsx`](../frontend/src/empresa/PanelAdministradores.test.jsx) · al abrir el panel, el foco va a su título; al cerrarlo, vuelve al botón de la fila<br>[`PanelAdministradores.test.jsx`](../frontend/src/empresa/PanelAdministradores.test.jsx) · el formulario de la contraseña recibe el foco en su título; al cancelar o al restablecer, vuelve al botón del administrador<br>[`Historial.test.jsx`](../frontend/src/lote/Historial.test.jsx) · la caja de la tabla recibe el foco y lleva su nombre, para recorrerla con el teclado si no cabe en la ventana<br>[`Marco.test.jsx`](../frontend/src/navegacion/Marco.test.jsx) · cada pantalla con menú empieza con el enlace «Saltar al contenido», que lleva al contenido<br>[`tema.test.js`](../frontend/src/tema/tema.test.js) · el texto de ejemplo de los campos va en el color del texto secundario y sin transparencia, en cada navegador<br>[`tokens.test.js`](../frontend/src/tema/tokens.test.js) · los textos y bordes de los dos temas cumplen el contraste WCAG AA: %s (parametrizada) |

Otras pruebas de RNF-06, que no cubren un criterio:

- [`cliente.test.js`](../frontend/src/api/cliente.test.js) · una respuesta de error que no es JSON, como la de un proxy caído, llega sin mensaje del backend
- [`CampoDeNumero.test.jsx`](../frontend/src/componentes/CampoDeNumero.test.jsx) · un campo de número de solo lectura no se puede cambiar y dice por qué, unido al campo para los lectores de pantalla
- [`CampoDeTexto.test.jsx`](../frontend/src/componentes/CampoDeTexto.test.jsx) · la ayuda de un campo se ve debajo de él y queda unida al campo para los lectores de pantalla
- [`CampoDeTexto.test.jsx`](../frontend/src/componentes/CampoDeTexto.test.jsx) · un campo sin ayuda no la anuncia
- [`EmpresaPantalla.test.jsx`](../frontend/src/generated/empresa/EmpresaPantalla.test.jsx) · sin conexión, la pantalla de empresa dice qué pasó y qué hacer, y se puede reintentar (generada)
- [`RangoRecomendacionPantalla.test.jsx`](../frontend/src/generated/rango/RangoRecomendacionPantalla.test.jsx) · sin conexión, la pantalla de rango_recomendacion dice qué pasó y qué hacer, y se puede reintentar (generada)
- [`UsuarioPantalla.test.jsx`](../frontend/src/generated/usuario/UsuarioPantalla.test.jsx) · sin conexión, la pantalla de usuario dice qué pasó y qué hacer, y se puede reintentar (generada)

## RNF-07 · Alcanzar el F1-score mínimo del modelo de IA

| Criterio | Pruebas |
|---|---|
| C1. El F1-score macro del modelo elegido en el conjunto de prueba es de 0,65 o más (P-02): EfficientNet-B0 logra 0,735 (IC 95 % 0,677–0,792). | **Pendiente** |
| C2. Se reportan también la precisión, el recall y la matriz de confusión de cada clase. | **Pendiente** |
| C3. Los 5 modelos se miden con el mismo conjunto de prueba (D-18), sin imágenes repetidas del entrenamiento ni del `val`. | **Pendiente** |
| C4. Los resultados de los 5 modelos quedan en la tabla comparativa (D-18). | **Pendiente** |
| C5. El modelo se elige con el `val` de Julián, antes de medir la prueba, y la prueba se usa una sola vez. | **Pendiente** |

## RNF-08 · Citar a los autores del dataset y no usarlo con fines comerciales

| Criterio | Pruebas |
|---|---|
| C1. La app tiene una sección «Acerca de» con la cita del dataset, sus autores y su licencia. | [`acerca-de.spec.js`](../frontend/e2e/acerca-de.spec.js) · con sesión, Acerca de se abre desde «Mi cuenta»<br>[`acerca-de.spec.js`](../frontend/e2e/acerca-de.spec.js) · sin sesión, desde Iniciar sesión se abre Acerca de con la cita del dataset, sus autores y su licencia<br>[`AcercaDe.test.jsx`](../frontend/src/paginas/AcercaDe.test.jsx) · Acerca de cita el dataset con su nombre, su autor, el artículo de los autores, la licencia CC BY-NC 4.0 y el enlace a Kaggle<br>[`AcercaDe.test.jsx`](../frontend/src/paginas/AcercaDe.test.jsx) · Acerca de se abre sin sesión, con un enlace para volver a Iniciar sesión<br>[`IniciarSesion.test.jsx`](../frontend/src/paginas/IniciarSesion.test.jsx) · sin sesión, desde Iniciar sesión se abre Acerca de con la cita del dataset |
| C2. El repositorio y la documentación citan el dataset y el artículo de los autores (Zhinin-Vera et al., 2023). | **Pendiente** |
| C3. Mientras use modelos entrenados con este dataset, InspectIA no se cobra. | **Pendiente** |

Otras pruebas de RNF-08, que no cubren un criterio:

- [`AcercaDe.test.jsx`](../frontend/src/paginas/AcercaDe.test.jsx) · Acerca de dice que InspectIA no se cobra mientras use modelos entrenados con esos datos
- [`AcercaDe.test.jsx`](../frontend/src/paginas/AcercaDe.test.jsx) · Acerca de muestra la versión de la app

## Pautas del docente y decisiones

Pruebas que no cubren un criterio de una ficha, sino una pauta del docente (RP) o una decisión (D) de `specs/decisiones.md`.

### RP-01 · API REST con el CRUD de las entidades del sistema, en Java con Spring Boot, JPA y Lombok

- [`VerificacionDeClasesTest`](../backend/src/test/java/co/edu/usco/inspectia/VerificacionDeClasesTest.java) · la JVM acepta todas las clases compiladas del backend

### RP-03 · Validaciones y seguridad en el back-end

- [`RangoRecomendacionApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/rango/RangoRecomendacionApiGeneradaIT.java) · un código de categoría que no existe responde 400 en el campo de su fila (generada)
- [`UsuarioApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/usuario/UsuarioApiGeneradaIT.java) · un código que no existe en una lista de usuario responde 400 con su campo (parametrizada, generada)

### RP-04 · Documentación con JavaDoc, comentarios y OpenAPI

- [`DocumentacionApiIT`](../backend/src/test/java/co/edu/usco/inspectia/comun/api/DocumentacionApiIT.java) · la documentación OpenAPI pide el token Bearer en toda la API, salvo para iniciar sesión

### RP-07 · Validación y seguridad de los formularios con Yup y Formik

- [`reglas.test.js`](../frontend/src/validacion/reglas.test.js) · cabeEnBytes cuenta los bytes en UTF-8, como @MaximoBytes en la API: %s (parametrizada)
- [`reglas.test.js`](../frontend/src/validacion/reglas.test.js) · cabeEnBytes deja pasar el dato que falta aunque el máximo sea pequeño: lo revisa obligatorio
- [`reglas.test.js`](../frontend/src/validacion/reglas.test.js) · maximoDecimales deja pasar el dato que falta y lo que no es un número: los revisan obligatorio y el tipo
- [`reglas.test.js`](../frontend/src/validacion/reglas.test.js) · maximoDecimales(%i) cuenta los decimales sin los ceros del final, como @MaximoDecimales en la API: %s (parametrizada)
- [`reglas.test.js`](../frontend/src/validacion/reglas.test.js) · minusculas deja igual lo que no es un texto
- [`reglas.test.js`](../frontend/src/validacion/reglas.test.js) · minusculas pasa un texto a minúsculas, también las letras con tilde y la eñe, como la API
- [`reglas.test.js`](../frontend/src/validacion/reglas.test.js) · numeroDecimal deja sin valor un campo %s: es el dato que falta, que revisa obligatorio (parametrizada)
- [`reglas.test.js`](../frontend/src/validacion/reglas.test.js) · numeroDecimal deja un número como llegó, y sin valor lo que no llegó
- [`reglas.test.js`](../frontend/src/validacion/reglas.test.js) · numeroDecimal lee un número escrito con coma o con punto: %s (parametrizada)
- [`reglas.test.js`](../frontend/src/validacion/reglas.test.js) · numeroDecimal no lee como número lo que no se escribe así en el formulario: %s (parametrizada)
- [`reglas.test.js`](../frontend/src/validacion/reglas.test.js) · recortar deja igual lo que no es un texto
- [`reglas.test.js`](../frontend/src/validacion/reglas.test.js) · recortar quita los mismos espacios del inicio y del final que String.strip() en la API: %s (parametrizada)
- [`reglas.test.js`](../frontend/src/validacion/reglas.test.js) · un dato está lleno con la misma regla de @NotBlank en la API: %s (parametrizada)
- [`reglas.test.js`](../frontend/src/validacion/reglas.test.js) · un dato opcional que llega no puede estar en blanco, como lo revisa @NoEnBlanco en la API: %s (parametrizada)
- [`reglas.test.js`](../frontend/src/validacion/reglas.test.js) · un texto vacío cuenta como ausente, como el dato que no llega a la API; lo demás queda igual

### RP-08 · Interfaz responsive y accesible, con tokens de colores y estilos, y temas

- [`Marco.test.jsx`](../frontend/src/navegacion/Marco.test.jsx) · el tema se cambia desde la barra
- [`IniciarSesion.test.jsx`](../frontend/src/paginas/IniciarSesion.test.jsx) · el tema se cambia a oscuro desde Iniciar sesión y queda guardado al volver a abrir la app
- [`claseInicial.test.js`](../frontend/src/tema/claseInicial.test.js) · con el tema del sistema, o sin elegir todavía, arranca con el del sistema
- [`claseInicial.test.js`](../frontend/src/tema/claseInicial.test.js) · con un tema elegido, la app arranca con ese, aunque el sistema esté en el otro
- [`claseInicial.test.js`](../frontend/src/tema/claseInicial.test.js) · sin acceso al almacenamiento del navegador, arranca con el tema del sistema

### RP-10 · Header en cada artefacto con nombre, descripción, fecha de creación, fecha de actualización, autor y versión

- [`test_salida.py`](../generador/pruebas/test_salida.py) · cada archivo generado lleva su header y la marca de no editar a mano, también los del frontend

### RP-11 · Internacionalización

- [`RegistroEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/RegistroEmpresaIT.java) · con Accept-Language: en, el error del dígito de verificación sale en inglés
- [`textos-del-backend.test.js`](../frontend/herramientas/textos-del-backend.test.js) · el plugin de Vite entrega los textos como el módulo virtual:textos-del-backend y vigila los cuatro .properties
- [`textos-del-backend.test.js`](../frontend/herramientas/textos-del-backend.test.js) · los textos del backend tienen las mismas claves en español y en inglés
- [`textos-del-backend.test.js`](../frontend/herramientas/textos-del-backend.test.js) · se leen como en Java las claves con escapes, el espacio como separador, la sangría, una clave sin valor y una barra al final del archivo
- [`textos-del-backend.test.js`](../frontend/herramientas/textos-del-backend.test.js) · se leen los .properties del backend: comentarios, escapes y letras con tilde en UTF-8
- [`textos-del-backend.test.js`](../frontend/herramientas/textos-del-backend.test.js) · si no encuentra los .properties del backend, lo dice en vez de dejar la app sin textos
- [`errores.test.js`](../frontend/src/api/errores.test.js) · sin conexión, el aviso sale en el idioma de la app
- [`decimales.test.js`](../frontend/src/crud/decimales.test.js) · un decimal que no llegó se muestra vacío
- [`decimales.test.js`](../frontend/src/crud/decimales.test.js) · un decimal se muestra con su escala y el separador del idioma, sin separar los miles: %s con escala %i en %s (parametrizada)
- [`i18n.test.js`](../frontend/src/i18n/i18n.test.js) · cada texto propio del frontend existe en español y en inglés, y ninguno está vacío
- [`i18n.test.js`](../frontend/src/i18n/i18n.test.js) · la app está en español si la persona no eligió otro idioma
- [`i18n.test.js`](../frontend/src/i18n/i18n.test.js) · los marcadores del backend, como {0} y {1}, se reemplazan igual que en el backend
- [`i18n.test.js`](../frontend/src/i18n/i18n.test.js) · los textos compartidos con el backend salen de sus archivos .properties: %s %s (parametrizada)
- [`i18n.test.js`](../frontend/src/i18n/i18n.test.js) · los textos de las pantallas generadas están en español y en inglés, y ninguno está vacío
- [`i18n.test.js`](../frontend/src/i18n/i18n.test.js) · los textos de las pantallas generadas salen de pantalla.textos de su JSON: %s %s (parametrizada)
- [`NuevoLote.test.jsx`](../frontend/src/lote/NuevoLote.test.jsx) · el campo de la finca no tiene el botón «Clear» de Material UI, que no está traducido: el texto se borra con el teclado
- [`Marco.test.jsx`](../frontend/src/navegacion/Marco.test.jsx) · el idioma se cambia desde la barra y el menú queda en inglés
- [`IniciarSesion.test.jsx`](../frontend/src/paginas/IniciarSesion.test.jsx) · Iniciar sesión cambia a inglés, y pide al backend los mensajes en inglés
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · los textos de cada pantalla salen de pantalla.textos del JSON, en español y en inglés
- [`test_java.py`](../generador/pruebas/test_java.py) · los textos de todas las entidades salen en español y en inglés

### RP-13 · Listado completo de casos de prueba, derivados de los criterios de aceptación de cada requerimiento, y verificación de su cumplimiento

- [`test_casos.py`](../generador/pruebas/test_casos.py) · docs/casos-de-prueba.md está al día con las pruebas y las fichas
- [`test_casos.py`](../generador/pruebas/test_casos.py) · el documento sale igual cada vez que se arma
- [`test_casos.py`](../generador/pruebas/test_casos.py) · las pruebas del frontend en generated/ cuentan como generadas
- [`test_casos.py`](../generador/pruebas/test_casos.py) · se leen las marcas de Java aunque las anotaciones lleven paréntesis, comillas o @
- [`test_casos.py`](../generador/pruebas/test_casos.py) · se leen las marcas de las pruebas de JavaScript: el título de test o it, en Vitest, Playwright y Selenium
- [`test_casos.py`](../generador/pruebas/test_casos.py) · se leen las marcas de las pruebas de Python
- [`test_casos.py`](../generador/pruebas/test_casos.py) · todas las pruebas del repositorio tienen una marca válida
- [`test_casos.py`](../generador/pruebas/test_casos.py) · un criterio sin prueba sale como pendiente; uno eliminado no
- [`test_casos.py`](../generador/pruebas/test_casos.py) · una marca que cita algo que no existe o un criterio eliminado es un error
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · las pruebas generadas del frontend llevan un título fijo con su marca: la ficha de la entidad o el RNF que cubren

### RP-14 · Herramientas de prueba

- [`espejo.test.js`](../frontend/herramientas/espejo.test.js) · la carpeta excluida de la copia no se toca al espejar: lo que se hace con ella lo decide quien llama
- [`espejo.test.js`](../frontend/herramientas/espejo.test.js) · la copia del backend lleva también infra/bd, para que las pruebas que leen ../infra/bd pasen en la copia
- [`espejo.test.js`](../frontend/herramientas/espejo.test.js) · la copia del backend nunca lleva infra/.env, con las contraseñas locales, ni lo demás de infra
- [`espejo.test.js`](../frontend/herramientas/espejo.test.js) · la copia puede estar dentro del original, en una carpeta excluida, como frontend/.demostracion/frontend
- [`espejo.test.js`](../frontend/herramientas/espejo.test.js) · la primera vez copia todo, menos las carpetas excluidas, como target
- [`espejo.test.js`](../frontend/herramientas/espejo.test.js) · lo que se borra del backend o de infra/bd desaparece de la copia, y se informa por separado
- [`espejo.test.js`](../frontend/herramientas/espejo.test.js) · se excluye la ruta exacta: con target, una carpeta target más adentro se espeja como las demás
- [`espejo.test.js`](../frontend/herramientas/espejo.test.js) · sin carpetas excluidas, lo copia todo
- [`espejo.test.js`](../frontend/herramientas/espejo.test.js) · un archivo borrado o renombrado en el original desaparece de la copia, y se informa
- [`espejo.test.js`](../frontend/herramientas/espejo.test.js) · un archivo cambiado se copia con su contenido nuevo y su fecha, para que Maven compile solo lo cambiado
- [`espejo.test.js`](../frontend/herramientas/espejo.test.js) · un archivo que pasó a ser carpeta se reemplaza en la copia
- [`espejo.test.js`](../frontend/herramientas/espejo.test.js) · una carpeta borrada en el original desaparece entera de la copia
- [`espejo.test.js`](../frontend/herramientas/espejo.test.js) · una carpeta excluida dentro de otra, como e2e/.entorno, ni se copia ni se borra de la copia

### RP-15 · Scripts de creación e inicialización de la base de datos

- [`EsquemaIT`](../backend/src/test/java/co/edu/usco/inspectia/EsquemaIT.java) · Flyway crea las 8 tablas del modelo de datos
- [`EsquemaIT`](../backend/src/test/java/co/edu/usco/inspectia/EsquemaIT.java) · el trigger mantiene fecha_actualizacion aunque un script intente cambiarla
- [`test_sql.py`](../generador/pruebas/test_sql.py) · cada referencia tiene índice, salvo si otro índice ya empieza por ella
- [`test_sql.py`](../generador/pruebas/test_sql.py) · cada tabla tiene id UUID v7, fechas y el trigger de fecha_actualizacion
- [`test_sql.py`](../generador/pruebas/test_sql.py) · el esquema crea las 8 tablas en orden de dependencias
- [`test_sql.py`](../generador/pruebas/test_sql.py) · la tabla usuario guarda la versión de sesión, un entero que empieza en 0 y no baja de 0, y ya no la hora sesiones_desde
- [`test_sql.py`](../generador/pruebas/test_sql.py) · las listas, patrones y CHECK de los JSON salen como restricciones
- [`test_sql.py`](../generador/pruebas/test_sql.py) · las tablas de cada empresa se referencian con FK compuestas (registro, empresa)
- [`test_sql.py`](../generador/pruebas/test_sql.py) · los únicos normalizados y parciales salen como índices únicos
- [`test_sql.py`](../generador/pruebas/test_sql.py) · ningún nombre de restricción, índice o trigger pasa de 63 caracteres

### RP-23 · Generación automatizada

- [`DecimalesTest`](../backend/src/test/java/co/edu/usco/inspectia/comun/numero/DecimalesTest.java) · un dato que no llegó sigue sin valor
- [`MaximoDecimalesValidadorTest`](../backend/src/test/java/co/edu/usco/inspectia/comun/validacion/MaximoDecimalesValidadorTest.java) · @MaximoDecimales responde en el campo con el mensaje general de los decimales
- [`MaximoDecimalesValidadorTest`](../backend/src/test/java/co/edu/usco/inspectia/comun/validacion/MaximoDecimalesValidadorTest.java) · @MaximoDecimales usa el máximo de su anotación, que sale de la escala del JSON (parametrizada)
- [`otra-tematica.test.js`](../frontend/herramientas/otra-tematica.test.js) · --entidad recibe la ruta del JSON, relativa a la carpeta donde se corrió el comando, y --con-pruebas va antes o después
- [`otra-tematica.test.js`](../frontend/herramientas/otra-tematica.test.js) · --entidad sin la ruta del JSON detiene la demostración y dice cómo se escribe
- [`otra-tematica.test.js`](../frontend/herramientas/otra-tematica.test.js) · con cafe.json, los textos y las pruebas de la demostración salen de su JSON: su nombre, su clase, su ruta y su rol
- [`otra-tematica.test.js`](../frontend/herramientas/otra-tematica.test.js) · el alcance de la demostración agrega la entidad al final de la API y de las pantallas, y deja lo demás igual
- [`otra-tematica.test.js`](../frontend/herramientas/otra-tematica.test.js) · en Windows el enlace a node_modules es un junction, que no pide permisos de administrador; en los demás, un enlace simbólico
- [`otra-tematica.test.js`](../frontend/herramientas/otra-tematica.test.js) · la copia del frontend usa el node_modules del original por un enlace, sin copiarlo; si ya lo tiene, lo deja
- [`otra-tematica.test.js`](../frontend/herramientas/otra-tematica.test.js) · la demostración dice con quién entrar según el rol de la entidad: el administrador de la plataforma, el de la empresa o el usuario final
- [`otra-tematica.test.js`](../frontend/herramientas/otra-tematica.test.js) · la demostración entra con el primer rol que registra en la entidad o, si nadie registra, con el primero que la lista
- [`otra-tematica.test.js`](../frontend/herramientas/otra-tematica.test.js) · la demostración lee el JSON de la entidad; si no existe o no es un JSON, se detiene y lo dice
- [`otra-tematica.test.js`](../frontend/herramientas/otra-tematica.test.js) · lo que escribió el generador para producto se lista por capa, en orden: de la base a las pruebas de la pantalla
- [`otra-tematica.test.js`](../frontend/herramientas/otra-tematica.test.js) · si nadie del entorno de la demostración tiene el rol de la entidad, se detiene y dice con qué roles se entra
- [`otra-tematica.test.js`](../frontend/herramientas/otra-tematica.test.js) · si npm se quedó con las opciones porque faltó «--» antes de ellas, la demostración se detiene en vez de seguir sin ellas
- [`otra-tematica.test.js`](../frontend/herramientas/otra-tematica.test.js) · sin --entidad, los textos y las pruebas de la demostración salen de producto.json
- [`otra-tematica.test.js`](../frontend/herramientas/otra-tematica.test.js) · sin opciones, la demostración agrega producto.json y no corre las pruebas generadas
- [`otra-tematica.test.js`](../frontend/herramientas/otra-tematica.test.js) · un JSON con el nombre de una entidad de InspectIA, como empresa.json, detiene la demostración: reemplazaría la de la app
- [`otra-tematica.test.js`](../frontend/herramientas/otra-tematica.test.js) · una capa en la que el generador no escribió nada no se lista
- [`otra-tematica.test.js`](../frontend/herramientas/otra-tematica.test.js) · una opción que la demostración no entiende, como --con-prueba mal escrito, la detiene en vez de ignorarla
- [`CampoDeTexto.test.jsx`](../frontend/src/componentes/CampoDeTexto.test.jsx) · con variasLineas, el campo de texto va en varias líneas, como la recomendación de un rango
- [`CampoDeLista.test.jsx`](../frontend/src/crud/CampoDeLista.test.jsx) · el campo de lista muestra los valores de la lista del JSON con su etiqueta y guarda el código del elegido
- [`CampoDeLista.test.jsx`](../frontend/src/crud/CampoDeLista.test.jsx) · una lista que empieza sin valor muestra «Elige una opción» en vez de aparentar que ya se eligió el primero, y al elegir deja de verse
- [`PantallaDeRegistros.test.jsx`](../frontend/src/crud/PantallaDeRegistros.test.jsx) · con más de 20 registros, la lista va en páginas fijas de 20, pasa a la siguiente y la pide a la API
- [`PantallaDeRegistros.test.jsx`](../frontend/src/crud/PantallaDeRegistros.test.jsx) · después de registrar, el formulario se cierra y la lista y su resumen se vuelven a pedir, con el registro nuevo
- [`PantallaDeRegistros.test.jsx`](../frontend/src/crud/PantallaDeRegistros.test.jsx) · las notas del JSON se ven donde van: la del formulario solo al registrar, y la de la lista debajo de ella
- [`PantallaDeRegistros.test.jsx`](../frontend/src/crud/PantallaDeRegistros.test.jsx) · un error de la API que no es de un campo, como el de una regla sin campo, se ve arriba del formulario, que sigue abierto
- [`PantallaDeRegistros.test.jsx`](../frontend/src/crud/PantallaDeRegistros.test.jsx) · «Cancelar» cierra el formulario sin enviar nada a la API
- [`registros.test.js`](../frontend/src/crud/registros.test.js) · con límite de activos, el resumen cuenta los activos contra el máximo y solo lo que usa su frase: %i activos (parametrizada)
- [`registros.test.js`](../frontend/src/crud/registros.test.js) · el resumen cuenta en singular y en plural con los textos de pantalla.textos: %i registros, %i activos (parametrizada)
- [`registros.test.js`](../frontend/src/crud/registros.test.js) · el resumen sale en el idioma de la app
- [`registros.test.js`](../frontend/src/crud/registros.test.js) · la frase del resumen también recibe el máximo de activos, y solo se piden los textos que ella usa
- [`registros.test.js`](../frontend/src/crud/registros.test.js) · los textos de una fila, como «Editar {nombre}», toman solo sus textos y sus números
- [`registros.test.js`](../frontend/src/crud/registros.test.js) · si el JSON no trae los textos del resumen, la pantalla no muestra resumen
- [`registros.test.js`](../frontend/src/crud/registros.test.js) · un error de un campo dentro de un grupo, como administrador.correo, queda en ese campo del formulario
- [`registros.test.js`](../frontend/src/crud/registros.test.js) · un texto que se cuenta va en singular solo con uno
- [`pantallasGeneradas.test.jsx`](../frontend/src/navegacion/pantallasGeneradas.test.jsx) · una pantalla generada con un ícono que el menú no conoce sale con uno genérico
- [`pantallasGeneradas.test.jsx`](../frontend/src/navegacion/pantallasGeneradas.test.jsx) · una pantalla generada que menu.js no nombra entra al final del menú de los roles de su listar
- [`esquemas.test.js`](../frontend/src/validacion/esquemas.test.js) · la contraseña nueva se revisa con las validaciones generadas de la contraseña de un usuario, en el orden de la API: el mismo error que al crearlo: %s (parametrizada)
- [`esquemas.test.js`](../frontend/src/validacion/esquemas.test.js) · las reglas de la contraseña nueva siguen los límites que les llegan del JSON de usuario: con otros, cambian
- [`esquemas.test.js`](../frontend/src/validacion/esquemas.test.js) · las reglas que se ven mientras se escribe aceptan lo mismo que las validaciones generadas de la contraseña de un usuario: %s (parametrizada)
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · alcance.json dice qué entidades tienen pantalla: empresa (RF-02), usuario (RF-03), rango_recomendacion (RF-13) y solo entidades con API
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · cada entidad de pantallas en alcance.json tiene su pantalla, su cliente de la API, sus esquemas de Yup y sus pruebas; las demás, no
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · con limite_de_activos, la pantalla recibe el máximo y el mensaje de su regla: el resumen cuenta los activos contra el máximo y solo pide los textos que usa su frase (RF-03 C5, D-34)
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · desactivar pide confirmación solo si pantalla.textos trae su texto (D-34): la empresa lo trae
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · el cliente de la API de una pantalla en bloque lista las filas sin páginas, las edita todas juntas y restaura los valores por defecto (RF-13)
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · el cliente de la API de una pantalla usa la ruta y las operaciones del JSON, y el listado filtra por activo (D-33)
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · el formato del correo se valida solo en el backend, como el dígito del NIT: Yup no lo revisa y su error llega al campo al guardar
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · el generador se detiene con una pantalla que todavía no sabe generar o a la que le faltan textos, y dice qué falta
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · la pantalla de usuario pide los textos de lo nuevo: la marca de la fila propia y los de la contraseña al editar, y el generador dice cuál falta
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · la pantalla de usuario sale de su JSON: el correo debajo del nombre, el rol con sus valores, el formulario al lado de la lista y desactivar sin confirmación (D-34)
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · la pantalla en bloque pide sus textos: titulo_editar y al_guardar con editar, y confirmar_restaurar_detalle y al_restaurar con restaurar_por_defecto; no tiene lista vacía, y el generador dice cuál falta
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · la pantalla toma del JSON sus campos, sus columnas, los roles de cada acción y sus textos
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · la prueba generada de Yup de una pantalla en bloque revisa en una fila los mismos casos que la prueba generada de la API, con los números escritos como en el formulario, con coma, y los valores del borde que acepta cada campo
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · la prueba generada de Yup rechaza cada valor de una lista que existe pero el formulario no ofrece, como ADMIN_PLATAFORMA en el rol, con el mismo mensaje del 403 de la API
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · la prueba generada de Yup rechaza el ejemplo de un patrón con algo pegado antes o después, como la API: en JavaScript, un patrón sin ^ ni $ aceptaría x900123456-8
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · la prueba generada de Yup revisa los mismos casos de límite que la prueba generada de la API, con el mismo mensaje en español, salvo el formato del correo
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · la prueba generada de la pantalla en bloque revisa las filas en su orden, el campo de solo lectura, guardar todas las filas juntas, los errores de una fila y del bloque, y restaurar con su confirmación
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · la prueba generada de la pantalla revisa lo nuevo de usuario: el límite de activos, la fila propia, la nota de la contraseña, la contraseña vacía al editar y el valor inicial
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · las reglas en_la_fila_propia se aplican en la pantalla antes que en la API: la fila de quien tiene la sesión lleva su marca y no tiene las acciones que impiden, y al editarla los campos que impiden no se cambian y muestran el mensaje de la regla (RF-03 C7, C8 y C11)
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · las validaciones del JSON pasan a Yup con la misma clave de mensaje, la propia del atributo o la general, y en el orden en que la API escoge el error de un campo: primero el dato que falta y después por el nombre de la restricción
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · los esquemas generados exportan los límites de cada campo, los del JSON, para que una página escrita a mano, como Cambiar contraseña (RF-18), no los copie
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · los mensajes de resultado salen de pantalla.textos con el valor de la fila, como «Se guardó {nombre}.», y la prueba generada los toma del JSON
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · pantalla.inicial da el valor con que empieza un campo al registrar, sin tocar la base: el rol empieza en Usuario final
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · un decimal del formulario pasa a Yup como número: lee el texto con coma o con punto, el vacío es el dato que falta y sus validaciones van en el orden en que la API escoge el error, DecimalMax, DecimalMin y MaximoDecimales
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · un decimal que falla por dos validaciones da en Yup el mismo error que en la API: 100,55, el del máximo, y −0,55, el del mínimo, que van antes que el de los decimales
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · un valor que falla por dos validaciones da en Yup el mismo error que en la API: el NIT largo y mal escrito, el del patrón, y la contraseña con demasiadas eñes, el de los bytes
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · una contraseña opcional al editar va en el formulario: vacía no se envía y queda la que había; si llega, no puede estar en blanco y cumple las mismas validaciones, en el orden de la API
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · una entidad con edicion_en_bloque tiene su pantalla: una fila por cada valor de la lista que identifica las filas, en su orden, con los campos del formulario, su tipo y la regla de solo lectura (RF-13)
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · una lista obligatoria vacía da en Yup el error de obligatorio, como la API, y no el de sus valores: Yup revisa oneOf antes que las demás pruebas, así que el vacío cuenta como ausente
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · una pantalla necesita la API de su entidad: el alcance no acepta una pantalla sin API ni de una entidad que no existe
- [`test_frontend.py`](../generador/pruebas/test_frontend.py) · varias_lineas: true hace de un texto del formulario un campo de varias líneas, en una pantalla en bloque y en una con lista, como la recomendación de un rango (RF-13)
- [`test_java.py`](../generador/pruebas/test_java.py) · cada CRUD tiene persistencia y solo las entidades del alcance tienen API
- [`test_java.py`](../generador/pruebas/test_java.py) · cada entidad generada lleva @DynamicUpdate: cada UPDATE escribe solo las columnas que cambiaron, así dos cambios a columnas distintas de la misma fila no se pisan
- [`test_java.py`](../generador/pruebas/test_java.py) · edicion_en_bloque: lista las filas sin páginas, las edita todas juntas y restaura los valores por defecto, siempre en la empresa de la sesión (RF-13, RNF-03)
- [`test_java.py`](../generador/pruebas/test_java.py) · el código generado escribe sus constructores, sin las anotaciones de Lombok que los crean
- [`test_java.py`](../generador/pruebas/test_java.py) · el generador escoge el error de un campo como ManejadorDeErrores: primero las mismas restricciones de «falta el dato» y después por el nombre de la restricción
- [`test_java.py`](../generador/pruebas/test_java.py) · el generador se detiene con un efecto que no sabe llamar: en listar o ver, o atado a un atributo que no es una contraseña
- [`test_java.py`](../generador/pruebas/test_java.py) · el generador se detiene con una edición en bloque que no sabe generar: con crear, sin una lista que identifique cada fila o con restaurar_por_defecto sin valores_iniciales
- [`test_java.py`](../generador/pruebas/test_java.py) · el listado de una entidad con atributo_activo filtra con ?activo=true o false, y sin el filtro trae todos (D-33)
- [`test_java.py`](../generador/pruebas/test_java.py) · la API de una entidad por empresa toma la empresa de la sesión y limita cada consulta a ella (RNF-03)
- [`test_java.py`](../generador/pruebas/test_java.py) · la contraseña del formulario es obligatoria al crear y opcional al editar, y solo se guarda su hash
- [`test_java.py`](../generador/pruebas/test_java.py) · la prueba generada de la API rechaza el ejemplo de un patrón con algo pegado antes o después, como x900123456-8 y 900123456-8x: así revisa que el patrón vaya anclado con ^ y $
- [`test_java.py`](../generador/pruebas/test_java.py) · la prueba generada de la API revisa que cada validación del JSON acepte su valor del borde: el largo máximo, el mínimo y los 72 bytes
- [`test_java.py`](../generador/pruebas/test_java.py) · la prueba generada de la API revisa un valor que falla por dos validaciones: el NIT largo y mal escrito responde el error del patrón, y la contraseña con demasiadas eñes, el de los bytes
- [`test_java.py`](../generador/pruebas/test_java.py) · la respuesta solo lleva los atributos de la lista o del formulario: nunca el hash ni los datos internos
- [`test_java.py`](../generador/pruebas/test_java.py) · las reglas de una entidad por empresa reciben la sesión, y los parámetros de una regla salen como constantes
- [`test_java.py`](../generador/pruebas/test_java.py) · las validaciones del JSON pasan a Bean Validation con la clave de su mensaje
- [`test_java.py`](../generador/pruebas/test_java.py) · lo generado de empresa solo cambia a propósito: es igual a su copia de referencia, salvo el header
- [`test_java.py`](../generador/pruebas/test_java.py) · lo que se pega al ejemplo de un patrón es algo que el patrón no acepta en ese lugar, y no se pega nada si no cabe en el largo, porque el valor fallaría también por largo
- [`test_java.py`](../generador/pruebas/test_java.py) · los rangos por defecto de una empresa nueva salen de valores_iniciales del JSON
- [`test_java.py`](../generador/pruebas/test_java.py) · solo_lectura_si: en la edición en bloque, ese dato no cambia en las filas que cumplen la condición, con el mensaje del JSON o el general
- [`test_java.py`](../generador/pruebas/test_java.py) · un atributo con solo_lo_cambian_las_reglas sale en la entidad sin que JPA lo escriba al crear ni al editar, y empieza en su por_defecto, como la columna
- [`test_java.py`](../generador/pruebas/test_java.py) · un decimal acepta como máximo los decimales de su escala, con el mensaje general en singular o en plural
- [`test_java.py`](../generador/pruebas/test_java.py) · un decimal se guarda y se responde con la escala de su columna: el mapeador la ajusta al copiar la solicitud
- [`test_java.py`](../generador/pruebas/test_java.py) · un efecto de la contraseña se programa a mano y la edición generada lo llama solo si llega una contraseña nueva, después de guardar su hash
- [`test_java.py`](../generador/pruebas/test_java.py) · un efecto sin atributo se llama en cada operación de cuando, después de los cambios y antes de escribirlos en la base
- [`test_java.py`](../generador/pruebas/test_java.py) · una contraseña de solo espacios se rechaza como si faltara: al crear con @NotBlank y al editar, si llega, con @NoEnBlanco
- [`test_java.py`](../generador/pruebas/test_java.py) · una entidad por empresa no puede dar operaciones al administrador de la plataforma, que no tiene empresa
- [`test_java.py`](../generador/pruebas/test_java.py) · valores_en_formulario se valida con @ValorPermitido, que responde 403 antes que los demás errores
- [`test_otra_tematica.py`](../generador/pruebas/test_otra_tematica.py) · el JSON de producto de la pauta del docente pasa la validación
- [`test_otra_tematica.py`](../generador/pruebas/test_otra_tematica.py) · el JSON del café especial del Huila pasa la validación
- [`test_otra_tematica.py`](../generador/pruebas/test_otra_tematica.py) · el generador produce la API de producto, otra temática, con las mismas plantillas
- [`test_otra_tematica.py`](../generador/pruebas/test_otra_tematica.py) · el generador produce la API del café especial, por empresa, con sus pruebas de RNF-03
- [`test_otra_tematica.py`](../generador/pruebas/test_otra_tematica.py) · el generador produce la pantalla de producto, otra temática, con las mismas plantillas del frontend
- [`test_otra_tematica.py`](../generador/pruebas/test_otra_tematica.py) · el generador produce la pantalla del café especial: sus listas sin valor por defecto empiezan vacías
- [`test_otra_tematica.py`](../generador/pruebas/test_otra_tematica.py) · el generador produce la tabla del café especial: por empresa y con los valores de sus listas
- [`test_otra_tematica.py`](../generador/pruebas/test_otra_tematica.py) · el generador produce los archivos de otra temática: producto de la pauta del docente
- [`test_salida.py`](../generador/pruebas/test_salida.py) · comprobar detecta archivos que faltan o que sobran en las carpetas generadas
- [`test_salida.py`](../generador/pruebas/test_salida.py) · comprobar detecta un archivo generado editado a mano
- [`test_salida.py`](../generador/pruebas/test_salida.py) · comprobar ignora la diferencia entre LF y CRLF
- [`test_salida.py`](../generador/pruebas/test_salida.py) · comprobar no encuentra diferencias justo después de generar
- [`test_salida.py`](../generador/pruebas/test_salida.py) · el generador también es dueño de frontend/src/generated y frontend/e2e/generated: escribe ahí, comprobar detecta una edición a mano y generar borra lo que sobra, sin tocar lo escrito a mano
- [`test_salida.py`](../generador/pruebas/test_salida.py) · generar borra de las carpetas generadas lo que ya no se genera
- [`test_salida.py`](../generador/pruebas/test_salida.py) · la salida es determinista y usa finales de línea LF
- [`test_salida.py`](../generador/pruebas/test_salida.py) · lo generado no lleva valores de Python: un booleano sale como true o false, también en los ejemplos de OpenAPI y en el frontend, y nada sale como None
- [`test_validacion.py`](../generador/pruebas/test_validacion.py) · el resumen y el texto de los activos pueden usar {maximo}, el de la regla de limite_de_activos, solo si la pantalla la declara
- [`test_validacion.py`](../generador/pruebas/test_validacion.py) · en_la_fila_propia va solo en usuario, la entidad de quien tiene la sesión: en una regla de desactivar sin atributo o en una de editar atada a un atributo del formulario
- [`test_validacion.py`](../generador/pruebas/test_validacion.py) · la validación conoce los textos de la fila propia y de la contraseña al editar; la nota de la contraseña de otra persona usa los valores de la fila, como {nombre}
- [`test_validacion.py`](../generador/pruebas/test_validacion.py) · la validación conoce los textos de la pantalla en bloque: la nota y el detalle de restaurar y el mensaje al restaurar, sin los valores de una fila
- [`test_validacion.py`](../generador/pruebas/test_validacion.py) · la validación detecta un CHECK con un valor que no está en ninguna lista
- [`test_validacion.py`](../generador/pruebas/test_validacion.py) · la validación detecta una ficha que no existe en specs/
- [`test_validacion.py`](../generador/pruebas/test_validacion.py) · la validación detecta una referencia a una entidad que no existe
- [`test_validacion.py`](../generador/pruebas/test_validacion.py) · la validación pide el nombre de crear_con, que es el campo de la solicitud
- [`test_validacion.py`](../generador/pruebas/test_validacion.py) · la validación rechaza solo_lectura_si en una entidad sin edicion_en_bloque, en vez de ignorarlo
- [`test_validacion.py`](../generador/pruebas/test_validacion.py) · la validación rechaza solo_lo_cambian_las_reglas donde no serviría: en un atributo del formulario, en una entidad sin CRUD y en un atributo obligatorio sin por_defecto
- [`test_validacion.py`](../generador/pruebas/test_validacion.py) · la validación rechaza un mensaje con un marcador que no está en validaciones
- [`test_validacion.py`](../generador/pruebas/test_validacion.py) · la validación revisa la clave pantalla: claves conocidas, textos en español y en inglés, singular y plural donde se cuenta, y marcadores que existen
- [`test_validacion.py`](../generador/pruebas/test_validacion.py) · la validación revisa las claves nuevas de pantalla: formulario encima o al lado, limite_de_activos con una regla de validación con maximo en crear o reactivar, e inicial con valores que el formulario ofrece
- [`test_validacion.py`](../generador/pruebas/test_validacion.py) · la validación revisa que el ejemplo de un atributo cumpla sus validaciones
- [`test_validacion.py`](../generador/pruebas/test_validacion.py) · los JSON de specs/entidades pasan la validación
- [`test_validacion.py`](../generador/pruebas/test_validacion.py) · un atributo con solo_lectura_si puede traer su mensaje; uno sin solo_lectura_si, no
- [`test_validacion.py`](../generador/pruebas/test_validacion.py) · un atributo no puede usar el nombre de una columna implícita
- [`test_validacion.py`](../generador/pruebas/test_validacion.py) · un patron va anclado con ^ y $ y sin lo que Java, PostgreSQL y JavaScript leen distinto, así los tres validan igual
- [`test_validacion.py`](../generador/pruebas/test_validacion.py) · una entidad con CRUD necesita modulo y ruta
- [`test_validacion.py`](../generador/pruebas/test_validacion.py) · varias_lineas va solo en un texto del formulario, y solo con true

### D-18 · Uso de los 5 modelos (RP-25)

- [`EsquemaIT`](../backend/src/test/java/co/edu/usco/inspectia/EsquemaIT.java) · solo un modelo puede estar en uso

### D-21 · Arquitectura del software

- [`InspectiaAplicacionIT`](../backend/src/test/java/co/edu/usco/inspectia/InspectiaAplicacionIT.java) · el backend arranca con sus módulos, Flyway y PostgreSQL 18

### D-27 · El administrador de la plataforma puede desactivar una empresa

- [`EdicionEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/EdicionEmpresaIT.java) · al editar, los datos obligatorios se validan igual que en el registro

### D-29 · Estados de las fichas (amplía D-07)

- [`test_casos.py`](../generador/pruebas/test_casos.py) · comprobar falla si una ficha «Implementado» tiene criterios sin prueba; generar solo avisa
- [`test_casos.py`](../generador/pruebas/test_casos.py) · ninguna ficha del repositorio está «Implementado» con criterios sin prueba
- [`test_casos.py`](../generador/pruebas/test_casos.py) · una ficha «Implementado» con criterios sin prueba se señala; los eliminados no cuentan

### D-30 · Quién cambia o restablece cada contraseña

- [`RestablecerAdminPlataformaIT`](../backend/src/test/java/co/edu/usco/inspectia/RestablecerAdminPlataformaIT.java) · con el reloj de la base adelantado respecto al del backend, un inicio de sesión hecho justo después del script sirve
- [`RestablecerAdminPlataformaIT`](../backend/src/test/java/co/edu/usco/inspectia/RestablecerAdminPlataformaIT.java) · el script corta las sesiones abiertas del administrador y quita su bloqueo por intentos fallidos: entra de una vez con la nueva
- [`RestablecerAdminPlataformaIT`](../backend/src/test/java/co/edu/usco/inspectia/RestablecerAdminPlataformaIT.java) · el script corta también una sesión abierta con el reloj de la base atrasado respecto al del backend: el corte no depende de la hora
- [`RestablecerAdminPlataformaIT`](../backend/src/test/java/co/edu/usco/inspectia/RestablecerAdminPlataformaIT.java) · el script no cambia nada si falta un dato, si el correo no es de ningún usuario o si sus caracteres no venían en UTF-8 (parametrizada)
- [`RestablecerAdminPlataformaIT`](../backend/src/test/java/co/edu/usco/inspectia/RestablecerAdminPlataformaIT.java) · el script no reactiva a un administrador desactivado
- [`RestablecerAdminPlataformaIT`](../backend/src/test/java/co/edu/usco/inspectia/RestablecerAdminPlataformaIT.java) · el script no toca a un usuario de una empresa: su contraseña se restablece desde la app
- [`RestablecerAdminPlataformaIT`](../backend/src/test/java/co/edu/usco/inspectia/RestablecerAdminPlataformaIT.java) · el script no toma la contraseña de INSPECTIA_ADMIN_CONTRASENA, la del script de creación que puede seguir en infra/.env: si falta la nueva, no cambia nada
- [`RestablecerAdminPlataformaIT`](../backend/src/test/java/co/edu/usco/inspectia/RestablecerAdminPlataformaIT.java) · el script restablece la contraseña del administrador de la plataforma: entra la nueva y no la anterior, con un hash que Spring Security acepta
- [`RestablecerAdminPlataformaIT`](../backend/src/test/java/co/edu/usco/inspectia/RestablecerAdminPlataformaIT.java) · la contraseña nueva acepta de 8 a 64 caracteres, con los mensajes de la API (prueba de límite) (parametrizada)
- [`RestablecerAdminPlataformaIT`](../backend/src/test/java/co/edu/usco/inspectia/RestablecerAdminPlataformaIT.java) · la contraseña nueva no sale en pantalla ni en el registro de PostgreSQL, aunque el servidor registre todas las sentencias
- [`RestablecerAdminPlataformaIT`](../backend/src/test/java/co/edu/usco/inspectia/RestablecerAdminPlataformaIT.java) · la contraseña nueva ocupa como máximo 72 bytes, con el mensaje de la API (prueba de límite) (parametrizada)
- [`RestablecerAdminPlataformaIT`](../backend/src/test/java/co/edu/usco/inspectia/RestablecerAdminPlataformaIT.java) · una contraseña nueva de solo espacios cuenta como si faltara; con una letra se acepta (prueba de límite) (parametrizada)
- [`CambiosALaVezIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/CambiosALaVezIT.java) · el quinto fallo seguido que llega mientras se restablece la contraseña no deja la cuenta bloqueada: el usuario entra de una vez con la nueva
- [`CambioDeContrasenaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/CambioDeContrasenaIT.java) · cambiar la propia contraseña deja los intentos fallidos en cero
