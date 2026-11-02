<!--
Nombre: demostracion-otra-tematica.md
Descripción: Guía de la demostración de otra temática para la sustentación (RP-23): cómo prepararla, el paso a paso con el café especial del Huila, qué produce cada campo del JSON, lo que el JSON no decide, un experimento con un campo nuevo y qué hacer si algo falla.
Fecha de creación: 02/11/2026
Actualización: 02/11/2026
Autor: Equipo InspectIA
Versión: 1.0
-->

# Demostración de otra temática (RP-23)

La pauta RP-23 pide definir las entidades en JSON y generar con Python la base de datos, el back-end y el front-end, para poder cambiar de temática. La demostración lo muestra en vivo: agrega a InspectIA una entidad que no es de cacao, escrita solo en un JSON, y saca su tabla, su API, su pantalla y sus pruebas. Todo pasa en una copia del proyecto, en `frontend/.demostracion`: el repositorio no cambia.

Hay dos ejemplos, en `generador/pruebas/ejemplos/`:

| JSON | Qué es | Quién lo usa |
|---|---|---|
| `cafe.json` | Un café especial del Huila: nombre, variedad, región, proceso y estado. Es por empresa | El administrador de empresa |
| `producto.json` | El producto de la pauta del docente para el back-end: nombre, categoría, descripción y estado | El administrador de la plataforma |

## Antes de la sustentación

1. Abre Docker Desktop con tiempo y espera a que arranque.
2. Ten creado el entorno del generador (`generador/.venv`, ver `generador/README.md`) y corre `npm ci` en `frontend/`.
3. Haz una corrida de prueba el mismo día, con el paso a paso de abajo. La primera vez tarda más, porque compila la copia. Después, la copia enciende en unos 30 s, y con `--con-pruebas`, en unos 3 minutos.
4. Deja abierto `generador/pruebas/ejemplos/cafe.json` en el editor.

## Paso a paso, con café

1. **El JSON.** Muestra `cafe.json`: es todo lo que se escribe. Dice:
   - la entidad (`cafe`) y su ruta en la API (`cafes`);
   - sus textos en español y en inglés;
   - quién la usa (`operaciones`) y que es por empresa (`por_empresa: "si"`);
   - sus cinco atributos.
2. **Encender.** Desde `frontend/`:

   ```bash
   npm run demo:otra-tematica -- --entidad ../generador/pruebas/ejemplos/cafe.json
   ```

   Las opciones van después de `--`. Sin él, npm se queda con ellas, y la demostración se detiene y lo dice.
3. **La consola.** Muestra, en orden:
   1. lo único que cambia en la copia: el JSON y el alcance (`generador/alcance.json`), con `cafe` en la API y en las pantallas;
   2. lo que escribió el generador, por capa: la tabla, la API y sus pruebas, los textos, y la pantalla y sus pruebas;
   3. la dirección de la copia, con quién entrar, Swagger y psql.
4. **Entrar** con lo que dice la consola: «Entra como Ana Pérez (administrador de empresa de Cacaos del Huila)», con su correo y una contraseña que solo sirve en esa corrida.
5. **«Cafés especiales»** está al final del menú, con el ícono genérico (ver «Lo que el JSON no decide»). La lista empieza vacía: «Todavía no hay cafés especiales registrados.».
6. **Las validaciones.** «Nuevo café especial» abre el formulario:
   - Variedad y Proceso empiezan en «Elige una opción»;
   - Estado empieza en Activo, su valor por defecto;
   - «Registrar café especial» sin llenar nada marca cuatro campos con «Este dato es obligatorio.».
7. **Registrar y editar.** Por ejemplo: Geisha de altura, Geisha, Pitalito y Lavado.
   - Sale «Se guardó Geisha de altura.», y en la lista, Pitalito va bajo el nombre.
   - «Editar» lo pasa a Inactivo.
8. **En inglés.** Con el idioma de arriba en English, la sección es «Specialty coffees», y Lavado es «Washed».
9. **La API**, en Swagger UI, en la dirección que da la consola: `/api/v1/cafes`. Para probarla, inicia sesión con `POST /api/v1/sesion` y pega el token en «Authorize».
10. **La tabla**, en psql, con el comando de la consola:

    ```bash
    docker exec -it inspectia-demo-postgres-1 psql -U inspectia -d inspectia -c "\d cafe"
    ```

### La separación por empresa

Café es por empresa (`por_empresa: "si"`). En el entorno de la demostración hay una sola empresa activa, Cacaos del Huila, así que la separación no se ve en el navegador. Se ve en dos lugares:

- **En la tabla:** `\d cafe` muestra la columna `empresa_id`, obligatoria y con su llave foránea a `empresa`. La API la llena con la empresa de la sesión, nunca con algo que llegue en la petición.
- **En las tres pruebas de RNF-03 de `CafeApiGeneradaIT`,** que corren con `--con-pruebas`:
  - un registro de otra empresa responde 404 en cada operación que lo usa;
  - el listado solo trae los registros de la empresa de la sesión (RNF-03 C2);
  - el registro nuevo queda en la empresa de la sesión.

### Con las pruebas generadas

```bash
npm run demo:otra-tematica -- --entidad ../generador/pruebas/ejemplos/cafe.json --con-pruebas
```

Antes de encender la copia, corre allí las pruebas que el generador escribió para café: 27 de Vitest (el cliente de la API, las validaciones y la pantalla) y 40 de `CafeApiGeneradaIT`, con PostgreSQL en Testcontainers.

## Al terminar

Ctrl+C en la consola apaga la copia y borra su base. Después,

```bash
docker compose ls
```

ya no muestra `inspectia-demo`. La copia, en `frontend/.demostracion`, queda; la próxima corrida la pone al día con el repositorio.

## Con producto

Sin `--entidad`, la demostración agrega producto, el de la pauta del docente:

```bash
npm run demo:otra-tematica
```

Entra el administrador de la plataforma, y «Productos» está al final de su menú.

## Qué produce cada campo del JSON

| En `cafe.json` | Base de datos | API | Pantalla |
|---|---|---|---|
| `entidad`, `ruta` y `etiquetas` | La tabla `cafe` | `/api/v1/cafes` | La ruta `/cafes` y «Cafés especiales» en el menú, con sus textos en español y en inglés |
| `por_empresa: "si"` | `empresa_id`, obligatoria, con su llave foránea a `empresa` | La empresa sale de la sesión; un registro de otra responde 404 (RNF-03) | Cada persona ve solo lo de su empresa |
| `operaciones` | — | Solo el administrador de empresa lista, ve, crea y edita; los demás roles reciben 403 | La sección en el menú de ese rol, con «Nuevo café especial» y «Editar» |
| `nombre`: texto obligatorio, hasta 120, que se recorta | `varchar(120) NOT NULL` | Obligatorio y hasta 120, sin espacios al inicio ni al final; el listado se ordena por él | Campo de texto y primera columna de la lista |
| `variedad` y `proceso`: listas obligatorias, sin valor por defecto | `varchar(30) NOT NULL`, con un `CHECK` de sus valores | Obligatorias; un código que no existe responde 400 | Listas que empiezan en «Elige una opción», y una columna con la etiqueta en el idioma elegido |
| `region`: texto obligatorio, hasta 80 | `varchar(80) NOT NULL` | Obligatorio y hasta 80 | Campo «Región» (en el código va sin tilde) y, en la lista, bajo el nombre (`debajo_del_nombre`) |
| `estado`: lista con `por_defecto: "ACTIVO"` | `varchar(30) NOT NULL DEFAULT 'ACTIVO'`, con su `CHECK` | Si no llega, queda Activo | Lista que empieza en Activo, y la columna Estado |

## Lo que el JSON no decide

- **Los números.** En una pantalla con lista y formulario, como la de café, el generador solo muestra texto y listas. Con un atributo `entero` o `decimal` en la lista o en el formulario, se detiene y lo dice: «No se puede generar la pantalla de cafe: altitud, de tipo entero, en el formulario». Fuera de la pantalla, un número opcional solo llega a la base y a la entidad JPA, no a la API. Hoy, los números en un formulario solo existen en la pantalla en bloque, como los límites de los rangos.
- **El ícono.** `pantalla.icono` solo lo nombra. El dibujo sale de los que ya tiene la app, en `ICONOS` de `frontend/src/navegacion/MenuLateral.jsx`. `cafe` no está entre ellos, así que el menú muestra el genérico, una lista. Se puede usar uno de los que tiene, como `empresas`; uno nuevo se agrega a mano en ese archivo.

## Un experimento: un campo nuevo

Muestra que el JSON manda, sin tocar el repositorio:

1. **Copia `cafe.json` fuera del repositorio**, en la carpeta de arriba. Desde `frontend/`:

   ```bash
   cp ../generador/pruebas/ejemplos/cafe.json ../../cafe.json
   ```

   La copia se sigue llamando `cafe.json`: el archivo debe llamarse como su `entidad`.
2. **Agrega las notas de cata.** En esa copia, al final de `atributos`, después de `estado`, agrega una coma y este atributo:

   ```json
   {
     "nombre": "notas_de_cata",
     "tipo": "texto",
     "longitud": 500,
     "obligatorio": false,
     "varias_lineas": true,
     "en_lista": false,
     "en_formulario": true,
     "etiquetas": { "es": "Notas de cata", "en": "Tasting notes" },
     "fichas": ["RP-23"]
   }
   ```

3. **Vuelve a correr**, con `--entidad` apuntando a la copia:

   ```bash
   npm run demo:otra-tematica -- --entidad ../../cafe.json
   ```

4. **Aparece el campo:**
   - en el formulario, «Notas de cata», opcional y en varias líneas;
   - en la base, la columna `notas_de_cata varchar(500)` (`\d cafe`);
   - en la API, `notasDeCata`.

   No va en la lista, porque `en_lista` es `false`. Al terminar, puedes borrar `../../cafe.json`.

## Si algo falla

Cuando algo falla, la consola dice «La demostración se detuvo:» y por qué.

| La consola dice | Qué hacer |
|---|---|
| `No entiendo «--con-prueba»: las opciones son --entidad <ruta del JSON> y --con-pruebas.` | Revisa cómo se escribió la opción |
| `npm se quedó con --entidad: falta «--» antes de las opciones…` | Escribe `--` después de `npm run demo:otra-tematica` |
| `Falta la ruta del JSON después de --entidad…` | Escribe la ruta después de `--entidad` |
| `No encuentro …` | La ruta se lee desde la carpeta donde corres el comando. Desde `frontend/`, los ejemplos están en `../generador/pruebas/ejemplos/` |
| `… no es un JSON válido: …` | Revisa la coma antes del atributo nuevo y las llaves. El mensaje dice en qué línea está el error |
| `… ya es una entidad de InspectIA …` | El archivo se llama como una entidad de la app, como `empresa.json`. Ponle otro nombre al archivo y a su `entidad` |
| `Falta el entorno del generador …` | Créalo como dice `generador/README.md` |
| Un error de `docker compose`, o que no se puede conectar con Docker | Abre Docker Desktop y espera a que arranque. Si no arranca, ciérralo y ábrelo otra vez o reinicia el PC. No uses «Reset to factory defaults»: borra todos los contenedores y volúmenes, también el de la base de desarrollo |
| `… terminó con 1:` y una lista de errores del generador | El JSON tiene algo que el generador no acepta, y la lista dice qué. El formato está en `specs/entidades/README.md` |

Si Docker no arranca, las pruebas del generador muestran lo mismo sin encender nada. Validan `cafe.json` y `producto.json` y generan su tabla, su API y su pantalla. Desde la raíz del repositorio:

```bash
cd generador
.venv/Scripts/python -m pytest pruebas/test_otra_tematica.py -v
```
