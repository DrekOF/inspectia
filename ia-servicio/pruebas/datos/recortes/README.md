<!--
Nombre: README.md (pruebas/datos/recortes)
Descripción: Origen, licencia y cita de los 5 recortes de granos que usa la prueba de paridad del servicio de IA (RNF-08, D-40).
Fecha de creación: 24/11/2026
Actualización: 24/11/2026
Autor: Equipo InspectIA
Versión: 1.0
-->

# Recortes de la prueba de paridad

Cinco recortes de un grano cada uno, copiados sin cambios de `recortes_script/val/` (el conjunto de prueba de 334 granos, RNF-07). Los hizo `generar_recortes.py` a partir de las fotos originales del dataset. No son fotos enteras del dataset: el dataset no va al repositorio (RNF-08).

| Archivo | Recorte original | Clase del dataset |
|---|---|---|
| `mala-1.jpg` | `val/mala (1).jpg` | mala (no sano) |
| `mala-12.jpg` | `val/mala (12).jpg` | mala (no sano) |
| `sana-1049.jpg` | `val/sana (1049).jpg` | sana |
| `sana-1117.jpg` | `val/sana (1117).jpg` | sana |
| `sana-1210.jpg` | `val/sana (1210).jpg` | sana |

Se eligieron para cubrir aciertos y errores del modelo: `mala-1` y `sana-1117` los clasifica mal, como pasa con cualquier modelo en el conjunto de prueba (F1 macro 0,735).

`esperado.json` tiene las probabilidades que da `inferencia.py` de Julián (release `modelos-v1.0`), sin cambios, con el SavedModel de EfficientNet-B0. `test_paridad.py` exige que el servicio dé las mismas, con una diferencia menor que 0,002 y sin cambiar ninguna clase, como en D-40.

## Cita y licencia del dataset

Dataset «Ecuadorian cacao beans for classification» (cacao-beans-for-classification), Luis Fernando Zhinin Vera, Kaggle, 2022: https://www.kaggle.com/datasets/luiszhinin979/ecuadorian-cocoa-beans-for-classification

Licencia CC BY-NC 4.0: uso académico citando a los autores; no permite uso comercial.

Artículo: Zhinin-Vera, L. et al. (2023). Artificial Vision Technique to Detect and Classify Cocoa Beans. IWANN 2023, LNCS 14135, Springer. DOI 10.1007/978-3-031-43078-7_18.
