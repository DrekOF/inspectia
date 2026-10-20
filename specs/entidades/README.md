<!--
Nombre: README.md (specs/entidades)
Descripción: Qué hay en specs/entidades/ y formato de los JSON de entidades que lee el generador (RP-23).
Fecha de creación: 12/10/2026
Actualización: 20/10/2026
Autor: Equipo InspectIA
Versión: 1.9
-->

# specs/entidades

Un archivo JSON por cada tabla de la base de datos. Es la entrada de `generador/` (RP-23), y las tablas solo salen de aquí (RP-15). `specs/modelo-datos.md` explica el modelo y su trazabilidad; si cambias un JSON, actualiza también ese documento en el mismo PR.

| Archivo | Tabla | CRUD generado | Quién lo usa |
|---|---|---|---|
| `empresa.json` | empresa | Sí | Administrador de la plataforma (RF-02, D-27) |
| `usuario.json` | usuario | Sí | Administrador de empresa (RF-03) |
| `rango_recomendacion.json` | rango_recomendacion | Sí: listar, editar en bloque y restaurar | Administrador de empresa (RF-13) |
| `lote.json` | lote | No | Módulo `analisis` (D-21) |
| `muestra.json` | muestra | No | Módulo `analisis` |
| `foto.json` | foto | No | Módulo `analisis` |
| `grano.json` | grano | No | Módulo `analisis` |
| `modelo.json` | modelo | No | Módulo `analisis` y estadísticas (RF-17) |

## Qué genera el generador

- **De todos los JSON:** el script SQL de creación de las tablas, con sus restricciones, índices y triggers (RP-15), y los textos en español e inglés (`i18n/generado.properties` y `generado_en.properties`): etiquetas de entidades, atributos y valores, y mensajes (RP-11).
- **De los que tienen `"crud": true`:** la persistencia en el backend: entidad JPA, repositorio (con las consultas de los datos únicos), un `enum` por cada lista y, si hay `valores_iniciales`, la clase que los arma.
- **De los que están en `generador/alcance.json`, además:** la API en `/api/v1/<ruta>`: servicio, controlador con OpenAPI, reglas de acceso por ruta, solicitudes con sus validaciones, respuesta, la interfaz de las `reglas` y una prueba de integración generada. Hoy todas las entidades con CRUD tienen API; una nueva la recibe cuando llega su ficha. Las de `edicion_en_bloque` tienen otra API (ver «Edición en bloque»). Las pantallas del frontend llegan en otra tarea.
- El código generado no se edita a mano (RP-23). Las `reglas` se programan a mano, fuera de las carpetas generadas, y el código generado las llama.

### Cómo responde la API generada

- **Roles:** los de `operaciones`. La cadena de seguridad los revisa por ruta antes de leer la petición, así que un rol sin permiso recibe 403 aunque envíe datos inválidos; `@PreAuthorize` es la segunda barrera (RNF-04).
- **Por empresa:** la empresa sale de la sesión y nunca de la petición. Cada consulta se limita a ella, y un registro de otra empresa responde 404, como si no existiera (RNF-03). Por eso ninguna operación de una entidad por empresa puede ser del administrador de la plataforma, que no tiene empresa.
- **Orden de las revisiones:** primero las validaciones del JSON y las `reglas` (400); después los datos únicos (409). Un valor de `valores_en_formulario` que no se puede asignar responde 403 antes que todo lo demás. Lo que se guarda en una operación, incluido lo de `crear_con` y `valores_iniciales`, va en una sola transacción.
- **Listado:** una página con `contenido`, `pagina` (desde 0), `tamano`, `totalElementos` y `totalPaginas`. Trae 20 filas por página si no se pide otro tamaño (máximo 100) y se ordena por `nombre`, sin distinguir mayúsculas, si la entidad tiene ese atributo; si no, por antigüedad. Se pide otra página con `?pagina=` y otro tamaño con `?tamano=`.
- **Respuesta:** los atributos de la lista o del formulario; los demás, como los intentos fallidos de un usuario, son internos. Una contraseña nunca sale: se guarda su hash BCrypt. Al editar, la contraseña es opcional si su `entrada` lo dice; si llega con solo espacios, se rechaza con el mismo mensaje que al crear, «Este dato es obligatorio.».
- **Decimales:** un `decimal` acepta como máximo los decimales de su `escala`, contados sin los ceros del final: con escala 1, 90.50 sirve, porque vale 90.5, y 90.55 no. Se guarda y se responde con la escala de su columna, así que 90.50 sale como 90.5 y 100 como 100.0, igual al guardar que al listar.
- **Reglas:** las de `validacion` se programan a mano y, en una entidad por empresa, reciben el usuario de la sesión. Sus `parametros` salen como constantes de la interfaz generada. Las de tipo `efecto` también se programan a mano: el código generado las llama en las operaciones de `cuando`, después de los cambios y antes de escribirlos en la base; si su `atributo` es una contraseña, solo cuando llega una nueva. Las de tipo `accion` son operaciones extra que se escriben a mano, fuera de lo generado.
- **Errores:** `application/problem+json` (RFC 9457). Los 400 y 409 traen `errores`, con un `campo` y un `mensaje` por cada dato que corregir, uno por campo. Dentro de una lista, el campo lleva su posición, como `filas[2].categoria`. Los mensajes salen en el idioma de `Accept-Language`; si la petición no pide ninguno, en español.

### Edición en bloque

Con `edicion_en_bloque: true`, cada empresa tiene una fila por cada valor de una lista, como un rango por cada categoría (RF-13), y las filas se editan todas juntas. No se crean ni se desactivan filas sueltas: se crean con la empresa, desde `valores_iniciales` (RF-02). La lista que identifica cada fila es la que forma un `unicos` con `empresa_id`.

- **Rutas:**
  - `GET /api/v1/<ruta>` lista las filas de la empresa de la sesión, sin páginas y en el orden de la lista.
  - `PUT /api/v1/<ruta>` recibe `{ "filas": [...] }`: una fila por cada valor de la lista, con ese valor y los atributos del formulario.
  - `POST /api/v1/<ruta>/restaurar-por-defecto` vuelve a los valores de `valores_iniciales`, los mismos que recibe cada empresa nueva.
  - Las tres responden las filas, en el orden de la lista.
- **Orden de las revisiones al editar.** Si algo falla, responde 400 y no guarda ninguna fila:
  1. Las validaciones del JSON en cada fila, con el error en `filas[i].campo`.
  2. Que llegue una fila por cada valor de la lista, sin repetir, con el error en `filas`. El mensaje sale de la etiqueta y los valores de la lista, como «Envía una fila por cada categoría, sin repetir: Premium o Especial, Estándar, Corriente y Fuera de norma.».
  3. Que no cambie un atributo con `solo_lectura_si` en las filas que cumplen la condición, con el error en `filas[i].campo`.
  4. Las `reglas` de `validacion`, que reciben todas las filas juntas, en el orden de la lista. Su error no lleva campo.
- **Dos cambios a la vez:** editar y restaurar bloquean las filas de la empresa hasta terminar (`SELECT … FOR UPDATE`). Si dos personas las cambian a la vez, la segunda espera a que la primera guarde y trabaja sobre lo que quedó, así no se mezclan sus filas: con los rangos, los límites podrían quedar fuera de orden.
- **Una fila que falte** (solo pasaría si alguien la borra en la base): editar y restaurar la crean con su valor por defecto, así la empresa siempre tiene una por cada valor de la lista.
- **Lo que necesita, o el generador se detiene:**
  - `operaciones` con `editar` y, si hace falta, `listar` y `restaurar_por_defecto`.
  - `por_empresa: "si"`.
  - La lista que identifica cada fila, con sus `valores` y fuera del formulario.
  - `valores_iniciales` con una fila por cada valor de esa lista.
  - `solo_lectura_si` solo en números del formulario y con la condición sobre esa lista.
  - Reglas de `validacion` sin `atributo` y solo al editar; ninguna de tipo `efecto`.

### Lo que la API generada todavía no soporta

Si una entidad del alcance usa algo de esta lista, el generador se detiene y dice qué falta: `no_posterior_a_hoy`, atributos `referencia` en el formulario, un `efecto` en `listar` o `ver` o atado a un atributo que no es una contraseña, y `restaurar_por_defecto` fuera de una edición en bloque. Se agregan a las plantillas cuando una ficha los necesite. `solo_lectura_si` en una entidad sin `edicion_en_bloque` ya lo rechaza la validación de los JSON.

## Columnas implícitas

No se escriben en los JSON. El generador las agrega a todas las tablas, y se pueden usar por su nombre en `unicos`, `indices` y `checks`:

| Columna | Definición |
|---|---|
| `id` | `uuid` v7, llave primaria. Lo genera Hibernate al guardar; la base tiene `DEFAULT uuidv7()` para los scripts |
| `fecha_creacion` | `timestamptz NOT NULL DEFAULT now()` |
| `fecha_actualizacion` | `timestamptz NOT NULL DEFAULT now()`; un trigger la actualiza en cada cambio |
| `empresa_id` | Solo si `por_empresa` es `si` (obligatoria) u `opcional` (puede ir vacía). FK a `empresa` (RNF-03) |

## Claves de la entidad

| Clave | Obligatoria | Qué es |
|---|---|---|
| `_header` | Sí | Header del artefacto (RP-10): nombre, descripción, fecha de creación, actualización, autor y versión |
| `entidad` | Sí | Nombre de la tabla: snake_case, singular, sin tildes. Igual al nombre del archivo |
| `clase` | Sí | Nombre de la clase Java y del componente React (PascalCase) |
| `modulo` | Con CRUD | Módulo del backend donde va el código generado (D-21), por ejemplo `rango` para `rango_recomendacion`. En minúsculas y sin tildes |
| `ruta` | Con CRUD | Segmento de la API: `/api/v1/<ruta>`, por ejemplo `empresas`. En minúsculas, con guiones y sin tildes. Va solo en entidades con CRUD |
| `descripcion` | Sí | Para qué sirve. Va al JavaDoc, al JSDoc y a OpenAPI |
| `etiquetas` | Sí | `es` y `en`, cada uno con `singular` y `plural` |
| `fichas` | Sí | RF, RNF, D, P o RP que piden la entidad. La primera es la ficha de su CRUD: la citan los headers y las marcas de las pruebas generadas |
| `crud` | Sí | `true`: se generan backend y pantallas. `false`: solo la tabla y los textos |
| `por_empresa` | Sí | `si`, `opcional` o `no`. Con `si` u `opcional`, cada operación ve solo las filas de la empresa de la sesión |
| `atributo_activo` | No | Atributo booleano que cambian `desactivar` y `reactivar` |
| `operaciones` | Con CRUD | Operación → roles que pueden usarla: `listar`, `ver`, `crear`, `editar`, `desactivar`, `reactivar` y `restaurar_por_defecto`, esta solo con `edicion_en_bloque`. No existe `eliminar`: en InspectIA no se borra nada por CRUD |
| `edicion_en_bloque` | No | `true`: cada empresa tiene una fila por cada valor de una lista, y se editan todas juntas (rangos, RF-13). Ver «Edición en bloque» |
| `crear_con` | No | Otra entidad que se crea en el mismo formulario y en la misma transacción: `entidad`, `nombre` (el campo de la solicitud, como `administrador`), `etiquetas`, `atributos` (con sus validaciones de origen) y `valores` fijos |
| `atributos` | Sí | Lista de atributos (ver abajo) |
| `unicos` | No | Unicidad de varias columnas, normalizada o parcial |
| `checks` | No | Condiciones entre columnas, en SQL de PostgreSQL |
| `indices` | No | Índices adicionales. Cada referencia ya tiene el suyo |
| `reglas` | No | Reglas que se programan a mano |
| `valores_iniciales` | No | Filas por defecto |

## Claves del atributo

| Clave | Qué es |
|---|---|
| `nombre` | Columna en snake_case; en Java pasa a camelCase (`fecha_recepcion` → `fechaRecepcion`) |
| `tipo` | Uno de los tipos de la tabla siguiente |
| `longitud` | `texto` y `contrasena`: largo máximo de la columna |
| `precision`, `escala` | `decimal`. La escala también es el máximo de decimales que acepta la API (ver «Validaciones») |
| `valores` | `lista`: `[{ "codigo", "etiquetas": { "es", "en" } }]`. Los códigos van en mayúsculas y sin tildes |
| `valores_de` | `lista`: usa los valores de otra lista, como `"rango_recomendacion.categoria"` |
| `valores_en_formulario` | `lista`: los únicos códigos que se pueden asignar desde el CRUD, si no son todos. El backend también los valida (ver «Valores permitidos en el CRUD») |
| `referencia` | `referencia`: `{ "entidad", "al_borrar": "restringir" o "cascada" }`. El nombre termina en `_id` |
| `obligatorio` | `true` → `NOT NULL`. Las obligaciones que dependen del estado van en `checks` |
| `unico` | `true` → `UNIQUE` de una sola columna, sin normalizar |
| `por_defecto` | Valor por defecto |
| `validaciones` | Ver la tabla de validaciones |
| `mensajes` | Mensaje propio por validación (`es` y `en`), además de `unico`, `valores`, `valores_en_formulario` y `solo_lectura_si`. Si no hay, se usa el genérico |
| `entrada` | `contrasena`: campo del formulario (`nombre`, `etiquetas`, `obligatoria_al_crear`, `obligatoria_al_editar`) |
| `solo_lectura_si` | `{ "atributo", "igual_a" }`: el campo no se edita en las filas que cumplen la condición, como el límite de Fuera de norma. Solo en una entidad con `edicion_en_bloque`, en un número del formulario y con la condición sobre la lista que identifica cada fila. Si cambia, la API responde 400 con `mensajes.solo_lectura_si` o el genérico, «Este dato no se puede cambiar.» |
| `sugerencias` | `texto`: el formulario sugiere los valores que ya usó la empresa |
| `ayuda` | Texto de ayuda bajo el campo (`es` y `en`) |
| `ejemplo` | Un valor válido, que cumple todas las validaciones y `reglas` del atributo. Va en la documentación OpenAPI y en los datos de las pruebas generadas. Obligatorio si el atributo tiene `patron` y va en el formulario de una API, porque no se puede inventar (un NIT con su dígito de verificación, por ejemplo); en los demás, el generador arma uno |
| `en_lista`, `en_formulario` | Solo con CRUD: si el atributo sale en la lista y en el formulario |
| `etiquetas` | `es` y `en` |
| `fichas` | Trazabilidad del atributo |

## Tipos

| Tipo | PostgreSQL | Java | Frontend |
|---|---|---|---|
| `texto` | `varchar(longitud)` | `String` | texto |
| `entero` | `integer` | `Integer` | número entero |
| `decimal` | `numeric(precision, escala)` | `BigDecimal` | número |
| `booleano` | `boolean` | `Boolean` | casilla o interruptor |
| `fecha` | `date` | `LocalDate` | fecha ISO (aaaa-mm-dd) |
| `fecha_hora` | `timestamptz` | `OffsetDateTime` | fecha y hora ISO 8601 |
| `lista` | `varchar(30)` con `CHECK (col IN (...))` | `enum` | lista desplegable con las etiquetas |
| `referencia` | `uuid` con FK e índice | `UUID` o `@ManyToOne` | identificador |
| `contrasena` | `varchar(longitud)` con el hash BCrypt | `String` que nunca sale en las respuestas | campo de contraseña (`entrada`) |

## Validaciones

| Clave | Base de datos | Backend | Frontend (Yup) |
|---|---|---|---|
| `min`, `max` (números) | `CHECK` | `@Min`/`@Max` o `@DecimalMin`/`@DecimalMax` | `min`/`max` |
| `escala` (de un `decimal`, no va en `validaciones`) | `numeric(precision, escala)` | `@MaximoDecimales`, que cuenta los decimales sin los ceros del final; el número se guarda con esa escala | prueba propia |
| `longitud_min`, `longitud_max` (texto escrito) | — | `@Size` | `min`/`max` |
| `bytes_max` | — | validador propio, contando bytes en UTF-8 | prueba propia |
| `patron` | `CHECK (col ~ 'patron')` | `@Pattern` | `matches` |
| `formato: "correo"` | — | `@Email` | `email` |
| `no_posterior_a_hoy` | — | `@PastOrPresent`, con la hora de Colombia | `max(hoy)` |
| `recortar` | — | quita los espacios del inicio y del final antes de guardar | igual |
| `minusculas` | — | pasa a minúsculas antes de guardar | igual |

La `longitud` de un `texto` también se valida en el backend y en el frontend.

**Valores permitidos en el CRUD.** `valores_en_formulario` no solo limita el formulario: el backend también lo valida, sin depender de los CHECK de la base. Los CHECK son la última defensa, no la validación que ve el usuario. Si alguien envía a mano a la API un código para una `lista`:

| Caso | Ejemplo con `usuario.rol` | Respuesta de la API | Mensaje |
|---|---|---|---|
| El código existe, pero no está en `valores_en_formulario` | ADMIN_PLATAFORMA enviado por un administrador de empresa | «prohibido» (403, RNF-04); no guarda nada | `mensajes.valores_en_formulario` |
| El código no existe en `valores` | «JEFE» | «dato inválido» (400); no guarda nada | `mensajes.valores` o el genérico |

## Relaciones, únicos, checks e índices

- **Referencia:** FK con `ON DELETE RESTRICT` (`restringir`) o `CASCADE` (`cascada`), más un índice.
  - Si las dos tablas tienen `empresa_id`, la FK es compuesta: `(x_id, empresa_id)` → `x (id, empresa_id)`. Así la base no deja apuntar a filas de otra empresa (RNF-03).
- **`unicos`:** `{ "atributos", "normalizar", "donde", "mensaje", "fichas" }`.
  - `normalizar: true` compara sin distinguir mayúsculas ni espacios al inicio o al final: índice único sobre `lower(btrim(col))`.
  - `donde` crea un índice único parcial: `WHERE donde`.
- **`checks`:** `{ "nombre", "sql", "descripcion", "fichas" }`.
  - El SQL es de PostgreSQL y solo usa columnas de la misma tabla.
  - Si cambian los códigos de una lista, hay que revisar los checks que los nombran.
- **`indices`:** `{ "atributos", "normalizar", "fichas" }`.

## Reglas

Lo que no se puede declarar se nombra como regla: `{ "codigo", "tipo", "cuando", "atributo", "roles", "parametros", "descripcion", "mensaje", "fichas" }`.

| Tipo | Qué es | Ejemplo |
|---|---|---|
| `validacion` | Se revisa antes de guardar, en las operaciones de `cuando`. Si falla, muestra `mensaje` | Máximo 25 usuarios activos (RF-03) |
| `efecto` | Algo que pasa además de la operación, en las de `cuando` | Cortar las sesiones abiertas al cambiar una contraseña (RNF-04) |
| `accion` | Operación extra para los `roles` indicados | Restablecer la contraseña de un administrador (RF-02) |

En los mensajes, `{nombre}` se reemplaza por el valor del mismo nombre en `parametros` o en `validaciones`.

En una entidad con `edicion_en_bloque`, una regla de `validacion` recibe todas las filas juntas, en el orden de la lista, y solo se revisa al editar.

## Valores iniciales

`{ "modo", "descripcion", "filas" }`. Con `modo: "al_crear_empresa"`, las filas se crean para cada empresa nueva y son las que usa `restaurar_por_defecto` (los rangos de A.3). En una edición en bloque también reponen una fila que falte al editar o al restaurar.

## Cambiar de temática

Para otro producto se cambian solo estos JSON: etiquetas, listas (categorías, clases y motivos), valores iniciales, mensajes y, si hace falta, atributos. Después se vuelve a generar. Si cambia el código de una lista, revisa los `checks` que lo usan.
