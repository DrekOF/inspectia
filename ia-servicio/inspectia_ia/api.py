# Nombre: api.py
# Descripción: API HTTP del servicio de IA (FastAPI): salud, catálogo de modelos, revisión rápida de una foto y
#              análisis de las fotos de una muestra (RF-06, RF-07, RF-08, D-18, D-46), con a lo sumo 2 trabajos a
#              la vez (RNF-01).
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.1
"""API del servicio de IA.

Escucha solo dentro de la red de Docker (el backend la llama en ``http://ia-servicio:8000``). Los nombres del JSON
van en camelCase, como la API del backend, y las posiciones en píxeles de la foto ya girada según su EXIF (D-45).

- ``GET /salud``: 200 con el modelo cargado; 503 mientras se carga.
- ``GET /modelos``: los 5 modelos de ``modelos.json`` con sus métricas, y cuál está en uso (D-18, D-48).
- ``POST /revisar`` (campo ``foto``): la revisión rápida de D-46 (luz, nitidez, granos pegados y cuántos granos).
- ``POST /analizar`` (campos ``foto1``, ``foto2`` y, si hay, ``foto3``): la revisión, la separación y la
  clasificación de cada foto (RF-06 a RF-08). Una foto por repetir va sin granos.

Las rutas son síncronas y corren en el grupo de hilos de AnyIO (40 hilos), pero a lo sumo ``TRABAJOS_A_LA_VEZ``
revisiones o análisis leen, revisan y clasifican a la vez; los demás esperan su turno. Cada foto de 12 MP abierta
ocupa de 150 a 250 MB, y el contenedor tiene 1,5 GB (RNF-01).
"""

import logging
import os
import threading
import time
from contextlib import asynccontextmanager

import cv2
from fastapi import FastAPI, File, UploadFile
from fastapi.responses import JSONResponse

from inspectia_ia import catalogo as catalogo_mod
from inspectia_ia.configuracion import desde_entorno
from inspectia_ia.imagen import ImagenInvalida, leer_foto
from inspectia_ia.modelos import origen_desde_uri, preparar_modelo
from inspectia_ia.separacion import APROBADA, recortar, revisar

log = logging.getLogger(__name__)

TRABAJOS_A_LA_VEZ = 2
"""Cuántas revisiones (``/revisar``) o análisis (``/analizar``) leen, revisan y clasifican fotos a la vez."""


class ImagenMuyGrande(ValueError):
    """La foto pasa del tamaño máximo de la configuración."""


class Estado:
    """Lo que el servicio sabe del modelo: la entrada del catálogo y, cuando ya cargó, el clasificador."""

    def __init__(self):
        self.entrada = None
        self.clasificador = None


def _terminar(error):
    """Si el modelo no carga, el servicio no puede trabajar: termina el proceso para que Docker lo reinicie."""
    log.critical('No se pudo cargar el modelo: %s', error)
    os._exit(1)


def _obtener_por_defecto(configuracion):
    """Obtiene y comprueba el SavedModel según la configuración (``INSPECTIA_MODELOS_URI``)."""
    def obtener(entrada):
        return preparar_modelo(entrada, origen_desde_uri(configuracion.modelos_uri), configuracion.cache)
    return obtener


def _cargar_por_defecto(configuracion):
    """Carga el SavedModel con TensorFlow."""
    def cargar(carpeta):
        from inspectia_ia.clasificador import ClasificadorSavedModel
        return ClasificadorSavedModel(carpeta, configuracion.hilos, configuracion.lote_clasificacion)
    return cargar


def _leer(archivo, maximo):
    """Lee un archivo subido sin pasar del tamaño máximo."""
    contenido = archivo.file.read(maximo + 1)
    if len(contenido) > maximo:
        raise ImagenMuyGrande(f'La foto pasa de {maximo} bytes')
    return contenido


def crear_app(configuracion=None, obtener=None, cargar=None, al_fallar=_terminar, trabajos_a_la_vez=TRABAJOS_A_LA_VEZ):
    """Arma la aplicación.

    Al arrancar, lee ``modelos.json``, obtiene el modelo en uso y comprueba su SHA-256 antes de atender: si algo
    falla, el servicio no arranca. Después carga el modelo en otro hilo; mientras tanto ``/salud`` responde 503.

    Args:
        configuracion (Configuracion): por defecto, la de las variables de entorno.
        obtener: función que recibe la entrada del catálogo y devuelve la carpeta del modelo, ya comprobado.
        cargar: función que recibe esa carpeta y devuelve el clasificador (con ``clasificar``).
        al_fallar: qué hacer si la carga en segundo plano falla (por defecto, terminar el proceso).
        trabajos_a_la_vez (int): cuántas revisiones o análisis trabajan a la vez; los demás esperan su turno.

    Returns:
        fastapi.FastAPI: la aplicación.
    """
    configuracion = configuracion or desde_entorno()
    obtener = obtener or _obtener_por_defecto(configuracion)
    cargar = cargar or _cargar_por_defecto(configuracion)
    catalogo = catalogo_mod.cargar(configuracion.catalogo)
    estado = Estado()
    turnos = threading.BoundedSemaphore(trabajos_a_la_vez)

    @asynccontextmanager
    async def ciclo_de_vida(_app):
        estado.entrada = catalogo.modelo(configuracion.modelo)
        carpeta = obtener(estado.entrada)

        def en_segundo_plano():
            inicio = time.perf_counter()
            try:
                estado.clasificador = cargar(carpeta)
            except Exception as error:  # noqa: BLE001 (cualquier falla del modelo detiene el servicio)
                al_fallar(error)
                return
            log.info('Modelo %s %s cargado en %.1f s', estado.entrada.nombre, estado.entrada.version,
                     time.perf_counter() - inicio)

        threading.Thread(target=en_segundo_plano, name='carga-del-modelo', daemon=True).start()
        yield

    app = FastAPI(title='InspectIA: servicio de IA', version='1.0', lifespan=ciclo_de_vida,
                  description='Revisión de las fotos, separación de los granos y clasificación (RF-06 a RF-08).')
    app.state.estado = estado

    @app.exception_handler(ImagenInvalida)
    async def imagen_invalida(_peticion, error):
        return JSONResponse(status_code=400, content={'error': 'IMAGEN_INVALIDA', 'mensaje': str(error)})

    @app.exception_handler(ImagenMuyGrande)
    async def imagen_muy_grande(_peticion, error):
        return JSONResponse(status_code=413, content={'error': 'IMAGEN_MUY_GRANDE', 'mensaje': str(error)})

    def modelo_json(con_sha=False):
        e = estado.entrada
        datos = {'nombre': e.nombre, 'version': e.version}
        if con_sha:
            datos['sha256'] = e.archivo.sha256
        return datos

    def no_listo():
        return JSONResponse(status_code=503, content={'listo': False, 'modelo': modelo_json(True)})

    @app.get('/salud', summary='Si el modelo ya está cargado')
    def salud():
        if estado.clasificador is None:
            return no_listo()
        return {'listo': True, 'modelo': modelo_json(True)}

    @app.get('/modelos', summary='Los 5 modelos de la comparación, con sus métricas (D-18, D-48)')
    def modelos():
        return {'enUso': configuracion.modelo, 'modelos': [m.a_json() for m in catalogo.modelos]}

    @app.post('/revisar', summary='Revisión rápida de una foto: luz, nitidez, granos pegados y granos (D-46)')
    def revisar_foto(foto: UploadFile = File(...)):
        contenido = _leer(foto, configuracion.tamano_maximo)
        with turnos:
            inicio = time.perf_counter()
            revision = revisar(leer_foto(contenido), configuracion.umbrales)
        log.info('Revisión: %s %s, %d granos, luz %.0f, nitidez %.0f, en %.0f ms', revision.estado,
                 revision.motivo or '', len(revision.granos), revision.luz, revision.nitidez,
                 (time.perf_counter() - inicio) * 1000)
        return revision.a_json()

    @app.post('/analizar', summary='Revisa, separa y clasifica los granos de las fotos de una muestra (RF-06 a RF-08)')
    def analizar(foto1: UploadFile = File(...), foto2: UploadFile | None = File(None),
                 foto3: UploadFile | None = File(None)):
        if estado.clasificador is None:
            return no_listo()
        fotos = [(n, _leer(f, configuracion.tamano_maximo))
                 for n, f in ((1, foto1), (2, foto2), (3, foto3)) if f is not None]
        resultados, recortes = [], []
        with turnos:
            inicio = time.perf_counter()
            for numero, contenido in fotos:
                imagen = leer_foto(contenido)
                revision = revisar(imagen, configuracion.umbrales)
                granos = revision.granos if revision.estado == APROBADA else []
                recortes += [cv2.cvtColor(recortar(imagen, g.contorno), cv2.COLOR_BGR2RGB) for g in granos]
                resultados.append((numero, revision, granos))
                del imagen  # la foto completa no se guarda mientras se lee la siguiente
            separado = time.perf_counter()
            clasificaciones = iter(estado.clasificador.clasificar(recortes))
            fin = time.perf_counter()
        log.info('Análisis: %d fotos, %d granos; separación %.0f ms, clasificación %.0f ms', len(fotos),
                 len(recortes), (separado - inicio) * 1000, (fin - separado) * 1000)
        respuesta = []
        for numero, revision, granos in resultados:
            lista = []
            for g in granos:
                c = next(clasificaciones)
                lista.append({**g.rectangulo.a_json(), 'clase': c.clase, 'confianza': c.confianza})
            respuesta.append({'numero': numero, 'revision': revision.a_json(), 'granos': lista})
        return {'modelo': modelo_json(), 'fotos': respuesta}

    return app
