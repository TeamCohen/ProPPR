trainDir=$1
jumpyNear1=$2
jumpyNear2=$3
coFailure=$4
mutexFailure1=$5
mutexFailure2=$6
smoothFailure1=$7
smoothFailure2=$8

cur=`pwd`
cd $trainDir
grep "predict1" all_train.examples > train.examples
grep "predict2" all_train.examples >> train.examples

grep jumpyNear1 all_train.examples | shuf | head -n ${jumpyNear1} >> train.examples
grep jumpyNear2 all_train.examples | shuf | head -n ${jumpyNear2} >> train.examples

grep coFailure all_train.examples | shuf | head -n ${coFailure} >> train.examples

grep mutexFailure1 all_train.examples | shuf | head -n ${mutexFailure1} >> train.examples
grep mutexFailure2 all_train.examples | shuf | head -n ${mutexFailure2} >> train.examples

grep smoothFailure1 all_train.examples | shuf | head -n ${smoothFailure1} >> train.examples
grep smoothFailure2 all_train.examples | shuf | head -n ${smoothFailure2} >> train.examples

cd $cur
