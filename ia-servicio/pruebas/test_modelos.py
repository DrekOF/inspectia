# Nombre: test_modelos.py
# Descripción: Pruebas de la obtención del modelo: SHA-256, caché, S3 con un doble (botocore Stubber), carpetas
#              locales, el zip del SavedModel y el catálogo modelos.json (D-18, D-40, D-48).
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.1
"""Pruebas de ``inspectia_ia.modelos`` y ``inspectia_ia.catalogo``, sin red: S3 es un doble de botocore."""

import hashlib
import io
import json
import zipfile

import boto3
import pytest
from botocore.response import StreamingBody
from botocore.stub import Stubber

from conftest import CATALOGO
from inspectia_ia import catalogo as catalogo_mod
from inspectia_ia.catalogo import Archivo, CatalogoInvalido
from inspectia_ia.modelos import (ModeloInvalido, OrigenCarpeta, OrigenS3, obtener_archivo, origen_desde_uri,
                                  preparar_modelo, sha256_de)


def _zip_savedmodel(entradas=None):
    """Un zip con la forma del de Julián: una carpeta con saved_model.pb y variables/."""
    datos = io.BytesIO()
    with zipfile.ZipFile(datos, 'w') as z:
        for nombre, contenido in (entradas or {
                'inspectia_X_savedmodel/saved_model.pb': b'grafo',
                'inspectia_X_savedmodel/variables/variables.index': b'indice'}).items():
            z.writestr(nombre, contenido)
    return datos.getvalue()


def _sha(contenido):
    return hashlib.sha256(contenido).hexdigest()


def _entrada(contenido, nombre='modelo.zip', formato='savedmodel-zip'):
    return catalogo_mod.EntradaModelo('X', '1.0', 'CNN', formato, Archivo(nombre, _sha(contenido)),
                                      Archivo('x.json', '0' * 64), {})


class OrigenContado:
    """Un origen en memoria que cuenta cuántas veces se baja cada archivo."""

    def __init__(self, archivos):
        self.archivos = archivos
        self.bajados = []

    def bajar(self, nombre, destino):
        self.bajados.append(nombre)
        destino.write_bytes(self.archivos[nombre])


@pytest.mark.caso('D-40 · el servicio baja el SavedModel, comprueba su SHA-256 y lo descomprime')
def test_prepara_el_savedmodel(tmp_path):
    contenido = _zip_savedmodel()
    origen = OrigenContado({'modelo.zip': contenido})
    carpeta = preparar_modelo(_entrada(contenido), origen, tmp_path / 'cache')
    assert (carpeta / 'saved_model.pb').read_bytes() == b'grafo'
    assert carpeta.name == 'inspectia_X_savedmodel'
    assert (tmp_path / 'cache' / 'modelo.zip').read_bytes() == contenido


@pytest.mark.caso('D-40 · si el SHA-256 del modelo no coincide con modelos.json, no se usa')
def test_sha256_distinto_no_se_usa(tmp_path):
    esperado = _zip_savedmodel()
    origen = OrigenContado({'modelo.zip': _zip_savedmodel({'otro/saved_model.pb': b'cambiado'})})
    with pytest.raises(ModeloInvalido, match='SHA-256'):
        preparar_modelo(_entrada(esperado), origen, tmp_path / 'cache')
    assert list((tmp_path / 'cache').iterdir()) == []


@pytest.mark.caso('D-40 · el modelo queda en la caché y no se baja en cada arranque, pero se vuelve a comprobar')
def test_la_cache_evita_bajarlo_otra_vez(tmp_path):
    contenido = _zip_savedmodel()
    origen = OrigenContado({'modelo.zip': contenido})
    preparar_modelo(_entrada(contenido), origen, tmp_path)
    preparar_modelo(_entrada(contenido), origen, tmp_path)
    assert origen.bajados == ['modelo.zip']
    # Si alguien cambia el archivo de la caché, se baja otra vez.
    (tmp_path / 'modelo.zip').write_bytes(b'danado')
    preparar_modelo(_entrada(contenido), origen, tmp_path)
    assert origen.bajados == ['modelo.zip', 'modelo.zip']
    assert sha256_de(tmp_path / 'modelo.zip') == _sha(contenido)


@pytest.mark.caso('D-40 · el servicio solo carga modelos en SavedModel')
def test_solo_carga_savedmodel(tmp_path):
    with pytest.raises(ModeloInvalido, match='SavedModel'):
        preparar_modelo(_entrada(b'k', 'm.keras', 'keras'), OrigenContado({}), tmp_path)


@pytest.mark.caso('D-40 · un zip con rutas fuera de su carpeta se rechaza')
def test_zip_con_rutas_peligrosas(tmp_path):
    contenido = _zip_savedmodel({'../fuera/saved_model.pb': b'x'})
    with pytest.raises(ModeloInvalido, match='ruta no permitida'):
        preparar_modelo(_entrada(contenido), OrigenContado({'modelo.zip': contenido}), tmp_path / 'c')
    assert not (tmp_path / 'fuera').exists()


@pytest.mark.caso('D-40 · un zip sin un único saved_model.pb se rechaza')
def test_zip_sin_saved_model(tmp_path):
    contenido = _zip_savedmodel({'carpeta/otro.txt': b'x'})
    with pytest.raises(ModeloInvalido, match='saved_model.pb'):
        preparar_modelo(_entrada(contenido), OrigenContado({'modelo.zip': contenido}), tmp_path)


def _cliente_s3():
    """Cliente de S3 con credenciales falsas: el Stubber responde antes de que salga nada a la red."""
    return boto3.client('s3', region_name='us-east-1', aws_access_key_id='prueba',
                        aws_secret_access_key='prueba')


@pytest.mark.caso('D-40 · el servicio baja el modelo de s3://bucket/modelos/ con el SDK')
def test_baja_de_s3_con_un_doble(tmp_path):
    contenido = _zip_savedmodel()
    cliente = _cliente_s3()
    with Stubber(cliente) as doble:
        doble.add_response('get_object',
                           {'Body': StreamingBody(io.BytesIO(contenido), len(contenido)),
                            'ContentLength': len(contenido)},
                           {'Bucket': 'inspectia-archivos-123', 'Key': 'modelos/modelo.zip'})
        origen = origen_desde_uri('s3://inspectia-archivos-123/modelos/', cliente)
        ruta = obtener_archivo(Archivo('modelo.zip', _sha(contenido)), origen, tmp_path)
        doble.assert_no_pending_responses()
    assert ruta.read_bytes() == contenido


@pytest.mark.caso('D-40 · si S3 no tiene el modelo, el servicio no arranca')
def test_s3_sin_el_modelo(tmp_path):
    cliente = _cliente_s3()
    with Stubber(cliente) as doble:
        doble.add_client_error('get_object', 'NoSuchKey', http_status_code=404)
        origen = OrigenS3('inspectia-archivos-123', 'modelos/', cliente)
        with pytest.raises(ModeloInvalido, match='No se pudo bajar'):
            obtener_archivo(Archivo('modelo.zip', '0' * 64), origen, tmp_path)
    assert list(tmp_path.iterdir()) == []


@pytest.mark.caso('D-40 · INSPECTIA_MODELOS_URI acepta s3:// y file://, y nada más')
def test_direcciones_de_los_modelos(tmp_path):
    s3 = origen_desde_uri('s3://inspectia-archivos-123/modelos')
    assert (s3.bucket, s3.prefijo) == ('inspectia-archivos-123', 'modelos/')
    local = origen_desde_uri(tmp_path.as_uri())
    assert isinstance(local, OrigenCarpeta)
    assert local.carpeta.resolve() == tmp_path.resolve()
    for malo in ('', 'http://otro/modelos/', 's3:///modelos/', 'file://servidor/carpeta/'):
        with pytest.raises(ModeloInvalido):
            origen_desde_uri(malo)


@pytest.mark.caso('D-40 · el modelo también se puede tomar de una carpeta local (desarrollo y CI)')
def test_baja_de_una_carpeta(tmp_path):
    contenido = _zip_savedmodel()
    (tmp_path / 'origen').mkdir()
    (tmp_path / 'origen' / 'modelo.zip').write_bytes(contenido)
    origen = origen_desde_uri((tmp_path / 'origen').as_uri() + '/')
    carpeta = preparar_modelo(_entrada(contenido), origen, tmp_path / 'cache')
    assert (carpeta / 'saved_model.pb').is_file()
    with pytest.raises(ModeloInvalido, match='No está'):
        origen.bajar('falta.zip', tmp_path / 'x')


@pytest.mark.caso('D-48 · modelos.json trae los 5 modelos con las métricas de la release de Julián')
def test_catalogo_del_repositorio():
    c = catalogo_mod.cargar(CATALOGO)
    assert [m.nombre for m in c.modelos] == ['MobileNetV3-L', 'EfficientNet-B0', 'ResNet-50', 'Swin-Tiny',
                                             'DeiT-Small']
    assert [m.tipo for m in c.modelos] == ['CNN', 'CNN', 'CNN', 'TRANSFORMER', 'TRANSFORMER']
    e = c.modelo('EfficientNet-B0')
    assert e.formato == 'savedmodel-zip'
    assert e.archivo == Archivo('inspectia_EfficientNet-B0_v1.0_savedmodel.zip',
                                'eb9a64394c51fdff8fc16e01fff71e3b3693bc36e43cafb0ddbb34ca5b765bfb')
    # D-39 y D-48: recall 91,20 y 53,40; matriz 238 / 23 / 34 / 39; precisión 87,50 y 62,90.
    assert (e.metricas['recallSano'], e.metricas['recallNoSano']) == (91.2, 53.4)
    assert (e.metricas['precisionSano'], e.metricas['precisionNoSano']) == (87.5, 62.9)
    assert [e.metricas[k] for k in ('sanosComoSanos', 'sanosComoNoSanos', 'noSanosComoSanos',
                                    'noSanosComoNoSanos')] == [238, 23, 34, 39]
    assert c.modelo('MobileNetV3-L').metricas['f1Macro'] == 63.8
    for m in c.modelos:
        vn, fp, fn, vp = (m.metricas[k] for k in ('sanosComoSanos', 'sanosComoNoSanos', 'noSanosComoSanos',
                                                  'noSanosComoNoSanos'))
        assert (vn + fp, fn + vp) == (261, 73)  # el conjunto de prueba de 334 granos (RNF-07)
        assert m.metricas['recallMacro'] == pytest.approx((vn / 261 + vp / 73) / 2 * 100, abs=0.005)
        assert m.metricas['precisionSano'] == pytest.approx(vn / (vn + fn) * 100, abs=0.01)
    assert len(c.archivos()) == 11


@pytest.mark.caso('D-48 · modelos.json trae para el modelo en uso el F1 publicado por Julián en su release, de 65 % o más '
                  '(RNF-07)')
def test_el_modelo_en_uso_cumple_el_f1_minimo():
    """Lee el F1 macro copiado en ``modelos.json`` del README de Julián (D-48); no vuelve a medir el modelo con los
    334 granos del conjunto de prueba."""
    c = catalogo_mod.cargar(CATALOGO)
    assert c.modelo('EfficientNet-B0').metricas['f1Macro'] >= 65


@pytest.mark.caso('D-18 · modelos.json mal formado no se acepta')
@pytest.mark.parametrize('cambio, mensaje', [
    (lambda d: d['modelos'][0].update(tipo='RNN'), 'tipo'),
    (lambda d: d['modelos'][0].update(sha256='abc'), 'SHA-256'),
    (lambda d: d['modelos'][0].update(archivo='../x.zip'), 'nombre de archivo'),
    (lambda d: d['modelos'][0]['metricas'].pop('f1Macro'), 'f1Macro'),
    (lambda d: d['modelos'].append(dict(d['modelos'][0])), 'repite'),
])
def test_catalogo_mal_formado(tmp_path, cambio, mensaje):
    datos = json.loads(CATALOGO.read_text(encoding='utf-8'))
    cambio(datos)
    ruta = tmp_path / 'modelos.json'
    ruta.write_text(json.dumps(datos), encoding='utf-8')
    with pytest.raises(CatalogoInvalido, match=mensaje):
        catalogo_mod.cargar(ruta)


@pytest.mark.caso('D-18 · pedir un modelo que no está en modelos.json es un error')
def test_modelo_que_no_esta():
    with pytest.raises(CatalogoInvalido, match='no está'):
        catalogo_mod.cargar(CATALOGO).modelo('GPT')
