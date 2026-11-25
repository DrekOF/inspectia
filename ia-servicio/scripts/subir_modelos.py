# Nombre: subir_modelos.py
# Descripción: Baja de la release modelos-v1.0 de Julián los 5 modelos, sus fichas y el manifest, comprueba el
#              SHA-256 de cada uno con modelos.json y con el manifest, y los sube a S3 bajo modelos/ (D-16, D-18,
#              D-40). Lo corre un integrante con sus credenciales de AWS; la app no lo usa.
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.0
"""Sube los modelos de la release ``modelos-v1.0`` al bucket de archivos.

Uso, desde la raíz del repositorio, con el entorno del servicio (``ia-servicio/.venv``) y credenciales de AWS con
permiso de escribir en el bucket (por ejemplo, ``aws sso login`` o un perfil)::

    ia-servicio/.venv/Scripts/python ia-servicio/scripts/subir_modelos.py --bucket inspectia-archivos-<cuenta>

Opciones: ``--prefijo`` (por defecto ``modelos/``), ``--region`` (``us-east-1``), ``--perfil`` (un perfil de
``~/.aws``), ``--carpeta`` (dónde guardar lo bajado, para no bajarlo otra vez; por defecto, una carpeta temporal)
y ``--simular`` (baja y comprueba, pero no sube nada).

Los archivos conservan el nombre de la release (``modelos/inspectia_EfficientNet-B0_v1.0_savedmodel.zip``). Cada
objeto se sube con su SHA-256 (``x-amz-checksum-sha256``: S3 rechaza el objeto si no llega igual) y en sus
metadatos (``sha256``); si ya está en S3 con el mismo SHA-256, no se vuelve a subir. Si un SHA-256 no coincide,
no sube nada y termina con código 1.
"""

import argparse
import base64
import sys
import tempfile
import urllib.request
from pathlib import Path

CARPETA_DEL_SERVICIO = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(CARPETA_DEL_SERVICIO))

from inspectia_ia.catalogo import cargar  # noqa: E402
from inspectia_ia.modelos import sha256_de  # noqa: E402

TIPOS = {'.zip': 'application/zip', '.json': 'application/json', '.keras': 'application/octet-stream'}


class ErrorDeSubida(RuntimeError):
    """Un archivo no se pudo bajar o no tiene el SHA-256 esperado."""


def bajar_url(url, destino):
    """Baja ``url`` a ``destino`` (solo HTTPS)."""
    if not url.startswith('https://'):
        raise ErrorDeSubida(f'Solo se baja por HTTPS: {url}')
    with urllib.request.urlopen(url, timeout=120) as respuesta, open(destino, 'wb') as f:  # noqa: S310
        while bloque := respuesta.read(1024 * 1024):
            f.write(bloque)


def _bajar_comprobado(url_base, archivo, esperado, carpeta, bajar, salida):
    """Deja ``archivo`` en ``carpeta`` con el SHA-256 ``esperado``; lo baja solo si falta o no coincide."""
    destino = carpeta / archivo
    if destino.is_file() and sha256_de(destino) == esperado:
        print(f'  ya bajado: {archivo}', file=salida)
        return destino
    print(f'  bajando: {archivo}', file=salida)
    bajar(url_base + archivo, destino)
    encontrado = sha256_de(destino)
    if encontrado != esperado:
        destino.unlink()
        raise ErrorDeSubida(f'El SHA-256 de {archivo} no coincide: se esperaba {esperado} y llegó {encontrado}')
    return destino


def preparar(catalogo, carpeta, bajar=bajar_url, salida=sys.stdout):
    """Baja y comprueba el manifest y todos los archivos del catálogo.

    Cada archivo se compara con ``modelos.json`` y con el ``manifest.json`` de la release.

    Returns:
        list[tuple[Path, str]]: cada archivo y su SHA-256.
    """
    import json

    carpeta.mkdir(parents=True, exist_ok=True)
    manifest = _bajar_comprobado(catalogo.url_release, catalogo.manifest.nombre, catalogo.manifest.sha256,
                                 carpeta, bajar, salida)
    del_manifest = json.loads(manifest.read_text(encoding='utf-8'))['archivos']
    listos = [(manifest, catalogo.manifest.sha256)]
    for archivo in catalogo.archivos()[1:]:
        if del_manifest.get(archivo.nombre) != archivo.sha256:
            raise ErrorDeSubida(f'{archivo.nombre}: modelos.json y el manifest de la release no dicen el mismo '
                                f'SHA-256')
        listos.append((_bajar_comprobado(catalogo.url_release, archivo.nombre, archivo.sha256, carpeta, bajar,
                                         salida), archivo.sha256))
    return listos


def subir(cliente, bucket, prefijo, listos, salida=sys.stdout):
    """Sube cada archivo a ``s3://bucket/prefijo``, salvo los que ya están con el mismo SHA-256.

    Returns:
        list[str]: las llaves que se subieron.
    """
    from botocore.exceptions import ClientError

    subidas = []
    for ruta, sha in listos:
        llave = prefijo + ruta.name
        try:
            actual = cliente.head_object(Bucket=bucket, Key=llave).get('Metadata', {}).get('sha256')
        except ClientError as error:
            if error.response.get('Error', {}).get('Code') not in ('404', 'NoSuchKey', 'NotFound'):
                raise
            actual = None
        if actual == sha:
            print(f'  ya está en S3: s3://{bucket}/{llave}', file=salida)
            continue
        with open(ruta, 'rb') as f:
            cliente.put_object(Bucket=bucket, Key=llave, Body=f,
                               ContentType=TIPOS.get(ruta.suffix, 'application/octet-stream'),
                               ChecksumSHA256=base64.b64encode(bytes.fromhex(sha)).decode('ascii'),
                               Metadata={'sha256': sha})
        print(f'  subido: s3://{bucket}/{llave}', file=salida)
        subidas.append(llave)
    return subidas


def main(argv=None, cliente=None, bajar=bajar_url, salida=sys.stdout):
    """Punto de entrada.

    Args:
        argv: los argumentos (por defecto, los de la línea de comandos).
        cliente: un cliente de S3 (para las pruebas); si falta, se crea con boto3.
        bajar: función ``(url, destino)`` que baja un archivo (para las pruebas).

    Returns:
        int: 0 si todo salió bien; 1 si un SHA-256 no coincide o no se pudo bajar.
    """
    p = argparse.ArgumentParser(description='Sube a S3 los modelos de la release modelos-v1.0 de Julián.')
    p.add_argument('--bucket', required=True, help='el bucket de archivos, como inspectia-archivos-<cuenta>')
    p.add_argument('--prefijo', default='modelos/', help='el prefijo en el bucket (por defecto, modelos/)')
    p.add_argument('--region', default='us-east-1', help='la región (por defecto, us-east-1)')
    p.add_argument('--perfil', help='un perfil de ~/.aws (por defecto, las credenciales del entorno)')
    p.add_argument('--carpeta', type=Path, help='dónde guardar lo bajado (por defecto, una carpeta temporal)')
    p.add_argument('--catalogo', type=Path, default=CARPETA_DEL_SERVICIO / 'modelos.json', help=argparse.SUPPRESS)
    p.add_argument('--simular', action='store_true', help='baja y comprueba, pero no sube nada')
    args = p.parse_args(argv)
    prefijo = args.prefijo if args.prefijo.endswith('/') or not args.prefijo else args.prefijo + '/'
    catalogo = cargar(args.catalogo)

    with tempfile.TemporaryDirectory(prefix='inspectia-modelos-') as temporal:
        carpeta = args.carpeta or Path(temporal)
        print(f'Bajando la release {catalogo.etiqueta} a {carpeta}', file=salida)
        try:
            listos = preparar(catalogo, carpeta, bajar, salida)
        except (ErrorDeSubida, OSError) as error:
            print(f'Error: {error}. No se subió nada.', file=sys.stderr)
            return 1
        print(f'Los {len(listos)} archivos tienen el SHA-256 esperado.', file=salida)
        if args.simular:
            print('Simulación: no se subió nada.', file=salida)
            return 0
        if cliente is None:
            import boto3
            sesion = boto3.Session(profile_name=args.perfil, region_name=args.region)
            cliente = sesion.client('s3')
        print(f'Subiendo a s3://{args.bucket}/{prefijo}', file=salida)
        subidas = subir(cliente, args.bucket, prefijo, listos, salida)
        print(f'Listo: {len(subidas)} subidos, {len(listos) - len(subidas)} ya estaban.', file=salida)
    return 0


if __name__ == '__main__':
    sys.exit(main())
