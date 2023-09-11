#!/bin/bash -e

for INDEX in $(ls /mnt/ceph/storage/data-in-progress/data-research/web-search/private-web-search-with-keyqueries/reranking-index-anserini/allow-lists)
do
	./crypsor-indexing/run-indexing-command.sh ${INDEX}
done

