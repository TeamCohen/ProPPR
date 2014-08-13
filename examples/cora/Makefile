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
PROGRAM=cora.crules:cora.graph

### TARGETS

all: results.txt

clean:
	rm -f *results.txt params.wts *.cooked *.examples *.graph

results.txt: pre.${TRAIN}.results.txt pre.${TEST}.results.txt post.${TRAIN}.results.txt post.${TEST}.results.txt
	cat $^ > $@

%.results.txt: %.Tester.log
	tail -1 $< | awk '{print "$*",$$2,$$3,$$4,$$5,$$6,$$7,$$8,$$9}' > $@

pre.%.Tester.log: %.examples cora.graph
	java ${JOPTS} -cp ${CP} edu.cmu.ml.praprolog.Tester --tester ${TESTER} --programFiles ${PROGRAM} --prover ${PROVER} --test $< --threads ${THREADS} | tee pre.$*.Tester.log

post.%.Tester.log: %.examples params.wts cora.graph
	java ${JOPTS} -cp ${CP} edu.cmu.ml.praprolog.Tester --tester ${TESTER} --programFiles ${PROGRAM} --prover ${PROVER} --test $< --threads ${THREADS} --params params.wts | tee post.$*.Tester.log

params.wts: ${TRAIN}.examples.cooked
	java ${JOPTS} -cp ${CP} edu.cmu.ml.praprolog.Trainer --prover ${PROVER} --trainer ${TRAINER} --train $< --params $@ --threads ${THREADS} --epochs ${EPOCHS} --srw ${SRW}

%.examples.cooked: %.examples cora.graph
	java ${JOPTS} -cp ${CP} edu.cmu.ml.praprolog.ExampleCooker --programFiles ${PROGRAM} --data $< --output $@ --prover ${PROVER} --threads ${THREADS}

#%.examples: %.solutions.txt %.queries
#	${SCRIPTS}/solutions2train.pl $*.queries $< $*.unreachable $@

#%.solutions.txt: %.queries
#	java ${JOPTS} -cp ${CP} edu.cmu.ml.praprolog.QueryAnswerer --programFiles ${PROGRAM} --queries $< --output $@ --prover ${PROVER}

.PRECIOUS: params.wts

### DATASET BUILDING

cora.graph: train.raw test.raw
	scripts/getGraph.sh train.raw > $@
	scripts/getGraph.sh test.raw >> $@

train.examples: train.raw
	scripts/getData.pl $^ > $@

test.examples: test.raw
	scripts/getData.pl $^ > $@