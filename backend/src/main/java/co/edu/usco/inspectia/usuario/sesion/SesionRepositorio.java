/*
 * Nombre: SesionRepositorio.java
 * Descripción: Consultas del inicio de sesión sobre la tabla usuario: el usuario con su empresa y el
 *              conteo de intentos fallidos (RF-01, RNF-04).
 * Fecha de creación: 14/10/2026
 * Actualización: 18/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
package co.edu.usco.inspectia.usuario.sesion;

import co.edu.usco.inspectia.usuario.Usuario;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultas del inicio de sesión sobre la tabla {@code usuario}.
 *
 * <p>Los intentos fallidos se cuentan con una sola sentencia {@code UPDATE}: si llegan varios a la
 * vez, la base los aplica uno tras otro y ninguno se pierde ni alarga el bloqueo.
 */
public interface SesionRepositorio extends Repository<Usuario, UUID> {

    /** El usuario con su empresa; los datos de la empresa quedan en {@code null} si no tiene. */
    String USUARIO_CON_EMPRESA = "select new co.edu.usco.inspectia.usuario.sesion.DatosDeSesion("
            + "u.id, u.nombre, u.correo, u.contrasenaHash, u.rol, u.activo, u.bloqueadoHasta, u.sesionesDesde, e.id,"
            + " e.nombre, e.activa)"
            + " from Usuario u left join Empresa e on e.id = u.empresaId";

    /**
     * Busca un usuario por su correo, sin distinguir mayúsculas ni espacios al inicio o al final.
     *
     * @param correo el correo
     * @return el usuario con su empresa, si existe
     */
    @Query(USUARIO_CON_EMPRESA + " where lower(trim(u.correo)) = lower(trim(:correo))")
    Optional<DatosDeSesion> buscarPorCorreo(@Param("correo") String correo);

    /**
     * Busca un usuario por su identificador.
     *
     * @param id el identificador
     * @return el usuario con su empresa, si existe
     */
    @Query(USUARIO_CON_EMPRESA + " where u.id = :id")
    Optional<DatosDeSesion> buscarPorId(@Param("id") UUID id);

    /**
     * Registra un intento fallido si la cuenta no está bloqueada en este momento: los intentos durante
     * el bloqueo no cuentan ni lo alargan (RNF-04). El intento número {@code permitidos} bloquea la
     * cuenta hasta {@code hasta} y deja los intentos en cero, así al terminar el bloqueo la cuenta
     * vuelve a tenerlos todos (RF-01 C7).
     *
     * <p>El {@code cast} de {@code :hasta} le dice a PostgreSQL su tipo: dentro de un {@code case}
     * con {@code null} no lo puede deducir y lo tomaría como texto.
     *
     * @param id el usuario
     * @param permitidos los intentos fallidos seguidos que bloquean la cuenta
     * @param ahora la hora del intento
     * @param hasta hasta cuándo se bloquea la cuenta si este intento completa los permitidos
     * @return 1 si se registró; 0 si la cuenta estaba bloqueada
     */
    @Modifying
    @Transactional
    @Query("""
            update Usuario u set
                u.intentosFallidos = case when u.intentosFallidos + 1 >= :permitidos then 0
                                          else u.intentosFallidos + 1 end,
                u.bloqueadoHasta = case when u.intentosFallidos + 1 >= :permitidos
                                        then cast(:hasta as OffsetDateTime) else null end
            where u.id = :id and (u.bloqueadoHasta is null or u.bloqueadoHasta <= :ahora)""")
    int registrarFallo(@Param("id") UUID id, @Param("permitidos") int permitidos, @Param("ahora") OffsetDateTime ahora,
            @Param("hasta") OffsetDateTime hasta);

    /**
     * Deja en cero los intentos fallidos después de un inicio de sesión correcto: deben ser seguidos
     * para bloquear la cuenta (RF-01 C5). Solo escribe si hay algo que reiniciar.
     *
     * @param id el usuario
     * @return 1 si reinició algo; 0 si ya estaba en cero
     */
    @Modifying
    @Transactional
    @Query("""
            update Usuario u set u.intentosFallidos = 0, u.bloqueadoHasta = null
            where u.id = :id and (u.intentosFallidos <> 0 or u.bloqueadoHasta is not null)""")
    int reiniciarIntentos(@Param("id") UUID id);
}
