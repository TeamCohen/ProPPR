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

.PRECIOUS: %.examples params.wts %.solutions.txt
