# 多架构支持：eclipse-temurin 官方镜像支持 linux/amd64、linux/arm64（Apple M1/M2/M3）
FROM maven:3.8-eclipse-temurin-11 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:11-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /data/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
