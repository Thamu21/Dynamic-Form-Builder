# Production-ready Dockerfile for Spring Boot
# Best Practices:
# 1. Use a lightweight base image (Alpine)
# 2. Run as a non-root user for security
# 3. Use ENTRYPOINT with JSON array for signal handling
# 4. Leverage JVM container awareness
# 5. Externalize configuration via environment variables

# Use Eclipse Temurin JRE on Alpine for a small, secure production image
FROM eclipse-temurin:17-jre-alpine

# Set working directory inside the container
WORKDIR /app

# Create a non-root user and group for security
# This prevents the application from having root privileges inside the container
RUN addgroup -S spring && adduser -S spring -G spring

# Mark the container as running under a specific user
USER spring:spring

# Copy the JAR file from the target directory to the container
# Replace 'formforge-backend-1.0.0.jar' with your actual JAR name if it differs
ARG JAR_FILE=target/formforge-backend-1.0.0.jar
COPY ${JAR_FILE} app.jar

# The application will listen on the port defined by the PORT environment variable.
# Default to 8080 if PORT is not provided.
ENV PORT=8080

# Inform Docker that the container listens on the specified port at runtime.
EXPOSE ${PORT}

# Application Configuration:
# - Use -XshowSettings:vm to log container memory/CPU limits on startup
# - Use -XX:+UseG1GC for modern garbage collection
# - Define the server port using the PORT env variable support in Spring Boot
ENTRYPOINT ["java", \
            "-XshowSettings:vm", \
            "-XX:+UseG1GC", \
            "-Dserver.port=${PORT}", \
            "-jar", \
            "app.jar"]
