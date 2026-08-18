# Stage 1: Base/Build stage
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Set Gradle User Home inside image layer for Docker layer caching
ENV GRADLE_USER_HOME=/app/.gradle

# Copy only dependency files first to leverage Docker layer caching on Render
COPY BE/gradle/ gradle/
COPY BE/gradlew BE/build.gradle BE/settings.gradle BE/gradle.properties* ./
RUN chmod +x gradlew

# Download dependencies directly into image layer (Docker will cache this layer if build.gradle hasn't changed)
RUN ./gradlew dependencies --no-daemon

# Development stage (for hot-reloading)
FROM build AS development

ARG BE_PORT
EXPOSE ${BE_PORT}

# Gradle JVM/runtime tuning is injected via GRADLE_OPTS from docker env.
ENV GRADLE_OPTS=${GRADLE_OPTS}

COPY docker/be-dev-entrypoint.sh /usr/local/bin/be-dev-entrypoint.sh
RUN chmod +x /usr/local/bin/be-dev-entrypoint.sh
ENTRYPOINT ["/usr/local/bin/be-dev-entrypoint.sh"]

# Stage 2: Builder stage (Production)
FROM build AS builder

# Copy source code after dependencies layer is cached
COPY BE/src/ src/

# Build JAR fast without running tests during Docker build
RUN ./gradlew bootJar -x test --no-daemon && \
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

# Configure environment variables
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}
ENV JAVA_OPTS=${JAVA_OPTS}

# Healthcheck uses SERVER_PORT from runtime env
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${SERVER_PORT}/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:--XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k} -jar /app-bin/app.jar"]
