FROM node:24-alpine@sha256:116b800c3754b440ee0aa3ed2f20f5bf3d99a554bfd9adeea09ea8017f6e85f5 AS interface
WORKDIR /web
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/index.html frontend/tsconfig.json frontend/vite.config.ts ./
COPY frontend/src src
RUN npm run build

FROM eclipse-temurin:25-jdk@sha256:ce2ef9d2d67a68de5e196d4de9d743327c44a10b57fe02ec39f572c652cacfd3 AS build
WORKDIR /src
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src/main src/main
COPY --from=interface /web/dist src/main/resources/static
RUN --mount=type=cache,target=/root/.m2 MAVEN_OPTS=-Xmx256m sh mvnw -B -Dmaven.test.skip=true package
FROM eclipse-temurin:25-jre@sha256:4ed132f6832e9804b123e89a849a7b4502756ae64a4ee08ebab939d9d5d0a9fa
WORKDIR /app
RUN mkdir uploads && chown 10001:10001 uploads
COPY --from=build /src/target/trilha-1.0.0.jar app.jar
USER 10001:10001
EXPOSE 8080 8443
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-XX:+ExitOnOutOfMemoryError","-jar","app.jar"]
