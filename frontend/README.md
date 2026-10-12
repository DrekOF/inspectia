<!--
Nombre: README.md (frontend)
Descripción: Qué va en la carpeta frontend/ de InspectIA.
Fecha de creación: 12/10/2026
Actualización: 12/10/2026
Autor: Equipo InspectIA
Versión: 1.0
-->

# frontend

Página web de InspectIA, para PC y celular (RNF-05). Hecha con React 19, Material UI y Vite, con Node 24.

Qué va aquí:

- Pantallas del análisis del lote: registrar el lote, tomar o subir las fotos con la guía de captura, ver el resultado con la foto marcada, el historial y el PDF (RF-04 a RF-15).
- Pantallas de estadísticas y métricas del modelo (RF-16 y RF-17).
- Pantallas CRUD de empresas, usuarios y rangos. **Las genera `generador/`: no se editan a mano** (RP-23).
- Formularios con Formik y Yup (RP-07); tokens de colores, temas y accesibilidad (RP-08); textos en español e inglés (RP-11); JSDoc y header en cada archivo (RP-09 y RP-10).
- Pruebas con Playwright y Selenium, en tamaño de PC y de celular (RP-14).

En producción se publica en S3 y se sirve con CloudFront (D-16).

Todavía está vacía: el proyecto se crea en la tarea del frontend.
