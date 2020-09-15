FROM openjdk:11
VOLUME /executor/js
WORKDIR /executor/js
COPY build/libs/js-executor.jar /executor/js
EXPOSE 8080
CMD java -jar js-executor.jar