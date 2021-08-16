#!/bin/bash -e

TOPICS=(207 213 222 236 253 254 262 266 273 286 287 209 214)

for TOPIC in "${TOPICS[@]}"
do
	echo "Run Topic ${TOPIC}"
	time ./run-cw12-topic.sh -scramblingApproach nounphrase -bm25 -topic ${TOPIC}
	time ./run-cw12-topic.sh -scramblingApproach nounphrase -qld -topic ${TOPIC}
done
