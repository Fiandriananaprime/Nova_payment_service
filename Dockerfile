FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn ./.mvn
COPY src ./src
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV SERVER_PORT=3006
COPY --from=build /app/target/*.jar /app/app.jar
EXPOSE 3006
ENTRYPOINT ["java", "-jar", "/app/app.jar"]