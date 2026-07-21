# Stage 1: Base/Build stage
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Set Gradle User Home to a location that can be persisted via volumes/cache
ENV GRADLE_USER_HOME=/app/.gradle

# (Removed curl installation as it's unnecessary and increases build time)
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

# Gradle JVM/runtime tuning is injected via GRADLE_OPTS from docker env.
# gradle.properties holds static defaults; GRADLE_OPTS can override org.gradle.*
# settings at runtime using -Dorg.gradle.* system properties (env-driven).
ENV GRADLE_OPTS=${GRADLE_OPTS}

# Development Entrypoint: bootRun plus a watcher that recompiles src/ into
# build/ so spring-boot-devtools can hot-reload. See the script for why the
# watcher polls instead of using `gradle --continuous`.
COPY docker/be-dev-entrypoint.sh /usr/local/bin/be-dev-entrypoint.sh
RUN chmod +x /usr/local/bin/be-dev-entrypoint.sh
ENTRYPOINT ["/usr/local/bin/be-dev-entrypoint.sh"]

# Stage 2: Builder stage (Production)
FROM build AS builder
# Use cache mount for the actual build process as well
RUN --mount=type=cache,target=/app/.gradle ./gradlew bootJar && \
    cp build/libs/*.jar /app/app.jar

# Stage 3: Runtime stage (Production)
FROM eclipse-temurin:17-jre-alpine AS production
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Create app-bin directory and set ownership so spring user can access it
RUN mkdir /app-bin && chown spring:spring /app-bin

USER spring:spring

# Copy the fixed app.jar from the builder stage to /app-bin/app.jar
# This prevents it from being hidden if the ../BE:/app volume is mounted
COPY --from=builder --chown=spring:spring /app/app.jar /app-bin/app.jar

# Expose the application port (from env)
ARG BE_PORT
EXPOSE ${BE_PORT}

# Configure environment variables (from .env, not hardcoded)
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}
ENV JAVA_OPTS=${JAVA_OPTS}

# Healthcheck uses SERVER_PORT from runtime env
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${SERVER_PORT}/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app-bin/app.jar"]
