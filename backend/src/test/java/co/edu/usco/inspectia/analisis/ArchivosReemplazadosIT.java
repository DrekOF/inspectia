/*
 * Nombre: ArchivosReemplazadosIT.java
 * Descripción: Pruebas de integración del borrado del archivo de una foto reemplazada (RF-05 C5): espera a que la
 *              transacción se confirme, y si se revierte, el archivo de la foto anterior queda.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.analisis;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usco.inspectia.ConfiguracionContenedores;
import co.edu.usco.inspectia.PruebaIntegracion;
import co.edu.usco.inspectia.analisis.aplicacion.ArchivoReemplazado;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * El archivo de una foto reemplazada se borra solo cuando la transacción que la reemplazó se confirma (RF-05 C5). Si
 * se revierte, por ejemplo porque la foto nueva no se pudo guardar, la fila de la anterior vuelve y su archivo tiene
 * que seguir ahí.
 */
@PruebaIntegracion
class ArchivosReemplazadosIT {

    @Autowired
    private ApplicationEventPublisher eventos;

    @Autowired
    private PlatformTransactionManager transacciones;

    private TransactionTemplate transaccion;
    private String llave;
    private Path archivo;

    @BeforeEach
    void preparar() throws IOException {
        transaccion = new TransactionTemplate(transacciones);
        llave = UUID.randomUUID() + "/" + UUID.randomUUID() + "/" + UUID.randomUUID() + ".jpg";
        archivo = ConfiguracionContenedores.CARPETA_DE_FOTOS.resolve(llave);
        Files.createDirectories(archivo.getParent());
        Files.write(archivo, ApiFotos.jpg());
    }

    @AfterEach
    void limpiar() {
        ConfiguracionContenedores.vaciarCarpetaDeFotos();
    }

    @Test
    @DisplayName("RF-05 · C5 · si la transacción que reemplazó la foto se revierte, el archivo de la anterior sigue en la carpeta")
    void siSeRevierteElArchivoQueda() {
        transaccion.executeWithoutResult(estado -> {
            eventos.publishEvent(new ArchivoReemplazado(llave));
            estado.setRollbackOnly();
        });

        assertThat(archivo).exists();
    }

    @Test
    @DisplayName("RF-05 · C5 · cuando la transacción que reemplazó la foto se confirma, el archivo de la anterior se borra, y no antes")
    void alConfirmarseSeBorra() {
        transaccion.executeWithoutResult(estado -> {
            eventos.publishEvent(new ArchivoReemplazado(llave));
            assertThat(archivo).as("antes de confirmar").exists();
        });

        assertThat(archivo).doesNotExist();
    }
}
