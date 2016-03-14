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

proppr.settings:
	proppr set $(TRAINER_OPTION) $(SRW_OPTION) --epochs $(EPOCHS) $(SQUASH_OPTION) --threads ${THREADS} --prover ${PROVER}
	proppr set --programFiles ${PROGRAM}

%.params: train_no_%.data.grounded
	proppr train $< $@ 

train_no_%.data.grounded: train_no_%.data
	proppr ground $^ $@

pre.%.solutions.txt: train_no_%.data
	proppr answer $< $@ 

post.%.solutions.txt: train_no_%.data %.params
	proppr answer $< $@ --params $(word 2,$^) 

.SECONDARY:
#.PRECIOUS: train_no_%.data.cooked %.params
