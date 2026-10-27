# Nombre: test_frontend.py
# Descripción: Pruebas de la generación del frontend: la pantalla de cada entidad de «pantallas» en
#              alcance.json, con su cliente de la API, sus esquemas de Yup, sus textos y sus pruebas.
# Fecha de creación: 26/10/2026
# Actualización: 27/10/2026
# Autor: Equipo InspectIA
# Versión: 1.1
"""Pruebas de lo que genera el generador para el frontend (RP-23).

De cada entidad de ``pantallas`` en ``alcance.json`` sale su pantalla (lista, formulario y acciones), con su
cliente de la API, sus esquemas de Yup y sus pruebas. Las validaciones de Yup salen del mismo modelo que las de
Bean Validation, con las mismas claves de mensaje, y su prueba generada revisa los mismos casos de límite que
la prueba generada de la API.
"""

import re

import pytest

from conftest import GENERADOR, cadena_yup, casos_java, compacto
from inspectia_generador.carga import cargar_alcance, revisar_alcance
from inspectia_generador.casos import pruebas_javascript
from inspectia_generador.generacion import generar_archivos
from inspectia_generador.sql import ErrorGeneracion

FRONTEND = 'frontend/src/generated'
E2E = 'frontend/e2e/generated'
PRUEBAS_JAVA = 'backend/src/test-generated/java/co/edu/usco/inspectia'
#: Lo que genera cada pantalla en frontend/src/generated/<modulo>/.
ARCHIVOS_DE_EMPRESA = ('empresaApi.js', 'empresaApi.test.js', 'empresaEsquemas.js', 'empresaEsquemas.test.js',
                       'EmpresaPantalla.jsx', 'EmpresaPantalla.test.jsx')
ARCHIVOS_DE_USUARIO = ('usuarioApi.js', 'usuarioApi.test.js', 'usuarioEsquemas.js', 'usuarioEsquemas.test.js',
                       'UsuarioPantalla.jsx', 'UsuarioPantalla.test.jsx')
#: La API y las pantallas con la de usuario (RF-03).
CON_USUARIO = {'api': ('empresa', 'usuario'), 'pantallas': ('empresa', 'usuario')}


def generar(entidades, api=('empresa',), pantallas=('empresa',)):
    """Lo que genera el generador, por ruta, con la API y las pantallas que se pidan."""
    return {a.ruta: a.contenido for a in generar_archivos(entidades, api, pantallas)}


def texto_js(literal):
    """El valor de un texto de JavaScript entre comillas sencillas."""
    return re.sub(r'\\(.)', r'\1', literal[1:-1])


def casos_js(prueba, constante):
    """Los casos de una lista de una prueba de JavaScript generada, en orden.

    Lee ``const CONSTANTE = [`` y sus líneas ``['campo', valor, 'mensaje'],``.

    Returns:
        list: ``(campo, valor, mensaje)``; el valor, como se escribe en JavaScript, y el mensaje, ``None`` si el
        caso no lo lleva.
    """
    inicio = prueba.index(f'const {constante} = [')
    cuerpo = prueba[inicio:prueba.index('\n];', inicio)]
    casos = []
    for linea in re.findall(r'^ {2}\[(.*)\],$', cuerpo, re.M):
        con_mensaje = re.fullmatch(r"'([^']+)', (.+?), ('(?:[^'\\]|\\.)*')", linea)
        if con_mensaje:
            casos.append((con_mensaje[1], con_mensaje[2], texto_js(con_mensaje[3])))
        else:
            campo, valor = re.fullmatch(r"'([^']+)', (.+)", linea).groups()
            casos.append((campo, valor, None))
    return casos


def como_en_javascript(casos):
    """Los casos de la prueba de Java como los escribe la de JavaScript: los textos van entre comillas sencillas, y
    el dato que falta, que en la API es null, es el campo vacío del formulario."""
    return [(campo, "''" if valor == 'null' else valor.replace('"', "'"), mensaje) for campo, valor, mensaje in casos]


@pytest.mark.caso('RP-23 · cada entidad de pantallas en alcance.json tiene su pantalla, su cliente de la API, sus esquemas de Yup y sus pruebas; las demás, no')
def test_archivos_de_las_pantallas_del_alcance(entidades):
    archivos = generar(entidades, api=('empresa', 'usuario'))

    for nombre in ARCHIVOS_DE_EMPRESA:
        assert f'{FRONTEND}/empresa/{nombre}' in archivos
    assert f'{E2E}/empresa.spec.js' in archivos
    assert not any(ruta.startswith(f'{FRONTEND}/usuario/') for ruta in archivos), 'usuario tiene API, pero no pantalla'
    sin_pantallas = [ruta for ruta in generar(entidades, pantallas=()) if ruta.startswith('frontend/')]
    assert sin_pantallas == [f'{FRONTEND}/pantallas.js', f'{FRONTEND}/textos.js'], \
        'sin pantallas, solo las listas vacías que lee la app'


@pytest.mark.caso('RP-23 · una pantalla necesita la API de su entidad: el alcance no acepta una pantalla sin API ni de una entidad que no existe')
def test_pantalla_necesita_su_api(entidades):
    errores = revisar_alcance(('empresa',), entidades, pantallas=('usuario', 'no_existe'))

    assert 'alcance.json: la pantalla de usuario necesita su API: agrega usuario a api' in errores
    assert 'alcance.json: la entidad no_existe no existe' in errores


@pytest.mark.caso('RP-23 · alcance.json dice qué entidades tienen pantalla: empresa (RF-02), usuario (RF-03) y solo entidades con API')
def test_alcance_de_las_pantallas():
    from inspectia_generador.carga import cargar_pantallas

    pantallas = cargar_pantallas(GENERADOR / 'alcance.json')

    assert 'empresa' in pantallas
    assert 'usuario' in pantallas
    assert set(pantallas) <= set(cargar_alcance(GENERADOR / 'alcance.json'))


@pytest.mark.caso('RNF-04 · la ruta, la entrada del menú y los roles de cada pantalla salen del JSON: la abren los roles de listar')
def test_ruta_menu_y_roles(entidades):
    pantallas = generar(entidades)[f'{FRONTEND}/pantallas.js']

    for propiedad in ("entidad: 'empresa'", "ruta: '/empresas'", "etiqueta: 'empresa.etiqueta.plural'",
                      "icono: 'empresas'", "roles: ['ADMIN_PLATAFORMA']",
                      "Pantalla: lazy(() => import('./empresa/EmpresaPantalla.jsx'))"):
        assert propiedad in pantallas

    entidades['empresa']['operaciones']['listar'] = ['ADMIN_PLATAFORMA', 'ADMIN_EMPRESA']

    assert "roles: ['ADMIN_PLATAFORMA', 'ADMIN_EMPRESA']" in generar(entidades)[f'{FRONTEND}/pantallas.js']


@pytest.mark.caso('RP-23 · el cliente de la API de una pantalla usa la ruta y las operaciones del JSON, y el listado filtra por activo (D-33)')
def test_cliente_de_la_api(entidades):
    api = generar(entidades)[f'{FRONTEND}/empresa/empresaApi.js']

    assert "const RUTA = '/empresas';" in api
    for operacion in ('listar', 'ver', 'crear', 'editar', 'desactivar', 'reactivar'):
        assert f'export function {operacion}(pedir' in api
    assert "parametros.set('activo', String(activo));" in api
    assert "return pedir('POST', `${RUTA}/${encodeURIComponent(id)}/desactivar`);" in api
    assert "return pedir('PUT', `${RUTA}/${encodeURIComponent(id)}`, datos);" in api


@pytest.mark.caso('RP-23 · las validaciones del JSON pasan a Yup con la misma clave de mensaje, la propia del atributo o la general, y en el orden en que la API escoge el error de un campo: primero el dato que falta y después por el nombre de la restricción')
def test_validaciones_a_yup(entidades):
    esquemas = generar(entidades)[f'{FRONTEND}/empresa/empresaEsquemas.js']
    obligatorio = ".test('obligatorio', t('validacion.obligatorio'), lleno)"

    assert cadena_yup(esquemas, 'esquemaParaCrear', 'nombre') == [
        '.transform(recortar)', obligatorio, ".max(120, t('validacion.longitud_max', { max: 120 }))"]
    assert cadena_yup(esquemas, 'esquemaParaCrear', 'nit') == [
        '.transform(recortar)', obligatorio, ".matches(/^[0-9]{6,10}-[0-9]$/, t('empresa.nit.patron'))",
        ".max(15, t('validacion.longitud_max', { max: 15 }))"], 'Pattern va antes que Size'
    assert cadena_yup(esquemas, 'esquemaParaCrear', 'administrador.correo') == [
        '.transform(recortar)', '.transform(minusculas)', obligatorio,
        ".max(254, t('validacion.longitud_max', { max: 254 }))"]
    assert cadena_yup(esquemas, 'esquemaParaCrear', 'administrador.contrasena') == [
        obligatorio, ".test('bytes_max', t('usuario.contrasena_hash.bytes_max'), cabeEnBytes(72))",
        ".max(64, t('usuario.contrasena_hash.longitud_max'))",
        ".min(8, t('usuario.contrasena_hash.longitud_min'))"], 'MaximoBytes va antes que Size'
    assert cadena_yup(esquemas, 'esquemaParaEditar', 'nit') == cadena_yup(esquemas, 'esquemaParaCrear', 'nit')
    editar = esquemas[esquemas.index('export function esquemaParaEditar(t) {'):]
    assert 'administrador' not in editar, 'el primer administrador solo va al registrar la empresa'


@pytest.mark.caso('RP-23 · el formato del correo se valida solo en el backend, como el dígito del NIT: Yup no lo revisa y su error llega al campo al guardar')
def test_correo_solo_en_el_backend(entidades):
    archivos = generar(entidades)

    esquemas = archivos[f'{FRONTEND}/empresa/empresaEsquemas.js']
    assert 'email(' not in esquemas and 'validacion.correo' not in esquemas
    assert 'sin-arroba' not in archivos[f'{FRONTEND}/empresa/empresaEsquemas.test.js']
    assert ('Arguments.of("administrador.correo", "sin-arroba", "Escribe un correo válido.")'
            in archivos[f'{PRUEBAS_JAVA}/empresa/EmpresaApiGeneradaIT.java'])
    pantalla = compacto(archivos[f'{FRONTEND}/empresa/EmpresaPantalla.test.jsx'])
    assert "{ campo: 'administrador.correo', mensaje: 'Escribe un correo válido.' }" in pantalla, \
        'la prueba de la pantalla revisa que el error del backend quede en el campo'


@pytest.mark.caso('RP-23 · la prueba generada de Yup revisa los mismos casos de límite que la prueba generada de la API, con el mismo mensaje en español, salvo el formato del correo')
def test_mismos_casos_de_limite_que_la_api(entidades):
    archivos = generar(entidades)
    prueba_api = archivos[f'{PRUEBAS_JAVA}/empresa/EmpresaApiGeneradaIT.java']
    prueba_yup = archivos[f'{FRONTEND}/empresa/empresaEsquemas.test.js']

    for metodo, constante in (('validacionesAlCrear', 'CASOS_AL_CREAR'), ('validacionesAlEditar', 'CASOS_AL_EDITAR'),
                              ('bordesAlCrear', 'BORDES_AL_CREAR'), ('bordesAlEditar', 'BORDES_AL_EDITAR')):
        de_la_api = [c for c in como_en_javascript(casos_java(prueba_api, metodo)) if c[1] != "'sin-arroba'"]
        assert casos_js(prueba_yup, constante) == de_la_api, constante
    assert len(casos_js(prueba_yup, 'CASOS_AL_CREAR')) == 19
    assert ('administrador.contrasena', "'ñ'.repeat(36)", None) in casos_js(prueba_yup, 'BORDES_AL_CREAR')


@pytest.mark.caso('RP-23 · un valor que falla por dos validaciones da en Yup el mismo error que en la API: el NIT largo y mal escrito, el del patrón, y la contraseña con demasiadas eñes, el de los bytes')
def test_un_error_por_campo_en_yup(entidades):
    prueba = generar(entidades)[f'{FRONTEND}/empresa/empresaEsquemas.test.js']
    patron = 'Escribe el NIT sin puntos y con el dígito de verificación, por ejemplo 900123456-8.'

    for constante in ('CASOS_AL_CREAR', 'CASOS_AL_EDITAR'):
        assert ('nit', "'x'.repeat(16)", patron) in casos_js(prueba, constante), constante
    assert ('administrador.contrasena', "'ñ'.repeat(65)",
            'La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes') in casos_js(prueba, 'CASOS_AL_CREAR')


@pytest.mark.caso('RP-23 · la prueba generada de Yup rechaza el ejemplo de un patrón con algo pegado antes o después, como la API: en JavaScript, un patrón sin ^ ni $ aceptaría x900123456-8')
def test_patron_anclado_en_yup(entidades):
    prueba = generar(entidades)[f'{FRONTEND}/empresa/empresaEsquemas.test.js']
    mensaje = 'Escribe el NIT sin puntos y con el dígito de verificación, por ejemplo 900123456-8.'

    for constante in ('CASOS_AL_CREAR', 'CASOS_AL_EDITAR'):
        casos = casos_js(prueba, constante)
        assert ('nit', "'x900123456-8'", mensaje) in casos, constante
        assert ('nit', "'900123456-8x'", mensaje) in casos, constante


@pytest.mark.caso('RP-11 · los textos de cada pantalla salen de pantalla.textos del JSON, en español y en inglés')
def test_textos_de_la_pantalla(entidades):
    archivos = generar(entidades)
    textos = archivos[f'{FRONTEND}/textos.js']
    espanol, ingles = textos[textos.index('  es: {'):textos.index('  en: {')], textos[textos.index('  en: {'):]

    assert "'empresa.pantalla.nuevo': 'Nueva empresa'," in espanol
    assert "'empresa.pantalla.nuevo': 'New company'," in ingles
    assert "'empresa.pantalla.resumen_total.singular': '{cantidad} empresa registrada'," in espanol
    assert "'empresa.pantalla.resumen_total.plural': '{cantidad} registered companies'," in ingles
    assert ("'empresa.pantalla.confirmar_desactivar_detalle': 'Its users won\\'t be able to sign in until you "
            "reactivate it. No data is deleted.',") in ingles
    propiedades = archivos['backend/src/generated/resources/i18n/generado.properties']
    assert 'usuario.correo.ayuda=Con este correo inicia sesión. No puede repetirse en la plataforma.' in propiedades


@pytest.mark.caso('RP-23 · la pantalla toma del JSON sus campos, sus columnas, los roles de cada acción y sus textos')
def test_pantalla_desde_el_json(entidades):
    pantalla = compacto(generar(entidades)[f'{FRONTEND}/empresa/EmpresaPantalla.jsx'])

    assert ("const CAMPOS_PARA_CREAR = ['nombre', 'nit', 'ciudad', 'administrador.nombre', 'administrador.correo', "
            "'administrador.contrasena'];") in pantalla
    assert "const CAMPOS_PARA_EDITAR = ['nombre', 'nit', 'ciudad'];" in pantalla
    assert ("{ atributo: 'nombre', etiqueta: 'empresa.pantalla.columna_nombre', "
            "debajo: { atributo: 'nit', texto: 'empresa.pantalla.debajo_del_nombre' } }") in pantalla
    assert "{ atributo: 'ciudad', etiqueta: 'empresa.ciudad.etiqueta' }" in pantalla
    assert pantalla.count("atributo: 'nit'") == 1, 'el NIT va debajo del nombre, no en su propia columna'
    assert "atributoActivo: 'activa'" in pantalla
    assert ("operaciones: { crear: ['ADMIN_PLATAFORMA'], editar: ['ADMIN_PLATAFORMA'], "
            "desactivar: ['ADMIN_PLATAFORMA'], reactivar: ['ADMIN_PLATAFORMA'] }") in pantalla
    assert ("<CampoDeTexto nombre=\"nit\" etiqueta={t('empresa.nit.etiqueta')} "
            "ayuda={t('empresa.nit.ayuda')}") in pantalla
    assert "<GrupoDeCampos titulo={t('empresa.administrador.etiqueta')}>" in pantalla, 'el grupo de crear_con'
    assert ("<CampoDeTexto nombre=\"administrador.correo\" etiqueta={t('usuario.correo.etiqueta')} "
            "ayuda={t('usuario.correo.ayuda')}") in pantalla
    assert ("<CampoContrasena nombre=\"administrador.contrasena\" etiqueta={t('empresa.pantalla.contrasena_al_crear')} "
            "ayuda={t('empresa.pantalla.ayuda_contrasena_al_crear')}") in pantalla
    assert "tituloLista: 'empresa.pantalla.titulo_lista'" in pantalla
    assert 'descripcion: null' in pantalla, 'un texto opcional que el JSON no trae'


@pytest.mark.caso('RP-23 · desactivar pide confirmación solo si pantalla.textos trae su texto (D-34): la empresa lo trae')
def test_confirmar_desactivar(entidades):
    pantalla = compacto(generar(entidades)[f'{FRONTEND}/empresa/EmpresaPantalla.jsx'])

    assert ("confirmarDesactivar: { titulo: 'empresa.pantalla.confirmar_desactivar', "
            "detalle: 'empresa.pantalla.confirmar_desactivar_detalle' }") in pantalla

    textos = entidades['empresa']['pantalla']['textos']
    del textos['confirmar_desactivar'], textos['confirmar_desactivar_detalle']

    assert 'confirmarDesactivar: null' in compacto(generar(entidades)[f'{FRONTEND}/empresa/EmpresaPantalla.jsx'])


@pytest.mark.caso('RP-23 · los mensajes de resultado salen de pantalla.textos con el valor de la fila, como «Se guardó {nombre}.», y la prueba generada los toma del JSON')
def test_mensajes_de_resultado(entidades):
    archivos = generar(entidades)

    pantalla = compacto(archivos[f'{FRONTEND}/empresa/EmpresaPantalla.jsx'])
    for js, clave in (('alGuardar', 'al_guardar'), ('alDesactivar', 'al_desactivar'), ('alReactivar', 'al_reactivar')):
        assert f"{js}: 'empresa.pantalla.{clave}'" in pantalla
    espanol = archivos[f'{FRONTEND}/textos.js'].split('  en: {')[0]
    assert "'empresa.pantalla.al_desactivar': 'Se desactivó {nombre}.'," in espanol
    assert "screen.findByText('Se desactivó Cacaos del Huila.')" in archivos[f'{FRONTEND}/empresa/EmpresaPantalla.test.jsx']

    # Como lo necesitará usuario: «Se desactivó Ana Pérez.» se leería como si ella misma se hubiera desactivado.
    entidades['empresa']['pantalla']['textos']['al_desactivar'] = {'es': 'Se desactivó a {nombre}.',
                                                                    'en': '{nombre} was deactivated.'}

    prueba = generar(entidades)[f'{FRONTEND}/empresa/EmpresaPantalla.test.jsx']
    assert "screen.findByText('Se desactivó a Cacaos del Huila.')" in prueba


@pytest.mark.caso('RP-23 · el generador se detiene con una pantalla que todavía no sabe generar o a la que le faltan textos, y dice qué falta')
def test_pantalla_que_no_sabe_generar(entidades):
    with pytest.raises(ErrorGeneracion) as error:
        generar(entidades, api=('empresa', 'usuario', 'rango_recomendacion'), pantallas=('rango_recomendacion',))

    assert 'No se puede generar la pantalla de rango_recomendacion' in str(error.value)
    assert 'edicion_en_bloque' in str(error.value)

    textos = entidades['empresa']['pantalla']['textos']
    del textos['nuevo'], textos['activo'], textos['al_guardar']
    entidades['empresa']['atributos'].append({
        'nombre': 'empleados', 'tipo': 'entero', 'obligatorio': False, 'en_lista': False, 'en_formulario': True,
        'etiquetas': {'es': 'Empleados', 'en': 'Employees'}, 'fichas': ['RF-02']})

    with pytest.raises(ErrorGeneracion) as error:
        generar(entidades)

    for falta in ('pantalla.textos.nuevo', 'pantalla.textos.activo', 'pantalla.textos.al_guardar',
                  'empleados, de tipo entero, en el formulario'):
        assert falta in str(error.value)


@pytest.mark.caso('RP-13 · las pruebas generadas del frontend llevan un título fijo con su marca: la ficha de la entidad o el RNF que cubren')
def test_marcas_de_las_pruebas_generadas(entidades):
    archivos = generar(entidades)

    for ruta in [f'{FRONTEND}/empresa/{nombre}' for nombre in ARCHIVOS_DE_EMPRESA if '.test.' in nombre] + [
            f'{E2E}/empresa.spec.js']:
        pruebas, errores = pruebas_javascript(archivos[ruta], ruta)
        assert not errores, ruta
        assert pruebas, ruta
        assert all(p.generada for p in pruebas)
        marcas = ('RF-02 · ', 'RNF-04 · ', 'RNF-05 · C1 · ', 'RNF-06 · ')
        assert all(p.marca.startswith(marcas) for p in pruebas), ruta
    con_usuario = generar(entidades, **CON_USUARIO)
    for ruta in [f'{FRONTEND}/usuario/{nombre}' for nombre in ARCHIVOS_DE_USUARIO if '.test.' in nombre] + [
            f'{E2E}/usuario.spec.js']:
        pruebas, errores = pruebas_javascript(con_usuario[ruta], ruta)
        assert not errores and pruebas, ruta
        assert all(p.marca.startswith(('RF-03 · ', 'RNF-04 · ', 'RNF-05 · C1 · ', 'RNF-06 · ')) for p in pruebas), ruta
    marcas = [p.marca for p in pruebas_javascript(archivos[f'{E2E}/empresa.spec.js'], f'{E2E}/empresa.spec.js')[0]]
    assert any(m.startswith('RNF-06 · C4 · ') for m in marcas), 'axe en la pantalla generada'
    assert any(m.startswith('RNF-05 · C1 · ') for m in marcas), 'sin desplazarse a los lados en un celular'


@pytest.mark.caso('RP-23 · la pantalla de usuario sale de su JSON: el correo debajo del nombre, el rol con sus valores, el formulario al lado de la lista y desactivar sin confirmación (D-34)')
def test_pantalla_de_usuario(entidades):
    archivos = generar(entidades, **CON_USUARIO)

    for nombre in ARCHIVOS_DE_USUARIO:
        assert f'{FRONTEND}/usuario/{nombre}' in archivos
    assert f'{E2E}/usuario.spec.js' in archivos
    pantalla = compacto(archivos[f'{FRONTEND}/usuario/UsuarioPantalla.jsx'])
    assert ("{ atributo: 'nombre', etiqueta: 'usuario.nombre.etiqueta', "
            "debajo: { atributo: 'correo', texto: null } }") in pantalla
    assert "{ atributo: 'rol', etiqueta: 'usuario.rol.etiqueta', valores: 'usuario.rol' }" in pantalla
    assert "formulario: 'al_lado'" in pantalla
    assert 'confirmarDesactivar: null' in pantalla, 'desactivar un usuario no pide confirmación (D-34)'
    assert "formulario: 'encima'" in compacto(archivos[f'{FRONTEND}/empresa/EmpresaPantalla.jsx'])
    assert "icono: 'usuarios'" in archivos[f'{FRONTEND}/pantallas.js']


@pytest.mark.caso('RP-23 · con limite_de_activos, la pantalla recibe el máximo y el mensaje de su regla: el resumen cuenta los activos contra el máximo y solo pide los textos que usa su frase (RF-03 C5, D-34)')
def test_limite_de_activos(entidades):
    archivos = generar(entidades, **CON_USUARIO)

    pantalla = compacto(archivos[f'{FRONTEND}/usuario/UsuarioPantalla.jsx'])
    assert "limiteDeActivos: { maximo: 25, mensaje: 'usuario.reglas.maximo_usuarios_activos' }" in pantalla
    assert "resumen: { frase: 'usuario.pantalla.resumen', activos: 'usuario.pantalla.resumen_activos' }" in pantalla
    assert 'limiteDeActivos: null' in compacto(archivos[f'{FRONTEND}/empresa/EmpresaPantalla.jsx'])
    espanol = archivos[f'{FRONTEND}/textos.js'].split('  en: {')[0]
    assert "'usuario.pantalla.resumen': '{activos}, contándote a ti.'," in espanol
    assert "'usuario.pantalla.resumen_activos.plural': '{cantidad} de {maximo} usuarios activos'," in espanol
    assert "'1 de 25 usuarios activos, contándote a ti.'" in archivos[f'{FRONTEND}/usuario/UsuarioPantalla.test.jsx']


@pytest.mark.caso('RP-23 · una lista obligatoria vacía da en Yup el error de obligatorio, como la API, y no el de sus valores: Yup revisa oneOf antes que las demás pruebas, así que el vacío cuenta como ausente')
def test_lista_obligatoria_vacia(entidades):
    esquemas = generar(entidades, **CON_USUARIO)[f'{FRONTEND}/usuario/usuarioEsquemas.js']
    rol = ['.transform(vacioComoAusente)', ".test('obligatorio', t('validacion.obligatorio'), lleno)",
           ".oneOf(['USUARIO_FINAL', 'ADMIN_EMPRESA'], t('usuario.rol.valores_en_formulario'))"]

    assert cadena_yup(esquemas, 'esquemaParaCrear', 'rol') == rol
    assert cadena_yup(esquemas, 'esquemaParaEditar', 'rol') == rol


@pytest.mark.caso('RP-23 · la prueba generada de Yup rechaza cada valor de una lista que existe pero el formulario no ofrece, como ADMIN_PLATAFORMA en el rol, con el mismo mensaje del 403 de la API')
def test_valores_que_el_formulario_no_ofrece(entidades):
    archivos = generar(entidades, **CON_USUARIO)
    prueba_yup = archivos[f'{FRONTEND}/usuario/usuarioEsquemas.test.js']
    prueba_api = archivos[f'{PRUEBAS_JAVA}/usuario/UsuarioApiGeneradaIT.java']
    mensaje = 'Solo puedes asignar los roles Usuario final o Administrador de empresa.'

    for constante in ('CASOS_AL_CREAR', 'CASOS_AL_EDITAR'):
        assert ('rol', "'ADMIN_PLATAFORMA'", mensaje) in casos_js(prueba_yup, constante), constante
    assert f'"rol", "ADMIN_PLATAFORMA", "{mensaje}"' in prueba_api, 'el mismo caso y el mismo mensaje del 403 de la API'


@pytest.mark.caso('RP-23 · pantalla.inicial da el valor con que empieza un campo al registrar, sin tocar la base: el rol empieza en Usuario final')
def test_valor_inicial(entidades):
    archivos = generar(entidades, **CON_USUARIO)

    pantalla = compacto(archivos[f'{FRONTEND}/usuario/UsuarioPantalla.jsx'])
    assert "return { nombre: '', correo: '', contrasena: '', rol: 'USUARIO_FINAL' };" in pantalla
    assert '    rol varchar(30) NOT NULL,' in archivos['backend/src/generated/resources/db/migration/V1__esquema_inicial.sql']


@pytest.mark.caso('RP-23 · una contraseña opcional al editar va en el formulario: vacía no se envía y queda la que había; si llega, no puede estar en blanco y cumple las mismas validaciones, en el orden de la API')
def test_contrasena_opcional_al_editar(entidades):
    archivos = generar(entidades, **CON_USUARIO)

    pantalla = compacto(archivos[f'{FRONTEND}/usuario/UsuarioPantalla.jsx'])
    assert "const CAMPOS_PARA_EDITAR = ['nombre', 'correo', 'contrasena', 'rol'];" in pantalla
    assert "contrasena: valores.contrasena === '' ? null : valores.contrasena" in pantalla
    assert "t('usuario.pantalla.contrasena_al_editar')" in pantalla
    assert "t('usuario.pantalla.ayuda_contrasena_al_editar')" in pantalla
    assert "notaContrasenaDeOtro: 'usuario.pantalla.nota_contrasena_de_otro'" in pantalla
    esquemas = archivos[f'{FRONTEND}/usuario/usuarioEsquemas.js']
    assert cadena_yup(esquemas, 'esquemaParaEditar', 'contrasena') == [
        '.transform(vacioComoAusente)', ".test('obligatorio', t('validacion.obligatorio'), noEnBlanco)",
        ".test('bytes_max', t('usuario.contrasena_hash.bytes_max'), cabeEnBytes(72))",
        ".max(64, t('usuario.contrasena_hash.longitud_max'))", ".min(8, t('usuario.contrasena_hash.longitud_min'))"]
    assert cadena_yup(esquemas, 'esquemaParaCrear', 'contrasena')[0] == \
        ".test('obligatorio', t('validacion.obligatorio'), lleno)", 'al registrar es obligatoria'
    espanol = archivos[f'{FRONTEND}/textos.js'].split('  en: {')[0]
    assert ("'usuario.pantalla.nota_contrasena_de_otro': 'Al guardar, se cierran las sesiones abiertas de {nombre} y "
            "se quita su bloqueo por intentos fallidos, si lo tenía. Compártela con {nombre}: podrá cambiarla en "
            "«Cambiar contraseña».',") in espanol


@pytest.mark.caso('RP-23 · las reglas en_la_fila_propia se aplican en la pantalla antes que en la API: la fila de quien tiene la sesión lleva su marca y no tiene las acciones que impiden, y al editarla los campos que impiden no se cambian y muestran el mensaje de la regla (RF-03 C7, C8 y C11)')
def test_fila_propia(entidades):
    archivos = generar(entidades, **CON_USUARIO)

    pantalla = compacto(archivos[f'{FRONTEND}/usuario/UsuarioPantalla.jsx'])
    assert "filaPropia: { marca: 'usuario.pantalla.fila_propia', sinAcciones: ['desactivar'] }" in pantalla
    assert 'function Campos({ paraCrear, propia })' in pantalla
    assert "t('usuario.reglas.no_cambiar_su_propio_rol')" in pantalla
    assert "t('usuario.reglas.no_cambiar_su_propia_contrasena')" in pantalla
    assert 'filaPropia: null' in compacto(archivos[f'{FRONTEND}/empresa/EmpresaPantalla.jsx'])
    espanol = archivos[f'{FRONTEND}/textos.js'].split('  en: {')[0]
    assert "'usuario.pantalla.fila_propia': 'Tú'," in espanol


@pytest.mark.caso('RP-23 · los esquemas generados exportan los límites de cada campo, los del JSON, para que una página escrita a mano, como Cambiar contraseña (RF-18), no los copie')
def test_limites_de_los_campos(entidades):
    esquemas = compacto(generar(entidades, **CON_USUARIO)[f'{FRONTEND}/usuario/usuarioEsquemas.js'])

    assert 'export const LIMITES = {' in esquemas
    assert 'contrasena: { minimo: 8, maximo: 64, maximoBytes: 72 }' in esquemas
    assert 'nombre: { maximo: 120 }' in esquemas


@pytest.mark.caso('RP-23 · la prueba generada de la pantalla revisa lo nuevo de usuario: el límite de activos, la fila propia, la nota de la contraseña, la contraseña vacía al editar y el valor inicial')
def test_pruebas_generadas_de_usuario(entidades):
    prueba = generar(entidades, **CON_USUARIO)[f'{FRONTEND}/usuario/UsuarioPantalla.test.jsx']

    for titulo in ('RF-03 · al llegar al máximo de activos de su regla, no deja registrar ni reactivar y muestra el '
                   'mensaje de la regla',
                   'RF-03 · la fila de quien tiene la sesión lleva su marca y no tiene las acciones que impiden sus '
                   'reglas',
                   'RF-03 · al editar la fila propia, los campos que impiden sus reglas no se cambian y muestran el '
                   'mensaje de la regla',
                   'RF-03 · al editar a otra persona, la nota de la contraseña aparece al escribir una nueva',
                   'RF-03 · al editar sin escribir una contraseña, no la envía y queda la que había',
                   'RF-03 · el formulario para registrar empieza con los valores iniciales del JSON'):
        assert titulo in prueba
    assert ("expect(within(parrafoCon('Para cambiar tu propia contraseña, usa «Cambiar contraseña».'))"
            ".getByRole('link', { name: 'Cambiar contraseña' })) .toHaveAttribute('href', '/cambiar-contrasena');"
            ) in compacto(prueba), 'el enlace va solo en el nombre de la página, y la frase se revisa completa'


@pytest.mark.caso('RP-23 · la pantalla de usuario pide los textos de lo nuevo: la marca de la fila propia y los de la contraseña al editar, y el generador dice cuál falta')
def test_textos_que_pide_la_pantalla_de_usuario(entidades):
    textos = entidades['usuario']['pantalla']['textos']
    del textos['fila_propia'], textos['contrasena_al_editar'], textos['nota_contrasena_de_otro']

    with pytest.raises(ErrorGeneracion) as error:
        generar(entidades, **CON_USUARIO)

    for falta in ('pantalla.textos.fila_propia, que pide en_la_fila_propia',
                  'pantalla.textos.contrasena_al_editar, que pide la contraseña opcional al editar',
                  'pantalla.textos.nota_contrasena_de_otro, que pide la contraseña opcional al editar'):
        assert falta in str(error.value)
