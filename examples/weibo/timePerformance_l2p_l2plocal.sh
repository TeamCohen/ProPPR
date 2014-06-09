#!/bin/bash

for var in l2plocal l2p; do
    for i in 1 2 3; do SRW=$var /usr/bin/time -ao $var.timeLog  make train.train ; done
    grep user $var.timeLog | awk '{gsub("[a-z]","",$3); en=split($3,foo,":"); em=foo[en]*1000+foo[en-1]*60*1000; print em,$1*1000,$2*1000}' | sed 's/[a-z]//g;s/ /\t/g' > $var.times
done




