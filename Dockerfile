# ── Etapa 1: compilacion ─────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

# Copiamos pom.xml primero: si no cambian las dependencias,
# Docker reutiliza esta capa en cache y no las vuelve a descargar
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Ahora si copiamos el codigo fuente y compilamos
COPY src ./src
RUN mvn package -DskipTests -q

# ── Etapa 2: imagen de runtime ───────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Buena practica: correr la app como usuario no-root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiamos unicamente el JAR generado en la etapa anterior
COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080 8081
ENTRYPOINT ["java", "-jar", "app.jar"]