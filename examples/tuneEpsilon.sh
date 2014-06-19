#!/bin/bash


mkdir -p eps_tuning
for epsilon in 1e-4 1e-5 1e-6 1e-7;
do
    make clean.fast
    ./configure.sh --proppr /home/krivard/git/rinkitink/ProPPR --threads 3 --epsilon $epsilon
    make
    for dataset in textcattoy top-1000-near-google webkb;
    do
	mv $dataset/results.txt eps_tuning/${dataset}_$epsilon.txt
    done
done
echo "Done collecting data."