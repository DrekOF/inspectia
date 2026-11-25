/*
 * Nombre: EncabezadosDeCloudFront.java
 * Descripción: En producción, toma de los encabezados que agrega CloudFront la IP del cliente
 *              (CloudFront-Viewer-Address) y el protocolo con que llegó (CloudFront-Forwarded-Proto), para que la IP y
 *              las direcciones que arma el servidor sean las del usuario y no las de CloudFront (D-16).
 * Fecha de creación: 24/11/2026
 * Actualización: 24/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
package co.edu.usco.inspectia.comun.produccion;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Los encabezados de CloudFront (D-16). En AWS la API llega por CloudFront y por HTTP a la instancia: sin este filtro,
 * el servidor vería la IP de CloudFront y el protocolo {@code http}, y armaría con él direcciones como la del
 * encabezado {@code Location} al crear una empresa.
 *
 * <p>No usa {@code X-Forwarded-For}: CloudFront agrega la IP del cliente al final de lo que mande el navegador, así que
 * el principio de ese encabezado lo puede escribir cualquiera. Los encabezados {@code CloudFront-*} los pone siempre
 * CloudFront y reemplaza los que mande el navegador. Llegan porque el comportamiento {@code /api/*} usa la política de
 * petición al origen {@code Managed-AllViewerAndCloudFrontHeaders-2022-06} (ver infra/README.md, paso 9.5), que además
 * pasa el {@code Host} de CloudFront: así el servidor arma las direcciones con el dominio público y no con el de la
 * instancia. El filtro cambia el protocolo y la IP; el host lo toma tal como llega.
 *
 * <p>Límite: el grupo de seguridad de la instancia solo deja entrar a la lista de prefijos de CloudFront, pero esa lista
 * es de todas las distribuciones de CloudFront, de cualquier cuenta. Una distribución ajena que apunte a la instancia
 * puede mandar estos encabezados con el valor que quiera. Hoy no da acceso a nada (la API exige el token) y ninguna regla
 * depende de la IP; cuando se use la IP para limitar intentos (P-17), hará falta un encabezado secreto de origen que
 * solo ponga nuestra distribución y que este filtro exija. Un valor que no se entiende se ignora.
 */
public class EncabezadosDeCloudFront extends OncePerRequestFilter {

    /** La IP y el puerto del cliente, como {@code 198.51.100.10:46532} o {@code 2001:db8::1:46532}. */
    static final String DIRECCION_DEL_CLIENTE = "CloudFront-Viewer-Address";

    /** El protocolo con que el cliente llegó a CloudFront: {@code https} o {@code http}. */
    static final String PROTOCOLO = "CloudFront-Forwarded-Proto";

    private static final Pattern IP_Y_PUERTO = Pattern.compile("^([0-9A-Fa-f.:]+):(\\d{1,5})$");

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        String ip = ipDe(peticion.getHeader(DIRECCION_DEL_CLIENTE));
        String protocolo = protocoloDe(peticion.getHeader(PROTOCOLO));
        if (ip == null && protocolo == null) {
            cadena.doFilter(peticion, respuesta);
            return;
        }
        cadena.doFilter(new PeticionDelCliente(peticion, ip, protocolo), respuesta);
    }

    /**
     * La IP de {@code CloudFront-Viewer-Address}.
     *
     * @param valor el encabezado
     * @return la IP, o {@code null} si falta o no se entiende
     */
    static String ipDe(String valor) {
        if (valor == null) {
            return null;
        }
        Matcher partes = IP_Y_PUERTO.matcher(valor.strip());
        if (!partes.matches()) {
            return null;
        }
        try {
            return InetAddress.ofLiteral(partes.group(1)).getHostAddress();
        } catch (IllegalArgumentException excepcion) {
            return null;
        }
    }

    /**
     * El protocolo de {@code CloudFront-Forwarded-Proto}.
     *
     * @param valor el encabezado
     * @return {@code https} o {@code http}, o {@code null} si falta o es otro
     */
    static String protocoloDe(String valor) {
        if (valor == null) {
            return null;
        }
        String protocolo = valor.strip().toLowerCase(Locale.ROOT);
        return "https".equals(protocolo) || "http".equals(protocolo) ? protocolo : null;
    }

    /** La petición con la IP y el protocolo del cliente. */
    private static final class PeticionDelCliente extends HttpServletRequestWrapper {

        private final String ip;
        private final String protocolo;

        /**
         * Envuelve la petición.
         *
         * @param peticion la petición que llegó de CloudFront
         * @param ip la IP del cliente, o {@code null} para dejar la de la petición
         * @param protocolo el protocolo del cliente, o {@code null} para dejar el de la petición
         */
        PeticionDelCliente(HttpServletRequest peticion, String ip, String protocolo) {
            super(peticion);
            this.ip = ip;
            this.protocolo = protocolo;
        }

        @Override
        public String getRemoteAddr() {
            return ip != null ? ip : super.getRemoteAddr();
        }

        @Override
        public String getRemoteHost() {
            return ip != null ? ip : super.getRemoteHost();
        }

        @Override
        public String getScheme() {
            return protocolo != null ? protocolo : super.getScheme();
        }

        @Override
        public boolean isSecure() {
            return protocolo != null ? "https".equals(protocolo) : super.isSecure();
        }

        @Override
        public int getServerPort() {
            if (protocolo == null) {
                return super.getServerPort();
            }
            return "https".equals(protocolo) ? 443 : 80;
        }

        @Override
        public StringBuffer getRequestURL() {
            if (protocolo == null) {
                return super.getRequestURL();
            }
            // El puerto es el de siempre del protocolo, que no se escribe.
            return new StringBuffer(protocolo).append("://").append(getServerName()).append(getRequestURI());
        }
    }
}
