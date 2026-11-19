FROM eclipse-temurin:24-jdk@sha256:7493205ffe6caa8074fa8a06a276bb1c5ac41d3dd0fd43a0db66d7f776e80b3e AS build
WORKDIR /src
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src/main src/main
RUN --mount=type=cache,target=/root/.m2 MAVEN_OPTS=-Xmx256m sh mvnw -B -Dmaven.test.skip=true package
FROM eclipse-temurin:24-jre@sha256:8cb2387a28af84cf0db0948d9c67d4480192f4e567027a3963f145d218e8b4f2
WORKDIR /app
RUN mkdir uploads && chown 10001:10001 uploads
COPY --from=build /src/target/selecao-1.0.0.jar app.jar
USER 10001:10001
EXPOSE 8080 8443
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-XX:+ExitOnOutOfMemoryError","-jar","app.jar"]
