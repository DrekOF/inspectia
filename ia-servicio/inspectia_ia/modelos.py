# Nombre: modelos.py
# Descripción: Obtiene el archivo del modelo en uso desde S3 o desde una carpeta, comprueba su SHA-256 con
#              modelos.json, lo guarda en la caché (un volumen) y lo descomprime (D-18, D-40).
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.0
"""Obtención y comprobación del modelo en uso.

``INSPECTIA_MODELOS_URI`` dice de dónde se baja: ``s3://inspectia-archivos-<cuenta>/modelos/`` en AWS (con las
credenciales del rol de la instancia, nunca con claves en archivos) o ``file:///carpeta/`` en desarrollo y en la
CI. El archivo queda en la caché (``/var/lib/inspectia/modelos``, un volumen de Docker) para no bajarlo en cada
arranque, y se vuelve a comprobar cada vez. Si su SHA-256 no es el de ``modelos.json``, el servicio no arranca.
"""

import hashlib
import logging
import os
import shutil
import zipfile
from pathlib import Path, PurePosixPath
from urllib.parse import urlparse
from urllib.request import url2pathname

log = logging.getLogger(__name__)

_BLOQUE = 1024 * 1024


class ModeloInvalido(RuntimeError):
    """El modelo no se pudo obtener, su SHA-256 no coincide o no tiene la forma esperada."""


def sha256_de(ruta):
    """SHA-256 de un archivo, en hexadecimal y minúscula."""
    h = hashlib.sha256()
    with open(ruta, 'rb') as f:
        for bloque in iter(lambda: f.read(_BLOQUE), b''):
            h.update(bloque)
    return h.hexdigest()


class OrigenCarpeta:
    """Los modelos en una carpeta local (``file://``)."""

    def __init__(self, carpeta):
        self.carpeta = Path(carpeta)

    def bajar(self, nombre, destino):
        """Copia ``nombre`` de la carpeta a ``destino``."""
        origen = self.carpeta / nombre
        if not origen.is_file():
            raise ModeloInvalido(f'No está {origen}')
        shutil.copyfile(origen, destino)

    def __repr__(self):
        return f'carpeta {self.carpeta}'


class OrigenS3:
    """Los modelos en S3, bajo un prefijo (``s3://bucket/modelos/``)."""

    def __init__(self, bucket, prefijo, cliente=None):
        """Args:
            bucket (str): el bucket.
            prefijo (str): el prefijo, como ``modelos/``.
            cliente: un cliente de S3 de boto3; si falta, se crea con la región de ``AWS_REGION`` y las
                credenciales del entorno (en AWS, el rol de la instancia).
        """
        self.bucket = bucket
        self.prefijo = prefijo
        self._cliente = cliente

    @property
    def cliente(self):
        if self._cliente is None:
            import boto3
            self._cliente = boto3.client('s3', region_name=os.environ.get('AWS_REGION') or None)
        return self._cliente

    def bajar(self, nombre, destino):
        """Baja ``s3://bucket/prefijo/nombre`` a ``destino``."""
        from botocore.exceptions import BotoCoreError, ClientError

        llave = self.prefijo + nombre
        try:
            respuesta = self.cliente.get_object(Bucket=self.bucket, Key=llave)
            with open(destino, 'wb') as f:
                for bloque in iter(lambda: respuesta['Body'].read(_BLOQUE), b''):
                    f.write(bloque)
        except (BotoCoreError, ClientError) as error:
            raise ModeloInvalido(f'No se pudo bajar s3://{self.bucket}/{llave}: {error}') from error

    def __repr__(self):
        return f's3://{self.bucket}/{self.prefijo}'


def origen_desde_uri(uri, cliente_s3=None):
    """El origen de los modelos según ``INSPECTIA_MODELOS_URI``.

    Args:
        uri (str): ``s3://bucket/prefijo/`` o ``file:///carpeta/``.
        cliente_s3: un cliente de S3 ya armado (para las pruebas).

    Raises:
        ModeloInvalido: si la dirección falta o no es ``s3://`` ni ``file://``.
    """
    if not uri:
        raise ModeloInvalido('Falta INSPECTIA_MODELOS_URI (s3://bucket/modelos/ o file:///carpeta/)')
    partes = urlparse(uri)
    if partes.scheme == 's3':
        if not partes.netloc:
            raise ModeloInvalido(f'INSPECTIA_MODELOS_URI no dice el bucket: {uri}')
        prefijo = partes.path.lstrip('/')
        if prefijo and not prefijo.endswith('/'):
            prefijo += '/'
        return OrigenS3(partes.netloc, prefijo, cliente_s3)
    if partes.scheme == 'file':
        if partes.netloc not in ('', 'localhost'):
            raise ModeloInvalido(f'INSPECTIA_MODELOS_URI debe ser una carpeta local: {uri}')
        return OrigenCarpeta(url2pathname(partes.path))
    raise ModeloInvalido(f'INSPECTIA_MODELOS_URI debe empezar por s3:// o file://, y es {uri}')


def _extraer(zip_ruta, carpeta):
    """Descomprime el zip en ``carpeta``, sin dejar que una entrada escriba fuera de ella."""
    with zipfile.ZipFile(zip_ruta) as z:
        for entrada in z.infolist():
            ruta = PurePosixPath(entrada.filename)
            if ruta.is_absolute() or '..' in ruta.parts or ':' in entrada.filename:
                raise ModeloInvalido(f'El zip del modelo tiene una ruta no permitida: {entrada.filename}')
        z.extractall(carpeta)


def obtener_archivo(archivo, origen, cache):
    """Deja en la caché el archivo con el SHA-256 esperado y devuelve su ruta.

    Si ya está en la caché y su SHA-256 coincide, no se baja. Si no coincide, se baja otra vez.

    Args:
        archivo (catalogo.Archivo): el nombre y el SHA-256 esperado.
        origen: ``OrigenS3`` u ``OrigenCarpeta``.
        cache (pathlib.Path): la carpeta de la caché.

    Raises:
        ModeloInvalido: si no se pudo bajar o lo bajado no tiene el SHA-256 esperado.
    """
    cache = Path(cache)
    cache.mkdir(parents=True, exist_ok=True)
    destino = cache / archivo.nombre
    if destino.is_file():
        encontrado = sha256_de(destino)
        if encontrado == archivo.sha256:
            log.info('Modelo en la caché, con el SHA-256 esperado: %s', destino)
            return destino
        log.warning('El modelo de la caché no tiene el SHA-256 esperado (%s); se baja otra vez', encontrado)
        destino.unlink()
    temporal = cache / (archivo.nombre + '.descarga')
    log.info('Bajando %s desde %r', archivo.nombre, origen)
    try:
        origen.bajar(archivo.nombre, temporal)
        encontrado = sha256_de(temporal)
        if encontrado != archivo.sha256:
            raise ModeloInvalido(f'El SHA-256 de {archivo.nombre} no coincide con modelos.json: se esperaba '
                                 f'{archivo.sha256} y llegó {encontrado}')
        os.replace(temporal, destino)
    finally:
        if temporal.exists():
            temporal.unlink()
    return destino


def preparar_modelo(entrada, origen, cache):
    """Obtiene, comprueba y descomprime el SavedModel del modelo en uso.

    Args:
        entrada (catalogo.EntradaModelo): el modelo de ``modelos.json``.
        origen: ``OrigenS3`` u ``OrigenCarpeta``.
        cache (pathlib.Path): la carpeta de la caché.

    Returns:
        pathlib.Path: la carpeta del SavedModel (la que tiene ``saved_model.pb``).

    Raises:
        ModeloInvalido: si el modelo no es un SavedModel, no se pudo bajar o su SHA-256 no coincide.
    """
    if entrada.formato != 'savedmodel-zip':
        raise ModeloInvalido(f'El servicio solo carga modelos en SavedModel (D-40); {entrada.nombre} está en '
                             f'{entrada.formato}. Para usarlo, Julián tendría que exportarlo como SavedModel.')
    zip_ruta = obtener_archivo(entrada.archivo, origen, cache)
    # Se descomprime en cada arranque, desde el zip ya comprobado: así nadie puede cambiar lo descomprimido.
    carpeta = Path(cache) / 'descomprimido' / entrada.archivo.sha256[:16]
    if carpeta.exists():
        shutil.rmtree(carpeta)
    carpeta.mkdir(parents=True)
    try:
        _extraer(zip_ruta, carpeta)
    except zipfile.BadZipFile as error:
        raise ModeloInvalido(f'{entrada.archivo.nombre} no es un zip válido') from error
    encontrados = sorted(p.parent for p in carpeta.rglob('saved_model.pb'))
    if len(encontrados) != 1:
        raise ModeloInvalido(f'{entrada.archivo.nombre} debe tener un solo saved_model.pb, y tiene '
                             f'{len(encontrados)}')
    return encontrados[0]
