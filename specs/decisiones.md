<!--
Nombre: decisiones.md
Descripción: Decisiones (D-xx) y pendientes (P-xx) del proyecto. Fuente: 01_Requerimientos/InspectIA_Requerimientos.docx, anexo A.
Fecha de creación: 12/10/2026
Actualización: 12/10/2026
Autor: Equipo InspectIA (exportado del documento de requerimientos v2.0)
Versión: 1.0
-->

# Decisiones y pendientes

Copia de trabajo del anexo A del documento de requerimientos (v2.0). Si una tarea choca con una decisión, pregunta antes de cambiarla.

## Decisiones

| ID | Decisión | Fecha |
|---|---|---|
| D-01 | El producto del caso es el grano de cacao. | 11/10/2026 |
| D-02 | El resultado del lote se expresa como porcentaje de granos sanos, no como «madurez», porque el dataset clasifica cada grano como sano o no sano. | 11/10/2026 |
| D-03 | Federated learning es la metodología con la que Julián entrena los modelos. La versión 1 usa el modelo ya entrenado y no lo reentrena. | 11/10/2026 |
| D-04 | El proyecto se llama InspectIA. El nombre no depende del producto, por si se cambia de temática. | 11/10/2026 |
| D-05 | Aplicación web responsive (PC y celular), multiempresa, con dos roles por empresa: usuario final y administrador. | 11/10/2026 |
| D-06 | El modelo de IA es un componente del proyecto: Julián entrega los modelos entrenados y el elegido se despliega como servicio (D-18). | 11/10/2026 |
| D-07 | Convenciones del documento: códigos RF-01 y RNF-01, versión inicial 1.0, Prioridad y Dificultad Alta / Media / Baja marcadas con X, y Estado Propuesto / Aprobado / Implementado. | 11/10/2026 |
| D-08 | Captura y análisis del lote (resuelve P-03): el usuario fotografía una muestra del lote (granos enteros sobre papel blanco, separados entre sí, con luz difusa, 2 o 3 fotos desde arriba). La app separa y recorta cada grano con un script de OpenCV, el modelo clasifica cada recorte y se calcula el porcentaje de granos sanos. El modelo se entrena con recortes hechos por el mismo script a partir de las fotos originales del dataset. | 11/10/2026 |
| D-09 | Control de granos pegados: si en una foto una región mide el doble del tamaño típico de los granos de esa misma foto, la app avisa que hay granos pegados y pide repetir la foto. | 11/10/2026 |
| D-10 | Criterios de la recomendación (resuelve P-05): según el porcentaje de granos sanos, la app indica una categoría probable basada en la NTC 1252:2021 y una acción sugerida (rangos en A.3). La categoría se presenta como probable y la oficial se confirma con la prueba de corte. Si el resultado queda a menos de 3 puntos de un límite, la app sugiere analizar otra muestra. Los rangos vienen por defecto y el administrador de cada empresa puede ajustarlos. | 11/10/2026 |
| D-11 | El documento separa los requerimientos (lo que debe hacer la app: RF y RNF) de los requisitos del proyecto (las pautas del docente para construirlo: RP). | 11/10/2026 |
| D-12 | El punto 7 del docente (administración, control y monitoreo) se refiere al seguimiento del equipo, que se llevará en Trello. | 11/10/2026 |
| D-13 | Las empresas las registra un administrador de la plataforma (rol del equipo InspectIA), que también crea el primer administrador de cada empresa (resuelve P-07). | 11/10/2026 |
| D-14 | El administrador de empresa ve las métricas de evaluación del modelo (F1-score, precisión y recall medidos en validación) y la confianza promedio de los análisis de su empresa (resuelve P-06). | 11/10/2026 |
| D-15 | El reporte de cada lote se descarga en PDF. | 11/10/2026 |
| D-16 | Despliegue en AWS con arquitectura de bajo costo (resuelve P-08): región us-east-1; CloudFront y S3 para el front-end; una instancia EC2 t4g.medium con Docker Compose (API Spring Boot, servicio de IA y PostgreSQL con copia diaria a S3); S3 para fotos y reportes; ECR, CloudWatch, SNS, EventBridge Scheduler y AWS Budgets. Costo estimado: ≈ US$31 al mes encendida 24/7 o ≈ US$16 apagándola de noche. Detalle en 02_Analisis_Diseno/InspectIA_Despliegue_AWS.pdf. | 12/10/2026 |
| D-17 | Diferenciación de InspectIA frente a soluciones similares (resuelve P-09): evalúa granos enteros sin cortar la muestra; entrega el resultado por lote; es un producto completo para la empresa (multiempresa, historial, reporte en PDF y estadísticas); recomienda según la NTC 1252:2021 con rangos ajustables por empresa; y está en español, para el mercado colombiano. Límite aceptado: no mide la fermentación interna. Detalle en el Anexo B. | 12/10/2026 |
| D-18 | Uso de los 5 modelos (RP-25): Julián los entrena con federated learning, los mismos recortes de entrenamiento y el mismo conjunto de validación, y los compara por F1-score, precisión, recall y tiempo por grano. La app usa el de mejor F1-score entre los que permitan cumplir el tiempo de respuesta (RNF-01). La comparación de los 5 queda documentada. | 12/10/2026 |
| D-19 | El caso es real, pero el problema es general del sector cacaotero: los requerimientos no salen de una sola empresa, sino del problema común de productores y empresas (resuelve P-10). | 12/10/2026 |
| D-20 | Las fichas del modelo de IA (RF-08 y RNF-07) se redactan en este documento y Julián las revisa antes de aprobarlas (resuelve P-04). | 12/10/2026 |
| D-21 | Arquitectura del software. Sistema: página web (React), servidor (Spring Boot) y servicio de IA (Python), con PostgreSQL y S3; el servidor es un solo programa dividido en módulos (monolito modular), sin microservicios. Dentro del servidor, el análisis del lote usa arquitectura hexagonal: las reglas del negocio quedan en el centro y se conectan con la IA, S3, el PDF y la base de datos por interfaces. Empresas, usuarios, rangos y estadísticas van por capas, y sus CRUD se generan desde los JSON (RP-23). Detalle en 02_Analisis_Diseno/InspectIA_Despliegue_AWS.pdf. | 12/10/2026 |
| D-22 | El equipo aprueba las prioridades de los requerimientos (sección 4). RNF-08 sube a Alta porque la licencia del dataset es obligatoria, aunque cumplirla es fácil. La dificultad de cada RF está en su ficha. | 12/10/2026 |
| D-23 | Tamaño de la muestra: entre 100 y 150 granos por análisis, repartidos en 2 o 3 fotos (unos 50 granos por foto). Con 100 granos o más, cada grano pesa como máximo 1 punto del porcentaje, en línea con los límites de la NTC 1252, que se expresan por cada 100 granos (A.3). Con este tamaño se mide el tiempo de respuesta (RNF-01). | 12/10/2026 |

## Pendientes

| ID | Pendiente | Responsable |
|---|---|---|
| P-01 | Definir las 5 arquitecturas (2 transformers y 3 CNN) y entregar su comparación (D-18). | Julián |
| P-02 | Definir la precisión mínima aceptable del modelo, medida con F1-score: el dataset tiene 77 % de granos sanos, así que la exactitud sola da una idea engañosa. | Julián |
| P-03 | Definir cómo se fotografía el lote. Resuelto: ver D-08 y D-09. | — |
| P-04 | Definir quién redacta las fichas de requerimientos del modelo de IA. Resuelto: ver D-20. | — |
| P-05 | Definir los criterios de la recomendación. Resuelto: ver D-10 y A.3. | — |
| P-06 | Definir qué métricas del modelo ve el administrador. Resuelto: ver D-14. | — |
| P-07 | Definir quién registra las empresas en la plataforma. Resuelto: ver D-13. | — |
| P-08 | Confirmar el despliegue en AWS. Resuelto: ver D-16. | — |
| P-09 | Revisar soluciones similares para definir en qué se diferencia InspectIA. Resuelto: ver D-17 y el Anexo B. | — |
| P-10 | Nombre de la empresa del caso real (fuente de los requerimientos). Resuelto: no es una sola empresa, ver D-19. | — |
| P-11 | Nombre del docente para la portada. Resuelto: Juan Antonio Castro Silva. | — |
| P-12 | Probar con fotos reales de unos 50 granos cada una (D-23): la guía de captura, los umbrales de nitidez y luz, la separación de granos y el control de granos pegados (D-09). | Cristhian y equipo |
| P-13 | Probar el modelo con fotos de granos de cacao colombiano: el dataset es de granos de Ecuador y el resultado puede variar con granos de otra región. | Equipo |
| P-14 | Revisar las fichas RF-08 y RNF-07 (secciones 5 y 6), incluida la métrica propuesta: F1-score macro. | Julián |
| P-15 | Fijar con los resultados de validación el umbral de confianza baja y el porcentaje de granos dudosos que pide repetir una foto (10 % inicial, RF-09). Son parámetros del sistema. | Julián |
