# Nombre: test_java.py
# Descripción: Pruebas de la generación de Java (persistencia, API y pruebas) y de los textos es/en.
# Fecha de creación: 13/10/2026
# Actualización: 14/10/2026
# Autor: Equipo InspectIA
# Versión: 1.1
"""Pruebas de lo que genera el generador para el backend: la persistencia de cada CRUD, la API de las
entidades del alcance y los textos en español e inglés."""

import pytest

from inspectia_generador.generacion import generar_archivos
from inspectia_generador.sql import ErrorGeneracion

JAVA = 'backend/src/generated/java/co/edu/usco/inspectia'
PRUEBAS = 'backend/src/test-generated/java/co/edu/usco/inspectia'
TEXTOS = 'backend/src/generated/resources/i18n'
# Con el JDK 25, al recompilar sin «clean», los constructores que crean estas anotaciones quedan sin
# la llamada a super() y la JVM rechaza la clase (VerifyError). Ver CLAUDE.md.
CONSTRUCTORES_LOMBOK = ('RequiredArgsConstructor', 'AllArgsConstructor', 'NoArgsConstructor', 'Builder', 'Data',
                        'Value')


def generar(entidades, api=('empresa',)):
    return {a.ruta: a.contenido for a in generar_archivos(entidades, api)}


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


@pytest.mark.caso('RP-23 · la API de una entidad por empresa no se genera hasta que exista la sesión con empresa (RF-01)')
def test_rechaza_la_api_de_una_entidad_por_empresa(entidades):
    with pytest.raises(ErrorGeneracion) as error:
        generar(entidades, api=('empresa', 'usuario'))

    mensaje = str(error.value)
    assert 'No se puede generar la API de usuario' in mensaje
    assert 'por empresa' in mensaje
    assert 'valores_en_formulario' in mensaje


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
