<!--
Nombre: README.md (infra)
Descripción: Qué va en la carpeta infra/ de InspectIA.
Fecha de creación: 12/10/2026
Actualización: 12/10/2026
Autor: Equipo InspectIA
Versión: 1.0
-->

# infra

Infraestructura y despliegue de InspectIA.

Qué hay hoy:

- `docker-compose.yml`: PostgreSQL 18 para desarrollo local. El servidor y el servicio de IA se agregan cuando existan.
- `.env.example`: variables de la base de datos local. Cópialo a `infra/.env`, que no se sube al repositorio, y escribe tu contraseña.

Qué irá aquí:

- Scripts de creación e inicialización de la base de datos (RP-15).
- Scripts de compilación y prueba del frontend y del backend (RP-16), y de gestión del repositorio en GitHub (RP-17).
- Configuración del despliegue en AWS: una EC2 t4g con Docker Compose (D-16).

Los flujos de GitHub Actions (RP-18 y RP-19) tienen que estar en `.github/workflows/`, en la raíz del repositorio, porque GitHub solo los busca ahí. Llaman a los scripts de esta carpeta. GitHub Actions se conecta a AWS por OIDC, sin claves guardadas.

La EC2 t4g es ARM (Graviton), así que toda imagen debe funcionar en amd64 y en arm64.
