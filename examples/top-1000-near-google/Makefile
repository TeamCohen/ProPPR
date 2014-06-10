include ../Makefile.in

### DATASET

TRAIN=${NAME}.train
TEST=${NAME}.test
NAME=top-1000-near-google
PROGRAM=${NAME}-nonrecursive.crules:${NAME}.cfacts:${NAME}-fact.graph


### TARGETS

all: results.txt

clean:
	rm -f *results.txt params.wts *.cooked

results.txt: pre.${TRAIN}.results.txt pre.${TEST}.results.txt post.${TRAIN}.results.txt post.${TEST}.results.txt
	cat $^ > $@

%.results.txt: %.Tester.log
	tail -1 $< | awk '{print "$*",$$2,$$3,$$4,$$5,$$6,$$7,$$8,$$9}' > $@

pre.%.Tester.log: %.examples
	java ${JOPTS} -cp ${CP} edu.cmu.ml.praprolog.Tester --tester rt --programFiles ${PROGRAM} --prover ${PROVER} --test $< --threads ${THREADS} | tee pre.$*.Tester.log

post.%.Tester.log: %.examples params.wts
	java ${JOPTS} -cp ${CP} edu.cmu.ml.praprolog.Tester --tester rt --programFiles ${PROGRAM} --prover ${PROVER} --test $< --threads ${THREADS} --params params.wts --srw $(SRW) | tee post.$*.Tester.log

params.wts: ${TRAIN}.examples.cooked
	java ${JOPTS} -cp ${CP} edu.cmu.ml.praprolog.Trainer --prover ${PROVER} --train $< --params $@ --threads ${THREADS} --srw $(SRW) --epochs $(EPOCHS)

%.examples.cooked: %.examples
	java ${JOPTS} -cp ${CP} edu.cmu.ml.praprolog.ExampleCooker --programFiles ${PROGRAM} --data $< --output $@ --prover ${PROVER} --threads ${THREADS}

#%.examples: %.solutions.txt %.queries
#	${SCRIPTS}/solutions2train.pl $*.queries $< $*.unreachable $@

#%.solutions.txt: %.queries
#	java ${JOPTS} -cp ${CP} edu.cmu.ml.praprolog.QueryAnswerer --programFiles ${PROGRAM} --queries $< --output $@ --prover ${PROVER}

.PRECIOUS: params.wts
