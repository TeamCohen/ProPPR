#!/bin/bash

for f in `find . -name "results.txt"`; do
    echo ${f%/results.txt}
    cat $f
done