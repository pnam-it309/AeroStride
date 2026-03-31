# Stage 1: Base/Build stage
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Copy only the dependency files first to leverage Docker layer caching
COPY BE/gradle/ gradle/
COPY BE/gradlew BE/build.gradle BE/settings.gradle ./

# Download dependencies
RUN ./gradlew dependencies --no-daemon || true

# Copy the source code
COPY BE/src/ src/

# Development stage (for hot-reloading)
FROM build AS development
# Note: we don't build the jar here, we'll run via gradlew bootRun in Compose
EXPOSE 8080
ENTRYPOINT ["./gradlew", "bootRun", "--no-daemon"]

# Stage 2: Runtime stage (Production)
FROM eclipse-temurin:17-jre-alpine AS production
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the jar from the build stage (requires a build task)
# We need to run the build in the 'build' stage first
FROM build AS builder
RUN ./gradlew build -x test --no-daemon

FROM production AS release
COPY --from=builder --chown=spring:spring /app/build/libs/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Configure environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
