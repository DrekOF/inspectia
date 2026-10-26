# Nombre: test_java.py
# Descripción: Pruebas de la generación de Java (persistencia, API y pruebas) y de los textos es/en.
# Fecha de creación: 13/10/2026
# Actualización: 26/10/2026
# Autor: Equipo InspectIA
# Versión: 1.7
"""Pruebas de lo que genera el generador para el backend: la persistencia de cada CRUD, la API de las
entidades del alcance, incluidas las de una entidad por empresa y las de edición en bloque, y los textos
en español e inglés."""

import re

import pytest

from conftest import EJEMPLOS, atributo, casos_java
from inspectia_generador.generacion import generar_archivos
from inspectia_generador.sql import ErrorGeneracion

JAVA = 'backend/src/generated/java/co/edu/usco/inspectia'
PRUEBAS = 'backend/src/test-generated/java/co/edu/usco/inspectia'
TEXTOS = 'backend/src/generated/resources/i18n'
# Lo que genera el generador para empresa: solo cambia a propósito, y entonces estas copias se actualizan en el
# mismo commit; se comparan sin el header. Salieron de la 1.2, antes de la API por empresa (RF-03), y la 1.7 les
# agregó el filtro activo del listado (D-33) y los valores del borde de la prueba generada.
EMPRESA_GENERADA = EJEMPLOS / 'empresa_generada'
# Con el JDK 25, al recompilar sin «clean», los constructores que crean estas anotaciones quedan sin
# la llamada a super() y la JVM rechaza la clase (VerifyError). Ver CLAUDE.md.
CONSTRUCTORES_LOMBOK = ('RequiredArgsConstructor', 'AllArgsConstructor', 'NoArgsConstructor', 'Builder', 'Data',
                        'Value')
# Con rango_recomendacion, la entidad con edicion_en_bloque (RF-13).
API_CON_RANGOS = ('empresa', 'usuario', 'rango_recomendacion')


def generar(entidades, api=('empresa',)):
    return {a.ruta: a.contenido for a in generar_archivos(entidades, api)}


def anotaciones_de(texto, campo):
    """Las anotaciones de un componente de un record generado: las líneas con @ justo antes de su tipo y nombre."""
    lineas = [linea.strip() for linea in texto.split('\n')]
    posicion = next(i for i, linea in enumerate(lineas) if re.fullmatch(rf'[\w<>]+ {campo}[,)].*', linea))
    anotaciones = []
    while posicion > 0 and lineas[posicion - 1].startswith('@'):
        posicion -= 1
        anotaciones.insert(0, lineas[posicion])
    return anotaciones


def sin_header(texto):
    """Un archivo generado sin su header, que cambia con la versión del generador y de los JSON."""
    return texto[texto.index('\npackage ') + 1:]


@pytest.mark.caso('RP-23 · cada CRUD tiene persistencia y solo las entidades del alcance tienen API')
def test_persistencia_de_todos_y_api_del_alcance(entidades):
    archivos = generar(entidades)

    for clase in ('empresa/Empresa', 'empresa/EmpresaRepositorio', 'usuario/Usuario', 'usuario/UsuarioRol',
                  'usuario/UsuarioRepositorio', 'rango/RangoRecomendacion',
                  'rango/RangoRecomendacionCategoria', 'rango/RangoRecomendacionValoresIniciales'):
        assert f'{JAVA}/{clase}.java' in archivos
    for clase in ('Controlador', 'Servicio', 'Acceso', 'CrearSolicitud', 'EditarSolicitud', 'Respuesta',
                  'Mapeador'):
        assert f'{JAVA}/empresa/Empresa{clase}.java' in archivos
        assert f'{JAVA}/usuario/Usuario{clase}.java' not in archivos
    assert f'{JAVA}/empresa/ReglasEmpresa.java' in archivos
    assert f'{JAVA}/empresa/AdministradorSolicitud.java' in archivos
    assert f'{PRUEBAS}/empresa/EmpresaApiGeneradaIT.java' in archivos
    assert not any('/lote/' in ruta or 'Lote' in ruta for ruta in archivos)


@pytest.mark.caso('RP-23 · la API de una entidad por empresa toma la empresa de la sesión y limita cada consulta a ella (RNF-03)')
def test_api_por_empresa(entidades):
    archivos = generar(entidades, api=('empresa', 'usuario'))

    controlador = archivos[f'{JAVA}/usuario/UsuarioControlador.java']
    assert controlador.count('@AuthenticationPrincipal UsuarioEnSesion sesion') == 6
    servicio = archivos[f'{JAVA}/usuario/UsuarioServicio.java']
    assert 'repositorio.findAllByEmpresaId(sesion.empresaId(), pedido)' in servicio
    assert 'repositorio.findByIdAndEmpresaId(id, sesion.empresaId())' in servicio
    assert 'usuario.setEmpresaId(sesion.empresaId());' in servicio
    assert 'findById(' not in servicio and 'findAll(' not in servicio, 'ninguna consulta sin la empresa'
    repositorio = archivos[f'{JAVA}/usuario/UsuarioRepositorio.java']
    assert 'Page<Usuario> findAllByEmpresaId(UUID empresaId, Pageable pagina);' in repositorio
    assert 'Optional<Usuario> findByIdAndEmpresaId(UUID id, UUID empresaId);' in repositorio
    for solicitud in ('UsuarioCrearSolicitud', 'UsuarioEditarSolicitud'):
        assert 'empresaId' not in archivos[f'{JAVA}/usuario/{solicitud}.java'], 'la empresa nunca sale de la petición'
    assert 'RNF-03 · ' in archivos[f'{PRUEBAS}/usuario/UsuarioApiGeneradaIT.java']


@pytest.mark.caso('RP-23 · el listado de una entidad con atributo_activo filtra con ?activo=true o false, y sin el filtro trae todos (D-33)')
def test_listado_filtra_por_activo(entidades):
    archivos = generar(entidades, api=('empresa', 'usuario'))

    controlador = archivos[f'{JAVA}/empresa/EmpresaControlador.java']
    assert '@RequestParam(name = "activo", required = false) Boolean activo' in controlador
    assert 'return servicio.listar(pagina, tamano, activo);' in controlador
    servicio = archivos[f'{JAVA}/empresa/EmpresaServicio.java']
    assert 'repositorio.findAll(pedido)' in servicio
    assert 'repositorio.findAllByActiva(activo, pedido)' in servicio
    assert ('Page<Empresa> findAllByActiva(Boolean activa, Pageable pagina);'
            in archivos[f'{JAVA}/empresa/EmpresaRepositorio.java'])
    assert 'repositorio.findAllByEmpresaIdAndActivo(sesion.empresaId(), activo, pedido)' in archivos[
        f'{JAVA}/usuario/UsuarioServicio.java'], 'en una entidad por empresa, el filtro también se limita a ella'
    assert ('Page<Usuario> findAllByEmpresaIdAndActivo(UUID empresaId, Boolean activo, Pageable pagina);'
            in archivos[f'{JAVA}/usuario/UsuarioRepositorio.java'])
    assert ('@DisplayName("RF-02 · el listado de empresa filtra por activo: con true trae solo los activos y con false, '
            'los desactivados")') in archivos[f'{PRUEBAS}/empresa/EmpresaApiGeneradaIT.java']


@pytest.mark.caso('RP-23 · la prueba generada de la API revisa que cada validación del JSON acepte su valor del borde: el largo máximo, el mínimo y los 72 bytes')
def test_bordes_aceptados(entidades):
    prueba = generar(entidades)[f'{PRUEBAS}/empresa/EmpresaApiGeneradaIT.java']

    assert '@DisplayName("RF-02 · cada validación del JSON de empresa acepta su valor del borde al crear")' in prueba
    assert '@DisplayName("RF-02 · cada validación del JSON de empresa acepta su valor del borde al editar")' in prueba
    assert casos_java(prueba, 'bordesAlCrear') == [
        ('nombre', '"x".repeat(120)', None), ('ciudad', '"x".repeat(80)', None),
        ('administrador.nombre', '"x".repeat(120)', None), ('administrador.correo', 'correoDeLargo(254)', None),
        ('administrador.contrasena', '"x".repeat(64)', None), ('administrador.contrasena', '"x".repeat(8)', None),
        ('administrador.contrasena', '"ñ".repeat(36)', None)]
    assert casos_java(prueba, 'bordesAlEditar') == [
        ('nombre', '"x".repeat(120)', None), ('ciudad', '"x".repeat(80)', None)], \
        'el NIT tiene patrón: su ejemplo ya es su valor válido'


@pytest.mark.caso('RP-23 · la prueba generada de la API rechaza el ejemplo de un patrón con algo pegado antes o después, como x900123456-8 y 900123456-8x: así revisa que el patrón vaya anclado con ^ y $')
def test_patron_anclado(entidades):
    prueba = generar(entidades)[f'{PRUEBAS}/empresa/EmpresaApiGeneradaIT.java']
    mensaje = 'Escribe el NIT sin puntos y con el dígito de verificación, por ejemplo 900123456-8.'

    for metodo in ('validacionesAlCrear', 'validacionesAlEditar'):
        casos = casos_java(prueba, metodo)
        assert ('nit', '"x900123456-8"', mensaje) in casos, metodo
        assert ('nit', '"900123456-8x"', mensaje) in casos, metodo


@pytest.mark.caso('RP-23 · lo que se pega al ejemplo de un patrón es algo que el patrón no acepta en ese lugar, y no se pega nada si no cabe en el largo, porque el valor fallaría también por largo')
def test_patron_anclado_en_los_bordes(entidades):
    nit = atributo(entidades['empresa'], 'nit')
    nit['validaciones']['patron'] = '^[0-9x]{6,10}-[0-9x]$'

    casos = casos_java(generar(entidades)[f'{PRUEBAS}/empresa/EmpresaApiGeneradaIT.java'], 'validacionesAlCrear')
    assert [valor for campo, valor, _ in casos if campo == 'nit'] == ['null', '"?"', '"?900123456-8"', '"900123456-8x"'], \
        'el patrón acepta una x al inicio: se pega ?'

    nit['longitud'] = len(nit['ejemplo'])
    casos = casos_java(generar(entidades)[f'{PRUEBAS}/empresa/EmpresaApiGeneradaIT.java'], 'validacionesAlCrear')
    assert [valor for campo, valor, _ in casos if campo == 'nit'] == ['null', '"?"']


@pytest.mark.caso('RP-23 · una entidad por empresa no puede dar operaciones al administrador de la plataforma, que no tiene empresa')
def test_por_empresa_sin_administrador_de_la_plataforma(entidades):
    entidades['usuario']['operaciones']['listar'] = ['ADMIN_EMPRESA', 'ADMIN_PLATAFORMA']

    with pytest.raises(ErrorGeneracion) as error:
        generar(entidades, api=('empresa', 'usuario'))

    assert 'ADMIN_PLATAFORMA' in str(error.value)
    assert 'no tiene empresa' in str(error.value)


@pytest.mark.caso('RP-23 · valores_en_formulario se valida con @ValorPermitido, que responde 403 antes que los demás errores')
def test_valores_en_formulario(entidades):
    archivos = generar(entidades, api=('empresa', 'usuario'))

    for solicitud in ('UsuarioCrearSolicitud', 'UsuarioEditarSolicitud'):
        assert ('@ValorPermitido(valores = {"USUARIO_FINAL", "ADMIN_EMPRESA"}, '
                'message = "{usuario.rol.valores_en_formulario}")') in anotaciones_de(
            archivos[f'{JAVA}/usuario/{solicitud}.java'], 'rol')


@pytest.mark.caso('RP-23 · la contraseña del formulario es obligatoria al crear y opcional al editar, y solo se guarda su hash')
def test_contrasena_en_formulario(entidades):
    archivos = generar(entidades, api=('empresa', 'usuario'))

    crear = anotaciones_de(archivos[f'{JAVA}/usuario/UsuarioCrearSolicitud.java'], 'contrasena')
    editar = anotaciones_de(archivos[f'{JAVA}/usuario/UsuarioEditarSolicitud.java'], 'contrasena')
    assert '@NotBlank(message = "{validacion.obligatorio}")' in crear
    assert '@NotBlank(message = "{validacion.obligatorio}")' not in editar
    for anotaciones in (crear, editar):
        assert '@MaximoBytes(value = 72, message = "{usuario.contrasena_hash.bytes_max}")' in anotaciones
    assert 'codificador.encode(solicitud.contrasena())' in archivos[f'{JAVA}/usuario/UsuarioServicio.java']
    assert 'setContrasenaHash(solicitud.' not in archivos[f'{JAVA}/usuario/UsuarioMapeador.java']


@pytest.mark.caso('RP-23 · una contraseña de solo espacios se rechaza como si faltara: al crear con @NotBlank y al editar, si llega, con @NoEnBlanco')
def test_contrasena_de_solo_espacios(entidades):
    archivos = generar(entidades, api=('empresa', 'usuario'))

    editar = archivos[f'{JAVA}/usuario/UsuarioEditarSolicitud.java']
    assert '@NoEnBlanco(message = "{validacion.obligatorio}")' in anotaciones_de(editar, 'contrasena')
    assert 'import co.edu.usco.inspectia.comun.validacion.NoEnBlanco;' in editar
    assert '@NoEnBlanco' not in archivos[f'{JAVA}/usuario/UsuarioCrearSolicitud.java'], 'al crear ya está @NotBlank'
    prueba = archivos[f'{PRUEBAS}/usuario/UsuarioApiGeneradaIT.java']
    assert prueba.count('Arguments.of("contrasena", " ".repeat(8), "Este dato es obligatorio.")') == 2, \
        'un caso al crear y otro al editar'
    assert ('Arguments.of("administrador.contrasena", " ".repeat(8), "Este dato es obligatorio.")'
            in archivos[f'{PRUEBAS}/empresa/EmpresaApiGeneradaIT.java'])


@pytest.mark.caso('RP-23 · un efecto de la contraseña se programa a mano y la edición generada lo llama solo si llega una contraseña nueva, después de guardar su hash')
def test_efecto_al_cambiar_contrasena(entidades):
    archivos = generar(entidades, api=('empresa', 'usuario'))

    reglas = archivos[f'{JAVA}/usuario/ReglasUsuario.java']
    assert 'void cortarSesionesAlCambiarContrasena(Usuario usuario, UsuarioEnSesion sesion);' in reglas
    servicio = archivos[f'{JAVA}/usuario/UsuarioServicio.java']
    bloque = servicio[servicio.index('if (solicitud.contrasena() != null) {'):]
    bloque = bloque[:bloque.index('}')]
    assert bloque.index('codificador.encode(') < bloque.index('reglas.cortarSesionesAlCambiarContrasena(usuario, sesion);')
    assert servicio.count('reglas.cortarSesionesAlCambiarContrasena(') == 1, 'solo al editar'


@pytest.mark.caso('RP-23 · un efecto sin atributo se llama en cada operación de cuando, después de los cambios y antes de escribirlos en la base')
def test_efecto_en_una_operacion(entidades):
    entidades['empresa']['reglas'].append({
        'codigo': 'avisar_al_desactivar', 'tipo': 'efecto', 'cuando': ['desactivar'],
        'descripcion': 'Avisa que la empresa se desactivó.', 'fichas': ['RF-02']})

    archivos = generar(entidades)

    assert 'void avisarAlDesactivar(Empresa empresa);' in archivos[f'{JAVA}/empresa/ReglasEmpresa.java']
    servicio = archivos[f'{JAVA}/empresa/EmpresaServicio.java']
    desactivar = servicio[servicio.index('public EmpresaRespuesta desactivar('):]
    desactivar = desactivar[:desactivar.index('return ')]
    assert (desactivar.index('empresa.setActiva(false);') < desactivar.index('reglas.avisarAlDesactivar(empresa);')
            < desactivar.index('repositorio.flush();'))
    assert servicio.count('reglas.avisarAlDesactivar(') == 1, 'solo al desactivar'


@pytest.mark.caso('RP-23 · el generador se detiene con un efecto que no sabe llamar: en listar o ver, o atado a un atributo que no es una contraseña')
def test_efectos_que_no_sabe_llamar(entidades):
    entidades['empresa']['reglas'] += [
        {'codigo': 'al_listar', 'tipo': 'efecto', 'cuando': ['listar'], 'descripcion': 'x', 'fichas': ['RF-02']},
        {'codigo': 'al_cambiar_nit', 'tipo': 'efecto', 'cuando': ['editar'], 'atributo': 'nit', 'descripcion': 'x',
         'fichas': ['RF-02']}]

    with pytest.raises(ErrorGeneracion) as error:
        generar(entidades)

    assert 'al_listar' in str(error.value)
    assert 'al_cambiar_nit' in str(error.value)


@pytest.mark.caso('RP-23 · la respuesta solo lleva los atributos de la lista o del formulario: nunca el hash ni los datos internos')
def test_respuesta_sin_datos_internos(entidades):
    archivos = generar(entidades, api=('empresa', 'usuario'))

    respuesta = archivos[f'{JAVA}/usuario/UsuarioRespuesta.java']
    for interno in ('contrasenaHash', 'intentosFallidos', 'bloqueadoHasta'):
        assert interno not in respuesta
    for publico in ('String nombre', 'String correo', 'UsuarioRol rol', 'Boolean activo'):
        assert publico in respuesta


@pytest.mark.caso('RP-23 · las reglas de una entidad por empresa reciben la sesión, y los parámetros de una regla salen como constantes')
def test_reglas_con_sesion_y_parametros(entidades):
    archivos = generar(entidades, api=('empresa', 'usuario'))

    reglas = archivos[f'{JAVA}/usuario/ReglasUsuario.java']
    assert 'int MAXIMO_USUARIOS_ACTIVOS = 25;' in reglas
    for regla in ('MaximoUsuariosActivos', 'NoDesactivarseASiMismo', 'NoCambiarSuPropioRol',
                  'NoCambiarSuPropiaContrasena'):
        assert f'boolean cumple{regla}(Usuario usuario, UsuarioEnSesion sesion);' in reglas
    assert 'boolean cumpleNitDigitoVerificacion(Empresa empresa);' in archivos[f'{JAVA}/empresa/ReglasEmpresa.java']


@pytest.mark.caso('RP-23 · lo generado de empresa solo cambia a propósito: es igual a su copia de referencia, salvo el header')
def test_empresa_sigue_igual(entidades):
    archivos = generar(entidades)

    referencias = sorted(EMPRESA_GENERADA.glob('*.java'))
    assert len(referencias) == 12
    for referencia in referencias:
        carpeta = PRUEBAS if referencia.name.endswith('IT.java') else JAVA
        esperado = referencia.read_bytes().decode('utf-8').replace('\r\n', '\n')
        assert sin_header(archivos[f'{carpeta}/empresa/{referencia.name}']) == sin_header(esperado), referencia.name


@pytest.mark.caso('RNF-04 · las reglas de acceso por ruta y @PreAuthorize salen de operaciones del JSON')
def test_roles_por_ruta_desde_operaciones(entidades):
    entidades['empresa']['operaciones']['ver'] = ['ADMIN_PLATAFORMA', 'ADMIN_EMPRESA']

    archivos = generar(entidades)

    acceso = archivos[f'{JAVA}/empresa/EmpresaAcceso.java']
    assert '.requestMatchers(HttpMethod.GET, "/api/v1/empresas").hasRole("ADMIN_PLATAFORMA")' in acceso
    assert ('.requestMatchers(HttpMethod.GET, "/api/v1/empresas/{id}")'
            '.hasAnyRole("ADMIN_PLATAFORMA", "ADMIN_EMPRESA")') in acceso
    assert '.requestMatchers(HttpMethod.POST, "/api/v1/empresas/{id}/desactivar")' in acceso
    controlador = archivos[f'{JAVA}/empresa/EmpresaControlador.java']
    assert '@PreAuthorize("hasAnyRole(\'ADMIN_PLATAFORMA\', \'ADMIN_EMPRESA\')")' in controlador


@pytest.mark.caso('RP-23 · las validaciones del JSON pasan a Bean Validation con la clave de su mensaje')
def test_validaciones_a_bean_validation(entidades):
    archivos = generar(entidades)

    empresa = archivos[f'{JAVA}/empresa/EmpresaCrearSolicitud.java']
    assert '@Pattern(regexp = "^[0-9]{6,10}-[0-9]$", message = "{empresa.nit.patron}")' in empresa
    assert '@Size(max = 120, message = "{validacion.longitud_max}")' in empresa
    assert 'nit = Textos.recortar(nit);' in empresa
    administrador = archivos[f'{JAVA}/empresa/AdministradorSolicitud.java']
    assert '@Size(min = 8, message = "{usuario.contrasena_hash.longitud_min}")' in administrador
    assert '@MaximoBytes(value = 72, message = "{usuario.contrasena_hash.bytes_max}")' in administrador
    assert 'correo = Textos.minusculas(Textos.recortar(correo));' in administrador


@pytest.mark.caso('RP-11 · los textos de todas las entidades salen en español y en inglés')
def test_textos_en_espanol_e_ingles(entidades):
    archivos = generar(entidades)

    espanol = archivos[f'{TEXTOS}/generado.properties']
    ingles = archivos[f'{TEXTOS}/generado_en.properties']
    assert 'empresa.reglas.nit_digito_verificacion=El dígito de verificación del NIT no corresponde.' in espanol
    assert 'empresa.reglas.nit_digito_verificacion=The NIT check digit is not valid.' in ingles
    assert 'usuario.contrasena_hash.longitud_min=La contraseña debe tener al menos 8 caracteres.' in espanol
    assert 'lote.estado.PENDIENTE=' in espanol
    assert 'validacion.obligatorio=This field is required.' in ingles


@pytest.mark.caso('RP-23 · los rangos por defecto de una empresa nueva salen de valores_iniciales del JSON')
def test_valores_iniciales(entidades):
    archivos = generar(entidades)

    iniciales = archivos[f'{JAVA}/rango/RangoRecomendacionValoresIniciales.java']
    assert 'RangoRecomendacionCategoria.PREMIUM,\n                        new BigDecimal("96.0"),' in iniciales
    assert iniciales.count('fila(empresaId,') == 4
    servicio = archivos[f'{JAVA}/empresa/EmpresaServicio.java']
    assert 'RangoRecomendacionValoresIniciales.paraEmpresa(empresa.getId())' in servicio


@pytest.mark.caso('RP-23 · el código generado escribe sus constructores, sin las anotaciones de Lombok que los crean')
def test_constructores_sin_lombok(entidades):
    archivos = generar(entidades)

    for ruta, contenido in archivos.items():
        usadas = [a for a in CONSTRUCTORES_LOMBOK if f'import lombok.{a};' in contenido]
        assert not usadas, f'{ruta} usa {usadas}'
    servicio = archivos[f'{JAVA}/empresa/EmpresaServicio.java']
    assert ('    public EmpresaServicio(\n'
            '            EmpresaRepositorio repositorio,\n'
            '            ReglasEmpresa reglas,\n') in servicio
    assert '        this.rangoRecomendacionRepositorio = rangoRecomendacionRepositorio;\n    }' in servicio
    controlador = archivos[f'{JAVA}/empresa/EmpresaControlador.java']
    assert '    public EmpresaControlador(EmpresaServicio servicio) {\n        this.servicio = servicio;' in controlador


@pytest.mark.caso('RP-23 · edicion_en_bloque: lista las filas sin páginas, las edita todas juntas y restaura los valores por defecto, siempre en la empresa de la sesión (RF-13, RNF-03)')
def test_api_en_bloque(entidades):
    archivos = generar(entidades, api=API_CON_RANGOS)

    controlador = archivos[f'{JAVA}/rango/RangoRecomendacionControlador.java']
    assert '@RequestMapping("/api/v1/rangos-recomendacion")' in controlador
    assert 'public List<RangoRecomendacionRespuesta> listar(' in controlador
    assert '@Valid @RequestBody RangoRecomendacionEdicionSolicitud solicitud' in controlador
    assert '@PostMapping("/restaurar-por-defecto")' in controlador
    assert 'public List<RangoRecomendacionRespuesta> restaurarPorDefecto(' in controlador
    assert '"/{id}' not in controlador, 'en bloque no hay rutas por fila'
    acceso = archivos[f'{JAVA}/rango/RangoRecomendacionAcceso.java']
    for metodo, ruta in (('GET', ''), ('PUT', ''), ('POST', '/restaurar-por-defecto')):
        assert (f'.requestMatchers(HttpMethod.{metodo}, "/api/v1/rangos-recomendacion{ruta}")'
                '.hasRole("ADMIN_EMPRESA")') in acceso
    assert 'List<RangoRecomendacionFilaSolicitud> filas' in archivos[f'{JAVA}/rango/RangoRecomendacionEdicionSolicitud.java']
    fila = archivos[f'{JAVA}/rango/RangoRecomendacionFilaSolicitud.java']
    for campo in ('RangoRecomendacionCategoria categoria', 'BigDecimal limiteInferior', 'String recomendacion'):
        assert campo in fila
    for sobra in ('CrearSolicitud', 'EditarSolicitud'):
        assert f'{JAVA}/rango/RangoRecomendacion{sobra}.java' not in archivos
    servicio = archivos[f'{JAVA}/rango/RangoRecomendacionServicio.java']
    assert 'repositorio.findAllByEmpresaId(sesion.empresaId())' in servicio
    assert 'RangoRecomendacionValoresIniciales.paraEmpresa(sesion.empresaId())' in servicio, \
        'restaurar usa la misma fuente que RF-02 al crear la empresa'
    assert '"rango_recomendacion.filas_completas"' in servicio
    assert 'findById(' not in servicio and 'findAll(' not in servicio, 'ninguna consulta sin la empresa'
    repositorio = archivos[f'{JAVA}/rango/RangoRecomendacionRepositorio.java']
    assert 'List<RangoRecomendacion> findAllByEmpresaId(UUID empresaId);' in repositorio
    reglas = archivos[f'{JAVA}/rango/ReglasRangoRecomendacion.java']
    assert 'boolean cumpleLimitesEnOrden(List<RangoRecomendacion> filas, UsuarioEnSesion sesion);' in reglas
    assert ('rango_recomendacion.filas_completas=Envía una fila por cada categoría, sin repetir: Premium o Especial, '
            'Estándar, Corriente y Fuera de norma.') in archivos[f'{TEXTOS}/generado.properties']
    assert ('rango_recomendacion.filas_completas=Send one row for each category, with no repeats: Premium or '
            'Specialty, Standard, Regular and Off-grade.') in archivos[f'{TEXTOS}/generado_en.properties']
    prueba = archivos[f'{PRUEBAS}/rango/RangoRecomendacionApiGeneradaIT.java']
    assert 'RF-13 · ' in prueba and 'RNF-03 · ' in prueba


@pytest.mark.caso('RP-23 · solo_lectura_si: en la edición en bloque, ese dato no cambia en las filas que cumplen la condición, con el mensaje del JSON o el general')
def test_solo_lectura_en_bloque(entidades):
    archivos = generar(entidades, api=API_CON_RANGOS)

    servicio = archivos[f'{JAVA}/rango/RangoRecomendacionServicio.java']
    assert 'RangoRecomendacionCategoria.FUERA_DE_NORMA' in servicio
    assert '"rango_recomendacion.limite_inferior.solo_lectura_si"' in servicio
    assert ('rango_recomendacion.limite_inferior.solo_lectura_si=El límite de Fuera de norma es fijo: 0 %.'
            in archivos[f'{TEXTOS}/generado.properties'])
    assert ('rango_recomendacion.limite_inferior.solo_lectura_si=The Off-grade limit is fixed: 0%.'
            in archivos[f'{TEXTOS}/generado_en.properties'])

    del atributo(entidades['rango_recomendacion'], 'limite_inferior')['mensajes']['solo_lectura_si']
    archivos = generar(entidades, api=API_CON_RANGOS)

    assert '"validacion.solo_lectura"' in archivos[f'{JAVA}/rango/RangoRecomendacionServicio.java']
    assert 'validacion.solo_lectura=Este dato no se puede cambiar.' in archivos[f'{TEXTOS}/generado.properties']


@pytest.mark.caso('RP-23 · un decimal acepta como máximo los decimales de su escala, con el mensaje general en singular o en plural')
def test_maximo_decimales(entidades):
    archivos = generar(entidades, api=API_CON_RANGOS)

    fila = archivos[f'{JAVA}/rango/RangoRecomendacionFilaSolicitud.java']
    assert '@MaximoDecimales(value = 1, message = "{validacion.decimales_uno}")' in anotaciones_de(fila, 'limiteInferior')
    assert 'import co.edu.usco.inspectia.comun.validacion.MaximoDecimales;' in fila
    espanol = archivos[f'{TEXTOS}/generado.properties']
    assert 'validacion.decimales_uno=Usa como máximo 1 decimal.' in espanol
    assert 'validacion.decimales=Usa como máximo {value} decimales.' in espanol
    assert 'validacion.decimales=Use at most {value} decimal places.' in archivos[f'{TEXTOS}/generado_en.properties']

    atributo(entidades['rango_recomendacion'], 'limite_inferior')['escala'] = 2
    fila = generar(entidades, api=API_CON_RANGOS)[f'{JAVA}/rango/RangoRecomendacionFilaSolicitud.java']

    assert '@MaximoDecimales(value = 2, message = "{validacion.decimales}")' in anotaciones_de(fila, 'limiteInferior')


@pytest.mark.caso('RP-23 · un decimal se guarda y se responde con la escala de su columna: el mapeador la ajusta al copiar la solicitud')
def test_decimal_con_la_escala_de_su_columna(entidades):
    archivos = generar(entidades, api=API_CON_RANGOS)

    mapeador = archivos[f'{JAVA}/rango/RangoRecomendacionMapeador.java']
    assert 'rangoRecomendacion.setLimiteInferior(Decimales.conEscala(solicitud.limiteInferior(), 1));' in mapeador
    assert 'import co.edu.usco.inspectia.comun.numero.Decimales;' in mapeador
    assert 'rangoRecomendacion.setRecomendacion(solicitud.recomendacion());' in mapeador, 'los demás datos, tal cual'
    assert 'Decimales' not in archivos[f'{JAVA}/empresa/EmpresaMapeador.java'], 'empresa no tiene decimales'


@pytest.mark.caso('RP-23 · el generador se detiene con una edición en bloque que no sabe generar: con crear, sin una lista que identifique cada fila o con restaurar_por_defecto sin valores_iniciales')
def test_en_bloque_que_no_sabe_generar(entidades):
    rango = entidades['rango_recomendacion']
    rango['operaciones']['crear'] = ['ADMIN_EMPRESA']
    rango['unicos'] = []
    del rango['valores_iniciales']

    with pytest.raises(ErrorGeneracion) as error:
        generar(entidades, api=API_CON_RANGOS)

    assert 'edicion_en_bloque con la operación crear' in str(error.value)
    assert 'edicion_en_bloque sin una lista que, con la empresa, identifique cada fila' in str(error.value)
    assert 'restaurar_por_defecto sin valores_iniciales' in str(error.value)
