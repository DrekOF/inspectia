# Nombre: clasificador.py
# Descripción: Clasificación de los recortes con el SavedModel de TensorFlow de Julián, por lotes, con la misma
#              preparación de inferencia.py (RF-08, D-40).
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.0
"""Clasificación de los granos con el modelo elegido (RF-08, D-18, D-40).

El modelo de la release ``modelos-v1.0`` recibe el recorte de un grano ya vuelto cuadrado y reducido a 224 x 224
(RGB, valores de 0 a 255) y devuelve ``[P(sana), P(mala)]``; la escala del preset y el softmax van dentro del
modelo. La preparación es la de ``preparar`` de ``inferencia.py`` (igual a ``07_data_pipeline.load_image`` del
entrenamiento): se prolonga el borde del recorte, suavizado a 16 valores, hasta volverlo cuadrado y se reduce a
224 x 224 con interpolación bilineal. Aquí corre como un ``tf.function`` con las mismas operaciones de TensorFlow,
así da los mismos números.

Cada recorte pasa antes por un JPG de calidad 95, como los que guardó ``generar_recortes.py``, y se lee con
``tf.io.decode_jpeg``, como en el entrenamiento: el modelo es sensible a esa diferencia (medido: leer el mismo JPG
con Pillow en lugar de TensorFlow cambia los píxeles en 1 nivel de media y P(mala) hasta en 0,05).

Un grano es NO_SANO si P(mala) es 0,5 o más, el umbral con que Julián midió el modelo (RF-08). La confianza es la
probabilidad de la clase elegida, de 0 a 100, con 2 decimales.
"""

from dataclasses import dataclass

import numpy as np

CALIDAD_JPG = 95
"""Calidad de los JPG de los recortes del entrenamiento (``CALIDAD_JPG`` de ``generar_recortes.py``)."""

TAMANO = 224
"""Tamaño de entrada de los 5 modelos (``entrada.forma`` del JSON de cada uno)."""

UMBRAL_MALA = 0.5
"""``umbral_mala`` del JSON del modelo (RF-08)."""

SANO, NO_SANO = 'SANO', 'NO_SANO'
"""Los códigos de la clase de un grano (``specs/entidades/grano.json``)."""


@dataclass(frozen=True)
class Clasificacion:
    """La clase de un grano y la confianza del modelo en ella."""

    clase: str
    confianza: float
    p_mala: float


def a_clasificacion(p_sana, p_mala):
    """Convierte la salida del modelo en la clase y su confianza (RF-08 C1 y C2).

    Args:
        p_sana (float): probabilidad de sana.
        p_mala (float): probabilidad de mala (no sano).

    Returns:
        Clasificacion: la clase y la probabilidad de esa clase x 100, con 2 decimales.
    """
    if p_mala >= UMBRAL_MALA:
        return Clasificacion(NO_SANO, round(float(p_mala) * 100, 2), float(p_mala))
    return Clasificacion(SANO, round(float(p_sana) * 100, 2), float(p_mala))


def a_jpg(recorte_rgb):
    """El recorte en JPG de calidad 95, como lo guardó ``generar_recortes.py``."""
    import cv2

    ok, datos = cv2.imencode('.jpg', cv2.cvtColor(recorte_rgb, cv2.COLOR_RGB2BGR), [cv2.IMWRITE_JPEG_QUALITY,
                                                                                    CALIDAD_JPG])
    if not ok:
        raise ValueError('No se pudo codificar el recorte')
    return datos.tobytes()


def _preparacion():
    """Arma el ``tf.function`` que lee un JPG y lo prepara como ``preparar`` de ``inferencia.py``."""
    import tensorflow as tf

    def suavizar(fila, puntos=16):
        n = tf.shape(fila)[1]
        corta = tf.image.resize(fila, (1, puntos), method='area')
        return tf.image.resize(corta, (1, n), method='bilinear')

    @tf.function(input_signature=[tf.TensorSpec([], tf.string)])
    def preparar(jpg):
        img = tf.cast(tf.io.decode_jpeg(jpg, channels=3), tf.float32)
        h, w = tf.shape(img)[0], tf.shape(img)[1]
        lado = tf.maximum(h, w)
        top, left = (lado - h) // 2, (lado - w) // 2
        bottom, right = lado - h - top, lado - w - left
        k = 4
        img = tf.concat([tf.repeat(suavizar(tf.reduce_mean(img[:k], 0, keepdims=True)), top, 0), img,
                         tf.repeat(suavizar(tf.reduce_mean(img[-k:], 0, keepdims=True)), bottom, 0)], 0)
        ci = tf.transpose(suavizar(tf.transpose(tf.reduce_mean(img[:, :k], 1, keepdims=True), [1, 0, 2])),
                          [1, 0, 2])
        cd = tf.transpose(suavizar(tf.transpose(tf.reduce_mean(img[:, -k:], 1, keepdims=True), [1, 0, 2])),
                          [1, 0, 2])
        img = tf.concat([tf.repeat(ci, left, 1), img, tf.repeat(cd, right, 1)], 1)
        return tf.clip_by_value(tf.image.resize(img, (TAMANO, TAMANO)), 0.0, 255.0)

    return preparar


class ClasificadorSavedModel:
    """Clasifica recortes con un SavedModel de TensorFlow, por lotes.

    El modelo se carga una sola vez y se guarda en el objeto: ``tf.saved_model.load(...).serve(...)`` en una sola
    línea falla, porque el modelo se libera antes de usarse (lección 13 de Julián).
    """

    def __init__(self, carpeta, hilos=0, lote=64):
        """Carga el modelo.

        Args:
            carpeta (pathlib.Path): la carpeta del SavedModel (la que tiene ``saved_model.pb``).
            hilos (int): hilos de CPU de TensorFlow; 0 deja los que elija TensorFlow.
            lote (int): cuántos recortes se clasifican de una vez.
        """
        import tensorflow as tf

        if hilos > 0:
            try:
                tf.config.threading.set_intra_op_parallelism_threads(hilos)
                tf.config.threading.set_inter_op_parallelism_threads(hilos)
            except RuntimeError:
                # TensorFlow ya arrancó (pasa en las pruebas): sigue con los hilos que tenía.
                pass
        self._tf = tf
        self._modelo = tf.saved_model.load(str(carpeta))
        self._preparar = _preparacion()
        self._lote = max(1, lote)
        # La primera llamada arma el grafo: se hace aquí y no en el primer análisis (RNF-01).
        self.probabilidades([np.full((40, 60, 3), 200, np.uint8)])

    def preparar(self, jpg):
        """Lee un JPG y lo prepara como ``preparar`` de ``inferencia.py``.

        Returns:
            numpy.ndarray: el recorte de 224 x 224 x 3, ``float32`` de 0 a 255.
        """
        return self._preparar(self._tf.constant(jpg)).numpy()

    def probabilidades_jpg(self, jpgs):
        """``[P(sana), P(mala)]`` de cada recorte en JPG, exactamente como ``inferencia.py``.

        Args:
            jpgs (list[bytes]): los recortes en JPG.

        Returns:
            numpy.ndarray: una fila por recorte.
        """
        tf = self._tf
        if not jpgs:
            return np.zeros((0, 2), np.float32)
        salidas = []
        for i in range(0, len(jpgs), self._lote):
            x = tf.stack([self._preparar(tf.constant(j)) for j in jpgs[i:i + self._lote]])
            salidas.append(self._modelo.serve(x).numpy())
        return np.concatenate(salidas)

    def probabilidades(self, recortes_rgb):
        """``[P(sana), P(mala)]`` de cada recorte, que pasa antes por un JPG como en el entrenamiento.

        Args:
            recortes_rgb (list[numpy.ndarray]): los recortes en RGB (``uint8``).

        Returns:
            numpy.ndarray: una fila por recorte.
        """
        return self.probabilidades_jpg([a_jpg(r) for r in recortes_rgb])

    def clasificar(self, recortes_rgb):
        """La clase y la confianza de cada recorte (RF-08).

        Args:
            recortes_rgb (list[numpy.ndarray]): los recortes en RGB (``uint8``).

        Returns:
            list[Clasificacion]: en el mismo orden.
        """
        return [a_clasificacion(p[0], p[1]) for p in self.probabilidades(recortes_rgb)]
