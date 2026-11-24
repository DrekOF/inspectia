/*
 * Nombre: EsquemaIT.java
 * Descripción: Pruebas del esquema que genera el generador (V1__esquema_inicial.sql) en PostgreSQL 18.
 * Fecha de creación: 13/10/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.2
 */
package co.edu.usco.inspectia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @DisplayName("RP-15 · Flyway crea las 8 tablas del modelo de datos")
    void creaLasOchoTablas() {
        List<String> tablas = jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
                  AND table_name <> 'flyway_schema_history'
                ORDER BY table_name""", String.class);

        assertThat(tablas).containsExactly("empresa", "foto", "grano", "lote", "modelo", "muestra",
                "rango_recomendacion", "usuario");
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
        jdbc.update("INSERT INTO modelo (nombre, version, recall_sano, recall_no_sano, en_uso) "
                + "VALUES ('EfficientNet-B0', '1.0', 91.20, 53.40, true)");

        assertThatThrownBy(() -> jdbc.update("INSERT INTO modelo (nombre, version, recall_sano, recall_no_sano, "
                + "en_uso) VALUES ('ResNet-50', '1.0', 87.40, 52.10, true)"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("modelo_en_uso_unico");
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
}
