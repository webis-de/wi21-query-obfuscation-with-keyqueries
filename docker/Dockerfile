FROM maven:3.6.3-openjdk-11-slim

ADD docker/anserini /root/.m2/repository/io/anserini/anserini

ADD docker/corpora /root/.m2/repository/de/webis/corpora

ADD src pom.xml /delete-me/

RUN cd /delete-me \
	&& mvn dependency:resolve

RUN cd /delete-me \
	&& (mvn clean package appassembler:assemble || echo "this fails the first time")

RUN cd /delete-me \
	&& mvn clean package appassembler:assemble \
	&& cd / && rm -R delete-me
