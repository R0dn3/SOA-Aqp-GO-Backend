# ── Etapa 1: Build ───────────────────────────────────────────
FROM gradle:8.14-jdk21 AS build

WORKDIR /app

# Copia solo los archivos de dependencias primero (mejor cache)
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

# Descarga dependencias (se cachea si build.gradle no cambia)
RUN gradle dependencies --no-daemon || true

# Copia el código fuente y compila
COPY src ./src
RUN gradle bootJar --no-daemon -x test

# ── Etapa 2: Runtime ──────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Instala pg_dump para los backups
RUN apk add --no-cache postgresql-client

# Crea carpeta para backups locales
RUN mkdir -p /app/backups

# Copia el jar generado en la etapa anterior
COPY --from=build /app/build/libs/*.jar app.jar

# Puerto que expone Spring
EXPOSE 8080

# Variables de entorno con valores por defecto
# Las reales se configuran en Render como Environment Variables
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "app.jar"]