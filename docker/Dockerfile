# Alpine Linux with OpenJDK JRE
FROM maven:3.6.0-jdk-8

EXPOSE 4567

# create directory
RUN mkdir -p /usr/app/des
WORKDIR /usr/app/des

# copy compiled jar in container
COPY RecapSim-package/RecapSim-API/target/RecapSim-API-0.0.1-SNAPSHOT-jar-with-dependencies.jar /usr/app/des/RecapSim-API-0.0.1-SNAPSHOT-jar-with-dependencies.jar

# run application with this command line 
CMD java -jar RecapSim-API-0.0.1-SNAPSHOT-jar-with-dependencies.jar
