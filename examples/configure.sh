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
    echo -e "  --mu value                    Set SRW regularization hyperparameter"
    echo -e "  --epochs value                Set number of training epochs"
    echo -e "  --tester value                Set tester variant"
    echo -e "  --trainer value                Set trainer variant"
    echo -e "  --srw value[:arg...]          Set SRW"
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
    elif [ "--epochs" = "$NAME" ]
    then
	echo -e "EPOCHS=$VALUE" >> Makefile.in
    elif [ "--tester" = "$NAME" ]
    then
	echo -e "TESTER=$VALUE" >> Makefile.in
    elif [ "--trainer" = "$NAME" ]
    then
	echo -e "TRAINER=$VALUE" >> Makefile.in
    elif [ "--srw" = "$NAME" ]
    then
	echo -e "SRW=$VALUE" >> Makefile.in
    else
	echo -e "Unrecognized option: $NAME"
	exit 0
    fi
    shift
    shift
done

if [ -n "$1" ]
then
    echo -e "Unrecognized option: $1"
fi

cat  >> Makefile.in <<EOF
SCRIPTS=\$(shell pwd)/scripts
#### JVM
ifeq (\$(strip \$(JOPTS)),)
JOPTS=
endif
ifeq (\$(suffix \$(PROPPR)),'jar')
CP:=.:\${PROPPR}
else
CP:=.:\${PROPPR}/bin:\${PROPPR}/conf/:\${PROPPR}/lib/*
endif
#### Hyperparameters
ifeq (\$(strip \$(EPSILON)),)
EPSILON=1e-5
endif
ifeq (\$(strip \$(ALPHA)),)
ALPHA=0.01
endif
ifeq (\$(strip \$(PROVER)),)
PROVER=dpr
endif
ifeq (\$(strip \$(MU)),)
MU=0.001
endif
ifeq (\$(strip \$(ETA)),)
ETA=1.0
endif
ifeq (\$(strip \$(SRW)),)
SRW=l2plocal:mu=\$(MU):eta=\$(ETA)
endif
ifeq (\$(strip \$(THREADS)),)
THREADS=3
endif
ifeq (\$(strip \$(EPOCHS)),)
EPOCHS=20
endif
ifeq (\$(strip \$(TESTER)),)
TESTER=rt
endif
ifeq (\$(strip \$(TRAINER)),)
TRAINER=trove.mrr
endif

EOF

echo "Done."