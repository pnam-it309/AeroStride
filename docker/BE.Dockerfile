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

ARG BE_PORT
EXPOSE ${BE_PORT}

# Performance tweaks for Gradle in Docker (injected from env, not hardcoded)
ENV GRADLE_OPTS=${GRADLE_OPTS}

# Development Entrypoint:
# Runs compilation quietly in background (for DevTools hot-reload) and starts the app
ENTRYPOINT ["sh", "-c", "(while true; do ./gradlew classes -q --no-daemon; sleep 5; done) & ./gradlew bootRun"]

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

# Expose the application port (from env)
ARG BE_PORT
EXPOSE ${BE_PORT}

# Configure environment variables (from .env, not hardcoded)
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}
ENV JAVA_OPTS=${JAVA_OPTS}

# Healthcheck uses SERVER_PORT and APP_API_PREFIX from runtime env
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${SERVER_PORT}${APP_API_PREFIX}/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
