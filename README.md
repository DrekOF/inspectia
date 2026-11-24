<!--
Nombre: README.md
Descripción: Presentación del monorepo de InspectIA: estructura, versiones, requisitos del entorno de desarrollo, base de datos local, frontend, integración continua, despliegue en AWS y cita del dataset.
Fecha de creación: 12/10/2026
Actualización: 24/11/2026
Autor: Equipo InspectIA
Versión: 1.16
-->

# InspectIA

Aplicación web multiempresa que estima el porcentaje de granos de cacao sanos de un lote a partir de 2 o 3 fotos de una muestra (100 a 150 granos), y entrega una categoría probable, una recomendación y un reporte en PDF.

Proyecto final de Inteligencia Artificial, Universidad Surcolombiana. Docente: Juan Antonio Castro Silva. Equipo: Cristhian Sánchez, Julián Silva y Jhon Perdomo.

## Estructura

| Carpeta | Qué va ahí |
|---|---|
| `frontend/` | Página web: React y Material UI. |
| `backend/` | Servidor: Java y Spring Boot, un solo programa dividido en módulos (D-21). |
| `ia-servicio/` | Servicio de IA: Python, OpenCV y ONNX. |
| `generador/` | Scripts de Python que generan los CRUD a partir de `specs/entidades/` (RP-23) y la lista de casos de prueba (RP-13). |
| `infra/` | Docker Compose, scripts de base de datos, de compilación y de despliegue. |
| `.github/workflows/` | La integración continua con GitHub Actions (RP-18 y RP-19). |
| `specs/` | Especificación: fichas RF y RNF, decisiones, arquitectura, pautas del docente y entidades. |
| `docs/` | Bitácora de construcción (RP-24), casos de prueba (RP-13), la guía de la demostración de otra temática (RP-23) y documentación. |

## Versiones

Fijadas el 12/10/2026; las del frontend, el 20/10/2026, y las del despliegue en AWS, el 24/11/2026. La versión exacta de cada librería queda en `backend/pom.xml` y `frontend/package.json`.

| Pieza | Versión | Notas |
|---|---|---|
| Java | 25 LTS (Eclipse Temurin 25.0.4 o superior) | |
| Spring Boot | 4.1.1 | Fija Lombok 1.18.46 (soporta el JDK 25) y Testcontainers 2.0.5. Se eligió la 4.1 porque el soporte gratuito de la 4.0 termina el 31/12/2026. |
| Spring Security | 7.1.1 | La fija Spring Boot. Los tokens del inicio de sesión (D-28) usan Nimbus JOSE + JWT 10.9.1, que trae el iniciador del servidor de recursos de Spring: no hace falta otra librería. |
| springdoc-openapi | 3.1.1 | La línea 3.1 está construida sobre Spring Boot 4.1. |
| Maven | Wrapper `mvnw`, incluido en `backend/` | No hace falta instalar Maven. |
| Node | 24 LTS (24.22.0) | La fija `frontend/.node-version`, que fnm lee solo. jsdom 30 y el plugin de JSDoc de ESLint piden la 24.15 o superior. |
| React | 19.3.1, con Vite 8.3.4 | React Router 8.5.0 para las rutas. |
| Material UI | 9.x (9.5.0) | Versión mayor estable actual. |
| Formularios e idiomas | Formik 2.4.9 y Yup 1.7.1; i18next 26.5.3 y react-i18next 17.1.0 | Los textos que comparte con el backend salen de sus `.properties` (RP-11). |
| Vitest | 4.1.12 | No la 5: Stryker 10.0.0, que hace las pruebas de mutación, no activa los mutantes con Vitest 5 (stryker-js #6210). Se vuelve a la 5 cuando Stryker publique el arreglo. Con Testing Library y MSW 3.0.3, que simula la API. |
| Pruebas de punta a punta | Playwright 1.64.1 y Selenium WebDriver 4.51.0 | Playwright con los motores de Chrome, Firefox y Safari, en PC y celular; Selenium con Chrome y Edge. |
| Calidad del frontend | ESLint 10.13.0 y Stryker 10.0.0 | |
| Calidad del backend | PIT 1.30.2 (`pitest-maven`), con `pitest-junit5-plugin` 1.2.3 | Pruebas de mutación del dominio del análisis, en el perfil `mutacion` de Maven: `./mvnw -Pmutacion test` desde `backend/`. |
| Python | 3.13 | Para `ia-servicio/` y `generador/`. El generador usa Jinja2 3.1.6 y pytest 9.1.1 (`generador/requirements*.txt`). |
| PostgreSQL | 18 | Corre en Docker; no hace falta instalarlo. |
| AWS SDK for Java | 2.59.8 (BOM `software.amazon.awssdk:bom`) | Solo el cliente de S3, con el cliente HTTP de Apache: el backend guarda las fotos y los reportes en S3 y firma sus enlaces temporales (D-16). Se excluye `netty-nio-client`, que solo usan los clientes asíncronos. |
| Docker Compose en la instancia | v5.6.0 | La instancia EC2 usa Amazon Linux 2023. `infra/aws/datos-de-usuario.sh` baja el plugin del release oficial y comprueba su SHA-256. |
| GitHub Actions | `checkout` v7, `setup-java` v6, `setup-python` v7, `setup-node` v7 y `upload-artifact` v7, en Ubuntu 24.04 | Las acciones oficiales de GitHub, en su versión mayor; fijadas el 04/11/2026. |
| GitHub Actions del despliegue | `docker/setup-buildx-action` v4, `docker/build-push-action` v7, `aws-actions/configure-aws-credentials` v6 y `aws-actions/amazon-ecr-login` v2 | Construyen las imágenes arm64 en `ubuntu-24.04-arm`, las suben a ECR y entran a AWS por OIDC, sin claves; fijadas el 24/11/2026. |

### Imágenes de Docker

En producción todo corre en una EC2 t4g, que tiene procesador ARM (Graviton, D-16), mientras que los PC del equipo son x86. Por eso todas las imágenes base deben existir en `linux/amd64` y en `linux/arm64`. Las de esta tabla se verificaron en Docker Hub:

| Uso | Imagen base | Por qué |
|---|---|---|
| Base de datos | `postgres:18-alpine` | Imagen oficial. |
| Backend: compilar | `eclipse-temurin:25-jdk-noble` | Ubuntu. La usa `backend/Dockerfile`. |
| Backend: ejecutar | `eclipse-temurin:25-jre-noble` | Solo el JRE: imagen más liviana. Corre con el usuario `inspectia` (UID 10001) y el heap en el 60 % de la memoria del contenedor. |
| Servicio de IA | `python:3.13-slim` | Debian y no Alpine: OpenCV y ONNX Runtime publican paquetes para Linux con glibc, no para Alpine (musl). |
| Frontend: compilar | `node:24-alpine` | Solo para compilar. En producción la página se sirve desde S3 y CloudFront. |

Las imágenes propias se construyen para las dos arquitecturas con `docker buildx build --platform linux/amd64,linux/arm64`.

## Requisitos e instalación

Necesitas Git, el JDK 25 de Temurin, Node 24, Python 3.13 y Docker Desktop. En Windows, desde PowerShell:

```powershell
winget install --id Git.Git -e
winget install --id EclipseAdoptium.Temurin.25.JDK -e
winget install --id Schniz.fnm -e
winget install --id Python.Python.3.13 -e
winget install --id Docker.DockerDesktop -e
```

Después de instalar, cierra y vuelve a abrir la terminal, y sigue con estos pasos:

1. **Java.** Si ya tenías otro JDK, `JAVA_HOME` puede seguir apuntando a él, y el wrapper de Maven usa `JAVA_HOME`. Para apuntarlo al JDK 25 (solo para tu usuario):

   ```powershell
   $jdk = (Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -Filter 'jdk-25*' | Select-Object -Last 1).FullName
   [Environment]::SetEnvironmentVariable('JAVA_HOME', $jdk, 'User')
   ```

   Abre otra terminal. Si `java -version` todavía muestra otra versión, sube la carpeta `bin` del JDK 25 en la variable `Path` del sistema o desinstala el JDK viejo.

2. **Node.** Se instala con fnm para quedarse en la 24: `winget install OpenJS.NodeJS.LTS` pasará a instalar Node 26 cuando esa versión sea LTS (finales de octubre de 2026). La versión exacta está en `frontend/.node-version` (24.22.0); desde `frontend/`, `fnm install` la instala:

   ```powershell
   cd frontend
   fnm install
   fnm default 24.22.0
   ```

   Para que la terminal encuentre `node` y cambie sola a la versión de `frontend/.node-version` al entrar a la carpeta, agrega esta línea a tu perfil de PowerShell (`notepad $PROFILE`):

   ```powershell
   fnm env --use-on-cd --shell powershell | Out-String | Invoke-Expression
   ```

3. **Python.** Puede convivir con otras versiones. Usa siempre `py -3.13`; por ejemplo, para crear un entorno virtual: `py -3.13 -m venv .venv`.

4. **Docker Desktop.** Necesita WSL 2 (el instalador lo ofrece). Ábrelo antes de usar `docker compose`.

En macOS o Linux instala las mismas versiones: Temurin 25 desde [adoptium.net](https://adoptium.net), Node 24 con [fnm](https://github.com/Schniz/fnm), Python 3.13 desde [python.org](https://www.python.org) o con pyenv, y Docker Desktop o Docker Engine con Compose.

Para comprobar el entorno:

```bash
git --version
java -version            # 25.x
node -v                  # v24.22.0, desde frontend/
py -3.13 --version       # Python 3.13.x  (macOS o Linux: python3.13 --version)
docker compose version
```

## Base de datos local

Desde la raíz del repositorio:

```bash
cp infra/.env.example infra/.env     # luego escribe una contraseña en POSTGRES_PASSWORD y la clave en INSPECTIA_JWT_CLAVE
docker compose -f infra/docker-compose.yml up -d
```

`INSPECTIA_JWT_CLAVE` es la clave con la que el backend firma los tokens del inicio de sesión: sin ella no arranca. El comando para crearla está en `infra/.env.example` y en `infra/README.md`.

PostgreSQL queda en `localhost`, en el puerto de `POSTGRES_PORT` (5432 por defecto), con base y usuario `inspectia`, y solo se puede usar desde tu equipo. `infra/.env` no se sube al repositorio. Más comandos en la sección Comandos de `CLAUDE.md`.

El puerto puede cambiar en cada equipo: el backend lo lee de la configuración (`POSTGRES_PORT`) y nunca usa el 5432 fijo.

Si Docker responde «forbidden by its access permissions», Windows tiene reservado ese puerto. Para ver los rangos reservados:

```powershell
netsh interface ipv4 show excludedportrange protocol=tcp
```

Elige un puerto que no esté en esos rangos, por ejemplo el 5433, escríbelo en `POSTGRES_PORT` de `infra/.env` y vuelve a encender la base.

## Backend, generador y pruebas

Los comandos están en la sección Comandos de `CLAUDE.md`. En resumen:

- **Backend:** desde `backend/`, `./mvnw verify` corre todas las pruebas (las de integración usan PostgreSQL 18 en Testcontainers, con Docker Desktop abierto) y `./mvnw spring-boot:run` lo arranca contra la base local. La API queda documentada con OpenAPI en http://localhost:8080/swagger-ui.html.
- **Primer administrador de la plataforma:** lo crea un script de la base de datos; los pasos están en `infra/README.md`.
- **Inicio de sesión:** `POST /api/v1/sesion` entrega un token que vence a las 8 horas; las demás rutas lo piden en el encabezado `Authorization: Bearer` (D-28). En Swagger UI, el botón «Authorize» recibe ese token. Los pasos para probarlo están en `infra/README.md`.
- **Contraseñas:** cada usuario cambia la suya con `PUT /api/v1/sesion/contrasena` (RF-18). Si el administrador de la plataforma olvida la suya, el equipo la restablece con un script de la base de datos (D-30); los pasos están en `infra/README.md`.
- **Rangos de recomendación:** el administrador de empresa lista, edita en bloque y restaura los 4 rangos de su empresa en la pantalla «Rangos de recomendación» o en `/api/v1/rangos-recomendacion` (RF-13); los pasos para probar la API están en `infra/README.md`.
- **Lotes:** el usuario final registra un lote en «Nuevo lote» o con `POST /api/v1/lotes` (RF-04), y él y el administrador de empresa ven los lotes de su empresa en «Historial» o en `GET /api/v1/lotes` (RF-14), con filtros por fecha del resultado, estado, categoría, código y quien registró el lote, solos o combinados (RF-14 C2); los pasos para probar la API están en `infra/README.md`.
- **Fotos de la muestra:** después de registrar un lote, o con «Continuar» en el historial, el usuario final sube 2 o 3 fotos en «Fotos de la muestra»: en el celular, «Tomar foto» abre la cámara, y en el PC, «Elegir archivo» abre el selector (RF-05, RF-14 C5). En AWS suben directo a S3 con enlaces prefirmados (D-16); en tu PC, las fotos se guardan en una carpeta, fuera del repositorio, con enlaces que firma el backend (D-41). No hace falta tocar `infra/.env` ni reiniciar la base; ver «Fotos en tu PC» en `infra/README.md`.
- **Resultado del lote:** con 2 o 3 fotos, el usuario final pulsa «Analizar la muestra». El análisis espera su turno en una cola guardada en la base (RNF-02) y la página muestra el avance; al terminar, «Resultado del lote» trae el porcentaje de granos sanos corregido (D-39) con su rango probable (D-43), la categoría probable con su recomendación (RF-12) y las fotos con cada grano marcado (RF-11). Si el resultado quedó a menos de 1 punto del límite de una categoría, la página lo dice, con el límite (D-47), y el usuario final puede analizar una segunda muestra del mismo lote; al terminar, el resultado suma los granos de las dos (D-25, RF-14 C6). «Ver» en el historial abre el mismo detalle, también a la administradora. Mientras no esté el servicio de IA (PR 4), los granos los da un clasificador falso; los pasos para probar la API están en `infra/README.md`.
- **Despliegue en AWS (D-16):** en cada push a `main`, la CI sube las imágenes a ECR, actualiza la instancia EC2 por SSM (con los secretos de SSM solo en memoria y la vuelta sola a la versión anterior si la nueva no queda sana), publica la página en S3 e invalida CloudFront. Trae el perfil `produccion` del backend, que exige S3 y el servicio de IA, la copia diaria de la base a S3 y la guía de la consola de AWS, paso a paso, en `infra/README.md`.
- **Generador:** los CRUD (tablas, backend y pantallas) salen de `specs/entidades/*.json` y no se editan a mano (RP-23). Hoy tienen pantalla las empresas, los usuarios y los rangos. Ver `generador/README.md`.
- **Casos de prueba:** `docs/casos-de-prueba.md` lista cada criterio de aceptación con sus pruebas y los que aún no tienen ninguna (RP-13). Lo arma `generador/casos_de_prueba.py` desde las marcas de las pruebas.

## Frontend

Desde `frontend/`, la primera vez `npm ci`. Los detalles están en `frontend/README.md`.

- **Arrancar:** `npm run dev` abre la página en http://localhost:5173. Vite pasa las peticiones de `/api` al backend de http://localhost:8080, así la página y la API comparten el origen y el backend no necesita CORS (D-31). Si tu backend corre en otro puerto, define `INSPECTIA_API_URL`.
- **Pruebas unitarias y de componentes:** `npm test`, con la API simulada (MSW): no necesitan el backend. `npm run lint` corre ESLint y `npm run test:mutacion`, las pruebas de mutación con Stryker (RP-12).
- **Pruebas de punta a punta (RP-14):** `npm run e2e:playwright` y `npm run e2e:selenium`, con Docker Desktop abierto. La primera vez, `npx playwright install chromium firefox webkit`. Cada corrida enciende su propio entorno: una base en otro proyecto de Docker Compose, el backend compilado en una copia y la página compilada, con contraseñas nuevas. No usa `infra/.env` ni tu base, y lo borra al terminar.
- **Firefox en Windows 11 25H2:** el Firefox de Playwright 1.64.1 no abre (Windows rechaza cargar su `mozglue.dll`). En esos equipos, `INSPECTIA_E2E_SIN_FIREFOX=1` lo salta; en CI corre siempre.
- **Edge en la CI:** Selenium lo salta con `INSPECTIA_E2E_SIN_EDGE=1`, porque en Ubuntu 24.04 AppArmor no deja que cree su sandbox y se cierra al abrir. Edge se prueba en Windows, en el PC de cada integrante.
- **Demostración de otra temática (RP-23):** `npm run demo:otra-tematica`, con Docker Desktop abierto. En una copia del proyecto agrega una entidad de otra temática, saca su tabla, su API y su pantalla, y la enciende; Ctrl+C la apaga y borra su base. El repositorio no cambia. Por defecto es el producto de la pauta del docente; con `-- --entidad ../generador/pruebas/ejemplos/cafe.json`, un café especial del Huila. Ver `docs/demostracion-otra-tematica.md`.

## Integración continua (RP-18 y RP-19)

GitHub Actions corre `.github/workflows/ci.yml` en cada PR hacia `main`, en cada push a `main` y a mano, con «Run workflow» en la pestaña Actions. Cada job corre en una máquina de Linux (Ubuntu 24.04) que empieza vacía y se borra al terminar, con las versiones del proyecto y caché de pip, Maven y npm. Si llega un push nuevo a la rama de un PR, la corrida anterior de ese PR se cancela.

| Job | Qué revisa | Tarda |
|---|---|---|
| Generador | Sus pruebas, que lo generado esté al día (`generar.py comprobar`) y que `docs/casos-de-prueba.md` también lo esté | 30 s |
| Backend | `./mvnw verify`: las pruebas unitarias y las de integración, con PostgreSQL en Testcontainers | 2 min |
| Frontend | `npm ci`, ESLint, Vitest y el build | 2 min |
| De punta a punta | Si pasan los tres anteriores, contra un solo entorno aislado: Playwright, en Chrome, Firefox y Safari, de PC y de celular, y Selenium, en Chrome | 13 min, 10 de ellos de Playwright |
| Infraestructura | `bash infra/probar.sh`: la sintaxis y ShellCheck de los scripts del despliegue, los JSON de AWS, el compose de producción, la CloudFront Function, las políticas IAM y el script de arranque de la instancia | Segundos |
| Imágenes Docker | Construye las imágenes arm64 del backend y del servicio de IA (si existe `ia-servicio/Dockerfile`); solo en `main` las sube a ECR | Sin medir todavía |
| Despliegue en AWS | Solo en `main`, si pasan todos los anteriores y están las 5 variables de AWS del repositorio: actualiza la instancia, publica la página, invalida CloudFront y comprueba el sitio. Sin las variables, se salta | Sin medir todavía |
| Aviso de fallas | Solo en `main`: abre, comenta o cierra el issue de cada job | Segundos |

- **Si un job falla en `main`:**
  - el aviso abre un issue «La CI falla en main: …», con el nombre del job y la etiqueta `ci`;
  - lo asigna a quien hizo el push, que al fusionar un PR es quien lo fusionó, y GitHub le avisa por correo y en la campana;
  - si ya hay uno abierto por ese job, comenta ahí;
  - cuando el job vuelve a pasar en `main`, comenta y cierra el issue.
- **Edge no corre en la CI:** ver «Edge en la CI», en «Frontend».
- **Si algo falla:** el reporte de Playwright, las capturas de Selenium y los reportes de Maven quedan como artefactos de la corrida, por 7 días.
- **Stryker y PIT no corren en la CI,** porque tardan mucho: se corren a mano, con `npm run test:mutacion` en `frontend/` y `./mvnw -Pmutacion test` en `backend/`.
- **Minutos:** el repositorio es privado, y el plan gratis trae unos 2.000 minutos de Linux al mes. GitHub cobra cada job redondeado hacia arriba al minuto entero. Las dos corridas medidas en el PR #13 cobraron 17 y 21 minutos; en `main` se suma 1, por el aviso. Con 21, caben unas 95 corridas al mes. La página de cada corrida muestra lo cobrado en «Usage».

### Lo que se configura en GitHub

- **Exigir que la CI pase antes de fusionar:** en un repositorio privado hace falta GitHub Pro, que GitHub Education da gratis a los estudiantes. Mientras tanto, el equipo no fusiona un PR en rojo. Con Pro:
  1. Settings → Rules → Rulesets → New ruleset → New branch ruleset.
  2. Nombre «main», Enforcement status en «Active» y, en Target branches, «Include default branch».
  3. Marca «Require a pull request before merging» y «Require status checks to pass».
  4. En «Add checks», elige Generador, Backend, Frontend y De punta a punta.
  5. Deja marcado «Block force pushes» y guarda con «Create».
- **Los avisos:** cada integrante, en la página del repositorio, Watch → Custom → Issues. Así se entera de los issues de la CI aunque no estén asignados a su nombre.
- **El gasto:** sin un método de pago en la cuenta, al llegar al cupo Actions se detiene hasta el mes siguiente; no cobra.

## Dataset y licencia (RNF-08)

Los modelos se entrenan con el dataset [*cacao-beans-for-classification*](https://www.kaggle.com/datasets/luiszhinin979/ecuadorian-cocoa-beans-for-classification) de Kaggle, con licencia [CC BY-NC 4.0](https://creativecommons.org/licenses/by-nc/4.0/): hay que citar a sus autores y no se puede usar con fines comerciales. Mientras InspectIA use modelos entrenados con este dataset, no se cobra. El dataset no se sube a este repositorio.

Dataset:

> Zhinin Vera, L. F. (2022). *cacao-beans-for-classification: Ecuadorian cacao beans for classification* [Conjunto de datos]. Kaggle. https://www.kaggle.com/datasets/luiszhinin979/ecuadorian-cocoa-beans-for-classification

Artículo de los autores:

> Zhinin-Vera, L., Zhiminaicela-Cabrera, J., Pretel, E., Suárez, P., Chang, O., Castro, F. A. y de la Rosa, F. L. (2023). Artificial Vision Technique to Detect and Classify Cocoa Beans. En *Advances in Computational Intelligence* (Lecture Notes in Computer Science, pp. 217-228). Springer. https://doi.org/10.1007/978-3-031-43078-7_18

## Documentación

- Especificación: `specs/` (fichas, decisiones, arquitectura y pautas del docente).
- Formato de las entidades y de la API generada: `specs/entidades/README.md`.
- Casos de prueba por criterio de aceptación (RP-13): `docs/casos-de-prueba.md`.
- Bitácora de construcción: `docs/bitacora.md`.
- Integración continua: `.github/workflows/ci.yml` (ver «Integración continua»).
- Demostración de otra temática, para la sustentación (RP-23): `docs/demostracion-otra-tematica.md`.
- Frontend: `frontend/README.md`.
