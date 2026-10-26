# Nombre: generar.py
# Descripción: Línea de comandos del generador de InspectIA: validar, generar y comprobar (RP-23).
# Fecha de creación: 13/10/2026
# Actualización: 26/10/2026
# Autor: Equipo InspectIA
# Versión: 1.2
"""Línea de comandos del generador de InspectIA (RP-23).

Uso, desde la raíz del repositorio (en Linux o macOS, ``generador/.venv/bin/python``)::

    generador/.venv/Scripts/python generador/generar.py validar     # solo revisa los JSON
    generador/.venv/Scripts/python generador/generar.py generar     # escribe lo generado
    generador/.venv/Scripts/python generador/generar.py comprobar   # compara sin escribir

``comprobar`` termina con código 1 si lo generado en disco no coincide con lo que produce el
generador: alguien lo editó a mano o falta regenerar después de cambiar un JSON o una plantilla.
"""

import argparse
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent

try:
    from inspectia_generador.carga import (cargar_alcance, cargar_codigos, cargar_entidades, cargar_pantallas,
                                           revisar_alcance)
    from inspectia_generador.generacion import generar_archivos
    from inspectia_generador.salida import comprobar, escribir
    from inspectia_generador.sql import ErrorGeneracion
    from inspectia_generador.validacion import validar
except ModuleNotFoundError as error:
    sys.exit(f'Falta la dependencia {error.name}. Crea el entorno del generador con:\n'
             '  py -3.13 -m venv generador/.venv\n'
             '  generador/.venv/Scripts/python -m pip install -r generador/requirements-dev.txt')


def main(argumentos=None):
    """Ejecuta un comando del generador y devuelve el código de salida (0 si todo está bien)."""
    parser = argparse.ArgumentParser(description='Generador de InspectIA (RP-23).')
    parser.add_argument('comando', choices=['validar', 'generar', 'comprobar'])
    parser.add_argument('--entidades', type=Path, nargs='+', default=[RAIZ / 'specs' / 'entidades'],
                        help='carpetas o archivos JSON de entidades (por defecto, specs/entidades)')
    parser.add_argument('--alcance', type=Path, default=RAIZ / 'generador' / 'alcance.json',
                        help='entidades con API y con pantalla (por defecto, generador/alcance.json)')
    parser.add_argument('--raiz', type=Path, default=RAIZ,
                        help='dónde se escribe o se comprueba lo generado (por defecto, el repositorio)')
    args = parser.parse_args(argumentos)

    entidades = cargar_entidades(*args.entidades)
    api = cargar_alcance(args.alcance)
    pantallas = cargar_pantallas(args.alcance)
    errores = validar(entidades, cargar_codigos(RAIZ / 'specs')) + revisar_alcance(api, entidades, pantallas)
    if errores:
        print(f'{len(errores)} errores en las entidades:')
        for error in errores:
            print(f'  - {error}')
        return 1
    if args.comando == 'validar':
        print(f'{len(entidades)} entidades válidas: {", ".join(entidades)}. Con API: {", ".join(api) or "ninguna"}. '
              f'Con pantalla: {", ".join(pantallas) or "ninguna"}.')
        return 0

    try:
        archivos = generar_archivos(entidades, api, pantallas)
    except ErrorGeneracion as error:
        print(error)
        return 1

    if args.comando == 'generar':
        resumen = escribir(archivos, args.raiz)
        print(f'{len(resumen.escritos)} escritos, {len(resumen.sin_cambios)} sin cambios, '
              f'{len(resumen.borrados)} borrados.')
        for ruta in resumen.escritos:
            print(f'  escrito: {ruta}')
        for ruta in resumen.borrados:
            print(f'  borrado: {ruta}')
        return 0

    diferencias = comprobar(archivos, args.raiz)
    if diferencias.vacia:
        print(f'Lo generado está al día ({len(archivos)} archivo{"" if len(archivos) == 1 else "s"}).')
        return 0
    print('Lo generado no coincide con las entidades y las plantillas:')
    for titulo, rutas in (('editado a mano o desactualizado', diferencias.modificados),
                          ('falta', diferencias.faltantes), ('sobra', diferencias.sobrantes)):
        for ruta in rutas:
            print(f'  {titulo}: {ruta}')
    print('Corre «generar» y revisa el diff; si alguien lo editó a mano, pasa ese cambio al JSON o a la plantilla.')
    return 1


if __name__ == '__main__':
    sys.exit(main())
