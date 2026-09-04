FROM maven:3.9.9-eclipse-temurin-23 AS build
WORKDIR /usr/src/app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:23-jdk
WORKDIR /app

COPY --from=build /usr/src/app/target/*.jar app.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]