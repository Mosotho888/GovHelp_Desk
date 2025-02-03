# Stage 1: Builder - Building the application
FROM eclipse-temurin:22.0.2_9-jdk-jammy AS builder
# Set working directory
WORKDIR /opt/app
# Copy Maven wrapper and POM files
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Download dependencies offline
RUN ./mvnw dependency:go-offline
# Copy source files and build the project
COPY ./src ./src
RUN ./mvnw clean install


# Stage 2: Final - Create the runtime image
FROM eclipse-temurin:22.0.2_9-jre-jammy AS final
# Set working directory for the final image
WORKDIR /opt/app
# Expose application port
EXPOSE 8080
# Copy the JAR file from the builder stage
COPY --from=builder /opt/app/target/*.jar /opt/app/*.jar
# Define the entrypoint for the application
ENTRYPOINT ["java", "-jar", "/opt/app/*.jar"]
