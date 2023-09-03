#!/bin/bash -e

docker run --rm -ti \
	-v ${PWD}:/anserini \
	-w /anserini \
	webis/wi21-query-obfuscation-with-keyqueries:0.0.1-dev \
	mvn -DskipTests=true -Dmaven.test.skip=true clean package appassembler:assemble

# I added '-DskipTests=true -Dmaven.test.skip=true' as described here: https://github.com/webis-de/wi21-query-obfuscation-with-keyqueries/issues/16
