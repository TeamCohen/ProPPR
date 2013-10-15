#!/bin/bash

#todo
#if ndef $1 print usage

if [ -z $1 ]
then
    echo Usage:
    echo $0 programDirectory
    exit 1
fi

rm -f $1/programFiles.arg

for rulefile in $1/*.rules
do
    python scripts/rulecompiler.py $rulefile ${rulefile%.rules}.crules
    echo -n "${rulefile%.rules}.crules:" >> $1/programFiles.arg
done

for factfile in $1/*.facts
do
    echo "converting $factfile"
    sed 's/[\(,]/\t/g;s/[\)]/\t/g' ${factfile} > ${factfile%.facts}.cfacts
    echo -n "${factfile%.facts}.cfacts:" >> $1/programFiles.arg
done

for graphfile in $1/*.graph
do
    echo -n "$graphfile:" >> $1/programFiles.arg
done

