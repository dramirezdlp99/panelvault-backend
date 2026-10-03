# syntax=docker/dockerfile:1

# ---------- Etapa 1: compilar ----------
# Imagen con Maven y el JDK 21 solo para construir el .jar; no llega a produccion.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Primero solo el pom.xml para descargar dependencias: si el codigo cambia pero
# el pom no, Docker reutiliza esta capa y no vuelve a descargar todo.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src/ src/
# Las pruebas necesitan PostgreSQL; se ejecutan antes, en la maquina de desarrollo o en CI.
RUN mvn -B -q package -DskipTests

# ---------- Etapa 2: ejecutar ----------
# Solo el JRE: imagen mas pequena y con menos superficie de ataque.
FROM eclipse-temurin:21-jre
WORKDIR /app

# Nunca correr como root dentro del contenedor.
RUN useradd --system --uid 10001 --no-create-home panelvault
COPY --from=build /app/target/panelvault-backend-*.jar app.jar
USER panelvault

# Render asigna el puerto en la variable PORT; en local se usa 9096.
EXPOSE 9096

# Render Free da 512 MB: la JVM usa como maximo el 75 % y un recolector de basura sencillo.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
