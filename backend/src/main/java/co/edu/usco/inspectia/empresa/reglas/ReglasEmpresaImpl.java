/*
 * Nombre: ReglasEmpresaImpl.java
 * Descripción: Reglas de empresa escritas a mano: implementa la interfaz generada ReglasEmpresa (RP-23).
 * Fecha de creación: 13/10/2026
 * Actualización: 13/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.empresa.reglas;

import co.edu.usco.inspectia.empresa.Empresa;
import co.edu.usco.inspectia.empresa.ReglasEmpresa;
import org.springframework.stereotype.Component;

/**
 * Reglas de {@code specs/entidades/empresa.json} que se programan a mano. El servicio generado las
 * llama al crear y al editar una empresa.
 */
@Component
public class ReglasEmpresaImpl implements ReglasEmpresa {

    /** El dígito de verificación del NIT corresponde al que calcula la DIAN (RF-02 C4, D-27). */
    @Override
    public boolean cumpleNitDigitoVerificacion(Empresa empresa) {
        return DigitoVerificacionNit.esValido(empresa.getNit());
    }
}
