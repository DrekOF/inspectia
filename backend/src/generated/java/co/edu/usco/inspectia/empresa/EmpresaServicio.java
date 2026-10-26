/*
 * Nombre: EmpresaServicio.java
 * Descripción: Operaciones de empresa: validan, guardan y responden (RF-02).
 *              Archivo generado por generador/ (RP-23): no se edita a mano; se cambia el JSON
 *              o la plantilla, y se regenera.
 * Fecha de creación: 13/10/2026
 * Actualización: 26/10/2026
 * Autor: Equipo InspectIA (generado por generador/ v1.8)
 * Versión: 1.8
 * Entidades: empresa 1.3, usuario 1.7, rango_recomendacion 1.2
 */
package co.edu.usco.inspectia.empresa;

import co.edu.usco.inspectia.comun.api.Pagina;
import co.edu.usco.inspectia.comun.errores.CampoInvalido;
import co.edu.usco.inspectia.comun.errores.DatoRepetidoExcepcion;
import co.edu.usco.inspectia.comun.errores.NoEncontradoExcepcion;
import co.edu.usco.inspectia.comun.errores.ReglaIncumplidaExcepcion;
import co.edu.usco.inspectia.rango.RangoRecomendacionRepositorio;
import co.edu.usco.inspectia.rango.RangoRecomendacionValoresIniciales;
import co.edu.usco.inspectia.usuario.UsuarioRepositorio;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operaciones de {@code empresa} (RF-02, RNF-03, D-13, D-27). Cada una revisa, en este orden:
 * <ol>
 *   <li>Los datos, con las validaciones del JSON (las revisa Spring antes de llamar al servicio).</li>
 *   <li>Las reglas que se programan a mano: si alguna no se cumple, responde 400.</li>
 *   <li>Los datos únicos: si alguno ya existe, responde 409.</li>
 * </ol>
 * Lo que se guarda en una operación queda en una sola transacción: o se guarda todo o nada.
 */
@Service
@Transactional(readOnly = true)
public class EmpresaServicio {

    /** Orden del listado: por nombre, sin distinguir mayúsculas; los empates, por antigüedad. */
    private static final Sort ORDEN = Sort.by(Sort.Order.asc("nombre").ignoreCase(), Sort.Order.asc("id"));

    private final EmpresaRepositorio repositorio;
    private final ReglasEmpresa reglas;
    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder codificador;
    private final RangoRecomendacionRepositorio rangoRecomendacionRepositorio;

    /**
     * Crea el servicio; Spring le pasa sus dependencias.
     *
     * @param repositorio el repositorio de {@code empresa}
     * @param reglas las reglas que se programan a mano
     * @param usuarioRepositorio el repositorio de {@code usuario}
     * @param codificador el codificador BCrypt de las contraseñas
     * @param rangoRecomendacionRepositorio el repositorio de {@code rango_recomendacion}
     */
    public EmpresaServicio(
            EmpresaRepositorio repositorio,
            ReglasEmpresa reglas,
            UsuarioRepositorio usuarioRepositorio,
            PasswordEncoder codificador,
            RangoRecomendacionRepositorio rangoRecomendacionRepositorio) {
        this.repositorio = repositorio;
        this.reglas = reglas;
        this.usuarioRepositorio = usuarioRepositorio;
        this.codificador = codificador;
        this.rangoRecomendacionRepositorio = rangoRecomendacionRepositorio;
    }

    /**
     * Una página de empresas, por nombre, sin distinguir mayúsculas.
     *
     * @param pagina número de la página, desde 0
     * @param tamano filas por página
     * @param activo solo los activos ({@code true}) o los desactivados ({@code false}); con {@code null}, todos
     * @return la página
     */
    public Pagina<EmpresaRespuesta> listar(int pagina, int tamano, Boolean activo) {
        PageRequest pedido = PageRequest.of(pagina, tamano, ORDEN);
        Page<Empresa> registros = activo == null
                ? repositorio.findAll(pedido)
                : repositorio.findAllByActiva(activo, pedido);
        return Pagina.de(registros, EmpresaMapeador::respuesta);
    }

    /**
     * Un registro por su identificador.
     *
     * @param id el identificador
     * @return el registro
     * @throws NoEncontradoExcepcion si no existe (404)
     */
    public EmpresaRespuesta ver(UUID id) {
        return EmpresaMapeador.respuesta(buscar(id));
    }

    /**
     * Guarda el registro nuevo. En la misma transacción guarda también su primer administrador
     * (usuario) y los valores por defecto de rango_recomendacion.
     *
     * @param solicitud los datos, ya validados con el JSON
     * @return lo que quedó guardado
     * @throws ReglaIncumplidaExcepcion si no se cumple una regla (400)
     * @throws DatoRepetidoExcepcion si un dato único ya existe (409)
     */
    @Transactional
    public EmpresaRespuesta crear(EmpresaCrearSolicitud solicitud) {
        Empresa empresa = EmpresaMapeador.deSolicitud(solicitud);
        revisarReglasAlCrear(empresa);
        List<CampoInvalido> repetidos = new ArrayList<>();
        if (repositorio.existeNit(empresa.getNit())) {
            repetidos.add(new CampoInvalido("nit", "empresa.nit.unico"));
        }
        if (usuarioRepositorio.existeCorreo(solicitud.administrador().correo())) {
            repetidos.add(new CampoInvalido("administrador.correo", "usuario.correo.unico"));
        }
        if (!repetidos.isEmpty()) {
            throw new DatoRepetidoExcepcion(repetidos);
        }
        repositorio.saveAndFlush(empresa);
        usuarioRepositorio.saveAndFlush(EmpresaMapeador.administradorDeSolicitud(solicitud.administrador(), empresa.getId(),
                codificador.encode(solicitud.administrador().contrasena())));
        rangoRecomendacionRepositorio.saveAllAndFlush(RangoRecomendacionValoresIniciales.paraEmpresa(empresa.getId()));
        return EmpresaMapeador.respuesta(empresa);
    }

    /**
     * Edita un registro con las mismas validaciones del registro. Los cambios se revisan en una copia
     * y solo se aplican si todo está bien.
     *
     * @param id el registro que se edita
     * @param solicitud los datos, ya validados con el JSON
     * @return lo que quedó guardado
     * @throws NoEncontradoExcepcion si no existe (404)
     * @throws ReglaIncumplidaExcepcion si no se cumple una regla (400)
     * @throws DatoRepetidoExcepcion si un dato único ya lo tiene otro registro (409)
     */
    @Transactional
    public EmpresaRespuesta editar(UUID id, EmpresaEditarSolicitud solicitud) {
        Empresa empresa = buscar(id);
        Empresa propuesta = EmpresaMapeador.copia(empresa);
        EmpresaMapeador.aplicar(solicitud, propuesta);
        revisarReglasAlEditar(propuesta);
        List<CampoInvalido> repetidos = new ArrayList<>();
        if (repositorio.existeNitEnOtro(propuesta.getNit(), id)) {
            repetidos.add(new CampoInvalido("nit", "empresa.nit.unico"));
        }
        if (!repetidos.isEmpty()) {
            throw new DatoRepetidoExcepcion(repetidos);
        }
        EmpresaMapeador.aplicar(solicitud, empresa);
        repositorio.flush();
        return EmpresaMapeador.respuesta(empresa);
    }

    /**
     * Desactiva un registro: cambia {@code activa} y no borra nada.
     *
     * @param id el registro
     * @return lo que quedó guardado
     * @throws NoEncontradoExcepcion si no existe (404)
     */
    @Transactional
    public EmpresaRespuesta desactivar(UUID id) {
        Empresa empresa = buscar(id);
        empresa.setActiva(false);
        repositorio.flush();
        return EmpresaMapeador.respuesta(empresa);
    }

    /**
     * Reactiva un registro: cambia {@code activa} y no borra nada.
     *
     * @param id el registro
     * @return lo que quedó guardado
     * @throws NoEncontradoExcepcion si no existe (404)
     */
    @Transactional
    public EmpresaRespuesta reactivar(UUID id) {
        Empresa empresa = buscar(id);
        empresa.setActiva(true);
        repositorio.flush();
        return EmpresaMapeador.respuesta(empresa);
    }

    private Empresa buscar(UUID id) {
        return repositorio.findById(id).orElseThrow(() -> new NoEncontradoExcepcion("empresa " + id));
    }

    private void revisarReglasAlCrear(Empresa empresa) {
        List<CampoInvalido> incumplidas = new ArrayList<>();
        if (!reglas.cumpleNitDigitoVerificacion(empresa)) {
            incumplidas.add(new CampoInvalido("nit", "empresa.reglas.nit_digito_verificacion"));
        }
        if (!incumplidas.isEmpty()) {
            throw new ReglaIncumplidaExcepcion(incumplidas);
        }
    }

    private void revisarReglasAlEditar(Empresa empresa) {
        List<CampoInvalido> incumplidas = new ArrayList<>();
        if (!reglas.cumpleNitDigitoVerificacion(empresa)) {
            incumplidas.add(new CampoInvalido("nit", "empresa.reglas.nit_digito_verificacion"));
        }
        if (!incumplidas.isEmpty()) {
            throw new ReglaIncumplidaExcepcion(incumplidas);
        }
    }
}
