FROM openjdk:17
MAINTAINER Manikumarp
ADD target/HelloWorld-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar", "/app.jar"]