<!--
Nombre: README.md (specs/entidades)
Descripción: Qué hay en specs/entidades/ y formato de los JSON de entidades que lee el generador (RP-23).
Fecha de creación: 12/10/2026
Actualización: 13/10/2026
Autor: Equipo InspectIA
Versión: 1.1
-->

# specs/entidades

Un archivo JSON por cada tabla de la base de datos. Es la entrada de `generador/` (RP-23), y las tablas solo salen de aquí (RP-15). `specs/modelo-datos.md` explica el modelo y su trazabilidad; si cambias un JSON, actualiza también ese documento en el mismo PR.

| Archivo | Tabla | CRUD generado | Quién lo usa |
|---|---|---|---|
| `empresa.json` | empresa | Sí | Administrador de la plataforma (RF-02, D-27) |
| `usuario.json` | usuario | Sí | Administrador de empresa (RF-03) |
| `rango_recomendacion.json` | rango_recomendacion | Sí: solo editar y restaurar | Administrador de empresa (RF-13) |
| `lote.json` | lote | No | Módulo `analisis` (D-21) |
| `muestra.json` | muestra | No | Módulo `analisis` |
| `foto.json` | foto | No | Módulo `analisis` |
| `grano.json` | grano | No | Módulo `analisis` |
| `modelo.json` | modelo | No | Módulo `analisis` y estadísticas (RF-17) |

## Qué genera el generador

- **De todos los JSON:** el script SQL de creación de las tablas, con sus restricciones, índices y triggers (RP-15), y los textos en español e inglés (etiquetas de entidades, atributos y valores, y mensajes).
- **De los que tienen `"crud": true`, además:** en el backend, entidad JPA, repositorio, servicio, controlador REST, DTO, validaciones, OpenAPI y pruebas; en el frontend, la lista, el formulario con Formik y Yup, las rutas y los permisos.
- El código generado no se edita a mano (RP-23). Las `reglas` se programan a mano, fuera de las carpetas generadas, y el código generado las llama.

## Columnas implícitas

No se escriben en los JSON. El generador las agrega a todas las tablas, y se pueden usar por su nombre en `unicos`, `indices` y `checks`:

| Columna | Definición |
|---|---|
| `id` | `uuid`, llave primaria, `DEFAULT uuidv7()` |
| `fecha_creacion` | `timestamptz NOT NULL DEFAULT now()` |
| `fecha_actualizacion` | `timestamptz NOT NULL DEFAULT now()`; un trigger la actualiza en cada cambio |
| `empresa_id` | Solo si `por_empresa` es `si` (obligatoria) u `opcional` (puede ir vacía). FK a `empresa` (RNF-03) |

## Claves de la entidad

| Clave | Obligatoria | Qué es |
|---|---|---|
| `_header` | Sí | Header del artefacto (RP-10): nombre, descripción, fecha de creación, actualización, autor y versión |
| `entidad` | Sí | Nombre de la tabla: snake_case, singular, sin tildes. Igual al nombre del archivo |
| `clase` | Sí | Nombre de la clase Java y del componente React (PascalCase) |
| `descripcion` | Sí | Para qué sirve. Va al JavaDoc, al JSDoc y a OpenAPI |
| `etiquetas` | Sí | `es` y `en`, cada uno con `singular` y `plural` |
| `fichas` | Sí | RF, RNF, D, P o RP que piden la entidad |
| `crud` | Sí | `true`: se generan backend y pantallas. `false`: solo la tabla y los textos |
| `por_empresa` | Sí | `si`, `opcional` o `no`. Con `si` u `opcional`, cada operación ve solo las filas de la empresa de la sesión |
| `atributo_activo` | No | Atributo booleano que cambian `desactivar` y `reactivar` |
| `operaciones` | Con CRUD | Operación → roles que pueden usarla: `listar`, `ver`, `crear`, `editar`, `desactivar`, `reactivar` y `restaurar_por_defecto`. No existe `eliminar`: en InspectIA no se borra nada por CRUD |
| `edicion_en_bloque` | No | `true`: el formulario edita juntas todas las filas de la empresa (rangos, RF-13) |
| `crear_con` | No | Otra entidad que se crea en el mismo formulario y en la misma transacción: `entidad`, `etiquetas`, `atributos` (con sus validaciones de origen) y `valores` fijos |
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
| `precision`, `escala` | `decimal` |
| `valores` | `lista`: `[{ "codigo", "etiquetas": { "es", "en" } }]`. Los códigos van en mayúsculas y sin tildes |
| `valores_de` | `lista`: usa los valores de otra lista, como `"rango_recomendacion.categoria"` |
| `valores_en_formulario` | `lista`: los códigos que ofrece el formulario, si no son todos |
| `referencia` | `referencia`: `{ "entidad", "al_borrar": "restringir" o "cascada" }`. El nombre termina en `_id` |
| `obligatorio` | `true` → `NOT NULL`. Las obligaciones que dependen del estado van en `checks` |
| `unico` | `true` → `UNIQUE` de una sola columna, sin normalizar |
| `por_defecto` | Valor por defecto |
| `validaciones` | Ver la tabla de validaciones |
| `mensajes` | Mensaje propio por validación (`es` y `en`), además de `unico` y `valores`. Si no hay, se usa el genérico |
| `entrada` | `contrasena`: campo del formulario (`nombre`, `etiquetas`, `obligatoria_al_crear`, `obligatoria_al_editar`) |
| `solo_lectura_si` | `{ "atributo", "igual_a" }`: el campo no se edita en las filas que cumplen la condición |
| `sugerencias` | `texto`: el formulario sugiere los valores que ya usó la empresa |
| `ayuda` | Texto de ayuda bajo el campo (`es` y `en`) |
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
| `longitud_min`, `longitud_max` (texto escrito) | — | `@Size` | `min`/`max` |
| `bytes_max` | — | validador propio, contando bytes en UTF-8 | prueba propia |
| `patron` | `CHECK (col ~ 'patron')` | `@Pattern` | `matches` |
| `formato: "correo"` | — | `@Email` | `email` |
| `no_posterior_a_hoy` | — | `@PastOrPresent`, con la hora de Colombia | `max(hoy)` |
| `recortar` | — | quita los espacios del inicio y del final antes de guardar | igual |
| `minusculas` | — | pasa a minúsculas antes de guardar | igual |

La `longitud` de un `texto` también se valida en el backend y en el frontend.

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
| `efecto` | Algo que pasa además de la operación, en las de `cuando` | — |
| `accion` | Operación extra para los `roles` indicados | Restablecer la contraseña de un administrador (RF-02) |

En los mensajes, `{nombre}` se reemplaza por el valor del mismo nombre en `parametros` o en `validaciones`.

## Valores iniciales

`{ "modo", "descripcion", "filas" }`. Con `modo: "al_crear_empresa"`, las filas se crean para cada empresa nueva y son las que usa `restaurar_por_defecto` (los rangos de A.3).

## Cambiar de temática

Para otro producto se cambian solo estos JSON: etiquetas, listas (categorías, clases y motivos), valores iniciales, mensajes y, si hace falta, atributos. Después se vuelve a generar. Si cambia el código de una lista, revisa los `checks` que lo usan.
