# Nombre: restablecer-admin-plataforma.ps1
# Descripción: Restablece la contraseña del administrador de la plataforma (D-30, RF-18): pide la
#              contraseña nueva por teclado y corre restablecer-admin-plataforma.sql con psql dentro del
#              contenedor de la base. Para Windows.
# Fecha de creación: 19/10/2026
# Actualización: 19/10/2026
# Autor: Equipo InspectIA
# Versión: 1.0
#
# Uso, con la base encendida:
#   powershell -ExecutionPolicy Bypass -File infra\bd\restablecer-admin-plataforma.ps1
#
# Datos:
#   - El correo sale de INSPECTIA_ADMIN_CORREO, del entorno o de infra\.env. Si no está en ninguno, lo pide
#     por teclado y se ve al escribirlo.
#   - La contraseña nueva siempre se pide por teclado, dos veces y sin mostrarla: no queda en ningún archivo
#     ni en el historial de la consola. Viaja en INSPECTIA_ADMIN_CONTRASENA_NUEVA, una variable que nunca
#     está en infra\.env: si docker compose no encuentra una variable en el entorno, la busca allí, y con
#     INSPECTIA_ADMIN_CONTRASENA tomaría la contraseña del script de creación. PowerShell borra una
#     variable de entorno si se le asigna un texto vacío.
# Los valores no pasan por la línea de comandos: con «-e NOMBRE», sin valor, docker compose copia la
# variable al contenedor.
#
# El archivo se guarda en UTF-8 con BOM: sin él, Windows PowerShell 5.1 lee mal las tildes.

$infra = Split-Path -Parent $PSScriptRoot
$compose = Join-Path $infra 'docker-compose.yml'

$servicios = docker compose -f $compose ps --status running --services
if ($servicios -notcontains 'postgres') {
    [Console]::Error.WriteLine('La base no está encendida. Enciéndela con: docker compose -f infra/docker-compose.yml up -d')
    exit 1
}

# El valor de una variable en infra\.env, sin las comillas que lo rodeen; vacío si no está.
function ValorEnEnv([string] $nombre) {
    $archivo = Join-Path $infra '.env'
    if (-not (Test-Path -LiteralPath $archivo)) {
        return ''
    }
    $linea = Get-Content -LiteralPath $archivo -Encoding UTF8 | Where-Object { $_.StartsWith("$nombre=") } |
        Select-Object -Last 1
    if (-not $linea) {
        return ''
    }
    $valor = $linea.Substring($nombre.Length + 1).Trim()
    if ($valor -match "^'(.*)'$" -or $valor -match '^"(.*)"$') {
        $valor = $Matches[1]
    }
    return $valor
}

# Lee una línea sin mostrarla en pantalla.
function LeerSinMostrar([string] $pregunta) {
    $segura = Read-Host -Prompt $pregunta -AsSecureString
    $puntero = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($segura)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($puntero)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($puntero)
    }
}

$correo = $env:INSPECTIA_ADMIN_CORREO
if (-not $correo) {
    $correo = ValorEnEnv 'INSPECTIA_ADMIN_CORREO'
}
if (-not $correo) {
    $correo = Read-Host -Prompt 'Correo del administrador de la plataforma'
}

$contrasena = LeerSinMostrar 'Contraseña nueva'
# -cne distingue mayúsculas: -ne no las distingue.
if ($contrasena -cne (LeerSinMostrar 'Repítela')) {
    [Console]::Error.WriteLine('Las dos contraseñas no coinciden: no se cambió nada.')
    exit 1
}

# PowerShell 5.1 pasa el texto a los programas en ASCII si no se le indica otra cosa: las tildes del
# script llegarían como «?».
$OutputEncoding = New-Object System.Text.UTF8Encoding $false

$env:INSPECTIA_ADMIN_CORREO = $correo
$env:INSPECTIA_ADMIN_CONTRASENA_NUEVA = $contrasena
try {
    Get-Content -LiteralPath (Join-Path $PSScriptRoot 'restablecer-admin-plataforma.sql') -Raw -Encoding UTF8 |
        docker compose -f $compose exec -T `
            -e INSPECTIA_ADMIN_CORREO -e INSPECTIA_ADMIN_CONTRASENA_NUEVA `
            -e PGCLIENTENCODING=UTF8 `
            postgres sh -c 'exec psql -X -q -U $POSTGRES_USER -d $POSTGRES_DB'
    $codigo = $LASTEXITCODE
} finally {
    Remove-Item Env:INSPECTIA_ADMIN_CONTRASENA_NUEVA -ErrorAction SilentlyContinue
}
exit $codigo
