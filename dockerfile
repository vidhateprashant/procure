FROM openjdk:11
COPY target/procure-0.0.1-SNAPSHOT.jar procure.jar
ENTRYPOINT ["java","-jar","procure.jar"]