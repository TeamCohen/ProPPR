include ../Makefile.in

### DATASET
TRAIN=train
TEST=test
PROGRAM=textcat.wam:toylabels.cfacts:toywords.graph

SRW=ppr:reg=l2:sched=local:loss=posneg
### TARGETS
include ../common.in

all: results.txt
	cat $^

clean:
	rm -f *results.txt params.wts *.grounded *.solutions.txt

.PRECIOUS: %.examples params.wts %.solutions.txt

results.txt: pre.${TRAIN}.results.txt post.${TRAIN}.results.txt pre.${TEST}.results.txt post.${TEST}.results.txt
	echo phase subset uR mR uMRR mMRR uMAP mMAP > $@
	cat $^ >> $@
