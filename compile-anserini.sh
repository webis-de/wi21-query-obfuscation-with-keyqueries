#!/bin/bash -e

docker run --rm -ti \
	-v ${PWD}:/anserini \
	-w /anserini \
	maven:3.6.3-openjdk-11-slim \
	mvn clean package appassembler:assemble

