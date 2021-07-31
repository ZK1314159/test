FROM openjdk:8-jdk-alpine

VOLUME /tmp

ADD target/test-1.0.jar app.jar

ENV JAVA_OPTS=" "

ENTRYPOINT exec java $JAVA_OPTS -jar /app.jar