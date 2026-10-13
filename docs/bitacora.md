<!--
Nombre: bitacora.md
Descripción: Registro paso a paso de cómo se construyó InspectIA, para la sustentación (RP-24).
Fecha de creación: 12/10/2026
Actualización: 13/10/2026
Autor: Equipo InspectIA
Versión: 1.6
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

## Plantilla para nuevas entradas

### dd/mm/aaaa · título corto

- Quién:
- Qué se hizo:
- Por qué:
- Cómo se pidió (prompt o instrucción):
- Archivos o PR:
