FROM eclipse-temurin:21-jdk@sha256:db5cc6341a1d7bc40cdb51e1f0d9824f3bb2cc8107d29da421dc4df7d11f05f1 AS build
WORKDIR /src
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src/main src/main
RUN --mount=type=cache,target=/root/.m2 MAVEN_OPTS=-Xmx256m sh mvnw -B -Dmaven.test.skip=true package
FROM eclipse-temurin:21-jre@sha256:ca73b4d3961703d6d7830a4361f49ef0478f8f28b39defc07bb72f4cc7f834f6
WORKDIR /app
RUN mkdir uploads && chown 10001:10001 uploads
COPY --from=build /src/target/selecao-1.0.0.jar app.jar
USER 10001:10001
EXPOSE 8080 8443
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-XX:+ExitOnOutOfMemoryError","-jar","app.jar"]
