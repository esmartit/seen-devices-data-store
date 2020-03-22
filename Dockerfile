FROM openjdk:8-jre-slim

EXPOSE 9000

RUN mkdir /app

COPY build/libs/*.jar /app/application.jar
#"-Xms512M", " -Xmx512M", " -XX:+UseG1GC",
ENTRYPOINT ["java", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-Djava.security.egd=file:/dev/./urandom","-jar", "/app/application.jar"]