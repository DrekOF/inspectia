-- Nombre: restablecer-admin-plataforma.sql
-- Descripción: Restablece la contraseña del administrador de la plataforma cuando la olvida (D-30,
--              RF-18). Script de psql que toma los datos de variables de entorno, para que la contraseña
--              no quede en el repositorio ni en la línea de comandos.
-- Fecha de creación: 19/10/2026
-- Actualización: 26/10/2026
-- Autor: Equipo InspectIA
-- Versión: 1.1
--
-- Uso: no se corre directamente. Lo corren restablecer-admin-plataforma.sh (Linux, macOS, Git Bash y la
-- EC2) o restablecer-admin-plataforma.ps1 (Windows), con psql dentro del contenedor de la base. Los
-- envoltorios piden la contraseña nueva por teclado, sin mostrarla, y la pasan en una variable de entorno.
--
-- Variables de entorno: INSPECTIA_ADMIN_CORREO y, con la contraseña nueva, INSPECTIA_ADMIN_CONTRASENA_NUEVA.
-- La contraseña nueva va en una variable propia, que nunca está en infra/.env: si docker compose no
-- encuentra una variable en el entorno, la busca allí, y con INSPECTIA_ADMIN_CONTRASENA tomaría la
-- contraseña del script de creación.
--
-- Requisitos: psql 15 o posterior (\getenv), un usuario superusuario, como el de la imagen de
-- PostgreSQL, y el esquema de Flyway.
--
-- Qué hace, en una sola transacción:
--   1. Valida la contraseña nueva con las reglas de specs/entidades/usuario.json: de 8 a 64 caracteres, como
--      máximo 72 bytes y no solo espacios. Si cambian esas reglas, cambia también este script:
--      backend/src/test/java/co/edu/usco/inspectia/RestablecerAdminPlataformaIT.java prueba los límites.
--   2. Busca al usuario por su correo, sin distinguir mayúsculas ni espacios al inicio o al final. Si no
--      existe, o si es de una empresa, se detiene: esas contraseñas se restablecen desde la app (D-30).
--   3. Guarda la contraseña en hash BCrypt de pgcrypto ($2a$, costo 10), el formato que verifica Spring
--      Security, y aplica lo mismo que la regla cortar_sesiones_al_cambiar_contrasena (D-30):
--      version_sesion sube en uno, así las sesiones abiertas dejan de servir, y los intentos fallidos
--      vuelven a cero, así la cuenta queda sin bloqueo. No reactiva nada.
--
-- El corte no depende de la hora: el backend compara la versión de sesión de cada token con la de la
-- base, así que no importa si el reloj de la base y el del backend difieren (RNF-04 C6).
--
-- La contraseña no sale en pantalla ni en el registro de PostgreSQL: va en una sola sentencia, que no
-- puede fallar y corre con el registro de sentencias apagado. El bloque DO la lee de una tabla temporal
-- que desaparece al terminar la transacción.

\set ON_ERROR_STOP on
\set ECHO none
\set QUIET on
\set VERBOSITY terse

-- \getenv no cambia la variable de psql si la de entorno no existe: así queda vacía y el bloque DO
-- dice cuál falta.
\set admin_correo ''
\set admin_contrasena ''
\getenv admin_correo INSPECTIA_ADMIN_CORREO
\getenv admin_contrasena INSPECTIA_ADMIN_CONTRASENA_NUEVA

BEGIN;

-- Por si el servidor registra las sentencias, el registro se apaga en esta transacción antes de la
-- sentencia que lleva la contraseña.
SET LOCAL log_statement = 'none';
SET LOCAL log_min_duration_statement = -1;

-- Sin el aviso «extension already exists» cuando pgcrypto ya está instalada.
SET LOCAL client_min_messages = warning;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TEMP TABLE datos_admin (correo text, contrasena text) ON COMMIT DROP;
INSERT INTO datos_admin VALUES (:'admin_correo', :'admin_contrasena');

SET LOCAL client_min_messages = notice;

DO $$
DECLARE
    correo_admin     text;
    contrasena_admin text;
    id_admin         uuid;
    rol_admin        text;
BEGIN
    IF to_regclass('usuario') IS NULL THEN
        RAISE EXCEPTION 'No existe la tabla usuario: arranca el backend una vez para que Flyway cree el esquema y vuelve a correr este script.';
    END IF;

    SELECT lower(btrim(d.correo)), d.contrasena
      INTO correo_admin, contrasena_admin
      FROM datos_admin d;

    -- U+FFFD reemplaza los caracteres que no venían en UTF-8, por ejemplo una eñe escrita en una consola
    -- que no usa UTF-8. Se guardaría una contraseña distinta de la que escribió la persona.
    IF strpos(correo_admin || contrasena_admin, U&'\FFFD') > 0 THEN
        RAISE EXCEPTION 'Hay caracteres que no se pudieron leer: revisa que la consola e infra/.env usen UTF-8.';
    END IF;

    IF correo_admin = '' THEN
        RAISE EXCEPTION 'Falta INSPECTIA_ADMIN_CORREO: escribe el correo del administrador.';
    END IF;

    -- Los mismos mensajes de usuario.json. Una contraseña de solo espacios (o tabuladores) cuenta como
    -- si faltara, como en la API: tampoco serviría para iniciar sesión.
    IF contrasena_admin !~ '[^[:space:]]' THEN
        RAISE EXCEPTION 'Falta la contraseña nueva.';
    ELSIF char_length(contrasena_admin) < 8 THEN
        RAISE EXCEPTION 'La contraseña debe tener al menos 8 caracteres.';
    ELSIF char_length(contrasena_admin) > 64 THEN
        RAISE EXCEPTION 'La contraseña puede tener como máximo 64 caracteres.';
    ELSIF octet_length(contrasena_admin) > 72 THEN
        RAISE EXCEPTION 'La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes';
    END IF;

    SELECT u.id, u.rol
      INTO id_admin, rol_admin
      FROM usuario u
     WHERE lower(btrim(u.correo)) = correo_admin;

    IF id_admin IS NULL THEN
        RAISE EXCEPTION 'No hay ningún administrador de la plataforma con el correo %.', correo_admin;
    ELSIF rol_admin <> 'ADMIN_PLATAFORMA' THEN
        RAISE EXCEPTION 'El correo % es de un usuario de una empresa: su contraseña se restablece desde la app (D-30).', correo_admin;
    END IF;

    UPDATE usuario
       SET contrasena_hash = crypt(contrasena_admin, gen_salt('bf', 10)),
           version_sesion = version_sesion + 1,
           intentos_fallidos = 0,
           bloqueado_hasta = NULL
     WHERE id = id_admin;

    RAISE NOTICE 'Contraseña restablecida para %: sus sesiones abiertas dejaron de servir y su cuenta quedó sin bloqueo.', correo_admin;
END
$$;

COMMIT;
