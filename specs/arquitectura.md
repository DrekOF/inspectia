<!--
Nombre: arquitectura.md
Descripción: Resumen de la arquitectura del sistema y del software de InspectIA (D-16, D-18, D-21, D-23 y D-25). Detalle en 02_Analisis_Diseno/InspectIA_Despliegue_AWS.pdf.
Fecha de creación: 12/10/2026
Actualización: 13/10/2026
Autor: Equipo InspectIA
Versión: 1.1
-->

# Arquitectura de InspectIA

## 1. Piezas del sistema (D-16 y D-21)

| Pieza | Tecnología | Qué hace |
|---|---|---|
| Página web | React + Material UI | Lo que usan las personas, en PC y celular (web responsive). |
| Servidor | Java + Spring Boot | API REST con JSON: sesión y roles, empresas, usuarios, lotes, análisis, recomendación, historial, reportes PDF y estadísticas. |
| Servicio de IA | Python + OpenCV + ONNX | Revisa la calidad de las fotos, separa los granos y los clasifica con el modelo elegido. |
| Base de datos | PostgreSQL | Datos de todas las empresas en una base compartida; cada registro guarda su empresa (RNF-03). |
| Archivos | Amazon S3 | Fotos, reportes PDF y los 5 modelos ONNX con su comparación. |

- El servidor es un solo programa dividido en módulos (monolito modular). Sin microservicios.
- Despliegue en AWS (us-east-1): CloudFront + S3 para la página web; una EC2 t4g.medium con Docker Compose (servidor, servicio de IA y PostgreSQL); S3; ECR; CloudWatch + SNS; EventBridge Scheduler; AWS Budgets. Costo estimado: ≈ US$31 al mes 24/7 o ≈ US$16 apagándola de noche.
- Docker Compose y no ECS: con una sola máquina, Compose basta. Se pasaría a ECS si una máquina no alcanza o si el producto exige alta disponibilidad.
- Las fotos se suben directo a S3 con enlaces temporales (URL prefirmadas) que genera el servidor.
- CI/CD con GitHub Actions: si las pruebas pasan, publica el front-end en S3 y las imágenes en ECR y actualiza la instancia por SSM; si fallan, crea un issue y avisa al equipo. Conexión a AWS por OIDC, sin claves en el repositorio.

## 2. Organización del servidor (D-21)

```
backend/src/main/java/.../inspectia/
├── analisis/        hexagonal
│   ├── dominio/     reglas sin Spring: % de granos sanos (RF-10), categoría y sugerencia (RF-12)
│   ├── aplicacion/  caso de uso AnalizarLote y puertos (interfaces):
│   │                ClasificadorDeGranos, AlmacenDeArchivos, GeneradorDeReporte, repositorios
│   └── adaptadores/ REST, cliente HTTP al servicio de IA, S3, PDF, JPA
├── empresa/         capas, generado desde specs/entidades/empresa.json
├── usuario/         capas, generado desde specs/entidades/usuario.json
├── rango/           capas, generado desde specs/entidades/rango_recomendacion.json
├── estadistica/     capas, a mano
└── comun/           seguridad (JWT y roles), i18n, manejo de errores
```

- Hexagonal solo en `analisis`, porque ahí están las reglas del negocio y las piezas de afuera que pueden cambiar (modelo de IA, S3, PDF). Las reglas se prueban con un clasificador falso, sin encender el servicio de IA.
- Mientras Julián entrega el modelo, `ClasificadorDeGranos` usa un adaptador falso para desarrollo y pruebas.
- Los CRUD (empresa, usuario, rango) no se escriben a mano: los genera `generador/` (RP-23).

## 3. Flujo del análisis de un lote

| Paso | Requerimiento | Dónde |
|---|---|---|
| 1. Registrar el lote | RF-04 | Servidor |
| 2. Tomar o subir 2 o 3 fotos de la muestra (la guía pide de 100 a 150 granos; se aceptan más, D-23 y D-26) | RF-05 | Página web → S3 |
| 3. Revisar nitidez y luz | RF-06 | Servicio de IA |
| 4. Separar y recortar los granos; granos pegados; mínimo 100 granos | RF-07 | Servicio de IA |
| 5. Clasificar cada grano (posición, clase y confianza) | RF-08 | Servicio de IA |
| 6. Pedir repetir una foto si hay más de 10 % de granos dudosos | RF-09 | Servidor |
| 7. Calcular el % de granos sanos del lote | RF-10 | Servidor (dominio) |
| 8. Categoría probable y recomendación; lote «Analizado». Si el resultado queda a menos de 1 punto de un límite, sugiere otra muestra (D-24) | RF-12 | Servidor (dominio) |
| 9. Mostrar la foto marcada y descargar el PDF | RF-11 y RF-15 | Página web y servidor |
| 10. Si la app la sugirió, segunda muestra: pasos 2 a 9 con fotos nuevas. El resultado del lote suma los granos de las 2 muestras, ya no sugiere otra y el PDF se rehace (D-25) | RF-05 a RF-15 | Todas las piezas |

El servidor le envía al servicio de IA las fotos de la muestra (sus enlaces de S3) y recibe, por cada grano, la posición, la clase y la confianza. El contrato exacto (OpenAPI) se define en la fase de diseño. Las tablas del flujo (lote, muestra, foto y grano) están en `specs/modelo-datos.md`.

## 4. Modelos de IA (D-18)

- Julián entrena 5 modelos (2 transformers y 3 CNN) con federated learning, fuera de AWS, con los mismos recortes, y los compara en el mismo conjunto de validación (F1-score macro, precisión, recall y tiempo por grano).
- La app usa uno solo: el de mejor F1-score entre los que permitan cumplir el tiempo de respuesta (RNF-01).
- Los 5 ONNX y la comparación se guardan en S3; el servicio de IA carga solo el elegido al arrancar, según una variable de configuración.
- La comparación de los 5 también queda en la tabla `modelo` de la base de datos (tipo, métricas y tiempo por grano), con el elegido marcado en uso. El script de base de datos carga esas filas (RP-15).

## 5. Módulos y requerimientos

| Módulo | Organización | Requerimientos |
|---|---|---|
| Análisis del lote | Hexagonal | RF-04 a RF-12, RF-14 y RF-15 |
| Empresas | Capas (generado) | RF-02 |
| Usuarios | Capas (generado) | RF-01 y RF-03 |
| Rangos de recomendación | Capas (generado) | RF-13 |
| Estadísticas | Capas | RF-16 y RF-17 |
