#!/bin/bash

PROPPR="/home/krivard/git/rinkitink/ProPPR"
if [ -n "$1" ];
then
    PROPPR="$1"
fi

mkdir -p epoch_tuning
for epoch in 80 100;#10 20 40 60 80 100;
do
    make clean.fast
    ./configure.sh --proppr $PROPPR --threads 3 --epochs $epoch
    make
    for dataset in textcattoy top-1000-near-google webkb;
    do
	mv $dataset/results.txt epoch_tuning/${dataset}_$epoch.txt
    done
done
echo "Done collecting data."