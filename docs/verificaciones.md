<!--
Nombre: verificaciones.md
Descripción: Verificaciones documentadas de los criterios de aceptación que no se prueban con código (RP-13):
             qué se revisó, con qué evidencia y qué falta. generador/casos_de_prueba.py las lee y las muestra en
             docs/casos-de-prueba.md junto a las pruebas de cada criterio.
Fecha de creación: 24/11/2026
Actualización: 24/11/2026
Autor: Equipo InspectIA
Versión: 1.0
-->

# Verificaciones documentadas

Algunos criterios de `specs/fichas.json` no se pueden comprobar con una prueba automática: una prueba con personas, lo que solo puede confirmar quien entrenó el modelo o un compromiso del equipo. Cada uno queda aquí como una **verificación documentada**: qué se revisó, la evidencia y lo que todavía falta.

## Cómo se escribe

- Cada verificación es una sección cuyo título es la marca del criterio, como en las pruebas: `## <código> · C<n> · …`. Las secciones cuyo título no empieza con un código, como esta, no cuentan.
- Debajo del título va su estado, en una línea: `**Estado:** Verificado.` o `**Estado:** Pendiente: <qué falta>.`
- Un criterio tiene como máximo una verificación.
- `generador/casos_de_prueba.py` las lee y, en `docs/casos-de-prueba.md`, las muestra junto a las pruebas del criterio como «verificación documentada». Una **verificada** cubre el criterio. Una **pendiente** lo deja pendiente aunque tenga pruebas, porque dice que falta algo: así una verificación nunca esconde un criterio sin evidencia. `comprobar` falla si una verificación está mal escrita o cita un criterio que no existe.
- Las citas del README de Julián son de su versión 4.3 (actualizada el 26/10/2026), en la rama `main` de [JulianSilvaMorales/Proyecto_IA-Cacao-Beans-](https://github.com/JulianSilvaMorales/Proyecto_IA-Cacao-Beans-). Ojo con los nombres: lo que las fichas llaman conjunto de prueba (`recortes_script/val`, 334 granos: 261 sanos y 73 no sanos) es el `test` de Julián; su `val` son 195 granos sacados del `train` del equipo.

## RNF-06 · C1 · prueba con 3 personas que no conocen la app: cada una completa un análisis en menos de 5 minutos, sin ayuda

**Estado:** Pendiente: falta la prueba con 3 personas, prevista para el 25/11/2026 con el formato de [prueba-de-usabilidad.md](prueba-de-usabilidad.md).

- **Cómo queda verificado:** 3 personas que no conocen InspectIA hacen, cada una, un análisis completo en la app desplegada: registrar un lote, subir 2 o 3 fotos de una muestra real y ver el resultado. Se anotan los tiempos de cada paso, si pidieron ayuda y los errores que vieron, en el formato de [prueba-de-usabilidad.md](prueba-de-usabilidad.md).
- **Pasa** si las 3 terminan en menos de 5 minutos y sin ayuda. Si alguna no pasa, se anota qué la detuvo, se corrige y se repite la prueba con otra persona.
- **Lo que ya prueba el código:** el número de pantallas (RNF-06 C2, `frontend/e2e/usabilidad.spec.js`), los mensajes de error (C3) y el teclado y el contraste (C4).
- **Al terminar:** se copian aquí los resultados (persona, tiempo total, ayuda sí o no) y el estado pasa a «Verificado».

## RNF-07 · C1 · el F1-score macro del modelo elegido en el conjunto de prueba es de 0,65 o más: EfficientNet-B0 logra 0,735

**Estado:** Verificado.

- **Con código:** `ia-servicio/pruebas/test_metricas_del_modelo.py` comprueba que `modelos.json` tiene, para EfficientNet-B0, el F1 macro de la tabla «Resultados» del README de Julián (0,735) y que recalculado desde su matriz de confusión (238 / 23 / 34 / 39) también es de 65 % o más. `ComparativaDeModelosIT` comprueba que ese modelo queda en uso en la tabla `modelo` y en la API, y que la base no deja en uso uno con menos de 65 % (P-02).
- **Lo que el código no puede hacer:** volver a medir el modelo con los 334 granos. El conjunto de prueba no está en el repositorio (RNF-08) y se usa una sola vez (C5). La medición es de Julián.
- **Cita del README de Julián** («Medición final en `test`», «Resultados»): la tabla da para EfficientNet-B0 «0,735 (0,677–0,792)» de F1 macro, con la matriz 238 / 23 / 34 / 39.

## RNF-07 · C3 · los 5 modelos se miden con el mismo conjunto de prueba, sin imágenes repetidas del entrenamiento ni del val

**Estado:** Verificado.

- **Con código:** `test_metricas_del_modelo.py` comprueba que la matriz de cada uno de los 5 modelos suma 261 sanos y 73 no sanos (334 granos), y `ComparativaDeModelosIT`, que así quedan en la API.
- **Lo que solo Julián puede confirmar:** que ninguna imagen del conjunto de prueba está repetida en su `train` ni en su `val`.
- **Cita del README de Julián** («Partición (semilla 42, estratificada)», «Por qué se particionó así»): «el script verifica con `assert` que no hay solapamiento por ruta ni por md5». En su repositorio, `ml/src/05_split.py` tiene esos `assert` (por ruta y por md5) y comprueba que los tres conjuntos suman 1634.
- **Límite:** el md5 encuentra archivos idénticos, no el mismo grano fotografiado dos veces. Se le pregunta a Julián (ver abajo).

## RNF-07 · C5 · el modelo se elige con el val de Julián, antes de medir la prueba, y la prueba se usa una sola vez

**Estado:** Verificado.

- **Lo que solo Julián puede confirmar:** el orden de lo que hizo. No hay nada en InspectIA que lo pruebe.
- **Cita del README de Julián** («Medición final en `test`», «Prerregistro»): la regla del modelo de la app es «el de mejor F1 en `val`» entre los que clasifican 100 granos en 4 s o menos, y en la «Lectura» dice que EfficientNet-B0 «se eligió sin ver `test`». El título de «Resultados» dice que `test` se usó «una sola vez el 26-oct-2026».
- **En su repositorio:** el prerregistro (`ml/results/test_final/preregistro.txt`) entró en el commit `c7e216d` (26/10/2026, 16:49 UTC), antes que la medición, en `642de84` (17:14 UTC). `ml/src/21_test_final.py` se niega a correr si ya existe `ml/results/test_final/test_usado.txt`, y ese archivo dice que `test` se usó el 26/10/2026 a las 11:59, hora de Colombia, entre los dos commits.
- **Límite:** la marca de un solo uso es un archivo que se puede borrar; que no se haya medido antes, en la validación cruzada o en Hyperband, lo confirma Julián (ver abajo).

## RNF-08 · C3 · mientras use modelos entrenados con este dataset, InspectIA no se cobra

**Estado:** Verificado.

- **Con código:** `generador/pruebas/test_sin_cobro.py` comprueba que ninguna entidad, tabla, API, pantalla ni el servicio de IA habla de precios, pagos, facturas ni suscripciones (en español y en inglés, también dentro de los identificadores), y que el README y Acerca de, en español y en inglés, dicen que InspectIA no se cobra mientras use modelos entrenados con el dataset.
- **Lo que el código no puede probar:** que nadie cobre por usarla. Es un compromiso del equipo, por la licencia CC BY-NC 4.0 del dataset. InspectIA es un proyecto de curso; no tiene clientes ni un medio de pago. El presupuesto de AWS (`infra/`) es lo que gasta la cuenta del equipo, no algo que se le cobre a una empresa.
- **Cuándo se vuelve a revisar:** si alguien propone cobrar, antes hay que entrenar el modelo con fotos propias o pedir permiso a los autores (descripción de RNF-08), y eso cambia la ficha: lo decide el equipo.

## Para confirmar con Julián

Las verificaciones de RNF-07 C3 y C5 se apoyan en su README y en su repositorio. Estas son las preguntas para que las confirme por escrito; su respuesta se copia aquí:

1. ¿Alguna imagen del `test` (los 334 de `recortes_script/val`) es del mismo grano que una del `train` o del `val`, aunque sea otra foto? El `assert` por md5 de `05_split.py` solo encuentra archivos idénticos.
2. Antes del 26/10/2026, ¿se usó el `test` para algo: la validación cruzada (`16_cv_ceiling.py`), Hyperband, los ensembles, el umbral o la elección del modelo? ¿Todas esas decisiones salieron solo del `train` y del `val`?
3. ¿`21_test_final.py` se corrió una sola vez sobre el `test`, la del 26/10/2026 a las 11:59, y `test_usado.txt` nunca se borró?
4. Los 5 modelos de la release `modelos-v1.0`, ¿son los mismos que se midieron ese día (ronda 15 del entrenamiento federado formal), sin reentrenar después?
