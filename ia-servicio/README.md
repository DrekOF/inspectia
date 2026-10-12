<!--
Nombre: README.md (ia-servicio)
Descripción: Qué va en la carpeta ia-servicio/ de InspectIA.
Fecha de creación: 12/10/2026
Actualización: 12/10/2026
Autor: Equipo InspectIA
Versión: 1.0
-->

# ia-servicio

Servicio de IA de InspectIA, en Python 3.13 con OpenCV y ONNX Runtime. El servidor (`backend/`) le envía los enlaces de S3 de las fotos del lote, y el servicio responde por cada grano su posición, su clase y su confianza.

Qué va aquí:

- Revisar la nitidez y la luz de cada foto (RF-06).
- Separar y recortar los granos, con control de granos pegados (RF-07, D-09).
- Clasificar cada grano como sano o no sano con el modelo elegido (RF-08).

Julián entrena 5 modelos y la app usa uno solo (D-18). El servicio carga ese modelo al arrancar, según una variable de configuración. Los archivos `.onnx` se guardan en S3, no en este repositorio, y la app no reentrena modelos (D-03).

La imagen de Docker usa `python:3.13-slim` (Debian), que existe para amd64 y arm64. No se usa Alpine porque OpenCV y ONNX Runtime no publican paquetes para ella.

Todavía está vacía: el servicio se crea en su propia tarea.
