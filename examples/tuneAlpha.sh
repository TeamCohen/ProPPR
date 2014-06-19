#!/bin/bash


mkdir -p alpha_tuning
for alpha in 0.01 0.025 0.05 0.075 0.1 0.125 0.15 0.175 0.2;
do
    make clean.fast
    ./configure.sh --proppr /home/krivard/git/rinkitink/ProPPR --threads 3 --alpha $alpha
    make
    for dataset in textcattoy top-1000-near-google webkb;
    do
	mv $dataset/results.txt alpha_tuning/${dataset}_$alpha.txt
    done
done
echo "Done tuning."