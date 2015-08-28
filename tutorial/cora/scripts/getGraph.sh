# input is *.raw, output is sld format graph
cat $* | sed '/Same/d' | sed '/^ *$/d' | sed 's/(/\t/' | sed 's/,/\t/' | sed 's/)//' | tr '[:upper:]' '[:lower:]' > regular.tmp;
awk '{print $1"inverse\t"$3"\t"$2}' regular.tmp > inverse.tmp;
cat regular.tmp inverse.tmp;