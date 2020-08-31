FROM openjdk:11
VOLUME /executor/js
WORKDIR /executor/js
COPY impl/build/libs/impl.jar /executor/js
EXPOSE 8080
CMD java -jar impl.jar