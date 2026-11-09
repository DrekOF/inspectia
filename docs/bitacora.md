<!--
Nombre: bitacora.md
Descripción: Registro paso a paso de cómo se construyó InspectIA, para la sustentación (RP-24).
Fecha de creación: 12/10/2026
Actualización: 09/11/2026
Autor: Equipo InspectIA
Versión: 1.21
-->

# Bitácora de construcción (RP-24)

Cada entrada dice qué se hizo, por qué y cómo se pidió. Las más recientes van al final.

## 11/10/2026 · Fase 1: levantamiento de requerimientos

- Qué se hizo: idea de negocio, actores y primeras decisiones (D-01 a D-10) en el documento de requerimientos, con la plantilla del curso. Prueba de separación de granos con OpenCV: 99,8 % contra las anotaciones del dataset. Recortes para entrenar (`recortes_script/`).
- Por qué: definir qué se construye antes de construirlo, y comprobar que una empresa puede fotografiar una muestra en lugar de grano por grano.
- Cómo se pidió: sesiones de trabajo con Claude, respondiendo un cuestionario inicial y resolviendo los pendientes uno por uno.
- Archivos: `01_Requerimientos/InspectIA_Requerimientos.docx`.

## 12/10/2026 · Fases 1 y 2: arquitectura, modelos y fichas

- Qué se hizo: despliegue en AWS de bajo costo (D-16), estado del arte (D-17), uso de los 5 modelos (D-18), arquitectura del software (D-21), prioridades (D-22), tamaño de la muestra (D-23) y las fichas de todos los RF y RNF (documento v2.0).
- Por qué: cerrar la fase 1 y dejar la especificación lista para construir con Claude Code.
- Cómo se pidió: sesiones de trabajo con Claude; un revisor automático buscó inconsistencias en las fichas y se corrigieron.
- Archivos: `02_Analisis_Diseno/InspectIA_Despliegue_AWS.pdf` (v1.2), `specs/`.

## 12/10/2026 · Esqueleto del monorepo

- Qué se hizo: carpetas `frontend/`, `backend/`, `ia-servicio/`, `generador/`, `infra/` y `specs/entidades/`, cada una con un README que dice qué va ahí. `infra/docker-compose.yml` con PostgreSQL 18 para desarrollo local; la contraseña va en `infra/.env`, que no se sube. README principal con las versiones, los requisitos y cómo instalarlos, y la cita del dataset (RNF-08). `.gitattributes` para que los scripts de Linux conserven LF. Repositorio git en la rama `main` con el primer commit. Sección Comandos de `CLAUDE.md`.
- Versiones: Claude las propuso y el equipo las confirmó con ajustes: Java 25 LTS, Spring Boot 4.1.1, Node 24 LTS, React 19, Python 3.13 y PostgreSQL 18. Se cambió Spring Boot 4.0 por 4.1 porque el soporte gratuito de la 4.0 termina el 31/12/2026. Antes se verificó en Maven Central que la 4.1.1 fija Lombok 1.18.46 (soporta el JDK 25) y Testcontainers 2.0.5, y que springdoc 3.1.x está construido sobre la 4.1. Se compila con el JDK 25 instalado en cada equipo, no solo dentro de Docker. Las imágenes base deben funcionar en amd64 y arm64, porque la EC2 t4g es ARM (D-16); las elegidas se verificaron en Docker Hub.
- Por qué: dejar la base del monorepo (D-21) y un entorno igual para todo el equipo antes de implementar los requerimientos, cada uno en su rama.
- Cómo se pidió: «Lee CLAUDE.md y los archivos de specs/. Todavía no escribas código de negocio. Primera tarea: armar el esqueleto del monorepo», en cinco pasos: proponer las versiones y esperar confirmación; crear las carpetas con su README; crear el docker-compose con PostgreSQL; inicializar git en `main` con el primer commit; y registrar lo hecho aquí y en la sección Comandos de `CLAUDE.md`. Al confirmar las versiones, el equipo pidió Spring Boot 4.1 tras revisar la compatibilidad, el JDK 25 local, imágenes amd64/arm64 y los requisitos de instalación en el README.
- Pendiente: decidir Material UI 7.x o 9.x antes de crear `frontend/`; agregar al README el enlace de Kaggle del dataset.
- Archivos o PR: `README.md`, los README de cada carpeta, `infra/docker-compose.yml`, `infra/.env.example`, `.gitattributes` y `CLAUDE.md`. Primer commit en `main`.

## 13/10/2026 · Ajustes al esqueleto: Material UI, dataset y puerto de la base

- Quién: Cristhian Sánchez (`DrekOF`), con Claude Code.
- Qué se hizo: se cerraron los pendientes del esqueleto. Material UI queda en la 9.x (9.5.0, la última estable en npm). El README cita el dataset con su enlace de Kaggle, su autor y su licencia (RNF-08). La sección «Base de datos local» del README explica que el puerto sale de `POSTGRES_PORT` (5432 por defecto) y qué hacer si Windows tiene reservado el puerto. `CLAUDE.md` pasa a la versión 1.1, con tres reglas nuevas: el backend lee el puerto de la configuración y nunca usa el 5432 fijo; al modificar un archivo se actualiza su header (versión y fecha); y cada entrada de esta bitácora dice quién trabajó, buscando `git config user.name` en la línea del equipo, que ahora incluye el usuario de GitHub de cada integrante. La plantilla de la bitácora tiene el campo «Quién:».
- Por qué: el repositorio ya está en GitHub (`DrekOF/inspectia`, privado). En el computador de Cristhian, Windows tiene reservado el 5432 y la base corre en el 5433, así que el puerto puede cambiar en cada equipo. El campo «Quién:» deja ver el aporte de cada integrante (RP-21 y RP-24).
- Cómo se pidió: con las novedades (repositorio en GitHub y base en el puerto 5433) se pidió anotar en `CLAUDE.md` la regla del puerto, y luego «Tarea 1. Pendientes del esqueleto (commit directo en main, porque son ajustes de documentación)»: Material UI 9.x, el enlace del dataset, la nota del puerto en el README y esta entrada. Claude mostró el diff y Cristhian dio el visto bueno con ajustes: subir la versión del header de `CLAUDE.md` y agregar la regla de actualizar los headers, poner los usuarios de GitHub en la línea del equipo y agregar el campo «Quién:».
- Archivos o PR: `README.md`, `CLAUDE.md`, `infra/.env.example` y esta bitácora. Commit directo en `main`.

## 13/10/2026 · Modelo de datos

- Quién: Cristhian Sánchez (`DrekOF`), con Claude Code.
- Qué se hizo: el modelo de datos completo, con 8 tablas: empresa, usuario, rango_recomendacion, lote, muestra, foto, grano y modelo.
  - Decisiones nuevas D-24 a D-27: margen de 1 punto para sugerir otra muestra; hasta 2 muestras por lote, con el resultado calculado sobre la suma de sus granos; se aceptan muestras de más de 150 granos; las empresas se desactivan, se reactivan y se editan.
  - 12 fichas actualizadas, con criterios nuevos: la prueba de límite del margen (90,9 % y 91,0 %), la suma de 2 muestras de distinto tamaño, la contraseña de 8 a 64 caracteres y como máximo 72 bytes, y el dígito de verificación del NIT.
  - `specs/arquitectura.md`: la segunda muestra en el flujo del análisis, y la comparación de los 5 modelos también en la tabla `modelo`.
  - `specs/modelo-datos.md`: diagrama entidad-relación en Mermaid, una tabla por entidad con su trazabilidad y las decisiones de diseño.
  - Un JSON por entidad en `specs/entidades/`, con el formato que usará el generador documentado en su README. Solo empresa, usuario y rango_recomendacion llevan `"crud": true`.
  - Los JSON se revisaron con un script que comprueba tipos, referencias, columnas, roles y que cada ficha citada exista.
- Por qué: definir los datos antes de escribir código, con cada atributo trazado a una ficha. Los JSON son la entrada del generador (RP-23) y de los scripts de base de datos (RP-15).
- Cómo se pidió:
  1. Claude leyó las fichas, las decisiones y la arquitectura y propuso el modelo, con 8 preguntas sobre lo que las fichas no decían. Cristhian aprobó las recomendaciones y tomó las decisiones D-24 a D-27.
  2. Al revisar la propuesta, Cristhian pidió estos ajustes:
     - el resultado pasa del análisis al lote;
     - fecha de envío en cada muestra, para medir RNF-01 y RNF-02 con datos reales;
     - tipo y tiempo por grano en la tabla `modelo`;
     - límite de 72 bytes para la contraseña;
     - editar y reactivar empresas;
     - la segunda muestra en la arquitectura;
     - renombrar la tabla `analisis` a `muestra`.
  3. Claude agregó la fecha de inicio y la de fin del proceso: sin ellas no se puede separar la espera en cola del procesamiento.
  4. Al verificar el ejemplo de NIT, Claude encontró que 900123456-7 tenía mal el dígito de verificación y lo corrigió a 900123456-8.
- Archivos o PR: rama `feature/modelo-datos`, con dos commits: decisiones y fichas; luego modelo de datos, entidades, `CLAUDE.md` y esta bitácora. El PR lo abre Cristhian.

## 13/10/2026 · Modelo de datos: reglas de roles

- Quién: Cristhian Sánchez (`DrekOF`), con Claude Code.
- Qué se hizo:
  - Regla nueva: el administrador no puede cambiar su propio rol. Está en `usuario.json`, en RF-03 (restricción y criterio; la ficha pasa a 1.2) y en `modelo-datos.md`.
  - El README de entidades aclara que `valores_en_formulario` también lo valida el backend. Un rol no permitido enviado a mano a la API se rechaza con «prohibido» (403, RNF-04) y un mensaje claro, que quedó en `usuario.json`, sin depender del CHECK de la base.
- Por qué: si el único administrador de una empresa se cambiaba a usuario final, la empresa quedaba sin administrador, y nadie podía arreglarlo porque el administrador de la plataforma no edita usuarios. Además, el formulario solo limita lo que se ofrece; la seguridad debe estar en el backend.
- Cómo se pidió: Cristhian revisó el modelo, los JSON, el README, las fichas y la bitácora y pidió estos dos detalles de roles en la misma rama.
- Archivos o PR: `usuario.json`, `fichas.json`, `modelo-datos.md`, el README de entidades y esta bitácora, en la rama `feature/modelo-datos`.

## 13/10/2026 · Modelo de datos: respuestas de la API a un rol no permitido

- Quién: Cristhian Sánchez (`DrekOF`), con Claude Code.
- Qué se hizo:
  - El README de entidades distingue dos casos. Un código que existe pero no se puede asignar responde «prohibido» (403); un código que no existe, como «JEFE», responde «dato inválido» (400).
  - RF-03 (v1.3) tiene un criterio nuevo: si un administrador de empresa envía a la API un usuario con rol ADMIN_PLATAFORMA, la API responde 403 y no guarda nada.
  - `modelo-datos.md` refleja los dos casos.
- Por qué: un rol que no se puede asignar es un problema de permisos (RNF-04), mientras que un código que no existe es un dato inválido. El intento de asignar ADMIN_PLATAFORMA es una prueba de seguridad (RP-12) y necesita su caso de prueba (RP-13).
- Cómo se pidió: Cristhian aprobó el 403 y pidió el 400 para códigos que no existen, y el criterio de seguridad en RF-03.
- Archivos o PR: `fichas.json`, el README de entidades, `modelo-datos.md` y esta bitácora, en la rama `feature/modelo-datos`.

## 13/10/2026 · Historia limpia: la autoría es de los integrantes

- Quién: Cristhian Sánchez (`DrekOF`), con Claude Code.
- Qué se hizo:
  1. **Respaldo:** un respaldo completo del repositorio, en `../inspectia-respaldo.bundle`, con todas las ramas y el merge del PR #1.
  2. **Reescritura de la historia de `main`** con `git filter-repo`:
     - Se quitaron las líneas «Co-Authored-By: Claude» de los 6 commits que las tenían.
     - El merge del PR #1 pasó a llamarse «Merge de la rama feature/modelo-datos: modelo de datos y decisiones D-24 a D-27».
     - En todas las versiones de los archivos, el autor de los headers quedó como «Equipo InspectIA» (o «Equipo InspectIA (exportado del documento de requerimientos v2.0)»), sin la nota sobre Claude.
     - Se borró la rama `feature/modelo-datos`, que ya estaba en `main`.
  3. **Verificación:**
     - Ningún mensaje tiene «Co-Authored-By».
     - La nota sobre Claude no aparece en ninguna versión de ningún archivo.
     - `main` conserva sus 7 commits, con los mismos títulos (salvo el del merge) y los mismos 26 archivos.
     - Entre antes y después solo cambian las 23 líneas de autor.
     - Esta bitácora no se tocó.
  4. **Para el futuro:**
     - `.claude/settings.json` desactiva la atribución de Claude en commits y PR para todo el equipo.
     - `.claude/settings.local.json` queda en `.gitignore`.
     - `CLAUDE.md` (v1.3) prohíbe agregar a Claude como coautor.
- Por qué: en GitHub solo deben aparecer los integrantes como autores. El PR #1 no existirá en el repositorio nuevo, así que su merge no puede nombrarlo. La bitácora sigue registrando cómo se trabajó con Claude Code (RP-24).
- Cómo se pidió: Cristhian pidió rehacer el repositorio con la historia limpia, en seis pasos: respaldo, traer el merge del PR #1, reescribir la historia, verificar, configurar para el futuro y esperar antes de subir. Él borra el repositorio en GitHub y lo crea vacío; después se sube `main`.
- Archivos o PR: historia reescrita de `main`, y un commit con `.claude/settings.json`, `.gitignore`, `CLAUDE.md` y esta bitácora. La subida a GitHub queda pendiente.

## 13/10/2026 · RF-02 y generador: backend de empresas, API generada y casos de prueba

- Quién: Cristhian Sánchez (`DrekOF`), con Claude Code.
- Qué se hizo:
  - **Antes de la rama, en `main`:** `decisiones.md` (v1.3) apunta al documento de requerimientos v2.1.
  - **Luego, en la rama `feature/RF-02-generador-empresa`, seis pasos, cada uno revisado por Cristhian antes del siguiente:**
    1. **Esqueleto del backend:** Spring Boot 4.1.1 con Java 25, los 6 módulos de D-21 y una prueba de arranque con PostgreSQL 18 en Testcontainers.
    2. **Generador con validación y SQL:**
       - `generador/` valida los JSON contra su formato y contra `specs/`.
       - Genera `V1__esquema_inicial.sql` con las 8 tablas, que Flyway aplica al arrancar.
       - `comprobar` detecta lo generado que alguien editó a mano.
       - Claves nuevas `modulo` y `ruta`, y el pendiente P-16 para las migraciones después del primer despliegue.
       - `producto.json`, la entidad de la pauta del docente, demuestra que el generador sirve para otra temática.
    3. **Primer administrador de la plataforma:**
       - `infra/bd/crear-admin-plataforma.sql` (psql y pgcrypto), con envoltorios `.sh` y `.ps1`.
       - Toma los datos de `infra/.env` o del entorno.
       - La contraseña no pasa por la línea de comandos ni queda en el registro de PostgreSQL.
    4. **Pruebas de RF-02 antes del código:** 73 pruebas, todas en rojo hasta el paso 5. Cubren los criterios C1 a C11 de RF-02, C1 a C3 de RNF-04 y el formato del listado.
    5. **Código:**
       - `comun`, escrito a mano: errores en `application/problem+json`, roles por ruta, BCrypt e i18n.
       - El generador produce la persistencia de los 3 CRUD y la API de empresa, con su prueba generada.
       - `ReglasEmpresaImpl` revisa el dígito de verificación del NIT.
       - Claves nuevas `crear_con.nombre` y `ejemplo`, y `generador/alcance.json` para decidir qué entidades tienen API.
    6. **Documentación:**
       - `generador/casos_de_prueba.py` arma `docs/casos-de-prueba.md` desde las marcas de las pruebas.
       - RF-02 (v1.2) tiene el criterio C12, restablecer la contraseña de un administrador, pendiente hasta la API de usuario (RF-03).
       - README, Comandos de `CLAUDE.md` y esta bitácora.
  - **Reglas nuevas en `CLAUDE.md`:**
    - Cada prueba lleva su marca: «<código> · C<n> · …» si cubre un criterio y «<código> · …» si no.
    - Los criterios nuevos van al final de su lista, y los que dejan de aplicar se marcan «Eliminado (D-xx)» sin borrarlos. Las pruebas citan el número del criterio, y ya pasó con RF-04: el criterio de la fecha cambió de C3 a C4.
  - **Resultado:**
    - Backend: 148 pruebas (31 unitarias y 117 de integración, 47 de ellas generadas), todas en verde.
    - Generador: 39 pruebas, todas en verde.
    - Criterios: de los 134 de las fichas, 16 tienen prueba; los demás llegan con sus fichas.
  - **Queda para otras fichas:** el inicio de sesión y la decisión sobre CSRF (RF-01), la API de usuario y el criterio C12 de RF-02 (RF-03), y la de rangos (RF-13).
- Por qué: RF-02 es la base de las demás funciones de la empresa, y el primer CRUD generado (RP-23) fija cómo serán todos: formato de la API, errores, permisos y pruebas. La lista de casos de prueba se arma desde el código para que no se desactualice (RP-13).
- Cómo se pidió: Cristhian pidió primero el plan del generador (RP-23) y del backend de empresas (RF-02). Lo aprobó con tres agregados: dónde se generan los UUID v7, el ejemplo producto y la marca de cada prueba. En el camino pidió:
  - El formato del listado: una página con `contenido`, `pagina`, `tamano`, `totalElementos` y `totalPaginas`, de 20 filas y ordenada por nombre.
  - Que el permiso se revise antes que los datos.
  - El criterio C12 de RF-02.
  - Que la lista de casos de prueba la arme un script.
- Archivos o PR: la rama `feature/RF-02-generador-empresa`; el PR lo abre Cristhian.

## 14/10/2026 · RF-01: inicio de sesión con JWT

- Quién: Cristhian Sánchez (`DrekOF`), con Claude Code.
- Qué se hizo:
  - **Antes del código:** D-28 en `decisiones.md`, con las decisiones del inicio de sesión, y P-17, el límite de intentos por IP en el despliegue.
  - **En la rama `feature/RF-01-inicio-sesion`, cuatro pasos, cada uno revisado por Cristhian antes del siguiente:**
    1. **Pruebas antes del código:**
       - 30 pruebas de integración (`InicioSesionIT` y `SesionAbiertaIT`) para los 6 criterios de RF-01, los que faltaban de RNF-04 (C4 y C5) y los tokens que no deben servir: vencido, alterado, firmado con otra clave, sin firma, de otro emisor o de un usuario que no existe.
       - 29 quedaron en rojo; la de «sin token, 401» ya se cumplía desde RF-02.
       - Los tokens de prueba se arman con HMAC de Java, sin usar el código del backend, y cada corrida usa una clave nueva.
    2. **Inicio de sesión:**
       - `POST /api/v1/sesion` revisa el correo y la contraseña y entrega el token con el usuario, su rol y su empresa. `GET /api/v1/sesion` responde la sesión actual.
       - El token se firma con HS256 y la clave de `INSPECTIA_JWT_CLAVE`. Solo lleva el emisor, el usuario y sus fechas, y vence a las 8 horas exactas. La API ya no usa CSRF.
       - Todo intento fallido recibe la misma respuesta y hace una sola comparación BCrypt, exista o no el correo. Los fallos se cuentan con un `UPDATE` atómico, y 5 seguidos bloquean la cuenta 15 minutos.
       - RF-01 (v1.2) tiene dos criterios nuevos. C7: al terminar el bloqueo vuelven los 5 intentos. C8: una contraseña de más de 72 bytes nunca inicia sesión, porque BCrypt solo compara los primeros 72.
    3. **Revisión en cada petición:**
       - Desactivar un usuario o su empresa corta sus sesiones en la siguiente petición.
       - Cada 401 dice su causa.
       - El inicio de sesión no lee el encabezado `Authorization`: quien tiene la sesión vencida puede volver a entrar aunque el frontend todavía envíe el token viejo.
    4. **Documentación y prueba completa:**
       - El esquema Bearer en OpenAPI (botón «Authorize» de Swagger UI), `infra/.env.example`, los README y los Comandos de `CLAUDE.md`.
       - El recorrido se probó con el backend real en una base aparte: crear la clave, arrancar, crear el administrador con el script y entrar desde Swagger UI. Después, Cristhian lo repitió en su PC, con su base.
  - **Problemas que encontraron las pruebas:**
    - **Constructores de Lombok con el JDK 25.**
      - Al correr las pruebas del paso 1, todo el backend falló con `VerifyError`. Al recompilar sin `clean`, los constructores que crea `@RequiredArgsConstructor` quedaban sin la llamada a `super()`, y la JVM rechazaba `EmpresaServicio`, `EmpresaControlador` y `RespuestasDeSeguridad`.
      - Compilando desde cero no pasaba; por eso no apareció en RF-02.
      - Se resolvió escribiendo los constructores, en las plantillas del generador y a mano. `VerificacionDeClasesTest` carga todas las clases para detectarlo, el generador tiene su prueba y `CLAUDE.md` tiene la regla.
      - Lombok queda para getters, setters y `@Slf4j`: su registro de cambios hasta la 1.18.48 no trae arreglo. El generador pasó a la versión 1.2.
    - **El `UPDATE` de los intentos fallidos.**
      - En el paso 2, las pruebas de integración recibieron 500 en cada intento fallido de un usuario que existe.
      - Dentro del `case ... then :hasta else null end`, PostgreSQL no podía deducir el tipo del parámetro y lo tomaba como texto.
      - Se resolvió con `cast(:hasta as OffsetDateTime)` en el JPQL, con la explicación en el código.
  - **Otras anotaciones:**
    - P-16: después del primer despliegue, las migraciones aplicadas quedan congeladas, incluido su header.
    - P-17: queda como riesgo aceptado la diferencia de tiempo, del orden de 1 ms, al guardar un intento fallido.
    - D-29: criterio de los estados de las fichas.
      - «Aprobado»: el equipo revisó la ficha. Pasan a Aprobado todos los RF menos RF-08, que espera la revisión de Julián (P-14).
      - «Implementado»: funciona de punta a punta, todos sus criterios tienen prueba y está en `main`. Ninguno lo está todavía: a RF-01 le falta el menú en el frontend, y a RF-02, el criterio C12 y sus pantallas.
      - `casos_de_prueba.py comprobar` falla si un RF está «Implementado» y tiene criterios sin prueba.
  - **Resultado:**
    - Backend: 201 pruebas (50 unitarias y 151 de integración), todas en verde.
    - Generador: 43 pruebas, todas en verde.
    - Criterios: RF-01 tiene sus 8 criterios con prueba y RNF-04 sus 5. De los 136 de las fichas, 26 tienen prueba.
- Por qué: todas las demás funciones dependen del inicio de sesión (RF-01), y RNF-04 pide sesión y permisos por rol en toda la API. La revisión en cada petición hace que desactivar un usuario o una empresa (RF-03, D-27) surta efecto de inmediato y no 8 horas después.
- Cómo se pidió: Cristhian pidió el plan del inicio de sesión con JWT (D-21) antes de escribir código. El plan debía cubrir el token, la API, la revisión en cada petición, el bloqueo sin revelar si un correo existe, las pruebas y cómo probarlo en su PC. Aprobó las cinco decisiones y pidió registrarlas como D-28 y P-17. En el camino pidió:
  - Dejar la regla de Lombok y subir la versión del generador.
  - Agregar C7 y C8 a RF-01.
  - Que el inicio de sesión ignore el token viejo, con su prueba marcada como RF-01 C6.
  - Anotar en P-17 el riesgo aceptado.
  - Registrar D-29 con el criterio de los estados de las fichas, y que `comprobar` falle si un RF «Implementado» tiene criterios sin prueba.
- Archivos o PR: la rama `feature/RF-01-inicio-sesion`; el PR lo abre Cristhian.

## 18/10/2026 · RF-03 y RF-02 C12: gestión de usuarios y restablecimiento de contraseñas

- Quién: Cristhian Sánchez (`DrekOF`), con Claude Code.
- Qué se hizo:
  - **Antes del plan:**
    - D-30 en `decisiones.md`: quién cambia o restablece cada contraseña. La de un usuario final la restablece su administrador (RF-03); la de un administrador de empresa, el administrador de la plataforma (RF-02 C12) u otro administrador de su empresa. Cambiar o restablecer una contraseña corta las sesiones abiertas de ese usuario.
    - La ficha RF-18, «Cambiar la propia contraseña», en «Aprobado». Se implementa en otra rama, junto con el script que restablece la contraseña del administrador de la plataforma.
    - Criterios nuevos, al final de su lista: RF-03 C11 (el administrador no cambia su propia contraseña desde la gestión de usuarios), RNF-04 C6 (cambiar o restablecer una contraseña corta las sesiones abiertas) y RF-18 C5.
  - **En la rama `feature/RF-03-usuarios`, cuatro pasos, cada uno revisado por Cristhian antes del siguiente:**
    1. **Pruebas antes del código:** `GestionUsuariosIT`, `UsuariosPorEmpresaIT` y `RestablecerContrasenaIT`, con tokens reales, para RF-03 C1 a C11, RF-02 C12, RNF-03 C2 y C5 y RNF-04 C6. Las 36 ejecuciones nuevas fallaban porque las rutas no existían, y también las 6 pruebas nuevas del generador.
    2. **Generador y API de usuarios:**
       - El generador (1.3) produce la API de una entidad por empresa: la empresa sale de la sesión, cada consulta se limita a ella y un registro de otra empresa responde 404.
       - Un rol que existe pero no se puede asignar (`valores_en_formulario`) responde 403, antes que los 400; uno que no existe responde 400 con su campo.
       - La contraseña va en el formulario: obligatoria al crear y opcional al editar, donde enviar una nueva es restablecerla. La respuesta no lleva el hash ni los datos internos.
       - `/api/v1/usuarios` sale generada, con su prueba. `ReglasUsuarioImpl` programa a mano el máximo de 25 activos y lo que el administrador no puede hacer consigo mismo.
       - Una copia de lo generado para empresa revisa que no cambie, salvo el header.
    3. **Ajustes de la revisión del paso 2:**
       - Para contar los 25 activos, la regla bloquea la fila de la empresa (`SELECT … FOR UPDATE`) dentro de la transacción. Una prueba manda dos altas al mismo tiempo.
       - Una contraseña de solo espacios se rechaza con «Este dato es obligatorio.» al crear, al editar, en RF-02 C12 y en el script del administrador de la plataforma. La regla quedó en las restricciones de RF-02, RF-03 y RF-18.
    4. **RF-02 C12 y corte de sesiones:**
       - `GET /api/v1/empresas/{empresaId}/administradores` y `PUT …/{usuarioId}/contrasena`, escritos a mano y solo para el administrador de la plataforma. No reactivan nada.
       - Una columna nueva, `usuario.sesiones_desde`, y una regla de tipo efecto, `cortar_sesiones_al_cambiar_contrasena`, que guarda la hora del cambio y desbloquea la cuenta. El generador (1.5) ya llama los efectos.
       - En cada petición, un token emitido antes de esa hora responde 401 «Tu contraseña cambió: inicia sesión de nuevo.».
       - Como cambió `V1__esquema_inicial.sql`, la base local se reinicia (P-16). Los pasos quedaron en `infra/README.md` y en el PR.
       - Documentación: `modelo-datos.md`, `arquitectura.md`, los README de entidades, del generador y de `infra/`, y esta bitácora.
  - **Problemas encontrados:**
    - **Marcas de las pruebas generadas.** Las de usuario citaban RF-01, la primera ficha de `usuario.json`. Ahora la primera ficha de una entidad es la de su CRUD (RF-03), y el README de entidades lo dice.
    - **Dos altas a la vez.** Cristhian notó al revisar el paso 2 que el límite de 25 no aguantaba dos altas simultáneas. La prueba nueva lo confirmó antes del arreglo: quedaban 26 activos.
    - **Contraseña de solo espacios.** Al editar se aceptaba, y el usuario quedaba sin poder entrar, porque el inicio de sesión la rechaza. Cristhian decidió rechazarla en todas partes.
  - **Otras anotaciones:**
    - Un token emitido en el mismo segundo del cambio de contraseña sigue sirviendo, porque el token guarda segundos enteros. Es una ventana de menos de 1 s, aceptada en el plan.
    - Un token sin fecha de emisión (`iat`) ya no sirve: sin ella no se sabe si es de antes del cambio.
  - **Resultado:**
    - Backend: 327 pruebas (74 unitarias y 253 de integración, 103 de ellas generadas), todas en verde.
    - Generador: 53 pruebas, todas en verde.
    - Criterios: RF-02 tiene sus 12 criterios con prueba, RF-03 sus 11 (la parte del historial de C6 se prueba con RF-14) y RNF-04 sus 6. De los 143 de las fichas, 41 tienen prueba.
    - El 19/10, Cristhian reinició su base con `down -v`, creó el administrador con el script y probó en Swagger UI.
    - Ninguna ficha pasa a «Implementado»: faltan las pantallas (D-29).
- Por qué: RF-03 deja que cada empresa gestione sus usuarios, con un máximo de 25 activos, y RF-02 C12 con D-30 asegura que toda contraseña tenga quién la restablezca. Cortar las sesiones al cambiar una contraseña impide que siga entrando quien conocía la anterior (RNF-04).
- Cómo se pidió: Cristhian pidió registrar D-30 y la ficha RF-18, y después el plan de RF-03 y RF-02 C12, sin código. Lo aprobó con cinco decisiones: el 403 antes que los 400, la regla `no_cambiar_su_propia_contrasena`, las rutas de RF-02 C12, cortar las sesiones con `sesiones_desde` y sacar los datos internos de la respuesta. En el camino pidió:
  - Ajustar D-30 y agregar RF-03 C11, RNF-04 C6 y RF-18 C5 antes de las pruebas.
  - Bloquear la fila de la empresa al contar el límite de 25, con una prueba de dos peticiones a la vez.
  - Rechazar en todas partes las contraseñas de solo espacios, con la regla en las restricciones de RF-02, RF-03 y RF-18.
  - Avisar en el PR y en `infra/README.md` que V1 cambió y cómo reiniciar la base.
  - Dejar que cambiar o restablecer una contraseña ponga en cero los intentos fallidos y quite el bloqueo de RF-01, aunque no estaba en los requisitos, para que el usuario entre de una vez con la nueva. Quedó anotado en D-30, en las observaciones de RF-01 y en las restricciones de RF-02 y RF-03, sin criterio nuevo.
- Archivos o PR: la rama `feature/RF-03-usuarios`; el PR lo abre Cristhian.

## 19/10/2026 · RF-18: cambiar la propia contraseña y restablecer la del administrador de la plataforma

- Quién: Cristhian Sánchez (`DrekOF`), con Claude Code.
- Qué se hizo:
  - **Antes del código:** la restricción 4 de RF-18 (v1.4) dice ahora, como D-30, que la contraseña de un administrador de empresa también la puede restablecer otro administrador de su empresa (RF-03).
  - **En la rama `feature/RF-18-cambiar-contrasena`, cuatro pasos, cada uno revisado por Cristhian antes del siguiente:**
    1. **Pruebas antes del código:**
       - `CambioDeContrasenaIT`, con tokens reales, para RF-18 C1 a C5 y los demás casos del plan.
       - `RestablecerAdminPlataformaIT`, para el script (D-30).
       - Fallaban 22 de 23 y 16 de 17. Ya pasaban la de sin sesión (401) y la de no reactivar.
       - Las sesiones «de antes» se emiten 5 segundos atrás, fuera de la ventana del mismo segundo.
    2. **Cambiar la propia contraseña:**
       - `PUT /api/v1/sesion/contrasena` lo usa cualquier rol con sesión. El usuario sale del token, así que cada uno solo cambia la suya.
       - Revisa primero los datos (400, sin contar intento), después el bloqueo (429) y después la contraseña actual, con una sola comparación BCrypt.
       - Si la actual no coincide, responde 400 con la regla del bloqueo y el intento cuenta para el de RF-01. Desde el quinto fallo seguido responde 429, con `bloqueadoHasta` y `Retry-After`.
       - Si cambia, aplica la regla `cortar_sesiones_al_cambiar_contrasena`. La sesión sigue con un token nuevo, emitido con la hora de `sesiones_desde` y que vence cuando vencía el anterior (C5).
       - No cambiaron los JSON ni lo generado: `V1` sigue igual y no hubo que reiniciar la base.
    3. **Script del administrador de la plataforma:**
       - `infra/bd/restablecer-admin-plataforma.sql`, con sus envoltorios `.sh` y `.ps1`. Valida la contraseña nueva, guarda su hash, corta las sesiones abiertas y quita el bloqueo (D-30). No toca a usuarios de una empresa ni reactiva a nadie.
       - Los envoltorios piden la contraseña nueva dos veces sin mostrarla. Si `INSPECTIA_ADMIN_CORREO` no está en el entorno ni en `infra/.env`, piden también el correo.
       - Se probaron en una base aparte, en otro proyecto de Docker Compose: el `.sh` con las respuestas por la entrada estándar y el `.ps1` con `Read-Host` simulado.
    4. **Documentación:**
       - La sección del script en `infra/README.md`, con la diferencia de relojes en local en los dos sentidos y cómo corregirla.
       - El cambio de contraseña en Swagger UI.
       - Los Comandos de `CLAUDE.md`, el README y esta bitácora.
  - **Problemas encontrados:**
    - **La variable vacía tomaba la contraseña de `infra/.env`.**
      - Al probar el `.ps1`, el script «restableció» la contraseña sin que nadie la escribiera.
      - La causa: PowerShell borra una variable de entorno si se le asigna un texto vacío. Al no encontrar `INSPECTIA_ADMIN_CONTRASENA` en el entorno, docker compose tomó la del script de creación, que seguía en `infra/.env`. También pasaría al pulsar Enter dos veces.
      - Se resolvió con una variable propia para la contraseña nueva, `INSPECTIA_ADMIN_CONTRASENA_NUEVA`, que nunca va en `infra/.env`. Una prueba nueva lo cubre.
    - **Tildes con «?» en los `.ps1`.**
      - Llamados con `.\` desde la consola o desde otro script, el `.sql` le llegaba a psql en ASCII: el `$OutputEncoding` que el script asignaba en su propio ámbito solo contaba con `powershell -File`.
      - Si PowerShell capturaba la salida (PowerShell ISE, una variable o `Tee-Object`), además la leía con la página de códigos de la consola.
      - Ahora los dos `.ps1`, el de creación y el de restablecimiento, usan UTF-8 en los dos sentidos mientras corre psql, y al terminar dejan la consola como estaba. Se comprobó con las dos versiones de cada script.
  - **Resultado:**
    - Backend: 374 pruebas (80 unitarias y 294 de integración, 103 de ellas generadas), todas en verde.
    - Generador: 53 pruebas, todas en verde.
    - Criterios: RF-18 tiene sus 5 criterios con prueba. De los 143 de las fichas, 46 tienen prueba.
    - El 19/10, Cristhian probó en su PC:
      - Cambió su contraseña por la API: el token anterior respondió 401 y el nuevo sirvió, con el mismo vencimiento.
      - Corrió el script de restablecimiento, escribiendo la contraseña dos veces, y las tildes salieron bien.
    - RF-18 sigue en «Aprobado»: falta su pantalla (D-29).
- Por qué: sin recuperación por correo (RF-01), cada usuario necesita poder cambiar su contraseña, y toda contraseña necesita quién la restablezca (D-30), incluida la del administrador de la plataforma. Que el intento fallido cuente para el bloqueo evita que esta función sirva para adivinar contraseñas.
- Cómo se pidió: Cristhian pidió primero el plan de RF-18 y del script, sin código. El plan debía responder seis preguntas: la ruta y sus respuestas, C5 con `sesiones_desde`, qué produce el generador, el script, las pruebas y si cambiaba V1. Lo aprobó con las seis decisiones propuestas y tres ajustes:
  - Corregir la restricción 4 de RF-18.
  - Que la otra sesión de la prueba de C5 se emita al menos un segundo antes del cambio.
  - Que el script pida también el correo si no está en el entorno ni en `infra/.env`.

  En el camino pidió:
  - Anotar en `infra/README.md` la diferencia de relojes en local en los dos sentidos, cómo corregirla y que en el servidor no pasa.
  - Corregir las tildes de los dos `.ps1` y dejar la consola como estaba.
  - Probar RF-18 y el script en su PC antes de la documentación.
- Archivos o PR: la rama `feature/RF-18-cambiar-contrasena`; el PR lo abre Cristhian.

## 20/10/2026 · RF-13: rangos de recomendación de la empresa, editados en bloque

- Quién: Cristhian Sánchez (`DrekOF`), con Claude Code.
- Qué se hizo:
  - **En la rama `feature/RF-13-rangos`, tres pasos, cada uno revisado por Cristhian antes del siguiente:**
    1. **Pruebas antes del código (19/10):**
       - La restricción 1 de RF-13 (v1.1) dice ahora que los límites van de 0 a 100 %, con un decimal como máximo, y que el de Fuera de norma es fijo en 0 %. No es un criterio nuevo.
       - `RangosRecomendacionIT`, con tokens reales y empresas creadas por la API de RF-02: C1, C3, C4 y C5.
       - Pruebas de límite: 0 y 100; un decimal contado sin los ceros del final (90,50 sí, 90,55 no); Fuera de norma fijo; la recomendación vacía, de solo espacios, de 300 y de 301 caracteres; filas de menos, de más o repetidas.
       - También RNF-03 (cada empresa ve y cambia solo sus rangos) y RNF-04 (sin sesión, 401; la plataforma, 403).
       - Pruebas del generador para la edición en bloque, `solo_lectura_si`, `@MaximoDecimales` y lo que no sabe generar.
       - Fallaban 32 de 33 con 404; ya pasaba la de sin sesión (401).
       - C2 (un análisis hecho después del cambio usa los rangos nuevos) queda para RF-12, cuando exista el análisis.
    2. **El generador 1.6 y la API de los rangos (20/10):**
       - `GET /api/v1/rangos-recomendacion` lista las 4 filas de la empresa de la sesión.
       - `PUT` las edita todas juntas.
       - `POST …/restaurar-por-defecto` vuelve a `RangoRecomendacionValoresIniciales`, la misma fuente que usa RF-02 al crear la empresa.
       - Al editar revisa, en orden: los datos de cada fila (error en `filas[i].campo`), que llegue una fila por categoría sin repetir, que no cambie el límite de Fuera de norma (`solo_lectura_si`) y la regla `limites_en_orden`, escrita a mano en `ReglasRangoRecomendacionImpl`. Si algo falla, responde 400 y no guarda nada.
       - `@MaximoDecimales` cuenta los decimales sin los ceros del final: `@Digits` de Bean Validation rechazaría 90,50.
       - `validar` rechaza `solo_lectura_si` en una entidad que no es en bloque, en vez de ignorarlo.
       - Los ejemplos booleanos de OpenAPI salen como `true`, no `True` de Python. Una prueba revisa que no quede `True`, `False` ni `None` en lo generado. De paso, la descripción de las respuestas ya no dice «Usuario como la responde la API».
       - `rango_recomendacion.json` 1.2 lleva RF-13 primero en `fichas`, para que lo citen las pruebas generadas, y el mensaje de Fuera de norma. `alcance.json` 1.2 agrega los rangos.
       - V1 cambió solo en su header (generador 1.6), pero igual hubo que reiniciar la base local.
    3. **Documentación (20/10):**
       - La edición en bloque en `specs/entidades/README.md` y `specs/modelo-datos.md`.
       - El README del generador.
       - En `infra/README.md`, el reinicio de la base también por un cambio de header y cómo probar los rangos en Swagger UI.
       - El README y esta bitácora.
  - **Problemas encontrados:**
    - **Dos cambios a la vez dejaban los límites fuera de orden.**
      - Sin un bloqueo, una edición leía las filas antes de que otra guardara y solo escribía las que ella cambiaba. Podía quedar, por ejemplo, Premium 91 con Estándar 95.
      - Ahora editar y restaurar bloquean las filas de la empresa (`SELECT … FOR UPDATE`), y la segunda espera a que la primera guarde.
      - Una prueba lo reproduce: sin el bloqueo falla y con él pasa.
    - **La respuesta de la edición repetía el número como llegaba.**
      - Con 90.50, el PUT respondía 90.50 y el listado, 90.5.
      - Ahora el mapeador generado copia cada decimal con la escala de su columna (`Decimales.conEscala`, sin redondear). Así el PUT responde 90.5, como lo guarda la base. Una prueba compara el JSON tal como llega en el PUT y en el GET.
  - **Comportamiento que no piden las fichas:** editar también repone una fila que falte, con su valor por defecto, igual que restaurar. Solo pasaría si alguien la borra en la base. Cristhian lo aprobó: la ficha ya dice que se mantienen las 4 categorías, así que no cambia.
  - **Resultado:**
    - Backend: 469 pruebas (112 unitarias y 357 de integración, 131 de ellas generadas), todas en verde sobre el código final.
    - Generador: 61 pruebas, todas en verde.
    - Criterios: RF-13 tiene 4 de sus 5 criterios con prueba; C2 espera a RF-12. De los 143 de las fichas, 50 tienen prueba.
    - El 20/10, Cristhian probó en su PC, con la base reiniciada:
      - Creó una empresa, listó y editó los rangos: 90.50 respondió 90.5.
      - Vio los errores de orden y de Fuera de norma.
      - Restauró los valores por defecto.
    - RF-13 sigue en «Aprobado»: faltan su pantalla y C2 (D-29).
- Por qué: cada empresa puede ajustar los límites y los textos de sus categorías (RF-13) sin cambiar los análisis ya hechos (C3), y siempre puede volver a los de D-10. Que la edición en bloque salga del generador deja esta API lista para otra temática, cambiando solo los JSON (RP-23).
- Cómo se pidió: Cristhian pidió primero el plan de RF-13, sin código. El plan debía responder seis preguntas:
  - Las rutas, y qué pasa con filas incompletas, repetidas o con un límite para Fuera de norma.
  - Qué aprende el generador y qué va a mano.
  - De dónde salen los valores por defecto al restaurar: la misma fuente de RF-02.
  - Cómo probar C2 y C3 sin el análisis.
  - Las pruebas por criterio, con los límites y la separación por empresa.
  - Si cambiaba V1.

  Lo aprobó con las seis decisiones propuestas y tres ajustes:
  - Agregar a las restricciones de RF-13 que los límites van de 0 a 100 con un decimal y que el de Fuera de norma es fijo en 0 %.
  - Contar los decimales sin los ceros del final (90,50 vale 90,5) y probar una recomendación de solo espacios.
  - Que `validar` rechace `solo_lectura_si` en una entidad que no es en bloque.

  En el camino pidió:
  - Corregir en el mismo paso los ejemplos booleanos de OpenAPI, para no tener otra versión del generador ni otro cambio de V1; revisar que no quedara ningún otro valor de Python y mencionarlo en el PR.
  - Que editar reponga una fila que falte, y anotarlo aquí y en el PR.
  - Que el PUT responda los límites como quedan guardados, igual que el GET.
  - Probar en su PC antes de la documentación, y correr la suite completa sobre el código final antes del borrador del PR.
- Archivos o PR: la rama `feature/RF-13-rangos`; el PR lo abre Cristhian.

## 20/10/2026 · RF-01, RF-18 y RNF-08: el frontend base

- Quién: Cristhian Sánchez (`DrekOF`), con Claude Code.
- Qué se hizo:
  - **En la rama `feature/frontend-base`, cuatro pasos, cada uno revisado por Cristhian antes del siguiente:**
    1. **El proyecto y sus pruebas, antes del código:**
       - `frontend/` con Vite, React, Material UI, React Router, Formik y Yup, e i18next, con las versiones exactas en `package.json`.
       - ESLint pide JSDoc en lo exportado (RP-09) y rechaza un color escrito a mano fuera de `src/tema/tokens.js` (RP-08). Los tokens salen de la tabla de colores del prototipo (`02_Analisis_Diseno/Prototipo_Frontend`), y una prueba revisa el contraste WCAG AA de cada par en los dos temas.
       - Las pruebas de las fichas, en rojo: unitarias y de componentes con Vitest y la API simulada (MSW); de punta a punta con Playwright (Chrome, Firefox y Safari, en PC y en celular) y con Selenium (Chrome y Edge).
       - `casos_de_prueba.py` 1.2 lee las marcas de las pruebas de JavaScript.
    2. **Las pantallas y la sesión:**
       - El marco del prototipo: la barra superior, el menú según el rol, «Saltar al contenido», el idioma y el tema; en el celular, el menú va detrás del botón.
       - Iniciar sesión; Cambiar contraseña, con sus reglas a la vista mientras se escribe; Acerca de, con la cita del dataset y la nota de que InspectIA no se cobra; el inicio de cada rol; y las secciones pendientes, que dicen «Esta sección estará disponible pronto.», sin códigos de requisitos.
       - La sesión (D-31): se guarda en el navegador, vence a las 8 horas, un 401 la cierra, vuelve a la pantalla anterior si entra el mismo usuario, y las pestañas se siguen entre sí.
       - Los textos que la página comparte con el backend salen de sus `.properties`, con un plugin de Vite; los propios, de `src/i18n/`.
       - Iniciar sesión solo revisa que la contraseña no esté vacía: el largo y los bytes los revisa el backend (RF-01 C8). Lo revisan tres pruebas; la de punta a punta entra con un byte más que una contraseña guardada de 72 bytes.
       - Sin conexión, el aviso dice «No se pudo conectar con el servidor. Revisa tu conexión a internet e intenta de nuevo.» (RNF-06 C3). Si el servidor responde un error sin mensaje, la página usa el `error.interno` del backend.
       - Las pruebas de «sin sesión, una pantalla interna lleva a Iniciar sesión» quedan sin criterio: RNF-04 C1 es de la API.
    3. **El entorno aislado de las pruebas de punta a punta:**
       - Una base en otro proyecto de Docker Compose (`inspectia-e2e`), el backend y la página compilada, en puertos libres y con contraseñas y clave nuevas en cada corrida. No usa `infra/.env`, la base local ni `backend/target`: compila una copia en espejo del backend dentro de `e2e/.entorno`.
       - El administrador de la plataforma sale del script de `infra/bd`, apuntado al proyecto aislado; antes de correrlo, el entorno revisa que docker compose llegue a esa base. Los datos de prueba se crean por la API.
       - `npm run e2e:entorno` lo deja encendido para correr las pruebas varias veces. Sin sus datos, las pruebas fallan diciendo cómo encenderlo.
       - La copia del backend es un espejo: lo borrado o renombrado en `backend/` desaparece de la copia, y entonces su `target` se compila de cero. Se probó con una clase temporal: al renombrarla y al borrarla, su `.java` y su `.class` desaparecieron de la copia.
    4. **Documentación:**
       - D-29 sin la frase del estado de las fichas, que ya está en `specs/fichas.json`.
       - D-31, con la Content-Security-Policy de CloudFront como riesgo aceptado, y D-32.
       - RF-01 (v1.6), con la restricción de volver a la pantalla anterior, y RF-18 (v1.5), en «Implementado».
       - El README, el del frontend, el de `infra/`, `CLAUDE.md` y esta bitácora.
  - **Problemas encontrados:**
    - **Stryker no activaba los mutantes con Vitest 5.**
      - Con Vitest 5, `volver.js` daba 5,6 % aunque sus pruebas cubren cada caso: los mutantes que se prueban con un filtro por nombre nunca se activaban. Es el fallo conocido de Stryker 10.0.0 con Vitest 5 (stryker-js #6210).
      - Se usa Vitest 4.1.12, que se mantiene y acepta Vite 8 y jsdom 30; con ella, `volver.js` llega al 100 %.
      - **Pendiente:** volver a Vitest 5 cuando Stryker publique el arreglo.
    - **El Firefox de Playwright no abre en Windows 11 25H2.** Windows rechaza cargar su `mozglue.dll` («la configuración en paralelo no es correcta»), aunque se reinstaló. Playwright 1.64.1 es la última y no trae otro Firefox. Con `INSPECTIA_E2E_SIN_FIREFOX=1` se salta en los equipos donde no abre; en CI corre siempre.
    - **axe marcó la barra superior (WCAG 2.5.3):** lo que se ve en un botón tiene que estar en su nombre accesible. El botón del idioma muestra ahora «Español»: se aparta del prototipo, que mostraba «ES». Las iniciales del avatar se dibujan con CSS (D-32).
    - **Pruebas que podían pasar sin revisar nada.** axe y la medida del desplazamiento lateral corrían sobre una página que aún no se había dibujado. La prueba del teclado en WebKit pulsaba Tab antes de que apareciera el formulario y fallaba 2 de cada 5 veces. Ahora todas esperan la pantalla.
    - **La hora del bloqueo:** `Intl` escribe «3:45 p. m.» con espacios que no se cortan, y `toHaveTextContent` los cambia por espacios comunes. La prueba los normaliza.
    - **Node:** el equipo tenía la 24.13.0, y jsdom 30 y el plugin de JSDoc de ESLint piden la 24.15 o superior. Se instaló la 24.22.0 con fnm, y `frontend/.node-version` la fija.
    - **En Windows, la consola no siempre busca programas en la carpeta actual** (`NoDefaultCurrentDirectoryInExePath`): el entorno llama a `.\mvnw.cmd`, con su ruta.
    - **Un intento contra el backend local.** Al revisar Iniciar sesión en el navegador, el proxy de Vite llevó un intento fallido al backend local de Cristhian, que estaba encendido. Desde entonces, las revisiones usan la API simulada o el entorno aislado, y el backend local no se usa sin preguntar.
    - **Una falla intermitente de Selenium** en la verificación final: 1 de 12, sin el detalle. No se repitió en las 8 corridas siguientes.
  - **Comportamiento que no piden las fichas**, que Cristhian aprobó como detalles de RNF-05 y RNF-06:
    - Al enviar un formulario con errores, el foco va al primer campo marcado.
    - Cada pantalla pone su título en la pestaña del navegador, como «Cambiar contraseña · InspectIA».
    - El aviso de por qué se cerró la sesión anterior es informativo (`role="status"`) y se quita al volver a enviar.
    - En el celular, el idioma y el tema van en el menú, porque la barra del prototipo no los muestra.
    - Volver a la pantalla anterior tras un 401 o al vencer la sesión, y que el 401 de un token ya reemplazado no cierre la sesión nueva, quedaron en D-31 y en las restricciones de RF-01.
  - **Resultado:**
    - Vitest: 204 pruebas en verde, con 98 % de cobertura de líneas y 95 % de mutantes muertos (Stryker). ESLint, limpio.
    - Playwright: 90 pruebas en verde, en Chrome y Safari de PC y de celular; las 2 omitidas son la del teclado en el celular. Firefox se saltó en este equipo.
    - Selenium: 12 pruebas en verde, en Chrome y Edge, de PC y de celular.
    - Generador: 63 pruebas en verde.
    - Criterios: RF-01 tiene sus 8 criterios con prueba y RF-18, sus 5; pasan a «Implementado» (D-29). De los 143 criterios de las fichas, 55 tienen prueba.
    - Todavía no hay workflow de CI. Cuando se cree, debe usar `node-version-file: frontend/.node-version`.
- Por qué: RF-01 y RF-18 necesitaban su pantalla para funcionar de punta a punta (D-29). El marco, los temas, los idiomas y las pruebas de punta a punta son la base de las pantallas que siguen, y el entorno aislado permite probar contra el backend real sin tocar la base de nadie.
- Cómo se pidió: Cristhian pidió primero el plan, sin código. El plan debía responder siete preguntas:
  - La estructura de `frontend/`.
  - Cómo se guarda y se cierra la sesión.
  - Las pantallas, el menú de cada rol y cómo se ven los errores de la API.
  - Cómo generará el generador las pantallas CRUD.
  - La paleta, con tonos de cacao.
  - Las pruebas, y si RF-01 y RF-18 pasarían a «Implementado».
  - Cómo se arranca junto con el backend y qué cambia en `CLAUDE.md` y los README.

  Lo aprobó con siete ajustes:
  - Seguir el prototipo como referencia visual.
  - Agregar Acerca de, con la cita del dataset.
  - Tras un 401, volver a la pantalla anterior solo si entra el mismo usuario y la ruta es interna.
  - Las secciones pendientes, sin códigos de requisitos.
  - Quitar de D-29 la frase del estado de las fichas.
  - D-31, con el riesgo de XSS y sus mitigaciones.
  - Usar los textos de los `.properties` del backend.

  En el camino pidió:
  - Marcar sin criterio las pruebas de RNF-04 C1, que es de la API.
  - Que Iniciar sesión no revise el largo de la contraseña, con una prueba (RF-01 C8), y revisar en la pantalla el texto sin conexión.
  - Usar Vitest 4.1.12 y anotar aquí la vuelta a Vitest 5.
  - Anotar lo no pedido como dice arriba.
  - Aplicar `npm audit fix` solo en el `package-lock.json`.
  - No usar su backend local sin preguntar.
  - Instalar Node 24.22.0 con fnm y saltar Firefox con una variable donde no abre.
  - Antes de la documentación:
    - La Content-Security-Policy de D-31 como riesgo aceptado.
    - No compilar en `backend/target` y que la copia sea un espejo, probado renombrando y borrando un archivo.
    - Registrar «ES» → «Español» como decisión.
    - Agregar `.node-version`.
    - Que las pruebas sin los datos del entorno fallen diciendo cómo encenderlo.
- Archivos o PR: la rama `feature/frontend-base`; el PR lo abre Cristhian.

## 26/10/2026 · RF-02 y RP-23: las pantallas generadas, empezando por la de empresas

- Quién: Cristhian Sánchez (`DrekOF`), con Claude Code.
- Qué se hizo:
  - **En la rama `feature/RF-02-pantallas-empresas`, seis pasos, cada uno revisado por Cristhian antes del siguiente:**
    1. **Las pruebas del generador, en rojo:** las pantallas de empresa y de producto (otra temática), sus textos, el filtro `activo` del listado (D-33) y los casos de límite que la prueba de Yup comparte con la de la API.
    2. **El generador 1.7:**
       - Un solo modelo de validaciones por campo (`campos.py`): de él salen Bean Validation, Yup y los casos de límite de las pruebas generadas de los dos lados.
       - Para cada entidad de `pantallas` en `alcance.json`: la pantalla, el cliente de su API, sus esquemas de Yup y sus pruebas (Vitest y Playwright), más el registro de pantallas y los textos de `pantalla.textos`, en español y en inglés.
       - El listado de la API generada filtra por `activo` (D-33). Su prueba generada lo revisa, junto con los valores del borde que acepta cada validación.
       - El formato del correo se valida solo en el backend, como el dígito del NIT.
       - Un patrón tiene que ir anclado con `^` y `$` y sin lo que Java, PostgreSQL y JavaScript leen distinto; si no, la generación se detiene.
       - Con dos ajustes de Cristhian: los mensajes de resultado, que llevan `{nombre}`, van en `pantalla.textos` de cada entidad; y la copia del backend del entorno aislado lleva también `infra/bd`, así sus pruebas pasan allí sin pasos a mano.
    3. **Las pruebas a mano del frontend, en rojo:** lo común de las pantallas (páginas, tarjetas en el celular, foco en el primer error, errores de la API y notas), Registrar empresa (RF-02 C1 a C6 y C11), el panel de administradores (C12) y el menú con una pantalla de otra temática.
    4. **El código del frontend:**
       - `PantallaDeRegistros` arma la pantalla de cada entidad con lo que dice su JSON: la lista en páginas fijas de 20 (tabla en el PC y tarjetas en el celular, D-34), su resumen, el formulario y la confirmación al desactivar.
       - Lo escrito a mano entra como extensión (`src/extensiones.js`): el panel de administradores de una empresa, que restablece la contraseña de uno con el aviso de D-30.
       - El menú y las rutas salen de `src/generated/pantallas.js`, con carga diferida. Una pantalla de otra temática recibe un ícono genérico.
       - En la revisión, Cristhian pidió tres cambios:
         - Quitar el selector «Filas por página»: las páginas van fijas de 20, como en el prototipo y en RF-14.
         - Que los casos de límite de un patrón prueben su anclaje, con el ejemplo y algo pegado antes o después (x900123456-8 y 900123456-8x).
         - Que Yup escoja el error de un campo como la API: primero el dato que falta y después por el nombre de la restricción, como `ManejadorDeErrores` (Pattern antes que Size), con un caso de límite que falla por el largo y por el patrón a la vez. Así el mismo dato da el mismo mensaje en los dos lados.
    5. **Las pruebas de punta a punta:**
       - Playwright, en PC y en celular: registrar una empresa y que su administrador entre (C8), desactivarla (C9), reactivarla (C10) y restablecer la contraseña de su administrador (C12). Cada prueba usa su propia empresa, con un NIT válido nuevo.
       - La prueba generada de la pantalla (permisos de cada rol, axe en los dos temas y 360 px) entra con `personaDelRol`. Para eso, el entorno aislado crea un usuario final fijo, Carlos Ramírez.
       - Selenium, en Chrome y Edge, de PC y de celular: registrar una empresa (C1).
       - A pedido de Cristhian, si una prueba de Selenium falla, guarda su captura y la consola del navegador en `frontend/reports/selenium/`, por si vuelve la falla intermitente del PR #6. Se probó con una prueba que fallaba a propósito, en Chrome y en Edge.
    6. **Documentación:**
       - D-33, D-34 y P-18.
       - RF-02 (v1.5) en «Implementado», con la confirmación de desactivar en sus restricciones.
       - El README de las entidades: la pantalla generada, el correo solo en el backend, los patrones anclados, un error por campo, los casos de límite y `mensajes.valores`.
       - Los README del generador, del frontend y el principal, `CLAUDE.md` y esta bitácora.
  - **Problemas encontrados:**
    - **El puntaje de mutación del frontend estaba inflado.**
      - Stryker corría con 23 procesos en un equipo de 24 procesadores lógicos. Las pruebas de componentes se volvían lentas, y Stryker cortaba por tiempo mutantes que no dejan un bucle infinito y los contaba como detectados: 129 «por tiempo», con 96,47 %. Con la mitad de los procesadores y 15 s de margen quedan 11, todos bucles de verdad, y el puntaje baja a 92,23 %.
      - Los mutantes estáticos (valores que se calculan al cargar un módulo, como las rutas del menú) no se evalúan bien con Vitest: su resultado depende de si el módulo ya estaba cargado, no de las pruebas. El de la ruta de Inicio «sobrevivía», pero al cambiarla a mano la prueba del menú falla. Eran 103 estáticos: 81 muertos y 22 vivos. Cristhian decidió dejarlos fuera del puntaje (`ignoreStatic: true`).
    - **RF-18 C5 falló una vez: el reloj de Windows retrocedió 2 segundos.**
      - En una corrida completa de Playwright, al recargar la otra pestaña después de cambiar la contraseña, la API respondió 200 en vez de 401: una sesión de antes del cambio siguió sirviendo (RNF-04 C6).
      - La causa: a las 11:14:03, un componente de Windows atrasó el reloj del sistema 2,004 s, de 16:14:05,530 a 16:14:03,525 en UTC, según el registro de eventos. El inicio de sesión de la pestaña principal quedó en el segundo 05 y el cambio, después, en el 04.
      - La regla compara en segundos enteros la emisión del token con la hora del cambio, y un token del mismo segundo sigue sirviendo (la ventana aceptada en el plan de RF-03). Por eso el token de la otra pestaña, del segundo 04, siguió sirviendo.
      - Se descartaron la zona horaria de `sesiones_desde` (es `timestamptz`), el orden de la transacción, un token equivocado y una caché.
      - En este equipo, el reloj retrocedió de 1 a 2 s seis veces en 14 días. Repetida 20 veces, la prueba pasó todas.
      - **Decisión de Cristhian:** un contador de sesión en lugar de la hora del cambio, en su propio PR (RNF-04), después de este y antes del de Usuarios (P-18). Hasta ese PR, RF-18 C5 puede fallar en este equipo si Windows ajusta la hora en ese momento.
  - **Mutantes equivalentes que quedan vivos**, con su explicación, como pidió Cristhian:
    - Los nombres de las pruebas de Yup, como `'obligatorio'` y `'bytes_max'`: cambiarlos no cambia ningún mensaje.
    - El mensaje del largo del NIT: todo NIT de más de 15 caracteres falla antes por el patrón, que va primero.
    - En el resumen, `if (resumen.activos)`: sin textos de activos, la frase no los usa.
  - **Comportamiento que no piden las fichas:**
    - Al abrir un panel (el formulario o el de administradores), el foco va a su título y, al cerrarlo, vuelve al botón que lo abrió. Desactivar y reactivar son el mismo botón, así el foco sigue en la fila (RNF-06 C4). Cristhian lo aprobó.
    - El caso de límite de la contraseña de 65 eñes, que falla por el largo y por los bytes: el nuevo orden de Yup cambiaba también ese mensaje, y sin el caso nada lo revisaba.
    - En el panel de administradores, los nombres de los botones y los mensajes llevan el nombre de la persona, como «Restablecer la contraseña de Jorge Medina» y «Se restableció la contraseña de Jorge Medina.».
    - `.gitattributes`: lo generado del frontend queda con LF, como lo del backend.
  - **Pendientes:**
    - **Para el PR de Usuarios (RF-03):** «Cambiar contraseña» (RF-18) revisa la contraseña nueva con
      Yup escrito a mano (`frontend/src/validacion/esquemas.js`), en el orden mínimo, máximo y bytes. Con 65 eñes,
      el formulario dice el error del largo y la API, el de los bytes. En ese PR, la pantalla pasa a usar las
      constantes generadas y revisa en el orden en que la API escoge el error de un campo, como las pantallas
      generadas (`campos.orden_de_la_api`, igual que `ManejadorDeErrores`). Además, ninguna prueba revisa el borde
      de 72 bytes exactos de su lista de reglas: en `reglasDeContrasena` (`frontend/src/validacion/reglas.js`),
      cambiar `<=` por `<` no hace fallar ninguna prueba.
    - **P-18, el PR del contador de sesión (RNF-04), después de este y antes del de Usuarios:** incluye `infra/bd/restablecer-admin-plataforma.sql`, que hoy toma la hora de la base (`now()`) y no la del backend. Con el contador desaparece también el desfase entre relojes que explica `infra/README.md`.
  - **Resultado:**
    - Vitest: 344 pruebas en verde y 93,95 % de mutantes muertos (Stryker, sin los estáticos). ESLint, limpio.
    - Playwright: 125 en verde y 2 omitidas (la del teclado en el celular), con 1 falla de RF-18 C5 por el reloj (arriba). Firefox se saltó en este equipo.
    - Selenium: 16 en verde.
    - Generador: 89 pruebas en verde, y lo generado, al día (49 archivos).
    - Backend: la suite completa (378 pruebas) pasó en el paso 2. Después pasaron las pruebas generadas de las API de empresa (65) y de usuario (68) y las 112 unitarias, en la copia del entorno aislado.
    - Criterios: RF-02 tiene sus 12 criterios con prueba y pasa a «Implementado» (D-29). De los 143 criterios de las fichas, 55 tienen prueba.
- Por qué: RF-02 necesitaba su pantalla para funcionar de punta a punta (D-29), y las pantallas generadas son la base de las de usuarios y rangos (RP-23): cambiar de temática sigue siendo cambiar solo los JSON.
- Cómo se pidió: Cristhian pidió primero el plan, y lo aprobó con siete decisiones:
  1. Los textos propios de cada pantalla van en `pantalla.textos`, sin claves de género ni de persona.
  2. Contar los activos con el filtro `?activo` del listado (D-33).
  3. El formato del correo, solo en el backend.
  4. Los cambios frente al prototipo de D-34.
  5. Tres PR (empresas, usuarios y rangos), con la demostración de otra temática en el tercero.
  6. RF-02 pasa a «Implementado» con este PR; RF-03 y RF-13 esperan a RF-14 y a RF-12.
  7. En Usuarios, la nota de D-30 cuando el administrador escribe la contraseña de otro.

  En el camino pidió:
  - Corregir lo que dice el README de las entidades sobre `mensajes.valores`.
  - Los mensajes de resultado en `pantalla.textos`, y que la copia del backend lleve `infra/bd`.
  - Quitar el selector de filas por página.
  - Probar el anclaje de los patrones y que Yup escoja el error como la API, con un caso que falle por dos validaciones.
  - Dejar los mutantes equivalentes con su explicación, y poner `ignoreStatic: true` en Stryker, anotando por qué.
  - Arreglar «Cambiar contraseña» en el PR de RF-03 y anotarlo aquí como pendiente, con el borde de 72 bytes.
  - Escribir en el README que un correo demasiado largo y sin arroba da un error en el formulario y otro en la API.
  - Que Selenium guarde su captura y la consola del navegador cuando una prueba falla.
  - Investigar en esta misma sesión la falla de RF-18 C5, y anotar el diagnóstico, la decisión del contador y su PR.
- Archivos o PR: la rama `feature/RF-02-pantallas-empresas`; el PR lo abre Cristhian.

## 26/10/2026 · RNF-04 y P-18: el contador de sesión

- Quién: Cristhian Sánchez (`DrekOF`), con Claude Code.
- Qué se hizo:
  - **En la rama `feature/RNF-04-contador-de-sesion`, commit por commit, cada uno revisado por Cristhian antes del siguiente:**
    1. **Las pruebas en rojo** (`b6cbdea`):
       - El corte de sesiones con un reloj de prueba (`RelojDePrueba`). Si el reloj se atrasa entre un inicio de sesión y el cambio de contraseña, esa sesión deja de servir; si se atrasa después del cambio, un inicio de sesión nuevo sirve. Una sesión del mismo segundo, pero de antes del cambio, deja de servir en los tres caminos que cambian una contraseña.
       - Cambios a la vez, con el orden de las dos transacciones fijo por `CountDownLatch`, sin esperas por tiempo: desactivar a un usuario mientras cambia su contraseña no lo reactiva, editar el nombre mientras se restablece la contraseña no la deshace, y dos restablecimientos a la vez suben el contador dos veces.
       - El script del administrador corta la sesión aunque el reloj de la base vaya atrasado respecto al del backend. Con el de la base adelantado, un inicio de sesión hecho justo después del script sirve.
       - Los tokens llevan `version_sesion`. En el generador, `solo_lo_cambian_las_reglas` y `@DynamicUpdate`.
    2. **El contador** (`9a40413`):
       - `usuario.version_sesion` reemplaza a `sesiones_desde`: un entero que empieza en 0 y sube en uno con cada cambio o restablecimiento de contraseña, con una sola sentencia (`UPDATE … RETURNING`). En cada petición, un token con otra versión responde 401 «Tu contraseña cambió».
       - El generador 1.8: la clave `solo_lo_cambian_las_reglas`, para una columna que JPA lee pero no escribe, y `@DynamicUpdate` en las entidades generadas.
       - El script `restablecer-admin-plataforma.sql` sube la versión en lugar de tomar la hora de la base.
    3. **Dos ajustes de Cristhian** (`97eefd3` en rojo y `0f1bf83`):
       - Un fallo que bloqueaba la cuenta mientras se restablecía la contraseña la dejaba bloqueada: con `@DynamicUpdate`, la entidad que se leyó sin bloqueo no escribía `bloqueado_hasta`. La misma sentencia que sube la versión deja ahora los intentos en 0 y quita el bloqueo, como el script.
       - La versión de sesión del token tiene que ser un entero de 0 a 2147483647, el rango de la columna; antes servía cualquier número.
    4. **El reloj al microsegundo** (`5c854aa` en rojo y `a031cd8`), por el problema que sigue.
    5. **Documentación** (27/10/2026):
       - D-35 (el contador de sesión) y D-36 (`@DynamicUpdate`), con la fecha en que se decidieron, el 26/10/2026. P-18 queda «Resuelto: ver D-35», y D-28 lleva una nota: el token lleva también la versión de sesión.
       - `usuario.json` cita D-35 en `version_sesion` y en la regla, cuya descripción dice ahora que la misma sentencia quita el bloqueo; lo generado se regeneró.
       - `modelo-datos.md`, el README de las entidades (`solo_lo_cambian_las_reglas` y `@DynamicUpdate`), el del generador, `infra/README.md`, sin «Relojes en local», y `frontend/README.md`, sin la nota de RF-18 C5.
    6. **Cierre** (27/10/2026): Playwright otra vez, sobre `93d4f10`, y el `clean` en el reinicio de la base de `infra/README.md`.
  - **Problemas encontrados:**
    - **El quinto fallo seguido de RF-18 respondía, a veces, que faltaban 901 segundos de un bloqueo de 900.**
      - Lo mostró la primera corrida de las pruebas en rojo: `CambioDeContrasenaIT.cincoFallosBloquean`, que este PR no tocaba, falló con un `Retry-After` de 901, y en la corrida siguiente, otra vez.
      - La causa: en Windows, la hora de Java trae décimas de microsegundo, y PostgreSQL guarda la hora al microsegundo; el driver (42.7.8) redondea lo que sobra. El quinto fallo guarda `bloqueado_hasta`, la hora más 15 minutos, lo vuelve a leer y le resta la misma hora. Si el driver redondeó hacia arriba, sobran unos cientos de nanosegundos, y la cuenta de los segundos que faltan, que redondea hacia arriba, da 901.
      - Mediciones en este equipo: la fracción bajo el microsegundo cambia más o menos una vez por milisegundo (unas 175 veces en 200 ms), y sus 10 valores, de 0 a 900 ns, salen con la misma frecuencia. El driver redondea hacia arriba los de 500 ns o más; por eso fallaba más o menos la mitad de las veces.
      - El arreglo, como lo pidió Cristhian: el bean `reloj` de `InspectiaAplicacion` da la hora al microsegundo (`Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000))`), en lugar de truncarla en los servicios. Así queda cubierto también el código que venga.
      - La prueba, en `InspectiaAplicacionIT`, lee el bean del contexto real 1.000 veces, una cada 50 µs, y ninguna lectura puede traer fracciones de microsegundo. La primera versión, con las lecturas sin pausa, pasó sin el arreglo: 1.000 lecturas seguidas duran unos 30 µs y caen casi siempre en una sola fracción, que una de cada 10 veces es 0. Espaciadas, cubren unos 50 ms, y sin el arreglo fallaron en tres corridas seguidas (926, 962 y 869 lecturas con fracción).
      - Con el arreglo, `cincoFallosBloquean` pasó en las 10 corridas seguidas en que se repitió, cada una con su propio reporte, y en la suite completa.
  - **Comportamiento que no piden las fichas:**
    - Al desplegar este cambio, las sesiones abiertas antes, cuyos tokens no traen versión de sesión, se cierran una vez: hay que iniciar sesión de nuevo (D-35).
    - Un token con una versión mayor que la de la base tampoco sirve, como uno con una menor. Pasaría si la base se restaura desde una copia.
    - La versión de sesión del token tiene que ser un entero de 0 a 2147483647: el plan decía «entero ≥ 0», y el máximo de la columna se agregó porque una versión mayor nunca coincidiría con la de la base y, al pasarla a `int`, se desbordaría. Cristhian lo aprobó.
    - El script del administrador tiene dos pruebas de relojes: la base atrasada y la base adelantada respecto al backend. El plan tenía una; la segunda cubre el otro caso de «Relojes en local», que así se pudo quitar de `infra/README.md`.
  - **Resultado:**
    - Backend: 119 pruebas unitarias y 399 de integración en verde, en la copia del entorno aislado.
    - Generador: 93 pruebas en verde, y lo generado, al día (49 archivos).
    - Playwright, en el entorno aislado sobre `93d4f10`, con todo el código del PR: 126 en verde y 2 omitidas, las del teclado en el celular. RF-18 C5 pasó en los cuatro proyectos. Sobre `9a40413`, antes de los ajustes, había dado lo mismo. Firefox se saltó en este equipo.
    - Vitest: 344 en verde, y ESLint, limpio. Este PR no cambia código del frontend, solo la fecha de un archivo generado. Una corrida al mismo tiempo que la suite del backend tuvo 4 fallas, y la misma corrida sola pasó completa, en 18,6 s contra 46 s; no se guardaron los mensajes de esas fallas.
    - Casos de prueba: 541 pruebas; de los 143 criterios de las fichas, 55 tienen prueba.
    - Recorrido: Cristhian lo hizo en su PC y estuvo bien.
- Por qué: P-18. El corte de sesiones comparaba, en segundos enteros, la hora de emisión del token con la del cambio de contraseña, así que dependía del reloj: en este equipo, Windows atrasó la hora 2 segundos y RF-18 C5 falló (ver la entrada anterior). Con un contador, el corte no depende de la hora.
- Cómo se pidió: Cristhian pidió primero el plan, sin código, y lo aprobó con cuatro decisiones:
  1. El token se rechaza si su versión es distinta de la de la base.
  2. Quitar `sesiones_desde`.
  3. La clave del generador se llama `solo_lo_cambian_las_reglas` y va solo en `version_sesion`.
  4. `@DynamicUpdate` en las entidades generadas, en este PR.

  Sumó al plan el caso del reloj que se atrasa después del cambio, las dos pruebas de cambios a la vez con el orden fijo y sin esperas por tiempo, el generador 1.8 y las decisiones D-35 y D-36. En el camino pidió:
  - Desbloquear la cuenta en la misma sentencia que sube la versión, con su prueba en rojo primero, y que la versión del token sea un entero ≥ 0, como decía el plan.
  - Arreglar el 901 en este PR, en commits aparte: el bean `reloj` al microsegundo, no truncar en los servicios; una prueba en rojo del bean con 1.000 lecturas, que quedaron espaciadas por la medición de arriba; comparar los `nanoTime` con una resta, por el desborde; corregir el JavaDoc de `RelojDePrueba`, anotar aquí la causa y el arreglo, y correr `cincoFallosBloquean` unas 10 veces.
  - D-35 y D-36 con la fecha en que se decidieron, el 26/10/2026, como D-31 y D-32, y el borrador del PR con el aviso de reiniciar la base local y sus pasos para PowerShell en VS Code.
  - Antes de subir la rama: correr Playwright otra vez sobre el último commit, porque después de `9a40413` cambiaron el desbloqueo, el validador y el reloj; arrancar el backend con `.\mvnw clean spring-boot:run` en el reinicio de la base, como lo corre en PowerShell; y subir la rama después de su recorrido.
- Archivos o PR: la rama `feature/RNF-04-contador-de-sesion`; el PR lo abre Cristhian.

## 27/10/2026 · RF-03 y RP-23: la pantalla de usuarios

- Quién: Cristhian Sánchez (`DrekOF`), con Claude Code.
- Qué se hizo:
  - **En la rama `feature/RF-03-pantalla-usuarios`, commit por commit, cada uno revisado por Cristhian antes del siguiente:**
    1. **Las pruebas del generador, en rojo** (`2c9550e`): las claves nuevas de `pantalla` (`formulario`, `limite_de_activos` e `inicial`), `en_la_fila_propia` en las reglas, `{maximo}` en el resumen, los textos de la fila propia y de la contraseña al editar, la pantalla de usuario y los límites de los campos que usa Cambiar contraseña (RF-18).
    2. **El generador 1.9 y los JSON** (`bd7b7fd`):
       - `usuario.json` (1.8) trae su pantalla: el correo bajo el nombre, el formulario al lado de la lista, el límite de `maximo_usuarios_activos`, el rol que empieza en Usuario final (`pantalla.inicial`) y sus textos, con el resumen «5 de 25 usuarios activos, contándote a ti.» y la nota de D-30. Sus tres reglas sobre la fila propia llevan `en_la_fila_propia` (C7, C8 y C11). `alcance.json` (1.4) agrega la pantalla.
       - El generador arma dónde va el formulario, el límite de activos, la fila propia con su marca, la contraseña opcional al editar (vacía va como `null`; si llega, no puede estar en blanco), los límites de los campos (`LIMITES`) y el resumen con `{maximo}`, que pide solo los textos contados que usa su frase.
       - Una lista obligatoria vacía, como el rol, da en Yup el error de obligatorio, como la API, y no el de sus valores: Yup revisa `oneOf` antes que las demás pruebas. Salió al generar la primera pantalla con una lista obligatoria. Su prueba del generador, `test_lista_obligatoria_vacia`, va en este commit y se vio fallar antes del arreglo.
       - El backend generado solo cambia en los headers, también el de `V1__esquema_inicial.sql`: hay que reiniciar la base local (P-16).
    3. **Las pruebas del frontend, en rojo** (`39489bc`): `GestionarUsuarios.test.jsx`, con C1 a C5, C7, C8, C10, C11 y la nota de D-30; el texto de una regla sin campo; Cambiar contraseña con el orden de la API y los límites del JSON; y Usuarios en el menú.
    4. **El código del frontend** (`d935bec`):
       - `PantallaDeRegistros`, lo común de las pantallas generadas:
         - el formulario va al lado de la lista en el PC y encima en el celular;
         - con el máximo de activos, un aviso que no interrumpe, y «Nuevo usuario» y «Reactivar» deshabilitados, con el aviso como descripción;
         - el resumen cuenta los activos contra el máximo;
         - la fila propia lleva «Tú» y no tiene «Desactivar»;
         - la nota de D-30 aparece al escribir la contraseña de otra persona, debajo de los campos y encima de los botones.
       - `MensajeDeLaFilaPropia`: el mensaje de la regla en lugar de la contraseña propia, con el enlace solo en «Cambiar contraseña». La plantilla de la prueba generada y la prueba de C11 revisan la frase completa, por el texto del párrafo, y el enlace por su nombre.
       - Cambiar contraseña (RF-18) usa la validación generada de la contraseña de un usuario, en el orden de la API: con 65 eñes da el error de los bytes, como la API. Así se cierra el pendiente que dejó el PR de empresas, con el borde de 72 bytes de `reglasDeContrasena`, que ahora recibe los límites del JSON (`LIMITES.contrasena`) como parámetro, para no tener un ciclo de importaciones.
       - El texto de una regla sin campo que no deja guardar es su mensaje, no «Revisa los datos marcados.».
       - Con el código van pruebas nuevas: `noEnBlanco` y `vacioComoAusente`, el resumen con el máximo, C11 en inglés y `textoDelError` con errores de campos y de reglas a la vez, que pidió Stryker.
    5. **El caso de `oneOf` en la prueba generada de Yup** (`db42659` en rojo y `10dc47c`): un caso por cada valor de una lista que existe pero el formulario no ofrece, como ADMIN_PLATAFORMA en el rol, con el mensaje del 403 de la API. Antes, el generador mandaba ese caso solo a la prueba de permisos de la API, y Stryker dejaba vivos los dos mutantes del mensaje de `oneOf` del rol.
    6. **Las pruebas de punta a punta** (`828b25d`):
       - Playwright, en PC y en celular, contra el backend real: C1, C2, C3 y C4 (al crear y al restablecer), C5, C6 y reactivar, C7 a C11 y dónde va el formulario. Cada prueba registra su propia empresa por la API y entra con su administrador, para no chocar con los otros navegadores ni llenar los 25 activos de la empresa de prueba.
       - Selenium, en Chrome y Edge, de PC y de celular: C1, en una empresa propia.
    7. **El formulario a 1250 px** (`5cd1171` en rojo y `dbe350c`): ver «Problemas encontrados».
    8. **Documentación:** D-37; el README de las entidades (las claves nuevas de `pantalla`, la fila propia, `en_la_fila_propia` con su motivo, RP-23, y el caso de `valores_en_formulario` en los casos de límite); los README del generador, del frontend, de infra y el principal; y esta bitácora.
  - **Problemas encontrados:**
    - **Una carrera en `botonDeLaFila` hizo fallar C9 y C10 de RF-02.**
      - En la primera corrida completa de Playwright fallaron dos pruebas de empresas en celular-chrome. Una no encontró la empresa en ninguna página; la otra encontró el botón en una fila que ya había cambiado («element was detached»).
      - La causa: al pasar de página, el rango («21–40 de 80») cambia enseguida, pero las filas siguen siendo las de la página anterior hasta que responde la API, y la ayuda solo esperaba el rango. Con las empresas que registran las pruebas nuevas, la lista tiene más páginas y la API tarda más, y la carrera apareció.
      - El arreglo: antes de buscar en la página nueva, la ayuda espera que cambie su primera fila, de la tabla o de la lista con nombre (no la del panel de administradores). Con él, Playwright pasó completo dos veces seguidas en el mismo entorno.
    - **A 1280 px, el formulario cabía al lado de la lista con 1 px de sobra.**
      - Se midió con la barra de desplazamiento visible, que ocupa 15 px en el Chromium de Playwright, en Edge y en Chrome. La fila mide 945 px, y el formulario necesitaba 944: la lista partía de 600, más 24 de separación y 320 del formulario, los valores del prototipo. En una ventana un poco más angosta, el formulario pasaba debajo de la lista.
      - **Decisión de Cristhian:** la lista parte de 560 px. Primero, una prueba de punta a punta en una ventana de 1250 px, que falló: el formulario empezaba en x = 305, debajo de la lista. Después, el cambio. A 1280 px se ve igual, porque la lista crece hasta llenar el espacio, y sobran 41 px. Queda en D-37.
  - **Mutantes que quedan vivos** (Stryker, sobre los `.js` que cambiaron):
    - En el paso 4, 94,55 % con 17 vivos. Dos eran huecos de las pruebas nuevas (`textoDelError` con errores de campos y de reglas, y el resumen, que pedía textos que no usa) y se cerraron: 95,19 %, con 15 vivos.
    - En el paso 5, `usuarioEsquemas.js` pasó de 76,92 % a 80,77 %: murieron los dos mutantes del mensaje de `oneOf` del rol.
    - Quedan 13, equivalentes: los nombres de las pruebas de Yup (10 en `usuarioEsquemas.js` y 2 en `esquemas.js`), que solo van en el tipo del error, que la app no lee, y el respaldo de `erroresPorCampo`.
    - Stryker no muta los `.jsx`: los cubren las pruebas de las pantallas.
  - **Los números de las reglas de «Cambiar contraseña» se quedan escritos en sus textos (8, 64 y 72).** Decisión de Cristhian: también están en la ayuda del panel de administradores y en las ayudas de la contraseña de `usuario.json`; cambiar solo un lugar no quita la copia, y son valores estables.
  - **Comportamiento que no piden las fichas:**
    - La nota de D-30 al escribir la contraseña de otra persona: «Al guardar, se cierran las sesiones abiertas de {nombre} y se quita su bloqueo por intentos fallidos, si lo tenía.». Solo cuenta lo que ya hace D-30.
    - El resumen cuenta a quien tiene la sesión: «5 de 25 usuarios activos, contándote a ti.».
    - Yup rechaza ADMIN_PLATAFORMA en el rol, con el mensaje del 403, aunque el formulario no lo ofrece.
  - **Resultado:**
    - Generador: 107 pruebas en verde, y lo generado, al día (56 archivos).
    - Vitest: 463 pruebas en verde, y ESLint, limpio.
    - Backend: 119 pruebas unitarias y 399 de integración en verde, en la copia del entorno aislado. En este PR solo cambiaron los headers de lo generado.
    - Playwright, en el entorno aislado sobre `dbe350c`, con todo el código del PR: 204 en verde y 4 omitidas (iniciar sesión solo con el teclado y la de 1250 px, en el celular). Firefox se saltó en este equipo.
    - Selenium: 20 en verde.
    - Las corridas de punta a punta se hicieron con un script temporal, fuera del repositorio, que enciende el entorno aislado una vez, corre `npx playwright test` y `npm run e2e:selenium` con `INSPECTIA_E2E_URL` y lo apaga al final.
    - Casos de prueba: 633 pruebas; de los 143 criterios de las fichas, 55 tienen prueba. RF-03 tiene sus 11 criterios con prueba, pero sigue en «Aprobado»: la parte del historial de C6 se prueba con RF-14 (D-29).
    - El recorrido: Cristhian lo hizo en su PC y estuvo bien. Se anota aquí porque no alcanzó a entrar en el PR #9.
- Por qué: RF-03 necesitaba su pantalla para funcionar de punta a punta (D-29). Es la segunda pantalla generada (RP-23): lo propio de la fila de quien tiene la sesión y el límite de activos quedan declarados en el JSON, así que otra temática los cambia sin tocar código.
- Cómo se pidió: Cristhian pidió primero el plan y lo aprobó con seis decisiones:
  1. El resumen «5 de 25 usuarios activos, contándote a ti.», sin la empresa ni negrita, con `{maximo}`.
  2. `en_la_fila_propia` va en el JSON, porque se puede declarar (RP-23), y el README de las entidades lo dice así.
  3. El rol inicial sale de `pantalla.inicial`.
  4. Con 25 activos, también «Reactivar» se deshabilita, con el aviso como descripción.
  5. La nota de D-30 aparece al escribir en «Contraseña nueva».
  6. El formulario va al lado de la lista en el PC y encima en el celular.

  D-37 recoge cómo se aparta del prototipo. En el camino pidió:
  - Dejar `test_lista_obligatoria_vacia` en el commit del generador y anotar aquí que se vio fallar antes del arreglo; la nota de D-30 debajo de los campos y encima de los botones; y el aviso de reiniciar la base en el PR, con sus pasos, como en el PR #8. Aprobó también que el resumen use el mismo texto en singular y en plural, que el inglés diga «they» y que empresa tenga `LIMITES` aunque no los use.
  - El enlace solo en «Cambiar contraseña», porque el texto del enlace dice a dónde lleva, y que la plantilla de la prueba generada y la prueba de C11 revisen la frase completa y el enlace por su nombre.
  - Que le escriba en español, también los avisos mientras trabajo.
  - El caso de `oneOf` en dos commits, antes de las pruebas de punta a punta, y dejar los números de las reglas de «Cambiar contraseña» como están, anotando por qué.
  - El arreglo de `botonDeLaFila` en «Problemas encontrados» del PR.
  - Bajar la base de la lista a 560 px, con la prueba de 1250 px primero, y anotarlo en D-37.
  - Al final, el borrador del PR con «Fichas y documentación» y el aviso de reiniciar la base y sus pasos.
- Archivos o PR: la rama `feature/RF-03-pantalla-usuarios`; el PR lo abre Cristhian.

## 28/10/2026 · RF-13 y RP-23: la pantalla de rangos y la demostración de otra temática

- Quién: Cristhian Sánchez (`DrekOF`), con Claude Code.
- Qué se hizo:
  - **En la rama `feature/RF-13-pantalla-rangos`, commit por commit, cada uno revisado por Cristhian antes del siguiente:**
    1. **Las pruebas del generador, en rojo** (`f526083`): la pantalla en bloque de rangos, los campos decimales y de varias líneas, los textos de editar y de restaurar, el mensaje «Escribe un número, como 90,5.» y los casos de límite que fallan por dos validaciones a la vez, 100,55 y −0,55.
    2. **El generador 1.10 y los JSON** (`19bf435`):
       - `rango_recomendacion.json` (1.3) trae su pantalla: el ícono, la descripción y los textos de editar, guardar y restaurar, con su confirmación. La recomendación lleva `varias_lineas`. `alcance.json` (1.5) agrega la pantalla.
       - El generador describe la pantalla en bloque: la lista que identifica las filas, los campos por su tipo (un número con su escala y su `solo_lectura_si`, o un texto en varias líneas) y los textos. La pantalla común, `PantallaEnBloque`, los arma.
       - La prueba generada de Yup usa los números escritos en español, como «100,55», y la de la API suma dos casos: 100,55 da el error del máximo, y −0,55, el del mínimo.
       - El backend generado solo cambia en los headers, también el de `V1__esquema_inicial.sql`: hay que reiniciar la base local (P-16).
    3. **Las pruebas del frontend, en rojo** (`0221b14`): `GestionarRangos.test.jsx`, con C1 (con los bordes del backend: el empate, 96,0 y 95,9, y Corriente en 0,1 y en 0), C4 y C5; el resumen; los números en español y en inglés; y Fuera de norma de solo lectura. También las pruebas de la regla del orden, de los decimales, del campo de número y del de varias líneas.
    4. **El código del frontend** (`aa8493e`):
       - `PantallaEnBloque`: las filas en un formulario, guardar, restaurar con su confirmación y los errores de la API, de cada fila y del bloque.
       - `CampoDeNumero`, `variasLineas` en `CampoDeTexto`, `textoDelDecimal`, `numeroDecimal` y `maximoDecimales`.
       - En `src/rango/`, lo escrito a mano para los rangos, conectado en `extensiones.js`: el resumen «Así quedan los rangos» y la regla del orden de los límites, la misma del backend.
       - Las esperas de las pruebas de Vitest (ver «Problemas encontrados»).
    5. **La demostración de otra temática** (`8dfcef7`): `npm run demo:otra-tematica`, desde `frontend/`.
       - En una copia del proyecto, en `frontend/.demostracion`, agrega a InspectIA el producto de la pauta del docente y lo suma al alcance.
       - Genera su tabla, su API y su pantalla, lista por capa lo que escribió el generador (23 archivos) y enciende la copia como el entorno aislado de las pruebas, en el proyecto `inspectia-demo`.
       - Con `--con-pruebas`, corre antes en la copia las pruebas generadas de producto: 27 de Vitest y 34 de `ProductoApiGeneradaIT`.
       - El repositorio no cambia. `encenderEntorno` recibe la carpeta, el proyecto y un paso de preparación, y sin opciones las pruebas de punta a punta lo usan igual.
       - En Windows, el `node_modules` se enlaza en la copia con un junction, que no pide permisos de administrador.
    6. **Las pruebas de punta a punta** (`12265be`):
       - Playwright, en PC y en celular, cada prueba en su propia empresa, porque guardar cambia sus rangos:
         - C1: con Estándar en 97 no guarda y, al recargar, Estándar sigue en 90,0;
         - C4: guarda un límite y un texto propios, restaura con la confirmación y, al recargar, están los de D-10;
         - 90,50 queda en 90,5, también al recargar.
       - Selenium: C4, en Chrome y en Edge, de PC y de celular.
    7. **Documentación:**
       - D-38;
       - el README de las entidades: la pantalla en bloque, `varias_lineas`, los textos de restaurar, los números en el formulario y los casos de límite, con los bordes de la API en una edición en bloque;
       - los README del generador, del frontend, de infra y el principal, y los Comandos de `CLAUDE.md`;
       - esta bitácora, con el recorrido de Usuarios en su entrada.
    8. **El apellido de Julián** (`de853e8`), en un commit aparte: es Silva, no Silvas. Se corrigió en `README.md`, `CLAUDE.md` y el texto de «Acerca de», en español y en inglés. Ninguna prueba revisaba ese texto.
  - **Problemas encontrados:**
    - **Pruebas de Vitest que fallaban con la máquina ocupada.** Es la segunda vez, después de las 4 del PR #8, y Cristhian pidió buscar la causa.
      - Con dos corridas de Vitest a la vez, en 3 rondas, fallaron de 6 a 10 pruebas por corrida, 12 distintas. Se guardaron los mensajes de cada corrida. Las causas fueron tres:
        - `findBy` y `waitFor` esperaban 1 s, lo que trae Testing Library. La primera vez que se abre una pantalla con carga diferida, como Empresas o Usuarios, su módulo tarda más que eso en cargar con la máquina ocupada.
        - Vitest cortaba cada prueba a los 5 s, y las que llenan un formulario entero, como registrar una empresa, pasaban de ese tiempo.
        - Una falla en cascada: RF-02 C3 falló solo cuando la prueba anterior, C2, se cortó por tiempo. Vitest corta la prueba, pero no su `usuario.type`, que siguió escribiendo en «Nombre», el campo con el foco de C3. Un registro temporal lo mostró: había 5 mensajes y no 6, y «Nombre» tenía el foco y ya no tenía su error. Corriendo C3 sola, nunca falla.
      - El arreglo, en la configuración común de las pruebas: `findBy` y `waitFor` esperan hasta 5 s (`src/pruebas/preparacion.js`) y cada prueba tiene hasta 20 s (`vite.config.js`). Una prueba que pasa no espera más.
      - Con la misma carga, 0 fallas en 6 corridas, antes y después del código de los rangos. La más lenta tardó 8,0 s.
    - **El nombre de una prueba del paso 3 decía algo que no puede pasar:** «como cuando otra persona cambió los rangos al mismo tiempo». La API revisa el bloque que le llega, no lo que hay guardado. Se quitó en el paso 4.
    - **La demostración no podía copiar el frontend dentro de sí mismo.** La primera corrida falló, porque `cpSync` no copia una carpeta dentro de sí misma, aunque esté excluida. Ahora la copia en espejo copia entrada por entrada del primer nivel. La copia va en `frontend/.demostracion`, y no en la raíz del repositorio, porque allí el backend de la copia encontraría `infra/.env`, con las contraseñas de desarrollo.
    - **Vite reescribe `new URL(…, import.meta.url)` en las pruebas.** Lo convierte en una dirección de su servidor (`http://localhost:3000/@fs/…`), y la prueba de la demostración no cargaba. `entorno.js` y la demostración ahora sacan sus rutas con `dirname(fileURLToPath(import.meta.url))`.
  - **Mutantes que quedan vivos** (Stryker, sobre los `.js` nuevos o cambiados):
    - `limites.js`, `decimales.js` y `reglas.js`: 100 %. En la primera corrida sobrevivían 4. Tres eran equivalentes y se quitaron simplificando el código; el cuarto pedía dos casos más del resumen.
    - `otra-tematica.js`: 100 %, sin contar la parte que enciende la copia (Docker, Maven y la consola), que se prueba corriéndola.
    - `espejo.js`: 3 vivos, equivalentes, que ya sobrevivían antes de este PR: `force` al borrar algo que existe, el valor por defecto de `excluir` y `preserveTimestamps`, porque en Windows copiar un archivo ya conserva su fecha.
  - **Mejora posible:** la regla del orden de los límites va escrita a mano, en el backend y en el frontend. Podría volverse declarativa en el JSON, y otra temática no necesitaría código propio para ella. Decisión de Cristhian: queda anotada aquí, sin pendiente.
  - **Resultado:**
    - Generador: 117 pruebas en verde, y lo generado, al día (63 archivos).
    - Vitest: 563 pruebas en verde, y ESLint, limpio.
    - Backend: 119 pruebas unitarias y 401 de integración en verde, en la copia del entorno aislado.
    - Playwright, en el entorno aislado sobre `de853e8`, con todo el código del PR: 236 en verde y 4 omitidas (iniciar sesión solo con el teclado y la de 1250 px, en el celular). Firefox se saltó en este equipo. Las de rangos pasaron también 3 veces seguidas en los 4 proyectos, sobre `12265be`.
    - Selenium, sobre `de853e8`: 24 en verde.
    - La demostración, con `--con-pruebas`: encendida en unos 50 s. El recorrido salió bien: «Productos» al final del menú; registrar, editar y pasar a Inactivo; Swagger con `/api/v1/productos`; y `\d producto` en psql.
    - Casos de prueba: 707 pruebas; de los 143 criterios de las fichas, 55 tienen prueba. RF-13 tiene prueba en C1, C3, C4 y C5, y sigue en «Aprobado»: C2, que un análisis nuevo use los rangos nuevos, llega con RF-12 (D-29).
    - El recorrido: Cristhian lo hizo en su PC y estuvo bien, también la demostración y su Ctrl+C.
- Por qué: RF-13 necesitaba su pantalla para funcionar de punta a punta (D-29). Es la tercera pantalla generada (RP-23) y la primera en bloque. La demostración muestra en la sustentación que, cambiando solo los JSON, sale otro CRUD completo.
- Cómo se pidió: Cristhian pidió primero el plan, sin código, y lo aprobó con seis decisiones:
  1. El formulario también revisa el orden de los límites, con el mensaje del JSON, y marca los dos límites de cada par que no baja. Sus pruebas usan los mismos bordes que las del backend.
  2. Números con coma en español y con punto en inglés; al escribir se aceptan los dos.
  3. Sin los subtítulos de las categorías, porque el resumen ya dice qué abarca cada una.
  4. La recomendación en varias líneas, con `varias_lineas: true` en el JSON.
  5. La demostración: InspectIA más producto, en una copia.
  6. La regla del orden se queda escrita a mano, con la mejora posible anotada aquí, sin pendiente.

  Sumó dos cosas: los casos que fallan por dos validaciones, 100,55 y −0,55, y el junction de Windows. Las diferencias con el prototipo van en D-38. En el camino pidió:
  - En el README de las entidades, decir que la prueba generada de la API no puede armar un bloque válido sin conocer la regla del orden, y que los bordes aceptados del lado de la API ya los prueba `RangosRecomendacionIT`, escrito a mano en el PR #5.
  - Antes del commit del paso 4, buscar la causa de las fallas con carga, corriendo la suite con carga a propósito, y llevarlas a «Problemas encontrados» del PR y a esta bitácora. Aprobó el diagnóstico y los cinco puntos del paso 4: el aviso de éxito junto a los botones, el nombre corregido de la prueba, «Restaurar» en el color de error, las marcas hasta el siguiente intento de guardar y la recomendación que crece con el texto.
  - Probar él mismo el Ctrl+C de la demostración en su recorrido, e incluir la demostración en «Para probarlo» del PR.
  - Anotar en la entrada de Usuarios que hizo el recorrido en su PC y estuvo bien.
  - Después de su recorrido y antes de subir la rama, corregir el apellido de Julián en un commit aparte, ajustar las pruebas que revisaran ese texto (no había ninguna) y sumarlo a «Fichas y documentación» del PR.
- Archivos o PR: la rama `feature/RF-13-pantalla-rangos`; el PR lo abre Cristhian.

## 02/11/2026 · RP-23: la demostración de otra temática con cualquier entidad

- Quién: Cristhian Sánchez (`DrekOF`), con Claude Code.
- Qué se hizo:
  - **En la rama `feature/RP-23-demostracion-cualquier-entidad`, commit por commit, cada uno revisado por Cristhian antes del siguiente:**
    1. **Las pruebas, en rojo** (`e3c6eb2`):
       - las del generador: `cafe.json` pasa la validación y genera su tabla por empresa, su API con las tres pruebas de RNF-03 de `CafeApiGeneradaIT` y su pantalla, donde variedad y proceso empiezan sin elegir;
       - las de la demostración: sus opciones, el JSON de la entidad, lo que saca de él con café y con producto, con quién entrar según el rol y el alcance con cualquier entidad;
       - la de `CampoDeLista`: una lista que empieza sin valor muestra «Elige una opción».
    2. **`cafe.json`, la demostración con `--entidad` y «Elige una opción»** (`a83e63d`):
       - `generador/pruebas/ejemplos/cafe.json`: un café especial del Huila, por empresa, para el administrador de empresa. Tiene nombre (hasta 120), variedad (Castillo, Caturra o Geisha), región (hasta 80), proceso (Lavado, Natural o Honey) y estado (Activo o Inactivo), con sus textos en español y en inglés.
       - `npm run demo:otra-tematica -- --entidad <ruta del JSON>`. Una ruta relativa se lee desde la carpeta donde se corrió el comando; sin `--entidad`, agrega producto, como antes. Los textos de la consola y las pruebas de `--con-pruebas` salen del JSON: su nombre en el menú, su ruta, sus pruebas y el rol con que se entra.
       - La demostración se detiene y dice qué pasó en estos casos:
         - una opción que no entiende, como `--con-prueba`;
         - `--entidad` sin ruta;
         - un JSON que no existe o que no es un JSON;
         - un JSON que se llama como una entidad de InspectIA, que la reemplazaría en la copia.
       - **Comportamiento no pedido:** también se detiene si npm se quedó con las opciones porque faltó `--` antes de ellas. npm no se las pasa al script, solo avisa, y la demostración seguía sin ellas. Sin `--con-pruebas`, por ejemplo, encendía sin correr las pruebas. Es justo el error que se puede cometer en la sustentación. Cristhian lo aprobó.
       - `CampoDeLista`: una lista que empieza sin valor muestra «Elige una opción» hasta que se elige una.
         - Antes, el navegador mostraba la primera opción como elegida mientras el valor seguía vacío, y al guardar salía «Este dato es obligatorio.».
         - No se notaba porque las listas de la app siempre empiezan con un valor: el rol, con `inicial`, y los estados, con `por_defecto`.
         - Variedad y proceso no tienen ninguno de los dos, así que lo habrían mostrado en la sustentación.
    3. **La guía y la documentación:**
       - `docs/demostracion-otra-tematica.md`:
         - qué preparar antes de la sustentación;
         - el paso a paso con café y dónde se ve la separación por empresa;
         - qué produce cada campo del JSON y lo que el JSON no decide: los números y el ícono;
         - un experimento con un campo nuevo;
         - qué hacer si algo falla.
       - La consola pone el rol entre paréntesis, así no choca con el género de la persona: «Entra como Ana Pérez (administrador de empresa de Cacaos del Huila)».
       - Los README del frontend, del generador, de las entidades y el principal, y los Comandos de `CLAUDE.md`. En el del frontend se corrigió que la copia «compila solo lo cambiado»: el backend se compila entero en cada corrida, porque el espejo borra lo que se generó en la anterior y, con eso, su `target`.
  - **Problemas encontrados:**
    - **Stryker no arrancaba con las pruebas nuevas de la demostración.** Corre las pruebas en una copia del frontend (`.stryker-tmp`), donde la carpeta de arriba no tiene `generador/` ni `specs/`, y la prueba que lee `cafe.json` fallaba antes de empezar. La raíz del repositorio ahora se busca subiendo, con `raizDelRepositorio`, como en el plugin de los textos del backend.
    - **ESLint pide conservar el error original al relanzarlo** (`preserve-caught-error`): el error de un JSON roto lleva su `cause`, y la prueba lo revisa.
    - **Docker Desktop no abrió al primer intento** el 02/11. Mostró «An unexpected error occurred», porque no pudo apartar el socket de su motor de secretos, que quedó de la sesión anterior; después arrancó. La guía dice qué hacer si pasa en la sustentación: no usar «Reset to factory defaults», que borra todos los contenedores y volúmenes.
  - **Mutantes que quedan vivos:** ninguno. `otra-tematica.js` queda en 100 %: 157 muertos y 2 por tiempo. No cuenta la parte que enciende la copia, que se prueba corriéndola.
  - **Resultado:**
    - Generador: 121 pruebas en verde, y lo generado, al día (63 archivos).
    - Vitest: 576 pruebas en verde, y ESLint, limpio.
    - Playwright, en el entorno aislado y con el código final del PR: 236 en verde y 4 omitidas, las mismas de antes (iniciar sesión solo con el teclado y la de 1250 px, en el celular). Firefox se saltó en este equipo.
    - Selenium: 24 en verde.
    - La demostración:
      - **Con café y `--con-pruebas`:** las pruebas generadas pasan, 27 de Vitest y 40 de `CafeApiGeneradaIT`, y queda encendida en 160 s. En el recorrido:
        - Ana Pérez ve «Cafés especiales» al final de su menú;
        - variedad y proceso empiezan en «Elige una opción», y registrar sin llenar marca 4 campos;
        - se registra un café, se edita y pasa a Inactivo;
        - en inglés sale «Specialty coffees», y Swagger tiene `/api/v1/cafes`;
        - en la base, el `empresa_id` del café es el de Cacaos del Huila.
      - **Con producto, sin opciones:** como antes, encendida en 31 s.
      - **El experimento de la guía,** con una copia de `cafe.json` fuera del repositorio y notas de cata en `varias_lineas`: el campo aparece en el formulario, en varias líneas, y en la base, donde las notas se guardan con su salto de línea.
      - **Ocho errores en la consola,** cada uno con su mensaje. El de un JSON al que le falta una coma dice en qué línea está.
    - Casos de prueba: 724 pruebas; de los 143 criterios de las fichas, 55 tienen prueba.
- Por qué: para la sustentación, la demostración de RP-23 tenía que servir con cualquier entidad, no solo con producto, y tener un paso a paso.
- Cómo se pidió: Cristhian pidió primero el plan, sin código, y lo aprobó con cuatro decisiones:
  1. Café por empresa, entrando como Ana Pérez. En el entorno hay una sola empresa activa, así que la guía dice dónde se ve la separación: en `\d cafe`, con la columna `empresa_id`, y en las tres pruebas de RNF-03 de `CafeApiGeneradaIT`.
  2. «Elige una opción» en `CampoDeLista`.
  3. 120 para el nombre y 80 para la región.
  4. La guía en `docs/demostracion-otra-tematica.md`.

  Sumó dos cosas al plan:
  - que la demostración se detenga con una opción que no conoce, en vez de ignorarla;
  - en la guía, una tabla de qué produce cada campo del JSON y lo que el JSON no decide.

  Al revisar el commit 2 pidió dos más: el rol entre paréntesis en la consola y, en la guía, el experimento con un campo nuevo.
- Archivos o PR: la rama `feature/RP-23-demostracion-cualquier-entidad`; el PR lo abre Cristhian.

## 09/11/2026 · RP-18 y RP-19: la integración continua con GitHub Actions

- Quién: Cristhian Sánchez (`DrekOF`), con Claude Code.
- Qué se hizo:
  - **En la rama `feature/RP-18-integracion-continua`, commit por commit, cada uno revisado por Cristhian antes del siguiente:**
    1. **El flujo** (`ddcd904`): `.github/workflows/ci.yml`, en la carpeta donde GitHub busca los flujos.
       - Corre en cada PR hacia `main`, en cada push a `main` y a mano. Un push nuevo a la rama de un PR cancela la corrida anterior; en `main` no se cancela ninguna.
       - **Generador:** sus pruebas, `generar.py comprobar` y `casos_de_prueba.py comprobar`.
       - **Backend:** `./mvnw verify`, con Testcontainers.
       - **Frontend:** ESLint, Vitest y el build.
       - **De punta a punta:** si los tres pasan, enciende un solo entorno aislado y corre Playwright, con Firefox, y Selenium.
       - **Aviso de fallas, solo en `main`:**
         - abre un issue por cada job que falla, con la etiqueta `ci`, asignado a quien hizo el push;
         - si ya hay uno abierto por esa falla, comenta ahí;
         - cuando el job vuelve a pasar, lo cierra.

         Lo que viene del push entra por variables de entorno, nunca dentro del script.
       - Versiones del proyecto, con caché de pip, Maven y npm; permisos de solo lectura, salvo el aviso; tiempo máximo en cada job; reportes como artefactos por 7 días si algo falla.
    2. **Selenium sin Edge en la CI** (`affeade`): con `INSPECTIA_E2E_SIN_EDGE=1` salta Edge, como Playwright salta Firefox con `INSPECTIA_E2E_SIN_FIREFOX`, y el flujo la usa (ver «Problemas encontrados»).
    3. **La documentación:**
       - en el README, la sección «Integración continua», con los pasos en GitHub, la fila de versiones de las acciones, `.github/workflows/` en la estructura y la nota de Edge;
       - la variable `INSPECTIA_E2E_SIN_EDGE` en el README del frontend;
       - el README de infra;
       - la línea de `CLAUDE.md`, que decía que GitHub Actions va en `infra/`;
       - esta bitácora.
  - **La prueba del aviso,** en `prueba/aviso-ci`, una rama de usar y botar con el mismo script y Generador fallando a propósito. Costó unos 13 minutos:
    1. El primer push abrió el issue #12, «La CI falla en prueba/aviso-ci: Generador», asignado a DrekOF y con la etiqueta `ci`.
    2. El segundo, con Generador fallando todavía, comentó en el mismo issue.
    3. El tercero, con Generador pasando, lo cerró.

    Después se borró la rama.
  - **La medida, en las dos corridas del PR #13.** GitHub cobra cada job redondeado hacia arriba al minuto entero:

    | Job | Primera corrida | Segunda corrida |
    |---|---|---|
    | Generador | 20 s (1 min) | 29 s (1 min) |
    | Backend | 1 min 57 s (2 min) | 2 min 12 s (3 min) |
    | Frontend | 2 min 58 s (3 min) | 2 min 12 s (3 min) |
    | De punta a punta | 10 min 31 s (11 min), con Edge sin abrir | 13 min 10 s (14 min) |
    | Total cobrado | 17 min | 21 min |

    - Lo que más varía es Playwright: 7,8 min en una corrida y 10,3 en la otra, con 1 worker.
    - En `main` se suma 1 minuto, por el aviso.
    - Con 21 minutos, caben unas 95 corridas completas al mes en los 2.000 minutos del plan gratis.
    - Eso está por debajo de la regla de los 30 minutos, así que las de punta a punta quedaron en cada PR.
  - **Problemas encontrados:**
    - **Edge no abre en la CI.** El error fue «SessionNotCreatedError: session not created: Chrome instance exited».
      - En Ubuntu 24.04, AppArmor no deja que un navegador sin perfil propio cree los espacios de nombres de su sandbox, y Edge se cierra al abrir. Chrome sí abre, y Playwright no lo sufre, porque abre sus navegadores sin sandbox.
      - La primera opción fue quitar esa restricción en el runner, como hace el CI de Playwright. El control de seguridad del modo automático de Claude Code la bloqueó, porque debilita una protección del sistema, y no se intentó por otro camino.
      - Cristhian escogió no correr Edge en la CI. Edge se sigue probando en Windows, en el PC de cada integrante.
    - **Exigir que la CI pase antes de fusionar no se puede en un repositorio privado del plan gratis:** las reglas de rama y los rulesets piden GitHub Pro. Cristhian lo pidió con GitHub Education; mientras tanto, nadie fusiona un PR en rojo. Los pasos para configurarlo quedaron en el README.
    - **`gh` no estaba instalado.** Claude Code lo instaló con winget, a pedido de Cristhian, y él hizo `gh auth login`. Se usa solo para leer corridas y registros; lo que escribe en GitHub necesita su permiso.
    - **La API de tiempos cobrados devuelve 0,** porque GitHub la está retirando. Los minutos se calcularon con la duración de cada job, redondeada como cobra GitHub. La página de cada corrida los muestra en «Usage».
  - **Resultado:**
    - En las dos corridas del PR, Generador, Backend y Frontend pasaron. El Backend corrió sus 119 pruebas unitarias y 401 de integración, y el Frontend, los 38 archivos de Vitest.
    - Playwright, en Linux y con Firefox incluido: 296 en verde y 4 omitidas, las de siempre (iniciar sesión solo con el teclado y la de 1250 px, en el celular).
    - Selenium, con Chrome, en PC y en celular: 12 en verde.
    - Casos de prueba: 724 pruebas; de los 143 criterios de las fichas, 55 tienen prueba. Este PR no agrega pruebas.
- Por qué: la fase 6 de la pauta pide que la CI «detecte los cambios en el repositorio y compile y pruebe; […] al probar si hay errores cree los issues, tareas y notifique a los integrantes» (RP-18 y RP-19). El despliegue a AWS va en otro PR.
- Cómo se pidió:
  - **Lo que pidió:** Cristhian pidió primero el plan, sin código:
    - la CI en cada PR y en cada push a `main`;
    - el generador, el backend, el frontend y las de punta a punta en Linux, con Firefox;
    - Stryker fuera de la CI;
    - las versiones del proyecto, con cachés;
    - un issue por cada falla en `main`, asignado a quien hizo el push, o un comentario si ya hay uno;
    - la medida de los minutos;
    - los pasos en GitHub.

    También pidió que le explicara de forma sencilla qué es la CI y cuándo corre.
  - **Las cinco decisiones con que aprobó el plan:**
    1. GitHub Pro con GitHub Education; mientras lo aprueban, nadie fusiona un PR en rojo.
    2. Las de punta a punta solo en `main` y a mano, con una regla: si una corrida completa cobra 30 minutos o menos, correrlas también en cada PR.
    3. Que el aviso cierre el issue cuando el job vuelva a pasar en `main`.
    4. `gh` solo para leer; para escribir en GitHub, pedirle permiso.
    5. Permiso para subir la rama y para la prueba del aviso en `prueba/aviso-ci`.
  - **Lo que decidió después:** con la medida, dejar las de punta a punta en cada PR. Para Edge, la opción de no correrlo en la CI, con `INSPECTIA_E2E_SIN_EDGE`, anotada en el README y en esta bitácora.
- Archivos o PR: la rama `feature/RP-18-integracion-continua` y el PR #13, que Cristhian abrió como borrador.

## Plantilla para nuevas entradas

### dd/mm/aaaa · título corto

- Quién:
- Qué se hizo:
- Por qué:
- Cómo se pidió (prompt o instrucción):
- Archivos o PR:
