include ../Makefile.in

### DATASET

SETS:=baseball beatles google
NAMES:=$(addprefix top-1M-near-,${SETS})

PROGRAM=$(subst .test,,$(subst .train,,$*-recursive.crules:$*-fact.graph))
TRAIN=$(addsuffix .train,$(NAMES))
TEST=$(addsuffix .test,$(NAMES))
#${NAME}-recursive.crules:${NAME}.cfacts:${NAME}-fact.graph


### TARGETS

all: results.txt

clean:
	rm -f *results.txt params.wts *.cooked

results.txt: $(addsuffix .results.txt,$(addprefix pre.,$(NAMES)) $(addprefix post.,$(NAMES)))
#pre.${TRAIN}.results.txt pre.${TEST}.results.txt post.${TRAIN}.results.txt post.${TEST}.results.txt
	cat $^ > $@

%.results.txt: %.Tester.log
	tail -1 $< | awk '{print "$*",$$2,$$3,$$4,$$5,$$6,$$7,$$8,$$9}' > $@

pre.%.Tester.log: $*
	java ${JOPTS} -cp ${CP} edu.cmu.ml.proppr.Tester --tester $(TESTER) --programFiles $(PROGRAM) --prover ${PROVER} --test $< --threads ${THREADS} | tee $@

post.%.train.Tester.log: %.train %.params.wts
	java ${JOPTS} -cp ${CP} edu.cmu.ml.proppr.Tester --tester $(TESTER) --programFiles $(PROGRAM) --prover ${PROVER} --test $< --threads ${THREADS} --params params.wts --srw $(SRW) | tee $@

post.%.test.Tester.log: %.test %.params.wts
	java ${JOPTS} -cp ${CP} edu.cmu.ml.proppr.Tester --tester $(TESTER) --programFiles $(PROGRAM) --prover ${PROVER} --test $< --threads ${THREADS} --params params.wts --srw $(SRW) | tee $@

%.params.wts: %.train.cooked
	java ${JOPTS} -cp ${CP} edu.cmu.ml.proppr.Trainer --prover ${PROVER} --trainer ${TRAINER} --train $< --params $@ --threads ${THREADS} --srw $(SRW) --epochs $(EPOCHS)

%.cooked: $*
	java ${JOPTS} -cp ${CP} edu.cmu.ml.proppr.ExampleCooker --programFiles $(PROGRAM) --data $< --output $@ --prover ${PROVER} --threads ${THREADS}

#%.examples: %.solutions.txt %.queries
#	${SCRIPTS}/solutions2train.pl $*.queries $< $*.unreachable $@

#%.solutions.txt: %.queries
#	java ${JOPTS} -cp ${CP} edu.cmu.ml.proppr.QueryAnswerer --programFiles $(PROGRAM) --queries $< --output $@ --prover ${PROVER}

.PRECIOUS: params.wts
