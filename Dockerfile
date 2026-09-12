FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
RUN mkdir -p /app/fonts
COPY --from=build /app/target/health-assistant-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xms256m", "-Xmx768m", "-jar", "app.jar"]
