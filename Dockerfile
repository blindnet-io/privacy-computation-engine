FROM eclipse-temurin:17

COPY target/scala-3.1.3/devkit_pce.jar /srv/devkit_pce.jar

CMD java -jar /srv/devkit_pce.jar
