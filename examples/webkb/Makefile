include ../Makefile.in

ifneq ($(strip $(SRW)),)
SRW_OPTION=--srw $(SRW)
endif
ifneq ($(strip $(TRAINER)),)
TRAINER_OPTION=--trainer $(TRAINER)
endif
ifneq ($(strip $(SQUASHINGFUNCTION)),)
SQUASH_OPTION=--squashingFunction $(SQUASHINGFUNCTION)
endif

### DATASET
PROGRAM:=webkb0.wam:webkb.graph:webkb.cfacts
SUBSETS:=wisc wash texas cornell

### TARGETS

all: results.txt

clean:
	rm -f *results.txt *.params *.grounded *.solutions.txt

results.txt: $(addsuffix .results.txt,${SUBSETS})
	echo phase.subset uR mR uMRR mMRR uMAP mMAP > $@
	cat $^ >> $@

%.results.txt: pre.%.solutions.txt post.%.solutions.txt train_no_%.data
	../scripts/makeResults.sh $(word 3,$^) $(word 1,$^) pre.$*   > $@
	../scripts/makeResults.sh $(word 3,$^) $(word 2,$^) post.$* >> $@	

%.params: train_no_%.data.grounded
	java ${JOPTS} -cp ${CP} edu.cmu.ml.proppr.Trainer $(TRAINER_OPTION) --train $< --params $@ --threads ${THREADS} $(SRW_OPTION) --epochs $(EPOCHS) $(SQUASH_OPTION) 

train_no_%.data.grounded: train_no_%.data
	java ${JOPTS} -cp ${CP} edu.cmu.ml.proppr.Grounder --programFiles ${PROGRAM} --queries $^ --grounded $@ --prover ${PROVER} --threads ${THREADS} $(SQUASH_OPTION)

pre.%.solutions.txt: train_no_%.data
	java ${JOPTS} -cp ${CP} edu.cmu.ml.proppr.QueryAnswerer --programFiles ${PROGRAM} --queries $^ --solutions $@ --prover ${PROVER} --threads ${THREADS} $(SQUASH_OPTION)

post.%.solutions.txt: train_no_%.data %.params
	java ${JOPTS} -cp ${CP} edu.cmu.ml.proppr.QueryAnswerer --programFiles ${PROGRAM} --queries $< --solutions $@ --prover ${PROVER} --threads ${THREADS} --params $(word 2,$^) $(SQUASH_OPTION)

.SECONDARY:
#.PRECIOUS: train_no_%.data.cooked %.params
