# Nombre: __init__.py (inspectia_generador)
# Descripción: Paquete del generador de InspectIA (RP-23).
# Fecha de creación: 13/10/2026
# Actualización: 26/10/2026
# Autor: Equipo InspectIA
# Versión: 1.7
"""Generador de InspectIA (RP-23).

Lee ``specs/entidades/*.json`` y genera las tablas de la base de datos, los textos en español e
inglés y el código de los CRUD: la persistencia de todos, la API de los de ``alcance.json`` y, de ellos, la
pantalla del frontend de los de ``pantallas``.
Lo generado queda en carpetas que solo escribe el generador y no se edita a mano.
"""

VERSION = '1.7'
