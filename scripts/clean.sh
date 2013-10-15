#!/bin/bash

if [ -z $1 ]
then
    echo Usage:
    echo $0 programDirectory
    exit 1
fi

rm -f $1/*.crules
rm -f $1/*.cfacts
rm -f $1/*.cooked
rm -f $1/programFiles.arg
echo Done.