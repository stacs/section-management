FROM eclipse-temurin:21.0.5_11-jre-noble
RUN apt update && apt upgrade -y && rm -rf /var/lib/apt/lists/*
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
