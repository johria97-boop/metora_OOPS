FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy

RUN useradd --system --uid 1001 mentora
WORKDIR /app

COPY --from=build --chown=mentora:mentora /workspace/target/mentora-backend-*.jar app.jar

USER mentora
EXPOSE 10000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
