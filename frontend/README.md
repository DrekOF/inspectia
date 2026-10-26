<!--
Nombre: README.md (frontend)
Descripción: La página web de InspectIA: qué hay en frontend/, cómo arrancarla junto con el backend, sus textos,
             sus pruebas y el entorno aislado de las pruebas de punta a punta.
Fecha de creación: 12/10/2026
Actualización: 20/10/2026
Autor: Equipo InspectIA
Versión: 1.1
-->

# frontend

Página web de InspectIA, para PC y celular (RNF-05). Hecha con React 19, Material UI 9 y Vite 8, con Node 24.22.0 (la de `.node-version`). Las versiones de cada librería están en `package.json` y en la tabla de versiones del README principal.

Esta versión tiene el marco (barra superior, menú según el rol, idioma y tema), Iniciar sesión (RF-01), Cambiar contraseña (RF-18) y Acerca de, con la cita del dataset (RNF-08). Las demás secciones del menú dicen «Esta sección estará disponible pronto.» hasta que llegue su función. La referencia visual es el prototipo de `02_Analisis_Diseno/Prototipo_Frontend`, fuera del repositorio.

## Qué hay

| Carpeta | Qué va ahí |
|---|---|
| `src/paginas/` | Las pantallas: Iniciar sesión, el inicio de cada rol, Cambiar contraseña, Acerca de, las secciones pendientes y las direcciones que no existen. |
| `src/navegacion/` | El marco de las pantallas con sesión (barra, menú y «Saltar al contenido»), las rutas y el menú de cada rol (`menu.js`). |
| `src/sesion/` | La sesión (D-28, D-31): se guarda en el navegador, vence a las 8 horas, se cierra con un 401 y vuelve a la pantalla anterior si entra el mismo usuario. |
| `src/api/` | El cliente de la API: el token, el idioma y los errores `application/problem+json` del backend. |
| `src/componentes/` | Piezas comunes: campos, avisos, tarjetas, títulos, la marca y los selectores de idioma y tema. |
| `src/validacion/` | Los esquemas de Yup (RP-07), con los mismos límites y mensajes del backend. |
| `src/i18n/` | Los idiomas (RP-11): español por defecto e inglés. |
| `src/tema/` | Los tokens de color, letra y medidas (`tokens.js`) y el tema de Material UI, claro y oscuro (RP-08). |
| `herramientas/` | La lectura de los `.properties` del backend (plugin de Vite) y la copia en espejo que usa el entorno de punta a punta. |
| `e2e/` | Las pruebas de punta a punta con Playwright y su entorno aislado (`e2e/entorno/`). |
| `selenium/` | Las pruebas con Selenium, en Chrome y en Edge. |

Las pantallas CRUD de empresas, usuarios y rangos saldrán de `generador/` y no se editarán a mano (RP-23).

## Textos

Las etiquetas y los mensajes que la página comparte con el backend no se copian: salen de `backend/src/generated/resources/i18n/generado*.properties` y `backend/src/main/resources/i18n/mensajes*.properties`. `herramientas/textos-del-backend.js` los entrega como el módulo `virtual:textos-del-backend`, y en desarrollo la página se recarga si cambian. Los textos propios del frontend van en `src/i18n/es.js` y `src/i18n/en.js`, con las mismas claves. Los marcadores van entre llaves, como en el backend: `{0}`, `{max}`.

## Comandos

Desde `frontend/`. La primera vez, `npm ci`.

| Comando | Qué hace |
|---|---|
| `npm run dev` | Arranca la página en http://localhost:5173. Vite pasa las peticiones de `/api` al backend de http://localhost:8080; si corre en otro puerto, define `INSPECTIA_API_URL`. |
| `npm run build` | Compila la página en `dist/`. |
| `npm test` | Pruebas unitarias y de componentes (Vitest), con la API simulada (MSW): no necesitan el backend. |
| `npm run test:cobertura` | Las mismas, con la cobertura. |
| `npm run test:mutacion` | Pruebas de mutación con Stryker (RP-12). El informe queda en `reports/mutacion/index.html`. |
| `npm run lint` | ESLint: JSDoc en lo exportado (RP-09), las reglas de los hooks de React y los colores solo en `src/tema/tokens.js` (RP-08). |
| `npm run e2e:playwright` | Pruebas de punta a punta con Playwright (RP-14), en su entorno aislado. |
| `npm run e2e:selenium` | Pruebas con Selenium en Chrome y Edge, en su entorno aislado. |
| `npm run e2e:entorno` | Enciende el entorno aislado y lo deja encendido hasta Ctrl+C. |

## Pruebas de punta a punta

Necesitan Docker Desktop abierto y el JDK 25. La primera vez, instala los navegadores de Playwright con `npx playwright install chromium firefox webkit`; los controladores de Chrome y Edge para Selenium los baja Selenium Manager.

Cada corrida enciende su propio entorno (`e2e/entorno/entorno.js`) y lo borra al terminar:

- Una base PostgreSQL con `infra/docker-compose.yml`, pero en otro proyecto (`inspectia-e2e`), en un puerto libre y con su propio archivo de variables. No usa `infra/.env` ni la base de desarrollo.
- El backend compilado en una copia en espejo dentro de `e2e/.entorno/backend`: nunca compila en `backend/target`, que puede estar usando el backend de desarrollo.
- El administrador de la plataforma, creado con el script de `infra/bd`, y los datos de prueba, creados por la API: Cacaos del Huila con su administradora y Cacao del Sur, desactivada.
- La página compilada, servida con `vite preview`.

Las contraseñas y la clave de los tokens son nuevas en cada corrida. Quedan en `e2e/.entorno/datos.json`, que está en `.gitignore` y se borra al apagar el entorno; los registros del backend y de la página quedan en `e2e/.entorno/`.

| Variable | Para qué |
|---|---|
| `INSPECTIA_E2E_URL` | La dirección de un entorno encendido con `npm run e2e:entorno`: las pruebas lo usan en vez de encender otro. |
| `INSPECTIA_E2E_JAR` | Un jar del backend ya compilado: el entorno lo usa en vez de compilar su copia. |
| `INSPECTIA_E2E_SIN_FIREFOX` | Con `1`, Playwright salta Firefox. En Windows 11 25H2, el Firefox de Playwright 1.64.1 no abre: Windows rechaza cargar su `mozglue.dll`. En CI corre siempre. |
| `INSPECTIA_E2E_VER` | Con cualquier valor, Selenium abre los navegadores a la vista en vez de ocultos. |

En PowerShell, por ejemplo: `$env:INSPECTIA_E2E_SIN_FIREFOX = '1'; npm run e2e:playwright`.

Playwright corre cada prueba en Chrome, Firefox y Safari en tamaño de PC (1280 × 800) y en Chrome (Pixel 7) y Safari (iPhone 13) de celular (RNF-05 C3). axe revisa las pautas WCAG 2.2 AA en cada pantalla, en los temas claro y oscuro (RNF-06 C4).

## Producción

La página se publica en S3 y se sirve con CloudFront (D-16), en el mismo origen que la API, así que el backend no necesita CORS (D-31). CloudFront agrega la cabecera Content-Security-Policy con `script-src 'self'` y `style-src 'self' 'unsafe-inline'`: Material UI inyecta sus estilos en la página. Es un riesgo aceptado en D-31.

## Convenciones

- Los colores salen de `src/tema/tokens.js` por medio del tema; ESLint rechaza un color escrito a mano en otro archivo (RP-08).
- Cada archivo lleva su header (RP-10) y lo exportado, su JSDoc (RP-09).
- Cada prueba dice qué cubre en su título, como `RF-01 · C2 · …`: de ahí sale `docs/casos-de-prueba.md` (RP-13).
- Los textos de la página no van escritos en los componentes: van en `src/i18n/` o salen del backend (RP-11).
