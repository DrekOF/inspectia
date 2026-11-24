# Nombre: test_subir_modelos.py
# Descripción: Pruebas del script que sube a S3 los modelos de la release de Julián, sin red: la release es una
#              carpeta local y S3 es un doble de botocore (Stubber) (D-16, D-40).
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.0
"""Pruebas de ``ia-servicio/scripts/subir_modelos.py``."""

import base64
import hashlib
import io
import json
import shutil
import sys
from pathlib import Path

import boto3
import pytest
from botocore.stub import ANY, Stubber

from conftest import CATALOGO

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / 'scripts'))
import subir_modelos  # noqa: E402

BUCKET = 'inspectia-archivos-123456789012'


def _sha(b):
    return hashlib.sha256(b).hexdigest()


@pytest.fixture
def release(tmp_path):
    """Una release falsa de 2 modelos en una carpeta, con su manifest y su modelos.json."""
    carpeta = tmp_path / 'release'
    carpeta.mkdir()
    archivos = {'m1_savedmodel.zip': b'zip del modelo 1', 'm1.json': b'{"ficha": 1}',
                'm2.keras': b'keras del modelo 2', 'm2.json': b'{"ficha": 2}'}
    for nombre, contenido in archivos.items():
        (carpeta / nombre).write_bytes(contenido)
    manifest = json.dumps({'archivos': {n: _sha(c) for n, c in archivos.items()}}).encode()
    (carpeta / 'manifest.json').write_bytes(manifest)
    datos = json.loads(CATALOGO.read_text(encoding='utf-8'))
    m1, m2 = datos['modelos'][1], datos['modelos'][0]
    m1.update(archivo='m1_savedmodel.zip', sha256=_sha(archivos['m1_savedmodel.zip']),
              ficha={'archivo': 'm1.json', 'sha256': _sha(archivos['m1.json'])})
    m2.update(archivo='m2.keras', sha256=_sha(archivos['m2.keras']),
              ficha={'archivo': 'm2.json', 'sha256': _sha(archivos['m2.json'])})
    datos['modelos'] = [m1, m2]
    datos['release'] = {'etiqueta': 'prueba', 'url': 'https://release.invalid/',
                        'manifest': {'archivo': 'manifest.json', 'sha256': _sha(manifest)}}
    catalogo = tmp_path / 'modelos.json'
    catalogo.write_text(json.dumps(datos), encoding='utf-8')
    bajados = []

    def bajar(url, destino):
        assert url.startswith('https://release.invalid/')
        nombre = url.rsplit('/', 1)[1]
        bajados.append(nombre)
        shutil.copyfile(carpeta / nombre, destino)

    return {'carpeta': carpeta, 'catalogo': catalogo, 'bajar': bajar, 'bajados': bajados, 'archivos': archivos,
            'manifest': manifest}


def _cliente():
    return boto3.client('s3', region_name='us-east-1', aws_access_key_id='prueba', aws_secret_access_key='prueba')


@pytest.mark.caso('D-40 · el script baja los modelos, comprueba su SHA-256 y los sube a modelos/ con su checksum')
def test_sube_todo_con_su_sha256(release, tmp_path):
    cliente = _cliente()
    orden = ['manifest.json', 'm1_savedmodel.zip', 'm1.json', 'm2.keras', 'm2.json']
    contenidos = {**release['archivos'], 'manifest.json': release['manifest']}
    with Stubber(cliente) as doble:
        for nombre in orden:
            sha = _sha(contenidos[nombre])
            if nombre == 'm1.json':  # ya está en S3 con el mismo SHA-256: no se sube otra vez
                doble.add_response('head_object', {'Metadata': {'sha256': sha}},
                                   {'Bucket': BUCKET, 'Key': f'modelos/{nombre}'})
                continue
            doble.add_client_error('head_object', '404', http_status_code=404,
                                   expected_params={'Bucket': BUCKET, 'Key': f'modelos/{nombre}'})
            doble.add_response('put_object', {}, {
                'Bucket': BUCKET, 'Key': f'modelos/{nombre}', 'Body': ANY, 'ContentType': ANY,
                'ChecksumSHA256': base64.b64encode(bytes.fromhex(sha)).decode(), 'Metadata': {'sha256': sha}})
        salida = io.StringIO()
        codigo = subir_modelos.main(['--bucket', BUCKET, '--catalogo', str(release['catalogo']),
                                     '--carpeta', str(tmp_path / 'bajados')], cliente, release['bajar'], salida)
        doble.assert_no_pending_responses()
    assert codigo == 0
    assert release['bajados'] == orden
    assert 'Listo: 4 subidos, 1 ya estaban.' in salida.getvalue()


@pytest.mark.caso('D-40 · si un SHA-256 de la release no coincide, el script no sube nada')
def test_no_sube_nada_si_un_sha256_no_coincide(release, tmp_path):
    (release['carpeta'] / 'm2.keras').write_bytes(b'otro contenido')
    cliente = _cliente()
    with Stubber(cliente) as doble:  # sin respuestas: cualquier llamada a S3 haría fallar la prueba
        codigo = subir_modelos.main(['--bucket', BUCKET, '--catalogo', str(release['catalogo']),
                                     '--carpeta', str(tmp_path / 'b')], cliente, release['bajar'], io.StringIO())
        doble.assert_no_pending_responses()
    assert codigo == 1
    assert not (tmp_path / 'b' / 'm2.keras').exists()


@pytest.mark.caso('D-40 · modelos.json y el manifest de la release deben decir el mismo SHA-256')
def test_catalogo_y_manifest_deben_coincidir(release, tmp_path):
    datos = json.loads(release['catalogo'].read_text(encoding='utf-8'))
    datos['modelos'][1]['sha256'] = '1' * 64
    release['catalogo'].write_text(json.dumps(datos), encoding='utf-8')
    codigo = subir_modelos.main(['--bucket', BUCKET, '--catalogo', str(release['catalogo']), '--simular',
                                 '--carpeta', str(tmp_path / 'b')], _cliente(), release['bajar'], io.StringIO())
    assert codigo == 1


@pytest.mark.caso('D-40 · con --simular el script baja y comprueba, pero no sube nada')
def test_simular_no_sube(release, tmp_path):
    cliente = _cliente()
    with Stubber(cliente):
        salida = io.StringIO()
        codigo = subir_modelos.main(['--bucket', BUCKET, '--catalogo', str(release['catalogo']), '--simular',
                                     '--carpeta', str(tmp_path / 'b')], cliente, release['bajar'], salida)
    assert codigo == 0
    assert 'no se subió nada' in salida.getvalue()
    # Lo ya bajado con el SHA-256 correcto no se baja otra vez.
    subir_modelos.main(['--bucket', BUCKET, '--catalogo', str(release['catalogo']), '--simular',
                        '--carpeta', str(tmp_path / 'b')], cliente, release['bajar'], io.StringIO())
    assert len(release['bajados']) == 5


@pytest.mark.caso('D-40 · el script solo baja por HTTPS')
def test_solo_https(tmp_path):
    with pytest.raises(subir_modelos.ErrorDeSubida):
        subir_modelos.bajar_url('http://release.invalid/x', tmp_path / 'x')
