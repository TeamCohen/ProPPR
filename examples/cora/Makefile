include ../Makefile.in

### PARAMETERS ###
ifeq ($(strip $(PROVER)),)
PROVER=dpr:1e-5
endif
ifeq ($(strip $(THREADS)),)
THREADS=4
endif
ifeq ($(strip $(EPOCHS)),)
EPOCHS=30
endif

### DATASET

TRAIN=train
TEST=test
PROGRAM=cora.wam:cora.graph

### TARGETS
include ../common.in

all: results.txt

clean:
	rm -f *results.txt params.wts *.grounded *.examples *.graph *.solutions.txt

results.txt: pre.${TRAIN}.results.txt post.${TRAIN}.results.txt pre.${TEST}.results.txt post.${TEST}.results.txt
	echo phase subset uR mR uMRR mMRR > $@
	cat $^ >> $@

.PRECIOUS: params.wts

### DATASET BUILDING

cora.graph: train.raw test.raw
	scripts/getGraph.sh train.raw > $@
	scripts/getGraph.sh test.raw >> $@

train.examples: train.raw
	scripts/getData.pl $^ > $@

test.examples: test.raw
	scripts/getData.pl $^ > $@