<!--
Nombre: README.md (frontend)
Descripción: La página web de InspectIA: qué hay en frontend/, cómo arrancarla junto con el backend, sus textos,
             sus pruebas, el entorno aislado de las pruebas de punta a punta y la demostración de otra temática.
Fecha de creación: 12/10/2026
Actualización: 24/11/2026
Autor: Equipo InspectIA
Versión: 1.12
-->

# frontend

Página web de InspectIA, para PC y celular (RNF-05). Hecha con React 19, Material UI 9 y Vite 8, con Node 24.22.0 (la de `.node-version`). Las versiones de cada librería están en `package.json` y en la tabla de versiones del README principal.

Esta versión tiene el marco (barra superior, menú según el rol, idioma y tema), Iniciar sesión (RF-01), Cambiar contraseña (RF-18), Acerca de, con la cita del dataset (RNF-08), las pantallas de Empresas (RF-02), Usuarios (RF-03) y Rangos de recomendación (RF-13), generadas desde su JSON (RP-23), y el análisis de un lote: Nuevo lote (RF-04), el Historial (RF-14), con «Continuar» en los lotes pendientes y «Ver» en los analizados, Fotos de la muestra, con la guía de captura, las 3 casillas de las fotos y «Analizar la muestra» (RF-05, RF-14 C5), y el Resultado del lote, con la espera del análisis, el porcentaje corregido con su rango probable, la categoría probable y las fotos con cada grano marcado (RF-10 a RF-12, RF-14 C3). Las demás secciones del menú dicen «Esta sección estará disponible pronto.» hasta que llegue su función. La referencia visual es el prototipo de `02_Analisis_Diseno/Prototipo_Frontend`, fuera del repositorio.

## Qué hay

| Carpeta | Qué va ahí |
|---|---|
| `src/generated/` | Lo que sale de `generador/` (RP-23): las pantallas, los clientes de su API, sus validaciones de Yup, sus textos y sus pruebas. No se edita a mano. |
| `src/crud/` | Lo común de las pantallas generadas: la lista en páginas de 20 (tabla en el PC y tarjetas en el celular), el formulario, encima de la lista o a su lado en el PC (D-37), la confirmación al desactivar, el máximo de activos, la fila de quien tiene la sesión y el foco al abrir y cerrar un panel. También la pantalla en bloque (`PantallaEnBloque`), con todas las filas en un formulario, como los rangos, y los decimales con el separador del idioma (`decimales.js`). |
| `src/empresa/` | Lo escrito a mano para la pantalla de empresas: el panel de administradores, que restablece la contraseña de uno (RF-02 C12). |
| `src/rango/` | Lo escrito a mano para la pantalla de rangos (RF-13): el resumen «Así quedan los rangos» y la regla del orden de los límites (`limites.js`), la misma del backend. |
| `src/lote/` | El análisis de un lote, escrito a mano: Nuevo lote (RF-04), con las sugerencias de finca o proveedor y su esquema de Yup (`esquemaDeLote.js`); el Historial (RF-14), con sus filtros en la dirección de la página (`FiltrosDelHistorial.jsx` y `filtros.js`); Fotos de la muestra, también la de la muestra 2, con la guía de captura y las casillas de las fotos (`TusFotos.jsx`, `CasillaDeFoto.jsx`), que revisan cada archivo antes de subirlo (`archivosDeFoto.js`) y lo suben directo al enlace firmado, sin el token (`src/api/fotos.js`, D-41); «Analizar la muestra» (`AnalizarMuestra.jsx`); el Resultado del lote (`ResultadoDelLote.jsx`), con la espera (`EsperaDelAnalisis.jsx`, que consulta el avance con `useAnalisisEnCurso.js`), el resumen (`ResumenDelResultado.jsx`), el aviso de la sugerencia de otra muestra (`SugerenciaDeOtraMuestra.jsx`, D-47) y las fotos marcadas de cada muestra (`FotosMarcadas.jsx`, `FotoMarcada.jsx` y `LeyendaDeMarcas.jsx`), con sus funciones puras en `granos.js` y `resultado.js`; la revisión de cada foto por el servicio de IA (RF-06, RF-07, RF-09, D-46): la casilla cambia el chip según el estado, dice cuántos granos tiene una foto aprobada o qué hacer con una por repetir, y la muestra entera con las zonas de granos pegados o los granos dudosos marcados (`FotoRevisada.jsx`); el total de granos de las fotos y el aviso de la muestra devuelta (`TusFotos.jsx`); el motivo de «Analizar la muestra» desactivado mientras hay fotos por repetir; por qué volvió la muestra sin resultado y qué hacer (`AnalisisSinTerminar.jsx`, D-44), y el aviso de confianza baja del resultado (`AvisoDeConfianzaBaja.jsx`), con sus funciones puras en `revision.js`; los pasos del análisis, y las fechas en la hora de Colombia (`fechas.js`). |
| `src/extensiones.js` | Lo escrito a mano de cada pantalla generada, por entidad. Las rutas se lo pasan a la pantalla, así lo generado no importa código escrito a mano. |
| `src/paginas/` | Las pantallas: Iniciar sesión, el inicio de cada rol, Cambiar contraseña, Acerca de, las secciones pendientes y las direcciones que no existen. |
| `src/navegacion/` | El marco de las pantallas con sesión (barra, menú y «Saltar al contenido»), las rutas y el menú de cada rol (`menu.js`). |
| `src/sesion/` | La sesión (D-28, D-31): se guarda en el navegador, vence a las 8 horas, se cierra con un 401 y vuelve a la pantalla anterior si entra el mismo usuario. |
| `src/api/` | El cliente de la API: el token, el idioma y los errores `application/problem+json` del backend. |
| `src/componentes/` | Piezas comunes: campos, avisos, tarjetas, títulos, la marca, los selectores de idioma y tema, y el estilo de lo que solo leen los lectores de pantalla (`ocultoALaVista.js`). |
| `src/validacion/` | Los esquemas de Yup de las páginas escritas a mano (RP-07), con los mismos límites y mensajes del backend; el de Nuevo lote va con su pantalla, en `src/lote/`. La contraseña nueva de Cambiar contraseña usa la validación generada de la contraseña de un usuario, en el orden en que la API escoge el error. En `reglas.js` va también lo que usan las validaciones generadas, como leer un número escrito con coma o con punto. |
| `src/i18n/` | Los idiomas (RP-11): español por defecto e inglés. |
| `src/tema/` | Los tokens de color, letra y medidas (`tokens.js`) y el tema de Material UI, claro y oscuro (RP-08). |
| `herramientas/` | La lectura de los `.properties` del backend (plugin de Vite), la copia en espejo que usan el entorno de punta a punta y la demostración, y la demostración de otra temática (`otra-tematica.js`). |
| `e2e/` | Las pruebas de punta a punta con Playwright, su entorno aislado (`e2e/entorno/`) y las generadas de cada pantalla (`e2e/generated/`). |
| `selenium/` | Las pruebas con Selenium, en Chrome y en Edge. |
| `reports/` | Lo que dejan las pruebas y no se sube: el informe de mutación y, si una prueba de Selenium falla, su captura y la consola del navegador. |

Las pantallas CRUD salen de `generador/` y no se editan a mano (RP-23): hoy las de empresas, usuarios y rangos. Para cambiar una, se cambia su JSON o la plantilla y se vuelve a generar. Lo que su JSON no puede declarar va en `src/extensiones.js`.

## Textos

Las etiquetas y los mensajes que la página comparte con el backend no se copian: salen de `backend/src/generated/resources/i18n/generado*.properties` y `backend/src/main/resources/i18n/mensajes*.properties`. `herramientas/textos-del-backend.js` los entrega como el módulo `virtual:textos-del-backend`, y en desarrollo la página se recarga si cambian. Los textos propios del frontend van en `src/i18n/es.js` y `src/i18n/en.js`, con las mismas claves. Los marcadores van entre llaves, como en el backend: `{0}`, `{max}`. Los textos propios de cada pantalla generada salen de `pantalla.textos` de su JSON (`src/generated/textos.js`); los comunes a todas, como «Guardar cambios», van en `src/i18n/`.

## Comandos

Desde `frontend/`. La primera vez, `npm ci`.

| Comando | Qué hace |
|---|---|
| `npm run dev` | Arranca la página en http://localhost:5173. Vite pasa las peticiones de `/api` al backend de http://localhost:8080; si corre en otro puerto, define `INSPECTIA_API_URL`. |
| `npm run build` | Compila la página en `dist/`. |
| `npm test` | Pruebas unitarias y de componentes (Vitest), con la API simulada (MSW): no necesitan el backend. |
| `npm run test:cobertura` | Las mismas, con la cobertura. |
| `npm run test:mutacion` | Pruebas de mutación con Stryker (RP-12), con la mitad de los procesadores y sin los mutantes estáticos (el porqué está en `stryker.config.mjs`). El informe queda en `reports/mutacion/index.html`. |
| `npm run lint` | ESLint: JSDoc en lo exportado (RP-09), las reglas de los hooks de React y los colores solo en `src/tema/tokens.js` (RP-08). |
| `npm run e2e:playwright` | Pruebas de punta a punta con Playwright (RP-14), en su entorno aislado. |
| `npm run e2e:selenium` | Pruebas con Selenium en Chrome y Edge, en su entorno aislado. |
| `npm run e2e:entorno` | Enciende el entorno aislado y lo deja encendido hasta Ctrl+C. |
| `npm run demo:otra-tematica` | La demostración de otra temática (RP-23), en una copia del proyecto (ver abajo). Con `-- --entidad <ruta del JSON>`, agrega esa entidad en vez de producto; con `-- --con-pruebas`, antes de encenderla corre sus pruebas generadas. |

En Vitest, `findBy` y `waitFor` esperan hasta 5 s y cada prueba tiene hasta 20 s (`src/pruebas/preparacion.js` y `vite.config.js`). Con la máquina ocupada, como con dos corridas a la vez o con Stryker, una pantalla que se carga la primera vez que se abre tarda más de un segundo, y las pruebas que llenan un formulario entero pasan de 5 s. Si Vitest corta una prueba, lo que ella seguía escribiendo cae en el campo con el foco de la prueba siguiente, que falla también. Una prueba que pasa no espera más por esto.

## Pruebas de punta a punta

Necesitan Docker Desktop abierto y el JDK 25. La primera vez, instala los navegadores de Playwright con `npx playwright install chromium firefox webkit`; los controladores de Chrome y Edge para Selenium los baja Selenium Manager.

Cada corrida enciende su propio entorno (`e2e/entorno/entorno.js`) y lo borra al terminar:

- Una base PostgreSQL con `infra/docker-compose.yml`, pero en otro proyecto (`inspectia-e2e`), en un puerto libre y con su propio archivo de variables. No usa `infra/.env` ni la base de desarrollo.
- El backend compilado en una copia en espejo dentro de `e2e/.entorno/backend`, con los scripts de `infra/bd` al lado para que también pasen allí sus pruebas: nunca compila en `backend/target`, que puede estar usando el backend de desarrollo. Guarda las fotos en una carpeta temporal del sistema (`inspectia-e2e-fotos`), nunca en la de tu PC, y la borra al apagarse.
- El administrador de la plataforma, creado con el script de `infra/bd`, y los datos de prueba, creados por la API: Cacaos del Huila con su administradora y un usuario final, Carlos Ramírez, y Cacao del Sur, desactivada.
- La página compilada, servida con `vite preview`.

Las contraseñas y la clave de los tokens son nuevas en cada corrida. Quedan en `e2e/.entorno/datos.json`, que está en `.gitignore` y se borra al apagar el entorno; los registros del backend y de la página quedan en `e2e/.entorno/`.

| Variable | Para qué |
|---|---|
| `INSPECTIA_E2E_URL` | La dirección de un entorno encendido con `npm run e2e:entorno`: las pruebas lo usan en vez de encender otro. |
| `INSPECTIA_E2E_JAR` | Un jar del backend ya compilado: el entorno lo usa en vez de compilar su copia. |
| `INSPECTIA_E2E_SIN_FIREFOX` | Con `1`, Playwright salta Firefox. En Windows 11 25H2, el Firefox de Playwright 1.64.1 no abre: Windows rechaza cargar su `mozglue.dll`. En CI corre siempre. |
| `INSPECTIA_E2E_SIN_EDGE` | Con `1`, Selenium salta Edge. La usa la CI: en Ubuntu 24.04, AppArmor no deja que Edge cree su sandbox y se cierra al abrir. Edge se prueba en Windows. |
| `INSPECTIA_E2E_VER` | Con cualquier valor, Selenium abre los navegadores a la vista en vez de ocultos. |

En PowerShell, por ejemplo: `$env:INSPECTIA_E2E_SIN_FIREFOX = '1'; npm run e2e:playwright`.

Las pruebas que solo miran, como las generadas de los permisos de una pantalla, entran con `personaDelRol`, que da la persona fija del entorno para cada rol. Las que cambian datos crean los suyos por la API, así no chocan entre navegadores: usuarios con `crearUsuario` y empresas con `crearEmpresa`, con un NIT válido nuevo. Las de la pantalla de usuarios registran cada una su propia empresa y entran con su administrador, para no llenar los 25 activos de Cacaos del Huila; las de rangos, también, porque guardar cambia los rangos de la empresa. Las de los lotes crean cada una su empresa con su usuario final (`crearEmpresaConUsuarioFinal`) y registran sus lotes por la API (`registrarLotes`): los navegadores corren a la vez sobre la misma base, y así ninguna prueba ve los lotes de otra. Las de las fotos hacen sus imágenes en la prueba (`e2e/imagenes.js`: un PNG de un color, de un tamaño exacto si hace falta, un PDF y un JPEG dibujado en el navegador), nunca con fotos del dataset (RNF-08); suben fotos por la API con `subirFotosPorLaApi` y en la pantalla con `elegirFoto`, que pulsa el botón de la casilla y elige el archivo en el selector que se abre. Las del resultado analizan un lote por la API con `loteAnalizado`, que recibe el ancho de cada foto: la prueba lo elige para que el clasificador falso dé el resultado que necesita (ver `infra/README.md`). La segunda muestra se analiza por la API con `segundaMuestraPorLaApi`, o el lote entero con `loteConDosMuestras`. Como el clasificador falso del entorno responde sin demora, `retenerLaEspera` sostiene la espera de la muestra 2 con `page.route` mientras la prueba la mira; el envío sí llega al backend, y al soltarla el final es real. Las de los filtros crean y desactivan usuarios con `crearUsuarioFinalEn` y `desactivarUsuarioEn`, y filtran con `filtrarElHistorial`. En Selenium, `analizarPorLaApi` sube, confirma y envía las fotos de un lote. En Selenium, el selector de una foto está oculto: `archivoDeLaFoto` lo encuentra sin esperar que se vea, y `sendKeys` recibe la ruta de un archivo escrito en la carpeta temporal. En Selenium, `escribirEn` reemplaza el texto de un campo con el teclado: `clear()` cambia el valor sin el evento que escucha React. Como la lista va de 20 en 20, `botonDeLaFila` pasa de página hasta encontrar la fila; en cada página espera que cambien las filas, porque el rango cambia antes de que lleguen.

Si una prueba de Selenium falla, su captura (`.png`) y la consola del navegador (`.consola.log`, que empieza con la prueba, el error, la dirección y la hora) quedan en `reports/selenium/`. No van en `test-results/` porque Playwright vacía esa carpeta en cada corrida.

Playwright corre cada prueba en Chrome, Firefox y Safari en tamaño de PC (1280 × 800) y en Chrome (Pixel 7) y Safari (iPhone 13) de celular (RNF-05 C3). axe revisa las pautas WCAG 2.2 AA en cada pantalla, en los temas claro y oscuro (RNF-06 C4).

## Demostración de otra temática

Para la sustentación: muestra que, cambiando solo los JSON, el generador saca otro CRUD completo (RP-23). El paso a paso, con qué produce cada campo del JSON, está en `docs/demostracion-otra-tematica.md`. Desde `frontend/`, con Docker Desktop abierto y el entorno del generador creado (ver `generador/README.md`):

```bash
npm run demo:otra-tematica -- --entidad ../generador/pruebas/ejemplos/cafe.json
```

1. Lee sus opciones y el JSON de la entidad. Una ruta relativa se lee desde la carpeta donde se corrió el comando. Sin `--entidad`, usa el producto de la pauta del docente (`generador/pruebas/ejemplos/producto.json`). Se detiene y dice qué pasó con:
   - una opción que no entiende, como `--con-prueba`, o `--entidad` sin ruta;
   - las opciones sin `--` antes, porque npm se queda con ellas;
   - un archivo que no existe o que no es un JSON;
   - un JSON que se llama como una entidad de InspectIA, que la reemplazaría en la copia.
2. Copia en espejo el backend, el frontend, `specs/` e `infra/bd` en `frontend/.demostracion`, que está en `.gitignore`. El `node_modules` del frontend no se copia: se enlaza; en Windows, con un junction, que no pide permisos de administrador.
3. Agrega el JSON a las entidades de la copia y suma la entidad a la API y a las pantallas del alcance. Es lo único que cambia.
4. Corre el generador en la copia, que revisa el JSON, y lista por capa lo que escribió: la tabla en V1, la API con su prueba de integración, los textos y la pantalla con sus pruebas.
5. Enciende la copia como el entorno aislado de las pruebas, en su propio proyecto de Docker Compose (`inspectia-demo`), con el backend y la página compilados en la copia.
6. Escribe la dirección; con quién entrar, con una contraseña solo de esa corrida; dónde está la sección en el menú; y cómo ver la API en Swagger UI y la tabla en psql.

Los textos de la consola y las pruebas de `--con-pruebas` salen del JSON: el nombre de la sección (`etiquetas`), la ruta de la API (`/api/v1/<ruta>`), las pruebas de Vitest (`src/generated/<modulo>`) y la de integración (`<clase>ApiGeneradaIT`). Se entra con la persona del entorno que tiene el primer rol de `crear` o, si no hay, el de `listar`: con café, Ana Pérez (administrador de empresa de Cacaos del Huila); con producto, el administrador de la plataforma.

La sección queda al final del menú de ese rol, con el ícono genérico, porque el menú no conoce el de los ejemplos. Ctrl+C apaga la copia y borra su base; después, `docker compose ls` ya no muestra `inspectia-demo`. El repositorio no cambia: `git status` queda limpio y `generar.py comprobar` sigue pasando.

Con `-- --con-pruebas`, antes de encender la copia corre allí las pruebas generadas de la entidad, con PostgreSQL en Testcontainers.

La copia queda, y la vez siguiente se pone al día con el repositorio. El backend se compila entero en cada corrida: el espejo borra lo que se generó en la anterior y, con eso, su `target`, así no quedan clases de otra entidad.

La copia va dentro de `frontend/` y no en la raíz del repositorio: el backend de la copia corre desde `.demostracion` y busca `../infra/.env`, que en la raíz sería el de desarrollo, con sus contraseñas.

## Producción

La página se publica en S3 y se sirve con CloudFront (D-16), en el mismo origen que la API, así que el backend no necesita CORS (D-31). CloudFront agrega la cabecera Content-Security-Policy con `script-src 'self'` y `style-src 'self' 'unsafe-inline'`: Material UI inyecta sus estilos en la página. Es un riesgo aceptado en D-31.

## Convenciones

- Los colores salen de `src/tema/tokens.js` por medio del tema; ESLint rechaza un color escrito a mano en otro archivo (RP-08).
- Cada archivo lleva su header (RP-10) y lo exportado, su JSDoc (RP-09).
- Cada prueba dice qué cubre en su título, como `RF-01 · C2 · …`: de ahí sale `docs/casos-de-prueba.md` (RP-13).
- Los textos de la página no van escritos en los componentes: van en `src/i18n/` o salen del backend (RP-11).
