# A imagem "openjdk" foi descontinuada no Docker Hub; a Temurin é a substituta recomendada.
FROM eclipse-temurin:25-jre
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
