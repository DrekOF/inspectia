# Nombre: casos_de_prueba.py
# Descripción: Línea de comandos que arma docs/casos-de-prueba.md desde las marcas de las pruebas y
#              specs/fichas.json, y comprueba que esté al día (RP-13).
# Fecha de creación: 13/10/2026
# Actualización: 14/10/2026
# Autor: Equipo InspectIA
# Versión: 1.1
"""Arma la lista de casos de prueba (RP-13) desde las marcas de las pruebas y ``specs/fichas.json``.

Uso, desde la raíz del repositorio (en Linux o macOS, ``generador/.venv/bin/python``)::

    generador/.venv/Scripts/python generador/casos_de_prueba.py generar     # escribe docs/casos-de-prueba.md
    generador/.venv/Scripts/python generador/casos_de_prueba.py comprobar   # código 1 si no está al día

Los dos terminan con código 1 si una prueba no tiene marca o si su marca cita algo que no existe.
Con ``--sin-pendientes`` también terminan con código 1 si un criterio no tiene prueba: sirve para
revisar RP-13 al cerrar el proyecto.

Si una ficha está «Implementado» y tiene criterios sin prueba, ``comprobar`` termina con código 1 y
``generar`` solo lo avisa: según D-29, esa ficha todavía no está implementada.
"""

import argparse
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent

try:
    from inspectia_generador.casos import RUTA_DOCUMENTO, armar_documento
except ModuleNotFoundError as error:
    sys.exit(f'Falta la dependencia {error.name}. Crea el entorno del generador con:\n'
             '  py -3.13 -m venv generador/.venv\n'
             '  generador/.venv/Scripts/python -m pip install -r generador/requirements-dev.txt')


def main(argumentos=None):
    """Ejecuta un comando y devuelve el código de salida (0 si todo está bien)."""
    parser = argparse.ArgumentParser(description='Casos de prueba de InspectIA (RP-13).')
    parser.add_argument('comando', choices=['generar', 'comprobar'])
    parser.add_argument('--sin-pendientes', action='store_true',
                        help='termina con código 1 si algún criterio no tiene prueba')
    parser.add_argument('--raiz', type=Path, default=RAIZ, help='raíz del repositorio')
    args = parser.parse_args(argumentos)

    resultado = armar_documento(args.raiz)
    if resultado.errores:
        print(f'{len(resultado.errores)} errores en las marcas de las pruebas:')
        for error in resultado.errores:
            print(f'  - {error}')
        return 1

    destino = args.raiz / RUTA_DOCUMENTO
    if args.comando == 'generar':
        actual = destino.read_bytes().decode('utf-8').replace('\r\n', '\n') if destino.is_file() else None
        if actual == resultado.texto:
            print(f'{RUTA_DOCUMENTO} ya estaba al día.')
        else:
            destino.parent.mkdir(parents=True, exist_ok=True)
            destino.write_bytes(resultado.texto.encode('utf-8'))
            print(f'Escrito {RUTA_DOCUMENTO}.')
    else:
        actual = destino.read_bytes().decode('utf-8').replace('\r\n', '\n') if destino.is_file() else None
        if actual != resultado.texto:
            print(f'{RUTA_DOCUMENTO} no está al día con las pruebas y las fichas: corre «generar».')
            return 1
        print(f'{RUTA_DOCUMENTO} está al día.')

    if resultado.implementadas_incompletas:
        print('Fichas en «Implementado» con criterios sin prueba (D-29):')
        for aviso in resultado.implementadas_incompletas:
            print(f'  - {aviso}')
        if args.comando == 'comprobar':
            return 1

    print(f'{len(resultado.pendientes)} criterios sin prueba.')
    if args.sin_pendientes and resultado.pendientes:
        print('Pendientes: ' + ', '.join(resultado.pendientes))
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
