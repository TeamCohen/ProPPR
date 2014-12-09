include ../Makefile.in

### DATASET

SETS:=baseball beatles google
NAMES:=$(addprefix top-1M-near-,${SETS})

PROGRAM=$(subst .test,,$(subst .train,,$*-recursive.wam:$*-fact.graph))
TRAIN=$(addsuffix .train,$(NAMES))
TEST=$(addsuffix .test,$(NAMES))
#${NAME}-recursive.crules:${NAME}.cfacts:${NAME}-fact.graph


### TARGETS
include ../common.in

all: results.txt

clean:
	rm -f *results.txt params.wts *.grounded

#results.txt: $(foreach name,$(NAMES),$(addsuffix .results.txt,pre.$(name).train post.$(name).train pre.$(name).test post.$(name).test))
results.txt: $(addsuffix .r,$(NAMES))
	echo phase.subset uR mR uMRR mMRR > $@
	cat $^ >> $@

%.r: pre.%.train.results.txt post.%.train.results.txt pre.%.test.results.txt post.%.test.results.txt
	@echo $*

post.%.train.solutions.txt: %.train.examples %.params.wts
	java ${JOPTS} -cp ${CP} edu.cmu.ml.proppr.QueryAnswerer --programFiles ${PROGRAM} --queries $< --solutions $@ --prover ${PROVER} --threads ${THREADS} --params $(word 2,$^)

post.%.test.solutions.txt: %.test.examples %.params.wts
	java ${JOPTS} -cp ${CP} edu.cmu.ml.proppr.QueryAnswerer --programFiles ${PROGRAM} --queries $< --solutions $@ --prover ${PROVER} --threads ${THREADS} --params $(word 2,$^)

%.params.wts: %.train.examples.grounded
	java ${JOPTS} -cp ${CP} edu.cmu.ml.proppr.Trainer --train $< --params $@ --threads ${THREADS} --srw ${SRW} --epochs ${EPOCHS}

.PRECIOUS: params.wts

.SECONDARY: