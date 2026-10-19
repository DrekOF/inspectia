# Nombre: crear-admin-plataforma.ps1
# Descripción: Crea el primer administrador de la plataforma (RF-01, RP-15): corre
#              crear-admin-plataforma.sql con psql dentro del contenedor de la base. Para Windows.
# Fecha de creación: 13/10/2026
# Actualización: 19/10/2026
# Autor: Equipo InspectIA
# Versión: 1.1
#
# Uso, con la base encendida y después de arrancar el backend una vez (Flyway crea las tablas):
#   powershell -ExecutionPolicy Bypass -File infra\bd\crear-admin-plataforma.ps1
#
# Datos: INSPECTIA_ADMIN_NOMBRE, INSPECTIA_ADMIN_CORREO e INSPECTIA_ADMIN_CONTRASENA. docker compose los
# toma del entorno o, si no están, de infra\.env. Los valores no pasan por la línea de comandos: con
# «-e NOMBRE», sin valor, docker compose copia la variable al contenedor.
#
# El archivo se guarda en UTF-8 con BOM: sin él, Windows PowerShell 5.1 lee mal las tildes.

$infra = Split-Path -Parent $PSScriptRoot
$compose = Join-Path $infra 'docker-compose.yml'

$servicios = docker compose -f $compose ps --status running --services
if ($servicios -notcontains 'postgres') {
    [Console]::Error.WriteLine('La base no está encendida. Enciéndela con: docker compose -f infra/docker-compose.yml up -d')
    exit 1
}

# Las tildes van y vienen en UTF-8, y al terminar todo queda como estaba:
#   - Lo que va a psql: Windows PowerShell 5.1 le pasa el texto a los programas en ASCII si no se le
#     indica otra cosa, y las tildes del .sql llegarían como «?». Se cambia $OutputEncoding en el ámbito
#     global, porque si el script se llama desde otro, o desde la consola con «.\», el de su propio
#     ámbito no cuenta.
#   - Lo que vuelve de psql: si PowerShell captura esa salida (en PowerShell ISE, en una variable o con
#     Tee-Object), la lee con la página de códigos de la consola. Mientras corre psql, la consola lee en
#     UTF-8. Sin consola no hay nada que cambiar.
$entradaAnterior = $global:OutputEncoding
$salidaAnterior = $null
try {
    $salidaAnterior = [Console]::OutputEncoding
    [Console]::OutputEncoding = New-Object System.Text.UTF8Encoding $false
} catch {
    $salidaAnterior = $null
}
$codigo = 1
try {
    $global:OutputEncoding = New-Object System.Text.UTF8Encoding $false
    Get-Content -LiteralPath (Join-Path $PSScriptRoot 'crear-admin-plataforma.sql') -Raw -Encoding UTF8 |
        docker compose -f $compose exec -T `
            -e INSPECTIA_ADMIN_NOMBRE -e INSPECTIA_ADMIN_CORREO -e INSPECTIA_ADMIN_CONTRASENA `
            -e PGCLIENTENCODING=UTF8 `
            postgres sh -c 'exec psql -X -q -U $POSTGRES_USER -d $POSTGRES_DB'
    $codigo = $LASTEXITCODE
} finally {
    $global:OutputEncoding = $entradaAnterior
    if ($salidaAnterior) {
        [Console]::OutputEncoding = $salidaAnterior
    }
}
exit $codigo
