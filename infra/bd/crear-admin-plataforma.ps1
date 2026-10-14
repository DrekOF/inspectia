# Nombre: crear-admin-plataforma.ps1
# Descripción: Crea el primer administrador de la plataforma (RF-01, RP-15): corre
#              crear-admin-plataforma.sql con psql dentro del contenedor de la base. Para Windows.
# Fecha de creación: 13/10/2026
# Actualización: 13/10/2026
# Autor: Equipo InspectIA
# Versión: 1.0
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

# PowerShell 5.1 pasa el texto a los programas en ASCII si no se le indica otra cosa: las tildes del
# script llegarían como «?».
$OutputEncoding = New-Object System.Text.UTF8Encoding $false

Get-Content -LiteralPath (Join-Path $PSScriptRoot 'crear-admin-plataforma.sql') -Raw -Encoding UTF8 |
    docker compose -f $compose exec -T `
        -e INSPECTIA_ADMIN_NOMBRE -e INSPECTIA_ADMIN_CORREO -e INSPECTIA_ADMIN_CONTRASENA `
        -e PGCLIENTENCODING=UTF8 `
        postgres sh -c 'exec psql -X -q -U $POSTGRES_USER -d $POSTGRES_DB'
exit $LASTEXITCODE
