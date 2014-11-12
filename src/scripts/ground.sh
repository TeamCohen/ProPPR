#!/bin/bash

CP=".:bin/:lib/*:conf/"
DIR=$1
shift
DATA=$1
shift

if [ -z "$DIR" -o -z "$DATA" ]
then
    echo Usage:
    echo     $0 projectDirectory datafile [options to pass to ExampleCooker]
    exit 1
fi

ARGS=$DIR/programFiles.arg

if [ ! -f $ARGS ]
then
    echo --- Compiling $DIR first:
    echo ${0%ground.sh}compile.sh $DIR
    ${0%ground.sh}compile.sh $DIR
    if [ $? -ne 0 ]
    then
	exit 1
    fi
fi

PROGRAM=`cat $DIR/programFiles.arg`

echo --- Cooking $DATA:
echo java -cp $CP edu.cmu.ml.proppr.ExampleCooker --programFiles ${PROGRAM%:} --data $DATA --output ${DATA%.data}.cooked $@
java -cp $CP edu.cmu.ml.proppr.ExampleCooker --programFiles ${PROGRAM%:} --data $DATA --output ${DATA%.data}.cooked $@

echo --- Done.