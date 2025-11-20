# workskillai/Dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy only pom first for caching
COPY pom.xml ./
# If you use a multi-module project you may need to copy other files referenced by pom
RUN mvn -B -q dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn -B clean package -DskipTests

# Runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy built jar (wildcard to match the built artifact)
COPY --from=build /app/target/*.jar app.jar

# Expose the port you set in application.properties (server.port=2090)
EXPOSE 2090

# Use a non-root user? For simplicity we'll run default
ENTRYPOINT ["java","-jar","/app/app.jar"]
