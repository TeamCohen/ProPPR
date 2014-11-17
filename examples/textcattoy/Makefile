include ../Makefile.in

### DATASET
TRAIN=train
TEST=test
PROGRAM=textcat.wam:toylabels.cfacts:toywords.graph


### TARGETS

all: results.txt

clean:
	rm -f *results.txt params.wts *.grounded

results.txt: pre.train.results.txt pre.test.results.txt post.train.results.txt post.test.results.txt
	cat $^ > $@

%.results.txt: %.Tester.log
	tail -1 $< | awk '{print "$*",$$2,$$3,$$4,$$5,$$6,$$7,$$8,$$9}' > $@

pre.%.Tester.log: %.examples
	java ${JOPTS} -cp ${CP} edu.cmu.ml.proppr.Tester --programFiles ${PROGRAM} --prover ${PROVER} --test $< --threads ${THREADS} --tester $(TESTER) | tee pre.$*.Tester.log

post.%.Tester.log: %.examples params.wts
	java ${JOPTS} -cp ${CP} edu.cmu.ml.proppr.Tester --programFiles ${PROGRAM} --prover ${PROVER} --test $< --threads ${THREADS} --params params.wts --tester $(TESTER) --srw $(SRW) | tee post.$*.Tester.log

params.wts: ${TRAIN}.examples.grounded
	java ${JOPTS} -cp ${CP} edu.cmu.ml.proppr.Trainer --train $< --params $@ --threads ${THREADS} --srw $(SRW) --epochs $(EPOCHS)

%.examples.grounded: %.examples
	java ${JOPTS} -cp ${CP} edu.cmu.ml.proppr.Grounder --programFiles ${PROGRAM} --queries $< --grounded $@ --prover ${PROVER} --threads ${THREADS}

#%.examples: %.solutions.txt %.queries
#	${SCRIPTS}/solutions2train.pl $*.queries $< $*.unreachable $@

%.solutions.txt: %.examples
	java ${JOPTS} -cp ${CP} edu.cmu.ml.proppr.QueryAnswerer --programFiles ${PROGRAM} --queries $< --solutions $@ --prover ${PROVER} --threads 1


%.trained.solutions.txt: %.examples params.wts
	java ${JOPTS} -cp ${CP} edu.cmu.ml.proppr.QueryAnswerer --programFiles ${PROGRAM} --queries $< --solutions $@ --prover ${PROVER} --threads 1 --params params.wts

.PRECIOUS: %.examples params.wts %.solutions.txt
