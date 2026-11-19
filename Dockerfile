FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src/main src/main
RUN chmod +x mvnw && ./mvnw -B -DskipTests package
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN mkdir uploads && chown 10001:10001 uploads
COPY --from=build /src/target/selecao-1.0.0.jar app.jar
USER 10001:10001
ENTRYPOINT ["java","-jar","app.jar"]
