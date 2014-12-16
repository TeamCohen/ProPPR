include ../Makefile.in

### DATASET

TRAIN=${NAME}.train
TEST=${NAME}.test
NAME=top-1000-near-google
PROGRAM=${NAME}-recursive.wam:${NAME}.cfacts:${NAME}-fact.graph


### TARGETS
include ../common.in

all: results.txt

clean:
	rm -f *results.txt params.wts *.grounded *.solutions.txt

graphKey: pre.${TRAIN}.examples.grounded post.${TRAIN}.examples.grounded

pre.%.examples.grounded: %.examples
	java ${JOPTS} -cp ${CP} edu.cmu.ml.proppr.Grounder --programFiles ${PROGRAM} --queries $< --grounded $@ --prover ${PROVER} --threads ${THREADS} --apr ${APR} --graphKey $@.key

post.%.examples.grounded: %.examples params.wts
	java ${JOPTS} -cp ${CP} edu.cmu.ml.proppr.Grounder --programFiles ${PROGRAM} --queries $< --grounded $@ --prover ${PROVER}:adjust --threads ${THREADS} --apr ${APR} --params params.wts --graphKey $@.key


.PRECIOUS: params.wts
