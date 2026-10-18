-- Nombre: V1__esquema_inicial.sql
-- Descripción: Crea las tablas de InspectIA desde specs/entidades (RP-15). Archivo generado por
--              generador/ (RP-23): no se edita a mano; se cambia el JSON o la plantilla y se regenera.
-- Fecha de creación: 13/10/2026
-- Actualización: 18/10/2026
-- Autor: Equipo InspectIA (generado por generador/ v1.5)
-- Versión: 1.5
-- Entidades: empresa 1.2, foto 1.0, grano 1.0, lote 1.0, modelo 1.0, muestra 1.0, rango_recomendacion 1.1, usuario 1.5

-- Mantiene fecha_actualizacion en cada cambio, también cuando lo hace un script.
CREATE FUNCTION actualizar_fecha_actualizacion() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    NEW.fecha_actualizacion := now();
    RETURN NEW;
END;
$$;

-- empresa -----------------------------------------------------------------------------------------

CREATE TABLE empresa (
    id uuid NOT NULL DEFAULT uuidv7(),
    nombre varchar(120) NOT NULL,
    nit varchar(15) NOT NULL,
    ciudad varchar(80) NOT NULL,
    activa boolean NOT NULL DEFAULT true,
    fecha_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_actualizacion timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT empresa_pk PRIMARY KEY (id),
    CONSTRAINT empresa_nit_unico UNIQUE (nit),
    CONSTRAINT empresa_nit_patron CHECK (nit ~ '^[0-9]{6,10}-[0-9]$')
);
CREATE TRIGGER empresa_fecha_actualizacion BEFORE UPDATE ON empresa
    FOR EACH ROW EXECUTE FUNCTION actualizar_fecha_actualizacion();
COMMENT ON TABLE empresa IS 'Empresa cliente de InspectIA. Sus usuarios, rangos, lotes y resultados están separados de los de las demás empresas (RNF-03). No se borra: se desactiva (D-27).';
COMMENT ON COLUMN empresa.id IS 'Identificador (UUID v7)';
COMMENT ON COLUMN empresa.nombre IS 'Nombre (RF-02)';
COMMENT ON COLUMN empresa.nit IS 'NIT (RF-02, D-27)';
COMMENT ON COLUMN empresa.ciudad IS 'Ciudad (RF-02)';
COMMENT ON COLUMN empresa.activa IS 'Activa (D-27, RF-01)';
COMMENT ON COLUMN empresa.fecha_creacion IS 'Fecha de creación del registro';
COMMENT ON COLUMN empresa.fecha_actualizacion IS 'Fecha de la última actualización; la mantiene un trigger';

-- modelo ------------------------------------------------------------------------------------------

CREATE TABLE modelo (
    id uuid NOT NULL DEFAULT uuidv7(),
    nombre varchar(80) NOT NULL,
    version varchar(20) NOT NULL,
    tipo varchar(30),
    f1_macro numeric(5,2),
    precision_macro numeric(5,2),
    recall_macro numeric(5,2),
    tiempo_por_grano_ms numeric(8,2),
    en_uso boolean NOT NULL DEFAULT false,
    fecha_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_actualizacion timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT modelo_pk PRIMARY KEY (id),
    CONSTRAINT modelo_nombre_version_unico UNIQUE (nombre, version),
    CONSTRAINT modelo_tipo_valores CHECK (tipo IN ('CNN', 'TRANSFORMER')),
    CONSTRAINT modelo_f1_macro_rango CHECK (f1_macro BETWEEN 0 AND 100),
    CONSTRAINT modelo_precision_macro_rango CHECK (precision_macro BETWEEN 0 AND 100),
    CONSTRAINT modelo_recall_macro_rango CHECK (recall_macro BETWEEN 0 AND 100),
    CONSTRAINT modelo_tiempo_por_grano_ms_minimo CHECK (tiempo_por_grano_ms >= 0),
    CONSTRAINT modelo_datos_completos CHECK (num_nonnulls(tipo, f1_macro, precision_macro, recall_macro, tiempo_por_grano_ms) IN (0, 5))
);
CREATE UNIQUE INDEX modelo_en_uso_unico ON modelo (en_uso) WHERE en_uso;
CREATE TRIGGER modelo_fecha_actualizacion BEFORE UPDATE ON modelo
    FOR EACH ROW EXECUTE FUNCTION actualizar_fecha_actualizacion();
COMMENT ON TABLE modelo IS 'Modelo de IA. La tabla guarda la comparación de D-18: los 5 modelos de Julián (2 transformers y 3 CNN, RP-25), con el elegido marcado en uso. Es común a todas las empresas (RNF-03). Las filas las carga el script de base de datos (RP-15); en desarrollo, también el clasificador falso, sin métricas.';
COMMENT ON COLUMN modelo.id IS 'Identificador (UUID v7)';
COMMENT ON COLUMN modelo.nombre IS 'Nombre (RF-08, RF-17)';
COMMENT ON COLUMN modelo.version IS 'Versión (RF-08, RF-17)';
COMMENT ON COLUMN modelo.tipo IS 'Tipo (D-18, RP-25)';
COMMENT ON COLUMN modelo.f1_macro IS 'F1-score macro (%) (RF-17, RNF-07)';
COMMENT ON COLUMN modelo.precision_macro IS 'Precisión macro (%) (RF-17, RNF-07)';
COMMENT ON COLUMN modelo.recall_macro IS 'Recall macro (%) (RF-17, RNF-07)';
COMMENT ON COLUMN modelo.tiempo_por_grano_ms IS 'Tiempo por grano (ms) (D-18)';
COMMENT ON COLUMN modelo.en_uso IS 'En uso (RF-17, D-18)';
COMMENT ON COLUMN modelo.fecha_creacion IS 'Fecha de creación del registro';
COMMENT ON COLUMN modelo.fecha_actualizacion IS 'Fecha de la última actualización; la mantiene un trigger';

-- rango_recomendacion -----------------------------------------------------------------------------

CREATE TABLE rango_recomendacion (
    id uuid NOT NULL DEFAULT uuidv7(),
    empresa_id uuid NOT NULL,
    categoria varchar(30) NOT NULL,
    limite_inferior numeric(4,1) NOT NULL,
    recomendacion varchar(300) NOT NULL,
    fecha_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_actualizacion timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT rango_recomendacion_pk PRIMARY KEY (id),
    CONSTRAINT rango_recomendacion_empresa_id_categoria_unico UNIQUE (empresa_id, categoria),
    CONSTRAINT rango_recomendacion_categoria_valores CHECK (categoria IN ('PREMIUM', 'ESTANDAR', 'CORRIENTE', 'FUERA_DE_NORMA')),
    CONSTRAINT rango_recomendacion_limite_inferior_rango CHECK (limite_inferior BETWEEN 0 AND 100),
    CONSTRAINT rango_limite_fuera_de_norma CHECK ((categoria = 'FUERA_DE_NORMA') = (limite_inferior = 0)),
    CONSTRAINT rango_recomendacion_empresa_id_fk FOREIGN KEY (empresa_id) REFERENCES empresa (id) ON DELETE RESTRICT
);
CREATE TRIGGER rango_recomendacion_fecha_actualizacion BEFORE UPDATE ON rango_recomendacion
    FOR EACH ROW EXECUTE FUNCTION actualizar_fecha_actualizacion();
COMMENT ON TABLE rango_recomendacion IS 'Rango de una categoría probable: desde qué porcentaje de granos sanos aplica y qué se recomienda (D-10, anexo A.3). Cada empresa tiene siempre las 4 categorías; solo cambian sus límites y sus textos (RF-13).';
COMMENT ON COLUMN rango_recomendacion.id IS 'Identificador (UUID v7)';
COMMENT ON COLUMN rango_recomendacion.empresa_id IS 'Empresa dueña del registro (RNF-03)';
COMMENT ON COLUMN rango_recomendacion.categoria IS 'Categoría (RF-12, RF-13)';
COMMENT ON COLUMN rango_recomendacion.limite_inferior IS 'Límite inferior (% de granos sanos) (RF-12, RF-13)';
COMMENT ON COLUMN rango_recomendacion.recomendacion IS 'Recomendación (RF-12, RF-13)';
COMMENT ON COLUMN rango_recomendacion.fecha_creacion IS 'Fecha de creación del registro';
COMMENT ON COLUMN rango_recomendacion.fecha_actualizacion IS 'Fecha de la última actualización; la mantiene un trigger';

-- usuario -----------------------------------------------------------------------------------------

CREATE TABLE usuario (
    id uuid NOT NULL DEFAULT uuidv7(),
    empresa_id uuid,
    nombre varchar(120) NOT NULL,
    correo varchar(254) NOT NULL,
    contrasena_hash varchar(100) NOT NULL,
    rol varchar(30) NOT NULL,
    activo boolean NOT NULL DEFAULT true,
    intentos_fallidos integer NOT NULL DEFAULT 0,
    bloqueado_hasta timestamptz,
    sesiones_desde timestamptz,
    fecha_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_actualizacion timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT usuario_pk PRIMARY KEY (id),
    CONSTRAINT usuario_id_empresa_unico UNIQUE (id, empresa_id),
    CONSTRAINT usuario_rol_valores CHECK (rol IN ('USUARIO_FINAL', 'ADMIN_EMPRESA', 'ADMIN_PLATAFORMA')),
    CONSTRAINT usuario_intentos_fallidos_minimo CHECK (intentos_fallidos >= 0),
    CONSTRAINT usuario_empresa_segun_rol CHECK ((rol = 'ADMIN_PLATAFORMA') = (empresa_id IS NULL)),
    CONSTRAINT usuario_empresa_id_fk FOREIGN KEY (empresa_id) REFERENCES empresa (id) ON DELETE RESTRICT
);
CREATE UNIQUE INDEX usuario_correo_unico ON usuario (lower(btrim(correo)));
CREATE INDEX usuario_empresa_id_idx ON usuario (empresa_id);
CREATE TRIGGER usuario_fecha_actualizacion BEFORE UPDATE ON usuario
    FOR EACH ROW EXECUTE FUNCTION actualizar_fecha_actualizacion();
COMMENT ON TABLE usuario IS 'Persona que usa InspectIA, con su rol. Los usuarios no se borran: se desactivan, para no perder quién hizo cada análisis (RF-03). Solo el administrador de la plataforma no tiene empresa (RNF-03).';
COMMENT ON COLUMN usuario.id IS 'Identificador (UUID v7)';
COMMENT ON COLUMN usuario.empresa_id IS 'Empresa dueña del registro (RNF-03)';
COMMENT ON COLUMN usuario.nombre IS 'Nombre (RF-02, RF-03)';
COMMENT ON COLUMN usuario.correo IS 'Correo (RF-01, RF-02, RF-03)';
COMMENT ON COLUMN usuario.contrasena_hash IS 'Contraseña (RF-01, RF-02, RF-03, RNF-04)';
COMMENT ON COLUMN usuario.rol IS 'Rol (RF-01, RF-03, D-05, D-13)';
COMMENT ON COLUMN usuario.activo IS 'Activo (RF-01, RF-03)';
COMMENT ON COLUMN usuario.intentos_fallidos IS 'Intentos fallidos (RF-01, RNF-04)';
COMMENT ON COLUMN usuario.bloqueado_hasta IS 'Bloqueado hasta (RF-01, RNF-04)';
COMMENT ON COLUMN usuario.sesiones_desde IS 'Sesiones válidas desde (RNF-04, D-30)';
COMMENT ON COLUMN usuario.fecha_creacion IS 'Fecha de creación del registro';
COMMENT ON COLUMN usuario.fecha_actualizacion IS 'Fecha de la última actualización; la mantiene un trigger';

-- lote --------------------------------------------------------------------------------------------

CREATE TABLE lote (
    id uuid NOT NULL DEFAULT uuidv7(),
    empresa_id uuid NOT NULL,
    codigo varchar(30) NOT NULL,
    fecha_recepcion date NOT NULL,
    origen varchar(120) NOT NULL,
    observaciones varchar(500),
    estado varchar(30) NOT NULL DEFAULT 'PENDIENTE',
    usuario_id uuid NOT NULL,
    porcentaje_sanos numeric(4,1),
    categoria varchar(30),
    recomendacion varchar(300),
    sugerir_otra_muestra boolean,
    aviso_confianza_baja boolean,
    fecha_resultado timestamptz,
    reporte_ruta varchar(255),
    fecha_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_actualizacion timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT lote_pk PRIMARY KEY (id),
    CONSTRAINT lote_id_empresa_unico UNIQUE (id, empresa_id),
    CONSTRAINT lote_estado_valores CHECK (estado IN ('PENDIENTE', 'ANALIZADO')),
    CONSTRAINT lote_porcentaje_sanos_rango CHECK (porcentaje_sanos BETWEEN 0 AND 100),
    CONSTRAINT lote_categoria_valores CHECK (categoria IN ('PREMIUM', 'ESTANDAR', 'CORRIENTE', 'FUERA_DE_NORMA')),
    CONSTRAINT lote_resultado_segun_estado CHECK ((estado = 'PENDIENTE' AND num_nonnulls(porcentaje_sanos, categoria, recomendacion, sugerir_otra_muestra, aviso_confianza_baja, fecha_resultado, reporte_ruta) = 0) OR (estado = 'ANALIZADO' AND num_nonnulls(porcentaje_sanos, categoria, recomendacion, sugerir_otra_muestra, aviso_confianza_baja, fecha_resultado) = 6)),
    CONSTRAINT lote_empresa_id_fk FOREIGN KEY (empresa_id) REFERENCES empresa (id) ON DELETE RESTRICT,
    CONSTRAINT lote_usuario_id_fk FOREIGN KEY (usuario_id, empresa_id) REFERENCES usuario (id, empresa_id) ON DELETE RESTRICT
);
CREATE UNIQUE INDEX lote_empresa_id_codigo_unico ON lote (empresa_id, lower(btrim(codigo)));
CREATE INDEX lote_usuario_id_idx ON lote (usuario_id);
CREATE INDEX lote_empresa_id_fecha_resultado_idx ON lote (empresa_id, fecha_resultado);
CREATE INDEX lote_empresa_id_origen_idx ON lote (empresa_id, lower(btrim(origen)));
CREATE TRIGGER lote_fecha_actualizacion BEFORE UPDATE ON lote
    FOR EACH ROW EXECUTE FUNCTION actualizar_fecha_actualizacion();
COMMENT ON TABLE lote IS 'Lote de cacao que la empresa analiza. Guarda el resultado del lote: con 2 muestras, el de la suma de sus granos (D-25). Lo maneja el módulo analisis del backend, escrito a mano (D-21).';
COMMENT ON COLUMN lote.id IS 'Identificador (UUID v7)';
COMMENT ON COLUMN lote.empresa_id IS 'Empresa dueña del registro (RNF-03)';
COMMENT ON COLUMN lote.codigo IS 'Código del lote (RF-04)';
COMMENT ON COLUMN lote.fecha_recepcion IS 'Fecha de recepción (RF-04)';
COMMENT ON COLUMN lote.origen IS 'Finca o proveedor (RF-04, RF-16)';
COMMENT ON COLUMN lote.observaciones IS 'Observaciones (RF-04)';
COMMENT ON COLUMN lote.estado IS 'Estado (RF-04, RF-12, RF-14)';
COMMENT ON COLUMN lote.usuario_id IS 'Registrado por (RF-04)';
COMMENT ON COLUMN lote.porcentaje_sanos IS '% de granos sanos (RF-10, D-25)';
COMMENT ON COLUMN lote.categoria IS 'Categoría probable (RF-12, RF-13)';
COMMENT ON COLUMN lote.recomendacion IS 'Recomendación (RF-12, RF-13)';
COMMENT ON COLUMN lote.sugerir_otra_muestra IS 'Sugiere otra muestra (RF-12, D-24, D-25)';
COMMENT ON COLUMN lote.aviso_confianza_baja IS 'Aviso de confianza baja (RF-09, D-25)';
COMMENT ON COLUMN lote.fecha_resultado IS 'Fecha del resultado (RF-14, RF-15, RF-16)';
COMMENT ON COLUMN lote.reporte_ruta IS 'Reporte PDF (llave en S3) (RF-15, D-25)';
COMMENT ON COLUMN lote.fecha_creacion IS 'Fecha de creación del registro';
COMMENT ON COLUMN lote.fecha_actualizacion IS 'Fecha de la última actualización; la mantiene un trigger';

-- muestra -----------------------------------------------------------------------------------------

CREATE TABLE muestra (
    id uuid NOT NULL DEFAULT uuidv7(),
    empresa_id uuid NOT NULL,
    lote_id uuid NOT NULL,
    numero integer NOT NULL,
    usuario_id uuid NOT NULL,
    estado varchar(30) NOT NULL DEFAULT 'PENDIENTE',
    modelo_id uuid,
    total_granos integer,
    granos_sanos integer,
    aviso_confianza_baja boolean,
    repeticiones integer NOT NULL DEFAULT 0,
    umbral_confianza_baja numeric(5,2),
    porcentaje_max_dudosos numeric(4,1),
    fecha_envio timestamptz,
    fecha_inicio timestamptz,
    fecha_fin timestamptz,
    fecha_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_actualizacion timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT muestra_pk PRIMARY KEY (id),
    CONSTRAINT muestra_id_empresa_unico UNIQUE (id, empresa_id),
    CONSTRAINT muestra_lote_id_numero_unico UNIQUE (lote_id, numero),
    CONSTRAINT muestra_numero_rango CHECK (numero BETWEEN 1 AND 2),
    CONSTRAINT muestra_estado_valores CHECK (estado IN ('PENDIENTE', 'EN_ESPERA', 'EN_PROCESO', 'TERMINADA')),
    CONSTRAINT muestra_total_granos_minimo CHECK (total_granos >= 100),
    CONSTRAINT muestra_granos_sanos_minimo CHECK (granos_sanos >= 0),
    CONSTRAINT muestra_repeticiones_rango CHECK (repeticiones BETWEEN 0 AND 2),
    CONSTRAINT muestra_umbral_confianza_baja_rango CHECK (umbral_confianza_baja BETWEEN 0 AND 100),
    CONSTRAINT muestra_porcentaje_max_dudosos_rango CHECK (porcentaje_max_dudosos BETWEEN 0 AND 100),
    CONSTRAINT muestra_terminada_completa CHECK (estado <> 'TERMINADA' OR num_nonnulls(modelo_id, total_granos, granos_sanos, aviso_confianza_baja, umbral_confianza_baja, porcentaje_max_dudosos, fecha_fin) = 7),
    CONSTRAINT muestra_fechas_segun_estado CHECK ((estado = 'PENDIENTE' OR fecha_envio IS NOT NULL) AND (estado NOT IN ('EN_PROCESO', 'TERMINADA') OR fecha_inicio IS NOT NULL)),
    CONSTRAINT muestra_sanos_no_superan_total CHECK (granos_sanos IS NULL OR granos_sanos <= total_granos),
    CONSTRAINT muestra_empresa_id_fk FOREIGN KEY (empresa_id) REFERENCES empresa (id) ON DELETE RESTRICT,
    CONSTRAINT muestra_lote_id_fk FOREIGN KEY (lote_id, empresa_id) REFERENCES lote (id, empresa_id) ON DELETE RESTRICT,
    CONSTRAINT muestra_usuario_id_fk FOREIGN KEY (usuario_id, empresa_id) REFERENCES usuario (id, empresa_id) ON DELETE RESTRICT,
    CONSTRAINT muestra_modelo_id_fk FOREIGN KEY (modelo_id) REFERENCES modelo (id) ON DELETE RESTRICT
);
CREATE INDEX muestra_empresa_id_idx ON muestra (empresa_id);
CREATE INDEX muestra_usuario_id_idx ON muestra (usuario_id);
CREATE INDEX muestra_modelo_id_idx ON muestra (modelo_id);
CREATE TRIGGER muestra_fecha_actualizacion BEFORE UPDATE ON muestra
    FOR EACH ROW EXECUTE FUNCTION actualizar_fecha_actualizacion();
COMMENT ON TABLE muestra IS 'Muestra de un lote: 2 o 3 fotos con 100 granos o más (D-23, D-26). Un lote tiene como máximo 2 muestras, y la segunda solo existe si la app la sugirió (D-25). La maneja el módulo analisis del backend, escrito a mano (D-21).';
COMMENT ON COLUMN muestra.id IS 'Identificador (UUID v7)';
COMMENT ON COLUMN muestra.empresa_id IS 'Empresa dueña del registro (RNF-03)';
COMMENT ON COLUMN muestra.lote_id IS 'Lote (RF-10, D-25)';
COMMENT ON COLUMN muestra.numero IS 'Número de muestra (D-25)';
COMMENT ON COLUMN muestra.usuario_id IS 'Analizada por (RF-14, RF-15)';
COMMENT ON COLUMN muestra.estado IS 'Estado (RF-12, RF-14, RNF-02)';
COMMENT ON COLUMN muestra.modelo_id IS 'Modelo de IA (RF-08, RF-15, D-18)';
COMMENT ON COLUMN muestra.total_granos IS 'Total de granos (RF-07, RF-10, D-26)';
COMMENT ON COLUMN muestra.granos_sanos IS 'Granos sanos (RF-10)';
COMMENT ON COLUMN muestra.aviso_confianza_baja IS 'Aviso de confianza baja (RF-09, D-25)';
COMMENT ON COLUMN muestra.repeticiones IS 'Repeticiones de foto (RF-09)';
COMMENT ON COLUMN muestra.umbral_confianza_baja IS 'Umbral de confianza baja (%) (RF-09, P-15)';
COMMENT ON COLUMN muestra.porcentaje_max_dudosos IS 'Máximo de granos dudosos (%) (RF-09, P-15)';
COMMENT ON COLUMN muestra.fecha_envio IS 'Enviada (RNF-01, RNF-02)';
COMMENT ON COLUMN muestra.fecha_inicio IS 'Inicio del proceso (RNF-02)';
COMMENT ON COLUMN muestra.fecha_fin IS 'Fin del proceso (RNF-01)';
COMMENT ON COLUMN muestra.fecha_creacion IS 'Fecha de creación del registro';
COMMENT ON COLUMN muestra.fecha_actualizacion IS 'Fecha de la última actualización; la mantiene un trigger';

-- foto --------------------------------------------------------------------------------------------

CREATE TABLE foto (
    id uuid NOT NULL DEFAULT uuidv7(),
    empresa_id uuid NOT NULL,
    muestra_id uuid NOT NULL,
    numero integer NOT NULL,
    ruta varchar(255) NOT NULL,
    formato varchar(30) NOT NULL,
    tamano_bytes integer NOT NULL,
    estado varchar(30) NOT NULL DEFAULT 'CARGADA',
    motivo varchar(30),
    fecha_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_actualizacion timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT foto_pk PRIMARY KEY (id),
    CONSTRAINT foto_ruta_unico UNIQUE (ruta),
    CONSTRAINT foto_id_empresa_unico UNIQUE (id, empresa_id),
    CONSTRAINT foto_muestra_id_numero_unico UNIQUE (muestra_id, numero),
    CONSTRAINT foto_numero_rango CHECK (numero BETWEEN 1 AND 3),
    CONSTRAINT foto_formato_valores CHECK (formato IN ('JPG', 'PNG')),
    CONSTRAINT foto_tamano_bytes_rango CHECK (tamano_bytes BETWEEN 1 AND 10485760),
    CONSTRAINT foto_estado_valores CHECK (estado IN ('CARGADA', 'APROBADA', 'POR_REPETIR')),
    CONSTRAINT foto_motivo_valores CHECK (motivo IN ('BORROSA', 'POCA_LUZ', 'GRANOS_PEGADOS', 'GRANOS_DUDOSOS')),
    CONSTRAINT foto_motivo_segun_estado CHECK ((estado = 'POR_REPETIR') = (motivo IS NOT NULL)),
    CONSTRAINT foto_empresa_id_fk FOREIGN KEY (empresa_id) REFERENCES empresa (id) ON DELETE RESTRICT,
    CONSTRAINT foto_muestra_id_fk FOREIGN KEY (muestra_id, empresa_id) REFERENCES muestra (id, empresa_id) ON DELETE RESTRICT
);
CREATE INDEX foto_empresa_id_idx ON foto (empresa_id);
CREATE TRIGGER foto_fecha_actualizacion BEFORE UPDATE ON foto
    FOR EACH ROW EXECUTE FUNCTION actualizar_fecha_actualizacion();
COMMENT ON TABLE foto IS 'Foto de una muestra, guardada en S3. Una foto rechazada o reemplazada se borra de la base de datos y de S3, con sus granos (RF-05). La maneja el módulo analisis del backend, escrito a mano (D-21).';
COMMENT ON COLUMN foto.id IS 'Identificador (UUID v7)';
COMMENT ON COLUMN foto.empresa_id IS 'Empresa dueña del registro (RNF-03)';
COMMENT ON COLUMN foto.muestra_id IS 'Muestra (RF-05, D-25)';
COMMENT ON COLUMN foto.numero IS 'Número de foto (RF-05, RF-09)';
COMMENT ON COLUMN foto.ruta IS 'Llave en S3 (RF-05, D-16)';
COMMENT ON COLUMN foto.formato IS 'Formato (RF-05)';
COMMENT ON COLUMN foto.tamano_bytes IS 'Tamaño (bytes) (RF-05)';
COMMENT ON COLUMN foto.estado IS 'Estado (RF-06, RF-07, RF-09)';
COMMENT ON COLUMN foto.motivo IS 'Motivo para repetir (RF-06, RF-07, RF-09)';
COMMENT ON COLUMN foto.fecha_creacion IS 'Fecha de creación del registro';
COMMENT ON COLUMN foto.fecha_actualizacion IS 'Fecha de la última actualización; la mantiene un trigger';

-- grano -------------------------------------------------------------------------------------------

CREATE TABLE grano (
    id uuid NOT NULL DEFAULT uuidv7(),
    empresa_id uuid NOT NULL,
    foto_id uuid NOT NULL,
    x integer NOT NULL,
    y integer NOT NULL,
    ancho integer NOT NULL,
    alto integer NOT NULL,
    clase varchar(30) NOT NULL,
    confianza numeric(5,2) NOT NULL,
    fecha_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_actualizacion timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT grano_pk PRIMARY KEY (id),
    CONSTRAINT grano_x_minimo CHECK (x >= 0),
    CONSTRAINT grano_y_minimo CHECK (y >= 0),
    CONSTRAINT grano_ancho_minimo CHECK (ancho >= 1),
    CONSTRAINT grano_alto_minimo CHECK (alto >= 1),
    CONSTRAINT grano_clase_valores CHECK (clase IN ('SANO', 'NO_SANO')),
    CONSTRAINT grano_confianza_rango CHECK (confianza BETWEEN 0 AND 100),
    CONSTRAINT grano_empresa_id_fk FOREIGN KEY (empresa_id) REFERENCES empresa (id) ON DELETE RESTRICT,
    CONSTRAINT grano_foto_id_fk FOREIGN KEY (foto_id, empresa_id) REFERENCES foto (id, empresa_id) ON DELETE CASCADE
);
CREATE INDEX grano_empresa_id_idx ON grano (empresa_id);
CREATE INDEX grano_foto_id_idx ON grano (foto_id);
CREATE TRIGGER grano_fecha_actualizacion BEFORE UPDATE ON grano
    FOR EACH ROW EXECUTE FUNCTION actualizar_fecha_actualizacion();
COMMENT ON TABLE grano IS 'Grano encontrado en una foto. La posición es un rectángulo sin rotación, en píxeles de la foto original. Un grano es dudoso si su confianza es menor que el umbral copiado en su muestra (RF-09); eso no se guarda. Lo maneja el módulo analisis del backend, escrito a mano (D-21).';
COMMENT ON COLUMN grano.id IS 'Identificador (UUID v7)';
COMMENT ON COLUMN grano.empresa_id IS 'Empresa dueña del registro (RNF-03)';
COMMENT ON COLUMN grano.foto_id IS 'Foto (RF-07, RF-05)';
COMMENT ON COLUMN grano.x IS 'Posición x (px) (RF-07, RF-11)';
COMMENT ON COLUMN grano.y IS 'Posición y (px) (RF-07, RF-11)';
COMMENT ON COLUMN grano.ancho IS 'Ancho (px) (RF-07, RF-11)';
COMMENT ON COLUMN grano.alto IS 'Alto (px) (RF-07, RF-11)';
COMMENT ON COLUMN grano.clase IS 'Clase (RF-08, RF-11)';
COMMENT ON COLUMN grano.confianza IS 'Confianza (%) (RF-08, RF-09, RF-17)';
COMMENT ON COLUMN grano.fecha_creacion IS 'Fecha de creación del registro';
COMMENT ON COLUMN grano.fecha_actualizacion IS 'Fecha de la última actualización; la mantiene un trigger';
