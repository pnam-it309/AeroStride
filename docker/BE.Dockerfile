# Stage 1: Base/Build stage
FROM gradle:8.5-jdk17-alpine AS build
WORKDIR /app

# Install curl for healthchecks
RUN apk add --no-cache curl

# Copy only the dependency files first to leverage Docker layer caching
COPY BE/gradle/ gradle/
COPY BE/gradlew BE/build.gradle BE/settings.gradle BE/gradle.properties* ./
RUN chmod +x gradlew

# Download dependencies (this will be cached unless gradle files change)
RUN ./gradlew dependencies --no-daemon --build-cache || true

# Copy the source code
COPY BE/src/ src/

# Development stage (for hot-reloading)
FROM build AS development
EXPOSE 8080
# Removed --configuration-cache to prevent environment leak issues in dev
ENTRYPOINT ["./gradlew", "bootRun", "-t", "--no-daemon"]

# Stage 2: Runtime stage (Production)
FROM eclipse-temurin:17-jre-alpine AS production
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Build the application
FROM build AS builder
RUN ./gradlew bootJar --no-daemon

FROM production AS release
# Copy the specific executable jar created by the bootJar task
COPY --from=builder --chown=spring:spring /app/build/libs/app.jar app.jar

# Expose the application port
EXPOSE 8080

# Configure environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
