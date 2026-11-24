/*
 * Nombre: EsquemaIT.java
 * Descripción: Pruebas del esquema que genera el generador (V1__esquema_inicial.sql) en PostgreSQL 18.
 * Fecha de creación: 13/10/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.3
 */
package co.edu.usco.inspectia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas del esquema generado desde {@code specs/entidades} (RP-15, RP-23), aplicado por Flyway en
 * PostgreSQL 18. Cada prueba corre en una transacción que se deshace al final.
 *
 * <p>En PostgreSQL, un error deja la transacción abortada; por eso la inserción que debe fallar va
 * siempre al final de cada prueba.
 */
@PruebaIntegracion
@Transactional
class EsquemaIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("RP-15 · Flyway crea las 9 tablas del modelo de datos")
    void creaLasNueveTablas() {
        List<String> tablas = jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
                  AND table_name <> 'flyway_schema_history'
                ORDER BY table_name""", String.class);

        assertThat(tablas).containsExactly("empresa", "foto", "grano", "lote", "modelo", "muestra",
                "rango_recomendacion", "region_pegada", "usuario");
    }

    @Test
    @DisplayName("RNF-03 · C4 · la base rechaza un usuario sin empresa que no sea ADMIN_PLATAFORMA")
    void usuarioSinEmpresaSoloSiEsAdministradorDeLaPlataforma() {
        int administradores = jdbc.update("""
                INSERT INTO usuario (nombre, correo, contrasena_hash, rol)
                VALUES ('Admin', 'admin@inspectia.co', 'x', 'ADMIN_PLATAFORMA')""");
        assertThat(administradores).isEqualTo(1);

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO usuario (nombre, correo, contrasena_hash, rol)
                VALUES ('Ana', 'ana@finca.co', 'x', 'ADMIN_EMPRESA')"""))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("usuario_empresa_segun_rol");
    }

    @Test
    @DisplayName("RNF-03 · un lote no puede apuntar a un usuario de otra empresa")
    void loteNoApuntaAUsuarioDeOtraEmpresa() {
        UUID empresaA = crearEmpresa("890903938-8");
        UUID empresaB = crearEmpresa("899999068-1");
        UUID usuarioDeB = crearUsuario(empresaB, "luis@b.co");

        assertThatThrownBy(() -> crearLote(empresaA, usuarioDeB, "L-001"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("lote_usuario_id_fk");
    }

    @Test
    @DisplayName("RF-04 · C3 · la base trata «l-001 » y «L-001» como el mismo código de la empresa")
    void codigoDelLoteSinDistinguirMayusculasNiEspacios() {
        UUID empresaA = crearEmpresa("890903938-8");
        UUID empresaB = crearEmpresa("899999068-1");
        crearLote(empresaA, crearUsuario(empresaA, "ana@a.co"), "L-001");
        crearLote(empresaB, crearUsuario(empresaB, "luis@b.co"), "L-001");
        UUID otroDeA = crearUsuario(empresaA, "eva@a.co");

        assertThatThrownBy(() -> crearLote(empresaA, otroDeA, "l-001 "))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("lote_empresa_id_codigo_unico");
    }

    @Test
    @DisplayName("D-18 · solo un modelo puede estar en uso")
    void unSoloModeloEnUso() {
        ModeloDeLaComparacion.EFFICIENTNET_B0.insertar(jdbc, true);

        assertThatThrownBy(() -> ModeloDeLaComparacion.RESNET_50.insertar(jdbc, true))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("modelo_en_uso_unico");
    }

    @Test
    @DisplayName("P-02 · la base no deja poner en uso un modelo con F1 macro menor que 65, como MobileNetV3-L (63,80)")
    void modeloEnUsoConElF1Minimo() {
        UUID mobilenet = ModeloDeLaComparacion.MOBILENET_V3_L.insertar(jdbc, false);

        assertThatThrownBy(() -> jdbc.update("UPDATE modelo SET en_uso = true WHERE id = ?", mobilenet))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("modelo_en_uso_con_f1_minimo");
    }

    @Test
    @DisplayName("D-48 · la base no deja en uso un modelo sin sus métricas, como el clasificador falso")
    void modeloEnUsoConSusMetricas() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO modelo (nombre, version, recall_sano, recall_no_sano, "
                + "en_uso) VALUES ('sin-metricas', '1.0', 91.20, 53.40, true)"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("modelo_en_uso_con_f1_minimo");
    }

    @Test
    @DisplayName("D-48 · la base rechaza un modelo con las métricas macro y sin la precisión de cada clase ni la matriz de confusión: van las 11 o ninguna")
    void modeloConSusOnceMetricasONinguna() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO modelo (nombre, version, tipo, f1_macro, precision_macro, recall_macro, recall_sano,
                    recall_no_sano, tiempo_por_grano_ms)
                VALUES ('sin-matriz', '1.0', 'CNN', 73.50, 75.20, 72.31, 91.20, 53.40, 19.00)"""))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("modelo_datos_completos");
    }

    @Test
    @DisplayName("D-39 · la base rechaza un modelo sin el recall de cada clase: sin ellos no se corrige el porcentaje")
    void modeloConSusRecall() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO modelo (nombre, version, recall_sano) "
                + "VALUES ('sin-recall', '1.0', 91.20)"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("recall_no_sano");
    }

    @Test
    @DisplayName("D-39 · la base rechaza un modelo cuyos recall suman 100 o menos: la corrección dividiría por cero")
    void modeloMejorQueElAzar() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO modelo (nombre, version, recall_sano, recall_no_sano) "
                + "VALUES ('al-azar', '1.0', 50.00, 50.00)"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("modelo_mejor_que_el_azar");
    }

    @Test
    @DisplayName("P-19 · un lote analizado guarda su margen: sin él, la base lo rechaza")
    void loteAnalizadoConMargen() {
        UUID empresa = crearEmpresa("900123456-8");
        UUID lote = crearLote(empresa, crearUsuario(empresa, "ana@a.co"), "L-001");

        assertThatThrownBy(() -> jdbc.update("UPDATE lote SET estado = 'ANALIZADO', porcentaje_sanos = 90.6, "
                + "categoria = 'ESTANDAR', recomendacion = 'Apto', sugerir_otra_muestra = true, "
                + "aviso_confianza_baja = false, fecha_resultado = now() WHERE id = ?", lote))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("lote_resultado_segun_estado");
    }

    @Test
    @DisplayName("D-47 · un lote que sugiere otra muestra guarda el límite cercano y su categoría: sin ellos, la base lo rechaza")
    void sugerenciaConSuLimite() {
        UUID empresa = crearEmpresa("900123456-8");
        UUID lote = crearLote(empresa, crearUsuario(empresa, "ana@a.co"), "L-001");

        assertThatThrownBy(() -> analizar(lote, true, null, null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("lote_sugerencia_con_su_limite");
    }

    @Test
    @DisplayName("D-47 · un lote sin sugerencia no guarda límite: si deja de sugerir otra muestra y conserva el límite, la base lo rechaza")
    void limiteSoloConSugerencia() {
        UUID empresa = crearEmpresa("900123456-8");
        UUID lote = crearLote(empresa, crearUsuario(empresa, "ana@a.co"), "L-001");
        analizar(lote, true, new BigDecimal("90.0"), "ESTANDAR");

        assertThatThrownBy(() -> jdbc.update("UPDATE lote SET sugerir_otra_muestra = false WHERE id = ?", lote))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("lote_sugerencia_con_su_limite");
    }

    @Test
    @DisplayName("D-47 · el límite de Fuera de norma nunca cuenta para la sugerencia: la base rechaza un límite de 0, aunque traiga otra categoría")
    void limiteCeroNoCuenta() {
        UUID empresa = crearEmpresa("900123456-8");
        UUID lote = crearLote(empresa, crearUsuario(empresa, "ana@a.co"), "L-001");

        assertThatThrownBy(() -> analizar(lote, true, new BigDecimal("0.0"), "CORRIENTE"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("lote_sugerencia_con_su_limite");
    }

    @Test
    @DisplayName("D-47 · el límite de Fuera de norma nunca cuenta para la sugerencia: la base rechaza la categoría Fuera de norma, aunque traiga un límite mayor que 0")
    void categoriaFueraDeNormaNoCuenta() {
        UUID empresa = crearEmpresa("900123456-8");
        UUID lote = crearLote(empresa, crearUsuario(empresa, "ana@a.co"), "L-001");

        assertThatThrownBy(() -> analizar(lote, true, new BigDecimal("82.0"), "FUERA_DE_NORMA"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("lote_sugerencia_con_su_limite");
    }

    @Test
    @DisplayName("D-44 · solo una muestra que volvió a pendiente después de enviarse tiene motivo de devolución: al enviarla de nuevo sin borrarlo, la base lo rechaza")
    void motivoSoloSiLaMuestraVolvio() {
        UUID empresa = crearEmpresa("900123456-8");
        UUID usuario = crearUsuario(empresa, "ana@a.co");
        UUID muestra = crearMuestra(empresa, crearLote(empresa, usuario, "L-001"), usuario);
        jdbc.update("UPDATE muestra SET fecha_envio = now(), motivo_devolucion = 'POCOS_GRANOS' WHERE id = ?", muestra);

        assertThatThrownBy(() -> jdbc.update("UPDATE muestra SET estado = 'EN_ESPERA' WHERE id = ?", muestra))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("muestra_motivo_solo_si_devuelta");
    }

    @Test
    @DisplayName("D-46 · una foto que el servicio de IA todavía no revisó, «Cargada», no tiene conteo de granos")
    void conteoSoloEnUnaFotoRevisada() {
        UUID empresa = crearEmpresa("900123456-8");
        UUID usuario = crearUsuario(empresa, "ana@a.co");
        UUID muestra = crearMuestra(empresa, crearLote(empresa, usuario, "L-001"), usuario);
        crearFoto(empresa, muestra, 1, "APROBADA", null, 52);

        assertThatThrownBy(() -> crearFoto(empresa, muestra, 2, "CARGADA", null, 48))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("foto_granos_solo_si_revisada");
    }

    @Test
    @DisplayName("D-45 · borrar una foto borra sus regiones de granos pegados")
    void regionesSeBorranConSuFoto() {
        UUID empresa = crearEmpresa("900123456-8");
        UUID usuario = crearUsuario(empresa, "ana@a.co");
        UUID muestra = crearMuestra(empresa, crearLote(empresa, usuario, "L-001"), usuario);
        UUID foto = crearFoto(empresa, muestra, 1, "POR_REPETIR", "GRANOS_PEGADOS", 47);
        UUID otra = crearFoto(empresa, muestra, 2, "POR_REPETIR", "GRANOS_PEGADOS", 50);
        for (UUID cual : List.of(foto, foto, otra)) {
            jdbc.update("""
                    INSERT INTO region_pegada (empresa_id, foto_id, x, y, ancho, alto)
                    VALUES (?, ?, 120, 80, 64, 40)""", empresa, cual);
        }

        jdbc.update("DELETE FROM foto WHERE id = ?", foto);

        assertThat(jdbc.queryForList("SELECT foto_id FROM region_pegada", UUID.class)).containsExactly(otra);
    }

    @Test
    @DisplayName("RP-15 · el trigger mantiene fecha_actualizacion aunque un script intente cambiarla")
    void triggerMantieneLaFechaDeActualizacion() {
        UUID empresa = crearEmpresa("900123456-8");

        jdbc.update("UPDATE empresa SET fecha_actualizacion = '2000-01-01' WHERE id = ?", empresa);

        OffsetDateTime fecha = jdbc.queryForObject(
                "SELECT fecha_actualizacion FROM empresa WHERE id = ?", OffsetDateTime.class, empresa);
        assertThat(fecha.getYear()).isGreaterThan(2000);
    }

    private UUID crearEmpresa(String nit) {
        return jdbc.queryForObject(
                "INSERT INTO empresa (nombre, nit, ciudad) VALUES ('Empresa ' || ?, ?, 'Neiva') RETURNING id",
                UUID.class, nit, nit);
    }

    private UUID crearUsuario(UUID empresa, String correo) {
        return jdbc.queryForObject("""
                INSERT INTO usuario (empresa_id, nombre, correo, contrasena_hash, rol)
                VALUES (?, 'Usuario', ?, 'x', 'USUARIO_FINAL') RETURNING id""",
                UUID.class, empresa, correo);
    }

    private UUID crearLote(UUID empresa, UUID usuario, String codigo) {
        return jdbc.queryForObject("""
                INSERT INTO lote (empresa_id, codigo, fecha_recepcion, origen, usuario_id)
                VALUES (?, ?, current_date, 'Finca La Esperanza', ?) RETURNING id""",
                UUID.class, empresa, codigo, usuario);
    }

    /** Deja el lote analizado con 90,6 % Estándar y, si se pide, con la sugerencia y su límite. */
    private void analizar(UUID lote, boolean sugerir, BigDecimal limite, String categoriaDelLimite) {
        jdbc.update("""
                UPDATE lote SET estado = 'ANALIZADO', porcentaje_sanos = 90.6, margen = 7.1, categoria = 'ESTANDAR',
                    recomendacion = 'Apto', sugerir_otra_muestra = ?, limite_sugerencia = ?,
                    categoria_del_limite = ?, aviso_confianza_baja = false, fecha_resultado = now()
                WHERE id = ?""", sugerir, limite, categoriaDelLimite, lote);
    }

    private UUID crearMuestra(UUID empresa, UUID lote, UUID usuario) {
        return jdbc.queryForObject("""
                INSERT INTO muestra (empresa_id, lote_id, numero, usuario_id) VALUES (?, ?, 1, ?) RETURNING id""",
                UUID.class, empresa, lote, usuario);
    }

    private UUID crearFoto(UUID empresa, UUID muestra, int numero, String estado, String motivo, int granos) {
        return jdbc.queryForObject("""
                INSERT INTO foto (empresa_id, muestra_id, numero, ruta, formato, tamano_bytes, estado, motivo,
                    granos_encontrados)
                VALUES (?, ?, ?, ?, 'JPG', 2048, ?, ?, ?) RETURNING id""",
                UUID.class, empresa, muestra, numero, "fotos/" + muestra + "/" + numero + ".jpg", estado, motivo,
                granos);
    }
}
