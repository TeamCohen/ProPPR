include ../Makefile.in

### DATASET
TRAIN=train
TEST=test
PROGRAM=textcat.wam:toylabels.cfacts:toywords.graph

### TARGETS
include ../common.in

all: results.txt

clean:
	rm -f *results.txt params.wts *.grounded *.solutions.txt

results.txt: pre.train.results.txt post.train.results.txt pre.test.results.txt  post.test.results.txt
	echo phase subset uR mR uMRR mMRR > $@
	cat $^ >> $@

.PRECIOUS: %.examples params.wts %.solutions.txt
