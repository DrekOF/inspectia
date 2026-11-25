<!--
Nombre: LEEME.md
Descripción: De dónde salen las fuentes del reporte PDF del lote (RF-15) y su licencia.
Fecha de creación: 24/11/2026
Actualización: 24/11/2026
Autor: Equipo InspectIA
Versión: 1.0
-->

# Fuentes del reporte PDF

El reporte PDF del lote (RF-15) usa Roboto, la letra del prototipo y de la página, embebida en el PDF: así se ve igual
en el PC y en el celular (RF-15 C3) y escribe las tildes, la eñe, «», · y – del español.

| Archivo | Peso | Uso en el reporte |
|---|---|---|
| `Roboto-Regular.ttf` | 400 | Texto |
| `Roboto-Medium.ttf` | 500 | Títulos y etiquetas |
| `Roboto-Bold.ttf` | 700 | La cifra del resultado y la categoría |

- Origen: el subconjunto latino de Roboto del paquete `@fontsource/roboto` 5.3.0, el mismo que usa la página
  (`frontend/node_modules/@fontsource/roboto/files/roboto-latin-<peso>-normal.woff`). El WOFF se pasó a TTF sin cambiar
  sus tablas: solo se descomprimieron, porque Apache PDFBox no lee WOFF.
- El subconjunto latino trae U+0000–00FF (con las tildes, la eñe, «, », ¿, ¡ y ·), la puntuación general (U+2000–206F,
  con – y —), € y ™. Un texto del reporte con otro carácter no se puede escribir: el adaptador lo cambia por «?».
- Licencia: SIL Open Font License 1.1, en `OFL.txt` (Copyright 2011 The Roboto Project Authors). Permite usarla,
  embeberla y redistribuirla con el programa.
