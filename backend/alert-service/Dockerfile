# Simple Dockerfile for Alert Service - uses pre-built JAR
# Build stage: Maven build (run locally, not in Docker)
# This Dockerfile expects JAR at: alert-service/target/alert-service-0.0.1-SNAPSHOT.jar

FROM eclipse-temurin:17-jre-alpine

# Security: Create non-root user  
RUN addgroup -g 1000 appuser && \
    adduser -u 1000 -G appuser -s /bin/sh -D appuser

WORKDIR /app

# Copy pre-built JAR from Maven build (target/ directory)
COPY alert-service/target/*-SNAPSHOT.jar app.jar

RUN chown -R appuser:appuser /app

USER appuser

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
