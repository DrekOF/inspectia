<!--
Nombre: prueba-de-usabilidad.md
Descripción: Formato corto e imprimible de la prueba de usabilidad de RNF-06 C1: 3 personas que no conocen InspectIA
             hacen un análisis completo cada una; se anotan los tiempos, la ayuda, los errores y las fotos usadas.
Fecha de creación: 24/11/2026
Actualización: 24/11/2026
Autor: Equipo InspectIA
Versión: 1.0
-->

# Prueba de usabilidad (RNF-06 C1)

**Criterio:** en una prueba con 3 personas que no conocen la app, cada una completa un análisis en menos de 5 minutos, sin ayuda.

**Pasa** si las 3 personas terminan en menos de 5 minutos (desde «Nuevo lote» hasta ver el porcentaje del resultado) y ninguna necesita ayuda. Los resultados se copian en [verificaciones.md](verificaciones.md).

## Antes de empezar (quien dirige la prueba)

- **La app:** la desplegada en AWS, con el modelo real. Cada persona entra con un usuario final propio de la empresa de la demostración, creado antes por su administradora. No se usan datos personales reales.
- **Las fotos:** cada persona fotografía una muestra **real** de 100 a 150 granos de cacao, siguiendo la guía de captura de la app: granos enteros y separados sobre papel blanco, luz difusa, desde arriba, en 2 o 3 fotos de unos 50 granos. Si no hay granos para las 3, quien dirige toma antes 2 o 3 juegos de fotos de muestras reales y los deja en el celular o en una carpeta fuera del repositorio, por ejemplo `..\fotos-prueba-usabilidad\`.
  - Las fotos del dataset (`..\Cacao_dataset_kaggle\`) **no sirven**: cada una tiene un solo grano.
  - Ninguna foto se sube al repositorio (RNF-08 y `CLAUDE.md`).
- **Qué se le dice a cada persona, y nada más:** «Recibiste un lote de cacao. Regístralo con el código que quieras, sube las fotos de su muestra y lee el resultado. Avísame cuando veas el porcentaje.» Quien dirige no explica la app ni contesta preguntas sobre cómo usarla: si la persona pide ayuda, se anota y se la da.
- **El reloj:** se mide desde que la persona toca «Nuevo lote» hasta que ve el porcentaje de granos sanos. Si toma las fotos durante la prueba, ese tiempo también cuenta.

## Hoja de cada persona

Una hoja por persona. Las horas, con minutos y segundos (hh:mm:ss).

| Dato | Persona 1 | Persona 2 | Persona 3 |
|---|---|---|---|
| Fecha | | | |
| Ocupación (sin nombre) | | | |
| ¿Conocía InspectIA? (debe ser «no») | | | |
| Dispositivo (PC o celular, y modelo) | | | |
| Navegador y versión | | | |
| Fotos usadas (cuántas, de qué muestra o qué juego, cuántos granos) | | | |
| 1. Registrar el lote: inicio | | | |
| 1. Registrar el lote: fin | | | |
| 2. Subir las fotos: inicio | | | |
| 2. Subir las fotos: fin («Analizar la muestra») | | | |
| 3. Ver el resultado: inicio | | | |
| 3. Ver el resultado: fin (ve el porcentaje) | | | |
| **Tiempo total** (de 1 inicio a 3 fin) | | | |
| ¿Pidió ayuda? ¿Dónde y qué preguntó? | | | |
| Errores que vio (mensaje y pantalla) | | | |
| ¿Tuvo que repetir alguna foto? ¿Por qué? | | | |
| Comentarios de la persona | | | |
| **¿Pasa?** (menos de 5 min y sin ayuda) | | | |

## Al terminar

- [ ] Las 3 personas pasan: copiar en [verificaciones.md](verificaciones.md), en RNF-06 C1, el tiempo total y si pidieron ayuda, y cambiar su estado a `**Estado:** Verificado.`
- [ ] Alguna no pasa: anotar qué la detuvo, abrir un issue con lo que hay que corregir y repetir la prueba con otra persona después de corregirlo.
- [ ] Volver a armar los casos de prueba: `generador/.venv/Scripts/python generador/casos_de_prueba.py generar`.
- [ ] Anotar la prueba en `docs/bitacora.md`.
