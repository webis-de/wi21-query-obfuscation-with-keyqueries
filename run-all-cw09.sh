#!/bin/bash -e

TOPICS=(2 8 11 17 18 26 30 33 38 40 46 47 50 57 59 61 62 66 67 78 82 88 89 95 98 104 105 109 111 117 119 121 123 128 131 136 140 142 147 152 156 162 168 173 175 177 182 196 199)

for TOPIC in "${TOPICS[@]}"
do
	echo "Run Topic ${TOPIC}"
	time ./run-cw09-topic.sh -scramblingApproach nounphrase -bm25 -topic ${TOPIC}
	time ./run-cw09-topic.sh -scramblingApproach nounphrase -qld -topic ${TOPIC}
done
