<!--
Nombre: README.md (generador)
Descripción: Qué hace el generador de InspectIA (RP-23), cómo se usa y cómo se prueba.
Fecha de creación: 12/10/2026
Actualización: 20/10/2026
Autor: Equipo InspectIA
Versión: 1.6
-->

# generador

Scripts de Python 3.13 que leen las entidades de `specs/entidades/*.json` (formato en su README) y generan las tablas y los CRUD (RP-23). El código generado **no se edita a mano**: para cambiar un CRUD se cambia el JSON o la plantilla y se vuelve a generar. La meta es poder cambiar de temática (otro producto en lugar de cacao) cambiando solo los JSON.

## Qué genera hoy

| De | Archivos | Qué es |
|---|---|---|
| Todas las entidades | `backend/src/generated/resources/db/migration/V1__esquema_inicial.sql` | Las 8 tablas, con restricciones, índices, triggers y comentarios. Flyway la aplica al arrancar el backend (RP-15) |
| Todas las entidades | `backend/src/generated/resources/i18n/generado.properties` y `generado_en.properties` | Etiquetas, valores de las listas y mensajes en español y en inglés (RP-11) |
| Las entidades con CRUD | `backend/src/generated/java/.../<modulo>/` | Entidad JPA, repositorio, un `enum` por lista y los valores iniciales |
| Las entidades de `alcance.json` | `backend/src/generated/java/.../<modulo>/` | API REST: servicio, controlador, reglas de acceso por ruta, solicitudes, respuesta, mapeador y la interfaz de las reglas |
| Las entidades de `alcance.json` | `backend/src/test-generated/java/.../<modulo>/<Clase>ApiGeneradaIT.java` | Prueba de integración: un caso por cada permiso y cada validación del JSON, más los datos únicos. En una edición en bloque, también las filas completas, los datos de solo lectura y los valores por defecto |

`alcance.json` dice qué entidades con CRUD tienen API. Hoy son las tres: `empresa` (RF-02), `usuario` (RF-03, una API por empresa) y `rango_recomendacion` (RF-13, una API de edición en bloque). Las pantallas del frontend llegan en otra tarea. Cómo responde la API generada y qué no soporta todavía está en `specs/entidades/README.md`.

Las carpetas generadas son solo del generador: `generar` borra lo que ya no se genera. Lo escrito a mano, como las reglas de cada módulo (`<modulo>/reglas/`) y el módulo `comun`, va fuera de ellas.

## Uso

Primera vez, desde la raíz del repositorio (en Linux o macOS, `generador/.venv/bin/python`):

```bash
py -3.13 -m venv generador/.venv
generador/.venv/Scripts/python -m pip install -r generador/requirements-dev.txt
```

Comandos:

```bash
generador/.venv/Scripts/python generador/generar.py validar     # revisa los JSON contra su formato y specs/
generador/.venv/Scripts/python generador/generar.py generar     # escribe lo generado y borra lo que sobra
generador/.venv/Scripts/python generador/generar.py comprobar   # compara sin escribir: código 1 si hay diferencias
```

`comprobar` encuentra los archivos generados que alguien editó a mano, los que faltan o sobran, y lo que quedó sin regenerar después de cambiar un JSON o una plantilla. Se corre antes de cada commit y, cuando exista, en el CI (RP-18). La salida es determinista: las fechas del header salen de los JSON y los finales de línea son LF.

Si una entidad de `alcance.json` usa algo que la API todavía no soporta, `generar` se detiene y dice qué falta.

## Casos de prueba (RP-13)

`casos_de_prueba.py` arma `docs/casos-de-prueba.md` desde `specs/fichas.json` y las marcas de las pruebas (`@DisplayName` en Java y `@pytest.mark.caso` en Python). Muestra cada criterio de aceptación con sus pruebas y, como pendientes, los que todavía no tienen ninguna. El documento no se escribe a mano:

```bash
generador/.venv/Scripts/python generador/casos_de_prueba.py generar     # escribe el documento
generador/.venv/Scripts/python generador/casos_de_prueba.py comprobar   # código 1 si no está al día
```

Los dos terminan con código 1 si una prueba no tiene marca, si la marca no tiene la forma «<código> · C<n> · …» o «<código> · …», o si cita una ficha, un criterio o un código que no existe. Con `--sin-pendientes` también fallan si queda algún criterio sin prueba, para revisar RP-13 al cerrar el proyecto. Una prueba del generador revisa que el documento esté al día: si agregas o cambias pruebas, vuelve a armarlo.

Una ficha solo pasa a «Implementado» cuando funciona de punta a punta, todos sus criterios tienen prueba y está en `main` (D-29). Si una ficha en «Implementado» tiene criterios sin prueba, `comprobar` termina con código 1 y `generar` solo lo avisa.

## Estructura

```
generador/
├── generar.py               línea de comandos del generador
├── casos_de_prueba.py       línea de comandos de la lista de casos de prueba (RP-13)
├── alcance.json             entidades con API
├── inspectia_generador/     carga, validación, modelo, SQL, Java, textos, headers, escritura y casos
├── plantillas/              plantillas de Jinja2: sql/, java/main/, java/test/, i18n/ y docs/
└── pruebas/                 pruebas con pytest y ejemplos/producto.json
```

## Pruebas

```bash
cd generador
.venv/Scripts/python -m pytest
```

Cada prueba lleva `@pytest.mark.caso("…")` con la ficha o el requisito que cubre (por ejemplo, «RP-23 · …»); una prueba sin esa marca hace fallar la ejecución. Así la lista de casos de prueba (RP-13) se arma desde las pruebas.

`pruebas/ejemplos/producto.json` es la entidad producto de la pauta del docente para el back-end (id, nombre, categoria, descripcion, estado). Las pruebas generan su tabla y su API junto con empresa y usuario, para demostrar que el generador sirve para otra temática. No entra a la app.

`pruebas/ejemplos/empresa_generada/` guarda lo que el generador escribe para la API de empresa. Una prueba revisa que un cambio al generador no lo mueva, salvo el header. Si cambias una plantilla a propósito y cambia lo de empresa, revisa la diferencia y copia ahí los archivos nuevos.

Otra prueba revisa que lo generado no lleve valores de Python (`True`, `False` ni `None`), como un `example = "True"` en OpenAPI: un valor de un JSON va como se escribe en Java o en JSON.
