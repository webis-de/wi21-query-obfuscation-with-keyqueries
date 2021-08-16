#!/bin/bash -e

docker run --rm -ti \
	-v ${PWD}:/anserini \
	-v /mnt/raid/data/av80sybu/anserini/indexes/lucene-index.cw12b13:/index \
	-v /mnt/ceph/storage/data-in-progress/data-research/web-search/private-web-search-with-keyqueries/scrambling-on-anserini:/out \
	-w /anserini \
	maven:3.6.3-openjdk-11-slim \
	java -cp target/anserini-0.9.5-SNAPSHOT-fatjar.jar \
	de.webis.crypsor.Main \
		-output /out \
		-index /index \
		-threads 34 \
		${@}

