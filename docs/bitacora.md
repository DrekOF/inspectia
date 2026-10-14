<!--
Nombre: bitacora.md
Descripción: Registro paso a paso de cómo se construyó InspectIA, para la sustentación (RP-24).
Fecha de creación: 12/10/2026
Actualización: 14/10/2026
Autor: Equipo InspectIA
Versión: 1.8
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
       - El recorrido se probó con el backend real en una base aparte: crear la clave, arrancar, crear el administrador con el script y entrar desde Swagger UI.
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
  - **Resultado:**
    - Backend: 201 pruebas (50 unitarias y 151 de integración), todas en verde.
    - Generador: 40 pruebas, todas en verde.
    - Criterios: RF-01 tiene sus 8 criterios con prueba y RNF-04 sus 5. De los 136 de las fichas, 26 tienen prueba.
- Por qué: todas las demás funciones dependen del inicio de sesión (RF-01), y RNF-04 pide sesión y permisos por rol en toda la API. La revisión en cada petición hace que desactivar un usuario o una empresa (RF-03, D-27) surta efecto de inmediato y no 8 horas después.
- Cómo se pidió: Cristhian pidió el plan del inicio de sesión con JWT (D-21) antes de escribir código. El plan debía cubrir el token, la API, la revisión en cada petición, el bloqueo sin revelar si un correo existe, las pruebas y cómo probarlo en su PC. Aprobó las cinco decisiones y pidió registrarlas como D-28 y P-17. En el camino pidió:
  - Dejar la regla de Lombok y subir la versión del generador.
  - Agregar C7 y C8 a RF-01.
  - Que el inicio de sesión ignore el token viejo, con su prueba marcada como RF-01 C6.
  - Anotar en P-17 el riesgo aceptado.
- Archivos o PR: la rama `feature/RF-01-inicio-sesion`; el PR lo abre Cristhian.

## Plantilla para nuevas entradas

### dd/mm/aaaa · título corto

- Quién:
- Qué se hizo:
- Por qué:
- Cómo se pidió (prompt o instrucción):
- Archivos o PR:
