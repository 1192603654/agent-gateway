# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY gateway-core/pom.xml gateway-core/
COPY gateway-server/pom.xml gateway-server/
RUN mvn dependency:go-offline

COPY gateway-core/src gateway-core/src
COPY gateway-server/src gateway-server/src
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/gateway-server/target/gateway-server-1.0-SNAPSHOT.jar app.jar

RUN mkdir -p /app/data
VOLUME /app/data
EXPOSE 9999
ENTRYPOINT ["java", "-jar", "app.jar"]
