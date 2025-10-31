# syntax=docker/dockerfile:1

################################################################################
# Stage 1: Build the Spring Boot application using Maven
################################################################################
FROM maven:3.9.3-eclipse-temurin-17-alpine AS build

# Set working directory inside the container
WORKDIR /item-integration-service

# Copy pom.xml and download dependencies (cache this layer)
#COPY pom.xml sop-config-service/pom.xml

#COPY ../pom.xml .	
#RUN mvn -B -e -C org.apache.maven.plugins:maven-dependency-plugin:3.1.2:go-offline
RUN ls -ltr
#RUN cat pom.xml
# Copy the source code
#COPY src ./src
# Configure Maven to use GitHub Maven Registry for dependencies (add settings.xml if necessary)
# For example, to authenticate and access private repositories, imagine you have a settings.xml prepared with your GitHub credentials
#COPY .m2/settings.xml /root/.m2/settings.xml
# Build the application and package it as a jar
#RUN mvn -B -e clean install -DskipTests=true

################################################################################
# Stage 2: Create a minimal runtime image
################################################################################
FROM eclipse-temurin:17-jre-alpine

# Create a non-root user to run the application
ARG APP_USER=springuser
ARG APP_UID=10001
RUN adduser -D -u ${APP_UID} ${APP_USER}

# Set working directory
WORKDIR /item-integration-service

RUN apk add --no-cache wget curl

# Copy the jar from the build stage
COPY src/main/resources/logback.xml /item-integration-service/logback.xml
COPY target/item-integration-service-1.0.0.jar  /item-integration-service/item-integration-service.jar
RUN mkdir logs
RUN pwd

#RUN tree
# Change ownership to non-root user
RUN chown ${APP_USER}:${APP_USER} item-integration-service.jar
RUN chmod 777 item-integration-service.jar
RUN chown ${APP_USER}:${APP_USER} logback.xml
RUN chmod 777 logback.xml
RUN chown ${APP_USER}:${APP_USER} /item-integration-service/logs
RUN chmod 777 /item-integration-service/logs
# Switch to non-root user
USER ${APP_USER}

# Expose the port the application listens on
EXPOSE 8084

# Set environment variables for production
ENV JAVA_OPTS="-Xms512m -Xmx1024m -Dspring.profiles.active=prod"

# Healthcheck to verify the app is running
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
CMD curl --no-verbose --tries=1 --spider http://localhost:8084/item-integration-service/actuator/health || exit 1

# Run the Spring Boot application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar item-integration-service.jar"]
