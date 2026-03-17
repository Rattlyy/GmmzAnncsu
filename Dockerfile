FROM eclipse-temurin:21-jre AS lightweight
WORKDIR /app

COPY build/libs/gmmzanncsu-all.jar /app/gmmzanncsu-all.jar

EXPOSE 8080
CMD ["java", "-jar", "/app/gmmzanncsu-all.jar", "serve"]

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

COPY build/libs/gmmzanncsu-all.jar /app/gmmzanncsu-all.jar
COPY files/dati.sqlite /app/files/dati.sqlite

EXPOSE 8080
CMD ["java", "-jar", "/app/gmmzanncsu-all.jar", "serve"]

