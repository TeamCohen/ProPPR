#!/bin/bash +x

export PROPPR=../../../ProPPR
export PATH=$PATH:$PROPPR/scripts
export PROPPR_JAVA_ARGS=-Xmx120g

s=citeseer
eps=5e-6
depth=10
make $s.inputs
make $s.dataset
make $s.sample
make $s.eval PPR=all EPS=$eps DEPTH=$depth

cat one_spearmint_result

