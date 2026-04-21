# Stage 1: Base/Build stage
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Install curl for healthchecks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

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
# Enable VFS watch by default, but GRADLE_OPTS should match gradle.properties
ENV GRADLE_OPTS="-Xmx2g -XX:MaxMetaspaceSize=512m -XX:TieredStopAtLevel=1 -Dfile.encoding=UTF-8 -Dorg.gradle.vfs.watch=true"

# Use continuous build for classes to support hot-reloading efficiently with the daemon.
# This prevents the 'loop' lag by letting Gradle handle file watching natively.
ENTRYPOINT ["sh", "-c", "./gradlew classes && (./gradlew classes --continuous & ./gradlew bootRun)"]

# Stage 2: Runtime stage (Production)
FROM eclipse-temurin:17-jre-alpine AS production
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Build the application
FROM build AS builder
RUN ./gradlew bootJar --no-daemon && \
    cp build/libs/*.jar /app/app.jar

FROM production AS release
# Copy the fixed app.jar from the builder stage
COPY --from=builder --chown=spring:spring /app/app.jar app.jar

# Expose the application port
EXPOSE 8080

# Configure environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
