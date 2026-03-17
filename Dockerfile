FROM eclipse-temurin:21-jre-alpine AS lightweight
WORKDIR /app

COPY build/libs/gmmzanncsu-all.jar /app/gmmzanncsu-all.jar
COPY src/main/webapp /app/webapp

EXPOSE 8080
CMD ["java", "-jar", "/app/gmmzanncsu-all.jar", "serve"]

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN set ENV=PROD && \
    apk update && apk upgrade --no-cache && \
    apk add --no-cache curl ca-certificates tzdata && \
    cp /usr/share/zoneinfo/Europe/Rome /etc/localtime && \
    echo "Europe/Rome" > /etc/timezone \
    && apk del tzdata \
    && rm -rf /var/cache/apk/*

COPY build/libs/gmmzanncsu-all.jar /app/gmmzanncsu-all.jar
COPY src/main/webapp /app/webapp
COPY files/dati.sqlite /app/files/dati.sqlite

EXPOSE 8080
CMD ["java", "-jar", "/app/gmmzanncsu-all.jar", "serve"]

