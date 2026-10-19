-- Nombre: crear-admin-plataforma.sql
-- Descripción: Crea el primer administrador de la plataforma (RF-01, D-13, RP-15). Script de psql que
--              toma los datos de variables de entorno, para que la contraseña no quede en el repositorio
--              ni en la línea de comandos.
-- Fecha de creación: 13/10/2026
-- Actualización: 18/10/2026
-- Autor: Equipo InspectIA
-- Versión: 1.1
--
-- Uso: no se corre directamente. Lo corren crear-admin-plataforma.sh (Linux, macOS, Git Bash y la EC2)
-- o crear-admin-plataforma.ps1 (Windows), con psql dentro del contenedor de la base.
--
-- Variables de entorno: INSPECTIA_ADMIN_NOMBRE, INSPECTIA_ADMIN_CORREO e INSPECTIA_ADMIN_CONTRASENA.
--
-- Requisitos: psql 15 o posterior (\getenv), un usuario superusuario, como el de la imagen de
-- PostgreSQL, y el esquema de Flyway: el backend tiene que haber arrancado una vez con esta base.
--
-- Qué hace, en una sola transacción:
--   1. Valida los datos con las reglas de specs/entidades/usuario.json: nombre de hasta 120 caracteres;
--      correo válido, sin espacios al inicio ni al final y en minúsculas; contraseña de 8 a 64
--      caracteres y de máximo 72 bytes (RF-02). Una contraseña de solo espacios cuenta como si faltara,
--      como en la API. Si cambian esas reglas, cambia también este script:
--      backend/src/test/java/co/edu/usco/inspectia/CrearAdminPlataformaIT.java prueba los límites.
--   2. Si el administrador ya existe, no cambia nada, así que se puede correr varias veces. Si el correo
--      es de un usuario de una empresa, se detiene.
--   3. Crea el usuario ADMIN_PLATAFORMA sin empresa (RNF-03), con la contraseña en hash BCrypt de
--      pgcrypto ($2a$, costo 10), el formato que verifica Spring Security.
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
\set admin_nombre ''
\set admin_correo ''
\set admin_contrasena ''
\getenv admin_nombre INSPECTIA_ADMIN_NOMBRE
\getenv admin_correo INSPECTIA_ADMIN_CORREO
\getenv admin_contrasena INSPECTIA_ADMIN_CONTRASENA

BEGIN;

-- Por si el servidor registra las sentencias, el registro se apaga en esta transacción antes de la
-- sentencia que lleva la contraseña.
SET LOCAL log_statement = 'none';
SET LOCAL log_min_duration_statement = -1;

-- Sin el aviso «extension already exists» cuando pgcrypto ya está instalada.
SET LOCAL client_min_messages = warning;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TEMP TABLE datos_admin (nombre text, correo text, contrasena text) ON COMMIT DROP;
INSERT INTO datos_admin VALUES (:'admin_nombre', :'admin_correo', :'admin_contrasena');

SET LOCAL client_min_messages = notice;

DO $$
DECLARE
    nombre_admin     text;
    correo_admin     text;
    contrasena_admin text;
    rol_existente    text;
BEGIN
    IF to_regclass('usuario') IS NULL THEN
        RAISE EXCEPTION 'No existe la tabla usuario: arranca el backend una vez para que Flyway cree el esquema y vuelve a correr este script.';
    END IF;

    SELECT btrim(d.nombre), lower(btrim(d.correo)), d.contrasena
      INTO nombre_admin, correo_admin, contrasena_admin
      FROM datos_admin d;

    -- U+FFFD reemplaza los caracteres que no venían en UTF-8, por ejemplo una eñe de un .env
    -- guardado en ANSI. Se guardaría una contraseña distinta de la que escribió la persona.
    IF strpos(nombre_admin || correo_admin || contrasena_admin, U&'\FFFD') > 0 THEN
        RAISE EXCEPTION 'Hay caracteres que no se pudieron leer: guarda infra/.env con codificación UTF-8.';
    END IF;

    IF nombre_admin = '' THEN
        RAISE EXCEPTION 'Falta INSPECTIA_ADMIN_NOMBRE: escribe el nombre del administrador.';
    ELSIF char_length(nombre_admin) > 120 THEN
        RAISE EXCEPTION 'El nombre puede tener como máximo 120 caracteres.';
    END IF;

    IF correo_admin = '' THEN
        RAISE EXCEPTION 'Falta INSPECTIA_ADMIN_CORREO: escribe el correo del administrador.';
    ELSIF char_length(correo_admin) > 254
          OR correo_admin !~ '^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$' THEN
        RAISE EXCEPTION 'INSPECTIA_ADMIN_CORREO no es un correo válido.';
    END IF;

    -- Los mismos mensajes de usuario.json. Una contraseña de solo espacios (o tabuladores) cuenta como
    -- si faltara, como en la API: tampoco serviría para iniciar sesión.
    IF contrasena_admin !~ '[^[:space:]]' THEN
        RAISE EXCEPTION 'Falta INSPECTIA_ADMIN_CONTRASENA: escribe la contraseña inicial.';
    ELSIF char_length(contrasena_admin) < 8 THEN
        RAISE EXCEPTION 'La contraseña debe tener al menos 8 caracteres.';
    ELSIF char_length(contrasena_admin) > 64 THEN
        RAISE EXCEPTION 'La contraseña puede tener como máximo 64 caracteres.';
    ELSIF octet_length(contrasena_admin) > 72 THEN
        RAISE EXCEPTION 'La contraseña es demasiado larga: usa menos caracteres o menos tildes y eñes';
    END IF;

    SELECT u.rol INTO rol_existente
      FROM usuario u
     WHERE lower(btrim(u.correo)) = correo_admin;

    IF rol_existente = 'ADMIN_PLATAFORMA' THEN
        RAISE NOTICE 'El administrador de la plataforma % ya existe: no se cambió nada.', correo_admin;
        RETURN;
    ELSIF rol_existente IS NOT NULL THEN
        RAISE EXCEPTION 'El correo % ya es de un usuario de una empresa: usa otro correo.', correo_admin;
    END IF;

    INSERT INTO usuario (nombre, correo, contrasena_hash, rol)
    VALUES (nombre_admin, correo_admin, crypt(contrasena_admin, gen_salt('bf', 10)), 'ADMIN_PLATAFORMA');

    RAISE NOTICE 'Administrador de la plataforma creado: %.', correo_admin;
END
$$;

COMMIT;
