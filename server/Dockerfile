# Build and runtime are separate stages so the shipped image carries a JRE and a jar, not Maven,
# the compiler, and ~200MB of downloaded dependencies.

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# pom.xml alone first: dependencies only re-download when the pom actually changes, so ordinary
# source edits reuse this layer instead of refetching the world on every build.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# -----------------------------------------------------------------------------

FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

# A compromised process should not be root inside the container.
RUN useradd --system --create-home --shell /usr/sbin/nologin spring
USER spring

# Wildcard rather than the literal name so a version bump in pom.xml does not break the build.
COPY --from=build --chown=spring:spring /build/target/*.jar app.jar

EXPOSE 8080

# MaxRAMPercentage, not -Xmx: the JVM then sizes the heap from the container's memory limit, which
# is what actually applies on a small instance. Without it the JVM reads the host's total memory
# and can size a heap the container is not allowed to use.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70.0"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
