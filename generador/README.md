<!--
Nombre: README.md (generador)
Descripción: Qué va en la carpeta generador/ de InspectIA.
Fecha de creación: 12/10/2026
Actualización: 12/10/2026
Autor: Equipo InspectIA
Versión: 1.0
-->

# generador

Scripts de Python 3.13 que leen las entidades de `specs/entidades/*.json` y generan sus CRUD completos (RP-23):

- Las tablas de PostgreSQL.
- El backend por capas: entidad, repositorio, servicio, controlador y pruebas.
- Las pantallas del frontend: lista y formulario con Formik y Yup, con textos en español e inglés.

Regla clave: el código generado **no se edita a mano**. Para cambiar un CRUD, se cambia el JSON o la plantilla y se vuelve a generar. La meta es poder cambiar de temática (otro producto en lugar de cacao) cambiando solo los JSON.

Todavía está vacía: los scripts y las plantillas se crean en la tarea del generador.
