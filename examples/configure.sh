#!/bin/bash

if [ "--help" = "$1" ]
then
    echo -e "Available options:"
    echo -e "  --proppr path/to/proppr       Should contain bin, lib, conf"
    echo -e "  --proppr path/to/proppr.jar   Alternate ProPPR spec"
    echo -e "  --jopts java_options          Example: \"-Xmx6g\""
    echo -e "  --threads nthreads            Ideally #cores - 1"
    echo -e "  --alpha value                 Set DPR reset hyperparameter"
    echo -e "  --epsilon value               Set DPR error bound hyperparameter"
    echo -e "  --mu value                    Set SRW ? hyperparameter"
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
    elif [ "--alpha" = "$NAME" ]
    then
	echo -e "ALPHA=$VALUE" >> Makefile.in
    elif [ "--epsilon" = "$NAME" ]
    then
	echo -e "EPSILON=$VALUE" >> Makefile.in
    elif [ "--mu" = "$NAME" ]
    then
	echo -e "MU=$VALUE" >> Makefile.in
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

echo -e "ifeq (\$(strip \$(ALPHA)),)" >> Makefile.in
echo -e "ALPHA=0.2" >> Makefile.in
echo -e "endif" >> Makefile.in

echo -e "SCRIPTS=\$(shell pwd)/scripts" >> Makefile.in

echo "Done."