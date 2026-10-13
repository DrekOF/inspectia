<!--
Nombre: README.md (generador)
Descripción: Qué hace el generador de InspectIA (RP-23), cómo se usa y cómo se prueba.
Fecha de creación: 12/10/2026
Actualización: 13/10/2026
Autor: Equipo InspectIA
Versión: 1.1
-->

# generador

Scripts de Python 3.13 que leen las entidades de `specs/entidades/*.json` (formato en su README) y generan las tablas y los CRUD (RP-23). El código generado **no se edita a mano**: para cambiar un CRUD se cambia el JSON o la plantilla y se vuelve a generar. La meta es poder cambiar de temática (otro producto en lugar de cacao) cambiando solo los JSON.

## Qué genera hoy

| Archivo | Qué es |
|---|---|
| `backend/src/generated/resources/db/migration/V1__esquema_inicial.sql` | Las 8 tablas, con restricciones, índices, triggers y comentarios. Flyway la aplica al arrancar el backend (RP-15) |

En las próximas tareas se suman el backend de los CRUD y sus pruebas (`backend/src/generated/java` y `backend/src/test-generated/java`) y, después, las pantallas del frontend.

Las carpetas generadas son solo del generador: `generar` borra lo que ya no se genera. Lo escrito a mano, como las reglas de cada módulo, va fuera de ellas.

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

## Estructura

```
generador/
├── generar.py               línea de comandos
├── inspectia_generador/     carga, validación, modelo, SQL, generación y escritura
├── plantillas/              plantillas de Jinja2 (sql/)
└── pruebas/                 pruebas con pytest y ejemplos/producto.json
```

## Pruebas

```bash
cd generador
.venv/Scripts/python -m pytest
```

Cada prueba lleva `@pytest.mark.caso("…")` con la ficha o el requisito que cubre (por ejemplo, «RP-23 · …»); una prueba sin esa marca hace fallar la ejecución. Así la lista de casos de prueba (RP-13) se arma desde las pruebas.

`pruebas/ejemplos/producto.json` es la entidad producto de la pauta del docente para el back-end (id, nombre, categoria, descripcion, estado). Las pruebas generan sus archivos junto con empresa y usuario, para demostrar que el generador sirve para otra temática. No entra a la app.
