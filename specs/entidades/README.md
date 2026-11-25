<!--
Nombre: README.md (specs/entidades)
Descripción: Qué hay en specs/entidades/ y formato de los JSON de entidades que lee el generador (RP-23).
Fecha de creación: 12/10/2026
Actualización: 24/11/2026
Autor: Equipo InspectIA
Versión: 1.15
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
| `region_pegada.json` | region_pegada | No | Módulo `analisis` (RF-07, D-45) |
| `modelo.json` | modelo | No | Módulo `analisis` y estadísticas (RF-17) |

## Qué genera el generador

- **De todos los JSON:** el script SQL de creación de las tablas, con sus restricciones, índices y triggers (RP-15), y los textos en español e inglés (`i18n/generado.properties` y `generado_en.properties`): etiquetas de entidades, atributos y valores, y mensajes (RP-11).
- **De los que tienen `"crud": true`:** la persistencia en el backend: entidad JPA, repositorio (con las consultas de los datos únicos), un `enum` por cada lista y, si hay `valores_iniciales`, la clase que los arma. La entidad lleva `@DynamicUpdate`: cada UPDATE escribe solo las columnas que cambiaron, así dos cambios a columnas distintas de la misma fila no se pisan; si dos cambian la misma columna a la vez, queda el último (D-36). Por eso, lo que una regla debe dejar en un valor aunque ya lo tuviera al leer la fila va en SQL, como el desbloqueo al cambiar una contraseña (D-35).
- **De los que están en `generador/alcance.json`, además:** la API en `/api/v1/<ruta>`: servicio, controlador con OpenAPI, reglas de acceso por ruta, solicitudes con sus validaciones, respuesta, la interfaz de las `reglas` y una prueba de integración generada. Hoy todas las entidades con CRUD tienen API; una nueva la recibe cuando llega su ficha. Las de `edicion_en_bloque` tienen otra API (ver «Edición en bloque»).
- **De los que están en `pantallas` de `generador/alcance.json`:** la pantalla del frontend, con su lista, su formulario y sus acciones; el cliente de su API; sus validaciones de Yup; y sus pruebas, con Vitest y con Playwright. Hoy la tienen `empresa` y `usuario`; la de rangos llega con su PR. Ver «Pantalla generada».
- El código generado no se edita a mano (RP-23). Las `reglas` se programan a mano, fuera de las carpetas generadas, y el código generado las llama. En el frontend, lo que el JSON no puede declarar de una pantalla, como el panel de administradores de una empresa, va a mano en `frontend/src/extensiones.js`: lo generado no importa código escrito a mano.

### Cómo responde la API generada

- **Roles:** los de `operaciones`. La cadena de seguridad los revisa por ruta antes de leer la petición, así que un rol sin permiso recibe 403 aunque envíe datos inválidos; `@PreAuthorize` es la segunda barrera (RNF-04).
- **Por empresa:** la empresa sale de la sesión y nunca de la petición. Cada consulta se limita a ella, y un registro de otra empresa responde 404, como si no existiera (RNF-03). Por eso ninguna operación de una entidad por empresa puede ser del administrador de la plataforma, que no tiene empresa.
- **Orden de las revisiones:** primero las validaciones del JSON y las `reglas` (400); después los datos únicos (409). Un valor de `valores_en_formulario` que no se puede asignar responde 403 antes que todo lo demás. Lo que se guarda en una operación, incluido lo de `crear_con` y `valores_iniciales`, va en una sola transacción.
- **Listado:** una página con `contenido`, `pagina` (desde 0), `tamano`, `totalElementos` y `totalPaginas`. Trae 20 filas por página si no se pide otro tamaño (máximo 100) y se ordena por `nombre`, sin distinguir mayúsculas, si la entidad tiene ese atributo; si no, por antigüedad. Se pide otra página con `?pagina=` y otro tamaño con `?tamano=`. Con `?activo=true` o `?activo=false` trae solo los activos o solo los desactivados (D-33).
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

### Pantalla generada

La pantalla de una entidad de `pantallas` se arma con lo que dice su JSON:

- **Lista:** páginas fijas de 20 registros, como en el prototipo. En el PC es una tabla, y en el celular, una tarjeta por registro (D-34). Muestra los atributos con `en_lista`, el de `debajo_del_nombre` bajo el nombre y, si la entidad tiene `atributo_activo`, su estado. Arriba va el resumen, como «7 empresas registradas: 6 activas y 1 desactivada.», que cuenta los activos con el filtro del listado (D-33). Con `limite_de_activos`, el resumen cuenta los activos contra el máximo de esa regla, como «5 de 25 usuarios activos, contándote a ti.», y al llegar a él «Nuevo» y «Reactivar» se deshabilitan, con un aviso que dice por qué: el mensaje de la regla.
- **Acciones:** cada rol ve solo las de sus `operaciones`; la API lo revisa igual (RNF-04). Desactivar pide confirmación si `textos` trae `confirmar_desactivar`. Después de guardar, desactivar o reactivar, un mensaje dice qué pasó, con el nombre del registro, como «Se guardó Cacaos del Huila.».
- **Formulario:** los atributos con `en_formulario` y, al registrar, los de `crear_con`, cada grupo con su título. Yup valida lo mismo que la API (ver «Validaciones»). Los errores que responde la API van a su campo, y los que no son de un campo, arriba del formulario. En el PC va encima de la lista o, con `formulario: "al_lado"`, a su lado; en el celular, siempre encima (D-37). Al registrar, un campo empieza con el valor de `inicial`, como el rol Usuario final; una lista sin él, con su `por_defecto`, y sin ninguno de los dos, en «Elige una opción», sin valor, como la variedad del café de `generador/pruebas/ejemplos/cafe.json`. Una contraseña opcional al editar, como la de un usuario, vacía va como `null` y queda la que había; al escribir la de otra persona aparece `nota_contrasena_de_otro`, con lo que pasa al guardarla (D-30).
- **Fila propia:** las reglas con `en_la_fila_propia` (ver «Reglas») se cumplen en la fila de quien tiene la sesión antes de llamar a la API, que las sigue revisando. Esa fila lleva la marca de `fila_propia`, como «Tú», y no tiene la acción que impide una regla sin `atributo`, como «Desactivar». Al editarla, el campo de una regla con `atributo` se ve deshabilitado, con el mensaje de la regla, como el rol; si es una contraseña, en su lugar va el mensaje, y si nombra «Cambiar contraseña», ese nombre es un enlace a esa página.
- **Teclado (RNF-06 C4):** al abrir un panel, el foco va a su título; al cerrarlo, vuelve al botón que lo abrió.
- **Menú y rutas:** la sección sale con su `icono` y con los roles de `listar`. Un ícono que el menú no conoce, como el de otra temática, sale genérico.

**Pantalla en bloque.** Con `edicion_en_bloque`, como los rangos (RF-13), la pantalla no tiene lista ni panel: todas las filas van en un solo formulario (`PantallaEnBloque`).

- **Filas:** una por cada valor de la lista que las identifica, en su orden. Cada fila es un grupo con el nombre de su valor y sus campos. En el PC, el nombre va a la izquierda, como en el prototipo; en el celular, todo va en una columna, y los botones a todo el ancho (D-38).
- **Campos:** los atributos con `en_formulario`, de tipo `texto` o `decimal`:
  - un `decimal` se escribe con coma o con punto, y se ve con el separador del idioma y la escala de su columna: 96,0 en español y 96.0 en inglés;
  - un `texto` con `varias_lineas` va en varias líneas;
  - en la fila que cumple `solo_lectura_si`, el campo se ve de solo lectura, con un candado, y el mensaje de la regla debajo, como motivo.
- **Guardar:** envía todas las filas juntas, con los números leídos. Después, los campos quedan como los responde la API: 90,50 queda 90,5.
- **Errores:** los de Yup y los de la API en `filas[i].campo` van debajo de su campo; los del bloque, en `filas` o sin campo, van arriba del formulario, con su mensaje.
- **Restaurar:** «Restaurar valores por defecto» pide confirmación, con `confirmar_restaurar_detalle` (D-34). El aviso de que se guardó o se restauró va junto a los botones, donde se ve después de usarlos, también en el celular (D-38).
- **Lo que va a mano**, en `frontend/src/extensiones.js`, porque el JSON no lo puede declarar:
  - un resumen antes del formulario, como «Así quedan los rangos»;
  - las `reglas` de todas las filas juntas, como el orden de los límites, que también van a mano en el backend.

  La pantalla revisa esas reglas antes de enviar, después de Yup, en el mismo orden que la API. Marca los campos de la regla, salvo los de solo lectura, y el aviso de arriba los describe, sin repetir el mensaje en cada uno.

| Clave de `pantalla` | Qué es |
|---|---|
| `icono` | El ícono de la sección en el menú, por su nombre, como `empresas` |
| `debajo_del_nombre` | El atributo que va bajo el nombre en la lista, como el NIT de una empresa |
| `formulario` | Dónde va el formulario en el PC: `encima` de la lista, como en Empresas, o `al_lado`, como en Usuarios. Sin la clave, encima. En el celular va siempre encima |
| `limite_de_activos` | El `codigo` de la regla de `validacion` cuyo `parametros.maximo` limita los activos al crear o al reactivar, como `maximo_usuarios_activos`. Necesita `atributo_activo` |
| `inicial` | El valor con que empieza un campo del formulario al registrar, como `{ "rol": "USUARIO_FINAL" }`; en una lista, uno de los que ofrece el formulario. No es un `por_defecto`: la base no cambia |
| `textos` | Los textos propios de la pantalla, en `es` y `en`. Los de una fila, como `editar_fila`, usan `{atributo}` con los valores de esa fila: «Editar {nombre}» |

| Textos | Cuándo son obligatorios | Ejemplo |
|---|---|---|
| `titulo_lista`, `vacia` | Con `listar` | «Empresas registradas» |
| `nuevo`, `crear` | Con `crear` | «Nueva empresa», «Registrar empresa» |
| `titulo_editar`, `editar_fila` | Con `editar` | «Editar empresa», «Editar {nombre}» |
| `al_guardar` | Con `crear` o `editar` | «Se guardó {nombre}.» |
| `desactivar_fila`, `al_desactivar` | Con `desactivar` | «Desactivar {nombre}», «Se desactivó {nombre}.» |
| `reactivar_fila`, `al_reactivar` | Con `reactivar` | «Reactivar {nombre}», «Se reactivó {nombre}.» |
| `activo`, `inactivo` | Con `atributo_activo` | «Activa», «Desactivada» |
| `resumen` y los contados que usa | `resumen`, si trae alguno de los textos del resumen; de `resumen_total`, `resumen_activos` y `resumen_inactivos`, los que usa `resumen` (`{total}`, `{activos}` o `{inactivos}`), y los de activos, solo con `atributo_activo`. Los contados van en `singular` y `plural`, con `{cantidad}`. Con `limite_de_activos`, `resumen` y `resumen_activos` pueden usar `{maximo}` | «{total}: {activos} y {inactivos}.»; en usuario, «{activos}, contándote a ti.», con «{cantidad} de {maximo} usuarios activos» |
| `confirmar_desactivar`, `confirmar_desactivar_detalle` | Si trae uno, los dos | «¿Desactivar {nombre}?» |
| `fila_propia` | Con una regla `en_la_fila_propia` | «Tú», en usuario |
| `contrasena_al_editar`, `ayuda_contrasena_al_editar`, `nota_contrasena_de_otro` | Con una contraseña opcional al editar | «Contraseña nueva», en usuario. La nota usa los atributos de la fila, como `{nombre}` |
| `titulo_editar`, `al_guardar`, en una pantalla en bloque | Con `editar` | «Editar los rangos», el nombre del formulario; «Se guardaron los rangos.» |
| `confirmar_restaurar_detalle`, `al_restaurar` | Con `restaurar_por_defecto` | «Se pierden los límites y los textos propios de la empresa.», «Se restauraron los valores por defecto.» |
| `descripcion`, `columna_nombre`, `debajo_del_nombre`, `nota_formulario`, `nota_lista`, `nota_restaurar`, `contrasena_al_crear`, `ayuda_contrasena_al_crear` | Opcionales | «NIT {nit}» bajo el nombre |

### Lo que la pantalla generada todavía no soporta

Si una entidad de `pantallas` usa algo de esta lista, el generador se detiene y dice qué falta: una entidad sin `listar` o sin `pantalla`, un texto obligatorio que falte, un atributo del formulario que no sea texto, contraseña o lista (en una pantalla en bloque, que no sea texto o decimal) y una columna de la lista que no sea texto ni lista.

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
| `pantalla` | Con pantalla | Lo propio de su pantalla generada: `icono`, `debajo_del_nombre`, `formulario`, `limite_de_activos`, `inicial` y `textos` (ver «Pantalla generada») |

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
| `mensajes` | Mensaje propio por validación (`es` y `en`), además de `unico`, `valores` (todavía no se usa: ver «Valores permitidos en el CRUD»), `valores_en_formulario` y `solo_lectura_si`. Si no hay, se usa el genérico |
| `entrada` | `contrasena`: campo del formulario (`nombre`, `etiquetas`, `obligatoria_al_crear`, `obligatoria_al_editar`) |
| `solo_lectura_si` | `{ "atributo", "igual_a" }`: el campo no se edita en las filas que cumplen la condición, como el límite de Fuera de norma. Solo en una entidad con `edicion_en_bloque`, en un número del formulario y con la condición sobre la lista que identifica cada fila. Si cambia, la API responde 400 con `mensajes.solo_lectura_si` o el genérico, «Este dato no se puede cambiar.». En la pantalla, el campo se ve de solo lectura, con ese mensaje como motivo |
| `varias_lineas` | `texto` del formulario: `true` para que vaya en varias líneas, como la recomendación de un rango. Empieza con dos y crece con el texto |
| `sugerencias` | `texto`: el formulario sugiere los valores que ya usó la empresa |
| `ayuda` | Texto de ayuda bajo el campo (`es` y `en`) |
| `ejemplo` | Un valor válido, que cumple todas las validaciones y `reglas` del atributo. Va en la documentación OpenAPI, en los datos de las pruebas generadas y en los casos de límite de su `patron`. Obligatorio si el atributo tiene `patron` y va en el formulario de una API, porque no se puede inventar (un NIT con su dígito de verificación, por ejemplo); en los demás, el generador arma uno |
| `en_lista`, `en_formulario` | Solo con CRUD: si el atributo sale en la lista y en el formulario |
| `solo_lo_cambian_las_reglas` | `true`: JPA lee la columna, pero no la escribe (`insertable` y `updatable` en `false`); solo la cambian las reglas, con SQL, como `usuario.version_sesion` (D-35, D-36). Solo con CRUD y fuera del formulario. Si el atributo es obligatorio, necesita `por_defecto`: al crear, el valor lo pone la base. La validación rechaza la clave donde no serviría, en vez de ignorarla |
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
| `escala` (de un `decimal`, no va en `validaciones`) | `numeric(precision, escala)` | `@MaximoDecimales`, que cuenta los decimales sin los ceros del final; el número se guarda con esa escala | prueba propia, que cuenta igual (`maximoDecimales`) |
| `longitud_min`, `longitud_max` (texto escrito) | — | `@Size` | `min`/`max` |
| `bytes_max` | — | validador propio, contando bytes en UTF-8 | prueba propia |
| `patron` | `CHECK (col ~ 'patron')` | `@Pattern` | `matches` |
| `formato: "correo"` | — | `@Email` | — (solo el backend) |
| `no_posterior_a_hoy` | — | `@PastOrPresent`, con la hora de Colombia | `max(hoy)` |
| `recortar` | — | quita los espacios del inicio y del final antes de guardar | igual |
| `minusculas` | — | pasa a minúsculas antes de guardar | igual |

La `longitud` de un `texto` también se valida en el backend y en el frontend.

**Números en el formulario.** Un `decimal` se escribe con coma o con punto, y Yup lo lee igual (`numeroDecimal`). Vacío cuenta como el dato que falta; lo que no se escribe como un número, como «noventa» o «1.000,5», da «Escribe un número, como 90,5.» (`validacion.numero`). A la API va un número.

**Patrones.** El `patron` va anclado, con `^` al inicio y `$` al final, y sin lo que Java, PostgreSQL y JavaScript leen distinto, como los grupos `(?…)`, los cuantificadores posesivos (`++`) o `\b`: el `CHECK` y `matches` buscan el patrón dentro del texto y `@Pattern` revisa el texto entero, así que solo anclado validan igual los tres. Si no, la generación se detiene.

**El correo.** Su formato lo valida solo el backend, como el dígito del NIT, para no tener dos algoritmos: su error llega al campo del formulario al guardar. Por eso un correo demasiado largo y sin arroba da el error del largo en el formulario y el del formato en la API.

**Un error por campo.** Si un valor falla por más de una validación, la API responde un solo error por campo (`ManejadorDeErrores`): el del dato que falta y, si no, el de la restricción de Bean Validation que va primero por su nombre, como `@Pattern` antes que `@Size` y `@MaximoBytes` antes que `@Size`. Yup revisa en ese mismo orden, así el formulario dice el mismo error que la API, salvo en el caso del correo.

**Casos de límite.** Las pruebas generadas de la API y de Yup revisan los mismos valores, con el mismo mensaje en español:

- El dato que falta, y un carácter de más y uno de menos que el largo.
- Con `bytes_max`, una eñe de más que la mitad de los bytes y, si también hay largo máximo, una eñe más que el largo, que falla por los dos.
- Con `patron`: un valor que no lo cumple; el `ejemplo` con algo pegado antes y con algo pegado después, como x900123456-8 y 900123456-8x, que solo aceptaría un patrón sin anclar; y un valor demasiado largo que tampoco lo cumple.
- Con `valores_en_formulario`, cada valor de la lista que existe pero el formulario no ofrece, como ADMIN_PLATAFORMA en el rol, con el mensaje del 403 de la API: la prueba de Yup lo rechaza, y la de la API lo revisa en la de permisos (ver «Valores permitidos en el CRUD»).
- Con `min` o `max`, uno de más y uno de menos que el límite, como −1 y 101.
- Con `escala`, un decimal de más, como 0,05 con escala 1. Y, con `min` o `max`, el número que falla por el límite y por los decimales a la vez, como 100,55 y −0,55: así queda fijo que Yup escoge el mismo error que la API, el del límite.
- Los valores del borde, que se aceptan. En una entidad con `edicion_en_bloque` solo los revisa la prueba de Yup: la prueba generada de la API no puede armar un bloque válido sin conocer la regla del orden. Los bordes aceptados del lado de la API ya los prueba `RangosRecomendacionIT`, escrito a mano en el PR #5 (100,0, 0,1, 90,50 y la recomendación de 300 caracteres).

**Valores permitidos en el CRUD.** `valores_en_formulario` no solo limita el formulario: el backend también lo valida, sin depender de los CHECK de la base. Los CHECK son la última defensa, no la validación que ve el usuario. Si alguien envía a mano a la API un código para una `lista`:

| Caso | Ejemplo con `usuario.rol` | Respuesta de la API | Mensaje |
|---|---|---|---|
| El código existe, pero no está en `valores_en_formulario` | ADMIN_PLATAFORMA enviado por un administrador de empresa | «prohibido» (403, RNF-04); no guarda nada | `mensajes.valores_en_formulario` |
| El código no existe en `valores` | «JEFE» | «dato inválido» (400); no guarda nada | El genérico, «El valor no es válido.»: el backend rechaza el código al leer la petición. `mensajes.valores` todavía no se usa: llega con RF-05, cuando `foto` tenga su API |

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

Lo que no se puede declarar se nombra como regla: `{ "codigo", "tipo", "cuando", "atributo", "en_la_fila_propia", "roles", "parametros", "descripcion", "mensaje", "fichas" }`.

| Tipo | Qué es | Ejemplo |
|---|---|---|
| `validacion` | Se revisa antes de guardar, en las operaciones de `cuando`. Si falla, muestra `mensaje` | Máximo 25 usuarios activos (RF-03) |
| `efecto` | Algo que pasa además de la operación, en las de `cuando` | Cortar las sesiones abiertas al cambiar una contraseña (RNF-04) |
| `accion` | Operación extra para los `roles` indicados | Restablecer la contraseña de un administrador (RF-02) |

En los mensajes, `{nombre}` se reemplaza por el valor del mismo nombre en `parametros` o en `validaciones`.

Con `en_la_fila_propia: true`, una regla de `validacion` se aplica a la fila de quien tiene la sesión, como «No puedes desactivar tu propio usuario.». Va en el JSON, y no solo en el código de la regla, porque se puede declarar (RP-23): así la pantalla generada la cumple antes de llamar a la API, sin código propio de cada entidad, y la API la sigue revisando. Va solo en `usuario`, la entidad de quien tiene la sesión: sin `atributo`, solo en `desactivar`; con `atributo`, solo en `editar` y con un atributo del formulario. Ver «Pantalla generada».

En una entidad con `edicion_en_bloque`, una regla de `validacion` recibe todas las filas juntas, en el orden de la lista, y solo se revisa al editar.

## Valores iniciales

`{ "modo", "descripcion", "filas" }`. Con `modo: "al_crear_empresa"`, las filas se crean para cada empresa nueva y son las que usa `restaurar_por_defecto` (los rangos de A.3). En una edición en bloque también reponen una fila que falte al editar o al restaurar.

## Cambiar de temática

Para otro producto se cambian solo estos JSON: etiquetas, listas (categorías, clases y motivos), valores iniciales, mensajes, los textos de `pantalla` y, si hace falta, atributos. Después se vuelve a generar. Si cambia el código de una lista, revisa los `checks` que lo usan.

`npm run demo:otra-tematica`, desde `frontend/`, lo muestra en una copia del proyecto, sin tocar el repositorio: agrega a InspectIA una entidad de otra temática y saca su tabla, su API y su pantalla. Por defecto es el producto de la pauta del docente (`generador/pruebas/ejemplos/producto.json`); con `-- --entidad <ruta del JSON>`, otra, como el café especial del Huila (`cafe.json`, en la misma carpeta). El paso a paso para la sustentación está en `docs/demostracion-otra-tematica.md`.
