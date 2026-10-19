<!--
Nombre: README.md (infra)
Descripción: Qué va en la carpeta infra/ de InspectIA, cómo reiniciar la base local y cómo probar el inicio de sesión en local.
Fecha de creación: 12/10/2026
Actualización: 19/10/2026
Autor: Equipo InspectIA
Versión: 1.3
-->

# infra

Infraestructura y despliegue de InspectIA.

Qué hay hoy:

- `docker-compose.yml`: PostgreSQL 18 para desarrollo local. El servidor y el servicio de IA se agregan cuando existan.
- `.env.example`: variables locales de la base de datos, la clave de los tokens del inicio de sesión y el primer administrador de la plataforma. Cópialo a `infra/.env`, que no se sube al repositorio, y escribe tus valores.
- `bd/crear-admin-plataforma.sql`, con sus envoltorios `.sh` y `.ps1`: crea el primer administrador de la plataforma (RF-01, RP-15). Las tablas no se crean aquí: las crea Flyway al arrancar el backend, con la migración que genera `generador/` (RP-23).

Qué irá aquí:

- Otros scripts de la base de datos, como la copia diaria a S3 (D-16).
- Scripts de compilación y prueba del frontend y del backend (RP-16), y de gestión del repositorio en GitHub (RP-17).
- Configuración del despliegue en AWS: una EC2 t4g con Docker Compose (D-16).

Los flujos de GitHub Actions (RP-18 y RP-19) tienen que estar en `.github/workflows/`, en la raíz del repositorio, porque GitHub solo los busca ahí. Llaman a los scripts de esta carpeta. GitHub Actions se conecta a AWS por OIDC, sin claves guardadas.

La EC2 t4g es ARM (Graviton), así que toda imagen debe funcionar en amd64 y en arm64.

## Base de datos local

Desde la raíz del repositorio, la base se enciende con `docker compose -f infra/docker-compose.yml up -d` y se apaga con `docker compose -f infra/docker-compose.yml down`. Los demás comandos están en la sección Comandos de `CLAUDE.md`.

Si cambia `V1__esquema_inicial.sql`, como cuando RF-03 agregó la columna `usuario.sesiones_desde`, quien ya tenga la base local debe reiniciarla: Flyway no arranca si una migración ya aplicada cambió (P-16). Esto borra los datos locales:

1. `docker compose -f infra/docker-compose.yml down -v`
2. `docker compose -f infra/docker-compose.yml up -d`
3. Arranca el backend (`./mvnw spring-boot:run` en `backend/`) y vuelve a crear el administrador con el script (ver «Primer administrador de la plataforma»).

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

## Probar el inicio de sesión en Swagger UI

Con el backend arrancado y el administrador creado:

1. Abre http://localhost:8080/swagger-ui.html.
2. En «Sesión», abre `POST /api/v1/sesion`, pulsa «Try it out», escribe en el cuerpo el correo y la contraseña del administrador y pulsa «Execute». La respuesta 200 trae el `token`, cuándo `vence` (8 horas después) y el `usuario` con su rol.
3. Copia el valor de `token`, sin las comillas. Pulsa «Authorize» (arriba a la derecha), pégalo en «Value», pulsa «Authorize» y después «Close».
4. Abre `GET /api/v1/sesion`, pulsa «Try it out» y «Execute»: responde 200 con tu usuario. Con el mismo token también responde la API de empresas.

Con datos incorrectos, el inicio de sesión responde 401 «Correo o contraseña incorrectos». Después de 5 fallos seguidos, la cuenta se bloquea 15 minutos, también para la contraseña correcta.
