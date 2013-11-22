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

RULES=`find -L $1 -name "*.rules"`
for rulefile in $RULES
do
    python scripts/rulecompiler.py $rulefile ${rulefile%.rules}.crules
    echo -n "${rulefile%.rules}.crules:" >> $1/programFiles.arg
done

FACTS=`find -L $1 -name "*.facts"`
if [ -n "$FACTS" ]
then
    for factfile in $FACTS
    do
	echo "converting $factfile"
	sed 's/[\(,]/\t/g;s/[\)]/\t/g' ${factfile} > ${factfile%.facts}.cfacts
	echo -n "${factfile%.facts}.cfacts:" >> $1/programFiles.arg
    done
fi

GRAPH=`find -L $1 -name "*.graph"`
if [ -n "$GRAPH" ]
then
    for graphfile in $GRAPH
    do
	echo -n "$graphfile:" >> $1/programFiles.arg
    done
fi
