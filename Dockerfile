# ==========================================
# 1. ETAPA DE CONSTRUCCIÓN (BUILD STAGE)
# ==========================================
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copiar POM para aprovechar la caché de capas de dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fuente y empaquetar
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# 2. ETAPA DE EJECUCIÓN (RUNTIME STAGE)
# ==========================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup && mkdir -p /app/logs && chown -R appuser:appgroup /app
USER appuser

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8083

ENV SERVER_PORT=8083 \
    JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
