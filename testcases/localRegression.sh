#!/bin/bash

FAILED=0
DIR="testcases/textcattoy"
echo "Cleaning regression directory..."
scripts/clean.sh $DIR
rm -f $DIR/log.log $DIR/params.wts $DIR/params.overweight
echo "Compiling logic program..."
scripts/compile.sh $DIR

if [ ! -f $DIR/programFiles.arg ]
then
    FAILED=1
    echo "FAILED: Trouble compiling. Troubleshoot your installation."
else
    PROGRAM=`cat $DIR/programFiles.arg`
    echo "Running regression experiment..."
    java -cp .:bin/:lib/*:conf/ edu.cmu.ml.praprolog.Experiment --programFiles ${PROGRAM%:} --train $DIR/toytrain.data --output $DIR/toytrain.cooked --test $DIR/toytest.data --params $DIR/params.wts --prover dpr --epochs 83 --srw l2plocal | tee $DIR/log.log 

    echo "Checking performance against gold standard..."
    sort -k 1b,1 $DIR/params.wts -o $DIR/params.wts
    RESULT=`grep pairTotal $DIR/log.log`
    RESULT_G=" INFO [Tester] pairTotal 7.0 pairErrors 0.0 errorRate 0.0 map 1.0 ggg"
#echo "$RESULT"
    if [ "$RESULT" != "$RESULT_G" ]
    then
	ARR=($RESULT)
	if [ "${ARR[3]}" != "7.0" ]; then echo "FAILED: expected pairTotal 7.0; got ${ARR[3]}."; FAILED=1; fi
	if [ "${ARR[5]}" != "0.0" ]; then echo "FAILED: expected pairErrors 0.0; got ${ARR[5]}."; FAILED=1; fi
	if [ "${ARR[7]}" != "0.0" ]; then echo "FAILED: expected errorRate 0.0; got ${ARR[7]}."; FAILED=1; fi
	if [ "${ARR[9]}" != "1.0" ]; then echo "FAILED: expected map 1.0; got ${ARR[9]}."; FAILED=1; fi
    fi
fi

if [ $FAILED -ne 1 ]
then
    echo "SUCCESS"
    echo "Checking param values against gold standard..."
    join $DIR/params.wts.gold $DIR/params.wts | awk '{ foo = sqrt( ($3-$2)^2 ); if (foo > .05) { print foo,$1 }}' > $DIR/params.overweight
    N=`wc -l $DIR/params.overweight | awk '{print $1}'`
#echo "$N"
    if [ $N -gt 0 ];
    then
	FAILED=1
	echo "SUSPICIOUS: Found $N params beyond normal range. This is occasionally due to a poor random seed. Re-run to verify. If problem persists, troubleshoot your installation."
    else
	echo "SUCCESS"
    fi
fi

if [ $FAILED -ne 1 ]
then
    echo "Regression passes."
fi


