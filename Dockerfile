FROM gradle:8.11.1-jdk21 AS build
WORKDIR /src

COPY gradle gradle
COPY gradlew gradlew
COPY gradlew.bat gradlew.bat
COPY settings.gradle.kts settings.gradle.kts
COPY build.gradle.kts build.gradle.kts
COPY gradle.properties gradle.properties
COPY src src

RUN ./gradlew --no-daemon shadowJar

FROM eclipse-temurin:21-jre AS lightweight
WORKDIR /app

COPY --from=build /src/build/libs/gmmzanncsu-all.jar /app/gmmzanncsu-all.jar

EXPOSE 8080
CMD ["java", "-jar", "/app/gmmzanncsu-all.jar", "serve"]

FROM eclipse-temurin:21-jre AS init
WORKDIR /app

COPY --from=build /src/build/libs/gmmzanncsu-all.jar /app/gmmzanncsu-all.jar
RUN java -jar /app/gmmzanncsu-all.jar init

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

COPY --from=build /src/build/libs/gmmzanncsu-all.jar /app/gmmzanncsu-all.jar
COPY --from=init /app/files/dati.sqlite /app/files/dati.sqlite

EXPOSE 8080
CMD ["java", "-jar", "/app/gmmzanncsu-all.jar", "serve"]

