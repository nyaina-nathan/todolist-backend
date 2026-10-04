FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle/wrapper/ ./gradle/wrapper/
RUN ./gradlew dependencies --no-daemon > /dev/null
COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon


FROM eclipse-temurin:21-jre
RUN useradd -r -u 1001 appuser
COPY --from=build /app/build/libs/*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]