work_dir=$1
corpus=$2
cur_dir=`pwd`

echo "entering dir: $work_dir to generate config.json"
cd $work_dir


rnd=`echo $RANDOM`
steps=`awk '{print $0}' ../steps.conf | paste -d" " -s`
#echo steps:$steps

go_first=""

echo "{"  > config.json
echo "   \"language\" : \"PYTHON\","  >> config.json
echo "   \"main-file\" : \"mySpearmint.py\","  >> config.json
echo "   \"experiment-name\" : \"${corpus}_${rnd}\","  >> config.json
echo "   \"variables\" : {"  >> config.json

for step in $steps
do 
  para=`echo $step | awk -F":" '{print $1}'`
  para_size=`echo $step | awk -F":" '{print $2}'`
  total_line=`grep ${para} train.examples | wc -l`
  max=`python -c "print (${total_line}+${para_size}-1)/${para_size}"`
  if [ ! -z "$go_first" ];then
    echo "$go_first"  >> config.json
  fi
  echo "       \"${para}\" : {"  >> config.json
  echo "           \"type\" : \"int\","  >> config.json
  echo "           \"size\" : 1,"  >> config.json
  echo "           \"min\" : 0,"  >> config.json
  echo "           \"max\" : ${max}"  >> config.json
  go_first="       },"
done
echo "       }"  >> config.json
echo "   }"  >> config.json
echo "}"  >> config.json

\cp config.json ../config.json

echo "leaving dir: $work_dir, after finishing config.json"
cd $cur_dir


