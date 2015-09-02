#!/bin/bash

if [ -z "$3" ]; then
    echo "Usage: $0 queryData answerData label"
    echo "       (make sure PROPPR is set)"
    exit 0
fi

python ${PROPPR}/scripts/answermetrics.py --data $1 --answers $2 --metric recall --metric mrr --metric map |\
grep -e "micro:" -e "macro:" |\
awk '{print $3}' |\
tr "\n" " " |\
awk -v LABEL=$3 '{print LABEL,$0}'
