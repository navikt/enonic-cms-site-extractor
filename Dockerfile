FROM ghcr.io/navikt/baseimages/temurin:17

COPY build/libs/enonic-cms-site-extractor-all.jar /app/app.jar

EXPOSE 8081
