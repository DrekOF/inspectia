<!--
Nombre: README.md (generador)
Descripción: Qué hace el generador de InspectIA (RP-23), cómo se usa y cómo se prueba.
Fecha de creación: 12/10/2026
Actualización: 24/11/2026
Autor: Equipo InspectIA
Versión: 1.14
-->

# generador

Scripts de Python 3.13 que leen las entidades de `specs/entidades/*.json` (formato en su README) y generan las tablas y los CRUD, con sus pantallas (RP-23). El código generado **no se edita a mano**: para cambiar un CRUD se cambia el JSON o la plantilla y se vuelve a generar. La meta es poder cambiar de temática (otro producto en lugar de cacao) cambiando solo los JSON.

## Qué genera hoy

| De | Archivos | Qué es |
|---|---|---|
| Todas las entidades | `backend/src/generated/resources/db/migration/V1__esquema_inicial.sql` | Las 9 tablas, con restricciones, índices, triggers y comentarios. Flyway la aplica al arrancar el backend (RP-15). Su header lleva la versión de la plantilla `sql/esquema.sql.j2`, no la del generador (D-49) |
| Todas las entidades | `backend/src/generated/resources/i18n/generado.properties` y `generado_en.properties` | Etiquetas, valores de las listas y mensajes en español y en inglés (RP-11) |
| Las entidades con CRUD | `backend/src/generated/java/.../<modulo>/` | Entidad JPA, con `@DynamicUpdate` (D-36), repositorio, un `enum` por lista y los valores iniciales. Un atributo con `solo_lo_cambian_las_reglas` sale en la entidad sin que JPA lo escriba |
| Las entidades de `alcance.json` | `backend/src/generated/java/.../<modulo>/` | API REST: servicio, controlador, reglas de acceso por ruta, solicitudes, respuesta, mapeador y la interfaz de las reglas |
| Las entidades de `alcance.json` | `backend/src/test-generated/java/.../<modulo>/<Clase>ApiGeneradaIT.java` | Prueba de integración: un caso por cada permiso y cada validación del JSON, más los datos únicos y el filtro `activo` del listado (D-33). En una edición en bloque, también las filas completas, los datos de solo lectura y los valores por defecto |
| Las entidades de `pantallas` | `frontend/src/generated/<modulo>/` | La pantalla (`<Clase>Pantalla.jsx`): una lista con su formulario o, con `edicion_en_bloque`, todas las filas en un solo formulario. También el cliente de su API (`<entidad>Api.js`) y sus validaciones de Yup (`<entidad>Esquemas.js`), cada uno con su prueba de Vitest |
| Las entidades de `pantallas` | `frontend/e2e/generated/<entidad>.spec.js` | Prueba de Playwright: qué roles abren la pantalla, sus fallas de accesibilidad (axe, en los dos temas) y que no se desplace a los lados desde 360 px |
| Siempre | `frontend/src/generated/pantallas.js` y `textos.js` | Las pantallas que leen el menú y las rutas, con carga diferida, y los textos de `pantalla.textos`, en español y en inglés |

`alcance.json` dice qué entidades con CRUD tienen API (`api`) y cuáles tienen pantalla (`pantallas`). La API la tienen las tres: `empresa` (RF-02), `usuario` (RF-03, una API por empresa) y `rango_recomendacion` (RF-13, una API de edición en bloque). La pantalla también la tienen las tres; la de rangos (RF-13) es una pantalla en bloque. Cómo responden la API y la pantalla generadas, y lo que no soportan todavía, está en `specs/entidades/README.md`.

Las carpetas generadas son solo del generador: `generar` borra lo que ya no se genera. Lo escrito a mano, como las reglas de cada módulo (`<modulo>/reglas/`) y el módulo `comun`, va fuera de ellas. En el frontend, lo que el JSON no puede declarar de una pantalla, como el panel de administradores de una empresa, va en `frontend/src/extensiones.js`: lo generado no importa código escrito a mano.

## Validaciones y casos de límite

Las validaciones de cada campo salen de un solo modelo, `inspectia_generador/campos.py`. De él salen las anotaciones de Bean Validation, las validaciones de Yup y los casos de límite de las pruebas generadas de la API y de Yup, con el mismo mensaje en español; los casos están en `specs/entidades/README.md`. Cada validación lleva su restricción de Bean Validation, y Yup revisa en el orden en que la API escoge el error de un campo (`orden_de_la_api`, como `ManejadorDeErrores`): así un valor que falla por dos validaciones da el mismo mensaje en el formulario y en la API. Una prueba del generador lee `ManejadorDeErrores` y falla si esa regla cambia en el backend. La prueba de Yup revisa además cada valor de una lista que existe pero el formulario no ofrece (`valores_en_formulario`), como ADMIN_PLATAFORMA en el rol, con el mensaje del 403 de la API; la prueba de la API lo revisa en la de permisos.

Un `decimal` del formulario se lee con coma o con punto (`numeroDecimal`), y lo que no es un número da «Escribe un número, como 90,5.» (`validacion.numero`). Sus decimales se cuentan como en `@MaximoDecimales` (`maximoDecimales`). Con `min` o `max`, los casos de límite incluyen el número que falla por el límite y por los decimales a la vez, como 100,55 y −0,55, para que Yup escoja el mismo error que la API. En una edición en bloque, la prueba generada de la API no revisa los valores del borde que se aceptan: no puede armar un bloque válido sin conocer las reglas escritas a mano, como el orden de los límites (ver `specs/entidades/README.md`).

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

## Demostración de otra temática

`npm run demo:otra-tematica`, desde `frontend/` y con Docker Desktop abierto, muestra que cambiando solo los JSON sale otro CRUD completo (RP-23). Hay dos ejemplos en `pruebas/ejemplos/`:

- `producto.json`, el de la pauta del docente, que usa si no se pide otro;
- `cafe.json`, un café especial del Huila, por empresa, con `-- --entidad ../generador/pruebas/ejemplos/cafe.json`.

1. En una copia del proyecto, en `frontend/.demostracion` (Git la ignora), agrega el JSON a las entidades de InspectIA y suma la entidad a la API y a las pantallas del alcance.
2. Corre `generar.py generar` en la copia, con `--entidades`, `--alcance` y `--raiz`, y lista por capa lo que escribió: la tabla en V1, la API con su prueba de integración, los textos y la pantalla con sus pruebas. Si al JSON le falta algo, el generador se detiene ahí y dice qué.
3. Enciende la copia como el entorno aislado de las pruebas, con su propia base, y escribe con quién entrar, cómo ver la API en Swagger UI y la tabla en psql. Ctrl+C la apaga y borra su base.

Con `-- --con-pruebas`, antes de encenderla corre en la copia las pruebas generadas de la entidad. El repositorio no cambia. Los detalles están en `frontend/README.md`, y el paso a paso para la sustentación, en `docs/demostracion-otra-tematica.md`. `pruebas/test_otra_tematica.py` revisa lo mismo con los dos ejemplos, sin encender nada.

## Casos de prueba (RP-13)

`casos_de_prueba.py` arma `docs/casos-de-prueba.md` desde `specs/fichas.json` y las marcas de las pruebas (`@DisplayName` en Java y `@pytest.mark.caso` en Python: las del generador, en `generador/pruebas`, y las del servicio de IA, en `ia-servicio/pruebas`). Muestra cada criterio de aceptación con sus pruebas y, como pendientes, los que todavía no tienen ninguna. El documento no se escribe a mano:

```bash
generador/.venv/Scripts/python generador/casos_de_prueba.py generar     # escribe el documento
generador/.venv/Scripts/python generador/casos_de_prueba.py comprobar   # código 1 si no está al día
```

Los dos terminan con código 1 si una prueba no tiene marca, si la marca no tiene la forma «<código> · C<n> · …» o «<código> · …», o si cita una ficha, un criterio o un código que no existe. Con `--sin-pendientes` también fallan si queda algún criterio sin prueba, para revisar RP-13 al cerrar el proyecto. Una prueba del generador revisa que el documento esté al día: si agregas o cambias pruebas, vuelve a armarlo.

Lo que no se puede probar con código, como una prueba con personas o lo que solo puede confirmar quien entrenó el modelo, va en `docs/verificaciones.md` como una **verificación documentada**: una sección cuyo título es la marca del criterio (`## <código> · C<n> · …`) y, debajo, su estado: `**Estado:** Verificado.` o `**Estado:** Pendiente: <qué falta>.` En el documento sale junto a las pruebas del criterio. Una verificada cubre el criterio; una pendiente lo deja pendiente aunque tenga pruebas, así nunca esconde un criterio sin evidencia. Una verificación sin estado, repetida o que cita un criterio que no existe es un error, como una marca mala.

Una ficha solo pasa a «Implementado» cuando funciona de punta a punta, todos sus criterios tienen prueba y está en `main` (D-29). Si una ficha en «Implementado» tiene criterios sin prueba, o con una verificación documentada pendiente, `comprobar` termina con código 1 y `generar` solo lo avisa.

## Estructura

```
generador/
├── generar.py               línea de comandos del generador
├── casos_de_prueba.py       línea de comandos de la lista de casos de prueba (RP-13)
├── alcance.json             entidades con API y con pantalla
├── inspectia_generador/     carga, validación, modelo, validaciones de cada campo, SQL, Java, frontend, textos,
│                            headers, escritura y casos
├── plantillas/              plantillas de Jinja2: sql/, java/main/, java/test/, frontend/, i18n/ y docs/
└── pruebas/                 pruebas con pytest y ejemplos/producto.json, que usa también la demostración
```

## Pruebas

```bash
cd generador
.venv/Scripts/python -m pytest
```

Cada prueba lleva `@pytest.mark.caso("…")` con la ficha o el requisito que cubre (por ejemplo, «RP-23 · …»); una prueba sin esa marca hace fallar la ejecución. Así la lista de casos de prueba (RP-13) se arma desde las pruebas.

`pruebas/ejemplos/producto.json` es la entidad producto de la pauta del docente para el back-end (id, nombre, categoria, descripcion, estado). Las pruebas generan su tabla, su API y su pantalla junto con empresa y usuario, para demostrar que el generador sirve para otra temática. No entra a la app.

`pruebas/ejemplos/empresa_generada/` guarda lo que el generador escribe para la API de empresa. Una prueba revisa que un cambio al generador no lo mueva, salvo el header. Si cambias una plantilla a propósito y cambia lo de empresa, revisa la diferencia y copia ahí los archivos nuevos.

Otra prueba revisa que lo generado no lleve valores de Python (`True`, `False` ni `None`), como un `example = "True"` en OpenAPI: un valor de un JSON va como se escribe en Java o en JSON.
