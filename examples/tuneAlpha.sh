#!/bin/bash


mkdir -p alpha_tuning
for alpha in 0.3 0.4 0.5 0.6 0.8 0.9; #0.2 0.1 0.02 0.01;
do
    make fast.clean
    ./configure.sh --proppr /home/krivard/git/rinkitink/ProPPR --threads 3 --alpha $alpha
    make
    for dataset in textcattoy top-1000-near-google webkb;
    do
	mv $dataset/results.txt alpha_tuning/${dataset}_$alpha.txt
    done
done
echo "Done tuning."