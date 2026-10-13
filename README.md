<!--
Nombre: README.md
Descripción: Presentación del monorepo de InspectIA: estructura, versiones, requisitos del entorno de desarrollo, base de datos local y cita del dataset.
Fecha de creación: 12/10/2026
Actualización: 12/10/2026
Autor: Equipo InspectIA
Versión: 1.1
-->

# InspectIA

Aplicación web multiempresa que estima el porcentaje de granos de cacao sanos de un lote a partir de 2 o 3 fotos de una muestra (100 a 150 granos), y entrega una categoría probable, una recomendación y un reporte en PDF.

Proyecto final de Inteligencia Artificial, Universidad Surcolombiana. Docente: Juan Antonio Castro Silva. Equipo: Cristhian Sánchez, Julián Silvas y Jhon Perdomo.

## Estructura

| Carpeta | Qué va ahí |
|---|---|
| `frontend/` | Página web: React y Material UI. |
| `backend/` | Servidor: Java y Spring Boot, un solo programa dividido en módulos (D-21). |
| `ia-servicio/` | Servicio de IA: Python, OpenCV y ONNX. |
| `generador/` | Scripts de Python que generan los CRUD a partir de `specs/entidades/` (RP-23). |
| `infra/` | Docker Compose, scripts de base de datos, de compilación y de despliegue. |
| `specs/` | Especificación: fichas RF y RNF, decisiones, arquitectura, pautas del docente y entidades. |
| `docs/` | Bitácora de construcción (RP-24) y documentación. |

## Versiones

Fijadas el 12/10/2026. La versión exacta de cada librería queda en `backend/pom.xml` y `frontend/package.json` cuando se crea cada proyecto.

| Pieza | Versión | Notas |
|---|---|---|
| Java | 25 LTS (Eclipse Temurin 25.0.4 o superior) | |
| Spring Boot | 4.1.1 | Fija Lombok 1.18.46 (soporta el JDK 25) y Testcontainers 2.0.5. Se eligió la 4.1 porque el soporte gratuito de la 4.0 termina el 31/12/2026. |
| springdoc-openapi | 3.1.1 | La línea 3.1 está construida sobre Spring Boot 4.1. |
| Maven | Wrapper `mvnw`, incluido en `backend/` | No hace falta instalar Maven. |
| Node | 24 LTS | |
| React | 19.x, con Vite | |
| Material UI | 9.x (9.5.0) | Versión mayor estable actual. |
| Python | 3.13 | Para `ia-servicio/` y `generador/`. |
| PostgreSQL | 18 | Corre en Docker; no hace falta instalarlo. |

### Imágenes de Docker

En producción todo corre en una EC2 t4g, que tiene procesador ARM (Graviton, D-16), mientras que los PC del equipo son x86. Por eso todas las imágenes base deben existir en `linux/amd64` y en `linux/arm64`. Las de esta tabla se verificaron en Docker Hub:

| Uso | Imagen base | Por qué |
|---|---|---|
| Base de datos | `postgres:18-alpine` | Imagen oficial. |
| Backend: compilar | `eclipse-temurin:25-jdk-noble` | Ubuntu. |
| Backend: ejecutar | `eclipse-temurin:25-jre-noble` | Solo el JRE: imagen más liviana. |
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

2. **Node.** Se instala con fnm para quedarse en la 24: `winget install OpenJS.NodeJS.LTS` pasará a instalar Node 26 cuando esa versión sea LTS (finales de octubre de 2026). Si ya tienes Node 24, este paso no hace falta.

   ```powershell
   fnm install 24
   fnm default 24
   ```

   Para que la terminal encuentre `node`, agrega esta línea a tu perfil de PowerShell (`notepad $PROFILE`):

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
node -v                  # v24.x
py -3.13 --version       # Python 3.13.x  (macOS o Linux: python3.13 --version)
docker compose version
```

## Base de datos local

Desde la raíz del repositorio:

```bash
cp infra/.env.example infra/.env     # luego escribe una contraseña en POSTGRES_PASSWORD
docker compose -f infra/docker-compose.yml up -d
```

PostgreSQL queda en `localhost`, en el puerto de `POSTGRES_PORT` (5432 por defecto), con base y usuario `inspectia`, y solo se puede usar desde tu equipo. `infra/.env` no se sube al repositorio. Más comandos en la sección Comandos de `CLAUDE.md`.

El puerto puede cambiar en cada equipo: el backend lo lee de la configuración (`POSTGRES_PORT`) y nunca usa el 5432 fijo.

Si Docker responde «forbidden by its access permissions», Windows tiene reservado ese puerto. Para ver los rangos reservados:

```powershell
netsh interface ipv4 show excludedportrange protocol=tcp
```

Elige un puerto que no esté en esos rangos, por ejemplo el 5433, escríbelo en `POSTGRES_PORT` de `infra/.env` y vuelve a encender la base.

## Dataset y licencia (RNF-08)

Los modelos se entrenan con el dataset [*cacao-beans-for-classification*](https://www.kaggle.com/datasets/luiszhinin979/ecuadorian-cocoa-beans-for-classification) de Kaggle, con licencia [CC BY-NC 4.0](https://creativecommons.org/licenses/by-nc/4.0/): hay que citar a sus autores y no se puede usar con fines comerciales. Mientras InspectIA use modelos entrenados con este dataset, no se cobra. El dataset no se sube a este repositorio.

Dataset:

> Zhinin Vera, L. F. (2022). *cacao-beans-for-classification: Ecuadorian cacao beans for classification* [Conjunto de datos]. Kaggle. https://www.kaggle.com/datasets/luiszhinin979/ecuadorian-cocoa-beans-for-classification

Artículo de los autores:

> Zhinin-Vera, L., Zhiminaicela-Cabrera, J., Pretel, E., Suárez, P., Chang, O., Castro, F. A. y de la Rosa, F. L. (2023). Artificial Vision Technique to Detect and Classify Cocoa Beans. En *Advances in Computational Intelligence* (Lecture Notes in Computer Science, pp. 217-228). Springer. https://doi.org/10.1007/978-3-031-43078-7_18

## Documentación

- Especificación: `specs/` (fichas, decisiones, arquitectura y pautas del docente).
- Bitácora de construcción: `docs/bitacora.md`.
