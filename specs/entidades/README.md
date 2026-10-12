<!--
Nombre: README.md (specs/entidades)
Descripción: Qué va en la carpeta specs/entidades/ de InspectIA.
Fecha de creación: 12/10/2026
Actualización: 12/10/2026
Autor: Equipo InspectIA
Versión: 1.0
-->

# specs/entidades

Un archivo JSON por cada entidad con CRUD. Es la entrada de `generador/` (RP-23):

- `empresa.json`: empresas (RF-02).
- `usuario.json`: usuarios de cada empresa (RF-01 y RF-03).
- `rango_recomendacion.json`: rangos de recomendación de cada empresa (RF-13).

Cada JSON describe la entidad, sus atributos, sus validaciones y sus textos en español e inglés. El formato exacto se define en la tarea del generador. Los nombres van en español y sin tildes, igual que en el código.

Para cambiar de temática, solo se cambian estos archivos y se vuelve a generar.
