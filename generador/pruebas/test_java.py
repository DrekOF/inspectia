# Nombre: test_java.py
# Descripción: Pruebas de la generación de Java (persistencia, API y pruebas) y de los textos es/en.
# Fecha de creación: 13/10/2026
# Actualización: 18/10/2026
# Autor: Equipo InspectIA
# Versión: 1.2
"""Pruebas de lo que genera el generador para el backend: la persistencia de cada CRUD, la API de las
entidades del alcance, incluidas las de una entidad por empresa, y los textos en español e inglés."""

import re

import pytest

from conftest import EJEMPLOS
from inspectia_generador.generacion import generar_archivos
from inspectia_generador.sql import ErrorGeneracion

JAVA = 'backend/src/generated/java/co/edu/usco/inspectia'
PRUEBAS = 'backend/src/test-generated/java/co/edu/usco/inspectia'
TEXTOS = 'backend/src/generated/resources/i18n'
# Lo que generaba el generador 1.2 para empresa, antes de la API por empresa (RF-03): debe seguir igual,
# salvo el header. Si un cambio a propósito lo modifica, se actualizan estas copias en el mismo commit.
EMPRESA_GENERADA = EJEMPLOS / 'empresa_generada'
# Con el JDK 25, al recompilar sin «clean», los constructores que crean estas anotaciones quedan sin
# la llamada a super() y la JVM rechaza la clase (VerifyError). Ver CLAUDE.md.
CONSTRUCTORES_LOMBOK = ('RequiredArgsConstructor', 'AllArgsConstructor', 'NoArgsConstructor', 'Builder', 'Data',
                        'Value')


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
    assert 'repositorio.findAllByEmpresaId(sesion.empresaId(), PageRequest.of(pagina, tamano, ORDEN))' in servicio
    assert 'repositorio.findByIdAndEmpresaId(id, sesion.empresaId())' in servicio
    assert 'usuario.setEmpresaId(sesion.empresaId());' in servicio
    assert 'findById(' not in servicio and 'findAll(' not in servicio, 'ninguna consulta sin la empresa'
    repositorio = archivos[f'{JAVA}/usuario/UsuarioRepositorio.java']
    assert 'Page<Usuario> findAllByEmpresaId(UUID empresaId, Pageable pagina);' in repositorio
    assert 'Optional<Usuario> findByIdAndEmpresaId(UUID id, UUID empresaId);' in repositorio
    for solicitud in ('UsuarioCrearSolicitud', 'UsuarioEditarSolicitud'):
        assert 'empresaId' not in archivos[f'{JAVA}/usuario/{solicitud}.java'], 'la empresa nunca sale de la petición'
    assert 'RNF-03 · ' in archivos[f'{PRUEBAS}/usuario/UsuarioApiGeneradaIT.java']


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


@pytest.mark.caso('RP-23 · lo generado de empresa no cambia al agregar la API por empresa: solo su header')
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
