# syntax=docker/dockerfile:1
# Nombre: Dockerfile (backend)
# Descripción: Imagen del servidor de InspectIA (D-16, D-21), para amd64 y arm64 (la EC2 t4g es Graviton). Compila
#              con Maven y Java 25, como el pom, sin correr las pruebas (las corre la CI antes), y corre con un JRE 25
#              liviano, con un usuario sin privilegios, la memoria medida para una instancia de 4 GB que comparte con
#              PostgreSQL y el servicio de IA, y un healthcheck. Los secretos llegan como archivos de /run/secrets
#              (ver docker/entrada.sh e infra/produccion/docker-compose.yml).
# Fecha de creación: 24/11/2026
# Actualización: 24/11/2026
# Autor: Equipo InspectIA
# Versión: 1.0
#
# Uso, desde la raíz del repositorio (la CI la construye para arm64 en runners arm64):
#   docker build -t inspectia/backend backend
#   docker buildx build --platform linux/arm64 -t inspectia/backend backend

# ---- Compilación: el JAR de Spring Boot, separado en capas ----
FROM eclipse-temurin:25-jdk-noble AS compilacion
WORKDIR /fuente

# Primero solo el pom y Maven: mientras el pom no cambie, esta capa (las dependencias) se reutiliza.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp -q dependency:go-offline

# El código escrito a mano y el que escribe generador/ (RP-23). Las pruebas no entran a la imagen.
COPY src/main/ src/main/
COPY src/generated/ src/generated/
RUN ./mvnw -B -ntp package -Dmaven.test.skip=true \
    && cp target/inspectia-backend-*.jar aplicacion.jar \
    && java -Djarmode=tools -jar aplicacion.jar extract --layers --destination /extraido \
    && mkdir -p /extraido/dependencies /extraido/spring-boot-loader /extraido/snapshot-dependencies /extraido/application

# ---- Ejecución: JRE 25, sin el JDK ni Maven ----
FROM eclipse-temurin:25-jre-noble

# Usuario sin privilegios con un UID fijo: actualizar.sh le da a él los archivos de los secretos.
RUN groupadd --system --gid 10001 inspectia \
    && useradd --system --uid 10001 --gid inspectia --home-dir /aplicacion --no-create-home \
        --shell /usr/sbin/nologin inspectia

WORKDIR /aplicacion
# Las capas de Spring Boot, de la que menos cambia a la que más: al subir una versión nueva solo cambia la última.
COPY --from=compilacion /extraido/dependencies/ ./
COPY --from=compilacion /extraido/spring-boot-loader/ ./
COPY --from=compilacion /extraido/snapshot-dependencies/ ./
COPY --from=compilacion /extraido/application/ ./
COPY --chmod=755 docker/entrada.sh /usr/local/bin/entrada.sh

# Memoria: el compose de producción le da 1 GB al contenedor. El heap usa hasta el 70 % (unos 700 MB) y el resto
# queda para el metaspace, los hilos y los búferes. Si se acaba, el proceso termina y Docker lo reinicia, en lugar
# de quedar vivo pero sin responder.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError" \
    LANG=C.UTF-8

USER 10001:10001
EXPOSE 8080

# Sano si el servidor responde: GET /api/v1/sesion sin token da 401 (RNF-04). Sin curl en la imagen, con bash.
HEALTHCHECK --interval=30s --timeout=5s --start-period=120s --retries=3 \
    CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8080 && printf 'GET /api/v1/sesion HTTP/1.1\\r\\nHost: localhost\\r\\nConnection: close\\r\\n\\r\\n' >&3 && read -r -t 4 estado <&3 && [[ $estado == 'HTTP/1.1 401'* ]]"]

ENTRYPOINT ["/usr/local/bin/entrada.sh"]
