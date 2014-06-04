#!/bin/bash

if [ "--help" = "$1" ]
then
    echo -e "Available options:"
    echo -e "  --proppr path/to/proppr       Should contain bin, lib, conf"
    echo -e "  --proppr path/to/proppr.jar   Alternate ProPPR spec"
    echo -e "  --jopts java_options          Example: \"-Xmx6g\""
    echo -e "  --threads nthreads            Ideally #cores - 1"
    exit 0
fi

if [ -n "$1" ]
then
    rm -f Makefile.in
fi

while [ -n "$2" ];
do
    NAME=$1
    VALUE=$2
    if [ "--proppr" = "$NAME" ]
    then
	echo -e "PROPPR=$VALUE" >> Makefile.in
    elif [ "--jopts" = "$NAME" ]
	then
	echo -e "JOPTS=$VALUE" >> Makefile.in
    elif [ "--threads" = "$NAME" ]
    then
	echo -e "THREADS=$VALUE" >> Makefile.in
    else
	echo -e "Unrecognized option: $NAME"
    fi
    shift
    shift
done

if [ -n "$1" ]
then
    echo -e "Unrecognized option: $1"
fi

echo -e "ifeq (\$(strip \$(JOPTS)),)" >> Makefile.in
echo -e "JOPTS=" >> Makefile.in
echo -e "endif" >> Makefile.in

echo -e "ifeq (\$(strip \$(THREADS)),)" >> Makefile.in
echo -e "THREADS=4" >> Makefile.in
echo -e "endif" >> Makefile.in

echo -e "ifeq (\$(suffix \$(PROPPR)),'jar')" >> Makefile.in
echo -e "CP:=.:\${PROPPR}" >> Makefile.in
echo -e "else" >> Makefile.in
echo -e "CP:=.:\${PROPPR}/bin:\${PROPPR}/conf/:\${PROPPR}/lib/*" >> Makefile.in
echo -e "endif" >> Makefile.in

echo -e "SCRIPTS=\$(shell pwd)/scripts

echo "Done."