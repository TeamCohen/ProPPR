#!/bin/bash +x

export PROPPR=../../../ProPPR
export PATH=$PATH:$PROPPR/scripts
export PROPPR_JAVA_ARGS=-Xmx120g
export PYTHONPATH=YOUR_PATH_TO_SPEARMINT/:$PYTHONPATH

s=citeseer
p=all

echo $s > tmp_option
echo PPR=$p >> tmp_option
echo EPS=5e-6 >> tmp_option

make $s.inputs
make $s.dataset
python /opt/Spearmint/spearmint/main.py `pwd` 2>&1 | tee spearmint.log

