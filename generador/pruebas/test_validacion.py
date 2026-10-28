# Nombre: test_validacion.py
# Descripción: Pruebas de la validación de specs/entidades/*.json.
# Fecha de creación: 13/10/2026
# Actualización: 27/10/2026
# Autor: Equipo InspectIA
# Versión: 1.6
"""Pruebas de la validación de los JSON de entidades contra su formato y contra specs/."""

import pytest

from conftest import atributo
from inspectia_generador.validacion import validar


@pytest.mark.caso('RP-23 · los JSON de specs/entidades pasan la validación')
def test_entidades_reales_son_validas(entidades, codigos):
    assert validar(entidades, codigos) == []


@pytest.mark.caso('RP-23 · la validación detecta una ficha que no existe en specs/')
def test_detecta_ficha_inexistente(entidades, codigos):
    atributo(entidades['lote'], 'categoria')['fichas'] = ['RF-99']

    errores = validar(entidades, codigos)

    assert any('RF-99' in e for e in errores)


@pytest.mark.caso('RP-23 · la validación detecta un CHECK con un valor que no está en ninguna lista')
def test_detecta_valor_inexistente_en_check(entidades, codigos):
    check = entidades['foto']['checks'][0]
    check['sql'] = check['sql'].replace("'POR_REPETIR'", "'POR_REPITIR'")

    errores = validar(entidades, codigos)

    assert any('POR_REPITIR' in e for e in errores)


@pytest.mark.caso('RP-23 · la validación detecta una referencia a una entidad que no existe')
def test_detecta_referencia_inexistente(entidades, codigos):
    atributo(entidades['muestra'], 'lote_id')['referencia']['entidad'] = 'lotes'

    errores = validar(entidades, codigos)

    assert any('muestra.lote_id' in e for e in errores)


@pytest.mark.caso('RP-23 · una entidad con CRUD necesita modulo y ruta')
def test_crud_necesita_modulo_y_ruta(entidades, codigos):
    del entidades['empresa']['modulo']
    del entidades['empresa']['ruta']

    errores = validar(entidades, codigos)

    assert any('empresa' in e and 'modulo' in e for e in errores)
    assert any('empresa' in e and 'ruta' in e for e in errores)


@pytest.mark.caso('RP-23 · un atributo no puede usar el nombre de una columna implícita')
def test_detecta_columna_implicita_repetida(entidades, codigos):
    entidades['empresa']['atributos'].append(
        {**atributo(entidades['empresa'], 'nombre'), 'nombre': 'fecha_creacion'})

    errores = validar(entidades, codigos)

    assert any('fecha_creacion' in e for e in errores)


@pytest.mark.caso('RP-23 · la validación pide el nombre de crear_con, que es el campo de la solicitud')
def test_crear_con_necesita_nombre(entidades, codigos):
    del entidades['empresa']['crear_con']['nombre']

    errores = validar(entidades, codigos)

    assert any('crear_con' in e and 'falta nombre' in e for e in errores)


@pytest.mark.caso('RP-23 · la validación revisa que el ejemplo de un atributo cumpla sus validaciones')
def test_ejemplo_debe_ser_valido(entidades, codigos):
    atributo(entidades['empresa'], 'nit')['ejemplo'] = '900.123.456-8'
    atributo(entidades['usuario'], 'correo')['ejemplo'] = 'sin-arroba'

    errores = validar(entidades, codigos)

    assert 'empresa.nit: el ejemplo no cumple el patron' in errores
    assert 'usuario.correo: el ejemplo no es un correo' in errores


@pytest.mark.caso('RP-23 · la validación rechaza un mensaje con un marcador que no está en validaciones')
def test_marcador_de_mensaje_inexistente(entidades, codigos):
    mensajes = atributo(entidades['usuario'], 'contrasena_hash')['mensajes']
    mensajes['longitud_min']['es'] = 'Debe tener al menos {minimo} caracteres.'

    errores = validar(entidades, codigos)

    assert any('{minimo}' in e for e in errores)


@pytest.mark.caso('RP-23 · la validación rechaza solo_lectura_si en una entidad sin edicion_en_bloque, en vez de ignorarlo')
def test_solo_lectura_si_solo_en_bloque(entidades, codigos):
    atributo(entidades['usuario'], 'nombre')['solo_lectura_si'] = {'atributo': 'rol', 'igual_a': 'ADMIN_PLATAFORMA'}

    errores = validar(entidades, codigos)

    assert 'usuario.nombre: solo_lectura_si va solo en una entidad con edicion_en_bloque' in errores


@pytest.mark.caso('RP-23 · un atributo con solo_lectura_si puede traer su mensaje; uno sin solo_lectura_si, no')
def test_mensaje_de_solo_lectura(entidades, codigos):
    mensaje = {'es': 'No cambia.', 'en': 'It does not change.'}
    atributo(entidades['rango_recomendacion'], 'limite_inferior').setdefault('mensajes', {})['solo_lectura_si'] = mensaje
    atributo(entidades['rango_recomendacion'], 'recomendacion')['mensajes'] = {'solo_lectura_si': mensaje}

    errores = validar(entidades, codigos)

    assert errores == ['rango_recomendacion.recomendacion: mensaje para algo que el atributo no tiene '
                       '(solo_lectura_si)']


@pytest.mark.caso('RP-23 · la validación revisa la clave pantalla: claves conocidas, textos en español y en inglés, singular y plural donde se cuenta, y marcadores que existen')
def test_valida_la_pantalla(entidades, codigos):
    pantalla = entidades['empresa']['pantalla']
    textos = pantalla['textos']
    textos['nuevo'] = {'es': 'Nueva empresa'}
    textos['titulo_raro'] = {'es': 'Raro', 'en': 'Odd'}
    textos['editar_fila'] = {'es': 'Editar {nombr}', 'en': 'Edit {nombre}'}
    textos['resumen'] = {'es': '{todas}.', 'en': '{total}.'}
    textos['resumen_total'] = {'es': '{cantidad} empresas', 'en': '{cantidad} companies'}
    pantalla['debajo_del_nombre'] = 'contrasena'
    pantalla['color'] = 'rojo'
    entidades['lote']['pantalla'] = {'textos': {}}

    errores = validar(entidades, codigos)

    for error in ('empresa.pantalla.textos.nuevo: necesita textos en es y en',
                  'empresa.pantalla.textos: texto desconocido titulo_raro',
                  'empresa.pantalla.textos.editar_fila: usa {nombr}, que no existe en este texto',
                  'empresa.pantalla.textos.resumen: usa {todas}, que no existe en este texto',
                  'empresa.pantalla.textos.resumen_total: necesita singular y plural en es y en',
                  'empresa.pantalla: debajo_del_nombre debe ser un atributo de la lista',
                  'empresa.pantalla: clave desconocida color',
                  'lote: pantalla va solo en entidades con CRUD'):
        assert error in errores


@pytest.mark.caso('RP-23 · un patron va anclado con ^ y $ y sin lo que Java, PostgreSQL y JavaScript leen distinto, así los tres validan igual')
def test_patron_que_todos_leen_igual(entidades, codigos):
    atributo(entidades['empresa'], 'nit')['validaciones']['patron'] = '[0-9]{6,10}-[0-9]'
    atributo(entidades['empresa'], 'ciudad')['validaciones']['patron'] = '^[A-Za-z]++$'

    errores = validar(entidades, codigos)

    assert 'empresa.nit: el patron va anclado: empieza con ^ y termina con $' in errores
    assert 'empresa.ciudad: el patron usa ++, que Java, PostgreSQL y JavaScript no leen igual' in errores


@pytest.mark.caso('RP-23 · la validación rechaza solo_lo_cambian_las_reglas donde no serviría: en un atributo del formulario, en una entidad sin CRUD y en un atributo obligatorio sin por_defecto')
def test_solo_lo_cambian_las_reglas(entidades, codigos):
    atributo(entidades['usuario'], 'nombre')['solo_lo_cambian_las_reglas'] = True
    atributo(entidades['lote'], 'codigo')['solo_lo_cambian_las_reglas'] = True
    intentos = atributo(entidades['usuario'], 'intentos_fallidos')
    del intentos['por_defecto']
    intentos['solo_lo_cambian_las_reglas'] = True

    errores = validar(entidades, codigos)

    for error in ('usuario.nombre: solo_lo_cambian_las_reglas no va en un atributo del formulario: JPA no lo '
                  'guardaría',
                  'lote.codigo: solo_lo_cambian_las_reglas va solo en entidades con CRUD',
                  'usuario.intentos_fallidos: solo_lo_cambian_las_reglas en un atributo obligatorio necesita '
                  'por_defecto: JPA no lo escribe al crear'):
        assert error in errores


@pytest.mark.caso('RP-23 · la validación revisa las claves nuevas de pantalla: formulario encima o al lado, limite_de_activos con una regla de validación con maximo en crear o reactivar, e inicial con valores que el formulario ofrece')
def test_claves_nuevas_de_la_pantalla(entidades, codigos):
    usuario = entidades['usuario']
    usuario['pantalla'] = {'formulario': 'abajo', 'limite_de_activos': 'no_desactivarse_a_si_mismo',
                           'inicial': {'rol': 'ADMIN_PLATAFORMA', 'activo': True}, 'textos': {}}

    errores = validar(entidades, codigos)

    for error in ('usuario.pantalla: formulario debe ser encima o al_lado',
                  'usuario.pantalla: limite_de_activos debe ser una regla de validación con parametros.maximo, en '
                  'crear o reactivar',
                  'usuario.pantalla.inicial: rol no puede empezar en ADMIN_PLATAFORMA, que el formulario no ofrece',
                  'usuario.pantalla.inicial: activo no es un campo del formulario'):
        assert error in errores

    usuario['pantalla'] = {'formulario': 'al_lado', 'limite_de_activos': 'maximo_usuarios_activos',
                           'inicial': {'rol': 'USUARIO_FINAL'}, 'textos': {}}

    assert not [e for e in validar(entidades, codigos) if e.startswith('usuario.pantalla')]

    del usuario['atributo_activo']

    assert 'usuario.pantalla: limite_de_activos necesita atributo_activo' in validar(entidades, codigos)


@pytest.mark.caso('RP-23 · en_la_fila_propia va solo en usuario, la entidad de quien tiene la sesión: en una regla de desactivar sin atributo o en una de editar atada a un atributo del formulario')
def test_en_la_fila_propia(entidades, codigos):
    entidades['empresa']['reglas'][0]['en_la_fila_propia'] = True
    reglas = {r['codigo']: r for r in entidades['usuario']['reglas']}
    for codigo in ('no_desactivarse_a_si_mismo', 'no_cambiar_su_propio_rol', 'maximo_usuarios_activos'):
        reglas[codigo]['en_la_fila_propia'] = True
    entidades['usuario']['reglas'].append({
        'codigo': 'no_cambiar_sus_intentos', 'tipo': 'validacion', 'cuando': ['editar'], 'atributo': 'intentos_fallidos',
        'en_la_fila_propia': True, 'descripcion': 'x', 'mensaje': {'es': 'x', 'en': 'x'}, 'fichas': ['RF-03']})

    errores = validar(entidades, codigos)

    for error in ('empresa.reglas.nit_digito_verificacion: en_la_fila_propia va solo en usuario, la entidad de quien '
                  'tiene la sesión',
                  'usuario.reglas.maximo_usuarios_activos: en_la_fila_propia, sin atributo, va solo en desactivar',
                  'usuario.reglas.no_cambiar_sus_intentos: en_la_fila_propia necesita un atributo del formulario, al '
                  'editar'):
        assert error in errores
    for valida in ('no_desactivarse_a_si_mismo', 'no_cambiar_su_propio_rol'):
        assert not [e for e in errores if e.startswith(f'usuario.reglas.{valida}')], valida


@pytest.mark.caso('RP-23 · el resumen y el texto de los activos pueden usar {maximo}, el de la regla de limite_de_activos, solo si la pantalla la declara')
def test_maximo_en_el_resumen(entidades, codigos):
    entidades['empresa']['pantalla']['textos']['resumen'] = {'es': '{activos} de {maximo}.', 'en': '{activos} of {maximo}.'}
    entidades['usuario']['pantalla'] = {'limite_de_activos': 'maximo_usuarios_activos', 'textos': {
        'resumen': {'es': '{activos}, contándote a ti.', 'en': '{activos}, counting you.'},
        'resumen_activos': {'es': {'singular': '{cantidad} de {maximo}', 'plural': '{cantidad} de {maximo}'},
                            'en': {'singular': '{cantidad} of {maximo}', 'plural': '{cantidad} of {maximo}'}}}}

    errores = validar(entidades, codigos)

    assert 'empresa.pantalla.textos.resumen: usa {maximo}, que no existe en este texto' in errores
    assert not [e for e in errores if e.startswith('usuario.pantalla')]


@pytest.mark.caso('RP-23 · la validación conoce los textos de la fila propia y de la contraseña al editar; la nota de la contraseña de otra persona usa los valores de la fila, como {nombre}')
def test_textos_de_la_fila_propia_y_de_la_contrasena(entidades, codigos):
    entidades['usuario']['pantalla'] = {'textos': {
        'fila_propia': {'es': 'Tú', 'en': 'You'},
        'contrasena_al_editar': {'es': 'Contraseña nueva', 'en': 'New password'},
        'ayuda_contrasena_al_editar': {'es': 'Déjala vacía para no cambiarla.', 'en': 'Leave it empty to keep it.'},
        'nota_contrasena_de_otro': {'es': 'Compártela con {nombre} y {jefe}.', 'en': 'Share it with {nombre} and {jefe}.'}}}

    errores = [e for e in validar(entidades, codigos) if e.startswith('usuario.pantalla')]

    assert errores == ['usuario.pantalla.textos.nota_contrasena_de_otro: usa {jefe}, que no existe en este texto']


@pytest.mark.caso('RP-23 · varias_lineas va solo en un texto del formulario, y solo con true')
def test_varias_lineas(entidades, codigos):
    atributo(entidades['rango_recomendacion'], 'limite_inferior')['varias_lineas'] = True
    atributo(entidades['lote'], 'observaciones')['varias_lineas'] = True
    atributo(entidades['usuario'], 'nombre')['varias_lineas'] = 'si'

    errores = validar(entidades, codigos)

    for error in ('rango_recomendacion.limite_inferior: varias_lineas va solo en un texto del formulario',
                  'lote.observaciones: varias_lineas va solo en un texto del formulario',
                  'usuario.nombre: varias_lineas solo puede ser true'):
        assert error in errores
    assert not [e for e in errores if e.startswith('rango_recomendacion.recomendacion')]


@pytest.mark.caso('RP-23 · la validación conoce los textos de la pantalla en bloque: la nota y el detalle de restaurar y el mensaje al restaurar, sin los valores de una fila')
def test_textos_de_la_pantalla_en_bloque(entidades, codigos):
    entidades['rango_recomendacion']['pantalla'] = {'icono': 'rangos', 'textos': {
        'nota_restaurar': {'es': 'Vuelve a los de la NTC 1252:2021.', 'en': 'Back to the NTC 1252:2021 ones.'},
        'confirmar_restaurar_detalle': {'es': 'Se pierden los textos propios.', 'en': 'Your own texts are lost.'},
        'al_restaurar': {'es': 'Se restauraron los de {categoria}.', 'en': '{categoria} were restored.'}}}

    errores = [e for e in validar(entidades, codigos) if e.startswith('rango_recomendacion.pantalla')]

    assert errores == ['rango_recomendacion.pantalla.textos.al_restaurar: usa {categoria}, que no existe en este texto']

