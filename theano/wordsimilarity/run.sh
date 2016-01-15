#!/bin/bash

make clean
make init
rm -f proppr.log
for i in 1 2 3 4 5 6 7 8 9 10; 
do 
    make proppr | tee -a proppr.log
    make theano
done
