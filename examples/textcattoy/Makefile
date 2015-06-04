include ../Makefile.in

### DATASET
TRAIN=train
TEST=test
NAME=textcattoy
PROGRAM=${NAME}.crules:toylabels.cfacts:toywords.graph


### TARGETS

all: results.txt

clean:
	rm -f *results.txt params.wts *.cooked

results.txt: pre.train.results.txt pre.test.results.txt post.train.results.txt post.test.results.txt
	cat $^ > $@

%.results.txt: %.Tester.log
	tail -1 $< | awk '{print "$*","${NAME}",$$2,$$3,$$4,$$5,$$6,$$7,$$8,$$9}' > $@

pre.%.Tester.log: %.examples
	java ${JOPTS} -cp ${CP} edu.cmu.ml.praprolog.Tester --programFiles ${PROGRAM} --prover ${PROVER} --test $< --threads ${THREADS} --tester $(TESTER) | tee pre.$*.Tester.log

post.%.Tester.log: %.examples params.wts
	java ${JOPTS} -cp ${CP} edu.cmu.ml.praprolog.Tester --programFiles ${PROGRAM} --prover ${PROVER} --test $< --threads ${THREADS} --params params.wts --tester $(TESTER) --srw $(SRW) | tee post.$*.Tester.log

params.wts: ${TRAIN}.examples.cooked
	java ${JOPTS} -cp ${CP} edu.cmu.ml.praprolog.Trainer --prover ${PROVER} --train $< --params $@ --threads ${THREADS} --srw $(SRW) --epochs $(EPOCHS) --trainer $(TRAINER)

%.examples.cooked: %.examples
	java ${JOPTS} -cp ${CP} edu.cmu.ml.praprolog.ExampleCooker --programFiles ${PROGRAM} --data $< --output $@ --prover ${PROVER} --threads ${THREADS}

#%.examples: %.solutions.txt %.queries
#	${SCRIPTS}/solutions2train.pl $*.queries $< $*.unreachable $@

pre.%.solutions.txt: %.examples
	java ${JOPTS} -cp ${CP} edu.cmu.ml.praprolog.MultithreadedQueryAnswerer --programFiles ${PROGRAM} --queries $< --output $@ --prover ${PROVER} --threads ${THREADS}

post.%.solutions.txt: %.examples params.wts
	java ${JOPTS} -cp ${CP} edu.cmu.ml.praprolog.MultithreadedQueryAnswerer --programFiles ${PROGRAM} --queries $< --output $@ --prover ${PROVER} --threads ${THREADS} --params params.wts


.PRECIOUS: %.examples params.wts %.solutions.txt
