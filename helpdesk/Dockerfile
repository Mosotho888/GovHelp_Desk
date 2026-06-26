# Stage 1: Builder - Building the application
FROM eclipse-temurin:21-jdk-jammy AS builder
# Set working directory
WORKDIR /opt/app
# Copy Maven wrapper and POM files
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# This command deletes the hidden Windows '\r' characters
RUN sed -i 's/\r$//' mvnw

# ✅ Grant execute permissions to mvnw
RUN chmod +x mvnw

# Download dependencies offline
RUN ./mvnw dependency:go-offline
# Copy source files and build the project
COPY ./src ./src

RUN ./mvnw clean package -DskipTests \
    -Dspotless.check.skip=true \
    -Dcheckstyle.skip=true \
    -Dpmd.skip=true \
    -Dspotbugs.skip=true


# Stage 2: Final - Create the runtime image
FROM eclipse-temurin:21-jre-jammy AS final
# Set working directory for the final image
WORKDIR /opt/app

RUN groupadd -r app && useradd -r -g app app
RUN mkdir -p /opt/app/uploads /opt/app/logs && chown -R app:app /opt/app

# Copy the JAR file from the builder stage
COPY --from=builder /opt/app/target/*.jar app.jar

RUN chown app:app app.jar

USER app

# Expose application port
EXPOSE 8080
# Define the entrypoint for the application
ENTRYPOINT ["java", "-jar", "app.jar"]
