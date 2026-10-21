# A imagem "openjdk" foi descontinuada no Docker Hub; a Temurin é a substituta recomendada.
FROM eclipse-temurin:25-jre

WORKDIR /app

# A aplicação não roda como root dentro do container
RUN groupadd --system app \
    && useradd --system --gid app --home-dir /app app \
    && mkdir -p /app/logs \
    && chown app:app /app/logs

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

USER app

ENV LOG_ARQUIVO=/app/logs/projeto.log
VOLUME /app/logs

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
