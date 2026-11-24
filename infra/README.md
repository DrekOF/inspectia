<!--
Nombre: README.md (infra)
Descripción: Qué va en la carpeta infra/ de InspectIA, cómo reiniciar la base local, dónde quedan las fotos en tu PC, cómo crear el administrador de la plataforma y restablecer su contraseña, cómo probar la sesión, los lotes y las fotos en local y cómo usan infra/ las pruebas de punta a punta y la demostración de otra temática.
Fecha de creación: 12/10/2026
Actualización: 23/11/2026
Autor: Equipo InspectIA
Versión: 1.13
-->

# infra

Infraestructura y despliegue de InspectIA.

Qué hay hoy:

- `docker-compose.yml`: PostgreSQL 18 para desarrollo local. El servidor y el servicio de IA se agregan cuando existan.
- `.env.example`: variables locales de la base de datos, la clave de los tokens del inicio de sesión, el primer administrador de la plataforma y, si quieres otra, la carpeta de las fotos. Cópialo a `infra/.env`, que no se sube al repositorio, y escribe tus valores.
- `bd/crear-admin-plataforma.sql`, con sus envoltorios `.sh` y `.ps1`: crea el primer administrador de la plataforma (RF-01, RP-15). Las tablas no se crean aquí: las crea Flyway al arrancar el backend, con la migración que genera `generador/` (RP-23).
- `bd/restablecer-admin-plataforma.sql`, con sus envoltorios `.sh` y `.ps1`: restablece la contraseña del administrador de la plataforma si la olvida (D-30).

Qué irá aquí:

- Otros scripts de la base de datos, como la copia diaria a S3 (D-16).
- Scripts de compilación y prueba del frontend y del backend (RP-16), y de gestión del repositorio en GitHub (RP-17).
- Configuración del despliegue en AWS: una EC2 t4g con Docker Compose (D-16).

Los flujos de GitHub Actions (RP-18 y RP-19) tienen que estar en `.github/workflows/`, en la raíz del repositorio, porque GitHub solo los busca ahí. La integración continua ya está: `.github/workflows/ci.yml` (ver el README principal); sus pruebas de punta a punta encienden el entorno aislado, que usa `infra/docker-compose.yml` y el script del administrador de la plataforma. El despliegue, cuando llegue, se conectará a AWS por OIDC, sin claves guardadas.

La EC2 t4g es ARM (Graviton), así que toda imagen debe funcionar en amd64 y en arm64.

## Base de datos local

Desde la raíz del repositorio, la base se enciende con `docker compose -f infra/docker-compose.yml up -d` y se apaga con `docker compose -f infra/docker-compose.yml down`. Los demás comandos están en la sección Comandos de `CLAUDE.md`.

Si cambia `V1__esquema_inicial.sql`, quien ya tenga la base local debe reiniciarla: Flyway no arranca si una migración ya aplicada cambió (P-16), aunque sea solo su header. Pasó cuando RF-03 agregó la columna `usuario.sesiones_desde`, cuando RF-13 subió el generador a la 1.6, cuando P-18 la cambió por `usuario.version_sesion` (D-35), cuando la pantalla de usuarios (RF-03) lo subió a la 1.9, cuando la de rangos (RF-13) lo subió a la 1.10 y cuando el resultado del lote (RF-10) agregó los recall de cada clase del modelo, el margen del lote y el índice de la cola de análisis; con la 1.6, la 1.9 y la 1.10 solo cambió el header. Esto borra los datos locales:

1. `docker compose -f infra/docker-compose.yml down -v`
2. `docker compose -f infra/docker-compose.yml up -d`
3. Arranca el backend con `clean`, para que no queden clases viejas de antes de regenerar: `./mvnw clean spring-boot:run` en `backend/` (en PowerShell, `.\mvnw clean spring-boot:run`). Después vuelve a crear el administrador con el script (ver «Primer administrador de la plataforma»).

Borra también la carpeta de las fotos (ver «Fotos en tu PC»): sus archivos eran de los lotes que se borraron.

## Fotos en tu PC

Mientras no haya despliegue en AWS, el backend guarda las fotos de las muestras en una carpeta de tu PC, fuera del repositorio, en lugar de S3 (D-41): por defecto, `<tu carpeta de usuario>/.inspectia/fotos` (en Windows, `C:\Users\<tu usuario>\.inspectia\fotos`). Al arrancar, el backend la crea si no existe y escribe en su registro cuál usa. No tienes que cambiar nada en `infra/.env`.

- **Otra carpeta:** escribe en `infra/.env` la línea `INSPECTIA_FOTOS_CARPETA` (está comentada en `.env.example`), con la ruta completa, con «/» también en Windows y sin tildes, por ejemplo `INSPECTIA_FOTOS_CARPETA=C:/Users/tu-usuario/fotos-inspectia`. Una ruta relativa no deja arrancar el backend, para que las fotos nunca queden dentro del repositorio.
- **Los enlaces:** el backend firma los enlaces para subir y ver cada foto, como haría S3: `/api/v1/archivos/<empresa>/<lote>/<archivo>?vence=…&firma=…`. No piden sesión, porque un `<img>` no manda el token: los autoriza su firma, que vence a los 10 minutos para subir y a los 15 para ver. Sin firma, con otra firma o vencida, responden 403. La clave de la firma se crea al azar cada vez que arranca el backend y no se guarda: después de reiniciarlo, la página pide enlaces nuevos sola.
- **Las fotos sin confirmar** (una subida que no terminó) pueden quedar en la carpeta. Si reinicias la base, bórrala completa.
- **Las pruebas** no usan esta carpeta: las de integración usan una temporal por corrida, y las de punta a punta, otra en la carpeta temporal del sistema, que se borra al apagar su entorno.

## Base de las pruebas de punta a punta

Las pruebas de punta a punta del frontend (`npm run e2e:playwright` y `npm run e2e:selenium`) encienden su propia base con este `docker-compose.yml`, pero en otro proyecto (`inspectia-e2e`), con su propio volumen, un puerto libre y una contraseña nueva en cada corrida. No leen `infra/.env` ni tocan la base local. El administrador de la plataforma lo crea el mismo script de `infra/bd`, apuntado a ese proyecto con `COMPOSE_PROJECT_NAME` y `COMPOSE_ENV_FILES`; antes de correrlo, el entorno revisa que docker compose llegue a esa base y no a la local. Al terminar se borra con `down -v`, y si una corrida se interrumpe, la siguiente borra lo que haya quedado. Ver `frontend/README.md`.

La demostración de otra temática (`npm run demo:otra-tematica`, desde `frontend/`) enciende la suya igual, en el proyecto `inspectia-demo`. Ctrl+C la borra con `down -v`; después, `docker compose ls` ya no lo muestra. Si queda encendido, por ejemplo porque se cerró la terminal, la próxima corrida lo borra al empezar.

## Clave de los tokens

El backend firma los tokens del inicio de sesión con la clave de `INSPECTIA_JWT_CLAVE` (D-28): 32 bytes al azar en Base64. Sin ella no arranca y se detiene con «Falta INSPECTIA_JWT_CLAVE, la clave de los tokens». Créala una vez y escríbela en `infra/.env`:

- PowerShell: `$b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)`
- Git Bash, Linux o macOS: `openssl rand -base64 32`

No la compartas ni la subas al repositorio. Si cambia, las sesiones abiertas dejan de servir y hay que iniciar sesión de nuevo. En la EC2 vendrá de AWS SSM.

## Primer administrador de la plataforma

RF-01 pide que el primer administrador de la plataforma lo cree un script de la base de datos. Desde la raíz del repositorio:

1. Escribe en `infra/.env` su nombre, su correo y su contraseña inicial (`INSPECTIA_ADMIN_*`; ver `.env.example`).
2. Enciende la base y arranca el backend una vez (`./mvnw spring-boot:run` en `backend/`), para que Flyway cree las tablas.
3. Corre el script:
   - Windows: `powershell -ExecutionPolicy Bypass -File infra\bd\crear-admin-plataforma.ps1`
   - Linux, macOS o Git Bash: `sh infra/bd/crear-admin-plataforma.sh`
4. Borra la contraseña de `infra/.env`.

Qué hace:

- Valida los datos con las reglas de `specs/entidades/usuario.json`. Si algo falta o no cumple, se detiene con un mensaje y no guarda nada.
- Guarda la contraseña en hash BCrypt con pgcrypto, el formato que verifica Spring Security.
- Si el administrador ya existe, no cambia nada, así que se puede correr varias veces. Si el correo es de un usuario de una empresa, se detiene.
- La contraseña no pasa por la línea de comandos ni queda en el registro de PostgreSQL.

`backend/src/test/java/co/edu/usco/inspectia/CrearAdminPlataformaIT.java` lo prueba con Testcontainers. En la EC2, el despliegue tomará los datos de AWS SSM en lugar de `infra/.env`.

## Restablecer la contraseña del administrador de la plataforma

Si el administrador de la plataforma olvida su contraseña, la restablece el equipo con este script, que solo se corre con acceso al servidor (D-30). Los demás no lo necesitan: cada usuario cambia la suya desde la app (RF-18); la de un usuario de una empresa la restablece su administrador (RF-03), y la de un administrador de empresa, el administrador de la plataforma (RF-02) u otro administrador de su empresa.

Con la base encendida, desde la raíz del repositorio:

- Windows: `powershell -ExecutionPolicy Bypass -File infra\bd\restablecer-admin-plataforma.ps1`
- Linux, macOS, Git Bash o el servidor: `sh infra/bd/restablecer-admin-plataforma.sh`

Qué pide y qué hace:

- El correo sale de `INSPECTIA_ADMIN_CORREO`, del entorno o de `infra/.env`. Si no está en ninguno, lo pide, y se ve al escribirlo: así se puede correr en el servidor sin crear archivos.
- La contraseña nueva la pide dos veces y sin mostrarla, así que no queda en ningún archivo ni en el historial de la consola. Pasa por las reglas de siempre: de 8 a 64 caracteres, como máximo 72 bytes y no solo espacios.
- Guarda la contraseña nueva, sube la versión de sesión del administrador, así sus sesiones abiertas dejan de servir, y quita su bloqueo por intentos fallidos, todo en una sola sentencia (D-30, D-35). No depende de la hora de la base ni de la del backend. No reactiva nada.
- Si el correo es de un usuario de una empresa, o no existe, se detiene sin cambiar nada.
- La contraseña nueva viaja en `INSPECTIA_ADMIN_CONTRASENA_NUEVA`, una variable que nunca va en `infra/.env`. Si viajara en `INSPECTIA_ADMIN_CONTRASENA` y quedara vacía, docker compose tomaría de `infra/.env` la del script de creación.

`backend/src/test/java/co/edu/usco/inspectia/RestablecerAdminPlataformaIT.java` lo prueba con Testcontainers.

## Probar el inicio de sesión en Swagger UI

Con el backend arrancado y el administrador creado:

1. Abre http://localhost:8080/swagger-ui.html.
2. En «Sesión», abre `POST /api/v1/sesion`, pulsa «Try it out», escribe en el cuerpo el correo y la contraseña del administrador y pulsa «Execute». La respuesta 200 trae el `token`, cuándo `vence` (8 horas después) y el `usuario` con su rol.
3. Copia el valor de `token`, sin las comillas. Pulsa «Authorize» (arriba a la derecha), pégalo en «Value», pulsa «Authorize» y después «Close».
4. Abre `GET /api/v1/sesion`, pulsa «Try it out» y «Execute»: responde 200 con tu usuario. Con el mismo token también responde la API de empresas.

Con datos incorrectos, el inicio de sesión responde 401 «Correo o contraseña incorrectos». Después de 5 fallos seguidos, la cuenta se bloquea 15 minutos, también para la contraseña correcta.

Para los rangos de recomendación (RF-13), crea una empresa con `POST /api/v1/empresas`, que trae su administrador, e inicia sesión con él. Ahí:

- `GET /api/v1/rangos-recomendacion` lista las 4 filas.
- `PUT /api/v1/rangos-recomendacion` con `filas` las edita todas juntas.
- `POST /api/v1/rangos-recomendacion/restaurar-por-defecto` vuelve a los valores de D-10.

El administrador de la plataforma recibe 403 en esas rutas, porque no tiene empresa.

Para los lotes (RF-04 y RF-14), con el administrador de esa empresa crea un usuario final con `POST /api/v1/usuarios` e inicia sesión con él. Ahí:

- `POST /api/v1/lotes` con `codigo`, `fechaRecepcion` (aaaa-mm-dd, no posterior a hoy en Colombia ni de hace más de un año), `origen` y `observaciones` registra un lote pendiente de análisis. Un código que ya existe en la empresa responde 409.
- `GET /api/v1/lotes?pagina=0&tamano=20` trae el historial de la empresa, del último movimiento al más antiguo, y `GET /api/v1/lotes/{id}`, un lote.
- `GET /api/v1/lotes/origenes?texto=paraiso` sugiere las fincas y los proveedores que ya usó la empresa.

El administrador de empresa también ve el historial y cada lote, pero no registra lotes: recibe 403. Un lote de otra empresa responde 404, como si no existiera (RNF-03).

Para las fotos de un lote pendiente (RF-05), con el mismo usuario final y el `id` del lote, en tres pasos, como la página:

1. `POST /api/v1/lotes/{id}/fotos/1/subida` con `tipo` (`image/jpeg` o `image/png`) y `tamanoBytes` (el tamaño exacto del archivo, de hasta 10.485.760). Responde 201 con la `llave` y la `url` firmada para subirla.
2. Sube el archivo a esa `url` con `PUT`, sin el token y con el mismo `Content-Type`. Swagger no sube archivos así; desde la carpeta de la foto, en Git Bash: `curl -X PUT --data-binary "@foto.jpg" -H "Content-Type: image/jpeg" "http://localhost:8080<url>"`. En PowerShell es igual, con `curl.exe` en vez de `curl`. El archivo y la `url` van entre comillas: sin ellas, PowerShell lee `@foto` como una variable y `&` como otro comando. Responde 200.
3. `PUT /api/v1/lotes/{id}/fotos/1` con la `llave`: confirma la foto, que queda «Cargada», y responde con su enlace de vista (`vista.url`). Ese enlace es relativo: se abre en el navegador con `http://localhost:8080` delante.

`GET /api/v1/lotes/{id}/fotos` trae las fotos de la muestra en curso, cada una con un enlace de vista nuevo. Las fotos van de la 1 a la 3; subir otra con el mismo número la reemplaza y borra la anterior. Un archivo que no es JPG ni PNG, o de más de 10 MB, responde 400 con el mensaje de qué hacer. El administrador de empresa recibe 403 en estas rutas, y un lote de otra empresa, 404.

Para analizar la muestra (RF-10 a RF-12), con 2 o 3 fotos confirmadas:

1. `POST /api/v1/lotes/{id}/analisis` la envía: responde 202 con la muestra EN_ESPERA. Con menos de 2 fotos responde 400 en `fotos`, y con el lote ya analizado, 409. Solo lo usa el usuario final.
2. `GET /api/v1/lotes/{id}/analisis` dice en qué va: EN_ESPERA, EN_PROCESO o TERMINADA. `fallo` es verdadero si el análisis no terminó y la muestra volvió a quedar pendiente.
3. `GET /api/v1/lotes/{id}/resultado` trae el resultado guardado: el porcentaje corregido, su margen, la categoría, la recomendación y si sugiere otra muestra, y cada muestra con sus fotos y sus granos (sin la confianza). Lo ven el usuario final y el administrador de empresa.

Mientras no esté el servicio de IA (PR 4), los granos los da un clasificador falso, que registra su modelo (`clasificador-falso` 1.0) al arrancar el backend. Es predecible, para poder probar cada caso con la foto que se sube: da 50 granos por foto, son no sanos tantos como el ancho de la foto módulo 50, y dudosos tantos como el alto módulo 50. Dos fotos de 106 × 100 y 107 × 100 px dan 87 de 100 sanos: 90,6 %, Estándar, con sugerencia de otra muestra. Con `INSPECTIA_IA_FALSO_DEMORA` (por ejemplo `INSPECTIA_IA_FALSO_DEMORA=3s` en `infra/.env`) tarda más, para ver la espera en la página.

Para cambiar tu propia contraseña (RF-18), abre `PUT /api/v1/sesion/contrasena` con `contrasenaActual` y `contrasenaNueva`. La respuesta 200 trae un token nuevo, que vence a la misma hora que el anterior: pégalo en «Authorize», porque el anterior y los de tus demás sesiones responden 401 «Tu contraseña cambió». Una contraseña actual errada responde 400 y cuenta para el bloqueo; desde el quinto fallo seguido responde 429 hasta que termine.
