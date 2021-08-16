#!/bin/bash -e

docker run --rm -ti \
        -v ${PWD}/../:/anserini \
	-v ${PWD}/indexes/${1}:/indexes \
	-v /mnt/ceph/storage/data-in-progress/data-research/web-search/private-web-search-with-keyqueries/reranking-index-anserini/${1}:/runs/ \
        -w /anserini \
        maven:3.6.3-openjdk-11-slim \
        target/appassembler/bin/SearchCollection \
	-topicreader Webxml \
	-topics src/main/resources/topics-and-qrels/topics.cw-private.txt \
	-output /runs/${1}.txt \
        -index /indexes \
	-bm25 \
        -threads 22

