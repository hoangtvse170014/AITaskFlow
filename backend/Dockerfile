# Stage 1: Build
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# Install Maven
RUN apk add --no-cache maven

# Copy dependency files first for better caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

# Add non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy the jar file
COPY --from=build /app/target/*.jar app.jar

# Change ownership
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port - Render uses PORT env variable
EXPOSE 8080

# Set default port if not provided
ENV PORT=8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT:-8080}/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -jar app.jar --server.port=${PORT:-8080}"]
