# Nombre: nombres.py
# Descripción: Nombres de Java y literales de Java y de .properties a partir de los nombres de los JSON.
# Fecha de creación: 13/10/2026
# Actualización: 20/10/2026
# Autor: Equipo InspectIA
# Versión: 1.1
"""Conversión de nombres (snake_case de los JSON a camelCase y PascalCase de Java), escritura de
literales de Java y de archivos ``.properties``, y etiquetas dentro de una frase."""

import textwrap

#: Paquete base del backend (D-21).
PAQUETE_BASE = 'co.edu.usco.inspectia'


def camel(nombre):
    """``fecha_recepcion`` → ``fechaRecepcion``."""
    primera, *resto = nombre.split('_')
    return primera + ''.join(p.capitalize() for p in resto)


def pascal(nombre):
    """``rango_recomendacion`` → ``RangoRecomendacion``."""
    return ''.join(p.capitalize() for p in nombre.split('_'))


def en_frase(etiqueta):
    """La etiqueta dentro de una frase: en minúscula, salvo las siglas como NIT."""
    return etiqueta if etiqueta.isupper() else etiqueta[0].lower() + etiqueta[1:]


def texto_java(valor):
    """Literal de texto de Java, con comillas y barras escapadas."""
    escapado = (str(valor).replace('\\', '\\\\').replace('"', '\\"')
                .replace('\n', '\\n').replace('\r', '\\r').replace('\t', '\\t'))
    return f'"{escapado}"'


def valor_properties(valor):
    """Valor de un archivo ``.properties`` (UTF-8): barras, saltos y espacios del inicio escapados."""
    escapado = str(valor).replace('\\', '\\\\').replace('\n', '\\n').replace('\r', '\\r')
    if escapado.startswith(' '):
        escapado = '\\' + escapado
    return escapado


def envolver(texto, sangria, ancho=104):
    """Parte un texto en líneas de un comentario: la primera sin sangría y las demás con ``sangria``.

    Sirve para la descripción de los headers (RP-10), que debe verse igual en cada generación.
    """
    lineas = textwrap.wrap(texto, width=ancho - len(sangria), break_long_words=False,
                           break_on_hyphens=False)
    return ('\n' + sangria).join(lineas)
