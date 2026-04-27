# Stage 1: Base/Build stage
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Set Gradle User Home to a location that can be persisted via volumes/cache
ENV GRADLE_USER_HOME=/app/.gradle

# Install curl for healthchecks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy only the dependency files first to leverage Docker layer caching
COPY BE/gradle/ gradle/
COPY BE/gradlew BE/build.gradle BE/settings.gradle BE/gradle.properties* ./
RUN chmod +x gradlew

# Download dependencies using BuildKit cache mount for much faster builds
# This cache persists between builds even if the dependency layer is invalidated
RUN --mount=type=cache,target=/app/.gradle ./gradlew dependencies --no-daemon --build-cache || true

# Copy the source code
COPY BE/src/ src/

# Development stage (for hot-reloading)
FROM build AS development
EXPOSE 8080

# Performance tweaks for Gradle in Docker
ENV GRADLE_OPTS="-Xmx2g -XX:MaxMetaspaceSize=512m -XX:TieredStopAtLevel=1 -Dfile.encoding=UTF-8"

# Development Entrypoint:
# Runs compilation in background and starts the app with DevTools support
ENTRYPOINT ["sh", "-c", "./gradlew classes --continuous & ./gradlew bootRun"]

# Stage 2: Builder stage (Production)
FROM build AS builder
# Use cache mount for the actual build process as well
RUN --mount=type=cache,target=/app/.gradle ./gradlew bootJar --no-daemon && \
    cp build/libs/*.jar /app/app.jar

# Stage 3: Runtime stage (Production)
FROM eclipse-temurin:17-jre-alpine AS production
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the fixed app.jar from the builder stage
COPY --from=builder --chown=spring:spring /app/app.jar app.jar

# Expose the application port
EXPOSE 8080

# Configure environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# Healthcheck to ensure the container is healthy
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
