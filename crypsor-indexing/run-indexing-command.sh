#!/bin/bash -e

docker run --rm -ti \
        -v ${PWD}/:/anserini \
	-v ${PWD}/indexes:/indexes \
        -v /mnt/ceph/storage/data-in-progress/data-research/web-search/private-web-search-with-keyqueries/reranking-index-anserini/documents:/mnt/ceph/storage/data-in-progress/data-research/web-search/private-web-search-with-keyqueries/reranking-index-anserini/documents \
	-v /mnt/ceph/storage/data-in-progress/data-research/web-search/private-web-search-with-keyqueries/reranking-index-anserini/allow-lists:/mnt/ceph/storage/data-in-progress/data-research/web-search/private-web-search-with-keyqueries/reranking-index-anserini/allow-lists \
        -w /anserini \
        maven:3.6.3-openjdk-11-slim \
        ./target/appassembler/bin/IndexCollection -collection JsonCollection \
        -input /mnt/ceph/storage/data-in-progress/data-research/web-search/private-web-search-with-keyqueries/reranking-index-anserini/documents \
        -index /indexes/${1} \
        -generator DefaultLuceneDocumentGenerator \
        -whitelist /mnt/ceph/storage/data-in-progress/data-research/web-search/private-web-search-with-keyqueries/reranking-index-anserini/allow-lists/${1} \
        -threads 22

