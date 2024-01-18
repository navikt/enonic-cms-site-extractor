FROM ghcr.io/navikt/baseimages/temurin:17

COPY build/libs/enonic-cms-site-extractor-all.jar /app/app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 \
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/tmp/oom-dump.hprof"

ENV PORT=8081
EXPOSE $PORT
