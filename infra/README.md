<!--
Nombre: README.md (infra)
Descripción: Qué va en la carpeta infra/ de InspectIA, cómo reiniciar la base local, dónde quedan las fotos en tu PC, cómo crear el administrador de la plataforma y restablecer su contraseña, cómo probar la sesión, los lotes y las fotos en local, cómo usan infra/ las pruebas de punta a punta y la demostración de otra temática, y el despliegue en AWS (D-16): la guía de la consola, paso a paso, la CI que despliega, el primer administrador en producción, las pruebas y la copia diaria de la base con su restauración.
Fecha de creación: 12/10/2026
Actualización: 24/11/2026
Autor: Equipo InspectIA
Versión: 1.15
-->

# infra

Infraestructura y despliegue de InspectIA.

Qué hay:

- `docker-compose.yml`: PostgreSQL 18 para desarrollo local.
- `.env.example`: variables locales de la base de datos, la clave de los tokens del inicio de sesión, el primer administrador de la plataforma y, si quieres otra, la carpeta de las fotos. Cópialo a `infra/.env`, que no se sube al repositorio, y escribe tus valores.
- `bd/crear-admin-plataforma.sql`, con sus envoltorios `.sh` y `.ps1`: crea el primer administrador de la plataforma (RF-01, RP-15). Las tablas no se crean aquí: las crea Flyway al arrancar el backend, con la migración que genera `generador/` (RP-23).
- `bd/restablecer-admin-plataforma.sql`, con sus envoltorios `.sh` y `.ps1`: restablece la contraseña del administrador de la plataforma si la olvida (D-30).
- `produccion/`: lo que corre en la instancia de AWS (D-16), y que cada despliegue deja en `/opt/inspectia`: el `docker-compose.yml` de producción y sus scripts (`actualizar.sh`, `respaldar.sh`, `restaurar-respaldo.sh`, `admin-plataforma.sh` y `comun.sh`). Ver «Despliegue en AWS».
- `aws/`: lo que se usa en la consola de AWS y en la CI: el script de arranque de la instancia (`datos-de-usuario.sh`), la CloudFront Function de las rutas de React (`cloudfront/rutas-react.js`), la política de seguridad de contenido (`cloudfront/csp.txt`), las políticas IAM (`iam/`), la configuración de los buckets (`s3/`), la regla de ECR (`ecr/`) y el paso de la CI que actualiza la instancia (`actualizar-instancia.sh`). Los JSON no llevan header porque la consola los pide tal cual: su header es esta lista.
- `probar.sh`: las pruebas de la infraestructura, sin red (RP-16): `bash infra/probar.sh`. Revisa los scripts, los JSON, el compose de producción, la CloudFront Function, las políticas IAM y el script de arranque. La CI lo corre en el job «Infraestructura».

Los flujos de GitHub Actions (RP-18 y RP-19) tienen que estar en `.github/workflows/`, en la raíz del repositorio, porque GitHub solo los busca ahí: `.github/workflows/ci.yml` prueba todo en cada PR y en cada push a `main` y, en `main`, despliega en AWS (ver «Despliegue en AWS»). Sus pruebas de punta a punta encienden el entorno aislado, que usa `infra/docker-compose.yml` y el script del administrador de la plataforma.

La EC2 t4g es ARM (Graviton), así que toda imagen debe funcionar en amd64 y en arm64. La CI las construye en runners arm64.

## Base de datos local

Desde la raíz del repositorio, la base se enciende con `docker compose -f infra/docker-compose.yml up -d` y se apaga con `docker compose -f infra/docker-compose.yml down`. Los demás comandos están en la sección Comandos de `CLAUDE.md`.

Si cambia `V1__esquema_inicial.sql`, quien ya tenga la base local debe reiniciarla: Flyway no arranca si una migración ya aplicada cambió (P-16), aunque sea solo su header. Pasó cuando RF-03 agregó la columna `usuario.sesiones_desde`, cuando RF-13 subió el generador a la 1.6, cuando P-18 la cambió por `usuario.version_sesion` (D-35), cuando la pantalla de usuarios (RF-03) lo subió a la 1.9, cuando la de rangos (RF-13) lo subió a la 1.10 y cuando el resultado del lote (RF-10) agregó los recall de cada clase del modelo, el margen del lote y el índice de la cola de análisis, y cuando la segunda muestra (PR 3b) dejó lista la `V1` del resto del módulo de análisis: por qué volvió una muestra a pendiente (D-44), la tabla `region_pegada` de los granos pegados (D-45), cuántos granos encontró la revisión de cada foto (D-46), el límite de la sugerencia con su categoría (D-47), la precisión y la matriz de confusión del modelo (D-48) y la versión de su plantilla en el header (D-49); con la 1.6, la 1.9 y la 1.10 solo cambió el header. Esto borra los datos locales:

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

No la compartas ni la subas al repositorio. Si cambia, las sesiones abiertas dejan de servir y hay que iniciar sesión de nuevo. En AWS sale del parámetro `/inspectia/jwt-clave` de SSM (ver «Despliegue en AWS»).

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

`backend/src/test/java/co/edu/usco/inspectia/CrearAdminPlataformaIT.java` lo prueba con Testcontainers. En AWS se corre otro envoltorio del mismo SQL, que pide los datos por teclado: ver «Primer administrador en producción».

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
- `GET /api/v1/lotes?pagina=0&tamano=20` trae el historial de la empresa, del último movimiento al más antiguo, y `GET /api/v1/lotes/{id}`, un lote. El historial acepta los filtros de RF-14 C2, solos o combinados: `desde` y `hasta` (aaaa-mm-dd, la fecha del resultado en días de Colombia, con las dos incluidas), `estado` (`PENDIENTE` o `ANALIZADO`), `categoria` (`PREMIUM`, `ESTANDAR`, `CORRIENTE` o `FUERA_DE_NORMA`), `codigo` (se busca dentro del código, sin distinguir mayúsculas) y `usuario` (el `id` de quien registró el lote). Un «hasta» anterior a «desde» responde 400.
- `GET /api/v1/lotes/registradores` trae el `id` y el nombre de quienes registraron lotes de la empresa, también los desactivados, para el filtro «Usuario».
- `GET /api/v1/lotes/origenes?texto=paraiso` sugiere las fincas y los proveedores que ya usó la empresa.

El administrador de empresa también ve el historial y cada lote, pero no registra lotes: recibe 403. Un lote de otra empresa responde 404, como si no existiera (RNF-03).

Para las fotos de un lote pendiente (RF-05), con el mismo usuario final y el `id` del lote, en tres pasos, como la página:

1. `POST /api/v1/lotes/{id}/fotos/1/subida` con `muestra` (1, o 2 para la segunda muestra), `tipo` (`image/jpeg` o `image/png`) y `tamanoBytes` (el tamaño exacto del archivo, de hasta 10.485.760). Responde 201 con la `llave` y la `url` firmada para subirla. Si `muestra` no es la que recibe las fotos del lote en ese momento, responde 409, y lo mismo al confirmar una llave pedida para otra muestra (D-25).
2. Sube el archivo a esa `url` con `PUT`, sin el token y con el mismo `Content-Type`. Swagger no sube archivos así; desde la carpeta de la foto, en Git Bash: `curl -X PUT --data-binary "@foto.jpg" -H "Content-Type: image/jpeg" "http://localhost:8080<url>"`. En PowerShell es igual, con `curl.exe` en vez de `curl`. El archivo y la `url` van entre comillas: sin ellas, PowerShell lee `@foto` como una variable y `&` como otro comando. Responde 200.
3. `PUT /api/v1/lotes/{id}/fotos/1` con la `llave`: confirma la foto, que queda «Cargada», y responde con su enlace de vista (`vista.url`). Ese enlace es relativo: se abre en el navegador con `http://localhost:8080` delante.

`GET /api/v1/lotes/{id}/fotos` trae las fotos de la muestra en curso, cada una con un enlace de vista nuevo. Las fotos van de la 1 a la 3; subir otra con el mismo número la reemplaza y borra la anterior. Un archivo que no es JPG ni PNG, o de más de 10 MB, responde 400 con el mensaje de qué hacer. El administrador de empresa recibe 403 en estas rutas, y un lote de otra empresa, 404.

Para analizar la muestra (RF-10 a RF-12), con 2 o 3 fotos confirmadas:

1. `POST /api/v1/lotes/{id}/analisis` la envía: responde 202 con la muestra EN_ESPERA. Con menos de 2 fotos responde 400 en `fotos`, y con el lote ya analizado, 409, salvo la segunda muestra (ver abajo). Solo lo usa el usuario final.
2. `GET /api/v1/lotes/{id}/analisis` dice en qué va: EN_ESPERA, EN_PROCESO o TERMINADA. `fallo` es verdadero si el análisis no terminó y la muestra volvió a quedar pendiente.
3. `GET /api/v1/lotes/{id}/resultado` trae el resultado guardado: el porcentaje corregido, su margen, la categoría, la recomendación y si sugiere otra muestra, con el límite más cercano y su categoría (`limiteSugerencia` y `categoriaDelLimite`, D-47), y cada muestra con sus fotos y sus granos (sin la confianza). Lo ven el usuario final y el administrador de empresa.

Si el resultado sugiere otra muestra (D-24), el usuario final analiza la segunda con los mismos tres pasos de las fotos y los mismos de arriba: la primera foto confirmada crea la muestra 2, sin tocar la 1, y `GET …/fotos` trae las de la 2. El lote sigue analizado con el resultado de la 1 hasta que la 2 termina; entonces el resultado se recalcula con los granos de las dos y ya no sugiere otra (D-25). Un lote sin sugerencia, o con sus 2 muestras, responde 409 a las fotos y al envío.

Mientras no esté el servicio de IA (PR 4), los granos los da un clasificador falso, que registra su modelo (`clasificador-falso` 1.0) al arrancar el backend. Es predecible, para poder probar cada caso con la foto que se sube: da 50 granos por foto, son no sanos tantos como el ancho de la foto módulo 50, y dudosos tantos como el alto módulo 50. Dos fotos de 106 × 100 y 107 × 100 px dan 87 de 100 sanos: 90,6 %, Estándar, con sugerencia de otra muestra, a 0,6 puntos del límite de Estándar (90 %). Una segunda muestra con 108 × 100 y 107 × 100 px suma 172 de 200: 88,3 %, Corriente. Con `INSPECTIA_IA_FALSO_DEMORA` (por ejemplo `INSPECTIA_IA_FALSO_DEMORA=3s` en `infra/.env`) tarda más, para ver la espera en la página.

Para cambiar tu propia contraseña (RF-18), abre `PUT /api/v1/sesion/contrasena` con `contrasenaActual` y `contrasenaNueva`. La respuesta 200 trae un token nuevo, que vence a la misma hora que el anterior: pégalo en «Authorize», porque el anterior y los de tus demás sesiones responden 401 «Tu contraseña cambió». Una contraseña actual errada responde 400 y cuenta para el bloqueo; desde el quinto fallo seguido responde 429 hasta que termine.

## Despliegue en AWS (D-16)

Todo corre en la región **us-east-1 (N. Virginia)**, como en `02_Analisis_Diseno/InspectIA_Despliegue_AWS.pdf`:

- **CloudFront** entrega la página desde el bucket `inspectia-web-<cuenta>` y lleva `/api/*` a la instancia por HTTP. Las rutas de React (`/lotes/12`) las resuelve la CloudFront Function `inspectia-rutas`, no las páginas de error: así un 404 de la API sigue siendo un 404 en JSON.
- **La instancia** `inspectia-servidor` (t4g.medium, Amazon Linux 2023 arm64, sin llave SSH, con IP elástica) corre con Docker Compose PostgreSQL 18, el backend y el servicio de IA. Solo el backend se publica, en el puerto 80, y el grupo de seguridad solo deja entrar a CloudFront.
- **S3** `inspectia-archivos-<cuenta>` guarda las fotos (`fotos/`), los reportes PDF (`reportes/`), los 5 modelos de Julián (`modelos/`) y la copia diaria de la base (`respaldos/`).
- **ECR** guarda las imágenes `inspectia/backend` e `inspectia/ia-servicio`, con el SHA del commit como etiqueta.
- **SSM Parameter Store** guarda los dos secretos: `/inspectia/jwt-clave` y `/inspectia/postgres-contrasena`. La instancia los lee al arrancar con su rol y los deja solo en memoria.
- **CloudWatch** recibe los registros de los contenedores y del servidor, y las métricas de memoria, disco y swap; sus alarmas avisan por **SNS** (`inspectia-alertas`) al correo del equipo. **EventBridge Scheduler** apaga y enciende la instancia y **AWS Budgets** avisa si el gasto pasa del límite.

Nada de esto se crea con código (ni Terraform ni CloudFormation): se crea una vez en la consola, con la guía de abajo. Lo que sí está en el repositorio es lo que se pega en la consola (`infra/aws/`), lo que corre en la instancia (`infra/produccion/`) y la CI que despliega.

### Cómo despliega la CI

`.github/workflows/ci.yml`, en cada push a `main` (o a mano, desde Actions, sobre `main`):

1. Corren las pruebas de siempre y el job **Infraestructura** (`bash infra/probar.sh`).
2. El job **Imágenes Docker** construye las imágenes del backend y del servicio de IA para arm64 en runners arm64 y las sube a ECR con la etiqueta del commit. En un PR solo las construye. La del servicio de IA solo se construye si existe `ia-servicio/Dockerfile`.
3. Si todo pasó, el job **Despliegue en AWS**:
   - revisa que las dos imágenes del commit estén en ECR;
   - enciende la instancia si estaba apagada (el horario la vuelve a apagar a su hora);
   - le manda por SSM (Run Command, sin SSH) `infra/produccion` y los SQL de `infra/bd`, que quedan en `/opt/inspectia`, y corre `actualizar.sh` con la etiqueta del commit: lee los secretos de SSM, baja las imágenes de ECR y levanta todo, esperando a que los tres servicios estén sanos. Solo cuando quedan sanos anota la etiqueta nueva en `/opt/inspectia/.env`. Si no quedan sanos, vuelve sola a la versión anterior (la que dice `.env`), el job falla y la página nueva no se publica: el sitio sigue con la versión anterior. Si la nueva aplicó una migración de Flyway, puede que la anterior no arranque con esa base: el registro del job lo dice y hay que seguir «Volver atrás»;
   - compila la página y la sube al bucket web (`assets/` con caché de un año, `index.html` sin caché), invalida CloudFront y comprueba por CloudFront la página, una ruta de React y la API.
4. Si algo falla en `main`, el job **Aviso de fallas** abre el issue, como siempre.

GitHub entra a AWS con el rol `inspectia-github` por OIDC: no hay claves de AWS en GitHub. Mientras falte alguna de las variables del repositorio (paso 15), el despliegue se salta y la CI sigue en verde.

### Guía de la consola, paso a paso

Hazlo en este orden, con un usuario administrador de la cuenta (no con la cuenta raíz) y con la región **us-east-1** elegida arriba a la derecha. Cuando un archivo diga `<cuenta>`, `<instancia>`, `<distribucion>` o `<dominio-de-cloudfront>`, cámbialo por el valor real: el ID de la cuenta (12 dígitos, arriba a la derecha), el ID de la instancia (`i-…`), el ID de la distribución (`E…`) y su dominio (`d….cloudfront.net`).

**1. AWS Budgets (el primer día).** Billing and Cost Management → Budgets → Create budget → Use a template → *Monthly cost budget*: nombre `inspectia-mensual`, monto **US$25** (lo previsto es ≈ US$16 apagándola de noche y ≈ US$31 encendida todo el mes) y los correos del equipo. AWS avisa al 85 % y al 100 % del gasto real, y si el pronóstico pasa del 100 %.

**2. SNS.** Simple Notification Service → Topics → Create topic → *Standard*, nombre `inspectia-alertas`. Después, Create subscription → protocolo *Email* → el correo del equipo. Cada correo debe aceptar el mensaje de confirmación que le llega.

**3. Los dos buckets de S3.** S3 → Create bucket, dos veces, en us-east-1, con *Block all public access* encendido, *ACLs disabled* y el cifrado por defecto (SSE-S3):

- `inspectia-web-<cuenta>`: la página. Queda vacío: lo llena la CI. Su política la da CloudFront en el paso 9.
- `inspectia-archivos-<cuenta>`: fotos, reportes, modelos y respaldos. Después de crearlo:
  - Permissions → Bucket policy: pega `infra/aws/s3/politica-archivos.json` (rechaza todo lo que no llegue por HTTPS).
  - Management → Create lifecycle rule:
    - `respaldos-35-dias`: prefijo `respaldos/`, *Expire current versions of objects* a los **35 días**.
    - `subidas-incompletas`: todo el bucket, *Delete expired object delete markers or incomplete multipart uploads* → incompletas a los **7 días**.
  - El CORS se pone en el paso 9, cuando ya está el dominio de CloudFront.

**4. ECR.** Elastic Container Registry → Private repositories → Create repository, dos veces: `inspectia/backend` e `inspectia/ia-servicio`, con *Tag immutability* en **Mutable** (así volver a correr un despliegue no falla) y *Scan on push* encendido. En cada uno, Lifecycle policy → Edit JSON → pega `infra/aws/ecr/ciclo-de-vida.json`: guarda las 15 imágenes más recientes.

**5. Los dos secretos en SSM, sin verlos ni pegarlos en ningún lado.** Abre **CloudShell** (el ícono `>_` de la barra de arriba de la consola) y corre:

```sh
aws ssm put-parameter --region us-east-1 --name /inspectia/jwt-clave --type SecureString --value "$(openssl rand -base64 32)"
aws ssm put-parameter --region us-east-1 --name /inspectia/postgres-contrasena --type SecureString --value "$(openssl rand -hex 32)"
```

Los valores salen de `openssl` directo a SSM: nadie los ve ni quedan en un chat. Quedan cifrados con la llave por defecto de SSM (`aws/ssm`). `actualizar.sh` revisa su forma y no arranca si no sirven: la clave, 32 bytes o más en Base64; la contraseña, de 20 caracteres o más, solo letras sin tildes, números y `. _ ~ + -`.

- **La contraseña de la base no se cambia** después del primer arranque: PostgreSQL la guarda al crear la base. Si hay que cambiarla, primero se cambia dentro de la base (`ALTER USER inspectia PASSWORD …` por Session Manager) y después en SSM, y se reinicia con `sudo /opt/inspectia/actualizar.sh && sudo docker compose -f /opt/inspectia/docker-compose.yml up -d --force-recreate`.
- Cambiar la clave de los tokens cierra todas las sesiones abiertas.

**6. El rol de la instancia, `inspectia-ec2`.** IAM → Roles → Create role → *AWS service* → *EC2* → agrega la política administrada **AmazonSSMManagedInstanceCore** (Session Manager y Run Command) → nombre `inspectia-ec2`. Después, en el rol: Add permissions → Create inline policy → JSON → pega `infra/aws/iam/inspectia-ec2-permisos.json` con tu `<cuenta>` → nombre `inspectia-ec2`. Con eso la instancia baja de ECR, usa solo sus prefijos del bucket de archivos (y no puede borrar los respaldos), lee los dos parámetros, escribe en CloudWatch y avisa a SNS si falla la copia. Riesgo aceptado: la política administrada de Session Manager deja leer cualquier parámetro de SSM; en esta cuenta solo están los de InspectIA.

**7. El grupo de seguridad.** EC2 → Security Groups → Create: nombre `inspectia-servidor`, en la VPC por defecto. Inbound: una sola regla, *HTTP* (80) con origen la lista de prefijos administrada **`com.amazonaws.global.cloudfront.origin-facing`** (escribe `pl-` en el origen y elígela). Nada de SSH ni del 22: se entra por Session Manager. Outbound: el de siempre (todo).

**8. La instancia.** EC2 → Launch instance:

- Nombre `inspectia-servidor`; AMI **Amazon Linux 2023**, arquitectura **64-bit (Arm)**; tipo **t4g.medium**.
- Key pair: **Proceed without a key pair**.
- Red: la VPC por defecto, una subred pública, *Auto-assign public IP* encendido y el grupo `inspectia-servidor`.
- Almacenamiento: **30 GiB gp3**, cifrado.
- Advanced details:
  - *IAM instance profile*: `inspectia-ec2`.
  - *Metadata accessible* Enabled, *Metadata version* **V2 only (token required)** y *Metadata response hop limit* **2**: así los contenedores llegan al rol de la instancia (con 1 no llegan y el backend no puede usar S3).
  - *Termination protection* encendido.
  - *User data*: pega completo `infra/aws/datos-de-usuario.sh`. Instala Docker con su plugin compose y el agente de CloudWatch, crea 2 GB de swap, las carpetas y los servicios de systemd (el que levanta InspectIA al encender y el temporizador de la copia diaria).

Después: EC2 → Elastic IPs → Allocate → Associate a `inspectia-servidor`. En la instancia, anota su ID (`i-…`) y su **Public IPv4 DNS** (`ec2-…compute-1.amazonaws.com`): es el origen de la API en CloudFront, que no acepta una IP.

Para revisar que arrancó bien, a los 3 minutos: Connect → **Session Manager** → Connect, y:

```sh
sudo tail -n 20 /var/log/cloud-init-output.log   # termina con «Listo. El primer despliegue llega desde GitHub Actions»
docker compose version                           # v5.6.0
swapon --show                                    # /swapfile de 2G
systemctl list-timers inspectia-respaldo.timer   # la próxima copia, a las 12:00 de Colombia
```

**9. CloudFront.**

1. **La función de las rutas.** CloudFront → Functions → Create function: nombre `inspectia-rutas`, runtime **cloudfront-js-2.0**. Pega `infra/aws/cloudfront/rutas-react.js` → Save. En Test, con *Viewer request* y la URL `/lotes/12`, la respuesta debe tener `"uri": "/index.html"`; con `/api/v1/sesion`, la misma URL. Publish.
2. **Los encabezados de seguridad.** CloudFront → Policies → Response headers → Create: nombre `inspectia-seguridad`, con *Strict-Transport-Security* (31536000 s, *includeSubDomains*), *X-Content-Type-Options*, *X-Frame-Options* `DENY`, *Referrer-Policy* `strict-origin-when-cross-origin` y *Content-Security-Policy* con el contenido de **`infra/aws/cloudfront/csp.txt`**, con los valores que ese archivo indica cambiar (el bucket de archivos, para subir y ver las fotos). Marca *Origin override* en todos. Es la política de D-31.
3. **La distribución.** Create distribution, con el plan de pago por uso (la capa gratuita de CloudFront cubre este uso):
   - Origen: el bucket `inspectia-web-<cuenta>` (elígelo de la lista, no el sitio web del bucket) con **Origin access control** (créalo con los valores por defecto). Al terminar, la consola ofrece copiar la política del bucket: pégala en `inspectia-web-<cuenta>` → Permissions → Bucket policy.
   - Comportamiento por defecto: *Redirect HTTP to HTTPS*; métodos `GET, HEAD`; cache policy **CachingOptimized**; compresión; Function associations → *Viewer request* → **CloudFront Functions** → `inspectia-rutas`; response headers policy `inspectia-seguridad`.
   - *Default root object*: `index.html`. WAF: no lo actives (cuesta; ver P-17 abajo). IPv6 y HTTP/2 y 3 encendidos; todas las ubicaciones.
4. **El origen de la API.** En la distribución: Origins → Create origin: *Origin domain* el **Public IPv4 DNS** de la instancia, protocolo **HTTP only**, puerto **80**, *Response timeout* **60** s, nombre `inspectia-servidor`.
5. **El comportamiento `/api/*`.** Behaviors → Create behavior: patrón **`/api/*`**, origen `inspectia-servidor`, *Redirect HTTP to HTTPS*, métodos **GET, HEAD, OPTIONS, PUT, POST, PATCH, DELETE**, cache policy **CachingDisabled**, origin request policy **AllViewerExceptHostHeader** (pasa el token `Authorization`, la consulta y los demás encabezados, sin el `Host` de CloudFront), response headers policy `inspectia-seguridad` y **sin** función. Revisa que `/api/*` quede antes que *Default (\*)*.
6. Anota el **ID** de la distribución (`E…`) y su **dominio** (`d….cloudfront.net`).
7. **El CORS del bucket de archivos**, ahora que está el dominio: `inspectia-archivos-<cuenta>` → Permissions → CORS → pega `infra/aws/s3/cors-archivos.json` con `https://d….cloudfront.net`. La página sube las fotos con `PUT` directo a S3, con la URL prefirmada que da el backend.

**10. El proveedor OIDC de GitHub.** IAM → Identity providers → Add provider → *OpenID Connect*: URL `https://token.actions.githubusercontent.com` y audiencia `sts.amazonaws.com`.

**11. El rol de la CI, `inspectia-github`.** IAM → Roles → Create role → *Web identity* → el proveedor `token.actions.githubusercontent.com`, audiencia `sts.amazonaws.com`, *GitHub organization* `DrekOF`, *repository* `inspectia`, *branch* `main` → sin políticas → nombre `inspectia-github`. Después, en el rol:

- Trust relationships → Edit: debe quedar igual a `infra/aws/iam/inspectia-github-confianza.json` (con tu `<cuenta>`): solo `repo:DrekOF/inspectia:ref:refs/heads/main`, sin comodines. Un PR o una rama no lo pueden asumir.
- Add permissions → Create inline policy → JSON → `infra/aws/iam/inspectia-github-permisos.json` con `<cuenta>`, `<instancia>` y `<distribucion>` → nombre `inspectia-github`. Solo sube a los dos repositorios de ECR, escribe en el bucket web, invalida esa distribución, enciende esa instancia y le manda el comando de SSM.
- Copia el **ARN** del rol (`arn:aws:iam::<cuenta>:role/inspectia-github`).

**12. CloudWatch.**

- Logs → Log groups → Create, con retención de **30 días**: `/inspectia/contenedores` (los tres contenedores, un flujo por contenedor), `/inspectia/servidor` (las actualizaciones, la copia diaria y el arranque de la instancia) y `/inspectia/despliegue` (la salida de cada comando de SSM de la CI). Si no los creas, se crean solos, pero sin retención.
- Alarms → Create alarm, todas con acción a `inspectia-alertas` y con *Treat missing data as* **good (not breaching)**, porque de noche la instancia está apagada:

| Alarma | Métrica | Condición |
|---|---|---|
| `inspectia-cpu` | EC2 → Per-Instance → `CPUUtilization` de la instancia | promedio > 80 % durante 3 periodos de 5 min |
| `inspectia-estado` | EC2 → Per-Instance → `StatusCheckFailed` | máximo ≥ 1 durante 2 periodos de 1 min |
| `inspectia-memoria` | CWAgent → InstanceId → `mem_used_percent` | promedio > 90 % durante 2 periodos de 5 min |
| `inspectia-disco` | CWAgent → InstanceId, fstype, path → `disk_used_percent` (path `/`) | máximo > 80 % durante 1 periodo de 5 min |
| `inspectia-errores-5xx` | CloudFront → Per-Distribution → `5xxErrorRate` (región *Global*) | promedio > 5 % durante 2 periodos de 5 min |

Las métricas de CWAgent aparecen unos minutos después de que el agente arranca. Si la copia diaria falla, `respaldar.sh` avisa directo a `inspectia-alertas`.

**13. EventBridge Scheduler.** EventBridge → Scheduler → Schedules → Create schedule, dos veces, con *Recurring schedule*, *Cron-based*, zona horaria **America/Bogota** y *Flexible time window* Off:

- `inspectia-encender`: `cron(0 7 ? * MON-FRI *)` → target *All APIs* → *Amazon EC2* → **StartInstances** con `{"InstanceIds": ["<instancia>"]}`.
- `inspectia-apagar`: `cron(0 22 ? * MON-FRI *)` → **StopInstances** con el mismo JSON.

En Permissions deja *Create new role for this schedule*. Ajusta las horas a las del equipo. **El jueves de la sustentación y durante la prueba de estrés, desactiva `inspectia-apagar`** (Disable) y vuelve a activarlo después. Si la CI despliega con la instancia apagada, la enciende; el horario la vuelve a apagar a su hora.

**14. Los modelos de IA en S3.** El servicio de IA baja de `s3://inspectia-archivos-<cuenta>/modelos/` el modelo elegido (EfficientNet-B0) y comprueba su SHA-256: sin él, no arranca. Súbelos una vez, antes de poner las variables del paso 15 (con ellas, el siguiente push a `main` ya despliega), con el script de `ia-servicio/scripts/` desde **CloudShell** (ya tiene las credenciales de tu sesión, así no hacen falta claves en tu PC): `git clone https://github.com/DrekOF/inspectia.git` y sigue `ia-servicio/README.md`. Ese script llega con el PR del servicio de IA: haz este paso cuando ese PR esté en `main`. El script baja los 5 modelos del release `modelos-v1.0` de Julián, comprueba su SHA-256 y los sube con sus JSON y su `manifest.json`. Revisa en S3 que estén en `modelos/`.

**15. Las variables del repositorio en GitHub.** En GitHub: Settings → Secrets and variables → Actions → pestaña **Variables** → New repository variable. Son variables, no secretos: no dan acceso a nada sin el rol, que solo se asume desde `main`.

**Ponlas solo cuando estén listos los dos requisitos del primer despliegue:**

- Los modelos ya están en S3 (paso 14). Sin ellos, el servicio de IA no queda sano y el despliegue falla.
- El PR del servicio de IA ya está fusionado en `main`. Trae `ia-servicio/Dockerfile` (sin él no hay imagen de `inspectia/ia-servicio` en ECR y el despliegue falla) y el clasificador `servicio` del backend, que el compose de producción pide con `INSPECTIA_IA_CLASIFICADOR: servicio` (sin él, el backend no arranca). Ese PR y el del despliegue van los dos en `main` antes del primer despliegue, en cualquier orden: mientras falten las variables, la CI no despliega.

| Variable | Valor | Ejemplo |
|---|---|---|
| `AWS_CUENTA` | el ID de la cuenta | `123456789012` |
| `AWS_ROL_GITHUB` | el ARN del rol del paso 11 | `arn:aws:iam::123456789012:role/inspectia-github` |
| `CLOUDFRONT_DISTRIBUCION` | el ID de la distribución | `E2ABCDEF123456` |
| `EC2_INSTANCIA` | el ID de la instancia | `i-0abc123def4567890` |
| `INSPECTIA_URL` | la dirección de la página, sin `/` al final | `https://d1234abcd.cloudfront.net` |

Con las cinco, el siguiente push a `main` despliega. Para desplegar sin un commit nuevo: Actions → CI → Run workflow → `main`.

**16. El primer despliegue.** Con los pasos anteriores listos (los modelos en S3, el PR del servicio de IA y el del despliegue en `main`, y las variables puestas), haz un push a `main` o corre la CI a mano en `main` y mira el job **Despliegue en AWS**. La primera vez tarda más: la instancia baja las imágenes y el servicio de IA baja el modelo de S3.

### Primer administrador en producción

Con InspectIA arriba (el backend ya creó las tablas), entra a la instancia por Session Manager y corre:

```sh
sudo /opt/inspectia/admin-plataforma.sh crear
```

Pide el nombre, el correo y la contraseña inicial (dos veces, sin mostrarla) y corre `infra/bd/crear-admin-plataforma.sql` contra la base de producción, con las mismas reglas que en local. No queda nada en archivos ni en el historial. Para restablecer su contraseña (D-30): `sudo /opt/inspectia/admin-plataforma.sh restablecer`.

### Probar que todo funciona

1. En GitHub Actions, la corrida de `main` en verde, con **Despliegue en AWS** terminado (no saltado).
2. En el navegador, `https://d….cloudfront.net`: carga la página con candado (HTTPS). Abre directo `https://d….cloudfront.net/lotes/1`: carga la página (la CloudFront Function), no un error de S3. `https://d….cloudfront.net/api/v1/sesion` responde 401 en JSON, no la página.
3. Inicia sesión con el administrador de la plataforma. Crea una empresa con su administrador, entra con él, crea un usuario final y entra con él (si el token no llegara al backend, cada petición después de iniciar sesión daría 401: ver «Si algo falla»).
4. Registra un lote, sube 2 fotos (van directo a S3: en `inspectia-archivos-<cuenta>/fotos/` aparecen) y analízalo: el resultado sale del modelo real, en pocos segundos (RNF-01). Descarga el reporte PDF (queda en `reportes/`).
5. En la instancia, por Session Manager:

   ```sh
   sudo docker compose -f /opt/inspectia/docker-compose.yml ps   # postgres, ia-servicio y backend «healthy»
   free -h                                                     # memoria y swap
   sudo tail -n 20 /var/log/inspectia/actualizar.log           # «InspectIA <etiqueta> quedó arriba y sano.»
   ```

6. En CloudWatch: Logs → `/inspectia/contenedores` con un flujo por contenedor; Metrics → CWAgent con la memoria y el disco de la instancia; las alarmas en *OK* (o *Insufficient data* al principio).
7. En SNS, `inspectia-alertas` → Publish message → «prueba»: llega al correo del equipo.
8. Apaga y enciende la instancia desde la consola: a los 3 o 4 minutos InspectIA vuelve a responder solo (el servicio `inspectia` de systemd lee los secretos y levanta todo).

### Copia diaria de la base y su restauración

Cada día a las 12:00 de Colombia (o al encender, si a esa hora estaba apagada), el temporizador `inspectia-respaldo` corre `/opt/inspectia/respaldar.sh`: saca un `pg_dump` comprimido (formato custom de PostgreSQL), comprueba que `pg_restore` lo lee y lo sube a `s3://inspectia-archivos-<cuenta>/respaldos/inspectia-<fecha en UTC>.dump`. S3 borra las copias de más de 35 días. Si falla, avisa a `inspectia-alertas`. Su registro queda en `/var/log/inspectia/respaldo.log` y en CloudWatch (`/inspectia/servidor`).

**Probar que una copia se restaura** (sin tocar producción), por Session Manager:

```sh
sudo systemctl start inspectia-respaldo.service              # saca una copia ahora
aws s3 ls s3://inspectia-archivos-<cuenta>/respaldos/        # ahí está
sudo /opt/inspectia/restaurar-respaldo.sh --prueba ultimo    # la restaura aparte y compara
```

`--prueba` restaura la copia en una base aparte (`inspectia_prueba`), compara cuántas filas tiene cada tabla con la base en uso, muestra el conteo y borra la base de prueba. Termina con «se restauró y tiene las mismas filas que la base en uso». Si alguien usó InspectIA entre la copia y la prueba, los conteos difieren: saca otra copia y repite. Hazlo una vez antes de la sustentación.

**Restaurar de verdad** (reemplaza la base en uso): `sudo /opt/inspectia/restaurar-respaldo.sh ultimo` o con el nombre de una copia. Pide escribir `RESTAURAR`, antes saca una copia de lo que hay, detiene el backend, restaura y lo vuelve a levantar.

### Operación

- **Ver registros:** CloudWatch → Logs → `/inspectia/contenedores` (backend, ia-servicio y postgres). En la instancia, `sudo docker compose -f /opt/inspectia/docker-compose.yml logs --tail 100 backend`.
- **Volver atrás:** `sudo /opt/inspectia/actualizar.sh <etiqueta>` con el SHA de un commit anterior que esté en ECR (el anterior queda en `/opt/inspectia/.env`, como `ETIQUETA_ANTERIOR`). Si la versión nueva aplicó una migración de Flyway, la anterior no arranca con esa base: en ese caso se corrige con un commit nuevo, o se restaura la copia de antes del despliegue.
- **Volver a levantar todo:** `sudo systemctl restart inspectia`.
- **Si algo falla:**
  - El job de despliegue dice que falta la imagen de `inspectia/ia-servicio`: el PR del servicio de IA (con `ia-servicio/Dockerfile`) todavía no está en `main`: ver el paso 15.
  - El job de despliegue falla con «no quedó sano. Sigue la versión anterior, …, sana»: el sitio sigue con la versión anterior. Mira el registro del job (o `/var/log/inspectia/actualizar.log`), corrige y haz un commit nuevo. Si dice que la anterior tampoco arrancó, ve a «Volver atrás».
  - `actualizar.sh` dice que un parámetro no es válido: revisa el paso 5.
  - El servicio de IA no queda sano: mira su registro; si no encuentra el modelo o su SHA-256 no coincide, revisa el paso 14.
  - Con la sesión iniciada, todo responde 401: CloudFront no está pasando el encabezado `Authorization`. Revisa que `/api/*` tenga **CachingDisabled** y **AllViewerExceptHostHeader**. Si aun así no pasa, crea una cache policy `inspectia-api` con TTL mínimo 0, por defecto 0 y máximo 1, y el encabezado `Authorization` en la clave de caché, y úsala en `/api/*` en lugar de CachingDisabled.
  - Las fotos no suben (error de CORS en la consola del navegador): revisa el paso 9.7 y la política de seguridad de contenido.
- **P-17 (límite de intentos de inicio de sesión por IP):** no está en este despliegue. Se puede con una regla *rate-based* de AWS WAF en la distribución (unos US$6 al mes o más) o en el backend, con la IP que CloudFront manda en `CloudFront-Viewer-Address`. Lo decide el equipo.
- **Riesgo aceptado:** entre CloudFront y la instancia el tráfico va por HTTP (el navegador sí usa HTTPS con CloudFront). La instancia solo acepta conexiones de CloudFront.
