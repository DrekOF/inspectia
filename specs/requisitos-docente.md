<!--
Nombre: requisitos-docente.md
Descripción: Requisitos del docente (RP-01 a RP-25) y listado de requerimientos RF y RNF.
Fecha de creación: 12/10/2026
Actualización: 18/10/2026
Autor: Equipo InspectIA (exportado del documento de requerimientos v2.0)
Versión: 1.1
-->

# Requisitos del docente y listado de requerimientos

## Requisitos del proyecto (pautas del docente, obligatorios)

| Código | Requisito | Fase |
|---|---|---|
| RP-01 | API REST con el CRUD de las entidades del sistema, en Java con Spring Boot, JPA y Lombok. | Back-end |
| RP-02 | Base de datos PostgreSQL. | Back-end |
| RP-03 | Validaciones y seguridad en el back-end. | Back-end |
| RP-04 | Documentación con JavaDoc, comentarios y OpenAPI. | Back-end |
| RP-05 | Intercambio de datos en formato JSON. | Back-end |
| RP-06 | CRUD en React con Material UI, que consume la API en JSON. | Front-end |
| RP-07 | Validación y seguridad de los formularios con Yup y Formik. | Front-end |
| RP-08 | Interfaz responsive y accesible, con tokens de colores y estilos, y temas. | Front-end |
| RP-09 | Documentación con JSDoc y comentarios. | Front-end |
| RP-10 | Header en cada artefacto con nombre, descripción, fecha de creación, fecha de actualización, autor y versión. | Back-end y front-end |
| RP-11 | Internacionalización. | Back-end y front-end |
| RP-12 | Pruebas unitarias, de integración, de sistema, de seguridad, de mutación, de estrés y de límite. | Pruebas |
| RP-13 | Listado completo de casos de prueba, derivados de los criterios de aceptación de cada requerimiento, y verificación de su cumplimiento. | Pruebas |
| RP-14 | Herramientas de prueba: JUnit, Spring Test y Mockito en el back-end; Playwright y Selenium en el front-end. | Pruebas |
| RP-15 | Scripts de creación e inicialización de la base de datos. | Despliegue |
| RP-16 | Scripts de compilación y prueba del front-end y del back-end. | Despliegue |
| RP-17 | Scripts para la gestión del repositorio en GitHub. | Despliegue |
| RP-18 | Integración continua: al detectar cambios en el repositorio, compilar y probar. | Despliegue |
| RP-19 | Si las pruebas pasan, desplegar a producción; si fallan, crear issues o tareas y notificar a los integrantes. | Despliegue |
| RP-20 | Definir las métricas del proyecto y cómo se evalúan. | Administración, control y monitoreo |
| RP-21 | Seguimiento del desempeño y la producción de cada integrante, en Trello (D-12). | Administración, control y monitoreo |
| RP-22 | Monitoreo de errores y de tareas. | Administración, control y monitoreo |
| RP-23 | Generación automatizada: las entidades se definen en JSON y scripts en Python generan la base de datos, el back-end y el front-end, para poder cambiar de temática. | Transversal |
| RP-24 | Registro paso a paso de cómo se construyó el proyecto, para la sustentación. | Transversal |
| RP-25 | Entrenar 5 modelos de clasificación de imágenes: 2 transformers y 3 CNN (D-18). | Modelo de IA |

## Requerimientos funcionales (detalle en fichas.json)

| Código | Requerimiento | Prioridad |
|---|---|---|
| RF-01 | Iniciar sesión según el rol: usuario final, administrador de empresa o administrador de la plataforma. | Alta |
| RF-02 | Registrar empresas y su primer administrador. | Alta |
| RF-03 | Gestionar los usuarios de la empresa (crear, editar y desactivar; máximo 25 por empresa). | Alta |
| RF-04 | Registrar un lote. | Alta |
| RF-05 | Tomar o subir las fotos de la muestra del lote, con guía de captura. | Alta |
| RF-06 | Validar la calidad de cada foto (nitidez y luz) y pedir repetirla si no sirve. | Media |
| RF-07 | Separar y recortar los granos de cada foto, con control de granos pegados (D-09). | Alta |
| RF-08 | Clasificar cada grano como sano o no sano con el modelo de IA. | Alta |
| RF-09 | Pedir una foto más clara o más fotos cuando la confianza del modelo es baja. | Media |
| RF-10 | Calcular el porcentaje de granos sanos del lote. | Alta |
| RF-11 | Mostrar el resultado con la foto marcada (cada grano sano o no sano). | Media |
| RF-12 | Generar la recomendación según los rangos de la empresa (D-10). | Alta |
| RF-13 | Configurar los rangos de recomendación de la empresa. | Media |
| RF-14 | Consultar el historial de lotes evaluados. | Alta |
| RF-15 | Descargar el reporte del lote en PDF. | Media |
| RF-16 | Consultar las estadísticas de la empresa. | Media |
| RF-17 | Consultar las métricas del modelo: F1-score, precisión, recall y confianza promedio (D-14). | Baja |
| RF-18 | Cambiar la propia contraseña, escribiendo la actual (D-30). | Media |

## Requerimientos no funcionales (detalle en fichas.json)

| Código | Requerimiento | Tipo | Prioridad |
|---|---|---|---|
| RNF-01 | Entregar el resultado del análisis de un lote en 5 a 8 segundos. | Rendimiento | Alta |
| RNF-02 | Soportar hasta 30 empresas con 25 usuarios cada una (750 usuarios). | Capacidad | Media |
| RNF-03 | Mantener separados los datos de cada empresa (multi-tenant). | Seguridad | Alta |
| RNF-04 | Exigir autenticación y restringir cada función según el rol. | Seguridad | Alta |
| RNF-05 | Funcionar en PC y en celular desde el navegador (web responsive). | Portabilidad | Alta |
| RNF-06 | Ser sencilla de usar para el usuario final. | Usabilidad | Media |
| RNF-07 | Alcanzar el F1-score mínimo que se defina para el modelo (P-02). | Desempeño del modelo | Alta |
| RNF-08 | Citar a los autores del dataset y no usarlo con fines comerciales (licencia CC BY-NC 4.0). | Legal | Alta |

## Pautas del docente por fase (textuales)

**3) Back-end:** genere un CRUD con rest api con java, spring-boot, jpa, lombok; base de datos postgresql; para una entidad producto que tiene los atributos id, nombre, categoria, descripcion, estado; que tenga validaciones y seguridad; documentacion JavaDoc, Comentarios, OpenAPI; genere un Header para cada artefacto que tenga nombre, descripcion, fecha_creacion, actualizacion, autor, version; con un formato de intercambio json; que implemente internacionalizacion; genere los casos de prueba de acuerdo a los criterios de aceptación.

**4) Front-end:** genere un CRUD por formato de intercambio json con react y material ui; validación y seguridad en los formularios con yup y formik; responsive, accesibilidad, tokens de colores-estilos, temas; documentacion JsDoc, Comentarios; genere un Header para cada artefacto que tenga nombre, descripcion, fecha_creacion, actualizacion, autor, versión; que implemente internacionalización.

**5) Pruebas:** genere pruebas unitarias, de integracion, de sistema, de seguridad y de mutacion, pruebas de estrés, de límite; que liste todos los casos de prueba; que utilice para back-end jUnit, SpringTest, Mockito y para front-end Playwright, Selenium; testear el cumplimiento de los criterios de aceptación.

**6) Deployment:** genere scripts de creación e inicialización de la base de datos; genere los scripts de compilación y prueba del front-end y back-end; los scripts de los comandos para la gestión del repositorio; detecte los cambios en el repositorio y compile y pruebe; al probar si no hay errores envíe a producción (despliegue); al probar si hay errores cree los issues, tareas y notifique a los integrantes.

**7) Administración + control + monitoreo:** definir las métricas y su evaluación, monitoreos; evaluar el desempeño y producción de los integrantes; monitoreo de errores, tareas.

**Enfoque del docente:** automatizar al máximo. Definir las entidades en JSON y usar scripts de Python que generen la base de datos, el back-end, el front-end, etc., como una plantilla, para poder cambiar de temática (por ejemplo, otro producto en lugar de cacao). Hay que sustentar paso a paso cómo se construyó el proyecto.
