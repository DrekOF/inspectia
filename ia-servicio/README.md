<!--
Nombre: README.md (ia-servicio)
Descripción: Servicio de IA de InspectIA: qué hace, su API, cómo correrlo en local (venv o Docker), sus variables, el script que sube los modelos a S3 y los tiempos medidos.
Fecha de creación: 12/10/2026
Actualización: 24/11/2026
Autor: Equipo InspectIA
Versión: 2.1
-->

# ia-servicio

Servicio de IA de InspectIA, en Python 3.12 con FastAPI, OpenCV y TensorFlow 2.21 (CPU). El backend le manda las fotos de una muestra y el servicio:

- revisa la luz y la nitidez de cada foto (RF-06) y busca granos pegados (RF-07 C2, D-09);
- separa los granos sobre el papel blanco y descarta los cortados por el borde (RF-07 C4);
- recorta cada grano igual que en el entrenamiento y lo clasifica como sano o no sano con EfficientNet-B0 v1.0, el modelo de la release `modelos-v1.0` de Julián (RF-08, D-18, D-40).

Escucha en el puerto 8000, solo dentro de la red de Docker: en producción no se publica al host y el backend lo llama en `http://ia-servicio:8000`. La app no reentrena el modelo (D-03).

## API

JSON con nombres en camelCase. Las posiciones son rectángulos sin rotación en píxeles de la foto ya girada según su EXIF, como la muestra el navegador (D-45). La documentación OpenAPI queda en `/docs`.

| Ruta | Qué hace |
|---|---|
| `GET /salud` | 200 `{"listo": true, "modelo": {"nombre", "version", "sha256"}}`; 503 `{"listo": false, …}` mientras el modelo carga. |
| `GET /modelos` | Los 5 modelos de `modelos.json` con sus métricas en el conjunto de prueba (D-18, D-48) y cuál está en uso: `{"enUso": "EfficientNet-B0", "modelos": [{"nombre", "version", "tipo", "f1Macro", "precisionMacro", "recallMacro", "recallSano", "recallNoSano", "precisionSano", "precisionNoSano", "sanosComoSanos", "sanosComoNoSanos", "noSanosComoSanos", "noSanosComoNoSanos", "tiempoPorGranoMs"}]}`. No necesita el modelo cargado. |
| `POST /revisar` | Multipart, campo `foto` (JPG o PNG). La revisión rápida de D-46: `{"estado": "APROBADA" \| "POR_REPETIR", "motivo": null \| "BORROSA" \| "POCA_LUZ" \| "GRANOS_PEGADOS", "granosEncontrados", "regionesPegadas": [{"x", "y", "ancho", "alto"}], "anchoImagen", "altoImagen"}`. |
| `POST /analizar` | Multipart, campos `foto1`, `foto2` y, si hay, `foto3`. `{"modelo": {"nombre", "version"}, "fotos": [{"numero", "revision": {…como /revisar…}, "granos": [{"x", "y", "ancho", "alto", "clase": "SANO" \| "NO_SANO", "confianza"}]}]}`. 503 mientras el modelo carga. |

- 400 `{"error": "IMAGEN_INVALIDA", "mensaje"}` si un archivo no es un JPG o PNG que se pueda leer o pasa de 120 megapíxeles (el tope de Pillow, unos 89, se sube a 120 de forma explícita en `imagen.py`); 413 `IMAGEN_MUY_GRANDE` si pasa de `INSPECTIA_TAMANO_MAXIMO`.
- `granosEncontrados` cuenta los granos sueltos y completos: no cuenta los cortados por el borde ni los que forman una región de granos pegados.
- Una foto `POR_REPETIR` va sin granos. El motivo se elige en este orden: poca luz, borrosa, granos pegados. `regionesPegadas` solo trae algo con el motivo `GRANOS_PEGADOS`.
- La clase es `NO_SANO` si la probabilidad de mala es 0,5 o más, el umbral con que Julián midió el modelo (RF-08). La confianza es la probabilidad de la clase elegida × 100, con 2 decimales, de 50 a 100. `GRANOS_DUDOSOS`, el mínimo de 100 granos y la devolución de la muestra (RF-09, D-44) los decide el backend.

## Cómo separa y clasifica

La separación es la de `generar_recortes.py`, el script con que se hicieron los recortes del entrenamiento (D-08): se estima la iluminación del papel y se divide la imagen por ella, se marca como grano lo más oscuro que 0,6 veces el papel, se limpia la máscara y se descartan las regiones pequeñas y las que tocan el borde. Cada grano se gira para dejar su eje largo horizontal y se recorta con un margen del 15 % de su lado corto (`recortar`, sin cambios). Lo que cambia, porque la foto de un lote tiene unos 50 granos y no uno:

- La detección corre en una imagen de 1156 px de lado mayor (la escala de las fotos del dataset en el script), así sus filtros miden lo mismo con cualquier cámara.
- Se guardan todas las regiones. El área mínima baja de 0,0015 a 0,0003 de la imagen y además se descarta como mancha lo que mide menos de 0,3 veces la mediana de la foto.
- Granos pegados: una región con al menos el doble del área mediana de la foto (D-09).
- Los recortes salen de la foto en su resolución completa, para darle a cada grano todos los píxeles que tiene (P-12).

Antes de clasificar, cada recorte pasa por un JPG de calidad 95 y se lee con `tf.io.decode_jpeg`, como los recortes del entrenamiento, y se prepara con las mismas operaciones de `preparar` de `inferencia.py` (borde prolongado y suavizado, 224 × 224). Se midió que el modelo es sensible a esto: leer el mismo JPG con Pillow en lugar de TensorFlow mueve P(mala) hasta 0,05. Con el mismo JPG, el servicio da las mismas probabilidades que `inferencia.py` (diferencia menor que 0,002, `test_paridad.py`). Los recortes se clasifican todos juntos, en lotes de `INSPECTIA_LOTE_CLASIFICACION`.

Con 20 fotos originales del conjunto de prueba, el recorte del servicio (de la foto completa) y el del script (de la foto a la mitad) dan la misma clase en los 20; P(mala) difiere 0,033 en promedio (máximo 0,086), un poco más baja con el recorte de más resolución.

### Umbrales de la revisión

Son provisionales hasta probar con fotos reales de unos 50 granos (P-12) y se cambian por variables de entorno, sin tocar el código. Se fijaron con 30 fotos originales del conjunto de prueba del dataset:

| Medida | Cómo se mide | Fotos del dataset | Umbral |
|---|---|---|---|
| Luz | Mediana del brillo estimado del papel (0 a 255) | 169 a 193 (mediana 182); con la mitad de la luz, 84 a 96 | Menos de 110 (60 % de la mediana): `POCA_LUZ` |
| Nitidez | Varianza del laplaciano en una franja de 7 px alrededor del borde de los granos, en la imagen de detección | 1346 a 4766; desenfocadas con un gaussiano de 3 px, 168 a 424, y de 6 px, 26 a 58 | Menos de 100: `BORROSA` |

La nitidez se mide en el borde de los granos y no en toda la foto para que no dependa de cuánto papel vacío tenga. En 3 fotos compuestas con 50 recortes reales cada una (papel con sombra y ruido), la luz dio 192 y la nitidez de 2347 a 2413: las tres pasan, con los 50 granos cada una.

## Modelos

`modelos.json` (en el repositorio y dentro de la imagen) trae los 5 modelos de la release `modelos-v1.0`: nombre, versión, tipo (`CNN` o `TRANSFORMER`), formato, archivo, SHA-256, la ficha JSON de cada uno y sus métricas en el conjunto de prueba (334 granos). F1 macro y recall de cada clase con los 3 decimales del README de Julián (D-39, D-48); precisión de cada clase con los 4 de su JSON; precisión y recall macro, de la matriz de confusión; tiempo por grano con 2 hilos, la estimación de Julián para la t4g.medium. El servicio solo carga modelos en SavedModel (D-40): hoy, EfficientNet-B0; los otros 4 están en formato `.keras` y sirven para la comparación (`GET /modelos`).

Al arrancar, el servicio busca el archivo del modelo en la caché (`INSPECTIA_MODELOS_CACHE`); si no está o su SHA-256 no coincide con `modelos.json`, lo baja de `INSPECTIA_MODELOS_URI`. Si lo bajado tampoco coincide, el servicio no arranca. Lo comprueba en cada arranque, descomprime el zip (rechaza rutas fuera de su carpeta) y carga el SavedModel en otro hilo; mientras tanto `/salud` responde 503. Si la carga falla, el proceso termina y Docker lo reinicia.

### Subir los modelos a S3

`scripts/subir_modelos.py` baja de la release de Julián el manifest, los 5 modelos y sus fichas JSON (11 archivos, unos 360 MB), comprueba cada SHA-256 con `modelos.json` y con el `manifest.json` de la release y los sube a `s3://inspectia-archivos-<cuenta>/modelos/` con el mismo nombre. Cada objeto lleva su SHA-256 como checksum (S3 lo rechaza si no llega igual) y en sus metadatos; lo que ya está con el mismo SHA-256 no se vuelve a subir. Si un SHA-256 no coincide, no sube nada.

Se corre una vez, con credenciales de AWS que puedan escribir en el bucket (por ejemplo, después de `aws sso login` o con un perfil). Desde la raíz del repositorio, con el entorno del servicio:

```bash
ia-servicio/.venv/Scripts/python ia-servicio/scripts/subir_modelos.py --bucket inspectia-archivos-<cuenta>
# opciones: --perfil <perfil de ~/.aws>, --region us-east-1, --prefijo modelos/,
#           --carpeta <dónde guardar lo bajado>, --simular (baja y comprueba, no sube)
```

En Linux o macOS, `ia-servicio/.venv/bin/python`.

## Variables de entorno

| Variable | Por defecto | Qué es |
|---|---|---|
| `INSPECTIA_MODELO` | `EfficientNet-B0` | El modelo en uso, como en `modelos.json`. |
| `INSPECTIA_MODELOS_URI` | (obligatoria) | `s3://inspectia-archivos-<cuenta>/modelos/` en AWS, con el rol de la instancia; `file:///carpeta/` en desarrollo y en la CI. |
| `INSPECTIA_MODELOS_CACHE` | `/var/lib/inspectia/modelos` | Carpeta de la caché (un volumen de Docker). |
| `AWS_REGION` | — | `us-east-1` en AWS. Las credenciales salen del rol de la instancia (IMDSv2), nunca de archivos. |
| `INSPECTIA_HILOS` | 0 (los de TensorFlow) | Hilos de CPU de TensorFlow. |
| `INSPECTIA_LOTE_CLASIFICACION` | 16 | Recortes por llamada al modelo. Con 16 el servicio cabe en el límite de memoria del contenedor (ver «Memoria»). |
| `INSPECTIA_TAMANO_MAXIMO` | 20971520 (20 MB) | Tamaño máximo de una foto. |
| `INSPECTIA_LUZ_MINIMA` | 110 | Umbral de luz (RF-06 C2). |
| `INSPECTIA_NITIDEZ_MINIMA` | 100 | Umbral de nitidez (RF-06 C1). |
| `INSPECTIA_FACTOR_PEGADOS` | 2.0 | Granos pegados: múltiplo de la mediana (D-09). |
| `INSPECTIA_AREA_MINIMA` | 0.0003 | Área mínima de una región, relativa a la imagen de detección. |
| `INSPECTIA_FACTOR_MANCHA` | 0.3 | Mancha: fracción de la mediana. |
| `INSPECTIA_LADO_DETECCION` | 1156 | Lado mayor de la imagen de detección, en píxeles. |
| `INSPECTIA_RAZON_GRANO_PAPEL` | 0.6 | Un píxel es grano si es más oscuro que esta fracción del papel. |

## Correrlo en local

Con Python 3.12 (si no está, `uv python install 3.12` lo instala sin tocar el Python del sistema):

```bash
cd ia-servicio
py -3.12 -m venv .venv                  # o: uv venv .venv --python 3.12
.venv/Scripts/python -m pip install -r requirements-dev.txt
```

Para arrancarlo hace falta el zip del modelo en una carpeta (bájalo de la release o con `scripts/subir_modelos.py --simular --carpeta <carpeta>`):

```bash
# PowerShell: $env:INSPECTIA_MODELOS_URI = 'file:///C:/ruta/modelos/'; $env:INSPECTIA_MODELOS_CACHE = "$env:TEMP/inspectia-modelos"
INSPECTIA_MODELOS_URI=file:///ruta/modelos/ INSPECTIA_MODELOS_CACHE=/tmp/inspectia-modelos \
  .venv/Scripts/python -m uvicorn inspectia_ia.principal:app --port 8000
```

Con Docker (la imagen es multiarquitectura; en la CI se construye para arm64):

```bash
docker build -t inspectia-ia-servicio ia-servicio
docker run --rm -p 8000:8000 -v /ruta/modelos:/modelos:ro -e INSPECTIA_MODELOS_URI=file:///modelos/ \
  -v inspectia-modelos:/var/lib/inspectia/modelos inspectia-ia-servicio
```

La imagen usa `python:3.12.16-slim-trixie` (Debian; TensorFlow y OpenCV no publican ruedas para Alpine), corre con un usuario sin privilegios y su HEALTHCHECK pide `/salud`.

## Pruebas

```bash
cd ia-servicio
.venv/Scripts/python -m pytest
```

Cada prueba lleva su marca `@pytest.mark.caso("RF-07 · C1 · …")` (RP-13). Las fotos se dibujan en la prueba (granos como elipses sobre papel con sombra y ruido): 50 granos separados, pegados, cortados por el borde, borrosa, oscura y con orientación EXIF. La API se prueba con un clasificador falso; S3, con un doble de botocore (Stubber), sin red. La prueba de paridad (`test_paridad.py`) usa el modelo real y 5 recortes reales del conjunto de prueba (`pruebas/datos/recortes/`, con la cita del dataset): define `INSPECTIA_MODELO_PRUEBA` con la ruta del zip del SavedModel; sin ella, se salta con el motivo. La CI (job «Servicio de IA») baja el zip de la release, comprueba su SHA-256 y las corre todas.

## Tiempos medidos

En el PC de desarrollo (Intel Core Ultra 9 275HX, Windows 11, Python 3.12.16, TensorFlow 2.21.0), con 3 fotos compuestas de 4000 × 3000 px con 50 recortes reales cada una (150 granos), en 3 vueltas:

| Paso | 2 hilos de TensorFlow | Todos los núcleos (24) |
|---|---|---|
| Leer, revisar, separar y recortar las 3 fotos | 350 a 400 ms | 350 a 400 ms |
| Clasificar los 150 granos (JPG, preparación y modelo) | 1975 a 2089 ms | 970 a 1023 ms |
| Total del servicio | 2354 a 2485 ms | 1334 a 1407 ms |
| Cargar el modelo al arrancar | 4,5 s | 4,4 s |

Con 2 hilos son unos 13 ms por grano, cerca de la estimación de Julián para la t4g.medium (19 ms). RNF-01 pide 8 s para todo el análisis (subir las fotos, este servicio y la respuesta) y se mide en la t4g.medium real (RNF-01 C3): su procesador ARM (Graviton2) es más lento por núcleo que el de este PC, así que allí el servicio puede tardar el doble. Hay que medirlo al desplegar.

### Memoria

La memoria del proceso crece con el lote de clasificación y no baja después de cada análisis (TensorFlow guarda lo que reservó). Medido en el mismo PC, con el modelo real, 2 hilos y 150 recortes reales del conjunto de prueba (de 396 × 666 a 481 × 603 px), en 3 vueltas seguidas:

| Lote | Memoria en uso tras cargar el modelo | Memoria en uso tras 150 granos | Clasificar 150 granos |
|---|---|---|---|
| 16 (por defecto) | 454 MB | 742 a 745 MB | 3,07 a 3,33 s |
| 32 | 449 MB | 1023 a 1028 MB | 2,34 a 2,54 s |
| 64 | 454 MB | 1589 a 1594 MB | 2,88 a 3,17 s |

Con 64 el proceso pasa de 1,5 GB, el límite del contenedor en producción, antes de abrir las fotos de 12 MP de `/analizar` (cada una ocupa de 150 a 250 MB mientras se revisa). Con 16 queda en unos 0,75 GB y el tiempo casi no cambia. La medición es de Windows x86; en la t4g.medium (arm64) hay que confirmarla con `docker stats` junto con RNF-01 C3.

## Dataset

Los recortes de `pruebas/datos/recortes/` y el modelo salen del dataset «Ecuadorian cacao beans for classification» (Kaggle), licencia CC BY-NC 4.0: uso académico citando a los autores, sin uso comercial (RNF-08). Zhinin-Vera, L. et al. (2023). Artificial Vision Technique to Detect and Classify Cocoa Beans. IWANN 2023, LNCS 14135, Springer. DOI 10.1007/978-3-031-43078-7_18.
