FROM eclipse-temurin:21-jdk AS build

ARG MODULE
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY services/ services/
RUN chmod +x mvnw && ./mvnw -pl services/${MODULE} -am -DskipTests package

FROM eclipse-temurin:21-jre

ARG MODULE
RUN apt-get update \
    && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --home-dir /app owoke
WORKDIR /app
COPY --from=build /workspace/services/${MODULE}/target/${MODULE}-0.0.1-SNAPSHOT.jar app.jar
USER 10001
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar"]
