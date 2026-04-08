# Multi-stage Dockerfile for Producer Service
# Build Stage: Compile the Spring Boot application
FROM eclipse-temurin:17-jdk-alpine AS builder

# Install Maven and additional tools
RUN apk add --no-cache curl maven

# Set working directory
WORKDIR /build

# Copy parent pom and common-models first (for dependency caching)
COPY pom.xml .
COPY common-models/ ./common-models/

# Copy producer-service
COPY producer-service/ ./producer-service/

# Build the application
RUN mvn clean package -pl producer-service -am -DskipTests

# Runtime Stage: Create minimal production image
FROM eclipse-temurin:17-jre-alpine

# Security: Create non-root user
RUN addgroup -g 1000 appuser && \
    adduser -u 1000 -G appuser -s /bin/sh -D appuser

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /build/producer-service/target/*.jar producer-service.jar

# Set ownership
RUN chown -R appuser:appuser /app

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD java -cp producer-service.jar org.springframework.boot.loader.JarLauncher health || \
        wget --spider -q http://localhost:8080/actuator/health || exit 1

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Set JVM options for production
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseStringDeduplication"

# Run the application
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar producer-service.jar"]
