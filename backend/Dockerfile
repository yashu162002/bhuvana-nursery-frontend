# Production image for the Bhuvana Nursery API (Render).
# The build stage uses the same Java 21 target as pom.xml; the runtime is a
# slim JRE so the container starts quickly and contains no build tooling.
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Copy the whole project and build with the same command verified locally.
# (Avoid dependency:go-offline here: it can fail on optional/plugin resolution
# and would make an otherwise valid production image unbuildable.)
COPY pom.xml ./
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy

RUN groupadd --system app \
    && useradd --system --gid app --home-dir /app app

WORKDIR /app

COPY --from=build /workspace/target/nursery-api-0.0.1-SNAPSHOT.jar app.jar

# Keep the non-root user able to write local uploads if someone starts the
# image with IMAGE_STORAGE=local for testing. Production uses R2.
RUN mkdir -p /app/uploads && chown -R app:app /app

USER app

EXPOSE 8080

# Render injects PORT; the application binds to ${PORT:8080}.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
