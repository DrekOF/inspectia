<!--
Nombre: casos-de-prueba.md
Descripción: Casos de prueba de InspectIA por ficha y criterio de aceptación, y los criterios que aún no
             tienen prueba (RP-13). Archivo generado por generador/casos_de_prueba.py: no se edita a mano;
             sale de specs/fichas.json y de las marcas de las pruebas.
Fecha de creación: 12/10/2026
Actualización: 18/10/2026
Autor: Equipo InspectIA (generado por generador/casos_de_prueba.py v1.1)
Versión: 1.1
-->

# Casos de prueba

Cada criterio de aceptación de `specs/fichas.json` con las pruebas que lo cubren, y los que todavía no tienen ninguna (RP-13). Cada prueba dice qué cubre con su marca: `@DisplayName` en Java y `@pytest.mark.caso` en Python, con la forma «<código> · C<n> · …» si cubre un criterio o «<código> · …» si no (ver `CLAUDE.md`).

Este documento no se edita a mano. Después de agregar o cambiar pruebas, o de cambiar las fichas, se vuelve a armar:

```bash
generador/.venv/Scripts/python generador/casos_de_prueba.py generar
```

## Resumen

- **140 criterios de aceptación** en 26 fichas: 26 con prueba y 114 pendientes.
- **135 pruebas:** 92 de Java (6 generadas) y 43 de Python. Una prueba parametrizada cuenta una vez.

| Ficha | Criterios | Con prueba | Pendientes |
|---|---:|---:|---:|
| RF-01 · Iniciar sesión según el rol | 8 | 8 | 0 |
| RF-02 · Registrar empresas y su primer administrador | 12 | 11 | 1 |
| RF-03 · Gestionar los usuarios de la empresa | 10 | 0 | 10 |
| RF-04 · Registrar un lote | 4 | 1 | 3 |
| RF-05 · Tomar o subir las fotos de la muestra del lote, con guía de captura | 5 | 0 | 5 |
| RF-06 · Validar la calidad de cada foto (nitidez y luz) y pedir repetirla si no sirve | 3 | 0 | 3 |
| RF-07 · Separar y recortar los granos de cada foto, con control de granos pegados | 5 | 0 | 5 |
| RF-08 · Clasificar cada grano como sano o no sano con el modelo de IA | 6 | 0 | 6 |
| RF-09 · Pedir una foto más clara o más fotos cuando la confianza del modelo es baja | 6 | 0 | 6 |
| RF-10 · Calcular el porcentaje de granos sanos del lote | 7 | 0 | 7 |
| RF-11 · Mostrar el resultado con la foto marcada (cada grano sano o no sano) | 5 | 0 | 5 |
| RF-12 · Generar la recomendación según los rangos de la empresa | 10 | 0 | 10 |
| RF-13 · Configurar los rangos de recomendación de la empresa | 5 | 0 | 5 |
| RF-14 · Consultar el historial de lotes evaluados | 6 | 0 | 6 |
| RF-15 · Descargar el reporte del lote en PDF | 5 | 0 | 5 |
| RF-16 · Consultar las estadísticas de la empresa | 4 | 0 | 4 |
| RF-17 · Consultar las métricas del modelo | 3 | 0 | 3 |
| RF-18 · Cambiar la propia contraseña | 4 | 0 | 4 |
| RNF-01 · Entregar el resultado del análisis de un lote en 5 a 8 segundos | 4 | 0 | 4 |
| RNF-02 · Soportar hasta 30 empresas con 25 usuarios cada una | 4 | 0 | 4 |
| RNF-03 · Mantener separados los datos de cada empresa | 5 | 1 | 4 |
| RNF-04 · Exigir autenticación y restringir cada función según el rol | 5 | 5 | 0 |
| RNF-05 · Funcionar en PC y en celular desde el navegador | 3 | 0 | 3 |
| RNF-06 · Ser sencilla de usar para el usuario final | 4 | 0 | 4 |
| RNF-07 · Alcanzar el F1-score mínimo del modelo de IA | 4 | 0 | 4 |
| RNF-08 · Citar a los autores del dataset y no usarlo con fines comerciales | 3 | 0 | 3 |

## RF-01 · Iniciar sesión según el rol

| Criterio | Pruebas |
|---|---|
| C1. Con datos correctos, el sistema muestra el menú del rol del usuario. | [`InicioSesionIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/InicioSesionIT.java) · con datos correctos, cada rol inicia sesión y recibe su rol, su nombre y su empresa para armar su menú (parametrizada)<br>[`SesionServicioTest`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionServicioTest.java) · con la contraseña correcta, los intentos vuelven a cero y entrega el token con el usuario, su rol y su empresa |
| C2. Con datos incorrectos, muestra «Correo o contraseña incorrectos», sin decir cuál de los dos falló. | [`InicioSesionIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/InicioSesionIT.java) · con datos incorrectos responde «Correo o contraseña incorrectos», igual si falló el correo o la contraseña<br>[`SesionServicioTest`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionServicioTest.java) · si el correo no existe, la contraseña igual se compara con BCrypt, para que la respuesta tarde lo mismo |
| C3. Un usuario desactivado no puede iniciar sesión. | [`InicioSesionIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/InicioSesionIT.java) · un usuario desactivado no puede iniciar sesión<br>[`SesionAbiertaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionAbiertaIT.java) · al desactivar un usuario, su sesión abierta deja de servir en la siguiente petición |
| C4. Un usuario de una empresa desactivada no puede iniciar sesión (D-27). | [`InicioSesionIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/InicioSesionIT.java) · un usuario de una empresa desactivada no puede iniciar sesión<br>[`SesionAbiertaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionAbiertaIT.java) · al desactivar una empresa, las sesiones abiertas de sus usuarios dejan de servir |
| C5. Después de 5 intentos fallidos seguidos, la cuenta se bloquea 15 minutos. | [`InicioSesionIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/InicioSesionIT.java) · después de 5 intentos fallidos seguidos la cuenta se bloquea: ni la contraseña correcta entra hasta que pasen 15 minutos<br>[`InicioSesionIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/InicioSesionIT.java) · los intentos fallidos deben ser seguidos: iniciar sesión vuelve a contar desde cero<br>[`SesionServicioTest`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionServicioTest.java) · durante el bloqueo, ni la contraseña correcta entra; el intento no cuenta y también se compara con BCrypt<br>[`SesionServicioTest`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionServicioTest.java) · el bloqueo dura hasta su hora exacta: un segundo antes no entra y en ese segundo ya entra (prueba de límite) (parametrizada)<br>[`SesionServicioTest`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionServicioTest.java) · un fallo se registra con la hora del servidor y, si es el quinto seguido, bloquea la cuenta 15 minutos desde ese momento |
| C6. La sesión vence a las 8 horas y el sistema pide iniciar sesión de nuevo. | [`ValidadorDeVigenciaTest`](../backend/src/test/java/co/edu/usco/inspectia/comun/seguridad/ValidadorDeVigenciaTest.java) · el token sirve hasta un segundo antes de vencer; desde el segundo en que vence, ya no (prueba de límite) (parametrizada)<br>[`SesionAbiertaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionAbiertaIT.java) · con la sesión vencida se puede volver a iniciar sesión, aunque el frontend todavía envíe el token viejo<br>[`SesionAbiertaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionAbiertaIT.java) · la sesión vence a las 8 horas y pide iniciar sesión de nuevo (prueba de límite) (parametrizada) |
| C7. Al terminar el bloqueo, la cuenta vuelve a tener 5 intentos: un fallo más no la bloquea. | [`InicioSesionIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/InicioSesionIT.java) · al terminar el bloqueo, la cuenta vuelve a tener 5 intentos: un fallo más no la bloquea |
| C8. Una contraseña de más de 72 bytes nunca inicia sesión, aunque sus primeros 72 bytes coincidan con la guardada. Recibe la misma respuesta y tarda lo mismo que cualquier intento fallido, y cuenta como uno. | [`InicioSesionIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/InicioSesionIT.java) · una contraseña de más de 72 bytes nunca inicia sesión, aunque sus primeros 72 bytes coincidan con la guardada, y cuenta como intento fallido<br>[`SesionServicioTest`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionServicioTest.java) · una contraseña de más de 72 bytes no entra aunque BCrypt diga que coincide, cuenta como fallo y también se compara con BCrypt |

Otras pruebas de RF-01, que no cubren un criterio:

- [`CrearAdminPlataformaIT`](../backend/src/test/java/co/edu/usco/inspectia/CrearAdminPlataformaIT.java) · el script crea el administrador de la plataforma sin empresa y con un hash que Spring Security acepta
- [`CrearAdminPlataformaIT`](../backend/src/test/java/co/edu/usco/inspectia/CrearAdminPlataformaIT.java) · el script no cambia nada si el administrador ya existe, así que se puede correr varias veces
- [`CrearAdminPlataformaIT`](../backend/src/test/java/co/edu/usco/inspectia/CrearAdminPlataformaIT.java) · el script no crea nada si falta un dato o si el nombre o el correo no son válidos (parametrizada)
- [`CrearAdminPlataformaIT`](../backend/src/test/java/co/edu/usco/inspectia/CrearAdminPlataformaIT.java) · el script no toca un usuario de una empresa que tenga el mismo correo
- [`CrearAdminPlataformaIT`](../backend/src/test/java/co/edu/usco/inspectia/CrearAdminPlataformaIT.java) · la contraseña del administrador acepta de 8 a 64 caracteres (prueba de límite) (parametrizada)
- [`CrearAdminPlataformaIT`](../backend/src/test/java/co/edu/usco/inspectia/CrearAdminPlataformaIT.java) · la contraseña del administrador ocupa como máximo 72 bytes, con el mensaje de RF-02 (prueba de límite) (parametrizada)
- [`CrearAdminPlataformaIT`](../backend/src/test/java/co/edu/usco/inspectia/CrearAdminPlataformaIT.java) · la contraseña no sale en pantalla ni en el registro de PostgreSQL, aunque el servidor registre todas las sentencias
- [`InicioSesionIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/InicioSesionIT.java) · el correo se compara sin distinguir mayúsculas ni espacios al inicio o al final
- [`InicioSesionIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/InicioSesionIT.java) · si falta el correo o la contraseña, responde 400 con cada campo que falta

## RF-02 · Registrar empresas y su primer administrador

| Criterio | Pruebas |
|---|---|
| C1. Con datos válidos, la empresa y su administrador quedan registrados. | [`RegistroEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/RegistroEmpresaIT.java) · con datos válidos, la empresa y su administrador quedan registrados |
| C2. Si el NIT o el correo del administrador ya existen, el sistema no registra la empresa y lo avisa. | [`RegistroEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/RegistroEmpresaIT.java) · si el NIT ya existe, no registra la empresa y lo avisa<br>[`RegistroEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/RegistroEmpresaIT.java) · si el correo del administrador ya existe, aunque cambien las mayúsculas, no registra la empresa y lo avisa |
| C3. Si faltan datos obligatorios, el formulario marca los campos que faltan. | [`RegistroEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/RegistroEmpresaIT.java) · si falta el administrador, lo marca y no guarda nada<br>[`RegistroEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/RegistroEmpresaIT.java) · si faltan datos obligatorios, responde un error por cada campo y no guarda nada |
| C4. Si el dígito de verificación del NIT no corresponde, el formulario lo marca y no registra la empresa. | [`RegistroEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/RegistroEmpresaIT.java) · si el dígito de verificación del NIT no corresponde, lo marca y no registra la empresa<br>[`DigitoVerificacionNitTest`](../backend/src/test/java/co/edu/usco/inspectia/empresa/reglas/DigitoVerificacionNitTest.java) · acepta un NIT con su dígito de verificación correcto (parametrizada)<br>[`DigitoVerificacionNitTest`](../backend/src/test/java/co/edu/usco/inspectia/empresa/reglas/DigitoVerificacionNitTest.java) · calcula el dígito de verificación con el algoritmo de la DIAN (parametrizada)<br>[`DigitoVerificacionNitTest`](../backend/src/test/java/co/edu/usco/inspectia/empresa/reglas/DigitoVerificacionNitTest.java) · rechaza un NIT cuyo dígito de verificación no corresponde (parametrizada)<br>[`DigitoVerificacionNitTest`](../backend/src/test/java/co/edu/usco/inspectia/empresa/reglas/DigitoVerificacionNitTest.java) · rechaza un NIT sin dígito de verificación o con otro formato (parametrizada) |
| C5. La contraseña inicial acepta de 8 a 64 caracteres: con 7 o con 65 se rechaza (prueba de límite). | [`RegistroEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/RegistroEmpresaIT.java) · la contraseña inicial acepta de 8 a 64 caracteres (prueba de límite) (parametrizada) |
| C6. Una contraseña inicial de 64 caracteres con 8 eñes (72 bytes) se acepta; con 9 eñes (73 bytes) se rechaza con el mensaje «La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes» (prueba de límite). | [`RegistroEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/RegistroEmpresaIT.java) · la contraseña inicial ocupa como máximo 72 bytes: con 8 eñes se acepta y con 9 se rechaza con el mensaje de la ficha (prueba de límite) (parametrizada) |
| C7. La empresa nueva queda con los rangos por defecto de D-10. | [`RegistroEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/RegistroEmpresaIT.java) · la empresa nueva queda con los rangos por defecto de D-10 |
| C8. El administrador creado puede iniciar sesión con la contraseña inicial. | [`RegistroEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/RegistroEmpresaIT.java) · el administrador queda activo, en una empresa activa y con la contraseña inicial (el inicio de sesión se prueba en RF-01)<br>[`InicioSesionIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/InicioSesionIT.java) · el administrador creado con la empresa inicia sesión con su contraseña inicial |
| C9. Al desactivar una empresa, sus usuarios no pueden iniciar sesión y sus lotes y resultados se conservan (D-27). | [`EdicionEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/EdicionEmpresaIT.java) · al desactivar una empresa no se borra nada: sus usuarios, rangos y lotes se conservan<br>[`InicioSesionIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/InicioSesionIT.java) · al desactivar una empresa, sus usuarios no pueden iniciar sesión |
| C10. Al reactivar una empresa, sus usuarios activos pueden volver a iniciar sesión (D-27). | [`EdicionEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/EdicionEmpresaIT.java) · al reactivar una empresa queda activa otra vez y cada usuario conserva su estado<br>[`InicioSesionIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/InicioSesionIT.java) · al reactivar una empresa, sus usuarios activos vuelven a iniciar sesión y los desactivados no |
| C11. Al editar una empresa, el NIT pasa por las mismas validaciones del registro: único y con dígito de verificación válido (D-27). | [`EdicionEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/EdicionEmpresaIT.java) · al editar, la empresa puede conservar su propio NIT y guardar los demás cambios<br>[`EdicionEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/EdicionEmpresaIT.java) · al editar, un NIT con dígito de verificación inválido responde 400 y no cambia nada<br>[`EdicionEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/EdicionEmpresaIT.java) · al editar, un NIT de otra empresa responde 409 y no cambia nada |
| C12. El administrador de la plataforma puede restablecer la contraseña de un administrador de empresa; la nueva contraseña pasa por las mismas validaciones de la contraseña inicial. | **Pendiente** |

Otras pruebas de RF-02, que no cubren un criterio:

- [`EmpresaApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/empresa/EmpresaApiGeneradaIT.java) · cada validación del JSON de empresa responde 400 con su mensaje al crear (parametrizada, generada)
- [`EmpresaApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/empresa/EmpresaApiGeneradaIT.java) · cada validación del JSON de empresa responde 400 con su mensaje al editar (parametrizada, generada)
- [`EmpresaApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/empresa/EmpresaApiGeneradaIT.java) · empresa guarda sin espacios al inicio ni al final los textos que su JSON pide recortar (generada)
- [`EmpresaApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/empresa/EmpresaApiGeneradaIT.java) · los datos únicos del JSON de empresa no se repiten: responde 409 (generada)
- [`EdicionEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/EdicionEmpresaIT.java) · ver, editar, desactivar o reactivar una empresa que no existe responde 404 (parametrizada)
- [`EmpresaServicioTest`](../backend/src/test/java/co/edu/usco/inspectia/empresa/EmpresaServicioTest.java) · al crear, guarda la empresa, su administrador con la contraseña codificada y los rangos, en ese orden
- [`EmpresaServicioTest`](../backend/src/test/java/co/edu/usco/inspectia/empresa/EmpresaServicioTest.java) · al crear, si la regla del NIT no se cumple, el servicio no guarda nada
- [`EmpresaServicioTest`](../backend/src/test/java/co/edu/usco/inspectia/empresa/EmpresaServicioTest.java) · al editar, revisa la regla con los datos nuevos y, si no se cumple, no cambia la empresa
- [`EmpresaServicioTest`](../backend/src/test/java/co/edu/usco/inspectia/empresa/EmpresaServicioTest.java) · crear guarda la empresa, su administrador y sus rangos en una sola transacción
- [`RegistroEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/RegistroEmpresaIT.java) · la empresa registrada aparece en el listado

## RF-03 · Gestionar los usuarios de la empresa

| Criterio | Pruebas |
|---|---|
| C1. Un usuario creado con datos válidos puede iniciar sesión. | **Pendiente** |
| C2. No se puede crear un usuario con un correo que ya existe en la plataforma. | **Pendiente** |
| C3. La contraseña acepta de 8 a 64 caracteres: con 7 o con 65 se rechaza (prueba de límite). | **Pendiente** |
| C4. Una contraseña de 64 caracteres con 8 eñes (72 bytes) se acepta; con 9 eñes (73 bytes) se rechaza con el mensaje «La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes» (prueba de límite). | **Pendiente** |
| C5. Con 25 usuarios activos, el sistema no deja crear ni reactivar otro, y explica por qué. | **Pendiente** |
| C6. Un usuario desactivado no puede iniciar sesión y sus análisis siguen en el historial. | **Pendiente** |
| C7. El administrador no puede desactivarse a sí mismo. | **Pendiente** |
| C8. El administrador no puede cambiar su propio rol: si lo intenta, el sistema no guarda el cambio y explica por qué. | **Pendiente** |
| C9. No puede ver ni editar usuarios de otra empresa. | **Pendiente** |
| C10. Si un administrador de empresa envía a la API un usuario con rol ADMIN_PLATAFORMA, la API responde «prohibido» (403) y no guarda nada (prueba de seguridad, RP-12). | **Pendiente** |

## RF-04 · Registrar un lote

| Criterio | Pruebas |
|---|---|
| C1. Con datos válidos, el lote queda registrado y el sistema pasa a la captura de fotos. | **Pendiente** |
| C2. Si el código ya existe en la empresa, el sistema lo avisa y no registra el lote. | **Pendiente** |
| C3. El código «l-001 » cuenta como repetido si ya existe «L-001». | [`EsquemaIT`](../backend/src/test/java/co/edu/usco/inspectia/EsquemaIT.java) · la base trata «l-001 » y «L-001» como el mismo código de la empresa |
| C4. La fecha de recepción no puede ser posterior a hoy. | **Pendiente** |

## RF-05 · Tomar o subir las fotos de la muestra del lote, con guía de captura

| Criterio | Pruebas |
|---|---|
| C1. En el celular, el botón abre la cámara; en el PC, permite elegir archivos. | **Pendiente** |
| C2. El sistema exige al menos 2 fotos y acepta como máximo 3. | **Pendiente** |
| C3. Rechaza archivos que no sean JPG o PNG o que pasen de 10 MB, con un mensaje claro. | **Pendiente** |
| C4. La guía de captura se puede consultar antes de cada foto. | **Pendiente** |
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
| C4. El modelo cumple el F1-score mínimo de RNF-07 en el conjunto de validación. | **Pendiente** |
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
| C1. Con 93 granos sanos de 100, el resultado es 93,0 %. | **Pendiente** |
| C2. Con 141 granos sanos de 150, el resultado es 94,0 %. | **Pendiente** |
| C3. Con 100 granos sanos de 107, el resultado es 93,5 % (redondeo a un decimal). | **Pendiente** |
| C4. Con 2 muestras, 107 sanos de 120 y 113 de 125 dan 220 de 245 = 89,8 % (D-25). | **Pendiente** |
| C5. Con 2 muestras de distinto tamaño se suman los granos: 109 sanos de 120 (90,8 %) y 120 de 150 (80,0 %) dan 229 de 270 = 84,8 %, no el promedio de los porcentajes (85,4 %). | **Pendiente** |
| C6. El porcentaje se guarda con el lote, y cada muestra guarda sus conteos y el modelo que la analizó (D-18, D-25). | **Pendiente** |
| C7. El cálculo tiene pruebas unitarias y de mutación (RP-12) que no dependen del servicio de IA (D-21). | **Pendiente** |

## RF-11 · Mostrar el resultado con la foto marcada (cada grano sano o no sano)

| Criterio | Pruebas |
|---|---|
| C1. Cada grano clasificado aparece marcado en la foto: verde si es sano y rojo si no lo es. | **Pendiente** |
| C2. Los granos sanos y no sanos se distinguen también por un símbolo, no solo por el color. | **Pendiente** |
| C3. Las cifras de la pantalla coinciden con el cálculo de RF-10. | **Pendiente** |
| C4. Al tocar un grano, o al pasar el cursor sobre él, se ve su clase; el valor de la confianza no se muestra al usuario final (sección 1). | **Pendiente** |
| C5. La pantalla se ve bien en celular y en PC (RNF-05). | **Pendiente** |

## RF-12 · Generar la recomendación según los rangos de la empresa

| Criterio | Pruebas |
|---|---|
| C1. Con 99 %, la categoría es Premium o Especial, sin sugerencia (está a 3 puntos de 96). | **Pendiente** |
| C2. Con 93 %, la categoría es Estándar, sin sugerencia (está a 3 puntos de 90 y de 96). | **Pendiente** |
| C3. Con 90,9 %, la categoría es Estándar y sugiere analizar otra muestra (está a 0,9 puntos de 90); con 91,0 % no la sugiere (está a 1 punto). Es la prueba de límite del margen (D-24). | **Pendiente** |
| C4. Con 90,0 % la categoría es Estándar y con 89,9 % es Corriente; ambos sugieren otra muestra (prueba de límite). | **Pendiente** |
| C5. Con 75 %, la categoría es Fuera de norma, sin sugerencia. | **Pendiente** |
| C6. Con 143 granos sanos de 149 (95,97 %, redondeado a 96,0 %), la categoría es Premium o Especial. | **Pendiente** |
| C7. Con 2 muestras, la categoría sale de la suma de sus granos: 220 sanos de 245 (89,8 %) es Corriente, y no se sugiere otra muestra aunque quede a 0,2 puntos de 90 (D-25). | **Pendiente** |
| C8. Un lote sin sugerencia, o que ya tiene 2 muestras, no permite agregar otra (D-25). | **Pendiente** |
| C9. Si la empresa cambió sus rangos (RF-13), se usan los de la empresa. | **Pendiente** |
| C10. La categoría siempre se muestra como probable. | **Pendiente** |

## RF-13 · Configurar los rangos de recomendación de la empresa

| Criterio | Pruebas |
|---|---|
| C1. Si los límites no bajan en orden (por ejemplo, Estándar 97 % y Premium 96 %), el sistema no guarda y explica el error. | **Pendiente** |
| C2. Un análisis hecho después del cambio usa los rangos nuevos. | **Pendiente** |
| C3. Los análisis anteriores no cambian. | **Pendiente** |
| C4. «Restaurar valores por defecto» vuelve a los rangos de D-10. | **Pendiente** |
| C5. Un usuario final no puede entrar a esta pantalla (RNF-04). | **Pendiente** |

## RF-14 · Consultar el historial de lotes evaluados

| Criterio | Pruebas |
|---|---|
| C1. La lista muestra solo lotes de la empresa del usuario. | **Pendiente** |
| C2. Los filtros por fecha del resultado, estado, categoría, código y usuario funcionan solos o combinados. | **Pendiente** |
| C3. El detalle muestra las mismas cifras del resultado guardado y las de cada muestra. | **Pendiente** |
| C4. Con más de 20 lotes, la lista se divide en páginas. | **Pendiente** |
| C5. Un lote pendiente se puede retomar desde la captura de fotos. | **Pendiente** |
| C6. Un lote con otra muestra sugerida permite analizarla desde el detalle (D-25). | **Pendiente** |

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
| C1. Con la contraseña actual correcta y una nueva válida, la contraseña cambia: la nueva inicia sesión y la anterior ya no. | **Pendiente** |
| C2. Con la contraseña actual errada no cambia nada, y el intento cuenta para el bloqueo de RF-01: después de 5 fallos seguidos, la cuenta se bloquea 15 minutos y tampoco se puede cambiar la contraseña hasta que termine. | **Pendiente** |
| C3. La contraseña nueva acepta de 8 a 64 caracteres: con 7 o con 65 se rechaza (prueba de límite). | **Pendiente** |
| C4. Una contraseña nueva de 64 caracteres con 8 eñes (72 bytes) se acepta; con 9 eñes (73 bytes) se rechaza con el mensaje «La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes» (prueba de límite). | **Pendiente** |

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
| C1. Si un usuario pide un lote de otra empresa por su identificador, el sistema responde que no existe. | **Pendiente** |
| C2. Las listas y las estadísticas nunca mezclan datos de empresas distintas. | **Pendiente** |
| C3. Las fotos y los reportes en S3 solo se abren con enlaces temporales generados para la empresa dueña. | **Pendiente** |
| C4. Solo el administrador de la plataforma puede no tener empresa: la base de datos rechaza un usuario de otro rol sin empresa. | [`EsquemaIT`](../backend/src/test/java/co/edu/usco/inspectia/EsquemaIT.java) · la base rechaza un usuario sin empresa que no sea ADMIN_PLATAFORMA |
| C5. Hay pruebas de seguridad que lo verifican (RP-12). | **Pendiente** |

Otras pruebas de RNF-03, que no cubren un criterio:

- [`EsquemaIT`](../backend/src/test/java/co/edu/usco/inspectia/EsquemaIT.java) · un lote no puede apuntar a un usuario de otra empresa

## RNF-04 · Exigir autenticación y restringir cada función según el rol

| Criterio | Pruebas |
|---|---|
| C1. Sin sesión, la API responde «no autorizado» (401). | [`EmpresaApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/empresa/EmpresaApiGeneradaIT.java) · sin sesión, cada operación de empresa responde 401 (parametrizada, generada)<br>[`SeguridadEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/SeguridadEmpresaIT.java) · sin sesión, cada operación de la API de empresas responde 401 en JSON (parametrizada)<br>[`SesionAbiertaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionAbiertaIT.java) · sin token, la sesión actual responde 401 |
| C2. Con un rol sin permiso, responde «prohibido» (403); por ejemplo, cuando un usuario final intenta gestionar usuarios. | [`EmpresaApiGeneradaIT`](../backend/src/test-generated/java/co/edu/usco/inspectia/empresa/EmpresaApiGeneradaIT.java) · cada operación de empresa solo la usan los roles de su JSON (parametrizada, generada)<br>[`SeguridadEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/SeguridadEmpresaIT.java) · el permiso se revisa antes que los datos: un usuario final recibe 403 aunque envíe datos inválidos<br>[`SeguridadEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/SeguridadEmpresaIT.java) · un administrador de empresa o un usuario final recibe 403 en cada operación de la API de empresas (parametrizada)<br>[`SesionAbiertaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionAbiertaIT.java) · con el token de un administrador de empresa, la API de empresas responde 403; con el de la plataforma, 200 |
| C3. Las contraseñas se guardan con hash (BCrypt), nunca en texto plano. | [`RegistroEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/RegistroEmpresaIT.java) · la contraseña del administrador se guarda con hash BCrypt y no sale en la respuesta |
| C4. La sesión vence a las 8 horas. | [`EmisorDeTokensTest`](../backend/src/test/java/co/edu/usco/inspectia/comun/seguridad/EmisorDeTokensTest.java) · el token vence 8 horas después de emitido, en segundos enteros, y solo lleva el emisor, el usuario y las fechas<br>[`SesionAbiertaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionAbiertaIT.java) · el token del inicio de sesión dura exactamente 8 horas y solo lleva el emisor, el usuario y las fechas |
| C5. Después de 5 intentos fallidos seguidos, la cuenta se bloquea 15 minutos. | [`InicioSesionIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/InicioSesionIT.java) · el bloqueo dura 15 minutos desde el quinto fallo y los intentos durante el bloqueo no lo alargan |

Otras pruebas de RNF-04, que no cubren un criterio:

- [`ConfiguracionJwtTest`](../backend/src/test/java/co/edu/usco/inspectia/comun/seguridad/ConfiguracionJwtTest.java) · sin INSPECTIA_JWT_CLAVE, o si no está en Base64 o tiene menos de 32 bytes, el servidor no arranca (parametrizada)
- [`ConfiguracionJwtTest`](../backend/src/test/java/co/edu/usco/inspectia/comun/seguridad/ConfiguracionJwtTest.java) · una clave de 32 bytes en Base64 sirve para firmar con HS256 (prueba de límite)
- [`ValidadorDeVigenciaTest`](../backend/src/test/java/co/edu/usco/inspectia/comun/seguridad/ValidadorDeVigenciaTest.java) · un token sin fecha de vencimiento no sirve, y no se informa como vencido
- [`SesionAbiertaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionAbiertaIT.java) · el rol sale de la base en cada petición: si cambia, la sesión abierta lo refleja sin otro token
- [`SesionAbiertaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionAbiertaIT.java) · el token de un usuario que no existe no sirve
- [`SesionAbiertaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionAbiertaIT.java) · un encabezado Authorization mal formado no sirve (parametrizada)
- [`SesionAbiertaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionAbiertaIT.java) · un token alterado no sirve: cambiar el usuario sin volver a firmar se rechaza
- [`SesionAbiertaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionAbiertaIT.java) · un token de otro emisor no sirve, aunque esté firmado con la clave correcta
- [`SesionAbiertaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionAbiertaIT.java) · un token firmado con otra clave no sirve
- [`SesionAbiertaIT`](../backend/src/test/java/co/edu/usco/inspectia/usuario/sesion/SesionAbiertaIT.java) · un token sin firma (alg none) no sirve
- [`test_java.py`](../generador/pruebas/test_java.py) · las reglas de acceso por ruta y @PreAuthorize salen de operaciones del JSON

## RNF-05 · Funcionar en PC y en celular desde el navegador

| Criterio | Pruebas |
|---|---|
| C1. Todas las pantallas se usan sin desplazamiento horizontal desde 360 px de ancho. | **Pendiente** |
| C2. En el celular, la captura abre la cámara, tanto en Android con Chrome como en iPhone con Safari. | **Pendiente** |
| C3. Las pruebas del front-end (RP-14) se ejecutan en tamaño de PC y de celular. | **Pendiente** |

## RNF-06 · Ser sencilla de usar para el usuario final

| Criterio | Pruebas |
|---|---|
| C1. En una prueba con 3 personas que no conocen la app, cada una completa un análisis en menos de 5 minutos, sin ayuda. | **Pendiente** |
| C2. Registrar un lote, subir las fotos y ver el resultado toma como máximo 3 pantallas. | **Pendiente** |
| C3. Cada mensaje de error dice qué pasó y qué hacer. | **Pendiente** |
| C4. Todas las funciones se pueden usar con el teclado y los textos tienen buen contraste. | **Pendiente** |

## RNF-07 · Alcanzar el F1-score mínimo del modelo de IA

| Criterio | Pruebas |
|---|---|
| C1. El F1-score macro del modelo elegido en validación es igual o mayor al mínimo de P-02. | **Pendiente** |
| C2. Se reportan también la precisión, el recall y la matriz de confusión de cada clase. | **Pendiente** |
| C3. Los 5 modelos se miden con el mismo conjunto de validación (D-18), sin imágenes repetidas del entrenamiento. | **Pendiente** |
| C4. Los resultados de los 5 modelos quedan en la tabla comparativa (D-18). | **Pendiente** |

## RNF-08 · Citar a los autores del dataset y no usarlo con fines comerciales

| Criterio | Pruebas |
|---|---|
| C1. La app tiene una sección «Acerca de» con la cita del dataset, sus autores y su licencia. | **Pendiente** |
| C2. El repositorio y la documentación citan el dataset y el artículo de los autores (Zhinin-Vera et al., 2023). | **Pendiente** |
| C3. Mientras use modelos entrenados con este dataset, InspectIA no se cobra. | **Pendiente** |

## Pautas del docente y decisiones

Pruebas que no cubren un criterio de una ficha, sino una pauta del docente (RP) o una decisión (D) de `specs/decisiones.md`.

### RP-01 · API REST con el CRUD de las entidades del sistema, en Java con Spring Boot, JPA y Lombok

- [`VerificacionDeClasesTest`](../backend/src/test/java/co/edu/usco/inspectia/VerificacionDeClasesTest.java) · la JVM acepta todas las clases compiladas del backend

### RP-04 · Documentación con JavaDoc, comentarios y OpenAPI

- [`DocumentacionApiIT`](../backend/src/test/java/co/edu/usco/inspectia/comun/api/DocumentacionApiIT.java) · la documentación OpenAPI pide el token Bearer en toda la API, salvo para iniciar sesión

### RP-10 · Header en cada artefacto con nombre, descripción, fecha de creación, fecha de actualización, autor y versión

- [`test_salida.py`](../generador/pruebas/test_salida.py) · cada archivo generado lleva su header y la marca de no editar a mano

### RP-11 · Internacionalización

- [`RegistroEmpresaIT`](../backend/src/test/java/co/edu/usco/inspectia/empresa/RegistroEmpresaIT.java) · con Accept-Language: en, el error del dígito de verificación sale en inglés
- [`test_java.py`](../generador/pruebas/test_java.py) · los textos de todas las entidades salen en español y en inglés

### RP-13 · Listado completo de casos de prueba, derivados de los criterios de aceptación de cada requerimiento, y verificación de su cumplimiento

- [`test_casos.py`](../generador/pruebas/test_casos.py) · docs/casos-de-prueba.md está al día con las pruebas y las fichas
- [`test_casos.py`](../generador/pruebas/test_casos.py) · el documento sale igual cada vez que se arma
- [`test_casos.py`](../generador/pruebas/test_casos.py) · se leen las marcas de Java aunque las anotaciones lleven paréntesis, comillas o @
- [`test_casos.py`](../generador/pruebas/test_casos.py) · se leen las marcas de las pruebas de Python
- [`test_casos.py`](../generador/pruebas/test_casos.py) · todas las pruebas del repositorio tienen una marca válida
- [`test_casos.py`](../generador/pruebas/test_casos.py) · un criterio sin prueba sale como pendiente; uno eliminado no
- [`test_casos.py`](../generador/pruebas/test_casos.py) · una marca que cita algo que no existe o un criterio eliminado es un error

### RP-15 · Scripts de creación e inicialización de la base de datos

- [`EsquemaIT`](../backend/src/test/java/co/edu/usco/inspectia/EsquemaIT.java) · Flyway crea las 8 tablas del modelo de datos
- [`EsquemaIT`](../backend/src/test/java/co/edu/usco/inspectia/EsquemaIT.java) · el trigger mantiene fecha_actualizacion aunque un script intente cambiarla
- [`test_sql.py`](../generador/pruebas/test_sql.py) · cada referencia tiene índice, salvo si otro índice ya empieza por ella
- [`test_sql.py`](../generador/pruebas/test_sql.py) · cada tabla tiene id UUID v7, fechas y el trigger de fecha_actualizacion
- [`test_sql.py`](../generador/pruebas/test_sql.py) · el esquema crea las 8 tablas en orden de dependencias
- [`test_sql.py`](../generador/pruebas/test_sql.py) · las listas, patrones y CHECK de los JSON salen como restricciones
- [`test_sql.py`](../generador/pruebas/test_sql.py) · las tablas de cada empresa se referencian con FK compuestas (registro, empresa)
- [`test_sql.py`](../generador/pruebas/test_sql.py) · los únicos normalizados y parciales salen como índices únicos
- [`test_sql.py`](../generador/pruebas/test_sql.py) · ningún nombre de restricción, índice o trigger pasa de 63 caracteres

### RP-23 · Generación automatizada

- [`test_java.py`](../generador/pruebas/test_java.py) · cada CRUD tiene persistencia y solo las entidades del alcance tienen API
- [`test_java.py`](../generador/pruebas/test_java.py) · el código generado escribe sus constructores, sin las anotaciones de Lombok que los crean
- [`test_java.py`](../generador/pruebas/test_java.py) · la API de una entidad por empresa no se genera hasta que exista la sesión con empresa (RF-01)
- [`test_java.py`](../generador/pruebas/test_java.py) · las validaciones del JSON pasan a Bean Validation con la clave de su mensaje
- [`test_java.py`](../generador/pruebas/test_java.py) · los rangos por defecto de una empresa nueva salen de valores_iniciales del JSON
- [`test_otra_tematica.py`](../generador/pruebas/test_otra_tematica.py) · el JSON de producto de la pauta del docente pasa la validación
- [`test_otra_tematica.py`](../generador/pruebas/test_otra_tematica.py) · el generador produce la API de producto, otra temática, con las mismas plantillas
- [`test_otra_tematica.py`](../generador/pruebas/test_otra_tematica.py) · el generador produce los archivos de otra temática: producto de la pauta del docente
- [`test_salida.py`](../generador/pruebas/test_salida.py) · comprobar detecta archivos que faltan o que sobran en las carpetas generadas
- [`test_salida.py`](../generador/pruebas/test_salida.py) · comprobar detecta un archivo generado editado a mano
- [`test_salida.py`](../generador/pruebas/test_salida.py) · comprobar ignora la diferencia entre LF y CRLF
- [`test_salida.py`](../generador/pruebas/test_salida.py) · comprobar no encuentra diferencias justo después de generar
- [`test_salida.py`](../generador/pruebas/test_salida.py) · generar borra de las carpetas generadas lo que ya no se genera
- [`test_salida.py`](../generador/pruebas/test_salida.py) · la salida es determinista y usa finales de línea LF
- [`test_validacion.py`](../generador/pruebas/test_validacion.py) · la validación detecta un CHECK con un valor que no está en ninguna lista
- [`test_validacion.py`](../generador/pruebas/test_validacion.py) · la validación detecta una ficha que no existe en specs/
- [`test_validacion.py`](../generador/pruebas/test_validacion.py) · la validación detecta una referencia a una entidad que no existe
- [`test_validacion.py`](../generador/pruebas/test_validacion.py) · la validación pide el nombre de crear_con, que es el campo de la solicitud
- [`test_validacion.py`](../generador/pruebas/test_validacion.py) · la validación rechaza un mensaje con un marcador que no está en validaciones
- [`test_validacion.py`](../generador/pruebas/test_validacion.py) · la validación revisa que el ejemplo de un atributo cumpla sus validaciones
- [`test_validacion.py`](../generador/pruebas/test_validacion.py) · los JSON de specs/entidades pasan la validación
- [`test_validacion.py`](../generador/pruebas/test_validacion.py) · un atributo no puede usar el nombre de una columna implícita
- [`test_validacion.py`](../generador/pruebas/test_validacion.py) · una entidad con CRUD necesita modulo y ruta

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
